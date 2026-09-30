package com.sap.sailing.simulator.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tractracadapter.TracTracAdapterFactory;
import com.sap.sailing.domain.tractracadapter.impl.TracTracAdapterFactoryImpl;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

@SuppressWarnings("restriction")
public class PathGeneratorTracTrac extends PathGeneratorBase {

    private static final Logger LOGGER = Logger.getLogger("com.sap.sailing.simulator");
    //private static final long DEFAULT_TIMEOUT_MILLISECONDS = 60000;

    //private RacingEventServiceImpl service = null;
    private TracTracAdapterFactory tracTracAdapterFactory;
    private RaceHandle raceHandle = null;
    @SuppressWarnings("unused")
    private URL raceURL = null;
    @SuppressWarnings("unused")
    private URI liveURI = null;
    @SuppressWarnings("unused")
    private URI storedURI = null;
    private double windScale = 0.0;
    private LinkedList<TimedPositionWithSpeed> raceCourse = null;
    private int legIndex = 0;
    private int competitorIndex = 0;

    public PathGeneratorTracTrac(SimulationParameters parameters) {

        this.parameters = parameters;
        //this.service = new RacingEventServiceImpl(EmptyWindStore.INSTANCE, EmptyGPSFixStore.INSTANCE, null);
        this.tracTracAdapterFactory = new TracTracAdapterFactoryImpl();

        this.legIndex = 0;
        this.competitorIndex = 0;
    }
    
    protected TracTracAdapterFactory getTracTracAdapterFactory() {
        return tracTracAdapterFactory;
    }

    private void intializeRaceHandle() {

        if (this.raceHandle != null) {
            return;
        }

        LOGGER.info("Calling service.addTracTracRace");

        try {
            // TODO: refactor, so that trackedRace is passed from server; no separate access to RacingEventService
            this.raceHandle = null; //SimulatorUtils.loadRace(service, tracTracAdapterFactory, raceURL, liveURI, storedURI, null, null, DEFAULT_TIMEOUT_MILLISECONDS);

        } catch (Exception error) {
            LOGGER.severe(error.getMessage());
        }
    }

    public void setSelectionParameters(int legIndex, int competitorIndex) {
        this.legIndex = legIndex;
        this.competitorIndex = competitorIndex;
    }

    public void setEvaluationParameters(String raceURLString, String liveURIString, String storedURIString,
            double windScale) {

        try {
            this.raceURL = new URL(raceURLString);
        } catch (MalformedURLException error) {
            LOGGER.severe("MalformedURLException when constructing the raceURL " + error.getMessage());
        }

        try {
            this.liveURI = (liveURIString == null) ? null : new URI(liveURIString);
        } catch (URISyntaxException error) {
            LOGGER.severe("URISyntaxException when constructing the liveURI " + error.getMessage());
        }

        try {
            this.storedURI = (storedURIString == null) ? null : new URI(storedURIString);
        } catch (URISyntaxException error) {
            LOGGER.severe("URISyntaxException when constructing the storedURI " + error.getMessage());
        }

        this.windScale = windScale;
    }

    public Path getRaceCourse() {
        return this.raceCourse == null ? null : new PathImpl(this.raceCourse, null, this.algorithmTimedOut);
    }

    public List<String> getLegsNames() {
        this.intializeRaceHandle();
        List<String> result = new ArrayList<String>();
        RaceDefinition race = this.raceHandle.getRace();
        for (Leg leg : race.getCourse().getLegs()) {
            result.add(leg.toString());
        }
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        this.intializeRaceHandle();

        // getting the first race
        RaceDefinition raceDef = this.raceHandle.getRace();
        Regatta regatta = this.raceHandle.getRegatta();

        // TODO: refactor, so that trackedRace is passed from server; no separate access to RacingEventService
        TrackedRace trackedRace = null; //this.service.getTrackedRace(regatta, raceDef)
        trackedRace.waitUntilNotLoading();
        Iterator<Competitor> competitors = raceDef.getCompetitors().iterator();
        Competitor competitor = null;
        for (int index = 0; index <= this.competitorIndex; index++) {
            competitor = competitors.next();
        }
        Leg leg = raceDef.getCourse().getLegs().get(this.legIndex);
        TimePoint startTime = trackedRace.getMarkPassing(competitor, leg.getFrom()).getTimePoint();
        TimePoint endTime = trackedRace.getMarkPassing(competitor, leg.getTo()).getTimePoint();
        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        track.lockForRead();
        try {
            Iterator<GPSFixMoving> it = track.getFixesIterator(startTime, true);
            LinkedList<TimedPositionWithSpeed> path = new LinkedList<TimedPositionWithSpeed>();
            while (it.hasNext()) {
                GPSFixMoving gpsFix = it.next();
                if (gpsFix.getTimePoint().after(endTime)) {
                    break;
                }
                Position position = gpsFix.getPosition();
                TimePoint timePoint = gpsFix.getTimePoint();
                Wind gpsWind = trackedRace.getWind(position, timePoint);
                if (gpsWind.getKnots() == 1.0) {
                    path.addLast(new TimedPositionWithSpeedImpl(timePoint, position, scale(gpsWind, this.windScale)));
                } else {
                    path.addLast(new TimedPositionWithSpeedImpl(timePoint, position, gpsWind));
                }
            }
            return new PathImpl(path, null, this.algorithmTimedOut);
        } finally {
            track.unlockAfterRead();
        }
    }

    @SuppressWarnings("null")
    public Path getPathPolyline() {
        this.intializeRaceHandle();
        // getting the race
        RaceDefinition raceDef = this.raceHandle.getRace();
        @SuppressWarnings("unused")
        Regatta regatta = this.raceHandle.getRegatta();

        // TODO: refactor, so that trackedRace is passed from server; no separate access to RacingEventService
        TrackedRace trackedRace = null; //this.service.getTrackedRace(regatta, raceDef);
        /*try {
            while (trackedRace.getStatus().getLoadingProgress() < 1.0) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

        Iterator<Competitor> competitors = raceDef.getCompetitors().iterator();
        Competitor competitor = null;

        for (int index = 0; index <= this.competitorIndex; index++) {
            competitor = competitors.next();
        }

        LinkedList<TimedPositionWithSpeed> path = new LinkedList<TimedPositionWithSpeed>();
        this.raceCourse = new LinkedList<TimedPositionWithSpeed>();

        Leg leg = raceDef.getCourse().getLegs().get(this.legIndex);

        TimePoint startTime = trackedRace.getMarkPassing(competitor, leg.getFrom()).getTimePoint();
        Position startPosition = trackedRace.getApproximatePosition(leg.getFrom(), startTime);
        Wind startWind = trackedRace.getWind(startPosition, startTime);
        this.raceCourse.addLast(new TimedPositionWithSpeedImpl(startTime, startPosition, startWind));

        TimePoint endTime = trackedRace.getMarkPassing(competitor, leg.getTo()).getTimePoint();
        Position endPosition = trackedRace.getApproximatePosition(leg.getTo(), endTime);
        Wind endWind = trackedRace.getWind(endPosition, endTime);
        this.raceCourse.addLast(new TimedPositionWithSpeedImpl(endTime, endPosition, endWind));

        Iterable<GPSFixMoving> gpsFixes = trackedRace.approximate(competitor, startTime, endTime);
        Iterator<GPSFixMoving> gpsIter = gpsFixes.iterator();

        while (gpsIter.hasNext()) {
            GPSFixMoving gpsFix = gpsIter.next();
            if (gpsFix.getTimePoint().after(endTime)) {
                break;
            }

            Position position = gpsFix.getPosition();
            TimePoint timePoint = gpsFix.getTimePoint();

            Wind gpsWind = trackedRace.getWind(position, timePoint);

            if (gpsWind.getKnots() == 1.0) {
                path.addLast(new TimedPositionWithSpeedImpl(timePoint, position, scale(gpsWind, this.windScale)));
            } else {
                path.addLast(new TimedPositionWithSpeedImpl(timePoint, position, gpsWind));
            }
        }

        return new PathImpl(path, null, this.algorithmTimedOut);
    }

    public static SpeedWithBearing scale(SpeedWithBearing speed, double scale) {
        return new KnotSpeedWithBearingImpl(speed.getKnots() * scale, new DegreeBearingImpl(speed.getBearing().getDegrees()));
    }

    public List<String> getComeptitorsNames() {
        this.intializeRaceHandle();
        List<String> result = new ArrayList<String>();
        RaceDefinition race = this.raceHandle.getRace();
        for (Entry<Competitor, Boat> competitorAndBoatEntry: race.getCompetitorsAndTheirBoats().entrySet()) {
            result.add(competitorAndBoatEntry.getKey().getName() + ", " + competitorAndBoatEntry.getValue().getName());
        }
        return result;
    }
}
