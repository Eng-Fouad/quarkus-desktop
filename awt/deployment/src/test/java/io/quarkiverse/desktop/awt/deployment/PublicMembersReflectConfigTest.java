package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The reflection configuration of the classes registered with their public members, and the serialization
 * registrations that have no list.
 */
class PublicMembersReflectConfigTest {

    @Test
    void reflectConfig() {
        String json = DesktopAwtProcessor.publicMembersReflectConfig(List.of("java.awt.Button", "java.util.Map$Entry"))
                .replaceAll("\\s", "");
        assertTrue(json.startsWith("[{") && json.endsWith("}]"), json);
        String[] classes = json.substring(2, json.length() - 2).split("\\},\\{");
        assertEquals(2, classes.length, json);
        for (int i = 0; i < classes.length; i++) {
            // the order of the members of a JSON object does not matter
            assertEquals(Set.of("\"name\":\"" + List.of("java.awt.Button", "java.util.Map$Entry").get(i) + "\"",
                    "\"allPublicConstructors\":true", "\"allPublicMethods\":true", "\"allPublicFields\":true"),
                    Set.of(classes[i].split(",")), json);
        }
    }

    @Test
    void reflectConfigLocation() {
        // native-image reads the configuration files of META-INF/native-image and of its sub directories
        assertTrue(DesktopAwtProcessor.PUBLIC_MEMBERS_REFLECT_CONFIG.startsWith("META-INF/native-image/"));
        assertTrue(DesktopAwtProcessor.PUBLIC_MEMBERS_REFLECT_CONFIG.endsWith("/reflect-config.json"));
    }

    @Test
    void serializableClasses() throws ClassNotFoundException {
        for (String className : AwtClassesAndResources.TEXT_ATTRIBUTE_SERIALIZABLE_CLASSES) {
            assertTrue(Serializable.class.isAssignableFrom(Class.forName(className, false,
                    PublicMembersReflectConfigTest.class.getClassLoader())), className);
        }
    }
}
