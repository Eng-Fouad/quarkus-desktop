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
     * application can list only those it uses. Metal, the default look and feel, is always included; Nimbus and GTK
     * include the Synth look and feel they extend, but not the loading of Synth XML files ({@code synth}). Setting a look
     * and feel that is not included fails with a {@code ClassNotFoundException}.
     * <p>
     * This property has no effect in JVM mode.
     */
    @WithDefault("all")
    Set<IncludedLookAndFeel> includedLookAndFeels();

    /**
     * The JavaBeans API in native executables.
     */
    JavaBeans javaBeans();

    /**
     * The JavaBeans API ({@code java.beans}) in native executables (see also
     * {@code quarkus.desktop.awt.java-beans.jdk-classes}).
     */
    interface JavaBeans {

        /**
         * Whether the JDK Swing classes support the JavaBeans API in native executables : their public constructors,
         * methods and fields are registered for reflection, so that the {@code Introspector} finds their bean properties
         * and event sets, and that {@code XMLEncoder}, {@code XMLDecoder}, {@code Statement}, {@code Expression},
         * {@code EventHandler} and {@code Beans.instantiate} work with them, as in JVM mode. The AWT classes that they
         * extend are registered too (as with {@code quarkus.desktop.awt.java-beans.jdk-classes=true}).
         * <p>
         * The classes are the public classes of {@code javax.swing}, {@code javax.swing.border},
         * {@code javax.swing.event}, {@code javax.swing.table} and {@code javax.swing.tree} (components, models,
         * layouts, borders, actions, key strokes, icons, renderers and editors, events and listeners), the text
         * components, documents and formatters of {@code javax.swing.text}, and the UI resources of
         * {@code javax.swing.plaf}. For instance {@code XMLEncoder} writes a {@code JPanel} with its border, layout and
         * components, a {@code JTabbedPane}, a {@code JTree} with its nodes, and {@code XMLDecoder} reads them.
         * <p>
         * It is disabled by default because it makes a native executable 3 to 4 MB larger (many public methods
         * of the Swing classes are not used otherwise). When disabled, the {@code Introspector} finds no bean property
         * of these classes (other than the properties usually transferred with {@code new TransferHandler("text")},
         * which are always registered) and {@code XMLEncoder} cannot write them.
         */
        @WithDefault("false")
        boolean jdkClasses();
    }

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
