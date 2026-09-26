package io.quarkiverse.desktop.awt.runtime.macos;

import java.util.function.BiConsumer;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.QuarkusApplication;

/**
 * Every production entry point of a Quarkus application (the generated main class, the {@code main} methods of
 * {@code @QuarkusMain} classes) ends in this method : on macOS, it moves the application off the first thread, which
 * runs the Cocoa event loop (see {@link MacMainThread}).
 */
@TargetClass(className = "io.quarkus.runtime.Quarkus", onlyWith = ParkMainThreadEnabled.class)
final class Target_io_quarkus_runtime_Quarkus {

    @Substitute
    public static void run(Class<? extends QuarkusApplication> quarkusApplication,
            BiConsumer<Integer, Throwable> exitHandler, String... args) {
        MacMainThread.runQuarkus(quarkusApplication, exitHandler, args);
    }
}
