package com.sap.sailing.simulator;

import java.util.Map;

import com.sap.sailing.domain.common.PathType;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class SimulationResults {

    private TimePoint startTime;
    private Duration timeStep;
    private Duration legDuration;
    private Position startPosition;
    private Position endPosition;
    private Map<PathType, Path> paths;
    private String notificationMessage;
    private TimePoint version;

    @Deprecated
    SimulationResults() { // for GWT RPC serialization only
        this.startTime = null;
        this.timeStep = null;
        this.legDuration = null;
        this.paths = null;
        this.notificationMessage = "";
    }

    public SimulationResults(final TimePoint startTime, final Duration timeStep, final Duration legDuration, final Position startPosition, final Position endPosition, final Map<PathType, Path> paths, final String notificationMessage, final TimePoint version) {
        this.startTime = startTime;
        this.timeStep = timeStep;
        this.legDuration = legDuration;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.paths = paths;
        this.notificationMessage = notificationMessage;
        this.version = version;
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

    public Position getStartPosition() {
        return this.startPosition;
    }
    
    public Position getEndPosition() {
        return this.endPosition;
    }
    
    public Map<PathType, Path> getPaths() {
        return this.paths;
    }

    public String getNotificationMessage() {
        return this.notificationMessage;
    }

    /**
     * The time point when the calculation of the simulation results represented by this object has finished.
     */
    public TimePoint getVersion() {
        return this.version;
    }
}
