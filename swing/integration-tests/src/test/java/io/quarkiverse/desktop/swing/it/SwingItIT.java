package io.quarkiverse.desktop.swing.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

@QuarkusMainIntegrationTest
public class SwingItIT extends SwingItTest {

    /**
     * Also compares what the native executable rendered (the galleries of the look and feels), printed (PostScript) and
     * resolved (the look and feel defaults) with what the JVM mode tests of the same build produced : they must be the
     * same.
     */
    @Test
    @Launch({ "swing", DIRECTORY })
    @Override
    public void swing(LaunchResult result) throws Exception {
        super.swing(result);
        List<String> compared = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(Path.of(DIRECTORY), "*-native.*")) {
            for (Path nativeFile : files) {
                Path jvmFile = nativeFile.resolveSibling(nativeFile.getFileName().toString().replace("-native.", "-jvm."));
                if (!Files.isRegularFile(jvmFile)) {
                    // The JVM mode tests did not run in this build
                    continue;
                }
                if (nativeFile.toString().endsWith(".png")) {
                    assertSameImage(jvmFile, nativeFile);
                } else {
                    assertArrayEquals(Files.readAllBytes(jvmFile), Files.readAllBytes(nativeFile),
                            nativeFile + " differs from " + jvmFile);
                }
                compared.add(nativeFile.getFileName().toString());
            }
        }
        System.out.println("Same as in JVM mode : " + compared);
    }

    private static void assertSameImage(Path expected, Path actual) throws IOException {
        BufferedImage jvm = ImageIO.read(expected.toFile());
        BufferedImage image = ImageIO.read(actual.toFile());
        assertEquals(jvm.getWidth() + "x" + jvm.getHeight(), image.getWidth() + "x" + image.getHeight(),
                "size of " + actual);
        int different = 0;
        for (int x = 0; x < jvm.getWidth(); x++) {
            for (int y = 0; y < jvm.getHeight(); y++) {
                if (jvm.getRGB(x, y) != image.getRGB(x, y)) {
                    different++;
                }
            }
        }
        assertTrue(different == 0, different + " pixels of " + actual + " differ from " + expected);
    }
}
