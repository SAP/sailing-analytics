package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreePosition;

public class SimulatorWindDTO implements IsSerializable {
    public Boolean isTurn;
    public Double trueWindSpeedInKnots;
    public Double trueWindBearingDeg;
    public Position position;
    public TimePoint timepoint;

    public SimulatorWindDTO() {
        this.isTurn = false;
        this.trueWindBearingDeg = 0.0;
        this.trueWindSpeedInKnots = 0.0;
        this.position = null;
        this.timepoint = null;
    }

    public SimulatorWindDTO(Position position, double windSpeedKn, double windBearingDeg, TimePoint timepoint) {
        this.position = position;
        this.trueWindBearingDeg = windBearingDeg;
        this.trueWindSpeedInKnots = windSpeedKn;
        this.timepoint = timepoint;
        this.isTurn = false;
    }

    public SimulatorWindDTO(double latDeg, double lngDeg, double windSpeedKn, double windBearingDeg, TimePoint timepoint) {
        this.position = new DegreePosition(latDeg, lngDeg);
        this.trueWindBearingDeg = windBearingDeg;
        this.trueWindSpeedInKnots = windSpeedKn;
        this.timepoint = timepoint;
        this.isTurn = false;
    }

    public SimulatorWindDTO(double latDeg, double lngDeg, double windSpeedKn, double windBearingDeg, TimePoint timepoint, boolean isTurn) {
        this.position = new DegreePosition(latDeg, lngDeg);
        this.trueWindBearingDeg = windBearingDeg;
        this.trueWindSpeedInKnots = windSpeedKn;
        this.timepoint = timepoint;
        this.isTurn = isTurn;
    }

    @Override
    public int hashCode() {

        int prime = 31;
        int result = 1;

        long temp = 0;

        temp = this.trueWindSpeedInKnots.intValue();
        result = prime * result + (int) (temp ^ (temp >>> 32));

        temp = this.trueWindBearingDeg.intValue();
        result = prime * result + (int) (temp ^ (temp >>> 32));

        temp = this.timepoint.asMillis();
        result = prime * result + (int) (temp ^ (temp >>> 32));

        temp = this.position.hashCode();
        result = prime * result + (int) (temp ^ (temp >>> 32));

        return result;

    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else {
            if (o instanceof SimulatorWindDTO) {
                SimulatorWindDTO other = (SimulatorWindDTO) o;
                return this.position.equals(other.position) && (Math.abs(this.timepoint.until(other.timepoint).asMillis()) < 9999);
            }
            return false;
        }
    }
}
