package io.quarkiverse.desktop.awt.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Requests the JavaBeans registration of the JDK AWT classes (their bean properties, event sets and public fields, as
 * with {@code quarkus.desktop.awt.java-beans.jdk-classes=true}) whatever the configuration of the Desktop AWT extension.
 * <p>
 * The Desktop Swing extension produces it when the JavaBeans registration of the Swing classes is enabled : the Swing
 * components extend the AWT ones, and the {@code Introspector} reads the properties of all the classes of a hierarchy.
 */
public final class AwtJavaBeansClassesBuildItem extends MultiBuildItem {
}
