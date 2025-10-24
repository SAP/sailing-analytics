package com.sap.sailing.domain.tractracadapter.impl;

import java.util.logging.Logger;

import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.tractrac.model.lib.api.data.IPosition;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.model.lib.api.map.IPositionedItem;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.map.IPositionedItemPositionListener;

/**
 * The positions of the {@link IMapItem control points}s of a {@link Course} are received dynamically through a callback interface.
 * Therefore, when connected to an {@link Regatta}, and even after receiving the order of the marks for a race course,
 * these orders are not yet defined. An instance of this class can be used to create the listeners needed to receive
 * this information and set it on an {@link Regatta}'s {@link IMapItem control points}.
 * <p>
 * 
 * As a {@link MarkPositionReceiver} requires a tracked race in order to update the mark positions received, and since a
 * {@link TrackedRace} depends on the {@link RaceDefinition} which in turn depends on having received the race course
 * description, at least a {@link RaceCourseReceiver} has to be active as well. Otherwise, the first call to
 * {@link #getTrackedRace} will block forever.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class MarkPositionReceiver extends AbstractReceiverWithQueue<IPositionedItem, IPosition, Void> {
    private static final Logger logger = Logger.getLogger(MarkPositionReceiver.class.getName());
    
    private int received;
    private final IRace tractracRace;
    final IPositionedItemPositionListener listener;

    /**
     * In order to establish single marks by their native names in case they are also
     * part of gates or lines as virtual marks, it's a good idea to ensure their {@link Mark}
     * domain objects are created before the gates / lines create theirs. However, this must
     * not happen upon each mark position received. Therefore, we do this once per receiver
     * and remember having done so in this flag.
     */
    private boolean singleMarksEnsuredAlready;

    public MarkPositionReceiver(final DynamicTrackedRegatta trackedRegatta, IEvent tractracEvent,
            IRace tractracRace, Simulator simulator, final DomainFactory domainFactory, IEventSubscriber eventSubscriber,
            IRaceSubscriber raceSubscriber, long timeoutInMilliseconds) {
        super(domainFactory, tractracEvent, trackedRegatta, simulator, eventSubscriber, raceSubscriber, timeoutInMilliseconds);
        // assumption: there is currently only one race per TracTrac Event object
        this.tractracRace = tractracRace;
        if (tractracEvent.getRaces().isEmpty()) {
            throw new IllegalArgumentException("Can't receive mark positions from event "+tractracEvent.getName()+" that has no race");
        }
        listener = new IPositionedItemPositionListener() {
            @Override
            public void gotPositionedItemPosition(IPositionedItem mark, IPosition position) {
                enqueue(new Triple<IPositionedItem, IPosition, Void>(mark, position, null));
            }
        };
    }
    
    @Override
    public void subscribe() {
        getRaceSubscriber().subscribePositionedItemPositions(listener);
        startThread();
    }
    
    @Override
    protected void unsubscribe() {
        getRaceSubscriber().unsubscribePositionedItemPositions(listener);
    }

    @Override
    protected void handleEvent(Util.Triple<IPositionedItem, IPosition, Void> event) {
        /* bug 5919 (https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5919)
         * 
         * TracTrac has re-keyed all mark IDs from old events.
         * See https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5919#c12.
         * Furthermore, they have made the course area field immutable (see
         * https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5919#c9).
         * When cloning an event in the TracTrac system, new mark IDs are now
         * issued. Therefore, it seems safe to now filter for the mark's course area
         * in case both, mark and race, specify one.
         */
        final String markCourseArea = event.getA() == null ? null : event.getA().getCourseArea();
        final String raceCourseArea = tractracRace.getCourseArea();
        // if mark and race both specify a course area, accept the mark position only
        // if the course area matches:
        if (markCourseArea == null || raceCourseArea == null || markCourseArea.equals(raceCourseArea)) {
            if (received++ % 1000 == 0) {
                System.out.print("M");
                if ((received / 1000 + 1) % 80 == 0) {
                    System.out.println();
                }
            }
            if (!singleMarksEnsuredAlready) {
                singleMarksEnsuredAlready = true;
                ensureAllSingleMarksOfCourseAreaAreCreated(tractracRace);
            }
            Mark mark = getDomainFactory().getMark(event.getA());
            DynamicTrackedRace trackedRace = getTrackedRace(tractracRace);
            if (trackedRace != null) {
                GPSFixMoving markPosition = getDomainFactory().createGPSFixMoving(event.getB());
                if (getSimulator() != null) {
                    getSimulator().scheduleMarkPosition(mark, markPosition);
                } else {
                    trackedRace.recordFix(mark, markPosition);
                }
            } else {
                logger.warning("Couldn't find tracked race for race " + tractracRace.getName()
                        + ". Dropping mark position event " + event);
            }
        }
    }
}
