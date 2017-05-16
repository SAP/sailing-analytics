package com.sap.sailing.gwt.ui.leaderboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Header;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.InvertibleComparator;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.SortingOrder;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.impl.InvertibleComparatorAdapter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.AbstractSortableColumnWithMinMax;
import com.sap.sailing.gwt.ui.leaderboard.DetailTypeColumn.LegDetailField;

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
    
    private abstract class AbstractLegDetailField<T extends Comparable<?>> implements LegDetailField<T> {
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
    
    private class DistanceTraveledInMeters extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.distanceTraveledInMeters;
        }
    }
    
    private class DistanceTraveledIncludingGateStartInMeters extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.distanceTraveledIncludingGateStartInMeters;
        }
    }
    
    private class TimeTraveledInSeconds extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return new Long(entry.timeInMilliseconds / 1000).doubleValue();
        }
    }
    
    private class CorrectedTimeTraveledInSeconds extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.correctedTotalTime == null ? null : entry.correctedTotalTime.asSeconds();
        }
    }
    
    private class AverageSpeedOverGroundInKnots extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.averageSpeedOverGroundInKnots;
        }
    }
    
    private class AverageAbsoluteCrossTrackErrorInMeters extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.averageAbsoluteCrossTrackErrorInMeters;
        }
    }
    
    private class AverageSignedCrossTrackErrorInMeters extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.averageSignedCrossTrackErrorInMeters;
        }
    }
    
    private class CurrentSpeedOverGroundInKnots extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.currentSpeedOverGroundInKnots;
        }
    }
    
    private class CurrentHeelInDegrees extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.currentHeelInDegrees;
        }
    }

    private class CurrentPitchInDegrees extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.currentPitchInDegrees;
        }
    }

    private class CurrentRideHeightInMeters extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.currentRideHeightInMeters;
        }
    }
    
    private class EstimatedTimeToNextWaypointInSeconds extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.estimatedTimeToNextWaypointInSeconds;
        }
    }
    
    private class GapToLeaderInSeconds extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.gapToLeaderInSeconds;
        }
    }
    
    private class GapChangeSinceLegStartInSeconds extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.gapChangeSinceLegStartInSeconds;
        }
    }
    
    private class SideToWhichMarkAtLegStartWasRounded extends AbstractLegDetailField<NauticalSide> {
        @Override
        protected NauticalSide getFromNonNullEntry(LegEntryDTO entry) {
            return entry.sideToWhichMarkAtLegStartWasRounded;
        }
    }
    
    private class VelocityMadeGoodInKnots extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.velocityMadeGoodInKnots;
        }
    }
    
    private class WindwardDistanceToGoInMeters extends AbstractLegDetailField<Double> {
        @Override
        protected Double getFromNonNullEntry(LegEntryDTO entry) {
            return entry.windwardDistanceToGoInMeters;
        }
    }
    
    private class RankGain implements LegDetailField<Integer> {
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
    
    private class ManeuverCountLegDetailsColumn extends FormattedDoubleDetailTypeColumn {
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
        
    public LegColumn(UnStyledLeaderboardPanel leaderboardPanel, String raceColumnName, int legIndex,
            SortingOrder preferredSortingOrder, StringMessages stringMessages,
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
    
    public static DetailType[] getAvailableLegDetailColumnTypes() {
        return new DetailType[] { DetailType.AVERAGE_SPEED_OVER_GROUND_IN_KNOTS, DetailType.DISTANCE_TRAVELED,
                DetailType.DISTANCE_TRAVELED_INCLUDING_GATE_START, DetailType.GAP_TO_LEADER_IN_SECONDS,
                DetailType.GAP_CHANGE_SINCE_LEG_START_IN_SECONDS, DetailType.SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED, 
                DetailType.CURRENT_SPEED_OVER_GROUND_IN_KNOTS, DetailType.CURRENT_HEEL_IN_DEGREES,
                DetailType.CURRENT_PITCH_IN_DEGREES, DetailType.CURRENT_RIDE_HEIGHT_IN_METERS,
                DetailType.WINDWARD_DISTANCE_TO_GO_IN_METERS, DetailType.NUMBER_OF_MANEUVERS,
                DetailType.ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS, DetailType.VELOCITY_MADE_GOOD_IN_KNOTS,
                DetailType.TIME_TRAVELED, DetailType.CORRECTED_TIME_TRAVELED, DetailType.AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS,
                DetailType.AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS, DetailType.RANK_GAIN };
    }

    @Override
    protected Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getDetailColumnMap(
            UnStyledLeaderboardPanel leaderboardPanel, StringMessages stringMessages, String detailHeaderStyle,
            String detailColumnStyle) {
        Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> result = new HashMap<>();
        result.put(DetailType.DISTANCE_TRAVELED,
                new FormattedDoubleDetailTypeColumn(DetailType.DISTANCE_TRAVELED, new DistanceTraveledInMeters(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.DISTANCE_TRAVELED_INCLUDING_GATE_START,
                new FormattedDoubleDetailTypeColumn(DetailType.DISTANCE_TRAVELED_INCLUDING_GATE_START, new DistanceTraveledIncludingGateStartInMeters(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.AVERAGE_SPEED_OVER_GROUND_IN_KNOTS, 
                new FormattedDoubleDetailTypeColumn(DetailType.AVERAGE_SPEED_OVER_GROUND_IN_KNOTS, new AverageSpeedOverGroundInKnots(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.CURRENT_SPEED_OVER_GROUND_IN_KNOTS, 
                new FormattedDoubleDetailTypeColumn(DetailType.CURRENT_SPEED_OVER_GROUND_IN_KNOTS, new CurrentSpeedOverGroundInKnots(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));

        result.put(DetailType.CURRENT_HEEL_IN_DEGREES, new HeelColumn(DetailType.CURRENT_HEEL_IN_DEGREES,
                new CurrentHeelInDegrees(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.CURRENT_PITCH_IN_DEGREES, new PitchColumn(DetailType.CURRENT_PITCH_IN_DEGREES,
                new CurrentPitchInDegrees(),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.CURRENT_RIDE_HEIGHT_IN_METERS, new RideHeightColumn(DetailType.CURRENT_RIDE_HEIGHT_IN_METERS,
                new CurrentRideHeightInMeters(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS,
                new FormattedDoubleDetailTypeColumn(DetailType.ESTIMATED_TIME_TO_NEXT_WAYPOINT_IN_SECONDS, new EstimatedTimeToNextWaypointInSeconds(),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.GAP_TO_LEADER_IN_SECONDS,
                new FormattedDoubleDetailTypeColumn(DetailType.GAP_TO_LEADER_IN_SECONDS, new GapToLeaderInSeconds(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.GAP_CHANGE_SINCE_LEG_START_IN_SECONDS,
                new FormattedDoubleDetailTypeColumn(DetailType.GAP_CHANGE_SINCE_LEG_START_IN_SECONDS, new GapChangeSinceLegStartInSeconds(),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.SIDE_TO_WHICH_MARK_AT_LEG_START_WAS_ROUNDED,
                new SideToWhichMarkAtLegStartWasRoundedColumn(stringMessages.sideToWhichMarkAtLegStartWasRounded(),
                        new SideToWhichMarkAtLegStartWasRounded(), detailHeaderStyle, detailColumnStyle, stringMessages, leaderboardPanel));
        result.put(DetailType.VELOCITY_MADE_GOOD_IN_KNOTS,
                new FormattedDoubleDetailTypeColumn(DetailType.VELOCITY_MADE_GOOD_IN_KNOTS, new VelocityMadeGoodInKnots(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.WINDWARD_DISTANCE_TO_GO_IN_METERS, 
                new FormattedDoubleDetailTypeColumn(DetailType.WINDWARD_DISTANCE_TO_GO_IN_METERS, new WindwardDistanceToGoInMeters(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.RANK_GAIN, new RankGainColumn(stringMessages.rankGain(), new RankGain(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.NUMBER_OF_MANEUVERS, new ManeuverCountLegDetailsColumn(detailHeaderStyle, detailColumnStyle));
        result.put(DetailType.TIME_TRAVELED,
                new FormattedDoubleDetailTypeColumn(DetailType.TIME_TRAVELED, new TimeTraveledInSeconds(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.CORRECTED_TIME_TRAVELED,
                new FormattedDoubleDetailTypeColumn(DetailType.CORRECTED_TIME_TRAVELED, new CorrectedTimeTraveledInSeconds(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS, 
                new FormattedDoubleDetailTypeColumn(DetailType.AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS, new AverageAbsoluteCrossTrackErrorInMeters(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS, 
                new FormattedDoubleDetailTypeColumn(DetailType.AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS, new AverageSignedCrossTrackErrorInMeters(), detailHeaderStyle, detailColumnStyle, leaderboardPanel));
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
    public Header<SafeHtml> getHeader() {
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
        }  else if (leaderboardEntryDTO.legDetails != null && legIndex+1 > leaderboardEntryDTO.legDetails.size()) {
            return "n/a";
        } else {
            return "";
        }
    }
    
}

