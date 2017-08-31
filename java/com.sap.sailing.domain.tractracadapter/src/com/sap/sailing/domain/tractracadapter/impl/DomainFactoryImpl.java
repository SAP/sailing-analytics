package com.sap.sailing.domain.tractracadapter.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicPerson;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SidelineImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.JSONService;
import com.sap.sailing.domain.tractracadapter.MetadataParser;
import com.sap.sailing.domain.tractracadapter.MetadataParser.BoatMetaData;
import com.sap.sailing.domain.tractracadapter.MetadataParser.ControlPointMetaData;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sailing.domain.tractracadapter.TracTracControlPoint;
import com.sap.sailing.domain.tractracadapter.TracTracRaceTracker;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.WeakIdentityHashMap;
import com.sap.sse.util.WeakValueCache;
import com.tractrac.model.lib.api.data.IPosition;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.model.lib.api.event.ICompetitor;
import com.tractrac.model.lib.api.event.ICompetitorClass;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.model.lib.api.route.IControl;
import com.tractrac.model.lib.api.route.IControlPoint;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;

import difflib.PatchFailedException;

public class DomainFactoryImpl implements DomainFactory {
    private static final Logger logger = Logger.getLogger(DomainFactoryImpl.class.getName());
    
    private final com.sap.sailing.domain.base.DomainFactory baseDomainFactory;
    
    private final WeakValueCache<TracTracControlPoint, com.sap.sailing.domain.base.ControlPoint> controlPointCache = new WeakValueCache<>(new HashMap<>());
    
    private final Map<com.sap.sse.common.Util.Pair<String, UUID>, DynamicPerson> personCache = new HashMap<>();
    
    /**
     * Caches regattas by their name and their boat class's name
     */
    private final WeakValueCache<com.sap.sse.common.Util.Pair<String, String>, com.sap.sailing.domain.base.Regatta> regattaCache = new WeakValueCache<>(new HashMap<>());
    
    /**
     * A cache based on weak references to the TracTrac race, allowing for quick race lookup as long as the
     * TracTrac race remains referenced. Synchronization for additions / removals is tied to the
     * synchronization for {@link #regattaCache}.
     */
    private final WeakIdentityHashMap<IRace, Regatta> weakDefaultRegattaCache = new WeakIdentityHashMap<>();
    
    /**
     * Maps from the TracTrac race UUIDs to the domain model's {@link RaceDefinition} objects that represent the race
     * identified by that UUID
     */
    private final WeakValueCache<UUID, RaceDefinition> raceCache = new WeakValueCache<>(new ConcurrentHashMap<>());
    
    private final MetadataParser metadataParser;

    public DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory baseDomainFactory) {
        this.baseDomainFactory = baseDomainFactory;
        this.metadataParser = new MetadataParserImpl();
    }
    
    @Override
    public MetadataParser getMetadataParser() {
        return metadataParser;
    }
    
    @Override
    public com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory() {
        return baseDomainFactory;
    }
    
    @Override
    public Position createPosition(
            IPosition position) {
        return new DegreePosition(position.getLatitude(), position.getLongitude());
    }
   
    @Override
    public GPSFixMoving createGPSFixMoving(IPosition position) {
        GPSFixMoving result = new GPSFixMovingImpl(createPosition(position), new MillisecondsTimePoint(position.getTimestamp()),
                new KilometersPerHourSpeedWithBearingImpl(position.getSpeed(), new DegreeBearingImpl(position.getDirection())));
        return result;
    }
    
    @Override
    public TimePoint createTimePoint(long timestamp) {
        return new MillisecondsTimePoint(timestamp);
    }
    
    @Override
    public void updateCourseWaypoints(Course courseToUpdate, Iterable<com.sap.sse.common.Util.Pair<TracTracControlPoint, PassingInstruction>> controlPoints) throws PatchFailedException {
        List<com.sap.sse.common.Util.Pair<com.sap.sailing.domain.base.ControlPoint, PassingInstruction>> newDomainControlPoints = new ArrayList<>();
        for (com.sap.sse.common.Util.Pair<TracTracControlPoint, PassingInstruction> tractracControlPoint : controlPoints) {
            com.sap.sailing.domain.base.ControlPoint newDomainControlPoint = getOrCreateControlPoint(tractracControlPoint.getA());
            newDomainControlPoints.add(new com.sap.sse.common.Util.Pair<com.sap.sailing.domain.base.ControlPoint, PassingInstruction>(newDomainControlPoint, tractracControlPoint.getB()));
        }
        courseToUpdate.update(newDomainControlPoints, baseDomainFactory);
    }

    @Override
    public List<Sideline> createSidelines(final String raceMetadataString, final Iterable<? extends TracTracControlPoint> allEventControlPoints) {
        List<Sideline> sidelines = new ArrayList<Sideline>();
        Map<String, Iterable<TracTracControlPoint>> sidelinesMetadata = getMetadataParser().parseSidelinesFromRaceMetadata(
                raceMetadataString, allEventControlPoints);
        for (Entry<String, Iterable<TracTracControlPoint>> sidelineEntry : sidelinesMetadata.entrySet()) {
            if (Util.size(sidelineEntry.getValue()) > 0) {
                sidelines.add(createSideline(sidelineEntry.getKey(), sidelineEntry.getValue()));
            }
        }
        return sidelines;
    }
    
    public com.sap.sailing.domain.base.ControlPoint getOrCreateControlPoint(TracTracControlPoint controlPoint) {
        synchronized (controlPointCache) {
            com.sap.sailing.domain.base.ControlPoint domainControlPoint = controlPointCache.get(controlPoint);
            if (domainControlPoint == null) {
                final Iterable<MetadataParser.ControlPointMetaData> controlPointMetadata = getMetadataParser().parseControlPointMetadata(controlPoint);
                List<Mark> marks = new ArrayList<Mark>();
                for (ControlPointMetaData markMetadata : controlPointMetadata) {
                    Mark mark = baseDomainFactory.getOrCreateMark(markMetadata.getId(), markMetadata.getName(),
                            markMetadata.getType(), markMetadata.getColor(), markMetadata.getShape(),
                            markMetadata.getPattern());
                    marks.add(mark);
                }
                if (controlPoint.getHasTwoPoints()) {
                    // it's a gate
                    Iterator<Mark> markIter = marks.iterator();
                    Mark mark1 = markIter.next();
                    Mark mark2 = markIter.next();
                    domainControlPoint = baseDomainFactory.createControlPointWithTwoMarks(controlPoint.getId(), mark1, mark2, controlPoint.getName());
                } else {
                    Mark mark = marks.iterator().next();
                    domainControlPoint = mark;
                }
                controlPointCache.put(controlPoint, domainControlPoint);
            }
            return domainControlPoint;
        }
    }

    @Override
    public Course createCourse(String name, Iterable<com.sap.sse.common.Util.Pair<TracTracControlPoint, PassingInstruction>> controlPoints) {
        List<Waypoint> waypointList = new ArrayList<Waypoint>();
        for (com.sap.sse.common.Util.Pair<TracTracControlPoint, PassingInstruction> controlPoint : controlPoints) {
            Waypoint waypoint = baseDomainFactory.createWaypoint(getOrCreateControlPoint(controlPoint.getA()), controlPoint.getB());
            waypointList.add(waypoint);
        }
        return new CourseImpl(name, waypointList);
    }

    @Override
    public Sideline createSideline(String name, Iterable<TracTracControlPoint> controlPoints) {
        List<Mark> marks = new ArrayList<Mark>();
        for (TracTracControlPoint controlPoint : controlPoints) {
            ControlPoint cp = getOrCreateControlPoint(controlPoint);
            for (Mark mark : cp.getMarks()) {
                marks.add(mark);
            }
        }
        return new SidelineImpl(name, marks);
    }

    @Override
    public Competitor resolveCompetitor(ICompetitor competitor) {
        Competitor result = null;
        CompetitorStore competitorStore = baseDomainFactory.getCompetitorStore();
        result = competitorStore.getExistingCompetitorById(competitor.getId());
        if (result == null) {
            result = competitorStore.getExistingCompetitorWithBoatById(competitor.getId());
        }
        return result;
    }

    private Competitor getOrCreateCompetitor(ICompetitor competitor) {
        Competitor result = getOrCreateCompetitor(competitor.getId(), competitor.getNationality(), competitor.getName(),
                competitor.getShortName(), competitor.getHandicapToT(), competitor.getHandicapToD(), null);
        return result;
    }

    private CompetitorWithBoat getOrCreateCompetitorWithBoat(ICompetitor competitor) {
        final String competitorClassName = competitor.getCompetitorClass()==null?null:competitor.getCompetitorClass().getName();
        CompetitorWithBoat result = getOrCreateCompetitorWithBoat(competitor.getId(), competitor.getNationality(), competitor.getName(),
                competitor.getShortName(), competitor.getHandicapToT(), competitor.getHandicapToD(), null, competitorClassName);
        return result;
    }

    private CompetitorWithBoat getOrCreateCompetitorWithBoat(final UUID competitorId,
            final String nationalityAsString, final String name, final String shortName, float timeOnTimeFactor,
            float timeOnDistanceAllowanceInSecondsPerNauticalMile, String searchTag, String competitorClassName) {
        CompetitorStore competitorStore = baseDomainFactory.getCompetitorStore();
        CompetitorWithBoat domainCompetitor = competitorStore.getExistingCompetitorWithBoatById(competitorId);
        if (domainCompetitor == null || competitorStore.isCompetitorToUpdateDuringGetOrCreate(domainCompetitor)) {
            BoatClass boatClass = getOrCreateBoatClass(competitorClassName);
            Nationality nationality;
            try {
                nationality = getOrCreateNationality(nationalityAsString);
            } catch (IllegalArgumentException iae) {
                // the country code was probably not a legal IOC country code
                nationality = null;
                logger.log(Level.SEVERE, "Unknown nationality "+nationalityAsString+" for competitor "+name+"; leaving null", iae);
            }
            DynamicTeam team = createTeam(name, nationality, competitorId);
            DynamicBoat boat = new BoatImpl(UUID.randomUUID(), shortName, boatClass, shortName);
            domainCompetitor = competitorStore.getOrCreateCompetitorWithBoat(competitorId, name, shortName, null /* displayColor */,
                    null /* email */, null /* flagImag */, team, (double) timeOnTimeFactor,
                    new MillisecondsDurationImpl((long) (timeOnDistanceAllowanceInSecondsPerNauticalMile*1000)), searchTag, (DynamicBoat) boat);
        }
        return domainCompetitor;
    }

    private Competitor getOrCreateCompetitor(final UUID competitorId, final String nationalityAsString, 
            final String name, final String shortName, float timeOnTimeFactor,
            float timeOnDistanceAllowanceInSecondsPerNauticalMile, String searchTag) {
        CompetitorStore competitorStore = baseDomainFactory.getCompetitorStore();
        Competitor domainCompetitor = competitorStore.getExistingCompetitorById(competitorId);
        if (domainCompetitor == null || competitorStore.isCompetitorToUpdateDuringGetOrCreate(domainCompetitor)) {
            Nationality nationality;
            try {
                nationality = getOrCreateNationality(nationalityAsString);
            } catch (IllegalArgumentException iae) {
                // the country code was probably not a legal IOC country code
                nationality = null;
                logger.log(Level.SEVERE, "Unknown nationality "+nationalityAsString+" for competitor "+name+"; leaving null", iae);
            }
            DynamicTeam team = createTeam(name, nationality, competitorId);
            domainCompetitor = competitorStore.getOrCreateCompetitor(competitorId, name, shortName, null /* displayColor */,
                    null /* email */, null /* flagImag */, team, (double) timeOnTimeFactor,
                    new MillisecondsDurationImpl((long) (timeOnDistanceAllowanceInSecondsPerNauticalMile*1000)), searchTag);
        }
        return domainCompetitor;
    }

    public Boat getOrCreateBoat(Serializable boatId, String boatName, BoatClass boatClass, String sailId, Color boatColor) {
        CompetitorStore competitorStore = baseDomainFactory.getCompetitorStore();
        Boat domainBoat = competitorStore.getExistingBoatById(boatId);
        if (domainBoat == null) {
            domainBoat = baseDomainFactory.getCompetitorStore().getOrCreateBoat(boatId, boatName, boatClass, sailId, boatColor);
        }
        return domainBoat;
    }

    private DynamicTeam createTeam(String name, Nationality nationality, UUID competitorId) {
        DynamicTeam result;
        String[] sailorNames = name.split("\\b*\\+\\b*");
        List<DynamicPerson> sailors = new ArrayList<DynamicPerson>();
        for (String sailorName : sailorNames) {
            sailors.add(getOrCreatePerson(sailorName.trim(), nationality, competitorId));
        }
        result = new TeamImpl(name, sailors, /* TODO coach not known */null);
        return result;
    }

    @Override
    public DynamicPerson getOrCreatePerson(String name, Nationality nationality, UUID competitorId) {
        synchronized (personCache) {
            com.sap.sse.common.Util.Pair<String, UUID> key = new com.sap.sse.common.Util.Pair<String, UUID>(name, competitorId);
            DynamicPerson result = personCache.get(key);
            if (result == null) {
                result = new PersonImpl(name, nationality, /* date of birth unknown */null, /* description */"");
                personCache.put(key, result);
            }
            return result;
        }
    }

    @Override
    public BoatClass getOrCreateBoatClass(String competitorClassName) {
        return baseDomainFactory.getOrCreateBoatClass(competitorClassName == null ? "" : competitorClassName);
    }

    @Override
    public Nationality getOrCreateNationality(String nationalityName) {
        return baseDomainFactory.getOrCreateNationality(nationalityName);
    }
    
    @Override
    public RaceDefinition getExistingRaceDefinitionForRace(UUID raceId) {
        return raceCache.get(raceId);
    }

    @Override
    public RaceDefinition getAndWaitForRaceDefinition(UUID raceId) {
        return getAndWaitForRaceDefinition(raceId, -1);
    }

    @Override
    public RaceDefinition getAndWaitForRaceDefinition(UUID raceId, long timeoutInMilliseconds) {
        long start = System.currentTimeMillis();
        RaceDefinition result = raceCache.get(raceId);
        boolean interrupted = false;
        if (result == null) {
            synchronized (raceCache) {
                // try again while under raceCache's monitor; otherwise we may miss a notification
                result = raceCache.get(raceId);
                while ((timeoutInMilliseconds == -1 || System.currentTimeMillis()-start < timeoutInMilliseconds) && !interrupted && result == null) {
                    try {
                        if (timeoutInMilliseconds == -1) {
                            raceCache.wait();
                        } else {
                            long timeToWait = timeoutInMilliseconds-(System.currentTimeMillis()-start);
                            if (timeToWait > 0) {
                                raceCache.wait(timeToWait);
                            }
                        }
                        result = raceCache.get(raceId);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Regatta getOrCreateDefaultRegatta(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            IRace race, TrackedRegattaRegistry trackedRegattaRegistry) {
        synchronized (regattaCache) {
            // FIXME Dialog with Lasse by Skype on 2011-06-17:
            //            [6:20:04 PM] Axel Uhl: Lasse, can Event.getCompetitorClassList() ever produce more than one result?
            //            [6:20:20 PM] Axel Uhl: Or is it similar to Event.getRaceList() which always delivers one Race?
            //            [6:22:19 PM] Lasse Staffensen: It can deliver several classes, if more classes are present in a race.
            //            [6:27:20 PM] Axel Uhl: Will that happen at Kiel Week?
            //            [6:27:58 PM] Lasse Staffensen: No
            //            [6:28:34 PM] Axel Uhl: Good :)
            // This means that currently it is permissible to assume that we'll get at most one
            // boat class per TracTrac event. Generally, however, we have to assume that
            // one TracTrac event may map to multiple domain Event objects with one BoatClass each
            
            // try a quick look-up in the weak cache using the TracTrac event as key; only if that delivers no result,
            // compute the dominant boat class which requires a lot more effort
            Regatta result = weakDefaultRegattaCache.get(race);
            if (result == null) {
                Pair<String, BoatClass> defaultRegattaNameAndBoatClass = getDefaultRegattaNameAndBoatClass(race);
                BoatClass boatClass = defaultRegattaNameAndBoatClass.getB();
                Pair<String, String> key = new Pair<String, String>(defaultRegattaNameAndBoatClass.getA(),
                        boatClass == null ? null : boatClass.getName());
                result = regattaCache.get(key);
                // FIXME When a Regatta is removed from RacingEventService, it isn't removed here. We use a "stale" regatta here.
                // This is particularly bad if a persistent regatta was loaded but a default regatta was accidentally created.
                // Then, there is no way but restart the server to get rid of this stale cache entry here.
                if (result == null) {
                    result = new RegattaImpl(raceLogStore, regattaLogStore, RegattaImpl.getDefaultName(
                            defaultRegattaNameAndBoatClass.getA(), boatClass.getName()), boatClass, 
                            /* canBoatsOfCompetitorsChangePerRace */ false, /*startDate*/ null, /*endDate*/ null,
                            trackedRegattaRegistry,
                            // use the low-point system as the default scoring scheme
                            getBaseDomainFactory().createScoringScheme(ScoringSchemeType.LOW_POINT), race.getId(), null);
                    regattaCache.put(key, result);
                    weakDefaultRegattaCache.put(race, result);
                    logger.info("Created regatta "+result.getName()+" ("+result.hashCode()+") because none found for key "+key);
                }
            }
            return result;
        }
    }

    private Pair<String, BoatClass> getDefaultRegattaNameAndBoatClass(IRace race) {
        Collection<ICompetitorClass> competitorClassList = new ArrayList<>();
        getCompetingCompetitors(race).forEach(competitor->{
            competitorClassList.add(competitor.getCompetitor().getCompetitorClass());
        });
        Pair<String, BoatClass> defaultRegattaNameAndBoatClass = new Pair<String, BoatClass>(race.getEvent().getName(),
                getDominantBoatClass(competitorClassList));
        return defaultRegattaNameAndBoatClass;
    }
    
    @Override
    public Iterable<Receiver> getUpdateReceivers(DynamicTrackedRegatta trackedRegatta, IRace tractracRace,
            WindStore windStore, long delayToLiveInMillis, Simulator simulator, DynamicRaceDefinitionSet raceDefinitionSetToUpdate,
            TrackedRegattaRegistry trackedRegattaRegistry, RaceLogResolver raceLogResolver,
            URI courseDesignUpdateURI, String tracTracUsername, String tracTracPassword,
            IEventSubscriber eventSubscriber, IRaceSubscriber raceSubscriber, boolean useInternalMarkPassingAlgorithm,
            long timeoutInMilliseconds, ReceiverType... types) {
        IEvent tractracEvent = tractracRace.getEvent();
        Collection<Receiver> result = new ArrayList<Receiver>();
        for (ReceiverType type : types) {
            switch (type) {
            case RACECOURSE:
                result.add(new RaceCourseReceiver(this, trackedRegatta, tractracEvent, tractracRace, windStore,
                        raceDefinitionSetToUpdate, delayToLiveInMillis,
                        WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND, simulator, courseDesignUpdateURI,
                        tracTracUsername, tracTracPassword, eventSubscriber, raceSubscriber,
                        useInternalMarkPassingAlgorithm, raceLogResolver, timeoutInMilliseconds));
                break;
            case MARKPOSITIONS:
                result.add(new MarkPositionReceiver(
                        trackedRegatta, tractracEvent, tractracRace, simulator, this, eventSubscriber, raceSubscriber, timeoutInMilliseconds));
                break;
            case RAWPOSITIONS:
                result.add(new RawPositionReceiver(
                        trackedRegatta, tractracEvent, this, simulator, eventSubscriber, raceSubscriber, timeoutInMilliseconds));
                break;
            case MARKPASSINGS:
                if (!useInternalMarkPassingAlgorithm) {
                    result.add(new MarkPassingReceiver(trackedRegatta, tractracEvent, simulator, this, eventSubscriber, raceSubscriber, timeoutInMilliseconds));
                }
                break;
            case RACESTARTFINISH:
                result.add(new RaceStartedAndFinishedReceiver(
                        trackedRegatta, tractracEvent, simulator, this, eventSubscriber, raceSubscriber, timeoutInMilliseconds));
                break;
            }
        }
        return result;
    }

    @Override
    public Iterable<Receiver> getUpdateReceivers(DynamicTrackedRegatta trackedRegatta,
            long delayToLiveInMillis, Simulator simulator, WindStore windStore,
            DynamicRaceDefinitionSet raceDefinitionSetToUpdate, TrackedRegattaRegistry trackedRegattaRegistry, RaceLogResolver raceLogResolver,
            IRace tractracRace, URI courseDesignUpdateURI, 
            String tracTracUsername, String tracTracPassword, IEventSubscriber eventSubscriber, IRaceSubscriber raceSubscriber,
            boolean useInternalMarkPassingAlgorithm, long timeoutInMilliseconds) {
        return getUpdateReceivers(trackedRegatta, tractracRace, windStore, delayToLiveInMillis, simulator,
                raceDefinitionSetToUpdate, trackedRegattaRegistry, raceLogResolver, courseDesignUpdateURI,
                tracTracUsername, tracTracPassword, eventSubscriber, raceSubscriber,
                useInternalMarkPassingAlgorithm, timeoutInMilliseconds, ReceiverType.RACECOURSE,
                ReceiverType.MARKPASSINGS, ReceiverType.MARKPOSITIONS, ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS);
    }
    
    @Override
    public Serializable getRaceID(IRace tractracRace) {
        return tractracRace.getId();
    }

    @Override
    public void removeRace(IEvent tractracEvent, IRace tractracRace, TrackedRegattaRegistry trackedRegattaRegistry) {
        RaceDefinition raceDefinition;
        synchronized (raceCache) {
            raceDefinition = getExistingRaceDefinitionForRace(tractracRace.getId());
            if (raceDefinition != null) { // otherwise, this domain factory doesn't seem to know about the race
                raceCache.remove(tractracRace.getId());
                logger.info("Removed race "+raceDefinition.getName()+" from TracTrac DomainFactoryImpl");
            }
        }
        if (raceDefinition != null) {
            // FIXME this only removes the race from its default regatta, not any explicit regatta
            Pair<String, BoatClass> defaultRegattaNameAndBoatClass = getDefaultRegattaNameAndBoatClass(tractracRace);
            Pair<String, String> key = new Pair<String, String>(defaultRegattaNameAndBoatClass.getA(),
                    defaultRegattaNameAndBoatClass.getB() == null ? null :
                        defaultRegattaNameAndBoatClass.getB().getName());
            synchronized (regattaCache) {
                Regatta regatta = regattaCache.get(key);
                if (regatta != null) {
                    // The following fixes bug 202: when tracking of multiple races of the same event has been started, this may not
                    // remove any race; however, the event may already have been created by another tracker whose race hasn't
                    // arrived yet and therefore the races list is still empty; therefore, only remove the event if its
                    // race list became empty by the removal performed here.
                    int oldSize = Util.size(regatta.getAllRaces());
                    regatta.removeRace(raceDefinition);
                    if (oldSize > 0 && Util.size(regatta.getAllRaces()) == 0) {
                        logger.info("Removing regatta "+regatta.getName()+" ("+regatta.hashCode()+") from TracTrac DomainFactoryImpl");
                        regattaCache.remove(key);
                        weakDefaultRegattaCache.remove(tractracRace);
                    }
                    TrackedRegatta trackedRegatta = trackedRegattaRegistry.getTrackedRegatta(regatta);
                    if (trackedRegatta != null) {
                        // see above; only remove tracked regatta if it *became* empty because of the tracked race removal here
                        final int oldSizeOfTrackedRaces;
                        final int newSizeOfTrackedRaces;
                        trackedRegatta.lockTrackedRacesForWrite();
                        try {
                            oldSizeOfTrackedRaces = Util.size(trackedRegatta.getTrackedRaces());
                            trackedRegatta.removeTrackedRace(raceDefinition);
                            newSizeOfTrackedRaces = Util.size(trackedRegatta.getTrackedRaces());
                        } finally {
                            trackedRegatta.unlockTrackedRacesAfterWrite();
                        }
                        if (oldSizeOfTrackedRaces > 0 && newSizeOfTrackedRaces == 0) {
                            trackedRegattaRegistry.removeTrackedRegatta(regatta);
                        }
                    }
                }
            }
        }
    }

    @Override
    public DynamicTrackedRace getOrCreateRaceDefinitionAndTrackedRace(DynamicTrackedRegatta trackedRegatta, UUID raceId,
			String raceName, BoatClass boatClass, Map<Competitor, Boat> competitorsAndBoats, Course course, Iterable<Sideline> sidelines, WindStore windStore,
			long delayToLiveInMillis, long millisecondsOverWhichToAverageWind,
			DynamicRaceDefinitionSet raceDefinitionSetToUpdate, URI tracTracUpdateURI, UUID tracTracEventUuid,
			String tracTracUsername, String tracTracPassword, boolean ignoreTracTracMarkPassings, RaceLogResolver raceLogResolver,
			Consumer<DynamicTrackedRace> runBeforeExposingRace, IRace tractracRace) {
        synchronized (raceCache) {
            RaceDefinition raceDefinition = raceCache.get(raceId);
            if (raceDefinition == null) {
                logger.info("Creating RaceDefinitionImpl for race "+raceName);
                raceDefinition = new RaceDefinitionImpl(raceName, course, boatClass, competitorsAndBoats, raceId);
            } else {
                logger.info("Already found RaceDefinitionImpl for race "+raceName);
            }
            DynamicTrackedRace trackedRace = trackedRegatta.getExistingTrackedRace(raceDefinition);
            if (trackedRace == null) {
                // add to existing regatta only if boat class matches
                if (raceDefinition.getBoatClass() == trackedRegatta.getRegatta().getBoatClass()) {
                    trackedRegatta.getRegatta().addRace(raceDefinition);
                    trackedRace = createTrackedRace(trackedRegatta, raceDefinition, sidelines, windStore,
                            delayToLiveInMillis, millisecondsOverWhichToAverageWind, raceDefinitionSetToUpdate, ignoreTracTracMarkPassings,
                            raceLogResolver);
                    logger.info("Added race " + raceDefinition + " to regatta " + trackedRegatta.getRegatta());
                    if (runBeforeExposingRace != null) {
                    	logger.fine("Running callback for tracked race creation for "+trackedRace.getRace());
                    	runBeforeExposingRace.accept(trackedRace);
                    }
                    addTracTracUpdateHandlers(tracTracUpdateURI, tracTracEventUuid, tracTracUsername, tracTracPassword,
                            raceDefinition, trackedRace, tractracRace);
                    raceCache.put(raceId, raceDefinition);
                    raceCache.notifyAll();
                } else {
                    final String reasonForNotAddingRaceToRegatta = "Not adding race " + raceDefinition + " to regatta " + trackedRegatta.getRegatta()
                            + " because boat class " + raceDefinition.getBoatClass()
                            + " doesn't match regatta's boat class " + trackedRegatta.getRegatta().getBoatClass();
                    logger.warning(reasonForNotAddingRaceToRegatta);
                    try {
                        raceDefinitionSetToUpdate.raceNotLoaded(reasonForNotAddingRaceToRegatta);
                    } catch (Exception e) {
                        logger.log(Level.INFO, "Something else went wrong while trying to notify the RaceDefinition set that the race "+
                                raceDefinition+" could not be added to the the regatta "+trackedRegatta.getRegatta(), e);
                    }
                }
            } else {
                logger.info("Found existing tracked race for race "+raceName+" with ID "+raceId);
            }
            return trackedRace;
        }
    }

    @Override
    public Iterable<IControlPoint> getControlPointsForCourseArea(IEvent tracTracEvent, String tracTracCourseAreaName) {
    	final Set<IControlPoint> result = new HashSet<>();
    	for (final IControl control : getControlsForCourseArea(tracTracEvent, tracTracCourseAreaName)) {
    	    result.addAll(control.getControlPoints());
    	}
    	return result;
    }
    
    @Override
    public Iterable<IControl> getControlsForCourseArea(IEvent tracTracEvent, String tracTracCourseAreaName) {
        final Set<IControl> result = new HashSet<>();
        if (tracTracCourseAreaName != null) {
            for (final IControl control : tracTracEvent.getControls()) {
                if (control.getCourseArea() != null && control.getCourseArea().equals(tracTracCourseAreaName)) {
                    result.add(control);
                }
            }
        }
        return result;
    }

    @Override
    public ControlPoint getExistingControlWithTwoMarks(Iterable<IControl> candidates, Mark first, Mark second) {
        final Set<Mark> pairOfMarksToFind = new HashSet<>();
        pairOfMarksToFind.add(first);
        pairOfMarksToFind.add(second);
        for (final IControl control : candidates) {
            TracTracControlPoint cp = new ControlPointAdapter(control);
            Set<Mark> marksInExistingControlPoint = new HashSet<>();
            final ControlPoint controlPoint = getOrCreateControlPoint(cp);
            Util.addAll(controlPoint.getMarks(), marksInExistingControlPoint);
            if (marksInExistingControlPoint.equals(pairOfMarksToFind)) {
                return controlPoint;
            }
        }
        return null;
    }
    
    @Override
    public void addTracTracUpdateHandlers(URI tracTracUpdateURI, UUID tracTracEventUuid, String tracTracUsername,
            String tracTracPassword, RaceDefinition raceDefinition, DynamicTrackedRace trackedRace, IRace tractracRace) {
        TracTracCourseDesignUpdateHandler courseDesignHandler = new TracTracCourseDesignUpdateHandler(
                tracTracUpdateURI, tracTracUsername, tracTracPassword, tracTracEventUuid,
                raceDefinition.getId(), tractracRace, this);
        trackedRace.addCourseDesignChangedListener(courseDesignHandler);
        TracTracStartTimeUpdateHandler startTimeHandler = new TracTracStartTimeUpdateHandler(
                tracTracUpdateURI, tracTracUsername, tracTracPassword, tracTracEventUuid,
                raceDefinition.getId(), trackedRace.getTrackedRegatta().getRegatta());
        trackedRace.addStartTimeChangedListener(startTimeHandler);
        TracTracRaceAbortedHandler raceAbortedHandler = new TracTracRaceAbortedHandler(
                tracTracUpdateURI, tracTracUsername, tracTracPassword, tracTracEventUuid,
                raceDefinition.getId());
        trackedRace.addRaceAbortedListener(raceAbortedHandler);
        trackedRace.addListener(new TracTracFinishTimeUpdateHandler(tracTracUpdateURI, tracTracUsername, tracTracPassword, tracTracEventUuid,
                raceDefinition.getId(), trackedRace.getTrackedRegatta().getRegatta()).getListener());
    }

    private DynamicTrackedRace createTrackedRace(TrackedRegatta trackedRegatta, RaceDefinition race,
            Iterable<Sideline> sidelines, WindStore windStore, long delayToLiveInMillis,
            long millisecondsOverWhichToAverageWind, DynamicRaceDefinitionSet raceDefinitionSetToUpdate,
            boolean useMarkPassingCalculator, RaceLogResolver raceLogResolver) {
        return trackedRegatta.createTrackedRace(race, sidelines,
                windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                /* time over which to average speed: */ race.getBoatClass().getApproximateManeuverDurationInMilliseconds(),
                raceDefinitionSetToUpdate, useMarkPassingCalculator, raceLogResolver);
    }

    /**
     * Obtains the boats for the given competitors.
     * There are 2 cases to distinguish:
     * 1. Races with boat information in the competitor metadata field  
     * 2. Races without any boat information
     */
    @Override
    public Map<Competitor, Boat> getOrCreateCompetitorsAndTheirBoats(DynamicTrackedRegatta trackedRegatta, IRace race, BoatClass defaultBoatClass) {
        final CompetitorStore competitorAndBoatStore = baseDomainFactory.getCompetitorStore();
        final Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        getCompetingCompetitors(race).forEach(rc->{
            Serializable competitorId = rc.getCompetitor().getId(); 
            BoatMetaData competitorBoatInfo = getMetadataParser().parseCompetitorBoat(rc);

            // If the tractrac race contains boat metadata we assume the regatta can have changing boats per race.
            // As the attribute 'canBoatsOfCompetitorsChangePerRace' is new and 'false' is the default value 
            // we need to set it's value to true for the regatta 
            if (competitorBoatInfo != null && trackedRegatta.getRegatta().canBoatsOfCompetitorsChangePerRace() == false) {
                // we need to set this to true for the regatta to make it possible to edit the boat/competitor mappings
                trackedRegatta.getRegatta().setCanBoatsOfCompetitorsChangePerRace(true);
            }

            // Case 1
            if (trackedRegatta.getRegatta().canBoatsOfCompetitorsChangePerRace()) {
                // create an unique identifier for the boat and try to find it in the boatStore
                Serializable boatId;
                String sailId;
                if (competitorBoatInfo != null) {
                    boatId = createUniqueBoatIdentifierFromBoatMetadata(race, competitorBoatInfo);
                    sailId = competitorBoatInfo.getId(); // we take here the boatId as sailID which is a number like 1, 2, 3
                } else {
                    boatId = createUniqueBoatIdentifierFromCompetitor(race, rc.getCompetitor());
                    sailId = rc.getCompetitor().getShortName();
                }

                Boat existingBoat = competitorAndBoatStore.getExistingBoatById(boatId);
                Competitor existingCompetitor = competitorAndBoatStore.getExistingCompetitorById(competitorId);

                // now we need to check if 
                // Now we check if we already have a separate competitor and boat for this competitor in the store
                if (existingCompetitor != null && existingBoat != null) {                    
                    competitorsAndBoats.put(existingCompetitor, existingBoat);
                } else {
                    CompetitorWithBoat existingCompetitorWithBoat = competitorAndBoatStore.getExistingCompetitorWithBoatById(competitorId);
                    if (existingCompetitorWithBoat != null) {
                        // migrate the one with the contained boat
                        Pair<Competitor, Boat> migratedCompetitorAndBoat = competitorAndBoatStore.migrateCompetitorToHaveASeparateBoat(boatId, existingCompetitorWithBoat);
                        competitorsAndBoats.put(migratedCompetitorAndBoat.getA(), migratedCompetitorAndBoat.getB());
                        
                        if (existingCompetitor.getShortName() != rc.getCompetitor().getShortName()) {
                            // in case we find a boat info we only want to update the shortName field of the competitor (instead of using sailID)
                            boolean isOldCompetitorToUpdateDuringGetOrCreate = competitorAndBoatStore.isCompetitorToUpdateDuringGetOrCreate(existingCompetitor);
                            competitorAndBoatStore.allowCompetitorResetToDefaults(existingCompetitor);
                            existingCompetitor = competitorAndBoatStore.getOrCreateCompetitor(existingCompetitor.getId(), existingCompetitor.getName(),
                                    rc.getCompetitor().getShortName(), existingCompetitor.getColor(),
                                    existingCompetitor.getEmail(), existingCompetitor.getFlagImage(),
                                    (DynamicTeam) existingCompetitor.getTeam(), existingCompetitor.getTimeOnTimeFactor(),
                                    existingCompetitor.getTimeOnDistanceAllowancePerNauticalMile(), existingCompetitor.getSearchTag());
                            if (isOldCompetitorToUpdateDuringGetOrCreate) {
                                competitorAndBoatStore.allowCompetitorResetToDefaults(existingCompetitor);
                            }
                        }
                    } else {
                        Competitor newCompetitor = getOrCreateCompetitor(rc.getCompetitor());
                        Boat newBoat;
                        if (competitorBoatInfo != null) {
                            newBoat = getOrCreateBoat(boatId, competitorBoatInfo.getName(), defaultBoatClass, sailId, AbstractColor.getCssColor(competitorBoatInfo.getColor()));
                        } else {
                            newBoat = getOrCreateBoat(boatId, "Boat of " + rc.getCompetitor().getShortName(), defaultBoatClass, sailId, null);
                        }
                        competitorsAndBoats.put(newCompetitor, newBoat);
                    }
                }
            } else {
                // Case 2 we assume here that the boat is contained in competitor as it's always the same
                CompetitorWithBoat competitorWithBoat = getOrCreateCompetitorWithBoat(rc.getCompetitor());
                competitorsAndBoats.put(competitorWithBoat, competitorWithBoat.getBoat());
            }
        });
        return competitorsAndBoats;
    }

    /**
     * Create an unique key for a boat derived from the tractrac event UUID and the boat Id given by the boat metadata
     * @return the unique key (per tractrac event) 
     */
    private String createUniqueBoatIdentifierFromBoatMetadata(IRace race, BoatMetaData boatMetadata) {
        UUID eventUUID = race.getEvent().getId();
        String boatIdentifier = eventUUID.toString() + '#' + boatMetadata.getId();
        return boatIdentifier;
    }

    /**
     * Create an unique key for a boat derived from the tractrac event UUID and the Id of the competitor
     * @return the unique key (per tractrac event) 
     */
    private String createUniqueBoatIdentifierFromCompetitor(IRace race, ICompetitor competitor) {
        UUID eventUUID = race.getEvent().getId();
        String boatIdentifier = eventUUID.toString() + '#' + competitor.getId().toString();
        return boatIdentifier;
    }

    /**
     * Obtains those {@link IRace#getRaceCompetitors() competitors} that are actually competing in the race
     * ({@link ICompetitor#isNonCompeting()}=={@code false})
     */
    private Stream<IRaceCompetitor> getCompetingCompetitors(IRace race) {
    	return race.getRaceCompetitors().stream().filter(rc->!rc.getCompetitor().isNonCompeting());
    }

    @Override
    public Util.Pair<Iterable<Competitor>, BoatClass> getCompetitorsAndDominantBoatClass(IRace race) {
        List<ICompetitorClass> competitorClasses = new ArrayList<ICompetitorClass>();
        final List<Competitor> competitors = new ArrayList<Competitor>();
        getCompetingCompetitors(race).forEach(rc->{
            // also add those whose race class doesn't match the dominant one (such as camera boats)
            // because they may still send data that we would like to record in some tracks
            competitors.add(getOrCreateCompetitor(rc.getCompetitor()));
            competitorClasses.add(rc.getCompetitor().getCompetitorClass());
        });
        BoatClass dominantBoatClass = getDominantBoatClass(competitorClasses);
        Util.Pair<Iterable<Competitor>, BoatClass> competitorsAndDominantBoatClass = new com.sap.sse.common.Util.Pair<Iterable<Competitor>, BoatClass>(
                competitors, dominantBoatClass);
        return competitorsAndDominantBoatClass;
    }
    
    private BoatClass getDominantBoatClass(Collection<ICompetitorClass> competitorClasses) {
        List<String> competitorClassNames = new ArrayList<>();
        for (ICompetitorClass competitorClass : competitorClasses) {
            competitorClassNames.add(competitorClass==null?null:competitorClass.getName());
        }
        BoatClass dominantBoatClass = getDominantBoatClass(competitorClassNames);
        return dominantBoatClass;
    }

    @Override
    public BoatClass getDominantBoatClass(Iterable<String> competitorClassNames) {
        final BoatClass result;
        if (competitorClassNames == null) {
            result = null;
        } else {
            Collection<BoatClass> boatClasses = new ArrayList<>();
            for (String competitorClassName : competitorClassNames) {
                BoatClass boatClass = getOrCreateBoatClass(competitorClassName);
                boatClasses.add(boatClass);
            }
            result = Util.getDominantObject(boatClasses);
        }
        return result;
    }

    @Override
    public Mark getMark(TracTracControlPoint controlPoint, int zeroBasedMarkIndex) {
        com.sap.sailing.domain.base.ControlPoint myControlPoint = getOrCreateControlPoint(controlPoint);
        Mark result;
        Iterator<Mark> iter = myControlPoint.getMarks().iterator();
        if (controlPoint.getHasTwoPoints()) {
            if (zeroBasedMarkIndex == 0) {
                result = iter.next();
            } else {
                iter.next();
                result = iter.next();
            }
        } else {
            result = iter.next();
        }
        return result;
    }

    @Override
    public MarkPassing createMarkPassing(TimePoint timePoint, Waypoint passed, com.sap.sailing.domain.base.Competitor competitor) {
        return baseDomainFactory.createMarkPassing(timePoint, passed, competitor);
    }

    @Override
    public TracTracRaceTracker createRaceTracker(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, WindStore windStore, TrackedRegattaRegistry trackedRegattaRegistry,
            RaceLogResolver raceLogResolver, RaceTrackingConnectivityParametersImpl connectivityParams, long timeoutInMilliseconds) throws MalformedURLException,
            FileNotFoundException, URISyntaxException, CreateModelException, SubscriberInitializationException {
        return new TracTracRaceTrackerImpl(this, raceLogStore, regattaLogStore, windStore, trackedRegattaRegistry, raceLogResolver, connectivityParams, timeoutInMilliseconds);
    }

    @Override
    public RaceTracker createRaceTracker(Regatta regatta, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, WindStore windStore,
            TrackedRegattaRegistry trackedRegattaRegistry, RaceLogResolver raceLogResolver, RaceTrackingConnectivityParametersImpl connectivityParams, long timeoutInMilliseconds)
            throws MalformedURLException, FileNotFoundException, URISyntaxException, CreateModelException,
            SubscriberInitializationException {
        return new TracTracRaceTrackerImpl(regatta, this, raceLogStore, regattaLogStore, windStore, trackedRegattaRegistry,
                raceLogResolver, connectivityParams, timeoutInMilliseconds);
    }

    @Override
    public JSONService parseJSONURLWithRaceRecords(URL jsonURL, boolean loadClientParams) throws IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        return new JSONServiceImpl(jsonURL, loadClientParams);
    }

    @Override
    public TracTracConfiguration createTracTracConfiguration(String name, String jsonURL, String liveDataURI, String storedDataURI, String courseDesignUpdateURI, String tracTracUsername, String tracTracPassword) {
        return new TracTracConfigurationImpl(name, jsonURL, liveDataURI, storedDataURI, courseDesignUpdateURI, tracTracUsername, tracTracPassword);
    }

    @Override
    public RaceTrackingConnectivityParameters createTrackingConnectivityParameters(URL paramURL, URI liveURI,
            URI storedURI, URI courseDesignUpdateURI, TimePoint startOfTracking, TimePoint endOfTracking,
            long delayToLiveInMillis, Duration offsetToStartTimeOfSimulatedRace, boolean useInternalMarkPassingAlgorithm, RaceLogStore raceLogStore,
            RegattaLogStore regattaLogStore, String tracTracUsername, String tracTracPassword, String raceStatus,
            String raceVisibility, boolean trackWind, boolean correctWindDirectionByMagneticDeclination, boolean preferReplayIfAvailable, int timeoutInMillis) throws Exception {
        return new RaceTrackingConnectivityParametersImpl(paramURL, liveURI, storedURI, courseDesignUpdateURI,
                startOfTracking, endOfTracking, delayToLiveInMillis, offsetToStartTimeOfSimulatedRace, useInternalMarkPassingAlgorithm, raceLogStore,
                regattaLogStore, this, tracTracUsername, tracTracPassword, raceStatus, raceVisibility, trackWind, correctWindDirectionByMagneticDeclination,
                preferReplayIfAvailable, timeoutInMillis);
    }

    @Override
    public JSONService parseJSONURLForOneRaceRecord(URL jsonURL, String raceId, boolean loadClientParams)
            throws IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        return new JSONServiceImpl(jsonURL, raceId, loadClientParams);
    }

}
