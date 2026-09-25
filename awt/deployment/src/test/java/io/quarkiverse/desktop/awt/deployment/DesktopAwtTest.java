package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.LogRecord;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class DesktopAwtTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setLogRecordPredicate(r -> message(r).contains("Installed features"))
            .assertLogRecords(records -> {
                assertEquals(1, records.size());
                assertTrue(message(records.get(0)).contains("desktop-awt"), message(records.get(0)));
            });

    @Test
    void feature() {
        // the log records are checked once the application stopped
    }

    static String message(LogRecord record) {
        // the formatted message, whatever the format style of the record (printf, message format or none)
        return new PatternFormatter("%s").format(record);
    }
}
