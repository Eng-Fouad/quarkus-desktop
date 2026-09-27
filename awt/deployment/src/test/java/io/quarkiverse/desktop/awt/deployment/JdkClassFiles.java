package io.quarkiverse.desktop.awt.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The class files and resources of a JDK, read from its {@code lib/modules} image with the {@code jrt} file system of
 * that JDK, which works on any operating system : the entries of the lists of a platform can be checked against the JDK
 * of that platform (for instance a macOS JDK on Windows).
 */
final class JdkClassFiles implements Closeable {

    private static final List<String> PLATFORMS = List.of("WINDOWS_", "LINUX_", "MAC_");

    private static final Map<String, String> PRIMITIVES = Map.of("boolean", "Z", "byte", "B", "char", "C", "short", "S",
            "int", "I", "long", "J", "float", "F", "double", "D", "void", "V");

    private final FileSystem fileSystem;
    private final List<Path> modules;
    private final Map<String, Optional<Members>> classes = new HashMap<>();

    private record Members(Set<String> methods, Set<String> fields) {
    }

    private JdkClassFiles(FileSystem fileSystem, List<Path> modules) {
        this.fileSystem = fileSystem;
        this.modules = modules;
    }

    /**
     * Opens the class files of the JDK installed in the given directory (its {@code java.home}).
     */
    static JdkClassFiles open(Path javaHome) throws IOException {
        FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of("java.home", javaHome.toString()));
        try (Stream<Path> modules = Files.list(fileSystem.getPath("/modules"))) {
            return new JdkClassFiles(fileSystem, modules.toList());
        }
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    /**
     * Whether the image has the module (a JRE image may lack some modules of the JDK, such as
     * {@code jdk.unsupported.desktop}).
     */
    boolean hasModule(String name) {
        return modules.stream().anyMatch(module -> module.getFileName().toString().equals(name));
    }

    boolean hasClass(String binaryName) {
        return members(binaryName).isPresent();
    }

    /**
     * Whether the type of a list entry exists : a class, a primitive type, or an array of them.
     */
    boolean hasType(String type) {
        String element = type.replace("[]", "");
        return PRIMITIVES.containsKey(element) || hasClass(element);
    }

    boolean hasPackage(String packageName) {
        String directory = packageName.replace('.', '/');
        return modules.stream().anyMatch(module -> Files.isDirectory(module.resolve(directory)));
    }

    boolean hasBundle(String baseName) {
        return hasClass(baseName) || hasResource(baseName.replace('.', '/') + ".properties");
    }

    boolean hasResource(String path) {
        return modules.stream().anyMatch(module -> Files.isRegularFile(module.resolve(path)));
    }

    /**
     * Whether the resources of the JDK match a glob ({@code **} matches across directories, {@code *} within one).
     */
    boolean hasGlob(String glob) throws IOException {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                regex.append(".*");
                i++;
            } else if (c == '*') {
                regex.append("[^/]*");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        Pattern pattern = Pattern.compile(regex.toString());
        for (Path module : modules) {
            try (Stream<Path> files = Files.walk(module)) {
                if (files.filter(Files::isRegularFile).map(file -> module.relativize(file).toString().replace('\\', '/'))
                        .anyMatch(file -> pattern.matcher(file).matches())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the class declares the method (or constructor, {@code <init>}) of a list entry.
     */
    boolean hasMethod(MemberEntry method) {
        StringBuilder parameters = new StringBuilder("(");
        for (String type : method.parameterTypes()) {
            parameters.append(descriptor(type));
        }
        parameters.append(')');
        String prefix = method.name() + parameters;
        return members(method.className()).map(m -> m.methods().stream().anyMatch(d -> d.startsWith(prefix)))
                .orElse(false);
    }

    /**
     * Whether the class declares the field of a list entry.
     */
    boolean hasField(MemberEntry field) {
        return members(field.className()).map(m -> m.fields().contains(field.name())).orElse(false);
    }

    /**
     * The entries of the common lists and of the lists of the given platform (for instance {@code "MAC_"}) of a lists
     * class that this JDK does not have.
     */
    List<String> missingEntries(Class<?> listsClass, String platform) throws IOException, ReflectiveOperationException {
        List<String> errors = new ArrayList<>();
        for (Field field : listsClass.getDeclaredFields()) {
            String list = field.getName();
            String prefix = PLATFORMS.stream().filter(list::startsWith).findFirst().orElse("");
            if (field.getType() != String[].class || !(prefix.isEmpty() || prefix.equals(platform))) {
                continue;
            }
            String kind = list.substring(prefix.length());
            field.setAccessible(true);
            for (String entry : Arrays.asList((String[]) field.get(null))) {
                if (!exists(kind, entry)) {
                    errors.add(list + " : " + entry);
                }
            }
        }
        return errors;
    }

    private boolean exists(String kind, String entry) throws IOException {
        return switch (kind) {
            case "RUNTIME_INITIALIZED_PACKAGES" -> hasPackage(entry);
            case "RUNTIME_INITIALIZED_CLASSES", "REFLECTIVE_CLASSES", "REFLECTIVE_CONSTRUCTORS",
                    "REFLECTIVE_PUBLIC_MEMBERS", "JAVA_BEANS_CLASSES", "REFLECTIVE_TYPES", "JNI_RUNTIME_ACCESS_CLASSES",
                    "SERVICE_PROVIDERS" ->
                hasType(entry);
            // lookups expected to fail
            case "NEGATIVE_CLASS_LOOKUPS" -> !hasType(entry);
            // the class may not declare the method
            case "METHOD_LOOKUPS" -> hasType(MemberEntry.method(entry).className());
            case "REFLECTIVE_METHODS", "JNI_RUNTIME_ACCESS_METHODS" -> hasMethod(MemberEntry.method(entry));
            case "REFLECTIVE_FIELDS", "JNI_RUNTIME_ACCESS_FIELDS" -> hasField(MemberEntry.field(entry));
            case "RESOURCE_BUNDLES" -> hasBundle(entry);
            case "RESOURCE_GLOBS" -> hasGlob(entry);
            default -> throw new IllegalArgumentException("Unknown list kind " + kind);
        };
    }

    private static String descriptor(String type) {
        String element = type;
        StringBuilder descriptor = new StringBuilder();
        while (element.endsWith("[]")) {
            descriptor.append('[');
            element = element.substring(0, element.length() - 2);
        }
        String primitive = PRIMITIVES.get(element);
        return descriptor.append(primitive != null ? primitive : "L" + element.replace('.', '/') + ";").toString();
    }

    private Optional<Members> members(String binaryName) {
        return classes.computeIfAbsent(binaryName, name -> {
            String file = name.replace('.', '/') + ".class";
            for (Path module : modules) {
                Path path = module.resolve(file);
                if (Files.isRegularFile(path)) {
                    try (InputStream in = Files.newInputStream(path)) {
                        return Optional.of(read(in.readAllBytes()));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            return Optional.empty();
        });
    }

    private static Members read(byte[] classFile) {
        Set<String> methods = new HashSet<>();
        Set<String> fields = new HashSet<>();
        new ClassReader(classFile).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                methods.add(name + descriptor);
                return null;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                fields.add(name);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return new Members(methods, fields);
    }
}
