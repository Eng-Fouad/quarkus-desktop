package io.quarkiverse.desktop.awt.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.builder.Json;

/**
 * The lookups of the JDK desktop code that a native executable built with {@code --exact-reachability-metadata} must
 * know (see {@link ReachabilityLookupsBuildItem}), and the {@code reachability-metadata.json} file that registers them.
 * Used by the Desktop AWT and Desktop Swing extensions.
 */
public final class ReachabilityLookups {

    /**
     * The resource of the reachability metadata of the lookups.
     */
    static final String REACHABILITY_METADATA = "META-INF/native-image/io.quarkiverse.desktop/"
            + "quarkus-desktop-awt-lookups/reachability-metadata.json";

    /**
     * The suffixes of the classes that the JavaBeans API looks up for a class ({@code Introspector} : bean info and
     * customizer, {@code XMLEncoder} : persistence delegate, {@code PropertyEditorManager} : property editor).
     */
    private static final List<String> JAVA_BEANS_SUFFIXES = List.of("BeanInfo", "Customizer", "PersistenceDelegate",
            "Editor");

    private ReachabilityLookups() {
    }

    /**
     * The lookups of the JavaBeans API for the given classes and their superclasses : the classes it looks up by name
     * and that do not exist in the JDK ({@code java.awt.ButtonBeanInfo}, {@code java.awt.ButtonCustomizer},
     * {@code java.awt.ButtonPersistenceDelegate}, {@code java.beans.MetaData$java_awt_Button_PersistenceDelegate},
     * {@code java.awt.ButtonEditor}, {@code com.sun.beans.editors.ButtonEditor}), and the supertypes (superclasses and
     * interfaces) whose members it queries.
     *
     * @return binary names of classes, sorted
     */
    public static Set<String> javaBeansTypes(Collection<String> classNames) {
        Set<String> types = new TreeSet<>();
        Set<Class<?>> seen = new LinkedHashSet<>();
        for (String className : classNames) {
            Optional<Class<?>> type = load(className);
            if (type.isEmpty()) {
                continue;
            }
            Deque<Class<?>> supertypes = new ArrayDeque<>(List.of(type.get()));
            while (!supertypes.isEmpty()) {
                Class<?> current = supertypes.pop();
                if (!seen.add(current)) {
                    continue;
                }
                types.add(current.getName());
                if (current.getSuperclass() != null) {
                    supertypes.push(current.getSuperclass());
                }
                supertypes.addAll(List.of(current.getInterfaces()));
            }
        }
        for (Class<?> type : seen) {
            if (type.isInterface()) {
                continue;
            }
            String name = type.getName();
            List<String> candidates = new java.util.ArrayList<>();
            for (String suffix : JAVA_BEANS_SUFFIXES) {
                candidates.add(name + suffix);
            }
            candidates.add("java.beans.MetaData$" + name.replace('.', '_') + "_PersistenceDelegate");
            candidates.add("com.sun.beans.editors." + name.substring(name.lastIndexOf('.') + 1) + "Editor");
            for (String candidate : candidates) {
                if (load(candidate).isEmpty()) {
                    types.add(candidate);
                }
            }
        }
        return types;
    }

    /**
     * The serialized forms that {@code Beans.instantiate} looks up first for a class name
     * ({@code java/awt/Button.ser}...) : one glob per package of the given classes.
     */
    static Set<String> javaBeansSerializedForms(Collection<String> classNames) {
        Set<String> globs = new TreeSet<>();
        for (String className : classNames) {
            int dot = className.lastIndexOf('.');
            if (dot > 0) {
                globs.add(className.substring(0, dot).replace('.', '/') + "/*.ser");
            }
        }
        return globs;
    }

    /**
     * The lookups of resource bundles of the JDK that do not exist, for the given locales : the classes (the bundle, its
     * locale variants, its {@code ResourceBundleProvider} in the {@code spi} sub package), and the {@code .properties}
     * files of the bundle.
     */
    static ReachabilityLookupsBuildItem missingBundles(Collection<String> bundles, Collection<Locale> locales,
            String module) {
        Set<String> types = new TreeSet<>();
        Set<String> globs = new TreeSet<>();
        ResourceBundle.Control control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);
        for (String bundle : bundles) {
            types.add(bundle);
            for (Locale locale : locales) {
                for (Locale candidate : control.getCandidateLocales(bundle, locale)) {
                    if (!candidate.equals(Locale.ROOT)) {
                        types.add(control.toBundleName(bundle, candidate));
                    }
                }
            }
            int dot = bundle.lastIndexOf('.');
            types.add(bundle.substring(0, dot) + ".spi." + bundle.substring(dot + 1) + "Provider");
            String path = bundle.replace('.', '/');
            globs.add(path + "*.properties");
            globs.add(module + ":" + path + "*.properties");
        }
        return new ReachabilityLookupsBuildItem(types, List.of(), globs);
    }

    /**
     * The {@code .properties} files that {@code ResourceBundle} looks up next to the classes of the given bundles of the
     * JDK, for the locales without a class ({@code basic_en.properties}...) : in the module of the bundle and on the
     * class path. The bundles that are properties files are skipped.
     */
    public static Set<String> bundlePropertiesGlobs(Collection<String> bundles) {
        Set<String> globs = new TreeSet<>();
        for (String bundle : bundles) {
            Optional<Class<?>> type = load(bundle);
            if (type.isEmpty() || type.get().getModule().getName() == null) {
                continue;
            }
            String glob = bundle.replace('.', '/') + "_*.properties";
            globs.add(glob);
            globs.add(type.get().getModule().getName() + ":" + glob);
        }
        return globs;
    }

    /**
     * The given resource globs that address a resource directory of a module of the JDK, qualified with that module :
     * {@code Module.getResourceAsStream} lookups (the icons of the look and feels...) only match module resources.
     */
    public static Set<String> moduleGlobs(Collection<String> globs) {
        Set<String> moduleGlobs = new TreeSet<>();
        for (String glob : globs) {
            for (String module : modules(glob)) {
                moduleGlobs.add(module + ":" + glob);
            }
        }
        return moduleGlobs;
    }

    /**
     * The modules of the JDK that have the directory of the given resource glob (the part before its first wildcard).
     *
     * @return module names, sorted, empty when no module has the directory
     */
    public static Set<String> modules(String glob) {
        Set<String> modules = new TreeSet<>();
        int wildcard = indexOfWildcard(glob);
        int slash = glob.lastIndexOf('/', wildcard < 0 ? glob.length() : wildcard);
        if (slash <= 0) {
            return modules;
        }
        String directory = glob.substring(0, slash);
        FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        for (Module module : ModuleLayer.boot().modules()) {
            if (Files.isDirectory(jrt.getPath("/modules", module.getName(), directory))) {
                modules.add(module.getName());
            }
        }
        return modules;
    }

    private static int indexOfWildcard(String glob) {
        int index = -1;
        for (char c : new char[] { '*', '?', '[', '{' }) {
            int i = glob.indexOf(c);
            if (i >= 0 && (index < 0 || i < index)) {
                index = i;
            }
        }
        return index;
    }

    /**
     * The {@code reachability-metadata.json} of the lookups.
     */
    static String reachabilityMetadata(Collection<ReachabilityLookupsBuildItem> lookups) {
        Set<String> types = new TreeSet<>();
        Set<String> methods = new TreeSet<>();
        Set<String> globs = new TreeSet<>();
        for (ReachabilityLookupsBuildItem item : lookups) {
            types.addAll(item.getTypes());
            methods.addAll(item.getMethods());
            globs.addAll(item.getResourceGlobs());
        }
        Json.JsonArrayBuilder reflection = Json.array();
        java.util.Map<String, Json.JsonArrayBuilder> methodsByType = new java.util.TreeMap<>();
        for (String method : methods) {
            MemberEntry entry = MemberEntry.method(method);
            Json.JsonArrayBuilder parameterTypes = Json.array();
            for (String parameterType : entry.parameterTypes()) {
                parameterTypes.add(parameterType);
            }
            methodsByType.computeIfAbsent(entry.className(), c -> Json.array())
                    .add(Json.object().put("name", entry.name()).put("parameterTypes", parameterTypes));
        }
        for (String type : types) {
            if (!methodsByType.containsKey(type)) {
                reflection.add(Json.object().put("type", type));
            }
        }
        methodsByType.forEach((type, typeMethods) -> reflection.add(Json.object().put("type", type)
                .put("methods", typeMethods)));
        Json.JsonArrayBuilder resources = Json.array();
        for (String glob : globs) {
            int colon = glob.indexOf(':');
            resources.add(colon < 0 ? Json.object().put("glob", glob)
                    : Json.object().put("module", glob.substring(0, colon)).put("glob", glob.substring(colon + 1)));
        }
        StringBuilder json = new StringBuilder();
        try {
            Json.object().put("reflection", reflection).put("resources", resources).appendTo(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return json.toString();
    }

    /**
     * The class of the JDK (or of the class path of the build) with this name, not initialized.
     */
    static Optional<Class<?>> load(String className) {
        for (ClassLoader loader : new ClassLoader[] { Thread.currentThread().getContextClassLoader(),
                ReachabilityLookups.class.getClassLoader() }) {
            if (loader == null) {
                continue;
            }
            try {
                return Optional.of(Class.forName(className, false, loader));
            } catch (ClassNotFoundException | LinkageError e) {
                // try the next class loader
            }
        }
        return Optional.empty();
    }
}
