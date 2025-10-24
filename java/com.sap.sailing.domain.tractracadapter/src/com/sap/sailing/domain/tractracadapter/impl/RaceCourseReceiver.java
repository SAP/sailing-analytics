package com.sap.sailing.domain.tractracadapter.impl;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.shared.tracking.impl.TrackingConnectorInfoImpl;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.TracTracAdapter;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.model.lib.api.route.IControlRoute;
import com.tractrac.model.lib.api.route.IPathRoute;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.control.IControlRouteChangeListener;

import difflib.PatchFailedException;

/**
 * The ordering of the {@link ControlPoint}s of a {@link Course} are received
 * dynamically through a callback interface. Therefore, when connected to an
 * {@link Regatta}, these orders are not yet defined. An instance of this class
 * can be used to create the listeners needed to receive this information and
 * set it on an {@link Regatta}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class RaceCourseReceiver extends AbstractReceiverWithQueue<IControlRoute, Long, Void>  {

    private final static Logger logger = Logger.getLogger(RaceCourseReceiver.class.getName());
    
    private final long millisecondsOverWhichToAverageWind;
    private final long delayToLiveInMillis;
    private final WindStore windStore;
    private final DynamicRaceDefinitionSet raceDefinitionSetToUpdate;
    private final URI tracTracUpdateURI;
    private final String tracTracUsername;
    private final String tracTracPassword;
    private final IRace tractracRace;
    private final IControlRouteChangeListener listener;
    private final boolean useInternalMarkPassingAlgorithm;
    private final RaceLogAndTrackedRaceResolver raceLogResolver;
    private final MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry;
    private final LeaderboardGroupResolver leaderboardGroupResolver;

    private final RaceTrackingHandler raceTrackingHandler;
    
    public RaceCourseReceiver(DomainFactory domainFactory, DynamicTrackedRegatta trackedRegatta, IEvent tractracEvent,
            IRace tractracRace, WindStore windStore, DynamicRaceDefinitionSet raceDefinitionSetToUpdate,
            long delayToLiveInMillis, long millisecondsOverWhichToAverageWind, Simulator simulator,
            URI updateURI, String tracTracUsername, String tracTracPassword,
            IEventSubscriber eventSubscriber, IRaceSubscriber raceSubscriber, boolean useInternalMarkPassingAlgorithm,
            RaceLogAndTrackedRaceResolver raceLogResolver, LeaderboardGroupResolver leaderboardGroupResolver, long timeoutInMilliseconds,
            RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        super(domainFactory, tractracEvent, trackedRegatta, simulator, eventSubscriber, raceSubscriber, timeoutInMilliseconds);
        this.tractracRace = tractracRace;
        this.raceLogResolver = raceLogResolver;
        this.markPassingRaceFingerprintRegistry = markPassingRaceFingerprintRegistry;
        this.leaderboardGroupResolver = leaderboardGroupResolver;
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        this.delayToLiveInMillis = delayToLiveInMillis;
        this.raceTrackingHandler = raceTrackingHandler;
        if (simulator == null) {
            this.windStore = windStore;
        } else {
            this.windStore = simulator.simulatingWindStore(windStore);
        }
        this.raceDefinitionSetToUpdate = raceDefinitionSetToUpdate;
        this.tracTracUpdateURI = updateURI;
        this.tracTracUsername = tracTracUsername;
        this.tracTracPassword = tracTracPassword;
        this.useInternalMarkPassingAlgorithm = useInternalMarkPassingAlgorithm;
        listener = new IControlRouteChangeListener() {
            @Override
            public void gotRouteChange(IControlRoute controlRoute, long timeStamp) {
                enqueue(new Triple<IControlRoute, Long, Void>(controlRoute, timeStamp, null));
            }

            @Override
            public void gotRouteChange(IPathRoute pathRoute, long timeStamp) {
                // will never be invoked for sailing events; Jorge Llodra (2018-07-30):
                // "The IControlRouteChangeListener has been extended adding a new method to get updates of a
                // IPathRoute. If you are managing "sailing events" this method will never be invoked."
            }
        };
    }

    @Override
    public void subscribe() {
        getRaceSubscriber().subscribeRouteChanges(listener);
        startThread();
    }
    
    @Override
    protected void unsubscribe() {
        getRaceSubscriber().unsubscribeRouteChanges(listener);
    }

    @Override
    protected void handleEvent(Triple<IControlRoute, Long, Void> event) {
        System.out.print("R");
        ensureAllSingleMarksOfCourseAreaAreCreated(tractracRace); // this way, single marks will be known by their original name, even if used as virtual marks in gates/lines
        final IControlRoute route = event.getA();
        final String routeMetadataString = route.getMetadata() != null ? route.getMetadata().getText() : null;
        final List<IMapItem> routeControlPoints = route.getControls();
        Map<Integer, PassingInstruction> courseWaypointPassingInstructions = getDomainFactory().getMetadataParser().parsePassingInstructionData(routeMetadataString, routeControlPoints.size());
        final List<Pair<IMapItem, PassingInstruction>> ttControlPoints = new ArrayList<>();
        int i = 0;
        for (IMapItem cp : routeControlPoints) {
            final PassingInstruction passingInstructions = courseWaypointPassingInstructions.containsKey(i) ? courseWaypointPassingInstructions.get(i) : PassingInstruction.None;
            ttControlPoints.add(new Pair<>(cp, passingInstructions));
            i++;
        }
        Course course = getDomainFactory().createCourse(route.getName(), ttControlPoints);
        List<Sideline> sidelines = getDomainFactory().createSidelines(
                tractracRace.getMetadata() != null ? tractracRace.getMetadata().getText() : null,
                        tractracRace.getEvent().getMapItems());
        RaceDefinition existingRaceDefinitionForRace = getDomainFactory().getExistingRaceDefinitionForRace(tractracRace.getId());
        DynamicTrackedRace trackedRace = null;
        // When the tracked race is created, we noted that for REPLAY races the TracAPI transmission of race times is not reliable.
        // Therefore, we poke the tracking start/end times and the race start time into the TrackedRace here when it's created here.
        if (existingRaceDefinitionForRace != null) {
            logger.log(Level.INFO, "Received course update for existing race "+tractracRace.getName()+": "+
                    event.getA().getControls());
            // Race already exists; this means that we obviously found a course change.
            // Create TrackedRace only if it doesn't exist (which is unlikely because it is usually created
            // in the else block below together with the RaceDefinition).
            try {
                getDomainFactory().updateCourseWaypoints(existingRaceDefinitionForRace.getCourse(), ttControlPoints);
                if (getTrackedRegatta().getExistingTrackedRace(existingRaceDefinitionForRace) == null) {
                    trackedRace = createTrackedRace(existingRaceDefinitionForRace, sidelines, tr->updateRaceTimes(tractracRace, tr));
                    addAllMarksFromCourseArea(trackedRace);
                }
            } catch (PatchFailedException e) {
                logger.log(Level.SEVERE, "Internal error updating race course "+course+": "+e.getMessage());
                logger.log(Level.SEVERE, "handleEvent", e);
            }
        } else {
            logger.log(Level.INFO, "Received course for non-existing race "+tractracRace.getName()+". Creating RaceDefinition.");
            // create race definition and add to event
            BoatClass dominantBoatClass = getDomainFactory().resolveDominantBoatClassOfRace(tractracRace);
            Map<Competitor, Boat> competitorAndBoats = getDomainFactory().getOrCreateCompetitorsAndTheirBoats(
                    getTrackedRegatta(), leaderboardGroupResolver, tractracRace, dominantBoatClass,
                    raceTrackingHandler);
            trackedRace = getDomainFactory().getOrCreateRaceDefinitionAndTrackedRace(
                    getTrackedRegatta(), tractracRace.getId(), tractracRace.getName(),
                    getTrackedRegatta().getRegatta().getBoatClass(), competitorAndBoats, course, sidelines, windStore, delayToLiveInMillis,
                    millisecondsOverWhichToAverageWind, raceDefinitionSetToUpdate, tracTracUpdateURI,
                    getTracTracEvent().getId(), tracTracUsername, tracTracPassword, useInternalMarkPassingAlgorithm, raceLogResolver, tr->updateRaceTimes(tractracRace, tr), tractracRace,
                    raceTrackingHandler, markPassingRaceFingerprintRegistry);
            addAllMarksFromCourseArea(trackedRace);
            if (getSimulator() != null) {
                getSimulator().setTrackedRace(trackedRace);
            }
        }
    }

    /**
     * For all marks associated with the course area of the {@link #tractracRace}'s {@link IRace#getCourseArea() course area} (if any; this
     * was introduced with the TracAPI version 3.6.1) a mark track is created in the {@link TrackedRace}. This will let the {@link TrackedRace#getMarks}
     * method return those, helping clients asking for "available" marks.
     */
    private void addAllMarksFromCourseArea(DynamicTrackedRace trackedRace) {
        for (final IMapItem tractracControlPoint : getDomainFactory().getControlsForCourseArea(getTracTracEvent(), tractracRace.getCourseArea())) {
            final ControlPoint cp = getDomainFactory().getOrCreateControlPoint(tractracControlPoint);
            for (final Mark mark : cp.getMarks()) {
                trackedRace.getOrCreateTrack(mark);
            }
        }
    }
    
    private void updateRaceTimes(IRace tractracRace, DynamicTrackedRace trackedRace) {
        final int liveDelayInSeconds = tractracRace.getLiveDelay();
        long delayInMillis = liveDelayInSeconds * 1000;
        if (trackedRace != null) {
            logger.info("Setting delay for race "+trackedRace.getRace().getName()+" to "+delayInMillis+"ms");
            trackedRace.setDelayToLiveInMillis(delayInMillis);
        }
        final TimePoint startTime;
        final long tractracRaceStartTime = tractracRace.getRaceStartTime();
        if (tractracRaceStartTime != 0) {
            if (getSimulator() != null) {
                startTime = getSimulator().advanceStartTime(new MillisecondsTimePoint(tractracRaceStartTime));
            } else {
                startTime = new MillisecondsTimePoint(tractracRaceStartTime);
            }
        } else {
            startTime = null;
        }
        if (trackedRace != null && startTime != null) {
            trackedRace.setStartTimeReceived(startTime);
        }
        final TimePoint startTrackingTime;
        final long tractracStartTrackingTime = tractracRace.getTrackingStartTime();
        if (tractracStartTrackingTime != 0) {
            if (getSimulator() != null) {
                startTrackingTime = getSimulator().advanceStartTime(new MillisecondsTimePoint(tractracStartTrackingTime));
            } else {
                startTrackingTime = new MillisecondsTimePoint(tractracStartTrackingTime);
            }
        } else {
            startTrackingTime = null;
        }
        if (trackedRace != null && startTrackingTime != null) {
            trackedRace.setStartOfTrackingReceived(startTrackingTime);
        }
        final TimePoint endTrackingTime;
        final long tractracEndTrackingTime = tractracRace.getTrackingEndTime();
        if (tractracEndTrackingTime != 0) {
            if (getSimulator() != null) {
                endTrackingTime = getSimulator().advanceStartTime(new MillisecondsTimePoint(tractracEndTrackingTime));
            } else {
                endTrackingTime = new MillisecondsTimePoint(tractracEndTrackingTime);
            }
        } else {
            endTrackingTime = null;
        }
        if (trackedRace != null && endTrackingTime != null) {
            trackedRace.setEndOfTrackingReceived(endTrackingTime);
        }
    }

    private DynamicTrackedRace createTrackedRace(RaceDefinition race, Iterable<Sideline> sidelines,
            Consumer<DynamicTrackedRace> runAfterCreatingTrackedRace) {
        final URL webUrl = tractracRace.getEvent().getWebURL();
        final String webUrlString = webUrl == null ? null : webUrl.toString();
        DynamicTrackedRace trackedRace = raceTrackingHandler.createTrackedRace(getTrackedRegatta(), race, sidelines,
                windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                /* time over which to average speed: */ race.getBoatClass()
                        .getApproximateManeuverDurationInMilliseconds(),
                raceDefinitionSetToUpdate, useInternalMarkPassingAlgorithm, raceLogResolver,
                /* ThreadLocalTransporter not needed because the RaceTracker is not active on a replica */ Optional
                        .empty(),
                new TrackingConnectorInfoImpl(TracTracAdapter.NAME, TracTracAdapter.DEFAULT_URL,
                        webUrlString), markPassingRaceFingerprintRegistry);
        if (runAfterCreatingTrackedRace != null) {
            runAfterCreatingTrackedRace.accept(trackedRace);
        }
        getDomainFactory().addTracTracUpdateHandlers(tracTracUpdateURI, getTracTracEvent().getId(), tracTracUsername,
                tracTracPassword, race, trackedRace, tractracRace);
        return trackedRace;
    }

    @Override
    public String toString() {
        return super.toString() + ", race "+tractracRace.getName()+" with ID "+tractracRace.getId();
    }
}
