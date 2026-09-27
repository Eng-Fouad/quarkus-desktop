package io.quarkiverse.desktop.swing.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * The application classes that Swing uses with reflection, found in the Jandex index : Swing creates some of them by
 * class name, and checks with reflection whether others override some methods.
 * <ul>
 * <li>UI delegates ({@code javax.swing.plaf.ComponentUI} subclasses) : {@code UIDefaults} finds them by class name (the
 * value of a UI class ID in the look and feel defaults) and creates them with their static
 * {@code createUI(JComponent)} method.</li>
 * <li>Look and feels ({@code javax.swing.LookAndFeel} subclasses) : set by class name
 * ({@code UIManager.setLookAndFeel(String)}, {@code swing.defaultlaf}, {@code swing.auxiliarylaf},
 * {@code quarkus.desktop.swing.look-and-feel}), created with their no argument constructor.</li>
 * <li>Editor kits ({@code javax.swing.text.EditorKit} subclasses) : registered by class name for a content type
 * ({@code JEditorPane.registerEditorKitForContentType}), created with their no argument constructor.</li>
 * <li>Synth painters ({@code javax.swing.plaf.synth.SynthPainter} subclasses) : declared in Synth XML files, created by
 * the beans decoder.</li>
 * <li>Plain text views ({@code javax.swing.text.PlainView} subclasses) : Swing checks with reflection which drawing
 * methods they override ({@code drawLine}, {@code drawSelectedText}, {@code drawUnselectedText},
 * {@code drawEchoCharacter}, in their {@code int} and {@code float} variants).</li>
 * <li>Text components ({@code javax.swing.text.JTextComponent} subclasses) : Swing checks with reflection whether they
 * override {@code processInputMethodEvent}.</li>
 * </ul>
 */
final class SwingApplicationClasses {

    static final DotName COMPONENT_UI = DotName.createSimple("javax.swing.plaf.ComponentUI");
    static final DotName LOOK_AND_FEEL = DotName.createSimple("javax.swing.LookAndFeel");
    static final DotName EDITOR_KIT = DotName.createSimple("javax.swing.text.EditorKit");
    static final DotName SYNTH_PAINTER = DotName.createSimple("javax.swing.plaf.synth.SynthPainter");
    static final DotName PLAIN_VIEW = DotName.createSimple("javax.swing.text.PlainView");
    static final DotName TEXT_COMPONENT = DotName.createSimple("javax.swing.text.JTextComponent");

    private static final DotName J_COMPONENT = DotName.createSimple("javax.swing.JComponent");
    private static final DotName INPUT_METHOD_EVENT = DotName.createSimple("java.awt.event.InputMethodEvent");

    /**
     * The classes Swing creates by name, registered for reflection with their constructors.
     */
    final Set<String> constructed = new TreeSet<>();

    /**
     * The static {@code createUI(JComponent)} methods of the UI delegates, by class.
     */
    final Map<String, MethodInfo> createUIMethods = new TreeMap<>();

    /**
     * The classes whose declared methods Swing queries (override checks).
     */
    final Set<String> queried = new TreeSet<>();

    /**
     * The {@code processInputMethodEvent(InputMethodEvent)} methods of text components.
     */
    final Map<String, MethodInfo> inputMethodHandlers = new TreeMap<>();

    /**
     * The text components that do not declare {@code processInputMethodEvent(InputMethodEvent)} : Swing looks the
     * method up, and expects not to find it ({@code "fqcn#processInputMethodEvent(java.awt.event.InputMethodEvent)"}).
     */
    final Set<String> inputMethodLookups = new TreeSet<>();

    /**
     * The icon directories of the application look and feels : the look and feels of the JDK load their icons with
     * {@code SwingUtilities2.makeIcon(getClass(), ...)}, which looks in the package of the class of the look and feel
     * first ({@code com/example/laf/icons/sortUp.png}), as glob patterns.
     */
    final Set<String> lookAndFeelIconGlobs = new TreeSet<>();

    private SwingApplicationClasses() {
    }

    /**
     * Finds the classes of the index that Swing uses with reflection.
     *
     * @param index the index of the application
     * @param classLoader the class loader of the application : the superclasses that are not in the index (the JDK
     *        classes) are loaded, without initialization
     */
    static SwingApplicationClasses scan(IndexView index, ClassLoader classLoader) {
        SwingApplicationClasses result = new SwingApplicationClasses();
        Hierarchy hierarchy = new Hierarchy(index, classLoader);
        for (ClassInfo classInfo : index.getKnownClasses()) {
            if (classInfo.isInterface() || classInfo.isAnnotation() || classInfo.isEnum() || classInfo.isRecord()) {
                continue;
            }
            String name = classInfo.name().toString();
            if (hierarchy.isSubclass(classInfo.name(), COMPONENT_UI)) {
                result.constructed.add(name);
                MethodInfo createUI = classInfo.method("createUI", Type.create(J_COMPONENT, Type.Kind.CLASS));
                if (createUI != null && Modifier.isStatic(createUI.flags())) {
                    result.createUIMethods.put(name, createUI);
                }
            }
            if (!classInfo.isAbstract() && (hierarchy.isSubclass(classInfo.name(), LOOK_AND_FEEL)
                    || hierarchy.isSubclass(classInfo.name(), EDITOR_KIT)
                    || hierarchy.isSubclass(classInfo.name(), SYNTH_PAINTER))) {
                result.constructed.add(name);
            }
            if (hierarchy.isSubclass(classInfo.name(), LOOK_AND_FEEL)) {
                String packageName = classInfo.name().packagePrefix();
                result.lookAndFeelIconGlobs.add((packageName == null ? "" : packageName.replace('.', '/') + "/")
                        + "icons/*");
            }
            if (hierarchy.isSubclass(classInfo.name(), PLAIN_VIEW)) {
                result.queried.add(name);
            }
            if (hierarchy.isSubclass(classInfo.name(), TEXT_COMPONENT)) {
                MethodInfo handler = classInfo.method("processInputMethodEvent",
                        Type.create(INPUT_METHOD_EVENT, Type.Kind.CLASS));
                if (handler != null) {
                    result.inputMethodHandlers.put(name, handler);
                } else {
                    result.inputMethodLookups.add(name + "#processInputMethodEvent(" + INPUT_METHOD_EVENT + ")");
                }
            }
        }
        return result;
    }

    /**
     * Answers subclass queries with the index for the application classes, and with class loading (without
     * initialization) for the others, with a cache.
     */
    static final class Hierarchy {

        private final IndexView index;
        private final ClassLoader classLoader;
        private final Map<DotName, Optional<Class<?>>> loaded = new HashMap<>();

        Hierarchy(IndexView index, ClassLoader classLoader) {
            this.index = index;
            this.classLoader = classLoader;
        }

        /**
         * Whether the class extends the given one (or is it).
         */
        boolean isSubclass(DotName name, DotName superclass) {
            DotName current = name;
            while (current != null) {
                if (current.equals(superclass)) {
                    return true;
                }
                ClassInfo classInfo = index.getClassByName(current);
                if (classInfo == null) {
                    Optional<Class<?>> type = load(current);
                    Optional<Class<?>> expected = load(superclass);
                    return type.isPresent() && expected.isPresent() && expected.get().isAssignableFrom(type.get());
                }
                current = classInfo.superName();
            }
            return false;
        }

        private Optional<Class<?>> load(DotName name) {
            return loaded.computeIfAbsent(name, n -> {
                for (ClassLoader loader : Stream.of(classLoader, SwingApplicationClasses.class.getClassLoader())
                        .filter(l -> l != null).toList()) {
                    try {
                        return Optional.of(Class.forName(n.toString(), false, loader));
                    } catch (ClassNotFoundException | LinkageError e) {
                        // try the next class loader
                    }
                }
                return Optional.empty();
            });
        }
    }

    /**
     * The names of the classes found, for logs.
     */
    List<String> summary() {
        return List.of("constructed=" + constructed, "createUI=" + createUIMethods.keySet(), "queried=" + queried,
                "inputMethodHandlers=" + inputMethodHandlers.keySet());
    }
}
