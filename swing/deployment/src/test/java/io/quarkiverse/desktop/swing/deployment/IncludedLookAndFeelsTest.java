package io.quarkiverse.desktop.swing.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel;

class IncludedLookAndFeelsTest {

    @Test
    void allByDefault() throws IllegalAccessException {
        for (Set<IncludedLookAndFeel> configured : List.of(Set.<IncludedLookAndFeel> of(), Set.of(IncludedLookAndFeel.ALL),
                Set.of(IncludedLookAndFeel.ALL, IncludedLookAndFeel.METAL))) {
            IncludedLookAndFeels included = new IncludedLookAndFeels(configured);
            List<String> entries = allEntries();
            assertEquals(entries, Arrays.asList(included.filter(entries.toArray(String[]::new))), configured.toString());
        }
    }

    @Test
    void metalOnly() throws IllegalAccessException {
        IncludedLookAndFeels metal = new IncludedLookAndFeels(Set.of(IncludedLookAndFeel.METAL));
        List<String> excluded = new ArrayList<>(allEntries());
        excluded.removeAll(Arrays.asList(metal.filter(allEntries().toArray(String[]::new))));
        for (String entry : excluded) {
            assertTrue(entry.contains("nimbus") || entry.contains("synth") || entry.contains("motif")
                    || entry.contains("windows") || entry.contains("gtk") || entry.contains("multi")
                    || entry.startsWith("com.sun.beans.decoder.") || entry.equals("java.lang.Class#newInstance()"),
                    "excluded : " + entry);
        }
        // The Swing core, text and bean properties stay
        assertTrue(metal.includes("javax.swing.plaf.metal.MetalLookAndFeel"));
        assertTrue(metal.includes("javax.swing.text.html.HTMLEditorKit"));
        assertTrue(metal.includes("javax.swing.JTable"));
        assertTrue(metal.includes("java.lang.Integer#<init>(java.lang.String)"));
        assertTrue(metal.includes("javax/swing/text/rtf/charsets/*"));
        // The other look and feels go
        assertFalse(metal.includes("javax.swing.plaf.nimbus.ButtonPainter"));
        assertFalse(metal.includes("javax.swing.plaf.synth.SynthLookAndFeel#createUI(javax.swing.JComponent)"));
        assertFalse(metal.includes("com.sun.swing.internal.plaf.synth.resources.synth"));
        assertFalse(metal.includes("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"));
        assertFalse(metal.includes("com/sun/java/swing/plaf/windows/icons/*"));
        assertFalse(metal.includes("javax.swing.plaf.multi.MultiButtonUI#createUI(javax.swing.JComponent)"));
    }

    @Test
    void nimbusAndGtkIncludeTheSynthLookAndFeel() {
        for (IncludedLookAndFeel lookAndFeel : List.of(IncludedLookAndFeel.NIMBUS, IncludedLookAndFeel.GTK)) {
            IncludedLookAndFeels included = new IncludedLookAndFeels(Set.of(lookAndFeel));
            assertTrue(included.includes("javax.swing.plaf.synth.SynthLookAndFeel#createUI(javax.swing.JComponent)"));
            assertTrue(included.includes("com.sun.swing.internal.plaf.synth.resources.synth"));
            // but not the loading of Synth XML files
            assertFalse(included.includes("com.sun.beans.decoder.ObjectElementHandler"));
            assertFalse(included.includes("javax.swing.plaf.synth.ColorType"));
        }
        IncludedLookAndFeels synth = new IncludedLookAndFeels(Set.of(IncludedLookAndFeel.SYNTH));
        assertTrue(synth.includes("com.sun.beans.decoder.ObjectElementHandler"));
        assertTrue(synth.includes("javax.swing.plaf.synth.SynthLookAndFeel"));
        assertFalse(synth.includes("javax.swing.plaf.nimbus.NimbusLookAndFeel"));
    }

    /**
     * All the entries of all the lists.
     */
    private static List<String> allEntries() throws IllegalAccessException {
        List<String> entries = new ArrayList<>();
        for (Field field : SwingClassesAndResources.class.getDeclaredFields()) {
            if (field.getType() == String[].class) {
                entries.addAll(Arrays.asList((String[]) field.get(null)));
            }
        }
        return entries;
    }
}
