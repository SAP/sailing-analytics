package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.TimingConstants;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.tracking.CourseDesignChangedListener;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.NamedReentrantReadWriteLock;

public class DynamicTrackedRaceImpl extends TrackedRaceImpl implements
DynamicTrackedRace, GPSTrackListener<Competitor, GPSFixMoving> {
    private static final long serialVersionUID = 1092726918239676958L;

    private static final Logger logger = Logger.getLogger(DynamicTrackedRaceImpl.class.getName());

    private transient Set<RaceChangeListener> listeners;

    private boolean raceIsKnownToStartUpwind;

    private boolean delayToLiveInMillisFixed;
    
    private transient DynamicTrackedRaceLogListener logListener;

    private transient Set<CourseDesignChangedListener> courseDesignChangedListeners;

    public DynamicTrackedRaceImpl(TrackedRegatta trackedRegatta, RaceDefinition race,
            WindStore windStore, long delayToLiveInMillis, long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            long delayForCacheInvalidationOfWindEstimation) {
        super(trackedRegatta, race, windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed,
                delayForCacheInvalidationOfWindEstimation);
        this.logListener = new DynamicTrackedRaceLogListener(this);
        this.courseDesignChangedListeners = new HashSet<>();
        this.raceIsKnownToStartUpwind = race.getBoatClass().typicallyStartsUpwind();
        if (!raceIsKnownToStartUpwind) {
            Set<WindSource> windSourcesToExclude = new HashSet<WindSource>();
            for (WindSource windSourceToExclude : getWindSourcesToExclude()) {
                windSourcesToExclude.add(windSourceToExclude);
            }
            windSourcesToExclude.add(new WindSourceImpl(WindSourceType.COURSE_BASED));
            setWindSourcesToExclude(windSourcesToExclude);
        }
        for (Competitor competitor : getRace().getCompetitors()) {
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
            track.addListener(this);
        }
        // default wind tracks are observed because they are created by the superclass constructor using
        // createWindTrack which adds this object as a listener
    }

    /**
     * After de-serialization sets a valid {@link #listeners} collection which is transient and therefore
     * hasn't been serialized.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        listeners = new HashSet<RaceChangeListener>();
    }

    /**
     * {@link #raceIsKnownToStartUpwind} (see also {@link #raceIsKnownToStartUpwind()}) is initialized based on the <code>race</code>'s
     * {@link RaceDefinition#getBoatClass()} boat class's {@link BoatClass#typicallyStartsUpwind()} result. It can be changed
     * using {@link #setRaceIsKnownToStartUpwind(boolean)}. Uses <code>millisecondsOverWhichToAverageWind/2</code> for the
     * <code>delayForCacheInvalidationOfWindEstimation</code> argument of the constructor.<p>
     * 
     * Loading wind tracks from the <code>windStore</code> happens asynchronously which means that when the constructor returns,
     * the caller cannot assume that all wind tracks have yet been loaded completely. The caller may call {@link #waitUntilWindLoadingComplete()}
     * to wait until all persistent wind sources have been successfully and completely loaded.
     */
    public DynamicTrackedRaceImpl(TrackedRegatta trackedRegatta, RaceDefinition race,
            WindStore windStore, long delayToLiveInMillis,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed) {
        this(trackedRegatta, race, windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed,
                millisecondsOverWhichToAverageWind/2);
    }

    @Override
    public void recordFix(Competitor competitor, GPSFixMoving fix) {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
        track.addGPSFix(fix); // the track notifies this tracked race which in turn notifies its listeners
    }

    @Override
    public void setStatus(TrackedRaceStatus newStatus) {
        super.setStatus(newStatus);
        notifyListeners(newStatus);
    }

    @Override
    public void recordFix(Mark mark, GPSFix fix) {
        getOrCreateTrack(mark).addGPSFix(fix);
    }

    @Override
    public void setMillisecondsOverWhichToAverageSpeed(long millisecondsOverWhichToAverageSpeed) {
        this.millisecondsOverWhichToAverageSpeed = millisecondsOverWhichToAverageSpeed; 
        for (Competitor competitor : getRace().getCompetitors()) {
            getTrack(competitor).setMillisecondsOverWhichToAverage(millisecondsOverWhichToAverageSpeed);
        }
        for (Waypoint waypoint : getRace().getCourse().getWaypoints()) {
            for (Mark mark : waypoint.getMarks()) {
                getOrCreateTrack(mark).setMillisecondsOverWhichToAverage(millisecondsOverWhichToAverageSpeed);
            }
        }
        updated(/* time point */null);
        triggerManeuverCacheRecalculationForAllCompetitors();
    }

    @Override
    public void setMillisecondsOverWhichToAverageWind(long millisecondsOverWhichToAverageWind) {
        long oldMillisecondsOverWhichToAverageWind = this.millisecondsOverWhichToAverageWind;
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        for (WindSource windSource : getWindSources()) {
            getOrCreateWindTrack(windSource).setMillisecondsOverWhichToAverage(millisecondsOverWhichToAverageWind);
        }
        updated(/* time point */null);
        triggerManeuverCacheRecalculationForAllCompetitors();
        notifyListenersWindAveragingChanged(oldMillisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageWind);
    }


    @Override
    public void setAndFixDelayToLiveInMillis(long delayToLiveInMillis) {
        super.setDelayToLiveInMillis(delayToLiveInMillis);
        delayToLiveInMillisFixed = true;
    }

    @Override
    public void setDelayToLiveInMillis(long delayToLiveInMillis) {
        if (!delayToLiveInMillisFixed && getDelayToLiveInMillis() != delayToLiveInMillis) {
            super.setDelayToLiveInMillis(delayToLiveInMillis);
            notifyListenersDelayToLiveChanged(delayToLiveInMillis);
        }
    }

    @Override
    public DynamicGPSFixTrack<Competitor, GPSFixMoving> getTrack(Competitor competitor) {
        return (DynamicGPSFixTrack<Competitor, GPSFixMoving>) super.getTrack(competitor);
    }

    @Override
    public DynamicGPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark) {
        return (DynamicGPSFixTrack<Mark, GPSFix>) super.getOrCreateTrack(mark);
    }

    @Override
    protected DynamicGPSFixTrackImpl<Mark> createMarkTrack(Mark mark) {
        DynamicGPSFixTrackImpl<Mark> result = super.createMarkTrack(mark);
        result.addListener(new GPSTrackListener<Mark, GPSFix>() {
            private static final long serialVersionUID = -2855787105725103732L;

            @Override
            public void gpsFixReceived(GPSFix fix, Mark mark) {
                triggerManeuverCacheRecalculationForAllCompetitors();
                notifyListeners(fix, mark);
            }

            @Override
            public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage,
                    long newMillisecondsOverWhichToAverage) {
                // nobody can currently listen for the change of the mark speed averaging because mark speed is not a value used
            }

            @Override
            public boolean isTransient() {
                return false;
            }
        });
        return result;
    }

    /**
     * Callers iterating over the result need to synchronize on the resulting collection while iterating
     * to avoid {@link ConcurrentModificationException}s.
     */
    private Set<RaceChangeListener> getListeners() {
        if (listeners == null) {
            listeners = new HashSet<RaceChangeListener>();
        }
        return listeners;
    }

    @Override
    public void addListener(RaceChangeListener listener) {
        synchronized (getListeners()) {
            getListeners().add(listener);
        }
    }

    @Override
    public void removeListener(RaceChangeListener listener) {
        synchronized (getListeners()) {
            getListeners().remove(listener);
        }
    }

    @Override
    public void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude) {
        super.setWindSourcesToExclude(windSourcesToExclude);
        notifyListenersWindSourcesToExcludeChanged(windSourcesToExclude);
    }

    private void notifyListenersWindSourcesToExcludeChanged(Iterable<? extends WindSource> windSourcesToExclude) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.windSourcesToExcludeChanged(windSourcesToExclude);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListenersWindSourcesToExcludeChanged(Iterable<WindSource>)", e);
            }
        }
    }

    private void notifyListenersRaceTimesChanged(TimePoint startOfTracking, TimePoint endOfTracking,
            TimePoint startTimeReceived) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.raceTimesChanged(startOfTracking, endOfTracking, startTimeReceived);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListenersRaceTimesChanged(TimePoint, TimePoint, TimePoint)", e);
            }
        }
    }

    private void notifyListeners(GPSFix fix, Mark mark) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.markPositionChanged(fix, mark);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListeners(GPSFix, Competitor)", e);
            }
        }
    }

    private void notifyListeners(GPSFixMoving fix, Competitor competitor) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.competitorPositionChanged(fix, competitor);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListeners(GPSFix, Competitor)", e);
            }
        }
    }

    private void notifyListeners(TrackedRaceStatus status) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.statusChanged(status);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListeners(TrackedRaceStatus)", e);
            }
        }
    }

    private void notifyListeners(Wind wind, WindSource windSource) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.windDataReceived(wind, windSource);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListeners(Wind)", e);
            }
        }
    }

    private void notifyListenersSpeedAveragingChanged(long oldMillisecondsOverWhichToAverageSpeed, long newMillisecondsOverWhichToAverageSpeed) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.speedAveragingChanged(oldMillisecondsOverWhichToAverageSpeed,
                        newMillisecondsOverWhichToAverageSpeed);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE,
                        "notifyListenersSpeedAveragingChanged(long, long)", e);
            }
        }
    }

    private void notifyListenersWindAveragingChanged(long oldMillisecondsOverWhichToAverageWind, long newMillisecondsOverWhichToAverageWind) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.windAveragingChanged(oldMillisecondsOverWhichToAverageWind,
                        newMillisecondsOverWhichToAverageWind);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE,
                        "notifyListenersWindAveragingChanged(long, long)", e);
            }
        }
    }

    private void notifyListenersDelayToLiveChanged(long delayToLiveInMillis) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.delayToLiveChanged(delayToLiveInMillis);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE,
                        "notifyListenersDelayToLiveChanged(long)", e);
            }
        }
    }

    private void notifyListenersWindRemoved(Wind wind, WindSource windSource) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.windDataRemoved(wind, windSource);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListenersWindRemoved(Wind)", e);
            }
        }
    }

    private void notifyListeners(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings, Iterable<MarkPassing> markPassings) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                listener.markPassingReceived(competitor, oldMarkPassings, markPassings);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListeners(MarkPassing)", e);
            }
        }
    }

    @Override
    public void updateMarkPassings(Competitor competitor, Iterable<MarkPassing> markPassings) {
        Map<Waypoint, MarkPassing> oldMarkPassings = new HashMap<Waypoint, MarkPassing>();
        MarkPassing oldStartMarkPassing = null;
        boolean requiresStartTimeUpdate = true;
        final NavigableSet<MarkPassing> markPassingsForCompetitor = getMarkPassings(competitor);
        lockForRead(markPassingsForCompetitor);
        try {
            for (MarkPassing oldMarkPassing : markPassingsForCompetitor) {
                if (oldStartMarkPassing == null) {
                    oldStartMarkPassing = oldMarkPassing;
                }
                oldMarkPassings.put(oldMarkPassing.getWaypoint(), oldMarkPassing);
            }
        } finally {
            unlockAfterRead(markPassingsForCompetitor);
        }
        clearMarkPassings(competitor);
        TimePoint timePointOfLatestEvent = new MillisecondsTimePoint(0);
        for (MarkPassing markPassing : markPassings) {
            // try to find corresponding old start mark passing
            if (oldStartMarkPassing != null
                    && markPassing.getWaypoint().getName().equals(oldStartMarkPassing.getWaypoint().getName())) {
                if (markPassing.getTimePoint() != null && oldStartMarkPassing.getTimePoint() != null
                        && markPassing.getTimePoint().equals(oldStartMarkPassing.getTimePoint())) {
                    requiresStartTimeUpdate = false;
                }
            }
            final NamedReentrantReadWriteLock markPassingsLock = getMarkPassingsLock(markPassingsForCompetitor);
            LockUtil.lockForWrite(markPassingsLock);
            try {
                if (!Util.contains(getRace().getCourse().getWaypoints(), markPassing.getWaypoint())) {
                    StringBuilder courseWaypointsWithID = new StringBuilder();
                    boolean first = true;
                    for (Waypoint courseWaypoint : getRace().getCourse().getWaypoints()) {
                        if (first) {
                            first = false;
                        } else {
                            courseWaypointsWithID.append(" -> ");
                        }
                        courseWaypointsWithID.append(courseWaypoint.toString());
                        courseWaypointsWithID.append(" (ID=");
                        courseWaypointsWithID.append(courseWaypoint.getId());
                        courseWaypointsWithID.append(")");
                    }
                    logger.severe("Received mark passing " + markPassing + " for race " + getRace()
                            + " for waypoint ID" + markPassing.getWaypoint().getId()
                            + " but the waypoint does not exist in course " + courseWaypointsWithID);
                } else {
                    markPassingsForCompetitor.add(markPassing);
                }
            } finally {
                LockUtil.unlockAfterWrite(markPassingsLock);
            }
            Collection<MarkPassing> markPassingsInOrderForWaypoint = getOrCreateMarkPassingsInOrderAsNavigableSet(markPassing
                    .getWaypoint());
            final NamedReentrantReadWriteLock markPassingsLock2 = getMarkPassingsLock(markPassingsInOrderForWaypoint);
            LockUtil.lockForWrite(markPassingsLock2);
            try {
                markPassingsInOrderForWaypoint.add(markPassing);
            } finally {
                LockUtil.unlockAfterWrite(markPassingsLock2);
            }
            if (markPassing.getTimePoint().compareTo(timePointOfLatestEvent) > 0) {
                timePointOfLatestEvent = markPassing.getTimePoint();
            }
        }
        updated(timePointOfLatestEvent);
        triggerManeuverCacheRecalculation(competitor);
        // update the race times like start, end and the leg times
        if (requiresStartTimeUpdate) {
            invalidateStartTime();
        }
        invalidateMarkPassingTimes();
        invalidateEndTime();

        // notify *after* all mark passings have been re-established; should avoid flicker
        notifyListeners(competitor, oldMarkPassings, markPassings);
    }

    @Override
    public Iterable<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint) {
        return (NavigableSet<MarkPassing>) super.getMarkPassingsInOrder(waypoint);
    }

    @Override
    public void lockForRead(Iterable<MarkPassing> markPassings) {
        getRace().getCourse().lockForRead();
        LockUtil.lockForRead(getMarkPassingsLock(markPassings));
    }

    @Override
    public void unlockAfterRead(Iterable<MarkPassing> markPassings) {
        LockUtil.unlockAfterRead(getMarkPassingsLock(markPassings));
        getRace().getCourse().unlockAfterRead();
    }

    private void clearMarkPassings(Competitor competitor) {
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        final NamedReentrantReadWriteLock markPassingsLock = getMarkPassingsLock(markPassings);
        LockUtil.lockForWrite(markPassingsLock);
        try {
            Iterator<MarkPassing> mpIter = markPassings.iterator();
            while (mpIter.hasNext()) {
                MarkPassing mp = mpIter.next();
                mpIter.remove();
                Collection<MarkPassing> markPassingsInOrder = getMarkPassingsInOrderAsNavigableSet(mp.getWaypoint());
                LockUtil.lockForWrite(getMarkPassingsLock(markPassingsInOrder));
                try {
                    markPassingsInOrder.remove(mp);
                } finally {
                    LockUtil.unlockAfterWrite(getMarkPassingsLock(markPassingsInOrder));
                }
            }
        } finally {
            LockUtil.unlockAfterWrite(markPassingsLock);
        }
    }

    @Override
    public void setStartTimeReceived(TimePoint startTimeReceived) {
        if ((startTimeReceived == null) != (getStartTimeReceived() == null)
                || (startTimeReceived != null && !startTimeReceived.equals(getStartTimeReceived()))) {
            super.setStartTimeReceived(startTimeReceived);
            notifyListenersRaceTimesChanged(getStartOfTracking(), getEndOfTracking(), getStartTimeReceived());
        }
    }

    @Override
    public void setStartOfTrackingReceived(TimePoint startOfTrackingReceived) {
        if ((getStartOfTracking() == null) != (startOfTrackingReceived == null)
                || (startOfTrackingReceived != null && !getStartOfTracking().equals(startOfTrackingReceived))) {
            super.setStartOfTrackingReceived(startOfTrackingReceived);
            notifyListenersRaceTimesChanged(getStartOfTracking(), getEndOfTracking(), getStartTimeReceived());
        }
    }

    @Override
    public void setEndOfTrackingReceived(TimePoint endOfTrackingReceived) {
        if ((getEndOfTracking() == null) != (endOfTrackingReceived == null)
                || (endOfTrackingReceived != null && !getEndOfTracking().equals(endOfTrackingReceived))) {
            super.setEndOfTrackingReceived(endOfTrackingReceived);
            notifyListenersRaceTimesChanged(getStartOfTracking(), getEndOfTracking(), getStartTimeReceived());
        }
    }

    /**
     * In addition to calling the super class implementation, notifies all race listeners registered with this tracked
     * race using their {@link RaceChangeListener#windDataReceived(Wind, WindSource)} method. In particular this
     * replicates all wind fixes that may have been loaded from the wind store for the new track.
     */
    @Override
    protected WindTrack createWindTrack(WindSource windSource, long delayForWindEstimationCacheInvalidation) {
        WindTrack result = super.createWindTrack(windSource, delayForWindEstimationCacheInvalidation);
        if (windSource.getType().canBeStored()) {
            // replicate all wind fixed that may have been loaded by the wind store
            result.lockForRead();
            try {
                for (Wind wind : result.getRawFixes()) {
                    notifyListeners(wind, windSource); // Note that this doesn't notify the track's listeners but the tracked race's listeners.
                } // In particular, the wind store won't receive events (again) for the wind fixes it already has.
            } finally {
                result.unlockAfterRead();
            }
        }
        return result;
    }

    @Override
    public void recordWind(Wind wind, WindSource windSource) {
        // TODO check what a good filter is; remember that start/end of tracking may change over time; what if we have discarded valuable wind fixes?
        TimePoint startOfRace = getStartOfRace();
        TimePoint startOfTracking = getStartOfTracking();
        TimePoint endOfRace = getEndOfRace();
        TimePoint endOfTracking = getEndOfTracking();
        if ((startOfTracking == null || !startOfTracking.after(wind.getTimePoint()) ||
                (startOfRace != null && !startOfRace.after(wind.getTimePoint())))
            &&
        (endOfTracking == null || endOfTracking.plus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS).after(wind.getTimePoint()) ||
        (endOfRace != null && endOfRace.plus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS).after(wind.getTimePoint())))) {
            getOrCreateWindTrack(windSource).add(wind);
            updated(/* time point */null); // wind events shouldn't advance race time
            triggerManeuverCacheRecalculationForAllCompetitors();
            notifyListeners(wind, windSource);
        }
    }

    @Override
    public void removeWind(Wind wind, WindSource windSource) {
        getOrCreateWindTrack(windSource).remove(wind);
        updated(/* time point */null); // wind events shouldn't advance race time
        triggerManeuverCacheRecalculationForAllCompetitors();
        notifyListenersWindRemoved(wind, windSource);
    }

    @Override
    public void gpsFixReceived(GPSFixMoving fix, Competitor competitor) {
        updated(fix.getTimePoint());
        triggerManeuverCacheRecalculation(competitor);
        notifyListeners(fix, competitor);
    }

    @Override
    public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        notifyListenersSpeedAveragingChanged(oldMillisecondsOverWhichToAverage, newMillisecondsOverWhichToAverage);
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    protected TrackedLeg createTrackedLeg(Leg leg) {
        return new TrackedLegImpl(this, leg, getRace().getCompetitors());
    }

    @Override
    public long getMillisecondsOverWhichToAverageSpeed() {
        long result = 0; // default in case there is no competitor
        Iterator<Competitor> compIter = getRace().getCompetitors().iterator();
        if (compIter.hasNext()) {
            DynamicGPSFixTrack<Competitor, GPSFixMoving> someTrack = getTrack(compIter.next());
            result = someTrack.getMillisecondsOverWhichToAverageSpeed();
        }
        return result;
    }

    @Override
    public long getMillisecondsOverWhichToAverageWind() {
        long result = 0; // default in case there is no competitor
        for (WindSource windSource : getWindSources()) {
            WindTrack someTrack = getOrCreateWindTrack(windSource);
            result = someTrack.getMillisecondsOverWhichToAverageWind();
        }
        return result;
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        return (DynamicTrackedRegatta) super.getTrackedRegatta();
    }

    @Override
    public void setRaceIsKnownToStartUpwind(boolean raceIsKnownToStartUpwind) {
        this.raceIsKnownToStartUpwind = raceIsKnownToStartUpwind;
    }

    @Override
    public boolean raceIsKnownToStartUpwind() {
        return raceIsKnownToStartUpwind;
    }
    
    @Override
    public void attachRaceLog(RaceLog raceLog) {
        super.attachRaceLog(raceLog);
        logListener.addTo(attachedRaceLog);
    }
    
    @Override
    public void detachRaceLog() {
        logListener.removeFrom(attachedRaceLog);
        super.detachRaceLog();
    }

    @Override
    public void addCourseDesignChangedListener(CourseDesignChangedListener listener) {
        this.courseDesignChangedListeners.add(listener);
    }

    @Override
    public void onCourseDesignChangedByRaceCommittee(CourseBase newCourseDesign) {
        try {
            for (CourseDesignChangedListener courseDesignChangedListener : courseDesignChangedListeners) {
                courseDesignChangedListener.courseDesignChanged(newCourseDesign);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
