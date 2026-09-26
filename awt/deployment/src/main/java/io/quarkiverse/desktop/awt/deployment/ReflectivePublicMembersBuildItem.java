package io.quarkiverse.desktop.awt.deployment;

import java.util.Collection;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Classes registered for reflection with their public constructors, their public methods (inherited ones included :
 * what {@code Class.getMethods()} returns) and their public fields, in native executables : what the JavaBeans API
 * (the {@code Introspector}, {@code XMLEncoder} and {@code XMLDecoder}, {@code Statement}, {@code Expression} and
 * {@code EventHandler}) sees of a class.
 * <p>
 * Quarkus has no build item for this kind of registration : the Desktop AWT extension adds a reflection configuration
 * file with the classes of all these build items to the native build.
 */
public final class ReflectivePublicMembersBuildItem extends MultiBuildItem {

    private final List<String> classNames;

    /**
     * @param classNames the binary names of the classes
     */
    public ReflectivePublicMembersBuildItem(Collection<String> classNames) {
        this.classNames = List.copyOf(classNames);
    }

    /**
     * @return the binary names of the classes
     */
    public List<String> getClassNames() {
        return classNames;
    }
}
