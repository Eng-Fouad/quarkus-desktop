package io.quarkiverse.desktop.swing.it;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;

/**
 * Swing values in static fields : the static initializer uses Swing classes, which are initialized at run time in a
 * native executable, so this class must be initialized at run time too (the extension detects it).
 */
public final class SwingPalette {

    public static final ColorUIResource ACCENT = new ColorUIResource(0x4695eb);
    public static final Border FRAME = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY),
            "Quarkus");
    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    private SwingPalette() {
    }
}
