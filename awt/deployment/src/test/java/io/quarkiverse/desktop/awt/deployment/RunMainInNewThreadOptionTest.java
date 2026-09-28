package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The native image options that move {@code main} off the first thread (an AWT or Swing user interface hangs on macOS).
 */
class RunMainInNewThreadOptionTest {

    @Test
    void nativeImageOptions() {
        assertTrue(DesktopAwtProcessor.isRunMainInNewThreadOption("-H:+RunMainInNewThread"));
        assertTrue(DesktopAwtProcessor.isRunMainInNewThreadOption(" -H:+RunMainInNewThread "));
        // the option that keeps main on the first thread
        assertFalse(DesktopAwtProcessor.isRunMainInNewThreadOption("-H:-RunMainInNewThread"));
        assertFalse(DesktopAwtProcessor.isRunMainInNewThreadOption("--exact-reachability-metadata"));
    }
}
