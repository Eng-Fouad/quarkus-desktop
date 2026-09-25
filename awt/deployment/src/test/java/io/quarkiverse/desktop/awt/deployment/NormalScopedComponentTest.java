package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Canvas;
import java.awt.Panel;
import java.util.logging.Level;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * A warning is logged for a normal scoped bean extending {@code java.awt.Component}, not for a {@code @Singleton} one.
 */
class NormalScopedComponentTest {

    @ApplicationScoped
    public static class NormalScopedPanel extends Panel {
    }

    @Singleton
    public static class SingletonCanvas extends Canvas {
    }

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(NormalScopedPanel.class, SingletonCanvas.class))
            .setLogRecordPredicate(r -> r.getLevel().equals(Level.WARNING)
                    && DesktopAwtTest.message(r).contains("java.awt.Component"))
            .assertLogRecords(records -> {
                assertEquals(1, records.size(), records.toString());
                String message = DesktopAwtTest.message(records.get(0));
                assertTrue(message.contains(NormalScopedPanel.class.getName()), message);
                assertTrue(message.contains("ApplicationScoped"), message);
            });

    @Test
    void warning() {
        // the log records are checked once the application stopped
    }
}
