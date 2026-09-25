package io.quarkiverse.desktop.swing.it;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Runs the scenario named by the first argument, prints its result, and exits.
 */
@QuarkusMain
public class SwingItMain implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        String scenario = args.length > 0 ? args[0] : "paint";
        switch (scenario) {
            case "paint" -> {
                // paints a button into an image, on the event dispatch thread, without showing any window
                AtomicReference<String> result = new AtomicReference<>();
                SwingUtilities.invokeAndWait(() -> {
                    JButton button = new JButton("Quarkus");
                    Dimension size = button.getPreferredSize();
                    button.setSize(size);
                    button.doLayout();
                    BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = image.createGraphics();
                    button.paint(g);
                    g.dispose();
                    result.set("swing-ok painted=" + isPainted(image));
                });
                System.out.println(result.get());
            }
            default -> {
                System.out.println("unknown scenario " + scenario);
                return 1;
            }
        }
        return 0;
    }

    private static boolean isPainted(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
