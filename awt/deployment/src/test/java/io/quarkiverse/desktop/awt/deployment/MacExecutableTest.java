package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The information property list of macOS native executables, and the check of the libraries next to them.
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
}
