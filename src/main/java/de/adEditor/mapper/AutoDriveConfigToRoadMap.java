package de.adEditor.mapper;

import de.adEditor.gui.editor.RoadMap;
import de.adEditor.gui.graph.GEdge;
import de.adEditor.gui.graph.GNode;
import de.adEditor.gui.graph.RoadMapGroup;
import de.adEditor.gui.graph.RoadMapMarker;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AutoDriveConfigToRoadMap {

    private static Logger LOG = LoggerFactory.getLogger(AutoDriveConfigToRoadMap.class);

    public RoadMap loadXmlConfigFile(File fXmlFile) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        LOG.info("loadXmlConfigFile: {}", fXmlFile.getName());

        RoadMap roadMap = new RoadMap();
        Graph<GNode, GEdge> graph = roadMap.getGraph();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document xmlDocument = dBuilder.parse(fXmlFile);
        xmlDocument.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();
        String x = (String) xPath.compile("/AutoDrive/*/waypoints/x").evaluate(xmlDocument, XPathConstants.STRING);
        String y = (String) xPath.compile("/AutoDrive/*/waypoints/y").evaluate(xmlDocument, XPathConstants.STRING);
        String z = (String) xPath.compile("/AutoDrive/*/waypoints/z").evaluate(xmlDocument, XPathConstants.STRING);
        String out = (String) xPath.compile("/AutoDrive/*/waypoints/out").evaluate(xmlDocument, XPathConstants.STRING);
        NodeList mapmarkers = (NodeList) xPath.compile("/AutoDrive/*/mapmarker").evaluate(xmlDocument, XPathConstants.NODESET);

        List<GNode> vertex = waypointsToVertex(x, y, z);
        toRoadMapMarkers(roadMap.getMarkers(), roadMap.getGroups(), vertex, mapmarkers);
        vertex.forEach(graph::addVertex);
        waypointsToEdges(graph, out, vertex);

        LOG.info("loadFile end.");
        return roadMap;
    }

    private void toRoadMapMarkers(List<RoadMapMarker> markers, List<RoadMapGroup> groups, List<GNode> vertex, NodeList mapmarkers) {
        for (int i = 0; i < mapmarkers.getLength(); i++) {
            Node item = mapmarkers.item(i);

            if (item.getNodeType()==Node.ELEMENT_NODE) {
                Element eElement = (Element) item;

                NodeList idNodeList = eElement.getElementsByTagName("id");
                NodeList nameNodeList = eElement.getElementsByTagName("name");
                NodeList groupNodeList = eElement.getElementsByTagName("group");

                for (int markerIndex = 0; markerIndex < idNodeList.getLength(); markerIndex++) {
                    Node node = idNodeList.item(markerIndex).getChildNodes().item(0);
                    String markerNodeId = node.getNodeValue();

                    node = nameNodeList.item(markerIndex).getChildNodes().item(0);
                    String markerName = node.getNodeValue();

                    node = groupNodeList.item(markerIndex).getChildNodes().item(0);
                    String markerGroup = node.getNodeValue();

                    RoadMapMarker marker = new RoadMapMarker();
                    marker.setName(markerName);
                    RoadMapGroup group;
                    Optional<RoadMapGroup> groupOptional = groups.stream()
                            .filter(g -> StringUtils.equalsIgnoreCase(g.getName(), markerGroup))
                            .findFirst();
                    if (groupOptional.isEmpty()) {
                        group = new RoadMapGroup(markerGroup);
                        groups.add(group);
                    } else {
                        group = groupOptional.get();
                    }
                    marker.setGroup(group);
                    group.getMarkers().add(marker);
                    GNode gNode = vertex.get(Integer.parseInt(markerNodeId) - 1);
                    gNode.setMarker(marker);
                    marker.setgNode(gNode);
                    markers.add(marker);
                }
            }

        }

    }

    private List<GNode> waypointsToVertex(String xNode, String yNode, String zNode) {
        String[] xValues = StringUtils.trimToEmpty(xNode).split(",");
        String[] yValues = StringUtils.trimToEmpty(yNode).split(",");
        String[] zValues = StringUtils.trimToEmpty(zNode).split(",");

        List<GNode> nodeList = new ArrayList<>(xValues.length);

        for (int i = 0; i < xValues.length; i++) {
            // SWAP Y-Z Values
            GNode gNode = new GNode(
                    Double.parseDouble(StringUtils.trimToEmpty(xValues[i])),
                    Double.parseDouble(StringUtils.trimToEmpty(zValues[i])),
                    Double.parseDouble(StringUtils.trimToEmpty(yValues[i]))
            );
            nodeList.add(gNode);
        }

        return nodeList;
    }


    private void waypointsToEdges(Graph<GNode, GEdge> graph, String outNode, List<GNode> vertex) {
        String[] outValues = StringUtils.trimToEmpty(outNode).split(";");

        for (int j = 0; j < outValues.length; j++) {
            String out = outValues[j];
            for (String outIndexStr : out.split(",")) {
                int outIndex = Integer.parseInt(StringUtils.trimToEmpty(outIndexStr));
                if (outIndex!=-1) {
                    GNode source = vertex.get(j);
                    GNode target = vertex.get(outIndex - 1);
                    if (source.equals(target)) {
                        LOG.warn("selfloop at: {}", source);
                    } else {
                        graph.addEdge(source, target, new GEdge(source, target));
                    }
                }
            }
        }
    }

    public void saveXmlConfig(File file, RoadMap roadMap) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document xmlDocument = docBuilder.parse(file);

        XPath xPath = XPathFactory.newInstance().newXPath();
        Node x = (Node) xPath.compile("/AutoDrive/*/waypoints/x").evaluate(xmlDocument, XPathConstants.NODE);

        Set<GNode> sourceSet = roadMap.getGraph().vertexSet();
        List<GNode> targetList = new ArrayList<>(sourceSet);

    }
}
