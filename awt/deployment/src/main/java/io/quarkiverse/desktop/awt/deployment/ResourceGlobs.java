package io.quarkiverse.desktop.awt.deployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;

/**
 * The resource globs of the lists, registered for native executables. Most of them address resources of the modules of
 * the JDK (the icons of the look and feels, the cursors, the print dialog icons...), which the JDK loads with
 * {@code Class.getResourceAsStream} from its own classes.
 * <p>
 * Quarkus 3.40 writes the resources in the legacy format, where a glob matches the resources of the class path and of
 * every module. Later versions write the reachability metadata format, where a glob without a module only matches the
 * resources of the class path, and have {@code NativeImageResourcePatternsBuildItem.Builder.module(String)} : the globs
 * are then registered in each module of the JDK that has their directory, and on the class path when no module has it.
 * Used by the Desktop AWT and Desktop Swing extensions.
 */
public final class ResourceGlobs {

    /**
     * The class path, in {@link #byModule(Collection)}.
     */
    static final String CLASS_PATH = "";

    /**
     * {@code NativeImageResourcePatternsBuildItem.Builder.module(String)} of the Quarkus versions after 3.40, or
     * {@code null}.
     */
    private static final Method MODULE = moduleMethod();

    private ResourceGlobs() {
    }

    /**
     * The build items that register the given globs.
     */
    public static List<NativeImageResourcePatternsBuildItem> patterns(Collection<String> globs) {
        List<NativeImageResourcePatternsBuildItem> items = new ArrayList<>();
        if (globs.isEmpty()) {
            return items;
        }
        if (MODULE == null) {
            // legacy format : a glob matches every module
            items.add(NativeImageResourcePatternsBuildItem.builder().includeGlobs(globs).build());
            return items;
        }
        for (Map.Entry<String, Set<String>> entry : byModule(globs).entrySet()) {
            NativeImageResourcePatternsBuildItem.Builder builder = NativeImageResourcePatternsBuildItem.builder()
                    .includeGlobs(entry.getValue());
            if (!entry.getKey().equals(CLASS_PATH)) {
                try {
                    MODULE.invoke(builder, entry.getKey());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Unable to register the resources of the module " + entry.getKey(), e);
                }
            }
            items.add(builder.build());
        }
        return items;
    }

    /**
     * The globs grouped by the modules of the JDK that have their directory ({@link #CLASS_PATH} for the others).
     */
    static Map<String, Set<String>> byModule(Collection<String> globs) {
        Map<String, Set<String>> byModule = new TreeMap<>();
        for (String glob : globs) {
            Set<String> modules = ReachabilityLookups.modules(glob);
            for (String module : modules.isEmpty() ? Set.of(CLASS_PATH) : modules) {
                byModule.computeIfAbsent(module, m -> new java.util.TreeSet<>()).add(glob);
            }
        }
        return byModule;
    }

    /**
     * Whether the Quarkus version of the build registers module resources ({@code Builder.module(String)}).
     */
    static boolean moduleResources() {
        return MODULE != null;
    }

    private static Method moduleMethod() {
        try {
            return NativeImageResourcePatternsBuildItem.Builder.class.getMethod("module", String.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
