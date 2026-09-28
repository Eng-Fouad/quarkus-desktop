package io.quarkiverse.desktop.awt.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ReachabilityLookupsTest {

    @Test
    void javaBeansTypes() {
        Set<String> types = ReachabilityLookups.javaBeansTypes(List.of("java.awt.Button"));
        // the probes of the JavaBeans API that do not exist, for the class and its superclasses
        for (String type : List.of("java.awt.ButtonBeanInfo", "java.awt.ButtonCustomizer",
                "java.awt.ButtonPersistenceDelegate", "java.awt.ButtonEditor", "com.sun.beans.editors.ButtonEditor",
                "java.beans.MetaData$java_awt_Button_PersistenceDelegate", "java.awt.ComponentCustomizer",
                "java.lang.ObjectBeanInfo", "java.lang.ObjectEditor", "com.sun.beans.editors.ObjectEditor")) {
            assertTrue(types.contains(type), type);
        }
        // the supertypes, whose members are queried
        for (String type : List.of("java.awt.Button", "java.awt.Component", "java.lang.Object",
                "java.awt.image.ImageObserver", "java.awt.MenuContainer", "java.io.Serializable",
                "javax.accessibility.Accessible")) {
            assertTrue(types.contains(type), type);
        }
        // the listener interfaces of the event sets (of the class and of its superclasses), with their supertypes
        for (String type : List.of("java.awt.event.ActionListener", "java.awt.event.ComponentListener",
                "java.awt.event.HierarchyBoundsListener", "java.beans.PropertyChangeListener", "java.util.EventListener")) {
            assertTrue(types.contains(type), type);
        }
        assertFalse(types.contains("java.awt.event.ActionListenerBeanInfo"));
        // the classes that exist are not lookups expected to fail
        assertFalse(types.contains("com.sun.beans.infos.ComponentBeanInfo"));
        assertFalse(types.contains("java.beans.MetaData$java_awt_Component_PersistenceDelegate"));
        // no probe for interfaces
        assertFalse(types.contains("java.io.SerializableBeanInfo"));
        assertTrue(ReachabilityLookups.javaBeansTypes(List.of("no.such.Class")).isEmpty());
    }

    @Test
    void missingBundles() {
        ReachabilityLookupsBuildItem lookups = ReachabilityLookups.missingBundles(
                List.of("javax.imageio.plugins.tiff.TIFFImageMetadataFormatResources"), List.of(Locale.US, Locale.GERMAN),
                "java.desktop");
        assertEquals(List.of("javax.imageio.plugins.tiff.TIFFImageMetadataFormatResources",
                "javax.imageio.plugins.tiff.TIFFImageMetadataFormatResources_de",
                "javax.imageio.plugins.tiff.TIFFImageMetadataFormatResources_en",
                "javax.imageio.plugins.tiff.TIFFImageMetadataFormatResources_en_US",
                "javax.imageio.plugins.tiff.spi.TIFFImageMetadataFormatResourcesProvider"), lookups.getTypes());
        assertEquals(List.of("java.desktop:javax/imageio/plugins/tiff/TIFFImageMetadataFormatResources*.properties",
                "javax/imageio/plugins/tiff/TIFFImageMetadataFormatResources*.properties"), lookups.getResourceGlobs());
        assertTrue(lookups.getMethods().isEmpty());
    }

    @Test
    void resourceGlobs() {
        assertEquals(Set.of("sun/awt/resources/awt_*.properties", "java.desktop:sun/awt/resources/awt_*.properties"),
                ReachabilityLookups.bundlePropertiesGlobs(List.of("sun.awt.resources.awt", "no.such.Bundle")));
        assertEquals(Set.of("java.desktop:javax/swing/plaf/basic/icons/*",
                "java.datatransfer:sun/datatransfer/resources/flavormap.properties",
                "java.base:jdk/internal/icu/impl/data/**/ubidi.icu"),
                ReachabilityLookups.moduleGlobs(List.of("javax/swing/plaf/basic/icons/*",
                        "sun/datatransfer/resources/flavormap.properties", "jdk/internal/icu/impl/data/**/ubidi.icu",
                        "no/such/directory/*", "toplevel.properties")));
    }

    @Test
    void javaBeansSerializedForms() {
        assertEquals(Set.of("java/awt/*.ser", "javax/swing/*.ser"),
                ReachabilityLookups.javaBeansSerializedForms(List.of("java.awt.Button", "java.awt.Label",
                        "javax.swing.JButton", "NoPackage")));
    }

    @Test
    void reachabilityMetadata() {
        String json = ReachabilityLookups.reachabilityMetadata(List.of(
                new ReachabilityLookupsBuildItem(List.of("a.B", "c.D"),
                        List.of("c.D#coalesceEvents(java.awt.AWTEvent,java.awt.AWTEvent)"),
                        List.of("META-INF/services/x.Y", "java.desktop:javax/swing/plaf/basic/icons/*")),
                new ReachabilityLookupsBuildItem(List.of("a.B"), List.of(), List.of())));
        String compact = json.replaceAll("\\s", "");
        // the order of the keys of an object is not defined
        assertEquals(compact.indexOf("{\"type\":\"a.B\"}"), compact.lastIndexOf("{\"type\":\"a.B\"}"), compact);
        assertTrue(compact.contains("{\"type\":\"a.B\"}"), compact);
        assertTrue(compact.contains("\"type\":\"c.D\""), compact);
        assertTrue(compact.contains("\"name\":\"coalesceEvents\""), compact);
        assertTrue(compact.contains("\"parameterTypes\":[\"java.awt.AWTEvent\",\"java.awt.AWTEvent\"]"), compact);
        assertTrue(compact.contains("{\"glob\":\"META-INF/services/x.Y\"}"), compact);
        assertTrue(compact.contains("\"module\":\"java.desktop\""), compact);
        assertTrue(compact.contains("\"glob\":\"javax/swing/plaf/basic/icons/*\""), compact);
    }
}
