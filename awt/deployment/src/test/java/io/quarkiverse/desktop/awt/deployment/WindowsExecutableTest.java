package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * The {@code native-image.properties} file of the linker options of the Windows (and macOS) executables.
 */
class WindowsExecutableTest {

    @Test
    void nativeImagePropertiesAreAscii() throws IOException {
        // a user name and directories with accents, another script, a character outside the BMP
        String manifest = "C:\\Users\\Jos\u00e9\\J\u00fcrgen\\\u30d7\u30ed\u30b8\u30a7\u30af\u30c8\\\ud83d\ude00\\target"
                + "\\quarkus-desktop-awt\\application.manifest";
        List<String> args = List.of("-H:NativeLinkerOption=/MANIFEST:EMBED",
                "-H:NativeLinkerOption=/MANIFESTINPUT:" + manifest, "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS");

        String content = WindowsExecutable.nativeImageProperties(args);

        assertTrue(content.chars().allMatch(c -> c == '\n' || c >= ' ' && c <= '~'), content);
        // as native-image reads it (NativeImage.DriverMetaInfProcessor : Properties.load(InputStream), ISO-8859-1)
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals(String.join(" ", args), properties.getProperty("Args"));
        // and with a reader
        Properties read = new Properties();
        read.load(new StringReader(content));
        assertEquals(String.join(" ", args), read.getProperty("Args"));
    }
}
