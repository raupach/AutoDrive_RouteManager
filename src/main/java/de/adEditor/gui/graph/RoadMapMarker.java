package de.adEditor.gui.graph;

public class RoadMapMarker {

    private GNode gNode;
    private String name;
    private RoadMapGroup group;

    public GNode getgNode() {
        return gNode;
    }

    public void setgNode(GNode gNode) {
        this.gNode = gNode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoadMapGroup getGroup() {
        return group;
    }

    public void setGroup(RoadMapGroup group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return name;
    }
}
