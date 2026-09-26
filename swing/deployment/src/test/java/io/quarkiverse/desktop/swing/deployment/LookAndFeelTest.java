package io.quarkiverse.desktop.swing.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The look and feel configured with {@code quarkus.desktop.swing.look-and-feel} is set when the application starts.
 */
class LookAndFeelTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.desktop.swing.look-and-feel", "nimbus");

    @Test
    void lookAndFeelIsSet() {
        assertEquals("javax.swing.plaf.nimbus.NimbusLookAndFeel", UIManager.getLookAndFeel().getClass().getName());
    }
}
