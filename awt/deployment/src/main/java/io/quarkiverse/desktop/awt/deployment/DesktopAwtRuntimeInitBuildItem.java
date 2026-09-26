package io.quarkiverse.desktop.awt.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced by the step that prepares the AWT run time environment of a native executable when it starts (the
 * {@code java.home} directory, the font configuration of the JDK), before any AWT class is used.
 * <p>
 * A {@code RUNTIME_INIT} step that uses AWT (fonts, look and feels, windows...) consumes it (as a {@code List}, since it
 * is only produced for native builds) to run after it : the font configuration is read once, when the fonts are first
 * used.
 */
public final class DesktopAwtRuntimeInitBuildItem extends MultiBuildItem {
}
