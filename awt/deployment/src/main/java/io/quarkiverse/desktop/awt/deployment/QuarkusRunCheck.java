package io.quarkiverse.desktop.awt.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Checks that {@code Quarkus.run(Class, BiConsumer, String...)} of the Quarkus version of the build is still the method
 * that {@code io.quarkiverse.desktop.awt.runtime.macos.MacMainThread} reproduces when it replaces it in macOS native
 * executables.
 */
final class QuarkusRunCheck {

    static final String QUARKUS_CLASS = "io/quarkus/runtime/Quarkus.class";
    static final String RUN_DESCRIPTOR = "(Ljava/lang/Class;Ljava/util/function/BiConsumer;[Ljava/lang/String;)V";

    /**
     * The steps of the method, in order.
     */
    static final List<String> EXPECTED_STEPS = List.of(
            "ldc io.quarkus.runner.ApplicationImpl",
            "invoke java/lang/Class.forName",
            "invoke java/lang/invoke/MethodHandles$Lookup.findConstructor",
            "invoke io/quarkus/runtime/ApplicationLifecycleManager.run");

    private QuarkusRunCheck() {
    }

    /**
     * The expected steps that {@code Quarkus.run} of the given class loader no longer has (in order), empty when it is
     * unchanged.
     */
    static List<String> missingSteps(ClassLoader classLoader) throws IOException {
        try (InputStream in = classLoader.getResourceAsStream(QUARKUS_CLASS)) {
            if (in == null) {
                return List.of(QUARKUS_CLASS + " not found");
            }
            return missingSteps(in.readAllBytes());
        }
    }

    /**
     * The expected steps that {@code Quarkus.run} of the given class file no longer has (in order), empty when it is
     * unchanged.
     */
    static List<String> missingSteps(byte[] quarkusClass) {
        List<String> steps = new ArrayList<>();
        boolean[] found = new boolean[1];
        new ClassReader(quarkusClass).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                if (!name.equals("run") || !descriptor.equals(RUN_DESCRIPTOR)) {
                    return null;
                }
                found[0] = true;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String string) {
                            steps.add("ldc " + string);
                        }
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                            boolean isInterface) {
                        steps.add("invoke " + owner + "." + name);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (!found[0]) {
            return List.of("Quarkus.run" + RUN_DESCRIPTOR + " not found");
        }
        List<String> missing = new ArrayList<>();
        int index = 0;
        for (String expected : EXPECTED_STEPS) {
            int at = steps.subList(index, steps.size()).indexOf(expected);
            if (at < 0) {
                missing.add(expected);
            } else {
                index += at + 1;
            }
        }
        return missing;
    }
}
