package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.dto.PositionDTO;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;

public class GPSFixDTO implements IsSerializable {
    public Date timepoint;
    public PositionDTO position;
    public SpeedWithBearingDTO speedWithBearing;

    /**
     * tells if this fix was computed by extrapolation instead of having been captured by a device directly
     * or having been interpolated between captured fixes
     */
    public boolean extrapolated;
    
    /**
     * Contains one of the literals of the {@link Tack} enumeration ("STARBOARD" or "PORT") or <code>null</code> if the tack
     * is not known
     */
    public Tack tack;
    
    /**
     * Contains one of the literals of the {@link LegType} enumeration ("UPWIND", "DOWNWIND" or "REACHING") or
     * <code>null</code> if the leg type is not known, e.g., because the competitor is not currently on any of the
     * race's legs.
     */
    public LegType legType;
    
    /**
     * degrees between boat's course over ground and the dampened true wind direction (from)
     */
    public Double degreesBoatToTheWind;
    
    public GPSFixDTO() {}

    public GPSFixDTO(Date timepoint, PositionDTO position, SpeedWithBearingDTO speedWithBearing, WindDTO wind,
            Tack tack, LegType legType, boolean extrapolated) {
        this(timepoint, position, speedWithBearing, (speedWithBearing != null && wind != null) ?
                new DegreeBearingImpl(speedWithBearing.bearingInDegrees).getDifferenceTo(
                        new DegreeBearingImpl(wind.dampenedTrueWindFromDeg)).getDegrees() : null, tack, legType, extrapolated);
    }

    public GPSFixDTO(Date timepoint, PositionDTO position, SpeedWithBearingDTO speedWithBearing, Double degreesBoatToTheWind,
            Tack tack, LegType legType, boolean extrapolated) {
        super();
        this.timepoint = timepoint;
        this.position = position;
        this.speedWithBearing = speedWithBearing;
        this.tack = tack;
        this.legType = legType;
        this.extrapolated = extrapolated;
        this.degreesBoatToTheWind = degreesBoatToTheWind;
    }
}
