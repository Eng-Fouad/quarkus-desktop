package io.quarkiverse.desktop.swing.it;

import javax.swing.UIDefaults;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * An application look and feel, set by class name : Swing creates it with reflection.
 */
public class QuarkusLookAndFeel extends MetalLookAndFeel {

    /**
     * The UI class ID of {@link RoundButton}.
     */
    static final String ROUND_BUTTON_UI = "RoundButtonUI";

    @Override
    public String getName() {
        return "Quarkus";
    }

    @Override
    public String getID() {
        return "Quarkus";
    }

    @Override
    public String getDescription() {
        return "The Quarkus Desktop integration tests look and feel";
    }

    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        // An application UI delegate, created with reflection (its static createUI method)
        table.put(ROUND_BUTTON_UI, RoundButtonUI.class.getName());
    }
}
