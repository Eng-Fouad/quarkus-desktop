package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;

class ResourceGlobsTest {

    private static final List<String> GLOBS = List.of("javax/swing/plaf/metal/icons/**", "sun/awt/resources/cursors/*",
            "sun/datatransfer/resources/flavormap.properties", "jdk/internal/icu/impl/data/**/ubidi.icu",
            "com/example/laf/icons/*");

    @Test
    void globsByModule() {
        assertEquals(Map.of(
                "java.base", Set.of("jdk/internal/icu/impl/data/**/ubidi.icu"),
                "java.datatransfer", Set.of("sun/datatransfer/resources/flavormap.properties"),
                "java.desktop", Set.of("javax/swing/plaf/metal/icons/**", "sun/awt/resources/cursors/*"),
                ResourceGlobs.CLASS_PATH, Set.of("com/example/laf/icons/*")), ResourceGlobs.byModule(GLOBS));
    }

    @Test
    void patterns() {
        List<NativeImageResourcePatternsBuildItem> patterns = ResourceGlobs.patterns(GLOBS);
        // one item per module with the Quarkus versions that register module resources, else one item
        assertEquals(ResourceGlobs.moduleResources() ? 4 : 1, patterns.size());
        assertEquals(List.of(), ResourceGlobs.patterns(List.of()));
    }
}
