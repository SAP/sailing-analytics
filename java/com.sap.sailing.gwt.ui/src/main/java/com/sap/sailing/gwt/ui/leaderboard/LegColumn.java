package com.sap.sailing.gwt.ui.leaderboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gwt.cell.client.TextCell;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.DetailTypeColumn.DataExtractor;
import com.sap.sse.common.Duration;
import com.sap.sse.common.InvertibleComparator;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.common.impl.InvertibleComparatorAdapter;
import com.sap.sse.gwt.client.celltable.AbstractSortableColumnWithMinMax;

/**
 * Displays competitor's rank in leg and makes the column sortable by rank. The leg is
 * identified as an index into the {@link LeaderboardEntryDTO#legDetails} list.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class LegColumn extends ExpandableSortableColumn<String> {
    private final String raceColumnName;
    private final int legIndex;
    private final StringMessages stringMessages;
    private final String headerStyle;
    private final String columnStyle;
    
    private abstract class AbstractLegDetailField<T extends Comparable<?>> implements DataExtractor<T, LeaderboardRowDTO> {
        public T get(LeaderboardRowDTO row) {
            LegEntryDTO entry = getLegEntry(row);
            if (entry == null) {
                return null;
            } else {
                return getFromNonNullEntry(entry);
            }
        }

        protected abstract T getFromNonNullEntry(LegEntryDTO entry);
    }
    
    private class TimeTraveledInSeconds extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return Long.valueOf(entry.timeInMilliseconds / 1000).doubleValue();
        }
    }
    
    private class RankGain implements DataExtractor<Integer, LeaderboardRowDTO> {
        @Override
        public Integer get(LeaderboardRowDTO row) {
            LegEntryDTO legEntry = getLegEntry(row);
            if (legEntry == null || getLegIndex() == 0) {
                // no gain/loss for first leg
                return null;
            } else {
                LegEntryDTO previousEntry = getLegEntry(row, getLegIndex()-1);
                return previousEntry == null ? null : legEntry.rank - previousEntry.rank;
            }
        }
    }
    
    private class ManeuverCountLegDetailsColumn extends FormattedDoubleLeaderboardRowDTODetailTypeColumn {
        public ManeuverCountLegDetailsColumn(String headerStyle, String columnStyle) {
            super(DetailType.NUMBER_OF_MANEUVERS, null, headerStyle, columnStyle, getLeaderboardPanel());
        }
        
        @Override
        protected String getTitle(LeaderboardRowDTO row) {
            String resultString = null;
            LegEntryDTO entry = getLegEntry(row);
            if (entry != null && entry.numberOfManeuvers != null) {
                StringBuilder result = new StringBuilder();
                if (entry.numberOfManeuvers.get(ManeuverType.TACK) != null) {
                    result.append(entry.numberOfManeuvers.get(ManeuverType.TACK));
                    result.append(" ");
                    result.append(stringMessages.tacks());
                }
                if (entry.numberOfManeuvers.get(ManeuverType.JIBE) != null) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    result.append(entry.numberOfManeuvers.get(ManeuverType.JIBE));
                    result.append(" ");
                    result.append(stringMessages.jibes());
                }
                if (entry.numberOfManeuvers.get(ManeuverType.PENALTY_CIRCLE) != null) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    result.append(entry.numberOfManeuvers.get(ManeuverType.PENALTY_CIRCLE));
                    result.append(" ");
                    result.append(stringMessages.penaltyCircles());
                }
                resultString = result.toString();
            }
            return resultString;
        }

        
        @Override
        public String getValue(LeaderboardRowDTO row) {
            Double fieldValue = getFieldValue(row);
            StringBuilder result = new StringBuilder();
            if (fieldValue != null) {
                result.append(getFormatter().format(fieldValue));
            }
            LegEntryDTO entry = getLegEntry(row);
            if (entry != null && entry.numberOfManeuvers != null &&
                    entry.numberOfManeuvers.get(ManeuverType.PENALTY_CIRCLE) != null && (int) entry.numberOfManeuvers.get(ManeuverType.PENALTY_CIRCLE) != 0) {
                result.append(" (");
                result.append(entry.numberOfManeuvers.get(ManeuverType.PENALTY_CIRCLE));
                result.append("P)");
            }
            return result.toString();
        }

        @Override
        protected Double getFieldValue(LeaderboardRowDTO row) {
            LegEntryDTO entry = getLegEntry(row);
            Double result = null;
            if (entry != null) {
                for (ManeuverType maneuverType : new ManeuverType[] { ManeuverType.TACK, ManeuverType.JIBE,
                        ManeuverType.PENALTY_CIRCLE }) {
                    if (entry.numberOfManeuvers != null && entry.numberOfManeuvers.get(maneuverType) != null) {
                        if (result == null) {
                            result = (double) entry.numberOfManeuvers.get(maneuverType);
                        } else {
                            result += (double) entry.numberOfManeuvers.get(maneuverType);
                        }
                    }
                }
            }
            return result;
        }
    }
    
    private class DurationAsSecondsDetailTypeExtractor extends DoubleDetailTypeExtractor {
        public DurationAsSecondsDetailTypeExtractor(Function<LegEntryDTO, Duration> valueExtractor) {
            super(entry -> {
                Duration duration = valueExtractor.apply(entry);
                return duration == null ? null : duration.asSeconds();
            });
        }
    }
    
    private class DoubleDetailTypeExtractor implements DataExtractor<Double, LeaderboardRowDTO> {
        
        private final Function<LegEntryDTO, Double> valueExtractor;

        public DoubleDetailTypeExtractor(Function<LegEntryDTO, Double> valueExtractor) {
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Double get(LeaderboardRowDTO row) {
            Double result = null;
            LegEntryDTO legEntry = getLegEntry(row);
            if (legEntry != null) {
                result = valueExtractor.apply(legEntry);
            }
            return result;
        }
    }

    private class SideToWhichMarkAtLegStartWasRounded extends AbstractLegDetailField<NauticalSide> {
        @Override
        protected NauticalSide getFromNonNullEntry(LegEntryDTO entry) {
            return entry.sideToWhichMarkAtLegStartWasRounded;
        }
    }

    public LegColumn(LeaderboardPanel<?> leaderboardPanel, String raceColumnName, int legIndex, SortingOrder preferredSortingOrder, StringMessages stringMessages,
            List<DetailType> legDetailSelection, String headerStyle, String columnStyle,
            String detailHeaderStyle, String detailColumnStyle) {
        super(leaderboardPanel, /* expandable */true /* all legs have details */, new TextCell(), preferredSortingOrder,
                stringMessages, detailHeaderStyle, detailColumnStyle, legDetailSelection, leaderboardPanel);
        setHorizontalAlignment(ALIGN_CENTER);
        this.raceColumnName = raceColumnName;
        this.legIndex = legIndex;
        this.stringMessages = stringMessages;
        this.headerStyle = headerStyle;
        this.columnStyle = columnStyle;
    }

    @Override
    protected Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getDetailColumnMap(
            LeaderboardPanel<?> leaderboardPanel, StringMessages stringMessages, String detailHeaderStyle,
            String detailColumnStyle) {
        Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> result = new HashMap<>();
        result.put(DetailType.LEG_DISTANCE_TRAVELED,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_DISTANCE_TRAVELED,
                        new DoubleDetailTypeExtractor(e -> e.distanceTraveledInMeters), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START,
                        new DoubleDetailTypeExtractor(e -> e.distanceTraveledIncludingGateStartInMeters),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                        new DoubleDetailTypeExtractor(e -> e.averageSpeedOverGroundInKnots), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS,
                        new DoubleDetailTypeExtractor(e -> e.currentSpeedOverGroundInKnots), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.BRAVO_LEG_CURRENT_HEEL_IN_DEGREES,
                new HeelColumn(DetailType.BRAVO_LEG_CURRENT_HEEL_IN_DEGREES,
                        new DoubleDetailTypeExtractor(e -> e.currentHeelInDegrees), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.BRAVO_LEG_CURRENT_PITCH_IN_DEGREES,
                new PitchColumn(DetailType.BRAVO_LEG_CURRENT_PITCH_IN_DEGREES,
                        new DoubleDetailTypeExtractor(e -> e.currentPitchInDegrees), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.BRAVO_LEG_CURRENT_RIDE_HEIGHT_IN_METERS,
                new RideHeightColumn(DetailType.BRAVO_LEG_CURRENT_RIDE_HEIGHT_IN_METERS,
                        new DoubleDetailTypeExtractor(e -> e.currentRideHeightInMeters), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.BRAVOEXTENDED_LEG_CURRENT_DISTANCE_FOILED_IN_METERS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.BRAVOEXTENDED_LEG_CURRENT_DISTANCE_FOILED_IN_METERS,
                        new DoubleDetailTypeExtractor(e -> e.currentDistanceFoiledInMeters), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.BRAVOEXTENDED_LEG_CURRENT_DURATION_FOILED_IN_SECONDS,
                new TotalTimeColumn(DetailType.BRAVOEXTENDED_LEG_CURRENT_DURATION_FOILED_IN_SECONDS,
                        new DoubleDetailTypeExtractor(e -> e.currentDurationFoiledInSeconds), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS,
                        new DoubleDetailTypeExtractor(e -> e.estimatedTimeToNextWaypointInSeconds), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_GAP_TO_LEADER_IN_SECONDS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_GAP_TO_LEADER_IN_SECONDS,
                        new DoubleDetailTypeExtractor(e -> e.gapToLeaderInSeconds), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_GAP_CHANGE_SINCE_LEG_START_IN_SECONDS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_GAP_CHANGE_SINCE_LEG_START_IN_SECONDS,
                        new DoubleDetailTypeExtractor(e -> e.gapChangeSinceLegStartInSeconds), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED,
                new SideToWhichMarkAtLegStartWasRoundedColumn(stringMessages.sideToWhichMarkAtLegStartWasRounded(),
                        new SideToWhichMarkAtLegStartWasRounded(), detailHeaderStyle, detailColumnStyle, stringMessages,
                        leaderboardPanel));
        result.put(DetailType.LEG_VELOCITY_MADE_GOOD_IN_KNOTS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_VELOCITY_MADE_GOOD_IN_KNOTS,
                        new DoubleDetailTypeExtractor(e -> e.velocityMadeGoodInKnots), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS,
                        new DoubleDetailTypeExtractor(e -> e.windwardDistanceToGoInMeters), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_RANK_GAIN, new RankGainColumn(stringMessages.rankGain(), new RankGain(),
                detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.NUMBER_OF_MANEUVERS,
                new ManeuverCountLegDetailsColumn(detailHeaderStyle, detailColumnStyle));
        result.put(DetailType.LEG_TIME_TRAVELED, new TotalTimeColumn(DetailType.LEG_TIME_TRAVELED,
                new TimeTraveledInSeconds(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_CORRECTED_TIME_TRAVELED,
                new TotalTimeColumn(DetailType.LEG_CORRECTED_TIME_TRAVELED,
                        new DurationAsSecondsDetailTypeExtractor(e -> e.correctedTotalTime), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS,
                        new DoubleDetailTypeExtractor(e -> e.currentOrAverageAbsoluteCrossTrackErrorInMeters), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.LEG_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.LEG_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS,
                        new DoubleDetailTypeExtractor(e -> e.currentOrAverageSignedCrossTrackErrorInMeters), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_AWA,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_AWA,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionAWA), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_AWS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_AWS,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionAWS), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TWA,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TWA,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTWA), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TWS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TWS,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTWS), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TWD,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TWD,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTWD), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TARG_TWA,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TARG_TWA,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTargTWA), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_BOAT_SPEED,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_BOAT_SPEED,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionBoatSpeed), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TARG_BOAT_SPEED,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TARG_BOAT_SPEED,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTargBoatSpeed), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_SOG,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_SOG,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionSOG), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_COG,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_COG,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionCOG), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_FORESTAY_LOAD,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_FORESTAY_LOAD,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionForestayLoad), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_RAKE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_RAKE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionRake), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_COURSE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_COURSE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionCourseDetail), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_HEADING,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_HEADING,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionHeading), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_VMG,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_VMG,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionVMG), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_VMG_TARG_VMG_DELTA,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_VMG_TARG_VMG_DELTA,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionVMGTargVMGDelta), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_RATE_OF_TURN,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_RATE_OF_TURN,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionRateOfTurn), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_RUDDER_ANGLE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_RUDDER_ANGLE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionRudderAngle), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TARGET_HEEL,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TARGET_HEEL,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTargetHeel), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_PORT_LAYLINE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_PORT_LAYLINE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToPortLayline), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_STB_LAYLINE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_STB_LAYLINE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToStbLayline), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_DIST_TO_PORT_LAYLINE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_DIST_TO_PORT_LAYLINE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionDistToPortLayline), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_DIST_TO_STB_LAYLINE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_DIST_TO_STB_LAYLINE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionDistToStbLayline), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_GUN,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_GUN,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToGUN),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_COMMITTEE_BOAT,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_COMMITTEE_BOAT,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToCommitteeBoat), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_PIN,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_PIN,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToPin), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_BURN_TO_LINE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_BURN_TO_LINE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToBurnToLineInSeconds),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_BURN_TO_COMMITTEE_BOAT,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_BURN_TO_COMMITTEE_BOAT,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToBurnToCommitteeBoat),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_TIME_TO_BURN_TO_PIN,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_TIME_TO_BURN_TO_PIN,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionTimeToBurnToPin), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_DISTANCE_TO_COMMITTEE_BOAT,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_DISTANCE_TO_COMMITTEE_BOAT,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionDistanceToCommitteeBoat),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_DISTANCE_TO_PIN,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_DISTANCE_TO_PIN,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionDistanceToPinDetail), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_DISTANCE_BELOW_LINE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_DISTANCE_BELOW_LINE,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionDistanceBelowLine), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_LINE_SQUARE_FOR_WIND_DIRECTION,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_LINE_SQUARE_FOR_WIND_DIRECTION,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionLineSquareForWindDirection),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_BARO,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_BARO,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionBaro), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_LOAD_S,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_LOAD_S,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionLoadS), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_LOAD_P,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_LOAD_P,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionLoadP), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_JIB_CAR_PORT,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_JIB_CAR_PORT,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionJibCarPort), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_JIB_CAR_STBD,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_JIB_CAR_STBD,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionJibCarStbd), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_LEG_MAST_BUTT,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_LEG_MAST_BUTT,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionMastButt), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        result.put(DetailType.EXPEDITION_RACE_KICKER_TENSION,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_KICKER_TENSION,
                        new DoubleDetailTypeExtractor(LegEntryDTO::getExpeditionKickerTension), detailHeaderStyle,
                        detailColumnStyle, leaderboardPanel));
        return result;
    }

    private int getLegIndex() {
        return legIndex;
    }
    
    @Override
    public String getColumnStyle() {
        return columnStyle;
    }
    
    @Override
    public String getHeaderStyle() {
        return headerStyle;
    }

    private String getRaceName() {
        return raceColumnName;
    }

    private LegEntryDTO getLegEntry(LeaderboardRowDTO row) {
        int theLegIndex = getLegIndex();
        return getLegEntry(row, theLegIndex);
    }

    private LegEntryDTO getLegEntry(LeaderboardRowDTO row, int theLegIndex) {
        LegEntryDTO legEntry = null;
        LeaderboardEntryDTO entry = row.fieldsByRaceColumnName.get(getRaceName());
        if (entry != null && entry.legDetails != null && entry.legDetails.size() > theLegIndex) {
            legEntry = entry.legDetails.get(theLegIndex);
        }
        return legEntry;
    }
    
    @Override
    public InvertibleComparator<LeaderboardRowDTO> getComparator() {
        return new InvertibleComparatorAdapter<LeaderboardRowDTO>(getPreferredSortingOrder().isAscending()) {
            @Override
            public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                LegEntryDTO o1Entry = getLegEntry(o1);
                LegEntryDTO o2Entry = getLegEntry(o2);
                return o1Entry == null ? o2Entry == null ? 0 : isAscending()?1:-1
                                       : o2Entry == null ? isAscending()?-1:1 : o1Entry.rank - o2Entry.rank;
            }
        };
    }

    @Override
    public SortableExpandableColumnHeader getHeader() {
        SortableExpandableColumnHeader result = new SortableExpandableColumnHeader(/* title */ stringMessages.leg()+(legIndex+1),
                /* iconURL */ null, getLeaderboardPanel(), this, stringMessages);
        return result;
    }
    
    @Override
    public String getValue(LeaderboardRowDTO row) {
        LeaderboardEntryDTO leaderboardEntryDTO = row.fieldsByRaceColumnName.get(raceColumnName);
        LegEntryDTO legEntry = getLegEntry(row);
        if (legEntry != null && legEntry.rank != 0) {
            return ""+legEntry.rank;
        }  else if (leaderboardEntryDTO != null && leaderboardEntryDTO.legDetails != null && legIndex+1 > leaderboardEntryDTO.legDetails.size()) {
            return "n/a";
        } else {
            return "";
        }
    }
    
}

