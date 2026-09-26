package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The Windows and macOS substitutions of quarkus-awt that break GUI applications, and the one that disables Type 1 fonts,
 * are removed from the application (the native integration tests check that the native build does not get them).
 */
class QuarkusAwtGuiBlockersTest {

    /**
     * The build and the test methods use different class loaders : the removed resources are passed in a system
     * property.
     */
    private static final String REMOVED = QuarkusAwtGuiBlockersTest.class.getName() + ".removed";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            // The quarkus-awt version of the build has no other substitution
            .setLogRecordPredicate(r -> DesktopAwtTest.message(r).contains("substitutions"))
            .assertLogRecords(records -> assertTrue(records.isEmpty(), records.toString()))
            .addBuildChainCustomizer(chain -> chain.addBuildStep(new BuildStep() {
                @Override
                public void execute(BuildContext context) {
                    Set<String> removed = new TreeSet<>();
                    for (RemovedResourceBuildItem item : context.consumeMulti(RemovedResourceBuildItem.class)) {
                        if (item.getArtifact().getGroupId().equals("io.quarkus")
                                && item.getArtifact().getArtifactId().equals("quarkus-awt")) {
                            removed.addAll(item.getResources());
                        }
                    }
                    System.setProperty(REMOVED, String.join(",", removed));
                    context.produce(new FeatureBuildItem("removed-resources-recorder"));
                }
            }).consumes(RemovedResourceBuildItem.class).produces(FeatureBuildItem.class).build());

    @Test
    void removedResourceBuildItemIsProduced() {
        assertEquals(String.join(",", new TreeSet<>(Set.of(
                "io/quarkus/awt/runtime/Target_sun_awt_windows_WObjectPeer.class",
                "io/quarkus/awt/runtime/Target_sun_java2d_windows_WindowsFlags.class",
                "io/quarkus/awt/runtime/Target_sun_awt_windows_WToolkit.class",
                "io/quarkus/awt/runtime/Target_sun_font_Type1Font.class",
                "io/quarkus/awt/runtime/Target_sun_awt_PlatformGraphicsInfo_Mac.class",
                "io/quarkus/awt/runtime/Target_sun_lwawt_macosx_LWCToolkit.class",
                "io/quarkus/awt/runtime/Target_sun_awt_CGraphicsEnvironment.class",
                "io/quarkus/awt/runtime/Target_sun_print_PlatformPrinterJobProxy.class"))), System.getProperty(REMOVED));
    }
}
