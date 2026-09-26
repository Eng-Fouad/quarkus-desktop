package io.quarkiverse.desktop.swing.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class SwingItTest {

    static final String DIRECTORY = "target/swing-it";

    @Test
    @Launch({ "swing", DIRECTORY })
    public void swing(LaunchResult result) throws Exception {
        String output = result.getOutput();
        assertEquals(0, result.exitCode(), output);
        assertTrue(output.contains("SUMMARY ok="), output);
        assertFalse(output.contains(" FAILED "), output);
        for (String check : new String[] { "environment", "static-initializer", "multi", "html", "html-page", "rtf",
                "editor-kits", "styled-text-undo", "formatters", "combo-box-editor", "table", "tree", "file-chooser",
                "file-chooser-system", "color-chooser", "option-pane", "internal-frames", "transfer-handler",
                "print-table", "print-text", "timer-worker", "right-to-left", "errors" }) {
            assertTrue(output.contains("RESULT " + check + " OK"), check + "\n" + output);
        }
        // Set at startup : quarkus.desktop.swing.look-and-feel=nimbus (application.properties)
        assertTrue(output.contains("RESULT look-and-feel-config OK startup=javax.swing.plaf.nimbus.NimbusLookAndFeel"),
                output);
        if (!GraphicsEnvironment.isHeadless()) {
            assertTrue(output.contains("RESULT popup OK"), output);
        }
        // The logical fonts have the same metrics as in the JVM running the tests : they size the components
        AtomicReference<String> fontMetrics = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> fontMetrics.set(LookAndFeels.fontMetrics()));
        assertEquals(fontMetrics.get(), find(output, "RESULT font-metrics OK (.*)"), output);
        // Each look and feel has the same defaults (UI delegates, icons, painters, borders, colors, fonts, UI texts) and
        // key bindings as in the JVM running the tests
        for (Map.Entry<String, Signatures> expected : signatures().entrySet()) {
            String laf = expected.getKey();
            if (expected.getValue() == null) {
                assertTrue(output.contains("RESULT laf-" + laf + " SKIPPED"), laf + "\n" + output);
                continue;
            }
            assertEquals(expected.getValue().defaults(), find(output, "RESULT laf-" + laf
                    + " OK class=\\S+ (keys=\\d+ nulls=\\d+ hash=\\w+)"), "defaults of " + laf + "\n" + output);
            assertEquals(expected.getValue().bindings(), find(output, "RESULT gallery-" + laf
                    + " OK .*?(actions=\\d+ keys=\\d+ bindings=\\w+)"), "key bindings of " + laf + "\n" + output);
        }
    }

    @Test
    @Launch("look-and-feel")
    public void lookAndFeel(LaunchResult result) {
        assertEquals(0, result.exitCode(), result.getOutput());
        assertTrue(result.getOutput().contains("startupLookAndFeel=javax.swing.plaf.nimbus.NimbusLookAndFeel"),
                result.getOutput());
    }

    record Signatures(String defaults, String bindings) {
    }

    /**
     * The signatures of the defaults and key bindings of each look and feel in this JVM, {@code null} for the look and
     * feels that this platform does not support.
     */
    static Map<String, Signatures> signatures() throws Exception {
        Map<String, Signatures> signatures = new LinkedHashMap<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            String previous = UIManager.getLookAndFeel().getClass().getName();
            try {
                // As the application : the look and feel set at startup, then each look and feel in turn
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                for (LookAndFeels.Laf laf : LookAndFeels.all()) {
                    try {
                        LookAndFeels.set(laf);
                    } catch (UnsupportedLookAndFeelException e) {
                        signatures.put(laf.id(), null);
                        continue;
                    }
                    signatures.put(laf.id(), new Signatures(LookAndFeels.signature(LookAndFeels.describeDefaults()),
                            LookAndFeels.bindingsSignature()));
                }
                UIManager.setLookAndFeel(previous);
            } catch (Exception e) {
                error.set(e);
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
        return signatures;
    }

    static String find(String output, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(output);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        assertEquals(1, found.size(), regex + " : " + found + "\n" + output);
        return found.get(0);
    }
}
