package io.quarkiverse.desktop.awt.deployment;

import java.util.List;

/**
 * What AWT needs in a native executable, on top of what {@code io.quarkus:quarkus-awt} (headless Java2D, ImageIO and
 * fonts) already registers : windows and peers, Java2D surfaces, printing, data transfer, input methods, sound and
 * accessibility. AWT itself uses parts of Swing (print dialogs, input method windows, the Linux text peers) : those
 * registrations belong here too, not in the Swing extension.
 * <p>
 * <b>Lists.</b> Each kind of registration has a list for all platforms ({@code KIND}) and one list per platform
 * ({@code WINDOWS_KIND}, {@code LINUX_KIND}, {@code MAC_KIND}) : a native executable gets the common list and the list
 * of the platform it is built for. A platform list only holds what that platform needs alone : an entry needed on every
 * platform belongs to the common list. Within a list, entries are grouped by area, with a short comment per group, and
 * sorted within a group. A list is a package-private {@code static String[]} field named {@code [PLATFORM_]KIND} : tools
 * (for instance the showcase metadata diff) read these fields reflectively. Any other constant of this class is a
 * {@code static final String} (or not a {@code String[]}), which these tools ignore.
 * <p>
 * <b>Target platform.</b> Windows when the build host is Windows and the build is not a container build (the rule of
 * {@code io.quarkus:quarkus-awt}), Linux otherwise. There is no macOS target : {@code io.quarkus:quarkus-awt} fails
 * native builds on macOS, so the {@code MAC_} lists are not declared.
 * <p>
 * <b>Kinds and entry formats.</b> Class names are binary names ({@code java.awt.Component$FlipBufferStrategy}).
 * Parameter types are binary names, primitive type names, or either followed by {@code []} for arrays; {@code ()}
 * means no parameters; a constructor is named {@code <init>}.
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
 * a JDK bundle from its package).</li>
 * <li>{@code RESOURCE_GLOBS} : resources included in the executable, as glob patterns.</li>
 * <li>{@code SERVICE_PROVIDERS} : service provider classes of JDK modules, registered for reflection (constructors and
 * methods). The module service catalog is always kept by native-image, so this is enough even though Quarkus disables
 * the GraalVM service loader feature.</li>
 * </ul>
 * <b>Sources.</b> The native methods of the JDK 25 desktop modules (the classes declaring native methods are
 * registered for JNI with all their members, since their native code calls back into them), the class names in the
 * JDK 25 native sources, and the tracing agent run on Windows and Linux (Xvfb) with an AWT application exercising
 * windows, peers, menus, dialogs, file dialogs, Java2D (buffered images, volatile images, buffer strategies, XOR mode),
 * fonts, images, cursors, Robot, clipboard, printing to a PostScript stream, Desktop, Taskbar, SystemTray and sound.
 * The exceptions thrown by the Windows and Linux native code of {@code java.desktop} ({@code JNU_Throw*},
 * {@code FindClass} and {@code ThrowNew}, exceptions created with a constructor) were audited against what GraalVM
 * registers, as were the members that the native code looks up on a class and that the class inherits.
 */
public final class AwtClassesAndResources {

    /**
     * The service provider of the Java Access Bridge (Windows, module {@code jdk.accessibility}), registered when the
     * Java Access Bridge is enabled.
     */
    static final String ACCESS_BRIDGE_PROVIDER = "com.sun.java.accessibility.internal.ProviderImpl";

    /**
     * The Java Access Bridge : its native code calls about 150 of its methods, registered when the Java Access Bridge
     * is enabled.
     */
    static final String ACCESS_BRIDGE = "com.sun.java.accessibility.internal.AccessBridge";

    /**
     * Read by reflection by the Java Access Bridge (its roles by name).
     */
    static final String ACCESSIBLE_ROLE = "javax.accessibility.AccessibleRole";

    /**
     * Registered for serialization : the clipboard and drag and drop copy the serializable data they transfer within the
     * application (sun.awt.datatransfer.TransferableProxy), and strings are the common case (DataFlavor.stringFlavor).
     */
    static final String TRANSFERRED_SERIALIZABLE_CLASS = "java.lang.String";

    /**
     * Registered for serialization : the text attributes (keys of the attributes of fonts and attributed strings,
     * serialized with a {@code readResolve} method that returns the constant), and the attribute class they extend,
     * whose class descriptor is serialized with them.
     */
    static final List<String> TEXT_ATTRIBUTE_SERIALIZABLE_CLASSES = List.of("java.awt.font.TextAttribute",
            "java.text.AttributedCharacterIterator$Attribute");

    private AwtClassesAndResources() {
        // Constants
    }

    // ----------------------------------------------------------------------------------------- run time initialization
    // Quarkus initializes every class at build time unless told otherwise. These classes are initialized at run time :
    // their static initializer loads native libraries or creates native state, starts threads, depends on the running
    // platform or toolkit, or reads system properties (their values would be frozen at build time). quarkus-awt already
    // initializes java.awt, javax.imageio, sun.awt, sun.datatransfer, sun.font, sun.java2d and com.sun.imageio at run time.

    static String[] RUNTIME_INITIALIZED_PACKAGES = {
            // Swing : AWT uses it (print and page dialogs, input method windows, the Linux text component peers)
            "com.sun.java.swing",
            "com.sun.swing",
            "javax.swing",
            "jdk.swing.interop",
            "sun.swing",

            // printing
            "javax.print",
            "sun.print",

            // sound
            "com.sun.media.sound",
            "javax.sound",

            // accessibility
            "com.sun.accessibility",
            "com.sun.java.accessibility",
            "javax.accessibility",

            // applets (deprecated, still in java.desktop)
            "java.applet",
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
            // key strokes ("ctrl C") are parsed with reflection on the VK_ fields : AWTKeyStroke, the key bindings of the
            // look and feels, the mnemonics of the print dialog
            "java.awt.event.KeyEvent",
            // system colors by name (BasicLookAndFeel)
            "java.awt.SystemColor",
    };

    static String[] WINDOWS_REFLECTIVE_CLASSES = {
    };

    static String[] LINUX_REFLECTIVE_CLASSES = {
    };

    static String[] REFLECTIVE_CONSTRUCTORS = {
            // AWT events
            "java.awt.SequencedEvent",

            // data transfer : representation classes of the data flavors (DataFlavor loads them by name)
            "byte[]",
            "char[]",
            "java.awt.Image",
            "java.io.InputStream",
            "java.io.Reader",
            "java.lang.String",
            "java.net.URL",
            "java.nio.ByteBuffer",
            "java.nio.CharBuffer",
            "java.rmi.Remote",
            "java.util.List",

            // fonts : charsets of the logical fonts (FontConfiguration loads them by name)
            "sun.awt.Symbol",

            // images : operations accelerated by mlib_image (ImagingLib loads them by name)
            "java.awt.image.AffineTransformOp",
            "java.awt.image.ConvolveOp",
            "java.awt.image.LookupOp",

            // ImageIO
            "javax.imageio.spi.ImageReaderSpi",
            "javax.imageio.spi.ImageWriterSpi",

            // ImageIO : the service providers of the JDK plugins, looked up by name (the reader of a writer and the writer
            // of a reader : ImageIO.getImageReader(ImageWriter), ImageIO.getImageWriter(ImageReader))
            "com.sun.imageio.plugins.bmp.BMPImageReaderSpi",
            "com.sun.imageio.plugins.bmp.BMPImageWriterSpi",
            "com.sun.imageio.plugins.gif.GIFImageReaderSpi",
            "com.sun.imageio.plugins.gif.GIFImageWriterSpi",
            "com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi",
            "com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi",
            "com.sun.imageio.plugins.png.PNGImageReaderSpi",
            "com.sun.imageio.plugins.png.PNGImageWriterSpi",
            "com.sun.imageio.plugins.tiff.TIFFImageReaderSpi",
            "com.sun.imageio.plugins.tiff.TIFFImageWriterSpi",
            "com.sun.imageio.plugins.wbmp.WBMPImageReaderSpi",
            "com.sun.imageio.plugins.wbmp.WBMPImageWriterSpi",

            // Java2D : the rendering engine and the general XOR loops (loaded by name ; quarkus-awt registers the others)
            "sun.java2d.loops.XorCopyArgbToAny",
            "sun.java2d.loops.XorDrawGlyphListAAANY",
            "sun.java2d.loops.XorDrawGlyphListANY",
            "sun.java2d.loops.XorDrawLineANY",
            "sun.java2d.loops.XorDrawPathANY",
            "sun.java2d.loops.XorDrawPolygonsANY",
            "sun.java2d.loops.XorDrawRectANY",
            "sun.java2d.loops.XorFillPathANY",
            "sun.java2d.loops.XorFillRectANY",
            "sun.java2d.loops.XorFillSpansANY",
            "sun.java2d.marlin.DMarlinRenderingEngine",

            // printing : representation classes of the doc flavors (SimpleDoc loads them by name)
            "java.awt.image.renderable.RenderableImage",
            "java.awt.print.Pageable",
            "java.awt.print.Printable",

            // shell folders (the Linux manager, and the fallback of the Windows one)
            "sun.awt.shell.ShellFolderManager",
    };

    static String[] WINDOWS_REFLECTIVE_CONSTRUCTORS = {
            // fonts : charsets of the logical fonts (FontConfiguration loads them by name)
            "sun.awt.HKSCS",
            "sun.awt.windows.WingDings",

            // Java2D
            "sun.java2d.d3d.D3DSurfaceData$D3DWindowSurfaceData",

            // shell folders : the manager (loaded by name) runs the COM calls of Desktop and Taskbar on its thread
            "sun.awt.shell.Win32ShellFolderManager2",

            // peers looked up by name
            "sun.awt.windows.WButtonPeer",
            "sun.awt.windows.WCanvasPeer",
            "sun.awt.windows.WCheckboxPeer",
            "sun.awt.windows.WChoicePeer",
            "sun.awt.windows.WDialogPeer",
            "sun.awt.windows.WLabelPeer",
            "sun.awt.windows.WLightweightFramePeer",
            "sun.awt.windows.WListPeer",
            "sun.awt.windows.WScrollbarPeer",
            "sun.awt.windows.WTextAreaPeer",
            "sun.awt.windows.WTextFieldPeer",
    };

    static String[] LINUX_REFLECTIVE_CONSTRUCTORS = {
            // peers looked up by name
            "sun.awt.X11.XButtonPeer",
            "sun.awt.X11.XCanvasPeer",
            "sun.awt.X11.XCheckboxPeer",
            "sun.awt.X11.XChoicePeer",
            "sun.awt.X11.XChoicePeer$UnfurledChoice",
            "sun.awt.X11.XContentWindow",
            "sun.awt.X11.XDialogPeer",
            "sun.awt.X11.XFramePeer",
            "sun.awt.X11.XLabelPeer",
            "sun.awt.X11.XListPeer",
            "sun.awt.X11.XMenuBarPeer",
            "sun.awt.X11.XPanelPeer",
            "sun.awt.X11.XScrollPanePeer",
            "sun.awt.X11.XScrollPanePeer$XScrollPaneContentWindow",
            "sun.awt.X11.XScrollbarPeer",
            "sun.awt.X11.XTextAreaPeer",
            "sun.awt.X11.XTextFieldPeer",
            "sun.awt.X11.XWindowPeer",
    };

    static String[] REFLECTIVE_METHODS = {
            // data transfer : DataFlavorUtil supports RMI flavors with reflection, and fails (AssertionError) when the class
            // exists without these members
            "java.rmi.MarshalledObject#<init>(java.lang.Object)",
            "java.rmi.MarshalledObject#get()",

            // ImageIO : the service provider interfaces are introspected by name
            "javax.imageio.spi.ImageReaderWriterSpi#getFileSuffixes()",
            "javax.imageio.spi.ImageReaderWriterSpi#getFormatNames()",
            "javax.imageio.spi.ImageReaderWriterSpi#getMIMETypes()",

            // ImageIO : the metadata formats of the JDK plugins, loaded by name (ImageReaderWriterSpi.getImageMetadataFormat,
            // getStreamMetadataFormat, IIOMetadata.getMetadataFormat)
            "com.sun.imageio.plugins.bmp.BMPMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.gif.GIFImageMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.gif.GIFStreamMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.jpeg.JPEGImageMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.jpeg.JPEGStreamMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.png.PNGMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.tiff.TIFFImageMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.tiff.TIFFStreamMetadataFormat#getInstance()",
            "com.sun.imageio.plugins.wbmp.WBMPMetadataFormat#getInstance()",

            // Swing core, basic look and feel : UI delegates (UIDefaults creates them with reflection)
            "javax.swing.plaf.basic.BasicButtonUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicCheckBoxMenuItemUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicCheckBoxUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicColorChooserUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicComboBoxUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicDesktopIconUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicDesktopPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicEditorPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicFileChooserUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicFormattedTextFieldUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicInternalFrameUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicLabelUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicListUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicMenuBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicMenuItemUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicMenuUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicOptionPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicPanelUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicPasswordFieldUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicPopupMenuSeparatorUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicPopupMenuUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicProgressBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicRadioButtonMenuItemUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicRadioButtonUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicRootPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicScrollBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicScrollPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicSeparatorUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicSliderUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicSpinnerUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicSplitPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicTabbedPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicTableHeaderUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicTableUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicTextAreaUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicTextFieldUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicTextPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicToggleButtonUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicToolBarSeparatorUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicToolBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicToolTipUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicTreeUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.basic.BasicViewportUI#createUI(javax.swing.JComponent)",

            // Swing core, basic look and feel : key bindings (LazyActionMap loads them with reflection ; a missing one is
            // silently ignored and the keyboard actions of the component do nothing)
            "javax.swing.plaf.basic.BasicButtonListener#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicComboBoxUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicDesktopPaneUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicInternalFrameUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicLabelUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicListUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicMenuBarUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicMenuItemUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicMenuUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicOptionPaneUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicPopupMenuUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicRootPaneUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicScrollBarUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicScrollPaneUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicSliderUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicSpinnerUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicSplitPaneUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicTabbedPaneUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicTableHeaderUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicTableUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicToolBarUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",
            "javax.swing.plaf.basic.BasicTreeUI#loadActionMap(javax.swing.plaf.basic.LazyActionMap)",

            // Swing core, Metal look and feel (the default one) : UI delegates
            "javax.swing.plaf.metal.MetalButtonUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalCheckBoxUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalComboBoxUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalDesktopIconUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalFileChooserUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalInternalFrameUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalLabelUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalMenuBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalPopupMenuSeparatorUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalProgressBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalRadioButtonUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalRootPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalScrollBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalScrollPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalSeparatorUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalSliderUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalSplitPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalTabbedPaneUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalTextFieldUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalToggleButtonUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalToolBarUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalToolTipUI#createUI(javax.swing.JComponent)",
            "javax.swing.plaf.metal.MetalTreeUI#createUI(javax.swing.JComponent)",
    };

    static String[] WINDOWS_REFLECTIVE_METHODS = {
    };

    static String[] LINUX_REFLECTIVE_METHODS = {
            // printing : PSPrinterJob checks CUPS print services by reflection
            "sun.print.IPPPrintService#isPostscript()",

            // Swing, Motif look and feel : the text component peers use it (XAWTLookAndFeel) : UI delegates
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

    // ------------------------------------------------------------------------------------------------------------- JNI

    static String[] JNI_RUNTIME_ACCESS_CLASSES = {
            // Java Sound (natives of jsound)
            "com.sun.media.sound.DirectAudioDevice",
            "com.sun.media.sound.DirectAudioDeviceProvider",
            "com.sun.media.sound.DirectAudioDeviceProvider$DirectAudioDeviceInfo",
            "com.sun.media.sound.MidiInDevice",
            "com.sun.media.sound.MidiInDeviceProvider",
            "com.sun.media.sound.MidiOutDevice",
            "com.sun.media.sound.MidiOutDeviceProvider",
            "com.sun.media.sound.Platform",
            "com.sun.media.sound.PortMixer",
            "com.sun.media.sound.PortMixer$BoolCtrl",
            "com.sun.media.sound.PortMixer$CompCtrl",
            "com.sun.media.sound.PortMixer$FloatCtrl",
            "com.sun.media.sound.PortMixerProvider",
            "com.sun.media.sound.PortMixerProvider$PortMixerInfo",
            "javax.sound.sampled.Control",

            // AWT (natives and callbacks of the toolkit and the peers, exceptions thrown by native code)
            "java.awt.AWTError",
            "java.awt.AWTEvent",
            "java.awt.AWTException",
            "java.awt.Checkbox",
            "java.awt.CheckboxMenuItem",
            "java.awt.Choice",
            "java.awt.Component",
            "java.awt.Container",
            "java.awt.Cursor",
            "java.awt.Dialog",
            "java.awt.Dimension",
            "java.awt.DisplayMode",
            "java.awt.Event",
            "java.awt.Font",
            "java.awt.FontMetrics",
            "java.awt.Frame",
            "java.awt.Insets",
            "java.awt.Label",
            "java.awt.Menu",
            "java.awt.MenuBar",
            "java.awt.MenuItem",
            "java.awt.Point",
            "java.awt.ScrollPane",
            "java.awt.ScrollPaneAdjustable",
            "java.awt.Scrollbar",
            "java.awt.TextArea",
            "java.awt.Toolkit",
            "java.awt.TrayIcon",
            "java.awt.Window",
            "java.awt.Window$Type",

            // events created or read by native code
            "java.awt.event.InputEvent",
            "java.awt.event.KeyEvent",
            "java.awt.event.MouseEvent",

            // images
            "java.awt.image.ImagingOpException",

            // toolkit
            "sun.awt.AWTAutoShutdown",
            "sun.awt.DebugSettings",
            "sun.awt.FontDescriptor",
            "sun.awt.PlatformFont",
            "sun.awt.SunToolkit",

            // images (Toolkit JPEG decoder, volatile images)
            "sun.awt.image.ImageFormatException",
            "sun.awt.image.JPEGImageDecoder",
            "sun.awt.image.SunVolatileImage",
            "sun.awt.image.VolatileSurfaceManager",

            // fonts
            "sun.font.ColorGlyphSurfaceData",
            "sun.font.FileFontStrike",
            "sun.font.FontDesignMetrics",
            "sun.font.FontUtilities",
            "sun.font.NativeFont",
            "sun.font.NativeStrike",
            "sun.font.NullFontScaler",
            "sun.font.StrikeCache",
            "sun.font.SunFontManager",
            "sun.font.SunLayoutEngine",

            // Java2D loops
            "sun.java2d.loops.TransformBlit",

            // Java2D OpenGL pipeline (-Dsun.java2d.opengl=true)
            "sun.java2d.opengl.OGLContext",
            "sun.java2d.opengl.OGLMaskFill",
            "sun.java2d.opengl.OGLRenderQueue",
            "sun.java2d.opengl.OGLRenderer",
            "sun.java2d.opengl.OGLSurfaceData",
            "sun.java2d.opengl.OGLTextRenderer",

            // Java2D pipes
            "sun.java2d.pipe.BufferedRenderPipe",
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_CLASSES = {
            // Swing windows recognized by native code
            "com.sun.java.swing.plaf.windows.WindowsPopupWindow",

            // AWT (natives and callbacks of the toolkit and the peers, exceptions thrown by native code)
            "java.awt.Button",
            "java.awt.Canvas",
            "java.awt.CheckboxGroup",
            "java.awt.FileDialog",
            "java.awt.List",
            "java.awt.PopupMenu",
            "java.awt.SequencedEvent",
            "java.awt.SystemColor",
            "java.awt.Taskbar$State",

            // drag and drop
            "java.awt.dnd.InvalidDnDOperationException",

            // events created or read by native code
            "java.awt.event.ActionEvent",
            "java.awt.event.ComponentEvent",
            "java.awt.event.MouseWheelEvent",

            // printing
            "java.awt.print.PageFormat",
            "java.awt.print.Paper",
            "java.awt.print.PrinterException",

            // Swing windows recognized by native code
            "javax.swing.Popup$HeavyWeightWindow",

            // toolkit
            "sun.awt.EmbeddedFrame",
            "sun.awt.ExtendedKeyCodes",
            "sun.awt.LightweightFrame",
            "sun.awt.PlatformGraphicsInfo",
            "sun.awt.TimedWindowEvent",
            "sun.awt.UngrabEvent",

            // data transfer
            "sun.awt.datatransfer.DataTransferer",

            // input methods
            "sun.awt.im.InputMethodWindow",

            // shell folders (Desktop and Taskbar run their COM calls on the thread of the shell folder manager)
            "sun.awt.shell.ShellFolderColumnInfo",
            "sun.awt.shell.Win32ShellFolder2",
            "sun.awt.shell.Win32ShellFolder2$KnownFolderDefinition",
            "sun.awt.shell.Win32ShellFolderManager2",

            // toolkit and peers (WObjectPeer : what the removed quarkus-awt substitution of WObjectPeer.initIDs hid)
            "sun.awt.windows.TranslucentWindowPainter$VIOptWindowPainter$1",
            "sun.awt.windows.WButtonPeer",
            "sun.awt.windows.WCanvasPeer",
            "sun.awt.windows.WCheckboxMenuItemPeer",
            "sun.awt.windows.WCheckboxPeer",
            "sun.awt.windows.WChoicePeer",
            "sun.awt.windows.WClipboard",
            "sun.awt.windows.WCustomCursor",
            "sun.awt.windows.WEmbeddedFrame",
            "sun.awt.windows.WEmbeddedFramePeer",
            "sun.awt.windows.WFileDialogPeer",
            "sun.awt.windows.WFontMetrics",
            "sun.awt.windows.WFontPeer",
            "sun.awt.windows.WFramePeer",
            "sun.awt.windows.WGlobalCursorManager",
            "sun.awt.windows.WInputMethod",
            "sun.awt.windows.WInputMethodDescriptor",
            "sun.awt.windows.WKeyboardFocusManagerPeer",
            "sun.awt.windows.WLabelPeer",
            "sun.awt.windows.WLightweightFramePeer",
            "sun.awt.windows.WListPeer",
            "sun.awt.windows.WMenuBarPeer",
            "sun.awt.windows.WMenuItemPeer",
            "sun.awt.windows.WMenuPeer",
            "sun.awt.windows.WMouseInfoPeer",
            "sun.awt.windows.WObjectPeer",
            "sun.awt.windows.WPageDialog",
            "sun.awt.windows.WPageDialogPeer",
            "sun.awt.windows.WPanelPeer",
            "sun.awt.windows.WPopupMenuPeer",
            "sun.awt.windows.WPrintDialog",
            "sun.awt.windows.WPrintDialogPeer",
            "sun.awt.windows.WPrinterJob",
            "sun.awt.windows.WRobotPeer",
            "sun.awt.windows.WScrollPanePeer",
            "sun.awt.windows.WScrollbarPeer",
            "sun.awt.windows.WTaskbarPeer",
            "sun.awt.windows.WTextAreaPeer",
            "sun.awt.windows.WTextComponentPeer",
            "sun.awt.windows.WTextFieldPeer",
            "sun.awt.windows.WTrayIconPeer",
            "sun.awt.windows.WWindowPeer",

            // fonts (a Runnable run by the Direct3D render queue)
            "sun.font.StrikeCache$1",

            // Java2D Direct3D pipeline (the render queue runs Runnables from native code)
            "sun.java2d.d3d.D3DGraphicsDevice",
            "sun.java2d.d3d.D3DGraphicsDevice$1",
            "sun.java2d.d3d.D3DGraphicsDevice$2",
            "sun.java2d.d3d.D3DGraphicsDevice$3",
            "sun.java2d.d3d.D3DGraphicsDevice$4",
            "sun.java2d.d3d.D3DGraphicsDevice$5",
            "sun.java2d.d3d.D3DGraphicsDevice$6",
            "sun.java2d.d3d.D3DGraphicsDevice$7",
            "sun.java2d.d3d.D3DGraphicsDevice$8",
            "sun.java2d.d3d.D3DMaskFill",
            "sun.java2d.d3d.D3DRenderQueue",
            "sun.java2d.d3d.D3DRenderQueue$1",
            "sun.java2d.d3d.D3DRenderer",
            "sun.java2d.d3d.D3DRenderer$Tracer$1",
            "sun.java2d.d3d.D3DSurfaceData",
            "sun.java2d.d3d.D3DSurfaceData$1",
            "sun.java2d.d3d.D3DSurfaceData$2",
            "sun.java2d.d3d.D3DSurfaceData$D3DDataBufferNative$1",
            "sun.java2d.d3d.D3DSurfaceData$D3DDataBufferNative$2",
            "sun.java2d.d3d.D3DTextRenderer",

            // Java2D OpenGL pipeline (-Dsun.java2d.opengl=true)
            "sun.java2d.opengl.WGLGraphicsConfig",
            "sun.java2d.opengl.WGLSurfaceData",

            // Java2D pipes (Runnables run by the Direct3D render queue)
            "sun.java2d.pipe.BufferedMaskFill$1",
            "sun.java2d.pipe.BufferedRenderPipe$1",
            "sun.java2d.pipe.BufferedTextPipe$1",

            // Java2D GDI pipeline, and the flags (DPI awareness : what the removed quarkus-awt substitution of
            // WindowsFlags.initNativeFlags hid)
            "sun.java2d.windows.GDIBlitLoops",
            "sun.java2d.windows.GDIRenderer",
            "sun.java2d.windows.GDIWindowSurfaceData",
            "sun.java2d.windows.WindowsFlags",

            // printing
            "sun.print.PrintServiceLookupProvider",
            "sun.print.RasterPrinterJob",
            "sun.print.Win32PrintJob",
            "sun.print.Win32PrintService",
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_CLASSES = {
            // AWT (natives and callbacks of the toolkit and the peers, exceptions thrown by native code)
            "java.awt.Desktop$Action",

            // toolkit
            "sun.awt.FcFontManager",
            "sun.awt.UNIXToolkit",
            "sun.awt.X11GraphicsEnvironment",
            "sun.awt.X11InputMethodBase",

            // toolkit and peers
            "sun.awt.X11.GtkFileDialogPeer",
            "sun.awt.X11.XBaseWindow",
            "sun.awt.X11.XDesktopPeer",
            "sun.awt.X11.XEmbeddedFrame",
            "sun.awt.X11.XErrorHandlerUtil",
            "sun.awt.X11.XInputMethod",
            "sun.awt.X11.XRobotPeer",
            "sun.awt.X11.XRootWindow",
            "sun.awt.X11.XTaskbarPeer",
            "sun.awt.X11.XToolkit",
            "sun.awt.X11.XWindow",
            "sun.awt.X11.XWindowPeer",
            "sun.awt.X11.XlibWrapper",

            // Robot screen capture on Wayland (PipeWire)
            "sun.awt.screencast.ScreencastHelper",
            "sun.awt.screencast.TokenStorage",

            // fonts
            "sun.font.NativeStrikeDisposer",
            "sun.font.X11TextRenderer",

            // Java2D OpenGL pipeline (-Dsun.java2d.opengl=true)
            "sun.java2d.opengl.GLXGraphicsConfig",
            "sun.java2d.opengl.GLXSurfaceData",

            // Java2D X11 pipeline (-Dsun.java2d.xrender=false)
            "sun.java2d.x11.X11PMBlitBgLoops",
            "sun.java2d.x11.X11PMBlitLoops",
            "sun.java2d.x11.X11Renderer",
            "sun.java2d.x11.X11SurfaceData",
            "sun.java2d.x11.XSurfaceData",

            // Java2D XRender pipeline
            "sun.java2d.xr.XIDGenerator",
            "sun.java2d.xr.XRBackendNative",
            "sun.java2d.xr.XRMaskBlit",
            "sun.java2d.xr.XRMaskFill",

            // printing
            "sun.print.CUPSPrinter",
    };

    static String[] JNI_RUNTIME_ACCESS_METHODS = {
            // java.base methods called from native code
            "java.io.InputStream#available()",
            "java.io.InputStream#read(byte[],int,int)",
            "java.lang.Boolean#getBoolean(java.lang.String)",
            "java.lang.Enum#name()",
            "java.lang.Thread#currentThread()",
            "java.math.BigInteger#<init>(byte[])",
            "java.util.ArrayList#<init>(int)",
            "java.util.ArrayList#add(java.lang.Object)",

            // exceptions thrown by native code (JNU_ThrowByName, FindClass and ThrowNew, or created with a constructor) that
            // GraalVM does not register : it registers the (String) constructor of the common java.lang and java.io
            // exceptions only. Without them, a NoClassDefFoundError or a NoSuchMethodError replaces the exception
            "java.lang.IllegalArgumentException#<init>(java.lang.String,java.lang.Throwable)",
            "java.lang.IllegalStateException#<init>(java.lang.String)",
            "javax.sound.midi.MidiUnavailableException#<init>(java.lang.String)",

            // Java Sound : the controls of the ports (PortMixer.nGetControls) are added to a vector
            "java.util.Vector#addElement(java.lang.Object)",

            // Java2D pipes : the native span filler flushes the render queue (Direct3D, OpenGL) when it is full. The
            // method is inherited : the registration of the render queue classes does not cover it
            "sun.java2d.pipe.RenderQueue#flushNow(int)",
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_METHODS = {
            // java.base methods called from native code
            "java.lang.System#getProperty(java.lang.String)",
            "java.util.Locale#forLanguageTag(java.lang.String)",

            // exceptions created by native code (the error of a peer that could not be created)
            "java.lang.InternalError#<init>()",
            "java.lang.OutOfMemoryError#<init>()",

            // drag and drop : the native code calls these methods on the Windows peers (WDragSourceContextPeer,
            // WDropTargetContextPeer), which inherit them : the registration of the peer classes does not cover them
            "sun.awt.dnd.SunDragSourceContextPeer#dragDropFinished(boolean,int,int,int)",
            "sun.awt.dnd.SunDragSourceContextPeer#dragEnter(int,int,int,int)",
            "sun.awt.dnd.SunDragSourceContextPeer#dragExit(int,int)",
            "sun.awt.dnd.SunDragSourceContextPeer#dragMotion(int,int,int,int)",
            "sun.awt.dnd.SunDragSourceContextPeer#dragMouseMoved(int,int,int,int)",
            "sun.awt.dnd.SunDragSourceContextPeer#operationChanged(int,int,int,int)",
            "sun.awt.dnd.SunDropTargetContextPeer#handleDropMessage(java.awt.Component,int,int,int,int,long[],long)",
            "sun.awt.dnd.SunDropTargetContextPeer#handleEnterMessage(java.awt.Component,int,int,int,int,long[],long)",
            "sun.awt.dnd.SunDropTargetContextPeer#handleExitMessage(java.awt.Component,long)",
            "sun.awt.dnd.SunDropTargetContextPeer#handleMotionMessage(java.awt.Component,int,int,int,int,long[],long)",
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_METHODS = {
            // java.base methods called from native code
            "java.lang.Thread#yield()",
            "java.util.ArrayList#<init>()",
            "java.util.ArrayList#clear()",

            // GTK settings (desktop properties) returned as boxed values
            "java.lang.Boolean#<init>(boolean)",
            "java.lang.Character#<init>(char)",
            "java.lang.Double#<init>(double)",
            "java.lang.Float#<init>(float)",
            "java.lang.Integer#<init>(int)",
            "java.lang.Long#<init>(long)",
    };

    static String[] JNI_RUNTIME_ACCESS_FIELDS = {
    };

    static String[] WINDOWS_JNI_RUNTIME_ACCESS_FIELDS = {
    };

    static String[] LINUX_JNI_RUNTIME_ACCESS_FIELDS = {
    };

    // ----------------------------------------------------------------------------------------------- service providers

    static String[] SERVICE_PROVIDERS = {
            // data transfer (clipboard, drag and drop, SystemFlavorMap)
            "sun.awt.datatransfer.DesktopDatatransferServiceImpl",

            // printing
            "sun.print.PrintServiceLookupProvider",
            "sun.print.PSStreamPrinterFactory",

            // URL content handlers (images and audio)
            "sun.awt.www.content.MultimediaContentHandlers",

            // sound : mixers, codecs, file readers and writers, MIDI devices, files and sound banks
            "com.sun.media.sound.AiffFileReader",
            "com.sun.media.sound.AiffFileWriter",
            "com.sun.media.sound.AlawCodec",
            "com.sun.media.sound.AuFileReader",
            "com.sun.media.sound.AuFileWriter",
            "com.sun.media.sound.AudioFileSoundbankReader",
            "com.sun.media.sound.AudioFloatFormatConverter",
            "com.sun.media.sound.DLSSoundbankReader",
            "com.sun.media.sound.DirectAudioDeviceProvider",
            "com.sun.media.sound.JARSoundbankReader",
            "com.sun.media.sound.MidiInDeviceProvider",
            "com.sun.media.sound.MidiOutDeviceProvider",
            "com.sun.media.sound.PCMtoPCMCodec",
            "com.sun.media.sound.PortMixerProvider",
            "com.sun.media.sound.RealTimeSequencerProvider",
            "com.sun.media.sound.SF2SoundbankReader",
            "com.sun.media.sound.SoftMidiAudioFileReader",
            "com.sun.media.sound.SoftProvider",
            "com.sun.media.sound.StandardMidiFileReader",
            "com.sun.media.sound.StandardMidiFileWriter",
            "com.sun.media.sound.UlawCodec",
            "com.sun.media.sound.WaveExtensibleFileReader",
            "com.sun.media.sound.WaveFileReader",
            "com.sun.media.sound.WaveFileWriter",
            "com.sun.media.sound.WaveFloatFileReader",
            "com.sun.media.sound.WaveFloatFileWriter",
    };

    static String[] WINDOWS_SERVICE_PROVIDERS = {
    };

    static String[] LINUX_SERVICE_PROVIDERS = {
    };

    // ------------------------------------------------------------------------------------------------ resource bundles

    static String[] RESOURCE_BUNDLES = {
            // toolkit (key names, input methods)
            "sun.awt.resources.awt",

            // ImageIO : the descriptions of the metadata formats (IIOMetadataFormat.getElementDescription...) and the
            // warnings of the JPEG plugin
            "com.sun.imageio.plugins.bmp.BMPMetadataFormatResources",
            "com.sun.imageio.plugins.common.StandardMetadataFormatResources",
            "com.sun.imageio.plugins.gif.GIFImageMetadataFormatResources",
            "com.sun.imageio.plugins.gif.GIFStreamMetadataFormatResources",
            "com.sun.imageio.plugins.jpeg.JPEGImageMetadataFormatResources",
            "com.sun.imageio.plugins.jpeg.JPEGImageReaderResources",
            "com.sun.imageio.plugins.jpeg.JPEGImageWriterResources",
            "com.sun.imageio.plugins.jpeg.JPEGStreamMetadataFormatResources",
            "com.sun.imageio.plugins.png.PNGMetadataFormatResources",

            // print and page dialogs
            "sun.print.resources.serviceui",

            // accessibility (role, state and relation names)
            "com.sun.accessibility.internal.resources.accessibility",

            // Swing core (the basic and Metal look and feels of print dialogs and input method windows)
            "com.sun.swing.internal.plaf.basic.resources.basic",
            "com.sun.swing.internal.plaf.metal.resources.metal",
    };

    static String[] WINDOWS_RESOURCE_BUNDLES = {
            // file dialogs and menu shortcuts
            "sun.awt.windows.awtLocalization",
    };

    static String[] LINUX_RESOURCE_BUNDLES = {
            // the Motif look and feel of the text component peers
            "com.sun.java.swing.plaf.motif.resources.motif",
    };

    // ------------------------------------------------------------------------------------------------------- resources

    static String[] RESOURCE_GLOBS = {
            // drag and drop cursors
            "sun/awt/resources/cursors/*",

            // data transfer : the native formats of the data flavors (module java.datatransfer)
            "sun/datatransfer/resources/flavormap.properties",

            // print dialog icons
            "sun/print/resources/*.png",

            // bidirectional text and line breaks (module java.base)
            "jdk/internal/icu/impl/data/**/ubidi.icu",
            "sun/text/resources/LineBreakIteratorData",

            // Swing core : basic and Metal icons, HTML text (labels, buttons and tool tips accept HTML)
            "javax/swing/plaf/basic/icons/*",
            "javax/swing/plaf/metal/icons/**",
            "javax/swing/text/html/default.css",
            "javax/swing/text/html/parser/html32.bdtd",
    };

    static String[] WINDOWS_RESOURCE_GLOBS = {
    };

    static String[] LINUX_RESOURCE_GLOBS = {
            // the Motif look and feel of the text component peers
            "com/sun/java/swing/plaf/motif/icons/*",
    };
}
