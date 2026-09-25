package io.quarkiverse.desktop.swing.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Checks the list conventions documented in {@link SwingClassesAndResources}.
 */
class SwingClassesAndResourcesTest {

    private static final List<String> PLATFORMS = List.of("WINDOWS_", "LINUX_", "MAC_");
    private static final Pattern LIST_NAME = Pattern.compile("(WINDOWS_|LINUX_|MAC_)?([A-Z_]+)");

    private static final String NAME = "[\\w$]+(\\.[\\w$]+)*";
    private static final String TYPE = NAME + "(\\[])*";
    private static final Pattern NAME_ENTRY = Pattern.compile(NAME);
    private static final Pattern METHOD_ENTRY = Pattern
            .compile(NAME + "#(<init>|[\\w$]+)\\((" + TYPE + "(," + TYPE + ")*)?\\)");
    private static final Pattern FIELD_ENTRY = Pattern.compile(NAME + "#[\\w$]+");
    private static final Pattern GLOB_ENTRY = Pattern.compile("[^\\s/]\\S*");

    private static final Map<String, Pattern> KINDS = Map.ofEntries(
            Map.entry("RUNTIME_INITIALIZED_PACKAGES", NAME_ENTRY),
            Map.entry("RUNTIME_INITIALIZED_CLASSES", NAME_ENTRY),
            Map.entry("REFLECTIVE_CLASSES", NAME_ENTRY),
            Map.entry("REFLECTIVE_CONSTRUCTORS", NAME_ENTRY),
            Map.entry("REFLECTIVE_METHODS", METHOD_ENTRY),
            Map.entry("JNI_RUNTIME_ACCESS_CLASSES", NAME_ENTRY),
            Map.entry("JNI_RUNTIME_ACCESS_METHODS", METHOD_ENTRY),
            Map.entry("JNI_RUNTIME_ACCESS_FIELDS", FIELD_ENTRY),
            Map.entry("RESOURCE_BUNDLES", NAME_ENTRY),
            Map.entry("RESOURCE_GLOBS", GLOB_ENTRY),
            Map.entry("SERVICE_PROVIDERS", NAME_ENTRY));

    @Test
    void listsFollowTheConventions() throws IllegalAccessException {
        Map<String, Set<String>> lists = new HashMap<>();
        for (Field field : SwingClassesAndResources.class.getDeclaredFields()) {
            if (field.getType() != String[].class) {
                continue;
            }
            String list = field.getName();
            int modifiers = field.getModifiers();
            assertTrue(Modifier.isStatic(modifiers)
                    && (modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0,
                    list + " must be a package-private static field");
            Matcher name = LIST_NAME.matcher(list);
            assertTrue(name.matches() && KINDS.containsKey(name.group(2)),
                    list + " must be named [WINDOWS_|LINUX_|MAC_]KIND, with KIND in " + KINDS.keySet());
            String[] entries = (String[]) field.get(null);
            Set<String> unique = new LinkedHashSet<>(Arrays.asList(entries));
            assertEquals(entries.length, unique.size(), list + " has duplicate entries");
            for (String entry : entries) {
                assertTrue(KINDS.get(name.group(2)).matcher(entry).matches(), list + " : malformed entry " + entry);
            }
            lists.put(list, unique);
        }
        for (String kind : KINDS.keySet()) {
            Set<String> common = lists.get(kind);
            assertNotNull(common, "missing common list " + kind);
            // an entry needed on every platform belongs to the common list only
            for (String platform : PLATFORMS) {
                for (String entry : lists.getOrDefault(platform + kind, Set.of())) {
                    assertFalse(common.contains(entry), platform + kind + " : " + entry + " is in the common list");
                }
            }
        }
    }
}
