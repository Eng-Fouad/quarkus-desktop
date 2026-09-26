package io.quarkiverse.desktop.awt.runtime.macos;

import java.util.List;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CLibrary;

/**
 * The CoreFoundation and libSystem functions used on the first thread of a macOS native executable.
 * <p>
 * native-image reads {@code @CLibrary} on every class of the image class path : the Darwin only directives keep
 * {@code -framework CoreFoundation} out of the Linux and Windows links.
 */
@CContext(CoreFoundation.DarwinOnly.class)
@CLibrary("-framework CoreFoundation")
final class CoreFoundation {

    /**
     * The directives of the macOS native executables only.
     */
    public static final class DarwinOnly implements CContext.Directives {

        @Override
        public boolean isInConfiguration() {
            return Platform.includedIn(Platform.DARWIN.class);
        }

        @Override
        public List<String> getHeaderFiles() {
            return List.of("<CoreFoundation/CoreFoundation.h>", "<pthread.h>");
        }
    }

    private CoreFoundation() {
    }

    /**
     * {@code void CFRunLoopRun(void)} : runs the run loop of the current thread in its default mode until it is stopped.
     * A transition to native code : it blocks, and AppKit calls back into Java (JNI) on this thread.
     */
    @CFunction(value = "CFRunLoopRun", transition = CFunction.Transition.TO_NATIVE)
    static native void runCurrentRunLoop();

    /**
     * {@code int pthread_main_np(void)} : {@code 1} on the first thread of the process.
     */
    @CFunction(value = "pthread_main_np", transition = CFunction.Transition.NO_TRANSITION)
    static native int pthreadMainNp();
}
