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
 * <b>Target platform.</b> Windows or macOS when the build host is Windows or macOS and the build is not a container
 * build, Linux otherwise. The {@code MAC_} lists are grouped in a section of their own, at the end of the class : the
 * Aqua look and feel is registered by {@link AwtClassesAndResources} on macOS, since the AWT components are drawn by
 * Aqua delegates there.
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
 * <li>{@code REFLECTIVE_FIELD_CLASSES} : classes registered for reflection with their fields only (constants that Swing
 * reads by name).</li>
 * <li>{@code REFLECTIVE_CONSTRUCTORS} : classes registered for reflection with their constructors (also used for
 * classes that are only looked up by name; array classes are allowed).</li>
 * <li>{@code REFLECTIVE_METHODS} : single methods registered for reflection, {@code "fqcn#name(paramType,...)"}.</li>
 * <li>{@code REFLECTIVE_FIELDS} : single fields registered for reflection, {@code "fqcn#field"}.</li>
 * <li>{@code REFLECTIVE_PUBLIC_MEMBERS} : classes registered for reflection with their public constructors, public
 * methods (inherited ones included) and public fields (what the JavaBeans API uses).</li>
 * <li>{@code JAVA_BEANS_CLASSES} : JDK classes registered as {@code REFLECTIVE_PUBLIC_MEMBERS} when
 * {@code quarkus.desktop.swing.java-beans.jdk-classes} is enabled : the bean properties, event sets and public fields
 * of the Swing components, models, borders, events and listeners, text components and documents, for the JavaBeans
 * API.</li>
 * <li>{@code REFLECTIVE_TYPES} : classes whose members are queried with reflection, registered as types (the
 * internal values of the Swing classes that the JavaBeans API meets), for {@code --exact-reachability-metadata}.</li>
 * <li>{@code NEGATIVE_CLASS_LOOKUPS} : class names that Swing looks up and expects not to find (they do not exist in the
 * JDK), for {@code --exact-reachability-metadata}.</li>
 * <li>{@code METHOD_LOOKUPS} : methods that Swing looks up with {@code getDeclaredMethod} to find out whether a class
 * declares them, {@code "fqcn#name(paramType,...)"}, for {@code --exact-reachability-metadata}.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_CLASSES} : classes reached from native code, with all their constructors, methods and
 * fields.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_METHODS} : single methods or constructors reached from native code,
 * {@code "fqcn#name(paramType,...)"}.</li>
 * <li>{@code JNI_RUNTIME_ACCESS_FIELDS} : single fields reached from native code, {@code "fqcn#field"}.</li>
 * <li>{@code RESOURCE_BUNDLES} : resource bundle base names, without module name (native-image finds the module of
 * a JDK bundle from its package). The locales of a bundle included in the native executable are those of the
 * application ({@code quarkus.locales}).</li>
 * <li>{@code RESOURCE_GLOBS} : resources included in the executable, as glob patterns.</li>
 * <li>{@code RESOURCE_LOOKUPS} : resources that Swing looks up and expects not to find (they do not exist in the JDK), as
 * glob patterns, for {@code --exact-reachability-metadata}.</li>
 * <li>{@code SERVICE_PROVIDERS} : service provider classes of JDK modules, registered for reflection (constructors and
 * methods).</li>
 * </ul>
 * <b>Sources.</b> The UI class defaults of the JDK 25 look and feels (read from the look and feels themselves), the
 * reflective and by-name lookups in the JDK 25 Swing sources, and the tracing agent run on Windows and on Linux (Xvfb,
 * GTK 3) with the checks of the Swing integration tests (every look and feel with a gallery of the components, their
 * defaults and key bindings, HTML and RTF text, formatters, table and tree editors, file and color choosers, option
 * panes, internal frames, property transfers, printing to a PostScript stream, popups, timers and workers). The
 * lookups for {@code --exact-reachability-metadata} come from native executables of the showcase built with it (Windows
 * and Linux, JDK 25), and from the JDK 25 classes (the text components that may declare
 * {@code processInputMethodEvent}).
 */
public final class SwingClassesAndResources {

    /**
     * The icons of the bean infos of the Swing components ({@code BeanInfo.getIcon} of a {@code javax.swing} class
     * annotated with {@code @JavaBean} : {@code SimpleBeanInfo} loads {@code JButtonColor16.gif}...), included with the
     * {@code JAVA_BEANS_CLASSES} (179 GIF files, 36 KB).
     */
    static final String JAVA_BEANS_ICONS = "javax/swing/beaninfo/images/*";

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
    };

    static String[] WINDOWS_REFLECTIVE_CLASSES = {
    };

    static String[] LINUX_REFLECTIVE_CLASSES = {
    };

    static String[] REFLECTIVE_FIELD_CLASSES = {
            // bean properties (TransferHandler) : the Introspector reads the constants of the enumerated properties of the
            // components (@BeanProperty(enumerationValues = "SwingConstants.LEFT") : PropertyInfo reads them with
            // Class.getField and Field.get) ; GraalVM 25.3 fails the introspection when one of them is not registered
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

            // Synth : color types of the XML files, by name (<color type="TEXT_FOREGROUND"> : SynthParser reads the
            // ColorType field)
            "javax.swing.plaf.synth.ColorType",
    };

    static String[] WINDOWS_REFLECTIVE_FIELD_CLASSES = {
    };

    static String[] LINUX_REFLECTIVE_FIELD_CLASSES = {
    };

    static String[] REFLECTIVE_CONSTRUCTORS = {
            // bean properties (TransferHandler) : the listener arrays of the event sets of the components (the Desktop AWT
            // extension registers the bean info of java.awt.Component)
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

            // JavaBeans : XMLEncoder instantiates the values of the properties it encodes, to compare them with the
            // default (the graphics utilities of the Synth look and feels)
            "javax.swing.plaf.synth.SynthGraphicsUtils",

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
            // bean properties (new TransferHandler("text")...) : the Introspector only sees the registered accessors : the
            // properties usually transferred (the Desktop AWT extension registers those of java.awt.Component, the
            // properties of its bean info)
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
            // from text with the String constructor of their class (the Desktop AWT extension registers those of the
            // java.lang wrappers, for XMLEncoder)
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

            // Motif look and feel : UI delegates (installed on every platform ; on Linux, the Desktop AWT extension
            // registers them too, for the X11 text components)
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
    };

    static String[] WINDOWS_REFLECTIVE_METHODS = {
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

    static String[] REFLECTIVE_FIELDS = {
    };

    static String[] WINDOWS_REFLECTIVE_FIELDS = {
    };

    static String[] LINUX_REFLECTIVE_FIELDS = {
    };

    static String[] REFLECTIVE_PUBLIC_MEMBERS = {
            // JavaBeans : the key strokes of the Swing properties (accelerators, input maps), which XMLEncoder writes with
            // their factory methods (the AWTKeyStroke persistence delegate)
            "javax.swing.KeyStroke",
    };

    static String[] WINDOWS_REFLECTIVE_PUBLIC_MEMBERS = {
    };

    static String[] LINUX_REFLECTIVE_PUBLIC_MEMBERS = {
    };

    // ---------------------------------------------------------------------------------------------------- JavaBeans
    // The bean properties, event sets and public fields of the Swing classes, for the Introspector, XMLEncoder and
    // XMLDecoder, Statement, Expression, EventHandler and Beans.instantiate : registered with their public constructors,
    // methods and fields when quarkus.desktop.swing.java-beans.jdk-classes is enabled (with the AWT classes that they
    // extend, see AwtClassesAndResources.JAVA_BEANS_CLASSES). The classes of an application are registered by the
    // application (@RegisterForReflection).

    static String[] JAVA_BEANS_CLASSES = {
            // components, models, layouts, actions, key strokes, icons, transfer handlers... : every public class of
            // javax.swing
            "javax.swing.AbstractAction",
            "javax.swing.AbstractButton",
            "javax.swing.AbstractCellEditor",
            "javax.swing.AbstractListModel",
            "javax.swing.AbstractSpinnerModel",
            "javax.swing.Action",
            "javax.swing.ActionMap",
            "javax.swing.BorderFactory",
            "javax.swing.BoundedRangeModel",
            "javax.swing.Box",
            "javax.swing.Box$Filler",
            "javax.swing.BoxLayout",
            "javax.swing.ButtonGroup",
            "javax.swing.ButtonModel",
            "javax.swing.CellEditor",
            "javax.swing.CellRendererPane",
            "javax.swing.ComboBoxEditor",
            "javax.swing.ComboBoxModel",
            "javax.swing.ComponentInputMap",
            "javax.swing.DebugGraphics",
            "javax.swing.DefaultBoundedRangeModel",
            "javax.swing.DefaultButtonModel",
            "javax.swing.DefaultCellEditor",
            "javax.swing.DefaultComboBoxModel",
            "javax.swing.DefaultDesktopManager",
            "javax.swing.DefaultFocusManager",
            "javax.swing.DefaultListCellRenderer",
            "javax.swing.DefaultListCellRenderer$UIResource",
            "javax.swing.DefaultListModel",
            "javax.swing.DefaultListSelectionModel",
            "javax.swing.DefaultRowSorter",
            "javax.swing.DefaultSingleSelectionModel",
            "javax.swing.DesktopManager",
            "javax.swing.DropMode",
            "javax.swing.FocusManager",
            "javax.swing.GrayFilter",
            "javax.swing.GroupLayout",
            "javax.swing.GroupLayout$Alignment",
            "javax.swing.GroupLayout$Group",
            "javax.swing.GroupLayout$ParallelGroup",
            "javax.swing.GroupLayout$SequentialGroup",
            "javax.swing.Icon",
            "javax.swing.ImageIcon",
            "javax.swing.InputMap",
            "javax.swing.InputVerifier",
            "javax.swing.InternalFrameFocusTraversalPolicy",
            "javax.swing.JApplet",
            "javax.swing.JButton",
            "javax.swing.JCheckBox",
            "javax.swing.JCheckBoxMenuItem",
            "javax.swing.JColorChooser",
            "javax.swing.JComboBox",
            "javax.swing.JComboBox$KeySelectionManager",
            "javax.swing.JComponent",
            "javax.swing.JDesktopPane",
            "javax.swing.JDialog",
            "javax.swing.JEditorPane",
            "javax.swing.JFileChooser",
            "javax.swing.JFormattedTextField",
            "javax.swing.JFormattedTextField$AbstractFormatter",
            "javax.swing.JFormattedTextField$AbstractFormatterFactory",
            "javax.swing.JFrame",
            "javax.swing.JInternalFrame",
            "javax.swing.JInternalFrame$JDesktopIcon",
            "javax.swing.JLabel",
            "javax.swing.JLayer",
            "javax.swing.JLayeredPane",
            "javax.swing.JList",
            "javax.swing.JList$DropLocation",
            "javax.swing.JMenu",
            "javax.swing.JMenuBar",
            "javax.swing.JMenuItem",
            "javax.swing.JOptionPane",
            "javax.swing.JPanel",
            "javax.swing.JPasswordField",
            "javax.swing.JPopupMenu",
            "javax.swing.JPopupMenu$Separator",
            "javax.swing.JProgressBar",
            "javax.swing.JRadioButton",
            "javax.swing.JRadioButtonMenuItem",
            "javax.swing.JRootPane",
            "javax.swing.JScrollBar",
            "javax.swing.JScrollPane",
            "javax.swing.JSeparator",
            "javax.swing.JSlider",
            "javax.swing.JSpinner",
            "javax.swing.JSpinner$DateEditor",
            "javax.swing.JSpinner$DefaultEditor",
            "javax.swing.JSpinner$ListEditor",
            "javax.swing.JSpinner$NumberEditor",
            "javax.swing.JSplitPane",
            "javax.swing.JTabbedPane",
            "javax.swing.JTable",
            "javax.swing.JTable$DropLocation",
            "javax.swing.JTable$PrintMode",
            "javax.swing.JTextArea",
            "javax.swing.JTextField",
            "javax.swing.JTextPane",
            "javax.swing.JToggleButton",
            "javax.swing.JToggleButton$ToggleButtonModel",
            "javax.swing.JToolBar",
            "javax.swing.JToolBar$Separator",
            "javax.swing.JToolTip",
            "javax.swing.JTree",
            "javax.swing.JTree$DropLocation",
            "javax.swing.JTree$DynamicUtilTreeNode",
            "javax.swing.JViewport",
            "javax.swing.JWindow",
            "javax.swing.KeyStroke",
            "javax.swing.LayoutFocusTraversalPolicy",
            "javax.swing.LayoutStyle",
            "javax.swing.LayoutStyle$ComponentPlacement",
            "javax.swing.ListCellRenderer",
            "javax.swing.ListModel",
            "javax.swing.ListSelectionModel",
            "javax.swing.LookAndFeel",
            "javax.swing.MenuElement",
            "javax.swing.MenuSelectionManager",
            "javax.swing.MutableComboBoxModel",
            "javax.swing.OverlayLayout",
            "javax.swing.Painter",
            "javax.swing.Popup",
            "javax.swing.PopupFactory",
            "javax.swing.ProgressMonitor",
            "javax.swing.ProgressMonitorInputStream",
            "javax.swing.Renderer",
            "javax.swing.RepaintManager",
            "javax.swing.RootPaneContainer",
            "javax.swing.RowFilter",
            "javax.swing.RowFilter$ComparisonType",
            "javax.swing.RowFilter$Entry",
            "javax.swing.RowSorter",
            "javax.swing.RowSorter$SortKey",
            "javax.swing.ScrollPaneConstants",
            "javax.swing.ScrollPaneLayout",
            "javax.swing.ScrollPaneLayout$UIResource",
            "javax.swing.Scrollable",
            "javax.swing.SingleSelectionModel",
            "javax.swing.SizeRequirements",
            "javax.swing.SizeSequence",
            "javax.swing.SortOrder",
            "javax.swing.SortingFocusTraversalPolicy",
            "javax.swing.SpinnerDateModel",
            "javax.swing.SpinnerListModel",
            "javax.swing.SpinnerModel",
            "javax.swing.SpinnerNumberModel",
            "javax.swing.Spring",
            "javax.swing.SpringLayout",
            "javax.swing.SpringLayout$Constraints",
            "javax.swing.SwingConstants",
            "javax.swing.SwingContainer",
            "javax.swing.SwingUtilities",
            "javax.swing.SwingWorker",
            "javax.swing.SwingWorker$StateValue",
            "javax.swing.Timer",
            "javax.swing.ToolTipManager",
            "javax.swing.TransferHandler",
            "javax.swing.TransferHandler$DropLocation",
            "javax.swing.TransferHandler$TransferSupport",
            "javax.swing.UIClientPropertyKey",
            "javax.swing.UIDefaults",
            "javax.swing.UIDefaults$ActiveValue",
            "javax.swing.UIDefaults$LazyInputMap",
            "javax.swing.UIDefaults$LazyValue",
            "javax.swing.UIDefaults$ProxyLazyValue",
            "javax.swing.UIManager",
            "javax.swing.UIManager$LookAndFeelInfo",
            "javax.swing.UnsupportedLookAndFeelException",
            "javax.swing.ViewportLayout",
            "javax.swing.WindowConstants",

            // borders
            "javax.swing.border.AbstractBorder",
            "javax.swing.border.BevelBorder",
            "javax.swing.border.Border",
            "javax.swing.border.CompoundBorder",
            "javax.swing.border.EmptyBorder",
            "javax.swing.border.EtchedBorder",
            "javax.swing.border.LineBorder",
            "javax.swing.border.MatteBorder",
            "javax.swing.border.SoftBevelBorder",
            "javax.swing.border.StrokeBorder",
            "javax.swing.border.TitledBorder",

            // events and listeners (event sets, EventHandler)
            "javax.swing.event.AncestorEvent",
            "javax.swing.event.AncestorListener",
            "javax.swing.event.CaretEvent",
            "javax.swing.event.CaretListener",
            "javax.swing.event.CellEditorListener",
            "javax.swing.event.ChangeEvent",
            "javax.swing.event.ChangeListener",
            "javax.swing.event.DocumentEvent",
            "javax.swing.event.DocumentEvent$ElementChange",
            "javax.swing.event.DocumentEvent$EventType",
            "javax.swing.event.DocumentListener",
            "javax.swing.event.EventListenerList",
            "javax.swing.event.HyperlinkEvent",
            "javax.swing.event.HyperlinkEvent$EventType",
            "javax.swing.event.HyperlinkListener",
            "javax.swing.event.InternalFrameAdapter",
            "javax.swing.event.InternalFrameEvent",
            "javax.swing.event.InternalFrameListener",
            "javax.swing.event.ListDataEvent",
            "javax.swing.event.ListDataListener",
            "javax.swing.event.ListSelectionEvent",
            "javax.swing.event.ListSelectionListener",
            "javax.swing.event.MenuDragMouseEvent",
            "javax.swing.event.MenuDragMouseListener",
            "javax.swing.event.MenuEvent",
            "javax.swing.event.MenuKeyEvent",
            "javax.swing.event.MenuKeyListener",
            "javax.swing.event.MenuListener",
            "javax.swing.event.MouseInputAdapter",
            "javax.swing.event.MouseInputListener",
            "javax.swing.event.PopupMenuEvent",
            "javax.swing.event.PopupMenuListener",
            "javax.swing.event.RowSorterEvent",
            "javax.swing.event.RowSorterEvent$Type",
            "javax.swing.event.RowSorterListener",
            "javax.swing.event.SwingPropertyChangeSupport",
            "javax.swing.event.TableColumnModelEvent",
            "javax.swing.event.TableColumnModelListener",
            "javax.swing.event.TableModelEvent",
            "javax.swing.event.TableModelListener",
            "javax.swing.event.TreeExpansionEvent",
            "javax.swing.event.TreeExpansionListener",
            "javax.swing.event.TreeModelEvent",
            "javax.swing.event.TreeModelListener",
            "javax.swing.event.TreeSelectionEvent",
            "javax.swing.event.TreeSelectionListener",
            "javax.swing.event.TreeWillExpandListener",
            "javax.swing.event.UndoableEditEvent",
            "javax.swing.event.UndoableEditListener",

            // the UI resources of the look and feels (values of the component properties), and the editors of the combo
            // boxes of the basic and Metal look and feels (the edited item of an editable combo box is a property of its
            // editor)
            "javax.swing.plaf.basic.BasicComboBoxEditor",
            "javax.swing.plaf.basic.BasicComboBoxEditor$UIResource",
            "javax.swing.plaf.metal.MetalComboBoxEditor",
            "javax.swing.plaf.metal.MetalComboBoxEditor$UIResource",
            "javax.swing.plaf.ActionMapUIResource",
            "javax.swing.plaf.BorderUIResource",
            "javax.swing.plaf.BorderUIResource$BevelBorderUIResource",
            "javax.swing.plaf.BorderUIResource$CompoundBorderUIResource",
            "javax.swing.plaf.BorderUIResource$EmptyBorderUIResource",
            "javax.swing.plaf.BorderUIResource$EtchedBorderUIResource",
            "javax.swing.plaf.BorderUIResource$LineBorderUIResource",
            "javax.swing.plaf.BorderUIResource$MatteBorderUIResource",
            "javax.swing.plaf.BorderUIResource$TitledBorderUIResource",
            "javax.swing.plaf.ColorUIResource",
            "javax.swing.plaf.ComponentInputMapUIResource",
            "javax.swing.plaf.DimensionUIResource",
            "javax.swing.plaf.FontUIResource",
            "javax.swing.plaf.IconUIResource",
            "javax.swing.plaf.InputMapUIResource",
            "javax.swing.plaf.InsetsUIResource",
            "javax.swing.plaf.UIResource",

            // tables : headers, columns, models, renderers and editors
            "javax.swing.table.AbstractTableModel",
            "javax.swing.table.DefaultTableCellRenderer",
            "javax.swing.table.DefaultTableCellRenderer$UIResource",
            "javax.swing.table.DefaultTableColumnModel",
            "javax.swing.table.DefaultTableModel",
            "javax.swing.table.JTableHeader",
            "javax.swing.table.TableCellEditor",
            "javax.swing.table.TableCellRenderer",
            "javax.swing.table.TableColumn",
            "javax.swing.table.TableColumnModel",
            "javax.swing.table.TableModel",
            "javax.swing.table.TableRowSorter",
            "javax.swing.table.TableStringConverter",

            // text : documents, carets, highlighters, key maps, editor kits, formatters
            "javax.swing.text.AbstractDocument",
            "javax.swing.text.AttributeSet",
            "javax.swing.text.Caret",
            "javax.swing.text.DateFormatter",
            "javax.swing.text.DefaultCaret",
            "javax.swing.text.DefaultEditorKit",
            "javax.swing.text.DefaultFormatter",
            "javax.swing.text.DefaultFormatterFactory",
            "javax.swing.text.DefaultHighlighter",
            "javax.swing.text.DefaultHighlighter$DefaultHighlightPainter",
            "javax.swing.text.DefaultStyledDocument",
            "javax.swing.text.Document",
            "javax.swing.text.DocumentFilter",
            "javax.swing.text.EditorKit",
            "javax.swing.text.Highlighter",
            "javax.swing.text.Highlighter$HighlightPainter",
            "javax.swing.text.InternationalFormatter",
            "javax.swing.text.JTextComponent",
            "javax.swing.text.JTextComponent$DropLocation",
            "javax.swing.text.JTextComponent$KeyBinding",
            "javax.swing.text.Keymap",
            "javax.swing.text.LayeredHighlighter",
            "javax.swing.text.LayeredHighlighter$LayerPainter",
            "javax.swing.text.MaskFormatter",
            "javax.swing.text.MutableAttributeSet",
            "javax.swing.text.NavigationFilter",
            "javax.swing.text.NumberFormatter",
            "javax.swing.text.PlainDocument",
            "javax.swing.text.SimpleAttributeSet",
            "javax.swing.text.Style",
            "javax.swing.text.StyleContext",
            "javax.swing.text.StyleContext$NamedStyle",
            "javax.swing.text.StyledDocument",
            "javax.swing.text.StyledEditorKit",
            "javax.swing.text.TabSet",
            "javax.swing.text.TabStop",
            "javax.swing.text.TextAction",

            // trees : nodes, models, paths, selection models, renderers and editors
            "javax.swing.tree.AbstractLayoutCache",
            "javax.swing.tree.AbstractLayoutCache$NodeDimensions",
            "javax.swing.tree.DefaultMutableTreeNode",
            "javax.swing.tree.DefaultTreeCellEditor",
            "javax.swing.tree.DefaultTreeCellEditor$DefaultTextField",
            "javax.swing.tree.DefaultTreeCellEditor$EditorContainer",
            "javax.swing.tree.DefaultTreeCellRenderer",
            "javax.swing.tree.DefaultTreeModel",
            "javax.swing.tree.DefaultTreeSelectionModel",
            "javax.swing.tree.ExpandVetoException",
            "javax.swing.tree.FixedHeightLayoutCache",
            "javax.swing.tree.MutableTreeNode",
            "javax.swing.tree.RowMapper",
            "javax.swing.tree.TreeCellEditor",
            "javax.swing.tree.TreeCellRenderer",
            "javax.swing.tree.TreeModel",
            "javax.swing.tree.TreeNode",
            "javax.swing.tree.TreePath",
            "javax.swing.tree.TreeSelectionModel",
            "javax.swing.tree.VariableHeightLayoutCache",
    };

    static String[] WINDOWS_JAVA_BEANS_CLASSES = {
    };

    static String[] LINUX_JAVA_BEANS_CLASSES = {
    };

    // ------------------------------------------------------------------------------------- exact reachability metadata
    // What a native executable built with --exact-reachability-metadata needs besides the registrations above (see the
    // Desktop AWT extension, which also computes the lookups that depend on names).

    static String[] REFLECTIVE_TYPES = {
            // bean properties (new TransferHandler("text")...) : the Introspector reads the class of the component (the
            // components of the transferred properties of REFLECTIVE_METHODS) : its BeanInfo and Customizer probes, and
            // the members of its supertypes and of the listener interfaces of its event sets
            "javax.swing.JButton",
            "javax.swing.JCheckBox",
            "javax.swing.JCheckBoxMenuItem",
            "javax.swing.JColorChooser",
            "javax.swing.JComboBox",
            "javax.swing.JEditorPane",
            "javax.swing.JFormattedTextField",
            "javax.swing.JLabel",
            "javax.swing.JList",
            "javax.swing.JMenu",
            "javax.swing.JMenuItem",
            "javax.swing.JPasswordField",
            "javax.swing.JProgressBar",
            "javax.swing.JRadioButton",
            "javax.swing.JRadioButtonMenuItem",
            "javax.swing.JScrollBar",
            "javax.swing.JSlider",
            "javax.swing.JSpinner",
            "javax.swing.JTabbedPane",
            "javax.swing.JTextArea",
            "javax.swing.JTextField",
            "javax.swing.JTextPane",
            "javax.swing.JToggleButton",
            "javax.swing.JToolTip",

            // JavaBeans : the internal values of the Swing components that XMLEncoder meets (layouts, borders, renderers,
            // transfer handlers, key maps of the basic and Metal look and feels), and the interfaces of the components
            "javax.swing.JRootPane$1",
            "javax.swing.TransferHandler$HasGetTransferHandler",
            "javax.swing.plaf.basic.BasicComboBoxRenderer",
            "javax.swing.plaf.basic.BasicComboBoxRenderer$UIResource",
            "javax.swing.plaf.basic.BasicComboBoxUI$ComboBoxLayoutManager",
            "javax.swing.plaf.basic.BasicComboBoxUI$DefaultKeySelectionManager",
            "javax.swing.plaf.basic.BasicListUI$ListTransferHandler",
            "javax.swing.plaf.basic.BasicTabbedPaneUI$TabbedPaneLayout",
            "javax.swing.plaf.basic.BasicTextUI$BasicHighlighter",
            "javax.swing.plaf.basic.BasicTextUI$TextTransferHandler",
            "javax.swing.plaf.basic.BasicTextUI$UpdateHandler",
            "javax.swing.plaf.basic.BasicTreeUI$NodeDimensionsHandler",
            "javax.swing.plaf.basic.BasicTreeUI$TreeTransferHandler",
            "javax.swing.plaf.basic.DefaultMenuLayout",
            "javax.swing.plaf.basic.LazyActionMap",
            "javax.swing.plaf.metal.MetalBorders$MenuBarBorder",
            "javax.swing.plaf.metal.MetalBorders$MenuItemBorder",
            "javax.swing.plaf.metal.MetalBorders$ScrollPaneBorder",
            "javax.swing.plaf.metal.MetalComboBoxUI$MetalComboBoxLayoutManager",
            "javax.swing.plaf.metal.MetalTabbedPaneUI$TabbedPaneLayout",
            "javax.swing.plaf.synth.SynthGraphicsUtils",
            "javax.swing.text.DefaultEditorKit$DefaultKeyTypedAction",
            "javax.swing.text.JTextComponent$DefaultKeymap",
            "javax.swing.text.JTextComponent$KeymapActionMap",
            "sun.swing.ImageIconUIResource",
            "sun.swing.PrintColorUIResource",
    };

    static String[] WINDOWS_REFLECTIVE_TYPES = {
    };

    static String[] LINUX_REFLECTIVE_TYPES = {
    };

    static String[] NEGATIVE_CLASS_LOOKUPS = {
            // Nimbus : NimbusDefaults looks up the class of each region and part of its skin, javax.swing.J<name> then
            // <name> (LazyStyle.Part), and uses the name when there is no such class
            "ArrowButton",
            "Button",
            "CheckBox",
            "CheckBoxMenuItem",
            "ColorChooser",
            "ComboBox",
            "DesktopIcon",
            "DesktopPane",
            "EditorPane",
            "FileChooser",
            "FormattedTextField",
            "InternalFrame",
            "InternalFrameTitlePane",
            "Label",
            "List",
            "Menu",
            "MenuBar",
            "MenuItem",
            "MenuItemAccelerator",
            "OptionPane",
            "Panel",
            "PasswordField",
            "PopupMenu",
            "PopupMenuSeparator",
            "ProgressBar",
            "RadioButton",
            "RadioButtonMenuItem",
            "RootPane",
            "ScrollBar",
            "ScrollBarThumb",
            "ScrollBarTrack",
            "ScrollPane",
            "Separator",
            "Slider",
            "SliderThumb",
            "SliderTrack",
            "Spinner",
            "SplitPane",
            "SplitPaneDivider",
            "TabbedPane",
            "TabbedPaneContent",
            "TabbedPaneTab",
            "TabbedPaneTabArea",
            "Table",
            "TableHeader",
            "TextArea",
            "TextField",
            "TextPane",
            "ToggleButton",
            "ToolBar",
            "ToolBarSeparator",
            "ToolTip",
            "Tree",
            "TreeCell",
            "Viewport",
            "javax.swing.JArrowButton",
            "javax.swing.JDesktopIcon",
            "javax.swing.JInternalFrameTitlePane",
            "javax.swing.JMenuItemAccelerator",
            "javax.swing.JPopupMenuSeparator",
            "javax.swing.JScrollBarThumb",
            "javax.swing.JScrollBarTrack",
            "javax.swing.JSliderThumb",
            "javax.swing.JSliderTrack",
            "javax.swing.JSplitPaneDivider",
            "javax.swing.JTabbedPaneContent",
            "javax.swing.JTabbedPaneTab",
            "javax.swing.JTabbedPaneTabArea",
            "javax.swing.JTableHeader",
            "javax.swing.JToolBarSeparator",
            "javax.swing.JTreeCell",
    };

    static String[] WINDOWS_NEGATIVE_CLASS_LOOKUPS = {
    };

    static String[] LINUX_NEGATIVE_CLASS_LOOKUPS = {
    };

    static String[] METHOD_LOOKUPS = {
            // text components : JTextComponent checks whether the class of a text component, or one of its superclasses
            // below JTextComponent, declares processInputMethodEvent (then it sends no KEY_TYPED events for committed
            // text). The text components of the JDK that do not declare it (JFormattedTextField does)
            "com.sun.java.swing.plaf.motif.MotifFileChooserUI$3#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "com.sun.java.swing.plaf.motif.MotifFileChooserUI$5#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.JEditorPane#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.JPasswordField#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.JTextArea#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.JTextField#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.JTextPane#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.plaf.basic.BasicComboBoxEditor$BorderlessTextField#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.plaf.basic.BasicOptionPaneUI$MultiplexingTextField#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.plaf.metal.MetalComboBoxEditor$1#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.plaf.metal.MetalFileChooserUI$3#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.plaf.synth.SynthTreeUI$SynthTreeCellEditor$1#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.text.html.FrameView$FrameEditorPane#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "javax.swing.tree.DefaultTreeCellEditor$DefaultTextField#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "sun.swing.plaf.synth.SynthFileChooserUIImpl$3#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "sun.swing.text.TextComponentPrintable$3#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "sun.swing.text.TextComponentPrintable$4#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "sun.swing.text.TextComponentPrintable$5#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "sun.swing.text.TextComponentPrintable$6#processInputMethodEvent(java.awt.event.InputMethodEvent)",
            "sun.swing.text.TextComponentPrintable$7#processInputMethodEvent(java.awt.event.InputMethodEvent)",
    };

    static String[] WINDOWS_METHOD_LOOKUPS = {
            // text components (see METHOD_LOOKUPS) of the Windows look and feel
            "com.sun.java.swing.plaf.windows.WindowsFileChooserUI$7#processInputMethodEvent(java.awt.event.InputMethodEvent)",
    };

    static String[] LINUX_METHOD_LOOKUPS = {
            // text components (see METHOD_LOOKUPS) of the GTK look and feel
            "com.sun.java.swing.plaf.gtk.GTKFileChooserUI$3#processInputMethodEvent(java.awt.event.InputMethodEvent)",
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

            // Motif look and feel (installed on every platform ; on Linux, the Desktop AWT extension registers it too)
            "com.sun.java.swing.plaf.motif.resources.motif",
    };

    static String[] WINDOWS_RESOURCE_BUNDLES = {
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

            // Motif look and feel (installed on every platform ; on Linux, the Desktop AWT extension registers them too)
            "com/sun/java/swing/plaf/motif/icons/*",
    };

    static String[] WINDOWS_RESOURCE_GLOBS = {
            // Windows look and feel (fallbacks of the shell icons)
            "com/sun/java/swing/plaf/windows/icons/*",
    };

    static String[] LINUX_RESOURCE_GLOBS = {
            // GTK look and feel : icons, and the theme of the window decorations drawn by the look and feel (Metacity)
            "com/sun/java/swing/plaf/gtk/icons/*",
            "com/sun/java/swing/plaf/gtk/resources/metacity/**",
    };

    // Resources that the JDK looks up and expects not to find : registered for --exact-reachability-metadata only (a
    // lookup of a resource that is not registered fails there, instead of finding nothing).

    static String[] RESOURCE_LOOKUPS = {
            // icons of the basic look and feel (html.pendingImage and html.missingImage of the HTML views, option pane,
            // file chooser...) : SwingUtilities2.makeIcon looks them up in the package of each look and feel class first,
            // up to BasicLookAndFeel. Nimbus and Synth have no icons ; the GTK look and feel (Linux) resolves the defaults
            // of Synth through UIManager too
            "javax/swing/plaf/nimbus/icons/*",
            "javax/swing/plaf/synth/icons/*",
    };

    static String[] WINDOWS_RESOURCE_LOOKUPS = {
    };

    static String[] LINUX_RESOURCE_LOOKUPS = {
    };

    // ----------------------------------------------------------------------------------------------------------- macOS
    // The Aqua look and feel is registered by the Desktop AWT extension on macOS (the AWT components are drawn by Aqua
    // delegates) : only what the Swing API alone reaches is here.

    static String[] MAC_RUNTIME_INITIALIZED_PACKAGES = {
    };

    static String[] MAC_RUNTIME_INITIALIZED_CLASSES = {
    };

    static String[] MAC_REFLECTIVE_CLASSES = {
    };

    static String[] MAC_REFLECTIVE_FIELD_CLASSES = {
    };

    static String[] MAC_REFLECTIVE_CONSTRUCTORS = {
    };

    static String[] MAC_REFLECTIVE_METHODS = {
    };

    static String[] MAC_REFLECTIVE_FIELDS = {
    };

    static String[] MAC_REFLECTIVE_PUBLIC_MEMBERS = {
    };

    static String[] MAC_JAVA_BEANS_CLASSES = {
    };

    static String[] MAC_REFLECTIVE_TYPES = {
    };

    static String[] MAC_NEGATIVE_CLASS_LOOKUPS = {
    };

    static String[] MAC_METHOD_LOOKUPS = {
    };

    static String[] MAC_JNI_RUNTIME_ACCESS_CLASSES = {
    };

    static String[] MAC_JNI_RUNTIME_ACCESS_METHODS = {
            // screen menu bar (apple.laf.useScreenMenuBar=true : a JMenuBar shown in the macOS menu bar) : libosxui calls
            // back into ScreenMenu when a menu opens, closes, or an item is highlighted or clicked
            "com.apple.laf.ScreenMenu#handleItemTargeted(int,int,int,int,int)",
            "com.apple.laf.ScreenMenu#handleMouseEvent(int,int,int,int,long)",
            "com.apple.laf.ScreenMenu#invokeMenuClosing()",
            "com.apple.laf.ScreenMenu#invokeOpenLater()",
    };

    static String[] MAC_JNI_RUNTIME_ACCESS_FIELDS = {
    };

    static String[] MAC_SERVICE_PROVIDERS = {
    };

    static String[] MAC_RESOURCE_BUNDLES = {
    };

    static String[] MAC_RESOURCE_GLOBS = {
    };

    static String[] MAC_RESOURCE_LOOKUPS = {
            // icons of the basic look and feel that Aqua does not override (html.pendingImage and html.missingImage of the
            // HTML views) : SwingUtilities2.makeIcon looks them up in the package of AquaLookAndFeel first, which has no
            // icons directory (the module glob belongs to java.desktop, the module of its parent directory)
            "com/apple/laf/icons/*",
    };
}
