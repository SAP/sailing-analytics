package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.maps.client.MapOptions;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.maps.client.controls.MapTypeStyle;
import com.google.gwt.maps.client.controls.PanControlOptions;
import com.google.gwt.maps.client.controls.ScaleControlOptions;
import com.google.gwt.maps.client.controls.ZoomControlOptions;
import com.google.gwt.maps.client.events.bounds.BoundsChangeMapEvent;
import com.google.gwt.maps.client.events.bounds.BoundsChangeMapHandler;
import com.google.gwt.maps.client.events.click.ClickMapEvent;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.idle.IdleMapEvent;
import com.google.gwt.maps.client.events.idle.IdleMapHandler;
import com.google.gwt.maps.client.maptypes.MapTypeStyleElementType;
import com.google.gwt.maps.client.maptypes.MapTypeStyleFeatureType;
import com.google.gwt.maps.client.maptypes.MapTypeStyler;
import com.google.gwt.maps.client.overlays.Circle;
import com.google.gwt.maps.client.overlays.CircleOptions;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.gwt.ui.client.RequiresDataInitialization;
import com.sap.sailing.gwt.ui.client.SimulatorServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.TimePanel;
import com.sap.sailing.gwt.ui.client.TimePanelSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.PathDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapStyleHelper;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapsLoader;
import com.sap.sailing.gwt.ui.shared.racemap.PathNameFormatter;
import com.sap.sailing.gwt.ui.simulator.util.ColorPalette;
import com.sap.sailing.gwt.ui.simulator.util.ColorPaletteGenerator;
import com.sap.sailing.gwt.ui.simulator.util.SimulatorResources;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternDisplay;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.RGBColor;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.player.Timer;

/**
 * This class implements simulation visualization using overlays on top of a Google maps widget
 * 
 * @author Christopher Ronnewinkel (D036654)
 *
 */
public class SimulatorMap extends AbsolutePanel implements RequiresDataInitialization, TimeListenerWithStoppingCriteria {
	
    private static SimulatorResources resources = GWT.create(SimulatorResources.class);

    private MapWidget map;
    private boolean mapPan = false;
    private boolean dataInitialized;
    private boolean overlaysInitialized;
    private WindFieldGenParamsDTO windParams;

    // the canvas overlays
    private WindLineGuidesCanvasOverlay windLineGuidesCanvasOverlay;
    private WindFieldCanvasOverlay windFieldCanvasOverlay;
    private WindGridCanvasOverlay windGridCanvasOverlay;
    private WindLineCanvasOverlay windLineCanvasOverlay;

    private RegattaAreaCanvasOverlay regattaAreaCanvasOverlay;
    private WindStreamletsCanvasOverlay windStreamletsCanvasOverlay;
    private ImageCanvasOverlay windRoseCanvasOverlay;
    private ImageCanvasOverlay windNeedleCanvasOverlay;
    private List<PathCanvasOverlay> replayPathCanvasOverlays;

    private RaceCourseCanvasOverlay raceCourseCanvasOverlay;
    private PathLegendCanvasOverlay legendCanvasOverlay;
    
    private List<TimeListenerWithStoppingCriteria> timeListeners;
    private SimulatorServiceAsync simulatorService;
    private StringMessages stringMessages;
    private ErrorReporter errorReporter;
    private Timer timer;
    private TimePanel<TimePanelSettings> timePanel;
    private SimpleBusyIndicator busyIndicator;
    private char mode;
    private ColorPalette colorPalette;
    private int xRes;
    private int yRes;
    private int border;
    private StreamletParameters streamletPars;
    private boolean warningAlreadyShown = false;
    private SimulatorMainPanel parent = null;
    private PathPolyline pathPolyline = null;
    private static Logger LOGGER = Logger.getLogger(SimulatorMap.class.getName());
    private static boolean SHOW_ONLY_PATH_POLYLINE = false;
    private char raceCourseDirection;
    private final CoordinateSystem coordinateSystem;
    private final PathNameFormatter pathNameFormatter;

    public enum ViewName {
        SUMMARY, REPLAY, WINDDISPLAY
    }

    private class ResultManager implements AsyncCallback<SimulatorResultsDTO> {
        private boolean summaryView;

        public ResultManager(boolean summaryView) {
            this.summaryView = summaryView;
        }

        @Override
        public void onFailure(Throwable message) {
            errorReporter.reportError(stringMessages.errorServletCall(message.getMessage()));
        }

        @Override
        public void onSuccess(SimulatorResultsDTO result) {
            String notificationMessage = result.getNotificationMessage();
            if (Util.hasLength(notificationMessage) && warningAlreadyShown == false) {
                errorReporter.reportError(notificationMessage, true);
                warningAlreadyShown = true;
            }
            PathDTO[] paths = result.getPaths();
            LOGGER.info("Number of Paths : " + paths.length);
            final TimePoint startTime = paths[0].getPoints().get(0).timepoint;
            long maxDurationMillis = 0;
            if (mode == SailingSimulatorConstants.ModeMeasured) {
                Position pos1 = result.getRaceCourse().coursePositions.waypointPositions.get(0);
                Position pos2 = result.getRaceCourse().coursePositions.waypointPositions.get(1);
                raceCourseCanvasOverlay.setStartEndPoint(coordinateSystem.toLatLng(pos1), coordinateSystem.toLatLng(pos2));
            }
            raceCourseCanvasOverlay.draw();
            removeOverlays();
            // pathCanvasOverlays.clear();
            replayPathCanvasOverlays.clear();
            colorPalette.reset();
            PathDTO currentPath = null;
            //String color = null;
            String pathName = null;
            boolean algorithmTimedOut = false;
            int noOfPaths = paths.length;
            for (int index = 0; index < noOfPaths; ++index) {
                currentPath = paths[index];
                pathName = currentPath.getName();
                algorithmTimedOut = currentPath.getAlgorithmTimedOut();
                //color = colorPalette.getColor(noOfPaths - 1 - index);
                if (pathName.equals("Polyline")) {
                    pathPolyline = createPathPolyline(currentPath);
                } else if (pathName.equals("GPS Poly")) {
                    continue;
                } else {
                    /* TODO Revisit for now creating a WindFieldDTO from the path */
                    WindFieldDTO pathWindDTO = null;
                    if (!currentPath.getMixedLeg()) {
                        pathWindDTO = new WindFieldDTO();
                        pathWindDTO.setMatrix(currentPath.getPoints());
                    }
                    ReplayPathCanvasOverlay replayPathCanvasOverlay = new ReplayPathCanvasOverlay(map, SimulatorMapOverlaysZIndexes.PATH_ZINDEX, 
                            pathNameFormatter.format(currentPath), timer, windParams, algorithmTimedOut, currentPath.getMixedLeg(), coordinateSystem);
                    replayPathCanvasOverlays.add(replayPathCanvasOverlay);
                    replayPathCanvasOverlay.setPathColor(colorPalette.getColor(Integer.parseInt(pathName.split("#")[0])-1));
                    if (summaryView) {
                        replayPathCanvasOverlay.displayWindAlongPath = true;
                        timer.removeTimeListener(replayPathCanvasOverlay);
                        replayPathCanvasOverlay.setTimer(null);
                    }
                    if (SHOW_ONLY_PATH_POLYLINE == false) {
                        replayPathCanvasOverlay.addToMap();
                    }
                    replayPathCanvasOverlay.setWindField(pathWindDTO);
                    replayPathCanvasOverlay.setRaceCourse(raceCourseCanvasOverlay.getStartPoint(), raceCourseCanvasOverlay.getEndPoint());
                    /*if (index == 0) {
                    	legendCanvasOverlay.setCurrent(result.getWindField().curSpeed,result.getWindField().curBearing);
                    } else {
                    	legendCanvasOverlay.setCurrent(-1.0,0.0);
                    }*/
                    if (SHOW_ONLY_PATH_POLYLINE == false) {
                        replayPathCanvasOverlay.draw();
                    }
                    legendCanvasOverlay.setPathOverlays(replayPathCanvasOverlays);
                    long tmpDurationTime = currentPath.getPathTime();
                    if ((!currentPath.getMixedLeg())&&(!currentPath.getAlgorithmTimedOut())&&(tmpDurationTime > maxDurationMillis)) {
                        maxDurationMillis = tmpDurationTime;
                    }
                }
            }
            legendCanvasOverlay.setCurrent(result.getWindField().curSpeed,result.getWindField().curBearing);
            if (timePanel != null) {
            	final TimePoint endDate = startTime.plus(maxDurationMillis);
                timePanel.setMinMax(startTime.asDate(), endDate.asDate(), true);
                timePanel.resetTimeSlider();
                timePanel.timeChanged(windParams.getStartTime(), null);
                if (windParams.isShowStreamlets()) {
                    windStreamletsCanvasOverlay.setEndDate(endDate.asDate());
                }
            }
            /**
             * Now we always get the wind field
             */
            WindFieldDTO windFieldDTO = result.getWindField();
            //LOGGER.info("Number of windDTO : " + windFieldDTO.getMatrix().size());
            if (windParams.isShowGrid()) {
                windGridCanvasOverlay.addToMap();
            }
            if (windParams.isShowLines()) {
                windLineCanvasOverlay.addToMap();
            }
            if (windParams.isShowArrows()) {
                windFieldCanvasOverlay.addToMap();
            }
            if (windParams.isShowLineGuides()) {
                windLineGuidesCanvasOverlay.addToMap();
            }
            if (windParams.isShowStreamlets()) {
                windStreamletsCanvasOverlay.addToMap();
            }
            refreshWindFieldOverlay(windFieldDTO);
            timeListeners.clear();
            if (windParams.isShowArrows()) {
                timeListeners.add(windFieldCanvasOverlay);
            }
            if (windParams.isShowLineGuides()) {
                timeListeners.add(windLineGuidesCanvasOverlay);
            }
            if (windParams.isShowStreamlets()) {
                timeListeners.add(windStreamletsCanvasOverlay);
            }
            if (windParams.isShowGrid()) {
                timeListeners.add(windGridCanvasOverlay);
            }
            if (windParams.isShowLines()) {
                timeListeners.add(windLineCanvasOverlay);
            }
            for (int i = 0; i < replayPathCanvasOverlays.size(); ++i) {
                timeListeners.add(replayPathCanvasOverlays.get(i));
            }
            if (summaryView) {
                if (windFieldCanvasOverlay != null) {
                    windFieldCanvasOverlay.setVisible(false);
                }
                if (windLineGuidesCanvasOverlay != null) {
                    windLineGuidesCanvasOverlay.setVisible(false);
                }
                if (windGridCanvasOverlay != null) {
                    windGridCanvasOverlay.setVisible(false);
                }
                if (windLineCanvasOverlay != null) {
                    windLineCanvasOverlay.setVisible(false);
                }
                if (windStreamletsCanvasOverlay != null) {
                    windStreamletsCanvasOverlay.setVisible(false);
                }
            } else {
                if (windStreamletsCanvasOverlay != null) {
                    windStreamletsCanvasOverlay.setVisible(true);
                }
            }
            legendCanvasOverlay.addToMap();
            legendCanvasOverlay.setVisible(true);
            legendCanvasOverlay.draw();

            busyIndicator.setBusy(false);
        }
    }

    public SimulatorMap(SimulatorServiceAsync simulatorSvc, StringMessages stringMessages, ErrorReporter errorReporter,
            int xRes, int yRes, int border, StreamletParameters streamletPars, Timer timer,
            TimePanel<TimePanelSettings> timePanel, WindFieldGenParamsDTO windParams,
            SimpleBusyIndicator busyIndicator, char mode, SimulatorMainPanel parent, boolean showMapControls, CoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
        this.pathNameFormatter = new PathNameFormatter(stringMessages);
        this.simulatorService = simulatorSvc;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.xRes = xRes;
        this.yRes = yRes;
        this.border = border;
        this.streamletPars = streamletPars;
        this.timer = timer;
        this.timePanel = timePanel;
        timer.addTimeListener(this);
        this.windParams = windParams;
        this.busyIndicator = busyIndicator;
        this.mode = mode;
        this.colorPalette = new ColorPaletteGenerator();
        this.dataInitialized = false;
        this.overlaysInitialized = false;
        this.windFieldCanvasOverlay = null;
        this.windLineGuidesCanvasOverlay = null;
        this.windGridCanvasOverlay = null;
        this.windLineCanvasOverlay = null;
        this.replayPathCanvasOverlays = null;
        this.raceCourseCanvasOverlay = null;
        this.timeListeners = new LinkedList<TimeListenerWithStoppingCriteria>();
        this.initializeData(showMapControls, /* showHeaderPanel */ true);
        this.parent = parent;
    }

    private void loadMapsAPIV3() {
        Runnable onLoad = new Runnable() {
          @Override
          public void run() {
              MapOptions mapOptions = MapOptions.newInstance();
              mapOptions.setScrollWheel(true);
              mapOptions.setMapTypeControl(false);
              mapOptions.setPanControl(true);
              mapOptions.setScaleControl(true);
              mapOptions.setRotateControl(true);
              mapOptions.setStreetViewControl(false);
              MapTypeStyle[] mapTypeStyles;
              if (windParams.isShowStreamlets()) {
            	  mapTypeStyles = new MapTypeStyle[11];
            	  // hide all transit lines including ferry lines
            	  mapTypeStyles[0] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.TRANSIT);
            	  // hide points of interest
            	  mapTypeStyles[1] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.POI);
            	  // simplify road display
            	  mapTypeStyles[2] = GoogleMapStyleHelper.createSimplifiedStyle(MapTypeStyleFeatureType.ROAD);
            	  // set water color
            	  mapTypeStyles[3] = GoogleMapStyleHelper.createColorStyle(MapTypeStyleFeatureType.WATER, new RGBColor(0, 136, 255), -50, -50);
            	  mapTypeStyles[4] = GoogleMapStyleHelper.createColorStyle(MapTypeStyleFeatureType.LANDSCAPE, new RGBColor(255, 255, 255), -100, -70);
            	  mapTypeStyles[5] = GoogleMapStyleHelper.createColorStyle(MapTypeStyleFeatureType.POI, new RGBColor(255, 255, 255), -100, -70);
            	  mapTypeStyles[6] = GoogleMapStyleHelper.createElementStyleOnlyLightness(MapTypeStyleFeatureType.ROAD, MapTypeStyleElementType.ALL, -40);
            	  MapTypeStyle mapStyle = MapTypeStyle.newInstance();
            	  mapStyle.setFeatureType(MapTypeStyleFeatureType.ADMINISTRATIVE);
            	  mapStyle.setElementType(MapTypeStyleElementType.LABELS__TEXT__FILL);
            	  MapTypeStyler[] typeStylers = new MapTypeStyler[1];
            	  typeStylers[0] = MapTypeStyler.newInvertLightnessStyler(true);
            	  mapStyle.setStylers(typeStylers);
            	  mapTypeStyles[7] = mapStyle;
            	  mapStyle = MapTypeStyle.newInstance();
            	  mapStyle.setFeatureType(MapTypeStyleFeatureType.ADMINISTRATIVE);
            	  mapStyle.setElementType(MapTypeStyleElementType.LABELS__TEXT__STROKE);
            	  typeStylers = new MapTypeStyler[1];
            	  typeStylers[0] = MapTypeStyler.newInvertLightnessStyler(true);
            	  mapStyle.setStylers(typeStylers);
            	  mapTypeStyles[8] = mapStyle;
            	  mapStyle = MapTypeStyle.newInstance();
            	  mapStyle.setFeatureType(MapTypeStyleFeatureType.ADMINISTRATIVE);
            	  mapStyle.setElementType(MapTypeStyleElementType.GEOMETRY__FILL);
            	  typeStylers = new MapTypeStyler[3];
              	  typeStylers[0] = MapTypeStyler.newHueStyler("#ffffff");
              	  typeStylers[1] = MapTypeStyler.newLightnessStyler(-70);
              	  typeStylers[2] = MapTypeStyler.newSaturationStyler(-100);              	  
            	  mapStyle.setStylers(typeStylers);
            	  mapTypeStyles[9] = mapStyle;
            	  mapStyle = MapTypeStyle.newInstance();
            	  mapStyle.setFeatureType(MapTypeStyleFeatureType.ADMINISTRATIVE);
            	  mapStyle.setElementType(MapTypeStyleElementType.GEOMETRY__STROKE);
            	  typeStylers = new MapTypeStyler[1];
              	  typeStylers[0] = MapTypeStyler.newLightnessStyler(-70);
            	  mapStyle.setStylers(typeStylers);
            	  mapTypeStyles[10] = mapStyle;
    		} else {
            	  mapTypeStyles = new MapTypeStyle[4];
            	  // hide all transit lines including ferry lines
            	  mapTypeStyles[0] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.TRANSIT);
            	  // hide points of interest
            	  mapTypeStyles[1] = GoogleMapStyleHelper.createHiddenStyle(MapTypeStyleFeatureType.POI);
            	  // simplify road display
            	  mapTypeStyles[2] = GoogleMapStyleHelper.createSimplifiedStyle(MapTypeStyleFeatureType.ROAD);
            	  // set water color
            	  mapTypeStyles[3] = GoogleMapStyleHelper.createColorStyle(MapTypeStyleFeatureType.WATER, new RGBColor(0, 136, 255), -35, -34);
              }
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
              map.setTitle(stringMessages.simulator() + " " + stringMessages.map());
              mapOptions.setDisableDoubleClickZoom(true);
              if (mode == SailingSimulatorConstants.ModeFreestyle) {
                  map.setZoom(14);                    
              } else if (mode == SailingSimulatorConstants.ModeEvent) {
                  map.setZoom(12);
              }
              add(map, 0, 0);
              map.setSize("100%", "100%");
              if (windParams.isShowStreamlets()) {
            	  map.addBoundsChangeHandler(new BoundsChangeMapHandler() {
            		  @Override
            		  public void onEvent(BoundsChangeMapEvent event) {
            			  // improve browser performance by deferred scheduling of redraws
            			  Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            				  public void execute() {
            					  if (windStreamletsCanvasOverlay.getSwarm() != null) {
            						  windStreamletsCanvasOverlay.getSwarm().onBoundsChanged(true, 5);
            					  }
            				  }
            			  });
            		  }
            	  });
              }
              map.addIdleHandler(new IdleMapHandler() {
            	  @Override
            	  public void onEvent(IdleMapEvent event) {
            		  // TODO Auto-generated method stub
            		  if (mapPan) {
            			  //System.out.println("Map Idle Event Handler.");
            			  getWindRoseCanvasOverlay().addToMap();
            			  getWindNeedleCanvasOverlay().addToMap();					
            			  mapPan = false;
            		  }
            	  }
              });
              initializeOverlays();
              dataInitialized = true;
              if (mode == SailingSimulatorConstants.ModeFreestyle) {
                    LatLng kiel = LatLng.newInstance(54.43450, 10.19559167); // regatta area for TV on Kieler Woche
                    // LatLng trave = LatLng.newInstance(54.007063, 10.838356); // in front of Timmendorfer Strand
                    map.setCenter(kiel);
              }
          }
        };
        simulatorService.getGoogleMapsLoaderAuthenticationParams(new AsyncCallback<String>() {
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

    private void initializeOverlays() {
        if (mode == SailingSimulatorConstants.ModeEvent) {
            if (regattaAreaCanvasOverlay == null) {
                regattaAreaCanvasOverlay = new RegattaAreaCanvasOverlay(map, SimulatorMapOverlaysZIndexes.REGATTA_AREA_ZINDEX, getMainPanel().getEvent(), this, coordinateSystem);
                regattaAreaCanvasOverlay.addToMap();
                
                int offsetLeft = 50;
                int offsetTop = 25;
                windRoseCanvasOverlay = new ImageCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WIND_ROSE_ZINDEX, resources.windRoseBackground(), coordinateSystem);
                windRoseCanvasOverlay.setOffset(offsetLeft, offsetTop);
                windRoseCanvasOverlay.addToMap();
                windNeedleCanvasOverlay = new ImageCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WIND_ROSE_ZINDEX, resources.windRoseNeedle(), coordinateSystem);
                windNeedleCanvasOverlay.setOffset(offsetLeft, offsetTop);                
                windNeedleCanvasOverlay.setBearing(180.0);
                windNeedleCanvasOverlay.addToMap();
            }
        }
        raceCourseCanvasOverlay = new RaceCourseCanvasOverlay(map, SimulatorMapOverlaysZIndexes.RACE_COURSE_ZINDEX, mode, coordinateSystem);
        if (mode == SailingSimulatorConstants.ModeEvent) {
            regattaAreaCanvasOverlay.setRaceCourseCanvas(raceCourseCanvasOverlay);
        }
        if (windParams.isShowArrows()) {
            windFieldCanvasOverlay = new WindFieldCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDFIELD_ZINDEX, timer, windParams, coordinateSystem);
        }
        if (windParams.isShowLineGuides()) {
            windLineGuidesCanvasOverlay = new WindLineGuidesCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDSTREAMLETS_ZINDEX, timer, xRes, coordinateSystem);
        }
        if (windParams.isShowStreamlets()) {
        	windStreamletsCanvasOverlay = new WindStreamletsCanvasOverlay(this, SimulatorMapOverlaysZIndexes.WINDFIELD_ZINDEX, timer, streamletPars, windParams, coordinateSystem);
        }
        if (windParams.isShowGrid()) {
            windGridCanvasOverlay = new WindGridCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDGRID_ZINDEX, timer, xRes, yRes, coordinateSystem);
        }
        if (windParams.isShowLines()) {
            windLineCanvasOverlay = new WindLineCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDLINE_ZINDEX, timer, coordinateSystem);
        }
        replayPathCanvasOverlays = new ArrayList<PathCanvasOverlay>();
        legendCanvasOverlay = new PathLegendCanvasOverlay(map, SimulatorMapOverlaysZIndexes.PATHLEGEND_ZINDEX, mode, coordinateSystem, stringMessages);
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                regattaAreaCanvasOverlay.onResize();
                raceCourseCanvasOverlay.onResize();
                for (PathCanvasOverlay pathCanvas : replayPathCanvasOverlays) {
                    pathCanvas.onResize();
                }
                if (windFieldCanvasOverlay != null) {
                    windFieldCanvasOverlay.onResize();
                }
                if (windLineGuidesCanvasOverlay != null) {
                    windLineGuidesCanvasOverlay.onResize();
                }
                if (windGridCanvasOverlay != null) {
                    windGridCanvasOverlay.onResize();
                }
                if (windLineCanvasOverlay != null) {
                    windLineCanvasOverlay.onResize();
                }
                legendCanvasOverlay.onResize();
                timePanel.resetTimeSlider();
            }
        });
        overlaysInitialized = true;
    }

    public void generateWindField(final WindPatternDisplay windPatternDisplay, final boolean removeOverlays) {
        LOGGER.info("In generateWindField");
        if (windPatternDisplay == null) {
            errorReporter.reportError(stringMessages.pleaseSelectAValidWindPattern());
            return;
        }
        
        if ((windStreamletsCanvasOverlay != null)&&(windStreamletsCanvasOverlay.isVisible())) {
        	windStreamletsCanvasOverlay.setVisible(false);
        }

        Position startPointDTO = new DegreePosition(raceCourseCanvasOverlay.getStartPoint().getLatitude(),
                raceCourseCanvasOverlay.getStartPoint().getLongitude());
        Position endPointDTO = new DegreePosition(raceCourseCanvasOverlay.getEndPoint().getLatitude(),
                raceCourseCanvasOverlay.getEndPoint().getLongitude());
        LOGGER.info("StartPoint:" + startPointDTO);
        windParams.setRaceCourseStart(startPointDTO);
        windParams.setRaceCourseEnd(endPointDTO);
        windParams.setxRes(xRes);
        windParams.setyRes(yRes);
        windParams.setBorder(border);
        busyIndicator.setBusy(true);
        timer.pause();
        timer.setTime(windParams.getStartTime().getTime());
        simulatorService.getWindField(windParams, windPatternDisplay, new AsyncCallback<WindFieldDTO>() {
            @Override
            public void onFailure(Throwable message) {
                errorReporter.reportError(stringMessages.errorServletCall(message.getMessage()));
            }

            @Override
            public void onSuccess(WindFieldDTO wl) {
                if (removeOverlays) {
                    removeOverlays();
                }
                //LOGGER.info("Number of windDTO : " + wl.getMatrix().size());
                // Window.alert("Number of windDTO : " + wl.getMatrix().size());
                if (windParams.isShowGrid()) {
                    windGridCanvasOverlay.addToMap();
                }
                if (windParams.isShowLines()) {
                    windLineCanvasOverlay.addToMap();
                }
                if (windParams.isShowArrows()) {
                    windFieldCanvasOverlay.addToMap();
                }
                if (windParams.isShowLineGuides()) {
                    windLineGuidesCanvasOverlay.addToMap();
                }
                if (windParams.isShowStreamlets()) {
                    windStreamletsCanvasOverlay.addToMap();
                }
                refreshWindFieldOverlay(wl);
                timeListeners.clear();
                if (windParams.isShowArrows()) {
                    timeListeners.add(windFieldCanvasOverlay);
                }
                if (windParams.isShowLineGuides()) {
                    timeListeners.add(windLineGuidesCanvasOverlay);
                }
                if (windParams.isShowStreamlets()) {
                    timeListeners.add(windStreamletsCanvasOverlay);
                    windStreamletsCanvasOverlay.setVisible(true);
                }
                if (windParams.isShowGrid()) {
                    timeListeners.add(windGridCanvasOverlay);
                }
                if (windParams.isShowLines()) {
                    timeListeners.add(windLineCanvasOverlay);
                }
                timePanel.setMinMax(windParams.getStartTime(), windParams.getEndTime(), true);
                timePanel.resetTimeSlider();
                timePanel.timeChanged(windParams.getStartTime(), null);
                if (windParams.isShowStreamlets()) {
                	windStreamletsCanvasOverlay.setEndDate(windParams.getEndTime());
                }

                busyIndicator.setBusy(false);
            }
        });
    }

    private void refreshWindFieldOverlay(WindFieldDTO windFieldDTO) {
        if (this.windStreamletsCanvasOverlay != null) {
            this.windStreamletsCanvasOverlay.setWindField(windFieldDTO);
        }
        if (this.windFieldCanvasOverlay != null) {
            this.windFieldCanvasOverlay.setWindField(windFieldDTO);
        }
        if (this.windLineGuidesCanvasOverlay != null) {
            this.windLineGuidesCanvasOverlay.setWindField(windFieldDTO);
        }
        if (this.windGridCanvasOverlay != null) {
            this.windGridCanvasOverlay.setWindField(windFieldDTO);
        }

        if (this.windLineCanvasOverlay != null) {
            this.windLineCanvasOverlay.setWindLinesDTO(windFieldDTO.getWindLinesDTO());
            if (this.windGridCanvasOverlay != null) {
                this.windLineCanvasOverlay.setGridCorners(this.windGridCanvasOverlay.getGridCorners());
            }
        }

        if (this.windParams.isShowArrows()) {
            this.windFieldCanvasOverlay.draw();
        }
        if (this.windParams.isShowLineGuides()) {
            this.windLineGuidesCanvasOverlay.draw();
        }
        if (this.windParams.isShowGrid()) {
            this.windGridCanvasOverlay.draw();
        }
        if (this.windParams.isShowLines()) {
            this.windLineCanvasOverlay.draw();
        }
    }

    private void generatePath(final WindPatternDisplay windPatternDisplay, boolean summaryView, final SimulatorUISelectionDTO selection) {
        LOGGER.info("In generatePath");
        if (windPatternDisplay == null) {
            errorReporter.reportError(stringMessages.pleaseSelectAValidWindPattern());
            return;
        }
        if ((windStreamletsCanvasOverlay != null)&&(windStreamletsCanvasOverlay.isVisible())) {
        	windStreamletsCanvasOverlay.setVisible(false);
        }
        if (mode != SailingSimulatorConstants.ModeMeasured) {
            Position startPointDTO = new DegreePosition(raceCourseCanvasOverlay.getStartPoint().getLatitude(), 
                    raceCourseCanvasOverlay.getStartPoint().getLongitude());
            windParams.setRaceCourseStart(startPointDTO);

            Position endPointDTO = new DegreePosition(raceCourseCanvasOverlay.getEndPoint().getLatitude(), 
                    raceCourseCanvasOverlay.getEndPoint().getLongitude());
            windParams.setRaceCourseEnd(endPointDTO);
        }
        windParams.setxRes(xRes);
        windParams.setyRes(yRes);
        windParams.setBorder(border);
        busyIndicator.setBusy(true);
        timer.pause();
        timer.setTime(windParams.getStartTime().getTime());
        simulatorService.getSimulatorResults(mode, raceCourseDirection, windParams, windPatternDisplay, true, selection, new ResultManager(summaryView));
    }

    private boolean isCourseSet() {
        return this.raceCourseCanvasOverlay.isCourseSet();
    }

    public void reset() {
        if (!overlaysInitialized) {
            initializeOverlays();
        } else {
            removeOverlays();
        }
//        map.setDoubleClickZoom(false);
        raceCourseCanvasOverlay.setSelected(true);
        // raceCourseCanvasOverlay.setVisible(true);
        raceCourseCanvasOverlay.reset();
        raceCourseCanvasOverlay.draw();
    }

    public void removeOverlays() {
        if (overlaysInitialized) {
            int num = 0; // tracking for debugging only
            if (windFieldCanvasOverlay != null) {
                windFieldCanvasOverlay.removeFromMap();
                num++;
            }
            if (windLineGuidesCanvasOverlay != null) {
                windLineGuidesCanvasOverlay.removeFromMap();
                num++;
            }
            if (windStreamletsCanvasOverlay != null) {
                windStreamletsCanvasOverlay.removeFromMap();
                num++;
            }
            if (windGridCanvasOverlay != null) {
                windGridCanvasOverlay.removeFromMap();
                num++;
            }
            if (windLineCanvasOverlay != null) {
                windLineCanvasOverlay.removeFromMap();
                num++;
            }

            for (int i = 0; i < replayPathCanvasOverlays.size(); ++i) {
                replayPathCanvasOverlays.get(i).removeFromMap();
                num++;
            }
            legendCanvasOverlay.removeFromMap();
            LOGGER.info("Removed " + num + " overlays");
        }
    }

    private void refreshSummaryView(WindPatternDisplay windPatternDisplay, SimulatorUISelectionDTO selection, boolean force) {
        // removeOverlays();
        if (force) {
            generatePath(windPatternDisplay, true, selection);
        } else {
            if (replayPathCanvasOverlays != null && !replayPathCanvasOverlays.isEmpty()) {
            	LOGGER.info("Soft refresh");
                for (PathCanvasOverlay pathCanvasOverlay : replayPathCanvasOverlays) {
                    pathCanvasOverlay.displayWindAlongPath = true;
                    timer.removeTimeListener(pathCanvasOverlay);
                    timeListeners.remove(pathCanvasOverlay);
                    pathCanvasOverlay.setTimer(null);
                    pathCanvasOverlay.setVisible(true);
                    pathCanvasOverlay.draw();
                }
                legendCanvasOverlay.setVisible(true);
                legendCanvasOverlay.draw();
                if (windFieldCanvasOverlay != null) {
                    windFieldCanvasOverlay.setVisible(false);
                }
                if (windStreamletsCanvasOverlay != null) {
                    windStreamletsCanvasOverlay.setVisible(false);
                }
                if (windLineGuidesCanvasOverlay != null) {
                    windLineGuidesCanvasOverlay.setVisible(false);
                }
                if (windGridCanvasOverlay != null) {
                    windGridCanvasOverlay.setVisible(false);
                }
                if (windLineCanvasOverlay != null) {
                    windLineCanvasOverlay.setVisible(false);
                }
            } else {
                generatePath(windPatternDisplay, true, selection);
            }
        }
    }

    private void refreshReplayView(WindPatternDisplay windPatternDisplay, SimulatorUISelectionDTO selection, boolean force) {
        // removeOverlays();
        if (force) {
            generatePath(windPatternDisplay, false, selection);
        } else {

            if (replayPathCanvasOverlays != null && !replayPathCanvasOverlays.isEmpty()) {
            	LOGGER.info("Soft refresh");
                timePanel.resetTimeSlider();
                for (PathCanvasOverlay pathCanvasOverlay : replayPathCanvasOverlays) {
                    pathCanvasOverlay.displayWindAlongPath = false;
                    pathCanvasOverlay.setTimer(timer);
                    timer.addTimeListener(pathCanvasOverlay);
                    if (!timeListeners.contains(pathCanvasOverlay)) {
                    	timeListeners.add(pathCanvasOverlay);
                    }
                    pathCanvasOverlay.setVisible(true);
                    pathCanvasOverlay.draw();
                }
                legendCanvasOverlay.setVisible(true);
                legendCanvasOverlay.draw();
                if (windFieldCanvasOverlay != null) {
                    windFieldCanvasOverlay.setVisible(true);
                }
                if (windLineGuidesCanvasOverlay != null) {
                    windLineGuidesCanvasOverlay.setVisible(true);
                }
                if ((windStreamletsCanvasOverlay != null)&&(!windStreamletsCanvasOverlay.isVisible())) {
                    windStreamletsCanvasOverlay.setVisible(true);
                }
                if (windGridCanvasOverlay != null) {
                    windGridCanvasOverlay.setVisible(true);
                }
                if (windLineCanvasOverlay != null) {
                    windLineCanvasOverlay.setVisible(true);
                }
            } else {
                generatePath(windPatternDisplay, false, selection);
            }
        }
    }
    
    private void refreshWindDisplayView(WindPatternDisplay windPatternDisplay, boolean force) {

        if (force) {
            // removeOverlays();
            parent.setDefaultTimeSettings();
            generateWindField(windPatternDisplay, true);
            // timeListeners.clear();
            // timeListeners.add(windFieldCanvasOverlay);
        } else {

            if (replayPathCanvasOverlays != null && !replayPathCanvasOverlays.isEmpty()) {
            	LOGGER.info("Soft refresh");
                timePanel.resetTimeSlider();
                for (PathCanvasOverlay r : replayPathCanvasOverlays) {
                    r.setVisible(false);
                    timer.removeTimeListener(r);
                    timeListeners.remove(r);
                }
                legendCanvasOverlay.setVisible(false);

                if (windFieldCanvasOverlay != null) {
                    windFieldCanvasOverlay.setVisible(true);
                    windFieldCanvasOverlay.draw();
                }
                if (windLineGuidesCanvasOverlay != null) {
                    windLineGuidesCanvasOverlay.setVisible(true);
                    windLineGuidesCanvasOverlay.draw();
                }
                if ((windStreamletsCanvasOverlay != null)&&(!windStreamletsCanvasOverlay.isVisible())) {
                    windStreamletsCanvasOverlay.setVisible(true);
                    //windStreamletsCanvasOverlay.draw();
                }
                if (windGridCanvasOverlay != null) {
                    windGridCanvasOverlay.setVisible(true);
                    windGridCanvasOverlay.draw();
                }
                if (windLineCanvasOverlay != null) {
                    windLineCanvasOverlay.setVisible(true);
                    windLineCanvasOverlay.draw();
                }

            } else {
                parent.setDefaultTimeSettings();
                generateWindField(windPatternDisplay, true);
            }
        }
    }

    public void clearOverlays() {
        if (replayPathCanvasOverlays != null) {
            removeOverlays();
            timeListeners.clear();
            replayPathCanvasOverlays.clear();
        }
    }
    
    public void refreshView(ViewName name, WindPatternDisplay windPatternDisplay, SimulatorUISelectionDTO selection, boolean force) {

        if (!overlaysInitialized) {
            initializeOverlays();
        }
        if ((isCourseSet()) || (mode == SailingSimulatorConstants.ModeMeasured)) {
//            map.setDoubleClickZoom(true);
            raceCourseCanvasOverlay.setSelected(false);
            windParams.setKeepState(!force);
            if (force) {
            	clearOverlays();
            }
            switch (name) {
            case SUMMARY:
                if ((windStreamletsCanvasOverlay != null)&&(windStreamletsCanvasOverlay.isVisible())) {
                	windStreamletsCanvasOverlay.setVisible(false);
                }
                refreshSummaryView(windPatternDisplay, selection, force);
                break;
            case REPLAY:
                refreshReplayView(windPatternDisplay, selection, force);
                break;
            case WINDDISPLAY:
                refreshWindDisplayView(windPatternDisplay, force);
                break;
            default:
                break;
            }

            if (mode == SailingSimulatorConstants.ModeMeasured && pathPolyline != null) {
                pathPolyline.setBoatClassID(selection.boatClassIndex);
            }

        } else {
            Notification.notify("No course set, please initialize the course with Start-End input", NotificationType.ERROR);
        }
    }

    @Override
    public void initializeData(boolean showMapControls, boolean showHeaderPanel) {
        loadMapsAPIV3();
    }

    @Override
    public boolean isDataInitialized() {
        return dataInitialized;
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if (shallStop()) {
            LOGGER.info("Stopping the timer");
            timer.pause();
        }
    }

    @Override
    public boolean shallStop() {
        boolean shallStop = false;
        for (TimeListenerWithStoppingCriteria t : timeListeners) {
            shallStop |= t.shallStop();
        }
        return shallStop;
    }

    private PathPolyline createPathPolyline(final PathDTO pathDTO) {
        SimulatorUISelectionDTO selection = new SimulatorUISelectionDTO(parent.getSelectedBoatClassIndex(), parent.getSelectedRaceIndex(),
                parent.getSelectedCompetitorIndex(), parent.getSelectedLegIndex());
        return PathPolyline.createPathPolyline(pathDTO.getPoints(), errorReporter, simulatorService, map, this, parent, selection, coordinateSystem);
    }

    public void addLegendOverlayForPathPolyline(long totalTimeMilliseconds) {
        PathCanvasOverlay pathCanvasOverlay = new PathCanvasOverlay(map, SimulatorMapOverlaysZIndexes.PATH_ZINDEX,
                PathPolyline.END_USER_NAME, totalTimeMilliseconds, PathPolyline.DEFAULT_COLOR, coordinateSystem);
        legendCanvasOverlay.addPathOverlay(pathCanvasOverlay);
    }

    public void redrawLegendCanvasOverlay() {
        legendCanvasOverlay.setVisible(true);
        if (SHOW_ONLY_PATH_POLYLINE == false) {
            legendCanvasOverlay.draw();
        }
    }

    private Polyline polyline = null;

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public void removePolyline() {
        if (pathPolyline != null) {
            polyline.setMap(null);
        }
    }

    public MapWidget getMap() {
    	return map;
    }
    
    public SimulatorMainPanel getMainPanel() {
    	return parent;
    }
    
    public void setRaceCourseDirection(char raceCourseDirection) {
    	clearOverlays();
    	this.raceCourseDirection = raceCourseDirection;
    	raceCourseCanvasOverlay.raceCourseDirection = raceCourseDirection;
    	regattaAreaCanvasOverlay.updateRaceCourse(0, 0);
        raceCourseCanvasOverlay.draw();
        windNeedleCanvasOverlay.draw();
    }

    public WindFieldGenParamsDTO getWindParams() {
        return windParams;
    }
    
    public RegattaAreaCanvasOverlay getRegattaAreaCanvasOverlay() {
    	return regattaAreaCanvasOverlay;
    }

    public ImageCanvasOverlay getWindRoseCanvasOverlay() {
    	return windRoseCanvasOverlay;
    }

    public ImageCanvasOverlay getWindNeedleCanvasOverlay() {
    	return windNeedleCanvasOverlay;
    }
    
    public RaceCourseCanvasOverlay getRaceCourseCanvasOverlay() {
    	return raceCourseCanvasOverlay;
    }
   
    public void drawCircleFromRadius(int regIdx, CourseAreaDescriptor courseArea) {
   	 
        CircleOptions circleOptions = CircleOptions.newInstance();
        circleOptions.setStrokeColor("white");
        circleOptions.setStrokeWeight(1);
        circleOptions.setStrokeOpacity(0.0);
        circleOptions.setFillColor("green");
        circleOptions.setFillOpacity(0.0);
        circleOptions.setCenter(courseArea.getCenterPos());
        Circle circle = Circle.newInstance(circleOptions);
        circle.setRadius(courseArea.getRadiusInMeters());
        final int regIdxFinal = regIdx;
        circle.addClickHandler(new ClickMapHandler() {
            public void onEvent(ClickMapEvent e) {
                //System.out.println("Click: "+currentCourseArea.getName());
                CourseAreaDescriptor newRegArea = regattaAreaCanvasOverlay.getVenue().getCourseAreas().get(regIdxFinal);

                if (newRegArea != regattaAreaCanvasOverlay.getCurrentCourseArea()) {
                	regattaAreaCanvasOverlay.setCurrentCourseArea(newRegArea);
                    clearOverlays();
                    getWindRoseCanvasOverlay().removeFromMap();
                    getWindNeedleCanvasOverlay().removeFromMap();
                    regattaAreaCanvasOverlay.updateRaceCourse(0, 0);
                    raceCourseCanvasOverlay.draw();
                }
                map.panTo(regattaAreaCanvasOverlay.getCurrentCourseArea().getCenterPos());
                mapPan = true;
                if (map.getZoom() < streamletPars.detailZoom) {
                    map.setZoom(streamletPars.detailZoom);
                }
            }
        });
        circle.setMap(getMap());           
    }
}
