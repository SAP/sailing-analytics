package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gwt.cell.client.AbstractSafeHtmlCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardPanelLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.RaceColumnSelectionStrategies;
import com.sap.sailing.gwt.ui.client.Collator;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.filter.FilterWithUI;
import com.sap.sailing.gwt.ui.client.shared.filter.LeaderboardFetcher;
import com.sap.sailing.gwt.ui.leaderboard.DetailTypeColumn.DataExtractor;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.CountryCodeFactory;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.InvertibleComparator;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.impl.InvertibleComparatorAdapter;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncAction;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.celltable.AbstractSortableColumnWithMinMax;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableSortedCellTableWithStylableHeaders;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.celltable.SortedCellTable;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.BusyStateChangeListener;
import com.sap.sse.gwt.client.controls.busyindicator.BusyStateProvider;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.panels.OverlayAssistantScrollPanel;
import com.sap.sse.gwt.client.player.PlayStateListener;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.ComponentResources;
import com.sap.sse.gwt.client.shared.components.IsEmbeddableComponent;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.WithSecurity;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

/**
 * A leaderboard essentially consists of a table widget that in its columns displays the entries.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public abstract class LeaderboardPanel<LS extends LeaderboardSettings> extends AbstractCompositeComponent<LS>
        implements TimeListener, PlayStateListener, DisplayedLeaderboardRowsProvider, IsEmbeddableComponent,
        CompetitorSelectionChangeListener, LeaderboardFetcher, BusyStateProvider, LeaderboardUpdateProvider {

    public static final String LOAD_LEADERBOARD_DATA_CATEGORY = "loadLeaderboardData";

    protected static final NumberFormat scoreFormat = NumberFormat.getFormat("0.##");

    private final SailingServiceAsync sailingService;

    private static String IS_LIVE_TEXT_COLOR = "#FF0000";
    private static String DEFAULT_TEXT_COLOR = "#000000";

    private static final String STYLE_LEADERBOARD_CONTENT = "leaderboardContent";
    private static final String STYLE_LEADERBOARD_INFO = "leaderboardInfo";
    private static final String STYLE_LEADERBOARD_TOOLBAR = "leaderboardContent-toolbar";
    private static final String STYLE_LEADERBOARD_LIVE_RACE = "leaderboardContent-liverace";
    private static RaceColumnTemplate raceColumnTemplate = new RaceColumnTemplate();

    static class RaceColumnTemplate {
        private MyTemplate template = GWT.create(MyTemplate.class);

        interface MyTemplate extends SafeHtmlTemplates {
            @SafeHtmlTemplates.Template("<div style='{0}'>")
            SafeHtml styledDiv(SafeStyles style);
        }

        /**
         * Originally in the safehtml template: color:{0}; border-bottom: 3px solid {1};
         * 
         * @param textColor
         * @param borderColor
         * @return
         */
        public SafeHtml cellFrameWithTextColorAndFleetBorder(String textColor, String borderColor) {
            SafeStylesBuilder sb = new SafeStylesBuilder();
            sb.trustedColor(textColor);
            sb.trustedNameAndValue("border-bottom", "3px solid " + borderColor);
            return template.styledDiv(sb.toSafeStyles());
        }

        /**
         *
         * Originally in the safehtml template: color:{0};
         * 
         * @param textColor
         * @return
         */
        public SafeHtml cellFrameWithTextColor(String textColor) {
            SafeStylesBuilder sb = new SafeStylesBuilder();
            sb.trustedColor(textColor);
            return template.styledDiv(sb.toSafeStyles());
        }
    }

    public interface LeaderBoardStyle {
        void renderNationalityFlag(ImageResource nationalityFlagImageResource, CountryCode countryCode, SafeHtmlBuilder sb);

        void renderFlagImage(String flagImageURL, SafeHtmlBuilder sb, CompetitorDTO competitor);

        public LeaderboardResources getResources();

        public ComponentResources getComponentresources();

        public LeaderboardTableResources getTableresources();

        public void processStyleForTotalNetPointsColumn(String textColor, SafeStylesBuilder ssb);

        void processStyleForRaceColumnWithoutReasonForMaxPoints(boolean isDiscarded, SafeStylesBuilder ssb);

        String determineBoatColorDivStyle(String competitorColor);

        void afterConstructorHook(LeaderboardPanel<?> leaderboardPanel);

        boolean hasRaceColumns();

        void hookLeaderBoardAttachment(FlowPanel contentPanel,
                FlushableSortedCellTableWithStylableHeaders<LeaderboardRowDTO> leaderboardTable);
    }

    /**
     * The leaderboard name is used to
     * {@link SailingServiceAsync#getLeaderboardByName(String, java.util.Date, String[], boolean, String, com.google.gwt.user.client.rpc.AsyncCallback)
     * obtain the leaderboard contents} from the server. It may change in case the leaderboard is renamed.
     */

    protected LS currentSettings;

    private String leaderboardName;

    private final ErrorReporter errorReporter;

    public final StringMessages stringMessages;

    private final FlushableSortedCellTableWithStylableHeaders<LeaderboardRowDTO> leaderboardTable;

    private final MultiSelectionModel<LeaderboardRowDTO> leaderboardSelectionModel;

    protected LeaderboardDTO leaderboard;

    private final TotalRankColumn totalRankColumn;
    
    protected Iterable<DetailType> availableDetailTypes;

    private final SelectionCheckboxColumn<LeaderboardRowDTO> selectionCheckboxColumn;

    /**
     * Passed to the {@link ManeuverCountRaceColumn}. Modifications to this list will modify the column's children list
     * when updated the next time.
     */
    protected final List<DetailType> selectedManeuverDetails = new ArrayList<DetailType>();;

    /**
     * Passed to the {@link LegColumn}. Modifications to this list will modify the column's children list when updated
     * the next time.
     */
    protected final List<DetailType> selectedLegDetails = new ArrayList<DetailType>();

    /**
     * Passed to the {@link TextRaceColumn}. Modifications to this list will modify the column's children list when
     * updated the next time.
     */
    protected final List<DetailType> selectedRaceDetails = new ArrayList<DetailType>();

    protected final List<DetailType> selectedOverallDetailColumns = new ArrayList<DetailType>();;

    private final Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> overallDetailColumnMap;

    protected RaceColumnSelection raceColumnSelection;
    protected final String RACE_COLUMN_HEADER_STYLE;

    protected final String LEG_DETAIL_COLUMN_HEADER_STYLE;

    protected final String LEG_DETAIL_COLUMN_STYLE;

    protected final String LEG_COLUMN_HEADER_STYLE;

    protected final String RACE_COLUMN_STYLE;

    protected final String LEG_COLUMN_STYLE;

    protected final String TOTAL_COLUMN_STYLE;

    protected final Timer timer;

    /**
     * A {@link LeaderboardDTO} tells something about the live delay through its
     * {@link LeaderboardDTO#getDelayToLiveInMillisForLatestRace()} method. If this flag is <code>true</code>, the live
     * delay in the {@link #timer} will be adjusted each time the {@link #updateLeaderboard(LeaderboardDTO)} method is
     * invoked. Otherwise, the timer's delay will be left alone which is helpful, e.g., if the leaderboard panel is
     * embedded in a race board panel that focuses on one particular race which may not be the same as the one
     * controlling the leaderboard's overall live delay.
     */
    private final boolean adjustTimerDelay;

    private boolean autoExpandLastRaceColumn;

    /**
     * When <code>true</code>, the race columns don't display the competitors' scores in the race represented by the
     * column but the cumulative score up to that race.
     */
    private boolean showAddedScores;

    private boolean showCompetitorShortName;
    private boolean showCompetitorFullName;
    private boolean showCompetitorBoatInfo;

    /**
     * Remembers whether the auto-expand of the pre-selected race (see {@link #autoExpandPreSelectedRace}) or last
     * selected race {@link #autoExpandLastRaceColumn} has been performed once. It must not be performed another time.
     */
    protected boolean autoExpandPerformedOnce;

    /**
     * This anchor's HTML holds the image tag for the play/pause button that needs to be updated when the {@link #timer}
     * changes its playing state
     */
    Anchor playPause;

    protected final CompetitorSelectionProvider competitorSelectionProvider;
    private final HorizontalPanel filterControlPanel;
    private Label filterStatusLabel;
    private Button filterClearButton;

    /**
     * The handler for changes in the leaderboard table's selection; its registration is kept in
     * {@link #leaderboardAsTableSelectionModelRegistration} while it is registered as a selection handler on the
     * {@link #leaderboardTable}.
     */
    private final Handler selectionChangeHandler;

    /**
     * While the {@link #selectionChangeHandler} is registered as a selection change handler on the
     * {@link #leaderboardTable}'s selection model, this field holds the registration which can be used to remove the
     * registration again. We'll use this to temporarily suspend selection events when actively modifying / adjusting
     * the table selection to match the {@link #competitorSelectionProvider}.
     */
    private HandlerRegistration leaderboardAsTableSelectionModelRegistration;

    private final FlowPanel contentPanel = new FlowPanel();
    protected final PaywallResolver paywallResolver;

    private HorizontalPanel refreshAndSettingsPanel;
    private Label scoreCorrectionLastUpdateTimeLabel;
    private Label scoreCorrectionCommentLabel;
    private Label liveRaceLabel;

    private final boolean isEmbedded;

    private ImageResource pauseIcon;
    private ImageResource playIcon;

    /**
     * For a leaderboard, zero or more tasks may be currently busy. The counter keeps track. If it goes to {@code 0},
     * the {@link #busyIndicator} is set to non-busy. If it goes from {@code 0} to {@code 1} the {@link #busyIndicator}
     * is set to busy.
     */
    private int busyTaskCounter;
    private final BusyIndicator busyIndicator;
    private final Set<BusyStateChangeListener> busyStateChangeListeners;

    private final AsyncActionsExecutor asyncActionsExecutor;

    
    /**
     * See also {@link #getDefaultSortColumn()}. If no other column is explicitly selected for sorting and this
     * attribute holds a non-<code>null</code> string identifying a valid race by name that is represented in this
     * leaderboard panel then sort by it. Otherwise, default sorting will default to the overall rank column.
     */
    private String raceNameForDefaultSorting;

    /**
     * Can be used to disallow users to drill into the race details.
     */
    private final boolean showRaceDetails;

    /**
     * The {@link LastNRacesColumnSelection} column selection strategy requires a {@link RaceTimesInfoProvider}. This
     * can either be injected by passing a non-<code>null</code> object of that type to the constructor, or such an
     * object is created and remembered in this attribute when required the first time.
     */
    private RaceTimesInfoProvider raceTimesInfoProvider;
    private RaceTimesInfoProviderListener raceTimesInfoProviderListener;

    private int blurInOnSelectionChanged;

    /**
     * When an element in the leaderboard receives focus, it needs to be blurred again to keep the surrounding scroll
     * panel from scrolling anything into view
     */
    private Element elementToBlur;
    private boolean showSelectionCheckbox;

    private final List<LeaderboardUpdateListener> leaderboardUpdateListener = new ArrayList<LeaderboardUpdateListener>();

    private boolean initialCompetitorFilterHasBeenApplied = false;
    private final boolean showCompetitorFilterStatus;

    protected CompetitorFilterPanel competitorFilterPanel;

    /**
     * Whether or not a second scroll bar, synchronized with the invisible native scroll bar, shall appear at the bottom
     * of the viewport. See {@link OverlayAssistantScrollPanel}.
     */
    private final boolean enableSyncedScroller;
    public boolean isShowCompetitorNationality;

    protected LeaderBoardStyle style;

    private FlowPanel informationPanel;

    private FlagImageResolver flagImageResolver;

    private Widget toolbarPanel;

    public LeaderboardPanel(Component<?> parent, ComponentContext<?> context, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, LS settings,
            CompetitorSelectionProvider competitorSelectionProvider, String leaderboardName,
            ErrorReporter errorReporter, final StringMessages stringMessages, boolean showRaceDetails,
            LeaderBoardStyle style, FlagImageResolver flagImageResolver, Iterable<DetailType> availableDetailTypes, WithSecurity sailingCF) {
        this(parent, context, sailingService, asyncActionsExecutor, settings, false, competitorSelectionProvider,
                leaderboardName, errorReporter, stringMessages, showRaceDetails, style, flagImageResolver, availableDetailTypes, sailingCF);
    }

    public LeaderboardPanel(Component<?> parent, ComponentContext<?> context, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, LS settings, boolean isEmbedded,
            CompetitorSelectionProvider competitorSelectionProvider,
            String leaderboardName, ErrorReporter errorReporter, final StringMessages stringMessages,
            boolean showRaceDetails, LeaderBoardStyle style, FlagImageResolver flagImageResolver, Iterable<DetailType> availableDetailTypes, WithSecurity sailingCF) {
        this(parent, context, sailingService, asyncActionsExecutor, settings, isEmbedded, competitorSelectionProvider,
                new Timer(
                        // perform the first request as "live" but don't by default auto-play
                        PlayModes.Live, PlayStates.Paused,
                        /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS),
                leaderboardName, errorReporter, stringMessages, showRaceDetails,
                /* competitorSearchTextBox */ null, /* showSelectionCheckbox */ true,
                /* optionalRaceTimesInfoProvider */ null, /* autoExpandLastRaceColumn */ false,
                /* adjustTimerDelay */ true, /* autoApplyTopNFilter */ false, /* showCompetitorFilterStatus */ false,
                /* enableSyncScroller */ false, style, flagImageResolver, availableDetailTypes, sailingCF);
    }

    public LeaderboardPanel(Component<?> parent, ComponentContext<?> context, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, LS settings, boolean isEmbedded,
            CompetitorSelectionProvider competitorSelectionProvider, Timer timer,
            String leaderboardName, final ErrorReporter errorReporter, final StringMessages stringMessages,
            boolean showRaceDetails, CompetitorFilterPanel competitorSearchTextBox, boolean showSelectionCheckbox,
            RaceTimesInfoProvider optionalRaceTimesInfoProvider, boolean autoExpandLastRaceColumn,
            boolean adjustTimerDelay, boolean autoApplyTopNFilter, boolean showCompetitorFilterStatus,
            boolean enableSyncScroller, LeaderBoardStyle style, FlagImageResolver flagImageResolver, 
            Iterable<DetailType> availableDetailTypes, WithSecurity sailingCF) {
        super(parent, context);
        this.style = style;
        this.showSelectionCheckbox = showSelectionCheckbox;
        this.showRaceDetails = showRaceDetails;
        this.sailingService = sailingService;
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.isEmbedded = isEmbedded;
        this.competitorSelectionProvider = competitorSelectionProvider;
        this.competitorSelectionProvider.addCompetitorSelectionChangeListener(this);
        this.setLeaderboardName(leaderboardName);
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        this.raceTimesInfoProvider = optionalRaceTimesInfoProvider;
        this.adjustTimerDelay = adjustTimerDelay;
        this.initialCompetitorFilterHasBeenApplied = !autoApplyTopNFilter;
        this.showCompetitorFilterStatus = showCompetitorFilterStatus;
        this.enableSyncedScroller = enableSyncScroller;
        this.autoExpandLastRaceColumn = autoExpandLastRaceColumn;
        this.flagImageResolver = flagImageResolver;
        this.timer = timer;
        RACE_COLUMN_HEADER_STYLE = style.getTableresources().cellTableStyle().cellTableRaceColumnHeader();
        LEG_COLUMN_HEADER_STYLE = style.getTableresources().cellTableStyle().cellTableLegColumnHeader();
        LEG_DETAIL_COLUMN_HEADER_STYLE = style.getTableresources().cellTableStyle().cellTableLegDetailColumnHeader();
        RACE_COLUMN_STYLE = style.getTableresources().cellTableStyle().cellTableRaceColumn();
        LEG_COLUMN_STYLE = style.getTableresources().cellTableStyle().cellTableLegColumn();
        LEG_DETAIL_COLUMN_STYLE = style.getTableresources().cellTableStyle().cellTableLegDetailColumn();
        TOTAL_COLUMN_STYLE = style.getTableresources().cellTableStyle().cellTableTotalColumn();
        this.paywallResolver = new PaywallResolverImpl(sailingCF.getUserService(), sailingCF.getSubscriptionServiceFactory());
        // Now register a user status event handler that notices changes in user sign-in/out or premium status change.
        // Leaderboard columns can depend on permissions, and currently (and we should change that!) the filtering
        // happens in the LeaderboardSettingsComponentDialog. Therefore, in order to filter the current settings
        // based on the user permissions it is currently required to instantiate (but not show) a settings dialog
        // that is initialized with the current settings, then retrieving the filtered settings using getResult() and
        // updating those using updateSettings(...).
        sailingCF.getUserService().addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                final LS adjustedSettingsAfterUserChange = new SettingsDialog<LS>(LeaderboardPanel.this, stringMessages).getResult();
                updateSettings(adjustedSettingsAfterUserChange);
            }
        });
        overallDetailColumnMap = createOverallDetailColumnMap();
        if (settings.getLegDetailsToShow() != null) {
            selectedLegDetails.addAll(settings.getLegDetailsToShow());
        }
        if (settings.getOverallDetailsToShow() != null) {
            selectedOverallDetailColumns.addAll(settings.getOverallDetailsToShow());
        }
        timer.addPlayStateListener(this);
        timer.addTimeListener(this);

        totalRankColumn = new TotalRankColumn();
        totalRankColumn.setCellStyleNames("totalRankColumn");
        leaderboardTable = new FlushableSortedCellTableWithStylableHeaders<LeaderboardRowDTO>(/* pageSize */10000,
                style.getTableresources());
        leaderboardTable.addCellPreviewHandler(new CellPreviewEvent.Handler<LeaderboardRowDTO>() {
            @Override
            public void onCellPreview(CellPreviewEvent<LeaderboardRowDTO> event) {
                if (BrowserEvents.FOCUS.equals(event.getNativeEvent().getType())) {

                    elementToBlur = event.getNativeEvent().getEventTarget().cast();
                    if (!SelectElement.is(elementToBlur)) {
                        elementToBlur.blur();
                        blurInOnSelectionChanged = 2; // blur a couple of times; doing it one time only doesn't seem to
                                                      // work
                        // reliably
                        blurFocusedElementAfterSelectionChange();
                    }
                }
            }
        });
        leaderboardTable.ensureDebugId("LeaderboardCellTable");
        selectionCheckboxColumn = new LeaderboardSelectionCheckboxColumn(competitorSelectionProvider);
        leaderboardTable.setWidth("100%");
        leaderboardSelectionModel = new MultiSelectionModel<LeaderboardRowDTO>();
        // remember handler registration so we can temporarily remove it and re-add it to suspend selection events while
        // we're actively changing it
        selectionChangeHandler = new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                List<CompetitorDTO> selection = new ArrayList<>();
                for (LeaderboardRowDTO row : getSelectedRows()) {
                    selection.add(row.competitor);
                }
                LeaderboardPanel.this.competitorSelectionProvider.setSelection(selection,
                        /* listenersNotToNotify */LeaderboardPanel.this);
                if (blurInOnSelectionChanged > 0) {
                    blurInOnSelectionChanged--;
                    blurFocusedElementAfterSelectionChange();
                }
            }
        };
        leaderboardAsTableSelectionModelRegistration = leaderboardSelectionModel
                .addSelectionChangeHandler(selectionChangeHandler);
        leaderboardTable.setSelectionModel(leaderboardSelectionModel, selectionCheckboxColumn.getSelectionManager());

        SimplePanel mainPanel = new SimplePanel();
        leaderboardTable.getElement().getStyle().setMarginTop(10, Unit.PX);
        contentPanel.setStyleName(STYLE_LEADERBOARD_CONTENT);
        busyIndicator = new SimpleBusyIndicator(false, 0.8f);
        busyIndicator.ensureDebugId("BusyIndicator");
        busyStateChangeListeners = new HashSet<>();

        //required to enforce proper margin layouting
        contentPanel.add(new Label());
        
        // the information panel
        if (!isEmbedded) {
            toolbarPanel = createToolbarPanel();
            contentPanel.add(toolbarPanel);
        }
        if (competitorSearchTextBox != null) {
            competitorSearchTextBox.add(busyIndicator);
            contentPanel.add(competitorSearchTextBox);
            competitorSearchTextBox.getSettingsButton().addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    openSettingsDialog();
                }
            });
            this.competitorFilterPanel = competitorSearchTextBox;
        }
        filterControlPanel = new HorizontalPanel();
        filterControlPanel.setStyleName("LeaderboardPanel-FilterControl-Panel");
        if (enableSyncedScroller) {
            contentPanel.add(new OverlayAssistantScrollPanel(leaderboardTable));
        } else {
            style.hookLeaderBoardAttachment(contentPanel, leaderboardTable);
        }
        if (showCompetitorFilterStatus) {
            contentPanel.add(createFilterDeselectionControl());
        }
        initWidget(mainPanel);
        mainPanel.setWidget(contentPanel);
        this.setTitle(stringMessages.leaderboard());
        this.availableDetailTypes = availableDetailTypes;
    }

    protected abstract void openSettingsDialog();

    protected void initialize(LS settings) {
        setDefaultRaceColumnSelection(settings);
        if (timer.isInitialized()) {
            loadCompleteLeaderboard(/* showProgress */ false);
        }
        updateSettings(settings);
        style.afterConstructorHook(this);
        //ensure proper margin styling
        contentPanel.add(new Label());
    }

    public void scrollRowIntoView(int selected) {
        leaderboardTable.getRowElement(selected).scrollIntoView();
    }

    Widget createToolbarPanel() {
        informationPanel = new FlowPanel();
        informationPanel.setStyleName(STYLE_LEADERBOARD_INFO);
        scoreCorrectionLastUpdateTimeLabel = new Label("");
        scoreCorrectionCommentLabel = new Label("");
        informationPanel.add(scoreCorrectionCommentLabel);
        informationPanel.add(scoreCorrectionLastUpdateTimeLabel);

        liveRaceLabel = new Label(stringMessages.live());
        liveRaceLabel.setStyleName(STYLE_LEADERBOARD_LIVE_RACE);
        liveRaceLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
        liveRaceLabel.getElement().getStyle().setColor(IS_LIVE_TEXT_COLOR);
        liveRaceLabel.setVisible(false);
        informationPanel.add(liveRaceLabel);

        // the toolbar panel
        DockPanel toolbarPanel = new DockPanel();
        toolbarPanel.ensureDebugId("ToolbarPanel");
        toolbarPanel.setStyleName(STYLE_LEADERBOARD_TOOLBAR);
        if (!isEmbedded) {
            toolbarPanel.add(informationPanel, DockPanel.WEST);
            toolbarPanel.add(busyIndicator, DockPanel.WEST);
        }
        toolbarPanel.setWidth("100%");
        toolbarPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        ClickHandler playPauseHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (LeaderboardPanel.this.timer.getPlayState() == PlayStates.Playing) {
                    LeaderboardPanel.this.timer.pause();
                } else {
                    // playing the standalone leaderboard means putting it into live mode
                    LeaderboardPanel.this.timer.setPlayMode(PlayModes.Live);
                }
            }
        };
        pauseIcon = style.getResources().autoRefreshEnabledIcon();
        playIcon = style.getResources().autoRefreshDisabledIcon();
        refreshAndSettingsPanel = new HorizontalPanel();
        refreshAndSettingsPanel.ensureDebugId("RefreshAndSettingsPanel");
        refreshAndSettingsPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        FlowPanel refreshPanel = new FlowPanel();
        refreshPanel.addStyleName("refreshPanel");
        toolbarPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        toolbarPanel.addStyleName("refreshAndSettings");
        playPause = new Anchor(getPlayPauseImgHtml(timer.getPlayState()));
        playPause.ensureDebugId("PlayAndPauseAnchor");
        playPause.addClickHandler(playPauseHandler);
        playStateChanged(timer.getPlayState(), timer.getPlayMode());
        refreshPanel.add(playPause);

        refreshAndSettingsPanel.add(refreshPanel);
        toolbarPanel.add(refreshAndSettingsPanel, DockPanel.EAST);
        return toolbarPanel;
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public Widget getHeaderWidget() {
        return null;
    }

    @Override
    public Widget getContentWidget() {
        return contentPanel;
    }

    @Override
    public Widget getToolbarWidget() {
        return informationPanel;
    }

    @Override
    public Widget getLegendWidget() {
        return null;
    }

    @Override
    public boolean isEmbedded() {
        return isEmbedded;
    }

    public FlowPanel getContentPanel() {
        return contentPanel;
    }

    protected ImageResource getSettingsIcon() {
        return style.getComponentresources().settingsIcon();
    }

    public void setShowCompetitorNationality(boolean isShowCompetitorNationality) {
        this.isShowCompetitorNationality = isShowCompetitorNationality;
    }

    @Override
    public void updateSettings(final LS newSettings) {
        this.currentSettings = newSettings;
        boolean oldShallAddOverallDetails = shallAddOverallDetails();
        if (newSettings.getOverallDetailsToShow() != null) {
            setValuesWithReferenceOrder(newSettings.getOverallDetailsToShow(), DetailType.getAvailableOverallDetailColumnTypes(),
                    selectedOverallDetailColumns);
        }
        setShowCompetitorNationality(newSettings.isShowCompetitorNationality());
        setShowAddedScores(newSettings.isShowAddedScores());
        setShowCompetitorShortName(newSettings.isShowCompetitorShortNameColumn());
        setShowCompetitorFullName(newSettings.isShowCompetitorFullNameColumn());
        setShowCompetitorBoatInfo(newSettings.isShowCompetitorBoatInfoColumn());
        final List<ExpandableSortableColumn<?>> columnsToExpandAgain = new ArrayList<ExpandableSortableColumn<?>>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(i);
            if (c instanceof ExpandableSortableColumn<?>) {
                ExpandableSortableColumn<?> expandableSortableColumn = (ExpandableSortableColumn<?>) c;
                if (expandableSortableColumn.isExpanded()) {
                    // now toggle expansion back and forth,
                    // enforcing a re-build of the visible
                    // child columns
                    expandableSortableColumn.changeExpansionState(/* expand */ false);
                    columnsToExpandAgain.add(expandableSortableColumn);
                }
            }
        }
        applyDetailSettings(newSettings);
        addBusyTask();
        Runnable doWhenNecessaryDetailHasBeenLoaded = new Runnable() {
            @Override
            public void run() {
                try {
                    // avoid expansion during updateLeaderboard(...); will expand
                    // later if it was expanded before
                    applyRaceSelection(newSettings);
                    updateLeaderboard(leaderboard);
                    postApplySettings(newSettings, columnsToExpandAgain);
                } finally {
                    removeBusyTask();
                }
            }
        };
        boolean newShallAddOverallDetails = shallAddOverallDetails();
        if (oldShallAddOverallDetails == newShallAddOverallDetails || oldShallAddOverallDetails
                || getLeaderboard().hasOverallDetails()) {
            doWhenNecessaryDetailHasBeenLoaded.run();
        } else { // meaning that now the details need to be loaded from the server
            updateLeaderboardAndRun(doWhenNecessaryDetailHasBeenLoaded);
        }
    }

    protected void postApplySettings(final LeaderboardSettings newSettings,
            final List<ExpandableSortableColumn<?>> columnsToExpandAgain) {
        if (newSettings.getDelayBetweenAutoAdvancesInMilliseconds() != null) {
            timer.setRefreshInterval(newSettings.getDelayBetweenAutoAdvancesInMilliseconds());
        }
        for (ExpandableSortableColumn<?> expandableSortableColumn : columnsToExpandAgain) {
            expandableSortableColumn.changeExpansionState(/* expand */ true);
        }
    }

    protected abstract void applyRaceSelection(final LeaderboardSettings newSettings);

    private void applyDetailSettings(final LeaderboardSettings newSettings) {
        if (newSettings.getOverallDetailsToShow() != null) {
            setValuesWithReferenceOrder(Util.retainCopy(newSettings.getOverallDetailsToShow(), availableDetailTypes),
                    DetailType.getAvailableOverallDetailColumnTypes(), selectedOverallDetailColumns);
        }
        setShowCompetitorNationality(newSettings.isShowCompetitorNationality());
        setShowAddedScores(newSettings.isShowAddedScores());
        setShowCompetitorShortName(newSettings.isShowCompetitorShortNameColumn());
        setShowCompetitorFullName(newSettings.isShowCompetitorFullNameColumn());
        setShowCompetitorBoatInfo(newSettings.isShowCompetitorBoatInfoColumn());
        if (newSettings.getManeuverDetailsToShow() != null) {
            setValuesWithReferenceOrder(Util.retainCopy(newSettings.getManeuverDetailsToShow(), availableDetailTypes),
                    ManeuverCountRaceColumn.getAvailableManeuverDetailColumnTypes(), selectedManeuverDetails);
        }
        if (newSettings.getLegDetailsToShow() != null) {
            setValuesWithReferenceOrder(Util.retainCopy(newSettings.getLegDetailsToShow(), availableDetailTypes),
                    DetailType.getAllLegDetailColumnTypes(), selectedLegDetails);
        }
        if (newSettings.getRaceDetailsToShow() != null) {
            List<DetailType> allRaceDetailsTypes = new ArrayList<>();
            allRaceDetailsTypes.addAll(DetailType.getAllRaceDetailTypes());
            allRaceDetailsTypes.addAll(DetailType.getRaceStartAnalysisColumnTypes());
            setValuesWithReferenceOrder(Util.retainCopy(newSettings.getRaceDetailsToShow(), availableDetailTypes),
                    allRaceDetailsTypes, selectedRaceDetails);
        }
    }

    protected abstract void setDefaultRaceColumnSelection(LS settings);

    private void setValuesWithReferenceOrder(Iterable<DetailType> valuesToSet, Collection<DetailType> referenceOrder,
            List<DetailType> collectionToSetValuesTo) {
        collectionToSetValuesTo.clear();
        collectionToSetValuesTo.addAll(referenceOrder);
        collectionToSetValuesTo.retainAll(Util.asList(valuesToSet));
    }

    /**
     * @param callWhenExpansionDataIsLoaded
     */

    protected abstract void updateLeaderboardAndRun(final Runnable callWhenExpansionDataIsLoaded);

    public void setRaceColumnSelectionToLastNStrategy(final Integer numberOfLastRacesToShow) {
        raceColumnSelection = new LastNRacesColumnSelection(numberOfLastRacesToShow, getRaceTimesInfoProvider());
        if (timer.getPlayState() != Timer.PlayStates.Playing) {
            // wait for the first update and adjust leaderboard once the race times have been received
            raceTimesInfoProviderListener = new RaceTimesInfoProviderListener() {
                @Override
                public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo,
                        long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest,
                        long clientTimeWhenResponseWasReceived) {
                    // remove
                    timer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest,
                            clientTimeWhenResponseWasReceived);
                    // remove the listener only in case a leaderboard has already been loaded
                    if (getLeaderboard() != null) {
                        updateLeaderboard(getLeaderboard());
                        getRaceTimesInfoProvider().removeRaceTimesInfoProviderListener(raceTimesInfoProviderListener);
                        raceTimesInfoProviderListener = null;
                    }
                }
            };
            getRaceTimesInfoProvider().addRaceTimesInfoProviderListener(raceTimesInfoProviderListener);
        }
    }

    /**
     * A leaderboard panel may have been provided with a valid {@link RaceTimesInfoProvider} upon creation; in this
     * case, that object will be returned. If none was provided to the constructor, one is created and remembered if no
     * previously created/remembered object exists.
     * <p>
     * 
     * Precondition: {@link #timer} is not <code>null</code>
     */
    private RaceTimesInfoProvider getRaceTimesInfoProvider() {
        if (raceTimesInfoProvider == null) {
            final List<RegattaAndRaceIdentifier> trackedRacesIdentifiers;
            if (leaderboard != null && getTrackedRacesIdentifiers() != null) {
                trackedRacesIdentifiers = getTrackedRacesIdentifiers();
            } else {
                trackedRacesIdentifiers = Collections.emptyList();
            }
            raceTimesInfoProvider = new RaceTimesInfoProvider(getSailingService(), asyncActionsExecutor, errorReporter,
                    trackedRacesIdentifiers, timer.getRefreshInterval());
        }
        return raceTimesInfoProvider;
    }

    public AsyncActionsExecutor getExecutor() {
        return asyncActionsExecutor;
    }

    /**
     * Shows the country flag and a competitor short info (short name or sailId), if present
     * @author Axel Uhl (d043530)
     */
    private class CompetitorInfoWithFlagColumn<T> extends LeaderboardSortableColumnWithMinMax<T, String> {
        private final CompetitorFetcher<T> competitorFetcher;

        protected CompetitorInfoWithFlagColumn(CompetitorFetcher<T> competitorFetcher, LeaderBoardStyle style) {
            super(new TextCell(), SortingOrder.ASCENDING, LeaderboardPanel.this);
            this.competitorFetcher = competitorFetcher;
            // This style is adding to avoid contained images CSS property "max-width: 100%", which could cause
            // an overflow to the next column (see https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=3537)
            setCellStyleNames(style.getTableresources().cellTableStyle().cellTableSailIdColumn());
        }
        
        /**
         * Depending on the {@link LeaderboardDTO LeaderboardDTO's}
         * {@link AbstractLeaderboardDTO#canBoatsOfCompetitorsChangePerRace canBoatsOfCompetitorsChangePerRace} flag
         * there is a different precedence of showing either the sail ID (no changing boats) or the competitor short
         * name (changing boats). This method handles the resolution of the text to show for a competitor in this column.
         */
        private String getRealTextToShow(CompetitorDTO competitor) {
            boolean preferSailId = !getLeaderboard().canBoatsOfCompetitorsChangePerRace;
            return competitor.getShortInfo(preferSailId);
        }

        @Override
        public InvertibleComparator<T> getComparator() {
            return new InvertibleComparatorAdapter<T>() {
                @Override
                public int compare(T o1, T o2) {
                    final String competitor1ShortInfo = getRealTextToShow(competitorFetcher.getCompetitor(o1));
                    final String competitor2ShortInfo = getRealTextToShow(competitorFetcher.getCompetitor(o2));
                    return competitor1ShortInfo == null ? competitor2ShortInfo == null ? 0 : -1
                            : competitor2ShortInfo == null ? 1
                                    : Collator.getInstance().compare(competitor1ShortInfo, competitor2ShortInfo);
                }
            };
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return new SafeHtmlHeaderWithTooltip(SafeHtmlUtils.fromString(stringMessages.competitor()),
                    stringMessages.shortName());
        }

        @Override
        public void render(Context context, T object, SafeHtmlBuilder sb) {
            final CompetitorDTO competitor = competitorFetcher.getCompetitor(object);
            LeaderboardPanel.this.renderCompetitorText(competitor, isShowCompetitorShortName(),
                    !isShowCompetitorFullName(), sb, builder -> builder.appendEscaped(getRealTextToShow(competitor)));
        }

        @Override
        public String getValue(T object) {
            return getRealTextToShow(competitorFetcher.getCompetitor(object));
        }
    }

    /**
     * A sortable leaderboard column showing boat information
     * @author Frank Mittag
     */
    private class BoatInfoColumn<T> extends LeaderboardSortableColumnWithMinMax<T, String> {
        private final BoatFetcher<T> boatFetcher;

        public BoatInfoColumn(BoatFetcher<T> boatFetcher, LeaderBoardStyle style) {
            super(new TextCell(), SortingOrder.ASCENDING, LeaderboardPanel.this);
            this.boatFetcher = boatFetcher;
            // This style is adding to avoid contained images CSS property "max-width: 100%", which could cause
            // an overflow to the next column (see https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=3537)
            setCellStyleNames(style.getTableresources().cellTableStyle().cellTableSailIdColumn());
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return new SafeHtmlHeaderWithTooltip(SafeHtmlUtils.fromString(stringMessages.boat()),
                    stringMessages.boat());
        }

        @Override
        public void render(Context context, T object, SafeHtmlBuilder sb) {
            BoatDTO boat = boatFetcher.getBoat(object);
            if (boat != null) {
                sb.appendEscaped(getShortInfo(boat));
            }
        }
        
        private String getShortInfo(BoatDTO boat) {
            final String result;
            if (boat.getName() != null) {
                result = boat.getName();
            } else if (boat.getSailId() != null) {
                result = boat.getSailId();
            } else {
                result = null;
            }
            return result;
        }
        
        @Override
        public InvertibleComparator<T> getComparator() {
            return new InvertibleComparatorAdapter<T>() {
                @Override
                public int compare(T o1, T o2) {
                    return getShortInfo(boatFetcher.getBoat(o1)) == null
                            ? getShortInfo(boatFetcher.getBoat(o2)) == null ? 0 : -1
                            : getShortInfo(boatFetcher.getBoat(o2)) == null ? 1
                                    : Collator.getInstance().compare(getShortInfo(boatFetcher.getBoat(o1)),
                                            getShortInfo(boatFetcher.getBoat(o2)));
                }
            };
        }

        @Override
        public String getValue(T object) {
            BoatDTO boat = boatFetcher.getBoat(object);
            return boat != null ? boat.getName() : null;
        }        
    }
    
    
    protected void processStyleForRaceColumnWithReasonForMaxPoints(boolean isDiscarded, SafeStylesBuilder ssb) {
        ssb.opacity(0.5d);
    }

    /**
     * Displays net/total points and possible max-points reasons based on a {@link LeaderboardRowDTO} and a race name
     * and makes the column sortable by the total points.
     * 
     * @author Axel Uhl (D043530)
     * 
     */
    protected abstract class RaceColumn<C> extends ExpandableSortableColumn<C> {
        RaceColumnDTO race;

        private final String headerStyle;
        private final String columnStyle;

        public RaceColumn(RaceColumnDTO race, boolean enableExpansion, Cell<C> cell, SortingOrder preferredSortingOrder,
                String headerStyle, String columnStyle) {
            super(LeaderboardPanel.this, enableExpansion, cell, preferredSortingOrder, stringMessages,
                    LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, selectedRaceDetails, LeaderboardPanel.this);
            setHorizontalAlignment(ALIGN_CENTER);
            this.race = race;
            this.headerStyle = headerStyle;
            this.columnStyle = columnStyle;
        }

        public RaceColumnDTO getRace() {
            return race;
        }

        public void setRace(RaceColumnDTO race) {
            this.race = race;
        }

        public String getRaceColumnName() {
            return race.getRaceColumnName();
        }

        public boolean isMedalRace() {
            return race.isMedalRace();
        }

        public boolean isLive(FleetDTO fleetDTO) {
            return race.isLive(fleetDTO, timer.getLiveTimePointInMillis());
        }

        @Override
        public String getColumnStyle() {
            return columnStyle;
        }

        /**
         * Computes added scores for this RaceColumn
         */
        private double computeAddedScores(LeaderboardRowDTO object) {
            double addedScores = 0;
            for (RaceColumnDTO raceColumn : getLeaderboard().getRaceList()) {
                LeaderboardEntryDTO entryBefore = object.fieldsByRaceColumnName.get(raceColumn.getName());
                if (entryBefore.netPoints != null) {
                    addedScores += entryBefore.netPoints;
                }
                // else "add 0"
                if (raceColumn.getName().equals(getRaceColumnName())) {
                    break; // we've reached the current column - stop here
                }
            }
            return addedScores;
        }

        /**
         * Displays a combination of net points and maxPointsReason in bold, transparent, strike-through, depending on
         * various criteria. Here's how:
         * 
         * <pre>
         *                                  net points                |    maxPointsReason
         * -------------------------------+-----------------------------+-----------------------
         *  not discarded, no maxPoints   | bold                        | none
         *  not discarded, maxPoints      | bold                        | transparent
         *  discarded, no maxPoints       | transparent, strike-through | none
         *  discarded, maxPoints          | transparent, strike-through | transparent, strike-through
         * </pre>
         */
        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder html) {
            LeaderboardEntryDTO entry = object.fieldsByRaceColumnName.get(getRaceColumnName());
            if (entry != null) {
                boolean isLive = isLive(entry.fleet);
                final String textColor = isLive ? IS_LIVE_TEXT_COLOR : DEFAULT_TEXT_COLOR;
                final String addedScores = isShowAddedScores() ? scoreFormat.format(computeAddedScores(object)) : "";
                String netOrAddedPointsAsText = isShowAddedScores() ? addedScores
                        : entry.netPoints == null ? "" : scoreFormat.format(entry.netPoints);
                String totalOrAddedPointsAsText = isShowAddedScores() ? addedScores
                        : entry.totalPoints == null ? "" : scoreFormat.format(entry.totalPoints);
                if (entry.fleet != null && entry.fleet.getColor() != null) {
                    html.append(raceColumnTemplate.cellFrameWithTextColorAndFleetBorder(textColor,
                            entry.fleet.getColor().getAsHtml()));
                } else {
                    html.append(raceColumnTemplate.cellFrameWithTextColor(textColor));
                }
                // don't show points if max points / penalty
                if (entry.reasonForMaxPoints == null || entry.reasonForMaxPoints == MaxPointsReason.NONE) {
                    SafeStylesBuilder ssb = new SafeStylesBuilder();
                    style.processStyleForRaceColumnWithoutReasonForMaxPoints(entry.discarded, ssb);
                    html.append(SafeHtmlUtils.fromTrustedString("<span style='"));
                    html.append(SafeHtmlUtils.fromTrustedString(ssb.toSafeStyles().asString()));
                    html.append(SafeHtmlUtils.fromTrustedString("'>"));
                    if (!entry.discarded) {
                        html.append(SafeHtmlUtils.fromTrustedString(netOrAddedPointsAsText));
                    } else {
                        html.append(SafeHtmlUtils.fromTrustedString("<del>"));
                        html.append(SafeHtmlUtils.fromTrustedString(totalOrAddedPointsAsText));
                        html.append(SafeHtmlUtils.fromTrustedString("</del>"));
                    }
                    html.append(SafeHtmlUtils.fromTrustedString("</span>"));

                } else {
                    SafeStylesBuilder ssb = new SafeStylesBuilder();
                    processStyleForRaceColumnWithReasonForMaxPoints(entry.discarded, ssb);
                    html.append(SafeHtmlUtils.fromTrustedString(
                            "<span title=\"" + totalOrAddedPointsAsText + "/" + netOrAddedPointsAsText + "\" style='"));
                    html.append(SafeHtmlUtils.fromTrustedString(ssb.toSafeStyles().asString()));
                    html.append(SafeHtmlUtils.fromTrustedString("'>"));
                    if (entry.discarded) {
                        html.append(SafeHtmlUtils.fromTrustedString("<del>"));
                    }
                    html.appendEscaped(
                            entry.reasonForMaxPoints == MaxPointsReason.NONE ? "" : entry.reasonForMaxPoints.name());
                    if (entry.discarded) {
                        html.appendHtmlConstant("</del>");
                    }
                    html.appendHtmlConstant("</span>");
                }
                html.appendHtmlConstant("</div>");
            }
        }

        @Override
        public InvertibleComparator<LeaderboardRowDTO> getComparator() {
            return new InvertibleComparatorAdapter<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    final int result;
                    if (isShowAddedScores()) {
                        double o1AddedScore = computeAddedScores(o1);
                        double o2AddedScore = computeAddedScores(o2);
                        double intermediate_result = o1AddedScore == 0.
                                ? (o2AddedScore == 0. ? -1. : isAscending() ? 1. : -1.)
                                : o2AddedScore == 0. ? (isAscending() ? -1. : 1.) : o1AddedScore - o2AddedScore;
                        if (!getLeaderboard().isHigherScoreBetter()) {
                            result = intermediate_result > 0 ? 1 : intermediate_result < 0 ? -1 : 0;
                        } else {
                            result = intermediate_result > 0 ? -1 : intermediate_result < 0 ? 1 : 0;
                        }
                    } else {
                        List<CompetitorDTO> competitorsFromBestToWorst = getLeaderboard().getCompetitorsFromBestToWorst(race);
                        int o1Rank = competitorsFromBestToWorst.indexOf(o1.competitor) + 1;
                        int o2Rank = competitorsFromBestToWorst.indexOf(o2.competitor) + 1;
                        result = o1Rank == 0 ? o2Rank == 0 ? 0 : isAscending() ? 1 : -1
                                : o2Rank == 0 ? isAscending() ? -1 : 1 : o1Rank - o2Rank;
                    }
                    return result;
                }
            };
        }

        @Override
        public String getHeaderStyle() {
            return headerStyle;
        }

        @Override
        public SortableExpandableColumnHeader getHeader() {
            SortableExpandableColumnHeader header = new SortableExpandableColumnHeader(
                    /* title */race.getRaceColumnName(),
                    /* iconURL */race.isMedalRace() ? style.getResources().medalSmall().getSafeUri() : null, LeaderboardPanel.this, this,
                    stringMessages);
            return header;
        }
    }

    private class TextRaceColumn extends RaceColumn<String> implements RaceNameProvider {
        /**
         * Remembers the leg columns; <code>null</code>-padded, if {@link #getLegColumn(int)} asks for a column index
         * not yet existing. It is important to remember the columns because column removal happens based on identity.
         */
        private final List<LegColumn> legColumns;

        private BoatInfoColumn<LeaderboardRowDTO> boatInfoColumn;

        public TextRaceColumn(RaceColumnDTO race, boolean expandable, SortingOrder preferredSortingOrder,
                String headerStyle, String columnStyle) {
            super(race, expandable, new TextCell(), preferredSortingOrder, headerStyle, columnStyle);
            legColumns = new ArrayList<LegColumn>();
        }

        public String getValue(LeaderboardRowDTO object) {
            // The following code exists only for robustness. This method should never be called because
            // RaceColumn implements its own render(...) method which doesn't make use of getValue(...)
            final Double netPoints = object.fieldsByRaceColumnName.get(getRaceColumnName()).netPoints;
            return "" + (netPoints == null ? "" : scoreFormat.format(netPoints));
        }

        @Override
        protected void ensureExpansionDataIsLoaded(final Runnable callWhenExpansionDataIsLoaded) {
            if (getLegCount(getLeaderboard(), getRaceColumnName()) != -1) {
                callWhenExpansionDataIsLoaded.run();
            } else {
                updateLeaderboardAndRun(callWhenExpansionDataIsLoaded);
            }
        }

        @Override
        protected Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getDetailColumnMap(
                LeaderboardPanel<?> leaderboardPanel, StringMessages stringMessages, String detailHeaderStyle,
                String detailColumnStyle) {
            Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> result = new HashMap<>();
            result.put(DetailType.RACE_RATIO_BETWEEN_TIME_SINCE_LAST_POSITION_FIX_AND_AVERAGE_SAMPLING_INTERVAL,
                    new TimeSinceLastGpsFixColumn(
                            DetailType.RACE_RATIO_BETWEEN_TIME_SINCE_LAST_POSITION_FIX_AND_AVERAGE_SAMPLING_INTERVAL,
                            new RaceRatioBetweenTimeSinceLastPositionFixAndAverageSamplingInterval(),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.RACE_DISTANCE_TRAVELED,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_DISTANCE_TRAVELED,
                            new RaceDistanceTraveledInMeters(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_DISTANCE_TRAVELED_INCLUDING_GATE_START,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_DISTANCE_TRAVELED_INCLUDING_GATE_START,
                            new RaceDistanceTraveledIncludingGateStartInMeters(), LEG_COLUMN_HEADER_STYLE,
                            LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                            new RaceAverageSpeedInKnots(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_GAP_TO_LEADER_IN_SECONDS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_GAP_TO_LEADER_IN_SECONDS,
                            new RaceGapToLeaderInSeconds(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.PERCENT_TARGET_BOAT_SPEED,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.PERCENT_TARGET_BOAT_SPEED,
                            new PercentTargetBoatSpeed(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS,
                            new RaceCurrentSpeedOverGroundInKnots(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES,
                            new RaceCurrentCourseOverGroundInTrueDegrees(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS,
                    new RideHeightColumn(DetailType.BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS,
                            new RaceCurrentRideHeightInMeters(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_CURRENT_DISTANCE_FOILED_IN_METERS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_CURRENT_DISTANCE_FOILED_IN_METERS,
                            new RaceDistanceFoiledInMeters(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_CURRENT_DURATION_FOILED_IN_SECONDS,
                    new TotalTimeColumn(DetailType.RACE_CURRENT_DURATION_FOILED_IN_SECONDS,
                            new RaceDurationFoiledInSeconds(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS,
                            new RaceDistanceToCompetitorFarthestAheadInMeters(), LEG_COLUMN_HEADER_STYLE,
                            LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS,
                            new RaceAverageAbsoluteCrossTrackErrorInMeters(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS,
                            new RaceAverageSignedCrossTrackErrorInMeters(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(
                            DetailType.RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START,
                            new DistanceToStartFiveSecondsBeforeStartInMeters(), LEG_COLUMN_HEADER_STYLE,
                            LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START,
                            new SpeedFiveSecondsBeforeStartInKnots(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.DISTANCE_TO_START_AT_RACE_START,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.DISTANCE_TO_START_AT_RACE_START,
                            new DistanceToStartAtRaceStartInMeters(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.TIME_BETWEEN_RACE_START_AND_COMPETITOR_START,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.TIME_BETWEEN_RACE_START_AND_COMPETITOR_START,
                            new TimeBetweenRaceStartAndCompetitorStartInSeconds(), LEG_COLUMN_HEADER_STYLE,
                            LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.SPEED_OVER_GROUND_AT_RACE_START,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.SPEED_OVER_GROUND_AT_RACE_START,
                            new SpeedOverGroundAtRaceStartInKnots(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.SPEED_OVER_GROUND_WHEN_PASSING_START,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.SPEED_OVER_GROUND_WHEN_PASSING_START,
                            new SpeedOverGroundWhenPassingStartInKnots(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(
                            DetailType.DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS,
                            new DistanceToStarboardSideOfStartLineInMeters(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.START_TACK,
                    new StartingTackColumn(new TackWhenStarting(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE));
            result.put(DetailType.NUMBER_OF_MANEUVERS,
                    new ManeuverCountRaceColumn(getLeaderboardPanel(), this, stringMessages,
                            LeaderboardPanel.this.selectedManeuverDetails, LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LEG_DETAIL_COLUMN_HEADER_STYLE, LEG_DETAIL_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.RACE_CURRENT_LEG, new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_CURRENT_LEG,
                    new CurrentLeg(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.RACE_TIME_TRAVELED,
                    new TimeTraveledRaceColumnInSeconds(getLeaderboardPanel(), this, stringMessages, LEG_COLUMN_HEADER_STYLE,
                            LEG_COLUMN_STYLE, LEG_DETAIL_COLUMN_HEADER_STYLE, LEG_DETAIL_COLUMN_STYLE));
            result.put(DetailType.RACE_CALCULATED_TIME_TRAVELED,
                    new TotalTimeColumn(DetailType.RACE_CALCULATED_TIME_TRAVELED,
                            new RaceCalculatedTimeTraveledInSeconds(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_IMPLIED_WIND,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.RACE_IMPLIED_WIND,
                            new RaceImpliedWindInKnots(), LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD,
                    new TotalTimeColumn(DetailType.RACE_CALCULATED_TIME_AT_ESTIMATED_ARRIVAL_AT_COMPETITOR_FARTHEST_AHEAD,
                            new RaceCalculatedTimeAtEstimatedArrivalAtCompetitorFarthestAheadInSeconds(),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE,
                            LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_AWA,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_AWA,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionAWA),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_AWS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_AWS,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionAWS),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TWA,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TWA,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTWA),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TWS,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TWS,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTWS),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TWD,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TWD,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTWD),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TARG_TWA,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TARG_TWA,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTargTWA),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_BOAT_SPEED,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_BOAT_SPEED,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionBoatSpeed),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TARG_BOAT_SPEED,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TARG_BOAT_SPEED,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTargBoatSpeed),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_SOG,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_SOG,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionSOG),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_COG,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_COG,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionCOG),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_FORESTAY_LOAD,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_FORESTAY_LOAD,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionForestayLoad),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_RAKE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_RAKE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionRake),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_COURSE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_COURSE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionCourseDetail),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_HEADING,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_HEADING,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionHeading),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_VMG,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_VMG,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionVMG),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_VMG_TARG_VMG_DELTA,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_VMG_TARG_VMG_DELTA,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionVMGTargVMGDelta),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_RATE_OF_TURN,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_RATE_OF_TURN,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionRateOfTurn),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_RUDDER_ANGLE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_RUDDER_ANGLE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionRudderAngle),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TARGET_HEEL,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TARGET_HEEL,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTargetHeel),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_PORT_LAYLINE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_PORT_LAYLINE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTimeToPortLayline),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_STB_LAYLINE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_STB_LAYLINE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTimeToStbLayline),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_DIST_TO_PORT_LAYLINE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_DIST_TO_PORT_LAYLINE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionDistToPortLayline),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_DIST_TO_STB_LAYLINE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_DIST_TO_STB_LAYLINE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionDistToStbLayline),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_GUN,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_GUN,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTimeToGun),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT,
                            new DoubleTextRaceDetailTypeExtractor(
                                    LeaderboardEntryDTO::getExpeditionTimeToCommitteeBoat),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_PIN,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_PIN,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTimeToPin),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_BURN_TO_LINE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_BURN_TO_LINE,
                            new DoubleTextRaceDetailTypeExtractor(
                                    LeaderboardEntryDTO::getExpeditionTimeToBurnToLine),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT,
                            new DoubleTextRaceDetailTypeExtractor(
                                    LeaderboardEntryDTO::getExpeditionTimeToBurnToCommitteeBoat),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_TIME_TO_BURN_TO_PIN,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_TIME_TO_BURN_TO_PIN,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionTimeToBurnToPin),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT,
                            new DoubleTextRaceDetailTypeExtractor(
                                    LeaderboardEntryDTO::getExpeditionDistanceToCommitteeBoat),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_DISTANCE_TO_PIN,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_DISTANCE_TO_PIN,
                            new DoubleTextRaceDetailTypeExtractor(
                                    LeaderboardEntryDTO::getExpeditionDistanceToPinDetail),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_DISTANCE_BELOW_LINE,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_DISTANCE_BELOW_LINE,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionDistanceBelowLine),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION,
                            new DoubleTextRaceDetailTypeExtractor(
                                    LeaderboardEntryDTO::getExpeditionLineSquareForWindDirection),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_BARO,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_BARO,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionBaro),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_LOAD_S,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_LOAD_S,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionLoadS),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_LOAD_P,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_LOAD_P,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionLoadP),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_JIB_CAR_PORT,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_JIB_CAR_PORT,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionJibCarPort),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_JIB_CAR_STBD,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_JIB_CAR_STBD,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionJibCarStbd),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.EXPEDITION_RACE_MAST_BUTT,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.EXPEDITION_RACE_MAST_BUTT,
                            new DoubleTextRaceDetailTypeExtractor(LeaderboardEntryDTO::getExpeditionJibCarStbd),
                            LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.BRAVO_RACE_HEEL_IN_DEGREES,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.BRAVO_RACE_HEEL_IN_DEGREES,
                            new BearingAsDegreeDetailTypeExtractor(e -> e.heel), LEG_COLUMN_HEADER_STYLE,
                            LEG_COLUMN_STYLE, LeaderboardPanel.this));
            result.put(DetailType.BRAVO_RACE_PITCH_IN_DEGREES,
                    new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.BRAVO_RACE_PITCH_IN_DEGREES,
                            new BearingAsDegreeDetailTypeExtractor(e -> e.pitch), LEG_COLUMN_HEADER_STYLE,
                            LEG_COLUMN_STYLE, LeaderboardPanel.this));
            return result;
        }
        
        private class BearingAsDegreeDetailTypeExtractor extends DoubleTextRaceDetailTypeExtractor {
            public BearingAsDegreeDetailTypeExtractor(Function<LeaderboardEntryDTO, Bearing> valueExtractor) {
                super(entry -> {
                    Bearing bearing = valueExtractor.apply(entry);
                    return bearing == null ? null : bearing.getDegrees();
                });
            }
        }
        
        private class DoubleTextRaceDetailTypeExtractor implements DataExtractor<Double, LeaderboardRowDTO> {
            
            private final Function<LeaderboardEntryDTO, Double> valueExtractor;

            public DoubleTextRaceDetailTypeExtractor(Function<LeaderboardEntryDTO, Double> valueExtractor) {
                this.valueExtractor = valueExtractor;
            }
            
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = valueExtractor.apply(fieldsForRace);
                }
                return result;
            }
        }

        @Override
        protected Iterable<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getDirectChildren() {
            List<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> result = new ArrayList<>();
            for (AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> column : super.getDirectChildren()) {
                result.add(column);
            }
            if (isExpanded() && getLeaderboard().canBoatsOfCompetitorsChangePerRace && selectedRaceDetails.contains(DetailType.RACE_DISPLAY_BOATS)) {
                if (boatInfoColumn == null) {
                    BoatFetcher<LeaderboardRowDTO> boatFetcher = (LeaderboardRowDTO row) -> getLeaderboard()
                            .getBoatOfCompetitor(getRaceColumnName(), row.competitor);
                    boatInfoColumn = new BoatInfoColumn<LeaderboardRowDTO>(boatFetcher, style);
                }
                result.add(boatInfoColumn);
            }
            if (isExpanded() && selectedRaceDetails.contains(DetailType.RACE_DISPLAY_LEGS)) {
                // it is important to re-use existing LegColumn objects because
                // removing the columns from the table
                // is based on column identity
                int maxLegCount = getLegCount(getLeaderboard(), getRaceColumnName());
                if (maxLegCount != -1) {
                    for (int i = 0; i < maxLegCount; i++) {
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
                result = new LegColumn(LeaderboardPanel.this, getRaceColumnName(), legNumber, SortingOrder.ASCENDING,
                        stringMessages, Collections.unmodifiableList(selectedLegDetails), LEG_COLUMN_HEADER_STYLE,
                        LEG_COLUMN_STYLE, LEG_DETAIL_COLUMN_HEADER_STYLE, LEG_DETAIL_COLUMN_STYLE);
                while (legColumns.size() <= legNumber) {
                    legColumns.add(null);
                }
                legColumns.set(legNumber, result);
            }
            return result;
        }

        /**
         * Reports the ratio of the time that passed since the last position fix and the average sampling interval of
         * the competitor's track. On a perfect track, this value will never exceed 1.0. Immediately when a fix is
         * received, this value goes to 0.0. For a competitor whose tracker is lagging, this value can grow considerably
         * greater than 1. The value goes to <code>null</code> if there are no fixes in the track.
         * 
         * @author Axel Uhl (D043530)
         *
         */
        private class RaceRatioBetweenTimeSinceLastPositionFixAndAverageSamplingInterval
                implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null && fieldsForRace.timeSinceLastPositionFixInSeconds != null
                        && fieldsForRace.averageSamplingInterval != null) {
                    result = fieldsForRace.timeSinceLastPositionFixInSeconds
                            / fieldsForRace.averageSamplingInterval.asSeconds();
                }
                return result;
            }
        }

        /**
         * Accumulates the average speed over all legs of a race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceAverageSpeedInKnots implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                final Distance distanceTraveledInMeters = row.getDistanceTraveled(getRaceColumnName());
                final Duration time = row.getTimeSailed(getRaceColumnName());
                final Double result;
                if (distanceTraveledInMeters != null && time != null) {
                    result = distanceTraveledInMeters.inTime(time).getKnots();
                } else {
                    result = null;
                }
                return result;
            }
        }

        /**
         * Fetches the average absolute (distance always counted as positive, no matter whether left or right)
         * cross-track error for the race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceCurrentSpeedOverGroundInKnots implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.currentSpeedAndCourseOverGround == null ? null :
                        fieldsForRace.currentSpeedAndCourseOverGround.getKnots();
                }
                return result;
            }
        }

        /**
         * Fetches the average absolute (distance always counted as positive, no matter whether left or right)
         * cross-track error for the race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceCurrentCourseOverGroundInTrueDegrees implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.currentSpeedAndCourseOverGround == null ? null :
                        fieldsForRace.currentSpeedAndCourseOverGround.getBearing().getDegrees();
                }
                return result;
            }
        }

        /**
         * Fetches the average absolute (distance always counted as positive, no matter whether left or right)
         * cross-track error for the race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceAverageAbsoluteCrossTrackErrorInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.averageAbsoluteCrossTrackErrorInMeters;
                }
                return result;
            }
        }

        /**
         * Fetches the average signed (right course side is positive, left course side is negative) cross-track error
         * for the race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceAverageSignedCrossTrackErrorInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.averageSignedCrossTrackErrorInMeters;
                }
                return result;
            }
        }

        /**
         * Fetches the competitor's distance to the start mark at the time the race started
         * 
         * @author Axel Uhl (D043530)
         */
        private class DistanceToStartAtRaceStartInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.distanceToStartLineAtStartOfRaceInMeters;
                }
                return result;
            }
        }

        /**
         * Fetches the time between start of race and the competitors start mark passing, telling how long after the gun
         * the competitor actually started.
         * 
         * @author Axel Uhl (D043530)
         */
        private class TimeBetweenRaceStartAndCompetitorStartInSeconds implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.timeBetweenRaceStartAndCompetitorStartInSeconds;
                }
                return result;
            }
        }

        /**
         * Fetches the competitor's speed over ground at the time the race started
         * 
         * @author Axel Uhl (D043530)
         */
        private class SpeedOverGroundAtRaceStartInKnots implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.speedOverGroundAtStartOfRaceInKnots;
                }
                return result;
            }
        }

        /**
         * Fetches the competitor's distance to the start mark five seconds before the race started
         */
        private class DistanceToStartFiveSecondsBeforeStartInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.distanceToStartLineFiveSecondsBeforeStartInMeters;
                }
                return result;
            }
        }

        /**
         * Fetches the competitor's speed over ground five seconds before the race started
         */
        private class SpeedFiveSecondsBeforeStartInKnots implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.speedOverGroundFiveSecondsBeforeStartInKnots;
                }
                return result;
            }
        }

        /**
         * Fetches the competitor's speed over ground at the time the competitor passed the start
         * 
         * @author Axel Uhl (D043530)
         */
        private class SpeedOverGroundWhenPassingStartInKnots implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.speedOverGroundAtPassingStartWaypointInKnots;
                }
                return result;
            }
        }

        /**
         * Fetches the competitor's distance to the starboard side of the start line when competitor passed the start.
         * If the start waypoint is not a gate/line, the distance to the single buoy is used.
         * 
         * @author Axel Uhl (D043530)
         */
        private class DistanceToStarboardSideOfStartLineInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.distanceToStarboardSideOfStartLineInMeters;
                }
                return result;
            }
        }

        /**
         * Fetches the competitor's speed over ground at the time the competitor passed the start
         * 
         * @author Axel Uhl (D043530)
         */
        private class TackWhenStarting implements DataExtractor<Tack, LeaderboardRowDTO> {
            @Override
            public Tack get(LeaderboardRowDTO row) {
                Tack result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.startTack;
                }
                return result;
            }
        }

        /**
         * Accumulates the distance traveled over all legs of a race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceDistanceTraveledInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Distance distanceForRaceColumn = row.getDistanceTraveled(getRaceColumnName());
                return distanceForRaceColumn == null ? null : distanceForRaceColumn.getMeters();
            }
        }

        /**
         * Accumulates the distance foiled over all legs of a race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceDistanceFoiledInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Distance distanceForRaceColumn = row.getDistanceFoiled(getRaceColumnName());
                return distanceForRaceColumn == null ? null : distanceForRaceColumn.getMeters();
            }
        }

        /**
         * Accumulates the duration foiled over all legs of a race
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceDurationFoiledInSeconds implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Duration durationForRaceColumn = row.getDurationFoiled(getRaceColumnName());
                return durationForRaceColumn == null ? null : durationForRaceColumn.asSeconds();
            }
        }

        private class RaceCalculatedTimeTraveledInSeconds implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.calculatedTime == null ? null : fieldsForRace.calculatedTime.asSeconds();
                }
                return result;
            }
        }

        private class RaceImpliedWindInKnots implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.impliedWind == null ? null : fieldsForRace.impliedWind.getKnots();
                }
                return result;
            }
        }

        private class RaceCalculatedTimeAtEstimatedArrivalAtCompetitorFarthestAheadInSeconds
                implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead == null ? null
                            : fieldsForRace.calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead.asSeconds();
                }
                return result;
            }
        }

        /**
         * Accumulates the distance traveled over all legs of a race and considers the specifics of a gate start. The
         * first leg of a gate start gets as additional distance traveled the distance from the port side of the line to
         * the point where the competitor started which helps normalize the distance traveled and make them comparable
         * across early and late starters.
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceDistanceTraveledIncludingGateStartInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null && fieldsForRace.legDetails != null) {
                    for (LegEntryDTO legDetail : fieldsForRace.legDetails) {
                        if (legDetail != null) {
                            if (legDetail.distanceTraveledIncludingGateStartInMeters != null) {
                                if (result == null) {
                                    result = 0.0;
                                }
                                result += legDetail.distanceTraveledIncludingGateStartInMeters;
                            }
                        }
                    }
                }
                return result;
            }
        }

        private class CurrentLeg implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    int currentLeg = fieldsForRace.getOneBasedCurrentLegNumber();
                    result = currentLeg == 0 ? null : (double) currentLeg;
                }
                return result;
            }
        }

        /**
         * Computes the gap to leader exploiting the ordering of the leg detail columns
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceGapToLeaderInSeconds implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.gapToLeaderInOwnTime == null ? null
                            : fieldsForRace.gapToLeaderInOwnTime.asSeconds();
                }
                return result;
            }
        }
        
        private class PercentTargetBoatSpeed implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                final LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.percentTargetBoatSpeed == null ? null : fieldsForRace.percentTargetBoatSpeed;
                }
                return result;
            }
        }

        /**
         * Computes the windward distance to the competitor farthest ahead in the race (not necessarily the leader with
         * handicap scoring) in meters
         * 
         * @author Axel Uhl (D043530)
         */
        private class RaceDistanceToCompetitorFarthestAheadInMeters implements DataExtractor<Double, LeaderboardRowDTO> {
            @Override
            public Double get(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null && fieldsForRace.windwardDistanceToCompetitorFarthestAheadInMeters != null) {
                    result = fieldsForRace.windwardDistanceToCompetitorFarthestAheadInMeters;
                }
                return result;
            }
        }

        private abstract class AbstractLastLegDetailField<T extends Comparable<?>> extends com.sap.sailing.gwt.ui.leaderboard.AbstractLastLegDetailField<T> {
            @Override
            public String getRaceColumnName() {
                return TextRaceColumn.this.getRaceColumnName();
            }
        }

        private class RaceCurrentRideHeightInMeters extends AbstractLastLegDetailField<Double> {
            @Override
            protected Double getBeforeLastLegFinished(LegEntryDTO currentLegDetail) {
                return currentLegDetail.currentRideHeightInMeters;
            }

            @Override
            protected Double getAfterLastLegFinished(LeaderboardRowDTO row) {
                Double result = null;
                LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
                if (fieldsForRace != null) {
                    result = fieldsForRace.averageRideHeightInMeters;
                }
                return result;
            }
        }
    }

    /**
     * Displays the net points totals for a competitor for the entire leaderboard.
     * 
     * @author Axel Uhl (D043530)
     * 
     */
    private class TotalNetPointsColumn extends LeaderboardSortableColumnWithMinMax<LeaderboardRowDTO, String> {
        private final String columnStyle;

        protected TotalNetPointsColumn(String columnStyle) {
            super(new TextCell(), SortingOrder.ASCENDING, LeaderboardPanel.this);
            this.columnStyle = columnStyle;
            setHorizontalAlignment(ALIGN_CENTER);
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            Double netPoints = object.netPoints;
            return "" + (netPoints == null ? "" : scoreFormat.format(netPoints));
        }

        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder sb) {
            String textColor = getLeaderboard().hasLiveRace(timer.getLiveTimePointInMillis()) ? IS_LIVE_TEXT_COLOR
                    : DEFAULT_TEXT_COLOR;

            sb.appendHtmlConstant("<span style=\"font-weight: bold; color:" + textColor + "\">");
            sb.appendEscaped(getValue(object));
            sb.appendHtmlConstant("</span>");
        }

        @Override
        public InvertibleComparator<LeaderboardRowDTO> getComparator() {
            return new InvertibleComparatorAdapter<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    return getLeaderboard().competitors.indexOf(o1.competitor)
                            - getLeaderboard().competitors.indexOf(o2.competitor);
                }
            };
        }

        @Override
        public String getColumnStyle() {
            return columnStyle;
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return new SafeHtmlHeaderWithTooltip(SafeHtmlUtils.fromString(stringMessages.total()),
                    stringMessages.totalNetPointsColumnTooltip());
        }
    }

    protected class CarryColumn extends LeaderboardSortableColumnWithMinMax<LeaderboardRowDTO, LeaderboardRowDTO> {
        public CarryColumn() {
            super(new AbstractSafeHtmlCell<LeaderboardRowDTO>(new AbstractSafeHtmlRenderer<LeaderboardRowDTO>() {
                @Override
                public SafeHtml render(LeaderboardRowDTO object) {
                    return new SafeHtmlBuilder()
                            .appendEscaped(object.carriedPoints == null ? "" : scoreFormat.format(object.carriedPoints))
                            .toSafeHtml();
                }
            }) {
                @Override
                protected void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml data,
                        SafeHtmlBuilder sb) {
                    sb.append(data);
                }
            }, SortingOrder.ASCENDING, LeaderboardPanel.this);
            setSortable(true);
        }

        protected CarryColumn(Cell<LeaderboardRowDTO> cell) {
            super(cell, SortingOrder.ASCENDING, LeaderboardPanel.this);
            setSortable(true);
        }

        @Override
        public LeaderboardRowDTO getValue(LeaderboardRowDTO object) {
            return object;
        }

        @Override
        public InvertibleComparator<LeaderboardRowDTO> getComparator() {
            return new InvertibleComparatorAdapter<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    Double o1CarriedPoints = o1.carriedPoints;
                    if (o1CarriedPoints == null) {
                        o1CarriedPoints = 0.0;
                    }
                    Double o2CarriedPoints = o2.carriedPoints;
                    if (o2CarriedPoints == null) {
                        o2CarriedPoints = 0.0;
                    }
                    return o1CarriedPoints.compareTo(o2CarriedPoints);
                }
            };
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return new SafeHtmlHeaderWithTooltip(SafeHtmlUtils.fromString(stringMessages.carry()),
                    stringMessages.carryColumnTooltip());
        }
    }

    private class LeaderboardSelectionCheckboxColumn
            extends com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn<LeaderboardRowDTO>
            implements CompetitorSelectionChangeListener {
        protected LeaderboardSelectionCheckboxColumn(final CompetitorSelectionProvider competitorSelectionProvider) {
            super(style.getTableresources().cellTableStyle().cellTableCheckboxSelected(),
                    style.getTableresources().cellTableStyle().cellTableCheckboxDeselected(),
                    style.getTableresources().cellTableStyle().cellTableCheckboxColumnCell(),
                    new EntityIdentityComparator<LeaderboardRowDTO>() {
                        @Override
                        public boolean representSameEntity(LeaderboardRowDTO dto1, LeaderboardRowDTO dto2) {
                            return dto1.competitor.getIdAsString().equals(dto2.competitor.getIdAsString());
                        }

                        @Override
                        public int hashCode(LeaderboardRowDTO t) {
                            return t.competitor.getIdAsString().hashCode();
                        }
                    }, getData(), leaderboardTable);
            competitorSelectionProvider.addCompetitorSelectionChangeListener(this);
        }

        @Override
        public Boolean getValue(LeaderboardRowDTO row) {
            return competitorSelectionProvider.isSelected(row.competitor);
        }

        @Override
        public void competitorsListChanged(Iterable<CompetitorDTO> competitors) {
        }

        @Override
        public void filterChanged(FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> oldFilterSet,
                FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> newFilterSet) {
        }

        @Override
        public void filteredCompetitorsListChanged(Iterable<CompetitorDTO> filteredCompetitors) {
        }

        /**
         * Ensure that the checkbox is redrawn when the competitor selection changes
         */
        @Override
        public void addedToSelection(CompetitorDTO competitor) {
            final LeaderboardRowDTO row = getRow(competitor.getIdAsString());
            if (row != null) {
                getSelectionModel().setSelected(row, true);
            }
        }

        /**
         * Ensure that the checkbox is redrawn when the competitor selection changes
         */
        @Override
        public void removedFromSelection(CompetitorDTO competitor) {
            final LeaderboardRowDTO row = getRow(competitor.getIdAsString());
            if (row != null) {
                getSelectionModel().setSelected(row, false);
            }
        }

        @Override
        public void updateMinMax() {
        }
    }

    private class TotalRankColumn extends LeaderboardSortableColumnWithMinMax<LeaderboardRowDTO, String> {
        public TotalRankColumn() {
            super(new TextCell(), SortingOrder.ASCENDING, LeaderboardPanel.this);
            setHorizontalAlignment(ALIGN_CENTER);
            setSortable(true);
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            final int totalRank = getLeaderboard().getTotalRank(object.competitor);
            return "" + (totalRank == 0 ? "" : totalRank);
        }

        @Override
        public InvertibleComparator<LeaderboardRowDTO> getComparator() {
            return new InvertibleComparatorAdapter<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    final int totalRank1 = getLeaderboard().getTotalRank(o1.competitor);
                    final int totalRank2 = getLeaderboard().getTotalRank(o2.competitor);
                    return totalRank1 == 0 ? totalRank2 == 0 ? 0 : 1 : totalRank2 == 0 ? -1 : totalRank1 - totalRank2;
                }
            };
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return new SafeHtmlHeaderWithTooltip(SafeHtmlUtils.fromString(stringMessages.totalRegattaRank()),
                    stringMessages.rankColumnTooltip());
        }

    }

    private class StartingTackColumn extends DetailTypeColumn<Tack, String, LeaderboardRowDTO> {
        public StartingTackColumn(DataExtractor<Tack, LeaderboardRowDTO> field, String headerStyle,
                String columnStyle) {
            super(DetailType.START_TACK, field, new TextCell(), headerStyle, columnStyle, LeaderboardPanel.this);
        }

        @Override
        public String getValue(LeaderboardRowDTO row) {
            return getField().get(row) == null ? null
                    : getField().get(row) == Tack.PORT ? stringMessages.portTack() : stringMessages.starboardTack();
        }
    }

    private Widget createFilterDeselectionControl() {
        filterStatusLabel = new Label();
        filterStatusLabel.setStyleName("LeaderboardPanel-FilterControl-StatusLabel");
        filterStatusLabel.setText("");
        filterControlPanel.add(filterStatusLabel);
        filterClearButton = new Button(stringMessages.showAll());
        filterClearButton.setStyleName("LeaderboardPanel-FilterClear-Button");
        filterClearButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                competitorFilterPanel.clearAllActiveFilters();
                loadCompleteLeaderboard(/* showProgress */ true);
            }
        });
        filterControlPanel.add(filterClearButton);
        filterControlPanel.setCellHorizontalAlignment(filterClearButton, HasHorizontalAlignment.ALIGN_RIGHT);
        setFilterControlStatus();
        return filterControlPanel;
    }

    public abstract int getLegCount(LeaderboardDTO leaderboardDTO, String raceColumnName);

    private void setFilterControlStatus() {
        if (showCompetitorFilterStatus) {
            boolean filtersActive = competitorSelectionProvider.hasActiveFilters();
            if (filtersActive) {
                String labelText = "";
                for (Filter<CompetitorDTO> filter : competitorSelectionProvider.getCompetitorsFilterSet().getFilters()) {
                    if (filter instanceof FilterWithUI<?>) {
                        labelText += ((FilterWithUI<CompetitorDTO>) filter).getLocalizedDescription(stringMessages) + ", ";
                    } else {
                        labelText += filter.getName() + ", ";
                    }
                }
                filterStatusLabel.setText(stringMessages.activeFilters(labelText.substring(0, labelText.length() - 2)));
                filterClearButton.setVisible(true);
                filterControlPanel.setVisible(true);
            } else {
                filterStatusLabel.setText("");
                filterClearButton.setVisible(false);
                filterControlPanel.setVisible(false);
                if (competitorFilterPanel != null) {
                    competitorFilterPanel.clearSelection();
                }
            }
        }
    }

    private static class TimeOnDistanceAllowanceInSecondsPerNauticalMileColumn implements DataExtractor<Double, LeaderboardRowDTO> {
        @Override
        public Double get(LeaderboardRowDTO row) {
            return row.effectiveTimeOnDistanceAllowancePerNauticalMile == null ? null
                    : row.effectiveTimeOnDistanceAllowancePerNauticalMile.asSeconds();
        }
    }

    private static class TotalAverageSpeedOverGroundField implements DataExtractor<Double, LeaderboardRowDTO> {
        @Override
        public Double get(LeaderboardRowDTO row) {
            final Double result;
            if (row.totalDistanceTraveledInMeters != null && row.totalTimeSailedInSeconds != null
                    && row.totalTimeSailedInSeconds != 0.0) {
                result = row.totalDistanceTraveledInMeters / row.totalTimeSailedInSeconds
                        / Mile.METERS_PER_NAUTICAL_MILE * 3600;
            } else {
                result = null;
            }
            return result;
        }
    }

    private Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> createOverallDetailColumnMap() {
        Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> result = new HashMap<>();
        result.put(DetailType.OVERALL_TOTAL_DISTANCE_TRAVELED,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.OVERALL_TOTAL_DISTANCE_TRAVELED,
                        e -> e.totalDistanceTraveledInMeters, RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE, this));
        result.put(DetailType.OVERALL_TOTAL_AVERAGE_SPEED_OVER_GROUND,
            new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.OVERALL_TOTAL_AVERAGE_SPEED_OVER_GROUND,
                    new TotalAverageSpeedOverGroundField(), RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE, this));
        result.put(DetailType.OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS,
                new MaxSpeedOverallColumn(RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE, this));
        result.put(DetailType.OVERALL_TOTAL_TIME_SAILED_IN_SECONDS, createOverallTimeTraveledColumn());
        result.put(DetailType.OVERALL_TOTAL_DURATION_FOILED_IN_SECONDS,
                new TotalTimeColumn(DetailType.OVERALL_TOTAL_DURATION_FOILED_IN_SECONDS,
                        e -> e.totalDurationFoiledInSeconds, RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE, this));
        result.put(DetailType.OVERALL_TOTAL_DISTANCE_FOILED_IN_METERS,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.OVERALL_TOTAL_DISTANCE_FOILED_IN_METERS,
                        e -> e.totalDistanceFoiledInMeters, RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE, this));
        result.put(DetailType.OVERALL_TIME_ON_TIME_FACTOR, new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.OVERALL_TIME_ON_TIME_FACTOR,
                e -> e.effectiveTimeOnTimeFactor, RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE, this));
        result.put(DetailType.OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE,
                new FormattedDoubleLeaderboardRowDTODetailTypeColumn(DetailType.OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE,
                        new TimeOnDistanceAllowanceInSecondsPerNauticalMileColumn(), RACE_COLUMN_HEADER_STYLE,
                        RACE_COLUMN_STYLE, this));
        result.put(DetailType.OVERALL_TOTAL_SCORED_RACE_COUNT, new IntegerDetailTypeColumn(DetailType.OVERALL_TOTAL_SCORED_RACE_COUNT,
                e -> e.totalScoredRaces, RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE, this));
        return result;
    }

    private OverallTimeTraveledColumn createOverallTimeTraveledColumn() {
        return new OverallTimeTraveledColumn(this, stringMessages, RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE,
                LEG_COLUMN_HEADER_STYLE, LEG_COLUMN_STYLE);
    }

    /**
     * Largely for subclasses to add more stuff to this panel, such as more toolbar buttons.
     */
    protected HorizontalPanel getRefreshAndSettingsPanel() {
        return refreshAndSettingsPanel;
    }

    protected RaceColumn<?> getRaceColumnByRaceName(String raceName) {
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> column = getLeaderboardTable().getColumn(i);
            if (column instanceof LeaderboardPanel.RaceColumn) {
                RaceColumnDTO raceInLeaderboard = ((RaceColumn<?>) column).getRace();
                for (FleetDTO fleet : raceInLeaderboard.getFleets()) {
                    final RegattaAndRaceIdentifier raceIdentifier = raceInLeaderboard.getRaceIdentifier(fleet);
                    if (raceIdentifier != null && raceIdentifier.getRaceName().equals(raceName)) {
                        return (RaceColumn<?>) column;
                    }
                }
            }
        }
        return null;
    }

    private RaceColumn<?> getRaceColumnByRaceColumnName(String raceColumnName) {
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> column = getLeaderboardTable().getColumn(i);
            if (column instanceof LeaderboardPanel.RaceColumn) {
                if (((RaceColumn<?>) column).getRaceColumnName().equals(raceColumnName)) {
                    return (RaceColumn<?>) column;
                }
            }
        }
        return null;
    }

    private SafeHtml getPlayPauseImgHtml(PlayStates playState) {
        if (playState == PlayStates.Playing) {
            return AbstractImagePrototype.create(pauseIcon).getSafeHtml();
        } else {
            return AbstractImagePrototype.create(playIcon).getSafeHtml();
        }
    }

    private void setDelayInMilliseconds(long delayInMilliseconds) {
        // If the timer is currently in a mode that causes null to be used as the leaderboard request time stamp,
        // don't let a live play delay change cause another load request by unregistering this panel as timer
        // listener before and re-registering it after adjusting the live play delay.
        if (useNullAsTimePoint()) {
            timer.removeTimeListener(this);
        }
        timer.setLivePlayDelayInMillis(delayInMilliseconds);
        if (useNullAsTimePoint()) {
            timer.addTimeListener(this);
        }
    }

    protected boolean isAutoExpandLastRaceColumn() {
        return autoExpandLastRaceColumn;
    }

    protected boolean isShowAddedScores() {
        return showAddedScores;
    }

    private void setShowAddedScores(boolean showAddedScores) {
        this.showAddedScores = showAddedScores;
    }

    protected boolean isShowCompetitorShortName() {
        return showCompetitorShortName;
    }

    private void setShowCompetitorShortName(boolean showCompetitorShortName) {
        this.showCompetitorShortName = showCompetitorShortName;
    }

    protected boolean isShowCompetitorFullName() {
        return showCompetitorFullName;
    }

    private void setShowCompetitorFullName(boolean showCompetitorFullName) {
        this.showCompetitorFullName = showCompetitorFullName;
    }

    protected boolean isShowCompetitorBoatInfo() {
        return showCompetitorBoatInfo;
    }

    protected abstract boolean canShowCompetitorBoatInfo();
    
    private void setShowCompetitorBoatInfo(boolean showCompetitorBoatInfo) {
        this.showCompetitorBoatInfo = showCompetitorBoatInfo;
    }

    /**
     * The time point for which the leaderboard currently shows results. In {@link PlayModes#Replay replay mode} this is
     * the {@link #timer}'s time point. In {@link PlayModes#Live live mode} the {@link #timer}'s time is quantized to
     * the closest full second to increase the likelihood of cache hits in the back end.
     */
    protected Date getLeaderboardDisplayDate() {
        return timer.getTime();
    }

    /**
     * adds the <code>column</code> to the right end of the {@link #getLeaderboardTable() leaderboard table} and sets
     * the column style according to the {@link LeaderboardSortableColumnWithMinMax#getColumnStyle() column's style
     * definition}.
     */
    protected void addColumn(AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> column) {
        leaderboardTable.addColumn(column, column.getHeader(), column.getComparator(),
                column.getPreferredSortingOrder().isAscending());
        String columnStyle = column.getColumnStyle();
        if (columnStyle != null) {
            getLeaderboardTable().addColumnStyleName(getLeaderboardTable().getColumnCount() - 1, columnStyle);
        }
    }

    protected void insertColumn(int beforeIndex, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> column) {
        // remove column styles of those columns whose index will shift right by
        // one:
        removeColumnStyles(beforeIndex);
        getLeaderboardTable().insertColumn(beforeIndex, column, column.getHeader(), column.getComparator(),
                column.getPreferredSortingOrder().isAscending());
        addColumnStyles(beforeIndex);
    }

    private void addColumnStyles(int startColumn) {
        for (int i = startColumn; i < getLeaderboardTable().getColumnCount(); i++) {
            AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> columnToRemoveStyleFor = (AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>) getLeaderboardTable()
                    .getColumn(i);
            String columnStyle = columnToRemoveStyleFor.getColumnStyle();
            if (columnStyle != null) {
                getLeaderboardTable().addColumnStyleName(i, columnStyle);
            }
        }
    }

    private void removeColumnStyles(int startColumn) {
        for (int i = startColumn; i < getLeaderboardTable().getColumnCount(); i++) {
            AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> columnToRemoveStyleFor = (AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>) getLeaderboardTable()
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
                expandableColumn.changeExpansionState(/* expand */ false);
                // them remember that column c was expanded:
                expandableColumn.setExpanded(true);
            }
        }
        removeColumnStyles(/* startColumn */columnIndex);
        getLeaderboardTable().removeColumn(columnIndex);
        addColumnStyles(/* startColumn */columnIndex);
    }

    /**
     * Looks up the column {@code c} in the {@link #getLeaderboardTable() leaderboard table} and if found,
     * removes it. If not found, nothing happens.
     */
    protected void removeColumn(Column<LeaderboardRowDTO, ?> c) {
        int columnIndex = getLeaderboardTable().getColumnIndex(c);
        if (columnIndex != -1) {
            removeColumn(columnIndex);
        }
    }

    abstract protected AsyncAction<LeaderboardDTO> getRetrieverAction();

    public void loadCompleteLeaderboard(final boolean showProgress) {
        if (needsDataLoading()) {
            if (showProgress) {
                addBusyTask();
            }

            AsyncAction<LeaderboardDTO> getLeaderboardByNameAction = getRetrieverAction();
            this.asyncActionsExecutor.execute(getLeaderboardByNameAction, LOAD_LEADERBOARD_DATA_CATEGORY,
                    new AsyncCallback<LeaderboardDTO>() {
                        @Override
                        public void onSuccess(LeaderboardDTO result) {
                            try {
                                final boolean wasEmptyRaceColumnSelection = Util
                                        .isEmpty(raceColumnSelection.getSelectedRaceColumns());
                                updateLeaderboard(result);
                                // This constructs a new settings object with juse the default for namesOfRaceColumnsToShow being adjusted.
                                // In case the namesOfRaceColumnsToShow setting isn't changed this will also cause the value to change.
                                // This causes in consequence potentiallyChangedSettings to not be equal to currentSettings anymore.
                                // We then apply the new settings to make all new race columns visible.
                                // TODO check if there is an easier way to get to know if we need to reapply the settings.
                                LS potentiallyChangedSettings = overrideDefaultsForNamesOfRaceColumns(currentSettings,
                                        result);
                                // reapply, when this is the first time we received the race columns or if columns have
                                // changed
                                if (wasEmptyRaceColumnSelection
                                        || !potentiallyChangedSettings.equals(currentSettings)) {
                                    updateSettings(potentiallyChangedSettings);
                                }
                            } finally {
                                if (showProgress) {
                                    removeBusyTask();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            if (showProgress) {
                                removeBusyTask();
                            }
                            getErrorReporter().reportError(
                                    "Error trying to obtain leaderboard contents: " + caught.getMessage(),
                                    true /* silentMode */);
                        }
                    });
        }
    }

    protected abstract LS overrideDefaultsForNamesOfRaceColumns(LS currentSettings, LeaderboardDTO result);

    protected boolean isFillTotalPointsUncorrected() {
        return false;
    }

    /**
     * In {@link PlayModes#Live live mode}, when {@link #loadCompleteLeaderboard(boolean) loading the leaderboard
     * contents}, <code>null</code> is used as time point. The condition for this is encapsulated in this method so
     * others can find out. For example, when a time change is signaled due to local offset / delay adjustments, no
     * additional call to {@link #loadCompleteLeaderboard(boolean)} would be required as <code>null</code> will be
     * passed in any case, not being affected by local time offsets.
     */
    protected boolean useNullAsTimePoint() {
        return timer.getPlayMode() == PlayModes.Live;
    }

    private boolean needsDataLoading() {
        return isVisible();
    }

    /**
     * Determine from column expansion state which races need their leg details
     */
    protected Collection<String> getNamesOfExpandedRaceColumns() {
        Collection<String> namesOfExpandedRaceColumns = new ArrayList<String>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> column = getLeaderboardTable().getColumn(i);
            if (column instanceof LeaderboardPanel.RaceColumn) {
                RaceColumn<?> raceColumn = (RaceColumn<?>) column;
                if (raceColumn.isExpanded()) {
                    namesOfExpandedRaceColumns.add(raceColumn.getRaceColumnName());
                }
            }
        }
        return namesOfExpandedRaceColumns;
    }

    protected boolean shallAddOverallDetails() {
        return !selectedOverallDetailColumns.isEmpty();
    }

    /**
     * Assigns <code>leaderboard</code> to {@link #leaderboard} and updates the UI accordingly. Also updates the min/max
     * values on the columns.
     */
    public void updateLeaderboard(LeaderboardDTO leaderboard) {
        if (leaderboard != null) {
            Collection<RaceColumn<?>> columnsToCollapseAndExpandAgain = getExpandedRaceColumnsWhoseDisplayedLegCountChanged(
                    leaderboard);
            for (RaceColumn<?> columnToCollapseAndExpandAgain : columnsToCollapseAndExpandAgain) {
                columnToCollapseAndExpandAgain.changeExpansionState(/* expand */ false);
            }
            updateCompetitors(leaderboard);
            if (!initialCompetitorFilterHasBeenApplied) {
                applyTop30FilterIfCompetitorSizeGreaterEqual40(leaderboard);
                initialCompetitorFilterHasBeenApplied = true;
            }
            raceColumnSelection.autoUpdateRaceColumnSelectionForUpdatedLeaderboard(getLeaderboard(), leaderboard);
            setLeaderboard(leaderboard);
            adjustColumnLayout(leaderboard);
            updateRaceColumnDTOsToRaceColumns(leaderboard);
            for (RaceColumn<?> columnToCollapseAndExpandAgain : columnsToCollapseAndExpandAgain) {
                columnToCollapseAndExpandAgain.changeExpansionState(/* expand */ true);
            }
            adjustDelayToLive();
            final Map<CompetitorDTO, LeaderboardRowDTO> rowsToDisplay = getRowsToDisplay();
            Set<LeaderboardRowDTO> rowsToAdd = new HashSet<>(rowsToDisplay.values());
            Map<Integer, LeaderboardRowDTO> rowsToUpdate = new HashMap<>();
            synchronized (getData().getList()) {
                int index = 0;
                for (Iterator<LeaderboardRowDTO> i = getData().getList().iterator(); i.hasNext();) {
                    LeaderboardRowDTO oldRow = i.next();
                    LeaderboardRowDTO newRow = rowsToDisplay.get(oldRow.competitor);
                    if (newRow != null) {
                        rowsToUpdate.put(index++, newRow); // update row in place, preserving its selection state
                        rowsToAdd.remove(newRow); // no need to add this row when it was updated in-place
                    } else {
                        i.remove(); // old row's competitor not found in new rows' competitors; remove old row from
                                    // table
                    }
                }
                for (Entry<Integer, LeaderboardRowDTO> updateEntry : rowsToUpdate.entrySet()) {
                    LeaderboardRowDTO oldElement = getData().getList().set(updateEntry.getKey(),
                            updateEntry.getValue());
                    leaderboardSelectionModel.setSelected(oldElement, false); // make sure the old element is no longer
                                                                              // part of the selection
                    updateSelection(updateEntry.getValue());
                }
                for (LeaderboardRowDTO rowToAdd : rowsToAdd) {
                    getData().getList().add(rowToAdd);
                    updateSelection(rowToAdd);
                }
            }
            RaceColumn<?> lastRaceColumn = null;
            for (int i = getLeaderboardTable().getColumnCount() - 1; i >= 0; i--) {
                if (getLeaderboardTable().getColumn(i) instanceof LeaderboardPanel.RaceColumn) {
                    lastRaceColumn = (RaceColumn<?>) getLeaderboardTable().getColumn(i);
                    break;
                }
            }
            for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
                AbstractSortableColumnWithMinMax<?, ?> c = (AbstractSortableColumnWithMinMax<?, ?>) getLeaderboardTable()
                        .getColumn(i);
                c.updateMinMax();
                processAutoExpands(c, lastRaceColumn);
            }
            if (leaderboardTable.getCurrentlySortedColumn() != null) {
                leaderboardTable.sort();
            } else {
                AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> columnToSortFor = getDefaultSortColumn();
                leaderboardTable.sortColumn(columnToSortFor, columnToSortFor.getPreferredSortingOrder().isAscending());
            }

            if (!isEmbedded) {
                updateToolbar(leaderboard);
            }
            informLeaderboardUpdateListenersAboutLeaderboardUpdated(leaderboard);
        }
    }

    protected void updateToolbar(LeaderboardDTO leaderboard) {
        scoreCorrectionCommentLabel.setText(leaderboard.getComment() != null ? leaderboard.getComment() : "");
        if (leaderboard.getTimePointOfLastCorrectionsValidity() != null) {
            Date lastCorrectionDate = leaderboard.getTimePointOfLastCorrectionsValidity();
            String lastUpdate = DateAndTimeFormatterUtil.formatLongDateAndTimeGMT(lastCorrectionDate);
            scoreCorrectionLastUpdateTimeLabel.setText(stringMessages.lastScoreUpdate() + ": " + lastUpdate);
        } else {
            scoreCorrectionLastUpdateTimeLabel.setText("");
        }

        boolean hasLiveRace = !leaderboard.getLiveRaces(timer.getLiveTimePointInMillis()).isEmpty();
        liveRaceLabel.setText(hasLiveRace ? getLiveRacesText() : "");
        scoreCorrectionLastUpdateTimeLabel.setVisible(!hasLiveRace);
        liveRaceLabel.setVisible(hasLiveRace);
    }

    protected abstract void processAutoExpands(AbstractSortableColumnWithMinMax<?, ?> c, RaceColumn<?> lastRaceColumn);

    protected abstract void applyTop30FilterIfCompetitorSizeGreaterEqual40(LeaderboardDTO leaderboard);

    /**
     * Adjusts the row's selection in the {@link #leaderboardSelectionModel} so it matches its selection state in the
     * {@link #competitorSelectionProvider}.
     */
    private void updateSelection(LeaderboardRowDTO row) {
        final boolean shallBeSelected = competitorSelectionProvider.isSelected(row.competitor);
        if (leaderboardAsTableSelectionModelRegistration != null) {
            // suspend selection events while actively adjusting the leaderboardSelectionModel to match the
            // competitorSelectionProvider
            leaderboardAsTableSelectionModelRegistration.removeHandler();
            leaderboardAsTableSelectionModelRegistration = null;
        }
        if (leaderboardSelectionModel.isSelected(row) != shallBeSelected) {
            leaderboardSelectionModel.setSelected(row, shallBeSelected);
        }
        // register the selection change handler again
        leaderboardAsTableSelectionModelRegistration = leaderboardTable.getSelectionModel()
                .addSelectionChangeHandler(selectionChangeHandler);
    }

    /**
     * The race columns hold a now outdated copy of a {@link RaceColumnDTO} which needs to be updated from the
     * {@link LeaderboardDTO} just received
     */
    private void updateRaceColumnDTOsToRaceColumns(LeaderboardDTO leaderboard) {
        for (RaceColumnDTO newRace : leaderboard.getRaceList()) {
            RaceColumn<?> raceColumn = getRaceColumnByRaceColumnName(newRace.getName());
            if (raceColumn != null) {
                raceColumn.setRace(newRace);
            }
        }
    }

    /**
     * Updates the competitors in the competitorSelectionProvider with the competitors received from the {@link LeaderboardDTO}
     */
    protected void updateCompetitors(LeaderboardDTO leaderboard) {
        competitorSelectionProvider.setCompetitors(leaderboard.competitors, /* listenersNotToNotify */this);
    }
    
    /** updates the selected competitors in the competitorSelectionProvider */
    public void setSelection(Iterable<CompetitorDTO> newSelection) {
        competitorSelectionProvider.setSelection(newSelection);
    }

    /**
     * Due to a course change, a race may change its number of legs. All expanded race columns that show leg columns and
     * whose leg count changed need to be collapsed before the leaderboard is replaced, and expanded afterwards again.
     * Race columns whose toggling is {@link ExpandableSortableColumn#isTogglingInProcess() currently in progress} are
     * not considered because their new state will be considered after replacing anyhow.
     * 
     * @param newLeaderboard
     *            the new leaderboard before assigning to {@link #leaderboard}
     * @return the columns that were collapsed in this step and that shall be expanded again after the leaderboard has
     *         been replaced
     */
    private Collection<RaceColumn<?>> getExpandedRaceColumnsWhoseDisplayedLegCountChanged(
            LeaderboardDTO newLeaderboard) {
        Set<RaceColumn<?>> result = new HashSet<RaceColumn<?>>();
        if (selectedRaceDetails.contains(DetailType.RACE_DISPLAY_LEGS)) {
            for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
                Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(i);
                if (c instanceof LeaderboardPanel.RaceColumn) {
                    RaceColumn<?> rc = (RaceColumn<?>) c;
                    // If the new leaderboard no longer contains the column, getLegCount will return -1, causing the
                    // column
                    // to be collapsed if it was expanded. This is correct because otherwise, removing it would no
                    // longer
                    // know the correct leg count.
                    if (!rc.isTogglingInProcess() && rc.isExpanded()) {
                        int oldLegCount = getLegCount(getLeaderboard(), rc.getRaceColumnName());
                        int newLegCount = getLegCount(newLeaderboard, rc.getRaceColumnName());
                        if (oldLegCount != newLegCount) {
                            result.add(rc);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Based on the {@link LeaderboardDTO#getDelayToLiveInMillisForLatestRace()} for the race that has the latest
     * {@link RaceColumnDTO#getStartDate(FleetDTO) start time}, automatically adjusts the delay accordingly unless the
     * {@link #adjustTimerDelay} flag is <code>false</code>
     */
    private void adjustDelayToLive() {
        if (adjustTimerDelay) {
            if (leaderboard.getDelayToLiveInMillisForLatestRace() != null) {
                setDelayInMilliseconds(leaderboard.getDelayToLiveInMillisForLatestRace());
            }
        }
    }

    /**
     * Extracts the rows to display of the <code>leaderboard</code>. These are all {@link AbstractLeaderboardDTO#rows
     * rows} in case {@link #preSelectedRace} is <code>null</code>, or only the rows of the competitors who scored in
     * the race identified by {@link #preSelectedRace} otherwise.
     */
    @Override
    public abstract Map<CompetitorDTO, LeaderboardRowDTO> getRowsToDisplay();

    /**
     * The {@link LeaderboardDTO} holds {@link LeaderboardDTO#getRaceList() races} as {@link RaceColumnDTO} objects.
     * Those map their fleet names to the {@link RegattaAndRaceIdentifier} which identifies the tracked race
     * representing the fleet race within the race column.
     * <p>
     * 
     * On the other hand, a {@link LeaderboardRowDTO} has {@link LeaderboardEntryDTO}, keyed by race column name. The
     * entry DTOs, in turn, store the {@link RaceIdentifier} of the race in which the respective competitor achieved the
     * score.
     * <p>
     * 
     * With this information it is possible to identify the competitors who participated in a particular tracked race,
     * as identified through the <code>race</code> parameter.
     * 
     * @return all competitors for which the {@link #getLeaderboard() leaderboard} has an entry whose
     *         {@link LeaderboardEntryDTO#race} equals <code>race</code>
     */
    public Iterable<CompetitorDTO> getCompetitors(RaceIdentifier race) {
        Set<CompetitorDTO> result = new HashSet<>();
        if (getLeaderboard() != null) {
            for (RaceColumnDTO raceColumn : getLeaderboard().getRaceList()) {
                if (raceColumn.hasTrackedRace(race)) {
                    for (Map.Entry<CompetitorDTO, LeaderboardRowDTO> e : getLeaderboard().rows.entrySet()) {
                        LeaderboardEntryDTO entry = e.getValue().fieldsByRaceColumnName
                                .get(raceColumn.getRaceColumnName());
                        if (entry != null && entry.race != null && entry.race.equals(race)) {
                            result.add(e.getKey());
                        }
                    }
                }
            }
        }
        return result;
    }

    private AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> getDefaultSortColumn() {
        AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> defaultSortColumn = null;
        if (raceNameForDefaultSorting != null) {
            defaultSortColumn = getRaceColumnByRaceName(raceNameForDefaultSorting);
        }
        if (defaultSortColumn == null) {
            defaultSortColumn = getRankColumn();
        }
        return defaultSortColumn;
    }

    private TotalRankColumn getRankColumn() {
        return totalRankColumn;
    }

    protected void setLeaderboard(LeaderboardDTO leaderboard) {
        this.leaderboard = leaderboard;
    }

    @Override
    public LeaderboardDTO getLeaderboard() {
        return leaderboard;
    }

    private void adjustColumnLayout(LeaderboardDTO leaderboard) {
        int columnIndex = 0;
        columnIndex = ensureRaceRankColumn(columnIndex);
        columnIndex = ensureSelectionCheckboxColumn(columnIndex);
        columnIndex = ensureRankColumn(columnIndex);
        columnIndex = ensureCompetitorInfoWithFlagColumnAndCompetitorColumn(columnIndex);
        columnIndex = updateCarryColumn(leaderboard, columnIndex);
        adjustOverallDetailColumns(leaderboard, columnIndex);
        // first remove race columns no longer needed:
        removeUnusedRaceColumns(leaderboard);
        if (leaderboard != null) {
            if (style.hasRaceColumns()) {
                createMissingAndAdjustExistingRaceColumns(leaderboard);
            }
            ensureTotalsColumn();
        }
    }

    // Single leaderboard hook
    protected int ensureRaceRankColumn(int columnIndex) {
        return 0;
    }

    /**
     * Ensures that the columns requested by {@link #selectedOverallDetailColumns} are in the table. Assumes that if
     * there are any existing overall details columns, they start at <code>indexOfFirstOverallDetailsColumn</code> and
     * are in the order defined by {@link #getAvailableOverallDetailColumnTypes()}.
     * 
     * @param indexOfFirstOverallDetailsColumn
     *            tells the column index for the first overall details column
     */
    private void adjustOverallDetailColumns(LeaderboardDTO leaderboard, int indexOfFirstOverallDetailsColumn) {
        List<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> overallDetailColumnsToShow = new ArrayList<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>>();
        // ensure the ordering in overallDetailColumnsToShow conforms to the ordering of
        // getAvailableOverallDetailColumnTypes()
        for (DetailType overallDetailType : DetailType.getAvailableOverallDetailColumnTypes()) {
            if (selectedOverallDetailColumns.contains(overallDetailType)
                    && overallDetailColumnMap.containsKey(overallDetailType)
                    ) {
                overallDetailColumnsToShow.add(overallDetailColumnMap.get(overallDetailType));
            }
        }
        int currentColumnIndex = indexOfFirstOverallDetailsColumn;
        int i = 0; // index into overallDetailColumnToShow
        Column<LeaderboardRowDTO, ?> currentColumn = currentColumnIndex < getLeaderboardTable().getColumnCount()
                ? getLeaderboardTable().getColumn(currentColumnIndex) : null;
        // repeat until no more column to check for removal and no more column left to check for need to insert
        while (i < overallDetailColumnsToShow.size() || overallDetailColumnMap.values().contains(currentColumn)) {
            if (i < overallDetailColumnsToShow.size() && currentColumn == overallDetailColumnsToShow.get(i)) {
                // found selected column in table; all good, advance both "pointers"
                i++;
                currentColumnIndex++;
                currentColumn = getLeaderboardTable().getColumn(currentColumnIndex);
            } else if (i < overallDetailColumnsToShow.size()) {
                // selected column is missing; insert
                insertColumn(currentColumnIndex++, overallDetailColumnsToShow.get(i++));
            } else {
                // based on the while's condition, currentColumn is an overallDetailsColumnMap value;
                // based on the previous if's failed condition, it is not selected. Remove:
                removeColumn(currentColumnIndex);
                currentColumn = getLeaderboardTable().getColumn(currentColumnIndex);
            }
        }
    }

    /**
     * If header information doesn't match the race column's actual state (tracked races attached meaning expandable;
     * medal race), the column is removed and inserted again
     * 
     * @param raceColumn
     *            the raceColumn to correct.
     * @param selectedRaceColumn
     *            the new race column data for <code>raceColumn</code>
     */
    private void correctColumnData(RaceColumn<?> raceColumn, RaceColumnDTO race) {
        int columnIndex = getRaceColumnPosition(raceColumn);
        if (raceColumn.isExpansionEnabled() != race.hasTrackedRaces()
                || race.isMedalRace() != raceColumn.isMedalRace()) {
            if (raceColumn.isExpanded()) {
                raceColumn.changeExpansionState(/* expand */ false); // remove children from table
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
    private void removeRaceColumnFromRaceColumnStartIndexBeforeRace(int raceColumnStartIndex, RaceColumnDTO race) {
        int counter = 0;
        for (int leaderboardposition = 0; leaderboardposition < getLeaderboardTable()
                .getColumnCount(); leaderboardposition++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(leaderboardposition);
            if (c instanceof RaceColumn) {
                RaceColumn<?> raceColumn = (RaceColumn<?>) c;
                if (!raceColumn.getRaceColumnName().equals(race.getRaceColumnName())) {
                    if (raceColumnStartIndex == counter) {
                        removeColumn(raceColumn);
                    }
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
        for (int leaderboardposition = 0; leaderboardposition < getLeaderboardTable()
                .getColumnCount(); leaderboardposition++) {
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
     * This method returns the position where a race column should get inserted.
     * 
     * @param raceName
     *            the name of the race to insert
     * @param listpos
     *            the position of the race in the {@link selectedRaceColumns selectedRaceColumns}
     * @return the position of a race column right before which to insert the race column so that it is the
     *         <code>listpos</code>th race in the table or -1 if no such race column was found, e.g., in case the column
     *         needs to be inserted as the last race in the table
     */
    private int getColumnPositionToInsert(RaceColumnDTO race, int listpos) {
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
     * Removes all columns of type {@link RaceColumn} from the leaderboard table if their name is not in the list of
     * names of the <code>selectedRaceColumns</code>.
     */
    private void removeRaceColumnsNotSelected(Iterable<RaceColumnDTO> selectedRaceColumns) {
        Set<String> selectedRaceColumnNames = new HashSet<String>();
        for (RaceColumnDTO selectedRaceColumn : selectedRaceColumns) {
            selectedRaceColumnNames.add(selectedRaceColumn.getRaceColumnName());
        }
        List<Column<LeaderboardRowDTO, ?>> columnsToRemove = new ArrayList<Column<LeaderboardRowDTO, ?>>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(i);
            if (c instanceof RaceColumn && (leaderboard == null
                    || !selectedRaceColumnNames.contains(((RaceColumn<?>) c).getRaceColumnName()))) {
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
        Iterable<RaceColumnDTO> correctedOrderSelectedRaces = raceColumnSelection
                .getSelectedRaceColumnsOrderedAsInLeaderboard(leaderboard);
        removeRaceColumnsNotSelected(correctedOrderSelectedRaces);
        for (int selectedRaceCount = 0; selectedRaceCount < Util
                .size(correctedOrderSelectedRaces); selectedRaceCount++) {
            RaceColumnDTO selectedRaceColumn = Util.get(correctedOrderSelectedRaces, selectedRaceCount);
            final RaceColumn<?> raceColumn = selectedRaceColumn == null ? null
                    : getRaceColumnByRaceColumnName(selectedRaceColumn.getName());
            if (raceColumn != null) {
                // remove all raceColumns, starting at a specific selectedRaceCount, up to but excluding the selected
                // raceName with the result that selectedRace is at position selectedRaceCount afterwards
                removeRaceColumnFromRaceColumnStartIndexBeforeRace(selectedRaceCount, selectedRaceColumn);
                correctColumnData(raceColumn, selectedRaceColumn);
            } else {
                // get correct position to insert the column
                int positionToInsert = getColumnPositionToInsert(selectedRaceColumn, selectedRaceCount);
                if (positionToInsert != -1) {
                    insertColumn(positionToInsert, createRaceColumn(selectedRaceColumn));
                } else {
                    // Add the raceColumn with addRaceColumn, if no RaceColumn is existing in leaderboard
                    addRaceColumn(createRaceColumn(selectedRaceColumn));
                }
            }
        }
    }

    protected RaceColumn<?> createRaceColumn(RaceColumnDTO raceInLeaderboard) {
        TextRaceColumn textRaceColumn = new TextRaceColumn(raceInLeaderboard, shallExpandRaceColumn(raceInLeaderboard),
                SortingOrder.ASCENDING, RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE);
        return textRaceColumn;
    }

    /**
     * To be expandable, a race column needs to have one or more tracked races associated. No GPS data is required
     * (anymore) because there are some interesting columns that may be fed solely based on the Race Log contents, such
     * as finishing times and start times, resulting in time sailed and calculated times. Wind data is not required for
     * expandability either because several metrics can reasonably be determined even without reliable wind information.
     */
    private boolean shallExpandRaceColumn(RaceColumnDTO raceColumnDTO) {
        return showRaceDetails && raceColumnDTO.hasTrackedRaces();
    }

    private void removeUnusedRaceColumns(LeaderboardDTO leaderboard) {
        List<Column<LeaderboardRowDTO, ?>> columnsToRemove = new ArrayList<Column<LeaderboardRowDTO, ?>>();
        for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
            Column<LeaderboardRowDTO, ?> c = getLeaderboardTable().getColumn(i);
            if (c instanceof RaceColumn && (leaderboard == null
                    || !leaderboard.raceListContains(((RaceColumn<?>) c).getRaceColumnName()))) {
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
        if (getLeaderboardTable().getColumn(
                getLeaderboardTable().getColumnCount() - 1) instanceof LeaderboardPanel.TotalNetPointsColumn) {
            removeColumn(getLeaderboardTable().getColumnCount() - 1);
        }
        addColumn(raceColumn);
    }

    /**
     * The regatta rank column shall be displayed if an only if the {@link DetailType#REGATTA_RANK} detail is selected
     * in the {@link #selectedOverallDetailColumns}. It will then be displayed after the selection checkbox column (if
     * any) and before the sail number / competitor name columns.
     */
    private boolean isShowRegattaRankColumn() {
        return selectedOverallDetailColumns.contains(DetailType.REGATTA_RANK);
    }

    /**
     * @param rankColumnIndex
     *            the column index (0-based) where to put the rank column, if needed
     * @return the column index (0-based) for the next column; will equal <code>rankColumnIndex</code> if the rank
     *         column is not {@link #isShowRegattaRankColumn() supposed to be shown}, or one greater otherwise.
     */
    private int ensureRankColumn(int rankColumnIndex) {
        final int indexOfNextColumn = rankColumnIndex + (isShowRegattaRankColumn() ? 1 : 0);
        if (getLeaderboardTable().getColumnCount() > rankColumnIndex) {
            if (isShowRegattaRankColumn()) {
                if (getLeaderboardTable().getColumn(rankColumnIndex) != getRankColumn()) {
                    insertColumn(rankColumnIndex, getRankColumn());
                } // else, the column is needed and is already in place
            } else {
                if (getLeaderboardTable().getColumn(rankColumnIndex) == getRankColumn()) {
                    removeColumn(rankColumnIndex);
                }
            }
        } else {
            if (isShowRegattaRankColumn()) {
                insertColumn(rankColumnIndex, getRankColumn());
            }
        }
        return indexOfNextColumn;
    }

    /**
     * @param selectionCheckboxColumnIndex
     *            the column index (0-based) where to put the selection checkbox column, if needed
     * @return the column index (0-based) for the next column; will equal <code>selectionCheckboxColumnIndex</code> if
     *         the selection checkbox column is not {@link #showSelectionCheckbox supposed to be shown}, or one greater
     *         otherwise.
     */
    private int ensureSelectionCheckboxColumn(int selectionCheckboxColumnIndex) {
        final int indexOfNextColumn = selectionCheckboxColumnIndex + (showSelectionCheckbox ? 1 : 0);
        if (getLeaderboardTable().getColumnCount() > selectionCheckboxColumnIndex) {
            if (showSelectionCheckbox) {
                if (getLeaderboardTable().getColumn(selectionCheckboxColumnIndex) != selectionCheckboxColumn) {
                    insertColumn(selectionCheckboxColumnIndex, selectionCheckboxColumn);
                } // else, the column is needed and is already in place
            } else {
                if (getLeaderboardTable().getColumn(selectionCheckboxColumnIndex) == selectionCheckboxColumn) {
                    removeColumn(selectionCheckboxColumnIndex);
                }
            }
        } else {
            if (showSelectionCheckbox) {
                insertColumn(selectionCheckboxColumnIndex, selectionCheckboxColumn);
            }
        }
        return indexOfNextColumn;
    }

    /**
     * @return the 0-based index for the next column
     */
    private int ensureCompetitorInfoWithFlagColumnAndCompetitorColumn(int columnIndexWhereToInsertTheNextColumn) {
        if (isShowCompetitorShortName()) {
            CompetitorInfoWithFlagColumn<LeaderboardRowDTO> competitorInfoWithFlagColumn;
            CompetitorFetcher<LeaderboardRowDTO> competitorFetcher = (LeaderboardRowDTO row) -> row.competitor;
                competitorInfoWithFlagColumn = new CompetitorInfoWithFlagColumn<LeaderboardRowDTO>(competitorFetcher, style);
            if (getLeaderboardTable().getColumnCount() <= columnIndexWhereToInsertTheNextColumn
                    || !(getLeaderboardTable().getColumn(columnIndexWhereToInsertTheNextColumn) instanceof CompetitorInfoWithFlagColumn)) {
                insertColumn(columnIndexWhereToInsertTheNextColumn, competitorInfoWithFlagColumn);
            }
            columnIndexWhereToInsertTheNextColumn++;
        } else {
            if (getLeaderboardTable().getColumnCount() > columnIndexWhereToInsertTheNextColumn && getLeaderboardTable()
                    .getColumn(columnIndexWhereToInsertTheNextColumn) instanceof CompetitorInfoWithFlagColumn) {
                removeColumn(columnIndexWhereToInsertTheNextColumn);
            }
        }
        if (isShowCompetitorFullName()) {
            if (getLeaderboardTable().getColumnCount() <= columnIndexWhereToInsertTheNextColumn
                    || !(getLeaderboardTable().getColumn(
                            columnIndexWhereToInsertTheNextColumn) instanceof LeaderboardPanel.CompetitorColumn)) {
                insertColumn(columnIndexWhereToInsertTheNextColumn, createCompetitorColumn());
            }
            columnIndexWhereToInsertTheNextColumn++;
        } else {
            if (getLeaderboardTable().getColumnCount() > columnIndexWhereToInsertTheNextColumn && getLeaderboardTable()
                    .getColumn(columnIndexWhereToInsertTheNextColumn) instanceof LeaderboardPanel.CompetitorColumn) {
                removeColumn(columnIndexWhereToInsertTheNextColumn);
            }
        }
        if (canShowCompetitorBoatInfo() && isShowCompetitorBoatInfo()) {
            if (getLeaderboardTable().getColumnCount() <= columnIndexWhereToInsertTheNextColumn
                    || !(getLeaderboardTable()
                            .getColumn(columnIndexWhereToInsertTheNextColumn) instanceof LeaderboardPanel.BoatInfoColumn)) {
                BoatFetcher<LeaderboardRowDTO> boatFetcher = (LeaderboardRowDTO row) -> row.boat;
                BoatInfoColumn<LeaderboardRowDTO> boatInfoColumn = new BoatInfoColumn<LeaderboardRowDTO>(boatFetcher, style);                
                insertColumn(columnIndexWhereToInsertTheNextColumn, boatInfoColumn);
            }
            columnIndexWhereToInsertTheNextColumn++;
        } else {
            if (getLeaderboardTable().getColumnCount() > columnIndexWhereToInsertTheNextColumn && getLeaderboardTable()
                    .getColumn(columnIndexWhereToInsertTheNextColumn) instanceof LeaderboardPanel.BoatInfoColumn) {
                removeColumn(columnIndexWhereToInsertTheNextColumn);
            }
        }

        return columnIndexWhereToInsertTheNextColumn;
    }

    protected CompetitorColumn createCompetitorColumn() {
        return new CompetitorColumn(new CompetitorColumnBase<LeaderboardRowDTO>(this, stringMessages,
                new CompetitorFetcher<LeaderboardRowDTO>() {
                    @Override
                    public CompetitorDTO getCompetitor(LeaderboardRowDTO t) {
                        return t.competitor;
                    }
                }));
    }
    
    private void ensureTotalsColumn() {
        // add a totals column on the right
        if (getLeaderboardTable().getColumnCount() == 0 || !(getLeaderboardTable().getColumn(
                getLeaderboardTable().getColumnCount() - 1) instanceof LeaderboardPanel.TotalNetPointsColumn)) {
            addColumn(new TotalNetPointsColumn(TOTAL_COLUMN_STYLE));
        }
    }

    /**
     * If the <code>leaderboard</code> {@link LeaderboardDTO#hasCarriedPoints has carried points} and if column #1
     * (second column, right of the competitor column) does not exist or is not of type {@link CarryColumn}, all columns
     * starting from #1 will be removed and a {@link CarryColumn} will be added. If the leaderboard has no carried
     * points but the display still shows a carry column, the column is removed.
     * 
     * @param zeroBasedIndexOfCarryColumn
     *            the 0-based column index where to put the carry column, if any
     * @return the 0-based index of the next column following this column if the carry column is to be shown, or
     *         <code>zeroBasedIndexOfCarryColumn</code> otherwise
     */
    protected int updateCarryColumn(LeaderboardDTO leaderboard, int zeroBasedIndexOfCarryColumn) {
        final boolean needsCarryColumn = leaderboard != null && leaderboard.hasCarriedPoints;
        if (needsCarryColumn) {
            ensureCarryColumn(zeroBasedIndexOfCarryColumn);
        } else {
            ensureNoCarryColumn(zeroBasedIndexOfCarryColumn);
        }
        return needsCarryColumn ? zeroBasedIndexOfCarryColumn + 1 : zeroBasedIndexOfCarryColumn;
    }

    protected void ensureNoCarryColumn(int zeroBasedIndexOfCarryColumn) {
        if (getLeaderboardTable().getColumnCount() > zeroBasedIndexOfCarryColumn && getLeaderboardTable()
                .getColumn(zeroBasedIndexOfCarryColumn) instanceof LeaderboardPanel.CarryColumn) {
            removeColumn(zeroBasedIndexOfCarryColumn);
        }
    }

    protected void ensureCarryColumn(int zeroBasedIndexOfCarryColumn) {
        if (getLeaderboardTable().getColumnCount() <= zeroBasedIndexOfCarryColumn || !(getLeaderboardTable()
                .getColumn(zeroBasedIndexOfCarryColumn) instanceof LeaderboardPanel.CarryColumn)) {
            while (getLeaderboardTable().getColumnCount() > zeroBasedIndexOfCarryColumn) {
                removeColumn(zeroBasedIndexOfCarryColumn);
            }
            addColumn(createCarryColumn());
        }
    }

    protected CarryColumn createCarryColumn() {
        return new CarryColumn();
    }

    public SortedCellTable<LeaderboardRowDTO> getLeaderboardTable() {
        return leaderboardTable;
    }

    public SailingServiceAsync getSailingService() {
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
        return getLeaderboardTable().getDataProvider();
    }

    /**
     * @param newTime
     *            ignored; may be <code>null</code>. The time for loading the leaderboard is determined using
     *            {@link #getLeaderboardDisplayDate()}.
     */
    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        loadCompleteLeaderboard(/* showProgress */ false);
    }

    @Override
    public void playStateChanged(PlayStates playState, PlayModes playMode) {
        if (!isEmbedded) {
            playPause.setHTML(getPlayPauseImgHtml(playState));
            playPause.setTitle(playState == PlayStates.Playing ? stringMessages.pauseAutomaticRefresh()
                    : stringMessages.autoRefresh());
        }
    }

    @Override
    public void playSpeedFactorChanged(double newPlaySpeedFactor) {
        // nothing to do
    }

    private List<RegattaAndRaceIdentifier> getTrackedRacesIdentifiers() {
        List<RegattaAndRaceIdentifier> result = new ArrayList<RegattaAndRaceIdentifier>();
        for (RaceColumnDTO raceColumn : getLeaderboard().getRaceList()) {
            for (FleetDTO fleet : raceColumn.getFleets()) {
                if (raceColumn.getRaceIdentifier(fleet) != null) {
                    result.add(raceColumn.getRaceIdentifier(fleet));
                }
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
    public String getLocalizedShortName() {
        return stringMessages.leaderboard();
    }

    private LeaderboardRowDTO getRow(String competitorIdAsString) {
        synchronized (getData().getList()) {
            for (LeaderboardRowDTO row : getData().getList()) {
                if (row.competitor.getIdAsString().equals(competitorIdAsString)) {
                    return row;
                }
            }
        }
        return null;
    }

    @Override
    public void addedToSelection(CompetitorDTO competitor) {
        LeaderboardRowDTO row = getRow(competitor.getIdAsString());
        if (row != null) {
            leaderboardSelectionModel.setSelected(row, true);
        }
    }

    @Override
    public void removedFromSelection(CompetitorDTO competitor) {
        LeaderboardRowDTO row = getRow(competitor.getIdAsString());
        if (row != null) {
            leaderboardSelectionModel.setSelected(row, false);
        }
    }

    private Iterable<LeaderboardRowDTO> getSelectedRows() {
        return leaderboardSelectionModel.getSelectedSet();
    }

    @Override
    public void competitorsListChanged(Iterable<CompetitorDTO> competitors) {
        setFilterControlStatus();
        if (timer.isInitialized()) {
            timeChanged(timer.getTime(), null);
        }
    }

    @Override
    public void filteredCompetitorsListChanged(Iterable<CompetitorDTO> filteredCompetitors) {
        setFilterControlStatus();
        updateLeaderboard(getLeaderboard());
    }

    @Override
    public void filterChanged(FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> oldFilterSet,
            FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> newFilterSet) {
        // nothing to do; if the list of filtered competitors has changed, a separate call to
        // filteredCompetitorsListChanged will occur
        setFilterControlStatus();
    }

    public RaceColumnSelection getRaceColumnSelection() {
        return raceColumnSelection;
    }

    public void removeAllListeners() {
        if (raceTimesInfoProviderListener != null) {
            getRaceTimesInfoProvider().removeRaceTimesInfoProviderListener(raceTimesInfoProviderListener);
        }
        if (raceColumnSelection != null && raceColumnSelection.getType() == RaceColumnSelectionStrategies.LAST_N) {
            getRaceTimesInfoProvider()
                    .removeRaceTimesInfoProviderListener((LastNRacesColumnSelection) raceColumnSelection);
        }
        if (timer != null) {
            timer.removeTimeListener(this);
        }
        if (leaderboardUpdateListener != null) {
            leaderboardUpdateListener.clear();
        }
    }

    protected Timer getTimer() {
        return timer;
    }

    protected void blurFocusedElementAfterSelectionChange() {
        // now "blur" the selected leaderboard element because it seems to cause the cell table to scroll to its top;
        // see bug 2093.
        blur();
        final ScheduledCommand blurCommand = new ScheduledCommand() {
            @Override
            public void execute() {
                blur();
            }
        };
        Scheduler.get().scheduleDeferred(blurCommand);
    }

    private void blur() {
        if (elementToBlur != null) {
            elementToBlur.blur();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            timeChanged(getLeaderboardDisplayDate(), null);
        }
    }

    @Override
    public void addLeaderboardUpdateListener(LeaderboardUpdateListener listener) {
        this.leaderboardUpdateListener.add(listener);
    }

    @Override
    public void removeLeaderboardUpdateListener(LeaderboardUpdateListener listener) {
        this.leaderboardUpdateListener.remove(listener);
    }

    protected void informLeaderboardUpdateListenersAboutLeaderboardUpdated(LeaderboardDTO leaderboard) {
        for (LeaderboardUpdateListener listener : new ArrayList<LeaderboardUpdateListener>(
                this.leaderboardUpdateListener)) {
            listener.updatedLeaderboard(leaderboard);
        }
    }

    protected void informLeaderboardUpdateListenersAboutRaceSelected(RaceIdentifier raceIdentifier,
            RaceColumnDTO raceColumn) {
        for (LeaderboardUpdateListener listener : this.leaderboardUpdateListener) {
            listener.currentRaceSelected(raceIdentifier, raceColumn);
        }
    }

    public boolean hasLiveRace() {
        return getLeaderboard().hasLiveRace(timer.getLiveTimePointInMillis());
    }

    public String getLiveRacesText() {
        String result = "";
        List<Pair<RaceColumnDTO, FleetDTO>> liveRaces = leaderboard.getLiveRaces(timer.getLiveTimePointInMillis());
        boolean isMeta = leaderboard.type.isMetaLeaderboard();
        if (!liveRaces.isEmpty()) {
            if (liveRaces.size() == 1) {
                String text = getLiveRaceText(liveRaces.get(0), isMeta);
                result = isMeta ? stringMessages.regattaIsLive(text) : stringMessages.raceIsLive(text);
            } else {
                String names = "";
                for (Pair<RaceColumnDTO, FleetDTO> liveRace : liveRaces) {
                    names += getLiveRaceText(liveRace, isMeta) + ", ";
                }
                // remove last ", "
                names = names.substring(0, names.length() - 2);
                result = isMeta ? stringMessages.regattasAreLive(names) : stringMessages.racesAreLive(names);
            }
        }
        return result;
    }

    private String getLiveRaceText(Pair<RaceColumnDTO, FleetDTO> liveRace, boolean isMeta) {
        String raceName = liveRace.getA().getRaceColumnName(), fleetName = liveRace.getB().getName();
        boolean isDefaultFleet = LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleetName);
        return raceName + ((isDefaultFleet || isMeta) ? "" : (" (" + liveRace.getB().getName() + ")"));
    }

    @Override
    public String getDependentCssClassName() {
        return "leaderboard";
    }

    @Override
    public void addBusyStateChangeListener(BusyStateChangeListener listener) {
        busyStateChangeListeners.add(listener);
    }

    @Override
    public void removeBusyStateChangeListener(BusyStateChangeListener listener) {
        busyStateChangeListeners.remove(listener);
    }

    @Override
    public boolean isBusy() {
        return busyIndicator.isBusy();
    }

    private void setBusyState(boolean isBusy) {
        if (busyIndicator.isBusy() != isBusy) {
            busyIndicator.setBusy(isBusy);
            for (BusyStateChangeListener listener : busyStateChangeListeners) {
                listener.onBusyStateChange(isBusy);
            }
        }
    }

    @Override
    public void addBusyTask() {
        busyTaskCounter++;
        if (busyTaskCounter == 1) {
            setBusyState(true);
        }
    }

    @Override
    public void removeBusyTask() {
        busyTaskCounter--;
        if (busyTaskCounter == 0) {
            setBusyState(false);
        }
    }

    @Override
    public String getId() {
        return LeaderboardPanelLifecycle.ID;
    }

    protected class CompetitorColumn extends LeaderboardSortableColumnWithMinMax<LeaderboardRowDTO, LeaderboardRowDTO> {
        private final CompetitorColumnBase<LeaderboardRowDTO> base;

        protected CompetitorColumn(CompetitorColumnBase<LeaderboardRowDTO> base) {
            super(base.getCell(getLeaderboard()), SortingOrder.ASCENDING, LeaderboardPanel.this);
            this.base = base;
        }

        public CompetitorColumn(CompositeCell<LeaderboardRowDTO> compositeCell,
                CompetitorColumnBase<LeaderboardRowDTO> base) {
            super(compositeCell, SortingOrder.ASCENDING, LeaderboardPanel.this);
            this.base = base;
        }

        @Override
        public InvertibleComparator<LeaderboardRowDTO> getComparator() {
            return new InvertibleComparatorAdapter<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    return Collator.getInstance().compare(getLeaderboard().getDisplayName(o1.competitor),
                            getLeaderboard().getDisplayName(o2.competitor));
                }
            };
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return base.getHeader();
        }

        @Override
        public LeaderboardRowDTO getValue(LeaderboardRowDTO object) {
            return object;
        }

        protected void defaultRender(Context context, LeaderboardRowDTO object, SafeHtmlBuilder sb) {
            super.render(context, object, sb);
        }

        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder sb) {
            LeaderboardPanel.this.renderCompetitorText(object.competitor, !isShowCompetitorShortName(),
                    isShowCompetitorFullName(), sb, builder -> base.render(context, object, builder));
        }
    }

    protected abstract String getCompetitorColor(CompetitorDTO competitor);

    private void renderCompetitorText(final CompetitorDTO competitor, final boolean withFlags, final boolean withColor,
            final SafeHtmlBuilder sb, final Consumer<SafeHtmlBuilder> textRenderer) {
        final String competitorColor = getCompetitorColor(competitor);
        final boolean showColor = withColor && competitorColor != null;
        final String divStyle = showColor ? style.determineBoatColorDivStyle(competitorColor) : "border: none;";
        sb.appendHtmlConstant("<div style=\"" + divStyle + "\">");

        if (withFlags) {
            final String flagImageURL = competitor.getFlagImageURL();
            if (isShowCompetitorNationality || flagImageURL == null || flagImageURL.isEmpty()) {
                final String twoLetterIsoCountryCode = competitor.getTwoLetterIsoCountryCode();
                final CountryCode countryCode = CountryCodeFactory.INSTANCE.getFromTwoLetterISOName(twoLetterIsoCountryCode);
                final ImageResource nationalityFlagImageResource;
                if (twoLetterIsoCountryCode == null || twoLetterIsoCountryCode.isEmpty()) {
                    nationalityFlagImageResource = flagImageResolver.getEmptyFlagImageResource();
                } else {
                    nationalityFlagImageResource = flagImageResolver.getFlagImageResource(twoLetterIsoCountryCode);
                }
                if (nationalityFlagImageResource != null) {
                    style.renderNationalityFlag(nationalityFlagImageResource, countryCode, sb);
                    sb.appendHtmlConstant("&nbsp;");
                }
            }
            if (flagImageURL != null && !flagImageURL.isEmpty()) {
                style.renderFlagImage(flagImageURL, sb, competitor);
                sb.appendHtmlConstant("&nbsp;");
            }
        }

        textRenderer.accept(sb);
        sb.appendHtmlConstant("</div>");
    }

    public PaywallResolver getPaywallResolver() {
        return this.paywallResolver;
    }
}
