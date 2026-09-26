package io.quarkiverse.desktop.swing.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * An application look and feel, set by class name.
 */
class LookAndFeelClassNameTest {

    public static class ApplicationLookAndFeel extends MetalLookAndFeel {
        @Override
        public String getName() {
            return "Application";
        }
    }

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClass(ApplicationLookAndFeel.class))
            .overrideConfigKey("quarkus.desktop.swing.look-and-feel", ApplicationLookAndFeel.class.getName());

    @Test
    void lookAndFeelIsSet() {
        assertEquals("Application", UIManager.getLookAndFeel().getName());
    }
}
