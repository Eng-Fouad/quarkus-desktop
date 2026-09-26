package io.quarkiverse.desktop.swing.it;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthPainter;

/**
 * An application Synth painter, declared in the Synth XML file : Synth creates it with reflection (beans decoding).
 */
public class GlossyPainter extends SynthPainter {

    static final Color TOP = new Color(0x4695eb);
    static final Color BOTTOM = new Color(0x0d1c2c);

    /**
     * How many times buttons were painted.
     */
    static final AtomicInteger PAINTED = new AtomicInteger();

    @Override
    public void paintButtonBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
        PAINTED.incrementAndGet();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(x, y, TOP, x, y + h, BOTTOM));
        g2.fillRoundRect(x, y, w - 1, h - 1, 8, 8);
        g2.dispose();
    }

    @Override
    public void paintPanelBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
        g.setColor(context.getStyle().getColor(context, ColorType.BACKGROUND));
        g.fillRect(x, y, w, h);
    }
}
