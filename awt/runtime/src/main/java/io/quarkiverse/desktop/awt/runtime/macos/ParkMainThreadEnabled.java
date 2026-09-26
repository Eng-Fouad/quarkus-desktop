package io.quarkiverse.desktop.awt.runtime.macos;

import java.util.function.BooleanSupplier;

import io.smallrye.common.os.OS;

/**
 * Whether the native executable keeps its first thread in the Cocoa event loop ({@link MacMainThread}) : evaluated in
 * the native image builder, which runs on the target operating system (a container build runs on Linux).
 */
public final class ParkMainThreadEnabled implements BooleanSupplier {

    /**
     * Builder system property : {@code false} to disable it on macOS ({@code quarkus.desktop.awt.macos.park-main-thread}).
     */
    public static final String PROPERTY = "io.quarkiverse.desktop.awt.macos.park-main-thread";

    /**
     * Builder system property, for the tests of the extension only : {@code true} enables it on any operating system,
     * where the first thread parks instead of running the Cocoa event loop. It checks on Windows and Linux that the
     * substitution builds and that the application runs and exits as usual on its new thread.
     */
    public static final String ANY_OS_PROPERTY = "io.quarkiverse.desktop.awt.macos.park-main-thread-on-any-os";

    @Override
    public boolean getAsBoolean() {
        return Boolean.getBoolean(ANY_OS_PROPERTY) || (OS.MAC.isCurrent() && !"false".equals(System.getProperty(PROPERTY)));
    }
}
