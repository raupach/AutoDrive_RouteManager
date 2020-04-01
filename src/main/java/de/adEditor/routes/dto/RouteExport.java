package de.adEditor.routes.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class RouteExport {

    private Waypoints waypoints;

    @JacksonXmlElementWrapper(localName = "markers")
    @JacksonXmlProperty(localName = "m")
    private List<Marker> markers = new ArrayList<Marker>();

    @JacksonXmlElementWrapper(localName = "groups")
    @JacksonXmlProperty(localName = "g")
    private List<Group> groups = new ArrayList<Group>();

    public List<Marker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<Marker> markers) {
        this.markers = markers;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public Waypoints getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(Waypoints waypoints) {
        this.waypoints = waypoints;
    }
}
