package de.adEditor.routes.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GameSettings {
    @JacksonXmlProperty(isAttribute = true)
    private Integer revision;

    private Player player;

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
