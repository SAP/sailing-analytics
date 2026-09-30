package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.tracking.impl.AbstractGPSFixImpl;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.ManeuverLoss;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

/**
 * @author Axel Uhl (d043530)
 *
 */
public abstract class ManeuverImpl extends AbstractGPSFixImpl implements Maneuver {
    private static final long serialVersionUID = -5317959066507472580L;
    private final ManeuverType type;
    private final Tack newTack;
    private final Position position;
    private final TimePoint timePoint;
    private final double maxTurningRateInDegreesPerSecond;
    private final ManeuverCurveBoundaries mainCurveBoundaries;
    private final ManeuverCurveBoundaries maneuverCurveWithStableSpeedAndCourseBoundaries;
    private final MarkPassing markPassing;
    private final ManeuverLoss maneuverLoss;

    public ManeuverImpl(ManeuverType type, Tack newTack, Position position, TimePoint timePoint,
            ManeuverCurveBoundaries mainCurveBoundaries,
            ManeuverCurveBoundaries maneuverCurveWithStableSpeedAndCourseBoundaries,
            double maxTurningRateInDegreesPerSecond, MarkPassing markPassing, ManeuverLoss maneuverLoss) {
        this.type = type;
        this.newTack = newTack;
        this.position = position;
        this.timePoint = timePoint;
        this.mainCurveBoundaries = mainCurveBoundaries;
        this.maneuverCurveWithStableSpeedAndCourseBoundaries = maneuverCurveWithStableSpeedAndCourseBoundaries;
        this.maxTurningRateInDegreesPerSecond = maxTurningRateInDegreesPerSecond;
        this.markPassing = markPassing;
        this.maneuverLoss = maneuverLoss;
    }

    @Override
    public ManeuverType getType() {
        return type;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    @Override
    public ManeuverCurveBoundaries getMainCurveBoundaries() {
        return mainCurveBoundaries;
    }

    @Override
    public ManeuverCurveBoundaries getManeuverCurveWithStableSpeedAndCourseBoundaries() {
        return maneuverCurveWithStableSpeedAndCourseBoundaries;
    }

    @Override
    public Tack getNewTack() {
        return newTack;
    }

    @Override
    public double getDirectionChangeInDegrees() {
        return getManeuverBoundaries().getDirectionChangeInDegrees();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingBefore() {
        return getManeuverBoundaries().getSpeedWithBearingBefore();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingAfter() {
        return getManeuverBoundaries().getSpeedWithBearingAfter();
    }

    @Override
    public Speed getLowestSpeed() {
        return getManeuverBoundaries().getLowestSpeed();
    }

    @Override
    public String toString() {
        return super.toString() + " " + type + " on new tack " + newTack + " on position " + position
                + " at time point " + timePoint + ", " + getManeuverBoundaries() + ", max. turning rate: "
                + maxTurningRateInDegreesPerSecond
                + (getManeuverLoss() == null ? "" : ", Lost approximately " + getManeuverLoss().getProjectedDistanceLost()) + ", Mark passing: "
                + markPassing;
    }

    @Override
    public double getMaxTurningRateInDegreesPerSecond() {
        return maxTurningRateInDegreesPerSecond;
    }

    @Override
    public Duration getDuration() {
        return getManeuverBoundaries().getDuration();
    }

    @Override
    public MarkPassing getMarkPassing() {
        return markPassing;
    }

    @Override
    public boolean isMarkPassing() {
        return markPassing != null;
    }

    @Override
    public NauticalSide getToSide() {
        return getMainCurveBoundaries().getDirectionChangeInDegrees() < 0 ? NauticalSide.PORT : NauticalSide.STARBOARD;
    }
    
    @Override
    public double getAvgTurningRateInDegreesPerSecond() {
        return Math.abs(getMainCurveBoundaries().getDirectionChangeInDegrees())
                / getMainCurveBoundaries().getDuration().asSeconds();
    }
    
    @Override
    public ManeuverLoss getManeuverLoss() {
        return maneuverLoss;
    }
    

}
