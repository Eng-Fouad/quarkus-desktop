package io.quarkiverse.desktop.swing.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

/**
 * Finds the classes named in the Synth XML files of the application ({@code SynthLookAndFeel.load}) : the objects of
 * these files ({@code <object class="...">}, {@code <new class="...">}, {@code <class>...</class>}) are created by the
 * beans decoder with reflection (constructors, static fields, methods, properties).
 */
final class SynthXmlClasses {

    private static final Logger LOGGER = Logger.getLogger(SynthXmlClasses.class);

    /**
     * Synth XML files are small : larger XML files are not read.
     */
    static final long MAX_SIZE = 4 * 1024 * 1024;

    private static final Pattern SYNTH_ROOT = Pattern.compile("<synth[\\s>/]");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("\\bclass\\s*=\\s*[\"']([\\w.$]+)[\"']");
    private static final Pattern CLASS_ELEMENT = Pattern.compile("<class>\\s*([\\w.$]+)\\s*</class>");
    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private SynthXmlClasses() {
    }

    /**
     * Whether the resource may be a Synth XML file.
     */
    static boolean isCandidate(String resource) {
        return resource.endsWith(".xml") && !resource.startsWith("META-INF/");
    }

    /**
     * The classes named in a Synth XML file, empty if the file is not a Synth XML file.
     */
    static Set<String> classes(Path file) {
        try {
            if (Files.size(file) > MAX_SIZE) {
                return Set.of();
            }
            try (InputStream in = Files.newInputStream(file)) {
                return classes(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.debugf(e, "Unable to read %s", file);
            return Set.of();
        }
    }

    /**
     * The classes named in the content of a Synth XML file, empty if it is not a Synth XML file.
     */
    static Set<String> classes(String content) {
        String xml = COMMENT.matcher(content).replaceAll("");
        if (!SYNTH_ROOT.matcher(xml).find()) {
            return Set.of();
        }
        Set<String> classes = new TreeSet<>();
        for (Pattern pattern : new Pattern[] { CLASS_ATTRIBUTE, CLASS_ELEMENT }) {
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find()) {
                classes.add(matcher.group(1));
            }
        }
        return classes;
    }
}
