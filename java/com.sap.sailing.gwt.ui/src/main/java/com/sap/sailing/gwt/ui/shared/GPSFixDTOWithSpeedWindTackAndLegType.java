package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class GPSFixDTOWithSpeedWindTackAndLegType extends GPSFixDTO implements IsSerializable {
    public SpeedWithBearingDTO speedWithBearing;
    
    public Bearing optionalTrueHeading;

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
    
    public Double detailValue;
    
    @Deprecated // GWT serialization only
    GPSFixDTOWithSpeedWindTackAndLegType() {}

    public GPSFixDTOWithSpeedWindTackAndLegType(Date timepoint, Position position, SpeedWithBearingDTO speedWithBearing, Bearing optionalTrueHeading,
            WindDTO wind, Tack tack, LegType legType, boolean extrapolated) {
        this(timepoint, position, speedWithBearing, optionalTrueHeading, (speedWithBearing != null && wind != null) ?
                                new DegreeBearingImpl(speedWithBearing.bearingInDegrees).getDifferenceTo(
                                        new DegreeBearingImpl(wind.dampenedTrueWindFromDeg)).getDegrees() : null, tack, legType, extrapolated);
    }
    
    public GPSFixDTOWithSpeedWindTackAndLegType(Date timepoint, Position position, SpeedWithBearingDTO speedWithBearing, Bearing optionalTrueHeading,
            WindDTO wind, Tack tack, LegType legType, boolean extrapolated, Double detailValue) {
        this(timepoint, position, speedWithBearing, optionalTrueHeading, (speedWithBearing != null && wind != null) ?
                                new DegreeBearingImpl(speedWithBearing.bearingInDegrees).getDifferenceTo(
                                        new DegreeBearingImpl(wind.dampenedTrueWindFromDeg)).getDegrees() : null, tack, legType, extrapolated, detailValue);
    }

    public GPSFixDTOWithSpeedWindTackAndLegType(Date timepoint, Position position, SpeedWithBearingDTO speedWithBearing, Bearing optionalTrueHeading,
            Double degreesBoatToTheWind, Tack tack, LegType legType, boolean extrapolated) {
        this(timepoint, position, speedWithBearing, optionalTrueHeading, degreesBoatToTheWind, tack, legType, extrapolated, /* detailValue */ null);
    }
    
    public GPSFixDTOWithSpeedWindTackAndLegType(Date timepoint, Position position, SpeedWithBearingDTO speedWithBearing, Bearing optionalTrueHeading,
            Double degreesBoatToTheWind, Tack tack, LegType legType, boolean extrapolated, Double detailValue) {
        super(timepoint, position);
        this.timepoint = timepoint;
        this.position = position;
        this.speedWithBearing = speedWithBearing;
        this.optionalTrueHeading = optionalTrueHeading;
        this.tack = tack;
        this.legType = legType;
        this.extrapolated = extrapolated;
        this.degreesBoatToTheWind = degreesBoatToTheWind;
        this.detailValue = detailValue;
    }

    @Override
    public String toString() {
        return "GPSFixDTOWithSpeedWindTackAndLegType [speedWithBearing=" + speedWithBearing + ", extrapolated="
                + extrapolated + ", tack=" + tack + ", legType=" + legType + ", degreesBoatToTheWind="
                + degreesBoatToTheWind + ", detailValue=" + detailValue + ", timepoint=" + timepoint + ", position="
                + position + "]";
    }
}
