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
import java.util.Set;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.LoadApi;
import com.google.gwt.maps.client.LoadApi.LoadLibrary;
import com.google.gwt.maps.client.MapOptions;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.LatLngBounds;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.maps.client.controls.MapTypeStyle;
import com.google.gwt.maps.client.controls.PanControlOptions;
import com.google.gwt.maps.client.controls.ScaleControlOptions;
import com.google.gwt.maps.client.controls.ZoomControlOptions;
import com.google.gwt.maps.client.events.bounds.BoundsChangeMapEvent;
import com.google.gwt.maps.client.events.bounds.BoundsChangeMapHandler;
import com.google.gwt.maps.client.events.click.ClickMapEvent;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.dragend.DragEndMapEvent;
import com.google.gwt.maps.client.events.dragend.DragEndMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapEvent;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapEvent;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapHandler;
import com.google.gwt.maps.client.events.zoom.ZoomChangeMapEvent;
import com.google.gwt.maps.client.events.zoom.ZoomChangeMapHandler;
import com.google.gwt.maps.client.maptypes.MapTypeStyleFeatureType;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.InfoWindow;
import com.google.gwt.maps.client.overlays.InfoWindowOptions;
import com.google.gwt.maps.client.overlays.Marker;
import com.google.gwt.maps.client.overlays.MarkerOptions;
import com.google.gwt.maps.client.overlays.Polygon;
import com.google.gwt.maps.client.overlays.PolygonOptions;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.maps.client.overlays.PolylineOptions;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.PositionDTO;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.RGBColor;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.gwt.ui.actions.AsyncActionsExecutor;
import com.sap.sailing.gwt.ui.actions.GetRaceMapDataAction;
import com.sap.sailing.gwt.ui.actions.GetWindInfoAction;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.NumberFormatterFactory;
import com.sap.sailing.gwt.ui.client.RaceSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.RequiresDataInitialization;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.TimeListener;
import com.sap.sailing.gwt.ui.client.Timer;
import com.sap.sailing.gwt.ui.client.Timer.PlayStates;
import com.sap.sailing.gwt.ui.client.WindSourceTypeFormatter;
import com.sap.sailing.gwt.ui.client.shared.components.Component;
import com.sap.sailing.gwt.ui.client.shared.components.SettingsDialogComponent;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings.HelpLineTypes;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.LegInfoDTO;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.QuickRankDTO;
import com.sap.sailing.gwt.ui.shared.RaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.SidelineDTO;
import com.sap.sailing.gwt.ui.shared.SpeedWithBearingDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapAPIKey;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapStyleHelper;

public class RaceMap extends AbsolutePanel implements TimeListener, CompetitorSelectionChangeListener, RaceSelectionChangeListener,
        RaceTimesInfoProviderListener, TailFactory, Component<RaceMapSettings>, RequiresDataInitialization, RequiresResize {
    private MapWidget map;

    private final SailingServiceAsync sailingService;
    private final ErrorReporter errorReporter;

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
     * Polyline for the course middle line.
     */
    private Polyline courseMiddleLine;

    private Map<SidelineDTO, Polygon> courseSidelines;
    
    private WindTrackInfoDTO lastCombinedWindTrackInfoDTO;
    
    /**
     * Manages the cached set of {@link GPSFixDTO}s for the boat positions as well as their graphical counterpart in the
     * form of {@link Polyline}s.
     */
    private final FixesAndTails fixesAndTails;

    /**
     * html5 canvases used as boat display on the map
     */
    private final Map<CompetitorDTO, BoatOverlay> boatOverlays;

    /**
     * html5 canvases used for competitor info display on the map
     */
    private final Map<CompetitorDTO, CompetitorInfoOverlay> competitorInfoOverlays;
    
    private SmallTransparentInfoOverlay countDownOverlay;

    /**
     * Map overlays with html5 canvas used to display wind sensors
     */
    private final Map<WindSource, WindSensorOverlay> windSensorOverlays;

    /**
     * Map overlays with html5 canvas used to display course marks including buoy zones
     */
    private final Map<String, CourseMarkOverlay> courseMarkOverlays;

    private final Map<String, MarkDTO> markDTOs;

    /**
     * markers displayed in response to
     * {@link SailingServiceAsync#getDouglasPoints(String, String, Map, Map, double, AsyncCallback)}
     */
    protected Set<Marker> douglasMarkers;

    /**
     * markers displayed in response to
     * {@link SailingServiceAsync#getDouglasPoints(String, String, Map, Map, double, AsyncCallback)}
     */
    private Set<Marker> maneuverMarkers;

    private Map<CompetitorDTO, List<ManeuverDTO>> lastManeuverResult;

    private Map<CompetitorDTO, List<GPSFixDTO>> lastDouglasPeuckerResult;
    
    private CompetitorSelectionProvider competitorSelection;

    private List<RegattaAndRaceIdentifier> selectedRaces;

    /**
     * Used to check if the first initial zoom to the mark markers was already done.
     */
    private boolean mapFirstZoomDone = false;

    private final Timer timer;

    private RaceTimesInfoDTO lastRaceTimesInfo;
    
    private InfoWindow lastInfoWindow = null;
    
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

    private RaceMapImageManager raceMapImageManager; 

    private final RaceMapSettings settings;
    
    private final StringMessages stringMessages;
    
    private boolean isMapInitialized;

    private Date lastTimeChangeBeforeInitialization;

    /**
     * The last quick ranks received from a call to {@link SailingServiceAsync#getQuickRanks(RaceIdentifier, Date, AsyncCallback)} upon
     * the last {@link #timeChanged(Date)} event. Therefore, the ranks listed here correspond to the {@link #timer}'s time.
     */
    private List<QuickRankDTO> quickRanks;

    private final CombinedWindPanel combinedWindPanel;
    
    private final AsyncActionsExecutor asyncActionsExecutor;

    /**
     * The map bounds as last received by map callbacks; used to determine whether to suppress the boat animation during zoom/pan
     */
    private LatLngBounds currentMapBounds;

    public RaceMap(SailingServiceAsync sailingService, AsyncActionsExecutor asyncActionsExecutor, ErrorReporter errorReporter, Timer timer,
            CompetitorSelectionProvider competitorSelection, StringMessages stringMessages) {
        this.setSize("100%", "100%");
        this.stringMessages = stringMessages;
        this.sailingService = sailingService;
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.errorReporter = errorReporter;
        this.timer = timer;
        timer.addTimeListener(this);
        raceMapImageManager = new RaceMapImageManager();
        fixesAndTails = new FixesAndTails();
        markDTOs = new HashMap<String, MarkDTO>();
        courseSidelines = new HashMap<SidelineDTO, Polygon>();
        boatOverlays = new HashMap<CompetitorDTO, BoatOverlay>();
        competitorInfoOverlays = new HashMap<CompetitorDTO, CompetitorInfoOverlay>();
        windSensorOverlays = new HashMap<WindSource, WindSensorOverlay>();
        courseMarkOverlays = new HashMap<String, CourseMarkOverlay>();
        this.competitorSelection = competitorSelection;
        competitorSelection.addCompetitorSelectionChangeListener(this);
        settings = new RaceMapSettings();
        lastTimeChangeBeforeInitialization = null;
        isMapInitialized = false;
        initializeData();
        
        combinedWindPanel = new CombinedWindPanel(raceMapImageManager, stringMessages);
        combinedWindPanel.setVisible(false);
    }
    
    private void loadMapsAPIV3() {
        boolean sensor = true;

        // load all the libs for use in the maps
        ArrayList<LoadLibrary> loadLibraries = new ArrayList<LoadApi.LoadLibrary>();
        loadLibraries.add(LoadLibrary.DRAWING);
        loadLibraries.add(LoadLibrary.GEOMETRY);

        Runnable onLoad = new Runnable() {
          @Override
          public void run() {
              MapOptions mapOptions = MapOptions.newInstance();
              mapOptions.setScrollWheel(true);
              mapOptions.setMapTypeControl(true);
              mapOptions.setPanControl(true);
              mapOptions.setZoomControl(true);
              mapOptions.setScaleControl(true);
              
              MapTypeStyle[] mapTypeStyles = new MapTypeStyle[4];
              
              // hide all transit lines including ferry lines
              mapTypeStyles[0] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.TRANSIT);
              // hide points of interest
              mapTypeStyles[1] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.POI);
              // simplify road display
              mapTypeStyles[2] = GoogleMapStyleHelper.createSimplifiedStyle(MapTypeStyleFeatureType.ROAD);
              // set water color
              mapTypeStyles[3] = GoogleMapStyleHelper.createColorStyle(MapTypeStyleFeatureType.WATER, new RGBColor(0, 136, 255), -35, -34);
              
              mapOptions.setMapTypeStyles(mapTypeStyles);
              
              ScaleControlOptions scaleControlOptions = ScaleControlOptions.newInstance();
              scaleControlOptions.setPosition(ControlPosition.BOTTOM_RIGHT);
              mapOptions.setScaleControlOptions(scaleControlOptions);

              ZoomControlOptions zoomControlOptions = ZoomControlOptions.newInstance();
              zoomControlOptions.setPosition(ControlPosition.TOP_RIGHT);
              mapOptions.setZoomControlOptions(zoomControlOptions);

              PanControlOptions panControlOptions = PanControlOptions.newInstance();
              panControlOptions.setPosition(ControlPosition.TOP_RIGHT);
              mapOptions.setPanControlOptions(panControlOptions);
              
              map = new MapWidget(mapOptions);
              RaceMap.this.add(map, 0, 0);
              RaceMap.this.add(combinedWindPanel, 10, 10);
              RaceMap.this.raceMapImageManager.loadMapIcons(map);
              map.setSize("100%", "100%");
              map.addZoomChangeHandler(new ZoomChangeMapHandler() {
                  @Override
                  public void onEvent(ZoomChangeMapEvent event) {
                      // stop automatic zoom after a manual zoom event; automatic zoom in zoomMapToNewBounds will restore old settings
                      final List<RaceMapZoomSettings.ZoomTypes> emptyList = Collections.emptyList();
                      settings.getZoomSettings().setTypesToConsiderOnZoom(emptyList);
                  }
              });
              map.addDragEndHandler(new DragEndMapHandler() {
                  @Override
                  public void onEvent(DragEndMapEvent event) {
                      // stop automatic zoom after a manual drag event
                      final List<RaceMapZoomSettings.ZoomTypes> emptyList = Collections.emptyList();
                      settings.getZoomSettings().setTypesToConsiderOnZoom(emptyList);
                  }
              });
              map.addBoundsChangeHandler(new BoundsChangeMapHandler() {
                  @Override
                  public void onEvent(BoundsChangeMapEvent event) {
                      if (!isAutoZoomInProgress() && !map.getBounds().equals(currentMapBounds)) {
                          // remove the canvas animations for boats 
                          for (BoatOverlay boatOverlay : RaceMap.this.getBoatOverlays().values()) {
                              boatOverlay.removeCanvasPositionTransition();
                          }
                          // remove the canvas animations for the info overlays of the selected boats 
                          for(CompetitorInfoOverlay infoOverlay: competitorInfoOverlays.values()) {
                              infoOverlay.removeCanvasPositionTransition();
                          }
                      }
                      currentMapBounds = map.getBounds();
                  }
              });
              
              //If there was a time change before the API was loaded, reset the time
              if (lastTimeChangeBeforeInitialization != null) {
                  timeChanged(lastTimeChangeBeforeInitialization);
                  lastTimeChangeBeforeInitialization = null;
              }
              //Data has been initialized
              RaceMap.this.isMapInitialized = true;
              RaceMap.this.redraw();
          }
        };

        LoadApi.go(onLoad, loadLibraries, sensor, "key="+GoogleMapAPIKey.V3_APIKey); 
    }
        
    public void redraw() {
        timeChanged(timer.getTime());
    }
    
    Map<CompetitorDTO, BoatOverlay> getBoatOverlays() {
        return Collections.unmodifiableMap(boatOverlays);
    }
    
    MapWidget getMap() {
        return map;
    }
    
    @Override
    public void onRaceSelectionChange(List<RegattaAndRaceIdentifier> selectedRaces) {
        mapFirstZoomDone = false;
        // TODO bug 494: reset zoom settings to user preferences
        this.selectedRaces = selectedRaces;
    }

    @Override
    public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfos, long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest, long clientTimeWhenResponseWasReceived) {
        timer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
        this.lastRaceTimesInfo = raceTimesInfos.get(selectedRaces.get(0));        
    }

    @Override
    public void timeChanged(final Date date) {
        if (date != null && isMapInitialized) {
            if (selectedRaces != null && !selectedRaces.isEmpty()) {
                RegattaAndRaceIdentifier race = selectedRaces.get(selectedRaces.size() - 1);
                final Iterable<CompetitorDTO> competitorsToShow = getCompetitorsToShow();
                
                if (race != null) {
                    final Triple<Map<CompetitorDTO, Date>, Map<CompetitorDTO, Date>, Map<CompetitorDTO, Boolean>> fromAndToAndOverlap = 
                            fixesAndTails.computeFromAndTo(date, competitorsToShow, settings.getEffectiveTailLengthInMilliseconds());
                    final int requestID = ++boatPositionRequestIDCounter;

                    GetRaceMapDataAction getRaceMapDataAction = new GetRaceMapDataAction(sailingService, competitorSelection.getAllCompetitors(), race,
                            date, fromAndToAndOverlap.getA(), fromAndToAndOverlap.getB(), true, new AsyncCallback<RaceMapDataDTO>() {
                  @Override
                  public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error obtaining racemap data: " + caught.getMessage(), true /*silentMode */);
                  }

                  @Override
                  public void onSuccess(RaceMapDataDTO raceMapDataDTO) {
                    if (map != null && raceMapDataDTO != null) {
                        quickRanks = raceMapDataDTO.quickRanks;
                        // process response only if not received out of order
                        if (startedProcessingRequestID < requestID) {
                            startedProcessingRequestID = requestID;
                            // Do boat specific actions
                            Map<CompetitorDTO, List<GPSFixDTO>> boatData = raceMapDataDTO.boatPositions;
                            fixesAndTails.updateFixes(boatData, fromAndToAndOverlap.getC(), RaceMap.this);
                            showBoatsOnMap(date, getCompetitorsToShow());
                            showCompetitorInfoOnMap(date, competitorSelection.getSelectedCompetitors());
                            if (douglasMarkers != null) {
                                removeAllMarkDouglasPeuckerpoints();
                            }
                            if (maneuverMarkers != null) {
                                removeAllManeuverMarkers();
                            }
                            
                            // Do mark specific actions
                            showCourseMarksOnMap(raceMapDataDTO.coursePositions);
                            showCourseSidelinesOnMap(raceMapDataDTO.courseSidelines);                            
                            showStartAndFinishLines(raceMapDataDTO.coursePositions);
                            showAdvantageLine(competitorsToShow, date);
                                
                            // Rezoom the map
                            // TODO make this a loop across the LatLngBoundsCalculators, pulling them from a collection updated in updateSettings
                            if (!settings.getZoomSettings().containsZoomType(ZoomTypes.NONE)) { // Auto zoom if setting is not manual
                                LatLngBounds bounds = settings.getZoomSettings().getNewBounds(RaceMap.this);
                                zoomMapToNewBounds(bounds);
                                mapFirstZoomDone = true;
                            } else if (!mapFirstZoomDone) { // Zoom once to the marks
                                zoomMapToNewBounds(new CourseMarksBoundsCalculator().calculateNewBounds(RaceMap.this));
                                mapFirstZoomDone = true;
                                /*
                                 * Reset the mapZoomedOrPannedSinceLastRaceSelection: In spite of the fact that
                                 * the map was just zoomed to the bounds of the marks, it was not a zoom or pan
                                 * triggered by the user. As a consequence the
                                 * mapZoomedOrPannedSinceLastRaceSelection option has to reset again.
                                 */
                                // TODO bug 494: consider initial user-specific zoom settings
                            }
                        }
                    } else {
                        lastTimeChangeBeforeInitialization = date;
                    }
                  }
               });
                    asyncActionsExecutor.execute(getRaceMapDataAction);
                    // draw the wind into the map, get the combined wind
                    List<String> windSourceTypeNames = new ArrayList<String>();
                    windSourceTypeNames.add(WindSourceType.EXPEDITION.name());
                    windSourceTypeNames.add(WindSourceType.COMBINED.name());
                    
                    GetWindInfoAction getWindInfoAction = new GetWindInfoAction(sailingService, race, date, 1000L, 1, windSourceTypeNames,
                        new AsyncCallback<WindInfoForRaceDTO>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError("Error obtaining wind information: " + caught.getMessage(), true /*silentMode */);
                                }

                                @Override
                                public void onSuccess(WindInfoForRaceDTO windInfo) {
                                    List<Pair<WindSource, WindTrackInfoDTO>> windSourcesToShow = new ArrayList<Pair<WindSource, WindTrackInfoDTO>>();
                                    if (windInfo != null) {
                                        for (WindSource windSource: windInfo.windTrackInfoByWindSource.keySet()) {
                                            WindTrackInfoDTO windTrackInfoDTO = windInfo.windTrackInfoByWindSource.get(windSource);
                                            switch (windSource.getType()) {
                                                case EXPEDITION:
                                                    // we filter out measured wind sources with a very little confidence
                                                    if (windTrackInfoDTO.minWindConfidence > 0.01) {
                                                        windSourcesToShow.add(new Pair<WindSource, WindTrackInfoDTO>(windSource, windTrackInfoDTO));
                                                    }
                                                    break;
                                                case COMBINED:
                                                    showCombinedWindOnMap(windSource, windTrackInfoDTO);
                                                    if (windTrackInfoDTO != null) {
                                                        lastCombinedWindTrackInfoDTO = windTrackInfoDTO; 
                                                        showAdvantageLine(competitorsToShow, date);
                                                    }
                                                    break;
                                            default:
                                                // Which wind sources are requested is defined in a list above this
                                                // action. So we throw here an exception to notice a missing source.
                                                throw new UnsupportedOperationException(
                                                        "There is currently no support for the enum value '"
                                                                + windSource.getType() + "' in this method.");
                                            }
                                        }
                                    }
                                    showWindSensorsOnMap(windSourcesToShow);
                                }
                            });
                    
                    asyncActionsExecutor.execute(getWindInfoAction);
                }
            }
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
                        sidelinePoints[i] = LatLng.newInstance(sidelineMark.position.latDeg, sidelineMark.position.lngDeg);
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
        
    protected void showCourseMarksOnMap(CoursePositionsDTO courseDTO) {
        if (map != null && courseDTO != null) {
            Map<String, CourseMarkOverlay> toRemoveCourseMarks = new HashMap<String, CourseMarkOverlay>(courseMarkOverlays);
            if (courseDTO.marks != null) {
                for (MarkDTO markDTO : courseDTO.marks) {
                    CourseMarkOverlay courseMarkOverlay = courseMarkOverlays.get(markDTO.getName());
                    if (courseMarkOverlay == null) {
                        courseMarkOverlay = createCourseMarkOverlay(RaceMapOverlaysZIndexes.COURSEMARK_ZINDEX, markDTO);
                        courseMarkOverlay.setShowBuoyZone(settings.getHelpLinesSettings().isVisible(HelpLineTypes.BUOYZONE));
                        courseMarkOverlay.setBuoyZoneRadiusInMeter(settings.getBuoyZoneRadiusInMeters());
                        courseMarkOverlays.put(markDTO.getName(), courseMarkOverlay);
                        markDTOs.put(markDTO.getName(), markDTO);
                        courseMarkOverlay.addToMap();
                    } else {
                        courseMarkOverlay.setMarkPosition(markDTO.position);
                        courseMarkOverlay.setShowBuoyZone(settings.getHelpLinesSettings().isVisible(HelpLineTypes.BUOYZONE));
                        courseMarkOverlay.setBuoyZoneRadiusInMeter(settings.getBuoyZoneRadiusInMeters());
                        courseMarkOverlay.draw();
                        toRemoveCourseMarks.remove(markDTO.getName());
                    }
                }
            }
            for (String toRemoveMarkName : toRemoveCourseMarks.keySet()) {
                CourseMarkOverlay removedOverlay = courseMarkOverlays.remove(toRemoveMarkName);
                if(removedOverlay != null) {
                    removedOverlay.removeFromMap();
                }
            }
        }
    }

    protected void showCombinedWindOnMap(WindSource windSource, WindTrackInfoDTO windTrackInfoDTO) {
        if (map != null) {
            combinedWindPanel.setWindInfo(windTrackInfoDTO, windSource);
            combinedWindPanel.redraw();
        }
    }

    protected void showWindSensorsOnMap(List<Pair<WindSource, WindTrackInfoDTO>> windSensorsList) {
        if (map != null) {
            Set<WindSource> toRemoveWindSources = new HashSet<WindSource>(windSensorOverlays.keySet());
            for (Pair<WindSource, WindTrackInfoDTO> windSourcePair : windSensorsList) {
                WindSource windSource = windSourcePair.getA(); 
                WindTrackInfoDTO windTrackInfoDTO = windSourcePair.getB();

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
                if(removedWindSensorOverlay != null) {
                    removedWindSensorOverlay.removeFromMap();
                }
            }
        }
    }

    protected void showCompetitorInfoOnMap(final Date date, final Iterable<CompetitorDTO> competitorsToShow) {
        if (map != null) {
            if (settings.isShowSelectedCompetitorsInfo()) {
                Set<CompetitorDTO> toRemoveCompetorInfoOverlays = new HashSet<CompetitorDTO>(
                        competitorInfoOverlays.keySet());
                final long timeForPositionTransitionMillis = timer.getPlayState() == PlayStates.Playing ? // animate when playing
                        1300*timer.getRefreshInterval()/1000 : -1;
                for (CompetitorDTO competitorDTO : competitorsToShow) {
                    if (fixesAndTails.hasFixesFor(competitorDTO)) {
                        GPSFixDTO lastBoatFix = getBoatFix(competitorDTO, date);
                        if (lastBoatFix != null) {
                            CompetitorInfoOverlay competitorInfoOverlay = competitorInfoOverlays.get(competitorDTO);
                            if (competitorInfoOverlay == null) {
                                competitorInfoOverlay = createCompetitorInfoOverlay(RaceMapOverlaysZIndexes.INFO_OVERLAY_ZINDEX, competitorDTO);
                                competitorInfoOverlays.put(competitorDTO, competitorInfoOverlay);
                                competitorInfoOverlay.setPosition(lastBoatFix.position, timeForPositionTransitionMillis);
                                competitorInfoOverlay.addToMap();
                            } else {
                                competitorInfoOverlay.setPosition(lastBoatFix.position, timeForPositionTransitionMillis);
                                competitorInfoOverlay.draw();
                            }
                            toRemoveCompetorInfoOverlays.remove(competitorDTO);
                        }
                    }
                }
                for (CompetitorDTO toRemoveCompetorDTO : toRemoveCompetorInfoOverlays) {
                    CompetitorInfoOverlay competitorInfoOverlay = competitorInfoOverlays.get(toRemoveCompetorDTO);
                    competitorInfoOverlay.removeFromMap();
                    competitorInfoOverlays.remove(toRemoveCompetorDTO);
                }
            } else {
                // remove all overlays
                for (CompetitorInfoOverlay competitorInfoOverlay : competitorInfoOverlays.values()) {
                    competitorInfoOverlay.removeFromMap();
                }
                competitorInfoOverlays.clear();
            }
        }
    }
    
    protected void showBoatsOnMap(final Date date, final Iterable<CompetitorDTO> competitorsToShow) {
        if (map != null) {
            Date tailsFromTime = new Date(date.getTime() - settings.getEffectiveTailLengthInMilliseconds());
            Date tailsToTime = date;
            Set<CompetitorDTO> competitorDTOsOfUnusedTails = new HashSet<CompetitorDTO>(fixesAndTails.getCompetitorsWithTails());
            Set<CompetitorDTO> competitorDTOsOfUnusedBoatCanvases = new HashSet<CompetitorDTO>(boatOverlays.keySet());
            final long timeForPositionTransitionMillis = timer.getPlayState() == PlayStates.Playing ? // animate when playing
                    1300*timer.getRefreshInterval()/1000 : -1;
            for (CompetitorDTO competitorDTO : competitorsToShow) {
                if (fixesAndTails.hasFixesFor(competitorDTO)) {
                    Polyline tail = fixesAndTails.getTail(competitorDTO);
                    if (tail == null) {
                        tail = fixesAndTails.createTailAndUpdateIndices(competitorDTO, tailsFromTime, tailsToTime, this);
                        tail.setMap(map);
                    } else {
                        fixesAndTails.updateTail(tail, competitorDTO, tailsFromTime, tailsToTime,
                                (int) (timeForPositionTransitionMillis==-1?-1:timeForPositionTransitionMillis/2));
                        competitorDTOsOfUnusedTails.remove(competitorDTO);
                        PolylineOptions newOptions = createTailStyle(competitorDTO, displayHighlighted(competitorDTO));
                        tail.setOptions(newOptions);
                    }
                    boolean usedExistingBoatCanvas = updateBoatCanvasForCompetitor(competitorDTO, date, 
                            timeForPositionTransitionMillis);
                    if (usedExistingBoatCanvas) {
                        competitorDTOsOfUnusedBoatCanvases.remove(competitorDTO);
                    }
                }
            }
            for (CompetitorDTO unusedBoatCanvasCompetitorDTO : competitorDTOsOfUnusedBoatCanvases) {
                BoatOverlay boatCanvas = boatOverlays.get(unusedBoatCanvasCompetitorDTO);
                boatCanvas.removeFromMap();
                boatOverlays.remove(unusedBoatCanvasCompetitorDTO);
            }
            for (CompetitorDTO unusedTailCompetitorDTO : competitorDTOsOfUnusedTails) {
                fixesAndTails.removeTail(unusedTailCompetitorDTO);
            }
        }
    }

    /**
     * This algorithm is limited to distances such that dlon < pi/2, i.e., those that extend around less than one
     * quarter of the circumference of the earth in longitude. A completely general, but more complicated algorithm is
     * necessary if greater distances are allowed.
     */
    public LatLng calculatePositionAlongRhumbline(LatLng position, double bearingDeg, double distanceInKm) {
        double distianceRad = distanceInKm / 6371.0;  // r = 6371 means earth's radius in km 
        double lat1 = position.getLatitude() / 180. * Math.PI;
        double lon1 = position.getLongitude() / 180. * Math.PI;
        double bearingRad = bearingDeg / 180. * Math.PI;

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distianceRad) + 
                        Math.cos(lat1) * Math.sin(distianceRad) * Math.cos(bearingRad));
        double lon2 = lon1 + Math.atan2(Math.sin(bearingRad)*Math.sin(distianceRad)*Math.cos(lat1), 
                       Math.cos(distianceRad)-Math.sin(lat1)*Math.sin(lat2));
        lon2 = (lon2+3*Math.PI) % (2*Math.PI) - Math.PI;  // normalize to -180..+180�
        
        return LatLng.newInstance(lat2 / Math.PI * 180., lon2  / Math.PI * 180.);
    }
    
    private Pair<Integer, CompetitorDTO> getLeadingVisibleCompetitorInfo(Iterable<CompetitorDTO> competitorsToShow) {
        CompetitorDTO leadingCompetitorDTO = null;
        int legOfLeaderCompetitor = -1;
        // this only works because the quickRanks are sorted
        for (QuickRankDTO quickRank : quickRanks) {
            if (Util.contains(competitorsToShow, quickRank.competitor)) {
                leadingCompetitorDTO = quickRank.competitor;
                legOfLeaderCompetitor = quickRank.legNumber;
                return new Pair<Integer, CompetitorDTO>(legOfLeaderCompetitor, leadingCompetitorDTO);
            }
        }
        return null;
    }

    private void showAdvantageLine(Iterable<CompetitorDTO> competitorsToShow, Date date) {
        if (map != null && lastRaceTimesInfo != null && quickRanks != null && lastCombinedWindTrackInfoDTO != null
                && lastCombinedWindTrackInfoDTO.windFixes.size() > 0) {
            boolean drawAdvantageLine = false;
            if (settings.getHelpLinesSettings().isVisible(HelpLineTypes.ADVANTAGELINE)) {
                // find competitor with highest rank
                Pair<Integer, CompetitorDTO> visibleLeaderInfo = getLeadingVisibleCompetitorInfo(competitorsToShow);
                // the boat fix may be null; may mean that no positions were loaded yet for the leading visible boat;
                // don't show anything
                GPSFixDTO lastBoatFix = null;
                boolean isVisibleLeaderInfoComplete = false;
                boolean isLegTypeKnown = false;
                if (visibleLeaderInfo != null && visibleLeaderInfo.getA() > 0
                        && visibleLeaderInfo.getA() <= lastRaceTimesInfo.getLegInfos().size()) {
                    isVisibleLeaderInfoComplete = true;
                    LegInfoDTO legInfoDTO = lastRaceTimesInfo.getLegInfos().get(visibleLeaderInfo.getA() - 1);
                    if (legInfoDTO.legType != null) {
                        isLegTypeKnown = true;
                    }
                    lastBoatFix = getBoatFix(visibleLeaderInfo.getB(), date);
                }
                if (isVisibleLeaderInfoComplete && isLegTypeKnown && lastBoatFix != null) {
                    LegInfoDTO legInfoDTO = lastRaceTimesInfo.getLegInfos().get(visibleLeaderInfo.getA() - 1);
                    double advantageLineLengthInKm = 1.0; // TODO this should probably rather scale with the visible
                                                          // area of the map; bug 616
                    double distanceFromBoatPositionInKm = visibleLeaderInfo.getB().getBoatClass().getHullLengthInMeters() / 1000.; // one hull length
                    // implement and use Position.translateRhumb()
                    double bearingOfBoatInDeg = lastBoatFix.speedWithBearing.bearingInDegrees;
                    LatLng boatPosition = LatLng.newInstance(lastBoatFix.position.latDeg, lastBoatFix.position.lngDeg);
                    LatLng posAheadOfFirstBoat = calculatePositionAlongRhumbline(boatPosition, bearingOfBoatInDeg,
                            distanceFromBoatPositionInKm);
                    final WindDTO windFix = lastCombinedWindTrackInfoDTO.windFixes.get(0);
                    double bearingOfCombinedWindInDeg = windFix.trueWindBearingDeg;
                    double rotatedBearingDeg1 = 0.0;
                    double rotatedBearingDeg2 = 0.0;
                    switch (legInfoDTO.legType) {
                    case UPWIND:
                    case DOWNWIND: {
                        rotatedBearingDeg1 = bearingOfCombinedWindInDeg + 90.0;
                        if (rotatedBearingDeg1 >= 360.0) {
                            rotatedBearingDeg1 -= 360.0;
                        }
                        rotatedBearingDeg2 = bearingOfCombinedWindInDeg - 90.0;
                        if (rotatedBearingDeg2 < 0.0) {
                            rotatedBearingDeg2 += 360.0;
                        }
                    }
                        break;
                    case REACHING: {
                        rotatedBearingDeg1 = legInfoDTO.legBearingInDegrees + 90.0;
                        if (rotatedBearingDeg1 >= 360.0) {
                            rotatedBearingDeg1 -= 360.0;
                        }
                        rotatedBearingDeg2 = legInfoDTO.legBearingInDegrees - 90.0;
                        if (rotatedBearingDeg2 < 0.0) {
                            rotatedBearingDeg2 += 360.0;
                        }
                    }
                        break;
                    }
                    LatLng advantageLinePos1 = calculatePositionAlongRhumbline(posAheadOfFirstBoat, rotatedBearingDeg1,
                            advantageLineLengthInKm / 2.0);
                    LatLng advantageLinePos2 = calculatePositionAlongRhumbline(posAheadOfFirstBoat, rotatedBearingDeg2,
                            advantageLineLengthInKm / 2.0);

                    if (advantageLine == null) {
                        PolylineOptions options = PolylineOptions.newInstance();
                        options.setClickable(true);
                        options.setGeodesic(true);
                        options.setStrokeColor("#000000");
                        options.setStrokeWeight(1);
                        options.setStrokeOpacity(0.5);

                        advantageLine = Polyline.newInstance(options);
                        MVCArray<LatLng> pointsAsArray = MVCArray.newInstance();
                        pointsAsArray.insertAt(0, advantageLinePos1);
                        pointsAsArray.insertAt(1, advantageLinePos2);
                        advantageLine.setPath(pointsAsArray);
                                               
                        advantageLineMouseOverHandler = new AdvantageLineMouseOverMapHandler(bearingOfCombinedWindInDeg,
                                new Date(windFix.measureTimepoint));
                        advantageLine.addMouseOverHandler(advantageLineMouseOverHandler);
                        advantageLine.addMouseOutMoveHandler(new MouseOutMapHandler() {
                            @Override
                            public void onEvent(MouseOutMapEvent event) {
                                map.setTitle("");
                            }
                        });
                        advantageLine.setMap(map);
                    } else {
                        advantageLine.getPath().removeAt(1);
                        advantageLine.getPath().removeAt(0);
                        advantageLine.getPath().insertAt(0, advantageLinePos1);
                        advantageLine.getPath().insertAt(1, advantageLinePos2);
                        advantageLineMouseOverHandler.setTrueWindBearing(bearingOfCombinedWindInDeg);
                        advantageLineMouseOverHandler.setDate(new Date(windFix.measureTimepoint));
                    }
                    drawAdvantageLine = true;
                }
            }
            if (!drawAdvantageLine) {
                if (advantageLine != null) {
                    advantageLine.setMap(null);
                    advantageLine = null;
                }
            }
        }
    }
    
    final StringBuilder startLineAdvantageText = new StringBuilder();
    final StringBuilder finishLineAdvantageText = new StringBuilder();

    /**
     * Tells whether currently an auto-zoom is in progress; this is used particularly to keep the smooth CSS boat transitions
     * active while auto-zooming whereas stopping them seems the better option for manual zooms.
     */
    private boolean autoZoomInProgress;

    private void showStartAndFinishLines(final CoursePositionsDTO courseDTO) {
        if (map != null && courseDTO != null && lastRaceTimesInfo != null) {
            Pair<Integer, CompetitorDTO> leadingVisibleCompetitorInfo = getLeadingVisibleCompetitorInfo(getCompetitorsToShow());
            int legOfLeadingCompetitor = leadingVisibleCompetitorInfo == null ? -1 : leadingVisibleCompetitorInfo.getA();
            int numberOfLegs = lastRaceTimesInfo.legInfos.size();
            // draw the start line
            updateCountdownCanvas(courseDTO.startMarkPositions);
            if (legOfLeadingCompetitor <= 1 && 
                    settings.getHelpLinesSettings().isVisible(HelpLineTypes.STARTLINE) && courseDTO.startMarkPositions != null && courseDTO.startMarkPositions.size() == 2) {
                LatLng startLinePoint1 = LatLng.newInstance(courseDTO.startMarkPositions.get(0).latDeg, courseDTO.startMarkPositions.get(0).lngDeg); 
                LatLng startLinePoint2 = LatLng.newInstance(courseDTO.startMarkPositions.get(1).latDeg, courseDTO.startMarkPositions.get(1).lngDeg); 
                if (courseDTO.startLineAngleToCombinedWind != null) {
                    startLineAdvantageText.replace(0, startLineAdvantageText.length(), " "+stringMessages.lineAngleToWindAndAdvantage(
                            NumberFormat.getFormat("0.0").format(courseDTO.startLineLengthInMeters),
                            NumberFormat.getFormat("0.0").format(Math.abs(courseDTO.startLineAngleToCombinedWind)),
                            courseDTO.startLineAdvantageousSide.name().charAt(0)+courseDTO.startLineAdvantageousSide.name().substring(1).toLowerCase(),
                            NumberFormat.getFormat("0.0").format(courseDTO.startLineAdvantageInMeters)));
                } else {
                    startLineAdvantageText.delete(0, startLineAdvantageText.length());
                }
                if (startLine == null) {
                    PolylineOptions options = PolylineOptions.newInstance();
                    options.setClickable(true);
                    options.setGeodesic(true);
                    options.setStrokeColor("#FFFFFF");
                    options.setStrokeWeight(1);
                    options.setStrokeOpacity(1.0);
                    
                    MVCArray<LatLng> pointsAsArray = MVCArray.newInstance();
                    pointsAsArray.insertAt(0, startLinePoint1);
                    pointsAsArray.insertAt(1, startLinePoint2);

                    startLine = Polyline.newInstance(options);
                    startLine.setPath(pointsAsArray);

                    startLine.addMouseOverHandler(new MouseOverMapHandler() {
                        @Override
                        public void onEvent(MouseOverMapEvent event) {
                            map.setTitle(stringMessages.startLine()+startLineAdvantageText);
                        }
                    });
                    startLine.addMouseOutMoveHandler(new MouseOutMapHandler() {
                        @Override
                        public void onEvent(MouseOutMapEvent event) {
                            map.setTitle("");
                        }
                    });
                    startLine.setMap(map);
                } else {
                    startLine.getPath().removeAt(1);
                    startLine.getPath().removeAt(0);
                    startLine.getPath().insertAt(0, startLinePoint1);
                    startLine.getPath().insertAt(1, startLinePoint2);
                }
            } else {
                if (startLine != null) {
                    startLine.setMap(null);
                    startLine = null;
                }
            }
            // draw the finish line
            if (legOfLeadingCompetitor > 0 && legOfLeadingCompetitor == numberOfLegs &&
                settings.getHelpLinesSettings().isVisible(HelpLineTypes.FINISHLINE) && courseDTO.finishMarkPositions != null && courseDTO.finishMarkPositions.size() == 2) {
                LatLng finishLinePoint1 = LatLng.newInstance(courseDTO.finishMarkPositions.get(0).latDeg, courseDTO.finishMarkPositions.get(0).lngDeg); 
                LatLng finishLinePoint2 = LatLng.newInstance(courseDTO.finishMarkPositions.get(1).latDeg, courseDTO.finishMarkPositions.get(1).lngDeg); 
                if (courseDTO.startLineAngleToCombinedWind != null) {
                    finishLineAdvantageText.replace(0, finishLineAdvantageText.length(), " "+stringMessages.lineAngleToWindAndAdvantage(
                            NumberFormat.getFormat("0.0").format(courseDTO.finishLineLengthInMeters),
                            NumberFormat.getFormat("0.0").format(Math.abs(courseDTO.finishLineAngleToCombinedWind)),
                            courseDTO.finishLineAdvantageousSide.name().charAt(0)+courseDTO.finishLineAdvantageousSide.name().substring(1).toLowerCase(),
                            NumberFormat.getFormat("0.0").format(courseDTO.finishLineAdvantageInMeters)));
                } else {
                    finishLineAdvantageText.delete(0, finishLineAdvantageText.length());
                }
                if (finishLine == null) {
                    PolylineOptions options = PolylineOptions.newInstance();
                    options.setClickable(true);
                    options.setGeodesic(true);
                    options.setStrokeColor("#000000");
                    options.setStrokeWeight(1);
                    options.setStrokeOpacity(1.0);
                   
                    MVCArray<LatLng> pointsAsArray = MVCArray.newInstance();
                    pointsAsArray.insertAt(0, finishLinePoint1);
                    pointsAsArray.insertAt(1, finishLinePoint2);

                    finishLine = Polyline.newInstance(options);
                    finishLine.setPath(pointsAsArray);

                    finishLine.addMouseOverHandler(new MouseOverMapHandler() {
                        @Override
                        public void onEvent(MouseOverMapEvent event) {
                            map.setTitle(stringMessages.finishLine()+finishLineAdvantageText);
                        }
                    });
                    finishLine.addMouseOutMoveHandler(new MouseOutMapHandler() {
                        @Override
                        public void onEvent(MouseOutMapEvent event) {
                            map.setTitle("");
                        }
                    });
                    finishLine.setMap(map);
                } else {
                    finishLine.getPath().removeAt(1);
                    finishLine.getPath().removeAt(0);
                    finishLine.getPath().insertAt(0, finishLinePoint1);
                    finishLine.getPath().insertAt(1, finishLinePoint2);
                }
            }
            else {
                if (finishLine != null) {
                    finishLine.setMap(null);
                    finishLine = null;
                }
            }
            // draw the course middle line
            if (legOfLeadingCompetitor > 0 && courseDTO.waypointPositions.size() > legOfLeadingCompetitor &&
                    settings.getHelpLinesSettings().isVisible(HelpLineTypes.COURSEMIDDLELINE)) {
                PositionDTO position1DTO = courseDTO.waypointPositions.get(legOfLeadingCompetitor-1);
                PositionDTO position2DTO = courseDTO.waypointPositions.get(legOfLeadingCompetitor);
                LatLng courseMiddleLinePoint1 = LatLng.newInstance(position1DTO.latDeg, position1DTO.lngDeg);
                LatLng courseMiddleLinePoint2 = LatLng.newInstance(position2DTO.latDeg, position2DTO.lngDeg); 
                if (courseMiddleLine == null) {
                    PolylineOptions options = PolylineOptions.newInstance();
                    options.setClickable(true);
                    options.setGeodesic(true);
                    options.setStrokeColor("#6896c6");
                    options.setStrokeWeight(1);
                    options.setStrokeOpacity(1.0);
                    
                    MVCArray<LatLng> pointsAsArray = MVCArray.newInstance();
                    pointsAsArray.insertAt(0, courseMiddleLinePoint1);
                    pointsAsArray.insertAt(1, courseMiddleLinePoint2);

                    courseMiddleLine = Polyline.newInstance(options);
                    courseMiddleLine.setPath(pointsAsArray);

                    courseMiddleLine.addMouseOverHandler(new MouseOverMapHandler() {
                        @Override
                        public void onEvent(MouseOverMapEvent event) {
                            map.setTitle(stringMessages.courseMiddleLine());
                        }
                    });
                    courseMiddleLine.addMouseOutMoveHandler(new MouseOutMapHandler() {
                        @Override
                        public void onEvent(MouseOutMapEvent event) {
                            map.setTitle("");
                        }
                    });
                    courseMiddleLine.setMap(map);
                } else {
                    courseMiddleLine.getPath().removeAt(1);
                    courseMiddleLine.getPath().removeAt(0);
                    courseMiddleLine.getPath().insertAt(0, courseMiddleLinePoint1);
                    courseMiddleLine.getPath().insertAt(1, courseMiddleLinePoint2);
                }
            }
            else {
                if (courseMiddleLine != null) {
                    courseMiddleLine.setMap(null);
                    courseMiddleLine = null;
                }
            }
        }
    }
    
    /**
     * If, according to {@link #lastRaceTimesInfo} and {@link #timer} the race is
     * still in the pre-start phase, show a {@link SmallTransparentInfoOverlay} at the
     * start line that shows the count down.
     */
    private void updateCountdownCanvas(List<PositionDTO> startMarkPositions) {
        if (!settings.isShowSelectedCompetitorsInfo() || startMarkPositions == null || startMarkPositions.isEmpty()
                || lastRaceTimesInfo.startOfRace == null || timer.getTime().after(lastRaceTimesInfo.startOfRace)) {
            if (countDownOverlay != null) {
                countDownOverlay.removeFromMap();
                countDownOverlay = null;
            }
        } else {
            final String countDownText = "-"+NumberFormat.getFormat("0.0").format(
                    Math.round(((double) lastRaceTimesInfo.startOfRace.getTime() - timer.getTime().getTime()) / 100.)/10.) + "s";
            if (countDownOverlay == null) {
                countDownOverlay = new SmallTransparentInfoOverlay(map, RaceMapOverlaysZIndexes.INFO_OVERLAY_ZINDEX,
                        countDownText);
                countDownOverlay.addToMap();
            } else {
                countDownOverlay.setInfoText(countDownText);
            }
            countDownOverlay.setPosition(startMarkPositions.get(startMarkPositions.size() - 1), -1);
            countDownOverlay.draw();
        }
    }

    private void zoomMapToNewBounds(LatLngBounds newBounds) {
        if (newBounds != null) {
            List<ZoomTypes> oldZoomSettings = settings.getZoomSettings().getTypesToConsiderOnZoom();
            setAutoZoomInProgress(true);
            map.setCenter(newBounds.getCenter());
            map.fitBounds(newBounds);
            settings.getZoomSettings().setTypesToConsiderOnZoom(oldZoomSettings);
            setAutoZoomInProgress(false);
        }
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
        boolean usedExistingCanvas = false;
        GPSFixDTO lastBoatFix = getBoatFix(competitorDTO, date);
        if (lastBoatFix != null) {
            BoatOverlay boatOverlay = boatOverlays.get(competitorDTO);
            if (boatOverlay == null) {
                boatOverlay = createBoatOverlay(RaceMapOverlaysZIndexes.BOATS_ZINDEX, competitorDTO, displayHighlighted(competitorDTO));
                boatOverlays.put(competitorDTO, boatOverlay);
                boatOverlay.setSelected(displayHighlighted(competitorDTO));
                boatOverlay.setBoatFix(lastBoatFix, timeForPositionTransitionMillis);
                boatOverlay.addToMap();
            } else {
                usedExistingCanvas = true;
                boatOverlay.setSelected(displayHighlighted(competitorDTO));
                boatOverlay.setBoatFix(lastBoatFix, timeForPositionTransitionMillis);
                boatOverlay.draw();
            }
        }

        return usedExistingCanvas;
    }

    private boolean displayHighlighted(CompetitorDTO competitorDTO) {
        return !settings.isShowOnlySelectedCompetitors() && competitorSelection.isSelected(competitorDTO);
    }

    protected CourseMarkOverlay createCourseMarkOverlay(int zIndex, final MarkDTO markDTO) {
        final CourseMarkOverlay courseMarkOverlay = new CourseMarkOverlay(map, zIndex, markDTO);
        courseMarkOverlay.addClickHandler(new ClickMapHandler() {
            @Override
            public void onEvent(ClickMapEvent event) {
                LatLng latlng = courseMarkOverlay.getMarkPosition();
                showMarkInfoWindow(markDTO, latlng);
            }
        });
        return courseMarkOverlay;
    }

    private CompetitorInfoOverlay createCompetitorInfoOverlay(int zIndex, final CompetitorDTO competitorDTO) {
        String infoText = competitorDTO.getSailID() == null || competitorDTO.getSailID().isEmpty() ? competitorDTO.getName() : competitorDTO.getSailID();
        return new CompetitorInfoOverlay(map, zIndex, competitorSelection.getColor(competitorDTO), infoText);
    }
    
    private BoatOverlay createBoatOverlay(int zIndex, final CompetitorDTO competitorDTO, boolean highlighted) {
        final BoatOverlay boatCanvas = new BoatOverlay(map, zIndex, competitorDTO, competitorSelection.getColor(competitorDTO));
        boatCanvas.setSelected(highlighted);
        boatCanvas.addClickHandler(new ClickMapHandler() {
            @Override
            public void onEvent(ClickMapEvent event) {
                if (lastInfoWindow != null) {
                    lastInfoWindow.close();
                }
                GPSFixDTO latestFixForCompetitor = getBoatFix(competitorDTO, timer.getTime());
                LatLng where = LatLng.newInstance(latestFixForCompetitor.position.latDeg, latestFixForCompetitor.position.lngDeg);
                InfoWindowOptions options = InfoWindowOptions.newInstance();
                InfoWindow infoWindow = InfoWindow.newInstance(options);
                infoWindow.setContent(getInfoWindowContent(competitorDTO, latestFixForCompetitor));
                infoWindow.setPosition(where);
                lastInfoWindow = infoWindow;
                infoWindow.open(map);
            }
        });

        boatCanvas.addMouseOverHandler(new MouseOverMapHandler() {
            @Override
            public void onEvent(MouseOverMapEvent event) {
                map.setTitle(competitorDTO.getSailID());
            }
        });
        boatCanvas.addMouseOutMoveHandler(new MouseOutMapHandler() {
            @Override
            public void onEvent(MouseOutMapEvent event) {
                map.setTitle("");
            }
        });

        return boatCanvas;
    }

    protected WindSensorOverlay createWindSensorOverlay(int zIndex, final WindSource windSource, final WindTrackInfoDTO windTrackInfoDTO) {
        final WindSensorOverlay windSensorOverlay = new WindSensorOverlay(map, zIndex, raceMapImageManager, stringMessages);
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
        if(lastInfoWindow != null) {
            lastInfoWindow.close();
        }
        InfoWindowOptions options = InfoWindowOptions.newInstance();
        InfoWindow infoWindow = InfoWindow.newInstance(options);
        infoWindow.setContent(getInfoWindowContent(markDTO));
        infoWindow.setPosition(position);
        lastInfoWindow = infoWindow;
        infoWindow.open(map);
    }

    private void showCompetitorInfoWindow(final CompetitorDTO competitorDTO, LatLng where) {
        if(lastInfoWindow != null) {
            lastInfoWindow.close();
        }
        GPSFixDTO latestFixForCompetitor = getBoatFix(competitorDTO, timer.getTime()); 
        // TODO find close fix where the mouse was; see BUG 470
        InfoWindowOptions options = InfoWindowOptions.newInstance();
        InfoWindow infoWindow = InfoWindow.newInstance(options);
        infoWindow.setContent(getInfoWindowContent(competitorDTO, latestFixForCompetitor));
        infoWindow.setPosition(where);
        lastInfoWindow = infoWindow;
        infoWindow.open(map);
    }

    private String formatPosition(double lat, double lng) {
        NumberFormat numberFormat = NumberFormat.getFormat("0.00000");
        String result = numberFormat.format(lat) + " lat, " + numberFormat.format(lng) + " lng";
        return result;
    }
    
    private void showWindSensorInfoWindow(final WindSensorOverlay windSensorOverlay) {
    	WindSource windSource = windSensorOverlay.getWindSource();
    	WindTrackInfoDTO windTrackInfoDTO = windSensorOverlay.getWindTrackInfoDTO();
        WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
        if(windDTO != null && windDTO.position != null) {
            if(lastInfoWindow != null) {
                lastInfoWindow.close();
            }
            LatLng where = LatLng.newInstance(windDTO.position.latDeg, windDTO.position.lngDeg);
            InfoWindowOptions options = InfoWindowOptions.newInstance();
            InfoWindow infoWindow = InfoWindow.newInstance(options);
            infoWindow.setContent(getInfoWindowContent(windSource, windTrackInfoDTO));
            infoWindow.setPosition(where);
            lastInfoWindow = infoWindow;
            infoWindow.open(map);
        }
    }

    private Widget createInfoWindowLabelAndValue(String labelName, String value) {
    	FlowPanel flowPanel = new FlowPanel();
        Label label = new Label(labelName + ":");
        label.setWordWrap(false);
        label.getElement().getStyle().setFloat(Style.Float.LEFT);
        label.getElement().getStyle().setPadding(3, Style.Unit.PX);
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        flowPanel.add(label);

        Label valueLabel = new Label(value);
        valueLabel.setWordWrap(false);
        valueLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        valueLabel.getElement().getStyle().setPadding(3, Style.Unit.PX);
        flowPanel.add(valueLabel);

        return flowPanel;
    }
    
    private Widget getInfoWindowContent(MarkDTO markDTO) {
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.mark(), markDTO.getName()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.position(), formatPosition(markDTO.position.latDeg, markDTO.position.lngDeg)));
        return vPanel;
    }

    private Widget getInfoWindowContent(WindSource windSource, WindTrackInfoDTO windTrackInfoDTO) {
        WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
        NumberFormat numberFormat = NumberFormat.getFormat("0.0");
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.windSource(), WindSourceTypeFormatter.format(windSource, stringMessages)));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.wind(), Math.round(windDTO.dampenedTrueWindFromDeg) + " " + stringMessages.degreesShort()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.windSpeed(), numberFormat.format(windDTO.dampenedTrueWindSpeedInKnots)));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.position(), formatPosition(windDTO.position.latDeg, windDTO.position.lngDeg)));
        return vPanel;
    }

    private Widget getInfoWindowContent(CompetitorDTO competitorDTO, GPSFixDTO lastFix) {
        final VerticalPanel vPanel = new VerticalPanel();
        vPanel.setWidth("350px");
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.competitor(), competitorDTO.getName()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.sailNumber(), competitorDTO.getSailID()));
        Integer rank = null;
        if (quickRanks != null) {
            for (QuickRankDTO quickRank : quickRanks) {
                if (quickRank.competitor.equals(competitorDTO)) {
                    rank = quickRank.rank;
                    break;
                }
            }
        }
        if (rank != null) {
            vPanel.add(createInfoWindowLabelAndValue(stringMessages.rank(), String.valueOf(rank)));
        }
        SpeedWithBearingDTO speedWithBearing = lastFix.speedWithBearing;
        if (speedWithBearing == null) {
            // TODO should we show the boat at all?
            speedWithBearing = new SpeedWithBearingDTO(0, 0);
        }
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.speed(),
                NumberFormatterFactory.getDecimalFormat(1).format(speedWithBearing.speedInKnots) + " "+stringMessages.knotsUnit()));
        vPanel.add(createInfoWindowLabelAndValue(stringMessages.bearing(), (int) speedWithBearing.bearingInDegrees + " "+stringMessages.degreesShort()));
        if (lastFix.degreesBoatToTheWind != null) {
            vPanel.add(createInfoWindowLabelAndValue(stringMessages.degreesBoatToTheWind(),
                    (int) Math.abs(lastFix.degreesBoatToTheWind) + " " + stringMessages.degreesShort()));
        }
        if (!selectedRaces.isEmpty()) {
            RegattaAndRaceIdentifier race = selectedRaces.get(selectedRaces.size() - 1);
            if (race != null) {
                Map<CompetitorDTO, Date> from = new HashMap<CompetitorDTO, Date>();
                from.put(competitorDTO, fixesAndTails.getFixes(competitorDTO).get(fixesAndTails.getFirstShownFix(competitorDTO)).timepoint);
                Map<CompetitorDTO, Date> to = new HashMap<CompetitorDTO, Date>();
                to.put(competitorDTO, getBoatFix(competitorDTO, timer.getTime()).timepoint);
                sailingService.getDouglasPoints(race, from, to, 3,
                        new AsyncCallback<Map<CompetitorDTO, List<GPSFixDTO>>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Error obtaining douglas positions: " + caught.getMessage(), true /*silentMode */);
                            }

                            @Override
                            public void onSuccess(Map<CompetitorDTO, List<GPSFixDTO>> result) {
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
                sailingService.getManeuvers(race, from, to,
                        new AsyncCallback<Map<CompetitorDTO, List<ManeuverDTO>>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Error obtaining maneuvers: " + caught.getMessage(), true /*silentMode */);
                            }

                            @Override
                            public void onSuccess(Map<CompetitorDTO, List<ManeuverDTO>> result) {
                                lastManeuverResult = result;
                                if (maneuverMarkers != null) {
                                    removeAllManeuverMarkers();
                                }
                                if (!(timer.getPlayState() == PlayStates.Playing)) {
                                    showManeuvers(result);
                                }
                            }
                        });

            }
        }
        return vPanel;
    }

    private Iterable<CompetitorDTO> getCompetitorsToShow() {
        Iterable<CompetitorDTO> result;
        Iterable<CompetitorDTO> selection = competitorSelection.getSelectedCompetitors();
        if (!settings.isShowOnlySelectedCompetitors() || Util.isEmpty(selection)) {
            result = competitorSelection.getFilteredCompetitors();
        } else {
            result = selection;
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

    protected void removeAllManeuverMarkers() {
        if (maneuverMarkers != null) {
            for (Marker marker : maneuverMarkers) {
                marker.setMap((MapWidget) null);
            }
            maneuverMarkers = null;
        }
    }

    protected void showMarkDouglasPeuckerPoints(Map<CompetitorDTO, List<GPSFixDTO>> gpsFixPointMapForCompetitors) {
        douglasMarkers = new HashSet<Marker>();
        if (map != null && gpsFixPointMapForCompetitors != null) {
            Set<CompetitorDTO> keySet = gpsFixPointMapForCompetitors.keySet();
            Iterator<CompetitorDTO> iter = keySet.iterator();
            while (iter.hasNext()) {
                CompetitorDTO competitorDTO = iter.next();
                List<GPSFixDTO> gpsFix = gpsFixPointMapForCompetitors.get(competitorDTO);
                for (GPSFixDTO fix : gpsFix) {
                    LatLng latLng = LatLng.newInstance(fix.position.latDeg, fix.position.lngDeg);
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

    protected void showManeuvers(Map<CompetitorDTO, List<ManeuverDTO>> maneuvers) {
        maneuverMarkers = new HashSet<Marker>();
        if (map != null && maneuvers != null) {
            Set<CompetitorDTO> keySet = maneuvers.keySet();
            Iterator<CompetitorDTO> iter = keySet.iterator();
            while (iter.hasNext()) {
                CompetitorDTO competitorDTO = iter.next();
                List<ManeuverDTO> maneuversForCompetitor = maneuvers.get(competitorDTO);
                for (ManeuverDTO maneuver : maneuversForCompetitor) {
                    if (settings.isShowManeuverType(maneuver.type)) {
                        LatLng latLng = LatLng.newInstance(maneuver.position.latDeg, maneuver.position.lngDeg);
                        Marker maneuverMarker = raceMapImageManager.maneuverIconsForTypeAndTargetTack.get(new Util.Pair<ManeuverType, Tack>(maneuver.type, maneuver.newTack));
                        MarkerOptions options = MarkerOptions.newInstance();
                        options.setTitle(maneuver.toString(stringMessages));
                        options.setIcon(maneuverMarker.getIcon_MarkerImage());
                        Marker marker = Marker.newInstance(options);
                        marker.setPosition(latLng);
                        maneuverMarkers.add(marker);
                        marker.setMap(map);
                    }
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
    private GPSFixDTO getBoatFix(CompetitorDTO competitorDTO, Date date) {
        GPSFixDTO result = null;
        List<GPSFixDTO> competitorFixes = fixesAndTails.getFixes(competitorDTO);
        if (competitorFixes != null && !competitorFixes.isEmpty()) {
            int i = Collections.binarySearch(competitorFixes, new GPSFixDTO(date, null, null, (WindDTO) null, null, null, false),
                    new Comparator<GPSFixDTO>() {
                @Override
                public int compare(GPSFixDTO o1, GPSFixDTO o2) {
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
                    final GPSFixDTO fixBefore = competitorFixes.get(i-1);
                    final GPSFixDTO fixAfter = competitorFixes.get(i);
                    final GPSFixDTO closer;
                    if (date.getTime() - fixBefore.timepoint.getTime() < fixAfter.timepoint.getTime() - date.getTime()) {
                        closer = fixBefore;
                    } else {
                        closer = fixAfter;
                    }
                    Date betweenDate = new Date((fixBefore.timepoint.getTime() + fixAfter.timepoint.getTime()) / 2);
                    PositionDTO betweenPosition = new PositionDTO((fixBefore.position.latDeg + fixAfter.position.latDeg)/2,
                            (fixBefore.position.lngDeg + fixAfter.position.lngDeg)/2);
                    double betweenBearing = new DegreeBearingImpl(fixBefore.speedWithBearing.bearingInDegrees).middle(
                            new DegreeBearingImpl(fixAfter.speedWithBearing.bearingInDegrees)).getDegrees();
                    SpeedWithBearingDTO betweenSpeed = new SpeedWithBearingDTO(
                            (fixBefore.speedWithBearing.speedInKnots + fixAfter.speedWithBearing.speedInKnots)/2,
                            betweenBearing);
                    // TODO move SpeedWithBearing and GPSFix with implementation classes to com.sap.sailing.domain.common and share in GWT bundle
                    result = new GPSFixDTO(betweenDate, betweenPosition, betweenSpeed, closer.degreesBoatToTheWind,
                            closer.tack, closer.legType, fixBefore.extrapolated || fixAfter.extrapolated);
                }
            } else {
                // perfect match
                final GPSFixDTO fixAfter = competitorFixes.get(i);
                result = fixAfter;
            }
        }
        return result;
    }

    public RaceMapSettings getSettings() {
        return settings;
    }

    @Override
    public void addedToSelection(CompetitorDTO competitor) {
        if (settings.isShowOnlySelectedCompetitors()) {
            if (Util.size(competitorSelection.getSelectedCompetitors()) == 1) {
                // first competitors selected; remove all others from map
                Iterator<Map.Entry<CompetitorDTO, BoatOverlay>> i = boatOverlays.entrySet().iterator();
                while (i.hasNext()) {
                    Entry<CompetitorDTO, BoatOverlay> next = i.next();
                    if (!next.getKey().equals(competitor)) {
                        BoatOverlay boatOverlay = next.getValue();
                        boatOverlay.removeFromMap();
                        fixesAndTails.removeTail(next.getKey());
                        i.remove(); // only this way a ConcurrentModificationException while looping can be avoided
                    }
                }
                showCompetitorInfoOnMap(timer.getTime(), competitorSelection.getSelectedCompetitors());
            } else {
                // adding a single competitor; may need to re-load data, so refresh:
                timeChanged(timer.getTime());
            }
        } else {
            // only change highlighting
            BoatOverlay boatCanvas = boatOverlays.get(competitor);
            if (boatCanvas != null) {
                boatCanvas.setSelected(displayHighlighted(competitor));
                boatCanvas.draw();
                showCompetitorInfoOnMap(timer.getTime(), competitorSelection.getSelectedCompetitors());
            } else {
                // seems like an internal error not to find the lowlighted marker; but maybe the
                // competitor was added late to the race;
                // data for newly selected competitor supposedly missing; refresh
                timeChanged(timer.getTime());
            }
        }
        //Trigger auto-zoom if needed
        RaceMapZoomSettings zoomSettings = settings.getZoomSettings();
        if (!zoomSettings.containsZoomType(ZoomTypes.NONE) && zoomSettings.isZoomToSelectedCompetitors()) {
            zoomMapToNewBounds(zoomSettings.getNewBounds(this));
        }
    }
    
    @Override
    public void removedFromSelection(CompetitorDTO competitor) {
        if (isShowAnyHelperLines()) {
            // helper lines depend on which competitor is visible, because the *visible* leader is used for
            // deciding which helper lines to show:
            timeChanged(timer.getTime());
        } else {
            // try a more incremental update otherwise
            if (settings.isShowOnlySelectedCompetitors()) {
                // if selection is now empty, show all competitors
                if (Util.isEmpty(competitorSelection.getSelectedCompetitors())) {
                    timeChanged(timer.getTime());
                } else {
                    // otherwise remove only deselected competitor's boat images and tail
                    BoatOverlay removedBoatOverlay = boatOverlays.remove(competitor);
                    if (removedBoatOverlay != null) {
                        removedBoatOverlay.removeFromMap();
                    }
                    fixesAndTails.removeTail(competitor);
                    showCompetitorInfoOnMap(timer.getTime(), competitorSelection.getSelectedCompetitors());
                }
            } else {
                // "lowlight" currently selected competitor
                BoatOverlay boatCanvas = boatOverlays.get(competitor);
                if (boatCanvas != null) {
                    boatCanvas.setSelected(displayHighlighted(competitor));
                    boatCanvas.draw();
                }
                showCompetitorInfoOnMap(timer.getTime(), competitorSelection.getSelectedCompetitors());
            }
        }
        //Trigger auto-zoom if needed
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
        return stringMessages.map();
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public SettingsDialogComponent<RaceMapSettings> getSettingsDialogComponent() {
        return new RaceMapSettingsDialogComponent(settings, stringMessages);
    }

    @Override
    public void updateSettings(RaceMapSettings newSettings) {
        boolean maneuverTypeSelectionChanged = false;
        boolean requiredRedraw = false;
        for (ManeuverType maneuverType : ManeuverType.values()) {
            if (newSettings.isShowManeuverType(maneuverType) != settings.isShowManeuverType(maneuverType)) {
                maneuverTypeSelectionChanged = true;
                settings.showManeuverType(maneuverType, newSettings.isShowManeuverType(maneuverType));
            }
        }
        if (maneuverTypeSelectionChanged) {
            if (!(timer.getPlayState() == PlayStates.Playing) && lastManeuverResult != null) {
                removeAllManeuverMarkers();
                showManeuvers(lastManeuverResult);
            }
        }
        if (newSettings.isShowDouglasPeuckerPoints() != settings.isShowDouglasPeuckerPoints()) {
            if (!(timer.getPlayState() == PlayStates.Playing) && lastDouglasPeuckerResult != null && newSettings.isShowDouglasPeuckerPoints()) {
                settings.setShowDouglasPeuckerPoints(true);
                removeAllMarkDouglasPeuckerpoints();
                showMarkDouglasPeuckerPoints(lastDouglasPeuckerResult);
            } else if (!newSettings.isShowDouglasPeuckerPoints()) {
                settings.setShowDouglasPeuckerPoints(false);
                removeAllMarkDouglasPeuckerpoints();
            }
        }
        if (newSettings.getTailLengthInMilliseconds() != settings.getTailLengthInMilliseconds()) {
            settings.setTailLengthInMilliseconds(newSettings.getTailLengthInMilliseconds());
            requiredRedraw = true;
        }
        if (newSettings.getBuoyZoneRadiusInMeters() != settings.getBuoyZoneRadiusInMeters()) {
            settings.setBuoyZoneRadiusInMeters(newSettings.getBuoyZoneRadiusInMeters());
            requiredRedraw = true;
        }
        if (newSettings.isShowOnlySelectedCompetitors() != settings.isShowOnlySelectedCompetitors()) {
            settings.setShowOnlySelectedCompetitors(newSettings.isShowOnlySelectedCompetitors());
            requiredRedraw = true;
        }
        if (newSettings.isShowSelectedCompetitorsInfo() != settings.isShowSelectedCompetitorsInfo()) {
            settings.setShowSelectedCompetitorsInfo(newSettings.isShowSelectedCompetitorsInfo());
            requiredRedraw = true;
        }
        if (!newSettings.getZoomSettings().equals(settings.getZoomSettings())) {
            settings.setZoomSettings(newSettings.getZoomSettings());
            if (!settings.getZoomSettings().containsZoomType(ZoomTypes.NONE)) {
                zoomMapToNewBounds(settings.getZoomSettings().getNewBounds(this));
            }
        }
        if (!newSettings.getHelpLinesSettings().equals(settings.getHelpLinesSettings())) {
            settings.setHelpLinesSettings(newSettings.getHelpLinesSettings());
            requiredRedraw = true;
        }
        if (requiredRedraw) {
            redraw();
        }
    }
    
    public static class BoatsBoundsCalculator extends LatLngBoundsCalculatorForSelected {

        @Override
        public LatLngBounds calculateNewBounds(RaceMap forMap) {
            LatLngBounds newBounds = null;
            Iterable<CompetitorDTO> selectedCompetitors = forMap.competitorSelection.getSelectedCompetitors();
            Iterable<CompetitorDTO> competitors = new ArrayList<CompetitorDTO>();
            if (selectedCompetitors == null || !selectedCompetitors.iterator().hasNext()) {
                competitors = forMap.getCompetitorsToShow();
            } else {
                competitors = isZoomOnlyToSelectedCompetitors(forMap) ? selectedCompetitors : forMap.getCompetitorsToShow();
            }
            for (CompetitorDTO competitor : competitors) {
                try {
                    GPSFixDTO competitorFix = forMap.getBoatFix(competitor, forMap.timer.getTime());
                    PositionDTO competitorPosition = competitorFix != null ? competitorFix.position : null;
                    LatLng competitorLatLng = competitorPosition != null ? LatLng.newInstance(competitorPosition.latDeg,
                            competitorPosition.lngDeg) : null;
                    LatLngBounds bounds = competitorLatLng != null ? LatLngBounds.newInstance(competitorLatLng,
                            competitorLatLng) : null;
                    if (bounds != null) {
                        if (newBounds == null) {
                            newBounds = bounds;
                        } else {
                            newBounds.extend(bounds.getNorthEast());
                            newBounds.extend(bounds.getSouthWest());
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
        public LatLngBounds calculateNewBounds(RaceMap racemap) {
            LatLngBounds newBounds = null;
            Iterable<CompetitorDTO> competitors = isZoomOnlyToSelectedCompetitors(racemap) ? racemap.competitorSelection.getSelectedCompetitors() : racemap.getCompetitorsToShow();
            for (CompetitorDTO competitor : competitors) {
                Polyline tail = racemap.fixesAndTails.getTail(competitor);
                LatLngBounds bounds = null;
                // TODO: Find a replacement for missing Polyline function getBounds() from v2
                // see also http://stackoverflow.com/questions/3284808/getting-the-bounds-of-a-polyine-in-google-maps-api-v3; 
                // optionally, consider providing a bounds cache with two sorted sets that organize the LatLng objects for O(1) bounds calculation and logarithmic add, ideally O(1) remove
                if(tail != null && tail.getPath().getLength() >= 2) {
                    bounds = LatLngBounds.newInstance(tail.getPath().get(0), tail.getPath().get(1));
                    for(int i = 2; i < tail.getPath().getLength(); i++) {
                        bounds.extend(tail.getPath().get(i));
                    }
                }
                if (bounds != null) {
                    if (newBounds == null) {
                        newBounds = bounds;
                    } else {
                        newBounds.extend(bounds.getNorthEast());
                        newBounds.extend(bounds.getSouthWest());
                    }
                }
            }
            return newBounds;
        }
        
    }
    
    public static class CourseMarksBoundsCalculator implements LatLngBoundsCalculator {
        @Override
        public LatLngBounds calculateNewBounds(RaceMap forMap) {
            LatLngBounds newBounds = null;
            Iterable<MarkDTO> marksToZoom = forMap.markDTOs.values();
            if (marksToZoom != null) {
                for (MarkDTO markDTO : marksToZoom) {
                    LatLng markLatLng = LatLng.newInstance(markDTO.position.latDeg, markDTO.position.lngDeg);
                    LatLngBounds bounds = LatLngBounds.newInstance(markLatLng, markLatLng);
                    if (newBounds == null) {
                        newBounds = bounds;
                    } else {
                        newBounds.extend(markLatLng);
                    }
                }
            }
            return newBounds;
        }
    }

    public static class WindSensorsBoundsCalculator implements LatLngBoundsCalculator {
        @Override
        public LatLngBounds calculateNewBounds(RaceMap forMap) {
            LatLngBounds newBounds = null;
            Collection<WindSensorOverlay> marksToZoom = forMap.windSensorOverlays.values();
            if (marksToZoom != null) {
                for (WindSensorOverlay windSensorOverlay: marksToZoom) {
                    LatLng windSensorLatLng = windSensorOverlay.getLatLngPosition();
                    if(windSensorLatLng != null) {
                        LatLngBounds bounds = LatLngBounds.newInstance(windSensorLatLng, windSensorLatLng);
                        if (newBounds == null) {
                            newBounds = bounds;
                        } else {
                            newBounds.extend(windSensorLatLng);
                        }
                    }
                }
            }
            return newBounds;
        }
    }

    @Override
    public void initializeData() {
        loadMapsAPIV3();
    }

    @Override
    public boolean isDataInitialized() {
        return isMapInitialized;
    }

    @Override
    public void onResize() {
        if (map != null) {
            map.triggerResize();
            zoomMapToNewBounds(settings.getZoomSettings().getNewBounds(RaceMap.this));
        }
    }

    @Override
    public void competitorsListChanged(Iterable<CompetitorDTO> competitors) {
        timeChanged(timer.getTime());
    }
    
    @Override
    public void filteredCompetitorsListChanged(Iterable<CompetitorDTO> filteredCompetitors) {
        timeChanged(timer.getTime());
    }

    @Override
    public PolylineOptions createTailStyle(CompetitorDTO competitor, boolean isHighlighted) {
        PolylineOptions options = PolylineOptions.newInstance();
        options.setClickable(true);
        options.setGeodesic(true);
        options.setStrokeOpacity(1.0);
        options.setStrokeColor(competitorSelection.getColor(competitor).getAsHtml());
        if (isHighlighted) {
            options.setStrokeWeight(2);
        } else {
            options.setStrokeWeight(1);
        }
        options.setZindex(RaceMapOverlaysZIndexes.BOATTAILS_ZINDEX);
        return options;
    }
    
    @Override
    public Polyline createTail(final CompetitorDTO competitor, List<LatLng> points) {
        PolylineOptions options = createTailStyle(competitor, displayHighlighted(competitor));
        Polyline result = Polyline.newInstance(options);

        MVCArray<LatLng> pointsAsArray = MVCArray.newInstance(points.toArray(new LatLng[0]));
        result.setPath(pointsAsArray);
        
        result.addClickHandler(new ClickMapHandler() {
            @Override
            public void onEvent(ClickMapEvent event) {
                showCompetitorInfoWindow(competitor, event.getMouseEvent().getLatLng());
            }
        });
        result.addMouseOverHandler(new MouseOverMapHandler() {
            @Override
            public void onEvent(MouseOverMapEvent event) {
                map.setTitle(competitor.getSailID() + ", " + competitor.getName());
            }
        });
        result.addMouseOutMoveHandler(new MouseOutMapHandler() {
            @Override
            public void onEvent(MouseOutMapEvent event) {
                map.setTitle("");
            }
        });
        return result;
    }
}
