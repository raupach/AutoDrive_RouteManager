package de.adEditor.routes.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Marker {

    @JacksonXmlProperty(isAttribute = true, localName = "i")
    private Integer waypointIndex;

    @JacksonXmlProperty(isAttribute = true, localName = "n")
    private String name;

    @JacksonXmlProperty(isAttribute = true, localName = "g")
    private String group;

    public Marker() {
    }

    public Marker(String name, String group, Integer waypointIndex) {
        this.name = name;
        this.group = group;
        this.waypointIndex = waypointIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Integer getWaypointIndex() {
        return waypointIndex;
    }

    public void setWaypointIndex(Integer waypointIndex) {
        this.waypointIndex = waypointIndex;
    }
}
