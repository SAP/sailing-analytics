package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.maps.client.LoadApi;
import com.google.gwt.maps.client.LoadApi.LoadLibrary;
import com.google.gwt.maps.client.MapOptions;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.maps.client.controls.MapTypeStyle;
import com.google.gwt.maps.client.controls.PanControlOptions;
import com.google.gwt.maps.client.controls.ScaleControlOptions;
import com.google.gwt.maps.client.controls.ZoomControlOptions;
import com.google.gwt.maps.client.maptypes.MapTypeStyleFeatureType;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.sap.sailing.domain.common.dto.PositionDTO;
import com.sap.sailing.domain.common.impl.RGBColor;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.RequiresDataInitialization;
import com.sap.sailing.gwt.ui.client.SimulatorServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.TimePanel;
import com.sap.sailing.gwt.ui.client.TimePanelSettings;
import com.sap.sailing.gwt.ui.client.Timer;
import com.sap.sailing.gwt.ui.client.shared.panels.SimpleBusyIndicator;
import com.sap.sailing.gwt.ui.shared.PathDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapAPIKey;
import com.sap.sailing.gwt.ui.shared.racemap.GoogleMapStyleHelper;
import com.sap.sailing.gwt.ui.simulator.util.ColorPalette;
import com.sap.sailing.gwt.ui.simulator.util.ColorPaletteGenerator;
import com.sap.sailing.gwt.ui.simulator.util.SimulatorResources;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternDisplay;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;

/**
 * This class implements simulation visualization using overlays on top of a Google maps widget
 * 
 * @author Christopher Ronnewinkel (D036654)
 *
 */
public class SimulatorMap extends AbsolutePanel implements RequiresDataInitialization, TimeListenerWithStoppingCriteria {
	
    private static SimulatorResources resources = GWT.create(SimulatorResources.class);

    private MapWidget map;
    private boolean dataInitialized;
    private boolean overlaysInitialized;
    private WindFieldGenParamsDTO windParams;

    // the canvas overlays
    private WindStreamletsCanvasOverlay windStreamletsCanvasOverlay;
    private WindFieldCanvasOverlay windFieldCanvasOverlay;
    private WindGridCanvasOverlay windGridCanvasOverlay;
    private WindLineCanvasOverlay windLineCanvasOverlay;

    private RegattaAreaCanvasOverlay regattaAreaCanvasOverlay;
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
    private boolean warningAlreadyShown = false;
    private SimulatorMainPanel parent = null;
    private PathPolyline pathPolyline = null;
    private static Logger LOGGER = Logger.getLogger(SimulatorMap.class.getName());
    private static boolean SHOW_ONLY_PATH_POLYLINE = false;
    private char raceCourseDirection; 

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
            errorReporter.reportError("Failed servlet call to SimulatorService\n" + message.getMessage());
        }

        @Override
        public void onSuccess(SimulatorResultsDTO result) {

            String notificationMessage = result.getNotificationMessage();
            if (notificationMessage != "" && notificationMessage.length() != 0 && warningAlreadyShown == false) {
                errorReporter.reportError(notificationMessage, true);
                warningAlreadyShown = true;
            }

            PathDTO[] paths = result.getPaths();

            LOGGER.info("Number of Paths : " + paths.length);
            long startTime = paths[0].getPoints().get(0).timepoint;
            long maxDurationTime = 0;

            if (mode == SailingSimulatorConstants.ModeMeasured) {
                PositionDTO pos1 = result.getRaceCourse().coursePositions.waypointPositions.get(0);
                PositionDTO pos2 = result.getRaceCourse().coursePositions.waypointPositions.get(1);
                
                raceCourseCanvasOverlay.setStartEndPoint(LatLng.newInstance(pos1.latDeg, pos1.lngDeg), LatLng.newInstance(pos2.latDeg, pos2.lngDeg));
            }

            raceCourseCanvasOverlay.draw();
            removeOverlays();
            // pathCanvasOverlays.clear();
            replayPathCanvasOverlays.clear();
            colorPalette.reset();

            PathDTO currentPath = null;
            //String color = null;
            String pathName = null;
            int noOfPaths = paths.length;

            for (int index = 0; index < noOfPaths; ++index) {

                currentPath = paths[index];
                pathName = currentPath.getName();
                //color = colorPalette.getColor(noOfPaths - 1 - index);

                if (pathName.equals("Polyline")) {
                    pathPolyline = createPathPolyline(currentPath);
                } else if (pathName.equals("GPS Poly")) {
                    continue;
                }
                else {

                    /* TODO Revisit for now creating a WindFieldDTO from the path */
                    WindFieldDTO pathWindDTO = new WindFieldDTO();
                    pathWindDTO.setMatrix(currentPath.getPoints());

                    ReplayPathCanvasOverlay replayPathCanvasOverlay = new ReplayPathCanvasOverlay(map, SimulatorMapOverlaysZIndexes.PATH_ZINDEX, 
                            pathName.split("#")[1], timer, windParams);
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
                    if (tmpDurationTime > maxDurationTime) {
                        maxDurationTime = tmpDurationTime;
                    }
                }
            }
            
        	legendCanvasOverlay.setCurrent(result.getWindField().curSpeed,result.getWindField().curBearing);

            if (timePanel != null) {
                timePanel.setMinMax(new Date(startTime), new Date(startTime + maxDurationTime), true);
                timePanel.resetTimeSlider();
            }

            /**
             * Now we always get the wind field
             */
            WindFieldDTO windFieldDTO = result.getWindField();
            LOGGER.info("Number of windDTO : " + windFieldDTO.getMatrix().size());

            if (windParams.isShowGrid()) {
                windGridCanvasOverlay.addToMap();
            }
            if (windParams.isShowLines()) {
                windLineCanvasOverlay.addToMap();
            }
            if (windParams.isShowArrows()) {
                windFieldCanvasOverlay.addToMap();
            }
            if (windParams.isShowStreamlets()) {
                windStreamletsCanvasOverlay.addToMap();
            }

            refreshWindFieldOverlay(windFieldDTO);

            timeListeners.clear();

            if (windParams.isShowArrows()) {
                timeListeners.add(windFieldCanvasOverlay);
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
                if (windStreamletsCanvasOverlay != null) {
                    windStreamletsCanvasOverlay.setVisible(false);
                }
                if (windGridCanvasOverlay != null) {
                    windGridCanvasOverlay.setVisible(false);
                }
                if (windLineCanvasOverlay != null) {
                    windLineCanvasOverlay.setVisible(false);
                }
            }

            legendCanvasOverlay.addToMap();
            legendCanvasOverlay.setVisible(true);
            legendCanvasOverlay.draw();

            busyIndicator.setBusy(false);
        }

    }

    public SimulatorMap(SimulatorServiceAsync simulatorSvc, StringMessages stringMessages, ErrorReporter errorReporter, int xRes, int yRes, Timer timer,
            WindFieldGenParamsDTO windParams, SimpleBusyIndicator busyIndicator, char mode,
            SimulatorMainPanel parent) {
        this.simulatorService = simulatorSvc;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.xRes = xRes;
        this.yRes = yRes;
        this.timer = timer;
        this.timePanel = null;
        timer.addTimeListener(this);
        this.windParams = windParams;
        this.busyIndicator = busyIndicator;
        this.mode = mode;
        this.colorPalette = new ColorPaletteGenerator();
        this.dataInitialized = false;
        this.overlaysInitialized = false;
        this.windFieldCanvasOverlay = null;
        this.windStreamletsCanvasOverlay = null;
        this.windGridCanvasOverlay = null;
        this.windLineCanvasOverlay = null;
        this.replayPathCanvasOverlays = null;
        this.raceCourseCanvasOverlay = null;
        this.timeListeners = new LinkedList<TimeListenerWithStoppingCriteria>();
        this.initializeData();
        this.parent = parent;    
    }

    public SimulatorMap(SimulatorServiceAsync simulatorSvc, StringMessages stringMessages, ErrorReporter errorReporter, int xRes, int yRes, Timer timer,
            TimePanel<TimePanelSettings> timePanel, WindFieldGenParamsDTO windParams, SimpleBusyIndicator busyIndicator, char mode, SimulatorMainPanel parent) {
        this.simulatorService = simulatorSvc;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.xRes = xRes;
        this.yRes = yRes;
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
        this.windStreamletsCanvasOverlay = null;
        this.windGridCanvasOverlay = null;
        this.windLineCanvasOverlay = null;
        this.replayPathCanvasOverlays = null;
        this.raceCourseCanvasOverlay = null;
        this.timeListeners = new LinkedList<TimeListenerWithStoppingCriteria>();
        this.initializeData();
        this.parent = parent;
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
              mapOptions.setMapTypeControl(false);
              mapOptions.setPanControl(true);
              mapOptions.setScaleControl(true);
              mapOptions.setRotateControl(true);
              mapOptions.setStreetViewControl(false);
              
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

              map.setTitle(stringMessages.simulator() + " " + stringMessages.map());
              mapOptions.setDisableDoubleClickZoom(true);
              
              if (mode == SailingSimulatorConstants.ModeFreestyle) {
                  map.setZoom(14);                    
              } else if (mode == SailingSimulatorConstants.ModeEvent) {
                  map.setZoom(12);
              }

              add(map, 0, 0);
              map.setSize("100%", "100%");
              
              initializeOverlays();
              
              dataInitialized = true;

              if (mode == SailingSimulatorConstants.ModeFreestyle) {
                    LatLng kiel = LatLng.newInstance(54.43450, 10.19559167); // regatta area for TV on Kieler Woche
                    // LatLng trave = LatLng.newInstance(54.007063, 10.838356); // in front of Timmendorfer Strand
                    map.setCenter(kiel);
              }
          }
        };

        LoadApi.go(onLoad, loadLibraries, sensor, "key="+GoogleMapAPIKey.V2_APIKey);  
    }

    private void initializeOverlays() {
        if (mode == SailingSimulatorConstants.ModeEvent) {
            if (regattaAreaCanvasOverlay == null) {
                regattaAreaCanvasOverlay = new RegattaAreaCanvasOverlay(map, SimulatorMapOverlaysZIndexes.REGATTA_AREA_ZINDEX, getMainPanel().getEvent(), this);
                regattaAreaCanvasOverlay.addToMap();
                
                int offsetLeft = 50;
                int offsetTop = 25;
                windRoseCanvasOverlay = new ImageCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WIND_ROSE_ZINDEX, resources.windRoseBackground());
                windRoseCanvasOverlay.setOffset(offsetLeft, offsetTop);
                windRoseCanvasOverlay.addToMap();
                windNeedleCanvasOverlay = new ImageCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WIND_ROSE_ZINDEX, resources.windRoseNeedle());
                windNeedleCanvasOverlay.setOffset(offsetLeft, offsetTop);                
                windNeedleCanvasOverlay.setBearing(180.0);
                windNeedleCanvasOverlay.addToMap();
            }
        }
    	
        raceCourseCanvasOverlay = new RaceCourseCanvasOverlay(map, SimulatorMapOverlaysZIndexes.RACE_COURSE_ZINDEX, mode);

        if (mode == SailingSimulatorConstants.ModeEvent) {
            regattaAreaCanvasOverlay.setRaceCourseCanvas(raceCourseCanvasOverlay);
        }

        if (windParams.isShowArrows()) {
            windFieldCanvasOverlay = new WindFieldCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDFIELD_ZINDEX, timer, windParams);
        }
        if (windParams.isShowStreamlets()) {
            windStreamletsCanvasOverlay = new WindStreamletsCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDSTREAMLETS_ZINDEX, timer, xRes);
        }
        if (windParams.isShowGrid()) {
            windGridCanvasOverlay = new WindGridCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDGRID_ZINDEX, timer, xRes, yRes);
        }
        if (windParams.isShowLines()) {
            windLineCanvasOverlay = new WindLineCanvasOverlay(map, SimulatorMapOverlaysZIndexes.WINDLINE_ZINDEX, timer);
        }
        replayPathCanvasOverlays = new ArrayList<PathCanvasOverlay>();
        legendCanvasOverlay = new PathLegendCanvasOverlay(map, SimulatorMapOverlaysZIndexes.PATHLEGEND_ZINDEX, mode);

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
                if (windStreamletsCanvasOverlay != null) {
                    windStreamletsCanvasOverlay.onResize();
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

    private void generateWindField(final WindPatternDisplay windPatternDisplay, final boolean removeOverlays) {
        LOGGER.info("In generateWindField");
        if (windPatternDisplay == null) {
            errorReporter.reportError("Please select a valid wind pattern.");
            return;
        }
        PositionDTO startPointDTO = new PositionDTO(raceCourseCanvasOverlay.getStartPoint().getLatitude(),
                raceCourseCanvasOverlay.getStartPoint().getLongitude());
        PositionDTO endPointDTO = new PositionDTO(raceCourseCanvasOverlay.getEndPoint().getLatitude(),
                raceCourseCanvasOverlay.getEndPoint().getLongitude());
        LOGGER.info("StartPoint:" + startPointDTO);
        windParams.setNorthWest(startPointDTO);
        windParams.setSouthEast(endPointDTO);
        windParams.setxRes(xRes);
        windParams.setyRes(yRes);
        busyIndicator.setBusy(true);
        timer.setTime(windParams.getStartTime().getTime());

        simulatorService.getWindField(windParams, windPatternDisplay, new AsyncCallback<WindFieldDTO>() {
            @Override
            public void onFailure(Throwable message) {
                errorReporter.reportError("Failed servlet call to SimulatorService\n" + message.getMessage());
            }

            @Override
            public void onSuccess(WindFieldDTO wl) {
                if (removeOverlays) {
                    removeOverlays();
                }
                LOGGER.info("Number of windDTO : " + wl.getMatrix().size());
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
                if (windParams.isShowStreamlets()) {
                    windStreamletsCanvasOverlay.addToMap();
                }
                refreshWindFieldOverlay(wl);
                timeListeners.clear();
                if (windParams.isShowArrows()) {
                    timeListeners.add(windFieldCanvasOverlay);
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
                timePanel.setMinMax(windParams.getStartTime(), windParams.getEndTime(), true);
                timePanel.resetTimeSlider();
                //timer.setTime(windParams.getStartTime().getTime());

                busyIndicator.setBusy(false);
            }
        });
    }

    private void refreshWindFieldOverlay(WindFieldDTO windFieldDTO) {
        if (this.windFieldCanvasOverlay != null) {
            this.windFieldCanvasOverlay.setWindField(windFieldDTO);
        }
        if (this.windStreamletsCanvasOverlay != null) {
            this.windStreamletsCanvasOverlay.setWindField(windFieldDTO);
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
        if (this.windParams.isShowStreamlets()) {
            this.windStreamletsCanvasOverlay.draw();
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
            errorReporter.reportError("Please select a valid wind pattern.");
            return;
        }

        if (mode != SailingSimulatorConstants.ModeMeasured) {
            PositionDTO startPointDTO = new PositionDTO(raceCourseCanvasOverlay.getStartPoint().getLatitude(), 
                    raceCourseCanvasOverlay.getStartPoint().getLongitude());
            windParams.setNorthWest(startPointDTO);

            PositionDTO endPointDTO = new PositionDTO(raceCourseCanvasOverlay.getEndPoint().getLatitude(), 
                    raceCourseCanvasOverlay.getEndPoint().getLongitude());
            windParams.setSouthEast(endPointDTO);
        }

        windParams.setxRes(xRes);
        windParams.setyRes(yRes);

        busyIndicator.setBusy(true);
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
                if (windStreamletsCanvasOverlay != null) {
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
                if (windStreamletsCanvasOverlay != null) {
                    windStreamletsCanvasOverlay.setVisible(true);
                    windStreamletsCanvasOverlay.draw();
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
            Window.alert("No course set, please initialize the course with Start-End input");
        }
    }

    @Override
    public void initializeData() {
        loadMapsAPIV3();
    }

    @Override
    public boolean isDataInitialized() {
        return dataInitialized;
    }

    @Override
    public void timeChanged(Date date) {
        if (shallStop()) {
            LOGGER.info("Stopping the timer");
            timer.stop();
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

        return PathPolyline.createPathPolyline(pathDTO.getPoints(), errorReporter, simulatorService, map, this, parent, selection);
    }

    public void addLegendOverlayForPathPolyline(long totalTimeMilliseconds) {
        PathCanvasOverlay pathCanvasOverlay = new PathCanvasOverlay(map, SimulatorMapOverlaysZIndexes.PATH_ZINDEX,
                PathPolyline.END_USER_NAME, totalTimeMilliseconds, PathPolyline.DEFAULT_COLOR);
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
    
}
