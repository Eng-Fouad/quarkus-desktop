package io.quarkiverse.desktop.swing.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.LogRecord;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class DesktopSwingTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setLogRecordPredicate(r -> message(r).contains("Installed features"))
            .assertLogRecords(records -> {
                assertEquals(1, records.size());
                String features = message(records.get(0));
                assertTrue(features.contains("desktop-awt") && features.contains("desktop-swing"), features);
            });

    @Test
    void features() {
        // the log records are checked once the application stopped
    }

    static String message(LogRecord record) {
        // the formatted message, whatever the format style of the record (printf, message format or none)
        return new PatternFormatter("%s").format(record);
    }
}
