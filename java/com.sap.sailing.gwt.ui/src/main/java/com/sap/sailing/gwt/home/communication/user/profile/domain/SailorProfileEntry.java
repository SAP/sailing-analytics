package com.sap.sailing.gwt.home.communication.user.profile.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.dispatch.shared.commands.Result;

public class SailorProfileEntry implements Result, Serializable {
    private static final long serialVersionUID = -5957161570595861618L;

    private UUID key;
    private String name;
    private ArrayList<SimpleCompetitorWithIdDTO> competitors = new ArrayList<>();
    private ArrayList<BadgeDTO> badges = new ArrayList<>();
    private ArrayList<BoatClassDTO> boatclasses = new ArrayList<>();

    public static final UUID SAILOR_PROFILE_KEY_NEW = UUID.fromString("00000000-00000000-00000000-00000001");

    protected SailorProfileEntry() {

    }

    public SailorProfileEntry(UUID key, String name) {
        this.key = key;
        this.name = name;
    }

    public SailorProfileEntry(UUID key, String name, Iterable<SimpleCompetitorWithIdDTO> competitors,
            Iterable<BadgeDTO> badges, Iterable<BoatClassDTO> boatclasses) {
        super();
        this.key = key;
        this.name = name;
        Util.addAll(competitors, this.competitors);
        Util.addAll(badges, this.badges);
        Util.addAll(boatclasses, this.boatclasses);
    }

    public UUID getKey() {
        return key;
    }

    public Collection<SimpleCompetitorWithIdDTO> getCompetitors() {
        return competitors;
    }

    public Collection<BadgeDTO> getBadges() {
        return badges;
    }

    public Iterable<BoatClassDTO> getBoatclasses() {
        return boatclasses;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SailorProfileEntry other = (SailorProfileEntry) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

    public void setName(String newName) {
        this.name = newName;
    }

}
