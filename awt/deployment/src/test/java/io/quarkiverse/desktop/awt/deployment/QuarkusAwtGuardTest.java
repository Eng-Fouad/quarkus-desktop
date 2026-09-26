package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

/**
 * The guard against quarkus-awt substitutions of the Windows AWT classes or of Type 1 fonts that the extension does not
 * remove (renamed or new ones).
 */
class QuarkusAwtGuardTest {

    @TempDir
    Path directory;

    @Test
    void knownSubstitutionsOnly() throws IOException {
        ResolvedDependency quarkusAwt = quarkusAwt("io/quarkus/awt/runtime/JDKSubstitutions.class",
                "io/quarkus/awt/runtime/Target_sun_awt_FontConfiguration_Windows.class",
                "io/quarkus/awt/runtime/Target_sun_awt_windows_WObjectPeer.class",
                "io/quarkus/awt/runtime/Target_sun_java2d_windows_WindowsFlags.class",
                "io/quarkus/awt/runtime/Target_sun_awt_windows_WToolkit.class",
                "io/quarkus/awt/runtime/Target_sun_font_Type1Font.class",
                "io/quarkus/awt/runtime/Target_sun_awt_FontConfiguration_Linux.class",
                "io/quarkus/awt/runtime/Target_sun_awt_im_CompositionAreaHandler.class");
        assertEquals(List.of(), DesktopAwtProcessor.unknownSubstitutions(quarkusAwt));
    }

    @Test
    void renamedSubstitution() throws IOException {
        ResolvedDependency quarkusAwt = quarkusAwt("io/quarkus/awt/runtime/JDKSubstitutions.class",
                "io/quarkus/awt/runtime/Target_sun_awt_windows_WObjectPeer_Headless.class",
                "io/quarkus/awt/runtime/Target_sun_java2d_windows_WindowsFlags.class");
        assertEquals(List.of("io/quarkus/awt/runtime/Target_sun_awt_windows_WObjectPeer_Headless.class"),
                DesktopAwtProcessor.unknownSubstitutions(quarkusAwt));
    }

    @Test
    void renamedType1FontSubstitution() throws IOException {
        ResolvedDependency quarkusAwt = quarkusAwt("io/quarkus/awt/runtime/JDKSubstitutions.class",
                "io/quarkus/awt/runtime/Target_sun_font_Type1Font_NotSupported.class");
        assertEquals(List.of("io/quarkus/awt/runtime/Target_sun_font_Type1Font_NotSupported.class"),
                DesktopAwtProcessor.unknownSubstitutions(quarkusAwt));
    }

    private ResolvedDependency quarkusAwt(String... entries) throws IOException {
        Path jar = directory.resolve("quarkus-awt.jar");
        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.closeEntry();
            }
        }
        return ResolvedDependencyBuilder.newInstance()
                .setCoords(ArtifactCoords.jar("io.quarkus", "quarkus-awt", "3.40.0"))
                .setResolvedPath(jar)
                .build();
    }
}
