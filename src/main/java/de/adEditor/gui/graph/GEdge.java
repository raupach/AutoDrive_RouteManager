package de.adEditor.gui.graph;

import org.jgrapht.graph.DefaultEdge;

public class GEdge extends DefaultEdge {
    private boolean selected = false;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
