package io.quarkiverse.desktop.awt.runtime.graal;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.InputMethodEvent;

import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * AWT and Swing check with reflection whether the classes they meet override some of their methods
 * ({@code getDeclaredMethod} : the method, or a {@code NoSuchMethodException}) :
 * <ul>
 * <li>{@code Component} : {@code coalesceEvents(AWTEvent, AWTEvent)}, in the class of each component and in its
 * superclasses ({@code Component.isCoalesceEventsOverriden}) ;</li>
 * <li>{@code JTextComponent} : {@code processInputMethodEvent(InputMethodEvent)}, in the class of each text component
 * and in its superclasses below {@code JTextComponent} ({@code JTextComponent.METHOD_OVERRIDDEN}) ;</li>
 * <li>{@code PlainView} : the drawing methods ({@code drawLine}, {@code drawSelectedText}, {@code drawUnselectedText},
 * {@code drawEchoCharacter}, in their {@code int} and {@code float} variants), in the class of each plain text view and
 * in its superclasses outside the JDK ({@code PlainView.isFPMethodOverridden}).</li>
 * </ul>
 * For every such class of the application or of a library that the analysis reaches, whether it is in the Jandex index
 * or not, this registers the method when the class declares it, or the lookup expected to fail otherwise (which
 * {@code --exact-reachability-metadata} needs) : a library component (a SwingX {@code JXLabel}, an RSyntaxTextArea text
 * area...) and the application classes that extend it are then created as in JVM mode. The classes of the JDK are
 * registered by the lists of the extensions.
 */
public final class OverrideChecksFeature implements Feature {

    @Override
    public String getDescription() {
        return "Quarkus Desktop AWT : the method override checks of AWT and Swing";
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        access.registerSubtypeReachabilityHandler(
                (duringAnalysis, type) -> queried(type, "coalesceEvents", AWTEvent.class, AWTEvent.class), Component.class);
        access.registerSubtypeReachabilityHandler(
                (duringAnalysis, type) -> queried(type, "processInputMethodEvent", InputMethodEvent.class),
                JTextComponent.class);
        access.registerSubtypeReachabilityHandler((duringAnalysis, type) -> {
            if (isApplication(type)) {
                // the drawing methods in both variants, declared or not
                RuntimeReflection.registerAllDeclaredMethods(type);
            }
        }, PlainView.class);
    }

    /**
     * Registers the query of a method of an application or library class : the method when the class declares it, the
     * lookup expected to fail otherwise.
     */
    static void queried(Class<?> type, String name, Class<?>... parameterTypes) {
        if (!isApplication(type)) {
            return;
        }
        try {
            RuntimeReflection.registerAsQueried(type.getDeclaredMethod(name, parameterTypes));
        } catch (NoSuchMethodException e) {
            RuntimeReflection.registerMethodLookup(type, name, parameterTypes);
        } catch (LinkageError e) {
            // a class whose methods cannot be resolved (a type of their signatures is missing) : AWT or Swing fail to
            // check it in JVM mode too
        }
    }

    /**
     * Whether the class is not a class of the JDK : AWT and Swing do not check the classes of the boot class loader for
     * {@code coalesceEvents}, nor those of {@code java.desktop} for the drawing methods ; the JDK text components are
     * registered by the lists of the extensions.
     */
    static boolean isApplication(Class<?> type) {
        return type.getClassLoader() != null;
    }
}
