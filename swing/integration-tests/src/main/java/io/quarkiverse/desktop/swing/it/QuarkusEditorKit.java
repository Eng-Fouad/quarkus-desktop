package io.quarkiverse.desktop.swing.it;

import javax.swing.text.StyledEditorKit;

/**
 * An application editor kit, registered by class name for a content type : Swing creates it with reflection.
 */
public class QuarkusEditorKit extends StyledEditorKit {

    static final String CONTENT_TYPE = "text/x-quarkus";

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }
}
