package de.adEditor.gui.editor;

import java.util.LinkedList;
import java.util.List;

public class MapNode {

    public List<MapNode> incoming = new LinkedList<>();
    public List<MapNode> outgoing = new LinkedList<>();
    public double x, y, z;
    public int id;

    public MapNode(int id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public List<MapNode> getIncoming() {
        return incoming;
    }

    public void setIncoming(List<MapNode> incoming) {
        this.incoming = incoming;
    }

    public List<MapNode> getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(List<MapNode> outgoing) {
        this.outgoing = outgoing;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
