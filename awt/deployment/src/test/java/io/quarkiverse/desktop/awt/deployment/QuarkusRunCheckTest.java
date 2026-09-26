package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The check of {@code Quarkus.run}, which the extension replaces in macOS native executables.
 */
class QuarkusRunCheckTest {

    /**
     * {@code io.quarkiverse.desktop.awt.runtime.macos.MacMainThread.runQuarkusHere} reproduces {@code Quarkus.run} of
     * the Quarkus version of the build.
     */
    @Test
    void quarkusOfTheBuild() throws IOException {
        assertEquals(List.of(), QuarkusRunCheck.missingSteps(getClass().getClassLoader()));
    }

    @Test
    void changedRun() {
        assertEquals(List.of("invoke java/lang/invoke/MethodHandles$Lookup.findConstructor",
                "invoke io/quarkus/runtime/ApplicationLifecycleManager.run"),
                QuarkusRunCheck.missingSteps(quarkusClass(QuarkusRunCheck.RUN_DESCRIPTOR, true)));
    }

    @Test
    void missingRun() {
        assertEquals(List.of("Quarkus.run" + QuarkusRunCheck.RUN_DESCRIPTOR + " not found"),
                QuarkusRunCheck.missingSteps(quarkusClass("([Ljava/lang/String;)V", false)));
    }

    @Test
    void missingClass() throws IOException {
        ClassLoader empty = new ClassLoader(null) {
        };
        assertEquals(List.of(QuarkusRunCheck.QUARKUS_CLASS + " not found"), QuarkusRunCheck.missingSteps(empty));
    }

    /**
     * A {@code Quarkus} class whose {@code run} method loads the application class, then creates it with reflection
     * (no method handle).
     */
    private static byte[] quarkusClass(String runDescriptor, boolean reflection) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "io/quarkus/runtime/Quarkus", null, "java/lang/Object", null);
        MethodVisitor run = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VARARGS, "run",
                runDescriptor, null, null);
        run.visitCode();
        run.visitLdcInsn("io.quarkus.runner.ApplicationImpl");
        run.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;",
                false);
        if (reflection) {
            run.visitInsn(Opcodes.ICONST_0);
            run.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
            run.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructor",
                    "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;", false);
        }
        run.visitInsn(Opcodes.POP);
        run.visitInsn(Opcodes.RETURN);
        run.visitMaxs(0, 0);
        run.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
