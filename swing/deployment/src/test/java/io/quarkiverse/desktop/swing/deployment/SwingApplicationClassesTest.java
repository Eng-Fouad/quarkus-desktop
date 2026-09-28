package io.quarkiverse.desktop.swing.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.event.InputMethodEvent;
import java.io.IOException;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.synth.SynthPainter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

class SwingApplicationClassesTest {

    public static class RoundButtonUI extends BasicButtonUI {
        public static ComponentUI createUI(JComponent component) {
            return new RoundButtonUI();
        }
    }

    /**
     * Inherits createUI (registered by the extension lists for the JDK class).
     */
    public static class PlainLabelUI extends BasicLabelUI {
    }

    public static class ApplicationLookAndFeel extends MetalLookAndFeel {
    }

    public abstract static class AbstractLookAndFeel extends MetalLookAndFeel {
    }

    public static class ApplicationKit extends DefaultEditorKit {
    }

    public static class ApplicationPainter extends SynthPainter {
    }

    public static class ApplicationView extends PlainView {
        public ApplicationView(Element element) {
            super(element);
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1) {
            return x;
        }
    }

    public static class InputMethodField extends JTextField {
        @Override
        protected void processInputMethodEvent(InputMethodEvent e) {
            super.processInputMethodEvent(e);
        }
    }

    public static class PlainField extends JTextField {
    }

    static class NotSwing {
        Segment segment;
    }

    @Test
    void findsTheClassesSwingUsesWithReflection() throws IOException {
        Index index = Index.of(RoundButtonUI.class, PlainLabelUI.class, ApplicationLookAndFeel.class,
                AbstractLookAndFeel.class, ApplicationKit.class, ApplicationPainter.class, ApplicationView.class,
                InputMethodField.class, PlainField.class, NotSwing.class);

        SwingApplicationClasses classes = SwingApplicationClasses.scan(index, getClass().getClassLoader());

        assertEquals(Set.of(RoundButtonUI.class.getName(), PlainLabelUI.class.getName(),
                ApplicationLookAndFeel.class.getName(), ApplicationKit.class.getName(),
                ApplicationPainter.class.getName()), classes.constructed);
        assertEquals(Set.of(RoundButtonUI.class.getName()), classes.createUIMethods.keySet());
        // the icons of the look and feels of the JDK are looked up in the package of the application look and feel first
        assertEquals(Set.of("io/quarkiverse/desktop/swing/deployment/icons/*"), classes.lookAndFeelIconGlobs);
    }
}
