package com.sap.sailing.domain.swisstimingadapter.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicPerson;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.Named;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WithID;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.Mark;
import com.sap.sailing.domain.swisstimingadapter.MessageType;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceSpecificMessageLoader;
import com.sap.sailing.domain.swisstimingadapter.RaceType;
import com.sap.sailing.domain.swisstimingadapter.RaceType.OlympicRaceCode;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;

import difflib.PatchFailedException;

/**
 * {@link RaceDefinition} objects created by this factory are created using the SwissTiming "Race ID"
 * as the {@link RaceDefinition#getName() race name}. This at the same time defines the name of the
 * single {@link Regatta} created per {@link RaceDefinition}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class DomainFactoryImpl implements DomainFactory {
    private final static Logger logger = Logger.getLogger(DomainFactoryImpl.class.getName());
    private final Map<String, Regatta> raceIDToRegattaCache;
    private final Map<Iterable<String>, ControlPoint> controlPointCache;
    private final Map<String, RaceType> raceTypeByID;
    private final RaceType unknownRaceType;
    private final com.sap.sailing.domain.base.DomainFactory baseDomainFactory;
    
    public DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory baseDomainFactory) {
        this.baseDomainFactory = baseDomainFactory;
        raceIDToRegattaCache = new HashMap<String, Regatta>();
        controlPointCache = new HashMap<Iterable<String>, ControlPoint>();
        raceTypeByID = new HashMap<String, RaceType>();
        
        for (OlympicRaceCode olympicRaceCode : OlympicRaceCode.values()) {
            raceTypeByID.put(
                    olympicRaceCode.swissTimingCode,
                    new RaceTypeImpl(olympicRaceCode, baseDomainFactory.getOrCreateBoatClass(
                            olympicRaceCode.boatClassName, olympicRaceCode.typicallyStartsUpwind)));
        }
        unknownRaceType = new RaceTypeImpl(OlympicRaceCode.UNKNOWN, baseDomainFactory.getOrCreateBoatClass("Unknown", OlympicRaceCode.UNKNOWN.typicallyStartsUpwind));
    }

    @Override
    public com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory() {
        return baseDomainFactory;
    }
    
    @Override
    public Regatta getOrCreateDefaultRegatta(RaceLogStore raceLogStore, String raceID, TrackedRegattaRegistry trackedRegattaRegistry) {
        Regatta result = trackedRegattaRegistry.getRememberedRegattaForRace(raceID);
        if (result == null) {
            result = raceIDToRegattaCache.get(raceID);
        }
        if (result == null) {
            result = new RegattaImpl(raceLogStore, raceID, getRaceTypeFromRaceID(raceID).getBoatClass(), trackedRegattaRegistry,
                    getBaseDomainFactory().createScoringScheme(ScoringSchemeType.LOW_POINT), raceID, null);
            logger.info("Created regatta "+result.getName()+" ("+result.hashCode()+")");
            raceIDToRegattaCache.put(raceID, result);
        }
        return result;
    }
    
    @Override
    public Competitor getCompetitorByBoatIDAndRaceType(String boatID, RaceType raceType) {
        return baseDomainFactory.getExistingCompetitorById(getCompetitorID(boatID, raceType));
    }

    @Override
    public Competitor getCompetitorByBoatIDAndRaceID(String boatID, String raceID) {
        RaceType raceType = getRaceTypeFromRaceID(raceID);
        if (raceType != null) {
            return getCompetitorByBoatIDAndRaceType(boatID, raceType);
        } else {
            return null;
        }
    }

    private String getCompetitorID(String boatID, RaceType raceType) {
        return boatID + "/" + raceType.getRaceCode();
    }
    
    @Override
    public Competitor getOrCreateCompetitor(com.sap.sailing.domain.swisstimingadapter.Competitor competitor, RaceType raceType) {
        Competitor result = getCompetitorByBoatIDAndRaceType(competitor.getBoatID(), raceType);
        CompetitorStore competitorStore = baseDomainFactory.getCompetitorStore();
        if (result == null || competitorStore.isCompetitorToUpdateDuringGetOrCreate(result)) {
            DynamicBoat boat = new BoatImpl(competitor.getName(), raceType.getBoatClass(), competitor.getBoatID());
            List<DynamicPerson> teamMembers = new ArrayList<DynamicPerson>();
            for (String teamMemberName : competitor.getName().split("[-+&]")) {
                teamMembers.add(new PersonImpl(teamMemberName.trim(), getOrCreateNationality(competitor.getThreeLetterIOCCode()),
                        /* dateOfBirth */ null, teamMemberName.trim()));
            }
            DynamicTeam team = new TeamImpl(competitor.getName(), teamMembers, /* coach */ null);
            result = competitorStore.getOrCreateCompetitor(getCompetitorID(competitor.getBoatID(), raceType),
                    competitor.getName(), null /*displayColor*/, team, boat);
        }
        return result;
    }

    @Override
    public Competitor getOrCreateCompetitor(String boatID, String threeLetterIOCCode, String name, 
            RaceType raceType) {
        return getOrCreateCompetitor(new CompetitorImpl(boatID, threeLetterIOCCode, name), raceType);
    }

    @Override
    public RaceDefinition createRaceDefinition(Regatta regatta, String raceID, Iterable<Competitor> competitors,
            List<ControlPoint> courseDefinition) {
        List<Waypoint> waypoints = new ArrayList<>();
        for (ControlPoint controlPoint : courseDefinition) {
            Waypoint waypoint = baseDomainFactory.createWaypoint(controlPoint, /* passingSide */ null);
            waypoints.add(waypoint);
        }
        com.sap.sailing.domain.base.Course domainCourse = new CourseImpl("Course", waypoints);
        BoatClass boatClass = getRaceTypeFromRaceID(raceID).getBoatClass();
        logger.info("Creating RaceDefinitionImpl for race "+raceID);
        RaceDefinition result = new RaceDefinitionImpl(regatta.getId().toString()+"/"+raceID, domainCourse, boatClass, competitors);
        regatta.addRace(result);
        return result;
    }

    @Override
    public RaceDefinition createRaceDefinition(Regatta regatta, Race race, StartList startList, Course course) {
        com.sap.sailing.domain.base.Course domainCourse = createCourse(race.getDescription(), course);
        RaceType raceType = getRaceTypeFromRaceID(race.getRaceID());
        Iterable<Competitor> competitors = createCompetitorList(startList, raceType);
        logger.info("Creating RaceDefinitionImpl for race "+race.getRaceID());
        RaceDefinition result = new RaceDefinitionImpl(race.getRaceID(), domainCourse,
                raceType.getBoatClass(), competitors);
        regatta.addRace(result);
        return result;
    }

    /**
     * Returns the SwissTiming olympic race type or race type "UNKNOWN" when no corresponding olympic race type can be found.
     * Will never return <code>null</code> neither throw a content-related exception.<p>
     * 
     * The <code>raceID</code> format from which the race type can be inferred either has to be of the form<pre>
     * 
     * DDGEEEPUU
     *
     *  D = discipline - in this case sailing or SA
     *  G = gender - M, F or X
     *  E = event - a three digit code for each event (known as class in sailing)
     *  P = phase - 1 being the grand final or medal working back to 9 as the first qualification step, generally sailing only uses 1 and 9 but we are investigating using 8 depending on the exact competition format
     *  U = event unit - race 1 through to 99 in the phase 
     * </pre>
     * 
     * for example "SAW005901" or of the form <pre>
     * CCCRRRNYYYY;DDGEEEPUU
     * 
     *  C = City
     *  R = Regatta
     *  N = Regatta Number
     *  Y = Year
     * </pre>
     * 
     * which allows for a globally-unique race ID as it includes a specification of the event / regatta at which the race
     * took place.
     */
    @Override
    public RaceType getRaceTypeFromRaceID(String raceID) {
        final RaceType result;
        if (raceID != null && raceID.length() >= 6) {
            final String[] optionalEventIDAndMandatoryRaceID = raceID.split("_");
            final String swissTimingRaceCode = optionalEventIDAndMandatoryRaceID[optionalEventIDAndMandatoryRaceID.length-1].substring(0, 6).toUpperCase();
            RaceType raceType = raceTypeByID.get(swissTimingRaceCode);
            if (raceType == null) {
                result = unknownRaceType;
            } else {
                result = raceType;
            }
        } else {
            result = unknownRaceType;
        }
        return result;
    }

    private Iterable<Competitor> createCompetitorList(StartList startList, RaceType raceType) {
        List<Competitor> result = new ArrayList<Competitor>();
        for (com.sap.sailing.domain.swisstimingadapter.Competitor swissTimingCompetitor : startList.getCompetitors()) {
            Competitor domainCompetitor = getOrCreateCompetitor(swissTimingCompetitor, raceType);
            result.add(domainCompetitor);
        }
        return result;
    }

    private com.sap.sailing.domain.base.Course createCourse(String courseName, Course course) {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        for (Mark mark : course.getMarks()) {
            ControlPoint controlPoint = getOrCreateControlPoint(mark.getDevices());
            Waypoint waypoint = baseDomainFactory.createWaypoint(controlPoint, /* passingSide */ null);
            waypoints.add(waypoint);
        }
        com.sap.sailing.domain.base.Course result = new CourseImpl(courseName, waypoints);
        return result;
    }

    @Override
    public ControlPoint getOrCreateControlPoint(Iterable<String> devices) {
        ControlPoint result;
        synchronized (controlPointCache) {
            result = controlPointCache.get(devices);
            if (result == null) {
                switch (Util.size(devices)) {
                case 1:
                    result = getOrCreateMark(devices.iterator().next());
                    break;
                case 2:
                    Iterator<String> markNameIter = devices.iterator();
                    String left = markNameIter.next();
                    String right = markNameIter.next();
                    result = baseDomainFactory.createGate(getOrCreateMark(left), getOrCreateMark(right), left + "/" + right);
                    break;
                default:
                    throw new RuntimeException(
                            "Don't know how to handle control points with number of devices neither 1 nor 2. Was "
                                    + Util.size(devices));
                }
                controlPointCache.put(devices, result);
            }
        }
        return result;
    }

    /**
     * @param trackerID
     *            the "device name" and the "sail number" in case of an {@link MessageType#RPD RPD} message, used as the mark's
     *            {@link Named#getName() name} and {@link WithID#getId() ID}.
     */
    @Override
    public com.sap.sailing.domain.base.Mark getOrCreateMark(String trackerID) {
        return baseDomainFactory.getOrCreateMark(trackerID);
    }

    @Override
    public GPSFixMoving createGPSFix(TimePoint timePointOfTransmission, Fix fix) {
        GPSFixMoving result = new GPSFixMovingImpl(fix.getPosition(), new MillisecondsTimePoint(
                timePointOfTransmission.asMillis() + fix.getAgeOfDataInMilliseconds()), fix.getSpeed());
        return result;
    }
    
    @Override
    public void updateCourseWaypoints(com.sap.sailing.domain.base.Course courseToUpdate, Iterable<Mark> marks) throws PatchFailedException {
        List<Pair<com.sap.sailing.domain.base.ControlPoint, NauticalSide>> newDomainControlPoints = new ArrayList<Pair<com.sap.sailing.domain.base.ControlPoint, NauticalSide>>();
        for (Mark mark : marks) {
            // TODO bug 1043: propagate the mark names to the waypoint names
            com.sap.sailing.domain.base.ControlPoint domainControlPoint = getOrCreateControlPoint(mark.getDevices());
            newDomainControlPoints.add(new Pair<com.sap.sailing.domain.base.ControlPoint, NauticalSide>(domainControlPoint, null));
        }
        courseToUpdate.update(newDomainControlPoints, baseDomainFactory);
    }

    @Override
    public MarkPassing createMarkPassing(TimePoint timePoint, Waypoint waypoint, com.sap.sailing.domain.base.Competitor competitor) {
        return baseDomainFactory.createMarkPassing(timePoint, waypoint, competitor);
    }

    @Override
    public void removeRace(String raceID) {
        Regatta regatta = raceIDToRegattaCache.get(raceID);
        if (regatta != null) {
            Set<RaceDefinition> toRemove = new HashSet<RaceDefinition>();
            for (RaceDefinition race : regatta.getAllRaces()) {
                if (race.getName().equals(raceID)) {
                    toRemove.add(race);
                }
            }
            for (RaceDefinition raceToRemove : toRemove) {
                regatta.removeRace(raceToRemove);
            }
            if (Util.isEmpty(regatta.getAllRaces())) {
                raceIDToRegattaCache.remove(raceID);
            }
        }
    }

    @Override
    public Nationality getOrCreateNationality(String threeLetterIOCCode) {
        return baseDomainFactory.getOrCreateNationality(threeLetterIOCCode);
    }

    @Override
    public RaceTrackingConnectivityParameters createTrackingConnectivityParameters(String hostname, int port, String raceID,
            boolean canSendRequests, long delayToLiveInMillis,
            SwissTimingFactory swissTimingFactory, DomainFactory domainFactory, RaceLogStore raceLogStore,
            RaceSpecificMessageLoader messageLoader) {
        return new SwissTimingTrackingConnectivityParameters(hostname, port, raceID, canSendRequests, delayToLiveInMillis, 
                swissTimingFactory, domainFactory, raceLogStore, messageLoader);
    }

}
