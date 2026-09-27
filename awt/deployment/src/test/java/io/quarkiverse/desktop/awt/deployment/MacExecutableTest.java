package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * The build version and the information property list of macOS native executables, and the check of the libraries next
 * to them.
 */
class MacExecutableTest {

    @TempDir
    Path directory;

    @Test
    void infoPlist() {
        String plist = MacExecutable.infoPlist("My <App> & Co", "1.2.3-SNAPSHOT");
        assertTrue(plist.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE plist"), plist);
        assertTrue(plist.contains("<key>CFBundleIdentifier</key>\n    <string>my-app-co</string>"), plist);
        assertTrue(plist.contains("<key>CFBundleName</key>\n    <string>My &lt;App&gt; &amp; Co</string>"), plist);
        assertTrue(plist.contains("<key>CFBundleShortVersionString</key>\n    <string>1.2.3</string>"), plist);
        assertTrue(plist.contains("<key>CFBundleVersion</key>\n    <string>1.2.3</string>"), plist);
        assertTrue(plist.contains("<key>NSHighResolutionCapable</key>\n    <true/>"), plist);
        assertTrue(plist.contains("<key>NSMicrophoneUsageDescription</key>"), plist);
        assertTrue(plist.endsWith("</dict>\n</plist>\n"), plist);
    }

    @Test
    void bundleValues() {
        assertEquals("quarkus-desktop-showcase", MacExecutable.bundleIdentifier("Quarkus Desktop Showcase"));
        assertEquals("org.acme.app", MacExecutable.bundleIdentifier("org.acme.app"));
        assertEquals("application", MacExecutable.bundleIdentifier("..."));
        assertEquals("1.0", MacExecutable.bundleVersion("1.0"));
        assertEquals("25.1.3", MacExecutable.bundleVersion("25.1.3.4-SNAPSHOT"));
        assertEquals("999", MacExecutable.bundleVersion("999-SNAPSHOT"));
        assertEquals("1.0", MacExecutable.bundleVersion("SNAPSHOT"));
        assertEquals("1.0", MacExecutable.bundleVersion(null));
    }

    @Test
    void linkerOption() throws IOException {
        List<String> args = MacExecutable.nativeImageArgs(MacExecutable.infoPlist("app", "1.0"), directory);
        Path file = directory.toAbsolutePath().resolve("quarkus-desktop-awt").resolve("Info.plist");
        assertEquals(List.of("-H:NativeLinkerOption=-Wl,-sectcreate,__TEXT,__info_plist," + file), args);
        assertEquals(MacExecutable.infoPlist("app", "1.0"), Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void unsafeBuildDirectory() throws IOException {
        Path build = directory.resolve("with,comma");
        Path file = MacExecutable.writeInfoPlist(build, "plist").orElseThrow();
        assertTrue(!file.toString().contains(",") && Files.isRegularFile(file), file.toString());
    }

    @Test
    void missingLibraries() throws IOException {
        Path executable = directory.resolve("app-runner");
        Files.createFile(executable);
        assertEquals(DesktopAwtProcessor.MAC_REQUIRED_LIBRARIES, MacExecutable.missingLibraries(executable));
        for (String library : DesktopAwtProcessor.MAC_REQUIRED_LIBRARIES) {
            if (!library.equals("libosxapp.dylib")) {
                Files.createFile(directory.resolve(library));
            }
        }
        assertEquals(List.of("libosxapp.dylib"), MacExecutable.missingLibraries(executable));
    }

    @Test
    void buildVersion() {
        // LC_BUILD_VERSION, platform macOS, minos 11.0, sdk 14.5, after another load command
        byte[] buildVersion = machO(command(0x19, 72), command(0x32, 24, 1, 0x000b0000, 0x000e0500, 0));
        assertEquals(Optional.of(new MacExecutable.BuildVersion("11.0", "14.5")), MacExecutable.buildVersion(buildVersion));
        assertEquals("-H:NativeLinkerOption=-Wl,-platform_version,macos,11.0,14.5",
                MacExecutable.buildVersion(buildVersion).orElseThrow().linkerOption());
        // the older LC_VERSION_MIN_MACOSX
        byte[] versionMin = machO(command(0x24, 16, 0x000a0f07, 0x000b0300));
        assertEquals(Optional.of(new MacExecutable.BuildVersion("10.15.7", "11.3")), MacExecutable.buildVersion(versionMin));
        // another platform (iOS), no version command, not a Mach-O file
        assertEquals(Optional.empty(), MacExecutable.buildVersion(machO(command(0x32, 24, 2, 0x00110000, 0x00110000, 0))));
        assertEquals(Optional.empty(), MacExecutable.buildVersion(machO(command(0x19, 72))));
        assertEquals(Optional.empty(), MacExecutable.buildVersion("MZ not a Mach-O file".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void universalFile() {
        byte[] arm64 = machO(command(0x32, 24, 1, 0x000c0000, 0x000f0000, 0));
        byte[] x86 = machO(command(0x32, 24, 1, 0x000a0f00, 0x000f0000, 0));
        ByteBuffer fat = ByteBuffer.allocate(8 + 2 * 20 + x86.length + arm64.length).order(ByteOrder.BIG_ENDIAN);
        fat.putInt(0xcafebabe).putInt(2);
        int offset = 8 + 2 * 20;
        fat.putInt(0x01000007).putInt(3).putInt(offset).putInt(x86.length).putInt(12);
        fat.putInt(0x0100000c).putInt(0).putInt(offset + x86.length).putInt(arm64.length).putInt(12);
        fat.put(x86).put(arm64);
        String minimum = "x86_64".equals(System.getProperty("os.arch")) ? "10.15" : "12.0";
        assertEquals(Optional.of(new MacExecutable.BuildVersion(minimum, "15.0")), MacExecutable.buildVersion(fat.array()));

        // fat_arch_64 entries : 64-bit offsets and sizes, a reserved field
        ByteBuffer fat64 = ByteBuffer.allocate(8 + 2 * 32 + x86.length + arm64.length).order(ByteOrder.BIG_ENDIAN);
        fat64.putInt(0xcafebabf).putInt(2);
        int offset64 = 8 + 2 * 32;
        fat64.putInt(0x01000007).putInt(3).putLong(offset64).putLong(x86.length).putInt(12).putInt(0);
        fat64.putInt(0x0100000c).putInt(0).putLong(offset64 + x86.length).putLong(arm64.length).putInt(12).putInt(0);
        fat64.put(x86).put(arm64);
        assertEquals(Optional.of(new MacExecutable.BuildVersion(minimum, "15.0")), MacExecutable.buildVersion(fat64.array()));
    }

    @Test
    void malformedFiles() {
        byte[] buildVersion = machO(command(0x32, 24, 1, 0x000b0000, 0x000e0500, 0));
        // truncated in the sdk field of LC_BUILD_VERSION, or in its minos field
        assertEquals(Optional.empty(), MacExecutable.buildVersion(Arrays.copyOf(buildVersion, 32 + 18)));
        assertEquals(Optional.empty(), MacExecutable.buildVersion(Arrays.copyOf(buildVersion, 32 + 14)));
        // a load command size that would overflow the offset
        byte[] huge = machO(command(0x19, 16), command(0x32, 24, 1, 0x000b0000, 0x000e0500, 0));
        ByteBuffer.wrap(huge).order(ByteOrder.LITTLE_ENDIAN).putInt(32 + 4, 0x7ffffff8);
        assertEquals(Optional.empty(), MacExecutable.buildVersion(huge));
        // a fat header whose entries point outside the file, or with more entries than the file holds
        ByteBuffer fat = ByteBuffer.allocate(8 + 20).order(ByteOrder.BIG_ENDIAN);
        fat.putInt(0xcafebabe).putInt(0x7fffffff);
        fat.putInt("x86_64".equals(System.getProperty("os.arch")) ? 0x01000007 : 0x0100000c).putInt(0).putInt(0x7ffffff0)
                .putInt(0x7ffffff0).putInt(12);
        assertEquals(Optional.empty(), MacExecutable.buildVersion(fat.array()));
    }

    /**
     * The java launcher of the JDK running the tests has a build version.
     */
    @Test
    @EnabledOnOs(OS.MAC)
    void launcherBuildVersion() {
        Optional<MacExecutable.BuildVersion> version = MacExecutable
                .launcherBuildVersion(Path.of(System.getProperty("java.home")));
        assertTrue(version.isPresent() && version.get().minimum().matches("\\d+\\.\\d+(\\.\\d+)?")
                && version.get().sdk().matches("\\d+\\.\\d+(\\.\\d+)?"), String.valueOf(version));
    }

    /**
     * A 64-bit Mach-O file with the given load commands.
     */
    private static byte[] machO(byte[]... commands) {
        int size = 0;
        for (byte[] command : commands) {
            size += command.length;
        }
        ByteBuffer file = ByteBuffer.allocate(32 + size).order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(0xfeedfacf).putInt(0x0100000c).putInt(0).putInt(2).putInt(commands.length).putInt(size).putInt(0)
                .putInt(0);
        for (byte[] command : commands) {
            file.put(command);
        }
        return file.array();
    }

    /**
     * A load command : its type, size and first words (padded with zeros to its size).
     */
    private static byte[] command(int type, int size, int... words) {
        ByteBuffer command = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        command.putInt(type).putInt(size);
        for (int word : words) {
            command.putInt(word);
        }
        return command.array();
    }
}
