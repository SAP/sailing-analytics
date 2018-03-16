package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sse.common.TimePoint;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

/**
 * Represents a maneuver detected within a competitor track. There are two important sections within a maneuver.
 * <ol>
 * <li>The first section starts from sailing a stable speed at TWA and ends at getting back to a stable speed and target
 * TWA. This section is defined as maneuver curve and its time range is represented by {@code timePointBefore} and
 * {@code timePointAfter}. The target speeds and courses are represented by {@code speedWithBearingBefore} and
 * {@code speedWithBearingAfter}.</li>
 * <li>The second section is called main curve and is defined as the section within the maneuver curve, where highest
 * course change has been performed. This means that the main curve is a subset of the maneuver curve which is
 * represented by {@code timePointBeforeMainCurve} and {@code timePointAfterMainCurve}.</li>
 * </ol>
 * The maneuver curve is an expansion of the main curve. The expansion relates the points with stable course and speed
 * before and after main curve. In contrast to maneuver curve, the main curve computation does not take speed into
 * account and is based only on gradual analysis of course changes within the maneuver progress. The main curve is
 * supposed to deliver information about the acceleration during continuous turning in the direction of maneuver which
 * can be used for boat class oriented investigations. On the other side, the maneuver curve describes a section where
 * the boat starts loosing speed and course stability due to maneuvering preparations, followed by maneuver performance,
 * acceleration and realignment to the target course on new tack. Based on the maneuver curve, the maneuver loss is
 * computed which is regarded as an important measurement feature in order to compare performances of competing racers.
 * In contrast to main curve, the maneuver curve reveals strategic decision making of individual sailors to master a
 * maneuver with minimal maneuver loss.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface Maneuver extends GPSFix {
    /**
     * Gets the type of this maneuver, e.g. whether its a tack, jibe and etc. The maneuver type is determined
     * considering the boat's course change, wind bearing and marks.
     * 
     * @return The type of maneuver
     */
    ManeuverType getType();

    /**
     * Gets the new tack applied after the maneuver. A tack in sailing is defined as the side of the boat (starboard, or
     * port) from which the wind is blowing.
     * 
     * @return The new tack after the performed maneuver
     */
    @Dimension(messageKey = "Tack", ordinal = 13)
    Tack getNewTack();

    /**
     * Gets the maneuver loss of this maneuver which is the distance projected onto the average course between entering
     * and exiting the maneuver that the boat lost compared to not having maneuvered. The maneuver loss is calculated
     * considering the maneuver curve, which was performed between {@link #getTimePointBefore()} and
     * {@link #getTimePointAfter()}.
     */
    Distance getManeuverLoss();

    /**
     * Gets the time point of the corresponding maneuver. The time point refers to a point within the main curve of
     * maneuver with the highest turning rate recorded toward the direction of maneuver. This point is called maneuver
     * climax.
     * 
     * @return The maneuver time point with the highest course change
     */
    TimePoint getTimePoint();

    /**
     * Gets time points and speeds with bearings of main curve beginning and end.
     * 
     * @return Entering and exiting details of maneuver main curve
     * @see Maneuver
     */
    ManeuverCurveBoundaries getMainCurveBoundaries();

    /**
     * Gets time points and speeds with bearings before and after the maneuver, such that the speed and course before
     * and after the maneuver are considered as stable.
     * 
     * @return Entering and exiting details of maneuver section, with stable speed and bearing before and after that
     *         section
     * @see Maneuver
     */
    ManeuverCurveBoundaries getManeuverCurveWithStableSpeedAndCourseBoundaries();

    /**
     * Gets time points and speeds with bearings before and after the maneuver considering the maneuver type. The
     * maneuver boundaries may be represented either by {@link #getMainCurveBoundaries()}, or
     * {@link #getManeuverCurveWithStableSpeedAndCourseBoundaries()}. The former is considered for HEAD_UP and BEAR_AWAY
     * maneuvers, whereas the latter is considered for the remainder.
     * 
     * @return Entering and exiting details of maneuver
     */
    ManeuverCurveBoundaries getManeuverBoundaries();

    /**
     * The maximal angular velocity recorded within the main curve at maneuver climax.
     * 
     * @return The maximal angular velocity in degrees per second
     * @see #getTimePoint()
     */
    @Statistic(messageKey = "MaxAngularVelocityInDegreesPerSecond", resultDecimals = 4, ordinal = 4)
    double getMaxAngularVelocityInDegreesPerSecond();

    /**
     * Gets the speed with bearing at maneuver start, which is at {@link #getManeuverBoundaries()}.getTimePointBefore().
     * 
     * @return The speed with bearing at maneuver start
     * @see #getManeuverBoundaries()
     * 
     */
    SpeedWithBearing getSpeedWithBearingBefore();

    /**
     * Gets the speed with bearing at maneuver end, which is at {@link #getManeuverBoundaries()}.getTimePointAfter().
     * 
     * @return The speed with bearing at maneuver end
     * @see #getManeuverBoundaries()
     */
    SpeedWithBearing getSpeedWithBearingAfter();

    /**
     * Gets the total course change performed within maneuver between
     * {@link #getManeuverBoundaries()}.getTimePointBefore() and {@link #getManeuverBoundaries()}.getTimePointAfter() in
     * degrees. The port side course changes produce a negative value. The value may exceed 360 degrees if the performed
     * maneuver is a penalty circle.
     * 
     * @return The total course change within the whole maneuver in degrees
     * @see #getManeuverBoundaries()
     */
    @Statistic(messageKey = "DirectionChange", resultDecimals = 2, ordinal = 2)
    double getDirectionChangeInDegrees();

    /**
     * Gets lowest speed recorded within {@link #getManeuverBoundaries()}.
     */
    Speed getLowestSpeed();

    /**
     * Gets the mark passing which is contained within maneuver. In case if no mark passing was passed, {@code null} is
     * returned.
     */
    MarkPassing getMarkPassing();

    /**
     * Determines whether the maneuver is mark passing maneuver.
     */
    boolean isMarkPassing();

    /**
     * Gets the direction of the maneuver. It corresponds to the direction of mark passing side.
     */
    NauticalSide getSide();

}
