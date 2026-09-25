package io.quarkiverse.desktop.swing.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class DesktopSwingProcessor {

    private static final String FEATURE = "desktop-swing";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
