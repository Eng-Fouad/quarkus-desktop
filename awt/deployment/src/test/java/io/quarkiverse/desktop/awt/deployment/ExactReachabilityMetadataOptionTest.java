package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExactReachabilityMetadataOptionTest {

    @Test
    void nativeImageOptions() {
        assertTrue(DesktopAwtProcessor.isExactReachabilityMetadataOption("--exact-reachability-metadata"));
        assertTrue(DesktopAwtProcessor.isExactReachabilityMetadataOption(" --exact-reachability-metadata=com.example"));
        assertTrue(DesktopAwtProcessor.isExactReachabilityMetadataOption("--exact-reachability-metadata-path=lib/a.jar"));
        assertTrue(DesktopAwtProcessor.isExactReachabilityMetadataOption("-H:ThrowMissingRegistrationErrors="));
        assertFalse(DesktopAwtProcessor.isExactReachabilityMetadataOption("-H:+UnlockExperimentalVMOptions"));
        assertFalse(DesktopAwtProcessor.isExactReachabilityMetadataOption("--enable-native-access=ALL-UNNAMED"));
    }
}
