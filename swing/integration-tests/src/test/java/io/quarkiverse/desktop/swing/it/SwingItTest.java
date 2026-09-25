package io.quarkiverse.desktop.swing.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class SwingItTest {

    @Test
    @Launch("paint")
    public void paint(LaunchResult result) {
        assertTrue(result.getOutput().contains("swing-ok painted=true"), result.getOutput());
    }
}
