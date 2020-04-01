package de.adEditor.routes;


import de.autoDrive.NetworkServer.rest.RoutesRestPath;
import de.autoDrive.NetworkServer.rest.dto_v1.RouteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutoDriveRemoteRoutesTableModel implements TableModel {

    private List<RouteDto> routes = new ArrayList<>();
    private static Logger LOG = LoggerFactory.getLogger(AutoDriveRemoteRoutesTableModel.class);

    public AutoDriveRemoteRoutesTableModel() {
    }

    public AutoDriveRemoteRoutesTableModel(List<RouteDto> routes) {
        this.routes = routes;
    }

    public RouteDto get(int index) {
        return routes.get(index);
    }

    @Override
    public int getRowCount() {
        return routes.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int i) {
        switch (i) {
            case 0:
                return "name";
            case 1:
                return "username";
            case 2:
                return "map";
            case 3:
                return "description";
            case 4:
                return "revision";
            case 5:
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
            case 2:
            case 3:
                return String.class;
            case 4:
                return Integer.class;
            case 5:
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
        RouteDto route = routes.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return route.getName();
            case 1:
                return route.getUsername();
            case 2:
                return route.getMap();
            case 3:
                return route.getDescription();
            case 4:
                return route.getRevision();
            case 5:
                SimpleDateFormat sdf = new SimpleDateFormat(RoutesRestPath.DATE_FORMAT);
                try {
                    return  sdf.parse(route.getDate());
                } catch (ParseException e) {
                    LOG.error(e.getMessage(),e);
                    return null;
                }
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
}