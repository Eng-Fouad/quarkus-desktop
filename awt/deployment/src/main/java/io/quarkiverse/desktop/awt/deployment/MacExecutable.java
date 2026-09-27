package io.quarkiverse.desktop.awt.deployment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
 * The macOS specific parts of a native executable : the minimum macOS version and the SDK version of the {@code java}
 * launcher, the information property list ({@code Info.plist}) that the {@code java} launcher embeds, and the libraries
 * next to the executable.
 * <p>
 * The versions and the property list are written by the linker ({@code -platform_version}, and the
 * {@code __TEXT,__info_plist} section), with {@code -H:NativeLinkerOption} options in a {@code native-image.properties}
 * file generated in the application jar.
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
     * The minimum macOS version and the SDK version of a Mach-O file, as {@code ld -platform_version} takes them
     * ({@code 11.0}, {@code 14.5}).
     */
    record BuildVersion(String minimum, String sdk) {

        /**
         * The native build option that writes these versions in the executable.
         */
        String linkerOption() {
            // ld : -platform_version platform min_version sdk_version
            return "-H:NativeLinkerOption=-Wl,-platform_version,macos," + minimum + "," + sdk;
        }
    }

    private static final int MH_MAGIC_64 = 0xfeedfacf;
    private static final int FAT_MAGIC = 0xcafebabe;
    private static final int CPU_TYPE_ARM64 = 0x0100000c;
    private static final int CPU_TYPE_X86_64 = 0x01000007;
    private static final int LC_VERSION_MIN_MACOSX = 0x24;
    private static final int LC_BUILD_VERSION = 0x32;
    private static final int PLATFORM_MACOS = 1;

    /**
     * The versions of the {@code LC_BUILD_VERSION} (or older {@code LC_VERSION_MIN_MACOSX}) load command of a 64-bit
     * Mach-O file, or of the slice of the current architecture of a universal file.
     *
     * @return empty when the file has none, or is not a Mach-O file
     */
    static Optional<BuildVersion> buildVersion(byte[] file) {
        ByteBuffer buffer = ByteBuffer.wrap(file).order(ByteOrder.BIG_ENDIAN);
        if (file.length >= 8 && buffer.getInt(0) == FAT_MAGIC) {
            // the build runs on the target platform (no cross compilation)
            int wanted = "x86_64".equals(System.getProperty("os.arch")) ? CPU_TYPE_X86_64 : CPU_TYPE_ARM64;
            int count = buffer.getInt(4);
            for (int i = 0; i < count && 8 + 20 * (i + 1) <= file.length; i++) {
                int entry = 8 + 20 * i;
                if (buffer.getInt(entry) == wanted) {
                    int offset = buffer.getInt(entry + 8);
                    int size = buffer.getInt(entry + 12);
                    if (offset >= 0 && size > 0 && (long) offset + size <= file.length) {
                        return buildVersion(java.util.Arrays.copyOfRange(file, offset, offset + size));
                    }
                }
            }
            return Optional.empty();
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (file.length < 32 || buffer.getInt(0) != MH_MAGIC_64) {
            return Optional.empty();
        }
        int commands = buffer.getInt(16);
        int offset = 32;
        for (int i = 0; i < commands && offset + 8 <= file.length; i++) {
            int command = buffer.getInt(offset);
            int size = buffer.getInt(offset + 4);
            if (command == LC_BUILD_VERSION && offset + 16 <= file.length && buffer.getInt(offset + 8) == PLATFORM_MACOS) {
                return Optional.of(new BuildVersion(version(buffer.getInt(offset + 12)), version(buffer.getInt(offset + 16))));
            }
            if (command == LC_VERSION_MIN_MACOSX && offset + 16 <= file.length) {
                return Optional.of(new BuildVersion(version(buffer.getInt(offset + 8)), version(buffer.getInt(offset + 12))));
            }
            if (size < 8) {
                break;
            }
            offset += size;
        }
        return Optional.empty();
    }

    /**
     * The versions of the {@code java} launcher of a JDK.
     */
    static Optional<BuildVersion> launcherBuildVersion(Path jdkHome) {
        Path launcher = jdkHome.resolve("bin").resolve("java");
        try {
            return Files.isRegularFile(launcher) ? buildVersion(Files.readAllBytes(launcher)) : Optional.empty();
        } catch (IOException e) {
            LOGGER.debugf(e, "Unable to read %s", launcher);
            return Optional.empty();
        }
    }

    /**
     * A version encoded as {@code xxxx.yy.zz} nibbles : {@code 11.0}, {@code 14.5}, {@code 10.15.7}.
     */
    static String version(int encoded) {
        int major = encoded >>> 16;
        int minor = (encoded >> 8) & 0xff;
        int patch = encoded & 0xff;
        return major + "." + minor + (patch != 0 ? "." + patch : "");
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
