package io.quarkiverse.desktop.awt.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkiverse.desktop.awt.deployment.DesktopTargetPlatformBuildItem.Platform;
import io.quarkiverse.desktop.awt.runtime.DesktopAwtConfig;
import io.quarkiverse.desktop.awt.runtime.DesktopAwtRecorder;
import io.quarkiverse.desktop.awt.runtime.graal.DesktopAwtFeature;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.builder.Json;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.os.OS;

// NativeOrNativeSourcesBuild and NativeBuild are deprecated, but Quarkus core has no replacement yet
@SuppressWarnings("deprecation")
class DesktopAwtProcessor {

    private static final Logger LOGGER = Logger.getLogger(DesktopAwtProcessor.class);

    private static final String FEATURE = "desktop-awt";

    private static final String REASON = "Quarkus Desktop AWT";

    static final String QUARKUS_AWT_GROUP_ID = "io.quarkus";
    static final String QUARKUS_AWT_ARTIFACT_ID = "quarkus-awt";

    /**
     * The substitutions of {@code io.quarkus:quarkus-awt} (headless Java2D) that break AWT GUI applications or disable a
     * feature of AWT : the Windows AWT substitutions, and the Type 1 font substitution. The other substitutions (the
     * {@code JDKSubstitutions} marker class, which Quarkus core checks, the font configuration, input methods of JDK 21)
     * stay.
     */
    static final List<String> QUARKUS_AWT_GUI_BLOCKERS = List.of(
            // WObjectPeer.initIDs() does nothing : every heavyweight peer (window, component, tray icon) crashes
            "io/quarkus/awt/runtime/Target_sun_awt_windows_WObjectPeer.class",
            // WindowsFlags.initNativeFlags() returns false : no DPI awareness, Direct3D flags ignored
            "io/quarkus/awt/runtime/Target_sun_java2d_windows_WindowsFlags.class",
            // WToolkit.getPrintJob(...) throws : no AWT print jobs (Toolkit.getPrintJob)
            "io/quarkus/awt/runtime/Target_sun_awt_windows_WToolkit.class",
            // Type1Font.verifyPFA/verifyPFB throw (on every platform) : no Type 1 fonts (.pfa and .pfb files,
            // Font.createFont(Font.TYPE1_FONT, ...), and the Type 1 fonts of the system on Linux). Native executables
            // read them as the JDK does, with the freetype library of the JDK, which supports them, and the JNI callback
            // Type1Font.readFile that quarkus-awt registers
            "io/quarkus/awt/runtime/Target_sun_font_Type1Font.class");

    /**
     * The resource of the reflection configuration of the classes registered with their public members.
     */
    static final String PUBLIC_MEMBERS_REFLECT_CONFIG = "META-INF/native-image/io.quarkiverse.desktop/"
            + "quarkus-desktop-awt-public-members/reflect-config.json";

    private static final DotName COMPONENT = DotName.createSimple("java.awt.Component");
    private static final DotName AWT_EVENT = DotName.createSimple("java.awt.AWTEvent");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    // ------------------------------------------------------------------------------------------------------ quarkus-awt

    @BuildStep
    RemovedResourceBuildItem removeQuarkusAwtGuiBlockers(CurateOutcomeBuildItem curateOutcome) {
        ApplicationModel model = curateOutcome.getApplicationModel();
        ArtifactKey quarkusAwt = ArtifactKey.ga(QUARKUS_AWT_GROUP_ID, QUARKUS_AWT_ARTIFACT_ID);
        Optional<ResolvedDependency> dependency = model.getRuntimeDependencies().stream()
                .filter(d -> QUARKUS_AWT_GROUP_ID.equals(d.getGroupId()) && QUARKUS_AWT_ARTIFACT_ID.equals(d.getArtifactId()))
                .findFirst();
        if (dependency.isPresent()) {
            quarkusAwt = dependency.get().getKey();
            // The substitutions are private classes of quarkus-awt : one renamed or added would break GUI applications
            List<String> unknown = unknownSubstitutions(dependency.get());
            if (!unknown.isEmpty()) {
                LOGGER.warnf("%s %s has substitutions of the Windows AWT classes or of Type 1 fonts that Quarkus Desktop"
                        + " AWT does not know : %s. They may break AWT GUI applications in native executables (for"
                        + " instance make AWT windows crash on Windows) : please report it to the Quarkus Desktop project.",
                        quarkusAwt.toGacString(), dependency.get().getVersion(), unknown);
            }
        }
        return new RemovedResourceBuildItem(quarkusAwt, Set.copyOf(QUARKUS_AWT_GUI_BLOCKERS));
    }

    /**
     * The substitutions of quarkus-awt that are not removed, of JDK Windows classes ({@code sun.awt.windows},
     * {@code sun.java2d.windows}) or of the Type 1 fonts ({@code sun.font.Type1Font}).
     */
    static List<String> unknownSubstitutions(ResolvedDependency dependency) {
        List<String> unknown = new ArrayList<>();
        try {
            dependency.getContentTree().walk(visit -> {
                String name = visit.getRelativePath("/");
                if (name.startsWith("io/quarkus/awt/runtime/Target_")
                        && (name.contains("_windows_") || name.contains("Type1Font"))
                        && name.endsWith(".class") && !QUARKUS_AWT_GUI_BLOCKERS.contains(name)) {
                    unknown.add(name);
                }
            });
        } catch (RuntimeException e) {
            LOGGER.debugf(e, "Unable to read %s", dependency);
        }
        return unknown;
    }

    // ------------------------------------------------------------------------------------------------ target platform

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    DesktopTargetPlatformBuildItem targetPlatform(NativeImageRunnerBuildItem nativeImageRunner) {
        // The quarkus-awt rule : a container build produces a Linux executable
        return new DesktopTargetPlatformBuildItem(OS.WINDOWS.isCurrent() && !nativeImageRunner.isContainerBuild()
                ? Platform.WINDOWS
                : Platform.LINUX);
    }

    // ------------------------------------------------------------------------------------------ run time initialization

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitialization(DesktopTargetPlatformBuildItem platform,
            BuildProducer<RuntimeInitializedPackageBuildItem> packages,
            BuildProducer<RuntimeInitializedClassBuildItem> classes) {
        for (String packageName : platform.withPlatform(AwtClassesAndResources.RUNTIME_INITIALIZED_PACKAGES,
                AwtClassesAndResources.WINDOWS_RUNTIME_INITIALIZED_PACKAGES,
                AwtClassesAndResources.LINUX_RUNTIME_INITIALIZED_PACKAGES)) {
            packages.produce(new RuntimeInitializedPackageBuildItem(packageName));
        }
        for (String className : platform.withPlatform(AwtClassesAndResources.RUNTIME_INITIALIZED_CLASSES,
                AwtClassesAndResources.WINDOWS_RUNTIME_INITIALIZED_CLASSES,
                AwtClassesAndResources.LINUX_RUNTIME_INITIALIZED_CLASSES)) {
            classes.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitializedDesktopUsers(CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedClassBuildItem> classes) {
        Set<String> users = DesktopStaticInitializerScanner.scan(combinedIndex.getIndex().getKnownClasses(),
                Thread.currentThread().getContextClassLoader());
        LOGGER.debugf("Classes using the desktop modules in their static initializer, initialized at run time : %s", users);
        for (String className : users) {
            classes.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    // ------------------------------------------------------------------------------------------------------ reflection

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void reflection(DesktopTargetPlatformBuildItem platform, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(platform.withPlatform(
                AwtClassesAndResources.REFLECTIVE_CLASSES,
                AwtClassesAndResources.WINDOWS_REFLECTIVE_CLASSES,
                AwtClassesAndResources.LINUX_REFLECTIVE_CLASSES)).methods().fields().reason(REASON).build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(platform.withPlatform(
                AwtClassesAndResources.REFLECTIVE_CONSTRUCTORS,
                AwtClassesAndResources.WINDOWS_REFLECTIVE_CONSTRUCTORS,
                AwtClassesAndResources.LINUX_REFLECTIVE_CONSTRUCTORS)).reason(REASON).build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(AwtClassesAndResources.TRANSFERRED_SERIALIZABLE_CLASS)
                .serialization().reason(REASON).build());
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(AwtClassesAndResources.TEXT_ATTRIBUTE_SERIALIZABLE_CLASSES).serialization().reason(REASON)
                .build());
        for (String method : platform.withPlatform(AwtClassesAndResources.REFLECTIVE_METHODS,
                AwtClassesAndResources.WINDOWS_REFLECTIVE_METHODS,
                AwtClassesAndResources.LINUX_REFLECTIVE_METHODS)) {
            MemberEntry entry = MemberEntry.method(method);
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(REASON, false, entry.className(), entry.name(),
                    entry.parameterTypes()));
        }
        for (String field : platform.withPlatform(AwtClassesAndResources.REFLECTIVE_FIELDS,
                AwtClassesAndResources.WINDOWS_REFLECTIVE_FIELDS,
                AwtClassesAndResources.LINUX_REFLECTIVE_FIELDS)) {
            MemberEntry entry = MemberEntry.field(field);
            reflectiveFields.produce(new ReflectiveFieldBuildItem(REASON, entry.className(), entry.name()));
        }
    }

    /**
     * The classes registered with their public members : always the {@code REFLECTIVE_PUBLIC_MEMBERS} lists, and the
     * {@code JAVA_BEANS_CLASSES} lists when {@code quarkus.desktop.awt.java-beans.jdk-classes} is enabled or when
     * another extension needs them (the JavaBeans registration of the Swing classes, which extend AWT classes).
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void publicMembers(DesktopTargetPlatformBuildItem platform, DesktopAwtConfig config,
            List<AwtJavaBeansClassesBuildItem> javaBeansRequests,
            BuildProducer<ReflectivePublicMembersBuildItem> publicMembers) {
        publicMembers.produce(new ReflectivePublicMembersBuildItem(List.of(platform.withPlatform(
                AwtClassesAndResources.REFLECTIVE_PUBLIC_MEMBERS,
                AwtClassesAndResources.WINDOWS_REFLECTIVE_PUBLIC_MEMBERS,
                AwtClassesAndResources.LINUX_REFLECTIVE_PUBLIC_MEMBERS))));
        if (config.javaBeans().jdkClasses() || !javaBeansRequests.isEmpty()) {
            publicMembers.produce(new ReflectivePublicMembersBuildItem(List.of(platform.withPlatform(
                    AwtClassesAndResources.JAVA_BEANS_CLASSES,
                    AwtClassesAndResources.WINDOWS_JAVA_BEANS_CLASSES,
                    AwtClassesAndResources.LINUX_JAVA_BEANS_CLASSES))));
        }
    }

    /**
     * Registers the classes of the {@link ReflectivePublicMembersBuildItem}s with their public constructors, methods
     * (inherited ones included) and fields. Quarkus has no build item for public methods and fields only : the extension
     * adds a reflection configuration file to the native build, as Quarkus does for its own reflection configuration.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void publicMembersReflectConfig(List<ReflectivePublicMembersBuildItem> publicMembers,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        Set<String> classNames = new TreeSet<>();
        for (ReflectivePublicMembersBuildItem item : publicMembers) {
            classNames.addAll(item.getClassNames());
        }
        if (!classNames.isEmpty()) {
            generatedResources.produce(new GeneratedResourceBuildItem(PUBLIC_MEMBERS_REFLECT_CONFIG,
                    publicMembersReflectConfig(classNames).getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * The reflection configuration ({@code reflect-config.json}) of classes registered with their public members.
     */
    static String publicMembersReflectConfig(Collection<String> classNames) {
        Json.JsonArrayBuilder classes = Json.array();
        for (String className : classNames) {
            classes.add(Json.object()
                    .put("name", className)
                    .put("allPublicConstructors", true)
                    .put("allPublicMethods", true)
                    .put("allPublicFields", true));
        }
        StringBuilder json = new StringBuilder();
        try {
            classes.appendTo(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return json.toString();
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void serviceProviders(DesktopTargetPlatformBuildItem platform,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(platform.withPlatform(
                AwtClassesAndResources.SERVICE_PROVIDERS,
                AwtClassesAndResources.WINDOWS_SERVICE_PROVIDERS,
                AwtClassesAndResources.LINUX_SERVICE_PROVIDERS)).methods().reason(REASON).build());
    }

    /**
     * AWT checks with reflection whether a component class overrides {@code coalesceEvents}
     * ({@code Component.isCoalesceEventsOverriden}) : register the method of the application classes that declare it.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void coalesceEventsOverrides(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {
        for (ClassInfo classInfo : combinedIndex.getIndex().getKnownClasses()) {
            MethodInfo method = classInfo.method("coalesceEvents", org.jboss.jandex.Type.create(AWT_EVENT,
                    org.jboss.jandex.Type.Kind.CLASS),
                    org.jboss.jandex.Type.create(AWT_EVENT, org.jboss.jandex.Type.Kind.CLASS));
            if (method != null && isComponent(classInfo.name(), combinedIndex.getIndex())) {
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(REASON, method));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------- JNI

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void jni(DesktopTargetPlatformBuildItem platform, BuildProducer<JniRuntimeAccessBuildItem> jniClasses,
            BuildProducer<JniRuntimeAccessMethodBuildItem> jniMethods,
            BuildProducer<JniRuntimeAccessFieldBuildItem> jniFields) {
        jniClasses.produce(new JniRuntimeAccessBuildItem(true, true, true, platform.withPlatform(
                AwtClassesAndResources.JNI_RUNTIME_ACCESS_CLASSES,
                AwtClassesAndResources.WINDOWS_JNI_RUNTIME_ACCESS_CLASSES,
                AwtClassesAndResources.LINUX_JNI_RUNTIME_ACCESS_CLASSES)));
        for (String method : platform.withPlatform(AwtClassesAndResources.JNI_RUNTIME_ACCESS_METHODS,
                AwtClassesAndResources.WINDOWS_JNI_RUNTIME_ACCESS_METHODS,
                AwtClassesAndResources.LINUX_JNI_RUNTIME_ACCESS_METHODS)) {
            MemberEntry entry = MemberEntry.method(method);
            jniMethods.produce(new JniRuntimeAccessMethodBuildItem(entry.className(), entry.name(), entry.parameterTypes()));
        }
        for (String field : platform.withPlatform(AwtClassesAndResources.JNI_RUNTIME_ACCESS_FIELDS,
                AwtClassesAndResources.WINDOWS_JNI_RUNTIME_ACCESS_FIELDS,
                AwtClassesAndResources.LINUX_JNI_RUNTIME_ACCESS_FIELDS)) {
            MemberEntry entry = MemberEntry.field(field);
            jniFields.produce(new JniRuntimeAccessFieldBuildItem(entry.className(), entry.name()));
        }
    }

    // ------------------------------------------------------------------------------------------------------- resources

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void resources(DesktopTargetPlatformBuildItem platform, BuildProducer<NativeImageResourceBundleBuildItem> bundles,
            BuildProducer<NativeImageResourcePatternsBuildItem> resources) {
        for (String bundle : platform.withPlatform(AwtClassesAndResources.RESOURCE_BUNDLES,
                AwtClassesAndResources.WINDOWS_RESOURCE_BUNDLES,
                AwtClassesAndResources.LINUX_RESOURCE_BUNDLES)) {
            // Without module name : native-image finds the module of a JDK bundle from its package, and GraalVM 25.3+
            // checks the module lookups of bundles (UIDefaults) against the bundle name only
            bundles.produce(new NativeImageResourceBundleBuildItem(bundle));
        }
        resources.produce(NativeImageResourcePatternsBuildItem.builder()
                .includeGlobs(platform.withPlatform(AwtClassesAndResources.RESOURCE_GLOBS,
                        AwtClassesAndResources.WINDOWS_RESOURCE_GLOBS,
                        AwtClassesAndResources.LINUX_RESOURCE_GLOBS))
                .build());
    }

    // --------------------------------------------------------------------------------------------- run time defaults

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runTimeDefaults(DesktopTargetPlatformBuildItem platform, DesktopAwtConfig config,
            BuildProducer<NativeImageFeatureBuildItem> features,
            BuildProducer<NativeImageSystemPropertyBuildItem> builderProperties) {
        features.produce(new NativeImageFeatureBuildItem(DesktopAwtFeature.class));
        if (platform.isWindows() && config.windows().dpiAware()) {
            builderProperties.produce(new NativeImageSystemPropertyBuildItem(DesktopAwtFeature.DPI_AWARE, "true"));
        }
    }

    /**
     * The Java Access Bridge (Windows) : included, or ignored so that the toolkit does not fail to start when the user
     * enabled it.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void accessBridge(DesktopTargetPlatformBuildItem platform, DesktopAwtConfig config,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<JniRuntimeAccessBuildItem> jniClasses,
            BuildProducer<NativeImageSystemPropertyBuildItem> builderProperties) {
        if (!platform.isWindows()) {
            // The Java Access Bridge only exists on Windows : ignore a configured assistive technology
            builderProperties.produce(
                    new NativeImageSystemPropertyBuildItem(DesktopAwtFeature.IGNORE_ASSISTIVE_TECHNOLOGIES, "true"));
            return;
        }
        if (config.windows().accessBridge()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(AwtClassesAndResources.ACCESS_BRIDGE_PROVIDER)
                    .methods().reason(REASON).build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(AwtClassesAndResources.ACCESSIBLE_ROLE)
                    .fields().reason(REASON).build());
            jniClasses.produce(new JniRuntimeAccessBuildItem(true, true, true, AwtClassesAndResources.ACCESS_BRIDGE));
        } else {
            builderProperties.produce(
                    new NativeImageSystemPropertyBuildItem(DesktopAwtFeature.IGNORE_ASSISTIVE_TECHNOLOGIES, "true"));
        }
    }

    // ------------------------------------------------------------------------------------------------ java.home, fonts

    /**
     * The charsets of the JDK font configuration (Windows : windows-125x, GBK, windows-31j, x-windows-949...).
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void charsets(DesktopTargetPlatformBuildItem platform, DesktopAwtConfig config,
            BuildProducer<NativeImageEnableAllCharsetsBuildItem> charsets) {
        if (platform.isWindows() && config.windows().fontConfiguration() == DesktopAwtConfig.FontConfiguration.JDK) {
            charsets.produce(new NativeImageEnableAllCharsetsBuildItem());
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem runtimeHome(DesktopTargetPlatformBuildItem platform, DesktopAwtConfig config,
            NativeConfig nativeConfig, DesktopAwtRecorder recorder,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<DesktopAwtRuntimeInitBuildItem> runtimeInit) {
        Path jdkHome = builderJdkHome(nativeConfig);
        embed(jdkHome.resolve("lib").resolve("psfontj2d.properties"), DesktopAwtRecorder.POSTSCRIPT_FONTS,
                generatedResources, resources);
        String fontConfiguration = null;
        if (platform.isWindows() && config.windows().fontConfiguration() == DesktopAwtConfig.FontConfiguration.JDK) {
            byte[] data = embed(jdkHome.resolve("lib").resolve("fontconfig.properties.src"),
                    DesktopAwtRecorder.FONT_CONFIGURATION, generatedResources, resources);
            if (data != null) {
                fontConfiguration = "quarkus-desktop-awt-fonts-" + sha256(data).substring(0, 16);
            }
        }
        // Before the application starts, so before any AWT class is used : the other steps that use AWT at startup
        // consume DesktopAwtRuntimeInitBuildItem
        recorder.initRuntimeHome(fontConfiguration);
        runtimeInit.produce(new DesktopAwtRuntimeInitBuildItem());
        return new ServiceStartBuildItem(FEATURE);
    }

    /**
     * The home of the JDK used by the native build : the GraalVM home, the Java home configured for native builds, or the
     * home of the JDK running the build.
     */
    static Path builderJdkHome(NativeConfig nativeConfig) {
        return nativeConfig.graalvmHome().filter(home -> !home.isBlank()).map(Path::of).filter(Files::isDirectory)
                .or(() -> Optional.ofNullable(nativeConfig.javaHome()).map(File::toPath).filter(Files::isDirectory))
                .orElse(Path.of(System.getProperty("java.home")));
    }

    /**
     * Embeds a file of the JDK in the native executable.
     *
     * @return the content of the file, or {@code null} when it does not exist
     */
    private static byte[] embed(Path file, String resource, BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> resources) {
        if (!Files.isRegularFile(file)) {
            LOGGER.warnf("%s not found : it is not included in the native executable", file);
            return null;
        }
        try {
            byte[] data = Files.readAllBytes(file);
            generatedResources.produce(new GeneratedResourceBuildItem(resource, data));
            resources.produce(new NativeImageResourceBuildItem(resource));
            return data;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------------------------------------- Windows executable

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void windowsExecutable(DesktopTargetPlatformBuildItem platform, DesktopAwtConfig config,
            OutputTargetBuildItem outputTarget, BuildProducer<GeneratedResourceBuildItem> generatedResources)
            throws IOException {
        if (!platform.isWindows()) {
            return;
        }
        List<String> args = WindowsExecutable.nativeImageArgs(config.windows(), outputTarget.getOutputDirectory());
        if (!args.isEmpty()) {
            LOGGER.debugf("Windows executable options : %s", args);
            generatedResources.produce(new GeneratedResourceBuildItem(WindowsExecutable.NATIVE_IMAGE_PROPERTIES,
                    WindowsExecutable.nativeImageProperties(args).getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * Copies the Visual C++ runtime next to the native executable. {@code ArtifactResultBuildItem} is never produced :
     * it makes this step run after the native build.
     */
    @BuildStep(onlyIf = NativeBuild.class)
    void copyVcRuntime(DesktopTargetPlatformBuildItem platform, DesktopAwtConfig config, NativeConfig nativeConfig,
            NativeImageBuildItem nativeImage, BuildProducer<ArtifactResultBuildItem> artifactResults) {
        if (platform.isWindows() && config.windows().copyVcRuntime()) {
            WindowsExecutable.copyVcRuntime(builderJdkHome(nativeConfig), nativeImage.getPath());
        }
    }

    // ----------------------------------------------------------------------------------------------------- CDI, dev mode

    /**
     * A client proxy is a subclass of the bean class : a normal scoped bean extending {@code java.awt.Component} would
     * create a second component (hidden, and created outside the event dispatch thread) for its proxy.
     */
    @BuildStep
    void warnNormalScopedComponents(BeanDiscoveryFinishedBuildItem beanDiscovery, CombinedIndexBuildItem combinedIndex,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        for (BeanInfo bean : beanDiscovery.getBeans()) {
            if (!bean.getScope().isNormal()) {
                continue;
            }
            DotName type = bean.getImplClazz() != null ? bean.getImplClazz().name() : bean.getProviderType().name();
            if (isComponent(type, combinedIndex.getIndex())) {
                LOGGER.warnf("The bean %s is %s and extends java.awt.Component : its client proxy extends it too, so"
                        + " creating the proxy creates another component, outside the event dispatch thread. Use"
                        + " @Singleton or @Dependent for AWT and Swing component beans.", bean.getBeanClass(),
                        bean.getScope().getDotName().withoutPackagePrefix());
            }
        }
    }

    /**
     * The AWT toolkit, its threads and windows outlive a restart of the application in dev mode : dispose the windows
     * when the application stops.
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void disposeWindowsOnRestart(DesktopAwtRecorder recorder, ShutdownContextBuildItem shutdownContext) {
        recorder.disposeWindowsOnShutdown(shutdownContext);
    }

    /**
     * The AWT event dispatch thread outlives the application in dev mode (live reload) and in tests (several
     * applications in the same JVM), with the context class loader of the first application : dispatch the events with
     * the class loader of the running application, so that the classes that Swing loads by name on the event dispatch
     * thread (look and feels, editor kits...) are the application ones.
     */
    @BuildStep(onlyIfNot = IsNormal.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void applicationClassLoaderOnEventDispatchThread(DesktopAwtRecorder recorder,
            ShutdownContextBuildItem shutdownContext) {
        recorder.useApplicationClassLoaderOnEventDispatchThread(shutdownContext);
    }

    /**
     * Whether the class extends {@code java.awt.Component}, using the index for application classes and class loading
     * (without initialization) for the others.
     */
    static boolean isComponent(DotName name, IndexView index) {
        DotName current = name;
        while (current != null) {
            if (current.equals(COMPONENT)) {
                return true;
            }
            ClassInfo classInfo = index.getClassByName(current);
            if (classInfo == null) {
                return isComponentClass(current.toString());
            }
            current = classInfo.superName();
        }
        return false;
    }

    private static boolean isComponentClass(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (ClassLoader loader : Stream.of(classLoader, DesktopAwtProcessor.class.getClassLoader())
                .filter(l -> l != null).toList()) {
            try {
                return java.awt.Component.class.isAssignableFrom(Class.forName(className, false, loader));
            } catch (ClassNotFoundException | LinkageError e) {
                // try the next class loader
            }
        }
        return false;
    }
}
