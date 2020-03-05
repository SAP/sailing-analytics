package com.sap.sailing.gwt.ui.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.sap.sailing.domain.common.DetailType;

public class DetailTypeFormatter {
    
    private static final String AVERAGE_SYMBOL = "\u2205 ";

    private DetailTypeFormatter() { }
	
    private static final StringMessages stringMessages = GWT.create(StringMessages.class);
	
    public static String format(DetailType detailType) {
        switch (detailType) {
        case LEG_DISTANCE_TRAVELED:
            return stringMessages.distanceInMeters();
        case LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START:
            return stringMessages.distanceIncludingGateStartInMeters();
        case LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.averageSpeedInKnots();
        case LEG_RANK_GAIN:
            return stringMessages.rankGain();
        case RACE_RANK:
            return stringMessages.rank();
        case REGATTA_RANK:
            return stringMessages.regattaRank();
        case OVERALL_RANK:
            return stringMessages.overallRank();
        case RACE_NET_POINTS:
            return stringMessages.netPoints();
        case REGATTA_NET_POINTS:
            return stringMessages.netPoints();            
        case REGATTA_NET_POINTS_SUM:
            return stringMessages.netPoints();            
        case NUMBER_OF_MANEUVERS:
            return stringMessages.numberOfManeuvers();
        case LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.currentOrAverageSpeedOverGroundInKnots();
        case RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.currentSpeedOverGroundInKnots();
        case RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES:
            return stringMessages.courseOverGroundTrueDegrees();
        case RACE_CURRENT_POSITION_LAT_DEG:
            return stringMessages.latitude();
        case RACE_CURRENT_POSITION_LNG_DEG:
            return stringMessages.longitude();
        case BRAVO_LEG_CURRENT_RIDE_HEIGHT_IN_METERS:
            return stringMessages.currentOrAverageRideHeightInMeters();
        case BRAVOEXTENDED_LEG_CURRENT_DISTANCE_FOILED_IN_METERS:
            return stringMessages.currentDistanceFoiledInMeters();
        case BRAVOEXTENDED_LEG_CURRENT_DURATION_FOILED_IN_SECONDS:
            return stringMessages.currentDurationFoiled();
        case BRAVO_LEG_CURRENT_HEEL_IN_DEGREES:
            return stringMessages.currentHeelInDegree();
        case BRAVO_LEG_CURRENT_PITCH_IN_DEGREES:
            return stringMessages.currentPitchInDegree();
        case BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS:
            return stringMessages.currentRideHeightInMeters();
        case RACE_CURRENT_DURATION_FOILED_IN_SECONDS:
            return stringMessages.currentDurationFoiled();
        case RACE_CURRENT_DISTANCE_FOILED_IN_METERS:
            return stringMessages.currentDistanceFoiledInMeters();
        case BRAVOEXTENDED_RACE_CURRENT_PORT_DAGGERBOARD_RAKE:
            return stringMessages.currentPortDaggerboardRake();
        case BRAVOEXTENDED_RACE_CURRENT_STBD_DAGGERBOARD_RAKE:
            return stringMessages.currentStbdDaggerboardRake();
        case BRAVOEXTENDED_RACE_CURRENT_PORT_RUDDER_RAKE:
            return stringMessages.currentPortRudderRake();
        case BRAVOEXTENDED_RACE_CURRENT_STBD_RUDDER_RAKE:
            return stringMessages.currentStbdRudderRake();
        case BRAVOEXTENDED_RACE_CURRENT_MAST_ROTATION_IN_DEGREES:
            return stringMessages.currentMastRotationInDegree();
        case BRAVOEXTENDED_RACE_CURRENT_DEPTH_IN_METERS:
            return stringMessages.currentDepthInMeters();
        case BRAVOEXTENDED_RACE_CURRENT_DRIFT_IN_DEGREES:
            return stringMessages.currentDriftInDegrees();
        case BRAVOEXTENDED_RACE_CURRENT_LEEWAY_IN_DEGREES:
            return stringMessages.currentLeewayInDegrees();
        case BRAVOEXTENDED_RACE_CURRENT_RUDDER_IN_DEGREES:
            return stringMessages.currentRudderInDegrees();
        case BRAVOEXTENDED_RACE_CURRENT_SET:
            return stringMessages.currentSet();
        case BRAVOEXTENDED_RACE_CURRENT_TACK_ANGLE_IN_DEGREES:
            return stringMessages.currentTackAngleInDegrees();
        case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_IN_MILLIMETERS:
            return stringMessages.currentDeflectorInMillimeters();
        case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_PERCENTAGE:
            return stringMessages.currentDeflectorPercentage();
        case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_LOAD:
            return stringMessages.currentForestayLoad();
        case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_PRESSURE:
            return stringMessages.currentForestayPressure();
        case BRAVOEXTENDED_RACE_CURRENT_RAKE_IN_DEGREES:
            return stringMessages.currentRakeInDegrees();
        case BRAVOEXTENDED_RACE_CURRENT_TARGET_BOATSPEED_PERCENTAGE:
            return stringMessages.currentTargetBoatspeedPercentage();
        case BRAVOEXTENDED_RACE_CURRENT_TARGET_HEEL_ANGLE_IN_DEGREES:
            return stringMessages.currentTargetHeelAngleInDegrees();
        case LEG_ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS:
            return stringMessages.estimatedTimeToNextWaypointInSeconds();
        case LEG_VELOCITY_MADE_GOOD_IN_KNOTS:
            return stringMessages.velocityMadeGoodInKnots();
        case LEG_GAP_TO_LEADER_IN_SECONDS:
            return stringMessages.gapToLeaderInSeconds();
        case LEG_CORRECTED_TIME_TRAVELED:
            return stringMessages.calculatedTimeTraveled();
        case LEG_GAP_CHANGE_SINCE_LEG_START_IN_SECONDS:
            return stringMessages.gapChangeSinceLegStartInSeconds();
        case LEG_SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED:
            return stringMessages.sideToWhichMarkAtLegStartWasRounded();
        case LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS:
            return stringMessages.windwardDistanceToGoInMeters();
        case RACE_DISTANCE_TRAVELED:
            return stringMessages.distanceInMeters();
        case RACE_DISTANCE_TRAVELED_INCLUDING_GATE_START:
            return stringMessages.distanceIncludingGateStartInMeters();
        case RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.averageSpeedInKnots();
        case RACE_GAP_TO_LEADER_IN_SECONDS:
            return stringMessages.gapToLeaderInSeconds();
        case RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS:
            return stringMessages.windwardDistanceToCompetitorFarthestAheadInMeters();
        case RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.averageAbsoluteCrossTrackErrorInMeters();
        case RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.averageSignedCrossTrackErrorInMeters();
        case START_TACK:
            return stringMessages.startTack();
        case CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD:
            return stringMessages.windwardDistanceToCompetitorFarthestAheadInMeters();
        case DISTANCE_TO_START_AT_RACE_START:
            return stringMessages.distanceToLineAtRaceStart();
        case TIME_BETWEEN_RACE_START_AND_COMPETITOR_START:
            return stringMessages.timeBetweenRaceStartAndCompetitorStartInSeconds();
        case SPEED_OVER_GROUND_AT_RACE_START:
            return stringMessages.speedOverGroundAtRaceStart();
        case SPEED_OVER_GROUND_WHEN_PASSING_START:
            return stringMessages.speedOverGroundWhenPassingStart();
        case DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS:
            return stringMessages.distanceToStarboardEndOfStartlineWhenPassingStart();
        case TACK:
            return stringMessages.tack();
        case JIBE:
            return stringMessages.jibe();
        case PENALTY_CIRCLE:
            return stringMessages.penaltyCircle();
        case RACE_DISPLAY_LEGS:
            return stringMessages.legs();
        case RACE_DISPLAY_BOATS:
            return stringMessages.boats();
        case RACE_CURRENT_LEG:
            return stringMessages.currentLeg();
        case LEG_TIME_TRAVELED:
            return stringMessages.time();
        case LEG_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.averageAbsoluteCrossTrackErrorInMeters();
        case LEG_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.averageSignedCrossTrackErrorInMeters();
        case OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.maximumSpeedOverGroundInKnots();
        case OVERALL_TOTAL_DISTANCE_TRAVELED:
            return stringMessages.totalDistanceTraveled();
        case OVERALL_TIME_ON_TIME_FACTOR:
            return stringMessages.timeOnTimeFactor();
        case OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE:
            return stringMessages.timeOnDistanceAllowanceInSecondsPerNauticalMile();
        case TOTAL_TIME_SAILED_DOWNWIND_IN_SECONDS:
            return stringMessages.totalTimeSailedDownwindInSeconds();
        case TOTAL_TIME_SAILED_UPWIND_IN_SECONDS:
            return stringMessages.totalTimeSailedUpwindInSeconds();
        case TOTAL_TIME_SAILED_REACHING_IN_SECONDS:
            return stringMessages.totalTimeSailedReachingInSeconds();
        case OVERALL_TOTAL_TIME_SAILED_IN_SECONDS:
            return stringMessages.totalTimeSailedInSeconds();
        case OVERALL_TOTAL_AVERAGE_SPEED_OVER_GROUND:
            return stringMessages.totalAverageSpeedOverGround();
        case OVERALL_TOTAL_DURATION_FOILED_IN_SECONDS:
            return stringMessages.totalDurationFoiledInSeconds();
        case OVERALL_TOTAL_DISTANCE_FOILED_IN_METERS:
            return stringMessages.totalDistanceFoiledInMeters();
        case AVERAGE_MANEUVER_LOSS_IN_METERS:
            return stringMessages.averageManeuverLossInMeters();
        case AVERAGE_TACK_LOSS_IN_METERS:
            return stringMessages.averageTackLossInMeters();
        case AVERAGE_JIBE_LOSS_IN_METERS:
            return stringMessages.averageJibeLossInMeters();
        case RACE_RATIO_BETWEEN_TIME_SINCE_LAST_POSITION_FIX_AND_AVERAGE_SAMPLING_INTERVAL:
            return stringMessages.ratioBetweenTimeSinceLastPositionFixAndAverageSamplingInterval();
        case RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START:
            return stringMessages.distanceToLineFiveSecondsBeforeStart();
        case RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START:
            return stringMessages.speedOverGroundFiveSecondsBeforeStart();
        case RACE_TIME_TRAVELED:
            return stringMessages.time();
        case RACE_CALCULATED_TIME_TRAVELED:
            return stringMessages.calculatedTimeTraveled();
        case RACE_IMPLIED_WIND:
            return stringMessages.impliedWind();
        case RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD:
            return stringMessages.raceCalculatedTimeAtEstimatedArrivalAtCompetitorFarthestAheadInSeconds();
        case RACE_TIME_TRAVELED_DOWNWIND:
            return stringMessages.timeDownwind();
        case RACE_TIME_TRAVELED_REACHING:
            return stringMessages.timeReaching();
        case RACE_TIME_TRAVELED_UPWIND:
            return stringMessages.timeUpwind();
        case CHART_DISTANCE_TO_START_LINE:
            return stringMessages.distanceToLine();
        case CHART_BEAT_ANGLE:
            return stringMessages.TWA();
        case CHART_ABS_TWA:
            return stringMessages.absoluteTWA();
        case CHART_COURSE_OVER_GROUND_TRUE_DEGREES:
            return stringMessages.courseOverGroundTrueDegrees();
        case OVERALL_TOTAL_SCORED_RACE_COUNT:
            return stringMessages.racesScored();
        case EXPEDITION_RACE_AWA:
            return stringMessages.expeditionAwa();
        case EXPEDITION_RACE_AWS:
            return stringMessages.expeditionAws();
        case EXPEDITION_RACE_TWA:
            return stringMessages.expeditionTwa();
        case EXPEDITION_RACE_TWS:
            return stringMessages.expeditionTws();
        case EXPEDITION_RACE_TWD:
            return stringMessages.expeditionTwd();
        case EXPEDITION_RACE_TARG_TWA:
            return stringMessages.expeditionTargTwa();
        case EXPEDITION_RACE_BOAT_SPEED:
            return stringMessages.expeditionBoatSpeed();
        case EXPEDITION_RACE_TARG_BOAT_SPEED:
            return stringMessages.expeditionTargBoatSpeed();
        case EXPEDITION_RACE_SOG:
            return stringMessages.expeditionSOG();
        case EXPEDITION_RACE_COG:
            return stringMessages.expeditionCOG();
        case EXPEDITION_RACE_FORESTAY_LOAD:
            return stringMessages.expeditionForestayLoad();
        case EXPEDITION_RACE_RAKE:
            return stringMessages.expeditionRake();
        case EXPEDITION_RACE_COURSE:
            return stringMessages.expeditionCourse();
        case EXPEDITION_RACE_HEADING:
            return stringMessages.expeditionHeading();
        case EXPEDITION_RACE_VMG:
            return stringMessages.expeditionVmg();
        case EXPEDITION_RACE_VMG_TARG_VMG_DELTA:
            return stringMessages.expeditionVmgTargVmgDelta();
        case EXPEDITION_RACE_RATE_OF_TURN:
            return stringMessages.expeditionRateOfTurn();
        case EXPEDITION_RACE_RUDDER_ANGLE:
            return stringMessages.expeditionRudderAngle();
        case EXPEDITION_RACE_TARGET_HEEL:
            return stringMessages.expeditionTargetHeel();
        case EXPEDITION_RACE_TIME_TO_PORT_LAYLINE:
            return stringMessages.expeditionTimeToPortLayline();
        case EXPEDITION_RACE_TIME_TO_STB_LAYLINE:
            return stringMessages.expeditionTimeToStbLayline();
        case EXPEDITION_RACE_DIST_TO_PORT_LAYLINE:
            return stringMessages.expeditionDistToPortLayline();
        case EXPEDITION_RACE_DIST_TO_STB_LAYLINE:
            return stringMessages.expeditionDistToStbLayline();
        case EXPEDITION_RACE_TIME_TO_GUN:
            return stringMessages.expeditionTimeToGun();
        case EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT:
            return stringMessages.expeditionTimeToCommitteeBoat();
        case EXPEDITION_RACE_TIME_TO_PIN:
            return stringMessages.expeditionTimeToPin();
        case EXPEDITION_RACE_TIME_TO_BURN_TO_LINE:
            return stringMessages.expeditionTimeToBurnToLine();
        case EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT:
            return stringMessages.expeditionTimeToBurnToCommitteeBoat();
        case EXPEDITION_RACE_TIME_TO_BURN_TO_PIN:
            return stringMessages.expeditionTimeToBurnToPin();
        case EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT:
            return stringMessages.expeditionDistanceToCommitteeBoat();
        case EXPEDITION_RACE_DISTANCE_TO_PIN:
            return stringMessages.expeditionDistanceToPin();
        case EXPEDITION_RACE_DISTANCE_BELOW_LINE:
            return stringMessages.expeditionDistanceBelowLine();
        case EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION:
            return stringMessages.expeditionLineSquareForWindDirection();
        case EXPEDITION_RACE_BARO:
            return stringMessages.expeditionBaro();
        case EXPEDITION_RACE_LOAD_S:
            return stringMessages.expeditionRaceLoadS();
        case EXPEDITION_RACE_LOAD_P:
            return stringMessages.expeditionRaceLoadP();
        case EXPEDITION_RACE_JIB_CAR_PORT:
            return stringMessages.expeditionRaceJibCarPort();
        case EXPEDITION_RACE_JIB_CAR_STBD:
            return stringMessages.expeditionRaceJibCarStbd();
        case EXPEDITION_RACE_MAST_BUTT:
            return stringMessages.expeditionRaceMastButt();
        case EXPEDITION_LEG_AWA:
            return AVERAGE_SYMBOL + stringMessages.expeditionAwa();
        case EXPEDITION_LEG_AWS:
            return AVERAGE_SYMBOL + stringMessages.expeditionAws();
        case EXPEDITION_LEG_BOAT_SPEED:
            return AVERAGE_SYMBOL + stringMessages.expeditionBoatSpeed();
        case EXPEDITION_LEG_COG:
            return AVERAGE_SYMBOL + stringMessages.expeditionCOG();
        case EXPEDITION_LEG_COURSE:
            return AVERAGE_SYMBOL + stringMessages.expeditionCourse();
        case EXPEDITION_LEG_DISTANCE_BELOW_LINE:
            return AVERAGE_SYMBOL + stringMessages.expeditionDistanceBelowLine();
        case EXPEDITION_LEG_DISTANCE_TO_COMMITTEE_BOAT:
            return AVERAGE_SYMBOL + stringMessages.expeditionDistanceToCommitteeBoat();
        case EXPEDITION_LEG_DISTANCE_TO_PIN:
            return AVERAGE_SYMBOL + stringMessages.expeditionDistanceToPin();
        case EXPEDITION_LEG_DIST_TO_PORT_LAYLINE:
            return AVERAGE_SYMBOL + stringMessages.expeditionDistToPortLayline();
        case EXPEDITION_LEG_DIST_TO_STB_LAYLINE:
            return AVERAGE_SYMBOL + stringMessages.expeditionDistToStbLayline();
        case EXPEDITION_LEG_FORESTAY_LOAD:
            return AVERAGE_SYMBOL + stringMessages.expeditionForestayLoad();
        case EXPEDITION_LEG_HEADING:
            return AVERAGE_SYMBOL + stringMessages.expeditionHeading();
        case EXPEDITION_LEG_LINE_SQUARE_FOR_WIND_DIRECTION:
            return AVERAGE_SYMBOL + stringMessages.expeditionLineSquareForWindDirection();
        case EXPEDITION_LEG_RAKE:
            return AVERAGE_SYMBOL + stringMessages.expeditionRake();
        case EXPEDITION_LEG_RATE_OF_TURN:
            return AVERAGE_SYMBOL + stringMessages.expeditionRateOfTurn();
        case EXPEDITION_LEG_RUDDER_ANGLE:
            return AVERAGE_SYMBOL + stringMessages.expeditionRudderAngle();
        case EXPEDITION_LEG_SOG:
            return AVERAGE_SYMBOL + stringMessages.expeditionSOG();
        case EXPEDITION_LEG_TARGET_HEEL:
            return AVERAGE_SYMBOL + stringMessages.expeditionTargetHeel();
        case EXPEDITION_LEG_TARG_BOAT_SPEED:
            return AVERAGE_SYMBOL + stringMessages.expeditionTargBoatSpeed();
        case EXPEDITION_LEG_TARG_TWA:
            return AVERAGE_SYMBOL + stringMessages.expeditionTargTwa();
        case EXPEDITION_LEG_TIME_TO_BURN_TO_COMMITTEE_BOAT:
            return AVERAGE_SYMBOL + stringMessages.expeditionTimeToBurnToCommitteeBoat();
        case EXPEDITION_LEG_TIME_TO_BURN_TO_LINE:
            return AVERAGE_SYMBOL + stringMessages.expeditionTimeToBurnToLine();
        case EXPEDITION_LEG_TIME_TO_BURN_TO_PIN:
            return AVERAGE_SYMBOL + stringMessages.expeditionTimeToBurnToPin();
        case EXPEDITION_LEG_TIME_TO_COMMITTEE_BOAT:
            return AVERAGE_SYMBOL + stringMessages.expeditionTimeToCommitteeBoat();
        case EXPEDITION_LEG_TIME_TO_GUN:
            return AVERAGE_SYMBOL + stringMessages.expeditionTimeToGun();
        case EXPEDITION_LEG_TIME_TO_PIN:
            return AVERAGE_SYMBOL + stringMessages.expeditionTimeToPin();
        case EXPEDITION_LEG_TIME_TO_PORT_LAYLINE:
            return AVERAGE_SYMBOL + stringMessages.expeditionDistToPortLayline();
        case EXPEDITION_LEG_TIME_TO_STB_LAYLINE:
            return AVERAGE_SYMBOL + stringMessages.expeditionDistToStbLayline();
        case EXPEDITION_LEG_TWA:
            return AVERAGE_SYMBOL + stringMessages.expeditionTwa();
        case EXPEDITION_LEG_TWD:
            return AVERAGE_SYMBOL + stringMessages.expeditionTwd();
        case EXPEDITION_LEG_TWS:
            return AVERAGE_SYMBOL + stringMessages.expeditionTws();
        case EXPEDITION_LEG_VMG:
            return AVERAGE_SYMBOL + stringMessages.expeditionVmg();
        case EXPEDITION_LEG_VMG_TARG_VMG_DELTA:
            return AVERAGE_SYMBOL + stringMessages.expeditionVmgTargVmgDelta();
        case EXPEDITION_LEG_BARO:
            return AVERAGE_SYMBOL + stringMessages.expeditionBaro();
        case EXPEDITION_LEG_LOAD_S:
            return AVERAGE_SYMBOL + stringMessages.expeditionRaceLoadS();
        case EXPEDITION_LEG_LOAD_P:
            return AVERAGE_SYMBOL + stringMessages.expeditionRaceLoadP();
        case EXPEDITION_LEG_JIB_CAR_PORT:
            return AVERAGE_SYMBOL + stringMessages.expeditionRaceJibCarPort();
        case EXPEDITION_LEG_JIB_CAR_STBD:
            return AVERAGE_SYMBOL + stringMessages.expeditionRaceJibCarStbd();
        case EXPEDITION_LEG_MAST_BUTT:
            return AVERAGE_SYMBOL + stringMessages.expeditionRaceMastButt();
        case BRAVO_RACE_HEEL_IN_DEGREES:
            return stringMessages.currentHeelInDegree();
        case BRAVO_RACE_PITCH_IN_DEGREES:
            return stringMessages.currentPitchInDegree();
        default:
            break;
        }
        return detailType.name();
    }
    
    /**
     * Returns the unit of the given {@link DetailType}, like 'm', 'kts' or an empty string, if the detail type has no
     * unit.<br>
     * Throws an UnsupportedOperationException if the given detail type isn't supported.
     * 
     * @param detailType
     * @return The unit of the detail type as string.
     */
    public static String getUnit(DetailType detailType) {
        switch (detailType) {
        case LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
        case RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
        case OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS:
        case RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
        case LEG_VELOCITY_MADE_GOOD_IN_KNOTS:
        case LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
        case OVERALL_TOTAL_AVERAGE_SPEED_OVER_GROUND:
        case SPEED_OVER_GROUND_AT_RACE_START:
        case SPEED_OVER_GROUND_WHEN_PASSING_START:
        case RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START:
        case RACE_IMPLIED_WIND:
            return stringMessages.knotsUnit();
        case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_PERCENTAGE:
        case BRAVOEXTENDED_RACE_CURRENT_TARGET_BOATSPEED_PERCENTAGE:
            return stringMessages.percent();
        case CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD:
        case LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS:
        case LEG_DISTANCE_TRAVELED:
        case LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START:
        case RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS:
        case RACE_DISTANCE_TRAVELED:
        case RACE_DISTANCE_TRAVELED_INCLUDING_GATE_START:
        case AVERAGE_TACK_LOSS_IN_METERS:
        case AVERAGE_JIBE_LOSS_IN_METERS:
        case AVERAGE_MANEUVER_LOSS_IN_METERS:
        case LEG_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS:
        case LEG_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS:
        case RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS:
        case RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS:
        case DISTANCE_TO_START_AT_RACE_START:
        case OVERALL_TOTAL_DISTANCE_TRAVELED:
        case DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS:
        case RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START:
        case CHART_DISTANCE_TO_START_LINE:
        case BRAVO_LEG_CURRENT_RIDE_HEIGHT_IN_METERS:
        case BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS:
        case RACE_CURRENT_DISTANCE_FOILED_IN_METERS:
        case BRAVOEXTENDED_RACE_CURRENT_DEPTH_IN_METERS:
        case BRAVOEXTENDED_LEG_CURRENT_DISTANCE_FOILED_IN_METERS:
        case OVERALL_TOTAL_DISTANCE_FOILED_IN_METERS:
        case EXPEDITION_RACE_DISTANCE_BELOW_LINE:
        case EXPEDITION_LEG_DISTANCE_BELOW_LINE:
            return stringMessages.metersUnit();
        case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_IN_MILLIMETERS:
            return stringMessages.millimetersUnit();
        case CHART_COURSE_OVER_GROUND_TRUE_DEGREES:
        case RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES:
        case RACE_CURRENT_POSITION_LAT_DEG:
        case RACE_CURRENT_POSITION_LNG_DEG:
        case BRAVO_LEG_CURRENT_HEEL_IN_DEGREES:
        case BRAVO_LEG_CURRENT_PITCH_IN_DEGREES:
        case BRAVOEXTENDED_RACE_CURRENT_MAST_ROTATION_IN_DEGREES:
        case BRAVOEXTENDED_RACE_CURRENT_DRIFT_IN_DEGREES:
        case BRAVOEXTENDED_RACE_CURRENT_LEEWAY_IN_DEGREES:
        case BRAVOEXTENDED_RACE_CURRENT_RAKE_IN_DEGREES:
        case BRAVOEXTENDED_RACE_CURRENT_RUDDER_IN_DEGREES:
        case BRAVOEXTENDED_RACE_CURRENT_TACK_ANGLE_IN_DEGREES:
        case BRAVOEXTENDED_RACE_CURRENT_TARGET_HEEL_ANGLE_IN_DEGREES:
        case BRAVO_RACE_HEEL_IN_DEGREES:
        case BRAVO_RACE_PITCH_IN_DEGREES:
            return stringMessages.degreesShort();
        case BRAVOEXTENDED_RACE_CURRENT_PORT_DAGGERBOARD_RAKE:
        case BRAVOEXTENDED_RACE_CURRENT_STBD_DAGGERBOARD_RAKE:
        case BRAVOEXTENDED_RACE_CURRENT_PORT_RUDDER_RAKE:
        case BRAVOEXTENDED_RACE_CURRENT_STBD_RUDDER_RAKE:
        case BRAVOEXTENDED_RACE_CURRENT_SET:
            // We currently do not show a specific unit for these measures because these are specific "uninterpreted" values
            return "";
        case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_LOAD:
        case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_PRESSURE:
            return stringMessages.tonsUnit();
        case LEG_GAP_TO_LEADER_IN_SECONDS:
        case LEG_GAP_CHANGE_SINCE_LEG_START_IN_SECONDS:
        case RACE_GAP_TO_LEADER_IN_SECONDS:
        case LEG_ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS:
        case LEG_CORRECTED_TIME_TRAVELED:
        case TIME_BETWEEN_RACE_START_AND_COMPETITOR_START:
        case EXPEDITION_RACE_TIME_TO_BURN_TO_LINE:
        case EXPEDITION_LEG_TIME_TO_BURN_TO_LINE:
        case EXPEDITION_RACE_TIME_TO_GUN:
        case EXPEDITION_LEG_TIME_TO_GUN:
            return stringMessages.secondsUnit();
        case OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE:
            return stringMessages.secondsPerNauticalMileUnit();
        case RACE_RATIO_BETWEEN_TIME_SINCE_LAST_POSITION_FIX_AND_AVERAGE_SAMPLING_INTERVAL:
            return stringMessages.ratio();
        case OVERALL_TOTAL_TIME_SAILED_IN_SECONDS:
        case TOTAL_TIME_SAILED_DOWNWIND_IN_SECONDS:
        case TOTAL_TIME_SAILED_UPWIND_IN_SECONDS:
        case TOTAL_TIME_SAILED_REACHING_IN_SECONDS:
        case RACE_TIME_TRAVELED:
        case RACE_TIME_TRAVELED_DOWNWIND:
        case RACE_TIME_TRAVELED_REACHING:
        case RACE_TIME_TRAVELED_UPWIND:
        case RACE_CALCULATED_TIME_TRAVELED:
        case RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD:
        case LEG_TIME_TRAVELED:
        case BRAVOEXTENDED_LEG_CURRENT_DURATION_FOILED_IN_SECONDS:
        case RACE_CURRENT_DURATION_FOILED_IN_SECONDS:
        case OVERALL_TOTAL_DURATION_FOILED_IN_SECONDS:
            return stringMessages.hhmmssUnit();
        case EXPEDITION_RACE_BARO:
            return stringMessages.milliBarUnits();
        case EXPEDITION_RACE_JIB_CAR_PORT:
            return stringMessages.degreesUnit();
        case EXPEDITION_RACE_JIB_CAR_STBD:
            return stringMessages.degreesUnit();
        case EXPEDITION_RACE_MAST_BUTT:
            return stringMessages.millimetersUnit();
        // Cases for detail types without unit, so that an empty string is returned.
        case RACE_RANK:
        case REGATTA_RANK:
        case OVERALL_RANK:
        case RACE_NET_POINTS:
        case REGATTA_NET_POINTS:
        case REGATTA_NET_POINTS_SUM:
        case RACE_CURRENT_LEG:
        case TACK:
        case START_TACK:
        case JIBE:
        case PENALTY_CIRCLE:
        case LEG_RANK_GAIN:
        case NUMBER_OF_MANEUVERS:
        case RACE_DISPLAY_LEGS:
        case RACE_DISPLAY_BOATS:
        case LEG_SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED:
        case CHART_BEAT_ANGLE:
        case CHART_ABS_TWA:
        case OVERALL_TIME_ON_TIME_FACTOR:
        case OVERALL_TOTAL_SCORED_RACE_COUNT:
        case EXPEDITION_RACE_AWA:
        case EXPEDITION_RACE_AWS:
        case EXPEDITION_RACE_TWA:
        case EXPEDITION_RACE_TWS:
        case EXPEDITION_RACE_TWD:
        case EXPEDITION_RACE_TARG_TWA:
        case EXPEDITION_RACE_BOAT_SPEED:
        case EXPEDITION_RACE_TARG_BOAT_SPEED:
        case EXPEDITION_RACE_SOG:
        case EXPEDITION_RACE_COG:
        case EXPEDITION_RACE_FORESTAY_LOAD:
        case EXPEDITION_RACE_RAKE:
        case EXPEDITION_RACE_COURSE:
        case EXPEDITION_RACE_HEADING:
        case EXPEDITION_RACE_VMG:
        case EXPEDITION_RACE_VMG_TARG_VMG_DELTA:
        case EXPEDITION_RACE_RATE_OF_TURN:
        case EXPEDITION_RACE_RUDDER_ANGLE:
        case EXPEDITION_RACE_TARGET_HEEL:
        case EXPEDITION_RACE_TIME_TO_PORT_LAYLINE:
        case EXPEDITION_RACE_TIME_TO_STB_LAYLINE:
        case EXPEDITION_RACE_DIST_TO_PORT_LAYLINE:
        case EXPEDITION_RACE_DIST_TO_STB_LAYLINE:
        case EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT:
        case EXPEDITION_RACE_TIME_TO_PIN:
        case EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT:
        case EXPEDITION_RACE_TIME_TO_BURN_TO_PIN:
        case EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT:
        case EXPEDITION_RACE_DISTANCE_TO_PIN:
        case EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION:
        case EXPEDITION_LEG_AWA:
        case EXPEDITION_LEG_AWS:
        case EXPEDITION_LEG_TWA:
        case EXPEDITION_LEG_TWS:
        case EXPEDITION_LEG_TWD:
        case EXPEDITION_LEG_TARG_TWA:
        case EXPEDITION_LEG_BOAT_SPEED:
        case EXPEDITION_LEG_TARG_BOAT_SPEED:
        case EXPEDITION_LEG_SOG:
        case EXPEDITION_LEG_COG:
        case EXPEDITION_LEG_FORESTAY_LOAD:
        case EXPEDITION_LEG_RAKE:
        case EXPEDITION_LEG_COURSE:
        case EXPEDITION_LEG_HEADING:
        case EXPEDITION_LEG_VMG:
        case EXPEDITION_LEG_VMG_TARG_VMG_DELTA:
        case EXPEDITION_LEG_RATE_OF_TURN:
        case EXPEDITION_LEG_RUDDER_ANGLE:
        case EXPEDITION_LEG_TARGET_HEEL:
        case EXPEDITION_LEG_TIME_TO_PORT_LAYLINE:
        case EXPEDITION_LEG_TIME_TO_STB_LAYLINE:
        case EXPEDITION_LEG_DIST_TO_PORT_LAYLINE:
        case EXPEDITION_LEG_DIST_TO_STB_LAYLINE:
        case EXPEDITION_LEG_TIME_TO_COMMITTEE_BOAT:
        case EXPEDITION_LEG_TIME_TO_PIN:
        case EXPEDITION_LEG_TIME_TO_BURN_TO_COMMITTEE_BOAT:
        case EXPEDITION_LEG_TIME_TO_BURN_TO_PIN:
        case EXPEDITION_LEG_DISTANCE_TO_COMMITTEE_BOAT:
        case EXPEDITION_LEG_DISTANCE_TO_PIN:
        case EXPEDITION_LEG_LINE_SQUARE_FOR_WIND_DIRECTION:
        case EXPEDITION_RACE_LOAD_S:
        case EXPEDITION_RACE_LOAD_P:
        case EXPEDITION_LEG_BARO:
        case EXPEDITION_LEG_LOAD_S:
        case EXPEDITION_LEG_LOAD_P:
        case EXPEDITION_LEG_JIB_CAR_PORT:
        case EXPEDITION_LEG_JIB_CAR_STBD:
        case EXPEDITION_LEG_MAST_BUTT:
            return "";
        default:
            break;
        }
        // Throwing an exception to get notified if an implementation of
        // a detail type is missing.
        throw new UnsupportedOperationException("There is currently no support for the enum value '" + detailType
                + "' in this method.");
    }
    
    /**
     * Returns a tooltip text for the given detail type or an empty string, if there's none.
     * @param detailType
     * @return A tooltip string for the given detail type.
     */
    public static String getTooltip(DetailType detailType) {
        switch (detailType) {
        case LEG_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.averageAbsoluteCrossTrackErrorInMetersTooltip();
        case LEG_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.averageSignedCrossTrackErrorInMetersTooltip();
        case LEG_GAP_CHANGE_SINCE_LEG_START_IN_SECONDS:
            return stringMessages.gapChangeSinceLegStartInSecondsTooltip();
        case RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.raceAverageAbsoluteCrossTrackErrorInMetersTooltip();
        case RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS:
            return stringMessages.raceAverageSignedCrossTrackErrorInMetersTooltip();
        case LEG_SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED:
            return stringMessages.sideToWhichMarkAtLegStartWasRoundedTooltip();
        case LEG_VELOCITY_MADE_GOOD_IN_KNOTS:
            return stringMessages.velocityMadeGoodInKnotsTooltip();
        case AVERAGE_JIBE_LOSS_IN_METERS:
            return stringMessages.averageJibeLossInMetersTooltip();
        case AVERAGE_MANEUVER_LOSS_IN_METERS:
            return stringMessages.averageManeuverLossInMetersTooltip();
        case AVERAGE_TACK_LOSS_IN_METERS:
            return stringMessages.averageTackLossInMetersTooltip();
        case RACE_CURRENT_LEG:
            return stringMessages.currentLegTooltip();
        case RACE_DISPLAY_LEGS:
            return "";
        case RACE_DISPLAY_BOATS:
            return "";
        case LEG_DISTANCE_TRAVELED:
            return stringMessages.distanceTraveledTooltip();
        case LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START:
            return stringMessages.distanceTraveledIncludingGateStartTooltip();
        case LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.averageSpeedInKnotsTooltip();
        case LEG_ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS:
            return stringMessages.estimatedTimeToNextWaypointInSecondsTooltip();
        case LEG_GAP_TO_LEADER_IN_SECONDS:
            return stringMessages.gapToLeaderInSecondsTooltip();
        case LEG_CORRECTED_TIME_TRAVELED:
            return stringMessages.calculatedTimeTraveledTooltip();
        case JIBE:
            return stringMessages.jibeTooltip();
        case OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.maximumSpeedOverGroundInKnotsTooltip();
        case NUMBER_OF_MANEUVERS:
            return stringMessages.numberOfManeuversTooltip();
        case PENALTY_CIRCLE:
            return stringMessages.penaltyCircleTooltip();
        case RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.raceAverageSpeedInKnotsTooltip();
        case LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.currentOrAverageSpeedOverGroundInKnotsTooltip();
        case RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
            return stringMessages.currentSpeedOverGroundInKnotsTooltip();
        case RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES:
            return stringMessages.courseOverGroundTrueDegreesTooltip();
        case RACE_CURRENT_POSITION_LAT_DEG:
            return stringMessages.currentPositionLatitudeTooltip();
        case RACE_CURRENT_POSITION_LNG_DEG:
            return stringMessages.currentPositionLongitudeTooltip();
        case BRAVO_LEG_CURRENT_RIDE_HEIGHT_IN_METERS:
            return stringMessages.currentOrAverageRideHeightInMetersTooltip();
        case BRAVOEXTENDED_LEG_CURRENT_DISTANCE_FOILED_IN_METERS:
            return stringMessages.currentDistanceFoiledInMetersTooltip();
        case BRAVOEXTENDED_LEG_CURRENT_DURATION_FOILED_IN_SECONDS:
            return stringMessages.currentDurationFoiledTooltip();
        case BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS:
            return stringMessages.currentRideHeightInMetersTooltip();
        case RACE_CURRENT_DURATION_FOILED_IN_SECONDS:
            return stringMessages.currentDurationFoiledTooltip();
        case RACE_CURRENT_DISTANCE_FOILED_IN_METERS:
            return stringMessages.currentDistanceFoiledInMetersTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_PORT_DAGGERBOARD_RAKE:
            return stringMessages.currentPortDaggerboardRakeTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_STBD_DAGGERBOARD_RAKE:
            return stringMessages.currentStbdDaggerboardRakeTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_PORT_RUDDER_RAKE:
            return stringMessages.currentPortRudderRakeTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_STBD_RUDDER_RAKE:
            return stringMessages.currentStbdRudderRakeTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_MAST_ROTATION_IN_DEGREES:
            return stringMessages.currentMastRotationInDegreeTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_DEPTH_IN_METERS:
            return stringMessages.currentDepthInMetersTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_DRIFT_IN_DEGREES:
            return stringMessages.currentDriftInDegreesTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_LEEWAY_IN_DEGREES:
            return stringMessages.currentLeewayInDegreesTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_RUDDER_IN_DEGREES:
            return stringMessages.currentRudderInDegreesTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_SET:
            return stringMessages.currentSetTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_TACK_ANGLE_IN_DEGREES:
            return stringMessages.currentTackAngleInDegreesTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_IN_MILLIMETERS:
            return stringMessages.currentDeflectorInMillimetersTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_PERCENTAGE:
            return stringMessages.currentDeflectorPercentageTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_LOAD:
            return stringMessages.currentForestayLoadTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_PRESSURE:
            return stringMessages.currentForestayPressureTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_RAKE_IN_DEGREES:
            return stringMessages.currentRakeInDegreesTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_TARGET_BOATSPEED_PERCENTAGE:
            return stringMessages.currentTargetBoatspeedPercentageTooltip();
        case BRAVOEXTENDED_RACE_CURRENT_TARGET_HEEL_ANGLE_IN_DEGREES:
            return stringMessages.currentTargetHeelAngleInDegreesTooltip();
        case BRAVO_LEG_CURRENT_HEEL_IN_DEGREES:
            return stringMessages.currentHeelInDegreeTooltip();
        case BRAVO_LEG_CURRENT_PITCH_IN_DEGREES:
            return stringMessages.currentPitchDegreeTooltip();
        case RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS:
            return stringMessages.windwardDistanceToCompetitorFarthestAheadInMetersTooltip();
        case CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD:
            return stringMessages.windwardDistanceToCompetitorFarthestAheadInMetersTooltip();
        case DISTANCE_TO_START_AT_RACE_START:
            return stringMessages.distanceToLineAtRaceStartTooltip();
        case TIME_BETWEEN_RACE_START_AND_COMPETITOR_START:
            return stringMessages.timeBetweenRaceStartAndCompetitorStartInSecondsTooltip();
        case SPEED_OVER_GROUND_AT_RACE_START:
            return stringMessages.speedOverGroundAtRaceStartTooltip();
        case SPEED_OVER_GROUND_WHEN_PASSING_START:
            return stringMessages.speedOverGroundWhenPassingStartTooltip();
        case DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS:
            return stringMessages.distanceToStarboardEndOfStartlineWhenPassingStartTooltip();
        case START_TACK:
            return stringMessages.startTackTooltip();
        case RACE_DISTANCE_TRAVELED:
            return stringMessages.raceDistanceTraveledTooltip();
        case RACE_DISTANCE_TRAVELED_INCLUDING_GATE_START:
            return stringMessages.raceDistanceTraveledIncludingGateStartTooltip();
        case RACE_GAP_TO_LEADER_IN_SECONDS:
            return stringMessages.gapToLeaderInSecondsTooltip();
        case RACE_CALCULATED_TIME_TRAVELED:
            return stringMessages.calculatedTimeTraveledTooltip();
        case RACE_IMPLIED_WIND:
            return stringMessages.impliedWindTooltip();
        case RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD:
            return stringMessages.raceCalculatedTimeAtEstimatedArrivalAtCompetitorFarthestAheadInSecondsTooltip();
        case RACE_RANK:
            return stringMessages.rankTooltip();
        case REGATTA_RANK:
            return stringMessages.regattaRankTooltip();
        case OVERALL_RANK:
            return stringMessages.overallRankTooltip();
        case RACE_NET_POINTS:
            return stringMessages.raceNetPointsTooltip();
        case REGATTA_NET_POINTS:
            return stringMessages.regattaNetPointsTooltip();            
        case REGATTA_NET_POINTS_SUM:
            return stringMessages.regattaNetPointsTooltip();            
        case LEG_RANK_GAIN:
            return stringMessages.rankGainTooltip();
        case TACK:
            return stringMessages.tackTooltip();
        case LEG_TIME_TRAVELED:
            return stringMessages.timeTooltip();
        case TOTAL_TIME_SAILED_DOWNWIND_IN_SECONDS:
            return stringMessages.totalTimeSailedDownwindInSecondsTooltip();
        case OVERALL_TOTAL_TIME_SAILED_IN_SECONDS:
            return stringMessages.totalTimeSailedInSecondsTooltip();
        case TOTAL_TIME_SAILED_REACHING_IN_SECONDS:
            return stringMessages.totalTimeSailedReachingInSecondsTooltip();
        case TOTAL_TIME_SAILED_UPWIND_IN_SECONDS:
            return stringMessages.totalTimeSailedUpwindInSecondsTooltip();
        case OVERALL_TOTAL_DISTANCE_TRAVELED:
            return stringMessages.totalDistanceTraveledTooltip();
        case OVERALL_TOTAL_AVERAGE_SPEED_OVER_GROUND:
            return stringMessages.totalAverageSpeedOverGroundTooltip();
        case OVERALL_TOTAL_DURATION_FOILED_IN_SECONDS:
            return stringMessages.totalDurationFoiledInSecondsTooltip();
        case OVERALL_TOTAL_DISTANCE_FOILED_IN_METERS:
            return stringMessages.totalDistanceFoiledInMetersTooltip();
        case OVERALL_TIME_ON_TIME_FACTOR:
            return stringMessages.timeOnTimeFactorTooltip();
        case OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE:
            return stringMessages.timeOnDistanceAllowanceInSecondsPerNauticalMileTooltip();
        case LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS:
            return stringMessages.windwardDistanceToGoInMetersTooltip();
        case RACE_RATIO_BETWEEN_TIME_SINCE_LAST_POSITION_FIX_AND_AVERAGE_SAMPLING_INTERVAL:
            return stringMessages.ratioBetweenTimeSinceLastPositionFixAndAverageSamplingIntervalTooltip();
        case RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START:
            return stringMessages.distanceToLineFiveSecondsBeforeStartTooltip();
        case RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START:
            return stringMessages.speedOverGroundFiveSecondsBeforeStartTooltip();
        case RACE_TIME_TRAVELED:
            return stringMessages.raceTimeTooltip();
        case RACE_TIME_TRAVELED_DOWNWIND:
            return stringMessages.raceTimeDownwindTooltip();
        case RACE_TIME_TRAVELED_REACHING:
            return stringMessages.raceTimeReachingTooltip();
        case RACE_TIME_TRAVELED_UPWIND:
            return stringMessages.raceTimeUpwindTooltip();
        case CHART_BEAT_ANGLE:
            return stringMessages.TWATooltip();
        case CHART_ABS_TWA:
            return stringMessages.absoluteTWATooltip();
        case CHART_DISTANCE_TO_START_LINE:
            return "";
        case CHART_COURSE_OVER_GROUND_TRUE_DEGREES:
            return stringMessages.courseOverGroundTrueDegreesTooltip();
        case OVERALL_TOTAL_SCORED_RACE_COUNT:
            return "";
        case EXPEDITION_RACE_AWA:
        case EXPEDITION_RACE_AWS:
        case EXPEDITION_RACE_BOAT_SPEED:
        case EXPEDITION_RACE_COG:
        case EXPEDITION_RACE_COURSE:
        case EXPEDITION_RACE_DISTANCE_BELOW_LINE:
        case EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT:
        case EXPEDITION_RACE_DISTANCE_TO_PIN:
        case EXPEDITION_RACE_DIST_TO_PORT_LAYLINE:
        case EXPEDITION_RACE_DIST_TO_STB_LAYLINE:
        case EXPEDITION_RACE_FORESTAY_LOAD:
        case EXPEDITION_RACE_HEADING:
        case EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION:
        case EXPEDITION_RACE_RAKE:
        case EXPEDITION_RACE_RATE_OF_TURN:
        case EXPEDITION_RACE_RUDDER_ANGLE:
        case EXPEDITION_RACE_SOG:
        case EXPEDITION_RACE_TARGET_HEEL:
        case EXPEDITION_RACE_TARG_BOAT_SPEED:
        case EXPEDITION_RACE_TARG_TWA:
        case EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT:
        case EXPEDITION_RACE_TIME_TO_BURN_TO_LINE:
        case EXPEDITION_RACE_TIME_TO_BURN_TO_PIN:
        case EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT:
        case EXPEDITION_RACE_TIME_TO_GUN:
        case EXPEDITION_RACE_TIME_TO_PIN:
        case EXPEDITION_RACE_TIME_TO_PORT_LAYLINE:
        case EXPEDITION_RACE_TIME_TO_STB_LAYLINE:
        case EXPEDITION_RACE_TWA:
        case EXPEDITION_RACE_TWD:
        case EXPEDITION_RACE_TWS:
        case EXPEDITION_RACE_VMG:
        case EXPEDITION_RACE_VMG_TARG_VMG_DELTA:
            return "";
        default:
            break;
        }
        
        return "";
    }

    public static NumberFormat getNumberFormat(DetailType detailType) {
        String decimalPlaces = "";
        for (int i = 0; i < detailType.getPrecision(); i++) {
            if (i == 0) {
                decimalPlaces += ".";
            }
            decimalPlaces += "0";
        }
        return NumberFormat.getFormat("0" + decimalPlaces);
    }
}
