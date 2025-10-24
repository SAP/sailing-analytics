package com.sap.sailing.domain.tractracadapter.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.tractrac.model.lib.api.data.IControlPassing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.tractrac.model.lib.api.data.IControlPassings;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.control.IControlPassingsListener;

public class MarkPassingReceiver extends AbstractReceiverWithQueue<IRaceCompetitor, IControlPassings, Void> {
    private static final Logger logger = Logger.getLogger(MarkPassingReceiver.class.getName());
    private final IControlPassingsListener listener;
    
    public MarkPassingReceiver(DynamicTrackedRegatta trackedRegatta, IEvent tractracEvent,
            Simulator simulator, DomainFactory domainFactory, IEventSubscriber eventSubscriber,
            IRaceSubscriber raceSubscriber, long timeoutInMilliseconds) {
        super(domainFactory, tractracEvent, trackedRegatta, simulator, eventSubscriber, raceSubscriber, timeoutInMilliseconds);
        listener = new IControlPassingsListener() {
            @Override
            public void gotControlPassings(long timestamp, IRaceCompetitor raceCompetitor, IControlPassings controlPassings) {
                enqueue(new Triple<IRaceCompetitor, IControlPassings, Void>(raceCompetitor, controlPassings, null));
            }
        };
    }

    @Override
    public void subscribe() {
        getRaceSubscriber().subscribeControlPassings(listener);
        startThread();
    }
    
    @Override
    protected void unsubscribe() {
        getRaceSubscriber().unsubscribeControlPassings(listener);
    }

    protected void handleEvent(Util.Triple<IRaceCompetitor, IControlPassings, Void> event) {
    	if (!event.getA().getCompetitor().isNonCompeting()) {
            System.out.print("L"); // as in "Leg"
            DynamicTrackedRace trackedRace = getTrackedRace(event.getA().getRace());
            if (trackedRace != null) {
                Course course = trackedRace.getRace().getCourse();
                Iterator<Waypoint> waypointsIter = course.getWaypoints().iterator();
                Map<Waypoint, MarkPassing> passingsByWaypoint = new HashMap<Waypoint, MarkPassing>();
                // Note: the entries always describe all mark passings for the competitor so far in the current race in order
                for (IControlPassing passing : event.getB().getPassings()) {
                    IMapItem controlPointPassed = passing.getControl();
                    com.sap.sailing.domain.base.ControlPoint domainControlPoint = getDomainFactory().getOrCreateControlPoint(controlPointPassed);
                    Waypoint passed = findWaypointForControlPoint(trackedRace, waypointsIter, domainControlPoint);
                    if (passed != null) {
                        TimePoint time = new MillisecondsTimePoint(passing.getTimestamp());
                        Competitor competitor = getDomainFactory().resolveCompetitor(event.getA().getCompetitor());
                        MarkPassing markPassing = getDomainFactory().createMarkPassing(time, passed, competitor);
                        passingsByWaypoint.put(passed, markPassing);
                    } else {
                        logger.warning("Didn't find waypoint in course " + course + " for mark passing around " + passing.getControl());
                    }
                }
                List<MarkPassing> markPassings = new ArrayList<MarkPassing>();
                for (Waypoint waypoint : course.getWaypoints()) {
                    MarkPassing passing = passingsByWaypoint.get(waypoint);
                    if (passing != null) {
                        markPassings.add(passing);
                    }
                }
                logger.fine("Received mark passings in race " + trackedRace.getRace().getName() + ": " + markPassings);
                Competitor competitor = getDomainFactory().resolveCompetitor(event.getA().getCompetitor());
                if (competitor != null) {
                    if (getSimulator() != null) {
                        getSimulator().delayMarkPassings(competitor, markPassings);
                    } else {
                        trackedRace.updateMarkPassings(competitor, markPassings);
                    }
                } else {
                    logger.warning("Didn't find competitor for mark passings");
                }
            } else {
                logger.warning("Couldn't find tracked race for race " + event.getA().getRace().getName()
                        + ". Dropping mark passing event " + event);
            }
    	}
    }

    /**
     * Starts searching in <code>waypointsIter</code> for a waypoint that has the given <code>controlPoint</code>.
     * The <code>waypointsIter</code> is advanced to that point or to the point where <code>waypointsIter.hasNext()</code>
     * returns <code>false</code>.
     */
    private Waypoint findWaypointForControlPoint(TrackedRace trackedRace, Iterator<Waypoint> waypointsIter,
            com.sap.sailing.domain.base.ControlPoint domainControlPoint) {
        while (waypointsIter.hasNext()) {
            Waypoint waypoint = waypointsIter.next();
            if (waypoint.getControlPoint() == domainControlPoint) {
                return waypoint;
            }
        }
        return null;
    }
}
