package io.quarkiverse.desktop.swing.it;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
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
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.LayerUI;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A gallery of the Swing components, built under the current look and feel (on the event dispatch thread).
 */
public final class Gallery {

    /**
     * A fixed date : the gallery looks the same whenever it is built.
     */
    static final Date DATE = date(2026, Calendar.SEPTEMBER, 26);

    private Gallery() {
    }

    static Date date(int year, int month, int day) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        calendar.clear();
        calendar.set(year, month, day, 12, 0);
        return calendar.getTime();
    }

    static Icon icon() {
        return new ImageIcon(Gallery.class.getResource("quarkus.png"));
    }

    /**
     * The menu bar of the gallery frame.
     */
    public static JMenuBar menuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        JMenuItem open = new JMenuItem("Open...", icon());
        open.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        file.add(open);
        file.addSeparator();
        file.add(new JCheckBoxMenuItem("Check", true));
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem one = new JRadioButtonMenuItem("One", true);
        JRadioButtonMenuItem two = new JRadioButtonMenuItem("Two");
        group.add(one);
        group.add(two);
        file.add(one);
        file.add(two);
        JMenu recent = new JMenu("Recent");
        recent.add(new JMenuItem("gallery.txt"));
        file.add(recent);
        menuBar.add(file);
        menuBar.add(new JMenu("Edit"));
        menuBar.add(new JMenu("Help"));
        return menuBar;
    }

    /**
     * The components of the gallery, each in a titled cell.
     *
     * @param applicationComponents whether to include the components with an application UI delegate
     */
    public static JPanel build(boolean applicationComponents) {
        JPanel gallery = new JPanel(new GridLayout(0, 5, 6, 6));
        gallery.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        Icon icon = icon();

        JButton button = new JButton("<html><b>Button</b> <i>html</i></html>", icon);
        button.setMnemonic('B');
        button.setToolTipText("A tool tip");
        cell(gallery, "JButton", button);
        if (applicationComponents) {
            cell(gallery, "RoundButton", new RoundButton("Round"));
        }
        cell(gallery, "JToggleButton", new JToggleButton("Toggle", true));

        JPanel choices = new JPanel(new GridLayout(2, 1));
        choices.add(new JCheckBox("Check", true));
        JRadioButton radio = new JRadioButton("Radio", true);
        new ButtonGroup().add(radio);
        choices.add(radio);
        cell(gallery, "JCheckBox JRadioButton", choices);

        JPanel combos = new JPanel(new GridLayout(2, 1));
        combos.add(new JComboBox<>(new String[] { "Combo", "Two", "Three" }));
        JComboBox<Integer> editable = new JComboBox<>(new Integer[] { 10, 20, 30 });
        editable.setEditable(true);
        combos.add(editable);
        cell(gallery, "JComboBox", combos);

        JList<String> list = new JList<>(new String[] { "Alpha", "Beta", "Gamma", "Delta" });
        list.setSelectedIndex(1);
        list.setVisibleRowCount(3);
        cell(gallery, "JList", new JScrollPane(list));

        if (sliderSupported()) {
            JSlider slider = new JSlider(0, 100, 40);
            slider.setMajorTickSpacing(25);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            cell(gallery, "JSlider", slider);
        }

        JPanel progress = new JPanel(new GridLayout(2, 1, 2, 2));
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(60);
        bar.setStringPainted(true);
        progress.add(bar);
        JProgressBar vertical = new JProgressBar(0, 100);
        vertical.setValue(30);
        progress.add(vertical);
        cell(gallery, "JProgressBar", progress);

        JPanel spinners = new JPanel(new GridLayout(3, 1));
        spinners.add(new JSpinner(new SpinnerNumberModel(5, 0, 10, 1)));
        JSpinner date = new JSpinner(new SpinnerDateModel(DATE, null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(date, "yyyy-MM-dd");
        dateEditor.getFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
        date.setEditor(dateEditor);
        spinners.add(date);
        spinners.add(new JSpinner(new SpinnerListModel(List.of("North", "South", "East", "West"))));
        cell(gallery, "JSpinner", spinners);

        JPanel texts = new JPanel(new GridLayout(3, 1));
        texts.add(new JTextField("Text مرحبا", 10));
        texts.add(new JPasswordField("secret", 8));
        JFormattedTextField formatted = new JFormattedTextField(NumberFormat.getNumberInstance(Locale.ROOT));
        formatted.setValue(12345.678);
        texts.add(formatted);
        cell(gallery, "JTextField", texts);

        JTextArea area = new JTextArea("Line one\nLine two\nLine three", 3, 12);
        area.setLineWrap(true);
        cell(gallery, "JTextArea", new JScrollPane(area));

        cell(gallery, "JTextPane", new JScrollPane(textPane(icon)));

        JEditorPane html = new JEditorPane("text/html",
                "<html><body><h3>HTML</h3><p style='color:#4269e1'>Styled <b>text</b></p>"
                        + "<ul><li>One</li><li>Two</li></ul></body></html>");
        html.setEditable(false);
        cell(gallery, "JEditorPane", new JScrollPane(html));

        JLabel label = new JLabel("Label", icon, SwingConstants.LEFT);
        label.setFont(SwingPalette.TITLE);
        JPanel labels = new JPanel(new BorderLayout());
        labels.add(label, BorderLayout.NORTH);
        labels.add(new JSeparator(), BorderLayout.CENTER);
        labels.add(new JScrollBar(JScrollBar.HORIZONTAL, 30, 20, 0, 100), BorderLayout.SOUTH);
        cell(gallery, "JLabel JSeparator JScrollBar", labels);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JLabel("Left"), new JLabel("Right"));
        split.setDividerLocation(60);
        split.setOneTouchExpandable(true);
        cell(gallery, "JSplitPane", split);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("First", icon, new JLabel("First tab"));
        tabs.addTab("Second", new JLabel("Second tab"));
        tabs.addTab("Third", new JLabel("Third tab"));
        cell(gallery, "JTabbedPane", tabs);

        JToolBar toolBar = new JToolBar();
        toolBar.add(new AbstractAction("Cut") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
            }
        });
        toolBar.addSeparator();
        toolBar.add(new JToggleButton(icon));
        toolBar.add(new JButton("Copy"));
        cell(gallery, "JToolBar", toolBar);

        JTable table = new JTable(new Object[][] { { "One", 1, true }, { "Two", 2, false }, { "Three", 3, true } },
                new Object[] { "Name", "Value", "Flag" }) {
            @Override
            public Class<?> getColumnClass(int column) {
                return getValueAt(0, column).getClass();
            }
        };
        table.setAutoCreateRowSorter(true);
        table.setPreferredScrollableViewportSize(new Dimension(150, 60));
        cell(gallery, "JTable", new JScrollPane(table));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        DefaultMutableTreeNode branch = new DefaultMutableTreeNode("Branch");
        branch.add(new DefaultMutableTreeNode("Leaf"));
        root.add(branch);
        root.add(new DefaultMutableTreeNode("Other"));
        JTree tree = new JTree(root);
        tree.expandRow(1);
        tree.setVisibleRowCount(4);
        cell(gallery, "JTree", new JScrollPane(tree));

        JDesktopPane desktop = new JDesktopPane();
        desktop.setPreferredSize(new Dimension(160, 90));
        JInternalFrame internal = new JInternalFrame("Internal", true, true, true, true);
        internal.add(new JLabel("Inside"));
        internal.setBounds(4, 4, 120, 70);
        internal.setVisible(true);
        desktop.add(internal);
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        cell(gallery, "JDesktopPane JInternalFrame", desktop);

        JLabel layered = new JLabel("JLayer", SwingConstants.CENTER);
        cell(gallery, "JLayer", new JLayer<>(layered, new LayerUI<>() {
            @Override
            public void paint(Graphics g, JComponent c) {
                super.paint(g, c);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g2.setColor(SwingPalette.ACCENT);
                g2.fillRect(0, 0, c.getWidth(), c.getHeight());
                g2.dispose();
            }
        }));

        JToolTip toolTip = button.createToolTip();
        toolTip.setTipText("A tool tip");
        JPanel tip = new JPanel(new BorderLayout());
        tip.add(toolTip, BorderLayout.NORTH);
        tip.add(Box.createVerticalStrut(4), BorderLayout.CENTER);
        cell(gallery, "JToolTip", tip);

        JPanel borders = new JPanel(new GridLayout(2, 2, 4, 4));
        borders.add(bordered("Etched", BorderFactory.createEtchedBorder()));
        borders.add(bordered("Bevel", BorderFactory.createRaisedSoftBevelBorder()));
        borders.add(bordered("Dashed", BorderFactory.createDashedBorder(Color.GRAY)));
        borders.add(bordered("Matte", BorderFactory.createMatteBorder(2, 2, 2, 2, icon)));
        cell(gallery, "Borders", borders);

        JPanel titled = new JPanel();
        titled.setBorder(SwingPalette.FRAME);
        titled.add(new JLabel("Titled"));
        cell(gallery, "TitledBorder", titled);
        return gallery;
    }

    /**
     * Whether a slider can be created : the Synth look and feels (Synth, Nimbus, GTK) cannot create one without a display
     * (SynthSliderUI asks for the mouse position).
     */
    static boolean sliderSupported() {
        return !GraphicsEnvironment.isHeadless() || !(UIManager.getLookAndFeel() instanceof SynthLookAndFeel);
    }

    static JTextPane textPane(Icon icon) {
        JTextPane pane = new JTextPane();
        StyledDocument document = pane.getStyledDocument();
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        StyleConstants.setForeground(bold, new Color(0x4269e1));
        SimpleAttributeSet italic = new SimpleAttributeSet();
        StyleConstants.setItalic(italic, true);
        try {
            document.insertString(0, "Styled ", bold);
            document.insertString(document.getLength(), "text ", italic);
            document.insertString(document.getLength(), "and עברית ", null);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        pane.setCaretPosition(document.getLength());
        pane.insertIcon(icon);
        return pane;
    }

    private static JComponent bordered(String text, javax.swing.border.Border border) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setBorder(border);
        return label;
    }

    private static void cell(JPanel gallery, String title, Component component) {
        JPanel cell = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(border.getTitleFont() == null ? null
                : border.getTitleFont().deriveFont(Font.PLAIN, 10f));
        cell.setBorder(border);
        cell.add(component, BorderLayout.CENTER);
        gallery.add(cell);
    }
}
