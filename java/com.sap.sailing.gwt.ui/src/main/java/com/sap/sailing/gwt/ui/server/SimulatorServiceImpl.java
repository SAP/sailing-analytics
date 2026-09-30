package com.sap.sailing.gwt.ui.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.PathType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.gwt.ui.client.SimulatorService;
import com.sap.sailing.gwt.ui.shared.BoatClassDTOsAndNotificationMessage;
import com.sap.sailing.gwt.ui.shared.ConfigurationException;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.PathDTO;
import com.sap.sailing.gwt.ui.shared.PolarDiagramDTO;
import com.sap.sailing.gwt.ui.shared.PolarDiagramDTOAndNotificationMessage;
import com.sap.sailing.gwt.ui.shared.RaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.Request1TurnerDTO;
import com.sap.sailing.gwt.ui.shared.RequestTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.Response1TurnerDTO;
import com.sap.sailing.gwt.ui.shared.ResponseTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.SimulatedPathsEvenTimedResultDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO.WindData;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.WindLatticeDTO;
import com.sap.sailing.gwt.ui.shared.WindLatticeGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.WindLinesDTO;
import com.sap.sailing.gwt.ui.shared.WindPatternDTO;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPattern;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternDisplay;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternDisplayManager;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternNotFoundException;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternSetting;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.SimulationService;
import com.sap.sailing.server.simulation.SimulationServiceFactory;
import com.sap.sailing.simulator.BoatClassProperties;
import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.Simulator;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.impl.ConfigurationManager;
import com.sap.sailing.simulator.impl.CurvedGrid;
import com.sap.sailing.simulator.impl.PathGenerator1Turner;
import com.sap.sailing.simulator.impl.PathImpl;
import com.sap.sailing.simulator.impl.PolarDiagramCSV;
import com.sap.sailing.simulator.impl.ReadingConfigurationFileStatus;
import com.sap.sailing.simulator.impl.SimulationParametersImpl;
import com.sap.sailing.simulator.impl.SimulatorImpl;
import com.sap.sailing.simulator.impl.TimedPositionImpl;
import com.sap.sailing.simulator.impl.TimedPositionWithSpeedImpl;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sailing.simulator.windfield.WindFieldGeneratorFactory;
import com.sap.sailing.simulator.windfield.impl.WindFieldGeneratorMeasured;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.NauticalMileDistance;
import com.sap.sse.gwt.server.DelegatingProxiedRemoteServiceServlet;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sap.sse.util.ThreadPoolUtil;

public class SimulatorServiceImpl extends DelegatingProxiedRemoteServiceServlet implements SimulatorService {
    private static final long serialVersionUID = 4445427185387524086L;

    private static final Logger logger = Logger.getLogger(SimulatorServiceImpl.class.getName());
    private static final Logger LOGGER = Logger.getLogger("com.sap.sailing");
    private static final WindFieldGeneratorFactory wfGenFactory = WindFieldGeneratorFactory.INSTANCE;
    private static final WindPatternDisplayManager wpDisplayManager = WindPatternDisplayManager.INSTANCE;
    private static final String POLYLINE_PATH_NAME = "Polyline";

    // private static final double TOTAL_TIME_SCALE_FACTOR = 0.9;
    private static final int DEFAULT_STEP_MAX = 800;
    private static final long DEFAULT_TIMESTEP = 6666;
    private final SimulationService simulationService;

    private double stepSizeMeters = 0.0;
    private WindControlParameters controlParameters = new WindControlParameters(0, 0);
    private SpeedWithBearing averageWind = null;

    private final PolarDiagramCache polarDiagramCache;
    
    private final ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;
    
    /**
     * The Simulator service references boat classes and their polar data by numeric indices. In order for this to work
     * reliably, also in the face of possibly changing orders and numbers of boat classes as returned by
     * {@link PolarDataService#getAllBoatClassesWithPolarSheetsAvailable()} and an initial set of polar diagrams
     * as obtained from the {@link ConfigurationManager#getBoatClassesInfo()} method, a stable indexing mechanism
     * is required. This cache implements such a mechanism by first installing all polar diagrams from
     * {@link ConfigurationManager#getBoatClassesInfo()}, and then dynamically adding boat classes from
     * {@link PolarDataService#getAllBoatClassesWithPolarSheetsAvailable()}
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private static class PolarDiagramCache {
        /**
         * Result of {@link ConfigurationManager#getErrorMessage()} in case there was an error reading the pre-defined
         * polar diagram data; {@code null} otherwise.
         */
        private final String notificationMessage;
        
        private final List<BoatClassDTO> boatClasses;
        
        private final ConcurrentMap<String, Integer> boatClassesByNameForQuickLookup;
        
        private final ConcurrentMap<String, PolarDiagram> predefinedPolarsByBoatClassName;

        private final SimulationService simulationService;
        
        public PolarDiagramCache(SimulationService simulationService) {
            this.simulationService = simulationService;
            boatClasses = new ArrayList<>();
            boatClassesByNameForQuickLookup = new ConcurrentHashMap<>();
            predefinedPolarsByBoatClassName = new ConcurrentHashMap<>();
            String messageToUse = null;
            ConfigurationManager config = ConfigurationManager.INSTANCE;
            if (config.getStatus() == ReadingConfigurationFileStatus.IO_ERROR) {
                messageToUse = config.getErrorMessage();
            } else if (config.getStatus() == ReadingConfigurationFileStatus.ERROR_FINDING_CONFIG_FILE
                    || config.getStatus() == ReadingConfigurationFileStatus.ERROR_READING_ENV_VAR_VALUE) {
                messageToUse = config.getErrorMessage();
            }
            for (BoatClassProperties tuple : ConfigurationManager.INSTANCE.getBoatClassesInfo()) {
                final BoatClassDTO boatClass = new BoatClassDTO(tuple.getName(), tuple.getLength(), null);
                addBoatClass(boatClass);
                try {
                    predefinedPolarsByBoatClassName.put(boatClass.getName(), new PolarDiagramCSV(tuple.getPolar()));
                } catch (IOException exception) {
                    messageToUse = "An IO error occured when parsing the CSV file! The original error message is " + exception.getMessage();
                }
            }
            notificationMessage = messageToUse;
        }

        private synchronized void addBoatClass(final BoatClassDTO boatClass) {
            if (!boatClassesByNameForQuickLookup.containsKey(boatClass.getName())) {
                final int index = boatClasses.size();
                boatClasses.add(boatClass);
                boatClassesByNameForQuickLookup.put(boatClass.getName(), index);
            }
        }
        
        String getNotificationMessage() {
            return notificationMessage;
        }

        BoatClassDTO getBoatClass(int boatClassIndex) {
            return boatClassIndex < 0 || boatClassIndex >= boatClasses.size() ? null : boatClasses.get(boatClassIndex);
        }
        
        PolarDiagram getPolarDiagram(BoatClassDTO boatClass) {
            final PolarDiagram predefinedPolar = predefinedPolarsByBoatClassName.get(boatClass.getName());
            final PolarDiagram result;
            if (predefinedPolar != null) {
                result = predefinedPolar;
            } else {
                result = simulationService.getPolarDiagram(simulationService.getBoatClass(boatClass.getName()));
            }
            return result;
        }

        public BoatClassDTO[] getBoatClasses() {
            updateBoatClassesFromPolarDataService();
            return boatClasses.toArray(new BoatClassDTO[boatClasses.size()]);
        }

        private void updateBoatClassesFromPolarDataService() {
            for (final BoatClass boatClassWithPolarData : simulationService.getBoatClassesWithPolarData()) {
                addBoatClass(new BoatClassDTO(boatClassWithPolarData.getName(), boatClassWithPolarData.getHullLength(), boatClassWithPolarData.getHullBeam()));
            }
        }
    }

    public SimulatorServiceImpl() throws InterruptedException {
        final ScheduledExecutorService simulatorExecutor = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor();
        racingEventServiceTracker = ServiceTrackerFactory.createAndOpen(Activator.getDefault(), RacingEventService.class);
        // TODO: initialize smart-future-cache for simulation-results and add to simulation-service
        simulationService = SimulationServiceFactory.INSTANCE.getService(simulatorExecutor, racingEventServiceTracker.waitForService(0));
        polarDiagramCache = new PolarDiagramCache(simulationService);
    }
    
    @Override
    public Position[] getRaceLocations() {
        checkSimulatorReadPermissionOnCurrentServer();
        Position lakeGarda = new DegreePosition(45.57055337226086, 10.693345069885254);
        Position lakeGeneva = new DegreePosition(46.23376539670794, 6.168651580810547);
        Position kiel = new DegreePosition(54.3232927, 10.122765200000003);
        Position travemuende = new DegreePosition(53.978276, 10.880156);
        return new Position[] { kiel, lakeGeneva, lakeGarda, travemuende };
    }

    private void checkSimulatorReadPermissionOnCurrentServer() {
        SecurityUtils.getSubject().checkPermission(SecuredDomainType.SIMULATOR.getStringPermissionForTypeRelativeIdentifier(DefaultActions.READ, new TypeRelativeObjectIdentifier(ServerInfo.getName())));
    }

    @Override
    public WindLatticeDTO getWindLatice(WindLatticeGenParamsDTO params) {
        checkSimulatorReadPermissionOnCurrentServer();
        Bearing north = new DegreeBearingImpl(0);
        Bearing east = new DegreeBearingImpl(90);
        Bearing south = new DegreeBearingImpl(180);
        Bearing west = new DegreeBearingImpl(270);
        double xSize = params.getxSize();
        double ySize = params.getySize();
        int gridsizeX = params.getGridsizeX();
        int gridsizeY = params.getGridsizeY();
        Position center = params.getCenter();
        WindLatticeDTO wl = new WindLatticeDTO();
        Position[][] matrix = new Position[gridsizeY][gridsizeX];
        Distance deastwest = new NauticalMileDistance((gridsizeX - 1.) / (2 * gridsizeX) * xSize);
        Distance dnorthsouth = new NauticalMileDistance((gridsizeY - 1.) / (2 * gridsizeY) * ySize);
        Position start = center.translateGreatCircle(south, dnorthsouth).translateGreatCircle(west, deastwest);
        deastwest = new NauticalMileDistance(xSize / gridsizeX);
        dnorthsouth = new NauticalMileDistance(ySize / gridsizeY);
        Position rowStart = null, crt = null;
        for (int i = 0; i < gridsizeY; i++) {
            if (i == 0) {
                rowStart = start;
            } else {
                rowStart = rowStart.translateGreatCircle(north, dnorthsouth);
            }
            for (int j = 0; j < gridsizeX; j++) {
                if (j == 0) {
                    crt = rowStart;
                } else {
                    crt = crt.translateGreatCircle(east, deastwest);
                    if ((i == 3) && (j == 5)) {
                        crt = crt.translateGreatCircle(north,
                                new NauticalMileDistance(ySize / gridsizeY * Math.random()));
                        crt = crt.translateGreatCircle(east,
                                new NauticalMileDistance(xSize / gridsizeX * Math.random()));
                        crt = crt.translateGreatCircle(south,
                                new NauticalMileDistance(ySize / gridsizeY * Math.random()));
                        crt = crt.translateGreatCircle(west,
                                new NauticalMileDistance(xSize / gridsizeX * Math.random()));
                    }
                }
                Position pdto = new DegreePosition(crt.getLatDeg(), crt.getLngDeg());
                matrix[i][j] = pdto;
            }
        }
        wl.setMatrix(matrix);
        return wl;
    }

    @Override
    public WindFieldDTO getWindField(WindFieldGenParamsDTO params, WindPatternDisplay pattern)
            throws WindPatternNotFoundException {
        checkSimulatorReadPermissionOnCurrentServer();
        LOGGER.info("Entering getWindField");
        Position start = params.getRaceCourseStart();
        Position end = params.getRaceCourseEnd();
        List<Position> course = new ArrayList<Position>();
        course.add(start);
        course.add(end);
        Grid bd = new CurvedGrid(start, end);
        controlParameters.resetBlastRandomStream = params.isKeepState();
        retrieveWindControlParameters(pattern);
        LOGGER.info("Boundary south direction " + bd.getSouth());
        controlParameters.baseWindBearing += bd.getSouth().getDegrees();
        WindFieldGenerator wf = wfGenFactory.createWindFieldGenerator(pattern.getWindPatternName(), bd,
                controlParameters);
        if (wf == null) {
            throw new WindPatternNotFoundException("Please select a valid wind pattern.");
        }
        Position[][] grid = bd.generatePositions(params.getxRes(), params.getyRes(), params.getBorderY(),
                params.getBorderX());
        wf.setPositionGrid(grid);
        TimePoint startTime = new MillisecondsTimePoint(params.getStartTime().getTime());
        Duration timeStep = params.getTimeStep();
        TimePoint endTime = new MillisecondsTimePoint(params.getEndTime().getTime());
        wf.generate(startTime, null, timeStep);
        if (params.getMode() != SailingSimulatorConstants.ModeMeasured) {
            Position[] gridAreaGps = new Position[2];
            gridAreaGps = course.toArray(gridAreaGps);
            wf.setGridAreaGps(gridAreaGps);
        }
        WindFieldDTO wfDTO = createWindFieldDTO(wf, startTime, endTime, timeStep, params);
        LOGGER.info("Exiting getWindField");
        return wfDTO;
    }

    @Override
    public List<WindPatternDTO> getWindPatterns(char mode) {
        checkSimulatorReadPermissionOnCurrentServer();
        return wpDisplayManager.getWindPatterns(mode);
    }

    @Override
    public WindPatternDisplay getWindPatternDisplay(WindPatternDTO pattern) {
        checkSimulatorReadPermissionOnCurrentServer();
        WindPatternDisplay display = wpDisplayManager.getDisplay(WindPattern.valueOf(pattern.getName()));
        try {
            return display;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Problem getting wind pattern display", e);
        }
        return null;
    }

    @Override
    public SimulatorResultsDTO getSimulatorResults(char mode, char rcDirection, WindFieldGenParamsDTO params,
            WindPatternDisplay pattern, boolean withWindField, SimulatorUISelectionDTO selection)
            throws WindPatternNotFoundException, ConfigurationException {
        checkSimulatorReadPermissionOnCurrentServer();
        WindFieldGenerator wf = null;
        List<Position> course = null;
        TimePoint startTime = new MillisecondsTimePoint(params.getStartTime().getTime());
        Duration timeStep = params.getTimeStep();
        this.controlParameters.resetBlastRandomStream = params.isKeepState();
        this.retrieveWindControlParameters(pattern);
        if (rcDirection == SailingSimulatorConstants.LegTypeDownwind) {
            this.controlParameters.baseWindBearing += 180.0;
        }
        wf = wfGenFactory.createWindFieldGenerator(pattern.getWindPatternName(), null, this.controlParameters);
        if (wf == null) {
            throw new WindPatternNotFoundException("Please select a valid wind pattern.");
        }
        if (mode != SailingSimulatorConstants.ModeMeasured) {
            Position start = params.getRaceCourseStart();
            Position end = params.getRaceCourseEnd();
            course = new ArrayList<Position>();
            course.add(start);
            course.add(end);
            Position[] gridAreaGps = new Position[2];
            gridAreaGps = course.toArray(gridAreaGps);
            wf.setGridAreaGps(gridAreaGps);
        }
        int[] gridRes = new int[4];
        gridRes[0] = params.getxRes();
        gridRes[1] = params.getyRes();
        gridRes[2] = params.getBorderY();
        gridRes[3] = params.getBorderX();
        wf.setGridResolution(gridRes);
        wf.generate(startTime, null, timeStep);
        Duration longestPathTime = Duration.NULL;
        SimulatedPathsEvenTimedResultDTO simulatedPaths = this.getSimulatedPathsEvenTimed(course, wf, mode, selection,
                params.showOmniscient, params.showOpportunist);
        PathDTO[] pathDTOs = simulatedPaths.pathDTOs;
        RaceMapDataDTO rcDTO = simulatedPaths.raceMapDataDTO;
        for (PathDTO path : pathDTOs) {
            if (path.getName().equals(POLYLINE_PATH_NAME)) {
                continue;
            }
            List<SimulatorWindDTO> points = path.getPoints();
            final Duration pathTime = points.get(0).timepoint.until(points.get(points.size() - 1).timepoint);
            if (pathTime.compareTo(longestPathTime) > 0) {
                longestPathTime = pathTime;
            }
        }
        TimePoint endTime = startTime.plus(longestPathTime);
        WindFieldDTO windFieldDTO = null;
        if (pattern != null) {
            windFieldDTO = this.createWindFieldDTO(wf, startTime, endTime, timeStep, params); // params.isShowStreamlets2(),
                                                                                              // params.isShowLines(),
                                                                                              // params.getSeedLines());
        }
        return new SimulatorResultsDTO(0, 0, null, timeStep, Duration.NULL, rcDTO, pathDTOs, windFieldDTO,
                simulatedPaths.notificationMessage);
    }

    @Override
    public BoatClassDTOsAndNotificationMessage getBoatClasses() throws ConfigurationException {
        checkSimulatorReadPermissionOnCurrentServer();
        final BoatClassDTOsAndNotificationMessage result = new BoatClassDTOsAndNotificationMessage();
        result.setBoatClassDTOs(polarDiagramCache.getBoatClasses());
        result.setNotificationMessage(polarDiagramCache.getNotificationMessage());
        return result;
    }

    @Override
    public PolarDiagramDTOAndNotificationMessage getPolarDiagram(Double bearingStep, int boatClassIndex)
            throws ConfigurationException {
        checkSimulatorReadPermissionOnCurrentServer();
        Util.Pair<PolarDiagram, String> polarDiagramAndNotificationMessage = this.getPolarDiagram(boatClassIndex);
        final PolarDiagramDTOAndNotificationMessage result;
        if (polarDiagramAndNotificationMessage != null && polarDiagramAndNotificationMessage.getA() != null) {
            NavigableMap<Speed, NavigableMap<Bearing, Speed>> navMap = polarDiagramAndNotificationMessage.getA()
                    .polarDiagramPlot(bearingStep);
            Set<Speed> validSpeeds = navMap.keySet();
            validSpeeds.remove(Speed.NULL);
            Number[][] series = new Number[validSpeeds.size()][];
            int i = 0;
            for (Speed s : validSpeeds) {
                Collection<Speed> boatSpeeds = navMap.get(s).values();
                series[i] = new Number[boatSpeeds.size()];
                int j = 0;
                for (Speed boatSpeed : boatSpeeds) {
                    series[i][j++] = Double.valueOf(boatSpeed.getKnots());
                }
                i++;
            }
            PolarDiagramDTO dto = new PolarDiagramDTO();
            dto.setNumberSeries(series);
            result = new PolarDiagramDTOAndNotificationMessage();
            result.setPolarDiagramDTO(dto);
            result.setNotificationMessage(polarDiagramAndNotificationMessage.getB());
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public ResponseTotalTimeDTO getTotalTime(RequestTotalTimeDTO requestData) throws ConfigurationException {
        checkSimulatorReadPermissionOnCurrentServer();
        this.averageWind = requestData.useRealAverageWindSpeed ? SimulatorServiceUtils
                .getAverage(requestData.allPoints) : SimulatorServiceUtils.DEFAULT_AVERAGE_WIND;
        this.stepSizeMeters = SimulatorServiceUtils.knotsToMetersPerSecond(this.averageWind.getKnots())
                * (requestData.stepDurationMilliseconds / 1000.);
        Util.Pair<PolarDiagram, String> polarDiagramAndNotificationMessage = this
                .getPolarDiagram(requestData.selection.boatClassIndex);
        PolarDiagram polarDiagram = polarDiagramAndNotificationMessage.getA();
        String notificationMessage = polarDiagramAndNotificationMessage.getB();
        Simulator simulator = new SimulatorImpl(new SimulationParametersImpl(null, polarDiagram, null,
                null, SailingSimulatorConstants.ModeMeasured, true, true));
        Path gpsTrack = simulator.getLegGPSTrack(SimulatorServiceUtils.toSimulatorUISelection(requestData.selection));
        double totalTimeGPSTrackSeconds = (gpsTrack.getPathPoints().get(gpsTrack.getPathPoints().size() - 1)
                .getTimePoint().asMillis() - gpsTrack.getPathPoints().get(0).getTimePoint().asMillis()) / 1000;
        final TimePoint startTimePoint = requestData.allPoints.get(0).timepoint;
        long segmentDurationMillis = 0;
        int segmentIndex = 0;
        long totalDurationMillis = 0;
        for (int index = 0; index < requestData.turnPoints.size() - 1; index++) {
            segmentIndex++;
            totalDurationMillis += segmentDurationMillis;
            segmentDurationMillis = getTimeMillisecondsBetween(requestData.turnPoints.get(index),
                    requestData.turnPoints.get(index + 1), this.stepSizeMeters, requestData.useRealAverageWindSpeed,
                    gpsTrack, polarDiagram, startTimePoint.plus(totalDurationMillis).asMillis());
            logger.fine("Total time of segment " + segmentIndex + " = " + segmentDurationMillis + " milliseconds");
        }
        totalDurationMillis += segmentDurationMillis;
        long totalTimeSeconds = totalDurationMillis / 1000;
        logger.fine("TotalTimeGPS: " + totalTimeGPSTrackSeconds + "  TotalTimePoly: " + totalTimeSeconds);
        return new ResponseTotalTimeDTO(totalTimeSeconds, totalTimeSeconds / totalTimeGPSTrackSeconds,
                notificationMessage);
    }

    @Override
    public Response1TurnerDTO get1Turner(Request1TurnerDTO requestData) throws ConfigurationException {
        checkSimulatorReadPermissionOnCurrentServer();
        Util.Pair<PolarDiagram, String> polarDiagramAndNotificationMessage = this
                .getPolarDiagram(requestData.selection.boatClassIndex);
        PolarDiagram polarDiagram = polarDiagramAndNotificationMessage.getA();
        String notificationMessage = polarDiagramAndNotificationMessage.getB();
        Position edgeStart = requestData.edgeStart;
        Position edgeEnd = requestData.edgeEnd;
        Position oldMovedPosition = requestData.oldMovedPoint;
        Position newMovedPosition = requestData.newMovedPoint;
        Bearing oldMovedToNewMovedBearing = oldMovedPosition.getBearingGreatCircle(newMovedPosition);
        boolean areTowardsSameDirection = SimulatorServiceUtils.areTowardsSameDirection(oldMovedToNewMovedBearing,
                new DegreeBearingImpl(requestData.startToEndBearingDegrees));
        logger.info("oldMovedToNewMovedBearing = " + oldMovedToNewMovedBearing.getDegrees() + " degrees");
        logger.info("requestData.startToEndBearingDegrees = " + requestData.startToEndBearingDegrees
                + " degrees");
        logger.info("areTowardsSameDirection = " + areTowardsSameDirection);
        SimulationParameters simulationParameters = new SimulationParametersImpl(null, polarDiagram, null, null,
                SailingSimulatorConstants.ModeMeasured, true, true);
        Simulator sailingSimulator = new SimulatorImpl(simulationParameters);
        Path gpsWind = sailingSimulator.getLegGPSTrack(SimulatorServiceUtils
                .toSimulatorUISelection(requestData.selection));
        Grid grid = new CurvedGrid(oldMovedPosition, newMovedPosition);
        this.controlParameters.baseWindBearing += grid.getSouth().getDegrees();
        WindFieldGeneratorMeasured windFieldGenerator = new WindFieldGeneratorMeasured(grid, this.controlParameters);
        windFieldGenerator.setGPSWind(gpsWind);
        TimePoint startTime = new MillisecondsTimePoint(requestData.oldMovedPointTimePoint);
        PathGenerator1Turner pathGenerator = new PathGenerator1Turner(null);
        TimedPositionWithSpeed leftSide1Turner = null;
        TimedPositionWithSpeed rightSide1Turner = null;
        Position realStart = areTowardsSameDirection ? oldMovedPosition : newMovedPosition;
        Position realEnd = areTowardsSameDirection ? newMovedPosition : oldMovedPosition;
        leftSide1Turner = pathGenerator.get1Turner(windFieldGenerator, polarDiagram, realStart, realEnd, startTime,
                true, DEFAULT_STEP_MAX, DEFAULT_TIMESTEP);
        rightSide1Turner = pathGenerator.get1Turner(windFieldGenerator, polarDiagram, realStart, realEnd, startTime,
                false, DEFAULT_STEP_MAX, DEFAULT_TIMESTEP);
        boolean isLeftSide1TurnerOnTheInside = SimulatorServiceUtils.isPointInsideTriangle(leftSide1Turner.getPosition(),
                requestData.beforeMovedPoint,
                requestData.newMovedPoint, requestData.edgeStart);
        logger.info("isLeftSide1TurnerOnTheInside = " + isLeftSide1TurnerOnTheInside);
        boolean isRightSide1TurnerOnTheInside = SimulatorServiceUtils.isPointInsideTriangle(
                rightSide1Turner.getPosition(),
                requestData.beforeMovedPoint,
                requestData.newMovedPoint, requestData.edgeStart);
        logger.info("isRightSide1TurnerOnTheInside = " + isRightSide1TurnerOnTheInside);
        TimedPositionWithSpeed correct1Turner = null;
        if (isLeftSide1TurnerOnTheInside && isRightSide1TurnerOnTheInside == false) {
            correct1Turner = rightSide1Turner;
            System.out.println("voi folosi right side 1 turner");
        } else if (isRightSide1TurnerOnTheInside && isLeftSide1TurnerOnTheInside == false) {
            correct1Turner = leftSide1Turner;
            logger.info("voi folosi left side 1 turner");
        } else {
            logger.info("voi folosi endPosition");
            if (SimulatorServiceUtils.equals(leftSide1Turner.getPosition(), newMovedPosition, 0.0001)) {
                logger.info("voi folosi endPosition = leftSide1Turner!");
                correct1Turner = leftSide1Turner;
            } else if (SimulatorServiceUtils.equals(rightSide1Turner.getPosition(), newMovedPosition, 0.0001)) {
                logger.info("voi folosi endPosition = rightSide1Turner!");
                correct1Turner = rightSide1Turner;
            } else {
                logger.info("nu ar trebui sa ajunga aici NICIODATA!");
            }
        }
        long timeStepMilliseconds = 2000;
        double minimumDistanceMeters = 4.0;
        List<TimedPositionWithSpeed> path = pathGenerator.getIntersectionOptimalTowardWind(windFieldGenerator,
                polarDiagram, edgeStart, edgeEnd, correct1Turner, true, timeStepMilliseconds, minimumDistanceMeters);
        return new Response1TurnerDTO(SimulatorServiceUtils.toSimulatorWindDTOList(path), SimulatorServiceUtils.toSimulatorWindDTO(leftSide1Turner),
                SimulatorServiceUtils.toSimulatorWindDTO(rightSide1Turner), oldMovedPosition,
                newMovedPosition,
                notificationMessage);
    }

    @Override
    public List<String> getLegsNames(int selectedRaceIndex) {
        checkSimulatorReadPermissionOnCurrentServer();
        if (selectedRaceIndex < 0) {
            selectedRaceIndex = 0;
        }
        Simulator simulator = new SimulatorImpl(new SimulationParametersImpl(null, null, null, null,
                SailingSimulatorConstants.ModeMeasured, true, true));
        return simulator.getLegsNames(selectedRaceIndex);
    }

    @Override
    public List<String> getRacesNames() {
        checkSimulatorReadPermissionOnCurrentServer();
        Simulator simulator = new SimulatorImpl(new SimulationParametersImpl(null, null, null, null,
                SailingSimulatorConstants.ModeMeasured, true, true));
        return simulator.getRacesNames();
    }

    @Override
    public List<String> getCompetitorsNames(int selectedRaceIndex) {
        checkSimulatorReadPermissionOnCurrentServer();
        if (selectedRaceIndex < 0) {
            selectedRaceIndex = 0;
        }
        Simulator simulator = new SimulatorImpl(new SimulationParametersImpl(null, null, null, null,
                SailingSimulatorConstants.ModeMeasured, true, true));
        return simulator.getCompetitorsNames(selectedRaceIndex);
    }

    private void retrieveWindControlParameters(WindPatternDisplay pattern) {
        controlParameters.setDefaults();
        for (WindPatternSetting<?> s : pattern.getSettings()) {
            Field f;
            try {
                f = controlParameters.getClass().getField(s.getName());
                try {
                    LOGGER.fine("Setting " + f.getName() + " to " + s.getName() + " value : " + s.getValue());
                    f.set(controlParameters, s.getValue());
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("SimulatorServiceImpl => IllegalArgumentException with message " + e.getMessage());
                } catch (IllegalAccessException e) {
                    LOGGER.warning("SimulatorServiceImpl => IllegalAccessException with message " + e.getMessage());
                }
            } catch (SecurityException e) {
                LOGGER.warning("SimulatorServiceImpl => SecurityException with message " + e.getMessage());
            } catch (NoSuchFieldException e) {
                LOGGER.warning("SimulatorServiceImpl => NoSuchFieldException with message " + e.getMessage());
            }
        }
    }

    private SimulatorWindDTO createSimulatorWindDTO(Wind wind) {
        Position position = wind.getPosition();
        TimePoint timePoint = wind.getTimePoint();
        SimulatorWindDTO result = new SimulatorWindDTO();
        result.trueWindBearingDeg = wind.getBearing().getDegrees();
        result.trueWindSpeedInKnots = wind.getKnots();
        if (position != null) {
            result.position = position;
        }
        if (timePoint != null) {
            result.timepoint = timePoint;
        }
        return result;
    }

    private SimulatorWindDTO createSimulatorWindDTO(TimedPositionWithSpeed timedPositionWithSpeed) {
        Position position = timedPositionWithSpeed.getPosition();
        SpeedWithBearing speedWithBearing = timedPositionWithSpeed.getSpeed();
        TimePoint timePoint = timedPositionWithSpeed.getTimePoint();
        SimulatorWindDTO result = new SimulatorWindDTO();
        if (speedWithBearing == null) {
            result.trueWindBearingDeg = 0.0;
            result.trueWindSpeedInKnots = 0.0;
        } else {
            result.trueWindBearingDeg = speedWithBearing.getBearing().getDegrees();
            result.trueWindSpeedInKnots = speedWithBearing.getKnots();
        }
        if (position != null) {
            result.position = position;
        }
        if (timePoint != null) {
            result.timepoint = timePoint;
        }
        return result;
    }

    private WindFieldDTO createWindFieldDTO(WindFieldGenerator wf, TimePoint startTime, TimePoint endTime,
            Duration timeStep, WindFieldGenParamsDTO params) {
        WindFieldDTO windFieldDTO = new WindFieldDTO();
        Position[][] positionGrid = wf.getPositionGrid();
        List<SimulatorWindDTO> wList = new ArrayList<SimulatorWindDTO>();
        if (positionGrid != null && positionGrid.length > 0) {
            TimePoint t = startTime;
            while (t.compareTo(endTime) <= 0) {
                for (int i = 0; i < positionGrid.length; ++i) {
                    for (int j = 0; j < positionGrid[i].length; ++j) {
                        Wind localWind = wf.getWind(new TimedPositionWithSpeedImpl(t, positionGrid[i][j], null));
                        LOGGER.finer(localWind.toString());
                        wList.add(createSimulatorWindDTO(localWind));
                    }
                }
                t = new MillisecondsTimePoint(t.asMillis() + timeStep.asMillis());
            }
        }
        windFieldDTO.setMatrix(wList);
        if (params.isShowLines() && params.getSeedLines() == 'f') {
            this.getWindLinesFromStartLine(wf, windFieldDTO, startTime, endTime, timeStep);
        }
        if (params.isShowLines() && params.getSeedLines() == 'b') {
            this.getWindLinesFromEndLine(wf, windFieldDTO, startTime, endTime, timeStep);
        }
        windFieldDTO.curBearing = wf.getWindParameters().curBearing;
        windFieldDTO.curSpeed = wf.getWindParameters().curSpeed;
        if (params.isShowStreamlets()) {
            WindData windData = new WindData();
            windData.rcStart = params.getRaceCourseStart();
            windData.rcEnd = params.getRaceCourseEnd();
            windData.resX = params.getxRes();
            windData.resY = params.getyRes();
            windData.borderX = params.getBorderX();
            windData.borderY = params.getBorderY();
            windData.xScale = 1.5;
            windFieldDTO.windData = windData;
        }
        return windFieldDTO;
    }

    private void getWindLinesFromStartLine(WindFieldGenerator wf, WindFieldDTO windFieldDTO, TimePoint startTime,
            TimePoint endTime, Duration timeStep) {
        Position[][] positionGrid = wf.getPositionGrid();
        WindLinesDTO windLinesDTO = windFieldDTO.getWindLinesDTO();
        if (windLinesDTO == null) {
            windLinesDTO = new WindLinesDTO();
            windFieldDTO.setWindLinesDTO(windLinesDTO);
        }
        if (positionGrid != null && positionGrid.length > 0 && positionGrid[0].length > 2) {
            for (int j = 1; j < positionGrid[0].length - 1; ++j) {
                // for (int j = 0; j < positionGrid[0].length; ++j) {
                TimePoint t = startTime;
                Position p0 = positionGrid[0][j];
                Position p1 = positionGrid[1][j];
                Position seed = new DegreePosition(p0.getLatDeg() + 0.5 * (p0.getLatDeg() - p1.getLatDeg()), p0.getLngDeg() + 0.5
                        * (p0.getLngDeg() - p1.getLngDeg()));
                Position startPosition = new DegreePosition(seed.getLatDeg(), seed.getLngDeg());
                while (t.compareTo(endTime) <= 0) {
                    TimedPosition tp = new TimedPositionImpl(t, seed);
                    Path p = wf.getLine(tp, false /* forward */);
                    if (p != null) {
                        List<Position> positions = new ArrayList<>();
                        for (TimedPositionWithSpeed pathPoint : p.getPathPoints()) {
                            Position position = pathPoint.getPosition();
                            DegreePosition positionDTO = new DegreePosition(position.getLatDeg(), position.getLngDeg());
                            positions.add(positionDTO);
                        }
                        windLinesDTO.addWindLine(startPosition, tp.getTimePoint().asMillis(), positions);
                    }
                    t = new MillisecondsTimePoint(t.asMillis() + timeStep.asMillis());
                }
            }
        }
        // TODO: throws null pointer exception for when reading serialized paths.
        // TODO: should windlines also be serialized?
        // logger.info("Added : " + windFieldDTO.getWindLinesDTO().getWindLinesMap().size() + " wind lines");
    }

    private void getWindLinesFromEndLine(WindFieldGenerator wf, WindFieldDTO windFieldDTO, TimePoint startTime,
            TimePoint endTime, Duration timeStep) {
        Position[][] positionGrid = wf.getPositionGrid();
        WindLinesDTO windLinesDTO = windFieldDTO.getWindLinesDTO();
        if (windLinesDTO == null) {
            windLinesDTO = new WindLinesDTO();
            windFieldDTO.setWindLinesDTO(windLinesDTO);
        }
        if (positionGrid != null && positionGrid.length > 1 && positionGrid[0].length > 2) {
            int lastRowIndex = positionGrid.length - 1;
            for (int j = 1; j < positionGrid[lastRowIndex].length - 1; ++j) {

                TimePoint t = startTime;
                Position p0 = positionGrid[lastRowIndex][j];
                Position p1 = positionGrid[lastRowIndex - 1][j];
                Position seed = new DegreePosition(p0.getLatDeg() + 0.5 * (p0.getLatDeg() - p1.getLatDeg()), p0.getLngDeg() + 0.5
                        * (p0.getLngDeg() - p1.getLngDeg()));
                Position startPosition = new DegreePosition(seed.getLatDeg(), seed.getLngDeg());
                while (t.compareTo(endTime) <= 0) {
                    TimedPosition tp = new TimedPositionImpl(t, seed);
                    Path p = wf.getLine(tp, true /* forward */);
                    if (p != null) {
                        List<Position> positions = new ArrayList<>();
                        for (TimedPositionWithSpeed pathPoint : p.getPathPoints()) {
                            Position position = pathPoint.getPosition();
                            Position positionDTO = new DegreePosition(position.getLatDeg(), position.getLngDeg());
                            positions.add(positionDTO);
                        }

                        windLinesDTO.addWindLine(startPosition, tp.getTimePoint().asMillis(), positions);
                    }
                    t = new MillisecondsTimePoint(t.asMillis() + timeStep.asMillis());
                }
            }
        }

    }

    private SimulatedPathsEvenTimedResultDTO getSimulatedPathsEvenTimed(List<Position> course, WindFieldGenerator wf,
            char mode, SimulatorUISelectionDTO selection, boolean showOmniscient, boolean showOpportunist)
            throws ConfigurationException {
        LOGGER.fine("Retrieving simulated paths");
        Util.Pair<PolarDiagram, String> polarDiagramAndNotificationMessage = this.getPolarDiagram(selection.boatClassIndex);
        PolarDiagram pd = polarDiagramAndNotificationMessage.getA();
        int[] gridRes = wf.getGridResolution();
        Position[] gridArea = wf.getGridAreaGps();
        LOGGER.fine("showOmniscient : "+showOmniscient);
        LOGGER.fine("showOpportunist: "+showOpportunist);        
        if (gridArea != null) {
            // initialize grid of supporting location for windfield
            Grid bd = new CurvedGrid(gridArea[0], gridArea[1]);
            // set base wind bearing
            wf.getWindParameters().baseWindBearing += bd.getSouth().getDegrees();
            LOGGER.fine("base wind: " + (pd==null?"null":pd.getWind().getKnots() + " kn, ")
                    + ((wf.getWindParameters().baseWindBearing) % 360.0) + "\u00B0");
            // set water current
            SpeedWithBearing current = new KnotSpeedWithBearingImpl(wf.getWindParameters().curSpeed, new DegreeBearingImpl(wf.getWindParameters().curBearing));
            if (pd != null) {
                if (wf.getWindParameters().curSpeed > 0) {
                    pd.initializeSOGwithCurrent(); // polar-diagram is extended with data to support water currents
                }
                pd.setCurrent(current);
                if (pd.getCurrent() != null) {
                    LOGGER.fine("water current: " + pd.getCurrent().getKnots() + " kn, "
                            + pd.getCurrent().getBearing().getDegrees() + "\u00B0");
                }
            }
            wf.setBoundary(bd);
            Position[][] positionGrid = bd.generatePositions(gridRes[0], gridRes[1], gridRes[2], gridRes[3]);
            wf.setPositionGrid(positionGrid);
            wf.generate(wf.getStartTime(), wf.getEndTime(), wf.getTimeStep());
        }
        SimulationParameters sp = new SimulationParametersImpl(course, pd, wf, null, mode, showOmniscient, showOpportunist);        
        Map<PathType, Path> pathsAndNames = null;
        try {
            pathsAndNames = simulationService.getAllPathsEvenTimed(sp, wf.getTimeStep().asMillis());
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted while calculating paths", e);
        } catch (ExecutionException e) {
            logger.log(Level.SEVERE, "Problem while calculating paths", e);
        }
        int noOfPaths = pathsAndNames.size();
        if (mode == SailingSimulatorConstants.ModeMeasured) {
            noOfPaths++; // the last path is the polyline
        }
        PathDTO[] pathDTOs = new PathDTO[noOfPaths];
        int index = noOfPaths - 1;
        if (mode == SailingSimulatorConstants.ModeMeasured) {
            // Adding the polyline
            // TODO bug4427: Eclipse Oxygen warnings have pointed at the strange get(String) invocations below; it turns out the whole mode=m set-up in the standalone simulator seems broken; Christopher to clarify
//            pathDTOs[0] = this.getPolylinePathDTO(pathsAndNames.get("6#GPS Poly"), pathsAndNames.get("7#GPS Track"));
            pathDTOs[0] = this.getPolylinePathDTO(pathsAndNames.get(null), pathsAndNames.get(null)); // TODO bug4427: the above expressions evaluate to null anyway, provoking an NPE; however, mode=m fails much earlier because the SimulatorMap.regattaAreaCanvasOverlay field is null, causing an NPE even earlier
        }
        for (Entry<PathType, Path> entry : pathsAndNames.entrySet()) {
            LOGGER.fine("Path " + entry.getKey().getTxtId());
            // NOTE: pathName convention is: sort-digit + "#" + path-name
            // pathsAndNames is TreeMap which ensures sorting
            pathDTOs[index] = new PathDTO(entry.getKey());
            // fill pathDTO with path points where speed is true wind speed
            List<SimulatorWindDTO> wList = new ArrayList<SimulatorWindDTO>();
            for (TimedPositionWithSpeed p : entry.getValue().getPathPoints()) {
                wList.add(createSimulatorWindDTO(p));
            }
            pathDTOs[index].setPoints(wList);
            pathDTOs[index].setAlgorithmTimedOut(entry.getValue().getAlgorithmTimedOut());
            pathDTOs[index].setMixedLeg(entry.getValue().getMixedLeg());
            index--;
        }
        RaceMapDataDTO rcDTO;
        if (mode == SailingSimulatorConstants.ModeMeasured) {
            rcDTO = new RaceMapDataDTO();
            rcDTO.coursePositions = new CoursePositionsDTO();
            rcDTO.coursePositions.waypointPositions = new ArrayList<>();
            rcDTO.coursePositions.waypointPositions.add(course.get(0));
            rcDTO.coursePositions.waypointPositions.add(course.get(1));
        } else {
            rcDTO = null;
        }
        return new SimulatedPathsEvenTimedResultDTO(pathDTOs, rcDTO, null, polarDiagramAndNotificationMessage.getB());
    }

    private Util.Pair<PolarDiagram, String> getPolarDiagram(int boatClassIndex) throws ConfigurationException {
        final BoatClassDTO boatClass = polarDiagramCache.getBoatClass(boatClassIndex);
        final String notificationMessage;
        final PolarDiagram polarDiagram;
        if (boatClass == null) {
            notificationMessage = Util.hasLength(polarDiagramCache.getNotificationMessage())
                    ? polarDiagramCache.getNotificationMessage()
                    : "Boat class with index " + boatClassIndex + " not found";
            polarDiagram = null;
        } else {
            polarDiagram = polarDiagramCache.getPolarDiagram(boatClass);
            if (polarDiagram == null) {
                notificationMessage = Util.hasLength(polarDiagramCache.getNotificationMessage())
                        ? polarDiagramCache.getNotificationMessage()
                                : "Couldn't find polar diagram for boat class "+boatClass.getName();
            } else {
                notificationMessage = null;
            }
        }
        return new Util.Pair<PolarDiagram, String>(polarDiagram, notificationMessage);
    }

    private PathDTO getPolylinePathDTO(Path gpsPoly, Path gpsTrack) {
        List<TimedPositionWithSpeed> gpsTrackPoints = gpsTrack.getPathPoints();
        List<TimedPositionWithSpeed> gpsPolyPoints = gpsPoly.getPathPoints();
        int noOfGpsTrackPoints = gpsTrackPoints.size();
        int noOfGpsPolyPoints = gpsPolyPoints.size();
        if (noOfGpsTrackPoints == 0 || noOfGpsTrackPoints == 1 || noOfGpsPolyPoints == 0 || noOfGpsPolyPoints == 1) {
            return null;
        }
        TimedPositionWithSpeed startPoint = gpsPolyPoints.get(0);
        TimedPositionWithSpeed endPoint = gpsPolyPoints.get(noOfGpsPolyPoints - 1);
        // System.out.println("gpsTrackPoints.size() = " + gpsTrackPoints.size());
        int startPointIndex = SimulatorServiceUtils.getIndexOfClosest(gpsTrackPoints, startPoint);
        // System.out.println("startPointIndex = " + startPointIndex);
        int endPointIndex = SimulatorServiceUtils.getIndexOfClosest(gpsTrackPoints, endPoint);
        // System.out.println("endPointIndex = " + endPointIndex);
        List<TimedPositionWithSpeed> polylinePoints = gpsTrackPoints.subList(startPointIndex, endPointIndex + 1);
        List<TimedPositionWithSpeed> turns = (new PathImpl(polylinePoints, null, false /* algorithmTimedOut */, false /* mixedLeg */)).getTurns();
        // PathImpl.saveToGpxFile(new PathImpl(turns, null), "C:\\gps_path_turns_20deg_not_even_timed.gpx");
        List<SimulatorWindDTO> points = new ArrayList<SimulatorWindDTO>();
        boolean isTurn = false;
        SpeedWithBearing speedWithBearing = null;
        Position position = null;
        for (TimedPositionWithSpeed point : polylinePoints) {
            isTurn = false;
            for (TimedPositionWithSpeed turn : turns) {
                if (turn.getPosition().getLatDeg() == point.getPosition().getLatDeg()
                        && turn.getPosition().getLngDeg() == point.getPosition().getLngDeg()
                        && turn.getTimePoint().asMillis() == point.getTimePoint().asMillis()
                        && turn.getSpeed().getKnots() == point.getSpeed().getKnots()
                        && turn.getSpeed().getBearing().getDegrees() == point.getSpeed().getBearing().getDegrees()) {
                    isTurn = true;
                    break;
                }
            }
            speedWithBearing = point.getSpeed();
            position = point.getPosition();
            points.add(new SimulatorWindDTO(position.getLatDeg(), position.getLngDeg(), speedWithBearing.getKnots(),
                    speedWithBearing.getBearing().getDegrees(), point.getTimePoint(), isTurn));
        }
        PathDTO result = new PathDTO(POLYLINE_PATH_NAME);
        result.setPoints(points);
        result.setAlgorithmTimedOut(false);
        return result;
    }

    private List<Position> getIntermediatePoints(Position startPoint, Position endPoint, double stepSizeMeters) {
        List<Position> result = new ArrayList<Position>();
        double distance = startPoint.getDistance(endPoint).getMeters();
        int noOfSteps = (int) (distance / stepSizeMeters) + 1;
        double bearing = startPoint.getBearingGreatCircle(endPoint).getDegrees();
        Position temp = null;
        result.add(startPoint);
        for (int stepIndex = 1; stepIndex < noOfSteps; stepIndex++) {
            temp = SimulatorServiceUtils.getDestinationPoint(startPoint, bearing, stepSizeMeters * stepIndex);
            result.add(temp);
            bearing = startPoint.getBearingGreatCircle(temp).getDegrees();
        }
        return result;
    }

    private long getTimeMillisecondsBetween(Position turn1, Position turn2, double stepSizeMeters, boolean useRealAverageWindSpeed, Path gpsTrack,
            PolarDiagram polarDiagram, long startTimePoint2asMillis) {
        Position p1 = turn1;
        Position p2 = turn2;
        List<Position> points = getIntermediatePoints(p1, p2, stepSizeMeters);
        int noOfPointsMinus1 = points.size() - 1;
        Position startPoint = null;
        Position endPoint = null;
        double boatBearingDeg = 0.;
        double boatSpeedMetersPerSecond = 0.;
        double distanceMeters = 0.;
        long timepointAsMillis = startTimePoint2asMillis;
        SpeedWithBearing windAtTimePoint = null;
        long stepTimeMilliseconds = 0L;
        for (int index = 0; index < noOfPointsMinus1; index++) {
            startPoint = points.get(index);
            endPoint = points.get(index + 1);
            distanceMeters = startPoint.getDistance(endPoint).getMeters();
            windAtTimePoint = useRealAverageWindSpeed ? SimulatorServiceUtils.getWindAtTimepoint(timepointAsMillis,
                    gpsTrack) : SimulatorServiceUtils.DEFAULT_AVERAGE_WIND;
            boatBearingDeg = SimulatorServiceUtils.getInitialBearing(startPoint, endPoint);
            polarDiagram.setWind(windAtTimePoint);
            boatSpeedMetersPerSecond = polarDiagram.getSpeedAtBearing(new DegreeBearingImpl(boatBearingDeg))
                    .getMetersPerSecond();
            stepTimeMilliseconds = (long) ((distanceMeters / boatSpeedMetersPerSecond) * 1000);
            // problem right here: boatSpeed might be 0 for very small distances
            // this is a rough fix
            if (boatSpeedMetersPerSecond == 0.0) {
                stepTimeMilliseconds = 1000;
            }
            timepointAsMillis += stepTimeMilliseconds;
        }
        return timepointAsMillis - startTimePoint2asMillis;
    }

    @Override
    public String getGoogleMapsLoaderAuthenticationParams() {
        return Activator.getInstance().getGoogleMapsLoaderAuthenticationParams();
    }
}
