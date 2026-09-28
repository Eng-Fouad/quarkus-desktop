package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

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
