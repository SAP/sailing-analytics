package com.sap.sailing.domain.swisstimingadapter;

import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.swisstimingadapter.impl.DomainFactoryImpl;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;

import difflib.PatchFailedException;

public interface DomainFactory {
    /**
     * A default domain factory for test purposes only. In a server environment, ensure NOT to use this. Use what can be
     * reached from <code>RacingEventService.getBaseDomainFactory()</code> instead which should be the single instance
     * used by all other services linked to the <code>RacingEventService</code>.
     */
    final static DomainFactory INSTANCE = new DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory.INSTANCE);
    
    com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory();

    Regatta getOrCreateDefaultRegatta(RaceLogStore raceLogStore, String raceID, BoatClass boatClass, TrackedRegattaRegistry trackedRegattaRegistry);

    Nationality getOrCreateNationality(String threeLetterIOCCode);

    Competitor createCompetitorWithoutID(com.sap.sailing.domain.swisstimingadapter.Competitor competitor, String raceId, BoatClass boatClass);

    Competitor createCompetitorWithID(com.sap.sailing.domain.swisstimingadapter.Competitor competitor, BoatClass boatClass);

    Competitor createCompetitorWithoutID(String boatID, String threeLetterIOCCode, String name, String raceId, BoatClass boatClass);
    
    String getCompetitorID(String boatID, RaceType raceType);

    String getCompetitorID(String boatID, BoatClass boatClass);

    RaceDefinition createRaceDefinition(Regatta regatta, Race race, StartList startList, com.sap.sailing.domain.swisstimingadapter.Course course);

    com.sap.sailing.domain.base.Mark getOrCreateMark(String trackerID);
    
    GPSFixMoving createGPSFix(TimePoint timePointOfTransmission, Fix fix);

    void updateCourseWaypoints(Course courseToUpdate, Iterable<Mark> marks) throws PatchFailedException;
    
    MarkPassing createMarkPassing(TimePoint timePoint, Waypoint waypoint, Competitor competitor);

    void removeRace(String raceID);

    RaceType getRaceTypeFromRaceID(String raceID);

    RaceTrackingConnectivityParameters createTrackingConnectivityParameters(String hostname, int port, String raceID, String raceName,
            String raceDescription, BoatClass boatClass, StartList startList, long delayToLiveInMillis,
            SwissTimingFactory swissTimingFactory, DomainFactory domainFactory, RaceLogStore raceLogStore);

    ControlPoint getOrCreateControlPoint(Iterable<String> devices);

    RaceDefinition createRaceDefinition(Regatta regatta, String raceID, Iterable<Competitor> competitors,
            List<ControlPoint> courseDefinition);

}
