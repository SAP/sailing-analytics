package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.gwt.ui.client.NauticalSideFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Position;

public class ManeuverDTO implements IsSerializable {
    private ManeuverType type;

    private Tack newTack;

    private Position position;

    private Date timePoint;

    private Date timePointBefore;
    
    private SpeedWithBearingDTO speedWithBearingBefore;

    private SpeedWithBearingDTO speedWithBearingAfter;

    private double directionChangeInDegrees;

    private double maxTurningRateInDegreesPerSecond;

    private double avgTurningRateInDegreesPerSecond;

    private double lowestSpeedInKnots;

    private Date markPassingTimePoint;

    private NauticalSide markPassingSide;
    
    private ManeuverLossDTO maneuverLoss;

    @Deprecated // for GWT serialization only
    ManeuverDTO() {}

    public ManeuverDTO(ManeuverType type, Tack newTack, Position position, Date timePoint, Date timePointBefore,
            SpeedWithBearingDTO speedWithBearingBefore, SpeedWithBearingDTO speedWithBearingAfter,
            double directionChangeInDegrees, double maxTurningRateInDegreesPerSecond,
            double avgTurningRateInDegreesPerSecond, double lowestSpeedInKnots, Date markPassingTimePoint,
            NauticalSide markPassingSide, ManeuverLossDTO maneuverLoss) {
        this.type = type;
        this.newTack = newTack;
        this.position = position;
        this.timePoint = timePoint;
        this.timePointBefore = timePointBefore;
        this.speedWithBearingBefore = speedWithBearingBefore;
        this.speedWithBearingAfter = speedWithBearingAfter;
        this.directionChangeInDegrees = directionChangeInDegrees;
        this.maxTurningRateInDegreesPerSecond = maxTurningRateInDegreesPerSecond;
        this.avgTurningRateInDegreesPerSecond = avgTurningRateInDegreesPerSecond;
        this.lowestSpeedInKnots = lowestSpeedInKnots;
        this.markPassingTimePoint = markPassingTimePoint;
        this.markPassingSide = markPassingSide;
        this.maneuverLoss = maneuverLoss;
    }

    public String toString(StringMessages stringMessages) {
        SpeedWithBearingDTO before = this.getSpeedWithBearingBefore();
        SpeedWithBearingDTO after = this.getSpeedWithBearingAfter();

        final DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(PredefinedFormat.TIME_FULL);
        String timeAndManeuver = dateTimeFormat.format(this.getTimePoint()) + ": " + this.getType().name();
        String timePointBefore = " (started: " + dateTimeFormat.format(this.getTimePointBefore()) + ")";
        String directionChange = stringMessages.directionChange() + ": "
                + ((int) Math.round(this.getDirectionChangeInDegrees())) + " " + stringMessages.degreesShort() + " ("
                + ((int) Math.round(before.bearingInDegrees)) + " " + stringMessages.degreesShort() + " -> "
                + ((int) Math.round(after.bearingInDegrees)) + " " + stringMessages.degreesShort() + ")";
        String speedChange = stringMessages.speedChange() + ": "
                + NumberFormat.getDecimalFormat().format(after.speedInKnots - before.speedInKnots) + " "
                + stringMessages.knotsUnit() + " (" + NumberFormat.getDecimalFormat().format(before.speedInKnots) + " "
                + stringMessages.knotsUnit() + " -> " + NumberFormat.getDecimalFormat().format(after.speedInKnots) + " "
                + stringMessages.knotsUnit() + ")";
        String maxTurningRate = stringMessages.maxTurningRate() + ": "
                + NumberFormat.getDecimalFormat().format(this.getMaxTurningRateInDegreesPerSecond()) + " "
                + stringMessages.degreesPerSecondUnit();
        String avgTurningRate = stringMessages.avgTurningRate() + ": "
                + NumberFormat.getDecimalFormat().format(this.getAvgTurningRateInDegreesPerSecond()) + " "
                + stringMessages.degreesPerSecondUnit();
        String lowestSpeed = stringMessages.lowestSpeed() + ": "
                + NumberFormat.getDecimalFormat().format(this.getLowestSpeedInKnots()) + " " + stringMessages.knotsUnit();
        String maneuverLoss = (this.getManeuverLoss() == null || this.getManeuverLoss().getDistanceLost() == null) ? ""
                : ("; " + stringMessages.maneuverLoss() + ": "
                        + NumberFormat.getDecimalFormat().format(this.getManeuverLoss().getDistanceLost().getMeters()) + " "
                        + stringMessages.metersUnit());
        String markPassing = getMarkPassingTimePoint() == null ? ""
                : "; " + stringMessages.markPassedToAt(
                        this.getMarkPassingSide() == null ? ""
                                : NauticalSideFormatter.format(this.getMarkPassingSide(), stringMessages),
                        DateTimeFormat.getFormat(PredefinedFormat.TIME_FULL).format(this.getMarkPassingTimePoint()));
        String maneuverTitle = timeAndManeuver + timePointBefore + "; " + directionChange + "; " + speedChange + "; "
                + maxTurningRate + "; " + avgTurningRate + "; " + lowestSpeed + maneuverLoss + markPassing;
        return maneuverTitle;
    }

    public ManeuverType getType() {
        return type;
    }

    public Tack getNewTack() {
        return newTack;
    }

    public Position getPosition() {
        return position;
    }

    public Date getTimePoint() {
        return timePoint;
    }

    public Date getTimePointBefore() {
        return timePointBefore;
    }

    public SpeedWithBearingDTO getSpeedWithBearingBefore() {
        return speedWithBearingBefore;
    }

    public SpeedWithBearingDTO getSpeedWithBearingAfter() {
        return speedWithBearingAfter;
    }

    public double getDirectionChangeInDegrees() {
        return directionChangeInDegrees;
    }

    public double getMaxTurningRateInDegreesPerSecond() {
        return maxTurningRateInDegreesPerSecond;
    }

    public double getAvgTurningRateInDegreesPerSecond() {
        return avgTurningRateInDegreesPerSecond;
    }

    public double getLowestSpeedInKnots() {
        return lowestSpeedInKnots;
    }

    public Date getMarkPassingTimePoint() {
        return markPassingTimePoint;
    }

    public NauticalSide getMarkPassingSide() {
        return markPassingSide;
    }

    public ManeuverLossDTO getManeuverLoss() {
        return maneuverLoss;
    }
}
