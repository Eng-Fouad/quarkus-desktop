package io.quarkiverse.desktop.swing.deployment;

import io.quarkiverse.desktop.awt.deployment.AwtClassesAndResources;

/**
 * What Swing needs in a native executable, on top of {@link AwtClassesAndResources} : the look and feels (Nimbus,
 * Synth, Windows, GTK, Multi), text editor kits, formatters and renderers, the file chooser shell folders, and the Swing
 * API used reflectively. The parts of Swing that AWT itself uses (print dialogs, input method windows, the Linux text
 * peers, the Basic and Metal UI delegates they need) are registered by {@link AwtClassesAndResources}.
 * <p>
 * <b>Lists.</b> Same conventions as {@link AwtClassesAndResources} : each kind of registration has a list for all
 * platforms ({@code KIND}) and one list per platform ({@code WINDOWS_KIND}, {@code LINUX_KIND}, {@code MAC_KIND}) : a
 * native executable gets the common list and the list of the platform it is built for. A platform list only holds what
 * that platform needs alone. Within a list, entries are grouped by area, with a short comment per group, and sorted
 * within a group. A list is a package-private {@code static String[]} field named {@code [PLATFORM_]KIND} : tools read
 * these fields reflectively. Any other constant of this class is a {@code static final String} (or not a
 * {@code String[]}), which these tools ignore.
 * <p>
 * <b>Target platform.</b> Windows when the build host is Windows and the build is not a container build, Linux
 * otherwise. There is no macOS target, so the {@code MAC_} lists are not declared.
 * <p>
 * <b>Kinds and entry formats.</b> Class names are binary names ({@code javax.swing.JTable$GenericEditor}). Parameter
 * types are binary names, primitive type names, or either followed by {@code []} for arrays; {@code ()} means no
 * parameters; a constructor is named {@code <init>}.
 * <ul>
 * <li>{@code RUNTIME_INITIALIZED_PACKAGES} : packages whose classes are initialized at run time (sub packages
 * included).</li>
 * <li>{@code RUNTIME_INITIALIZED_CLASSES} : classes initialized at run time.</li>
 * <li>{@code REFLECTIVE_CLASSES} : classes registered for reflection with all their constructors, methods and
 * fields.</li>
 * <li>{@code REFLECTIVE_CONSTRUCTORS} : classes registered for reflection with their constructors.</li>
 * <li>{@code REFLECTIVE_METHODS} : single methods registered for reflection, {@code "fqcn#name(paramType,...)"}.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_CLASSES} : classes reached from native code, with all their constructors, methods and
 * fields.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_METHODS} : single methods or constructors reached from native code,
 * {@code "fqcn#name(paramType,...)"}.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_FIELDS} : single fields reached from native code, {@code "fqcn#field"}.</li>
 * <li>{@code RESOURCE_BUNDLES} : resource bundle base names (module {@code java.desktop} unless stated
 * otherwise).</li>
 * <li>{@code RESOURCE_GLOBS} : resources included in the executable, as glob patterns.</li>
 * <li>{@code SERVICE_PROVIDERS} : service provider classes of JDK modules, registered for reflection (constructors and
 * methods).</li>
 * </ul>
 */
public final class SwingClassesAndResources {

    private SwingClassesAndResources() {
        // Constants
    }

    // ----------------------------------------------------------------------------------------- run time initialization
    // Quarkus initializes every class at build time unless told otherwise. These classes are initialized at run time :
    // their static initializer loads native libraries or creates native state, starts threads, depends on the running
    // platform or toolkit, or reads system properties (their values would be frozen at build time).

    static String[] RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] WINDOWS_RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] LINUX_RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] RUNTIME_INITIALIZED_CLASSES = {
    };

    static String[] WINDOWS_RUNTIME_INITIALIZED_CLASSES = {
    };

    static String[] LINUX_RUNTIME_INITIALIZED_CLASSES = {
    };

    // ------------------------------------------------------------------------------------------------------ reflection

    static String[] REFLECTIVE_CLASSES = {
    };

    static String[] WINDOWS_REFLECTIVE_CLASSES = {
    };

    static String[] LINUX_REFLECTIVE_CLASSES = {
    };

    static String[] REFLECTIVE_CONSTRUCTORS = {
    };

    static String[] WINDOWS_REFLECTIVE_CONSTRUCTORS = {
    };

    static String[] LINUX_REFLECTIVE_CONSTRUCTORS = {
    };

    static String[] REFLECTIVE_METHODS = {
    };

    static String[] WINDOWS_REFLECTIVE_METHODS = {
    };

    static String[] LINUX_REFLECTIVE_METHODS = {
    };

    // ------------------------------------------------------------------------------------------------------------- JNI

    static String[] JNI_RUNTIME_ACCESS_CLASSES = {
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_CLASSES = {
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_CLASSES = {
    };

    static String[] JNI_RUNTIME_ACCESS_METHODS = {
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_METHODS = {
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_METHODS = {
    };

    static String[] JNI_RUNTIME_ACCESS_FIELDS = {
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_FIELDS = {
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_FIELDS = {
    };

    // ----------------------------------------------------------------------------------------------- service providers

    static String[] SERVICE_PROVIDERS = {
    };

    static String[] WINDOWS_SERVICE_PROVIDERS = {
    };

    static String[] LINUX_SERVICE_PROVIDERS = {
    };

    // ------------------------------------------------------------------------------------------------ resource bundles

    static String[] RESOURCE_BUNDLES = {
    };

    static String[] WINDOWS_RESOURCE_BUNDLES = {
    };

    static String[] LINUX_RESOURCE_BUNDLES = {
    };

    // ------------------------------------------------------------------------------------------------------- resources

    static String[] RESOURCE_GLOBS = {
    };

    static String[] WINDOWS_RESOURCE_GLOBS = {
    };

    static String[] LINUX_RESOURCE_GLOBS = {
    };
}
