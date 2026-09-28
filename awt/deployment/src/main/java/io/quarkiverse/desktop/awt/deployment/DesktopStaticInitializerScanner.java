package io.quarkiverse.desktop.awt.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Finds the classes whose static initializer uses the JDK desktop modules (AWT, Java2D, fonts, ImageIO, printing,
 * sound, accessibility, Swing), directly or through the methods it calls.
 * <p>
 * The JDK desktop classes are initialized at run time in native mode, while Quarkus initializes the application classes
 * at build time. A class initialized at build time creating desktop objects in its static initializer (e.g.
 * {@code static final Color ACCENT = new Color(0x0096c9)}, {@code static final Font TITLE}, a {@code static} icon or
 * border) would initialize run time classes in the native image builder, which fails the native build. Such classes are
 * initialized at run time too, as in JVM mode.
 * <p>
 * Port of the {@code FxStaticInitializerScanner} of the Quarkus FX extension, with the desktop packages.
 */
final class DesktopStaticInitializerScanner {

    private static final Logger LOGGER = Logger.getLogger(DesktopStaticInitializerScanner.class);

    private static final String CLINIT = "<clinit>()V";

    /**
     * Internal name prefixes of the packages of the JDK desktop modules : the packages initialized at run time by the
     * extension ({@code RUNTIME_INITIALIZED_PACKAGES} of {@link AwtClassesAndResources}) and by {@code quarkus-awt}.
     */
    static final List<String> DESKTOP_PACKAGES = List.of("java/awt/", "javax/swing/", "sun/awt/", "sun/java2d/",
            "sun/font/", "javax/imageio/", "com/sun/imageio/", "javax/print/", "sun/print/", "javax/sound/",
            "com/sun/media/sound/", "javax/accessibility/", "com/sun/accessibility/", "com/sun/java/accessibility/",
            "sun/swing/", "com/sun/swing/", "com/sun/java/swing/", "jdk/swing/interop/", "sun/datatransfer/",
            // applets (deprecated, still in java.desktop : Applet.newAudioClip, an Applet is an AWT Panel)
            "java/applet/",
            // macOS : the toolkit, the Aqua look and feel, the application events, the dock and the screen menu bar
            "sun/lwawt/", "com/apple/", "apple/laf/");

    /**
     * GraalVM substitutions are never initialized.
     */
    private static final DotName TARGET_CLASS = DotName.createSimple("com.oracle.svm.core.annotate.TargetClass");

    private final Set<String> scannedClasses = new HashSet<>();
    // method ("owner.name descriptor") -> methods of scanned classes it invokes, or whose class it initializes
    private final Map<String, Set<String>> edges = new HashMap<>();
    private final Set<String> usingDesktop = new HashSet<>();

    private DesktopStaticInitializerScanner() {
    }

    /**
     * @return the names of the classes to initialize at run time
     */
    static Set<String> scan(Iterable<ClassInfo> classes, ClassLoader classLoader) {
        DesktopStaticInitializerScanner scanner = new DesktopStaticInitializerScanner();
        Map<String, byte[]> bytecode = new HashMap<>();
        for (ClassInfo classInfo : classes) {
            String internalName = classInfo.name().toString().replace('.', '/');
            if (isDesktop(internalName) || classInfo.hasDeclaredAnnotation(TARGET_CLASS)) {
                continue;
            }
            try (InputStream in = classLoader.getResourceAsStream(internalName + ".class")) {
                if (in != null) {
                    bytecode.put(internalName, in.readAllBytes());
                }
            } catch (IOException e) {
                LOGGER.debugf(e, "Unable to read %s", internalName);
            }
        }
        scanner.scannedClasses.addAll(bytecode.keySet());
        bytecode.forEach(scanner::visit);
        return scanner.classesToInitializeAtRunTime();
    }

    private void visit(String owner, byte[] bytes) {
        try {
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName,
                        String[] interfaces) {
                    // Initializing a class initializes its super class first
                    if (superName != null) {
                        classInitialization(owner + "." + CLINIT, superName);
                    }
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                    return new InstructionVisitor(owner + "." + name + descriptor);
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (RuntimeException e) {
            // e.g. class file version not supported by ASM
            LOGGER.debugf(e, "Unable to scan %s", owner);
        }
    }

    private final class InstructionVisitor extends MethodVisitor {

        private final String method;

        InstructionVisitor(String method) {
            super(Opcodes.ASM9);
            this.method = method;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            reference(owner);
            reference(descriptor);
            edge(owner + "." + name + descriptor);
            if (opcode == Opcodes.INVOKESTATIC) {
                classInitialization(method, owner);
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            reference(owner);
            reference(descriptor);
            if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
                classInitialization(method, owner);
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            reference(type);
            if (opcode == Opcodes.NEW) {
                classInitialization(method, type);
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            constant(value);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            reference(descriptor);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                Object... bootstrapMethodArguments) {
            reference(descriptor);
            for (Object argument : bootstrapMethodArguments) {
                constant(argument);
            }
        }

        private void constant(Object value) {
            if (value instanceof Type type) {
                reference(type.getDescriptor());
            } else if (value instanceof Handle handle) {
                reference(handle.getOwner());
                reference(handle.getDesc());
            } else if (value instanceof ConstantDynamic constantDynamic) {
                reference(constantDynamic.getDescriptor());
            }
        }

        private void reference(String nameOrDescriptor) {
            if (isDesktop(nameOrDescriptor)) {
                usingDesktop.add(method);
            }
        }

        private void edge(String callee) {
            edges.computeIfAbsent(method, k -> new HashSet<>()).add(callee);
        }
    }

    private void classInitialization(String method, String initializedClass) {
        if (!method.startsWith(initializedClass + ".") && scannedClasses.contains(initializedClass)) {
            edges.computeIfAbsent(method, k -> new HashSet<>()).add(initializedClass + "." + CLINIT);
        }
    }

    private Set<String> classesToInitializeAtRunTime() {
        // Propagate "uses the desktop modules" from callees to callers
        Map<String, Set<String>> callers = new HashMap<>();
        edges.forEach((caller, callees) -> callees
                .forEach(callee -> callers.computeIfAbsent(callee, k -> new HashSet<>()).add(caller)));
        Deque<String> queue = new ArrayDeque<>(usingDesktop);
        while (!queue.isEmpty()) {
            for (String caller : callers.getOrDefault(queue.poll(), Set.of())) {
                if (usingDesktop.add(caller)) {
                    queue.add(caller);
                }
            }
        }
        Set<String> result = new TreeSet<>();
        for (String method : usingDesktop) {
            if (method.endsWith("." + CLINIT)) {
                result.add(method.substring(0, method.length() - CLINIT.length() - 1).replace('/', '.'));
            }
        }
        return result;
    }

    private static boolean isDesktop(String nameOrDescriptor) {
        for (String desktopPackage : DESKTOP_PACKAGES) {
            if (nameOrDescriptor.startsWith(desktopPackage) || nameOrDescriptor.contains("L" + desktopPackage)) {
                return true;
            }
        }
        return false;
    }
}
