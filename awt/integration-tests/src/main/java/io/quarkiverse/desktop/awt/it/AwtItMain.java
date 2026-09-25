package io.quarkiverse.desktop.awt.it;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Runs the scenario named by the first argument, prints its result, and exits.
 */
@QuarkusMain
public class AwtItMain implements QuarkusApplication {

    @Override
    public int run(String... args) {
        String scenario = args.length > 0 ? args[0] : "java2d";
        switch (scenario) {
            case "java2d" -> {
                BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.RED);
                g.fillRect(0, 0, 16, 16);
                g.dispose();
                System.out.println("java2d-ok " + Integer.toHexString(image.getRGB(8, 8)));
            }
            case "headless" -> System.out.println("headless=" + GraphicsEnvironment.isHeadless());
            default -> {
                System.out.println("unknown scenario " + scenario);
                return 1;
            }
        }
        return 0;
    }
}
