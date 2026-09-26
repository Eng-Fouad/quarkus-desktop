package io.quarkiverse.desktop.swing.it;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import javax.swing.plaf.synth.SynthLookAndFeel;

/**
 * The look and feels exercised by the checks, and a signature of their defaults that must be the same in JVM mode and
 * in a native executable.
 */
public final class LookAndFeels {

    /**
     * A look and feel exercised by the checks.
     *
     * @param id the name of the look and feel in the check names
     * @param className the look and feel class, set by name ({@code UIManager.setLookAndFeel(String)}), except for
     *        Synth, which is loaded from an XML file
     */
    public record Laf(String id, String className) {
    }

    public static final String METAL = MetalLookAndFeel.class.getName();
    public static final String NIMBUS = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
    public static final String MOTIF = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
    public static final String WINDOWS = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    public static final String WINDOWS_CLASSIC = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
    public static final String GTK = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
    public static final String SYNTH = SynthLookAndFeel.class.getName();

    /**
     * UI texts of the look and feels (their resource bundles), checked with each look and feel.
     */
    static final List<String> TEXTS = List.of(
            "AbstractButton.clickText",
            "ColorChooser.hsvText",
            "FileChooser.acceptAllFileFilterText",
            "FileChooser.cancelButtonText",
            "FileChooser.detailsViewActionLabelText",
            "FileChooser.foldersLabelText",
            "FileChooser.lookInLabelText",
            "FileChooser.pathLabelText",
            "FormView.submitButtonText",
            "InternalFrameTitlePane.closeButtonText",
            "MetalTitlePane.closeTitle");

    private LookAndFeels() {
    }

    /**
     * The look and feels to check on this platform : the JDK ones installed on this platform (Metal with its themes,
     * Nimbus, Motif, Windows and Windows Classic, GTK), Synth loaded from XML, the system one, and an application one.
     * An installed look and feel may still be unsupported (GTK without the GTK libraries) : setting it fails with an
     * {@code UnsupportedLookAndFeelException}.
     */
    public static List<Laf> all() {
        List<Laf> lafs = new ArrayList<>();
        lafs.add(new Laf("metal", METAL));
        lafs.add(new Laf("metal-steel", METAL));
        lafs.add(new Laf("metal-custom", METAL));
        lafs.add(new Laf("nimbus", NIMBUS));
        lafs.add(new Laf("synth", SYNTH));
        lafs.add(new Laf("motif", MOTIF));
        if (isInstalled(WINDOWS)) {
            lafs.add(new Laf("windows", WINDOWS));
        }
        if (isInstalled(WINDOWS_CLASSIC)) {
            lafs.add(new Laf("windows-classic", WINDOWS_CLASSIC));
        }
        if (isInstalled(GTK)) {
            lafs.add(new Laf("gtk", GTK));
        }
        lafs.add(new Laf("system", UIManager.getSystemLookAndFeelClassName()));
        lafs.add(new Laf("application", QuarkusLookAndFeel.class.getName()));
        return lafs;
    }

    static boolean isInstalled(String className) {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (info.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the look and feel (call on the event dispatch thread).
     */
    public static void set(Laf laf) throws Exception {
        switch (laf.id()) {
            case "metal" -> MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            case "metal-steel" -> MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
            case "metal-custom" -> MetalLookAndFeel.setCurrentTheme(new QuarkusTheme());
            default -> {
                // Reset the theme changed by the checks
                MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            }
        }
        if (laf.id().equals("synth")) {
            SynthLookAndFeel synth = new SynthLookAndFeel();
            synth.load(LookAndFeels.class.getResourceAsStream("synth/synth.xml"), LookAndFeels.class);
            UIManager.setLookAndFeel(synth);
        } else {
            UIManager.setLookAndFeel(laf.className());
        }
    }

    /**
     * The defaults of the current look and feel, each resolved (lazy and active values are created), described by type
     * and main properties, followed by the UI texts.
     */
    public static Map<String, String> describeDefaults() {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Map<String, String> description = new TreeMap<>();
        for (Object key : new ArrayList<>(defaults.keySet())) {
            if (!(key instanceof String name) || defaults.get(key) instanceof Class) {
                continue;
            }
            description.put(name, describe(defaults.get(key)));
        }
        for (String text : TEXTS) {
            description.put("text:" + text, String.valueOf(UIManager.getString(text, Locale.ROOT)));
        }
        return description;
    }

    /**
     * A signature of the defaults of the current look and feel : the number of entries, the number of entries that
     * resolve to {@code null}, and a hash of their description.
     */
    public static String signature(Map<String, String> description) {
        StringBuilder all = new StringBuilder();
        long nulls = description.values().stream().filter("null"::equals).count();
        description.forEach((key, value) -> all.append(key).append('=').append(value).append('\n'));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(all.toString().getBytes(StandardCharsets.UTF_8));
            return "keys=" + description.size() + " nulls=" + nulls + " hash="
                    + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The key bindings that the current look and feel installs on the components (call on the event dispatch thread) :
     * the actions (loaded with reflection by the basic look and feel) and the key strokes (parsed with reflection on
     * {@code KeyEvent}) of each component.
     */
    public static String bindingsSignature() {
        Map<String, String> bindings = new TreeMap<>();
        List<JComponent> components = new ArrayList<>(List.of(new JButton(), new JCheckBox(), new JComboBox<>(),
                new JDesktopPane(), new JEditorPane(), new JFormattedTextField(), new JInternalFrame(), new JLabel("label"),
                new JList<>(), new JMenu(), new JMenuBar(), new JMenuItem(), new JOptionPane(), new JPasswordField(),
                new JPopupMenu(), new JProgressBar(), new JRadioButton(), new JRootPane(), new JScrollBar(),
                new JScrollPane(), new JSpinner(), new JSplitPane(), new JTabbedPane(), new JTable(), new JTextArea(),
                new JTextField(), new JTextPane(), new JToggleButton(), new JToolBar(), new JTree()));
        if (Gallery.sliderSupported()) {
            components.add(new JSlider());
        }
        int actions = 0;
        int keys = 0;
        for (JComponent component : components) {
            Object[] actionKeys = component.getActionMap().allKeys();
            int componentActions = actionKeys == null ? 0 : actionKeys.length;
            int componentKeys = 0;
            for (int condition : new int[] { JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                    JComponent.WHEN_IN_FOCUSED_WINDOW }) {
                KeyStroke[] strokes = component.getInputMap(condition).allKeys();
                componentKeys += strokes == null ? 0 : strokes.length;
            }
            actions += componentActions;
            keys += componentKeys;
            bindings.put(component.getClass().getSimpleName(), componentActions + "/" + componentKeys);
        }
        StringBuilder all = new StringBuilder();
        bindings.forEach((component, count) -> all.append(component).append('=').append(count).append('\n'));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(all.toString().getBytes(StandardCharsets.UTF_8));
            return "actions=" + actions + " keys=" + keys + " bindings=" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The metrics of the logical fonts, and of the fonts of some components with the current look and feel (call on the
     * event dispatch thread) : they size the components.
     */
    public static String fontMetrics() {
        StringBuilder metrics = new StringBuilder();
        JLabel label = new JLabel();
        for (String name : new String[] { Font.DIALOG, Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED, Font.DIALOG_INPUT }) {
            for (int style : new int[] { Font.PLAIN, Font.BOLD }) {
                java.awt.FontMetrics fm = label.getFontMetrics(new Font(name, style, 12));
                metrics.append(name).append(style == Font.BOLD ? "-bold" : "").append('=').append(fm.getAscent())
                        .append('/').append(fm.getDescent()).append('/').append(fm.getLeading()).append('/')
                        .append(fm.stringWidth("Quarkus Desktop")).append(' ');
            }
        }
        return metrics.toString().trim();
    }

    static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        String type = typeName(value.getClass());
        if (value instanceof Color color) {
            return type + "#" + Integer.toHexString(color.getRGB());
        } else if (value instanceof Font font) {
            return type + ":" + font.getFamily(Locale.ROOT) + "," + font.getStyle() + "," + font.getSize();
        } else if (value instanceof Icon icon) {
            // The size of the GTK icons depends on the GTK theme, loaded when a component is painted
            return type.startsWith("com.sun.java.swing.plaf.gtk.") ? type
                    : type + ":" + icon.getIconWidth() + "x" + icon.getIconHeight();
        } else if (value instanceof Insets insets) {
            return type + ":" + insets.top + "," + insets.left + "," + insets.bottom + "," + insets.right;
        } else if (value instanceof Dimension dimension) {
            return type + ":" + dimension.width + "x" + dimension.height;
        } else if (value instanceof InputMap inputMap) {
            return type + ":" + (inputMap.allKeys() == null ? 0 : inputMap.allKeys().length);
        } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return type + ":" + value;
        } else if (value instanceof Object[] array) {
            return type + ":" + array.length;
        }
        return type;
    }

    /**
     * The name of a class, the same in JVM mode and in a native executable (lambda class names are not).
     */
    static String typeName(Class<?> type) {
        String name = type.getName();
        int lambda = name.indexOf("$$Lambda");
        return lambda < 0 ? name : name.substring(0, lambda) + "$$Lambda";
    }
}
