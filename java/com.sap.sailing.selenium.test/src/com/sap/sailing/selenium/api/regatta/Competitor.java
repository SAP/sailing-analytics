package com.sap.sailing.selenium.api.regatta;

import java.util.UUID;

import org.json.simple.JSONObject;

import com.sap.sailing.selenium.api.core.JsonWrapper;

public class Competitor extends JsonWrapper {

    public Competitor(JSONObject json) {
        super(json);
    }

    public UUID getId() {
        return UUID.fromString(get("id"));
    }

    public String getName() {
        return get("name");
    }

    public String getShortName() {
        return get("shortName");
    }

    public String getColor() {
        return get("displayColor");
    }

    public String getNationality() {
        return get("nationality");
    }

    public String getNationalityISO2() {
        return get("nationalityISO2");
    }

    public String getNationalityISO3() {
        return get("nationalityISO3");
    }

    public String getFlagImageUri() {
        return get("flagImageUri");
    }

    public Number getTimeOnTimeFactor() {
        return get("timeOnTimeFactor");
    }

    public Number getTimeOnDistanceAllowanceInSecondsPerNauticalMile() {
        return get("timeOnDistanceAllowanceInSecondsPerNauticalMile");
    }

    public Team getTeam() {
        return new Team(get("team"));
    }

    public Boat getBoat() {
        return new Boat(get("boat"));
    }

    public class Team extends JsonWrapper {

        public Team(JSONObject json) {
            super(json);
        }

        public String getName() {
            return getJson() != null ? get("name") : null;
        }

        public String getNationality() {
            return getJson() != null ? (String) ((JSONObject) get("nationality")).get("IOC") : null;
        }

        public String getTeamImageUri() {
            return getJson() != null ? get("imageUri") : null;
        }
    }

    public class Boat extends JsonWrapper {

        public Boat(JSONObject json) {
            super(json);
        }

        public UUID getId() {
            return UUID.fromString(get("id"));
        }

        public String getName() {
            return get("name");
        }

        public String getSailId() {
            return get("sailId");
        }

        public String getColor() {
            return get("color");
        }

        public BoatClass getBoatClass() {
            return new BoatClass(get("boatClass"));
        }
    }

    public class BoatClass extends JsonWrapper {

        public BoatClass(JSONObject json) {
            super(json);
        }

        public String getName() {
            return get("name");
        }

        public String getDisplayName() {
            return get("displayName");
        }

        public Boolean getTypciallyStartsUpwind() {
            return get("typicallyStartsUpwind");
        }

        public Double getHullLengthInMeters() {
            return get("hullLengthInMeters");
        }

        public Double getHullBeamInMeters() {
            return get("hullBeamInMeters");
        }
    }
}