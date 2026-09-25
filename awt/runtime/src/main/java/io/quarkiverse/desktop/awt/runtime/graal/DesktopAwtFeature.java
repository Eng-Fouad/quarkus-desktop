package io.quarkiverse.desktop.awt.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeSystemProperties;

/**
 * Sets the default value of system properties in the native executable : the ones the JVM sets for AWT, and the ones
 * configured by the extension.
 * <p>
 * The Quarkus build passes the settings to the native image builder as builder JVM system properties (a
 * {@code NativeImageSystemPropertyBuildItem} only reaches the builder JVM). A value registered here is the default value
 * in the native executable : a {@code -D} command line option of the native executable still overrides it.
 */
public final class DesktopAwtFeature implements Feature {

    /**
     * Builder system property : {@code true} to make the native executable DPI aware on Windows.
     */
    public static final String DPI_AWARE = "io.quarkiverse.desktop.awt.dpi-aware";

    /**
     * Builder system property : {@code true} to ignore the assistive technologies configured by the user (Java Access
     * Bridge not included in the native executable).
     */
    public static final String IGNORE_ASSISTIVE_TECHNOLOGIES = "io.quarkiverse.desktop.awt.ignore-assistive-technologies";

    @Override
    public String getDescription() {
        return "Quarkus Desktop AWT run time system property defaults";
    }

    /**
     * Registers the defaults. {@link RuntimeSystemProperties} cannot be used before this phase (its support singleton
     * is registered during the setup of the native image builder), and the Quarkus generated feature registers its own
     * defaults in this phase too.
     */
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // Set by the JVM (UnicodeLittle on little endian platforms), not by native executables : sun.awt.FontDescriptor
        // reads it to encode the text of the native AWT components (buttons, labels, lists, menus...) in UTF-16, and
        // defaults to big endian, which garbles every native component text on Windows
        String unicodeEncoding = System.getProperty("sun.io.unicode.encoding");
        if (unicodeEncoding != null) {
            RuntimeSystemProperties.register("sun.io.unicode.encoding", unicodeEncoding);
        }
        if (Boolean.getBoolean(DPI_AWARE)) {
            // Read by sun.java2d.windows.WindowsFlags : the java launcher makes the JVM DPI aware through the
            // sun.java.launcher property, which a native executable does not have
            RuntimeSystemProperties.register("sun.java2d.dpiaware", "true");
        }
        if (Boolean.getBoolean(IGNORE_ASSISTIVE_TECHNOLOGIES)) {
            // Read by java.awt.Toolkit before the assistive_technologies of ~/.accessibility.properties : an empty value
            // loads no assistive technology
            RuntimeSystemProperties.register("javax.accessibility.assistive_technologies", "");
        }
    }
}
