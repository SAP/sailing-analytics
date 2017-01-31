package com.sap.sailing.domain.tractracadapter.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.TracTracControlPoint;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.route.IControl;
import com.tractrac.model.lib.api.route.IControlRoute;
import com.tractrac.model.lib.api.route.IRoute;
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
    private final RaceLogResolver raceLogResolver;
    
    public RaceCourseReceiver(DomainFactory domainFactory, DynamicTrackedRegatta trackedRegatta, IEvent tractracEvent,
            IRace tractracRace, WindStore windStore, DynamicRaceDefinitionSet raceDefinitionSetToUpdate,
            long delayToLiveInMillis, long millisecondsOverWhichToAverageWind, Simulator simulator,
            URI courseDesignUpdateURI, String tracTracUsername, String tracTracPassword,
            IEventSubscriber eventSubscriber, IRaceSubscriber raceSubscriber, boolean useInternalMarkPassingAlgorithm,
            RaceLogResolver raceLogResolver, long timeoutInMilliseconds) {
        super(domainFactory, tractracEvent, trackedRegatta, simulator, eventSubscriber, raceSubscriber, timeoutInMilliseconds);
        this.tractracRace = tractracRace;
        this.raceLogResolver = raceLogResolver;
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        this.delayToLiveInMillis = delayToLiveInMillis;
        if (simulator == null) {
            this.windStore = windStore;
        } else {
            this.windStore = simulator.simulatingWindStore(windStore);
        }
        this.raceDefinitionSetToUpdate = raceDefinitionSetToUpdate;
        this.tracTracUpdateURI = courseDesignUpdateURI;
        this.tracTracUsername = tracTracUsername;
        this.tracTracPassword = tracTracPassword;
        this.useInternalMarkPassingAlgorithm = useInternalMarkPassingAlgorithm;
        listener = new IControlRouteChangeListener() {
            @Override
            public void gotRouteChange(IControlRoute controlRoute, long timeStamp) {
                enqueue(new Triple<IControlRoute, Long, Void>(controlRoute, timeStamp, null));
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
        final IRoute route = event.getA();
        final String routeMetadataString = route.getMetadata() != null ? route.getMetadata().getText() : null;
        final LinkedHashMap<IControl, TracTracControlPoint> ttControlPointsForAllOriginalEventControlPoints = new LinkedHashMap<>();
        for (IControl cp : getTracTracEvent().getControls()) {
            ttControlPointsForAllOriginalEventControlPoints.put(cp, new ControlPointAdapter(cp));
        }
        final List<TracTracControlPoint> routeControlPoints = new ArrayList<>();
        for (IControl cp : event.getA().getControls()) {
            routeControlPoints.add(ttControlPointsForAllOriginalEventControlPoints.get(cp));
        }
        Map<Integer, PassingInstruction> courseWaypointPassingInstructions = getDomainFactory().getMetadataParser().parsePassingInstructionData(routeMetadataString, routeControlPoints);
        final List<Pair<TracTracControlPoint, PassingInstruction>> ttControlPoints = new ArrayList<>();
        int i = 0;
        for (IControl cp : event.getA().getControls()) {
            PassingInstruction passingInstructions = courseWaypointPassingInstructions.containsKey(i) ? courseWaypointPassingInstructions.get(i) : PassingInstruction.None;
            ttControlPoints.add(new Pair<TracTracControlPoint, PassingInstruction>(ttControlPointsForAllOriginalEventControlPoints.get(cp), passingInstructions));
            i++;
        }

        Course course = getDomainFactory().createCourse(route.getName(), ttControlPoints);
        List<Sideline> sidelines = getDomainFactory().createSidelines(
                tractracRace.getMetadata() != null ? tractracRace.getMetadata().getText() : null,
                ttControlPointsForAllOriginalEventControlPoints.values());

        RaceDefinition existingRaceDefinitionForRace = getDomainFactory().getExistingRaceDefinitionForRace(tractracRace.getId());
        DynamicTrackedRace trackedRace = null;
        // When the tracked race is created, we noted that for REPLAY races the TracAPI transmission of race times is not reliable.
        // Therefore, we poke the tracking start/end times and the race start time into the TrackedRace here when it's created here.
        boolean needToUpdateRaceTimes = false;
        if (existingRaceDefinitionForRace != null) {
            logger.log(Level.INFO, "Received course update for existing race "+tractracRace.getName()+": "+
                    event.getA().getControls());
            // Race already exists; this means that we obviously found a course change.
            // Create TrackedRace only if it doesn't exist (which is unlikely because it is usually created
            // in the else block below together with the RaceDefinition).
            try {
                getDomainFactory().updateCourseWaypoints(existingRaceDefinitionForRace.getCourse(), ttControlPoints);
                if (getTrackedRegatta().getExistingTrackedRace(existingRaceDefinitionForRace) == null) {
                    trackedRace = createTrackedRace(existingRaceDefinitionForRace, sidelines);
                    needToUpdateRaceTimes = true;
                }
            } catch (PatchFailedException e) {
                logger.log(Level.SEVERE, "Internal error updating race course "+course+": "+e.getMessage());
                logger.log(Level.SEVERE, "handleEvent", e);
            }
        } else {
            logger.log(Level.INFO, "Received course for non-existing race "+tractracRace.getName()+". Creating RaceDefinition.");
            // create race definition and add to event
            com.sap.sse.common.Util.Pair<Iterable<Competitor>, BoatClass> competitorsAndDominantBoatClass = getDomainFactory().getCompetitorsAndDominantBoatClass(tractracRace);
            Map<Competitor, Boat> competitorBoats = getDomainFactory().getBoatsInfoForCompetitors(tractracRace, competitorsAndDominantBoatClass.getB());
            trackedRace = getDomainFactory().getOrCreateRaceDefinitionAndTrackedRace(
                    getTrackedRegatta(), tractracRace.getId(), tractracRace.getName(), competitorsAndDominantBoatClass.getA(),
                    competitorsAndDominantBoatClass.getB(), competitorBoats, course, sidelines, windStore, delayToLiveInMillis,
                    millisecondsOverWhichToAverageWind, raceDefinitionSetToUpdate, tracTracUpdateURI,
                    getTracTracEvent().getId(), tracTracUsername, tracTracPassword, useInternalMarkPassingAlgorithm, raceLogResolver);
            needToUpdateRaceTimes = true;
            if (getSimulator() != null) {
                getSimulator().setTrackedRace(trackedRace);
            }
        }
        if (needToUpdateRaceTimes) {
            updateRaceTimes(tractracRace, trackedRace);
        }
    }

    private void updateRaceTimes(IRace tractracRace, DynamicTrackedRace trackedRace) {
        final int liveDelayInSeconds = tractracRace.getLiveDelay();
        long delayInMillis = liveDelayInSeconds * 1000;
        if (trackedRace != null) {
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

    private DynamicTrackedRace createTrackedRace(RaceDefinition race, Iterable<Sideline> sidelines) {
        DynamicTrackedRace trackedRace = getTrackedRegatta().createTrackedRace(race, sidelines,
                windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                /* time over which to average speed: */ race.getBoatClass().getApproximateManeuverDurationInMilliseconds(),
                raceDefinitionSetToUpdate, useInternalMarkPassingAlgorithm, raceLogResolver);
        getDomainFactory().addTracTracUpdateHandlers(tracTracUpdateURI, getTracTracEvent().getId(), tracTracUsername, tracTracPassword, race, trackedRace);
        return trackedRace;
    }

}
