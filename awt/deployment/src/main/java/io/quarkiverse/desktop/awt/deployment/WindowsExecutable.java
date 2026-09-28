package io.quarkiverse.desktop.awt.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkiverse.desktop.awt.runtime.DesktopAwtConfig;

/**
 * The Windows specific parts of a native executable : application manifest, subsystem and Visual C++ runtime.
 * <p>
 * native-image links a Windows executable without manifest and with the console subsystem. The manifest and the
 * subsystem are linker options, passed with {@code -H:NativeLinkerOption} options in a {@code native-image.properties}
 * file generated in the application jar.
 */
final class WindowsExecutable {

    private static final Logger LOGGER = Logger.getLogger(WindowsExecutable.class);

    /**
     * The generated {@code native-image.properties} file (its {@code Args} are added to the native build).
     */
    static final String NATIVE_IMAGE_PROPERTIES = "META-INF/native-image/io.quarkiverse.desktop/quarkus-desktop-awt-windows/native-image.properties";

    /**
     * The Visual C++ runtime libraries : {@code awt.dll} needs {@code msvcp140.dll}, the executable and the JDK
     * libraries need the others.
     */
    static final List<String> VC_RUNTIME = List.of("msvcp140.dll", "vcruntime140.dll", "vcruntime140_1.dll");

    private WindowsExecutable() {
    }

    /**
     * The application manifest : the one of the {@code java} launcher, without its identity.
     */
    static String manifest(boolean dpiAware) {
        StringBuilder manifest = new StringBuilder();
        manifest.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        manifest.append("<assembly xmlns=\"urn:schemas-microsoft-com:asm.v1\" manifestVersion=\"1.0\"")
                .append(" xmlns:asmv3=\"urn:schemas-microsoft-com:asm.v3\">\n");
        manifest.append("  <!-- Visual styles of the native controls and dialogs -->\n");
        manifest.append("  <dependency>\n");
        manifest.append("    <dependentAssembly>\n");
        manifest.append("      <assemblyIdentity type=\"win32\" name=\"Microsoft.Windows.Common-Controls\" version=\"6.0.0.0\"")
                .append(" processorArchitecture=\"*\" publicKeyToken=\"6595b64144ccf1df\" language=\"*\"/>\n");
        manifest.append("    </dependentAssembly>\n");
        manifest.append("  </dependency>\n");
        if (dpiAware) {
            manifest.append("  <!-- Per monitor DPI awareness -->\n");
            manifest.append("  <asmv3:application>\n");
            manifest.append("    <asmv3:windowsSettings")
                    .append(" xmlns:dpi1=\"http://schemas.microsoft.com/SMI/2005/WindowsSettings\"")
                    .append(" xmlns:dpi2=\"http://schemas.microsoft.com/SMI/2016/WindowsSettings\">\n");
            manifest.append("      <dpi1:dpiAware>true/PM</dpi1:dpiAware>\n");
            manifest.append("      <dpi2:dpiAwareness>PerMonitorV2, PerMonitor, system</dpi2:dpiAwareness>\n");
            manifest.append("    </asmv3:windowsSettings>\n");
            manifest.append("  </asmv3:application>\n");
        }
        manifest.append("  <!-- Windows Vista, 7, 8, 8.1, 10 and later -->\n");
        manifest.append("  <compatibility xmlns=\"urn:schemas-microsoft-com:compatibility.v1\">\n");
        manifest.append("    <application>\n");
        for (String os : List.of("e2011457-1546-43c5-a5fe-008deee3d3f0", "35138b9a-5d96-4fbd-8e2d-a2440225f93a",
                "4a2f28e3-53b9-4441-ba9c-d69d4a4a6e38", "1f676c76-80e1-4239-95bb-83d0f6d0da78",
                "8e0f7a12-bfb3-4fe8-b9a5-48fd50a15a9a")) {
            manifest.append("      <supportedOS Id=\"{").append(os).append("}\"/>\n");
        }
        manifest.append("    </application>\n");
        manifest.append("  </compatibility>\n");
        manifest.append("</assembly>\n");
        return manifest.toString();
    }

    /**
     * The native build options for the given configuration, writing the manifest file if needed.
     *
     * @param buildDirectory the build output directory (the manifest is written in a sub directory)
     */
    static List<String> nativeImageArgs(DesktopAwtConfig.Windows config, Path buildDirectory) throws IOException {
        List<String> args = new ArrayList<>();
        if (config.manifest()) {
            Optional<Path> manifest = writeManifest(buildDirectory, manifest(config.dpiAware()));
            if (manifest.isPresent()) {
                args.add("-H:NativeLinkerOption=/MANIFEST:EMBED");
                args.add("-H:NativeLinkerOption=/MANIFESTINPUT:" + manifest.get());
            }
        }
        if (config.subsystem() == DesktopAwtConfig.Subsystem.WINDOWS) {
            // The entry point of a console application : the Windows subsystem expects WinMain otherwise
            args.add("-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS");
            args.add("-H:NativeLinkerOption=/ENTRY:mainCRTStartup");
        }
        return args;
    }

    /**
     * The content of a {@code native-image.properties} file with the given native build options, in ASCII : native-image
     * reads these files with {@code Properties.load(InputStream)}, as ISO-8859-1, so the characters above {@code ~} (in
     * the path of the manifest when the user name or the project directory has accents) are Unicode escapes.
     */
    static String nativeImageProperties(List<String> args) {
        // native-image splits Args on white space ; properties files use \ as escape character
        String value = String.join(" ", args).replace("\\", "\\\\");
        StringBuilder properties = new StringBuilder("Args = ");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > '~') {
                properties.append(String.format("\\u%04X", (int) c));
            } else {
                properties.append(c);
            }
        }
        return properties.append('\n').toString();
    }

    /**
     * Writes the manifest in the build directory, or in the temporary directory when the path of the build directory
     * has white space (native-image splits the options of a {@code native-image.properties} file on white space).
     *
     * @return the absolute path of the manifest, or empty when no directory without white space is available
     */
    static Optional<Path> writeManifest(Path buildDirectory, String manifest) throws IOException {
        Path directory = buildDirectory.toAbsolutePath().resolve("quarkus-desktop-awt");
        if (hasWhiteSpace(directory)) {
            directory = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath()
                    .resolve("quarkus-desktop-awt-" + Integer.toHexString(manifest.hashCode()));
            if (hasWhiteSpace(directory)) {
                LOGGER.warnf("The application manifest is not embedded in the native executable : the paths of the build"
                        + " directory and of the temporary directory contain white space. Build from a directory without"
                        + " white space, or set quarkus.desktop.awt.windows.manifest=false");
                return Optional.empty();
            }
        }
        Files.createDirectories(directory);
        Path file = directory.resolve("application.manifest");
        Files.writeString(file, manifest, StandardCharsets.UTF_8);
        return Optional.of(file);
    }

    private static boolean hasWhiteSpace(Path path) {
        return path.toString().chars().anyMatch(Character::isWhitespace);
    }

    /**
     * Copies the Visual C++ runtime libraries of the given JDK next to the executable, when the executable uses the AWT
     * libraries ({@code awt.dll} next to it).
     */
    static void copyVcRuntime(Path jdkHome, Path executable) {
        Path directory = executable.toAbsolutePath().getParent();
        if (!Files.isRegularFile(directory.resolve("awt.dll"))) {
            LOGGER.debugf("No awt.dll next to %s : Visual C++ runtime not copied", executable);
            return;
        }
        for (String library : VC_RUNTIME) {
            Path source = jdkHome.resolve("bin").resolve(library);
            if (!Files.isRegularFile(source)) {
                LOGGER.warnf("%s not found : it is not copied next to the native executable", source);
                continue;
            }
            try {
                Files.copy(source, directory.resolve(library), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.debugf("Copied %s to %s", source, directory);
            } catch (IOException e) {
                LOGGER.warnf(e, "Unable to copy %s to %s", source, directory);
            }
        }
    }
}
