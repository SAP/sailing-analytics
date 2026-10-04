package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public class SimulatorResultsDTO implements IsSerializable {

    private long version;
    private int leg;
    private TimePoint startTime;
    private Duration timeStep;
    private Duration legDuration;
    private RaceMapDataDTO raceCourse;
    private WindFieldDTO windField;
    private PathDTO[] paths;
    private String notificationMessage;

    @Deprecated
    SimulatorResultsDTO() { // for GWT RPC serialization only
    }

    public SimulatorResultsDTO(final long version, final int leg, final TimePoint startTime, final Duration timeStep,
            final Duration legDuration, final RaceMapDataDTO raceCourse, final PathDTO[] paths,
            final WindFieldDTO windField, final String notificationMessage) {
        this.version = version;
        this.leg = leg;
        this.startTime = startTime;
        this.timeStep = timeStep;
        this.legDuration = legDuration;
        this.raceCourse = raceCourse;
        this.paths = paths;
        this.windField = windField;
        this.notificationMessage = notificationMessage;
    }
    
    public long getVersion() {
        return this.version;
    }
    
    public int getLeg() {
        return this.leg;
    }
    
    public TimePoint getStartTime() {
        return this.startTime;
    }

    public Duration getTimeStep() {
        return this.timeStep;
    }

    public Duration getLegDuration() {
        return this.legDuration;
    }

    public RaceMapDataDTO getRaceCourse() {
        return this.raceCourse;
    }

    public WindFieldDTO getWindField() {
        return this.windField;
    }

    public PathDTO[] getPaths() {
        return this.paths;
    }

    public String getNotificationMessage() {
        return this.notificationMessage;
    }
}
