package io.quarkiverse.desktop.awt.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

/**
 * The macOS specific parts of a native executable : the information property list ({@code Info.plist}) that the
 * {@code java} launcher embeds, and the libraries next to the executable.
 * <p>
 * The property list is embedded by the linker in the {@code __TEXT,__info_plist} section, with a
 * {@code -H:NativeLinkerOption} option in a {@code native-image.properties} file generated in the application jar.
 */
final class MacExecutable {

    private static final Logger LOGGER = Logger.getLogger(MacExecutable.class);

    /**
     * The generated {@code native-image.properties} file (its {@code Args} are added to the native build).
     */
    static final String NATIVE_IMAGE_PROPERTIES = "META-INF/native-image/io.quarkiverse.desktop/quarkus-desktop-awt-macos/native-image.properties";

    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+){0,2}");

    private MacExecutable() {
    }

    /**
     * The information property list of an application : the one of the {@code java} launcher
     * ({@code make/data/bundle/cmdline-Info.plist.template} of the JDK), with the name and version of the application.
     */
    static String infoPlist(String name, String version) {
        String bundleVersion = bundleVersion(version);
        StringBuilder plist = new StringBuilder();
        plist.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        plist.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\"")
                .append(" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        plist.append("<plist version=\"1.0\">\n");
        plist.append("<dict>\n");
        entry(plist, "CFBundleIdentifier", bundleIdentifier(name));
        entry(plist, "CFBundleInfoDictionaryVersion", "6.0");
        entry(plist, "CFBundleName", name);
        entry(plist, "CFBundleShortVersionString", bundleVersion);
        entry(plist, "CFBundleVersion", bundleVersion);
        plist.append("    <key>NSHighResolutionCapable</key>\n    <true/>\n");
        entry(plist, "NSMicrophoneUsageDescription", "The application is requesting access to the microphone.");
        plist.append("</dict>\n");
        plist.append("</plist>\n");
        return plist.toString();
    }

    private static void entry(StringBuilder plist, String key, String value) {
        plist.append("    <key>").append(key).append("</key>\n");
        plist.append("    <string>").append(escape(value)).append("</string>\n");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * A bundle identifier : letters, digits, dots and dashes only.
     */
    static String bundleIdentifier(String name) {
        String identifier = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.-]+", "-").replaceAll("^[-.]+|[-.]+$", "");
        return identifier.isEmpty() ? "application" : identifier;
    }

    /**
     * A bundle version : up to three numbers separated by dots ({@code 1.2.3} for {@code 1.2.3-SNAPSHOT}).
     */
    static String bundleVersion(String version) {
        Matcher matcher = VERSION.matcher(version == null ? "" : version);
        return matcher.lookingAt() ? matcher.group() : "1.0";
    }

    /**
     * The native build options embedding the given property list, writing it in the build directory.
     *
     * @param buildDirectory the build output directory (the property list is written in a sub directory)
     */
    static List<String> nativeImageArgs(String infoPlist, Path buildDirectory) throws IOException {
        Optional<Path> file = writeInfoPlist(buildDirectory, infoPlist);
        if (file.isEmpty()) {
            return List.of();
        }
        // ld : -sectcreate segment section file
        return List.of("-H:NativeLinkerOption=-Wl,-sectcreate,__TEXT,__info_plist," + file.get());
    }

    /**
     * Writes the property list in the build directory, or in the temporary directory when the path of the build
     * directory has white space or commas (native-image splits the options of a {@code native-image.properties} file on
     * white space, and the compiler driver splits {@code -Wl} options on commas).
     *
     * @return the absolute path of the property list, or empty when no suitable directory is available
     */
    static Optional<Path> writeInfoPlist(Path buildDirectory, String infoPlist) throws IOException {
        Path directory = buildDirectory.toAbsolutePath().resolve("quarkus-desktop-awt");
        if (!isSafe(directory)) {
            directory = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath()
                    .resolve("quarkus-desktop-awt-" + Integer.toHexString(infoPlist.hashCode()));
            if (!isSafe(directory)) {
                LOGGER.warnf("The Info.plist file is not embedded in the native executable : the paths of the build"
                        + " directory and of the temporary directory contain white space or commas. Build from another"
                        + " directory, or set quarkus.desktop.awt.macos.info-plist=false");
                return Optional.empty();
            }
        }
        Files.createDirectories(directory);
        Path file = directory.resolve("Info.plist");
        Files.writeString(file, infoPlist, StandardCharsets.UTF_8);
        return Optional.of(file);
    }

    private static boolean isSafe(Path path) {
        return path.toString().chars().noneMatch(c -> Character.isWhitespace(c) || c == ',');
    }

    /**
     * The libraries of {@link DesktopAwtProcessor#MAC_REQUIRED_LIBRARIES} missing next to the executable.
     */
    static List<String> missingLibraries(Path executable) {
        Path directory = executable.toAbsolutePath().getParent();
        return DesktopAwtProcessor.MAC_REQUIRED_LIBRARIES.stream()
                .filter(library -> !Files.isRegularFile(directory.resolve(library))).toList();
    }
}
