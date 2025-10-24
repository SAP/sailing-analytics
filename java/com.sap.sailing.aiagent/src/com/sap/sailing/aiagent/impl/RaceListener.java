package com.sap.sailing.aiagent.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.aiagent.impl.rules.GoodStartRule;
import com.sap.sailing.aiagent.impl.rules.Rule;
import com.sap.sailing.aiagent.impl.rules.TopThreeMarkRoundingRule;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sse.common.TimePoint;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Used to observe one or more races, usually of the same event, for changes that may be relevant for one or more
 * {@link Rule}s. Any change event received in this object's role of a {@link RaceChangeListener} is forwarded to the
 * {@link #rules} it knows, unless the {@link TrackedRace} is in {@link TrackedRaceStatusEnum#LOADING} or
 * {@link TrackedRaceStatusEnum#PREPARED} mode in which case the forwarding-to-rules action is enqueued, and those
 * enqueued actions will then be triggered by a call to {@link #statusChanged(TrackedRaceStatus, TrackedRaceStatus)}
 * when the race changes to any other than these two statuses.
 * <p>
 * 
 * The constructor defines the set of rules to evaluate; those will then
 * {@link AIAgentImpl#produceCommentFromPrompt(String, String, String, String, String, TimePoint, String) produce comments} that are
 * inserted as tags / comments for the race.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RaceListener implements RaceChangeListener {
    private static final Logger logger = Logger.getLogger(RaceListener.class.getName());

    private final TrackedRace trackedRace;
    private final Iterable<Rule> rules;
    private final ScheduledExecutorService backgroundExecutor;
    private final ConcurrentLinkedDeque<Runnable> tasksEnqueuedWhileRaceStillLoading;
    
    public RaceListener(AIAgentImpl aiAgent, Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        super();
        this.trackedRace = trackedRace;
        this.tasksEnqueuedWhileRaceStillLoading = new ConcurrentLinkedDeque<>();
        this.backgroundExecutor = ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor();
        final List<Rule> myRules = new ArrayList<>();
        myRules.add(new GoodStartRule(aiAgent, leaderboard, raceColumn, fleet, trackedRace));
        myRules.add(new TopThreeMarkRoundingRule(aiAgent, leaderboard, raceColumn, fleet, trackedRace));
        this.rules = myRules;
    }
    
    private boolean isTaskExecutionToBeSuspendedBasedOnRaceStatus(TrackedRaceStatusEnum status) {
        return status == TrackedRaceStatusEnum.PREPARED || status == TrackedRaceStatusEnum.LOADING;
    }
    
    private synchronized void executeInBackgroundWithCurrentSubject(Runnable runnable) {
        final Subject subject = SecurityUtils.getSubject();
        final Runnable taskWithSubject = subject.associateWith(runnable);
        if (isTaskExecutionToBeSuspendedBasedOnRaceStatus(trackedRace.getStatus().getStatus())) {
            enqueue(taskWithSubject);
        } else {
            backgroundExecutor.execute(taskWithSubject);
        }
    }

    private synchronized void enqueue(Runnable taskWithSubject) {
        logger.fine(()->"Enqueuing task because tracked race "
                +trackedRace.getRaceIdentifier()
                +" is in status "+trackedRace.getStatus().getStatus());
        tasksEnqueuedWhileRaceStillLoading.add(taskWithSubject);
    }

    @Override
    public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.waypointAdded(zeroBasedIndex, waypointThatGotAdded)));
    }

    @Override
    public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.waypointRemoved(zeroBasedIndex, waypointThatGotRemoved)));
    }

    @Override
    public void competitorPositionChanged(GPSFixMoving fix, Competitor competitor, AddResult addedOrReplaced) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.competitorPositionChanged(fix, competitor, addedOrReplaced)));
    }

    @Override
    public void markPositionChanged(GPSFix fix, Mark mark, boolean firstInTrack, AddResult addedOrReplaced) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.markPositionChanged(fix, mark, firstInTrack, addedOrReplaced)));
    }

    @Override
    public void firstGPSFixReceived() {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.firstGPSFixReceived()));
    }

    @Override
    public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings,
            Iterable<MarkPassing> markPassings) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.markPassingReceived(competitor, oldMarkPassings, markPassings)));
    }

    @Override
    public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.speedAveragingChanged(oldMillisecondsOverWhichToAverage, newMillisecondsOverWhichToAverage)));
    }

    @Override
    public void windDataReceived(Wind wind, WindSource windSource) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.windDataReceived(wind, windSource)));
    }

    @Override
    public void windDataRemoved(Wind wind, WindSource windSource) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.windDataRemoved(wind, windSource)));
    }

    @Override
    public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.windAveragingChanged(oldMillisecondsOverWhichToAverage, newMillisecondsOverWhichToAverage)));
    }

    @Override
    public void startOfTrackingChanged(TimePoint oldStartOfTracking, TimePoint newStartOfTracking) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.startOfTrackingChanged(oldStartOfTracking, newStartOfTracking)));
    }

    @Override
    public void endOfTrackingChanged(TimePoint oldEndOfTracking, TimePoint newEndOfTracking) {
        rules.forEach(r->r.endOfTrackingChanged(oldEndOfTracking, newEndOfTracking));
    }

    @Override
    public void startTimeReceivedChanged(TimePoint startTimeReceived) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.startTimeReceivedChanged(startTimeReceived)));
    }

    @Override
    public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.startOfRaceChanged(oldStartOfRace, newStartOfRace)));
    }

    @Override
    public void finishingTimeChanged(TimePoint oldFinishingTime, TimePoint newFinishingTime) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.finishingTimeChanged(oldFinishingTime, newFinishingTime)));
    }

    @Override
    public void finishedTimeChanged(TimePoint oldFinishedTime, TimePoint newFinishedTime) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.finishedTimeChanged(oldFinishedTime, newFinishedTime)));
    }

    @Override
    public void delayToLiveChanged(long delayToLiveInMillis) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.delayToLiveChanged(delayToLiveInMillis)));
    }

    @Override
    public void windSourcesToExcludeChanged(Iterable<? extends WindSource> windSourcesToExclude) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.windSourcesToExcludeChanged(windSourcesToExclude)));
    }

    /**
     * If the status changes away from a {@link TrackedRaceStatusEnum#LOADING} or {@link TrackedRaceStatusEnum#PREPARED}
     * status then any {@link #tasksEnqueuedWhileRaceStillLoading enqueued} tasks will first be submitted for execution
     * before submitting the task to pass the status change itself on to all rules.
     */
    @Override
    public void statusChanged(TrackedRaceStatus newStatus, TrackedRaceStatus oldStatus) {
        synchronized (this) {
            if (isTaskExecutionToBeSuspendedBasedOnRaceStatus(oldStatus.getStatus()) && !isTaskExecutionToBeSuspendedBasedOnRaceStatus(newStatus.getStatus())) {
                logger.info("Executing "+tasksEnqueuedWhileRaceStillLoading.size()+" enqueued tasks for "+trackedRace.getRaceIdentifier());
                while (!tasksEnqueuedWhileRaceStillLoading.isEmpty()) {
                    backgroundExecutor.submit(tasksEnqueuedWhileRaceStillLoading.pop());
                }
            }
        }
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.statusChanged(newStatus, oldStatus)));
    }

    @Override
    public void competitorSensorTrackAdded(DynamicSensorFixTrack<Competitor, ?> track) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.competitorSensorTrackAdded(track)));
    }

    @Override
    public void competitorSensorFixAdded(Competitor competitor, String trackName, SensorFix fix,
            AddResult addedOrReplaced) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.competitorSensorFixAdded(competitor, trackName, fix, addedOrReplaced)));
    }

    @Override
    public void regattaLogAttached(RegattaLog regattaLog) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.regattaLogAttached(regattaLog)));
    }

    @Override
    public void raceLogAttached(RaceLog raceLog) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.raceLogAttached(raceLog)));
    }

    @Override
    public void raceLogDetached(RaceLog raceLog) {
        executeInBackgroundWithCurrentSubject(()->
            rules.forEach(r->r.raceLogDetached(raceLog)));
    }
}
