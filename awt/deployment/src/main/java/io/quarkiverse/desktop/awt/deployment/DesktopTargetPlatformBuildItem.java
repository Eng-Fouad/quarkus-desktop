package io.quarkiverse.desktop.awt.deployment;

import java.util.stream.Stream;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The platform a native executable is built for, produced for native builds only.
 * <p>
 * Windows when the build host is Windows and the build is not a container build (the rule of
 * {@code io.quarkus:quarkus-awt}), Linux otherwise. There is no macOS target : {@code io.quarkus:quarkus-awt} fails
 * native builds on macOS.
 */
public final class DesktopTargetPlatformBuildItem extends SimpleBuildItem {

    /**
     * A target platform.
     */
    public enum Platform {
        WINDOWS,
        LINUX
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

    /**
     * The value of the given ones that applies to this platform.
     */
    public <T> T select(T windows, T linux) {
        return isWindows() ? windows : linux;
    }

    /**
     * The entries of a common list, followed by those of the list of this platform.
     */
    public String[] withPlatform(String[] common, String[] windows, String[] linux) {
        return Stream.concat(Stream.of(common), Stream.of(select(windows, linux))).toArray(String[]::new);
    }
}
