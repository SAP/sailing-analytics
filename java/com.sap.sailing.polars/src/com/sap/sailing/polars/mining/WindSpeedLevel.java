package com.sap.sailing.polars.mining;

import java.io.Serializable;

import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.WindSpeedStepping;

public class WindSpeedLevel implements Serializable {

    private static final long serialVersionUID = -4498750034316806342L;
    private final int level;
    private final WindSpeedStepping stepping;

    public WindSpeedLevel(Speed windSpeed, WindSpeedStepping stepping) {
        this.level = stepping.getLevelIndexForValue(windSpeed.getKnots());
        this.stepping = stepping;
    }

    @Override
    public String toString() {
        // FIXME right now the toString equals for different steppings. This will resolve in an equal groupkey in the
        // datamining framework. The current approach is to use only the default WindStepping.
        return "" + level;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + level;
        result = prime * result + ((stepping == null) ? 0 : stepping.hashCode());
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
        WindSpeedLevel other = (WindSpeedLevel) obj;
        if (level != other.level)
            return false;
        if (stepping == null) {
            if (other.stepping != null)
                return false;
        } else if (!stepping.equals(other.stepping))
            return false;
        return true;
    }

}
