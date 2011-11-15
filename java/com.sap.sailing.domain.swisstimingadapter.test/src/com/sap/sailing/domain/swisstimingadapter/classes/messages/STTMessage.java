package com.sap.sailing.domain.swisstimingadapter.classes.messages;

import java.util.Date;

public class STTMessage {
    private String raceId;
    private Date startTime;
    public STTMessage(String raceId, Date startTime) {
        super();
        this.raceId = raceId;
        this.startTime = startTime;
    }
    public STTMessage() {
        super();
    }
    public String getRaceId() {
        return raceId;
    }
    public void setRaceId(String raceId) {
        this.raceId = raceId;
    }
    public Date getStartTime() {
        return startTime;
    }
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
  
}
