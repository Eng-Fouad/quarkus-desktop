package io.quarkiverse.desktop.awt.runtime;

import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Run time support of the Desktop AWT extension.
 */
@Recorder
public class DesktopAwtRecorder {

    private static final Logger LOGGER = Logger.getLogger(DesktopAwtRecorder.class);

    /**
     * Directory of the files that the extension embeds in native executables.
     */
    public static final String RESOURCES = "META-INF/quarkus-desktop-awt/";

    /**
     * The logical font configuration of the JDK used for the native build (Windows).
     */
    public static final String FONT_CONFIGURATION = RESOURCES + "fontconfig.properties";

    /**
     * The PostScript font names of the JDK used for the native build ({@code lib/psfontj2d.properties}), used to print
     * text with PostScript fonts.
     */
    public static final String POSTSCRIPT_FONTS = RESOURCES + "psfontj2d.properties";

    /**
     * The directory that {@code io.quarkus:quarkus-awt} uses as {@code java.home} in native executables, relative to
     * {@code java.io.tmpdir}.
     */
    static final String RUNTIME_HOME = "quarkus-awt-tmp-fonts";

    /**
     * Prepares the {@code java.home} directory of a native executable, which has no JDK : some JDK desktop classes read
     * files in it, and some fail when {@code java.home} is not set (for instance the Java Sound audio system and the
     * font configuration).
     * <p>
     * {@code io.quarkus:quarkus-awt} sets {@code java.home} to a temporary directory when fonts are initialized. This
     * sets it to the same directory at startup (unless it is set), so that it is set whatever AWT feature the
     * application uses first. It also writes the PostScript font names there, and makes the logical fonts use the font
     * configuration of the JDK when one is embedded.
     *
     * @param fontConfiguration the name of the directory (in {@code java.io.tmpdir}) to extract the embedded font
     *        configuration to, or {@code null} when none is embedded
     */
    public void initRuntimeHome(String fontConfiguration) {
        try {
            Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
            Path home = tmp.resolve(RUNTIME_HOME);
            String javaHome = System.getProperty("java.home");
            if (javaHome == null || javaHome.isBlank()) {
                System.setProperty("java.home", home.toString());
            }
            Files.createDirectories(home.resolve("lib"));
            Files.createDirectories(home.resolve("conf").resolve("fonts"));
            extract(POSTSCRIPT_FONTS, home.resolve("lib").resolve("psfontj2d.properties"));
            if (fontConfiguration != null && System.getProperty("sun.awt.fontconfig") == null) {
                Path file = tmp.resolve(fontConfiguration).resolve("fontconfig.properties");
                if (extract(FONT_CONFIGURATION, file)) {
                    // Read by sun.awt.FontConfiguration instead of the configuration files of java.home
                    System.setProperty("sun.awt.fontconfig", file.toString());
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warnf(e, "Unable to prepare the java.home directory of the native executable");
        }
    }

    /**
     * Extracts a resource to a file, unless the file exists.
     *
     * @return whether the file exists
     */
    private static boolean extract(String resource, Path file) throws IOException {
        if (Files.isRegularFile(file)) {
            return true;
        }
        try (InputStream in = DesktopAwtRecorder.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                LOGGER.debugf("Resource %s not found", resource);
                return false;
            }
            Files.createDirectories(file.getParent());
            // Other processes may extract the same file at the same time : write a private copy, then move it
            Path copy = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            try {
                Files.copy(in, copy, StandardCopyOption.REPLACE_EXISTING);
                Files.move(copy, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                if (!Files.isRegularFile(file)) {
                    throw e;
                }
            } finally {
                Files.deleteIfExists(copy);
            }
        }
        return true;
    }

    /**
     * Disposes the windows of the application when it stops, in dev mode : the AWT toolkit, its threads and its windows
     * outlive a restart of the application in the same JVM.
     */
    public void disposeWindowsOnShutdown(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(DesktopAwtRecorder::disposeWindows);
    }

    static void disposeWindows() {
        // Do not start the AWT toolkit to dispose windows that cannot exist : the toolkit starts threads named "AWT-..."
        if (Thread.getAllStackTraces().keySet().stream().noneMatch(thread -> thread.getName().startsWith("AWT-"))) {
            return;
        }
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                LOGGER.debugf("Disposing %s", window);
                window.dispose();
            }
        }
    }
}
