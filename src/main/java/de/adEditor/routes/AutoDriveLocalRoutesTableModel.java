package de.adEditor.routes;


import de.adEditor.routes.dto.GameSettings;
import de.adEditor.routes.dto.Route;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutoDriveLocalRoutesTableModel implements TableModel {

    private List<Route> routes = new ArrayList<>();
    private String username;

    public AutoDriveLocalRoutesTableModel() {
    }

    public AutoDriveLocalRoutesTableModel(List<Route> routes, GameSettings gameSettings) {
        this.routes = routes;
        if ( gameSettings.getPlayer() != null) {
            this.username = gameSettings.getPlayer().getName();
        }
    }

    public Route get(int index) {
        return routes.get(index);
    }

    @Override
    public int getRowCount() {
        return routes.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int i) {
        switch (i) {
            case 0:
                return "name";
            case 1:
                return "map";
            case 2:
                return "revision";
            case 3:
                return "date";
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int i) {
        switch (i) {
            case 0:
            case 1:
                return String.class;
            case 2:
                return Integer.class;
            case 3:
                return Date.class;
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Route route = routes.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return route.getName();
            case 1:
                return route.getMap();
            case 2:
                return route.getRevision();
            case 3:
                return route.getDate();
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object o, int i, int i1) {

    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {

    }

    public String getUsername() {
        return username;
    }
}