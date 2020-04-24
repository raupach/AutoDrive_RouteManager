package de.adEditor.gui.graph;

import org.jgrapht.graph.DefaultEdge;

import java.awt.geom.Point2D;

public class GEdge extends DefaultEdge {
    private boolean selected = false;
    private Point2D midpoint = null;

    public GEdge() {
    }

    public GEdge(Point2D.Double midpoint) {
        this.midpoint = midpoint;
    }

    public GEdge(Point2D.Double midpoint, boolean selected) {
        this.midpoint = midpoint;
        this.selected = selected;
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
}
