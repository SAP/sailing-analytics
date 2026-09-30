package com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage.precalculation;

import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.Position;

/**
 * In order to calculate the startline adavantage by wind, the data objects defined in this class
 * need to be defined.
 * 
 * @author Alexander Ries (D062114)
 *
 */
public class StartlineAdvantageCalculationData {
    private Position startBoatPosition;
    private Position pinEndPosition;
    private Position firstMarkPosition;
    private Double startlineAdvantageAtPinEndInMeters;
    private Double startlineLenghtInMeters;
    private Wind wind;
    private Double maneuverAngle;
    
    public StartlineAdvantageCalculationData(Position startBoatPosition, Position pinEndPosition,
            Position firstMarkPosition, Double startlineAdvantageAtPinEndInMeters, Double startlineLenghtInMeters,
            Wind wind, Double maneuverAngle) {
        this.startBoatPosition = startBoatPosition;
        this.pinEndPosition = pinEndPosition;
        this.firstMarkPosition = firstMarkPosition;
        this.startlineAdvantageAtPinEndInMeters = startlineAdvantageAtPinEndInMeters;
        this.startlineLenghtInMeters = startlineLenghtInMeters;
        this.wind = wind;
        this.maneuverAngle = maneuverAngle;
    }

    public Position getStartBoatPosition() {
        return startBoatPosition;
    }
    
    public Position getPinEndPosition() {
        return pinEndPosition;
    }
    
    public Position getFirstMarkPosition() {
        return firstMarkPosition;
    }
    
    public Double getStartlineAdvantageAtPinEndInMeters() {
        return startlineAdvantageAtPinEndInMeters;
    }
    
    public Double getStartlineLenghtInMeters() {
        return startlineLenghtInMeters;
    }
    
    public Wind getWind() {
        return wind;
    }
    
    public Double getManeuverAngle() {
        return maneuverAngle;
    }
}
