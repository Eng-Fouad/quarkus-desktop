package io.quarkiverse.desktop.awt.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class AwtItTest {

    @Test
    @Launch("java2d")
    public void java2d(LaunchResult result) {
        assertTrue(result.getOutput().contains("java2d-ok ffff0000"), result.getOutput());
    }
}
