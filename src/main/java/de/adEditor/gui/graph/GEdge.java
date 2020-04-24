package de.adEditor.gui.graph;

import org.jgrapht.graph.DefaultEdge;

import java.awt.geom.Point2D;

public class GEdge extends DefaultEdge {
    private boolean selected = false;
    private Point2D midpoint = null;

    public GEdge() {
    }

    public GEdge(GNode source, GNode target) {
        this.midpoint = calcMidpoint(source, target);
    }

    public GEdge(GNode source, GNode target, boolean selected) {
        this.midpoint = calcMidpoint(source, target);
        this.selected = selected;
    }

    private Point2D.Double calcMidpoint(GNode source, GNode target) {
        return  new Point2D.Double ((source.getX() + target.getX()) / 2, (source.getY() + target.getY()) / 2) ;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Point2D getMidpoint() {
        return midpoint;
    }

    public void setMidpoint(Point2D midpoint) {
        this.midpoint = midpoint;
    }

    public void setMidpoint(GNode source, GNode target) {
        midpoint = calcMidpoint(source, target);
    }
}
