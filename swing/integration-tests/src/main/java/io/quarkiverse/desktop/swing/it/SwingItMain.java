package io.quarkiverse.desktop.swing.it;

import java.nio.file.Path;

import javax.swing.UIManager;

import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Runs the scenario named by the first argument, prints its results, and exits with 1 when a check failed.
 * <p>
 * Scenarios :
 * <ul>
 * <li>{@code swing [directory]} : the Swing checks ({@link SwingChecks}). The rendered images and the defaults of the
 * look and feels are written to the directory (the temporary directory by default).</li>
 * <li>{@code look-and-feel} : the look and feel set at startup ({@code quarkus.desktop.swing.look-and-feel}).</li>
 * </ul>
 */
@QuarkusMain
public class SwingItMain implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        // Set by the extension at startup, before the application runs
        String startupLookAndFeel = UIManager.getLookAndFeel().getClass().getName();
        String scenario = args.length > 0 ? args[0] : "swing";
        switch (scenario) {
            case "swing" -> {
                Path directory = Path.of(args.length > 1 ? args[1] : System.getProperty("java.io.tmpdir"));
                String mode = ImageMode.current().isNativeImage() ? "native" : "jvm";
                return new SwingChecks(directory, mode, startupLookAndFeel).run();
            }
            case "look-and-feel" -> {
                System.out.println("startupLookAndFeel=" + startupLookAndFeel);
                return 0;
            }
            default -> {
                System.out.println("Unknown scenario " + scenario);
                return 2;
            }
        }
    }
}
