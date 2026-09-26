package io.quarkiverse.desktop.awt.runtime.macos;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;

import org.graalvm.nativeimage.Platform;

import io.quarkus.runtime.Application;
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

/**
 * Keeps the first thread of a macOS native executable in the Cocoa event loop, as the {@code java} launcher does, and
 * runs the Quarkus application on a new thread named {@code main}.
 * <p>
 * AppKit only runs on the first thread of the process. When AWT starts (or JavaFX), it asks that thread to create the
 * application and to run its event loop ({@code [NSApp run]}), and waits for it : the first thread must run a Cocoa run
 * loop, which a native executable does not do by itself, since it runs {@code main} on that thread. The first thread
 * runs {@code CFRunLoopRun()} instead, in which AppKit starts lazily and runs for the life of the process, whether the
 * application uses AWT or not (headless applications too : some AWT functions also wait for the first thread).
 * <p>
 * Every production entry point of a Quarkus application ends in {@code Quarkus.run(Class, BiConsumer, String...)}, which
 * {@link Target_io_quarkus_runtime_Quarkus} replaces with {@link #runQuarkus}.
 */
public final class MacMainThread {

    /**
     * Run time system property set to {@code true} once the first thread runs the Cocoa event loop (not a
     * {@code quarkus.*} property : Quarkus would report it as an unknown configuration property).
     */
    public static final String PARKED = "io.quarkiverse.desktop.main-thread-parked";

    /**
     * System property : the stack size of the {@code main} thread, in bytes (a run time default registered from the
     * configuration).
     */
    public static final String STACK_SIZE_PROPERTY = "io.quarkiverse.desktop.awt.macos.main-thread-stack-size";

    /**
     * System property : how long {@code System.exit} may take before the process is halted, in milliseconds, {@code 0}
     * to wait for ever (a run time default registered from the configuration).
     */
    public static final String EXIT_HALT_TIMEOUT_PROPERTY = "io.quarkiverse.desktop.awt.macos.exit-halt-timeout-ms";

    /**
     * The name of the first thread while it runs the Cocoa event loop. AWT renames it {@code AppKit Thread} when it
     * starts AppKit itself (not when it runs embedded in JavaFX).
     */
    public static final String RUN_LOOP_THREAD_NAME = "Cocoa Run Loop";

    static final long DEFAULT_STACK_SIZE = 8L << 20;
    static final long DEFAULT_EXIT_HALT_TIMEOUT_MILLIS = 10_000L;

    private MacMainThread() {
    }

    /**
     * Runs the Quarkus application on a new thread named {@code main}, and the Cocoa event loop on this thread for the
     * life of the process, when this is the first thread of the process. Runs the Quarkus application on this thread
     * otherwise.
     */
    static void runQuarkus(Class<? extends QuarkusApplication> quarkusApplication,
            BiConsumer<Integer, Throwable> exitHandler, String... args) {
        Runnable quarkus = () -> runQuarkusHere(quarkusApplication, exitHandler, args);
        if (System.getProperty(PARKED) != null) {
            // the application is already running on its thread (Quarkus.run called again)
            quarkus.run();
            return;
        }
        if (!isFirstThread()) {
            System.err.println("[quarkus-desktop] Quarkus.run is not called on the first thread of the process : an AWT"
                    + " or Swing user interface needs this thread for the Cocoa event loop and may hang"
                    + " (-H:+RunMainInNewThread, or Quarkus.run called from another thread)");
            quarkus.run();
            return;
        }
        System.setProperty(PARKED, "true");
        Thread first = Thread.currentThread();
        Thread main = new Thread(first.getThreadGroup(), () -> runApplication(quarkus, first), "main",
                Long.getLong(STACK_SIZE_PROPERTY, DEFAULT_STACK_SIZE));
        main.setContextClassLoader(first.getContextClassLoader());
        main.setDaemon(false);
        first.setName(RUN_LOOP_THREAD_NAME);
        main.start();
        runCocoaEventLoop();
    }

    private static boolean isFirstThread() {
        if (Platform.includedIn(Platform.DARWIN.class)) {
            return CoreFoundation.pthreadMainNp() == 1;
        }
        // Tests on other operating systems (ParkMainThreadEnabled.ANY_OS_PROPERTY) : Quarkus.run is called by main
        return true;
    }

    /**
     * Never returns : the process exits from another thread, as with the {@code java} launcher.
     */
    private static void runCocoaEventLoop() {
        for (;;) {
            long start = System.nanoTime();
            if (Platform.includedIn(Platform.DARWIN.class)) {
                // [NSApp run] of AWT (or JavaFX) runs nested in here, for the life of the process
                CoreFoundation.runCurrentRunLoop();
            } else {
                // Tests on other operating systems (ParkMainThreadEnabled.ANY_OS_PROPERTY) : no event loop
                LockSupport.park();
            }
            if (System.nanoTime() - start < 1_000_000L) {
                // a stopped or empty run loop (not expected) : no busy loop
                LockSupport.parkNanos(10_000_000L);
            }
        }
    }

    private static void runApplication(Runnable quarkus, Thread first) {
        installExitWatchdog();
        int code = 0;
        try {
            // the default exit handler ends with System.exit(code)
            quarkus.run();
        } catch (Throwable t) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
            code = 1;
        }
        // An exit handler that does not exit : wait for the other threads (the event dispatch thread...), as the java
        // launcher does when main returns
        joinNonDaemonThreads(first);
        System.exit(code);
    }

    private static void joinNonDaemonThreads(Thread first) {
        Thread self = Thread.currentThread();
        for (;;) {
            Thread next = null;
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread != self && thread != first && !thread.isDaemon() && thread.isAlive()) {
                    next = thread;
                    break;
                }
            }
            if (next == null) {
                return;
            }
            try {
                next.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Halts the process when {@code System.exit} does not complete in time once the application has stopped
     * (oracle/graal#13994 reports an exit that never completes while AppKit runs on the first thread).
     */
    private static void installExitWatchdog() {
        long timeout = Long.getLong(EXIT_HALT_TIMEOUT_PROPERTY, DEFAULT_EXIT_HALT_TIMEOUT_MILLIS);
        if (timeout <= 0) {
            return;
        }
        BiConsumer<Integer, Throwable> delegate = ApplicationLifecycleManager.getDefaultExitCodeHandler();
        ApplicationLifecycleManager.setDefaultExitCodeHandler((Integer code, Throwable cause) -> {
            Thread watchdog = new Thread(() -> {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    return;
                }
                System.err.println("[quarkus-desktop] System.exit(" + code + ") did not complete within " + timeout
                        + " ms : halting");
                Runtime.getRuntime().halt(code);
            }, "quarkus-desktop-exit-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
            delegate.accept(code, cause);
        });
    }

    /**
     * The production path of {@code Quarkus.run(Class, BiConsumer, String...)} (the deployment module warns when it
     * changes).
     */
    @SuppressWarnings("unchecked")
    static void runQuarkusHere(Class<? extends QuarkusApplication> quarkusApplication,
            BiConsumer<Integer, Throwable> exitHandler, String... args) {
        try {
            System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
            System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory",
                    "io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory");
            Class<? extends Application> applicationClass = (Class<? extends Application>) Class.forName(
                    Application.APP_CLASS_NAME, false, Thread.currentThread().getContextClassLoader());
            Quarkus.class.getModule().addReads(applicationClass.getModule());
            MethodHandle constructor = MethodHandles.lookup().findConstructor(applicationClass,
                    MethodType.methodType(void.class));
            Application application = (Application) constructor.invoke();
            ApplicationLifecycleManager.run(application, quarkusApplication, exitHandler, args);
        } catch (Throwable t) {
            t.printStackTrace();
            (exitHandler != null ? exitHandler : ApplicationLifecycleManager.getDefaultExitCodeHandler()).accept(1, t);
        }
    }
}
