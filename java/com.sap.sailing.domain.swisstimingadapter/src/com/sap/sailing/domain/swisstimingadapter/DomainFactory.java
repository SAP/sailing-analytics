package com.sap.sailing.domain.swisstimingadapter;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.impl.DomainFactoryImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

import difflib.PatchFailedException;

public interface DomainFactory {
    /**
     * A default domain factory for test purposes only. In a server environment, ensure NOT to use this. Use what can be
     * reached from <code>RacingEventService.getBaseDomainFactory()</code> instead which should be the single instance
     * used by all other services linked to the <code>RacingEventService</code>.
     */
    final static DomainFactory INSTANCE = new DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory.INSTANCE);
    
    com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory();

    /**
     * @param boatClass if {@code null}, the boat class will be inferred from the Race ID
     */
    Regatta getOrCreateDefaultRegatta(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, String raceID, BoatClass boatClass, TrackedRegattaRegistry trackedRegattaRegistry);

    Nationality getOrCreateNationality(String threeLetterIOCCode);

    Pair<Competitor, Boat> createCompetitorWithoutID(com.sap.sailing.domain.swisstimingadapter.Competitor competitor, String raceId, BoatClass boatClass);

    Pair<Competitor, Boat> createCompetitorWithID(com.sap.sailing.domain.swisstimingadapter.Competitor competitor, BoatClass boatClass);

    Pair<Competitor, Boat> createCompetitorWithoutID(String competitorId, String threeLetterIOCCode, String name, String raceId, BoatClass boatClass);
    
    String getCompetitorID(String boatID, RaceType raceType);

    String getCompetitorID(String boatID, BoatClass boatClass);

    RaceDefinition createRaceDefinition(Regatta regatta, Race race, StartList startList, com.sap.sailing.domain.swisstimingadapter.Course course);

    com.sap.sailing.domain.base.Mark getOrCreateMark(Serializable trackerID, String description);
    
    GPSFixMoving createGPSFix(TimePoint timePointOfTransmission, Fix fix);

    void updateCourseWaypoints(Course courseToUpdate, Iterable<Mark> marks) throws PatchFailedException;
    
    MarkPassing createMarkPassing(TimePoint timePoint, Waypoint waypoint, Competitor competitor);

    void removeRace(String raceID);

    RaceType getRaceTypeFromRaceID(String raceID);

    RaceTrackingConnectivityParameters createTrackingConnectivityParameters(String hostname, int port, String raceID,
            String raceName, String raceDescription, BoatClass boatClass, StartList startList,
            long delayToLiveInMillis, SwissTimingFactory swissTimingFactory, DomainFactory domainFactory,
            RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, boolean useInternalMarkPassingAlgorithm, boolean trackWind, boolean correctWindDirectionByMagneticDeclination, String updateURL, String updateUsername, String updatePassword);

    ControlPoint getOrCreateControlPoint(String description, Iterable<Serializable> deviceIds, MarkType markType);

    RaceDefinition createRaceDefinition(Regatta regatta, String swissTimingRaceID, Map<Competitor, Boat> competitorsAndBoats,
            List<ControlPoint> courseDefinition, String raceName, String raceIdForRaceDefinition);

    /**
     * Adds update handlers that forward events about a race, such as start time changes, course changes
     * or postponents, to an update URL using specific REST requests.
     */
    void addUpdateHandlers(String updateURL, String username, String password, Serializable eventId,
            RaceDefinition raceDefinition, DynamicTrackedRace trackedRace) throws URISyntaxException;
}
