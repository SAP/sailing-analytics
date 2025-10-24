package com.sap.sailing.domain.common;

import static com.sap.sailing.domain.common.security.SecuredDomainType.LeaderboardActions.PREMIUM_LEADERBOARD_INFORMATION;
import static com.sap.sse.common.SortingOrder.ASCENDING;
import static com.sap.sse.common.SortingOrder.DESCENDING;
import static com.sap.sse.common.SortingOrder.NONE;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sap.sse.common.ColorMapper.ValueSpreader;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.HasPermissions.SecuredEnum;

/**
 * Identifies details that can be requested from the racing service. Optionally, the details can specify a precision as
 * the number of decimal digits in which they are usually provided and should be formatted. Additionally a default
 * sorting order can be specified which can be used for table oriented views.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public enum DetailType implements Serializable, SecuredEnum {
    LEG_TACKTYPE_LONGTACK_SHORTTACK(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "TACKTYPE"),
    LEG_DISTANCE_TRAVELED(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "DISTANCE_TRAVELED"),
    LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "DISTANCE_TRAVELED_INCLUDING_GATE_START"),
    LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS(2, DESCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "AVERAGE_SPEED_OVER_GROUND_IN_KNOTS"),
    RACE_RANK(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    REGATTA_RANK(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    OVERALL_RANK(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    LEG_RANK_GAIN(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "RANK_GAIN"),
    NUMBER_OF_MANEUVERS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS(2, DESCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "CURRENT_SPEED_OVER_GROUND_IN_KNOTS"),
    BRAVO_LEG_CURRENT_HEEL_IN_DEGREES(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_HEEL_IN_DEGREES"),
    BRAVO_LEG_CURRENT_PITCH_IN_DEGREES(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_PITCH_IN_DEGREES"),
    BRAVO_LEG_CURRENT_RIDE_HEIGHT_IN_METERS(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_RIDE_HEIGHT_IN_METERS"),
    BRAVOEXTENDED_LEG_CURRENT_DISTANCE_FOILED_IN_METERS(0, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_DISTANCE_FOILED_IN_METERS"),
    BRAVOEXTENDED_LEG_CURRENT_DURATION_FOILED_IN_SECONDS(0, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_DURATION_FOILED_IN_SECONDS"),
    BRAVOEXTENDED_RACE_CURRENT_PORT_DAGGERBOARD_RAKE(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_PORT_DAGGERBOARD_RAKE"),
    BRAVOEXTENDED_RACE_CURRENT_STBD_DAGGERBOARD_RAKE(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_STBD_DAGGERBOARD_RAKE"),
    BRAVOEXTENDED_RACE_CURRENT_PORT_RUDDER_RAKE(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_PORT_RUDDER_RAKE"),
    BRAVOEXTENDED_RACE_CURRENT_STBD_RUDDER_RAKE(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_STBD_RUDDER_RAKE"),
    BRAVOEXTENDED_RACE_CURRENT_MAST_ROTATION_IN_DEGREES(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_MAST_ROTATION_IN_DEGREES"),
    BRAVOEXTENDED_RACE_CURRENT_LEEWAY_IN_DEGREES(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_LEEWAY_IN_DEGREES"),
    BRAVOEXTENDED_RACE_CURRENT_SET(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_SET"),
    BRAVOEXTENDED_RACE_CURRENT_DRIFT_IN_DEGREES(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_DRIFT_IN_DEGREES"),
    BRAVOEXTENDED_RACE_CURRENT_DEPTH_IN_METERS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_DEPTH_IN_METERS"),
    BRAVOEXTENDED_RACE_CURRENT_RUDDER_IN_DEGREES(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_RUDDER_IN_DEGREES"),
    BRAVOEXTENDED_RACE_CURRENT_TACK_ANGLE_IN_DEGREES(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_TACK_ANGLE_IN_DEGREES"),
    BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_PERCENTAGE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_DEFLECTOR_PERCENTAGE"),
    BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_IN_MILLIMETERS(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_DEFLECTOR_IN_MILLIMETERS"),
    BRAVOEXTENDED_RACE_CURRENT_RAKE_IN_DEGREES(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_RAKE_IN_DEGREES"),
    BRAVOEXTENDED_RACE_CURRENT_TARGET_HEEL_ANGLE_IN_DEGREES(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_TARGET_HEEL_ANGLE_IN_DEGREES"),
    BRAVOEXTENDED_RACE_CURRENT_FORESTAY_LOAD(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_FORESTAY_LOAD"),
    BRAVOEXTENDED_RACE_CURRENT_FORESTAY_PRESSURE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_FORESTAY_PRESSURE"),
    BRAVOEXTENDED_RACE_CURRENT_TARGET_BOATSPEED_PERCENTAGE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_TARGET_BOATSPEED_PERCENTAGE"),
    LEG_ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS"),
    LEG_VELOCITY_MADE_GOOD_IN_KNOTS(2, DESCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "VELOCITY_MADE_GOOD_IN_KNOTS"),
    LEG_GAP_TO_LEADER_IN_SECONDS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "GAP_TO_LEADER_IN_SECONDS"),
    LEG_GAP_TO_LEADER_IN_SECONDS_CHANGE(2, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "GAP_TO_LEADER_IN_SECONDS_CHANGE"),
    LEG_GAP_CHANGE_SINCE_LEG_START_IN_SECONDS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "GAP_CHANGE_SINCE_LEG_START_IN_SECONDS"),
    LEG_SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED"),
    LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "WINDWARD_DISTANCE_TO_GO_IN_METERS"),
    LEG_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS"),
    LEG_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS"),
    LEG_CURRENT_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    LEG_CURRENT_SIGNED_CROSS_TRACK_ERROR_IN_METERS(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_DISTANCE_TRAVELED(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    RACE_DISTANCE_TRAVELED_INCLUDING_GATE_START(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS(2, DESCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    RACE_GAP_TO_LEADER_IN_SECONDS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_CHANGE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    START_TACK(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    DISTANCE_TO_START_AT_RACE_START(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    TIME_BETWEEN_RACE_START_AND_COMPETITOR_START(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    SPEED_OVER_GROUND_AT_RACE_START(1, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    SPEED_OVER_GROUND_WHEN_PASSING_START(1, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    TACK(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    JIBE(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    PENALTY_CIRCLE(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    AVERAGE_MANEUVER_LOSS_IN_METERS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    AVERAGE_TACK_LOSS_IN_METERS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    AVERAGE_JIBE_LOSS_IN_METERS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_CURRENT_LEG(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CURRENT_LEG"),
    RACE_DISPLAY_LEGS(0, NONE, null, ValueSpreader.CUBIC_ROOT, "DISPLAY_LEGS"),
    RACE_DISPLAY_BOATS(0, NONE, null, ValueSpreader.CUBIC_ROOT),
    LEG_TIME_TRAVELED(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "TIME_TRAVELED"),
    LEG_CORRECTED_TIME_TRAVELED(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "CORRECTED_TIME_TRAVELED"),
    TOTAL_TIME_SAILED_DOWNWIND_IN_SECONDS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    TOTAL_TIME_SAILED_UPWIND_IN_SECONDS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    TOTAL_TIME_SAILED_REACHING_IN_SECONDS(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS(1, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS"),
    OVERALL_TIME_ON_TIME_FACTOR(4, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "TIME_ON_TIME_FACTOR"),
    OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE(
            0,
            ASCENDING,
            null,
            ValueSpreader.CUBIC_ROOT, "TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE"),
    OVERALL_TOTAL_DISTANCE_TRAVELED(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "TOTAL_DISTANCE_TRAVELED"),
    OVERALL_TOTAL_AVERAGE_SPEED_OVER_GROUND(2, DESCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "TOTAL_AVERAGE_SPEED_OVER_GROUND"),
    OVERALL_TOTAL_TIME_SAILED_IN_SECONDS(1, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT, "TOTAL_TIME_SAILED_IN_SECONDS"),
    OVERALL_TOTAL_DURATION_FOILED_IN_SECONDS(0, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "TOTAL_DURATION_FOILED_IN_SECONDS"),
    OVERALL_TOTAL_DISTANCE_FOILED_IN_METERS(0, DESCENDING, null, ValueSpreader.CUBIC_ROOT, "TOTAL_DISTANCE_FOILED_IN_METERS"),
    RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS(2, DESCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES(1, ASCENDING, null, ValueSpreader.LINEAR),
    RACE_CURRENT_POSITION_LAT_DEG(10, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_CURRENT_POSITION_LNG_DEG(10, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_CURRENT_DISTANCE_FOILED_IN_METERS(0, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_CURRENT_DURATION_FOILED_IN_SECONDS(0, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_NET_POINTS(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    REGATTA_NET_POINTS(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    REGATTA_NET_POINTS_SUM(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_RATIO_BETWEEN_TIME_SINCE_LAST_POSITION_FIX_AND_AVERAGE_SAMPLING_INTERVAL(1, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START(1, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_CALCULATED_TIME_TRAVELED(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_IMPLIED_WIND(5, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD(0, ASCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    RACE_TIME_TRAVELED(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_TIME_TRAVELED_UPWIND(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_TIME_TRAVELED_DOWNWIND(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    RACE_TIME_TRAVELED_REACHING(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    CHART_DISTANCE_TO_START_LINE(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "DISTANCE_TO_START_LINE"),
    CHART_BEAT_ANGLE(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "BEAT_ANGLE"),
    CHART_ABS_TWA(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    CHART_COURSE_OVER_GROUND_TRUE_DEGREES(0, ASCENDING, null, ValueSpreader.LINEAR, "COURSE_OVER_GROUND_TRUE_DEGREES"),
    OVERALL_TOTAL_SCORED_RACE_COUNT(0, ASCENDING, null, ValueSpreader.CUBIC_ROOT, "TOTAL_SCORED_RACE_COUNT"),
    EXPEDITION_RACE_AWA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_AWS(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TWA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TWS(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TWD(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TARG_TWA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_BOAT_SPEED(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TARG_BOAT_SPEED(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_SOG(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_COG(2, ASCENDING, null, ValueSpreader.LINEAR),
    EXPEDITION_RACE_FORESTAY_LOAD(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_RAKE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_COURSE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_HEADING(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_VMG(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_VMG_TARG_VMG_DELTA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_RATE_OF_TURN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_RUDDER_ANGLE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TARGET_HEEL(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_PORT_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_STB_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_DIST_TO_PORT_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_DIST_TO_STB_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_GUN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_PIN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_BURN_TO_LINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_TIME_TO_BURN_TO_PIN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_DISTANCE_TO_PIN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_DISTANCE_BELOW_LINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_AWA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_AWS(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TWA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TWS(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TWD(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TARG_TWA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_BOAT_SPEED(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TARG_BOAT_SPEED(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_SOG(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_COG(2, ASCENDING, null, ValueSpreader.LINEAR),
    EXPEDITION_LEG_FORESTAY_LOAD(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_RAKE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_COURSE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_HEADING(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_VMG(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_VMG_TARG_VMG_DELTA(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_RATE_OF_TURN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_RUDDER_ANGLE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TARGET_HEEL(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_PORT_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_STB_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_DIST_TO_PORT_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_DIST_TO_STB_LAYLINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_GUN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_COMMITTEE_BOAT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_PIN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_BURN_TO_LINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_BURN_TO_COMMITTEE_BOAT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_TIME_TO_BURN_TO_PIN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_DISTANCE_TO_COMMITTEE_BOAT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_DISTANCE_TO_PIN(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_DISTANCE_BELOW_LINE(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_LINE_SQUARE_FOR_WIND_DIRECTION(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_BARO(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_LOAD_S(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_LOAD_P(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_JIB_CAR_PORT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_JIB_CAR_STBD(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_MAST_BUTT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_BARO(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_LOAD_S(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_LOAD_P(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_JIB_CAR_PORT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_JIB_CAR_STBD(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_LEG_MAST_BUTT(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT),
    BRAVO_RACE_HEEL_IN_DEGREES(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    BRAVO_RACE_PITCH_IN_DEGREES(2, DESCENDING, null, ValueSpreader.CUBIC_ROOT),
    PERCENT_TARGET_BOAT_SPEED(2, DESCENDING, PREMIUM_LEADERBOARD_INFORMATION, ValueSpreader.CUBIC_ROOT),
    EXPEDITION_RACE_KICKER_TENSION(2, ASCENDING, null, ValueSpreader.CUBIC_ROOT);

    private int precision;

    private SortingOrder defaultSortingOrder;
    
    private Action premiumAction;

    private final String[] oldNames;

    private final ValueSpreader valueSpreader;

    /**
     * @param precision
     *            the number of decimal digits in which they are usually provided and should be formatted
     * @param premiumAction
     *            If not {@code null} then the user must have permission to execute this action on the leaderboard to
     *            which it is applied.
     * @param valueSpreader
     *            the strategy to use when mapping values of this detail type to a color range
     */
    DetailType(int precision, SortingOrder defaultSortingOrder, Action premiumAction, ValueSpreader valueSpreader, String... oldNames) {
        this.oldNames = oldNames;
        this.precision = precision;
        this.premiumAction = premiumAction;
        this.defaultSortingOrder = defaultSortingOrder;
        this.valueSpreader = valueSpreader;
    }

    /**
     * @return the number of decimal digits in which they are usually provided and should be formatted
     */
    public int getPrecision() {
        return precision;
    }
    
    /**
     * The logic to use to map numeric values of this detail type to a color spectrum used for a certain value range.
     * Typical choices would be {@link ValueSpreader#LINEAR} for a linear use of the available color spectrum, or
     * {@link ValueSpreader#CUBIC_ROOT} to use more of the color spectrum around the center of the value range so that
     * small variations around the value range center are shown as greater color variations, whereas values far out and
     * close to the range boarders use less color variation.
     */
    public ValueSpreader getValueSpreader() {
        return valueSpreader;
    }

    public SortingOrder getDefaultSortingOrder() {
        return defaultSortingOrder;
    }
    
    /**
     * If not {@code null} then the user must have permission to execute the action returned on the
     * leaderboard to which it is applied.
     */
    public Action getPremiumAction() {
        return this.premiumAction;
    }

    /**
     * Determines whether or not this {@link DetailType} represents an expedition data type.
     * 
     * @return <code>true</code> if this {@link DetailType} is an expedition data type, <code>false</code> otherwise
     * 
     * @see #getLegExpeditionDetailColumnTypes()
     * @see #getRaceExpeditionDetailTypes()
     */
    public boolean isExpeditionType() {
        return getLegExpeditionDetailColumnTypes().contains(this) || getRaceExpeditionDetailTypes().contains(this);
    }

    /**
     * Determines whether or not this {@link DetailType} represents a degree detail which needs to be recalculated, e.g.
     * to ensure continuous linear rendering in chart avoiding leaps from 360 to 0 degrees and vice verse.
     * 
     * @return <code>true</code> if this {@link DetailType} is a degree detail type which needs to be recalculated,
     *         <code>false</code> otherwise
     * 
     * @see #CHART_COURSE_OVER_GROUND_TRUE_DEGREES
     */
    public boolean isDegreeTypeWithRecalculation() {
        return this == CHART_COURSE_OVER_GROUND_TRUE_DEGREES;
    }

    /**
     * Special collection of {@link DetailType}s, that allows operators to select for example the RideHeight, that is
     * usually only selectable, if it already has data.
     */
    public static Collection<DetailType> getAutoplayDetailTypesForChart() {
        final Collection<DetailType> availableDetailsTypes = new LinkedHashSet<>();
        availableDetailsTypes.add(DetailType.LEG_TACKTYPE_LONGTACK_SHORTTACK);
        availableDetailsTypes.add(DetailType.CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD);
        availableDetailsTypes.add(DetailType.CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_CHANGE);
        availableDetailsTypes.add(DetailType.LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS);
        availableDetailsTypes.add(DetailType.LEG_DISTANCE_TRAVELED);
        availableDetailsTypes.add(DetailType.LEG_VELOCITY_MADE_GOOD_IN_KNOTS);
        availableDetailsTypes.add(DetailType.LEG_GAP_TO_LEADER_IN_SECONDS);
        availableDetailsTypes.add(DetailType.LEG_GAP_TO_LEADER_IN_SECONDS_CHANGE);
        availableDetailsTypes.add(DetailType.RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS);
        availableDetailsTypes.add(DetailType.RACE_RANK);
        availableDetailsTypes.add(DetailType.REGATTA_RANK);
        availableDetailsTypes.add(DetailType.CHART_DISTANCE_TO_START_LINE);
        availableDetailsTypes.add(DetailType.CHART_BEAT_ANGLE);
        availableDetailsTypes.add(DetailType.CHART_ABS_TWA);
        availableDetailsTypes.add(DetailType.CHART_COURSE_OVER_GROUND_TRUE_DEGREES);
        availableDetailsTypes.add(DetailType.BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS);
        availableDetailsTypes.add(DetailType.BRAVO_RACE_HEEL_IN_DEGREES);
        availableDetailsTypes.add(DetailType.BRAVO_RACE_PITCH_IN_DEGREES);
        availableDetailsTypes.add(DetailType.PERCENT_TARGET_BOAT_SPEED);
        return availableDetailsTypes;
    }

    public static Collection<DetailType> getRaceExpeditionDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(EXPEDITION_RACE_AWA);
        allowed.add(EXPEDITION_RACE_AWS);
        allowed.add(EXPEDITION_RACE_TWA);
        allowed.add(EXPEDITION_RACE_TWS);
        allowed.add(EXPEDITION_RACE_TWD);
        allowed.add(EXPEDITION_RACE_TARG_TWA);
        allowed.add(EXPEDITION_RACE_BOAT_SPEED);
        allowed.add(EXPEDITION_RACE_TARG_BOAT_SPEED);
        allowed.add(EXPEDITION_RACE_SOG);
        allowed.add(EXPEDITION_RACE_COG);
        allowed.add(EXPEDITION_RACE_FORESTAY_LOAD);
        allowed.add(EXPEDITION_RACE_RAKE);
        allowed.add(EXPEDITION_RACE_COURSE);
        allowed.add(EXPEDITION_RACE_HEADING);
        allowed.add(EXPEDITION_RACE_VMG);
        allowed.add(EXPEDITION_RACE_VMG_TARG_VMG_DELTA);
        allowed.add(EXPEDITION_RACE_RATE_OF_TURN);
        allowed.add(EXPEDITION_RACE_RUDDER_ANGLE);
        allowed.add(EXPEDITION_RACE_TARGET_HEEL);
        allowed.add(EXPEDITION_RACE_TIME_TO_PORT_LAYLINE);
        allowed.add(EXPEDITION_RACE_TIME_TO_STB_LAYLINE);
        allowed.add(EXPEDITION_RACE_DIST_TO_PORT_LAYLINE);
        allowed.add(EXPEDITION_RACE_DIST_TO_STB_LAYLINE);
        allowed.add(EXPEDITION_RACE_TIME_TO_GUN);
        allowed.add(EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT);
        allowed.add(EXPEDITION_RACE_TIME_TO_PIN);
        allowed.add(EXPEDITION_RACE_TIME_TO_BURN_TO_LINE);
        allowed.add(EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT);
        allowed.add(EXPEDITION_RACE_TIME_TO_BURN_TO_PIN);
        allowed.add(EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT);
        allowed.add(EXPEDITION_RACE_DISTANCE_TO_PIN);
        allowed.add(EXPEDITION_RACE_DISTANCE_BELOW_LINE);
        allowed.add(EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION);
        allowed.add(EXPEDITION_RACE_BARO);
        allowed.add(EXPEDITION_RACE_LOAD_S);
        allowed.add(EXPEDITION_RACE_LOAD_P);
        allowed.add(EXPEDITION_RACE_JIB_CAR_PORT);
        allowed.add(EXPEDITION_RACE_JIB_CAR_STBD);
        allowed.add(EXPEDITION_RACE_MAST_BUTT);
        allowed.add(EXPEDITION_RACE_KICKER_TENSION);
        return allowed;
    }

     public static Collection<DetailType> getRaceExtendedBravoDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_PORT_DAGGERBOARD_RAKE);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_STBD_DAGGERBOARD_RAKE);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_PORT_RUDDER_RAKE);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_STBD_RUDDER_RAKE);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_MAST_ROTATION_IN_DEGREES);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_LEEWAY_IN_DEGREES);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_SET);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_DRIFT_IN_DEGREES);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_DEPTH_IN_METERS);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_RUDDER_IN_DEGREES);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_TACK_ANGLE_IN_DEGREES);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_PERCENTAGE);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_IN_MILLIMETERS);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_RAKE_IN_DEGREES);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_TARGET_HEEL_ANGLE_IN_DEGREES);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_FORESTAY_LOAD);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_FORESTAY_PRESSURE);
        allowed.add(DetailType.BRAVOEXTENDED_RACE_CURRENT_TARGET_BOATSPEED_PERCENTAGE);
        return allowed;
    }

    public static Collection<DetailType> getRaceBravoDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(DetailType.BRAVO_RACE_HEEL_IN_DEGREES);
        allowed.add(DetailType.BRAVO_RACE_PITCH_IN_DEGREES);
        allowed.add(DetailType.BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS);
        allowed.add(DetailType.RACE_CURRENT_DISTANCE_FOILED_IN_METERS);
        allowed.add(DetailType.RACE_CURRENT_DURATION_FOILED_IN_SECONDS);
        return allowed;
    }

    public static Collection<DetailType> getLegBravoDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(DetailType.BRAVO_LEG_CURRENT_HEEL_IN_DEGREES);
        allowed.add(DetailType.BRAVO_LEG_CURRENT_PITCH_IN_DEGREES);
        allowed.add(DetailType.BRAVO_LEG_CURRENT_RIDE_HEIGHT_IN_METERS);
        allowed.add(DetailType.BRAVOEXTENDED_LEG_CURRENT_DISTANCE_FOILED_IN_METERS);
        allowed.add(DetailType.BRAVOEXTENDED_LEG_CURRENT_DURATION_FOILED_IN_SECONDS);
        return allowed;
    }

    public static Collection<DetailType> getAllRaceDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(RACE_GAP_TO_LEADER_IN_SECONDS);
        allowed.add(PERCENT_TARGET_BOAT_SPEED);
        allowed.add(RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS);
        allowed.add(RACE_DISTANCE_TRAVELED);
        allowed.add(RACE_DISTANCE_TRAVELED_INCLUDING_GATE_START);
        allowed.add(RACE_TIME_TRAVELED);
        allowed.add(RACE_CALCULATED_TIME_TRAVELED);
        allowed.add(RACE_IMPLIED_WIND);
        allowed.add(RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD);
        allowed.add(RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS);
        allowed.add(RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES);
        allowed.add(RACE_CURRENT_DISTANCE_FOILED_IN_METERS);
        allowed.add(RACE_CURRENT_DURATION_FOILED_IN_SECONDS);
        allowed.add(RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS);
        allowed.add(NUMBER_OF_MANEUVERS);
        allowed.add(RACE_DISPLAY_LEGS);
        allowed.add(RACE_DISPLAY_BOATS);
        allowed.add(RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS);
        allowed.add(RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS);
        allowed.add(RACE_RATIO_BETWEEN_TIME_SINCE_LAST_POSITION_FIX_AND_AVERAGE_SAMPLING_INTERVAL);
        allowed.add(RACE_CURRENT_LEG);
        allowed.addAll(getRaceBravoDetailTypes());
        allowed.addAll(getRaceExtendedBravoDetailTypes());
        allowed.addAll(getRaceExpeditionDetailTypes());
        return allowed;
    }

    /**
     * Returns all types in the enum, minus those for expedition, bravo, bravo extended
     * and the ToT/ToD/ORC PCS ones
     */
    public static Collection<DetailType> getAllNonRestrictedDetailTypes() {
        final Collection<DetailType> all = new LinkedHashSet<>(Arrays.asList(values()));
        all.removeAll(getRaceBravoDetailTypes());
        all.removeAll(getLegBravoDetailTypes());
        all.removeAll(getRaceExtendedBravoDetailTypes());
        all.removeAll(getRaceExpeditionDetailTypes());
        all.removeAll(getLegExpeditionDetailColumnTypes());
        all.removeAll(getOverallBravoDetailTypes());
        all.removeAll(getAllToTToDHandicapDetailTypes());
        all.removeAll(getAllOrcPerformanceCurveDetailTypes());
        return all;
    }

    public static Collection<DetailType> getAvailableOverallDetailColumnTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(REGATTA_RANK);
        allowed.add(OVERALL_TOTAL_DISTANCE_TRAVELED);
        allowed.add(OVERALL_TOTAL_AVERAGE_SPEED_OVER_GROUND);
        allowed.add(OVERALL_TOTAL_TIME_SAILED_IN_SECONDS);
        allowed.add(OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS);
        // TODO bug5209 make ToT / ToD details depend on ranking metric
        allowed.add(OVERALL_TIME_ON_TIME_FACTOR);
        allowed.add(OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE);
        allowed.add(OVERALL_TOTAL_SCORED_RACE_COUNT);
        return allowed;
    }

    public static Collection<DetailType> getOverallBravoDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(OVERALL_TOTAL_DISTANCE_FOILED_IN_METERS);
        allowed.add(OVERALL_TOTAL_DURATION_FOILED_IN_SECONDS);
        return allowed;
    }

    public static Collection<? extends DetailType> getAllToTToDHandicapDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD);
        allowed.add(RACE_CALCULATED_TIME_TRAVELED);
        return allowed;
    }

    public static Collection<? extends DetailType> getAllOrcPerformanceCurveDetailTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD);
        allowed.add(RACE_CALCULATED_TIME_TRAVELED);
        allowed.add(RACE_IMPLIED_WIND);
        return allowed;
    }

    public static Collection<DetailType> getLegDetailColumnTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS);
        allowed.add(LEG_DISTANCE_TRAVELED);
        allowed.add(LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START);
        allowed.add(LEG_GAP_TO_LEADER_IN_SECONDS);
        allowed.add(LEG_GAP_CHANGE_SINCE_LEG_START_IN_SECONDS);
        allowed.add(LEG_SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED);
        allowed.add(LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS);
        allowed.add(LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS);
        allowed.add(NUMBER_OF_MANEUVERS);
        allowed.add(LEG_ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS);
        allowed.add(LEG_VELOCITY_MADE_GOOD_IN_KNOTS);
        allowed.add(LEG_TIME_TRAVELED);
        allowed.add(LEG_CORRECTED_TIME_TRAVELED);
        allowed.add(LEG_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS);
        allowed.add(LEG_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS);
        allowed.add(LEG_RANK_GAIN);
        return allowed;
    }

    public static Collection<DetailType> getRaceStartAnalysisColumnTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START);
        allowed.add(RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START);
        allowed.add(DISTANCE_TO_START_AT_RACE_START);
        allowed.add(TIME_BETWEEN_RACE_START_AND_COMPETITOR_START);
        allowed.add(SPEED_OVER_GROUND_AT_RACE_START);
        allowed.add(SPEED_OVER_GROUND_WHEN_PASSING_START);
        allowed.add(DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS);
        allowed.add(START_TACK);
        return allowed;
    }

    public static Collection<DetailType> getLegExpeditionDetailColumnTypes() {
        final Collection<DetailType> allowed = new LinkedHashSet<>();
        allowed.add(EXPEDITION_LEG_AWA);
        allowed.add(EXPEDITION_LEG_AWS);
        allowed.add(EXPEDITION_LEG_TWA);
        allowed.add(EXPEDITION_LEG_TWS);
        allowed.add(EXPEDITION_LEG_TWD);
        allowed.add(EXPEDITION_LEG_TARG_TWA);
        allowed.add(EXPEDITION_LEG_BOAT_SPEED);
        allowed.add(EXPEDITION_LEG_TARG_BOAT_SPEED);
        allowed.add(EXPEDITION_LEG_SOG);
        allowed.add(EXPEDITION_LEG_COG);
        allowed.add(EXPEDITION_LEG_FORESTAY_LOAD);
        allowed.add(EXPEDITION_LEG_RAKE);
        allowed.add(EXPEDITION_LEG_COURSE);
        allowed.add(EXPEDITION_LEG_HEADING);
        allowed.add(EXPEDITION_LEG_VMG);
        allowed.add(EXPEDITION_LEG_VMG_TARG_VMG_DELTA);
        allowed.add(EXPEDITION_LEG_RATE_OF_TURN);
        allowed.add(EXPEDITION_LEG_RUDDER_ANGLE);
        allowed.add(EXPEDITION_LEG_TARGET_HEEL);
        allowed.add(EXPEDITION_LEG_TIME_TO_PORT_LAYLINE);
        allowed.add(EXPEDITION_LEG_TIME_TO_STB_LAYLINE);
        allowed.add(EXPEDITION_LEG_DIST_TO_PORT_LAYLINE);
        allowed.add(EXPEDITION_LEG_DIST_TO_STB_LAYLINE);
        allowed.add(EXPEDITION_LEG_TIME_TO_GUN);
        allowed.add(EXPEDITION_LEG_TIME_TO_COMMITTEE_BOAT);
        allowed.add(EXPEDITION_LEG_TIME_TO_PIN);
        allowed.add(EXPEDITION_LEG_TIME_TO_BURN_TO_LINE);
        allowed.add(EXPEDITION_LEG_TIME_TO_BURN_TO_COMMITTEE_BOAT);
        allowed.add(EXPEDITION_LEG_TIME_TO_BURN_TO_PIN);
        allowed.add(EXPEDITION_LEG_DISTANCE_TO_COMMITTEE_BOAT);
        allowed.add(EXPEDITION_LEG_DISTANCE_TO_PIN);
        allowed.add(EXPEDITION_LEG_DISTANCE_BELOW_LINE);
        allowed.add(EXPEDITION_LEG_LINE_SQUARE_FOR_WIND_DIRECTION);
        allowed.add(EXPEDITION_LEG_BARO);
        allowed.add(EXPEDITION_LEG_LOAD_S);
        allowed.add(EXPEDITION_LEG_LOAD_P);
        allowed.add(EXPEDITION_LEG_JIB_CAR_PORT);
        allowed.add(EXPEDITION_LEG_JIB_CAR_STBD);
        allowed.add(EXPEDITION_LEG_MAST_BUTT);
        return allowed;
    }

    /**
     * Not yet implemented types can be added here, they will be hidden from any user under all circumstances
     */
    public static Collection<DetailType> getDisabledDetailColumTypes() {
        final Collection<DetailType> disabled = new LinkedHashSet<>();
        disabled.add(EXPEDITION_LEG_TARG_TWA);
        disabled.add(EXPEDITION_LEG_VMG);
        disabled.add(EXPEDITION_LEG_VMG_TARG_VMG_DELTA);
        disabled.add(EXPEDITION_LEG_RATE_OF_TURN);
        disabled.add(EXPEDITION_LEG_RUDDER_ANGLE);
        disabled.add(EXPEDITION_LEG_TIME_TO_PORT_LAYLINE);
        disabled.add(EXPEDITION_LEG_TIME_TO_STB_LAYLINE);
        disabled.add(EXPEDITION_LEG_DIST_TO_PORT_LAYLINE);
        disabled.add(EXPEDITION_LEG_DIST_TO_STB_LAYLINE);
        disabled.add(EXPEDITION_LEG_TIME_TO_COMMITTEE_BOAT);
        disabled.add(EXPEDITION_LEG_TIME_TO_PIN);
        disabled.add(EXPEDITION_LEG_TIME_TO_BURN_TO_COMMITTEE_BOAT);
        disabled.add(EXPEDITION_LEG_TIME_TO_BURN_TO_PIN);
        disabled.add(EXPEDITION_LEG_DISTANCE_TO_COMMITTEE_BOAT);
        disabled.add(EXPEDITION_LEG_DISTANCE_TO_PIN);
        disabled.add(EXPEDITION_LEG_LINE_SQUARE_FOR_WIND_DIRECTION);
        disabled.add(EXPEDITION_RACE_TARG_TWA);
        disabled.add(EXPEDITION_RACE_VMG);
        disabled.add(EXPEDITION_RACE_VMG_TARG_VMG_DELTA);
        disabled.add(EXPEDITION_RACE_RUDDER_ANGLE);
        disabled.add(EXPEDITION_RACE_TIME_TO_PORT_LAYLINE);
        disabled.add(EXPEDITION_RACE_TIME_TO_STB_LAYLINE);
        disabled.add(EXPEDITION_RACE_DIST_TO_PORT_LAYLINE);
        disabled.add(EXPEDITION_RACE_DIST_TO_STB_LAYLINE);
        disabled.add(EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT);
        disabled.add(EXPEDITION_RACE_TIME_TO_PIN);
        disabled.add(EXPEDITION_RACE_TIME_TO_BURN_TO_PIN);
        disabled.add(EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT);
        disabled.add(EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT);
        disabled.add(EXPEDITION_RACE_DISTANCE_TO_PIN);
        disabled.add(EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION);
        return disabled;
    }

    public static Collection<DetailType> getAllLegDetailColumnTypes() {
        final Collection<DetailType> all = new LinkedHashSet<>();
        all.addAll(getLegDetailColumnTypes());
        all.addAll(getLegBravoDetailTypes());
        all.addAll(getLegExpeditionDetailColumnTypes());
        return all;
    }

    /**
     * This method allows callers to also restore legacy names of older settings that do not match the renamed
     * detailtypes anymore. It matches {@code value} primarily to the literals; if no match is found, all
     * {@link #oldNames old names} are checked, and the first match, if any, is returned. If no match is found, an
     * {@link IllegalArgumentException} is thrown.
     */
    public static DetailType valueOfString(String value) {
        // fastpath for directly mappable values
        try {
            return DetailType.valueOf(value);
        } catch (IllegalArgumentException e) {
            // fallback for renamed legacy settings
            for (DetailType t : values()) {
                if (t.oldNames != null) {
                    for (String oldname : t.oldNames) {
                        if (oldname.equals(value)) {
                            return t;
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("Could not restore " + value + " to an DetailType enum");
    }

    /**
     * Get a list of all defined {@link Action}s from all possible {@link DetailType}s).
     * 
     * @return a {@link Set} of all Actions which are defined on the {@link DetailType}s.
     */
    public static Set<Action> getAllPremiumActionsFromAll() {
        final Set<Action> premiumLeaderboardActions = new HashSet<>();
        for (DetailType detailType : DetailType.values()) {
            premiumLeaderboardActions.add(detailType.getPremiumAction());
        }
        return premiumLeaderboardActions;
    }

    /**
     * Get a list of all defined {@link Action}s from a list (mostly a subset of all possible {@link DetailType}s).
     * 
     * @param detailTypes
     *            a list of DetailType
     * @return a {@link Set} of all Actions which are defined on the given {@link DetailType}s.
     */
    public static Set<Action> getAllPremiumActionsFromSubset(Iterable<DetailType> detailTypes) {
        final Set<Action> premiumLeaderboardActions = new HashSet<>();
        for (DetailType detailType : detailTypes) {
            if (detailType.getPremiumAction() != null) {
                premiumLeaderboardActions.add(detailType.getPremiumAction());
            }
        }
        return premiumLeaderboardActions;
    }
}
