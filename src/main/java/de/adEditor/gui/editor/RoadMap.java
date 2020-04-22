package de.adEditor.gui.editor;


import de.adEditor.gui.graph.GEdge;
import de.adEditor.gui.graph.GNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.LinkedList;
import java.util.List;

public class RoadMap {

    public List<MapNode> mapNodes = new LinkedList<>();
    public List<MapMarker> mapMarkers = new LinkedList<>();
    private Graph<GNode, GEdge> graph = new SimpleDirectedGraph<>(GEdge.class);

    public void addMapNode(MapNode mapNode) {
        mapNodes.add(mapNode);
    }

    public void addMapMarker(MapMarker mapMarker) {
        mapMarkers.add(mapMarker);
    }

    public void removeMapNode(MapNode toDelete) {
        boolean deleted = false;
        if (mapNodes.contains(toDelete)) {
            mapNodes.remove(toDelete);
            deleted = true;
        }

        for (MapNode mapNode : mapNodes) {
            if (mapNode.outgoing.contains(toDelete)) {
                mapNode.outgoing.remove(toDelete);
            }
            if (mapNode.incoming.contains(toDelete)) {
                mapNode.incoming.remove(toDelete);
            }
            if (deleted && mapNode.id > toDelete.id) {
                mapNode.id--;
            }
        }

        List<MapMarker> mapMarkersToDelete = new LinkedList<>();
        for (MapMarker mapMarker : mapMarkers) {
            if (mapMarker.mapNode == toDelete) {

                mapMarkersToDelete.add(mapMarker);
            }
        }
        for (MapMarker mapMarker : mapMarkersToDelete) {
            removeMapMarker(mapMarker);
            this.mapMarkers.remove(mapMarker);
        }
    }

    public void removeMapMarker(MapMarker mapMarker) {
        List<MapMarker> mapMarkersToKeep = new LinkedList<>();
        for (MapMarker mapMarkerIter : mapMarkers) {
            if (mapMarkerIter.mapNode.id != mapMarker.mapNode.id) {
                mapMarkersToKeep.add(mapMarkerIter);
            }
        }
        mapMarkers = mapMarkersToKeep;
    }

    public static boolean isDual(MapNode start, MapNode target) {
        for (MapNode outgoing : start.outgoing) {
            if (outgoing == target) {
                for (MapNode outgoingTarget : target.outgoing) {
                    if (outgoingTarget == start) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public List<MapNode> getMapNodes() {
        return mapNodes;
    }

    public void setMapNodes(List<MapNode> mapNodes) {
        this.mapNodes = mapNodes;
    }

    public List<MapMarker> getMapMarkers() {
        return mapMarkers;
    }

    public void setMapMarkers(List<MapMarker> mapMarkers) {
        this.mapMarkers = mapMarkers;
    }

    public Graph<GNode, GEdge> getGraph() {
        return graph;
    }

    public void setGraph(Graph<GNode, GEdge> graph) {
        this.graph = graph;
    }
}
