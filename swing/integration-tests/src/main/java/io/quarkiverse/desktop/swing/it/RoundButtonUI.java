package io.quarkiverse.desktop.swing.it;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * An application UI delegate : Swing finds it by class name and creates it with its static {@code createUI} method.
 */
public class RoundButtonUI extends BasicButtonUI {

    static final Color FILL = new Color(0x4269e1);

    public static ComponentUI createUI(JComponent component) {
        return new RoundButtonUI();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(FILL);
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), c.getHeight(), c.getHeight());
        g2.dispose();
        ((AbstractButton) c).setForeground(Color.WHITE);
        super.paint(g, c);
    }
}
