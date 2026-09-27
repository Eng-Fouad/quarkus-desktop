package io.quarkiverse.desktop.awt.deployment;

import java.util.Collection;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * What the JDK desktop code looks up by name and may not find, for native executables built with
 * {@code --exact-reachability-metadata} : with exact metadata, a lookup that is not registered fails with a missing
 * registration error instead of returning "not found", even when "not found" is the expected answer (the JavaBeans API
 * looks for {@code <class>BeanInfo} classes that usually do not exist, {@code Component} checks whether a class declares
 * {@code coalesceEvents}, {@code ResourceBundle} looks for {@code .properties} files next to the bundle classes...).
 * Registering these lookups makes them answer "not found" (or find what exists) in both modes.
 * <p>
 * The Desktop AWT extension writes the lookups of all these build items to a {@code reachability-metadata.json} file of
 * the native build.
 */
public final class ReachabilityLookupsBuildItem extends MultiBuildItem {

    private final List<String> types;
    private final List<String> methods;
    private final List<String> resourceGlobs;

    /**
     * @param types binary names of classes looked up by name, or queried for their members (a class that does not exist
     *        is registered as a lookup expected to fail)
     * @param methods methods looked up by name ({@code "fqcn#name(paramType,...)"}), declared or not by their class
     * @param resourceGlobs resources looked up, as glob patterns : {@code "glob"} for the class path, or
     *        {@code "module:glob"} for the resources of a named module
     */
    public ReachabilityLookupsBuildItem(Collection<String> types, Collection<String> methods,
            Collection<String> resourceGlobs) {
        this.types = List.copyOf(types);
        this.methods = List.copyOf(methods);
        this.resourceGlobs = List.copyOf(resourceGlobs);
    }

    public List<String> getTypes() {
        return types;
    }

    public List<String> getMethods() {
        return methods;
    }

    public List<String> getResourceGlobs() {
        return resourceGlobs;
    }
}
