package io.quarkiverse.desktop.swing.it;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * An application Metal theme (Quarkus colors).
 */
public class QuarkusTheme extends DefaultMetalTheme {

    private static final ColorUIResource PRIMARY1 = new ColorUIResource(0x0d, 0x1c, 0x2c);
    private static final ColorUIResource PRIMARY2 = new ColorUIResource(0x42, 0x69, 0xe1);
    private static final ColorUIResource PRIMARY3 = new ColorUIResource(0xa6, 0xc1, 0xf7);

    @Override
    public String getName() {
        return "Quarkus";
    }

    @Override
    protected ColorUIResource getPrimary1() {
        return PRIMARY1;
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return PRIMARY2;
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return PRIMARY3;
    }
}
