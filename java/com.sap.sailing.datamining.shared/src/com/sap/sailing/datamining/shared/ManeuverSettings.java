package com.sap.sailing.datamining.shared;

import com.sap.sse.common.settings.SerializableSettings;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public abstract class ManeuverSettings extends SerializableSettings {

    private static final long serialVersionUID = -393430331972342L;

    public abstract Double getMinManeuverDuration();

    public abstract Double getMaxManeuverDuration();

    public abstract Double getMinManeuverEnteringSpeedInKnots();

    public abstract Double getMaxManeuverEnteringSpeedInKnots();

    public abstract Double getMinManeuverExitingSpeedInKnots();

    public abstract Double getMaxManeuverExitingSpeedInKnots();

}
