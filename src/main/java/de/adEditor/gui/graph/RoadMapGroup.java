package de.adEditor.gui.graph;

import java.util.ArrayList;
import java.util.List;

public class RoadMapGroup {

    private String name;
    private List<RoadMapMarker> markers = new ArrayList<>();

    public RoadMapGroup() {
    }

    @Override
    public String toString() {
        return name;
    }

    public RoadMapGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RoadMapMarker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<RoadMapMarker> markers) {
        this.markers = markers;
    }
}
