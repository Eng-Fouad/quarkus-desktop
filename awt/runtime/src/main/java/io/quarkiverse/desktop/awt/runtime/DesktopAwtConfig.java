package io.quarkiverse.desktop.awt.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Desktop AWT configuration.
 * <p>
 * Every property applies to native executables only and is fixed when the native executable is built : JVM mode
 * applications use the JDK launcher and runtime, which already behave as a desktop application expects.
 */
@ConfigMapping(prefix = "quarkus.desktop.awt")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DesktopAwtConfig {

    /**
     * Native executables built for Windows.
     */
    Windows windows();

    /**
     * Native executables built for Windows (a native build on a Windows host, not a container build).
     */
    interface Windows {

        /**
         * Whether the native executable is DPI aware, as a JVM started by the {@code java} launcher is.
         * <p>
         * When enabled, the {@code sun.java2d.dpiaware} system property defaults to {@code true} in the native executable
         * (a {@code -Dsun.java2d.dpiaware=false} command line option still overrides it), and the application manifest
         * (see {@code quarkus.desktop.awt.windows.manifest}) declares per monitor DPI awareness. Windows then lets the
         * application scale itself to the display scale factor (crisp text and images). When disabled, Windows stretches
         * the windows of the application as bitmaps (blurry) on displays with a scale factor above 100 %.
         */
        @WithDefault("true")
        boolean dpiAware();

        /**
         * Whether to embed an application manifest in the native executable, as the {@code java} launcher has one.
         * <p>
         * The manifest selects the version 6 of the Windows common controls (the visual styles of the native AWT
         * components and dialogs; without it they look like Windows 2000 controls), declares the DPI awareness of the
         * application (see {@code quarkus.desktop.awt.windows.dpi-aware}) and the supported Windows versions. It is
         * embedded by the linker, through {@code -H:NativeLinkerOption} options that the extension adds to the native
         * build.
         */
        @WithDefault("true")
        boolean manifest();

        /**
         * The Windows subsystem of the native executable.
         * <p>
         * {@code console}, the default, is the subsystem of the {@code java} launcher : started from Explorer, the
         * application gets a console window, which shows its log. {@code windows} is the subsystem of the {@code javaw}
         * launcher : no console window (the standard output and error streams of the application are lost unless they
         * are redirected, so configure a log file).
         */
        @WithDefault("console")
        Subsystem subsystem();

        /**
         * Whether to copy the Microsoft Visual C++ runtime libraries ({@code msvcp140.dll}, {@code vcruntime140.dll} and
         * {@code vcruntime140_1.dll}) of the GraalVM used for the native build next to the native executable, when the
         * native executable uses the AWT libraries.
         * <p>
         * The JDK AWT library {@code awt.dll} needs {@code msvcp140.dll}, which a Windows installation does not always
         * have (it comes with the Visual C++ Redistributable). With a local copy, the native executable and its libraries
         * can be distributed as they are, as the JDK does.
         */
        @WithDefault("true")
        boolean copyVcRuntime();

        /**
         * Whether the native executable supports the Java Access Bridge, which screen readers such as JAWS or NVDA use to
         * access the user interface of Java applications.
         * <p>
         * Users enable the Java Access Bridge with {@code jabswitch -enable} (or in the Windows accessibility settings),
         * which configures the {@code assistive_technologies} of every Java application in
         * {@code %USERPROFILE%\.accessibility.properties}. When enabled, the Java Access Bridge is included in the native
         * executable, as it is in the JDK ({@code javaaccessbridge.dll} and {@code jawt.dll} are copied next to the native
         * executable), and it is loaded when the user enabled it. When disabled, the
         * {@code javax.accessibility.assistive_technologies} system property defaults to an empty value in the native
         * executable, so that the native executable ignores the user setting, instead of failing to start with a
         * {@code java.awt.AWTError: Could not load or activate service provider}.
         */
        @WithDefault("true")
        boolean accessBridge();

        /**
         * The logical font configuration (the physical fonts behind the {@code Dialog}, {@code SansSerif},
         * {@code Serif}, {@code Monospaced} and {@code DialogInput} logical fonts, used by AWT components and by default
         * in Swing).
         * <p>
         * {@code jdk}, the default, is the font configuration of the JDK used for the native build (its
         * {@code lib/fontconfig.properties.src} file, embedded in the native executable and extracted to the temporary
         * directory at startup) : the logical fonts display the same scripts as in JVM mode (Arabic, Hebrew, Chinese,
         * Japanese, Korean, Thai, Indic scripts...), and all the charsets of the JDK are included in the native
         * executable, since the font configuration uses many of them. {@code minimal} is the minimal font configuration
         * of the Quarkus AWT extension (Latin scripts only) : a slightly smaller native executable.
         */
        @WithDefault("jdk")
        FontConfiguration fontConfiguration();
    }

    /**
     * Windows subsystem of a native executable.
     */
    enum Subsystem {
        /**
         * Console application (the subsystem of {@code java.exe}).
         */
        CONSOLE,
        /**
         * Graphical application without console (the subsystem of {@code javaw.exe}).
         */
        WINDOWS
    }

    /**
     * Logical font configuration of a native executable.
     */
    enum FontConfiguration {
        /**
         * The font configuration of the JDK used for the native build.
         */
        JDK,
        /**
         * The minimal font configuration of the Quarkus AWT extension.
         */
        MINIMAL
    }
}
