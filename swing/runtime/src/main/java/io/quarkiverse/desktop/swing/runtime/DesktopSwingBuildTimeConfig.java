package io.quarkiverse.desktop.swing.runtime;

import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Desktop Swing build time configuration.
 */
@ConfigMapping(prefix = "quarkus.desktop.swing")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DesktopSwingBuildTimeConfig {

    /**
     * The look and feels of the JDK included in native executables : {@code all}, or a list of {@code metal},
     * {@code nimbus}, {@code synth}, {@code motif}, {@code windows} (with Windows Classic), {@code gtk} and
     * {@code multi} (the multiplexing look and feel of the auxiliary look and feels).
     * <p>
     * By default, native executables include all the look and feels of the JDK for their platform. They make the native
     * executable larger (about 10 MB for all of them : Nimbus and the multiplexing look and feel about 3 MB each), so an
     * application can list only those it uses. Metal, the default look and feel, is always included (and Aqua on macOS,
     * which draws the AWT components there); Nimbus and GTK include the Synth look and feel they extend, but not the
     * loading of Synth XML files ({@code synth}). Setting a look and feel that is not included fails with a
     * {@code ClassNotFoundException}.
     * <p>
     * This property has no effect in JVM mode.
     */
    @WithDefault("all")
    Set<IncludedLookAndFeel> includedLookAndFeels();

    /**
     * A look and feel of the JDK included in native executables.
     */
    enum IncludedLookAndFeel {
        /**
         * All the look and feels of the JDK.
         */
        ALL,
        /**
         * Metal, always included (the default look and feel).
         */
        METAL,
        /**
         * Nimbus (with the Synth look and feel it extends).
         */
        NIMBUS,
        /**
         * Synth, with the loading of Synth XML files.
         */
        SYNTH,
        /**
         * CDE/Motif.
         */
        MOTIF,
        /**
         * Windows and Windows Classic (Windows native executables).
         */
        WINDOWS,
        /**
         * GTK (with the Synth look and feel it extends; Linux native executables).
         */
        GTK,
        /**
         * The multiplexing look and feel, used when auxiliary look and feels are installed
         * ({@code UIManager.addAuxiliaryLookAndFeel}).
         */
        MULTI
    }
}
