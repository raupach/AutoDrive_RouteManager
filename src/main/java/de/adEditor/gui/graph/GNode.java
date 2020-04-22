package de.adEditor.gui.graph;

import java.awt.geom.Point2D;

public class GNode {
    private Double x, y, z;
    private boolean selected = false;

    public GNode(){
    }

    public GNode(Double x, Double y, Double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public GNode(Point2D point) {
        x = point.getX();
        y = point.getY();
    }

    public void setPos(Point2D position) {
        x = position.getX();
        y = position.getY();
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
