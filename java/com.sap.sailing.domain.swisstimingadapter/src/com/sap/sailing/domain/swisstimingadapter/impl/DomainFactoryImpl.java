package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
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
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.CrewMember;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.Mark;
import com.sap.sailing.domain.swisstimingadapter.MessageType;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceType;
import com.sap.sailing.domain.swisstimingadapter.RaceType.OlympicRaceCode;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.impl.CourseDesignUpdateHandler;
import com.sap.sailing.domain.tracking.impl.FinishTimeUpdateHandler;
import com.sap.sailing.domain.tracking.impl.RaceAbortedHandler;
import com.sap.sailing.domain.tracking.impl.StartTimeUpdateHandler;
import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.util.impl.UUIDHelper;

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
    private final Map<Iterable<Serializable>, ControlPoint> controlPointCache;
    private final Map<String, RaceType> raceTypeByID;
    private final RaceType unknownRaceType;
    private final com.sap.sailing.domain.base.DomainFactory baseDomainFactory;
    
    public DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory baseDomainFactory) {
        this.baseDomainFactory = baseDomainFactory;
        raceIDToRegattaCache = new HashMap<String, Regatta>();
        controlPointCache = new HashMap<>();
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
    public Regatta getOrCreateDefaultRegatta(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            String raceID, BoatClass boatClass, TrackedRegattaRegistry trackedRegattaRegistry) {
        Regatta result = trackedRegattaRegistry.getRememberedRegattaForRace(raceID);
        if (result == null) {
            result = raceIDToRegattaCache.get(raceID);
        }
        if (result == null) {
            BoatClass regattaBoatClass = boatClass != null ? boatClass : getRaceTypeFromRaceID(raceID).getBoatClass();
            Calendar calendar = Calendar.getInstance();
            result = new RegattaImpl(raceLogStore, regattaLogStore, RegattaImpl.getDefaultName(
                    "ST Regatta " + calendar.get(Calendar.YEAR) + " for race " + raceID, regattaBoatClass.getName()),
                    regattaBoatClass, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                    /*startDate*/ null, /*endDate*/ null,
                    trackedRegattaRegistry, getBaseDomainFactory().createScoringScheme(
                            ScoringSchemeType.LOW_POINT),
                    raceID, null, /* registrationLinkSecret */ UUID.randomUUID().toString());
            logger.info("Created regatta "+result.getName()+" ("+result.hashCode()+")");
            raceIDToRegattaCache.put(raceID, result);
        }
        return result;
    }
    
    @Override
    public Pair<Competitor, Boat> createCompetitorWithID(com.sap.sailing.domain.swisstimingadapter.Competitor competitor, BoatClass boatClass, RaceTrackingHandler raceTrackHandler) {
        CompetitorAndBoatStore competitorAndBoatStore = baseDomainFactory.getCompetitorAndBoatStore();
        final Serializable competitorId = UUIDHelper.tryUuidConversion(competitor.getIdAsString());
        CompetitorWithBoat domainCompetitor = competitorAndBoatStore.getExistingCompetitorWithBoatById(competitorId);
        if (domainCompetitor == null || competitorAndBoatStore.isCompetitorToUpdateDuringGetOrCreate(domainCompetitor)) {
            List<DynamicPerson> teamMembers = new ArrayList<DynamicPerson>();
            if (competitor.getCrew().isEmpty()) {
                DynamicPerson dummyPerson = new PersonImpl(competitor.getName().trim(), getOrCreateNationality(competitor.getThreeLetterIOCCode()),
                        /* dateOfBirth */ null, /* description */ "Team");
                teamMembers.add(dummyPerson);
            } else {
                for (CrewMember crewMember: competitor.getCrew()) {
                	DynamicPerson person = new PersonImpl(crewMember.getName().trim(), getOrCreateNationality(crewMember.getNationality()),
                			/* dateOfBirth */ null, crewMember.getPosition());
                    teamMembers.add(person);
                }
            }
            DynamicTeam team = new TeamImpl(competitor.getName(), teamMembers, /* coach */ null);
            final DynamicBoat domainBoat = raceTrackHandler.getOrCreateBoat(competitorAndBoatStore, competitorId, /* name */ null, boatClass, competitor.getBoatID(), /* color */ null);
            domainCompetitor = raceTrackHandler.getOrCreateCompetitorWithBoat(competitorAndBoatStore, competitorId,
                    competitor.getName(), null /* shortName */, null /* displayColor */, null /* email */, null, team,
                    /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, domainBoat);
        }
        return new Pair<Competitor, Boat>(domainCompetitor, domainCompetitor.getBoat());
    }

    @Override
    public Pair<Competitor, Boat> createCompetitorWithoutID(
            com.sap.sailing.domain.swisstimingadapter.Competitor competitor, String raceId, BoatClass boatClass,
            RaceTrackingHandler raceTrackHandler) {
        CompetitorAndBoatStore competitorAndBoatStore = baseDomainFactory.getCompetitorAndBoatStore();
        List<DynamicPerson> teamMembers = new ArrayList<DynamicPerson>();
        for (String teamMemberName : competitor.getName().split("[-+&]")) {
            teamMembers.add(new PersonImpl(teamMemberName.trim(), getOrCreateNationality(competitor.getThreeLetterIOCCode()),
                    /* dateOfBirth */ null, teamMemberName.trim()));
        }
        DynamicTeam team = new TeamImpl(competitor.getName(), teamMembers, /* coach */ null);
        String competitorID = getCompetitorID(competitor.getBoatID(), competitor.getName(), raceId, boatClass);
        // TODO wouldn't the boat also need to be constructed using competitorAndBoatStore.getOrCreateBoat...?
        DynamicBoat domainBoat = new BoatImpl(UUID.randomUUID(), null, boatClass, competitor.getBoatID(), null);
        CompetitorWithBoat domainCompetitor = raceTrackHandler.getOrCreateCompetitorWithBoat(competitorAndBoatStore,
                competitorID,
                competitor.getName(), null /* short name */, null /*displayColor*/, null /*email*/, null, team,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, domainBoat);
        return new Pair<Competitor, Boat>(domainCompetitor, domainCompetitor.getBoat());
    }

    @Override
    public Pair<Competitor, Boat> createCompetitorWithoutID(String boatID, String threeLetterIOCCode, String name,
            String raceId, BoatClass boatClass, RaceTrackingHandler raceTrackingHandler) {
        return createCompetitorWithoutID(new CompetitorWithoutID(boatID, threeLetterIOCCode, name), raceId, boatClass,raceTrackingHandler);
    }

    private String getCompetitorID(String boatID, String name, String raceId, BoatClass boatClass) {
        String result = null;
        if (boatClass != null) {
            result = getCompetitorID(boatID, name, boatClass);
        } else {
            RaceType raceType = getRaceTypeFromRaceID(raceId);
            if (raceType != null) {
                result = getCompetitorID(boatID, name, raceType);
            }
        }
        return result;
    }

    private String getCompetitorID(String boatID, String name, BoatClass boatClass) {
        return getCompetitorID(boatID, boatClass) + "/" + name;
    }

    private String getCompetitorID(String boatID, String name, RaceType raceType) {
        return getCompetitorID(boatID, raceType) + "/" + name;
    }

    @Override
    public String getCompetitorID(String boatID, RaceType raceType) {
        return boatID + "/" + raceType.getRaceCode();
    }

    @Override
    public String getCompetitorID(String boatID, BoatClass boatClass) {
        return boatID + "/" + boatClass.getName();
    }

    @Override
    public RaceDefinition createRaceDefinition(Regatta regatta, String swissTimingRaceID, Map<Competitor, Boat> competitorsAndBoats,
            List<ControlPoint> courseDefinition, String raceName, String raceIdForRaceDefinition,
            RaceTrackingHandler raceTrackingHandler) {
        List<Waypoint> waypoints = new ArrayList<>();
        for (ControlPoint controlPoint : courseDefinition) {
            Waypoint waypoint = baseDomainFactory.createWaypoint(controlPoint, /* passingInstruction */ PassingInstruction.None);
            waypoints.add(waypoint);
        }
        com.sap.sailing.domain.base.Course domainCourse = new CourseImpl("Course", waypoints);
        BoatClass boatClass = getRaceTypeFromRaceID(swissTimingRaceID).getBoatClass();
        logger.info("Creating RaceDefinitionImpl for race "+swissTimingRaceID);
        RaceDefinition result = raceTrackingHandler.createRaceDefinition(regatta, raceName, domainCourse, boatClass,
                competitorsAndBoats, raceIdForRaceDefinition);
        regatta.addRace(result);
        return result;
    }

    @Override
    public RaceDefinition createRaceDefinition(Regatta regatta, Race race, StartList startList, Course course,
            RaceTrackingHandler raceTrackingHandler) {
        com.sap.sailing.domain.base.Course domainCourse = createCourse(race.getDescription(), course);
        Map<Competitor, Boat> competitorsAndBoats = createCompetitorsAndBoats(startList, race.getRaceID(),
                race.getBoatClass(), raceTrackingHandler);
        logger.info("Creating RaceDefinitionImpl for race "+race.getRaceID());
        BoatClass boatClass = race.getBoatClass() != null ? race.getBoatClass() : getRaceTypeFromRaceID(race.getRaceID()).getBoatClass();
        RaceDefinition result = raceTrackingHandler.createRaceDefinition(regatta, race.getRaceName(), domainCourse, boatClass, competitorsAndBoats, race.getRaceID());
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

    @Override
    public Map<Competitor, Boat> createCompetitorsAndBoats(StartList startList, String raceId, BoatClass boatClass,
            RaceTrackingHandler raceTrackHandler) {
        Map<Competitor, Boat> result = new LinkedHashMap<>();
        for (com.sap.sailing.domain.swisstimingadapter.Competitor swissTimingCompetitor : startList.getCompetitors()) {
            Pair<Competitor, Boat> domainCompetitorAndBoat;
            if (swissTimingCompetitor.getIdAsString() != null) {
                domainCompetitorAndBoat = createCompetitorWithID(swissTimingCompetitor, boatClass, raceTrackHandler);
            } else {
                domainCompetitorAndBoat = createCompetitorWithoutID(swissTimingCompetitor, raceId, boatClass,
                        raceTrackHandler);
            }
            result.put(domainCompetitorAndBoat.getA(), domainCompetitorAndBoat.getB());
        }
        return result;
    }

    private com.sap.sailing.domain.base.Course createCourse(String courseName, Course course) {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        for (Mark mark : course.getMarks()) {
            ControlPoint controlPoint = getOrCreateControlPoint(mark.getDescription(), mark.getDeviceIds(),
                    getMarkType(mark.getMarkType()), mark.getDescription());
            if (controlPoint != null) {
                Waypoint waypoint = baseDomainFactory.createWaypoint(controlPoint, /* passingInstruction */ PassingInstruction.None);
                waypoints.add(waypoint);
            }
        }
        com.sap.sailing.domain.base.Course result = new CourseImpl(courseName, waypoints);
        return result;
    }

    /**
     * Converts a mark type as defined by SwissTiming into a {@link MarkType} as defined by the domain model
     */
    private MarkType getMarkType(com.sap.sailing.domain.swisstimingadapter.Mark.MarkType markType) {
        final MarkType result;
        if (markType == null) {
            result = null;
        } else {
            result = MarkType.BUOY;
        }
        return result;
    }

    @Override
    public ControlPoint getOrCreateControlPoint(String description, Iterable<Serializable> deviceIds, MarkType markType,
            String shortNameOfPotentialGate) {
        ControlPoint result;
        synchronized (controlPointCache) {
            result = controlPointCache.get(deviceIds);
            if (result == null) {
                switch (Util.size(deviceIds)) {
                case 1:
                    result = getOrCreateMark(description, deviceIds.iterator().next(), markType);
                    break;
                case 2:
                    Iterator<Serializable> markNameIter = deviceIds.iterator();
                    final Serializable idLeft = markNameIter.next();
                    final Serializable idRight = markNameIter.next();
                    result = baseDomainFactory.createControlPointWithTwoMarks(getOrCreateMark(idLeft, description),
                            getOrCreateMark(idRight, description), description, shortNameOfPotentialGate);
                    break;
                default:
                    logger.info("Ignoring mark "+description+" because it doesn't have any devices assigned");
                }
                if (result != null) {
                    controlPointCache.put(deviceIds, result);
                }
            }
        }
        return result;
    }

    private ControlPoint getOrCreateMark(String description, Serializable id, MarkType markType) {
        return baseDomainFactory.getOrCreateMark(id, description, markType);
    }

    /**
     * @param trackerId
     *            the "device name" and the "sail number" in case of an {@link MessageType#RPD RPD} message, used as the mark's
     *            {@link Named#getName() name} and {@link WithID#getId() ID}.
     */
    @Override
    public com.sap.sailing.domain.base.Mark getOrCreateMark(Serializable trackerId, String description) {
        return baseDomainFactory.getOrCreateMark(trackerId, description, /* no short name available */ description);
    }

    @Override
    public GPSFixMoving createGPSFix(TimePoint timePointOfTransmission, Fix fix) {
        GPSFixMoving result = new GPSFixMovingImpl(fix.getPosition(), new MillisecondsTimePoint(
                timePointOfTransmission.asMillis() + fix.getAgeOfDataInMilliseconds()), fix.getSpeed(), /* optionalTrueHeading */ null);
        return result;
    }
    
    @Override
    public void updateCourseWaypoints(com.sap.sailing.domain.base.Course courseToUpdate, Iterable<Mark> marks) throws PatchFailedException {
        List<com.sap.sse.common.Util.Pair<com.sap.sailing.domain.base.ControlPoint, PassingInstruction>> newDomainControlPoints = new ArrayList<com.sap.sse.common.Util.Pair<com.sap.sailing.domain.base.ControlPoint, PassingInstruction>>();
        for (Mark mark : marks) {
            // TODO bug 1043: propagate the mark names to the waypoint names
            com.sap.sailing.domain.base.ControlPoint domainControlPoint = getOrCreateControlPoint(mark.getDescription(),
                    mark.getDeviceIds(), getMarkType(mark.getMarkType()), mark.getDescription());
            if (domainControlPoint != null) {
                newDomainControlPoints.add(new com.sap.sse.common.Util.Pair<>(domainControlPoint, PassingInstruction.None));
            }
        }
        courseToUpdate.update(newDomainControlPoints, courseToUpdate.getAssociatedRoles(),
                courseToUpdate.getOriginatingCourseTemplateIdOrNull(), baseDomainFactory);
    }

    @Override
    public MarkPassing createMarkPassing(TimePoint timePoint, Waypoint waypoint, com.sap.sailing.domain.base.Competitor competitor) {
        return baseDomainFactory.createMarkPassing(timePoint, waypoint, competitor);
    }

    @Override
    public Nationality getOrCreateNationality(String threeLetterIOCCode) {
        return baseDomainFactory.getOrCreateNationality(threeLetterIOCCode);
    }

    @Override
    public RaceTrackingConnectivityParameters createTrackingConnectivityParameters(String hostname, int port,
            String raceID, String raceName, String raceDescription, BoatClass boatClass, StartList startList,
            long delayToLiveInMillis, SwissTimingFactory swissTimingFactory, DomainFactory domainFactory,
            RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, boolean useInternalMarkPassingAlgorithm,
            boolean trackWind, boolean correctWindDirectionByMagneticDeclination, String updateURL,
            String apiToken, String eventName, String manage2SailEventUrl) {
        return new SwissTimingTrackingConnectivityParameters(hostname, port, raceID, raceName, raceDescription,
                boatClass, startList, delayToLiveInMillis, swissTimingFactory, domainFactory, raceLogStore,
                regattaLogStore, useInternalMarkPassingAlgorithm, trackWind, correctWindDirectionByMagneticDeclination,
                updateURL, apiToken, eventName, manage2SailEventUrl);
    }

    @Override
    public void addUpdateHandlers(String updateURL, String apiToken, Serializable eventId,
            RaceDefinition raceDefinition, DynamicTrackedRace trackedRace) throws URISyntaxException {
        final URI updateURI = updateURL == null ? null : new URI(updateURL);
        CourseDesignUpdateHandler courseDesignHandler = new CourseDesignUpdateHandler(
                updateURI, apiToken, eventId, raceDefinition.getId());
        StartTimeUpdateHandler startTimeHandler = new StartTimeUpdateHandler(
                updateURI, apiToken, eventId,
                raceDefinition.getId(), trackedRace.getTrackedRegatta().getRegatta());
        RaceAbortedHandler raceAbortedHandler = new RaceAbortedHandler(
                updateURI, apiToken, eventId,
                raceDefinition.getId());
        final FinishTimeUpdateHandler finishTimeUpdateHandler = new FinishTimeUpdateHandler(updateURI, apiToken, eventId,
                raceDefinition.getId(), trackedRace.getTrackedRegatta().getRegatta());
        baseDomainFactory.addUpdateHandlers(trackedRace, courseDesignHandler, startTimeHandler, raceAbortedHandler,
                finishTimeUpdateHandler);
    }
}
