package de.adEditor.gui.editor;


import de.adEditor.gui.graph.GEdge;
import de.adEditor.gui.graph.GNode;
import de.adEditor.gui.graph.RoadMapGroup;
import de.adEditor.gui.graph.RoadMapMarker;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.List;

public class RoadMap {

    private Graph<GNode, GEdge> graph = new SimpleDirectedGraph<>(GEdge.class);
    private List<RoadMapMarker> markers = new ArrayList<>();
    private List<RoadMapGroup> groups = new ArrayList<>();

    public Graph<GNode, GEdge> getGraph() {
        return graph;
    }

    public void setGraph(Graph<GNode, GEdge> graph) {
        this.graph = graph;
    }

    public List<RoadMapMarker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<RoadMapMarker> markers) {
        this.markers = markers;
    }

    public List<RoadMapGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<RoadMapGroup> groups) {
        this.groups = groups;
    }
}
