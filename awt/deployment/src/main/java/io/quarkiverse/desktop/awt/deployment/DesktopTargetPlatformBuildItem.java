package io.quarkiverse.desktop.awt.deployment;

import java.util.stream.Stream;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The platform a native executable is built for, produced for native builds only.
 * <p>
 * The rule of {@code io.quarkus:quarkus-awt} : native-image does not cross compile, so a native build on a Windows host
 * produces a Windows executable and a native build on a macOS host a macOS executable, while a container build produces
 * a Linux executable whatever the host.
 */
public final class DesktopTargetPlatformBuildItem extends SimpleBuildItem {

    /**
     * A target platform.
     */
    public enum Platform {
        WINDOWS,
        LINUX,
        MAC
    }

    private final Platform platform;

    public DesktopTargetPlatformBuildItem(Platform platform) {
        this.platform = platform;
    }

    public Platform getPlatform() {
        return platform;
    }

    public boolean isWindows() {
        return platform == Platform.WINDOWS;
    }

    public boolean isLinux() {
        return platform == Platform.LINUX;
    }

    public boolean isMac() {
        return platform == Platform.MAC;
    }

    /**
     * The value of the given ones that applies to this platform.
     */
    public <T> T select(T windows, T linux, T mac) {
        return switch (platform) {
            case WINDOWS -> windows;
            case LINUX -> linux;
            case MAC -> mac;
        };
    }

    /**
     * The entries of a common list, followed by those of the list of this platform.
     */
    public String[] withPlatform(String[] common, String[] windows, String[] linux, String[] mac) {
        return Stream.concat(Stream.of(common), Stream.of(select(windows, linux, mac))).toArray(String[]::new);
    }
}
