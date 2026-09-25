package io.quarkiverse.desktop.awt.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class DesktopAwtProcessor {

    private static final String FEATURE = "desktop-awt";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
