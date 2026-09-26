package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkiverse.desktop.awt.deployment.DesktopTargetPlatformBuildItem.Platform;
import io.smallrye.common.os.OS;

/**
 * The target platform of a native build, and the lists it selects.
 */
class DesktopTargetPlatformTest {

    @Test
    void localBuildTargetsTheHost() {
        assertEquals(Platform.WINDOWS, DesktopAwtProcessor.targetPlatform(OS.WINDOWS, false));
        assertEquals(Platform.MAC, DesktopAwtProcessor.targetPlatform(OS.MAC, false));
        assertEquals(Platform.LINUX, DesktopAwtProcessor.targetPlatform(OS.LINUX, false));
        assertEquals(Platform.LINUX, DesktopAwtProcessor.targetPlatform(OS.OTHER, false));
    }

    @Test
    void containerBuildTargetsLinux() {
        for (OS host : OS.values()) {
            assertEquals(Platform.LINUX, DesktopAwtProcessor.targetPlatform(host, true), host.name());
        }
    }

    @Test
    void platformLists() {
        String[] common = { "c1", "c2" };
        String[] windows = { "w" };
        String[] linux = { "l1", "l2" };
        String[] mac = { "m" };
        assertArrayEquals(new String[] { "c1", "c2", "w" },
                new DesktopTargetPlatformBuildItem(Platform.WINDOWS).withPlatform(common, windows, linux, mac));
        assertArrayEquals(new String[] { "c1", "c2", "l1", "l2" },
                new DesktopTargetPlatformBuildItem(Platform.LINUX).withPlatform(common, windows, linux, mac));
        assertArrayEquals(new String[] { "c1", "c2", "m" },
                new DesktopTargetPlatformBuildItem(Platform.MAC).withPlatform(common, windows, linux, mac));
    }

    @Test
    void selection() {
        DesktopTargetPlatformBuildItem mac = new DesktopTargetPlatformBuildItem(Platform.MAC);
        assertTrue(mac.isMac() && !mac.isWindows() && !mac.isLinux());
        assertEquals("mac", mac.select("windows", "linux", "mac"));
        assertEquals("windows", new DesktopTargetPlatformBuildItem(Platform.WINDOWS).select("windows", "linux", "mac"));
        assertEquals("linux", new DesktopTargetPlatformBuildItem(Platform.LINUX).select("windows", "linux", "mac"));
    }
}
