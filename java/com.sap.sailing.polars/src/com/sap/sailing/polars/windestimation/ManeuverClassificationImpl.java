package com.sap.sailing.polars.windestimation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CompetitorAndBoat;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

/**
 * Collects data about a maneuver that will be used to assign probabilities for maneuver types such as tack or jibe,
 * looking at a larger set of such objects and finding the most probable overall solution.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ManeuverClassificationImpl implements ManeuverClassification {
    private final String competitorName;
    private final TimePoint timePoint;
    private final Position position;
    /**
     * Course change implied by the maneuver
     */
    private final double maneuverAngleDeg;
    private final SpeedWithBearing speedAtManeuverStart;
    private final Bearing middleManeuverCourse;
    private final Distance maneuverLossDistanceLost;
    protected Double[] likelihoodPerManeuverType;
    private ScalableBearingAndScalableDouble scalableMiddleManeuverCourseAndManeuverAngleDegCache;
    private final PolarDataService polarService;
    private final BoatClass boatClass;

    public ManeuverClassificationImpl(String competitorName, BoatClass boatClass, TimePoint timePoint,
            Position position, double maneuverAngleDeg, SpeedWithBearing speedAtManeuverStart,
            Bearing middleManeuverCourse, Distance maneuverLossDistanceLost, PolarDataService polarService) {
        this.competitorName = competitorName;
        this.timePoint = timePoint;
        this.position = position;
        this.maneuverAngleDeg = maneuverAngleDeg;
        this.speedAtManeuverStart = speedAtManeuverStart;
        this.middleManeuverCourse = middleManeuverCourse;
        this.maneuverLossDistanceLost = maneuverLossDistanceLost;
        this.polarService = polarService;
        this.boatClass = boatClass;
        this.likelihoodPerManeuverType = new Double[ManeuverType.values().length];
    }

    public ManeuverClassificationImpl(CompetitorAndBoat competitorAndBoat, Maneuver maneuver,
            PolarDataService polarService) {
        this.boatClass = competitorAndBoat.getBoat().getBoatClass();
        this.polarService = polarService;
        this.competitorName = competitorAndBoat.getCompetitor().getName();
        this.timePoint = maneuver.getTimePoint();
        this.position = maneuver.getPosition();
        this.maneuverAngleDeg = maneuver.getDirectionChangeInDegrees();
        this.speedAtManeuverStart = maneuver.getSpeedWithBearingBefore();
        this.middleManeuverCourse = maneuver.getSpeedWithBearingBefore().getBearing()
                .middle(maneuver.getSpeedWithBearingAfter().getBearing());
        this.maneuverLossDistanceLost = maneuver.getManeuverLoss() == null ? null
                : maneuver.getManeuverLoss().getProjectedDistanceLost();
        this.likelihoodPerManeuverType = new Double[ManeuverType.values().length];
    }

    public String getCompetitorName() {
        return competitorName;
    }

    public BoatClass getBoatClass() {
        return boatClass;
    }

    public TimePoint getTimePoint() {
        return timePoint;
    }

    public Position getPosition() {
        return position;
    }

    public double getManeuverAngleDeg() {
        return maneuverAngleDeg;
    }

    public SpeedWithBearing getSpeedAtManeuverStart() {
        return speedAtManeuverStart;
    }

    public Bearing getMiddleManeuverCourse() {
        return middleManeuverCourse;
    }

    public ScalableBearingAndScalableDouble getScalableMiddleManeuverCourseAndManeuverAngleDeg() {
        if (scalableMiddleManeuverCourseAndManeuverAngleDegCache == null) {
            scalableMiddleManeuverCourseAndManeuverAngleDegCache = new ScalableBearingAndScalableDouble(
                    getMiddleManeuverCourse(), getManeuverAngleDeg());
        }
        return scalableMiddleManeuverCourseAndManeuverAngleDegCache;
    }

    public SpeedWithBearingWithConfidence<Void> getEstimatedWindSpeedAndBearing(ManeuverType maneuverType) {
        Pair<Double, SpeedWithBearingWithConfidence<Void>> likelihoodAndTWSBasedOnSpeedAndAngle = polarService
                .getManeuverLikelihoodAndTwsTwa(getBoatClass(), getSpeedAtManeuverStart(), getManeuverAngleDeg(),
                        maneuverType);
        final SpeedWithBearingWithConfidenceImpl<Void> result;
        // if no reasonable wind speed was found for the maneuver speed, return null
        if (likelihoodAndTWSBasedOnSpeedAndAngle.getB() == null) {
            result = null;
        } else {
            final Bearing bearing;
            final double confidence = likelihoodAndTWSBasedOnSpeedAndAngle.getB().getConfidence()
                    * likelihoodAndTWSBasedOnSpeedAndAngle.getA();
            final Speed speed = likelihoodAndTWSBasedOnSpeedAndAngle.getB().getObject();
            switch (maneuverType) {
            case TACK:
                bearing = getMiddleManeuverCourse().reverse();
                break;
            case JIBE:
                bearing = getMiddleManeuverCourse();
                break;
            default:
                throw new IllegalStateException("Found leg type " + maneuverType + " but can only handle "
                        + LegType.UPWIND.name() + " and " + LegType.DOWNWIND.name());
            }
            result = new SpeedWithBearingWithConfidenceImpl<Void>(
                    new KnotSpeedWithBearingImpl(speed.getKnots(), bearing), confidence, /* relativeTo */null);
        }
        return result;
    }

    public Distance getManeuverLossDistanceLost() {
        return maneuverLossDistanceLost;
    }

    /**
     * Computes the likelihood that the maneuver represented by this object is of the <code>type</code> requested. The
     * polar service may offer more than one possible wind condition for the given speed. In this case, the true wind
     * angle that fits the actual {@link #getManeuverAngleDeg() maneuver angle} better is chosen to judge how close the
     * maneuver is to the expected angle. The likelihood as well as the estimated true wind speed are returned.
     * 
     * @return a value between 0 and 1
     */
    public double getLikelihoodForManeuverType(final ManeuverType maneuverType) {
        if (likelihoodPerManeuverType[maneuverType.ordinal()] == null) {
            Pair<Double, SpeedWithBearingWithConfidence<Void>> maneuverLikelihoodAndTwsTwa = polarService
                    .getManeuverLikelihoodAndTwsTwa(getBoatClass(), getSpeedAtManeuverStart(), getManeuverAngleDeg(),
                            maneuverType);
            likelihoodPerManeuverType[maneuverType.ordinal()] = maneuverLikelihoodAndTwsTwa.getA();
        }
        return likelihoodPerManeuverType[maneuverType.ordinal()];
    }

    @Override
    public String toString() {
        return format(/* id */null);
    }

    public String format(String id) {
        DateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        final String prefix = competitorName + "\t" + df.format(getTimePoint().asDate()) + "\t" + getManeuverAngleDeg()
                + "\t" + getSpeedAtManeuverStart().getKnots() + "\t"
                + getSpeedAtManeuverStart().getBearing().getDegrees();
        final StringBuilder result = new StringBuilder();
        if (id != null) {
            result.append("ID");
            result.append(id);
            result.append('\t');
        }
        result.append(prefix);
        result.append("\t");
        result.append(getMiddleManeuverCourse().getDegrees());
        result.append("\t");
        result.append(getManeuverLossDistanceLost() == null ? 0.0 : getManeuverLossDistanceLost().getMeters());
        result.append("\t");
        result.append(getLikelihoodForManeuverType(ManeuverType.TACK));
        result.append("\t");
        result.append(getLikelihoodForManeuverType(ManeuverType.JIBE));
        return result.toString();
    }

}