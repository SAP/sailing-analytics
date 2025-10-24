package com.sap.sailing.gwt.home.communication.anniversary;

import java.util.UUID;

import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.domain.common.dto.EventType;
import com.sap.sse.gwt.shared.DTO;

/**
 * {@link DTO} object representing an anniversary entry (count-down or announcement) to be shown on the start page.
 */
public class AnniversaryDTO implements DTO {

    private int target;
    private int currentRaceCount;
    private EventType eventType;
    private AnniversaryType type;
    private UUID eventID;
    private String leaderboardName;
    private String remoteUrl;
    private String regattaName;
    private String raceName;
    private String leaderboardDisplayName;
    private String eventName;

    /**
     * Gwt Serialisation only constructor
     */
    protected AnniversaryDTO() {
    }

    AnniversaryDTO(int target, int currentRaceCount, AnniversaryType type) {
        this.target = target;
        this.currentRaceCount = currentRaceCount;
        this.type = type;
    }

    AnniversaryDTO(int target,int currentRaceCount, AnniversaryType type, UUID eventID, String leaderboardName, String remoteUrl,
            String raceName, String regattaName, String eventName, String leaderboardDisplayName,
            EventType eventType) {
        this(target, currentRaceCount, type);
        this.eventID = eventID;
        this.leaderboardName = leaderboardName;
        this.remoteUrl = remoteUrl;
        this.raceName = raceName;
        this.regattaName = regattaName;
        this.eventName = eventName;
        this.leaderboardDisplayName = leaderboardDisplayName;
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getTarget() {
        return target;
    }

    public String getEventName() {
        return eventName;
    }

    public String getLeaderboardDisplayName() {
        return leaderboardDisplayName;
    }

    public int getCurrentRaceCount() {
        return currentRaceCount;
    }
    
    public int getCountdown() {
        return target - currentRaceCount;
    }

    public AnniversaryType getType() {
        return type;
    }

    public UUID getEventID() {
        return eventID;
    }

    public String getLeaderboardName() {
        return leaderboardName;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getRegattaName() {
        return regattaName;
    }

    public String getRaceName() {
        return raceName;
    }

    public boolean isAnnouncement() {
        return eventID != null && leaderboardName != null && regattaName != null
                && raceName != null;
    }

    @Override
    public String toString() {
        return "AnniversaryInformation [target=" + target + ", countdown=" + currentRaceCount + ", type=" + type + ", eventID="
                + eventID + ", leaderboardName=" + leaderboardName + ", remoteUrl=" + remoteUrl + ", regattaName="
                + regattaName + ", raceName=" + raceName + "]";
    }
}