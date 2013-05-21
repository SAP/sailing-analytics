package com.sap.sailing.simulator.test.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.gwt.ui.server.SimulatorServiceImpl;
import com.sap.sailing.gwt.ui.shared.ConfigurationException;
import com.sap.sailing.gwt.ui.shared.PositionDTO;
import com.sap.sailing.gwt.ui.shared.RequestTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.ResponseTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.impl.PathImpl;
import com.sap.sailing.simulator.impl.TimedPositionWithSpeedImpl;

public class TestTimeComparison {

    private static final String RESOURCES = "resources/";

    private List<PositionDTO> turnPoints = null;
    private List<SimulatorWindDTO> allPoints = null;

    private Map<Integer, String> boatClassesIndexes = null;
    // private Map<Integer, String> averageWindFlags = null;
    // private List<Integer> timeStepMillisecondsSizes = null;
    private Integer timeStepMilliseconds = 1000;

    public static void main(String[] args) throws Exception {
        System.out.println("Collection of test methods for evaluating simulator race times for calibration.");  
    }
    
    public void initialize() {

        this.boatClassesIndexes = new HashMap<Integer, String>();
        this.boatClassesIndexes.put(0, "49er");
        this.boatClassesIndexes.put(1, "49er rBethwaite");
        this.boatClassesIndexes.put(2, "49er ORC");
        this.boatClassesIndexes.put(3, "49er STG");
        this.boatClassesIndexes.put(4, "505 STG");

        /*
         * this.averageWindFlags = new HashMap<Integer, String>(); this.averageWindFlags.put(0, "default average wind");
         * this.averageWindFlags.put(1, "real average wind");
         */

        /*
         * this.timeStepMillisecondsSizes = new ArrayList<Integer>(); this.timeStepMillisecondsSizes.add(1000);
         * this.timeStepMillisecondsSizes.add(1250); this.timeStepMillisecondsSizes.add(1500);
         * this.timeStepMillisecondsSizes.add(1750); this.timeStepMillisecondsSizes.add(2000);
         */
    }

    public void runSimulation() throws Exception {

        File dir = getFile("");
        String[] flist = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".data");
            }
        });

        TracTracReader ttreader = new TracTracReaderFromFiles(flist);

        List<TrackedRace> lst = ttreader.read();

        // List<String> csvRows = new ArrayList<String>();

        BufferedWriter outputCSV = new BufferedWriter(new FileWriter(
                getFile("test.csv")));
        String header = "Event, Race, Competitor, Leg#, Leg, GPS time";

        for (String pd : boatClassesIndexes.values()) {
            header += ", " + pd;
        }
        header += ", Avg Wind Speed, Avg Wind Bearing, Avg Boat Speed, Avg Boat Bearing";

        outputCSV.write(header);
        outputCSV.newLine();

        for (TrackedRace tr : lst) {

            // String csvLine = "";
            // System.out.println(tr.getRace().getBoatClass());;
            RegattaAndRaceIdentifier id = tr.getRaceIdentifier();
            Iterable<Competitor> competitors = tr.getRace().getCompetitors();
            List<Leg> legs = tr.getRace().getCourse().getLegs();
            // System.out.println(legs);
            // System.out.println(id.getRaceName() + "/" + id.getRegattaName());

            int competitorIndex = -1;
            for (Competitor competitor : competitors) {
                competitorIndex++;
                GPSFixTrack<Competitor, GPSFixMoving> track = tr.getTrack(competitor);
                track.lockForRead();
                Iterator<GPSFixMoving> it = track.getFixes().iterator();

                int legIndex = -1;
                for (Leg leg : legs) {
                    legIndex++;
                    List<TimedPositionWithSpeed> polylinePoints = new ArrayList<TimedPositionWithSpeed>();
                    MarkPassing mp = tr.getMarkPassing(competitor, leg.getTo());
                    GPSFixMoving current;
                    while (mp.getTimePoint().after((current = it.next()).getTimePoint())) {

                        Position currentPosition = current.getPosition();
                        TimePoint currentTime = current.getTimePoint();
                        SpeedWithBearing currentSpeed = current.getSpeed();
                        TimedPositionWithSpeed currentTPWS = new TimedPositionWithSpeedImpl(currentTime,
                                currentPosition, currentSpeed);
                        polylinePoints.add(currentTPWS);

                    }
                    allPoints = new ArrayList<SimulatorWindDTO>();
                    List<TimedPositionWithSpeed> turns = (new PathImpl(polylinePoints, null)).getTurns();
                    turnPoints = new ArrayList<PositionDTO>();
                    boolean isTurn = false;
                    Position position = null;
                    TimePoint timePoint = null;
                    Wind wind = null;

                    for (TimedPositionWithSpeed point : polylinePoints) {
                        isTurn = false;
                        position = point.getPosition();
                        timePoint = point.getTimePoint();
                        wind = tr.getWind(position, timePoint);
                        for (TimedPositionWithSpeed turn : turns) {
                            if (turn.getPosition().getLatDeg() == point.getPosition().getLatDeg()
                                    && turn.getPosition().getLngDeg() == point.getPosition().getLngDeg()
                                    && turn.getTimePoint().asMillis() == point.getTimePoint().asMillis()
                                    && turn.getSpeed().getKnots() == point.getSpeed().getKnots()
                                    && turn.getSpeed().getBearing().getDegrees() == point.getSpeed().getBearing()
                                    .getDegrees()) {
                                isTurn = true;
                                turnPoints.add(new PositionDTO(position.getLatDeg(), position.getLngDeg()));
                                break;
                            }
                        }
                        allPoints.add(new SimulatorWindDTO(position.getLatDeg(), position.getLngDeg(), wind.getKnots(),
                                wind.getBearing().getDegrees(), timePoint.asMillis(), isTurn));
                        // end polylinePoints loop
                    }

                    // getTotalTime(0, 0, 1000);
                    // System.out.println(getSummary(id, competitor, leg));
                    outputCSV.write(getSummary(id, competitor, leg, competitorIndex, legIndex));
                    outputCSV.newLine();

                    // end legs loop
                }

                // end competitors loop
            }

            // end tracked races loop
        }

        outputCSV.close();
        // end method
    }

    /*
     * private void getTotalTime(final int boatClassIndex, final int useRealAverageWindSpeed, final int
     * stepDurationMilliseconds) throws ConfigurationException {
     * 
     * final SimulatorServiceImpl simulatorService = new SimulatorServiceImpl(); final RequestTotalTimeDTO requestData =
     * new RequestTotalTimeDTO(boatClassIndex, this.allPoints, this.turnPoints, useRealAverageWindSpeed == 1,
     * stepDurationMilliseconds, true); final ResponseTotalTimeDTO receiveData =
     * simulatorService.getTotalTime_new(requestData);
     * 
     * final SpeedWithBearing averageWind = simulatorService.getAverageWind(); final double stepSizeMeters =
     * simulatorService.getStepSizeMeters(); int stepIndex = 0;
     * 
     * System.err.println("==================================================");
     * System.err.println(this.boatClassesIndexes.get(boatClassIndex)); System.err.println("average wind speed = " +
     * averageWind.getKnots() + " knots, bearing = " + averageWind.getBearing().getDegrees() + " degrees");
     * System.err.println("step size = " + stepSizeMeters + " meters"); stepIndex = 0; for (final Quadruple<PositionDTO,
     * PositionDTO, Double, Double> segment : receiveData.segments) { System.err.println("segment " + stepIndex +
     * " from [" + segment.getA().latDeg + "," + segment.getA().lngDeg + "] to [" + segment.getB().latDeg + "," +
     * segment.getB().lngDeg + "], length = " + segment.getC() + " meters, time = " + segment.getD() / 1000. +
     * " seconds"); stepIndex++; } System.err.println(this.boatClassesIndexes.get(boatClassIndex) + ", " +
     * this.averageWindFlags.get(useRealAverageWindSpeed) + ", " + stepDurationMilliseconds +
     * "milliseconds timestep, total time: " + receiveData.totalTimeSeconds + " seconds");
     * System.err.println("=================================================="); }
     */

    private String getSummary(final RegattaAndRaceIdentifier id, final Competitor competitor, final Leg leg,
            int competitorIndex, final int legIndex) throws ConfigurationException {

        int noPoints = this.allPoints.size();
        long finishTime = this.allPoints.get(noPoints - 1).timepoint;
        long startTime = this.allPoints.get(0).timepoint;
        double gpsTime = (finishTime - startTime) / 1000.0;
        long totalDistanceMeters = 0;
        Iterator<SimulatorWindDTO> it = this.allPoints.iterator();
        SimulatorWindDTO prec = it.next();
        SimulatorWindDTO succ = null;
        double avgBearing = 0.0;
        while (it.hasNext()) {
            succ = it.next();
            Position p1 = new DegreePosition(prec.position.latDeg, prec.position.lngDeg);
            Position p2 = new DegreePosition(succ.position.latDeg, succ.position.lngDeg);
            totalDistanceMeters += p2.getDistance(p1).getMeters();
            avgBearing += 1.0 / noPoints * p1.getBearingGreatCircle(p2).getDegrees();
            prec = succ;
        }
        double speedMetersPerSecond = totalDistanceMeters / gpsTime;
        double speedKnots = speedMetersPerSecond * 1.94;
        String result = "";
        result = id.getRegattaName() + ", " + id.getRaceName();
        result += ", " + competitor.getName() + ", " + legIndex + ", " + leg.toString() + ", " + gpsTime;

        SpeedWithBearing averageWind = null;

        for (Integer boatClassIndex : boatClassesIndexes.keySet()) {
            final SimulatorServiceImpl simulatorService = new SimulatorServiceImpl();
            final RequestTotalTimeDTO requestData = new RequestTotalTimeDTO(new SimulatorUISelectionDTO(boatClassIndex, 0, competitorIndex, legIndex),
                    this.timeStepMilliseconds, this.allPoints, this.turnPoints, true);

            final ResponseTotalTimeDTO receiveData = simulatorService.getTotalTime(requestData);
            averageWind = simulatorService.getAverageWind();
            result += ", " + receiveData.totalTimeSeconds;
        }
        result += ", " + averageWind.getKnots() + ", " + averageWind.getBearing();
        result += ", " + speedKnots + ", " + avgBearing;

        return result;

    }

    private File getFile(String fileName) {
        return new File(RESOURCES + fileName);
    }

}
