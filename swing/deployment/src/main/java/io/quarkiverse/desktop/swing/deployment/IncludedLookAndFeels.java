package io.quarkiverse.desktop.swing.deployment;

import static io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel.ALL;
import static io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel.GTK;
import static io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel.MOTIF;
import static io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel.MULTI;
import static io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel.NIMBUS;
import static io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel.SYNTH;
import static io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel.WINDOWS;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkiverse.desktop.swing.runtime.DesktopSwingBuildTimeConfig.IncludedLookAndFeel;

/**
 * The look and feels of the JDK included in a native executable ({@code quarkus.desktop.swing.included-look-and-feels}) :
 * filters the entries of the {@link SwingClassesAndResources} lists. An entry (class, member, resource bundle or
 * resource) belongs to a look and feel by its package or path; the other entries are always included.
 */
final class IncludedLookAndFeels {

    /**
     * The prefixes of the entries of each look and feel.
     */
    private static final Map<IncludedLookAndFeel, List<String>> PREFIXES = Map.of(
            NIMBUS, List.of("javax.swing.plaf.nimbus."),
            // The loading of Synth XML files : the beans decoder, the color types by name
            SYNTH, List.of("com.sun.beans.decoder.", "javax.swing.plaf.synth.ColorType", "java.lang.Class#newInstance()"),
            MOTIF, List.of("com.sun.java.swing.plaf.motif.", "com/sun/java/swing/plaf/motif/"),
            WINDOWS, List.of("com.sun.java.swing.plaf.windows.", "com/sun/java/swing/plaf/windows/"),
            GTK, List.of("com.sun.java.swing.plaf.gtk.", "com/sun/java/swing/plaf/gtk/"),
            MULTI, List.of("javax.swing.plaf.multi."));

    /**
     * The Synth look and feel, which Nimbus and GTK extend.
     */
    private static final List<String> SYNTH_CORE = List.of("javax.swing.plaf.synth.SynthLookAndFeel",
            "com.sun.swing.internal.plaf.synth.");

    private final Set<IncludedLookAndFeel> included;

    IncludedLookAndFeels(Set<IncludedLookAndFeel> configured) {
        this.included = configured.isEmpty() || configured.contains(ALL) ? EnumSet.allOf(IncludedLookAndFeel.class)
                : EnumSet.copyOf(configured);
    }

    boolean isIncluded(IncludedLookAndFeel lookAndFeel) {
        return included.contains(lookAndFeel);
    }

    /**
     * Whether a list entry is included.
     */
    boolean includes(String entry) {
        for (Map.Entry<IncludedLookAndFeel, List<String>> lookAndFeel : PREFIXES.entrySet()) {
            if (!included.contains(lookAndFeel.getKey()) && startsWithAny(entry, lookAndFeel.getValue())) {
                return false;
            }
        }
        if (startsWithAny(entry, SYNTH_CORE)) {
            return included.contains(SYNTH) || included.contains(NIMBUS) || included.contains(GTK);
        }
        return true;
    }

    /**
     * The included entries of a list.
     */
    String[] filter(String[] entries) {
        return Stream.of(entries).filter(this::includes).toArray(String[]::new);
    }

    private static boolean startsWithAny(String entry, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (entry.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
