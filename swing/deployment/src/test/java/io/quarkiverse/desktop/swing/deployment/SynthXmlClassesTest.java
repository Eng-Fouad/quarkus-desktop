package io.quarkiverse.desktop.swing.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SynthXmlClassesTest {

    @Test
    void findsTheClassesOfSynthFiles() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!-- <object class="com.example.Commented"/> -->
                <synth>
                    <object id="color" class="javax.swing.plaf.ColorUIResource"><int>1</int><int>2</int><int>3</int></object>
                    <object id="orange" class='java.awt.Color' field="ORANGE"/>
                    <object id="painter" class="com.example.GlossyPainter"/>
                    <object id="list" class="java.util.ArrayList"><void method="add"><class>com.example.Item</class></void></object>
                    <style id="default"><painter idref="painter"/></style>
                </synth>
                """;
        assertEquals(Set.of("javax.swing.plaf.ColorUIResource", "java.awt.Color", "com.example.GlossyPainter",
                "java.util.ArrayList", "com.example.Item"), SynthXmlClasses.classes(xml));
    }

    @Test
    void ignoresOtherXmlFiles() {
        assertEquals(Set.of(), SynthXmlClasses.classes("<beans><bean class=\"com.example.Bean\"/></beans>"));
        assertEquals(Set.of(), SynthXmlClasses.classes("<synthesizer class=\"com.example.Bean\"/>"));
    }

    @Test
    void candidates() {
        assertTrue(SynthXmlClasses.isCandidate("com/example/laf/synth.xml"));
        assertFalse(SynthXmlClasses.isCandidate("META-INF/beans.xml"));
        assertFalse(SynthXmlClasses.isCandidate("com/example/laf/synth.properties"));
    }
}
