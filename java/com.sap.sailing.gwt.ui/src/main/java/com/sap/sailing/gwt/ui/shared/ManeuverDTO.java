package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.dto.PositionDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class ManeuverDTO implements IsSerializable {
    public ManeuverType type;
    
    public Tack newTack;
    
    public PositionDTO position;
    
    public Date timepoint;
    
    public SpeedWithBearingDTO speedWithBearingBefore;
    
    public SpeedWithBearingDTO speedWithBearingAfter;
    
    public double directionChangeInDegrees;
    
    public Double maneuverLossInMeters;

    public ManeuverDTO() {}
    
    public ManeuverDTO(ManeuverType type, Tack newTack, PositionDTO position, Date timepoint, SpeedWithBearingDTO speedWithBearingBefore,
            SpeedWithBearingDTO speedWithBearingAfter, double directionChangeInDegrees, Double maneuverLossInMeters) {
        super();
        this.type = type;
        this.newTack = newTack;
        this.position = position;
        this.timepoint = timepoint;
        this.speedWithBearingBefore = speedWithBearingBefore;
        this.speedWithBearingAfter = speedWithBearingAfter;
        this.directionChangeInDegrees = directionChangeInDegrees;
        this.maneuverLossInMeters = maneuverLossInMeters;
    }

    public String toString(StringMessages stringMessages) {
        SpeedWithBearingDTO before = this.speedWithBearingBefore;
        SpeedWithBearingDTO after = this.speedWithBearingAfter;
        
        String timeAndManeuver = DateTimeFormat.getFormat(PredefinedFormat.TIME_FULL).format(this.timepoint)
                + ": " + this.type.name();
        String directionChange = stringMessages.directionChange() + ": "
                + ((int) this.directionChangeInDegrees) + " "+stringMessages.degreesShort()+" ("
                + ((int) before.bearingInDegrees) + " deg -> " + ((int) after.bearingInDegrees) + " "+stringMessages.degreesShort()+")";
        String speedChange = stringMessages.speedChange() + ": " 
                + NumberFormat.getDecimalFormat().format(after.speedInKnots - before.speedInKnots) + " "+stringMessages.knotsUnit()+" ("
                + NumberFormat.getDecimalFormat().format(before.speedInKnots) + " "+stringMessages.knotsUnit()+" -> "
                + NumberFormat.getDecimalFormat().format(after.speedInKnots) + " "+stringMessages.knotsUnit()+")";
        String maneuverLoss = this.maneuverLossInMeters == null ? "" : ("; "+stringMessages.maneuverLoss()+": "+
                NumberFormat.getDecimalFormat().format(this.maneuverLossInMeters)+"m");
        String maneuverTitle = timeAndManeuver + "; " + directionChange + "; " + speedChange + maneuverLoss;
        return maneuverTitle;
    }
}
