package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * DTO holding all information to set a new start time on a specific race.
 */
public class RaceLogSetStartTimeDTO implements IsSerializable {
    public String leaderboardName;
    public String raceColumnName;
    public String fleetName;
    public int passId;
    public String authorName;
    public Integer authorPriority;
    public Date logicalTimePoint;
    public Date startTime;
}
