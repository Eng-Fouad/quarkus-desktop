package io.quarkiverse.desktop.swing.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Desktop Swing configuration.
 */
@ConfigMapping(prefix = "quarkus.desktop.swing")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DesktopSwingConfig {

    /**
     * The look and feel set when the application starts, before it runs (on the event dispatch thread, in JVM mode and
     * in native executables).
     * <p>
     * {@code system} (the look and feel of the platform : Windows on Windows, GTK on a GNOME desktop, Metal otherwise),
     * {@code cross-platform} (Metal), {@code metal}, {@code nimbus}, {@code motif}, {@code windows},
     * {@code windows-classic}, {@code gtk}, or the class name of a look and feel (an application look and feel, or a
     * library one such as FlatLaf). When not set, Swing uses its default look and feel (Metal, unless the
     * {@code swing.defaultlaf} system property sets another one), and the application can set the look and feel itself
     * with {@code UIManager.setLookAndFeel}.
     * <p>
     * A look and feel that this platform does not support (for instance {@code windows} on Linux) or that cannot be
     * created is reported as a warning, and the default look and feel is kept. The look and feel classes of the JDK are
     * always included in native executables; an application look and feel class is registered for reflection when it is
     * in the Jandex index of the application, or when this property names it at build time.
     */
    Optional<String> lookAndFeel();
}
