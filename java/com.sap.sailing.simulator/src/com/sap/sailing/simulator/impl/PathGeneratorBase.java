package com.sap.sailing.simulator.impl;

import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PathGenerator;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorBase implements PathGenerator {
    protected SimulationParameters parameters;
    // maximum duration of one path generation; two minutes
    protected final Duration algorithmMaxDuration = Duration.ONE_MINUTE.times(2); 
    protected TimePoint algorithmStartTime;
    protected boolean algorithmTimedOut = false;

    public PathGeneratorBase() {
        this.parameters = null;
    }

    public PathGeneratorBase(SimulationParameters params) {
        this.parameters = params;
    }

    @Override
    public void setSimulationParameters(SimulationParameters params) {
        this.parameters = params;
    }

    @Override
    public SimulationParameters getSimulationParameters() {
        return this.parameters;
    }

    @Override
    public Path getPath() throws SparseSimulationDataException {
        return null;
    }

    @Override
    public boolean isTimedOut() {
        // check for time-out
        TimePoint now = MillisecondsTimePoint.now();
        if (this.algorithmStartTime.until(now).compareTo(algorithmMaxDuration) <= 0) {
            this.algorithmTimedOut = false;
        } else {
            this.algorithmTimedOut = true;
        }
        return this.algorithmTimedOut;
    }

    @Override
    public Path getPathEvenTimed(long stepMilliseconds) throws SparseSimulationDataException {

        Path path = this.getPath();

        return path == null ? null : path.getEvenTimedPath(stepMilliseconds);
    }
}
