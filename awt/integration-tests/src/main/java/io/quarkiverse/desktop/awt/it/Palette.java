package io.quarkiverse.desktop.awt.it;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

/**
 * Constants creating AWT objects in a static initializer : Quarkus initializes application classes at build time, the
 * extension detects this class and initializes it at run time (otherwise the native build fails).
 */
public final class Palette {

    public static final Color BACKGROUND = new Color(0xf0f4f8);

    public static final Color ACCENT = new Color(0x0096c9);

    public static final Color HIGHLIGHT = new Color(0xe0, 0x40, 0x40);

    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    public static final Stroke OUTLINE = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private Palette() {
    }
}
