package de.adEditor.gui.graph;

import javafx.geometry.Point3D;

import java.awt.geom.Point2D;

import static javafx.geometry.Point3D.ZERO;

public class GNode {
    private boolean selected = false;
    private Point3D point = ZERO;
    private RoadMapMarker marker;

    public GNode(){
    }

    public Point2D getPoint2D() {
        return new Point2D.Double(point.getX(), point.getY());
    }

    public Point3D getPoint() {
        return point;
    }

    public void setPoint(Point3D point) {
        this.point = point;
    }

    public GNode(Double x, Double y, Double z) {
        point = new Point3D(x, y, z);
    }

    public GNode(Point2D point) {
        this.point = new Point3D(point.getX(), point.getY(), 0);
    }

    public void setPos(Point2D position) {
        this.point = new Point3D(point.getX(), point.getY(), this.point.getZ());
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public double getX() {
        return point.getX();
    }

    public double getY() {
        return point.getY();
    }

    public void setX(double x) {
        point = new Point3D(x, point.getY(), point.getZ());
    }

    public void setY(double y) {
        point = new Point3D(point.getX(), y, point.getZ());
    }

    public double getZ() {
        return point.getZ();
    }

    public RoadMapMarker getMarker() {
        return marker;
    }

    public void setMarker(RoadMapMarker marker) {
        this.marker = marker;
    }
}
