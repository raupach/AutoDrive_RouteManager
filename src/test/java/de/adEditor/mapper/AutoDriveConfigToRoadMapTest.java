package de.adEditor.mapper;

import de.adEditor.gui.editor.RoadMap;
import de.adEditor.gui.graph.GEdge;
import de.adEditor.gui.graph.GNode;
import javafx.geometry.Point3D;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.*;

public class AutoDriveConfigToRoadMapTest {

    @Test
    public void testLoadXmlConfigFile () throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        File file = new File(getClass().getClassLoader().getResource("AutoDrive_Felsbrunn_TestConfig.xml").getFile());
        AutoDriveConfigToRoadMap autoDriveConfigToRoadMap = new AutoDriveConfigToRoadMap();
        RoadMap roadMap = autoDriveConfigToRoadMap.loadXmlConfigFile(file);
        assertNotNull(roadMap);
        assertNotNull(roadMap.getGraph());
        assertNotNull(roadMap.getGroups());
        assertNotNull(roadMap.getMarkers());

        assertEquals(2, roadMap.getMarkers().size());
        assertEquals(2, roadMap.getGroups().size());

        // Test Groups
        assertTrue(roadMap.getGroups().stream().anyMatch(g -> StringUtils.equals(g.getName(), "All")));
        assertTrue(roadMap.getGroups().stream().anyMatch(g -> StringUtils.equals(g.getName(), "Sell Points")));

        // Test Markers
        assertTrue(roadMap.getMarkers().stream().anyMatch(g -> StringUtils.equals(g.getName(), "Shop")));
        assertTrue(roadMap.getMarkers().stream().anyMatch(g -> StringUtils.equals(g.getName(), "Spinnery")));

        Set<GEdge> edges = roadMap.getGraph().edgeSet();
        Set<GNode> vertex = roadMap.getGraph().vertexSet();
        assertEquals(3, edges.size());
        assertEquals(4, vertex.size());


        GNode node1 = vertex.stream().filter(g -> g.getPoint().equals(new Point3D(-21.728, 191.719, 66.9))).findFirst().orElseThrow();
        GNode node2 = vertex.stream().filter(g -> g.getPoint().equals(new Point3D(-24.751, 191.703, 66.957))).findFirst().orElseThrow();
        GNode node3 = vertex.stream().filter(g -> g.getPoint().equals(new Point3D(-36.843, 192.051, 66.958))).findFirst().orElseThrow();
        GNode node4 = vertex.stream().filter(g -> g.getPoint().equals(new Point3D(-41.056, 193.637, 66.123))).findFirst().orElseThrow();
        assertNotNull(node1);
        assertNotNull(node2);
        assertNotNull(node3);
        assertNotNull(node4);
        assertNotNull(roadMap.getGraph().getEdge(node1, node2));
        assertNotNull(roadMap.getGraph().getEdge(node2, node3));
        assertNotNull(roadMap.getGraph().getEdge(node4, node1));

        assertEquals (0, roadMap.getGraph().outgoingEdgesOf(node3).size());
        assertEquals (1, roadMap.getGraph().incomingEdgesOf(node3).size());
    }
}