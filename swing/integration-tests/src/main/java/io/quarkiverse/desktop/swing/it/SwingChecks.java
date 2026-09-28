package io.quarkiverse.desktop.swing.it;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.print.DocFlavor;
import javax.print.SimpleDoc;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.LookAndFeel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.basic.BasicFileChooserUI;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.multi.MultiButtonUI;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthStyle;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

/**
 * Checks of Swing features : every look and feel of the JDK with a gallery of the components, text (HTML, RTF,
 * styled), formatters and editors, tables, trees, the file and color choosers, option panes, internal frames, data
 * transfer through bean properties, printing to a PostScript stream, popups, timers and workers, and application
 * classes that Swing creates by name.
 * <p>
 * Prints one {@code RESULT <check> OK|FAILED|SKIPPED} line per check and a {@code SUMMARY} line. It never prints on a
 * printer (only to a PostScript stream in memory), never shows a dialog, does not take the focus, does not use the
 * clipboard, and disposes its windows. It does not depend on Quarkus : {@link #main(String[])} runs it in a plain JVM
 * (for instance with the native image tracing agent).
 */
public final class SwingChecks {

    private static final long TIMEOUT_SECONDS = 20;

    private final Path directory;
    private final String mode;
    private final String startupLookAndFeel;
    private final PrintStream out;
    private final List<String> failures = new ArrayList<>();
    private final List<Throwable> uncaught = Collections.synchronizedList(new ArrayList<>());
    private int ok;

    /**
     * @param directory where the rendered images and the look and feel defaults are written
     * @param mode {@code jvm} or {@code native}
     * @param startupLookAndFeel the class of the look and feel set when the application started
     */
    public SwingChecks(Path directory, String mode, String startupLookAndFeel) {
        this.directory = directory;
        this.mode = mode;
        this.startupLookAndFeel = startupLookAndFeel;
        this.out = System.out;
    }

    /**
     * Runs the checks in a plain JVM : {@code [directory]}.
     */
    public static void main(String[] args) {
        Path directory = Path.of(args.length > 0 ? args[0] : System.getProperty("java.io.tmpdir"));
        int status = new SwingChecks(directory, "jvm", UIManager.getLookAndFeel().getClass().getName()).run();
        System.exit(status);
    }

    /**
     * Runs the checks.
     *
     * @return the exit status : 0 when every check passed, 1 otherwise
     */
    public int run() {
        boolean headless = GraphicsEnvironment.isHeadless();
        // Exceptions of the event dispatch thread, and errors that Swing only prints (a missing painter, UI delegate...)
        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> uncaught.add(error));
        PrintStream previousErr = System.err;
        ErrorWatcher errors = new ErrorWatcher(previousErr);
        System.setErr(new PrintStream(errors, true, StandardCharsets.UTF_8));
        try {
            Files.createDirectories(directory);
            check("environment", () -> environment(headless));
            check("static-initializer", () -> {
                require(SwingPalette.ACCENT.getRGB() == 0xff4695eb, "SwingPalette.ACCENT " + SwingPalette.ACCENT);
                return "accent=" + Integer.toHexString(SwingPalette.ACCENT.getRGB()) + " border="
                        + SwingPalette.FRAME.getClass().getSimpleName();
            });
            for (LookAndFeels.Laf laf : LookAndFeels.all()) {
                if (!isSupported(laf)) {
                    skip("laf-" + laf.id(), "unsupported");
                    skip("gallery-" + laf.id(), "unsupported");
                    continue;
                }
                check("laf-" + laf.id(), () -> lookAndFeel(laf));
                check("gallery-" + laf.id(), () -> gallery(laf, headless));
            }
            onEdt(() -> {
                LookAndFeels.set(new LookAndFeels.Laf("metal", LookAndFeels.METAL));
                return null;
            });
            check("font-metrics", () -> onEdt(LookAndFeels::fontMetrics));
            check("multi", this::multiLookAndFeel);
            check("html", this::html);
            check("html-page", this::htmlPage);
            check("rtf", this::rtf);
            check("editor-kits", this::editorKits);
            check("styled-text-undo", this::styledTextAndUndo);
            check("formatters", this::formatters);
            check("combo-box-editor", this::comboBoxEditor);
            check("table", this::table);
            check("tree", this::tree);
            check("file-chooser", () -> fileChooser(null));
            check("file-chooser-system", () -> fileChooser(UIManager.getSystemLookAndFeelClassName()));
            check("color-chooser", this::colorChooser);
            check("option-pane", this::optionPane);
            check("internal-frames", this::internalFrames);
            check("transfer-handler", this::transferHandler);
            check("print-table", this::printTable);
            check("print-text", this::printText);
            check("timer-worker", this::timerAndWorker);
            check("right-to-left", this::rightToLeft);
            check("override-checks", this::overrideChecks);
            if (headless) {
                skip("popup", "headless");
            } else {
                check("popup", this::popup);
            }
            check("look-and-feel-config", () -> "startup=" + startupLookAndFeel);
            // Let the event dispatch thread finish what the checks queued
            onEdt(() -> null);
            check("errors", () -> {
                require(uncaught.isEmpty(), "uncaught exceptions " + uncaught);
                require(errors.errors.isEmpty(), "errors printed " + errors.errors);
                return "none";
            });
        } catch (Exception e) {
            check("setup", () -> {
                throw e;
            });
        } finally {
            System.setErr(previousErr);
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
            disposeWindows();
        }
        out.println("SUMMARY ok=" + ok + " failed=" + failures.size() + " " + failures);
        return failures.isEmpty() ? 0 : 1;
    }

    // -------------------------------------------------------------------------------------------------- environment

    private String environment(boolean headless) {
        StringBuilder environment = new StringBuilder();
        environment.append("headless=").append(headless);
        environment.append(" mode=").append(mode);
        environment.append(" startup=").append(startupLookAndFeel);
        environment.append(" locale=").append(Locale.getDefault());
        environment.append(" jnuEncoding=").append(System.getProperty("sun.jnu.encoding"));
        environment.append(" fontconfig=").append(System.getProperty("sun.awt.fontconfig") != null);
        environment.append(" system=").append(UIManager.getSystemLookAndFeelClassName());
        environment.append(" crossPlatform=").append(UIManager.getCrossPlatformLookAndFeelClassName());
        environment.append(" installed=").append(Stream.of(UIManager.getInstalledLookAndFeels())
                .map(info -> info.getName().replace(' ', '_')).collect(Collectors.joining(",")));
        environment.append(" lookAndFeels=").append(LookAndFeels.all().stream().map(LookAndFeels.Laf::id)
                .collect(Collectors.joining(",")));
        return environment.toString();
    }

    // ------------------------------------------------------------------------------------------------ look and feels

    /**
     * Whether the look and feel can be set on this platform (GTK needs the GTK libraries).
     */
    private static boolean isSupported(LookAndFeels.Laf laf) throws Exception {
        return onEdt(() -> {
            try {
                LookAndFeels.set(laf);
                return true;
            } catch (javax.swing.UnsupportedLookAndFeelException e) {
                return false;
            }
        });
    }

    /**
     * Sets the look and feel by name, resolves all its defaults (UI delegates, icons, painters, borders, input maps,
     * UI texts...), and writes them.
     */
    private String lookAndFeel(LookAndFeels.Laf laf) throws Exception {
        return onEdt(() -> {
            LookAndFeels.set(laf);
            LookAndFeel current = UIManager.getLookAndFeel();
            Map<String, String> defaults = LookAndFeels.describeDefaults();
            write("defaults-" + laf.id(), defaults.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n")));
            StringBuilder result = new StringBuilder();
            result.append("class=").append(current.getClass().getName());
            result.append(" ").append(LookAndFeels.signature(defaults));
            switch (laf.id()) {
                case "nimbus" -> {
                    List<String> painters = defaults.keySet().stream().filter(k -> k.endsWith("Painter")).toList();
                    List<String> missing = painters.stream().filter(k -> "null".equals(defaults.get(k))).toList();
                    require(painters.size() > 100 && missing.isEmpty(), "painters not created : " + missing);
                    result.append(" painters=").append(painters.size());
                }
                case "synth" -> result.append(" ").append(synthDefaults());
                case "application" -> require(current instanceof QuarkusLookAndFeel, "not the application one");
                default -> {
                }
            }
            return result.toString();
        });
    }

    /**
     * The values decoded from the Synth XML file.
     */
    private static String synthDefaults() {
        Object gridColor = UIManager.get("Table.gridColor");
        require(new Color(200, 210, 220).equals(gridColor), "Table.gridColor " + gridColor);
        require(Color.ORANGE.equals(UIManager.get("Quarkus.focusColor")), "Quarkus.focusColor");
        require("Synth Quarkus".equals(UIManager.get("Quarkus.name")), "Quarkus.name");
        require(Integer.valueOf(22).equals(UIManager.get("Quarkus.rowHeight")), "Quarkus.rowHeight");
        JLabel named = new JLabel("named");
        named.setName("quarkus.label");
        SynthStyle namedStyle = SynthLookAndFeel.getStyle(named, Region.LABEL);
        SynthContext namedContext = new SynthContext(named, Region.LABEL, namedStyle, SynthConstants.ENABLED);
        require(Boolean.TRUE.equals(namedStyle.get(namedContext, "Quarkus.named")), "named style not bound");
        javax.swing.JCheckBox checkBox = new javax.swing.JCheckBox("check");
        SynthStyle checkStyle = SynthLookAndFeel.getStyle(checkBox, Region.CHECK_BOX);
        Icon checkIcon = checkStyle.getIcon(new SynthContext(checkBox, Region.CHECK_BOX, checkStyle,
                SynthConstants.ENABLED), "CheckBox.icon");
        require(checkIcon != null && checkIcon.getIconWidth() == 13, "CheckBox.icon " + checkIcon);
        return "gridColor=" + Integer.toHexString(((Color) gridColor).getRGB()) + " checkIcon="
                + checkIcon.getIconWidth();
    }

    /**
     * Shows a frame with the gallery of components under the look and feel, renders it, and checks the key bindings of
     * the components.
     */
    private String gallery(LookAndFeels.Laf laf, boolean headless) throws Exception {
        GlossyPainter.PAINTED.set(0);
        // A frame shown on the screen, or a panel without display (no windows in headless mode)
        JFrame frame = headless ? null : onEdt(() -> {
            LookAndFeels.set(laf);
            JFrame f = new JFrame("Swing gallery " + laf.id());
            f.setAutoRequestFocus(false);
            f.setFocusableWindowState(false);
            if (UIManager.getLookAndFeel().getSupportsWindowDecorations()) {
                // The title bar and borders of the look and feel (JRootPane UI delegate)
                f.setUndecorated(true);
                f.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            }
            f.setIconImage(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
            f.setJMenuBar(Gallery.menuBar());
            f.setContentPane(Gallery.build(true));
            f.pack();
            f.setLocation(40, 40);
            return f;
        });
        try {
            if (frame != null) {
                show(frame);
            }
            return onEdt(() -> {
                JComponent content;
                if (frame != null) {
                    content = frame.getRootPane();
                } else {
                    LookAndFeels.set(laf);
                    content = new JPanel(new java.awt.BorderLayout());
                    content.add(Gallery.menuBar(), java.awt.BorderLayout.NORTH);
                    content.add(Gallery.build(true), java.awt.BorderLayout.CENTER);
                    content.setSize(content.getPreferredSize());
                    layout(content);
                }
                BufferedImage image = render(content);
                writeImage("gallery-" + laf.id(), image);
                StringBuilder result = new StringBuilder();
                result.append("size=").append(image.getWidth()).append('x').append(image.getHeight());
                int colors = distinctColors(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
                require(colors > 30, "too few colors : " + colors);
                result.append(" colors=").append(colors);
                RoundButton round = find(content, RoundButton.class);
                require(round.getUI() instanceof RoundButtonUI, "RoundButton UI " + round.getUI());
                if (laf.id().equals("synth")) {
                    require(GlossyPainter.PAINTED.get() > 0, "the Synth painter was not used");
                    result.append(" glossyPainted=").append(GlossyPainter.PAINTED.get() > 0);
                }
                result.append(" ").append(LookAndFeels.bindingsSignature());
                return result.toString();
            });
        } finally {
            if (frame != null) {
                dispose(frame);
            }
        }
    }

    private String multiLookAndFeel() throws Exception {
        LookAndFeel auxiliary = new MetalLookAndFeel();
        return onEdt(() -> {
            UIManager.addAuxiliaryLookAndFeel(auxiliary);
            try {
                JButton button = new JButton("Multi");
                require(button.getUI() instanceof MultiButtonUI, "button UI " + button.getUI());
                JPanel panel = Gallery.build(false);
                panel.setSize(panel.getPreferredSize());
                layout(panel);
                BufferedImage image = render(panel);
                return "ui=" + button.getUI().getClass().getSimpleName() + " colors="
                        + distinctColors(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            } finally {
                UIManager.removeAuxiliaryLookAndFeel(auxiliary);
            }
        });
    }

    // --------------------------------------------------------------------------------------------------------- text

    private static final String HTML = """
            <html><head><style>
            body { font-family: sans-serif; font-size: 12pt; }
            h1 { color: #4269e1; border-bottom: 1px solid #0d1c2c; }
            .note { background-color: #fff3c4; padding: 4px; }
            td, th { border: 1px solid gray; padding: 2px; }
            </style></head><body>
            <h1>Quarkus Desktop</h1>
            <p class="note">A <b>bold</b>, <i>italic</i>, <u>underlined</u> and <font color="red">red</font> text,
            with a <a href="https://quarkus.io">link</a> and an image <img src="%s" width="16" height="16">.</p>
            <table><tr><th>Name</th><th>Value</th></tr><tr><td>One</td><td>1</td></tr><tr><td>Two</td><td>2</td></tr></table>
            <ol><li>First</li><li>Second</li></ol>
            <form action="submit"><input type="text" value="field"> <input type="checkbox" checked>
            <select><option>One</option><option>Two</option></select> <input type="submit" value="Send"></form>
            <hr><pre>preformatted   text</pre>
            </body></html>
            """;

    private String html() throws Exception {
        return onEdt(() -> {
            JEditorPane pane = new JEditorPane("text/html", HTML.formatted(Gallery.class.getResource("quarkus.png")));
            pane.setEditable(false);
            require(pane.getEditorKit() instanceof HTMLEditorKit, "kit " + pane.getEditorKit());
            HTMLDocument document = (HTMLDocument) pane.getDocument();
            require(document.getStyleSheet().getRule("h1").getAttributeCount() > 0, "no h1 style rule");
            pane.setSize(500, 400);
            layout(pane);
            BufferedImage image = render(pane);
            StringWriter html = new StringWriter();
            pane.getEditorKit().write(html, document, 0, document.getLength());
            require(html.toString().contains("<table"), "no table written : " + html);
            // The form controls are components inside the editor pane
            int components = count(pane, JComponent.class);
            require(components >= 4, "form components " + components);
            String text = document.getText(0, document.getLength());
            require(text.contains("Quarkus Desktop") && text.contains("preformatted"), "text " + text);
            return "length=" + document.getLength() + " components=" + components + " colors="
                    + distinctColors(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        });
    }

    /**
     * A page of the class path : a jar: URL in JVM mode, a resource: URL in a native executable.
     */
    private String htmlPage() throws Exception {
        URL page = Gallery.class.getResource("page.html");
        require(page != null, "page.html not found");
        CountDownLatch loaded = new CountDownLatch(1);
        JEditorPane pane = onEdt(() -> {
            JEditorPane p = new JEditorPane();
            p.setEditable(false);
            p.addPropertyChangeListener("page", event -> loaded.countDown());
            p.setPage(page);
            return p;
        });
        require(loaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "page not loaded");
        return onEdt(() -> {
            Document document = pane.getDocument();
            String text = document.getText(0, document.getLength());
            require(text.contains("Loaded from the class path"), "page text " + text);
            return "contentType=" + pane.getContentType() + " length=" + document.getLength() + " protocol="
                    + page.getProtocol();
        });
    }

    private String rtf() throws Exception {
        return onEdt(() -> {
            StringBuilder result = new StringBuilder();
            // Charsets of the RTF reader : resources of the JDK
            Map<String, String> documents = new LinkedHashMap<>();
            documents.put("ansi", "{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Serif;}}{\\colortbl;\\red66\\green105\\blue225;}"
                    + "\\f0\\fs24 Caf\\'e9 {\\b bold} {\\i italic} {\\cf1 blue}\\par}");
            documents.put("mac", "{\\rtf1\\mac Caf\\'8e\\par}");
            documents.put("pc", "{\\rtf1\\pc Caf\\'82\\par}");
            documents.put("pca", "{\\rtf1\\pca Caf\\'82\\par}");
            for (Map.Entry<String, String> rtf : documents.entrySet()) {
                RTFEditorKit kit = new RTFEditorKit();
                Document document = kit.createDefaultDocument();
                kit.read(new StringReader(rtf.getValue()), document, 0);
                String text = document.getText(0, document.getLength());
                require(text.startsWith("Café"), rtf.getKey() + " : " + text);
                result.append(rtf.getKey()).append("=ok ");
            }
            JEditorPane pane = new JEditorPane("text/rtf", documents.get("ansi"));
            require(pane.getEditorKit() instanceof RTFEditorKit, "kit " + pane.getEditorKit());
            ByteArrayOutputStream written = new ByteArrayOutputStream();
            pane.getEditorKit().write(written, pane.getDocument(), 0, pane.getDocument().getLength());
            String rtf = written.toString(StandardCharsets.ISO_8859_1);
            require(rtf.startsWith("{\\rtf1"), "written " + rtf);
            pane.setSize(300, 100);
            layout(pane);
            render(pane);
            return result.append("written=").append(rtf.length()).toString();
        });
    }

    private String editorKits() throws Exception {
        return onEdt(() -> {
            // Editor kits by content type : the JDK ones and an application one, created with reflection
            JEditorPane.registerEditorKitForContentType(QuarkusEditorKit.CONTENT_TYPE, QuarkusEditorKit.class.getName());
            StringBuilder result = new StringBuilder();
            for (String type : List.of("text/plain", "text/html", "text/rtf", "application/rtf",
                    QuarkusEditorKit.CONTENT_TYPE)) {
                JEditorPane pane = new JEditorPane();
                pane.setContentType(type);
                // application/rtf is an alias of text/rtf
                require(pane.getContentType().equals(type.equals("application/rtf") ? "text/rtf" : type),
                        type + " : " + pane.getContentType());
                result.append(type).append('=').append(pane.getEditorKit().getClass().getSimpleName()).append(' ');
            }
            require(JEditorPane.createEditorKitForContentType(QuarkusEditorKit.CONTENT_TYPE) instanceof QuarkusEditorKit,
                    "application editor kit not created");
            return result.toString().trim();
        });
    }

    private String styledTextAndUndo() throws Exception {
        return onEdt(() -> {
            javax.swing.JTextPane pane = Gallery.textPane(Gallery.icon());
            pane.setSize(300, 80);
            layout(pane);
            render(pane);
            Document document = new PlainDocument();
            UndoManager undo = new UndoManager();
            document.addUndoableEditListener(undo);
            document.insertString(0, "Hello", null);
            document.insertString(5, " world", null);
            undo.undo();
            require("Hello".equals(document.getText(0, document.getLength())), "undo");
            undo.redo();
            require("Hello world".equals(document.getText(0, document.getLength())), "redo");
            return "undo=" + undo.getUndoPresentationName().replace(' ', '_') + " elements="
                    + pane.getStyledDocument().getDefaultRootElement().getElementCount();
        });
    }

    /**
     * The formatters create values from text with the {@code String} constructor of their value class.
     */
    private String formatters() throws Exception {
        return onEdt(() -> {
            StringBuilder result = new StringBuilder();
            Map<Class<?>, Object> values = new LinkedHashMap<>();
            values.put(Integer.class, 42);
            values.put(Long.class, 42L);
            values.put(Short.class, (short) 42);
            values.put(Byte.class, (byte) 42);
            values.put(Float.class, 42f);
            values.put(Double.class, 42d);
            values.put(BigDecimal.class, new BigDecimal("42"));
            values.put(BigInteger.class, new BigInteger("42"));
            for (Map.Entry<Class<?>, Object> value : values.entrySet()) {
                NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance(Locale.ROOT));
                formatter.setValueClass(value.getKey());
                JFormattedTextField field = new JFormattedTextField(formatter);
                field.setText("42");
                field.commitEdit();
                require(value.getValue().equals(field.getValue()), value.getKey().getSimpleName() + " : "
                        + field.getValue() + " (" + (field.getValue() == null ? null : field.getValue().getClass()) + ")");
                result.append(value.getKey().getSimpleName()).append(' ');
            }
            // DefaultFormatter : any class with a String constructor
            Map<Class<?>, String> texts = new LinkedHashMap<>();
            texts.put(String.class, "text");
            texts.put(URL.class, "http://quarkus.io");
            for (Map.Entry<Class<?>, String> text : texts.entrySet()) {
                DefaultFormatter formatter = new DefaultFormatter();
                formatter.setValueClass(text.getKey());
                Object value = formatter.stringToValue(text.getValue());
                require(text.getKey().isInstance(value) && value.toString().equals(text.getValue()),
                        text.getKey().getSimpleName() + " : " + value);
                result.append(text.getKey().getSimpleName()).append(' ');
            }
            MaskFormatter mask = new MaskFormatter("##-UU");
            require("12-AB".equals(mask.stringToValue("12-AB")), "mask");
            JFormattedTextField date = new JFormattedTextField(new Date(0));
            require(date.getValue() instanceof Date, "date");
            return result.append("mask date").toString();
        });
    }

    /**
     * An editable combo box keeps the class of its items : its editor creates them with {@code valueOf(String)}.
     */
    private String comboBoxEditor() throws Exception {
        return onEdt(() -> {
            JComboBox<Integer> combo = new JComboBox<>(new Integer[] { 10, 20, 30 });
            combo.setEditable(true);
            combo.setSelectedIndex(1);
            combo.getEditor().setItem(20);
            ((JTextField) combo.getEditor().getEditorComponent()).setText("42");
            Object item = combo.getEditor().getItem();
            require(Integer.valueOf(42).equals(item), "item " + item + " (" + (item == null ? null : item.getClass())
                    + ")");
            return "item=" + item.getClass().getSimpleName();
        });
    }

    // ----------------------------------------------------------------------------------------------------- tables

    private static final class ValuesModel extends AbstractTableModel {

        final Object[][] rows = {
                { "One", 1, 1L, 1.5f, 1.5d, new BigDecimal("1.5"), BigInteger.ONE, Gallery.DATE, true, null },
                { "Two", 2, 2L, 2.5f, 2.5d, new BigDecimal("2.5"), BigInteger.TWO, Gallery.DATE, false, null } };
        final Class<?>[] types = { Object.class, Integer.class, Long.class, Float.class, Double.class, BigDecimal.class,
                BigInteger.class, Date.class, Boolean.class, Icon.class };

        @Override
        public int getRowCount() {
            return rows.length;
        }

        @Override
        public int getColumnCount() {
            return types.length;
        }

        @Override
        public String getColumnName(int column) {
            return types[column].getSimpleName();
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return types[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            return rows[row][column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column < 7;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            rows[row][column] = value;
        }
    }

    /**
     * Edits the cells of a table : the generic and number editors create the values with the {@code String}
     * constructor of the column class.
     */
    private String table() throws Exception {
        return onEdt(() -> {
            ValuesModel model = new ValuesModel();
            model.rows[0][9] = Gallery.icon();
            model.rows[1][9] = Gallery.icon();
            JTable table = new JTable(model);
            table.setAutoCreateRowSorter(true);
            String[] texts = { "Edited", "42", "42", "4.25", "4.25", "4.25", "42" };
            Object[] expected = { "Edited", 42, 42L, 4.25f, 4.25d, new BigDecimal("4.25"), new BigInteger("42") };
            for (int column = 0; column < texts.length; column++) {
                require(table.editCellAt(0, column), "cell " + column + " not editable");
                JTextField editor = (JTextField) table.getEditorComponent();
                editor.setText(texts[column]);
                require(table.getCellEditor().stopCellEditing(), "column " + column + " : edit not stopped");
                Object value = model.getValueAt(0, column);
                require(expected[column].equals(value), "column " + column + " : " + value + " ("
                        + (value == null ? null : value.getClass()) + ")");
            }
            table.getRowSorter().toggleSortOrder(1);
            require(table.convertRowIndexToModel(0) == 1, "not sorted");
            JPanel panel = new JPanel(new java.awt.BorderLayout());
            panel.add(table.getTableHeader(), java.awt.BorderLayout.NORTH);
            panel.add(table, java.awt.BorderLayout.CENTER);
            panel.setSize(900, 120);
            layout(panel);
            BufferedImage image = render(panel);
            return "columns=" + model.getColumnCount() + " colors="
                    + distinctColors(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        });
    }

    private String tree() throws Exception {
        return onEdt(() -> {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
            DefaultMutableTreeNode leaf = new DefaultMutableTreeNode("Leaf");
            root.add(leaf);
            DefaultTreeModel model = new DefaultTreeModel(root);
            JTree tree = new JTree(model);
            tree.setEditable(true);
            tree.setSize(200, 100);
            layout(tree);
            TreePath path = new TreePath(new Object[] { root, leaf });
            tree.startEditingAtPath(path);
            require(tree.isEditing(), "not editing");
            JTextField editor = find(tree, JTextField.class);
            require(editor != null, "no editor");
            editor.setText("Renamed");
            require(tree.stopEditing(), "edit not stopped");
            require("Renamed".equals(leaf.getUserObject()), "leaf " + leaf.getUserObject());
            render(tree);
            return "rows=" + tree.getRowCount() + " leaf=" + leaf.getUserObject();
        });
    }

    // --------------------------------------------------------------------------------------------------- choosers

    /**
     * A file chooser in a temporary directory (never shown) : its directory model, file filter, file system view and
     * rendering.
     */
    private String fileChooser(String lookAndFeel) throws Exception {
        Path folder = Files.createTempDirectory("swing-it-chooser");
        Files.writeString(folder.resolve("notes.txt"), "notes");
        Files.writeString(folder.resolve("image.png"), "png");
        Files.createDirectories(folder.resolve("folder"));
        String previous = UIManager.getLookAndFeel().getClass().getName();
        try {
            JFileChooser chooser = onEdt(() -> {
                if (lookAndFeel != null) {
                    UIManager.setLookAndFeel(lookAndFeel);
                }
                JFileChooser c = new JFileChooser(folder.toFile());
                c.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
                c.setMultiSelectionEnabled(true);
                return c;
            });
            // The files are listed by a background thread
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
            int files = 0;
            while (System.nanoTime() < deadline) {
                files = onEdt(() -> chooser.getUI() instanceof BasicFileChooserUI ui ? ui.getModel().getSize() : 2);
                if (files == 2) {
                    break;
                }
                Thread.sleep(50);
            }
            require(files == 2, "files listed " + files);
            return onEdt(() -> {
                FileSystemView view = chooser.getFileSystemView();
                java.io.File notes = folder.resolve("notes.txt").toFile();
                Icon icon = view.getSystemIcon(notes);
                String name = view.getSystemDisplayName(notes);
                String type = view.getSystemTypeDescription(notes);
                java.io.File[] roots = view.getRoots();
                java.io.File home = view.getHomeDirectory();
                require(roots.length > 0 && home != null, "roots " + Arrays.toString(roots) + " home " + home);
                require(icon != null && icon.getIconWidth() > 0, "system icon " + icon);
                chooser.setSize(640, 420);
                layout(chooser);
                BufferedImage image = render(chooser);
                return "ui=" + chooser.getUI().getClass().getSimpleName() + " files=2 icon=" + icon.getIconWidth()
                        + " name=" + name.replace(' ', '_') + " type=" + String.valueOf(type).replace(' ', '_')
                        + " roots=" + roots.length + " colors="
                        + distinctColors(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            });
        } finally {
            onEdt(() -> {
                UIManager.setLookAndFeel(previous);
                return null;
            });
            try (Stream<Path> paths = Files.walk(folder)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private String colorChooser() throws Exception {
        return onEdt(() -> {
            JColorChooser chooser = new JColorChooser(new Color(0x4269e1));
            AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
            require(panels.length == 5, "panels " + panels.length);
            String names = Stream.of(panels).map(AbstractColorChooserPanel::getDisplayName)
                    .collect(Collectors.joining(","));
            chooser.setSize(chooser.getPreferredSize());
            layout(chooser);
            BufferedImage image = render(chooser);
            int colors = distinctColors(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            require(colors > 50, "colors " + colors);
            if (!GraphicsEnvironment.isHeadless()) {
                // Created (with its native window), never shown
                JDialog dialog = JColorChooser.createDialog(null, "Color", true, chooser, null, null);
                dialog.dispose();
            }
            return "panels=" + names.replace(' ', '_') + " colors=" + colors;
        });
    }

    private String optionPane() throws Exception {
        return onEdt(() -> {
            StringBuilder result = new StringBuilder();
            for (int type : new int[] { JOptionPane.ERROR_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
                    JOptionPane.WARNING_MESSAGE, JOptionPane.QUESTION_MESSAGE }) {
                JOptionPane pane = new JOptionPane("Message", type, JOptionPane.YES_NO_CANCEL_OPTION);
                require(pane.getIcon() == null, "no default icon expected");
                pane.setSize(pane.getPreferredSize());
                layout(pane);
                render(pane);
                JLabel iconLabel = findIconLabel(pane);
                require(iconLabel != null && iconLabel.getIcon().getIconWidth() > 0, "no icon for type " + type);
                result.append(type).append('=').append(iconLabel.getIcon().getIconWidth()).append(' ');
            }
            JOptionPane input = new JOptionPane("Choose", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            input.setWantsInput(true);
            input.setSelectionValues(new Object[] { "Alpha", "Beta", "Gamma" });
            input.setInitialSelectionValue("Beta");
            input.setSize(input.getPreferredSize());
            layout(input);
            render(input);
            require(find(input, JComboBox.class) != null, "no combo box in the input pane");
            if (!GraphicsEnvironment.isHeadless()) {
                // Created (with its native window), never shown
                JDialog dialog = input.createDialog(null, "Input");
                dialog.dispose();
            }
            return result.append("input=combo").toString();
        });
    }

    private static JLabel findIconLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getIcon() != null) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel label = findIconLabel(child);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    private String internalFrames() throws Exception {
        return onEdt(() -> {
            JDesktopPane desktop = new JDesktopPane();
            desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
            JInternalFrame frame = new JInternalFrame("Frame", true, true, true, true);
            frame.add(new JLabel("Content"));
            frame.setBounds(10, 10, 200, 120);
            frame.setVisible(true);
            desktop.add(frame);
            JInternalFrame palette = new JInternalFrame("Palette", false, true);
            palette.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
            palette.setBounds(220, 10, 120, 80);
            palette.setVisible(true);
            desktop.add(palette);
            desktop.setSize(400, 300);
            layout(desktop);
            frame.setMaximum(true);
            frame.setMaximum(false);
            frame.setIcon(true);
            require(frame.getDesktopIcon().getParent() == desktop, "not iconified");
            frame.setIcon(false);
            render(desktop);
            return "frames=" + desktop.getAllFrames().length + " icon="
                    + frame.getDesktopIcon().getUI().getClass().getSimpleName();
        });
    }

    // ------------------------------------------------------------------------------------------------ data transfer

    /**
     * Transfers a bean property between components ({@code new TransferHandler("text")}), without the clipboard.
     */
    private String transferHandler() throws Exception {
        return onEdt(() -> {
            JLabel source = new JLabel("Transferred");
            JTextField target = new JTextField();
            PropertyTransfer handler = new PropertyTransfer("text");
            require((handler.getSourceActions(source) & TransferHandler.COPY) != 0, "no source action");
            Transferable transferable = handler.export(source);
            require(transferable != null, "nothing exported");
            DataFlavor flavor = transferable.getTransferDataFlavors()[0];
            Object data = transferable.getTransferData(flavor);
            require("Transferred".equals(data), "data " + data);
            require(handler.canImport(target, new DataFlavor[] { flavor }), "cannot import");
            require(handler.importData(target, transferable), "not imported");
            require("Transferred".equals(target.getText()), "target " + target.getText());
            JColorChooser chooser = new JColorChooser(Color.RED);
            PropertyTransfer color = new PropertyTransfer("color");
            JColorChooser other = new JColorChooser(Color.BLUE);
            require(color.importData(other, color.export(chooser)) && Color.RED.equals(other.getColor()), "color");
            // Object properties of components with enumerated properties (their constants are introspected)
            PropertyTransfer value = new PropertyTransfer("value");
            JFormattedTextField formatted = new JFormattedTextField(42);
            JFormattedTextField formattedTarget = new JFormattedTextField(7);
            require(value.importData(formattedTarget, value.export(formatted))
                    && Integer.valueOf(42).equals(formattedTarget.getValue()), "formatted value");
            javax.swing.JSpinner spinner = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(3, 0, 9, 1));
            javax.swing.JSpinner spinnerTarget = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 9, 1));
            require(value.importData(spinnerTarget, value.export(spinner))
                    && Integer.valueOf(3).equals(spinnerTarget.getValue()),
                    "spinner value");
            PropertyTransfer item = new PropertyTransfer("selectedItem");
            JComboBox<String> combo = new JComboBox<>(new String[] { "One", "Two" });
            combo.setSelectedItem("Two");
            JComboBox<String> comboTarget = new JComboBox<>(new String[] { "One", "Two" });
            require(item.importData(comboTarget, item.export(combo)) && "Two".equals(comboTarget.getSelectedItem()),
                    "combo selected item");
            return "flavor=" + flavor.getMimeType().replace(' ', '_') + " properties=text,color,value,selectedItem";
        });
    }

    private static final class PropertyTransfer extends TransferHandler {

        PropertyTransfer(String property) {
            super(property);
        }

        Transferable export(JComponent component) {
            return createTransferable(component);
        }
    }

    // --------------------------------------------------------------------------------------------------- printing

    private String printTable() throws Exception {
        return onEdt(() -> {
            JTable table = new JTable(new ValuesModel());
            table.setSize(700, 60);
            layout(table);
            Printable printable = table.getPrintable(JTable.PrintMode.FIT_WIDTH, new MessageFormat("Table"),
                    new MessageFormat("Page {0}"));
            return printToPostScript("table", printable);
        });
    }

    private String printText() throws Exception {
        return onEdt(() -> {
            JTextArea area = new JTextArea("Printed by Quarkus Desktop Swing\n".repeat(80));
            area.setSize(400, 1200);
            layout(area);
            String text = printToPostScript("text", area.getPrintable(new MessageFormat("Text"),
                    new MessageFormat("Page {0}")));
            JEditorPane html = new JEditorPane("text/html", "<html><body><h1>Printed</h1><p>HTML</p></body></html>");
            html.setSize(400, 200);
            layout(html);
            return text + " html:" + printToPostScript("html", html.getPrintable(null, null));
        });
    }

    /**
     * Prints to a PostScript stream in memory (never to a printer).
     */
    private String printToPostScript(String name, Printable printable) throws Exception {
        DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
        StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor,
                "application/postscript");
        require(factories.length > 0, "no PostScript stream print service");
        ByteArrayOutputStream postScript = new ByteArrayOutputStream();
        StreamPrintService service = factories[0].getPrintService(postScript);
        HashPrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(MediaSizeName.ISO_A4);
        service.createPrintJob().print(new SimpleDoc(printable, flavor, null), attributes);
        String header = new String(postScript.toByteArray(), 0, Math.min(postScript.size(), 10),
                StandardCharsets.ISO_8859_1);
        require(header.startsWith("%!PS-Adobe"), "not PostScript : " + header);
        Files.write(directory.resolve("print-" + name + "-" + mode + ".ps"), postScript.toByteArray());
        String content = postScript.toString(StandardCharsets.ISO_8859_1);
        int pages = content.split("%%Page:", -1).length - 1;
        require(pages > 0, "no page printed");
        return "pages=" + pages + " bytes=" + postScript.size();
    }

    // ---------------------------------------------------------------------------------------------- other features

    private String timerAndWorker() throws Exception {
        CountDownLatch fired = new CountDownLatch(1);
        Timer timer = new Timer(10, event -> fired.countDown());
        timer.setRepeats(false);
        timer.start();
        require(fired.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "timer not fired");
        AtomicInteger processed = new AtomicInteger();
        AtomicReference<String> doneThread = new AtomicReference<>();
        SwingWorker<Integer, Integer> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                publish(1, 2, 3);
                setProgress(100);
                return 6;
            }

            @Override
            protected void process(List<Integer> chunks) {
                processed.addAndGet(chunks.size());
            }

            @Override
            protected void done() {
                doneThread.set(SwingUtilities.isEventDispatchThread() ? "edt" : Thread.currentThread().getName());
            }
        };
        worker.execute();
        require(worker.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) == 6, "worker result");
        // process and done run later on the event dispatch thread (through a Swing timer)
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while ((doneThread.get() == null || processed.get() < 3) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        require(processed.get() == 3 && "edt".equals(doneThread.get()),
                "processed " + processed.get() + " done " + doneThread.get());
        return "worker=" + worker.getProgress() + " processed=" + processed.get() + " done=" + doneThread.get();
    }

    private String rightToLeft() throws Exception {
        return onEdt(() -> {
            JPanel gallery = Gallery.build(true);
            gallery.applyComponentOrientation(java.awt.ComponentOrientation.getOrientation(Locale.forLanguageTag("ar")));
            gallery.setSize(gallery.getPreferredSize());
            layout(gallery);
            BufferedImage image = render(gallery);
            require(!gallery.getComponentOrientation().isLeftToRight(), "not right to left");
            return "colors=" + distinctColors(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        });
    }

    // ------------------------------------------------------------------------------------------- override checks

    /**
     * Swing checks with reflection which methods of the application classes override its own : a plain text view that
     * only overrides the {@code int} variant of {@code drawUnselectedText} (deprecated) is painted with it, and a text
     * field that overrides {@code processInputMethodEvent} gets the committed text of an input method without the
     * {@code KEY_TYPED} events that Swing synthesizes for the other text fields.
     */
    private String overrideChecks() throws Exception {
        return onEdt(() -> {
            IntVariantView.painted.set(0);
            JTextArea area = new JTextArea("override");
            area.setUI(new BasicTextAreaUI() {
                @Override
                public View create(Element element) {
                    return new IntVariantView(element);
                }
            });
            area.setSize(200, 40);
            render(area);
            require(IntVariantView.painted.get() > 0, "the int variant of drawUnselectedText was not used");
            String plain = committedText(new PlainInputField());
            String overriding = committedText(new InputMethodField());
            require(plain.equals("text=ab keyTyped=2"), "PlainInputField " + plain);
            require(overriding.equals("text=ab keyTyped=0"), "InputMethodField " + overriding);
            return "view=int field=" + plain + " inputMethodField=" + overriding;
        });
    }

    /**
     * Commits "ab" with an input method event.
     */
    private static String committedText(JTextField field) {
        AtomicInteger typed = new AtomicInteger();
        field.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                typed.incrementAndGet();
            }
        });
        field.dispatchEvent(new java.awt.event.InputMethodEvent(field,
                java.awt.event.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                new java.text.AttributedString("ab").getIterator(), 2, null, null));
        return "text=" + field.getText() + " keyTyped=" + typed.get();
    }

    /**
     * Overrides the {@code int} variant only : {@code PlainView} finds it with reflection and calls it.
     */
    static final class IntVariantView extends PlainView {

        static final AtomicInteger painted = new AtomicInteger();

        IntVariantView(Element element) {
            super(element);
        }

        @Override
        @SuppressWarnings("deprecation")
        protected int drawUnselectedText(java.awt.Graphics g, int x, int y, int p0, int p1)
                throws javax.swing.text.BadLocationException {
            painted.incrementAndGet();
            return super.drawUnselectedText(g, x, y, p0, p1);
        }
    }

    /**
     * A text field that does not handle the input method events itself.
     */
    static final class PlainInputField extends JTextField {
    }

    /**
     * A text field that handles the input method events itself.
     */
    static final class InputMethodField extends JTextField {
        @Override
        protected void processInputMethodEvent(java.awt.event.InputMethodEvent e) {
            super.processInputMethodEvent(e);
        }
    }

    /**
     * A heavyweight popup (a window) with a tool tip, and a popup menu.
     */
    private String popup() throws Exception {
        JFrame owner = onEdt(() -> {
            JFrame f = new JFrame("Popup owner");
            f.setAutoRequestFocus(false);
            f.setFocusableWindowState(false);
            f.setContentPane(new JPanel());
            f.setSize(200, 100);
            f.setLocation(40, 40);
            return f;
        });
        try {
            show(owner);
            JToolTip tip = onEdt(() -> {
                JToolTip t = new JToolTip();
                t.setTipText("Heavyweight popup");
                Point location = owner.getLocationOnScreen();
                // Outside of the owner window : a heavyweight popup
                Popup popup = PopupFactory.getSharedInstance().getPopup(owner.getContentPane(), t,
                        location.x + owner.getWidth() + 20, location.y);
                popup.show();
                t.putClientProperty(Popup.class, popup);
                return t;
            });
            Toolkit.getDefaultToolkit().sync();
            return onEdt(() -> {
                Window window = SwingUtilities.getWindowAncestor(tip);
                ((Popup) tip.getClientProperty(Popup.class)).hide();
                require(window instanceof JWindow, "not a heavyweight popup : " + window);
                JPopupMenu menu = new JPopupMenu();
                menu.add("Cut");
                menu.add("Copy");
                menu.addSeparator();
                menu.add("Paste");
                menu.setSize(menu.getPreferredSize());
                layout(menu);
                BufferedImage image = render(menu);
                return "window=" + window.getClass().getName() + " menu=" + image.getWidth() + "x" + image.getHeight();
            });
        } finally {
            dispose(owner);
        }
    }

    // ------------------------------------------------------------------------------------------------------ support

    private void check(String name, Callable<Object> check) {
        try {
            Object value = check.call();
            out.println("RESULT " + name + " OK " + value);
            ok++;
        } catch (Throwable t) {
            out.println("RESULT " + name + " FAILED " + t);
            t.printStackTrace(out);
            failures.add(name);
        }
    }

    private void skip(String name, String reason) {
        out.println("RESULT " + name + " SKIPPED " + reason);
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Runs on the event dispatch thread and returns the result, or throws what it threw.
     */
    static <T> T onEdt(Callable<T> task) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return task.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            try {
                result.set(task.call());
            } catch (Exception e) {
                error.set(e);
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }

    private static void show(Window window) throws Exception {
        onEdt(() -> {
            window.setVisible(true);
            return null;
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!window.isShowing() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        require(window.isShowing(), "the window is not showing");
        Toolkit.getDefaultToolkit().sync();
        Thread.sleep(200);
        // Let the event dispatch thread paint the window
        onEdt(() -> null);
    }

    private static void dispose(Window window) throws Exception {
        onEdt(() -> {
            window.dispose();
            return null;
        });
    }

    private static void disposeWindows() {
        try {
            onEdt(() -> {
                for (Window window : Window.getWindows()) {
                    window.dispose();
                }
                return null;
            });
        } catch (Exception e) {
            // nothing to dispose
        }
    }

    static void layout(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layout(child);
            }
        }
    }

    /**
     * Renders a component (with its children) into an image.
     */
    static BufferedImage render(JComponent component) {
        Dimension size = component.getSize();
        BufferedImage image = new BufferedImage(Math.max(1, size.width), Math.max(1, size.height),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        component.printAll(g);
        g.dispose();
        return image;
    }

    static int distinctColors(BufferedImage image, Rectangle area) {
        Set<Integer> colors = new HashSet<>();
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                colors.add(image.getRGB(x, y));
            }
        }
        return colors.size();
    }

    static <T> T find(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = find(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static int count(Container container, Class<?> type) {
        int count = 0;
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                count++;
            }
            if (component instanceof Container child) {
                count += count(child, type);
            }
        }
        return count;
    }

    private void writeImage(String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", directory.resolve(name + "-" + mode + ".png").toFile());
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(directory.resolve(name + "-" + mode + ".txt"), content);
    }

    /**
     * Collects the lines printed to the standard error that report an exception or an error, and forwards everything.
     */
    private static final class ErrorWatcher extends OutputStream {

        private final PrintStream forward;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();
        final List<String> errors = Collections.synchronizedList(new ArrayList<>());

        ErrorWatcher(PrintStream forward) {
            this.forward = forward;
        }

        @Override
        public synchronized void write(int b) {
            forward.write(b);
            if (b == '\n') {
                String text = line.toString(StandardCharsets.UTF_8);
                if (text.contains("Exception") || text.contains("Error") || text.contains("failed")) {
                    errors.add(text.trim());
                }
                line.reset();
            } else {
                line.write(b);
            }
        }

        @Override
        public void flush() {
            forward.flush();
        }
    }
}
