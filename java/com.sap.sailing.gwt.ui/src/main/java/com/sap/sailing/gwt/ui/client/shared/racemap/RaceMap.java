package com.sap.sailing.gwt.ui.client.shared.racemap;

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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gwt.ajaxloader.client.ArrayHelper;
import com.google.gwt.canvas.dom.client.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapOptions;
import com.google.gwt.maps.client.MapTypeId;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.RenderingType;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.LatLngBounds;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.maps.client.controls.MapTypeStyle;
import com.google.gwt.maps.client.events.bounds.BoundsChangeMapEvent;
import com.google.gwt.maps.client.events.bounds.BoundsChangeMapHandler;
import com.google.gwt.maps.client.events.click.ClickMapEvent;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.dragend.DragEndMapEvent;
import com.google.gwt.maps.client.events.dragend.DragEndMapHandler;
import com.google.gwt.maps.client.events.idle.IdleMapEvent;
import com.google.gwt.maps.client.events.idle.IdleMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapEvent;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapEvent;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapHandler;
import com.google.gwt.maps.client.maptypes.MapTypeStyleFeatureType;
import com.google.gwt.maps.client.maptypes.StyledMapType;
import com.google.gwt.maps.client.maptypes.StyledMapTypeOptions;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.Marker;
import com.google.gwt.maps.client.overlays.MarkerOptions;
import com.google.gwt.maps.client.overlays.Polygon;
import com.google.gwt.maps.client.overlays.PolygonOptions;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.maps.client.overlays.PolylineOptions;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.AnimationType;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NonCardinalBounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalablePosition;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.security.SecuredDomainType.TrackedRaceActions;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sailing.gwt.common.client.FullscreenUtil;
import com.sap.sailing.gwt.common.client.premium.SailingPremiumListBox;
import com.sap.sailing.gwt.common.client.sharing.FloatingSharingButtonsResources;
import com.sap.sailing.gwt.ui.actions.GetBoatPositionsAction;
import com.sap.sailing.gwt.ui.actions.GetBoatPositionsCallback;
import com.sap.sailing.gwt.ui.actions.GetPolarAction;
import com.sap.sailing.gwt.ui.actions.GetRaceMapDataAction;
import com.sap.sailing.gwt.ui.actions.GetWindInfoAction;
import com.sap.sailing.gwt.ui.client.ClientResources;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.DetailTypeComparator;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.NauticalSideFormatter;
import com.sap.sailing.gwt.ui.client.NumberFormatterFactory;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.RequiresDataInitialization;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.WindSourceTypeFormatter;
import com.sap.sailing.gwt.ui.client.media.MediaPlayerManagerComponent;
import com.sap.sailing.gwt.ui.client.shared.filter.QuickFlagDataValuesProvider;
import com.sap.sailing.gwt.ui.client.shared.racemap.BoatOverlay.DisplayMode;
import com.sap.sailing.gwt.ui.client.shared.racemap.Colorline.MouseOverLineEvent;
import com.sap.sailing.gwt.ui.client.shared.racemap.Colorline.MouseOverLineHandler;
import com.sap.sailing.gwt.ui.client.shared.racemap.FixesAndTails.PositionRequest;
import com.sap.sailing.gwt.ui.client.shared.racemap.QuickFlagDataProvider.QuickFlagDataListener;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceCompetitorSet.CompetitorsForRaceDefinedListener;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings.HelpLineTypes;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sailing.gwt.ui.client.shared.racemap.windladder.WindLadder;
import com.sap.sailing.gwt.ui.raceboard.RaceboardDropdownResources;
import com.sap.sailing.gwt.ui.server.SailingServiceImpl;
import com.sap.sailing.gwt.ui.shared.CompactBoatPositionsDTO;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegTypeIterable;
import com.sap.sailing.gwt.ui.shared.LegInfoDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.QuickRankDTO;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.RaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.SidelineDTO;
import com.sap.sailing.gwt.ui.shared.SpeedWithBearingDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sailing.gwt.ui.shared.racemap.DetailTypeMetricOverlay;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapStyleHelper;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapsLoader;
import com.sap.sailing.gwt.ui.shared.racemap.RaceSimulationOverlay;
import com.sap.sailing.gwt.ui.shared.racemap.WindStreamletsRaceboardOverlay;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Color;
import com.sap.sse.common.ColorMapper;
import com.sap.sse.common.ColorMapperChangedListener;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.ValueRangeFlexibleBoundaries;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.RGBColor;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.gwt.client.DOMUtils;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.async.TimeRangeActionsExecutor;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.gwt.shared.DebugConstants;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public class RaceMap extends AbstractCompositeComponent<RaceMapSettings> implements TimeListener, CompetitorSelectionChangeListener,
        RaceTimesInfoProviderListener, TailFactory, ColorMapperChangedListener, RequiresDataInitialization, RequiresResize, QuickFlagDataValuesProvider {
    static final private String SAILING_ANALYTICS_MAP_TYPE_ID = "sailing_analytics";

    /* Line colors */
    static private final RGBColor COURSE_MIDDLE_LINE_COLOR = new RGBColor("#0eed1d");
    static final Color ADVANTAGE_LINE_COLOR = new RGBColor("#ff9900"); // orange
    static final Color START_LINE_COLOR = Color.WHITE;
    static final Color FINISH_LINE_COLOR = Color.BLACK;
    static final Color LOWLIGHTED_TAIL_COLOR = new RGBColor(200, 200, 200);
    /* Line opacities and stroke weights */
    static final double LOWLIGHTED_TAIL_OPACITY = 0.3;
    static final double STANDARD_LINE_OPACITY = 1.0;
    static final int STANDARD_LINE_STROKEWEIGHT = 1;
    
    public static final String GET_RACE_MAP_DATA_CATEGORY = "getRaceMapData";
    public static final String GET_WIND_DATA_CATEGORY = "getWindData";
    private static final String COMPACT_HEADER_STYLE = "compactHeader";
    public static final Color WATER_COLOR = new RGBColor(0, 67, 125);
    
    private AbsolutePanel rootPanel = new AbsolutePanel();
    
    private MapWidget map;
    private final Runnable shareLinkAction;
    private Collection<Runnable> mapInitializedListener;
    private Button addVideoToRaceButton;
    private final Button trueNorthIndicatorButtonButtonGroup;
    private final PopupPanel advancedFunctionsPopup;
    private final Button advancedFunctionsButton;
    
    /**
     * Always valid, non-<code>null</code>. Must be used to map all coordinates, headings, bearings, and directions
     * displayed on the map, including the orientations of any canvases such as boat icons, wind displays etc. that are
     * embedded in the map. The coordinate systems facilitates the possibility of transformed displays such as
     * rotated and translated versions of the map, implementing the "wind-up" view.
     */
    private DelegateCoordinateSystem coordinateSystem;
    
    /**
     * Tells whether the {@link RenderingType#VECTOR} is supported on the current platform / browser.
     * This then decides which {@link CoordinateSystem} to use for {@code #coordinateSystem} in case
     * the user requests wind-up display: if {@link RenderingType#VECTOR} is supported, a
     * {@link RotatedCoordinateSystem} will be used, and positions are mapped to their true
     * {@link LatLng} counterparts. Otherwise, the legacy {@link RotateAndTranslateCoordinateSystem} is
     * used, keeping the map's original heading at 0deg and virtually mapping the coordinate system
     * to 0N 0E, rotating all directions such that wind appears to come from the north.
     */
    private boolean vectorRenderingTypeSupported;
    
    /**
     * A panel with flex-box display, representing the semi-transparent header bar. It aligns its flex-items
     * on the center line vertically and uses "space-between" for the horizontal alignment. It has a fixed height
     * and uses "border-box" sizing. Things to display in the header bar at the top of the map must be added
     * as elements to it, making the children "flex-items" which may again use "display: flex" in their styles
     * to nest flex boxes in the header.
     */
    private FlowPanel headerPanel;

    private final SailingServiceAsync sailingService;
    private final ErrorReporter errorReporter;

    private final static ClientResources resources = GWT.create(ClientResources.class);

    /**
     * Polyline for the start line (connecting two marks representing the start gate).
     */
    private Polyline startLine;

    /**
     * Polyline for the finish line (connecting two marks representing the finish gate).
     */
    private Polyline finishLine;

    /**
     * Polyline for the advantage line (the leading line for the boats, orthogonal to the wind direction; touching the leading boat).
     */
    private Polyline advantageLine;
    
    private AdvantageLineAnimator advantageTimer;
    
    /**
     * The windward of two Polylines representing a triangle between startline and first mark.
     */
    private Polyline windwardStartLineMarkToFirstMarkLine;
    
    /**
     * The leeward of two Polylines representing a triangle between startline and first mark.
     */
    private Polyline leewardStartLineMarkToFirstMarkLine;

    private class AdvantageLineMouseOverMapHandler implements MouseOverMapHandler {
        private double trueWindAngle;
        private Date date;
        
        public AdvantageLineMouseOverMapHandler(double trueWindAngle, Date date) {
            this.trueWindAngle = trueWindAngle;
            this.date = date;
        }
        
        public void setTrueWindBearing(double trueWindAngle) {
            this.trueWindAngle = trueWindAngle;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        @Override
        public void onEvent(MouseOverMapEvent event) {
            map.setTitle(stringMessages.advantageLine()+" (from "+new DegreeBearingImpl(Math.round(trueWindAngle)).reverse().getDegrees()+"deg"+
                    (date == null ? ")" : ", "+ date) + ")");
        }
    };
    
    private AdvantageLineMouseOverMapHandler advantageLineMouseOverHandler;
    
    /**
     * Polylines for the course middle lines; keys are the two control points delimiting the leg for which the
     * {@link Polyline} value shows the course middle line. As only one course middle line is shown even if there
     * are multiple legs using the same control points in different directions, using a {@link Set} makes this
     * independent of the order of the two control points. If no course middle line is currently being shown for
     * a pair of control points, the map will not contain a value for this pair.
     */
    private final Map<Set<ControlPointDTO>, Polyline> courseMiddleLines;

    private final Map<SidelineDTO, Polygon> courseSidelines;
    
    /**
     * When the {@link HelpLineTypes#COURSEGEOMETRY} option is selected, little markers will be displayed on the
     * lines that show the tooltip text in a little info box linked to the line. When the line is removed by
     * {@link #showOrRemoveOrUpdateLine(Polyline, boolean, Position, Position, LineInfoProvider, String)}, these
     * overlays need to be removed as well. Also, when the {@link HelpLineTypes#COURSEGEOMETRY} setting is
     * deactivated, all these overlays need to go.
     */
    private final Map<Polyline, SmallTransparentInfoOverlay> infoOverlaysForLinesForCourseGeometry;
    
    /**
     * Wind data used to display the advantage line. Retrieved by a {@link GetWindInfoAction} execution and used in
     * {@link #showAdvantageLine(Iterable, Date)}.
     */
    private WindInfoForRaceDTO lastCombinedWindTrackInfoDTO;
    
    /**
     * Manages the cached set of {@link GPSFixDTOWithSpeedWindTackAndLegType}s for the boat positions as well as their graphical counterpart in the
     * form of {@link Polyline}s.
     */
    private final FixesAndTails fixesAndTails;

    /**
     * html5 canvases used as boat display on the map
     */
    private final Map<String, BoatOverlay> boatOverlaysByCompetitorIdsAsStrings;

    /**
     * html5 canvases used for competitor info display on the map
     */
    private final CompetitorInfoOverlays competitorInfoOverlays;
    
    private SmallTransparentInfoOverlay countDownOverlay;

    /**
     * Map overlays with html5 canvas used to display wind sensors
     */
    private final Map<WindSource, WindSensorOverlay> windSensorOverlays;

    /**
     * Map from the {@link MarkDTO#getIdAsString() mark's ID converted to a string} to the corresponding overlays with
     * html5 canvas used to display course marks including buoy zones
     */
    private final Map<String, CourseMarkOverlay> courseMarkOverlays;
    
    private final Map<String, HandlerRegistration> courseMarkClickHandlers;

    private WindLadder windLadder;

    /**
     * Maps from the {@link MarkDTO#getIdAsString() mark's ID converted to a string} to the corresponding {@link MarkDTO}
     */
    private final Map<String, MarkDTO> markDTOs;

    /**
     * markers displayed in response to
     * {@link SailingServiceAsync#getDouglasPoints(String, String, Map, Map, double, AsyncCallback)}
     */
    protected Set<Marker> douglasMarkers;

    private Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> lastDouglasPeuckerResult;
    
    private final RaceCompetitorSelectionProvider competitorSelection;
    
    private final RaceCompetitorSet raceCompetitorSet;

    /**
     * Used to check if the first initial zoom to the mark markers was already done.
     */
    private boolean mapFirstZoomDone = false;

    private final Timer timer;

    private RaceTimesInfoDTO lastRaceTimesInfo;
    
    /**
     * RPC calls may receive responses out of order if there are multiple calls in-flight at the same time. If the time
     * slider is moved quickly it generates many requests for boat positions quickly after each other. Sometimes,
     * responses for requests send later may return before the responses to all earlier requests have been received and
     * processed. This counter is used to number the requests. When processing of a response for a later request has
     * already begun, responses to earlier requests will be ignored.
     */
    private int boatPositionRequestIDCounter;

    /**
     * Corresponds to {@link #boatPositionRequestIDCounter}. As soon as the processing of a response for a request ID
     * begins, this attribute is set to the ID. A response won't be processed if a later response is already being
     * processed.
     */
    private int startedProcessingRequestID;

    private final RaceMapImageManager raceMapImageManager; 

    private RaceMapSettings settings;
    private final RaceMapLifecycle raceMapLifecycle;
    
    private final StringMessages stringMessages;
    
    private boolean isMapInitialized;

    private Date lastTimeChangeBeforeInitialization;
    
    private int lastLegNumber;

    /**
     * The strategy for maintaining and delivering the "flag data" information. The provider will be informed about
     * flag data (rank or speed) received from a {@link RaceMapDataDTO} field but may choose to ignore this information, e.g.,
     * if it can assume that more current information about ranks and speed and leg numbers is available from a {@link LeaderboardDTO}.
     */
    private QuickFlagDataProvider quickFlagDataProvider;
    
    private final CombinedWindPanel combinedWindPanel;
    private final TrueNorthIndicatorPanel trueNorthIndicatorPanel;
    private final FlowPanel topLeftControlsWrapperPanel;

    private final TimeRangeActionsExecutor<CompactBoatPositionsDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable, Pair<String, DetailType>> timeRangeActionsExecutor;
    private final AsyncActionsExecutor asyncActionsExecutor;

    /**
     * The map bounds as last received by map callbacks; used to determine whether to suppress the boat animation during zoom/pan
     */
    private NonCardinalBounds currentMapBounds; // bounds of the visible part of the map to which bounds-changed-handler compares, in real-world coordinates
    private double currentZoomLevel;            // zoom-level to which bounds-changed-handler compares
    
    private boolean autoZoomIn = false;  // flags auto-zoom-in in progress
    private boolean autoZoomOut = false; // flags auto-zoom-out in progress
    private double autoZoomLevel;        // zoom-level to which auto-zoom-in/-out is zooming
    NonCardinalBounds autoZoomBounds;    // bounds to which auto-zoom-in/-out is panning&zooming, in real-world coordinates
    
    private RaceSimulationOverlay simulationOverlay;
    private WindStreamletsRaceboardOverlay streamletOverlay;
    private DetailTypeMetricOverlay metricOverlay;
    
    private static final String GET_POLAR_CATEGORY = "getPolar";
    
    /**
     * Tells about the availability of polar / VPP data for this race. If available, the simulation feature can be
     * offered to the user.
     */
    private boolean hasPolar;
    
    private final RegattaAndRaceIdentifier raceIdentifier;
    
    /**
     * While {@link #raceIdentifier} tells the race that this {@link RaceMap} uses as its "primary" context,
     * users may be interested in seeing the course marks and/or competitor positions of other races (usually
     * of the same event), too. This can, e.g., be used for safety reasons when the race committees want to
     * see whether all sailors have made it back to shore after racing, or how the various course marks are
     * aligned with the {@link #courseAreaCirclesToShow course area circles}.
     */
    private final Set<RegattaAndRaceIdentifier> otherRacesToShow;
    
    /**
     * A course area can optionally define a {@link CourseAreaDTO#getCenterPosition() center position} and a
     * {@link CourseAreaDTO#getRadius() radius}. The user may choose to display none of these, or a single one,
     * e.g., the course area circle of the {@link #raceIdentifier primary race} shown on this map, but also
     * other course areas, e.g., from the same venue, to see how mark positions align with those circles.
     */
    private final Map<CourseAreaDTO, CourseAreaCircleOverlay> courseAreaCirclesToShow;
    
    /**
     * When the user requests wind-up display this may happen at a point where no mark positions are known or when
     * no wind direction is known yet. In this case, this flag will be set, and when wind information or course mark
     * positions are received later, this flag is checked, and if set, a {@link #updateCoordinateSystemFromSettings()}
     * call is issued to make sure that the user's request for a new coordinate system is honored.
     */
    private boolean requiresCoordinateSystemUpdateWhenCoursePositionAndWindDirectionIsKnown;
    
    /**
     * Tells whether currently an auto-zoom is in progress; this is used particularly to keep the smooth CSS boat transitions
     * active while auto-zooming whereas stopping them seems the better option for manual zooms.
     */
    private boolean autoZoomInProgress;
    
    /**
     * The length of the advantage line; default is 1000m, but upon map initialization and zoom it is set to the
     * length of the diagonal spanning the map, so it should always cover the entire map. See also bug 616.
     */
    private Distance advantageLineLength = new MeterDistance(1000);

    /**
     * Tells whether currently an orientation change is in progress; this is required handle map events during the configuration of the map
     * during an orientation change.
     */
    private boolean orientationChangeInProgress;
    
    private final NumberFormat numberFormatNoDecimal = NumberFormatterFactory.getDecimalFormat(0);
    private final NumberFormat numberFormatOneDecimal = NumberFormatterFactory.getDecimalFormat(1);
    private final NumberFormat numberFormatTwoDecimals = NumberFormatterFactory.getDecimalFormat(2);
    
    /**
     * The competitor for which the advantage line is currently showing. Should this competitor's quick rank change, or
     * should ranks be received while this field is {@code null}, the advantage line
     * {@link #showAdvantageLineAndUpdateWindLadder(Iterable, Date, long)} drawing procedure} needs to be triggered.
     */
    private CompetitorDTO advantageLineCompetitor;
    protected Label estimatedDurationOverlay;
    private RaceMapStyle raceMapStyle;
    private final boolean showHeaderPanel;
    
    /** Callback to set the visibility of the wind chart. */
    private final Consumer<WindSource> showWindChartForProvider;
    private ManagedInfoWindow managedInfoWindow;
    
    private final ManeuverMarkersAndLossIndicators maneuverMarkersAndLossIndicators;
    
    /**
     * Needed to get some metrics from {@link SailingServiceImpl.getCompetitorRaceDataEntry}.
     */
    private final String leaderboardName;
    /**
     * Needed to get some metrics from {@link SailingServiceImpl.getCompetitorRaceDataEntry}.
     */
    private final String leaderboardGroupName;
    /**
     * Needed to get some metrics from {@link SailingServiceImpl.getCompetitorRaceDataEntry}.
     */
    private final UUID leaderboardGroupId;

    /**
     * Contains all available {@link DetailType}s for this race. Gets filled by {@link #loadAvailableDetailTypes()} and
     * is used in {@link #getInfoWindowContent(WindSource, WindTrackInfoDTO)} to build a dropdown selection.
     * See {@link #selectedDetailType}.
     */
    private List<DetailType> sortedAvailableDetailTypes;
    /**
     * The currently selected {@link DetailType}. {@code null} if no {@link DetailType} is selected.
     */
    private DetailType selectedDetailType;
    
    /**
     * The number format that matches the {@link DetailType#getPrecision() precision} of the {@link #selectedDetailType}
     */
    private NumberFormat numberFormatterForSelectedDetailType;
    
    /**
     * Indicates if {@link #selectedDetailType} has changed. If that is the case {@link FixesAndTails} needs to
     * overwrite its cache with new data and needs to reset its internal {@link ValueRangeFlexibleBoundaries} which is
     * tracking the {@link DetailType} values. If set to {@code true}, {@link #refreshMap(Date, long, boolean)} will pass
     * the information along and set it back to {@code false} by
     * {@link #updateBoatPositions(Date, long, Map, Iterable, Map, boolean, boolean, DetailType)} once the first update with the new
     * {@link DetailType} values arrives at the client.
     */
    private boolean selectedDetailTypeChanged;
    /**
     * Used to visualize the {@link #selectedDetailType} on the tails of {@link #fixesAndTails}.
     */
    private ColorMapper tailColorMapper;

    private final FloatingSharingButtonsResources floatingSharingButtonsResources;
    private final PaywallResolver paywallResolver;

    static class MultiHashSet<T> {
        private HashMap<T, List<T>> map = new HashMap<>();

        /** @return true if value already in set */
        public boolean add(T t) {
            List<T> l = map.get(t);
            if (l != null) {
                l.add(t);
                return true;
            } else {
                l = new ArrayList<>();
                l.add(t);
                map.put(t, l);
                return false;
            }
        }

        public void addAll(MultiHashSet<T> col) {
            if (col != null) {
                col.map.entrySet().forEach(e -> e.getValue().forEach(v -> add(v)));
            }
        }

        /**
         * @return {@code true} if an object that equals {@code t} was found and removed in this multi-set,
         *         {@code false} otherwise
         */
        public boolean remove(T t) {
            List<T> l = map.get(t);
            if (l != null) {
                l.remove(t);
                if (l.size() == 0) {
                    map.remove(t);
                }
                return true;
            } else {
                return false;
            }
        }

        public boolean contains(T t) {
            return (map.containsKey(t));
        }

        public void clear() {
            map.clear();
        }

        public void removeAll(T t) {
            map.remove(t);
        }
    }

    private class AdvantageLineUpdater implements QuickFlagDataListener {
        @Override
        public void rankChanged(String competitorIdAsString, QuickRankDTO oldQuickRank, QuickRankDTO quickRank) {
            if (advantageLineCompetitor == null ||
                    (oldQuickRank != null && advantageLineCompetitor.getIdAsString().equals(oldQuickRank.competitor.getIdAsString())) ||
                    (quickRank != null && advantageLineCompetitor.getIdAsString().equals(quickRank.competitor.getIdAsString()))) {
                showAdvantageLineAndUpdateWindLadder(getCompetitorsToShow(), getTimer().getTime(), /* timeForPositionTransitionMillis */ -1 /* (no transition) */);
            }
        }

        @Override
        public void speedInKnotsChanged(CompetitorDTO competitor, Double quickSpeedInKnots) {
            // empty body
        }
    }
    
    public RaceMap(Component<?> parent, ComponentContext<?> context, RaceMapLifecycle raceMapLifecycle,
            RaceMapSettings raceMapSettings, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, ErrorReporter errorReporter, Timer timer,
            RaceCompetitorSelectionProvider competitorSelection, RaceCompetitorSet raceCompetitorSet,
            StringMessages stringMessages, RegattaAndRaceIdentifier raceIdentifier, RaceMapResources raceMapResources,
            boolean showHeaderPanel, QuickFlagDataProvider quickRanksDTOProvider, PaywallResolver paywallResolver, boolean isSimulationEnabled) {
        this(parent, context, raceMapLifecycle, raceMapSettings, sailingService, asyncActionsExecutor, errorReporter,
                timer, competitorSelection, raceCompetitorSet, stringMessages, raceIdentifier, raceMapResources,
                showHeaderPanel, quickRanksDTOProvider, /* leaderboardName */ "", /* leaderboardGroupName */ "",
                /* leaderboardGroupId */ null, paywallResolver, isSimulationEnabled);
    }

    public RaceMap(Component<?> parent, ComponentContext<?> context, RaceMapLifecycle raceMapLifecycle,
            RaceMapSettings raceMapSettings, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, ErrorReporter errorReporter, Timer timer,
            RaceCompetitorSelectionProvider competitorSelection, RaceCompetitorSet raceCompetitorSet,
            StringMessages stringMessages, RegattaAndRaceIdentifier raceIdentifier, RaceMapResources raceMapResources,
            boolean showHeaderPanel, QuickFlagDataProvider quickRanksDTOProvider, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, PaywallResolver paywallResolver, boolean isSimulationEnabled) {
        this(parent, context, raceMapLifecycle, raceMapSettings, sailingService, asyncActionsExecutor, errorReporter,
                timer, competitorSelection, raceCompetitorSet, stringMessages, raceIdentifier, raceMapResources,
                showHeaderPanel, quickRanksDTOProvider, visible -> {
                }, leaderboardName, leaderboardGroupName, leaderboardGroupId, /* shareLinkAction */ null, paywallResolver);
    }
    
    public RaceMap(Component<?> parent, ComponentContext<?> context, RaceMapLifecycle raceMapLifecycle,
            RaceMapSettings raceMapSettings, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, ErrorReporter errorReporter, Timer timer,
            RaceCompetitorSelectionProvider competitorSelection, RaceCompetitorSet raceCompetitorSet,
            StringMessages stringMessages, RegattaAndRaceIdentifier raceIdentifier, RaceMapResources raceMapResources,
            boolean showHeaderPanel, QuickFlagDataProvider quickFlagDataProvider,
            Consumer<WindSource> showWindChartForProvider, String leaderboardName, String leaderboardGroupName,
            UUID leaderboardGroupId, Runnable shareLinkAction, PaywallResolver paywallResolver) {
        super(parent, context);
        this.otherRacesToShow = new HashSet<>();
        this.courseAreaCirclesToShow = new HashMap<>();
        this.shareLinkAction = shareLinkAction;
        this.paywallResolver = paywallResolver;
        this.maneuverMarkersAndLossIndicators = new ManeuverMarkersAndLossIndicators(this, sailingService, errorReporter, stringMessages);
        this.showHeaderPanel = showHeaderPanel;
        this.quickFlagDataProvider = quickFlagDataProvider;
        this.raceMapLifecycle = raceMapLifecycle;
        this.stringMessages = stringMessages;
        this.sailingService = sailingService;
        this.raceIdentifier = raceIdentifier;
        this.timeRangeActionsExecutor = new TimeRangeActionsExecutor<>();
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.errorReporter = errorReporter;
        this.timer = timer;
        this.showWindChartForProvider = showWindChartForProvider;
        this.leaderboardName = leaderboardName;
        this.leaderboardGroupName = leaderboardGroupName;
        this.leaderboardGroupId = leaderboardGroupId;
        this.advancedFunctionsPopup = new PopupPanel();
        this.advancedFunctionsButton = new Button();
        floatingSharingButtonsResources = FloatingSharingButtonsResources.INSTANCE;
        floatingSharingButtonsResources.css().ensureInjected();
        timer.addTimeListener(this);
        raceMapImageManager = new RaceMapImageManager(raceMapResources);
        markDTOs = new HashMap<String, MarkDTO>();
        courseSidelines = new HashMap<>();
        courseMiddleLines = new HashMap<>();
        infoOverlaysForLinesForCourseGeometry = new HashMap<>();
        boatOverlaysByCompetitorIdsAsStrings = new HashMap<>();
        competitorInfoOverlays = new CompetitorInfoOverlays(this, stringMessages);
        quickFlagDataProvider.addQuickFlagDataListener(competitorInfoOverlays);
        quickFlagDataProvider.addQuickFlagDataListener(new AdvantageLineUpdater());
        windSensorOverlays = new HashMap<WindSource, WindSensorOverlay>();
        courseMarkOverlays = new HashMap<String, CourseMarkOverlay>();
        courseMarkClickHandlers = new HashMap<String, HandlerRegistration>();
        this.competitorSelection = competitorSelection;
        this.raceCompetitorSet = raceCompetitorSet;
        competitorSelection.addCompetitorSelectionChangeListener(this);
        settings = raceMapSettings;
        coordinateSystem = new DelegateCoordinateSystem(new IdentityCoordinateSystem());
        fixesAndTails = new FixesAndTails(coordinateSystem);
        updateCoordinateSystemFromSettings();
        loadAvailableDetailTypes();
        lastTimeChangeBeforeInitialization = null;
        isMapInitialized = false;
        mapInitializedListener = new ArrayList<>();
        hasPolar = false;
        headerPanel = new FlowPanel();
        headerPanel.setStyleName("RaceMap-HeaderPanel");
        raceMapStyle = raceMapResources.raceMapStyle();
        raceMapStyle.ensureInjected();
        combinedWindPanel = new CombinedWindPanel(this, raceMapImageManager, raceMapStyle, stringMessages, coordinateSystem, paywallResolver, raceMapLifecycle.getRaceDTO());
        combinedWindPanel.setVisible(false);
        trueNorthIndicatorPanel = new TrueNorthIndicatorPanel(this, raceMapImageManager, raceMapStyle, stringMessages, coordinateSystem, raceMapLifecycle.getRaceDTO(), paywallResolver);
        trueNorthIndicatorPanel.setVisible(false);
        topLeftControlsWrapperPanel = new FlowPanel();
        topLeftControlsWrapperPanel.addStyleName(raceMapStyle.topLeftControlsWrapperPanel());
        topLeftControlsWrapperPanel.add(combinedWindPanel);
        topLeftControlsWrapperPanel.add(trueNorthIndicatorPanel);
        orientationChangeInProgress = false;
        mapFirstZoomDone = false;
        addVideoToRaceButton = createAddVideoToRaceButton();
        trueNorthIndicatorButtonButtonGroup = createTrueNorthIndicatorButton();
        // TODO bug 494: reset zoom settings to user preferences
        initWidget(rootPanel);
        initializeData(settings.isShowMapControls(), showHeaderPanel);
        this.setSize("100%", "100%");
    }

    ManagedInfoWindow getManagedInfoWindow() {
        return managedInfoWindow;
    }

    RaceMapImageManager getRaceMapImageManager() {
        return raceMapImageManager;
    }
    
    protected Set<RegattaAndRaceIdentifier> getOtherRacesToShow() {
        return Collections.unmodifiableSet(otherRacesToShow);
    }

    public void setQuickRanksDTOProvider(QuickFlagDataProvider newQuickRanksDTOProvider) {
        if (this.quickFlagDataProvider != null) {
            this.quickFlagDataProvider.moveListernersTo(newQuickRanksDTOProvider);
        }
        this.quickFlagDataProvider = newQuickRanksDTOProvider;
    }
    /**
     * The {@link WindDTO#dampenedTrueWindFromDeg} direction if {@link #lastCombinedWindTrackInfoDTO} has a
     * {@link WindSourceType#COMBINED} source which has at least one fix recorded; <code>null</code> otherwise.
     */
    private Bearing getLastCombinedTrueWindFromDirection() {
        if (lastCombinedWindTrackInfoDTO != null) {
            for (Entry<WindSource, WindTrackInfoDTO> e : lastCombinedWindTrackInfoDTO.windTrackInfoByWindSource.entrySet()) {
                if (e.getKey().getType() == WindSourceType.COMBINED) {
                    final List<WindDTO> windFixes = e.getValue().windFixes;
                    if (!windFixes.isEmpty()) {
                        return new DegreeBearingImpl(windFixes.get(0).dampenedTrueWindFromDeg);
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * @return {@code true} if the map was redrawn by the call to this method
     */
    private boolean updateCoordinateSystemFromSettings() {
        boolean redrawn = false;
        final MapOptions mapOptions;
        orientationChangeInProgress = true;
        if (getSettings().isWindUp()) {
            final Position centerOfCourse = getCenterOfCourse();
            if (centerOfCourse != null) {
                final Bearing lastCombinedTrueWindFromDirection = getLastCombinedTrueWindFromDirection();
                if (lastCombinedTrueWindFromDirection != null) {
                    final int lastCombinedTrueWindFromDirectionInIntegerDegrees = (int) Math.round(lastCombinedTrueWindFromDirection.getDegrees());
                    // new equator shall point 90deg right of the "from" wind direction to make wind come from top of map
                    // TODO bug6098 use RotateAndTranslateCoordinateSystem if map fell back to rendering type RASTER:
                    coordinateSystem.setCoordinateSystem(vectorRenderingTypeSupported
                            ? new RotatedCoordinateSystem(new DegreeBearingImpl(lastCombinedTrueWindFromDirectionInIntegerDegrees).add(new DegreeBearingImpl(90)))
                            : new RotateAndTranslateCoordinateSystem(centerOfCourse, new DegreeBearingImpl(lastCombinedTrueWindFromDirectionInIntegerDegrees).add(new DegreeBearingImpl(90))));
                    if (map != null) {
                        mapOptions = getMapOptions(/* wind-up */ true, settings.isShowSatelliteLayer());
                        if (vectorRenderingTypeSupported) {
                            mapOptions.setHeading(lastCombinedTrueWindFromDirectionInIntegerDegrees);
                        }
                    } else {
                        mapOptions = null;
                    }
                    requiresCoordinateSystemUpdateWhenCoursePositionAndWindDirectionIsKnown = false;
                } else {
                    // register callback in case center of course and wind info becomes known
                    requiresCoordinateSystemUpdateWhenCoursePositionAndWindDirectionIsKnown = true;
                    mapOptions = null;
                }
            } else {
                // register callback in case center of course and wind info becomes known
                requiresCoordinateSystemUpdateWhenCoursePositionAndWindDirectionIsKnown = true;
                mapOptions = null;
            }
        } else {
            if (map != null) {
                mapOptions = getMapOptions(/* wind-up */ false, settings.isShowSatelliteLayer());
                if (vectorRenderingTypeSupported) {
                    mapOptions.setHeading(0);
                }
            } else {
                mapOptions = null;
            }
            coordinateSystem.setCoordinateSystem(new IdentityCoordinateSystem());
        }
        if (mapOptions != null) { // if no coordinate system change happened that affects an existing map, don't redraw 
            fixesAndTails.clearTails();
            redraw();
            redrawn = true;
            // zooming and setting options while the event loop is still working doesn't work reliably; defer until event loop returns
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    if (map != null) {
                        map.setOptions(mapOptions);
                        // ensure zooming to what the settings tell, or defaults if what the settings tell isn't possible right now
                        mapFirstZoomDone = false;
                        boolean visible = coordinateSystem.mapDegreeBearing(0) != 0;
                        trueNorthIndicatorPanel.setVisible(visible);
                        trueNorthIndicatorButtonButtonGroup.getElement().getStyle().setProperty("transform", "rotate(" + coordinateSystem.mapDegreeBearing(0) + "deg)");
                        if (trueNorthIndicatorPanel.isVisible()) {
                            trueNorthIndicatorPanel.redraw();
                        }
                        if (combinedWindPanel.isVisible()) {
                            combinedWindPanel.redraw();
                        }
                        orientationChangeInProgress = false;
                    }
                }
            });
        }
        return redrawn;
    }
    
    private void loadAvailableDetailTypes() {
        sailingService.determineDetailTypesForCompetitorChart(leaderboardGroupName, leaderboardGroupId, raceIdentifier,
                new AsyncCallback<Iterable<DetailType>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorLoadingAvailableDetailTypes());
                    }
                    @Override
                    public void onSuccess(Iterable<DetailType> result) {
                        sortedAvailableDetailTypes = new ArrayList<DetailType>();
                        Util.addAll(result, sortedAvailableDetailTypes);
                        sortedAvailableDetailTypes.remove(DetailType.LEG_CURRENT_SPEED_OVER_GROUND_IN_KNOTS);
                        Collections.sort(sortedAvailableDetailTypes, new DetailTypeComparator());
                    }
                });
    }
    
    private boolean isVectorRenderingTypeSupported(MapWidget map) {
        final CanvasElement canvasElement = map.getElement().getOwnerDocument().createCanvasElement();
        final Context webgl2Context = canvasElement.getContext("webgl2");
        final boolean result = webgl2Context != null;
        canvasElement.removeFromParent();
        return result;
    }

    private void loadMapsAPIV3(final boolean showMapControls, final boolean showHeaderPanel, final boolean showSatelliteLayer) {
        Runnable onLoad = new Runnable() {
            @Override
            public void run() {
                MapOptions mapOptions = getMapOptions(/* wind up */ false, showSatelliteLayer);
                mapOptions.setRenderingType(RenderingType.VECTOR);
                map = new MapWidget(mapOptions);
                vectorRenderingTypeSupported = isVectorRenderingTypeSupported(map);
                if (!vectorRenderingTypeSupported) {
                    GWT.log("No support for VECTOR rendering type; resorting to RASTER; wind-up display will use coordinate transformation");
                } else {
                    GWT.log("Assuming VECTOR rendering type is supported because a WebGL2 context was created successfully");
                }
                map.addRenderingTypeChangeHandler(e->{
                    final RenderingType newRenderingType = map.getRenderingType();
                    if (vectorRenderingTypeSupported && newRenderingType == RenderingType.RASTER) {
                        vectorRenderingTypeSupported = false;
                        GWT.log("Map fell back to RASTER rendering type; wind-up display will use coordinate transformation");
                    }
                });
                map.getElement().setId("googleMapsArea");
                rootPanel.add(map, 0, 0);
                map.setControls(ControlPosition.LEFT_TOP, topLeftControlsWrapperPanel);
                adjustLeftControlsIndent();
                RaceMap.this.raceMapImageManager.loadMapIcons(map);
                final StyledMapTypeOptions styledMapTypeOptions = StyledMapTypeOptions.newInstance();
                styledMapTypeOptions.setName(SAILING_ANALYTICS_MAP_TYPE_ID);
                MapTypeStyle[] mapTypeStyles = new MapTypeStyle[4];
                // hide all transit lines including ferry lines
                mapTypeStyles[0] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.TRANSIT);
                // hide points of interest
                mapTypeStyles[1] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.POI);
                // simplify road display
                mapTypeStyles[2] = GoogleMapStyleHelper.createSimplifiedStyle(MapTypeStyleFeatureType.ROAD);
                // set water color
                mapTypeStyles[3] = GoogleMapStyleHelper.createColorStyle(MapTypeStyleFeatureType.WATER, WATER_COLOR);
                final StyledMapType styledMapType = StyledMapType.newInstance(ArrayHelper.toJsArray(mapTypeStyles), styledMapTypeOptions);
                map.setCustomMapType(SAILING_ANALYTICS_MAP_TYPE_ID, styledMapType);
                map.getMapTypeRegistry().set(SAILING_ANALYTICS_MAP_TYPE_ID, styledMapType);
                map.setMapTypeId(getMapTypeId(/* wind up */ false, showSatelliteLayer));
                map.setSize("100%", "100%");
                map.addZoomChangeHandler(e->afterZoomOrHeadingChanged());
                map.addHeadingChangeHandler(e->afterZoomOrHeadingChanged());
                map.addDragEndHandler(new DragEndMapHandler() {
                    @Override
                    public void onEvent(DragEndMapEvent event) {
                        // stop automatic zoom after a manual drag event
                        autoZoomIn = false;
                        autoZoomOut = false;
                        final List<RaceMapZoomSettings.ZoomTypes> emptyList = Collections.emptyList();
                        RaceMapZoomSettings clearedZoomSettings = new RaceMapZoomSettings(emptyList,
                                settings.getZoomSettings().isZoomToSelectedCompetitors());
                        settings = new RaceMapSettings
                                .RaceMapSettingsBuilder(settings, raceMapLifecycle.getRaceDTO(), paywallResolver)
                                .withZoomSettings(clearedZoomSettings).build();
                        refreshMapWithoutAnimation();
                    }
                });
                map.addDragStartHandler(event -> {
                    if (streamletOverlay != null 
                            && settings.isShowWindStreamletOverlay()
                            && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceMapLifecycle.getRaceDTO())) {
                        streamletOverlay.onDragStart();
                    }
                });
                map.addIdleHandler(new IdleMapHandler() {
                    @Override
                    public void onEvent(IdleMapEvent event) {
                        // the "idle"-event is raised at the end of map-animations
                        if (autoZoomIn) {
                            // finalize zoom-in that was started with panTo() in zoomMapToNewBounds()
                            map.setZoom(autoZoomLevel);
                            autoZoomIn = false;
                        }
                        if (autoZoomOut) {
                            // finalize zoom-out that was started with setZoom() in zoomMapToNewBounds()
                            map.panTo(coordinateSystem.toLatLng(autoZoomBounds.getCenter()));
                            autoZoomOut = false;
                        }
                        if (streamletOverlay != null 
                                && settings.isShowWindStreamletOverlay()
                                && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceMapLifecycle.getRaceDTO())) {
                            streamletOverlay.onDragEnd();
                            streamletOverlay.onBoundsChanged(/* zoomChanged */ false);
                        }
                        advantageLineLength = getMapDiagonalVisibleDistance();
                        showAdvantageLineAndUpdateWindLadder(getCompetitorsToShow(), getTimer().getTime(), /* timeForPositionTransitionMillis */ -1 /* (no transition) */);
                        refreshMapWithoutAnimation();
                        if (!mapFirstZoomDone) {
                            zoomMapToNewBounds(settings.getZoomSettings().getNewBounds(RaceMap.this));
                            redraw();
                        }
                        if (!isMapInitialized) {
                            RaceMap.this.isMapInitialized = true;
                            mapInitializedListener.forEach(c -> c.run());
                            mapInitializedListener.clear();
                            // ensure at least one redraw after starting the map, but not before other things like modes
                            // initialize, as they might change the timer or other settings
                            redraw();
                        }
                    }
                });
                map.addBoundsChangeHandler(new BoundsChangeMapHandler() {
                    @Override
                    public void onEvent(BoundsChangeMapEvent event) {
                        double newZoomLevel = map.getZoom();
                        if (!isAutoZoomInProgress() && (newZoomLevel != currentZoomLevel)) {
                            removeTransitions();
                        }
                        currentMapBounds = BoundsUtil.getMapBounds(map.getBounds(), new DegreeBearingImpl(map.getHeading()), coordinateSystem);
                        currentZoomLevel = newZoomLevel;
                        headerPanel.getElement().getStyle().setWidth(map.getOffsetWidth(), Unit.PX);
                        advantageLineLength = getMapDiagonalVisibleDistance();
                        showAdvantageLineAndUpdateWindLadder(getCompetitorsToShow(), getTimer().getTime(), /* timeForPositionTransitionMillis */ -1 /* (no transition) */);
                    }
                });
                // If there was a time change before the API was loaded, reset the time
                if (lastTimeChangeBeforeInitialization != null) {
                    timeChanged(lastTimeChangeBeforeInitialization, null);
                    lastTimeChangeBeforeInitialization = null;
                }
                // Initialize streamlet canvas for wind visualization; it shouldn't be doing anything unless it's
                // visible
                streamletOverlay = new WindStreamletsRaceboardOverlay(getMap(), /* zIndex */ 0, timer, raceIdentifier,
                        sailingService, asyncActionsExecutor, stringMessages, coordinateSystem);
                streamletOverlay.addToMap();
                streamletOverlay.setColors(settings.isShowWindStreamletColors());
                if (settings.isShowWindStreamletOverlay() && paywallResolver.hasPermission(
                        SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceMapLifecycle.getRaceDTO())) {
                    streamletOverlay.setVisible(true);
                    streamletOverlay.setCanvasSettings();
                }
                if (settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEAREACIRCLES)) {
                    getAndShowCourseAreaCircles();
                }
                // determine availability of polar diagram
                setHasPolar();
                // initialize simulation canvas
                simulationOverlay = new RaceSimulationOverlay(getMap(), /* zIndex */ 0, raceIdentifier, sailingService,
                        stringMessages, asyncActionsExecutor, coordinateSystem,
                        () -> updateSettings(new RaceMapSettings.RaceMapSettingsBuilder(settings, raceMapLifecycle.getRaceDTO(), paywallResolver)
                                .withShowSimulationOverlay(false).build()));
                simulationOverlay.addToMap();
                showSimulationOverlay(settings.isShowSimulationOverlay() && paywallResolver
                        .hasPermission(SecuredDomainType.TrackedRaceActions.SIMULATOR, raceMapLifecycle.getRaceDTO()));
                metricOverlay = new DetailTypeMetricOverlay(getMap(), 0, coordinateSystem, stringMessages);
                metricOverlay.setVisible(false);
                metricOverlay.addToMap();
                if (showHeaderPanel) {
                    createHeaderPanel(map);
                    if (ClientConfiguration.getInstance().isBrandingActive()) {
                        getHeaderPanel().insert(createSAPLogo(), 0);
                    }
                }
                createAdvancedFunctionsButtonGroup(showMapControls);
                // Data has been initialized
                RaceMap.this.redraw();
                if (trueNorthIndicatorPanel.isVisible()) {
                    trueNorthIndicatorPanel.redraw();
                }
                showAdditionalControls(map);
                RaceMap.this.managedInfoWindow = new ManagedInfoWindow(map);
            }
        };
        sailingService.getGoogleMapsLoaderAuthenticationParams(new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorNoAuthenticationParamsForGoogleMapsFound(caught.getMessage()));
            }

            @Override
            public void onSuccess(String googleMapsLoaderAuthenticationParams) {
                GoogleMapsLoader.load(onLoad, googleMapsLoaderAuthenticationParams);
            }
        });
    }
    
    private void createAdvancedFunctionsButtonGroup(boolean showMapControls) {
        final VerticalPanel sharingAndVideoPanel = new VerticalPanel();
        sharingAndVideoPanel.add(advancedFunctionsButton);
        sharingAndVideoPanel.add(createExitFullScreenButton());
        map.setControls(ControlPosition.RIGHT_TOP, sharingAndVideoPanel);
        
        final HorizontalPanel popupContent = new HorizontalPanel();
        popupContent.addStyleName(raceMapStyle.moreOptions());
        popupContent.add(createZoomOutButton());
        popupContent.add(createZoomInButton());
        popupContent.add(createFullScreenButton());
        popupContent.add(trueNorthIndicatorButtonButtonGroup);
        // add-video button
        if (shareLinkAction != null) {
            final Button shareLinkButton = createShareLinkButton();
            shareLinkButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    hideAdvancedFunctionsPopup();
                }
            });
            popupContent.add(shareLinkButton);
        }
        addVideoToRaceButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hideAdvancedFunctionsPopup();
            }
        });
        popupContent.add(addVideoToRaceButton);
        if (showMapControls) {
            Button settingsButton = createSettingsButton(map);
            settingsButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    hideAdvancedFunctionsPopup();
                }
            });
            popupContent.add(settingsButton);
        }
        advancedFunctionsPopup.addStyleName(raceMapStyle.advancedFunctionsPopup());
        advancedFunctionsPopup.setAnimationEnabled(true);
        advancedFunctionsPopup.setAnimationType(AnimationType.ROLL_DOWN);
        advancedFunctionsPopup.setGlassEnabled(false);
        advancedFunctionsPopup.setModal(false);
        advancedFunctionsPopup.setAutoHideEnabled(true);
        advancedFunctionsPopup.addAutoHidePartner(advancedFunctionsButton.getElement());
        advancedFunctionsButton.setStylePrimaryName(raceMapStyle.moreOptionsButton());
        advancedFunctionsButton.setTitle(stringMessages.moreOptions());
        advancedFunctionsButton.ensureDebugId("moreOptionsButton");
        advancedFunctionsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (advancedFunctionsPopup.isShowing()) {
                    hideAdvancedFunctionsPopup();
                } else {
                    showAdvancedFunctionsPopup();
                }
            }
        });
        advancedFunctionsPopup.add(popupContent);
    }
    
    private void hideAdvancedFunctionsPopup() {
        advancedFunctionsPopup.hide();
    }
    
    private void showAdvancedFunctionsPopup() {
        advancedFunctionsPopup.setPopupPositionAndShow(new PositionCallback() {
            @Override
            public void setPosition(int offsetWidth, int offsetHeight) {
                int left = advancedFunctionsButton.getAbsoluteLeft();
                int top = advancedFunctionsButton.getAbsoluteTop();
                left += advancedFunctionsButton.getOffsetWidth();
                top += advancedFunctionsButton.getOffsetHeight();
                left -= offsetWidth;
                if (left < 0) {
                    left = advancedFunctionsButton.getAbsoluteLeft();
                }
                advancedFunctionsPopup.setPopupPosition(left, top);
            }
        });
    }

    /**
     * Subclasses may define additional stuff to be shown on the map.
     */
    protected void showAdditionalControls(MapWidget map) {
    }

    private void setHasPolar() {
        GetPolarAction getPolar = new GetPolarAction(sailingService, raceIdentifier);
        asyncActionsExecutor.execute(getPolar, GET_POLAR_CATEGORY,
                new MarkedAsyncCallback<>(new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorDeterminingPolarAvailability(
                                raceIdentifier.getRaceName(), caught.getMessage()), /* silent */ true);
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        // store results
                        hasPolar = result.booleanValue();
                    }
                }));

    }

    /**
     * Creates a header panel where additional information can be displayed by using 
     * {@link #getLeftHeaderPanel()} or {@link #getRightHeaderPanel()}. 
     * 
     * This panel is transparent and configured in such a way that it moves other controls
     * down by its height. To achieve the goal of not having added widgets transparent
     * this widget consists of two parts: First one is the transparent panel and the
     * second one is the panel for the controls. The controls then need to moved onto
     * the panel by using CSS.
     */
    private void createHeaderPanel(MapWidget map) {
        // we need a panel that does not have any transparency to have the
        // labels shown in the right color. This panel also needs to have
        // a higher z-index than other elements on the map
        AbsolutePanel panelForLeftHeaderLabels = new AbsolutePanel();
        panelForLeftHeaderLabels.setHeight("60px");
        AbsolutePanel panelForRightHeaderLabels = new AbsolutePanel();
        panelForRightHeaderLabels.setHeight("60px");
        map.setControls(ControlPosition.TOP_LEFT, panelForLeftHeaderLabels);
        panelForLeftHeaderLabels.getElement().getParentElement().getStyle().setProperty("zIndex", "1");
        panelForLeftHeaderLabels.getElement().getStyle().setProperty("overflow", "visible");
        rootPanel.add(panelForRightHeaderLabels);
        panelForRightHeaderLabels.getElement().getStyle().setProperty("zIndex", "1");
        panelForRightHeaderLabels.getElement().getStyle().setProperty("overflow", "visible");
        panelForRightHeaderLabels.getElement().getStyle().setProperty("pointerEvents", "none");
        // need to initialize size before css kicks in to make sure
        // that controls get positioned right
        headerPanel.getElement().getStyle().setHeight(60, Unit.PX);
        headerPanel.getElement().getStyle().setWidth(map.getOffsetWidth(), Unit.PX);
        headerPanel.ensureDebugId("headerPanel");
        // some sort of hack: not positioning TOP_LEFT because then the
        // controls at RIGHT would not get the correct top setting
        map.setControls(ControlPosition.TOP_RIGHT, panelForRightHeaderLabels);
        rootPanel.add(headerPanel);
    }
    
    public FlowPanel getHeaderPanel() {
        return headerPanel;
    }
    
    private Button createSettingsButton(MapWidget map) {
        final Component<RaceMapSettings> component = this;
        Button settingsButton = new Button();
        settingsButton.setStyleName(raceMapStyle.settingsButton());
        settingsButton.ensureDebugId("raceMapSettingsButton");
        settingsButton.setTitle(stringMessages.settings());
        settingsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SettingsDialog<RaceMapSettings> dialog = new SettingsDialog<RaceMapSettings>(component, stringMessages);
                dialog.ensureDebugId("raceMapSettings");
                dialog.show();
            }
        });
        return settingsButton;
    }

    private Button createShareLinkButton() {
        Button shareLinkButton = new Button();
        shareLinkButton.setStyleName(floatingSharingButtonsResources.css().sharing_item());
        shareLinkButton.addStyleName(floatingSharingButtonsResources.css().sharing_itemshare());
        shareLinkButton.addStyleName(raceMapStyle.raceMapShareLinkButton());
        shareLinkButton.ensureDebugId("raceMapShareLink");
        shareLinkButton.setTitle(stringMessages.shareTheLink());
        shareLinkButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                shareLinkAction.run();
            }
        });
        return shareLinkButton;
    }

    private Button createAddVideoToRaceButton() {
        Button addVideoButton = new Button();
        addVideoButton.setStylePrimaryName(raceMapStyle.raceMapVideoUploadButton());
        addVideoButton.setTitle(stringMessages.addMediaTrack());
        addVideoButton.ensureDebugId("addVideoAudioButton");
        addVideoButton.setVisible(false);
        return addVideoButton;
    }

    private Button createZoomInButton() {
        Button zoomInButton = new Button();
        zoomInButton.setStylePrimaryName(raceMapStyle.zoomInButton());
        zoomInButton.setTitle(stringMessages.zoomIn());
        zoomInButton.ensureDebugId("zoomInButton");
        zoomInButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                map.setZoom(map.getZoom() + 1);
            }
        });
        return zoomInButton;
    }

    private Button createFullScreenButton() {
        Button fullScreenButton = new Button();
        fullScreenButton.setStylePrimaryName(raceMapStyle.fullScreenButton());
        fullScreenButton.setTitle(stringMessages.openFullscreenView());
        fullScreenButton.ensureDebugId("fullScreenButton");
        fullScreenButton.setVisible(FullscreenUtil.isFullscreenSupported());
        fullScreenButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hideAdvancedFunctionsPopup();
                FullscreenUtil.requestFullScreenToggle("googleMapsArea");
            }
        });
        return fullScreenButton;
    }

    private Button createExitFullScreenButton() {
        Button exitFullScreenButton = new Button();
        exitFullScreenButton.setStylePrimaryName(raceMapStyle.exitFullScreenButton());
        exitFullScreenButton.setTitle(stringMessages.closeFullscreenView());
        exitFullScreenButton.ensureDebugId("exitFullScreenButton");
        exitFullScreenButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hideAdvancedFunctionsPopup();
                FullscreenUtil.exitFullscreen();
            }
        });
        return exitFullScreenButton;
    }

    private Button createTrueNorthIndicatorButton() {
        Button trueNorthIndicatorButton = new Button();
        trueNorthIndicatorButton.setStylePrimaryName(raceMapStyle.trueNorthIndicatorButton());
        trueNorthIndicatorButton.setTitle(stringMessages.clickToToggleWindUp());
        trueNorthIndicatorButton.ensureDebugId("trueNorthIndicatorButton");
        trueNorthIndicatorButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                trueNorthIndicatorPanel.toggle();
                hideAdvancedFunctionsPopup();
            }
        });
        return trueNorthIndicatorButton;
    }

    private Button createZoomOutButton() {
        Button zoomOutButton = new Button();
        zoomOutButton.setStylePrimaryName(raceMapStyle.zoomOutButton());
        zoomOutButton.setTitle(stringMessages.zoomOut());
        zoomOutButton.ensureDebugId("zoomOutButton");
        zoomOutButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (map.getZoom() > 1) {
                    map.setZoom(map.getZoom() - 1);
                }
            }
        });
        return zoomOutButton;
    }

    private void removeTransitions() {
        // remove the canvas animations for boats
        for (CanvasOverlayV3 boatOverlay : RaceMap.this.getBoatOverlaysByCompetitorIdAsString().values()) {
            boatOverlay.removeCanvasPositionAndRotationTransition();
        }
        // remove the canvas animations for the info overlays of the selected boats
        competitorInfoOverlays.removeTransitions();
        for (CourseMarkOverlay markOverlay : courseMarkOverlays.values()) {
            markOverlay.removeCanvasPositionAndRotationTransition();
        }
        // remove the advantage line animation
        if (advantageTimer != null) {
            advantageTimer.removeAnimation();
        }
    }

    private void redraw() {
        timeChanged(timer.getTime(), null);
    }
    
    Map<String, BoatOverlay> getBoatOverlaysByCompetitorIdAsString() {
        return Collections.unmodifiableMap(boatOverlaysByCompetitorIdsAsStrings);
    }
    
    protected RaceCompetitorSelectionProvider getCompetitorSelection() {
        return competitorSelection;
    }

    protected Timer getTimer() {
        return timer;
    }

    protected RegattaAndRaceIdentifier getRaceIdentifier() {
        return raceIdentifier;
    }

    public MapWidget getMap() {
        return map;
    }
    
    public RaceSimulationOverlay getSimulationOverlay() {
        return simulationOverlay;
    }
    
    @Override
    public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfos, long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest, long clientTimeWhenResponseWasReceived) {
        timer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
        this.lastRaceTimesInfo = raceTimesInfos.get(raceIdentifier);
    }

    /**
     * In {@link PlayModes#Live live mode}, when {@link #loadCompleteLeaderboard(Date) loading the leaderboard contents}, <code>null</code>
     * is used as time point. The condition for this is encapsulated in this method so others can find out. For example, when a time change
     * is signaled due to local offset / delay adjustments, no additional call to {@link #loadCompleteLeaderboard(Date)} would be required
     * as <code>null</code> will be passed in any case, not being affected by local time offsets.
     */
    private boolean useNullAsTimePoint() {
        return timer.getPlayMode() == PlayModes.Live;
    }

    private void refreshMapWithoutAnimation() {
        removeTransitions();
    }

    private void updateMapWithWindInfo(final Date newTime, final long transitionTimeInMillis,
            final Iterable<CompetitorDTO> competitorsToShow, final WindInfoForRaceDTO windInfo,
            final List<com.sap.sse.common.Util.Pair<WindSource, WindTrackInfoDTO>> windSourcesToShow) {
        showAdvantageLineAndUpdateWindLadder(competitorsToShow, newTime, transitionTimeInMillis);
        for (WindSource windSource : windInfo.windTrackInfoByWindSource.keySet()) {
            WindTrackInfoDTO windTrackInfoDTO = windInfo.windTrackInfoByWindSource.get(windSource);
            switch (windSource.getType()) {
            case EXPEDITION:
            case WINDFINDER:
                windSourcesToShow.add(new com.sap.sse.common.Util.Pair<WindSource, WindTrackInfoDTO>(windSource, windTrackInfoDTO));
                break;
            case COMBINED:
                showCombinedWindOnMap(windSource, windTrackInfoDTO);
                if (requiresCoordinateSystemUpdateWhenCoursePositionAndWindDirectionIsKnown) {
                    updateCoordinateSystemFromSettings();
                }
                break;
            default:
                // Which wind sources are requested is defined in a list above this
                // action. So we throw here an exception to notice a missing source.
                throw new UnsupportedOperationException("There is currently no support for the enum value '"
                        + windSource.getType() + "' in this method.");
            }
        }
    }

    private void refreshMap(final Date newTime, final long transitionTimeInMillis, boolean isRedraw) {
        final Iterable<CompetitorDTO> competitorsToShow = getCompetitorsToShow();
        final Pair<PositionRequest, PositionRequest> quickAndSlowRequest = fixesAndTails
                .computeFromAndTo(newTime, competitorsToShow, settings.getEffectiveTailLengthInMilliseconds(), transitionTimeInMillis, selectedDetailType);
        // Request map data update, possibly in two calls; see method details
        callGetRaceMapDataForAllOverlappingAndTipsOfNonOverlappingAndGetBoatPositionsForAllOthers(quickAndSlowRequest,
                raceIdentifier, newTime, transitionTimeInMillis, competitorsToShow, isRedraw, selectedDetailType,
                selectedDetailTypeChanged);
        // draw the wind into the map, get the combined wind
        // TODO bug5586 shouldn't we extend this to fetch the one decisive averaged fix from *all* relevant sources?
        // TODO bug5586 Then we could push the wind data received here into the WindInfoForRaceVectorField instance providing the data and
        // TODO bug5586 interpolation for the streamlet overlay if switched on.
        List<String> windSourceTypeNames = new ArrayList<String>();
        windSourceTypeNames.add(WindSourceType.EXPEDITION.name());
        windSourceTypeNames.add(WindSourceType.WINDFINDER.name());
        windSourceTypeNames.add(WindSourceType.COMBINED.name());
        GetWindInfoAction getWindInfoAction = new GetWindInfoAction(sailingService, raceIdentifier, newTime, 1000L,
                1, windSourceTypeNames,
                /* onlyUpToNewestEvent==false means get us any data we can get by a best effort */ false);
        asyncActionsExecutor.execute(getWindInfoAction, GET_WIND_DATA_CATEGORY,
                new AsyncCallback<WindInfoForRaceDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error obtaining wind information: " + caught.getMessage(),
                                true /* silentMode */);
                    }

                    @Override
                    public void onSuccess(WindInfoForRaceDTO windInfo) {
                        if (windInfo != null) {
                            List<com.sap.sse.common.Util.Pair<WindSource, WindTrackInfoDTO>> windSourcesToShow = new ArrayList<com.sap.sse.common.Util.Pair<WindSource, WindTrackInfoDTO>>();
                            lastCombinedWindTrackInfoDTO = windInfo;
                            updateMapWithWindInfo(newTime, transitionTimeInMillis, competitorsToShow, windInfo, windSourcesToShow);
                            showWindSensorsOnMap(windSourcesToShow);
                        }
                    }
                });
    }

    @Override
    public void timeChanged(final Date newTime, final Date oldTime) {
        boolean isRedraw = oldTime == null;
        if (isMapInitialized) {
            if (newTime != null) {
                if (raceIdentifier != null) {
                    final long transitionTimeInMillis = calculateTimeForPositionTransitionInMillis(newTime, oldTime);
                    refreshMap(newTime, transitionTimeInMillis, isRedraw);
                }
            }
        }
    }

    public WindInfoForRaceDTO getLastCombinedWindTrackInfoDTO() {
        return lastCombinedWindTrackInfoDTO;
    }
    
    /**
     * Requests updates for map data and, when received, updates the map structures accordingly.
     * <p>
     * 
     * The update can happen in one or two round trips. We assume that overlapping segments usually don't require a lot
     * of loading time as the most typical case will be to update a longer tail with a few new fixes that were received
     * since the last time tick. These supposedly fast requests are handled by a {@link GetRaceMapDataAction} which also
     * requests updates for mark positions, sidelines and quick ranks. The same request also loads boat positions for
     * the zero-length interval at <code>newTime</code> for the non-overlapping tails assuming that this will work
     * fairly fast and in particular in O(1) time regardless of tail length, compared to fetching the entire tail for
     * all competitors. This will at least provide quick feedback about those competitors' positions even if loading
     * their entire tail may take a little longer. The {@link GetRaceMapDataAction} therefore is intended to be a rather
     * quick call.
     * <p>
     * 
     * Non-overlapping position requests typically occur for the first request when no fix at all is known for the
     * competitor yet, or when the user has radically moved the time slider to some other time such that given the
     * current tail length setting the new tail segment does not overlap with the old one, requiring a full load of the
     * entire tail data for that competitor. For these non-overlapping requests, this method creates a
     * {@link GetBoatPositionsAction} request loading the entire tail required, but not quick ranks, sidelines and mark
     * positions. Updating the results of this call is done in {@link #updateBoatPositions(Date, long, Map, Iterable, Map, boolean)}.
     * <p>
     */
    private void callGetRaceMapDataForAllOverlappingAndTipsOfNonOverlappingAndGetBoatPositionsForAllOthers(
            final Pair<PositionRequest, PositionRequest> quickAndSlowRequest,
            RegattaAndRaceIdentifier race, final Date newTime, final long transitionTimeInMillis,
            final Iterable<CompetitorDTO> competitorsToShow, boolean isRedraw, final DetailType detailType, boolean detailTypeChanged) {
        final Map<String, Date> fromTimesForQuickCall = quickAndSlowRequest.getA().getFromByCompetitorIdAsString();
        final Map<String, Date> toTimesForQuickCall = quickAndSlowRequest.getA().getToByCompetitorIdAsString();
        final Map<String, Date> fromTimesForNonOverlappingTailsCall = quickAndSlowRequest.getB().getFromByCompetitorIdAsString();
        final Map<String, Date> toTimesForNonOverlappingTailsCall = quickAndSlowRequest.getB().getToByCompetitorIdAsString();
        final Map<String, CompetitorDTO> competitorsByIdAsString = new HashMap<>();
        for (CompetitorDTO competitor : competitorSelection.getAllCompetitors()) {
            competitorsByIdAsString.put(competitor.getIdAsString(), competitor);
        }
        // only update the tails for these competitors
        asyncActionsExecutor.execute(new GetRaceMapDataAction(sailingService, timeRangeActionsExecutor, competitorsByIdAsString,
                    race, useNullAsTimePoint() ? null : newTime, fromTimesForQuickCall, toTimesForQuickCall, /* extrapolate */ true,
                    (settings.isShowSimulationOverlay() ? simulationOverlay.getLegIdentifier() : null),
                    raceCompetitorSet.getMd5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID(),
                    newTime, settings.isShowEstimatedDuration(), detailType, leaderboardName, leaderboardGroupName, leaderboardGroupId,
                    // callback to use for alternative GetBoatPositionsAction fired when GetRaceMapDataAction gets dropped:
                    getBoatPositionsCallback(quickAndSlowRequest.getA(), newTime, transitionTimeInMillis, competitorsToShow, detailType, detailTypeChanged, competitorsByIdAsString)),
            GET_RACE_MAP_DATA_CATEGORY,
            getRaceMapDataCallback(newTime, transitionTimeInMillis, quickAndSlowRequest.getA(), competitorsToShow,
                                   ++boatPositionRequestIDCounter, isRedraw, detailTypeChanged, detailType, fromTimesForQuickCall, toTimesForQuickCall, competitorsByIdAsString));
        // next, if necessary, do the full thing; the two calls have different action classes, so throttling should not drop one for the other
        if (!fromTimesForNonOverlappingTailsCall.keySet().isEmpty()) {
            timeRangeActionsExecutor.execute(new GetBoatPositionsAction(sailingService, race,
                    fromTimesForNonOverlappingTailsCall, toTimesForNonOverlappingTailsCall, /* extrapolate */ true,
                    detailType, leaderboardName, leaderboardGroupName, leaderboardGroupId),
                    getBoatPositionsCallback(quickAndSlowRequest.getB(), newTime, transitionTimeInMillis, competitorsToShow,
                            detailType, detailTypeChanged, competitorsByIdAsString));
        }
    }

    private GetBoatPositionsCallback getBoatPositionsCallback(
            final PositionRequest positionRequest, final Date newTime,
            final long transitionTimeInMillis, final Iterable<CompetitorDTO> competitorsToShow,
            final DetailType detailType, boolean detailTypeChanged,
            final Map<String, CompetitorDTO> competitorsByIdAsString) {
        return new GetBoatPositionsCallback(detailType, new AsyncCallback<CompactBoatPositionsDTO>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error obtaining racemap data: " + t.getMessage(), true /*silentMode */);
            }

            @Override
            public void onSuccess(CompactBoatPositionsDTO result) {
                // Note: the fromAndToAndOverlap.getC() map will be UPDATED by the call to updateBoatPositions for those
                // entries that are considered not overlapping; subsequently, fromAndToOverlap.getC() will contain true for
                // all its entries so that the other response received for GetRaceMapDataAction will consider this an
                // overlap if it happens after this update.
                final Map<CompetitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable> boatPositionsForCompetitors =
                        result.getBoatPositionsForCompetitors(competitorsByIdAsString);
                positionRequest.processResponse(boatPositionsForCompetitors);
                updateBoatPositions(newTime, transitionTimeInMillis,
                        competitorsToShow, boatPositionsForCompetitors, /* updateTailsOnly */ true, detailTypeChanged, detailType);
            }
        });
    }

    private AsyncCallback<RaceMapDataDTO> getRaceMapDataCallback(
            final Date newTime,
            final long transitionTimeInMillis,
            final PositionRequest quickRequest,
            final Iterable<CompetitorDTO> competitorsToShow, final int requestID, boolean isRedraw, boolean detailTypeChanged,
            DetailType detailType, final Map<String, Date> fromTimesForQuickCall, final Map<String, Date> toTimesForQuickCall,
            final Map<String, CompetitorDTO> competitorsByIdAsString) {
        return new MarkedAsyncCallback<>(new AsyncCallback<RaceMapDataDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error obtaining racemap data: " + caught.getMessage(), true /*silentMode */);
            }
            
            @Override
            public void onSuccess(RaceMapDataDTO raceMapDataDTO) {
                if (map != null && raceMapDataDTO != null) {
                    final Map<CompetitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable> boatData = raceMapDataDTO.boatPositions;
                    quickRequest.processResponse(boatData); // stores the boat fixes received into the FixesAndTails cache
                    // process the rest of the response only if not received out of order
                    if (startedProcessingRequestID < requestID) {
                        startedProcessingRequestID = requestID;
                        // Uncomment the following for enhanced log output regarding getRaceMapData requests
                        // GWT.log("Processing race map data request "+requestID+" with detail type "+detailType+"\n"+getFromAndToTimesAsString());
                        if (raceMapDataDTO.raceCompetitorIdsAsStrings != null) {
                            try {
                                raceCompetitorSet.setIdsAsStringsOfCompetitorsInRace(raceMapDataDTO.raceCompetitorIdsAsStrings);
                            } catch (Exception e) {
                                GWT.log("Error trying to update competitor set for race "+raceIdentifier.getRaceName()+
                                        " in regatta "+raceIdentifier.getRegattaName(), e);
                            }
                        }
                        quickFlagDataProvider.quickRanksReceivedFromServer(raceMapDataDTO.quickRanks);
                        if (settings.isShowSimulationOverlay() && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.SIMULATOR, raceMapLifecycle.getRaceDTO())) {
                            lastLegNumber = raceMapDataDTO.coursePositions.currentLegNumber;
                            simulationOverlay.updateLeg(Math.max(lastLegNumber, 1), /* clearCanvas */ false, raceMapDataDTO.simulationResultVersion);
                        }
                        final Map<String, Double> quickSpeedsFromServerInKnotsByCompetitorIdAsString = getCompetitorsSpeedInKnotsMap(boatData); // TODO: why do we need this from the *response*, and why couldn't this come straight from the FixesAndTails cache?
                        quickFlagDataProvider.quickSpeedsInKnotsReceivedFromServer(quickSpeedsFromServerInKnotsByCompetitorIdAsString, competitorsByIdAsString);
                        // Do boat specific actions
                        updateBoatPositions(newTime, transitionTimeInMillis,
                                competitorsToShow, boatData, /* updateTailsOnly */ false, detailTypeChanged, detailType);
                        if (!isRedraw) {
                            // only remove markers if the time is actually changed
                            if (douglasMarkers != null) {
                                removeAllMarkDouglasPeuckerpoints();
                            }
                            maneuverMarkersAndLossIndicators.clearAllManeuverMarkers();
                        }
                        if (requiresCoordinateSystemUpdateWhenCoursePositionAndWindDirectionIsKnown) {
                            updateCoordinateSystemFromSettings();
                        }
                        // Do mark specific actions
                        showCourseMarksOnMap(raceMapDataDTO.coursePositions, transitionTimeInMillis);
                        showCourseSidelinesOnMap(raceMapDataDTO.courseSidelines);
                        showStartAndFinishAndCourseMiddleLines(raceMapDataDTO.coursePositions);
                        showStartLineToFirstMarkTriangle(raceMapDataDTO.coursePositions);
                        // Rezoom the map
                        NonCardinalBounds zoomToBounds = null;
                        if (!settings.getZoomSettings().containsZoomType(ZoomTypes.NONE)) {
                            // Auto zoom if setting is not manual
                            zoomToBounds = settings.getZoomSettings().getNewBounds(RaceMap.this);
                            if (zoomToBounds == null && !mapFirstZoomDone) {
                                // the user-specified zoom couldn't find what it was looking for; try defaults once
                                zoomToBounds = getDefaultZoomBounds();
                            }
                        }
                        if (!mapFirstZoomDone) {
                            // Zoom once to the marks if marks exist
                            zoomToBounds = new CourseMarksBoundsCalculator().calculateNewBounds(RaceMap.this);
                            if (zoomToBounds == null) {
                                // use default zoom, e.g.,
                                zoomToBounds = getDefaultZoomBounds();
                            }
                            /*
                             * Reset the mapZoomedOrPannedSinceLastRaceSelection: In spite of the fact that the map was
                             * just zoomed to the bounds of the marks, it was not a zoom or pan triggered by the user.
                             * As a consequence the mapZoomedOrPannedSinceLastRaceSelection option has to reset again.
                             */
                        }
                        zoomMapToNewBounds(zoomToBounds);
                        updateEstimatedDuration(raceMapDataDTO.estimatedDuration);
                    } else {
                        GWT.log("Dropped result from getRaceMapData(...) except for boat positions with detail type "+detailType+
                                " because it was for request ID "+requestID+
                                " while we already started processing request "+startedProcessingRequestID+"\n"+
                                getFromAndToTimesAsString());
                    }
                } else { // map was null or we didn't get a valid response; record time in case this was because map API hasn't loaded yet
                    lastTimeChangeBeforeInitialization = newTime;
                }
            }

            private String getFromAndToTimesAsString() {
                final StringBuilder result = new StringBuilder();
                for (final Entry<String, Date> from : fromTimesForQuickCall.entrySet()) {
                    result.append(from.getKey());
                    result.append(": ");
                    result.append(from.getValue());
                    result.append("..");
                    result.append(toTimesForQuickCall.get(from.getKey()));
                    result.append("; ");
                }
                return result.toString();
            }

            private Map<String, Double> getCompetitorsSpeedInKnotsMap(
                    Map<CompetitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable> boatData) {
                final Map<String, Double> quickSpeedsFromServerInKnots = new HashMap<>();
                for (Entry<CompetitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable> boatDataEntry : boatData.entrySet()) {
                    GPSFixDTOWithSpeedWindTackAndLegTypeIterable fixesList = boatDataEntry.getValue();
                    if (!fixesList.isEmpty()) {
                        SpeedWithBearingDTO speedWithBearing = fixesList.last().speedWithBearing;
                        if (speedWithBearing != null) {
                            Double speedInKnots = speedWithBearing.speedInKnots;
                            quickSpeedsFromServerInKnots.put(boatDataEntry.getKey().getIdAsString(), speedInKnots);
                        }
                    }
                }
                return quickSpeedsFromServerInKnots;
            }
        });
    }

    protected void updateEstimatedDuration(Duration estimatedDuration) {
        if (estimatedDuration == null) {
            return;
        }
        if (estimatedDurationOverlay == null) {
            estimatedDurationOverlay = new Label("");
            estimatedDurationOverlay.setStyleName(raceMapStyle.estimatedDuration());
            if(showHeaderPanel) {
                estimatedDurationOverlay.addStyleName(raceMapStyle.estimatedDurationWithHeader());
            }
            map.setControls(ControlPosition.TOP_CENTER, estimatedDurationOverlay);
        }
        estimatedDurationOverlay.setText(stringMessages.estimatedDuration()
                + " " + DateAndTimeFormatterUtil.formatElapsedTime(estimatedDuration.asMillis()));

    }

    /**
     * Assumes that the fixes required for displaying the boat position have been received and updated to
     * the {@link FixesAndTails} cache already.
     * 
     * @param hasTailOverlapForCompetitor
     *            if for a competitor whose fixes are provided in <code>boatData</code> this holds <code>false</code>,
     *            any fixes previously stored for that competitor are removed, and the tail is deleted from the map (see
     *            {@link #removeTail(CompetitorWithBoatDTO)}); the new fixes are then added to the {@link #fixes} map,
     *            and a new tail will have to be constructed as needed (does not happen here). If this map holds
     *            <code>true</code>, {@link #mergeFixes(CompetitorWithBoatDTO, List, long)} is used to merge the new
     *            fixes from <code>fixesForCompetitors</code> into the {@link #fixes} collection, and the tail is left
     *            unchanged. <b>NOTE:</b> When a non-overlapping set of fixes is updated (<code>false</code>), this
     *            map's record for the competitor is <b>UPDATED</b> to <code>true</code> after the tail deletion and
     *            {@link #fixes} replacement has taken place. This helps in cases where this update is only one of two
     *            into which an original request was split (one quick update of the tail's head and another one for the
     *            longer tail itself), such that the second request that uses the <em>same</em> map will be considered
     *            having an overlap now, not leading to a replacement of the previous update originating from the same
     *            request. See also {@link FixesAndTails#updateFixes(Map, Map, TailFactory, long, DetailType)}.
     * @param updateTailsOnly
     *            if <code>true</code>, only the tails are updated according to <code>boatData</code> and
     *            <code>hasTailOverlapForCompetitor</code>, but the advantage line is not updated, and neither are the
     *            competitor info bubbles moved. This assumes that the tips of these tails are loaded in a separate call
     *            which <em>does</em> update those structures. In particular, tails that do not appear in
     *            <code>boatData</code> are not removed from the map in case <code>updateTailsOnly</code> is
     *            <code>true</code>.
     * @param detailType
     *            the detail type the {@code boatData} fixes contain detail values for; used for consistency check in
     *            {@link FixesAndTails} cache.
     */
    private void updateBoatPositions(final Date newTime, final long transitionTimeInMillis,
            final Iterable<CompetitorDTO> competitorsToShow, Map<CompetitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable> boatData,
            boolean updateTailsOnly, boolean detailTypeChanged, DetailType detailType) {
        showBoatsOnMap(newTime, transitionTimeInMillis,
                /* re-calculate; it could have changed since the asynchronous request was made: */
                getCompetitorsToShow(), updateTailsOnly, detailType);
        if (detailTypeChanged) {
            selectedDetailTypeChanged = false;
            tailColorMapper.notifyListeners();
        }
        if (!updateTailsOnly) {
            showCompetitorInfoOnMap(newTime, transitionTimeInMillis,
                    competitorSelection.getSelectedFilteredCompetitors());
            // even though the wind data is retrieved by a separate call, re-draw the advantage line because it
            // needs to adjust to new boat positions
            showAdvantageLineAndUpdateWindLadder(competitorsToShow, newTime, transitionTimeInMillis);
        }
    }

    private void showCourseSidelinesOnMap(List<SidelineDTO> sidelinesDTOs) {
        if (map != null && sidelinesDTOs != null ) {
            Map<SidelineDTO, Polygon> toRemoveSidelines = new HashMap<SidelineDTO, Polygon>(courseSidelines);
            for (SidelineDTO sidelineDTO : sidelinesDTOs) {
                if (sidelineDTO.getMarks().size() == 2) { // right now we only support sidelines with 2 marks
                    Polygon sideline = courseSidelines.get(sidelineDTO);
                    LatLng[] sidelinePoints = new LatLng[sidelineDTO.getMarks().size()];
                    int i=0;
                    for (MarkDTO sidelineMark : sidelineDTO.getMarks()) {
                        sidelinePoints[i] = coordinateSystem.toLatLng(sidelineMark.position);
                        i++;
                    }
                    if (sideline == null) {
                        PolygonOptions options = PolygonOptions.newInstance();
                        options.setClickable(true);
                        options.setStrokeColor("#0000FF");
                        options.setStrokeWeight(1);
                        options.setStrokeOpacity(1.0);
                        options.setFillColor(null);
                        options.setFillOpacity(1.0);
                        
                        sideline = Polygon.newInstance(options);
                        MVCArray<LatLng> pointsAsArray = MVCArray.newInstance(sidelinePoints);
                        sideline.setPath(pointsAsArray);

                        sideline.addMouseOverHandler(new MouseOverMapHandler() {
                            @Override
                            public void onEvent(MouseOverMapEvent event) {
                                map.setTitle(stringMessages.sideline());
                            }
                        });
                        sideline.addMouseOutMoveHandler(new MouseOutMapHandler() {
                            @Override
                            public void onEvent(MouseOutMapEvent event) {
                                map.setTitle("");
                            }
                        });
                        courseSidelines.put(sidelineDTO, sideline);
                        sideline.setMap(map);
                    } else {
                        sideline.getPath().removeAt(1);
                        sideline.getPath().removeAt(0);
                        sideline.getPath().insertAt(0, sidelinePoints[0]);
                        sideline.getPath().insertAt(1, sidelinePoints[1]);
                        toRemoveSidelines.remove(sidelineDTO);
                    }
                }
            }
            for (SidelineDTO toRemoveSideline : toRemoveSidelines.keySet()) {
                Polygon sideline = courseSidelines.remove(toRemoveSideline);
                sideline.setMap(null);
            }
        }
    }
    
    private void showCourseMarksOnMap(CoursePositionsDTO courseDTO, long transitionTimeInMillis) {
        if (map != null && courseDTO != null) {
            WaypointDTO endWaypointForCurrentLegNumber = null;
            if (courseDTO.currentLegNumber > 0 && courseDTO.currentLegNumber <= courseDTO.totalLegsCount) {
                endWaypointForCurrentLegNumber = courseDTO.getEndWaypointForLegNumber(courseDTO.currentLegNumber);
            }
            Map<String, CourseMarkOverlay> toRemoveCourseMarks = new HashMap<String, CourseMarkOverlay>(courseMarkOverlays);
            if (courseDTO.marks != null) {
                for (MarkDTO markDTO : courseDTO.marks) {
                    boolean isSelected = false;
                    if (endWaypointForCurrentLegNumber != null && Util.contains(endWaypointForCurrentLegNumber.controlPoint.getMarks(), markDTO)) {
                        isSelected = true;
                    }
                    CourseMarkOverlay courseMarkOverlay = courseMarkOverlays.get(markDTO.getIdAsString());
                    if (courseMarkOverlay == null) {
                        courseMarkOverlay = new CourseMarkOverlay(map, RaceMapOverlaysZIndexes.COURSEMARK_ZINDEX, markDTO, coordinateSystem, courseDTO);
                        courseMarkOverlay.setShowBuoyZone(settings.getHelpLinesSettings().isVisible(HelpLineTypes.BUOYZONE));
                        courseMarkOverlay.setBuoyZoneRadius(settings.getBuoyZoneRadius());
                        courseMarkOverlay.setSelected(isSelected);
                        courseMarkOverlays.put(markDTO.getIdAsString(), courseMarkOverlay);
                        markDTOs.put(markDTO.getIdAsString(), markDTO);
                        registerCourseMarkInfoWindowClickHandler(markDTO.getIdAsString());
                        courseMarkOverlay.addToMap();
                    } else {
                        courseMarkOverlay.setMarkPosition(markDTO.position, transitionTimeInMillis);
                        courseMarkOverlay.setShowBuoyZone(settings.getHelpLinesSettings().isVisible(HelpLineTypes.BUOYZONE));
                        courseMarkOverlay.setBuoyZoneRadius(settings.getBuoyZoneRadius());
                        courseMarkOverlay.setSelected(isSelected);
                        courseMarkOverlay.setCourse(courseDTO);
                        courseMarkOverlay.draw();
                        toRemoveCourseMarks.remove(markDTO.getIdAsString());
                    }
                }
            }
            for (String toRemoveMarkIdAsString : toRemoveCourseMarks.keySet()) {
                final CourseMarkOverlay removedOverlay = courseMarkOverlays.remove(toRemoveMarkIdAsString);
                if (removedOverlay != null) {
                    removedOverlay.removeFromMap();
                }
            }
        }
    }
    
    /**
     * Based on the mark positions in {@link #courseMarkOverlays}' values this method determines the center of gravity of these marks'
     * {@link CourseMarkOverlay#getPosition() positions}.
     */
    private Position getCenterOfCourse() {
        ScalablePosition center = null;
        int count = 0;
        for (CourseMarkOverlay markOverlay : courseMarkOverlays.values()) {
            ScalablePosition markPosition = new ScalablePosition(markOverlay.getPosition());
            if (center == null) {
                center = markPosition;
            } else {
                center.add(markPosition);
            }
            count++;
        }
        return center == null ? null : center.divide(count);
    }

    private void showCombinedWindOnMap(WindSource windSource, WindTrackInfoDTO windTrackInfoDTO) {
        if (map != null) {
            combinedWindPanel.setWindInfo(windTrackInfoDTO, windSource);
            combinedWindPanel.redraw();
        }
    }

    private void showWindSensorsOnMap(List<com.sap.sse.common.Util.Pair<WindSource, WindTrackInfoDTO>> windSensorsList) {
        if (map != null) {
            final Set<WindSource> toRemoveWindSources = new HashSet<WindSource>(windSensorOverlays.keySet());
            for (com.sap.sse.common.Util.Pair<WindSource, WindTrackInfoDTO> windSourcePair : windSensorsList) {
                final WindSource windSource = windSourcePair.getA(); 
                final WindTrackInfoDTO windTrackInfoDTO = windSourcePair.getB();
                WindSensorOverlay windSensorOverlay = windSensorOverlays.get(windSource);
                if (windSensorOverlay == null) {
                    windSensorOverlay = createWindSensorOverlay(RaceMapOverlaysZIndexes.WINDSENSOR_ZINDEX, windSource, windTrackInfoDTO);
                    windSensorOverlays.put(windSource, windSensorOverlay);
                    windSensorOverlay.addToMap();
                } else {
                    windSensorOverlay.setWindInfo(windTrackInfoDTO, windSource);
                    windSensorOverlay.draw();
                    toRemoveWindSources.remove(windSource);
                }
            }
            for (WindSource toRemoveWindSource : toRemoveWindSources) {
                WindSensorOverlay removedWindSensorOverlay = windSensorOverlays.remove(toRemoveWindSource);
                if (removedWindSensorOverlay != null) {
                    removedWindSensorOverlay.removeFromMap();
                }
            }
        }
    }

    private void showCompetitorInfoOnMap(final Date newTime, final long timeForPositionTransitionMillis, final Iterable<CompetitorDTO> competitorsToShow) {
        if (map != null) {
            if (settings.isShowSelectedCompetitorsInfo()) {
                Set<String> toRemoveCompetorInfoOverlaysIdsAsStrings = new HashSet<>();
                Util.addAll(competitorInfoOverlays.getCompetitorIdsAsStrings(), toRemoveCompetorInfoOverlaysIdsAsStrings);
                for (CompetitorDTO competitorDTO : competitorsToShow) {
                    if (fixesAndTails.hasFixesFor(competitorDTO)) {
                        GPSFixDTOWithSpeedWindTackAndLegType lastBoatFix = getBoatFix(competitorDTO, newTime);
                        if (lastBoatFix != null) {
                            CompetitorInfoOverlay competitorInfoOverlay = competitorInfoOverlays.get(competitorDTO);
                            final Integer rank = getRank(competitorDTO);
                            final Double speed = getSpeedInKnots(competitorDTO);
                            if (competitorInfoOverlay == null) {
                                competitorInfoOverlay = competitorInfoOverlays.createCompetitorInfoOverlay(RaceMapOverlaysZIndexes.INFO_OVERLAY_ZINDEX, competitorDTO,
                                        lastBoatFix, rank, speed, timeForPositionTransitionMillis);
                                competitorInfoOverlay.addToMap();
                            } else {
                                competitorInfoOverlays.updatePosition(competitorDTO, lastBoatFix, timeForPositionTransitionMillis);
                            }
                            toRemoveCompetorInfoOverlaysIdsAsStrings.remove(competitorDTO.getIdAsString());
                        }
                    }
                }
                for (String toRemoveCompetitorOverlayIdAsString : toRemoveCompetorInfoOverlaysIdsAsStrings) {
                    competitorInfoOverlays.remove(toRemoveCompetitorOverlayIdAsString);
                }
            } else {
                // remove all overlays
                competitorInfoOverlays.clear();
            }
        }
    }

    private long calculateTimeForPositionTransitionInMillis(final Date newTime, final Date oldTime) {
        final long timeForPositionTransitionMillisSmoothed;
        final long timeForPositionTransitionMillis;
        if (newTime != null && oldTime != null) {
            timeForPositionTransitionMillis = newTime.getTime() - oldTime.getTime();
        } else {
            timeForPositionTransitionMillis = -1;
        }
        if (timer.getPlayState() == PlayStates.Playing) {
            // choose 130% of the refresh interval as transition period to make it unlikely that the transition
            // stops before the next update has been received
            long smoothInterval = 1300 * timer.getRefreshInterval() / 1000;
            if (timeForPositionTransitionMillis > 0 && timeForPositionTransitionMillis < smoothInterval) {
                timeForPositionTransitionMillisSmoothed = smoothInterval;
            } else {
                // either a large positive transition happend or any negative one, do not use the smooth
                // value
                if (timeForPositionTransitionMillis > 0) {
                    timeForPositionTransitionMillisSmoothed = timeForPositionTransitionMillis;
                } else {
                    timeForPositionTransitionMillisSmoothed = -1;
                }
            }

        } else {
            // do not animate in non live modus
            timeForPositionTransitionMillisSmoothed = -1; // -1 means 'no transition
        }
        return timeForPositionTransitionMillisSmoothed;
    }
    
    /**
     * @param updateTailsOnly
     *            if <code>false</code>, tails of competitors not in <code>competitorsToShow</code> are removed from the
     *            map
     * @param detailTypeToShow
     *            the detail type expected to be shown on the fixes; used for consistency check for now
     */
    private void showBoatsOnMap(final Date newTime, final long timeForPositionTransitionMillis,
            final Iterable<CompetitorDTO> competitorsToShow, boolean updateTailsOnly, DetailType detailTypeToShow) {
        if (map != null) {
            Date tailsFromTime = new Date(newTime.getTime() - settings.getEffectiveTailLengthInMilliseconds());
            Date tailsToTime = newTime;
            Set<String> competitorIdsAsStringOfUnusedTails = new HashSet<>();
            Set<String> competitorIdsAsStringOfUnusedBoatCanvases = new HashSet<>();
            if (!updateTailsOnly) {
                competitorIdsAsStringOfUnusedTails.addAll(fixesAndTails.getCompetitorIdsAsStringWithTails());
                competitorIdsAsStringOfUnusedBoatCanvases.addAll(boatOverlaysByCompetitorIdsAsStrings.keySet());
            }
            if (timeForPositionTransitionMillis > 3 * timer.getRefreshInterval()) {
                fixesAndTails.clearTails();
            }
            for (CompetitorDTO competitorDTO : competitorsToShow) {
                if (fixesAndTails.hasFixesFor(competitorDTO)) {
                    if (!fixesAndTails.hasTail(competitorDTO)) {
                        fixesAndTails.createTailAndUpdateIndices(competitorDTO, tailsFromTime, tailsToTime, this, detailTypeToShow);
                    } else {
                        fixesAndTails.updateTail(competitorDTO, tailsFromTime, tailsToTime,
                                (int) (timeForPositionTransitionMillis == -1 ? -1
                                        : timeForPositionTransitionMillis / 2), detailTypeToShow);
                        if (!updateTailsOnly) {
                            competitorIdsAsStringOfUnusedTails.remove(competitorDTO.getIdAsString());
                        }
                    }
                    boolean usedExistingBoatCanvas = updateBoatCanvasForCompetitor(competitorDTO, newTime,
                            timeForPositionTransitionMillis);
                    if (usedExistingBoatCanvas && !updateTailsOnly) {
                        competitorIdsAsStringOfUnusedBoatCanvases.remove(competitorDTO.getIdAsString());
                    }
                }
            }
            if (selectedDetailType != null) {
                fixesAndTails.updateDetailValueBoundaries(competitorSelection.getSelectedCompetitors());
            }
            if (!updateTailsOnly) {
                for (String unusedBoatCanvasCompetitorDTO : competitorIdsAsStringOfUnusedBoatCanvases) {
                    CanvasOverlayV3 boatCanvas = boatOverlaysByCompetitorIdsAsStrings.get(unusedBoatCanvasCompetitorDTO);
                    boatCanvas.removeFromMap();
                    boatOverlaysByCompetitorIdsAsStrings.remove(unusedBoatCanvasCompetitorDTO);
                }
                for (String unusedTailCompetitorDTO : competitorIdsAsStringOfUnusedTails) {
                    fixesAndTails.removeTail(unusedTailCompetitorDTO);
                }
            }
        }
    }

    /**
     * This algorithm is limited to distances such that dlon < pi/2, i.e., those that extend around less than one
     * quarter of the circumference of the earth in longitude. A completely general, but more complicated algorithm is
     * necessary if greater distances are allowed.
     */
    public LatLng calculatePositionAlongRhumbline(LatLng position, double bearingDeg, Distance distance) {
        double distanceRad = distance.getCentralAngleRad(); 
        double lat1 = position.getLatitude() / 180. * Math.PI;
        double lon1 = position.getLongitude() / 180. * Math.PI;
        double bearingRad = bearingDeg / 180. * Math.PI;
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distanceRad) + 
                        Math.cos(lat1) * Math.sin(distanceRad) * Math.cos(bearingRad));
        double lon2 = lon1 + Math.atan2(Math.sin(bearingRad)*Math.sin(distanceRad)*Math.cos(lat1), 
                       Math.cos(distanceRad)-Math.sin(lat1)*Math.sin(lat2));
        lon2 = (lon2+3*Math.PI) % (2*Math.PI) - Math.PI;  // normalize to -180..+180�
        // position is already in LatLng space, so no mapping through coordinateSystem is required here
        return LatLng.newInstance(lat2 / Math.PI * 180., lon2  / Math.PI * 180.);
    }
    
    /**
     * Returns a pair whose first component is the leg number (one-based) of the competitor returned as the second component.
     * The competitor returned currently has the best ranking in the quick ranks provided by the {@link #quickFlagDataProvider}.
     */
    private com.sap.sse.common.Util.Pair<Integer, CompetitorDTO> getBestVisibleCompetitorWithOneBasedLegNumber(
            Iterable<CompetitorDTO> competitorsToShow) {
        CompetitorDTO leadingCompetitorDTO = null;
        int legOfLeaderCompetitor = -1;
        int bestOneBasedRank = Integer.MAX_VALUE;
        for (QuickRankDTO competitorFromBestToWorstAndOneBasedLegNumber : quickFlagDataProvider.getQuickRanks().values()) {
            if (Util.contains(competitorsToShow, competitorFromBestToWorstAndOneBasedLegNumber.competitor) && 
                    competitorFromBestToWorstAndOneBasedLegNumber.legNumberOneBased != 0 &&
                    competitorFromBestToWorstAndOneBasedLegNumber.oneBasedRank < bestOneBasedRank) {
                leadingCompetitorDTO = competitorFromBestToWorstAndOneBasedLegNumber.competitor;
                legOfLeaderCompetitor = competitorFromBestToWorstAndOneBasedLegNumber.legNumberOneBased;
                bestOneBasedRank = competitorFromBestToWorstAndOneBasedLegNumber.oneBasedRank;
                if (bestOneBasedRank == 1) {
                    break; // as good as it gets
                }
            }
        }
        return leadingCompetitorDTO == null ? null :
            new com.sap.sse.common.Util.Pair<Integer, CompetitorDTO>(legOfLeaderCompetitor, leadingCompetitorDTO);
    }

    private void showAdvantageLineAndUpdateWindLadder(Iterable<CompetitorDTO> competitorsToShow, Date date, long timeForPositionTransitionMillis) {
        if (map != null && lastRaceTimesInfo != null && !quickFlagDataProvider.getQuickRanks().isEmpty()
                && lastCombinedWindTrackInfoDTO != null) {
            final Pair<Integer, CompetitorDTO> visibleLeaderInfo;
            final WindTrackInfoDTO windDataForLegMiddle;
            GPSFixDTOWithSpeedWindTackAndLegType lastBoatFix = null;
            boolean isVisibleLeaderInfoComplete = false;
            boolean isLegTypeKnown = false;
            LegInfoDTO legInfoDTO = null;
            if (settings.getHelpLinesSettings().isVisible(HelpLineTypes.ADVANTAGELINE) || settings.isShowWindLadder()) {
                // find competitor with highest rank
                visibleLeaderInfo = getBestVisibleCompetitorWithOneBasedLegNumber(competitorsToShow);
                if (visibleLeaderInfo != null
                        && visibleLeaderInfo.getA() > 0
                        && visibleLeaderInfo.getA() <= lastRaceTimesInfo.getLegInfos().size()) {
                        // get wind at middle of leg for leading visible competitor
                    windDataForLegMiddle = lastCombinedWindTrackInfoDTO.getCombinedWindOnLegMiddle(visibleLeaderInfo.getA() - 1);
                    if (windDataForLegMiddle != null && !windDataForLegMiddle.windFixes.isEmpty()) {
                        isVisibleLeaderInfoComplete = true;
                        legInfoDTO = lastRaceTimesInfo.getLegInfos().get(visibleLeaderInfo.getA() - 1);
                        if (legInfoDTO.legType != null) {
                            isLegTypeKnown = true;
                        }
                        lastBoatFix = getBoatFix(visibleLeaderInfo.getB(), date);
                    }
                } else {
                    windDataForLegMiddle = null;
                }
            } else {
                visibleLeaderInfo = null;
                windDataForLegMiddle = null;
            }
            boolean drawAdvantageLine = false;
            if (settings.getHelpLinesSettings().isVisible(HelpLineTypes.ADVANTAGELINE)) {
                // the boat fix may be null; may mean that no positions were loaded yet for the leading visible boat;
                // don't show anything
                if (isVisibleLeaderInfoComplete && isLegTypeKnown && lastBoatFix != null && lastBoatFix.speedWithBearing != null) {
                    BoatDTO boat = competitorSelection.getBoat(visibleLeaderInfo.getB());
                    Distance distanceFromBoatPosition = boat.getBoatClass().getHullLength(); // one hull length
                    // implement and use Position.translateRhumb()
                    final Position boatPosition = lastBoatFix.position;
                    final Position posAheadOfFirstBoat = boatPosition.translateRhumb(new DegreeBearingImpl(lastBoatFix.speedWithBearing.bearingInDegrees), distanceFromBoatPosition);
                    final WindDTO windFix = windDataForLegMiddle.windFixes.get(0);
                    final Bearing bearingOfCombinedWind = new DegreeBearingImpl(windFix.trueWindBearingDeg);
                    Bearing rotatedBearing1 = Bearing.NORTH;
                    Bearing rotatedBearing2 = Bearing.NORTH;
                    if (lastBoatFix.legType == null) {
                        GWT.log("no legType to display advantage line; competitor was "+visibleLeaderInfo.getB().getName()+
                                ", fix from "+lastBoatFix.timepoint);
                    } else {
                        switch (lastBoatFix.legType) {
                        case UPWIND:
                        case DOWNWIND:
                            rotatedBearing1 = bearingOfCombinedWind.add(Bearing.EAST);
                            rotatedBearing2 = bearingOfCombinedWind.add(Bearing.WEST);
                            break;
                        case REACHING:
                            final Bearing legBearing = new DegreeBearingImpl(legInfoDTO.legBearingInDegrees);
                            rotatedBearing1 = legBearing.add(Bearing.EAST);
                            rotatedBearing2 = legBearing.add(Bearing.WEST);
                            break;
                        }
                        MVCArray<LatLng> nextPath = MVCArray.newInstance();
                        final Position advantageLinePos1 = posAheadOfFirstBoat.translateRhumb(rotatedBearing1, advantageLineLength.scale(0.5));
                        final Position advantageLinePos2 = posAheadOfFirstBoat.translateRhumb(rotatedBearing2, advantageLineLength.scale(0.5));
                        if (advantageLine == null) {
                            PolylineOptions options = PolylineOptions.newInstance();
                            options.setClickable(true);
                            options.setGeodesic(true);
                            options.setStrokeColor(ADVANTAGE_LINE_COLOR.getAsHtml());
                            options.setStrokeWeight(1);
                            options.setStrokeOpacity(0.5);
                            
                            advantageLine = Polyline.newInstance(options);
                            advantageTimer = new AdvantageLineAnimator(advantageLine);
                            MVCArray<LatLng> pointsAsArray = MVCArray.newInstance();
                            pointsAsArray.insertAt(0, coordinateSystem.toLatLng(advantageLinePos1));
                            pointsAsArray.insertAt(1, coordinateSystem.toLatLng(advantageLinePos2));
                            advantageLine.setPath(pointsAsArray);
                            advantageLine.setMap(map);
                            Hoverline advantageHoverline = new Hoverline(advantageLine, options, this);
                            advantageLineMouseOverHandler = new AdvantageLineMouseOverMapHandler(
                                    bearingOfCombinedWind.getDegrees(), new Date(windFix.measureTimepoint));
                            advantageLine.addMouseOverHandler(advantageLineMouseOverHandler);
                            advantageHoverline.addMouseOutMoveHandler(new MouseOutMapHandler() {
                                @Override
                                public void onEvent(MouseOutMapEvent event) {
                                    map.setTitle("");
                                }
                            });
                        } else {
                            nextPath.push(coordinateSystem.toLatLng(advantageLinePos1));
                            nextPath.push(coordinateSystem.toLatLng(advantageLinePos2));
                            advantageTimer.setNextPositionAndTransitionMillis(nextPath, timeForPositionTransitionMillis);
                            if (advantageLineMouseOverHandler != null) {
                                advantageLineMouseOverHandler.setTrueWindBearing(bearingOfCombinedWind.getDegrees());
                                advantageLineMouseOverHandler.setDate(new Date(windFix.measureTimepoint));
                            }
                        }
                        drawAdvantageLine = true;
                        advantageLineCompetitor = visibleLeaderInfo.getB();
                    }
                }
            }
            if (!drawAdvantageLine) {
                if (advantageLine != null) {
                    advantageLine.setMap(null);
                    advantageLine = null;
                    advantageTimer = null;
                }
            }
            showWindLadder(lastBoatFix, windDataForLegMiddle, visibleLeaderInfo, timeForPositionTransitionMillis);
        }
    }

    private void showWindLadder(GPSFixDTOWithSpeedWindTackAndLegType bestVisibleCompetitorPosition,
            WindTrackInfoDTO windDataForLegMiddle,
            Pair<Integer, CompetitorDTO> bestVisibleCompetitor, long timeForPositionTransitionMillis) {
        if (settings.isShowWindLadder() && map != null && bestVisibleCompetitorPosition != null && lastCombinedWindTrackInfoDTO != null) {
            if (bestVisibleCompetitor != null) {
                if (windDataForLegMiddle != null && windDataForLegMiddle.windFixes != null && !windDataForLegMiddle.windFixes.isEmpty()) {
                    final WindDTO windFix = windDataForLegMiddle.windFixes.get(0);
                    if (windLadder == null) {
                        windLadder = new WindLadder(map, 0 /* TODO z-index */, coordinateSystem);
                    } else {
                        windLadder.swap();
                    }
                    windLadder.update(windFix, bestVisibleCompetitorPosition.position, timeForPositionTransitionMillis);
                    if (!windLadder.isVisible()) {
                        windLadder.setVisible(true);
                    }
                }
            }
        } else if (windLadder != null && windLadder.isVisible()) {
            windLadder.setVisible(false);
        }
    }

    private final StringBuilder windwardStartLineMarkToFirstMarkLineText = new StringBuilder();
    private final StringBuilder leewardStartLineMarkToFirstMarkLineText = new StringBuilder();
    
    private void showStartLineToFirstMarkTriangle(final CoursePositionsDTO courseDTO) {
        final List<Position> startMarkPositions = courseDTO.getStartMarkPositions();
        if (startMarkPositions.size() > 1 && courseDTO.waypointPositions.size() > 1) {
            final Position windwardStartLinePosition = startMarkPositions.get(0);
            final Position leewardStartLinePosition = startMarkPositions.get(1);
            final Position firstMarkPosition = courseDTO.waypointPositions.get(1);
            windwardStartLineMarkToFirstMarkLineText.replace(0, windwardStartLineMarkToFirstMarkLineText.length(),
                    stringMessages.startLineToFirstMarkTriangle(numberFormatOneDecimal
                            .format(windwardStartLinePosition.getDistance(firstMarkPosition)
                                    .getMeters())));
            leewardStartLineMarkToFirstMarkLineText.replace(0, leewardStartLineMarkToFirstMarkLineText.length(),
                    stringMessages.startLineToFirstMarkTriangle(numberFormatOneDecimal
                            .format(leewardStartLinePosition.getDistance(firstMarkPosition)
                                    .getMeters())));
            final LineInfoProvider windwardStartLineMarkToFirstMarkLineInfoProvider = new LineInfoProvider() {
                @Override
                public String getLineInfo() {
                    return windwardStartLineMarkToFirstMarkLineText.toString();
                }
            };
            final LineInfoProvider leewardStartLineMarkToFirstMarkLineInfoProvider = new LineInfoProvider() {
                @Override
                public String getLineInfo() {
                    return leewardStartLineMarkToFirstMarkLineText.toString();
                }
            };
            windwardStartLineMarkToFirstMarkLine = showOrRemoveOrUpdateLine(windwardStartLineMarkToFirstMarkLine, /* showLine */
                    (settings.getHelpLinesSettings().isVisible(HelpLineTypes.STARTLINETOFIRSTMARKTRIANGLE))
                            && startMarkPositions.size() > 1 && courseDTO.waypointPositions.size() > 1,
                    windwardStartLinePosition, firstMarkPosition, windwardStartLineMarkToFirstMarkLineInfoProvider,
                    "grey", STANDARD_LINE_STROKEWEIGHT, STANDARD_LINE_OPACITY);
            leewardStartLineMarkToFirstMarkLine = showOrRemoveOrUpdateLine(leewardStartLineMarkToFirstMarkLine, /* showLine */
                    (settings.getHelpLinesSettings().isVisible(HelpLineTypes.STARTLINETOFIRSTMARKTRIANGLE))
                            && startMarkPositions.size() > 1 && courseDTO.waypointPositions.size() > 1,
                    leewardStartLinePosition, firstMarkPosition, leewardStartLineMarkToFirstMarkLineInfoProvider,
                    "grey", STANDARD_LINE_STROKEWEIGHT, STANDARD_LINE_OPACITY);
        }
    }

    private final StringBuilder startLineAdvantageText = new StringBuilder();
    private final StringBuilder finishLineAdvantageText = new StringBuilder();
    final LineInfoProvider startLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.startLine()+startLineAdvantageText;
        }
    };
    final LineInfoProvider finishLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.finishLine()+finishLineAdvantageText;
        }
    };

    private void showStartAndFinishAndCourseMiddleLines(final CoursePositionsDTO courseDTO) {
        if (map != null && courseDTO != null && courseDTO.course != null && courseDTO.course.waypoints != null &&
                !courseDTO.course.waypoints.isEmpty()) {
            // draw the start line
            final WaypointDTO startWaypoint = courseDTO.course.waypoints.get(0);
            updateCountdownCanvas(startWaypoint);
            final int numberOfStartWaypointMarks = courseDTO.getStartMarkPositions() == null ? 0 : courseDTO.getStartMarkPositions().size();
            final int numberOfFinishWaypointMarks = courseDTO.getFinishMarkPositions() == null ? 0 : courseDTO.getFinishMarkPositions().size();
            final Position startLineLeftPosition = numberOfStartWaypointMarks == 0 ? null : courseDTO.getStartMarkPositions().get(0);
            final Position startLineRightPosition = numberOfStartWaypointMarks < 2 ? null : courseDTO.getStartMarkPositions().get(1);
            if (courseDTO.startLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind != null) {
                startLineAdvantageText.replace(0, startLineAdvantageText.length(), " "+stringMessages.lineAngleToWindAndAdvantage(
                        numberFormatOneDecimal.format(courseDTO.startLineLengthInMeters),
                        numberFormatOneDecimal.format(Math.abs(courseDTO.startLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind)),
                        NauticalSideFormatter.format(courseDTO.startLineAdvantageousSide, stringMessages),
                        numberFormatOneDecimal.format(courseDTO.startLineAdvantageInMeters)));
            } else {
                startLineAdvantageText.delete(0, startLineAdvantageText.length());
            }
            final boolean showStartLineBasedOnCurrentLeg = numberOfStartWaypointMarks == 2 && courseDTO.currentLegNumber <= 1;
            final boolean showFinishLineBasedOnCurrentLeg = numberOfFinishWaypointMarks == 2 && courseDTO.currentLegNumber == courseDTO.totalLegsCount;
            // show the line when STARTLINE is selected and the current leg is around the start leg,
            // or when COURSEGEOMETRY is selected and the finish line isn't equal and wouldn't be shown at the same time based on the current leg.
            // With this, if COURSEGEOMETRY is selected and start and finish line are equal, the start line will not be displayed if
            // based on the race progress the finish line is to be preferred, so only the finish line will be shown.
            final boolean reallyShowStartLine =
                    (settings.getHelpLinesSettings().isVisible(HelpLineTypes.STARTLINE) && showStartLineBasedOnCurrentLeg) ||
                    (settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEGEOMETRY) &&
                             (!showFinishLineBasedOnCurrentLeg || !startLineEqualsFinishLine(courseDTO)));
            // show the line when FINISHLINE is selected and the current leg is the last leg,
            // or when COURSEGEOMETRY is selected and the start line isn't equal or the current leg is the last leg.
            // With this, if COURSEGEOMETRY is selected and start and finish line are equal, the start line will be displayed unless
            // the finish line should take precedence based on race progress.
            final boolean reallyShowFinishLine = showFinishLineBasedOnCurrentLeg &&
                    (!showStartLineBasedOnCurrentLeg || !startLineEqualsFinishLine(courseDTO)) &&
                    (settings.getHelpLinesSettings().isVisible(HelpLineTypes.FINISHLINE) && showFinishLineBasedOnCurrentLeg) ||
                    (settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEGEOMETRY) &&
                            (!startLineEqualsFinishLine(courseDTO) || showFinishLineBasedOnCurrentLeg));
            startLine = showOrRemoveOrUpdateLine(startLine, reallyShowStartLine, startLineLeftPosition,
                    startLineRightPosition, startLineInfoProvider, START_LINE_COLOR.getAsHtml(), STANDARD_LINE_STROKEWEIGHT,
                    STANDARD_LINE_OPACITY);
            // draw the finish line
            final Position finishLineLeftPosition = numberOfFinishWaypointMarks == 0 ? null : courseDTO.getFinishMarkPositions().get(0);
            final Position finishLineRightPosition = numberOfFinishWaypointMarks < 2 ? null : courseDTO.getFinishMarkPositions().get(1);
            if (courseDTO.finishLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind != null) {
                finishLineAdvantageText.replace(0, finishLineAdvantageText.length(), " "+stringMessages.lineAngleToWindAndAdvantage(
                        numberFormatOneDecimal.format(courseDTO.finishLineLengthInMeters),
                        numberFormatOneDecimal.format(Math.abs(courseDTO.finishLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind)),
                        NauticalSideFormatter.format(courseDTO.finishLineAdvantageousSide, stringMessages),
                        numberFormatOneDecimal.format(courseDTO.finishLineAdvantageInMeters)));
            } else {
                finishLineAdvantageText.delete(0, finishLineAdvantageText.length());
            }
            finishLine = showOrRemoveOrUpdateLine(finishLine, reallyShowFinishLine, finishLineLeftPosition,
                    finishLineRightPosition, finishLineInfoProvider, FINISH_LINE_COLOR.getAsHtml(), STANDARD_LINE_STROKEWEIGHT,
                    STANDARD_LINE_STROKEWEIGHT);
            // the control point pairs for which we already decided whether or not
            // to show a course middle line for; values tell whether to show the line and for which zero-based
            // start waypoint index to do so; when for an equal control point pair multiple decisions with different
            // outcome are made, a decision to show the line overrules the decision to not show it (OR-semantics)
            final Map<Set<ControlPointDTO>, Pair<Boolean, Integer>> keysAlreadyHandled = new HashMap<>();
            for (int zeroBasedIndexOfStartWaypoint = 0; zeroBasedIndexOfStartWaypoint<courseDTO.waypointPositions.size()-1; zeroBasedIndexOfStartWaypoint++) {
                final Set<ControlPointDTO> key = getCourseMiddleLinesKey(courseDTO, zeroBasedIndexOfStartWaypoint);
                boolean showCourseMiddleLine = keysAlreadyHandled.containsKey(key) && keysAlreadyHandled.get(key).getA() ||
                        settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEGEOMETRY) ||
                        (settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEMIDDLELINE)
                        // show the line for the current leg or for the first leg if we are still before the start
                         && ((courseDTO.currentLegNumber-1 == zeroBasedIndexOfStartWaypoint) ||
                              courseDTO.currentLegNumber == 0 && zeroBasedIndexOfStartWaypoint == 0));
                keysAlreadyHandled.put(key, new Pair<>(showCourseMiddleLine, zeroBasedIndexOfStartWaypoint));
            }
            Set<Set<ControlPointDTO>> keysToConsider = new HashSet<>(keysAlreadyHandled.keySet());
            keysToConsider.addAll(courseMiddleLines.keySet());
            for (final Set<ControlPointDTO> key : keysToConsider) {
                final int zeroBasedIndexOfStartWaypoint = keysAlreadyHandled.containsKey(key) ?
                        keysAlreadyHandled.get(key).getB() : 0; // if not handled, the line will be removed, so the waypoint index doesn't matter
                final Pair<Boolean, Integer> showLineAndZeroBasedIndexOfStartWaypoint = keysAlreadyHandled.get(key);
                final boolean showCourseMiddleLine = showLineAndZeroBasedIndexOfStartWaypoint != null && showLineAndZeroBasedIndexOfStartWaypoint.getA();
                courseMiddleLines.put(key, showOrRemoveCourseMiddleLine(courseDTO, courseMiddleLines.get(key), zeroBasedIndexOfStartWaypoint, showCourseMiddleLine));
            }
        }
    }

    private boolean startLineEqualsFinishLine(CoursePositionsDTO courseDTO) {
        final List<WaypointDTO> waypoints;
        return courseDTO != null && courseDTO.course != null &&
                (waypoints = courseDTO.course.waypoints) != null &&
                waypoints.get(0).controlPoint.equals(waypoints.get(waypoints.size()-1).controlPoint);
    }

    /**
     * Given a zero-based index into <code>courseDTO</code>'s {@link RaceCourseDTO#waypoints waypoints list} that denotes the start
     * waypoint of the leg in question, returns a key that can be used for the {@link #courseMiddleLines} map, consisting of a set
     * that holds the two {@link ControlPointDTO}s representing the start and finish control point of that leg.
     */
    private Set<ControlPointDTO> getCourseMiddleLinesKey(final CoursePositionsDTO courseDTO,
            final int zeroBasedIndexOfStartWaypoint) {
        ControlPointDTO startControlPoint = courseDTO.course.waypoints.get(zeroBasedIndexOfStartWaypoint).controlPoint;
        ControlPointDTO endControlPoint = courseDTO.course.waypoints.get(zeroBasedIndexOfStartWaypoint+1).controlPoint;
        final Set<ControlPointDTO> key = new HashSet<>();
        key.add(startControlPoint);
        key.add(endControlPoint);
        return key;
    }

    /**
     * @param showLine
     *            tells whether or not to show the line; if the <code>lineToShowOrRemoveOrUpdate</code> references a
     *            line but the line shall not be shown, the line is removed from the map; conversely, if the line is not
     *            yet shown but shall be, a new line is created, added to the map and returned. If the line is shown and
     *            shall continue to be shown, the line is returned after updating its vertex coordinates.
     * @return <code>null</code> if the line is not shown; the polyline object representing the line being displayed
     *         otherwise
     */
    private Polyline showOrRemoveCourseMiddleLine(final CoursePositionsDTO courseDTO, Polyline lineToShowOrRemoveOrUpdate,
            final int zeroBasedIndexOfStartWaypoint, final boolean showLine) {
        final Position position1DTO = courseDTO.waypointPositions.get(zeroBasedIndexOfStartWaypoint);
        final Position position2DTO = courseDTO.waypointPositions.get(zeroBasedIndexOfStartWaypoint+1);
        final LineInfoProvider lineInfoProvider = new LineInfoProvider() {
            @Override
            public String getLineInfo() {
                final StringBuilder sb = new StringBuilder();
                sb.append(stringMessages.courseMiddleLine());
                sb.append('\n');
                sb.append(numberFormatNoDecimal.format(
                        Math.abs(position1DTO.getDistance(position2DTO).getMeters()))+stringMessages.metersUnit());
                sb.append(" (");
                sb.append(numberFormatTwoDecimals.format(
                        Math.abs(position1DTO.getDistance(position2DTO).getNauticalMiles()))+"NM");
                sb.append(")\n");
                final double legBearingDeg = position1DTO.getBearingGreatCircle(position2DTO).getDegrees();
                sb.append(NumberFormatterFactory.getThreeDigitDecimalFormat(0).format(legBearingDeg)+stringMessages.degreesUnit());
                if (lastCombinedWindTrackInfoDTO != null) {
                    final WindTrackInfoDTO windTrackAtLegMiddle = lastCombinedWindTrackInfoDTO.getCombinedWindOnLegMiddle(zeroBasedIndexOfStartWaypoint);
                    if (windTrackAtLegMiddle != null && windTrackAtLegMiddle.windFixes != null && !windTrackAtLegMiddle.windFixes.isEmpty()) {
                        WindDTO windAtLegMiddle = windTrackAtLegMiddle.windFixes.get(0);
                        final String diff = numberFormatOneDecimal.format(
                                Math.min(Math.abs(windAtLegMiddle.dampenedTrueWindBearingDeg-legBearingDeg),
                                                     Math.abs(windAtLegMiddle.dampenedTrueWindFromDeg-legBearingDeg)));
                        sb.append(", ");
                        sb.append(stringMessages.degreesToWind(diff));
                    }
                }
                return sb.toString();
            }
        };
        return showOrRemoveOrUpdateLine(lineToShowOrRemoveOrUpdate, showLine, position1DTO, position2DTO,
                lineInfoProvider, COURSE_MIDDLE_LINE_COLOR.getAsHtml(), STANDARD_LINE_STROKEWEIGHT, STANDARD_LINE_OPACITY);
    }

    /**
     * @param showLine
     *            tells whether or not to show the line; if the <code>lineToShowOrRemoveOrUpdate</code> references a
     *            line but the line shall not be shown, the line is removed from the map; conversely, if the line is not
     *            yet shown but shall be, a new line is created, added to the map and returned. If the line is shown and
     *            shall continue to be shown, the line is returned after updating its vertex coordinates.
     * @return <code>null</code> if the line is not shown; the polyline object representing the line being displayed
     *         otherwise
     */
    Polyline showOrRemoveOrUpdateLine(Polyline lineToShowOrRemoveOrUpdate, final boolean showLine,
            final Position position1DTO, final Position position2DTO, final LineInfoProvider lineInfoProvider,
            String lineColorRGB, int strokeWeight, double strokeOpacity) {
        if (position1DTO != null && position2DTO != null) {
            if (showLine) {
                LatLng courseMiddleLinePoint1 = coordinateSystem.toLatLng(position1DTO);
                LatLng courseMiddleLinePoint2 = coordinateSystem.toLatLng(position2DTO);
                final MVCArray<LatLng> pointsAsArray;
                if (lineToShowOrRemoveOrUpdate == null) {
                    PolylineOptions options = PolylineOptions.newInstance();
                    options.setClickable(true);
                    options.setGeodesic(true);
                    options.setStrokeColor(lineColorRGB);
                    options.setStrokeWeight(strokeWeight);
                    options.setStrokeOpacity(strokeOpacity);
                    pointsAsArray = MVCArray.newInstance();
                    lineToShowOrRemoveOrUpdate = Polyline.newInstance(options);
                    lineToShowOrRemoveOrUpdate.setPath(pointsAsArray);
                    lineToShowOrRemoveOrUpdate.setMap(map);
                    final Hoverline lineToShowOrRemoveOrUpdateHoverline = new Hoverline(lineToShowOrRemoveOrUpdate, options, this);
                    lineToShowOrRemoveOrUpdate.addMouseOverHandler(new MouseOverMapHandler() {
                        @Override
                        public void onEvent(MouseOverMapEvent event) {
                            map.setTitle(lineInfoProvider.getLineInfo());
                        }
                    });
                    lineToShowOrRemoveOrUpdateHoverline.addMouseOutMoveHandler(new MouseOutMapHandler() {
                        @Override
                        public void onEvent(MouseOutMapEvent event) {
                            map.setTitle("");
                        }
                    });
                } else {
                    pointsAsArray = lineToShowOrRemoveOrUpdate.getPath();
                    pointsAsArray.removeAt(1);
                    pointsAsArray.removeAt(0);
                }
                adjustInfoOverlayForVisibleLine(lineToShowOrRemoveOrUpdate, position1DTO, position2DTO, lineInfoProvider);
                pointsAsArray.insertAt(0, courseMiddleLinePoint1);
                pointsAsArray.insertAt(1, courseMiddleLinePoint2);
            } else {
                if (lineToShowOrRemoveOrUpdate != null) {
                    lineToShowOrRemoveOrUpdate.setMap(null);
                    adjustInfoOverlayForRemovedLine(lineToShowOrRemoveOrUpdate);
                    lineToShowOrRemoveOrUpdate = null;
                }
            }
        }
        return lineToShowOrRemoveOrUpdate;
    }

    private void adjustInfoOverlayForRemovedLine(Polyline lineToShowOrRemoveOrUpdate) {
        SmallTransparentInfoOverlay infoOverlay = infoOverlaysForLinesForCourseGeometry.remove(lineToShowOrRemoveOrUpdate);
        if (infoOverlay != null) {
            infoOverlay.removeFromMap();
        }
    }

    private void adjustInfoOverlayForVisibleLine(Polyline lineToShowOrRemoveOrUpdate, final Position position1DTO,
            final Position position2DTO, final LineInfoProvider lineInfoProvider) {
        if (lineInfoProvider.isShowInfoOverlayWithHelplines()) {
            SmallTransparentInfoOverlay infoOverlay = infoOverlaysForLinesForCourseGeometry.get(lineToShowOrRemoveOrUpdate);
            if (getSettings().getHelpLinesSettings().isVisible(HelpLineTypes.COURSEGEOMETRY)) {
                if (infoOverlay == null) {
                    infoOverlay = new SmallTransparentInfoOverlay(map, RaceMapOverlaysZIndexes.INFO_OVERLAY_ZINDEX,
                            lineInfoProvider.getLineInfo(), coordinateSystem);
                    infoOverlaysForLinesForCourseGeometry.put(lineToShowOrRemoveOrUpdate, infoOverlay);
                    infoOverlay.addToMap();
                } else {
                    infoOverlay.setInfoText(lineInfoProvider.getLineInfo());
                }
                infoOverlay.setPosition(position1DTO.translateGreatCircle(position1DTO.getBearingGreatCircle(position2DTO),
                                position1DTO.getDistance(position2DTO).scale(0.5)), /* transition time */ -1);
                infoOverlay.draw();
            } else {
                if (infoOverlay != null) {
                    infoOverlay.removeFromMap();
                    infoOverlaysForLinesForCourseGeometry.remove(lineToShowOrRemoveOrUpdate);
                }
            }
        }
    }
    
    /**
     * If, according to {@link #lastRaceTimesInfo} and {@link #timer} the race is
     * still in the pre-start phase, show a {@link SmallTransparentInfoOverlay} at the
     * start line that shows the count down.
     */
    private void updateCountdownCanvas(WaypointDTO startWaypoint) {
        if (!settings.isShowSelectedCompetitorsInfo() || startWaypoint == null || Util.isEmpty(startWaypoint.controlPoint.getMarks())
                || lastRaceTimesInfo == null || lastRaceTimesInfo.startOfRace == null || timer.getTime().after(lastRaceTimesInfo.startOfRace)) {
            if (countDownOverlay != null) {
                countDownOverlay.removeFromMap();
                countDownOverlay = null;
            }
        } else {
            long timeToStartInMs = lastRaceTimesInfo.startOfRace.getTime() - timer.getTime().getTime();
            String countDownText = timeToStartInMs > 1000 ? stringMessages.timeToStart(DateAndTimeFormatterUtil
                    .formatElapsedTime(timeToStartInMs)) : stringMessages.start();
            if (countDownOverlay == null) {
                countDownOverlay = new SmallTransparentInfoOverlay(map, RaceMapOverlaysZIndexes.INFO_OVERLAY_ZINDEX,
                        countDownText, coordinateSystem, getSettings().getStartCountDownFontSizeScaling());
                countDownOverlay.addToMap();
            } else {
                countDownOverlay.setInfoText(countDownText);
            }
            countDownOverlay.setPosition(startWaypoint.controlPoint.getMarks().iterator().next().position, /* transition time */ -1);
            countDownOverlay.draw();
        }
    }

    private static final int GLOBE_PXSIZE = 256; // a constant in Google's map projection
    private static final int MAX_ZOOM = 18; // maximum zoom-level that should be automatically selected
    private static final double LOG2 = Math.log(2.0);
    private static final double ZOOM_BORDER_LEEWAY = 0.4; // a bit of border room to leave to not squeeez content into viewport
    public double getZoomLevel(NonCardinalBounds viewportBounds) {
        final double result;
        final LatLngBounds bounds = BoundsUtil.getAsBounds(coordinateSystem.toLatLng(viewportBounds.getLowerLeft()))
                .extend(coordinateSystem.toLatLng(viewportBounds.getUpperLeft()))
                .extend(coordinateSystem.toLatLng(viewportBounds.getLowerRight()))
                .extend(coordinateSystem.toLatLng(viewportBounds.getUpperRight()));
        double deltaLng = bounds.getNorthEast().getLongitude() - bounds.getSouthWest().getLongitude();
        double deltaLat = bounds.getNorthEast().getLatitude() - bounds.getSouthWest().getLatitude();
        if ((deltaLng == 0) && (deltaLat == 0)) {
            result = MAX_ZOOM;
        } else {
            if (deltaLng < 0) {
                deltaLng += 360;
            }
            double zoomLng = Math.log(map.getDiv().getClientWidth() * 360 / deltaLng / GLOBE_PXSIZE) / LOG2 - ZOOM_BORDER_LEEWAY;
            double zoomLat = Math.log((map.getDiv().getClientHeight()-60) /* subtract the transparent margin at the top of the map*/
                    * 2*85 /* Google's Mercator projection covers the world only from 85S to 85N */ / deltaLat / GLOBE_PXSIZE) / LOG2 - ZOOM_BORDER_LEEWAY;
            result = Math.min(Math.min(zoomLat, zoomLng), MAX_ZOOM);
        }
        return result;
    }

    private void zoomMapToNewBounds(NonCardinalBounds newBounds) {
        if (newBounds != null) {
            final double newZoomLevel = getZoomLevel(newBounds);
            if (mapNeedsToPanOrZoom(newBounds, newZoomLevel)) {
                Iterable<ZoomTypes> oldZoomTypesToConsiderSettings = settings.getZoomSettings().getTypesToConsiderOnZoom();
                setAutoZoomInProgress(true);
                if (newZoomLevel != map.getZoom()) {
                    // distinguish between zoom-in and zoom-out, because the sequence of panTo() and setZoom()
                    // appears different on the screen due to map-animations
                    // following sequences keep the selected boats always visible:
                    //   zoom-in : 1. panTo(), 2. setZoom()
                    //   zoom-out: 1. setZoom(), 2. panTo() 
                    autoZoomIn = newZoomLevel > map.getZoom();
                    autoZoomOut = !autoZoomIn;
                    autoZoomLevel = newZoomLevel;
                    if (autoZoomIn) {
                        map.panTo(coordinateSystem.toLatLng(newBounds.getCenter()));
                    } else {
                        map.setZoom(autoZoomLevel);
                    }
                } else {
                    map.panTo(coordinateSystem.toLatLng(newBounds.getCenter()));
                }
                autoZoomBounds = newBounds;
                RaceMapZoomSettings restoredZoomSettings = new RaceMapZoomSettings(oldZoomTypesToConsiderSettings,
                        settings.getZoomSettings().isZoomToSelectedCompetitors());
                settings = new RaceMapSettings.RaceMapSettingsBuilder(settings, raceMapLifecycle.getRaceDTO(), paywallResolver)
                        .withZoomSettings(restoredZoomSettings)
                        .build();
                setAutoZoomInProgress(false);
                mapFirstZoomDone = true;
            }
        }
    }

    private boolean mapNeedsToPanOrZoom(NonCardinalBounds newBounds, double newZoomLevel) {
        // we never updated, update now
        if (currentMapBounds == null) {
            return true;
        }
        // we do not fit the required bounds, update now
        if (!currentMapBounds.contains(newBounds)) {
            return true;
        }
        // we do fit the required bounds, however we might be too far zoomed out, check if we can zoom to a better level
        if (Math.abs(newZoomLevel - map.getZoom()) > 0.5) {
            return true;
        }
        return false;
    }
    
    private void setAutoZoomInProgress(boolean autoZoomInProgress) {
        this.autoZoomInProgress = autoZoomInProgress;
    }
    
    boolean isAutoZoomInProgress() {
        return autoZoomInProgress;
    }
    
    /**
     * @param timeForPositionTransitionMillis use -1 to not animate the position transition, e.g., during map zoom or non-play
     */
    private boolean updateBoatCanvasForCompetitor(CompetitorDTO competitorDTO, Date date, long timeForPositionTransitionMillis) {
        boolean hasTimeJumped = timeForPositionTransitionMillis > 3 * timer.getRefreshInterval();
        if (hasTimeJumped) {
            timeForPositionTransitionMillis = -1;
        }
        boolean usedExistingCanvas = false;
        final GPSFixDTOWithSpeedWindTackAndLegType lastBoatFix = getBoatFix(competitorDTO, date);
        if (lastBoatFix != null) {
            BoatOverlay boatOverlay = boatOverlaysByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString());
            if (boatOverlay == null) {
                boatOverlay = createBoatOverlay(RaceMapOverlaysZIndexes.BOATS_ZINDEX, competitorDTO, displayHighlighted(competitorDTO));
                if (boatOverlay != null) {
                    boatOverlaysByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), boatOverlay);
                    boatOverlay.setDisplayMode(displayHighlighted(competitorDTO));
                    boatOverlay.setBoatFix(lastBoatFix, timeForPositionTransitionMillis);
                    boatOverlay.addToMap();
                }
            } else {
                usedExistingCanvas = true;
                boatOverlay.setDisplayMode(displayHighlighted(competitorDTO));
                boatOverlay.setBoatFix(lastBoatFix, timeForPositionTransitionMillis);
                boatOverlay.draw();
            }
        }
        return usedExistingCanvas;
    }

    private DisplayMode displayHighlighted(CompetitorDTO competitorDTO) {
        boolean competitorisSelected = competitorSelection.isSelected(competitorDTO);
        if (!settings.isShowOnlySelectedCompetitors()) {
            if (competitorisSelected) {
                return DisplayMode.SELECTED;
            } else {
                if (isSomeOtherCompetitorSelected()) {
                    return DisplayMode.NOT_SELECTED;
                } else {
                    return DisplayMode.DEFAULT;
                }
            }
        }
        else{
            return competitorSelection.isSelected(competitorDTO) ? DisplayMode.SELECTED : DisplayMode.DEFAULT;
        }
       
    }
    
    private boolean isSomeOtherCompetitorSelected(){
        return Util.size(competitorSelection.getSelectedCompetitors()) > 0;
    }
    
    private class CourseMarkInfoWindowClickHandler implements ClickMapHandler {
        private final MarkDTO markDTO;
        private final CourseMarkOverlay courseMarkOverlay;
        
        public CourseMarkInfoWindowClickHandler(MarkDTO markDTO, CourseMarkOverlay courseMarkOverlay) {
            this.markDTO = markDTO;
            this.courseMarkOverlay = courseMarkOverlay;
        }
        
        @Override
        public void onEvent(ClickMapEvent event) {
            LatLng latlng = courseMarkOverlay.getMarkLatLngPosition();
            showMarkInfoWindow(markDTO, latlng);
        }
    }
    
    private void registerCourseMarkInfoWindowClickHandler(final String markDTOIdAsString) {
        final CourseMarkOverlay courseMarkOverlay = courseMarkOverlays.get(markDTOIdAsString);
        courseMarkClickHandlers.put(markDTOIdAsString, 
                courseMarkOverlay.addClickHandler(new CourseMarkInfoWindowClickHandler(markDTOs.get(markDTOIdAsString), courseMarkOverlay)));
    }
    
    public void registerAllCourseMarkInfoWindowClickHandlers() {
        for (String markDTOIdAsString : markDTOs.keySet()) {
            registerCourseMarkInfoWindowClickHandler(markDTOIdAsString);
        }
    }
    
    public void unregisterAllCourseMarkInfoWindowClickHandlers() {
        Iterator<Entry<String, HandlerRegistration>> iterator = courseMarkClickHandlers.entrySet().iterator();
        while(iterator.hasNext()) {
            Entry<String, HandlerRegistration> handler = iterator.next();
            handler.getValue().removeHandler();
            iterator.remove();
        }
    }

    /**
     * @return a valid {@link BoatOverlay} if a boat was found for the competitor, or {@code null} otherwise
     */
    private BoatOverlay createBoatOverlay(int zIndex, final CompetitorDTO competitorDTO, DisplayMode displayMode) {
        final BoatDTO boatOfCompetitor = competitorSelection.getBoat(competitorDTO);
        final BoatOverlay boatCanvas;
        if (boatOfCompetitor == null) {
            GWT.log("Error: no boat found for competitor "+competitorDTO.getName()+". Not showing on map.");
            boatCanvas = null;
        } else {
            boatCanvas = new BoatOverlay(map, zIndex, boatOfCompetitor, competitorSelection.getColor(competitorDTO, raceIdentifier), coordinateSystem);
            boatCanvas.setDisplayMode(displayMode);
            boatCanvas.addClickHandler(event -> {
                GPSFixDTOWithSpeedWindTackAndLegType latestFixForCompetitor = getBoatFix(competitorDTO, timer.getTime());
                Widget content = getInfoWindowContent(competitorDTO, latestFixForCompetitor);
                LatLng where = coordinateSystem.toLatLng(latestFixForCompetitor.position);
                managedInfoWindow.openAtPosition(content, where);
            });
            boatCanvas.addMouseOverHandler(new MouseOverMapHandler() {
                @Override
                public void onEvent(MouseOverMapEvent event) {
                    map.setTitle(boatOfCompetitor.getSailId());
                }
            });
            boatCanvas.addMouseOutMoveHandler(new MouseOutMapHandler() {
                @Override
                public void onEvent(MouseOutMapEvent event) {
                    map.setTitle("");
                }
            });
        }
        return boatCanvas;
    }

    protected WindSensorOverlay createWindSensorOverlay(int zIndex, final WindSource windSource, final WindTrackInfoDTO windTrackInfoDTO) {
        final WindSensorOverlay windSensorOverlay = new WindSensorOverlay(map, zIndex, raceMapImageManager, stringMessages, coordinateSystem);
        windSensorOverlay.setWindInfo(windTrackInfoDTO, windSource);
        windSensorOverlay.addClickHandler(new ClickMapHandler() {
            @Override
            public void onEvent(ClickMapEvent event) {
                showWindSensorInfoWindow(windSensorOverlay);
            }
        });
        return windSensorOverlay;
    }

    private void showMarkInfoWindow(MarkDTO markDTO, LatLng position) {
        managedInfoWindow.openAtPosition(getInfoWindowContent(markDTO), position);
    }

    private void showCompetitorInfoWindow(final CompetitorDTO competitorDTO, LatLng where) {
        final GPSFixDTOWithSpeedWindTackAndLegType latestFixForCompetitor = getBoatFix(competitorDTO, timer.getTime());
        final Widget content = getInfoWindowContent(competitorDTO, latestFixForCompetitor);
        managedInfoWindow.openAtPosition(content, where);
    }

    private void showWindSensorInfoWindow(final WindSensorOverlay windSensorOverlay) {
        WindSource windSource = windSensorOverlay.getWindSource();
        WindTrackInfoDTO windTrackInfoDTO = windSensorOverlay.getWindTrackInfoDTO();
        WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
        if (windDTO != null && windDTO.position != null) {
            final LatLng where = coordinateSystem.toLatLng(windDTO.position);
            final Widget content = getInfoWindowContent(windSource, windTrackInfoDTO);
            managedInfoWindow.openAtPosition(content, where);
        }
    }

    Widget createInfoWindowLabelAndValue(String labelName, String value) {
        Label valueLabel = new Label(value);
        valueLabel.setWordWrap(false);
        return createInfoWindowLabelWithWidget(labelName, valueLabel);
    }

    public Widget createInfoWindowLabelWithWidget(String labelName, Widget value) {
        FlowPanel flowPanel = new FlowPanel();
        Label label = new Label(labelName + ":");
        label.setWordWrap(false);
        label.getElement().getStyle().setFloat(Style.Float.LEFT);
        label.getElement().getStyle().setPadding(3, Style.Unit.PX);
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        value.getElement().getStyle().setFloat(Style.Float.LEFT);
        value.getElement().getStyle().setPadding(3, Style.Unit.PX);
        flowPanel.add(label);
        flowPanel.add(value);
        return flowPanel;
    }
    
    private Widget getInfoWindowContent(MarkDTO markDTO) {
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.mark(), markDTO.getName()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.position(), markDTO.position.getAsDegreesAndDecimalMinutesWithCardinalPoints()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.position(), markDTO.position.getAsSignedDecimalDegrees()));
        return vPanel;
    }

    private Widget getInfoWindowContent(final WindSource windSource, WindTrackInfoDTO windTrackInfoDTO) {
        WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
        final VerticalPanel vPanel = new VerticalPanel();
        final Anchor windSourceNameAnchor = new Anchor(WindSourceTypeFormatter.format(windSource, stringMessages));
        vPanel.add(createInfoWindowLabelWithWidget(stringMessages.windSource(), windSourceNameAnchor));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.wind(), Math.round(windDTO.dampenedTrueWindFromDeg) + " " + stringMessages.degreesShort()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.windSpeed(), numberFormatOneDecimal.format(windDTO.dampenedTrueWindSpeedInKnots)));
        final MillisecondsTimePoint timePoint = new MillisecondsTimePoint(windDTO.measureTimepoint);
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.time(), timePoint.asDate().toString()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.position(), windDTO.position.getAsDegreesAndDecimalMinutesWithCardinalPoints()));
        final Label positionInDecimalDegreesLabel = new Label(windDTO.position.getAsSignedDecimalDegrees());
        positionInDecimalDegreesLabel.setWordWrap(false);
        positionInDecimalDegreesLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        positionInDecimalDegreesLabel.getElement().getStyle().setPadding(3, Style.Unit.PX);
        positionInDecimalDegreesLabel.getElement().getStyle().setFontWeight(Style.FontWeight.LIGHTER);
        positionInDecimalDegreesLabel.getElement().getStyle().setFontSize(0.7, Unit.EM);
        vPanel.add(positionInDecimalDegreesLabel);
        if (windSource.getType() == WindSourceType.WINDFINDER) {
            final HorizontalPanel container = new HorizontalPanel();
            container.setSpacing(1);
            final WindfinderIcon windfinderIcon = new WindfinderIcon(raceMapImageManager, stringMessages);
            container.add(windfinderIcon);
            container.add(vPanel);
            sailingService.getWindFinderSpot(windSource.getId().toString(), new AsyncCallback<SpotDTO>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError(stringMessages.unableToResolveWindFinderSpotId(
                            windSource.getId().toString(), caught.getMessage()), /* silentMode */ true);
                }

                @Override
                public void onSuccess(SpotDTO result) {
                    final String url = result.getCurrentlyMostAppropriateUrl(timePoint);
                    windSourceNameAnchor.setTarget("_blank");
                    windSourceNameAnchor.setText(result.getName());
                    windSourceNameAnchor.setHref(url);
                    windfinderIcon.setHref(url);
                }
            });
            return container;
        } else {
            windSourceNameAnchor.addClickHandler(event -> showWindChartForProvider.accept(windSource));
        }
        return vPanel;
    }

    private Widget getInfoWindowContent(CompetitorDTO competitorDTO, GPSFixDTOWithSpeedWindTackAndLegType lastFix) {
        final VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.competitor(), competitorDTO.getName()));
        final BoatDTO boat = competitorSelection.getBoat(competitorDTO);
        if (Util.hasLength(boat.getName())) {
            vPanel.add(createInfoWindowLabelAndValue(stringMessages.boat(), boat.getName()));
        }
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.sailNumber(), boat.getSailId()));
        final Integer rank = getRank(competitorDTO);
        if (rank != null) {
            vPanel.add(createInfoWindowLabelAndValue(stringMessages.rank(), String.valueOf(rank)));
        }
        SpeedWithBearingDTO speedWithBearing = lastFix.speedWithBearing;
        if (speedWithBearing == null) {
            speedWithBearing = new SpeedWithBearingDTO(0, 0);
        }
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.speed(),
                numberFormatOneDecimal.format(speedWithBearing.speedInKnots) + " "+stringMessages.knotsUnit()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.bearing(), (int) speedWithBearing.bearingInDegrees + " "+stringMessages.degreesShort()));
        if (lastFix.degreesBoatToTheWind != null) {
            vPanel.add(createInfoWindowLabelAndValue(stringMessages.degreesBoatToTheWind(),
                    (int) Math.abs(lastFix.degreesBoatToTheWind) + " " + stringMessages.degreesShort()));
        }
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.position(), lastFix==null||lastFix.position==null ? "" : lastFix.position.getAsDegreesAndDecimalMinutesWithCardinalPoints()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.position(), lastFix==null||lastFix.position==null ? "" : lastFix.position.getAsSignedDecimalDegrees()));
        vPanel.add(createInfoWindowLabelWithWidget(stringMessages.selectedDetailType(), createDetailTypeDropdown(competitorDTO)));
        if (raceIdentifier != null) {
            final RegattaAndRaceIdentifier race = raceIdentifier;
            if (race != null) {
                final Map<CompetitorDTO, TimeRange> timeRange = new HashMap<>();
                final Integer firstShownFix = fixesAndTails.getFirstShownFix(competitorDTO);
                if (firstShownFix != null) {
                    final TimePoint from = new MillisecondsTimePoint(fixesAndTails.getFixes(competitorDTO).get(firstShownFix).timepoint);
                    final TimePoint to = new MillisecondsTimePoint(getBoatFix(competitorDTO, timer.getTime()).timepoint);
                    timeRange.put(competitorDTO, new TimeRangeImpl(from, to, true));
                    if (settings.isShowDouglasPeuckerPoints()) {
                        sailingService.getDouglasPoints(race, timeRange, 3,
                                new AsyncCallback<Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>>>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError("Error obtaining douglas positions: " + caught.getMessage(), true /*silentMode */);
                                    }
        
                                    @Override
                                    public void onSuccess(Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> result) {
                                        lastDouglasPeuckerResult = result;
                                        if (douglasMarkers != null) {
                                            removeAllMarkDouglasPeuckerpoints();
                                        }
                                        if (!(timer.getPlayState() == PlayStates.Playing)) {
                                            if (settings.isShowDouglasPeuckerPoints()) {
                                                showMarkDouglasPeuckerPoints(result);
                                            }
                                        }
                                    }
                                });
                    }
                    maneuverMarkersAndLossIndicators.getAndShowManeuvers(race, timeRange);
                }
            }
        }
        // If a metric is shown a click on any competitor / competitors tail will
        // add them to the selection effectively showing the metric on their tail.
        if (selectedDetailType != null) {
            competitorSelection.setSelected(competitorDTO, true);
        }
        return vPanel;
    }

    private VerticalPanel createDetailTypeDropdown(CompetitorDTO competitor) {
        final VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(createDetailTypePremiumList(competitor));
        // reset component if user changes (login/out)
        paywallResolver.registerUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                vPanel.clear();
                vPanel.add(createDetailTypePremiumList(competitor));
            }
        });
        return vPanel;
    }

    private SailingPremiumListBox createDetailTypePremiumList(CompetitorDTO competitor) {
        // create new premium list box
        final String EMPTY_VALUE = "none";
        SailingPremiumListBox lb = new SailingPremiumListBox(stringMessages.none(), EMPTY_VALUE, 
                TrackedRaceActions.COLORED_TAILS, paywallResolver, raceMapLifecycle.getRaceDTO());
        fillItemsFromAvailableDetailTypes(lb);
        lb.setVisibleItemCount(1);
        lb.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                final String value = lb.getSelectedValue();
                final DetailType previous = selectedDetailType;
                if (value == null || value.equals(EMPTY_VALUE)) {
                    selectedDetailType = null;
                    metricOverlay.setVisible(false);
                    selectedDetailTypeChanged = previous != null;
                } else {
                    selectedDetailType = DetailType.valueOfString(value);
                    numberFormatterForSelectedDetailType = NumberFormatterFactory.getDecimalFormat(selectedDetailType.getPrecision());
                    selectedDetailTypeChanged = selectedDetailType != previous;
                    metricOverlay.setVisible(true);
                    if (!competitorSelection.isSelected(competitor)) {
                        competitorSelection.setSelected(competitor, true);
                    }
                }
                if (selectedDetailTypeChanged) {
                    // Causes an overwrite of what are now wrong detailValues
                    if (selectedDetailType != null) {
                        // start with fresh value boundaries
                        setTailVisualizer(selectedDetailType);
                    }
                    // In case the new values don't make it through this will make the tails visible
                    tailColorMapper.notifyListeners();
                    // Forces update of tail values which subsequently results
                    // in another call to tailColorMapper.notifyListeners
                    redraw();
                }
            }
        });
        return lb;
    }

    private void fillItemsFromAvailableDetailTypes(SailingPremiumListBox lb) {
        final NodeList<OptionElement> options = DOMUtils.getOptions(lb.getListBox());
        if (lb.isEnabled() && sortedAvailableDetailTypes != null) {
            for (int i = 0; i < sortedAvailableDetailTypes.size(); i++) {
                final DetailType detail = sortedAvailableDetailTypes.get(i);
                lb.addItem(DetailTypeFormatter.format(detail), detail.name());
                final String tooltip = DetailTypeFormatter.getTooltip(detail);
                if (Util.hasLength(tooltip)) {
                    options.getItem(options.getLength()-1).setTitle(tooltip);
                }
                if (detail == selectedDetailType) {
                    lb.setSelectedIndex(i + 1);
                }
            }
        } else {
            lb.reset();
        }
    }

    /**
     * @return the {@link CompetitorSelectionProvider#getSelectedCompetitors()} if
     *         {@link RaceMapSettings#isShowOnlySelectedCompetitors() only selected competitors are to be shown}, the
     *         {@link CompetitorSelectionProvider#getFilteredCompetitors() filtered competitors} otherwise. In both cases,
     *         if we have {@link RaceCompetitorSet information about the competitors participating} in this map's race,
     *         the result set is reduced to those, no matter if other regatta participants would otherwise have been in
     *         the result set
     */
    private Iterable<CompetitorDTO> getCompetitorsToShow() {
        final Iterable<CompetitorDTO> result;
        Iterable<CompetitorDTO> selection = competitorSelection.getSelectedCompetitors();
        final Set<String> raceCompetitorIdsAsString = raceCompetitorSet.getIdsOfCompetitorsParticipatingInRaceAsStrings();
        if (!settings.isShowOnlySelectedCompetitors() || Util.isEmpty(selection)) {
            result = Util.filter(competitorSelection.getFilteredCompetitors(), filteredCompetitor -> raceCompetitorIdsAsString == null || raceCompetitorIdsAsString.contains(filteredCompetitor.getIdAsString()));
        } else {
            result = Util.filter(selection, selectedCompetitor -> raceCompetitorIdsAsString == null || raceCompetitorIdsAsString.contains(selectedCompetitor.getIdAsString()));
        }
        return result;
    }
    
    protected void removeAllMarkDouglasPeuckerpoints() {
        if (douglasMarkers != null) {
            for (Marker marker : douglasMarkers) {
                marker.setMap((MapWidget) null);
            }
        }
        douglasMarkers = null;
    }

    private void showMarkDouglasPeuckerPoints(Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> gpsFixPointMapForCompetitors) {
        douglasMarkers = new HashSet<Marker>();
        if (map != null && gpsFixPointMapForCompetitors != null) {
            Set<CompetitorDTO> keySet = gpsFixPointMapForCompetitors.keySet();
            Iterator<CompetitorDTO> iter = keySet.iterator();
            while (iter.hasNext()) {
                CompetitorDTO competitorDTO = iter.next();
                List<GPSFixDTOWithSpeedWindTackAndLegType> gpsFix = gpsFixPointMapForCompetitors.get(competitorDTO);
                for (GPSFixDTOWithSpeedWindTackAndLegType fix : gpsFix) {
                    LatLng latLng = coordinateSystem.toLatLng(fix.position);
                    MarkerOptions options = MarkerOptions.newInstance();
                    options.setTitle(fix.timepoint+": "+fix.position+", "+fix.speedWithBearing.toString());
                    Marker marker = Marker.newInstance(options);
                    marker.setPosition(latLng);
                    douglasMarkers.add(marker);
                    marker.setMap(map);
                }
            }
        }
    }
    
    /**
     * @param date
     *            the point in time for which to determine the competitor's boat position; approximated by using the fix
     *            from {@link #fixes} whose time point comes closest to this date
     * 
     * @return The GPS fix for the given competitor from {@link #fixes} that is closest to <code>date</code>, or
     *         <code>null</code> if no fix is available
     */
    private GPSFixDTOWithSpeedWindTackAndLegType getBoatFix(CompetitorDTO competitorDTO, Date date) {
        final GPSFixDTOWithSpeedWindTackAndLegType result;
        final List<GPSFixDTOWithSpeedWindTackAndLegType> competitorFixes = fixesAndTails.getFixes(competitorDTO);
        if (competitorFixes != null && !competitorFixes.isEmpty()) {
            int i = Collections.binarySearch(competitorFixes, new GPSFixDTOWithSpeedWindTackAndLegType(date, null, null, /* optionalTrueHeading */ null, (WindDTO) null, null, null, false),
                    new Comparator<GPSFixDTOWithSpeedWindTackAndLegType>() {
                @Override
                public int compare(GPSFixDTOWithSpeedWindTackAndLegType o1, GPSFixDTOWithSpeedWindTackAndLegType o2) {
                    return o1.timepoint.compareTo(o2.timepoint);
                }
            });
            if (i<0) {
                i = -i-1; // no perfect match; i is now the insertion point
                // if the insertion point is at the end, use last fix
                if (i >= competitorFixes.size()) {
                    result = competitorFixes.get(competitorFixes.size()-1);
                } else if (i == 0) {
                    // if the insertion point is at the beginning, use first fix
                    result = competitorFixes.get(0);
                } else {
                    // competitorFixes must have at least two elements, and i points neither to the end nor the beginning;
                    // get the fix from i and i+1 whose timepoint is closer to date
                    final GPSFixDTOWithSpeedWindTackAndLegType fixBefore = competitorFixes.get(i-1);
                    final GPSFixDTOWithSpeedWindTackAndLegType fixAfter = competitorFixes.get(i);
                    final GPSFixDTOWithSpeedWindTackAndLegType closer;
                    if (date.getTime() - fixBefore.timepoint.getTime() < fixAfter.timepoint.getTime() - date.getTime()) {
                        closer = fixBefore;
                    } else {
                        closer = fixAfter;
                    }
                    // now compute a weighted average depending on the time difference to "date" (see also bug 1924)
                    double factorForAfter = (double) (date.getTime()-fixBefore.timepoint.getTime()) / (double) (fixAfter.timepoint.getTime() - fixBefore.timepoint.getTime());
                    double factorForBefore = 1-factorForAfter;
                    final DegreePosition betweenPosition = new DegreePosition(factorForBefore*fixBefore.position.getLatDeg() + factorForAfter*fixAfter.position.getLatDeg(),
                            factorForBefore*fixBefore.position.getLngDeg() + factorForAfter*fixAfter.position.getLngDeg());
                    final double betweenBearing;
                    if (fixBefore.speedWithBearing == null) {
                        if (fixAfter.speedWithBearing == null) {
                            betweenBearing = 0;
                        } else {
                            betweenBearing = fixAfter.speedWithBearing.bearingInDegrees;
                        }
                    } else if (fixAfter.speedWithBearing == null) {
                        betweenBearing = fixBefore.speedWithBearing.bearingInDegrees;
                    } else {
                        betweenBearing = new ScalableBearing(new DegreeBearingImpl(fixBefore.speedWithBearing.bearingInDegrees)).
                                multiply(factorForBefore).add(new ScalableBearing(new DegreeBearingImpl(fixAfter.speedWithBearing.bearingInDegrees)).
                                        multiply(factorForAfter)).divide(1).getDegrees();
                    }
                    final SpeedWithBearingDTO betweenSpeed = new SpeedWithBearingDTO(
                            factorForBefore*(fixBefore.speedWithBearing==null?0:fixBefore.speedWithBearing.speedInKnots) +
                            factorForAfter*(fixAfter.speedWithBearing==null?0:fixAfter.speedWithBearing.speedInKnots),
                            betweenBearing);
                    result = new GPSFixDTOWithSpeedWindTackAndLegType(date, betweenPosition, betweenSpeed,
                            interpolateOptionalTrueHeading(fixBefore, factorForBefore, fixAfter, factorForAfter),
                            closer.degreesBoatToTheWind, closer.tack, closer.legType, fixBefore.extrapolated || fixAfter.extrapolated);
                }
            } else {
                // perfect match
                final GPSFixDTOWithSpeedWindTackAndLegType fixAfter = competitorFixes.get(i);
                result = fixAfter;
            }
        } else {
            result = null;
        }
        return result;
    }
    
    private Bearing interpolateOptionalTrueHeading(GPSFixDTOWithSpeedWindTackAndLegType fixBefore, double factorForBefore, GPSFixDTOWithSpeedWindTackAndLegType fixAfter, double factorForAfter) {
        assert fixBefore != null && fixAfter != null;
        return (fixBefore == null || fixBefore.optionalTrueHeading == null) ? (fixAfter == null || fixAfter.optionalTrueHeading == null)
                ? null
                : fixAfter.optionalTrueHeading
              : (fixAfter == null || fixAfter.optionalTrueHeading == null)
                ? fixBefore.optionalTrueHeading
                : new DegreeBearingImpl(factorForBefore * fixBefore.optionalTrueHeading.getDegrees() + factorForAfter * fixAfter.optionalTrueHeading.getDegrees());
    }

    public RaceMapSettings getSettings() {
        return settings;
    }

    @Override
    public void addedToSelection(CompetitorDTO competitor) {
        if (settings.isShowOnlySelectedCompetitors()) {
            if (Util.size(competitorSelection.getSelectedCompetitors()) == 1) {
                // first competitors selected; remove all others from map
                Iterator<Map.Entry<String, BoatOverlay>> i = boatOverlaysByCompetitorIdsAsStrings.entrySet().iterator();
                while (i.hasNext()) {
                    Entry<String, BoatOverlay> next = i.next();
                    if (!next.getKey().equals(competitor.getIdAsString())) {
                        CanvasOverlayV3 boatOverlay = next.getValue();
                        boatOverlay.removeFromMap();
                        fixesAndTails.removeTail(next.getKey());
                        i.remove(); // only this way a ConcurrentModificationException while looping can be avoided
                    }
                }
                showCompetitorInfoOnMap(timer.getTime(), -1, competitorSelection.getSelectedFilteredCompetitors());
            }
        } else {
            // only change highlighting
            BoatOverlay boatCanvas = boatOverlaysByCompetitorIdsAsStrings.get(competitor.getIdAsString());
            if (boatCanvas != null) {
                boatCanvas.setDisplayMode(displayHighlighted(competitor));
                boatCanvas.draw();
                showCompetitorInfoOnMap(timer.getTime(), -1, competitorSelection.getSelectedFilteredCompetitors());
            }
        }
        if (selectedDetailType != null && !selectedDetailTypeChanged) {
            // assumes that the detail values have already been loaded, as the detail type hasn't changed
            fixesAndTails.updateDetailValueBoundaries(competitorSelection.getSelectedCompetitors());
        }
        // update tails for all competitors because selection change may also affect all unselected competitors
        for (CompetitorDTO oneOfAllCompetitors : competitorSelection.getAllCompetitors()) {
            Colorline tail = fixesAndTails.getTail(oneOfAllCompetitors);
            if (tail != null) {
                ColorlineOptions newOptions = createTailStyle(oneOfAllCompetitors, displayHighlighted(oneOfAllCompetitors));
                tail.setOptions(newOptions); // depends on the min/max boundaries computed above
            }
        }
        // Trigger auto-zoom if needed
        final RaceMapZoomSettings zoomSettings = settings.getZoomSettings();
        if (!zoomSettings.containsZoomType(ZoomTypes.NONE) && zoomSettings.isZoomToSelectedCompetitors()) {
            zoomMapToNewBounds(zoomSettings.getNewBounds(this));
        }
        redraw(); 
    }
    
    @Override
    public void removedFromSelection(CompetitorDTO competitor) {
        if (isShowAnyHelperLines()) {
            // helper lines depend on which competitor is visible, because the *visible* leader is used for
            // deciding which helper lines to show:
            redraw();
        } else {
            // try a more incremental update otherwise
            if (settings.isShowOnlySelectedCompetitors()) {
                // if selection is now empty, show all competitors
                if (Util.isEmpty(competitorSelection.getSelectedCompetitors())) {
                    redraw();
                } else {
                    // otherwise remove only deselected competitor's boat images and tail
                    final BoatOverlay removedBoatOverlay = boatOverlaysByCompetitorIdsAsStrings.remove(competitor.getIdAsString());
                    if (removedBoatOverlay != null) {
                        removedBoatOverlay.removeFromMap();
                    }
                    fixesAndTails.removeTail(competitor.getIdAsString());
                    showCompetitorInfoOnMap(timer.getTime(), -1, competitorSelection.getSelectedFilteredCompetitors());
                }
            } else {
                // "lowlight" currently selected competitor
                final BoatOverlay boatCanvas = boatOverlaysByCompetitorIdsAsStrings.get(competitor.getIdAsString());
                if (boatCanvas != null) {
                    boatCanvas.setDisplayMode(displayHighlighted(competitor));
                    boatCanvas.draw();
                }
                showCompetitorInfoOnMap(timer.getTime(), -1, competitorSelection.getSelectedFilteredCompetitors());
            }
        }
        // Now update tails for all competitors because selection change may also affect all unselected competitors
        if (selectedDetailType != null && !selectedDetailTypeChanged) {
            fixesAndTails.updateDetailValueBoundaries(competitorSelection.getSelectedCompetitors());
        }
        for (CompetitorDTO oneOfAllCompetitors : competitorSelection.getAllCompetitors()) {
            Colorline tail = fixesAndTails.getTail(oneOfAllCompetitors);
            if (tail != null) {
                ColorlineOptions newOptions = createTailStyle(oneOfAllCompetitors, displayHighlighted(oneOfAllCompetitors));
                tail.setOptions(newOptions);
            }
        }
        // Trigger auto-zoom if needed
        RaceMapZoomSettings zoomSettings = settings.getZoomSettings();
        if (!zoomSettings.containsZoomType(ZoomTypes.NONE) && zoomSettings.isZoomToSelectedCompetitors()) {
            zoomMapToNewBounds(zoomSettings.getNewBounds(this));
        }
    }

    private boolean isShowAnyHelperLines() {
        return settings.getHelpLinesSettings().isShowAnyHelperLines();
    }

    @Override
    public String getLocalizedShortName() {
        return raceMapLifecycle.getLocalizedShortName();
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public boolean hasSettings() {
        return raceMapLifecycle.hasSettings();
    }

    @Override
    public SettingsDialogComponent<RaceMapSettings> getSettingsDialogComponent(RaceMapSettings settings) {
        return new RaceMapSettingsDialogComponent(settings, stringMessages, hasPolar);
    }

    @Override
    public void updateSettings(RaceMapSettings newSettings) {
        boolean maneuverTypeSelectionChanged = false;
        boolean showManeuverLossChanged = false;
        boolean requiresRedraw = false;
        boolean requiresUpdateCoordinateSystem = false;
        if (newSettings.isShowSatelliteLayer() != settings.isShowSatelliteLayer()) {
            requiresUpdateCoordinateSystem = true;
        }
        if (newSettings.isShowManeuverLossVisualization() != settings.isShowManeuverLossVisualization()) {
            showManeuverLossChanged = true;
        }
        for (ManeuverType maneuverType : ManeuverType.values()) {
            if (newSettings.isShowManeuverType(maneuverType) != settings.isShowManeuverType(maneuverType)) {
                maneuverTypeSelectionChanged = true;
            }
        }
        if (newSettings.isShowDouglasPeuckerPoints() != settings.isShowDouglasPeuckerPoints()) {
            if (!(timer.getPlayState() == PlayStates.Playing) && lastDouglasPeuckerResult != null && newSettings.isShowDouglasPeuckerPoints()) {
                removeAllMarkDouglasPeuckerpoints();
                showMarkDouglasPeuckerPoints(lastDouglasPeuckerResult);
            } else if (!newSettings.isShowDouglasPeuckerPoints()) {
                removeAllMarkDouglasPeuckerpoints();
            }
        }
        if (newSettings.getTailLengthInMilliseconds() != settings.getTailLengthInMilliseconds()) {
            requiresRedraw = true;
        }
        if (!newSettings.getBuoyZoneRadius().equals(settings.getBuoyZoneRadius())) {
            requiresRedraw = true;
        }
        if (newSettings.isShowOnlySelectedCompetitors() != settings.isShowOnlySelectedCompetitors()) {
            requiresRedraw = true;
        }
        if (newSettings.isShowSelectedCompetitorsInfo() != settings.isShowSelectedCompetitorsInfo()) {
            requiresRedraw = true;
        }
        final boolean requiresZoom = !newSettings.getZoomSettings().equals(settings.getZoomSettings()) && !newSettings.getZoomSettings().containsZoomType(ZoomTypes.NONE);
        if (!newSettings.getHelpLinesSettings().equals(settings.getHelpLinesSettings())) {
            requiresRedraw = true;
        }
        if (!newSettings.isShowEstimatedDuration() && estimatedDurationOverlay != null){
            estimatedDurationOverlay.removeFromParent();
        }
        if (newSettings.isShowWindStreamletOverlay() != settings.isShowWindStreamletOverlay()) {
            streamletOverlay.setVisible(newSettings.isShowWindStreamletOverlay() 
                    && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceMapLifecycle.getRaceDTO()));
            streamletOverlay.setColors(newSettings.isShowWindStreamletColors());
        }
        if (newSettings.isShowWindStreamletColors() != settings.isShowWindStreamletColors()) {
            streamletOverlay.setColors(newSettings.isShowWindStreamletColors());
        }
        if (newSettings.isShowSimulationOverlay() != settings.isShowSimulationOverlay()) {
            showSimulationOverlay(newSettings.isShowSimulationOverlay()
                    && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.SIMULATOR, raceMapLifecycle.getRaceDTO()));
        }
        if (newSettings.isWindUp() != settings.isWindUp()) {
            requiresUpdateCoordinateSystem = true;
            requiresRedraw = true;
        }
        if (!newSettings.isShowEstimatedDuration() && estimatedDurationOverlay != null){
            estimatedDurationOverlay.removeFromParent();
        }
        if (newSettings.getStartCountDownFontSizeScaling() != settings.getStartCountDownFontSizeScaling()) {
            if (countDownOverlay != null) {
                countDownOverlay.removeFromMap();
                countDownOverlay = null;
            }
            requiresRedraw = true;
        }
        if (newSettings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEAREACIRCLES) != settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEAREACIRCLES)) {
            if (newSettings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEAREACIRCLES)) {
                getAndShowCourseAreaCircles();
            } else {
                for (final CourseAreaCircleOverlay overlay : courseAreaCirclesToShow.values()) {
                    overlay.removeFromMap();
                }
                courseAreaCirclesToShow.clear();
            }
        }
        final boolean needToUpdateWindLadder;
        if (newSettings.isShowWindLadder() != settings.isShowWindLadder()) {
            if (windLadder != null) {
                windLadder.setVisible(newSettings.isShowWindLadder());
            }
            needToUpdateWindLadder = newSettings.isShowWindLadder();
        } else {
            needToUpdateWindLadder = false;
        }
        // now the settings will actually be updated:
        this.settings = newSettings;
        if (requiresZoom) {
            removeTransitions();
            zoomMapToNewBounds(newSettings.getZoomSettings().getNewBounds(this));
        }
        if (maneuverTypeSelectionChanged || showManeuverLossChanged) {
            if (timer.getPlayState() != PlayStates.Playing) {
                maneuverMarkersAndLossIndicators.updateManeuverMarkersAfterSettingsChanged();
            }
        }
        if (needToUpdateWindLadder) {
            showAdvantageLineAndUpdateWindLadder(getCompetitorsToShow(), getTimer().getTime(), /* timeForPositionTransitionMillis */ -1 /* (no transition) */);
        }
        if (requiresUpdateCoordinateSystem) {
            updateCoordinateSystemFromSettings();
        }
        if (requiresRedraw) {
            redraw();
        }
    }

    /**
     * Obtains all course areas for the event of this map's {@link #raceIdentifier primary race} from the server,
     * stores them in {@link #courseAreaCirclesToShow} and draws the corresponding overlays to the map.
     */
    private void getAndShowCourseAreaCircles() {
        sailingService.getCourseAreaForEventOfLeaderboard(leaderboardName, new AsyncCallback<List<CourseAreaDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(stringMessages.errorObtainingCourseAreasForLeaderboard(leaderboardName, caught.getMessage()), NotificationType.ERROR);
            }

            @Override
            public void onSuccess(List<CourseAreaDTO> result) {
                for (final CourseAreaCircleOverlay overlayToRemove : courseAreaCirclesToShow.values()) {
                    overlayToRemove.removeFromMap();
                }
                courseAreaCirclesToShow.clear();
                for (final CourseAreaDTO courseArea : result) {
                    final CourseAreaCircleOverlay overlayToAdd = new CourseAreaCircleOverlay(map, RaceMapOverlaysZIndexes.COURSEAREA_ZINDEX, courseArea, coordinateSystem, stringMessages);
                    courseAreaCirclesToShow.put(courseArea, overlayToAdd);
                    overlayToAdd.addToMap();
                }
            }
        });
    }

    private void showSimulationOverlay(boolean visible) {
        simulationOverlay.setVisible(visible);
        if (visible) {
            simulationOverlay.updateLeg(Math.max(lastLegNumber, 1), true, -1 /* ensure ui-update */);
        }
    }

    public static class BoatsBoundsCalculator extends LatLngBoundsCalculatorForSelected {
        @Override
        public NonCardinalBounds calculateNewBounds(RaceMap forMap) {
            NonCardinalBounds newBounds = null;
            final Iterable<CompetitorDTO> selectedCompetitors = forMap.competitorSelection.getSelectedCompetitors();
            final Iterable<CompetitorDTO> competitors;
            if (selectedCompetitors == null || !selectedCompetitors.iterator().hasNext()) {
                competitors = forMap.getCompetitorsToShow();
            } else {
                competitors = isZoomOnlyToSelectedCompetitors(forMap) ? selectedCompetitors : forMap.getCompetitorsToShow();
            }
            for (final CompetitorDTO competitor : competitors) {
                try {
                    GPSFixDTOWithSpeedWindTackAndLegType competitorFix = forMap.getBoatFix(competitor, forMap.timer.getTime());
                    Position competitorPosition = competitorFix != null ? competitorFix.position : null;
                    if (competitorPosition != null) {
                        if (newBounds == null) {
                            newBounds = NonCardinalBounds.create(competitorPosition, new DegreeBearingImpl(forMap.getMap().getHeading()));
                        } else {
                            newBounds = newBounds.extend(competitorPosition);
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    // TODO can't this be predicted and the exception be avoided in the first place?
                    // Catch this in case the competitor has no GPS fixes at the current time (e.g. in race 'Finale 2' of STG)
                }
            }
            return newBounds;
        }
    }
    
    public static class TailsBoundsCalculator extends LatLngBoundsCalculatorForSelected {
        @Override
        public NonCardinalBounds calculateNewBounds(RaceMap racemap) {
            NonCardinalBounds newBounds = null;
            Iterable<CompetitorDTO> competitors = isZoomOnlyToSelectedCompetitors(racemap) ? racemap.competitorSelection.getSelectedCompetitors() : racemap.getCompetitorsToShow();
            for (CompetitorDTO competitor : competitors) {
                Colorline tail = racemap.fixesAndTails.getTail(competitor);
                NonCardinalBounds bounds = null;
                // TODO: Find a replacement for missing Polyline function getBounds() from v2
                // see also http://stackoverflow.com/questions/3284808/getting-the-bounds-of-a-polyine-in-google-maps-api-v3; 
                // optionally, consider providing a bounds cache with two sorted sets that organize the LatLng objects for O(1) bounds calculation and logarithmic add, ideally O(1) remove
                if (tail != null && tail.getLength() >= 1) {
                    bounds = NonCardinalBounds.create(racemap.getCoordinateSystem().getPosition(tail.getPath().get(0)), new DegreeBearingImpl(racemap.getMap().getHeading()));
                    for (int i = 1; i < tail.getLength(); i++) {
                        bounds = bounds.extend(racemap.getCoordinateSystem().getPosition(tail.getPath().get(i)));
                    }
                }
                if (bounds != null) {
                    if (newBounds == null) {
                        newBounds = bounds;
                    } else {
                        newBounds = newBounds.extend(bounds);
                    }
                }
            }
            return newBounds;
        }
    }

    public static class CourseMarksBoundsCalculator implements NonCardinalBoundsCalculator {
        @Override
        public NonCardinalBounds calculateNewBounds(RaceMap forMap) {
            NonCardinalBounds newBounds = null;
            final Iterable<MarkDTO> marksToZoom = forMap.markDTOs.values();
            if (marksToZoom != null) {
                for (MarkDTO markDTO : marksToZoom) {
                    if (newBounds == null) {
                        newBounds = NonCardinalBounds.create(markDTO.position, new DegreeBearingImpl(forMap.getMap().getHeading()));
                    } else {
                        newBounds = newBounds.extend(markDTO.position);
                    }
                }
            }
            return newBounds;
        }
    }

    public static class WindSensorsBoundsCalculator implements NonCardinalBoundsCalculator {
        @Override
        public NonCardinalBounds calculateNewBounds(RaceMap forMap) {
            NonCardinalBounds newBounds = null;
            Collection<WindSensorOverlay> marksToZoom = forMap.windSensorOverlays.values();
            if (marksToZoom != null) {
                for (WindSensorOverlay windSensorOverlay : marksToZoom) {
                    final LatLng latLngPosition = windSensorOverlay.getLatLngPosition();
                    if (Objects.nonNull(latLngPosition)) {
                        NonCardinalBounds bounds = NonCardinalBounds.create(forMap.getCoordinateSystem().getPosition(latLngPosition), new DegreeBearingImpl(forMap.getMap().getHeading()));
                        if (newBounds == null) {
                            newBounds = bounds;
                        } else {
                            newBounds = newBounds.extend(forMap.getCoordinateSystem().getPosition(latLngPosition));
                        }
                    }
                }
            }
            return newBounds;
        }
    }

    @Override
    public void initializeData(boolean showMapControls, boolean showHeaderPanel) {
        loadMapsAPIV3(showMapControls, showHeaderPanel, this.settings.isShowSatelliteLayer());
    }

    @Override
    public boolean isDataInitialized() {
        return isMapInitialized;
    }

    @Override
    public void onResize() {
        if (map != null) {
            map.triggerResize();
            if (isMapInitialized) {
                zoomMapToNewBounds(settings.getZoomSettings().getNewBounds(RaceMap.this));
            }
        }
        // Adjust RaceMap headers to avoid overlapping based on the RaceMap width  
        boolean isCompactHeader = this.getOffsetWidth() <= 600;
        headerPanel.setStyleName(COMPACT_HEADER_STYLE, isCompactHeader);
        headerPanel.setStyleName(RaceboardDropdownResources.INSTANCE.css().compactHeader(), isCompactHeader);
        // Adjust combined wind and true north indicator panel indent, based on the RaceMap height
        if (topLeftControlsWrapperPanel.getParent() != null) {
            this.adjustLeftControlsIndent();
        }
    }
    
    private void adjustLeftControlsIndent() {
        String leftControlsIndentStyle = getLeftControlsIndentStyle();
        if (leftControlsIndentStyle != null) {
            topLeftControlsWrapperPanel.getParent().addStyleName(leftControlsIndentStyle);
        }
    }

    @Override
    public void competitorsListChanged(Iterable<CompetitorDTO> competitors) {
        redraw();
    }
    
    @Override
    public void filteredCompetitorsListChanged(Iterable<CompetitorDTO> filteredCompetitors) {
        redraw();
    }
    
    @Override
    public void filterChanged(FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> oldFilterSet,
            FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> newFilterSet) {
        // nothing to do; if the list of filtered competitors has changed, a separate call to filteredCompetitorsListChanged will occur
    }
    
    @Override
    public void onColorMappingChanged() {
        metricOverlay.updateLegend(fixesAndTails.getDetailValueBoundaries(), tailColorMapper, selectedDetailType);
        for (final CompetitorDTO competitor : competitorSelection.getSelectedCompetitors()) {
            final Colorline tail = fixesAndTails.getTail(competitor);
            if (tail != null) {
                final ColorlineOptions options = createTailStyle(competitor, displayHighlighted(competitor));
                tail.setOptions(options);
            }
        }
    }

    @Override
    public ColorlineOptions createTailStyle(CompetitorDTO competitor, DisplayMode displayMode) {
        final ColorlineOptions options = new ColorlineOptions();
        options.setClickable(true);
        options.setGeodesic(true);
        options.setStrokeOpacity(1.0);
        switch (displayMode) {
        case DEFAULT:
            options.setColorMode(ColorlineMode.MONOCHROMATIC);
            options.setColorProvider(fixIndexInTail -> competitorSelection.getColor(competitor, raceIdentifier).getAsHtml());
            options.setStrokeWeight(1);
            break;
        case SELECTED:
            options.setColorMode(ColorlineMode.POLYCHROMATIC);
            options.setColorProvider(fixIndexInTail -> {
                final String resultColor;
                final Double detailValue;
                // If a DetailType has been selected and we are not currently waiting for the first update with the new values
                if (selectedDetailType != null && !selectedDetailTypeChanged && (detailValue = fixesAndTails.getDetailValueAt(competitor, fixIndexInTail)) != null) {
                    resultColor = tailColorMapper.getColor(detailValue);
                } else {
                    resultColor = competitorSelection.getColor(competitor, raceIdentifier).getAsHtml();
                }
                return resultColor;
            });
            options.setStrokeWeight(2);
            break;
        case NOT_SELECTED:
            options.setColorMode(ColorlineMode.MONOCHROMATIC);
            options.setColorProvider(fixIndexInTail -> LOWLIGHTED_TAIL_COLOR.getAsHtml());
            options.setStrokeOpacity(LOWLIGHTED_TAIL_OPACITY);
            break;
        }
        options.setZIndex(RaceMapOverlaysZIndexes.BOATTAILS_ZINDEX);
        return options;
    }
    
    @Override
    public Colorline createTail(final CompetitorDTO competitor, List<LatLng> points) {
        final BoatDTO boat = competitorSelection.getBoat(competitor);
        ColorlineOptions options = createTailStyle(competitor, displayHighlighted(competitor));
        Colorline result = new Colorline(options);
        MVCArray<LatLng> pointsAsArray = MVCArray.newInstance(points.toArray(new LatLng[points.size()]));
        result.setPath(pointsAsArray);
        result.setMap(map);
        ColorlineOptions hoverlineOptions = new ColorlineOptions(options);
        hoverlineOptions.setColorMode(ColorlineMode.MONOCHROMATIC);
        hoverlineOptions.setColorProvider(fixIndexInTail -> competitorSelection.getColor(competitor, raceIdentifier).getAsHtml());
        Hoverline resultHoverline = new Hoverline(result, hoverlineOptions, this);
        final ClickMapHandler clickHandler = new ClickMapHandler() {
            @Override
            public void onEvent(ClickMapEvent event) {
                showCompetitorInfoWindow(competitor, event.getMouseEvent().getLatLng());
            }
        };
        result.addClickHandler(clickHandler);
        resultHoverline.addClickHandler(clickHandler);
        result.addMouseOverLineHandler(new MouseOverLineHandler() {
            @Override
            public void onEvent(MouseOverLineEvent event) {
                final Double detailValue;
                map.setTitle(boat.getSailId() + ", " + competitor.getName() +
                        ((selectedDetailType != null && event.getFixIndexInTail() != -1
                        && (detailValue = fixesAndTails.getDetailValueAt(competitor, event.getFixIndexInTail())) != null)
                            ? "\n" + DetailTypeFormatter.format(selectedDetailType)
                             + ": " + numberFormatterForSelectedDetailType.format(detailValue)
                             + DetailTypeFormatter.getUnit(selectedDetailType)
                            : ""));
                
            }
        });
        resultHoverline.addMouseOutMoveHandler(new MouseOutMapHandler() {
            @Override
            public void onEvent(MouseOutMapEvent event) {
                map.setTitle("");
            }
        });
        return result;
    }

    protected void setTailVisualizer(DetailType detailType) {
        ValueRangeFlexibleBoundaries boundaries = new ValueRangeFlexibleBoundaries(0, 10, 0.1, 0.25);
        createTailColorMapper(boundaries, detailType);
        fixesAndTails.setDetailValueBoundaries(boundaries);
    }
    
    protected void createTailColorMapper(ValueRangeFlexibleBoundaries valueRange, DetailType detailType) {
        tailColorMapper = new ColorMapper(valueRange, /*isGrey*/ false, detailType.getValueSpreader());
        tailColorMapper.addListener(this);
    }

    @Override
    public Integer getRank(CompetitorDTO competitor) {
        final Integer result;
        QuickRankDTO quickRank = quickFlagDataProvider.getQuickRanks().get(competitor.getIdAsString());
        if (quickRank != null) {
            result = quickRank.oneBasedRank;
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public Double getSpeedInKnots(CompetitorDTO competitor) {
        return quickFlagDataProvider.getQuickSpeedsInKnots(competitor);
    }
    
    private Image createSAPLogo() {
        ImageResource sapLogoResource = resources.sapLogoOverlay();
        Image sapLogo = new Image(sapLogoResource);
        sapLogo.addClickHandler(event -> Window.open(stringMessages.sapAnalyticsURL(), "_blank", null));
        sapLogo.setStyleName("raceBoard-Logo");
        sapLogo.getElement().setAttribute(DebugConstants.DEBUG_ID_ATTRIBUTE, "raceBoardSapLogo");
        return sapLogo;
    }

    @Override
    public String getDependentCssClassName() {
        return "raceMap";
    }

    /**
     * The default zoom bounds are defined by the boats
     */
    private NonCardinalBounds getDefaultZoomBounds() {
        return new BoatsBoundsCalculator().calculateNewBounds(RaceMap.this);
    }
    
    private MapOptions getMapOptions(boolean windUp, boolean showSatelliteLayer) {
        MapOptions mapOptions = MapOptions.newInstance();
        // Google Maps API does not support rotated satellite images
        mapOptions.setMapTypeId(getMapTypeId(windUp, showSatelliteLayer));
        mapOptions.setScrollWheel(true);
        mapOptions.setMapTypeControl(false);
        mapOptions.setPanControl(false);
        mapOptions.setZoomControl(false);
        mapOptions.setScaleControl(true);
        mapOptions.setDisableDefaultUi(true);
        mapOptions.setFullscreenControl(false);
        // no need to try to position the scale control; it always ends up at the right bottom corner
        mapOptions.setStreetViewControl(false);
        mapOptions.setIsFractionalZoomEnabled(true);
        return mapOptions;
    }

    private String getMapTypeId(boolean windUp, boolean showSatelliteLayer) {
        return showSatelliteLayer && !windUp ? MapTypeId.SATELLITE.toString() : SAILING_ANALYTICS_MAP_TYPE_ID;
    }

    /**
     * @return CSS style name to adjust the indent of left controls (combined wind and true north indicator).
     */
    protected String getLeftControlsIndentStyle() {
        return null;
    }

    public Map<String, CourseMarkOverlay> getCourseMarkOverlays() {
        return courseMarkOverlays;
    }
    
    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }
    
    public void hideAllHelplines() {
        if (startLine != null) {
            startLine.setVisible(false);
        }
        if (finishLine != null) {
            finishLine.setVisible(false);
        }
        if (advantageLine != null) {
            advantageLine.setVisible(false);
        }
        if (windwardStartLineMarkToFirstMarkLine != null && leewardStartLineMarkToFirstMarkLine != null) {
            windwardStartLineMarkToFirstMarkLine.setVisible(false);
            leewardStartLineMarkToFirstMarkLine.setVisible(false);
        }
        for (Polyline courseMiddleline : courseMiddleLines.values()) {
            if (courseMiddleline != null) {
                courseMiddleline.setVisible(false);
            }
        }
        for (Polygon courseSideline : courseSidelines.values()) {
            if (courseSideline != null) {
                courseSideline.setVisible(false);
            }
        }
    }
    
    public void showAllHelplinesToShow() {
        if (settings.getHelpLinesSettings().isShowAnyHelperLines()) {
            if (startLine != null && settings.getHelpLinesSettings().isVisible(HelpLineTypes.STARTLINE))
                startLine.setVisible(true);
            if (finishLine != null && settings.getHelpLinesSettings().isVisible(HelpLineTypes.FINISHLINE))
                finishLine.setVisible(true);
            if (advantageLine != null && settings.getHelpLinesSettings().isVisible(HelpLineTypes.ADVANTAGELINE))
                advantageLine.setVisible(true);
            if (windwardStartLineMarkToFirstMarkLine != null && leewardStartLineMarkToFirstMarkLine != null &&
                    settings.getHelpLinesSettings().isVisible(HelpLineTypes.STARTLINETOFIRSTMARKTRIANGLE)) {
                windwardStartLineMarkToFirstMarkLine.setVisible(true);
                leewardStartLineMarkToFirstMarkLine.setVisible(true);
            }
            if (settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEMIDDLELINE)) {
                for (Polyline courseMiddleline : courseMiddleLines.values()) {
                    if (courseMiddleline != null) {
                        courseMiddleline.setVisible(true);
                    }
                }
            }
            if (settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEGEOMETRY)) {
                for (Polygon courseSideline : courseSidelines.values()) {
                    if (courseSideline != null) {
                        courseSideline.setVisible(true);
                    }
                }
            }
        }
    }
    
    public void addCompetitorsForRaceDefinedListener(CompetitorsForRaceDefinedListener listener) {
        raceCompetitorSet.addCompetitorsForRaceDefinedListener(listener);
    }

    public void removeCompetitorsForRaceDefinedListener(CompetitorsForRaceDefinedListener listener) {
        raceCompetitorSet.removeCompetitorsForRaceDefinedListener(listener);
    }

    @Override
    public String getId() {
        return raceMapLifecycle.getComponentId();
    }
    
    public RaceMapLifecycle getLifecycle() {
        return raceMapLifecycle;
    }

    public void addMapInitializedListener(Runnable runnable) {
        if (isMapInitialized) {
            runnable.run();
        } else {
            this.mapInitializedListener.add(runnable);
        }
    }

    public void addMediaPlayerManagerComponent(final MediaPlayerManagerComponent mediaPlayerManagerComponent) {
        this.addVideoToRaceButton.addClickHandler(clickEvent -> {
            mediaPlayerManagerComponent.addMediaTrack();
        });
    }
    
    public void setAddVideoToRaceButtonVisible(boolean visible) {
        this.addVideoToRaceButton.setVisible(visible);
    }

    /**
     * Based on the map's {@link MapWidget#getBounds()} which may be more than what's actually visible because it's the smallest
     * NW/SE lat/lng "rectangle" that contains the possibly rotated visible area.
     */
    private Distance getMapDiagonalVisibleDistance() {
        return currentMapBounds.getLowerLeft().getDistance(currentMapBounds.getUpperRight());
    }

    private void afterZoomOrHeadingChanged() {
        final boolean resetAutoZoomSettings = !autoZoomIn && !autoZoomOut && !orientationChangeInProgress;
        if (streamletOverlay != null
                && settings.isShowWindStreamletOverlay()
                && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceMapLifecycle.getRaceDTO())) {
            streamletOverlay.onDragStart();
        }
        new com.google.gwt.user.client.Timer() {
            @Override
            public void run() {
                if (resetAutoZoomSettings) {
                    // stop automatic zoom after a manual zoom event; automatic zoom in zoomMapToNewBounds will
                    // restore old settings
                    final List<RaceMapZoomSettings.ZoomTypes> emptyList = Collections.emptyList();
                    RaceMapZoomSettings clearedZoomSettings = new RaceMapZoomSettings(emptyList,
                            settings.getZoomSettings().isZoomToSelectedCompetitors());
                    settings = new RaceMapSettings
                            .RaceMapSettingsBuilder(settings, raceMapLifecycle.getRaceDTO(), paywallResolver)
                            .withZoomSettings(clearedZoomSettings)
                            .build();
                    simulationOverlay.setVisible(false);
                    simulationOverlay.setVisible(settings.isShowSimulationOverlay()
                            && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.SIMULATOR, raceMapLifecycle.getRaceDTO()));
                }
                if (streamletOverlay != null
                        && settings.isShowWindStreamletOverlay()
                        && paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceMapLifecycle.getRaceDTO())) {
                    streamletOverlay.onDragEnd();
                    streamletOverlay.setCanvasSettings();
                    streamletOverlay.onBoundsChanged(map.getZoom() != currentZoomLevel);
                }
                advantageLineLength = getMapDiagonalVisibleDistance();
                showAdvantageLineAndUpdateWindLadder(getCompetitorsToShow(), getTimer().getTime(), /* timeForPositionTransitionMillis */ -1 /* (no transition) */);
            }
        }.schedule(500);
    }
}

