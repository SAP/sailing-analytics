package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SelectionModel;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.gwt.ui.client.Collator;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.PlayStateListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.TimeListener;
import com.sap.sailing.gwt.ui.client.Timer;
import com.sap.sailing.gwt.ui.client.Timer.PlayModes;
import com.sap.sailing.gwt.ui.client.Timer.PlayStates;
import com.sap.sailing.gwt.ui.client.UserAgentChecker.UserAgentTypes;
import com.sap.sailing.gwt.ui.leaderboard.LegDetailColumn.LegDetailField;
import com.sap.sailing.gwt.ui.shared.CompetitorDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardEntryDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardRowDTO;
import com.sap.sailing.gwt.ui.shared.LegEntryDTO;
import com.sap.sailing.gwt.ui.shared.RaceInLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.components.Component;
import com.sap.sailing.gwt.ui.shared.components.IsEmbeddableComponent;
import com.sap.sailing.gwt.ui.shared.components.SettingsDialog;
import com.sap.sailing.gwt.ui.shared.components.SettingsDialogComponent;
import com.sap.sailing.gwt.ui.shared.panels.BusyIndicator;
import com.sap.sailing.gwt.ui.shared.panels.SimpleBusyIndicator;

/**
 * A leaderboard essentially consists of a table widget that in its columns displays the entries.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class LeaderboardPanel extends FormPanel implements TimeListener, PlayStateListener,
        Component<LeaderboardSettings>, IsEmbeddableComponent, CompetitorSelectionChangeListener {
    private static final int RANK_COLUMN_INDEX = 0;

    private static final int SAIL_ID_COLUMN_INDEX = 1;

    private static final int CARRY_COLUMN_INDEX = 3;

    private final SailingServiceAsync sailingService;

    /**
     * The leaderboard name is used to
     * {@link SailingServiceAsync#getLeaderboardByName(String, java.util.Date, String[], com.google.gwt.user.client.rpc.AsyncCallback)
     * obtain the leaderboard contents} from the server. It may change in case the leaderboard is renamed.
     */
    private String leaderboardName;

    private final ErrorReporter errorReporter;

    private final StringMessages stringMessages;

    private final CellTable<LeaderboardRowDTO> leaderboardTable;

    private final SelectionModel<LeaderboardRowDTO> leaderboardSelectionModel;

    private ListDataProvider<LeaderboardRowDTO> data;

    private final ListHandler<LeaderboardRowDTO> listHandler;

    private LeaderboardDTO leaderboard;

    private final RankColumn rankColumn;

    /**
     * Passed to the {@link ManeuverCountRaceColumn}. Modifications to this list will modify the column's children list
     * when updated the next time.
     */
    private final List<DetailType> selectedManeuverDetails;

    /**
     * Passed to the {@link LegColumn}. Modifications to this list will modify the column's children list when updated
     * the next time.
     */
    private final List<DetailType> selectedLegDetails;

    /**
     * Passed to the {@link TextRaceColumn}. Modifications to this list will modify the column's children list when
     * updated the next time.
     */
    private final List<DetailType> selectedRaceDetails;

    private List<RaceInLeaderboardDTO> selectedRaceColumns;

    protected final String RACE_COLUMN_HEADER_STYLE;

    protected final String LEG_DETAIL_COLUMN_HEADER_STYLE;

    protected final String LEG_DETAIL_COLUMN_STYLE;

    protected final String LEG_COLUMN_HEADER_STYLE;

    protected final String RACE_COLUMN_STYLE;

    protected final String LEG_COLUMN_STYLE;

    protected final String TOTAL_COLUMN_STYLE;

    private final Timer timer;

    private boolean autoExpandFirstRace;

    /**
     * This anchor's HTML holds the image tag for the play/pause button that needs to be updated when the {@link #timer}
     * changes its playing state
     */
    private final Anchor playPause;

    private final CompetitorSelectionProvider competitorSelectionProvider;

    /**
     * If this is <code>null</code>, all leaderboard columns added by updating the leaderboard from the server are
     * automatically added to the table. Otherwise, only the column whose
     * {@link RaceInLeaderboardDTO#getRaceIdentifier() race identifier} matches the value of this attribute will be
     * added.
     */
    private final RaceIdentifier preSelectedRace;

    private final VerticalPanel contentPanel;
    private final DockPanel headerPanel;
    private final HorizontalPanel refreshAndSettingsPanel;

    private boolean isEmbedded = false;

    private static LeaderboardResources resources = GWT.create(LeaderboardResources.class);
    private static LeaderboardTableResources tableResources = GWT.create(LeaderboardTableResources.class);

    private final ImageResource pauseIcon;
    private final ImageResource playIcon;
    
    private final BusyIndicator busyIndicator;

    /**
     * Tells whether the leaderboard settings were explicitly changed by an external call to
     * {@link #updateSettings(LeaderboardSettings)}. If so, a {@link #playStateChanged(PlayStates, PlayModes) play state
     * change} will not automatically lead to a settings change.
     */
    private boolean settingsUpdatedExplicitly = false;

    /**
     * Tells if the leaderboard is currently handling a {@link #playStateChanged(PlayStates, PlayModes) play state change}.
     * If this is the case, a call to {@link #updateSettings(LeaderboardSettings)} won't set the
     * {@link #settingsUpdatedExplicitly} flag.
     */
    private boolean currentlyHandlingPlayStateChange;

    private PlayModes oldPlayMode;
    
    private class SettingsClickHandler implements ClickHandler {
        private final StringMessages stringMessages;

        private SettingsClickHandler(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public void onClick(ClickEvent event) {
            new SettingsDialog<LeaderboardSettings>(LeaderboardPanel.this, stringMessages).show();
        }
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public Widget getHeaderWidget() {
        return headerPanel;
    }

    @Override
    public Widget getContentWidget() {
        return contentPanel;
    }

    @Override
    public Widget getToolbarWidget() {
        return null;
    }

    @Override
    public boolean isEmbedded() {
        return isEmbedded;
    }

    public void updateSettings(LeaderboardSettings newSettings) {
        if (!currentlyHandlingPlayStateChange) {
            settingsUpdatedExplicitly = true;
        }
        List<ExpandableSortableColumn<?>> columnsToExpandAgain = new ArrayList<ExpandableSortableColumn<?>>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(i);
            if (c instanceof ExpandableSortableColumn<?>) {
                ExpandableSortableColumn<?> expandableSortableColumn = (ExpandableSortableColumn<?>) c;
                if (expandableSortableColumn.isExpanded()) {
                    // now toggle expansion back and forth,
                    // enforcing a re-build of the visible
                    // child columns
                    expandableSortableColumn.toggleExpansion();
                    columnsToExpandAgain.add(expandableSortableColumn);
                }
            }
        }
        if (newSettings.getManeuverDetailsToShow() != null) {
            selectedManeuverDetails.clear();
            selectedManeuverDetails.addAll(newSettings.getManeuverDetailsToShow());
        }
        if (newSettings.getLegDetailsToShow() != null) {
            selectedLegDetails.clear();
            selectedLegDetails.addAll(newSettings.getLegDetailsToShow());
        }
        if (newSettings.getRaceDetailsToShow() != null) {
            selectedRaceDetails.clear();
            selectedRaceDetails.addAll(newSettings.getRaceDetailsToShow());
        }
        if (newSettings.getRaceColumnsToShow() != null) {
            selectedRaceColumns.clear();
            selectedRaceColumns.addAll(newSettings.getRaceColumnsToShow());
        }
        setAutoExpandFirstRace(false); // avoid expansion during updateLeaderboard(...); will expand later if it was expanded before
        // update leaderboard after settings panel column selection change
        updateLeaderboard(leaderboard);
        setAutoExpandFirstRace(newSettings.isAutoExpandFirstRace());

        if (newSettings.getDelayBetweenAutoAdvancesInMilliseconds() != null) {
            timer.setRefreshInterval(newSettings.getDelayBetweenAutoAdvancesInMilliseconds());
        }
        if (newSettings.getDelayInMilliseconds() != null) {
            setDelayInMilliseconds(newSettings.getDelayInMilliseconds());
        }
        for (ExpandableSortableColumn<?> expandableSortableColumn : columnsToExpandAgain) {
            expandableSortableColumn.toggleExpansion();
        }
        if (newSettings.getNameOfRaceToSort() != null) {
            sort(getRaceColumnByRaceName(newSettings.getNameOfRaceToSort()), /* ascending */ true);
        }
    }

    protected class CompetitorColumn extends SortableColumn<LeaderboardRowDTO, String> {

        protected CompetitorColumn() {
            super(new TextCell());
        }

        protected CompetitorColumn(EditTextCell editTextCell) {
            super(editTextCell);
        }

        @Override
        public Comparator<LeaderboardRowDTO> getComparator() {
            return new Comparator<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    return Collator.getInstance().compare(getLeaderboard().getDisplayName(o1.competitor),
                            getLeaderboard().getDisplayName(o2.competitor));
                }
            };
        }

        @Override
        public Header<String> getHeader() {
            return new TextHeader(stringMessages.name());
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            return getLeaderboard().getDisplayName(object.competitor);
        }
    }

    /**
     * Shows the country flag and sail ID, if present
     * 
     * @author Axel Uhl (d043530)
     * 
     */
    private class SailIDColumn extends SortableColumn<LeaderboardRowDTO, String> {

        protected SailIDColumn() {
            super(new TextCell());
        }

        @Override
        public Comparator<LeaderboardRowDTO> getComparator() {
            return new Comparator<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    return o1.competitor.sailID == null ? o2.competitor.sailID == null ? 0 : -1
                            : o2.competitor.sailID == null ? 1 : Collator.getInstance().compare(o1.competitor.sailID,
                                    o2.competitor.sailID);
                }
            };
        }

        @Override
        public Header<String> getHeader() {
            return new TextHeader(stringMessages.competitor());
        }

        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<img title=\"" + object.competitor.countryName + "\" src=\""
                    + getFlagURL(object.competitor.twoLetterIsoCountryCode) + "\"/>&nbsp;");
            sb.appendEscaped(object.competitor.sailID);
        }

        private String getFlagURL(String twoLetterIsoCountryCode) {
            return "/gwt/images/flags/" + twoLetterIsoCountryCode.toLowerCase() + ".png";
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            return object.competitor.sailID;
        }
    }

    /**
     * Displays net/total points and possible max-points reasons based on a {@link LeaderboardRowDTO} and a race name
     * and makes the column sortable by the total points.
     * 
     * @author Axel Uhl (D043530)
     * 
     */
    protected abstract class RaceColumn<C> extends ExpandableSortableColumn<C> {
        private RaceInLeaderboardDTO race;

        private final String headerStyle;
        private final String columnStyle;

        public RaceColumn(RaceInLeaderboardDTO race, boolean enableExpansion, Cell<C> cell,
                String headerStyle, String columnStyle) {
            super(LeaderboardPanel.this, enableExpansion, cell, stringMessages, LEG_COLUMN_HEADER_STYLE,
                    LEG_COLUMN_STYLE, selectedRaceDetails);
            setHorizontalAlignment(ALIGN_CENTER);
            this.race = race;
            this.headerStyle = headerStyle;
            this.columnStyle = columnStyle;
        }

        public RaceInLeaderboardDTO getRace() {
            return race;
        }

        public String getRaceName() {
            return race.getRaceColumnName();
        }

        public boolean isMedalRace() {
            return race.isMedalRace();
        }

        @Override
        public String getColumnStyle() {
            return columnStyle;
        }

        /**
         * Displays a combination of total points and maxPointsReason in bold, transparent, strike-through, depending on
         * various criteria. Here's how:
         * 
         * <pre>
         *                                  total points                |    maxPointsReason
         * -------------------------------+-----------------------------+-----------------------
         *  not discarded, no maxPoints   | bold                        | none
         *  not discarded, maxPoints      | bold                        | transparent
         *  discarded, no maxPoints       | transparent, strike-through | none
         *  discarded, maxPoints          | transparent, strike-through | transparent, strike-through
         * </pre>
         */
        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder html) {
            LeaderboardEntryDTO entry = object.fieldsByRaceName.get(getRaceName());
            if (entry != null) {
                // don't show points if max points / penalty
                if (entry.reasonForMaxPoints.equals("NONE")) {
                    if (!entry.discarded) {
                        html.appendHtmlConstant("<span style=\"font-weight: bold;\">");
                        html.append(entry.totalPoints);
                        html.appendHtmlConstant("</span>");
                    } else {
                        html.appendHtmlConstant(" <span style=\"opacity: 0.5;\"><del>");
                        html.append(entry.netPoints);
                        html.appendHtmlConstant("</del></span>");
                    }
                } else {
                    html.appendHtmlConstant(" <span style=\"opacity: 0.5;\">");
                    if (entry.discarded) {
                        html.appendHtmlConstant("<del>");
                    }
                    html.appendEscaped(entry.reasonForMaxPoints);
                    if (entry.discarded) {
                        html.appendHtmlConstant("</del>");
                    }
                    html.appendHtmlConstant("</span>");
                }
            }
        }

        @Override
        public Comparator<LeaderboardRowDTO> getComparator() {
            if (race.isMedalRace()) {
                return getLeaderboard().getMedalRaceComparator(race.getRaceColumnName());
            } else {
                return new NetPointsComparator(isSortedAscendingForThisColumn(getLeaderboardPanel().getLeaderboardTable()),
                        race.getRaceColumnName());
            }
        }

        @Override
        public String getHeaderStyle() {
            return headerStyle;
        }

        @Override
        public Header<SafeHtml> getHeader() {
            SortableExpandableColumnHeader header = new SortableExpandableColumnHeader(
            /* title */race.getRaceColumnName(),
            /* iconURL */race.isMedalRace() ? "/gwt/images/medal_small.png" : null, LeaderboardPanel.this, this, stringMessages);
            return header;
        }

    }

    public static DetailType[] getAvailableRaceDetailColumnTypes() {
        return new DetailType[] { DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                DetailType.RACE_DISTANCE_TRAVELED, DetailType.RACE_GAP_TO_LEADER_IN_SECONDS, DetailType.RACE_DISTANCE_TO_LEADER_IN_METERS,
                DetailType.NUMBER_OF_MANEUVERS, DetailType.DISPLAY_LEGS, DetailType.CURRENT_LEG };
    }

    private class TextRaceColumn extends RaceColumn<String> implements RaceNameProvider {
        /**
         * Remembers the leg columns; <code>null</code>-padded, if {@link #getLegColumn(int)} asks for a column index
         * not yet existing. It is important to remember the columns because column removal happens based on identity.
         */
        private final List<LegColumn> legColumns;

        public TextRaceColumn(RaceInLeaderboardDTO race, boolean expandable, String headerStyle,
                String columnStyle) {
            super(race, expandable, new TextCell(), headerStyle, columnStyle);
            legColumns = new ArrayList<LegColumn>();
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            return "" + object.fieldsByRaceName.get(getRaceName()).totalPoints;
        }

        @Override
        protected void ensureExpansionDataIsLoaded(final Runnable callWhenExpansionDataIsLoaded) {
            if (getLeaderboard().getLegCount(getRaceName()) != -1) {
                callWhenExpansionDataIsLoaded.run();
            } else {
                getSailingService().getLeaderboardByName(getLeaderboardName(), getLeaderboardDisplayDate(),
                /* namesOfRacesForWhichToLoadLegDetails */getNamesOfExpandedRaces(),
                        new AsyncCallback<LeaderboardDTO>() {
                            @Override
                            public void onSuccess(LeaderboardDTO result) {
                                updateLeaderboard(result);
                                callWhenExpansionDataIsLoaded.run();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                getErrorReporter().reportError(
                                        "Error trying to obtain leaderboard contents: " + caught.getMessage());
                            }
                        });
            }
        }

        @Override
        protected Map<DetailType, SortableColumn<LeaderboardRowDTO, ?>> getDetailColumnMap(
                LeaderboardPanel leaderboardPanel, StringMessages stringMessages, String detailHeaderStyle,
                String detailColumnStyle) {
            Map<DetailType, SortableColumn<LeaderboardRowDTO, ?>> result = new HashMap<DetailType, SortableColumn<LeaderboardRowDTO, ?>>();
            result.put(
                    DetailType.RACE_DISTANCE_TRAVELED,
                    new FormattedDoubleLegDetailColumn(stringMessages.distanceInMeters(), "["+stringMessages
                            .distanceInMetersUnit()+"]", new RaceDistanceTraveledInMeters(), 0, getLeaderboardPanel()
                            .getLeaderboardTable(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE));
            result.put(DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS, new FormattedDoubleLegDetailColumn(
                    stringMessages.averageSpeedInKnots(), "["+stringMessages.averageSpeedInKnotsUnit()+"]",
                    new RaceAverageSpeedInKnots(), 2, getLeaderboardPanel().getLeaderboardTable(),
                    LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE));
            result.put(
                    DetailType.RACE_GAP_TO_LEADER_IN_SECONDS,
                    new FormattedDoubleLegDetailColumn(stringMessages.gapToLeaderInSeconds(), "["+stringMessages
                            .gapToLeaderInSecondsUnit()+"]", new RaceGapToLeaderInSeconds(), 0, getLeaderboardPanel()
                            .getLeaderboardTable(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE));
            result.put(
                    DetailType.RACE_DISTANCE_TO_LEADER_IN_METERS,
                    new FormattedDoubleLegDetailColumn(stringMessages.windwardDistanceToOverallLeader(), "["+stringMessages
                            .distanceInMetersUnit()+"]", new RaceDistanceToLeaderInMeters(), 0, getLeaderboardPanel()
                            .getLeaderboardTable(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE));
            result.put(DetailType.NUMBER_OF_MANEUVERS, getManeuverCountRaceColumn());
            result.put(DetailType.CURRENT_LEG, new FormattedDoubleLegDetailColumn(stringMessages.currentLeg(), "",
                    new CurrentLeg(), 0, getLeaderboardPanel().getLeaderboardTable(), LEG_COLUMN_HEADER_STYLE,
                    LEG_COLUMN_STYLE));
            return result;
        }

        private ManeuverCountRaceColumn getManeuverCountRaceColumn() {
            return new ManeuverCountRaceColumn(getLeaderboardPanel(), this, stringMessages,
                    LeaderboardPanel.this.selectedManeuverDetails, LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                    LEG_DETAIL_COLUMN_HEADER_STYLE, LEG_DETAIL_COLUMN_STYLE);
        }

        @Override
        protected Iterable<SortableColumn<LeaderboardRowDTO, ?>> getDirectChildren() {
            List<SortableColumn<LeaderboardRowDTO, ?>> result = new ArrayList<SortableColumn<LeaderboardRowDTO, ?>>();
            for (SortableColumn<LeaderboardRowDTO, ?> column : super.getDirectChildren()) {
                result.add(column);
            }
            if (isExpanded() && selectedRaceDetails.contains(DetailType.DISPLAY_LEGS)) {
                // it is important to re-use existing LegColumn objects because
                // removing the columns from the table
                // is based on column identity
                int legCount = getLeaderboard().getLegCount(getRaceName());
                if (legCount != -1) {
                    for (int i = 0; i < legCount; i++) {
                        LegColumn legColumn = getLegColumn(i);
                        result.add(legColumn);
                    }
                } else {
                    // the race is no longer part of the LeaderboardDTO; consider the non-null legs in legColumns:
                    for (LegColumn legColumn : legColumns) {
                        if (legColumn != null) {
                            result.add(legColumn);
                        }
                    }
                }
            }
            return result;
        }

        private LegColumn getLegColumn(int legNumber) {
            LegColumn result;
            if (legColumns.size() > legNumber && legColumns.get(legNumber) != null) {
                result = legColumns.get(legNumber);
            } else {
                result = new LegColumn(LeaderboardPanel.this, getRaceName(), legNumber, stringMessages,
                        Collections.unmodifiableList(selectedLegDetails), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                        LEG_DETAIL_COLUMN_HEADER_STYLE, LEG_DETAIL_COLUMN_STYLE);
                while (legColumns.size() <= legNumber) {
                    legColumns.add(null);
                }
                legColumns.set(legNumber, result);
            }
            return result;
        }

        /**
         * Accumulates the average speed over all legs of a race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceAverageSpeedInKnots implements LegDetailField<Double> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceName.get(getRaceName());
                if (fieldsForRace != null && fieldsForRace.legDetails != null) {
                    double distanceTraveledInMeters = 0;
                    long timeInMilliseconds = 0;
                    for (LegEntryDTO legDetail : fieldsForRace.legDetails) {
                        if (legDetail != null) {
                            distanceTraveledInMeters += legDetail.distanceTraveledInMeters;
                            timeInMilliseconds += legDetail.timeInMilliseconds;
                        }
                    }
                    if (timeInMilliseconds != 0) {
                        result = distanceTraveledInMeters / (double) timeInMilliseconds * 1000 * 3600 / 1852;
                    }
                }
                return result;
            }
        }

        /**
         * Accumulates the distance traveled over all legs of a race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceDistanceTraveledInMeters implements LegDetailField<Double> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceName.get(getRaceName());
                if (fieldsForRace != null && fieldsForRace.legDetails != null) {
                    for (LegEntryDTO legDetail : fieldsForRace.legDetails) {
                        if (legDetail != null) {
                            if (legDetail.distanceTraveledInMeters != null) {
                                if (result == null) {
                                    result = 0.0;
                                }
                                result += legDetail.distanceTraveledInMeters;
                            }
                        }
                    }
                }
                return result;
            }
        }
        
        private class CurrentLeg implements LegDetailField<Double> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceName.get(getRaceName());
                if (fieldsForRace != null && fieldsForRace.legDetails != null && !fieldsForRace.legDetails.isEmpty()) {
                    for (LegEntryDTO legDetail : fieldsForRace.legDetails) {
                        if (legDetail != null) {
                            if (legDetail.started) {
                                if (result == null) {
                                    result = 0.0;
                                }
                                result++;
                            } else {
                                if (result != null) {
                                    result++;
                                }
                                break;
                            }
                        }
                    }
                }
                return result;
            }
        }

        /**
         * Accumulates the average speed over all legs of a race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceGapToLeaderInSeconds implements LegDetailField<Double> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceName.get(getRaceName());
                if (fieldsForRace != null && fieldsForRace.legDetails != null) {
                    int lastLegIndex = fieldsForRace.legDetails.size() - 1;
                    LegEntryDTO lastLegDetail = fieldsForRace.legDetails.get(lastLegIndex);
                    // competitor may be in leg prior to the one the leader is in; find competitors current leg
                    while (lastLegDetail == null && lastLegIndex > 0) {
                        lastLegDetail = fieldsForRace.legDetails.get(--lastLegIndex);
                    }
                    if (lastLegDetail != null) {
                        result = lastLegDetail.gapToLeaderInSeconds;
                    }
                }
                return result;
            }
        }

        /**
         * Computes the windward distance to the overall leader in meters
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceDistanceToLeaderInMeters implements LegDetailField<Double> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceName.get(getRaceName());
                if (fieldsForRace != null && fieldsForRace.windwardDistanceToOverallLeaderInMeters != null) {
                    result = fieldsForRace.windwardDistanceToOverallLeaderInMeters;
                }
                return result;
            }
        }
    }

    /**
     * Displays the totals for a competitor for the entire leaderboard.
     * 
     * @author Axel Uhl (D043530)
     * 
     */
    private class TotalsColumn extends SortableColumn<LeaderboardRowDTO, String> {
        private final String columnStyle;

        protected TotalsColumn(String columnStyle) {
            super(new TextCell());
            this.columnStyle = columnStyle;
            setHorizontalAlignment(ALIGN_CENTER);
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            int totalPoints = getLeaderboard().getTotalPoints(object);
            return "" + totalPoints;
        }

        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<span style=\"font-weight: bold;\">");
            sb.appendEscaped(getValue(object));
            sb.appendHtmlConstant("</span>");
        }

        @Override
        public Comparator<LeaderboardRowDTO> getComparator() {
            return getLeaderboard().getTotalRankingComparator();
        }

        @Override
        public String getColumnStyle() {
            return columnStyle;
        }

        @Override
        public Header<String> getHeader() {
            return new TextHeader(stringMessages.total());
        }
    }

    protected class CarryColumn extends SortableColumn<LeaderboardRowDTO, String> {
        public CarryColumn() {
            super(new TextCell());
            setSortable(true);
        }

        protected CarryColumn(EditTextCell editTextCell) {
            super(editTextCell);
            setSortable(true);
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            return object.carriedPoints == null ? "" : "" + object.carriedPoints;
        }

        @Override
        public Comparator<LeaderboardRowDTO> getComparator() {
            return new Comparator<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    return (o1.carriedPoints == null ? 0 : o1.carriedPoints)
                            - (o2.carriedPoints == null ? 0 : o2.carriedPoints);
                }
            };
        }

        @Override
        public Header<String> getHeader() {
            return new TextHeader(stringMessages.carry());
        }
    }

    private class RankColumn extends SortableColumn<LeaderboardRowDTO, String> {
        public RankColumn() {
            super(new TextCell());
            setHorizontalAlignment(ALIGN_CENTER);
            setSortable(true);
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            return "" + getLeaderboard().getRank(object.competitor);
        }

        @Override
        public Comparator<LeaderboardRowDTO> getComparator() {
            return new Comparator<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    return getLeaderboard().getRank(o1.competitor) - getLeaderboard().getRank(o2.competitor);
                }
            };
        }

        @Override
        public Header<String> getHeader() {
            return new TextHeader(stringMessages.rank());
        }
    }

    public LeaderboardPanel(SailingServiceAsync sailingService, LeaderboardSettings settings,
            CompetitorSelectionProvider competitorSelectionProvider, String leaderboardName,
            String leaderboardGroupName, ErrorReporter errorReporter, final StringMessages stringMessages,
            final UserAgentTypes userAgentType) {
        this(sailingService, settings, /* preSelectedRace */null, competitorSelectionProvider, leaderboardName,
                leaderboardGroupName, errorReporter, stringMessages, userAgentType);
    }

    public LeaderboardPanel(SailingServiceAsync sailingService, LeaderboardSettings settings, RaceIdentifier preSelectedRace,
            CompetitorSelectionProvider competitorSelectionProvider, String leaderboardName, String leaderboardGroupName,
            ErrorReporter errorReporter, final StringMessages stringMessages, final UserAgentTypes userAgentType) {
        this(sailingService, settings, preSelectedRace, competitorSelectionProvider, new Timer(PlayModes.Replay, /* delayBetweenAutoAdvancesInMilliseconds */3000l),
                leaderboardName, leaderboardGroupName, errorReporter, stringMessages, userAgentType);
    }

    public LeaderboardPanel(SailingServiceAsync sailingService, LeaderboardSettings settings, RaceIdentifier preSelectedRace,
            CompetitorSelectionProvider competitorSelectionProvider, Timer timer, String leaderboardName, String leaderboardGroupName,
            ErrorReporter errorReporter, final StringMessages stringMessages, final UserAgentTypes userAgentType) {
        this.sailingService = sailingService;
        this.preSelectedRace = preSelectedRace;
        this.competitorSelectionProvider = competitorSelectionProvider;
        competitorSelectionProvider.addCompetitorSelectionChangeListener(this);
        this.setLeaderboardName(leaderboardName);
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        this.selectedLegDetails = new ArrayList<DetailType>();
        this.selectedRaceDetails = new ArrayList<DetailType>();
        this.selectedRaceColumns = new ArrayList<RaceInLeaderboardDTO>();
        this.selectedManeuverDetails = new ArrayList<DetailType>();

        selectedLegDetails.addAll(settings.getLegDetailsToShow());
        selectedManeuverDetails.addAll(settings.getManeuverDetailsToShow());
        selectedRaceDetails.addAll(settings.getRaceDetailsToShow());
        setAutoExpandFirstRace(settings.isAutoExpandFirstRace());

        this.timer = timer;
        timer.addPlayStateListener(this);
        timer.addTimeListener(this);
        rankColumn = new RankColumn();
        RACE_COLUMN_HEADER_STYLE = tableResources.cellTableStyle().cellTableRaceColumnHeader();
        LEG_COLUMN_HEADER_STYLE = tableResources.cellTableStyle().cellTableLegColumnHeader();
        LEG_DETAIL_COLUMN_HEADER_STYLE = tableResources.cellTableStyle().cellTableLegDetailColumnHeader();
        RACE_COLUMN_STYLE = tableResources.cellTableStyle().cellTableRaceColumn();
        LEG_COLUMN_STYLE = tableResources.cellTableStyle().cellTableLegColumn();
        LEG_DETAIL_COLUMN_STYLE = tableResources.cellTableStyle().cellTableLegDetailColumn();
        TOTAL_COLUMN_STYLE = tableResources.cellTableStyle().cellTableTotalColumn();
        leaderboardTable = new CellTableWithStylableHeaders<LeaderboardRowDTO>(
        /* pageSize */100, tableResources);
        getLeaderboardTable().setWidth("100%");
        if (userAgentType == UserAgentTypes.MOBILE) {
            leaderboardSelectionModel = new ToggleSelectionModel<LeaderboardRowDTO>();
        } else {
            leaderboardSelectionModel = new MultiSelectionModel<LeaderboardRowDTO>();
        }
        leaderboardSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                List<CompetitorDTO> selection = new ArrayList<CompetitorDTO>();
                for (LeaderboardRowDTO row : getSelectedRows()) {
                    selection.add(row.competitor);
                }
                LeaderboardPanel.this.competitorSelectionProvider.setSelection(selection,
                /* listenersNotToNotify */LeaderboardPanel.this);
            }
        });
        getLeaderboardTable().setSelectionModel(leaderboardSelectionModel);
        setData(new ListDataProvider<LeaderboardRowDTO>());
        getData().addDataDisplay(getLeaderboardTable());
        listHandler = new ListHandler<LeaderboardRowDTO>(getData().getList());
        getLeaderboardTable().addColumnSortHandler(listHandler);
        loadCompleteLeaderboard(getLeaderboardDisplayDate());

        if(preSelectedRace == null) {
            isEmbedded = false;
        } else {
            isEmbedded = true;
        }
        contentPanel = new VerticalPanel();
        headerPanel = new DockPanel();
        DockPanel toolbarPanel = new DockPanel();
        busyIndicator = new SimpleBusyIndicator(false, 0.8f);
        if (!isEmbedded) {
            toolbarPanel.add(busyIndicator, DockPanel.WEST);
        }
        headerPanel.setWidth("100%");
        headerPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        toolbarPanel.setWidth("100%");
        toolbarPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        Label leaderboardLabel = new Label(stringMessages.leaderboard() + " " + leaderboardName.toUpperCase());
        leaderboardLabel.addStyleName("leaderboardLabel boldLabel");
        headerPanel.add(leaderboardLabel, DockPanel.WEST);
        ClickHandler playPauseHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (LeaderboardPanel.this.timer.getPlayState() == PlayStates.Playing) {
                    LeaderboardPanel.this.timer.pause();
                } else {
                    LeaderboardPanel.this.timer.play();
                }
            }
        };
        ImageResource chartIcon = resources.chartIcon();
        ImageResource settingsIcon = resources.settingsIcon();
        pauseIcon = resources.pauseIcon();
        playIcon = resources.playIcon();
        refreshAndSettingsPanel = new HorizontalPanel();
        refreshAndSettingsPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        HorizontalPanel refreshPanel = new HorizontalPanel();
        refreshPanel.setSpacing(5);
        refreshPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        refreshPanel.addStyleName("refreshPanel");
        headerPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        toolbarPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        toolbarPanel.addStyleName("refreshAndSettings");
        playPause = new Anchor(getPlayPauseImgHtml(timer.getPlayState()));
        playPause.addClickHandler(playPauseHandler);
        playStateChanged(timer.getPlayState(), timer.getPlayMode());
        refreshPanel.add(playPause);
        Anchor chartsAnchor = new Anchor(AbstractImagePrototype.create(chartIcon).getSafeHtml());
        chartsAnchor.setTitle(stringMessages.showCharts());
        chartsAnchor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                compareCompetitors();
            }
        });
        Anchor settingsAnchor = new Anchor(AbstractImagePrototype.create(settingsIcon).getSafeHtml());
        settingsAnchor.setTitle(stringMessages.settings());
        settingsAnchor.addClickHandler(new SettingsClickHandler(stringMessages));
        refreshAndSettingsPanel.add(chartsAnchor);
        refreshAndSettingsPanel.add(refreshPanel);
        refreshAndSettingsPanel.add(settingsAnchor);
        toolbarPanel.add(refreshAndSettingsPanel, DockPanel.EAST);
        if(!isEmbedded) {
            contentPanel.add(headerPanel);
            contentPanel.add(toolbarPanel);
        }
        contentPanel.add(getLeaderboardTable());
        setWidget(contentPanel);
        if (settings.getNameOfRaceToSort() != null) {
            RaceColumn<?> column = getRaceColumnByRaceName(settings.getNameOfRaceToSort());
            sort(column, settings.isSortAscending());
        }
    }

    private RaceColumn<?> getRaceColumnByRaceName(String raceName) {
        for (int i=0; i<getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> column = getLeaderboardTable().getColumn(i);
            if (column instanceof RaceColumn<?> && ((RaceColumn<?>) column).getRaceName().equals(raceName)) {
                return (RaceColumn<?>) column;
            }
        }
        return null;
    }

    private SafeHtml getPlayPauseImgHtml(PlayStates playState) {
        if (playState == PlayStates.Playing)
            return AbstractImagePrototype.create(pauseIcon).getSafeHtml();
        else
            return AbstractImagePrototype.create(playIcon).getSafeHtml();
    }

    private void setDelayInMilliseconds(long delayInMilliseconds) {
        timer.setDelay(delayInMilliseconds);
    }
    
    public boolean isAutoExpandFirstRace() {
        return autoExpandFirstRace;
    }
    
    private void setAutoExpandFirstRace(boolean autoExpandFirstRace) {
        this.autoExpandFirstRace = autoExpandFirstRace;
    }

    /**
     * The time point for which the leaderboard currently shows results
     */
    protected Date getLeaderboardDisplayDate() {
        return timer.getTime();
    }

    /**
     * adds the <code>column</code> to the right end of the {@link #getLeaderboardTable() leaderboard table} and sets
     * the column style according to the {@link SortableColumn#getColumnStyle() column's style definition}.
     */
    protected void addColumn(SortableColumn<LeaderboardRowDTO, ?> column) {
        getLeaderboardTable().addColumn(column, column.getHeader());
        listHandler.setComparator(column, column.getComparator());
        String columnStyle = column.getColumnStyle();
        if (columnStyle != null) {
            getLeaderboardTable().addColumnStyleName(getLeaderboardTable().getColumnCount() - 1, columnStyle);
        }
    }

    protected void insertColumn(int beforeIndex, SortableColumn<LeaderboardRowDTO, ?> column) {
        // remove column styles of those columns whose index will shift right by
        // one:
        removeColumnStyles(beforeIndex);
        getLeaderboardTable().insertColumn(beforeIndex, column, column.getHeader());
        addColumnStyles(beforeIndex);
        listHandler.setComparator(column, column.getComparator());
    }

    private void addColumnStyles(int startColumn) {
        for (int i = startColumn; i < getLeaderboardTable().getColumnCount(); i++) {
            SortableColumn<LeaderboardRowDTO, ?> columnToRemoveStyleFor = (SortableColumn<LeaderboardRowDTO, ?>) getLeaderboardTable()
                    .getColumn(i);
            String columnStyle = columnToRemoveStyleFor.getColumnStyle();
            if (columnStyle != null) {
                getLeaderboardTable().addColumnStyleName(i, columnStyle);
            }
        }
    }

    private void removeColumnStyles(int startColumn) {
        for (int i = startColumn; i < getLeaderboardTable().getColumnCount(); i++) {
            SortableColumn<LeaderboardRowDTO, ?> columnToRemoveStyleFor = (SortableColumn<LeaderboardRowDTO, ?>) getLeaderboardTable()
                    .getColumn(i);
            String columnStyle = columnToRemoveStyleFor.getColumnStyle();
            if (columnStyle != null) {
                getLeaderboardTable().removeColumnStyleName(i, columnStyle);
            }
        }
    }

    /**
     * removes the column specified by <code>columnIndex</code> from the {@link #getLeaderboardTable() leaderboard
     * table} and fixes the column styles again (see {@link #addColumnStyles(int)}).
     */
    protected void removeColumn(int columnIndex) {
        Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(columnIndex);
        if (c instanceof ExpandableSortableColumn<?>) {
            ExpandableSortableColumn<?> expandableColumn = (ExpandableSortableColumn<?>) c;
            if (expandableColumn.isExpanded()) {
                // remove expanded child columns from the leaderboard...
                expandableColumn.toggleExpansion();
                // them remember that column c was expanded:
                expandableColumn.setExpanded(true);
            }
        }
        removeColumnStyles(/* startColumn */columnIndex);
        getLeaderboardTable().removeColumn(columnIndex);
        addColumnStyles(/* startColumn */columnIndex);
    }

    protected void removeColumn(Column<LeaderboardRowDTO, ?> c) {
        int columnIndex = getLeaderboardTable().getColumnIndex(c);
        if (columnIndex != -1) {
            removeColumn(columnIndex);
        }
    }

    private void loadCompleteLeaderboard(Date date) {
        getSailingService().getLeaderboardByName(getLeaderboardName(), date,
        /* namesOfRacesForWhichToLoadLegDetails */getNamesOfExpandedRaces(), new AsyncCallback<LeaderboardDTO>() {
            @Override
            public void onSuccess(LeaderboardDTO result) {
                updateLeaderboard(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                getErrorReporter().reportError("Error trying to obtain leaderboard contents: " + caught.getMessage());
            }
        });
    }

    /**
     * Determine from column expansion state which races need their leg details
     */
    private Collection<String> getNamesOfExpandedRaces() {
        Collection<String> namesOfExpandedRaces = new ArrayList<String>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> column = getLeaderboardTable().getColumn(i);
            if (column instanceof RaceColumn<?>) {
                RaceColumn<?> raceColumn = (RaceColumn<?>) column;
                if (raceColumn.isExpanded()) {
                    namesOfExpandedRaces.add(raceColumn.getRaceName());
                }
            }
        }
        return namesOfExpandedRaces;
    }

    /**
     * Also updates the min/max values on the columns
     */
    protected void updateLeaderboard(LeaderboardDTO leaderboard) {
        if (leaderboard != null) {
            competitorSelectionProvider.setCompetitors(leaderboard.competitors);
            selectedRaceColumns.addAll(getRaceColumnsToAddImplicitly(leaderboard));
            setLeaderboard(leaderboard);
            adjustColumnLayout(leaderboard);
            getData().getList().clear();
            if (leaderboard != null) {
                boolean firstRace = true;
                getData().getList().addAll(leaderboard.rows.values());
                for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
                    SortableColumn<?, ?> c = (SortableColumn<?, ?>) getLeaderboardTable().getColumn(i);
                    c.updateMinMax(leaderboard);
                    // Toggle the first race, if the setting is set and it isn't open yet
                    if (firstRace && isAutoExpandFirstRace() && c instanceof ExpandableSortableColumn<?>) {
                        ExpandableSortableColumn<?> expandableSortableColumn = (ExpandableSortableColumn<?>) c;
                        if (!expandableSortableColumn.isExpanded()) {
                            expandableSortableColumn.toggleExpansion();
                        }
                        firstRace = false;
                    }
                }
                Comparator<LeaderboardRowDTO> comparator = getComparatorForSelectedSorting();
                if (comparator != null) {
                    Collections.sort(getData().getList(), comparator);
                } else {
                    RankColumn columnToSortFor = getRankColumn();
                    // if no sorting was selected, sort by ascending rank and mark
                    // table header so
                    sort(columnToSortFor, true);
                }
                // Reselect the selected rows
                clearSelection();
                for (LeaderboardRowDTO row : data.getList()) {
                    if (competitorSelectionProvider.isSelected(row.competitor)) {
                        leaderboardSelectionModel.setSelected(row, true);
                    }
                }
            }
        }
    }

    /**
     * Sorts the leaderboard contents in ascending order according to <code>columnToSortFor</code>'s comparator and
     * marks the table's column sort list so.
     */
    private void sort(SortableColumn<LeaderboardRowDTO, ?> columnToSortFor, boolean ascending) {
        Collections.sort(getData().getList(), getComparator(columnToSortFor, ascending));
        ColumnSortInfo columnSortInfo = getLeaderboardTable().getColumnSortList().push(columnToSortFor);
        if (ascending != columnSortInfo.isAscending()) {
            // flip ascending bit by repeating the push:
            getLeaderboardTable().getColumnSortList().push(columnToSortFor);
        }
    }

    private void clearSelection() {
        for (LeaderboardRowDTO row : getData().getList()) {
            leaderboardSelectionModel.setSelected(row, false);
        }
    }

    /**
     * Considers the {@link #preSelectedRace} field as follows: if <code>null</code>, all race columns that <code>leaderboard</code>
     * adds on top of the existing {@link #getLeaderboard() leaderboard} are returned. Otherwise, the column list obtained as described before
     * is filtered such that only columns pass whose race identifier equals {@link #preSelectedRace}.
     */
    private List<RaceInLeaderboardDTO> getRaceColumnsToAddImplicitly(LeaderboardDTO leaderboard) {
        List<RaceInLeaderboardDTO> columnsToAddImplicitly = getRacesAddedNew(getLeaderboard(), leaderboard);
        if (preSelectedRace != null) {
            for (Iterator<RaceInLeaderboardDTO> i=columnsToAddImplicitly.iterator(); i.hasNext(); ) {
                RaceInLeaderboardDTO next = i.next();
                if (!preSelectedRace.equals(next.getRaceIdentifier())) {
                    i.remove();
                }
            }
        }
        return columnsToAddImplicitly;
    }

    private Comparator<LeaderboardRowDTO> getComparatorForSelectedSorting() {
        Comparator<LeaderboardRowDTO> result = null;
        if (getLeaderboardTable().getColumnSortList().size() > 0) {
            ColumnSortInfo columnSortInfo = getLeaderboardTable().getColumnSortList().get(0);
            @SuppressWarnings("unchecked")
            SortableColumn<LeaderboardRowDTO, ?> castResult = (SortableColumn<LeaderboardRowDTO, ?>) columnSortInfo
                    .getColumn();
            final boolean ascending = columnSortInfo.isAscending();
            result = getComparator(castResult, ascending);
        }
        return result;
    }

    private Comparator<LeaderboardRowDTO> getComparator(SortableColumn<LeaderboardRowDTO, ?> column,
            final boolean ascending) {
        Comparator<LeaderboardRowDTO> result;
        if (ascending) {
            result = column.getComparator();
        } else {
            result = Collections.reverseOrder(column.getComparator());
        }
        return result;
    }

    private RankColumn getRankColumn() {
        return rankColumn;
    }

    private void setLeaderboard(LeaderboardDTO leaderboard) {
        this.leaderboard = leaderboard;
    }

    public LeaderboardDTO getLeaderboard() {
        return leaderboard;
    }

    private void adjustColumnLayout(LeaderboardDTO leaderboard) {
        ensureRankColumn();
        ensureSailIDAndCompetitorColumn();
        updateCarryColumn(leaderboard);
        // first remove race columns no longer needed:
        removeUnusedRaceColumns(leaderboard);
        if (leaderboard != null) {
            createMissingAndAdjustExistingRaceColumns(leaderboard);
            ensureTotalsColumn();
        }
    }

    private List<RaceInLeaderboardDTO> getRacesAddedNew(LeaderboardDTO oldLeaderboard, LeaderboardDTO newLeaderboard) {
        List<RaceInLeaderboardDTO> result = new ArrayList<RaceInLeaderboardDTO>();
        for (RaceInLeaderboardDTO s : newLeaderboard.getRaceList()) {
            if (oldLeaderboard == null || !leaderboardContainsColumnNamed(oldLeaderboard, s.getRaceColumnName())) {
                result.add(s);
            }
        }
        return result;
    }

    private boolean leaderboardContainsColumnNamed(LeaderboardDTO leaderboard, String raceColumnName) {
        for (RaceInLeaderboardDTO column : leaderboard.getRaceList()) {
            if (column.getRaceColumnName().equals(raceColumnName)) {
                return true;
            }
        }
        return false;
    }

    private boolean leaderboardTableContainsRace(RaceInLeaderboardDTO race) {
        for (int leaderboardposition = 0; leaderboardposition < getLeaderboardTable().getColumnCount(); leaderboardposition++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(leaderboardposition);
            if (c instanceof RaceColumn) {
                RaceColumn<?> raceColumn = (RaceColumn<?>) c;
                if (raceColumn.getRaceName().equals(race.getRaceColumnName())) {
                    correctColumnData(raceColumn);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Corrects the data linke medalRace of the given raceColumn
     * 
     * @param raceColumn
     *            the raceColumn to correct.
     */
    private void correctColumnData(RaceColumn<?> raceColumn) {
        RaceInLeaderboardDTO race = raceColumn.getRace();
        int columnIndex = getRaceColumnPosition(raceColumn);
        if (raceColumn.isExpansionEnabled() != race.isTrackedRace()
                || race.isMedalRace() != raceColumn.isMedalRace()) {
            if (raceColumn.isExpanded()) {
                raceColumn.toggleExpansion(); // remove children from table
            }
            removeColumn(columnIndex);
            insertColumn(columnIndex, createRaceColumn(race));
        }
    }

    /**
     * Removes all RaceColumns, starting at count {@link raceColumnStartIndex raceColumnStartIndex}
     * 
     * @param raceColumnStartIndex
     *            The index of the race column should be deleted from.
     * @param raceName
     *            The name of the racing column until the table should be cleared.
     */
    private void removeRaceColumnFromRaceColumnStartIndexBeforeRace(int raceColumnStartIndex, RaceInLeaderboardDTO race) {
        int counter = 0;
        for (int leaderboardposition = 0; leaderboardposition < getLeaderboardTable().getColumnCount(); leaderboardposition++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(leaderboardposition);
            if (c instanceof RaceColumn) {
                RaceColumn<?> raceColumn = (RaceColumn<?>) c;
                if (!raceColumn.getRaceName().equals(race.getRaceColumnName()) && raceColumnStartIndex == counter) {
                    removeColumn(raceColumn);
                } else {
                    return;
                }
                counter++;
            }
        }
    }

    /**
     * Gets a ColumnPosition of a raceColumn
     * 
     * @param raceColumn
     *            The column for which the position is to be found in the leaderboard table
     * @return the position. Returns -1 if raceColumn not existing in leaderboardTable.
     */
    private int getRaceColumnPosition(RaceColumn<?> raceColumn) {
        for (int leaderboardposition = 0; leaderboardposition < getLeaderboardTable().getColumnCount(); leaderboardposition++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(leaderboardposition);
            if (c instanceof RaceColumn) {
                RaceColumn<?> rc = (RaceColumn<?>) c;
                if (rc.equals(raceColumn)) {
                    return leaderboardposition;
                }
            }
        }
        return -1;
    }

    /**
     * This method returns the position where a racecolumn should get inserted.
     * 
     * @param raceName
     *            the name of the race to insert
     * @param listpos
     *            the position of the race in the {@link selectedRaceColumns selectedRaceColumns}
     * @return the position of a race column right before which to insert the race column so that it is the
     *         <code>listpos</code>th race in the table or -1 if no such race column was found, e.g., in case
     *         the column needs to be inserted as the last race in the table
     */
    private int getColumnPositionToInsert(RaceInLeaderboardDTO race, int listpos) {
        int raceColumnCounter = 0;
        int noRaceColumnCounter = 0;
        boolean raceColumnFound = false;
        for (int leaderboardPosition = 0; !raceColumnFound
                && leaderboardPosition < getLeaderboardTable().getColumnCount(); leaderboardPosition++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(leaderboardPosition);
            if (c instanceof RaceColumn) {
                // RaceColumn<?> raceColumn = (RaceColumn<?>) c;
                if (raceColumnCounter == listpos) {
                    raceColumnFound = true;
                } else {
                    raceColumnCounter++;
                }
            } else {
                noRaceColumnCounter++;
            }
        }
        if (raceColumnFound) {
            return raceColumnCounter + noRaceColumnCounter;
        } else {
            return -1;
        }
    }

    /**
     * Removes all Columns of type racecolumns of leaderboardTable
     */
    private void removeRaceColumnsNotSelected(List<RaceInLeaderboardDTO> selectedRaceColumns) {
        Set<String> selectedRaceColumnNames = new HashSet<String>();
        for (RaceInLeaderboardDTO selectedRaceColumn : selectedRaceColumns) {
            selectedRaceColumnNames.add(selectedRaceColumn.getRaceColumnName());
        }
        List<Column<LeaderboardRowDTO, ?>> columnsToRemove = new ArrayList<Column<LeaderboardRowDTO, ?>>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(i);
            if (c instanceof RaceColumn
                    && (leaderboard == null || !selectedRaceColumnNames.contains(((RaceColumn<?>) c).getRaceName()))) {
                columnsToRemove.add(c);
            }
        }
        for (Column<LeaderboardRowDTO, ?> c : columnsToRemove) {
            removeColumn(c);
        }
    }

    /**
     * Existing and matching race columns may still need to be removed, re-created and inserted because the "tracked"
     * property may have changed, changing the columns expandability.
     */
    private void createMissingAndAdjustExistingRaceColumns(LeaderboardDTO leaderboard) {
        // Correct order of races in selectedRaceColum
        List<RaceInLeaderboardDTO> correctedOrderSelectedRaces = new ArrayList<RaceInLeaderboardDTO>();
        for (RaceInLeaderboardDTO raceInLeaderboard : leaderboard.getRaceList()) {
            if (selectedRaceColumns.contains(raceInLeaderboard)) {
                correctedOrderSelectedRaces.add(raceInLeaderboard);
            }
        }
        selectedRaceColumns = correctedOrderSelectedRaces;
        removeRaceColumnsNotSelected(selectedRaceColumns);
        for (int selectedRaceCount = 0; selectedRaceCount < selectedRaceColumns.size(); selectedRaceCount++) {
            RaceInLeaderboardDTO selectedRace = selectedRaceColumns.get(selectedRaceCount);
            if (leaderboardTableContainsRace(selectedRace)) {
                // remove all raceColumns, starting at a specific selectedRaceCount, up to but excluding the selected raceName
                // with the result that selectedRace is at position selectedRaceCount afterwards
                removeRaceColumnFromRaceColumnStartIndexBeforeRace(selectedRaceCount, selectedRace);
            } else {
                // get correct position to insert the column
                int positionToInsert = getColumnPositionToInsert(selectedRace, selectedRaceCount);
                if (positionToInsert != -1) {
                    insertColumn(positionToInsert, createRaceColumn(selectedRace));
                } else {
                    // Add the raceColumn with addRaceColumn, if no RaceColumn is existing in leaderboard
                    addRaceColumn(createRaceColumn(selectedRace));
                }
            }
        }
    }

    protected RaceColumn<?> createRaceColumn(RaceInLeaderboardDTO raceInLeaderboard) {
        TextRaceColumn textRaceColumn = new TextRaceColumn(raceInLeaderboard, raceInLeaderboard.isTrackedRace(), RACE_COLUMN_HEADER_STYLE,
                RACE_COLUMN_STYLE);
        return textRaceColumn;
    }

    private void removeUnusedRaceColumns(LeaderboardDTO leaderboard) {
        List<Column<LeaderboardRowDTO, ?>> columnsToRemove = new ArrayList<Column<LeaderboardRowDTO, ?>>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(i);
            if (c instanceof RaceColumn
                    && (leaderboard == null || !leaderboard.raceListContains(((RaceColumn<?>) c).getRaceName()))) {
                columnsToRemove.add(c);
            }
        }
        // Tricky issue: if the race column is currently expanded, we can't know anymore how many detail columns
        // there are because the updated LeaderboardDTO object doesn't contain the race anymore. We have to
        // collapse and remove all LegColumns following the RaceColumn
        for (Column<LeaderboardRowDTO, ?> c : columnsToRemove) {
            removeColumn(c);
        }
    }

    /**
     * If the last column is the totals column, remove it. Add the race column as the last column.
     */
    private void addRaceColumn(RaceColumn<?> raceColumn) {
        if (getLeaderboardTable().getColumn(getLeaderboardTable().getColumnCount() - 1) instanceof TotalsColumn) {
            removeColumn(getLeaderboardTable().getColumnCount() - 1);
        }
        addColumn(raceColumn);
    }

    private void ensureRankColumn() {
        if (getLeaderboardTable().getColumnCount() == RANK_COLUMN_INDEX) {
            addColumn(getRankColumn());
        } else {
            if (!(getLeaderboardTable().getColumn(RANK_COLUMN_INDEX) instanceof RankColumn)) {
                throw new RuntimeException("The first column must always be the rank column but it was of type "
                        + getLeaderboardTable().getColumn(RANK_COLUMN_INDEX).getClass().getName());
            }
        }
    }

    private void ensureSailIDAndCompetitorColumn() {
        if (getLeaderboardTable().getColumnCount() <= SAIL_ID_COLUMN_INDEX) {
            addColumn(new SailIDColumn());
            addColumn(createCompetitorColumn());
        } else {
            if (!(getLeaderboardTable().getColumn(SAIL_ID_COLUMN_INDEX) instanceof SailIDColumn)) {
                throw new RuntimeException("The second column must always be the sail ID column but it was of type "
                        + getLeaderboardTable().getColumn(SAIL_ID_COLUMN_INDEX).getClass().getName());
            }
        }
    }

    protected CompetitorColumn createCompetitorColumn() {
        return new CompetitorColumn();
    }

    private void ensureTotalsColumn() {
        // add a totals column on the right
        if (getLeaderboardTable().getColumnCount() == 0
                || !(getLeaderboardTable().getColumn(getLeaderboardTable().getColumnCount() - 1) instanceof TotalsColumn)) {
            addColumn(new TotalsColumn(TOTAL_COLUMN_STYLE));
        }
    }

    /**
     * If the <code>leaderboard</code> {@link LeaderboardDTO#hasCarriedPoints has carried points} and if column #1
     * (second column, right of the competitor column) does not exist or is not of type {@link CarryColumn}, all columns
     * starting from #1 will be removed and a {@link CarryColumn} will be added. If the leaderboard has no carried
     * points but the display still shows a carry column, the column is removed.
     */
    protected void updateCarryColumn(LeaderboardDTO leaderboard) {
        if (leaderboard != null && leaderboard.hasCarriedPoints) {
            ensureCarryColumn();
        } else {
            ensureNoCarryColumn();
        }
    }

    private void ensureNoCarryColumn() {
        if (getLeaderboardTable().getColumnCount() > CARRY_COLUMN_INDEX
                && getLeaderboardTable().getColumn(CARRY_COLUMN_INDEX) instanceof CarryColumn) {
            removeColumn(CARRY_COLUMN_INDEX);
        }
    }

    protected void ensureCarryColumn() {
        if (getLeaderboardTable().getColumnCount() <= CARRY_COLUMN_INDEX
                || !(getLeaderboardTable().getColumn(CARRY_COLUMN_INDEX) instanceof CarryColumn)) {
            while (getLeaderboardTable().getColumnCount() > CARRY_COLUMN_INDEX) {
                removeColumn(CARRY_COLUMN_INDEX);
            }
            addColumn(createCarryColumn());
        }
    }

    protected CarryColumn createCarryColumn() {
        return new CarryColumn();
    }

    protected CellTable<LeaderboardRowDTO> getLeaderboardTable() {
        return leaderboardTable;
    }

    protected SailingServiceAsync getSailingService() {
        return sailingService;
    }

    protected String getLeaderboardName() {
        return leaderboardName;
    }

    protected void setLeaderboardName(String leaderboardName) {
        this.leaderboardName = leaderboardName;
    }

    protected ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    protected ListDataProvider<LeaderboardRowDTO> getData() {
        return data;
    }

    private void setData(ListDataProvider<LeaderboardRowDTO> data) {
        this.data = data;
    }

    @Override
    public void timeChanged(Date date) {
        loadCompleteLeaderboard(getLeaderboardDisplayDate());
    }

    @Override
    public void playStateChanged(PlayStates playState, PlayModes playMode) {
        currentlyHandlingPlayStateChange = true;
        playPause.setHTML(getPlayPauseImgHtml(playState));
        playPause.setTitle(playState == PlayStates.Playing ? stringMessages.pauseAutomaticRefresh() : stringMessages.autoRefresh());
        if (!settingsUpdatedExplicitly && playMode != oldPlayMode) {
            // if settings weren't explicitly modified, auto-switch to live mode settings and sort for
            // any pre-selected race
            updateSettings(LeaderboardSettingsFactory.getInstance().createNewSettingsForPlayMode(playMode,
                    /* don't touch columnToSort */ preSelectedRace == null ? null : preSelectedRace.getRaceName()));
        }
        currentlyHandlingPlayStateChange = false;
        oldPlayMode = playMode;
    }
    
    private void compareCompetitors() {
        List<RaceIdentifier> races = getTrackedRacesIdentifiers();
        CompareCompetitorsChartDialog chartDialog = new CompareCompetitorsChartDialog(sailingService, races,
                competitorSelectionProvider, timer, stringMessages, errorReporter);
        chartDialog.show();
    }
    
    private List<RaceIdentifier> getTrackedRacesIdentifiers() {
        List<RaceIdentifier> result = new ArrayList<RaceIdentifier>();
        for (RaceInLeaderboardDTO raceColumn : getLeaderboard().getRaceList()) {
            if (raceColumn.getRaceIdentifier() != null) {
                result.add(raceColumn.getRaceIdentifier());
            }
        }
        return result;
    }
    
    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public boolean hasToolbar() {
        return false;
    }

    @Override
    public SettingsDialogComponent<LeaderboardSettings> getSettingsDialogComponent() {
        return new LeaderboardSettingsDialogComponent(Collections.unmodifiableList(selectedManeuverDetails),
                Collections.unmodifiableList(selectedLegDetails),
                Collections.unmodifiableList(selectedRaceDetails), /*  All races to select */
                leaderboard.getRaceList(), selectedRaceColumns,
                autoExpandFirstRace, timer.getRefreshInterval(), timer.getLivePlayDelayInMillis(), stringMessages);
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.leaderboard();
    }

    private LeaderboardRowDTO getRow(CompetitorDTO competitor) {
        for (LeaderboardRowDTO row : getData().getList()) {
            if (row.competitor.equals(competitor)) {
                return row;
            }
        }
        return null;
    }

    @Override
    public void addedToSelection(CompetitorDTO competitor) {
        LeaderboardRowDTO row = getRow(competitor);
        if (row != null) {
            leaderboardSelectionModel.setSelected(row, true);
        }
    }

    @Override
    public void removedFromSelection(CompetitorDTO competitor) {
        LeaderboardRowDTO row = getRow(competitor);
        if (row != null) {
            leaderboardSelectionModel.setSelected(row, false);
        }
    }

    @Override
    public BusyIndicator getBusyIndicator() {
        return busyIndicator;
    }

    @Override
    public boolean hasBusyIndicator() {
        return true;
    }

    private Iterable<LeaderboardRowDTO> getSelectedRows() {
        ArrayList<LeaderboardRowDTO> selectedRows = new ArrayList<LeaderboardRowDTO>();
        for (LeaderboardRowDTO row : getData().getList()) {
            if (leaderboardSelectionModel.isSelected(row)) {
                selectedRows.add(row);
            }
        }
        return selectedRows;
    }
}
