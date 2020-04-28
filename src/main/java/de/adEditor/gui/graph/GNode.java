package de.adEditor.gui.graph;

import java.awt.geom.Point2D;

public class GNode {
    private boolean selected = false;
    private Double x ,y ,z;
    private RoadMapMarker marker;

    public GNode(){
    }

    public Point2D getPoint2D() {
        return new Point2D.Double(x, y);
    }

    public GNode(Double x, Double y, Double z) {
        this.x =x;
        this.y = y;
        this.z = z;
    }

    public GNode(Point2D point) {
        this.x = point.getX();
        this.y = point.getY();
    }

    public void setPos(Point2D position) {
        this.x = position.getX();
        this.y = position.getY();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public RoadMapMarker getMarker() {
        return marker;
    }

    public void setMarker(RoadMapMarker marker) {
        this.marker = marker;
    }
}
