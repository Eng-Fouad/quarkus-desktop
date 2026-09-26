package io.quarkiverse.desktop.swing.deployment;

import io.quarkiverse.desktop.awt.deployment.AwtClassesAndResources;

/**
 * What Swing needs in a native executable, on top of {@link AwtClassesAndResources} : the look and feels (Nimbus,
 * Synth, Motif, Windows and Windows Classic, GTK, Multi), text editor kits, formatters and editors, bean properties of
 * the components, the Swing API used reflectively. The parts of Swing that AWT itself uses (print dialogs, input method
 * windows, the Linux text peers) are registered by {@link AwtClassesAndResources} : the Swing core (the run time
 * initialization of the Swing packages, the basic and Metal UI delegates and their key bindings, the basic and Metal
 * resources, HTML text), and the shell folders of the file choosers.
 * <p>
 * <b>Lists.</b> Same conventions as {@link AwtClassesAndResources} : each kind of registration has a list for all
 * platforms ({@code KIND}) and one list per platform ({@code WINDOWS_KIND}, {@code LINUX_KIND}, {@code MAC_KIND}) : a
 * native executable gets the common list and the list of the platform it is built for. A platform list only holds what
 * that platform needs alone. Within a list, entries are grouped by area, with a short comment per group, and sorted
 * within a group. A list is a package-private {@code static String[]} field named {@code [PLATFORM_]KIND} : tools read
 * these fields reflectively. Any other constant of this class is a {@code static final String} (or not a
 * {@code String[]}), which these tools ignore.
 * <p>
 * <b>Target platform.</b> Windows when the build host is Windows and the build is not a container build, Linux
 * otherwise. There is no macOS target, so the {@code MAC_} lists are not declared.
 * <p>
 * <b>Kinds and entry formats.</b> Class names are binary names ({@code javax.swing.JEditorPane$PlainEditorKit}).
 * Parameter types are binary names, primitive type names, or either followed by {@code []} for arrays; {@code ()} means
 * no parameters; a constructor is named {@code <init>}.
 * <ul>
 * <li>{@code RUNTIME_INITIALIZED_PACKAGES} : packages whose classes are initialized at run time (sub packages
 * included).</li>
 * <li>{@code RUNTIME_INITIALIZED_CLASSES} : classes initialized at run time.</li>
 * <li>{@code REFLECTIVE_CLASSES} : classes registered for reflection with all their constructors, methods and
 * fields.</li>
 * <li>{@code REFLECTIVE_CONSTRUCTORS} : classes registered for reflection with their constructors (also used for
 * classes that are only looked up by name; array classes are allowed).</li>
 * <li>{@code REFLECTIVE_METHODS} : single methods registered for reflection, {@code "fqcn#name(paramType,...)"}.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_CLASSES} : classes reached from native code, with all their constructors, methods and
 * fields.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_METHODS} : single methods or constructors reached from native code,
 * {@code "fqcn#name(paramType,...)"}.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_FIELDS} : single fields reached from native code, {@code "fqcn#field"}.</li>
 * <li>{@code RESOURCE_BUNDLES} : resource bundle base names, without module name (native-image finds the module of
 * a JDK bundle from its package). The locales of a bundle included in the native executable are those of the
 * application ({@code quarkus.locales}).</li>
 * <li>{@code RESOURCE_GLOBS} : resources included in the executable, as glob patterns.</li>
 * <li>{@code SERVICE_PROVIDERS} : service provider classes of JDK modules, registered for reflection (constructors and
 * methods).</li>
 * </ul>
 * <b>Sources.</b> The UI class defaults of the JDK 25 look and feels (read from the look and feels themselves), the
 * reflective and by-name lookups in the JDK 25 Swing sources, and the tracing agent run on Windows and on Linux (Xvfb,
 * GTK 3) with the checks of the Swing integration tests (every look and feel with a gallery of the components, their
 * defaults and key bindings, HTML and RTF text, formatters, table and tree editors, file and color choosers, option
 * panes, internal frames, property transfers, printing to a PostScript stream, popups, timers and workers).
 */
public final class SwingClassesAndResources {

    private SwingClassesAndResources() {
        // Constants
    }

    // ----------------------------------------------------------------------------------------- run time initialization
    // The Swing packages are initialized at run time by the Desktop AWT extension (AWT uses Swing) : nothing to add.

    static String[] RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] WINDOWS_RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] LINUX_RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] RUNTIME_INITIALIZED_CLASSES = {
    };

    static String[] WINDOWS_RUNTIME_INITIALIZED_CLASSES = {
    };

    static String[] LINUX_RUNTIME_INITIALIZED_CLASSES = {
    };

    // ------------------------------------------------------------------------------------------------------ reflection

    static String[] REFLECTIVE_CLASSES = {
            // bean properties (TransferHandler) : the Introspector reads the constants of the enumerated properties of the
            // components (@BeanProperty(enumerationValues = "SwingConstants.LEFT")) ; GraalVM 25.3 fails the introspection
            // when one of them is not registered
            "java.awt.Adjustable",
            "javax.swing.DebugGraphics",
            "javax.swing.JDesktopPane",
            "javax.swing.JFileChooser",
            "javax.swing.JFormattedTextField",
            "javax.swing.JList",
            "javax.swing.JRootPane",
            "javax.swing.JSplitPane",
            "javax.swing.JTabbedPane",
            "javax.swing.JTable",
            "javax.swing.JViewport",
            "javax.swing.ListSelectionModel",
            "javax.swing.ScrollPaneConstants",
            "javax.swing.SwingConstants",
            "javax.swing.WindowConstants",

            // Synth : color types of the XML files, by name (<color type="TEXT_FOREGROUND">)
            "javax.swing.plaf.synth.ColorType",
    };

    static String[] WINDOWS_REFLECTIVE_CLASSES = {
    };

    static String[] LINUX_REFLECTIVE_CLASSES = {
    };

    static String[] REFLECTIVE_CONSTRUCTORS = {
            // bean properties (TransferHandler) : the bean info of java.awt.Component, found by the Introspector, and the
            // listener arrays of the event sets of the components
            "com.sun.beans.infos.ComponentBeanInfo",
            "java.awt.event.ActionListener[]",
            "java.awt.event.ItemListener[]",
            "java.awt.event.MouseMotionListener[]",
            "javax.swing.event.ChangeListener[]",
            "javax.swing.event.DocumentListener[]",
            "javax.swing.event.ListDataListener[]",

            // look and feels set by class name (UIManager.setLookAndFeel(String), UIManager.createLookAndFeel, the
            // swing.defaultlaf and swing.plaf.multiplexinglaf system properties)
            "com.sun.java.swing.plaf.motif.MotifLookAndFeel",
            "javax.swing.plaf.metal.MetalLookAndFeel",
            "javax.swing.plaf.multi.MultiLookAndFeel",
            "javax.swing.plaf.nimbus.NimbusLookAndFeel",
            "javax.swing.plaf.synth.SynthLookAndFeel",

            // Nimbus : the components of the style prefixes, by name (NimbusDefaults.LazyStyle)
            "javax.swing.JButton",
            "javax.swing.JCheckBox",
            "javax.swing.JCheckBoxMenuItem",
            "javax.swing.JColorChooser",
            "javax.swing.JComboBox",
            "javax.swing.JComponent",
            "javax.swing.JDesktopPane",
            "javax.swing.JEditorPane",
            "javax.swing.JFileChooser",
            "javax.swing.JFormattedTextField",
            "javax.swing.JInternalFrame",
            "javax.swing.JLabel",
            "javax.swing.JList",
            "javax.swing.JMenu",
            "javax.swing.JMenuBar",
            "javax.swing.JMenuItem",
            "javax.swing.JOptionPane",
            "javax.swing.JPanel",
            "javax.swing.JPasswordField",
            "javax.swing.JPopupMenu",
            "javax.swing.JProgressBar",
            "javax.swing.JRadioButton",
            "javax.swing.JRadioButtonMenuItem",
            "javax.swing.JRootPane",
            "javax.swing.JScrollBar",
            "javax.swing.JScrollPane",
            "javax.swing.JSeparator",
            "javax.swing.JSlider",
            "javax.swing.JSpinner",
            "javax.swing.JSplitPane",
            "javax.swing.JTabbedPane",
            "javax.swing.JTable",
            "javax.swing.JTextArea",
            "javax.swing.JTextField",
            "javax.swing.JTextPane",
            "javax.swing.JToggleButton",
            "javax.swing.JToolBar",
            "javax.swing.JToolTip",
            "javax.swing.JTree",
            "javax.swing.JViewport",

            // Nimbus : painters, created by name (NimbusDefaults.LazyPainter)
            "javax.swing.plaf.nimbus.ArrowButtonPainter",
            "javax.swing.plaf.nimbus.ButtonPainter",
            "javax.swing.plaf.nimbus.CheckBoxMenuItemPainter",
            "javax.swing.plaf.nimbus.CheckBoxPainter",
            "javax.swing.plaf.nimbus.ComboBoxArrowButtonPainter",
            "javax.swing.plaf.nimbus.ComboBoxPainter",
            "javax.swing.plaf.nimbus.ComboBoxTextFieldPainter",
            "javax.swing.plaf.nimbus.DesktopIconPainter",
            "javax.swing.plaf.nimbus.DesktopPanePainter",
            "javax.swing.plaf.nimbus.EditorPanePainter",
            "javax.swing.plaf.nimbus.FileChooserPainter",
            "javax.swing.plaf.nimbus.FormattedTextFieldPainter",
            "javax.swing.plaf.nimbus.InternalFramePainter",
            "javax.swing.plaf.nimbus.InternalFrameTitlePaneCloseButtonPainter",
            "javax.swing.plaf.nimbus.InternalFrameTitlePaneIconifyButtonPainter",
            "javax.swing.plaf.nimbus.InternalFrameTitlePaneMaximizeButtonPainter",
            "javax.swing.plaf.nimbus.InternalFrameTitlePaneMenuButtonPainter",
            "javax.swing.plaf.nimbus.MenuBarMenuPainter",
            "javax.swing.plaf.nimbus.MenuBarPainter",
            "javax.swing.plaf.nimbus.MenuItemPainter",
            "javax.swing.plaf.nimbus.MenuPainter",
            "javax.swing.plaf.nimbus.OptionPaneMessageAreaOptionPaneLabelPainter",
            "javax.swing.plaf.nimbus.OptionPanePainter",
            "javax.swing.plaf.nimbus.PasswordFieldPainter",
            "javax.swing.plaf.nimbus.PopupMenuPainter",
            "javax.swing.plaf.nimbus.PopupMenuSeparatorPainter",
            "javax.swing.plaf.nimbus.ProgressBarPainter",
            "javax.swing.plaf.nimbus.RadioButtonMenuItemPainter",
            "javax.swing.plaf.nimbus.RadioButtonPainter",
            "javax.swing.plaf.nimbus.ScrollBarButtonPainter",
            "javax.swing.plaf.nimbus.ScrollBarThumbPainter",
            "javax.swing.plaf.nimbus.ScrollBarTrackPainter",
            "javax.swing.plaf.nimbus.ScrollPanePainter",
            "javax.swing.plaf.nimbus.SeparatorPainter",
            "javax.swing.plaf.nimbus.SliderThumbPainter",
            "javax.swing.plaf.nimbus.SliderTrackPainter",
            "javax.swing.plaf.nimbus.SpinnerNextButtonPainter",
            "javax.swing.plaf.nimbus.SpinnerPanelSpinnerFormattedTextFieldPainter",
            "javax.swing.plaf.nimbus.SpinnerPreviousButtonPainter",
            "javax.swing.plaf.nimbus.SplitPaneDividerPainter",
            "javax.swing.plaf.nimbus.TabbedPaneTabAreaPainter",
            "javax.swing.plaf.nimbus.TabbedPaneTabPainter",
            "javax.swing.plaf.nimbus.TableEditorPainter",
            "javax.swing.plaf.nimbus.TableHeaderPainter",
            "javax.swing.plaf.nimbus.TableHeaderRendererPainter",
            "javax.swing.plaf.nimbus.TextAreaPainter",
            "javax.swing.plaf.nimbus.TextFieldPainter",
            "javax.swing.plaf.nimbus.TextPanePainter",
            "javax.swing.plaf.nimbus.ToggleButtonPainter",
            "javax.swing.plaf.nimbus.ToolBarButtonPainter",
            "javax.swing.plaf.nimbus.ToolBarPainter",
            "javax.swing.plaf.nimbus.ToolBarToggleButtonPainter",
            "javax.swing.plaf.nimbus.ToolTipPainter",
            "javax.swing.plaf.nimbus.TreeCellEditorPainter",
            "javax.swing.plaf.nimbus.TreeCellPainter",
            "javax.swing.plaf.nimbus.TreePainter",

            // Synth : element handlers of the beans decoder (the objects of the XML files; also java.beans.XMLDecoder)
            "com.sun.beans.decoder.ArrayElementHandler",
            "com.sun.beans.decoder.BooleanElementHandler",
            "com.sun.beans.decoder.ByteElementHandler",
            "com.sun.beans.decoder.CharElementHandler",
            "com.sun.beans.decoder.ClassElementHandler",
            "com.sun.beans.decoder.DoubleElementHandler",
            "com.sun.beans.decoder.FalseElementHandler",
            "com.sun.beans.decoder.FieldElementHandler",
            "com.sun.beans.decoder.FloatElementHandler",
            "com.sun.beans.decoder.IntElementHandler",
            "com.sun.beans.decoder.JavaElementHandler",
            "com.sun.beans.decoder.LongElementHandler",
            "com.sun.beans.decoder.MethodElementHandler",
            "com.sun.beans.decoder.NewElementHandler",
            "com.sun.beans.decoder.NullElementHandler",
            "com.sun.beans.decoder.ObjectElementHandler",
            "com.sun.beans.decoder.PropertyElementHandler",
            "com.sun.beans.decoder.ShortElementHandler",
            "com.sun.beans.decoder.StringElementHandler",
            "com.sun.beans.decoder.TrueElementHandler",
            "com.sun.beans.decoder.VarElementHandler",
            "com.sun.beans.decoder.VoidElementHandler",

            // text : editor kits by content type (JEditorPane)
            "javax.swing.JEditorPane$PlainEditorKit",
            "javax.swing.text.html.HTMLEditorKit",
            "javax.swing.text.rtf.RTFEditorKit",
    };

    static String[] WINDOWS_REFLECTIVE_CONSTRUCTORS = {
            // file chooser : the columns of the details view (Win32ShellFolder2.doGetColumnInfo)
            "sun.awt.shell.ShellFolderColumnInfo[]",

            // look and feels set by class name
            "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel",
            "com.sun.java.swing.plaf.windows.WindowsLookAndFeel",

            // Windows look and feel : UI delegate without its own createUI (the inherited one is registered)
            "com.sun.java.swing.plaf.windows.WindowsSeparatorUI",
    };

    static String[] LINUX_REFLECTIVE_CONSTRUCTORS = {
            // look and feels set by class name
            "com.sun.java.swing.plaf.gtk.GTKLookAndFeel",
    };

    static String[] REFLECTIVE_METHODS = {
            // bean properties (new TransferHandler("text")...) : the Introspector only sees the registered accessors.
            // Those of java.awt.Component are the properties of its bean info (all required), the others are the
            // properties usually transferred
            "java.awt.Component#getBackground()",
            "java.awt.Component#getFont()",
            "java.awt.Component#getForeground()",
            "java.awt.Component#getName()",
            "java.awt.Component#isEnabled()",
            "java.awt.Component#isFocusable()",
            "java.awt.Component#isVisible()",
            "java.awt.Component#setBackground(java.awt.Color)",
            "java.awt.Component#setEnabled(boolean)",
            "java.awt.Component#setFocusable(boolean)",
            "java.awt.Component#setFont(java.awt.Font)",
            "java.awt.Component#setForeground(java.awt.Color)",
            "java.awt.Component#setName(java.lang.String)",
            "java.awt.Component#setVisible(boolean)",
            "javax.swing.AbstractButton#getIcon()",
            "javax.swing.AbstractButton#getText()",
            "javax.swing.AbstractButton#isSelected()",
            "javax.swing.AbstractButton#setIcon(javax.swing.Icon)",
            "javax.swing.AbstractButton#setSelected(boolean)",
            "javax.swing.AbstractButton#setText(java.lang.String)",
            "javax.swing.JColorChooser#getColor()",
            "javax.swing.JColorChooser#setColor(java.awt.Color)",
            "javax.swing.JComboBox#getSelectedIndex()",
            "javax.swing.JComboBox#getSelectedItem()",
            "javax.swing.JComboBox#setSelectedIndex(int)",
            "javax.swing.JComboBox#setSelectedItem(java.lang.Object)",
            "javax.swing.JComponent#getToolTipText()",
            "javax.swing.JComponent#setBackground(java.awt.Color)",
            "javax.swing.JComponent#setEnabled(boolean)",
            "javax.swing.JComponent#setFont(java.awt.Font)",
            "javax.swing.JComponent#setForeground(java.awt.Color)",
            "javax.swing.JComponent#setToolTipText(java.lang.String)",
            "javax.swing.JComponent#setVisible(boolean)",
            "javax.swing.JFormattedTextField#getValue()",
            "javax.swing.JFormattedTextField#setValue(java.lang.Object)",
            "javax.swing.JLabel#getIcon()",
            "javax.swing.JLabel#getText()",
            "javax.swing.JLabel#setIcon(javax.swing.Icon)",
            "javax.swing.JLabel#setText(java.lang.String)",
            "javax.swing.JList#getSelectedIndex()",
            "javax.swing.JList#setSelectedIndex(int)",
            "javax.swing.JProgressBar#getValue()",
            "javax.swing.JProgressBar#setValue(int)",
            "javax.swing.JScrollBar#getValue()",
            "javax.swing.JScrollBar#setValue(int)",
            "javax.swing.JSlider#getValue()",
            "javax.swing.JSlider#setValue(int)",
            "javax.swing.JSpinner#getValue()",
            "javax.swing.JSpinner#setValue(java.lang.Object)",
            "javax.swing.JTabbedPane#getSelectedIndex()",
            "javax.swing.JTabbedPane#setSelectedIndex(int)",
            "javax.swing.JToolTip#getTipText()",
            "javax.swing.JToolTip#setTipText(java.lang.String)",
            "javax.swing.text.JTextComponent#getText()",
            "javax.swing.text.JTextComponent#setText(java.lang.String)",

            // formatters (DefaultFormatter, NumberFormatter) and table editors (JTable.GenericEditor) : values created
            // from text with the String constructor of their class
            "java.lang.Boolean#<init>(java.lang.String)",
            "java.lang.Byte#<init>(java.lang.String)",
            "java.lang.Double#<init>(java.lang.String)",
            "java.lang.Float#<init>(java.lang.String)",
            "java.lang.Integer#<init>(java.lang.String)",
            "java.lang.Long#<init>(java.lang.String)",
            "java.lang.Short#<init>(java.lang.String)",
            "java.math.BigDecimal#<init>(java.lang.String)",
            "java.math.BigInteger#<init>(java.lang.String)",
            "java.util.Date#<init>(java.lang.String)",

            // editable combo boxes (BasicComboBoxEditor) : items created from text with valueOf(String)
            "java.lang.Boolean#valueOf(java.lang.String)",
            "java.lang.Byte#valueOf(java.lang.String)",
            "java.lang.Double#valueOf(java.lang.String)",
            "java.lang.Float#valueOf(java.lang.String)",
            "java.lang.Integer#valueOf(java.lang.String)",
            "java.lang.Long#valueOf(java.lang.String)",
            "java.lang.Short#valueOf(java.lang.String)",

            // input methods : whether a text component handles the input method events itself is checked with
            // reflection (JTextComponent.METHOD_OVERRIDDEN)
            "javax.swing.JFormattedTextField#processInputMethodEvent(java.awt.event.InputMethodEvent)",

            // Multi look and feel (auxiliary look and feels) : UI delegates
            "javax.swing.plaf.multi.MultiButtonUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiColorChooserUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiComboBoxUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiDesktopIconUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiDesktopPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiFileChooserUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiInternalFrameUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiLabelUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiListUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiMenuBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiMenuItemUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiOptionPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiPanelUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiPopupMenuUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiProgressBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiRootPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiScrollBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiScrollPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiSeparatorUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiSliderUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiSpinnerUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiSplitPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiTabbedPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiTableHeaderUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiTableUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiTextUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiToolBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiToolTipUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiTreeUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.multi.MultiViewportUI#createUI(javax.swing.JComponent)",

            // Synth look and feels (Synth, Nimbus, GTK) : SynthLookAndFeel creates their UI delegates
            "javax.swing.plaf.synth.SynthLookAndFeel#createUI(javax.swing.JComponent)",

            // Synth : objects of the XML files without arguments (created with Class.newInstance by the beans decoder)
            "java.lang.Class#newInstance()",
    };

    static String[] WINDOWS_REFLECTIVE_METHODS = {
            // Motif look and feel : UI delegates (on Linux, the Desktop AWT extension registers them)
            "com.sun.java.swing.plaf.motif.MotifButtonUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifCheckBoxMenuItemUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifCheckBoxUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifComboBoxUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifDesktopIconUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifDesktopPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifEditorPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifFileChooserUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifInternalFrameUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifLabelUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifMenuBarUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifMenuItemUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifMenuUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifOptionPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifPasswordFieldUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifPopupMenuSeparatorUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifPopupMenuUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifProgressBarUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifRadioButtonMenuItemUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifRadioButtonUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifScrollBarUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifScrollPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifSeparatorUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifSliderUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifSplitPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifTabbedPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifTextAreaUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifTextFieldUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifTextPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifToggleButtonUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.motif.MotifTreeUI#createUI(javax.swing.JComponent)",

            // Windows look and feel : UI delegates
            "com.sun.java.swing.plaf.windows.WindowsButtonUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsCheckBoxMenuItemUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsCheckBoxUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsComboBoxUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsDesktopIconUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsDesktopPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsEditorPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsFileChooserUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsInternalFrameUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsLabelUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsMenuBarUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsMenuItemUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsMenuUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsPasswordFieldUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsPopupMenuSeparatorUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsPopupMenuUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsProgressBarUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsRadioButtonMenuItemUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsRadioButtonUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsRootPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsScrollBarUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsSliderUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsSpinnerUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsSplitPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsTableHeaderUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsTextAreaUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsTextFieldUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsTextPaneUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsToggleButtonUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsToolBarSeparatorUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsToolBarUI#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.windows.WindowsTreeUI#createUI(javax.swing.JComponent)",
    };

    static String[] LINUX_REFLECTIVE_METHODS = {
            // GTK look and feel : UI delegates (the file chooser, then Synth), lazy values (GTKStyle.GTKLazyValue) and
            // icon painters (GTKIconFactory)
            "com.sun.java.swing.plaf.gtk.GTKLookAndFeel#createUI(javax.swing.JComponent)",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getAscendingSortIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getCheckBoxIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getCheckBoxMenuItemCheckIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getDescendingSortIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getMenuArrowIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getRadioButtonIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getRadioButtonMenuItemCheckIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getTreeCollapsedIcon()",
            "com.sun.java.swing.plaf.gtk.GTKIconFactory#getTreeExpandedIcon()",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintAscendingSortIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintCheckBoxIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintCheckBoxMenuItemCheckIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintDescendingSortIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintMenuArrowIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int,com.sun.java.swing.plaf.gtk.GTKConstants$ArrowType)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintRadioButtonIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintRadioButtonMenuItemCheckIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintToolBarHandleIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int,com.sun.java.swing.plaf.gtk.GTKConstants$Orientation)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintTreeCollapsedIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter#paintTreeExpandedIcon(javax.swing.plaf.synth.SynthContext,java.awt.Graphics,int,int,int,int,int)",
            "com.sun.java.swing.plaf.gtk.GTKPainter$ListTableFocusBorder#getNoFocusCellBorder()",
            "com.sun.java.swing.plaf.gtk.GTKPainter$ListTableFocusBorder#getSelectedCellBorder()",
            "com.sun.java.swing.plaf.gtk.GTKPainter$ListTableFocusBorder#getUnselectedCellBorder()",
            "com.sun.java.swing.plaf.gtk.Metacity#getTitlePaneLayout()",
    };

    // ------------------------------------------------------------------------------------------------------------- JNI
    // The native code of Swing (Windows themes, GTK engine, shell folders) creates the AWT and java.base objects that
    // the Desktop AWT extension registers.

    static String[] JNI_RUNTIME_ACCESS_CLASSES = {
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_CLASSES = {
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_CLASSES = {
    };

    static String[] JNI_RUNTIME_ACCESS_METHODS = {
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_METHODS = {
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_METHODS = {
    };

    static String[] JNI_RUNTIME_ACCESS_FIELDS = {
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_FIELDS = {
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_FIELDS = {
    };

    // ----------------------------------------------------------------------------------------------- service providers

    static String[] SERVICE_PROVIDERS = {
    };

    static String[] WINDOWS_SERVICE_PROVIDERS = {
    };

    static String[] LINUX_SERVICE_PROVIDERS = {
    };

    // ------------------------------------------------------------------------------------------------ resource bundles

    static String[] RESOURCE_BUNDLES = {
            // Synth look and feels (Synth, Nimbus, GTK)
            "com.sun.swing.internal.plaf.synth.resources.synth",
    };

    static String[] WINDOWS_RESOURCE_BUNDLES = {
            // Motif look and feel (on Linux, the Desktop AWT extension registers it)
            "com.sun.java.swing.plaf.motif.resources.motif",

            // Windows look and feel
            "com.sun.java.swing.plaf.windows.resources.windows",
    };

    static String[] LINUX_RESOURCE_BUNDLES = {
            // GTK look and feel
            "com.sun.java.swing.plaf.gtk.resources.gtk",
    };

    // ------------------------------------------------------------------------------------------------------- resources

    static String[] RESOURCE_GLOBS = {
            // Metal look and feel : auditory cues (opt-in, AuditoryCues.playList)
            "javax/swing/plaf/metal/sounds/*",

            // text : the charsets of the RTF reader, and the content types of the pages (JEditorPane.setPage)
            "javax/swing/text/rtf/charsets/*",
            "sun/net/www/content-types.properties",
    };

    static String[] WINDOWS_RESOURCE_GLOBS = {
            // Motif look and feel (on Linux, the Desktop AWT extension registers them)
            "com/sun/java/swing/plaf/motif/icons/*",

            // Windows look and feel (fallbacks of the shell icons)
            "com/sun/java/swing/plaf/windows/icons/*",
    };

    static String[] LINUX_RESOURCE_GLOBS = {
            // GTK look and feel : icons, and the theme of the window decorations drawn by the look and feel (Metacity)
            "com/sun/java/swing/plaf/gtk/icons/*",
            "com/sun/java/swing/plaf/gtk/resources/metacity/**",
    };
}
