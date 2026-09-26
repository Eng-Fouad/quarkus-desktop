package io.quarkiverse.desktop.swing.it;

import javax.swing.JButton;
import javax.swing.UIManager;

/**
 * A button with an application UI delegate ({@link RoundButtonUI}), found by UI class ID.
 */
public class RoundButton extends JButton {

    public RoundButton(String text) {
        super(text);
    }

    @Override
    public void updateUI() {
        if (UIManager.get(QuarkusLookAndFeel.ROUND_BUTTON_UI) == null) {
            // The application look and feel is not set : register the UI delegate for the current one
            UIManager.put(QuarkusLookAndFeel.ROUND_BUTTON_UI, RoundButtonUI.class.getName());
        }
        super.updateUI();
    }

    @Override
    public String getUIClassID() {
        return QuarkusLookAndFeel.ROUND_BUTTON_UI;
    }
}
