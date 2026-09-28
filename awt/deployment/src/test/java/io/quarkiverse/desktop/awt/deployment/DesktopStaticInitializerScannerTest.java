package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.paths.PathTree;

class DesktopStaticInitializerScannerTest {

    static class ConstantColor {
        static final Color ACCENT = new Color(0x0096c9);

        static String name() {
            return "accent";
        }
    }

    static class ConstantFont {
        static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    }

    static class SwingBorder {
        static final Border PADDING = new EmptyBorder(4, 4, 4, 4);
    }

    static class InstanceUseOnly {
        static final String NAME = "instance";

        Color color() {
            return Color.RED;
        }
    }

    static class NoDesktop {
        static final List<String> NAMES = List.of("a", "b");
    }

    static class Helper {
        static List<Object> colors() {
            return List.of(Color.BLUE);
        }
    }

    static class ViaHelper {
        static final List<Object> COLORS = Helper.colors();
    }

    static class Holder {
        final Object color;

        Holder(int rgb) {
            this.color = new Color(rgb);
        }
    }

    static class ViaConstructor {
        static final List<Holder> HOLDERS = List.of(new Holder(0xffffff));
    }

    enum Palette {
        PRIMARY(0x0096c9);

        final Object color;

        Palette(int rgb) {
            this.color = new Color(rgb);
        }
    }

    interface Colors {
        Color BACKGROUND = Color.WHITE;
    }

    static class SubclassOfDesktopUser extends ConstantColor {
    }

    static class TriggersDesktopUserInitialization {
        static final String NAME = ConstantColor.name();
    }

    @SuppressWarnings("removal")
    static class AppletAudioClip {
        // java.applet is initialized at run time, and the only desktop type of this initializer
        static final java.applet.AudioClip CLICK = java.applet.Applet
                .newAudioClip(AppletAudioClip.class.getResource("click.wav"));
    }

    // A library that is not in the index : its classes are read from its jars

    static class LibraryFactory {
        static Object create() {
            return new Color(0x336699);
        }
    }

    static class LibraryDefaults {
        static final Object BACKGROUND = new Color(0xeeeeee);
    }

    static class LibraryName {
        static final String NAME = "library";
    }

    /**
     * No desktop type in its class file : only through a class of another jar.
     */
    static class LibraryTheme {
        static final Object DEFAULT = LibraryFactory.create();
    }

    @TempDir
    Path directory;

    @Test
    void detectsClassesCreatingDesktopObjectsInStaticInitializers() throws IOException {
        Index index = Index.of(ConstantColor.class, ConstantFont.class, SwingBorder.class, InstanceUseOnly.class,
                NoDesktop.class, Helper.class, ViaHelper.class, Holder.class, ViaConstructor.class, Palette.class,
                Colors.class, SubclassOfDesktopUser.class, TriggersDesktopUserInitialization.class,
                AppletAudioClip.class);

        Set<String> classes = DesktopStaticInitializerScanner.scan(index.getKnownClasses(), getClass().getClassLoader());

        assertEquals(Set.of(
                ConstantColor.class.getName(),
                ConstantFont.class.getName(),
                SwingBorder.class.getName(),
                ViaHelper.class.getName(),
                ViaConstructor.class.getName(),
                Palette.class.getName(),
                Colors.class.getName(),
                SubclassOfDesktopUser.class.getName(),
                TriggersDesktopUserInitialization.class.getName(),
                AppletAudioClip.class.getName()), classes);
    }

    @Test
    void scansTheLibrariesOutsideTheIndex() throws IOException {
        Path first = jar("first.jar", LibraryTheme.class, LibraryName.class);
        Path second = jar("second.jar", LibraryFactory.class, LibraryDefaults.class);

        Set<String> classes = DesktopStaticInitializerScanner.scan(DesktopStaticInitializerScanner.classFiles(
                List.of(PathTree.ofArchive(first), PathTree.ofArchive(second)), List.of(), getClass().getClassLoader()));

        assertEquals(Set.of(LibraryTheme.class.getName(), LibraryDefaults.class.getName()), classes);
    }

    @Test
    void classNames() {
        assertEquals("com/example/Palette", DesktopStaticInitializerScanner.className("com/example/Palette.class"));
        assertEquals("com/example/Palette$1", DesktopStaticInitializerScanner.className("com/example/Palette$1.class"));
        // multi-release jars : the class that a versioned class file versions
        assertEquals("com/example/Palette",
                DesktopStaticInitializerScanner.className("META-INF/versions/17/com/example/Palette.class"));
        assertEquals("Palette", DesktopStaticInitializerScanner.className("Palette.class"));
        for (String other : List.of("module-info.class", "META-INF/versions/9/module-info.class",
                "com/example/package-info.class", "META-INF/Other.class", "com/example/palette.properties")) {
            assertNull(DesktopStaticInitializerScanner.className(other), other);
        }
    }

    private Path jar(String name, Class<?>... classes) throws IOException {
        Path jar = directory.resolve(name);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (Class<?> type : classes) {
                String entry = type.getName().replace('.', '/') + ".class";
                out.putNextEntry(new JarEntry(entry));
                try (InputStream in = type.getClassLoader().getResourceAsStream(entry)) {
                    in.transferTo(out);
                }
                out.closeEntry();
            }
        }
        return jar;
    }

    /**
     * The packages that the extension initializes at run time are desktop packages for the scanner : a class using them
     * in its static initializer would otherwise be initialized at build time, and fail the native build.
     */
    @Test
    void desktopPackagesCoverTheRunTimeInitializedPackages() {
        List<String> packages = new ArrayList<>();
        for (String[] list : List.of(AwtClassesAndResources.RUNTIME_INITIALIZED_PACKAGES,
                AwtClassesAndResources.WINDOWS_RUNTIME_INITIALIZED_PACKAGES,
                AwtClassesAndResources.LINUX_RUNTIME_INITIALIZED_PACKAGES,
                AwtClassesAndResources.MAC_RUNTIME_INITIALIZED_PACKAGES)) {
            packages.addAll(List.of(list));
        }
        List<String> uncovered = packages.stream().map(p -> p.replace('.', '/') + "/")
                .filter(p -> DesktopStaticInitializerScanner.DESKTOP_PACKAGES.stream().noneMatch(p::startsWith)).toList();
        assertEquals(List.of(), uncovered);
    }
}
