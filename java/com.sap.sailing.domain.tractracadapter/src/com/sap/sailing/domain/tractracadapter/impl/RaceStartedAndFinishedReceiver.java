package com.sap.sailing.domain.tractracadapter.impl;

import java.util.logging.Logger;

import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.tractrac.model.lib.api.data.IStartStopData;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.race.IRaceStartStopTimesChangeListener;

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
public class RaceStartedAndFinishedReceiver extends AbstractReceiverWithQueue<IRace, IStartStopData, IStartStopData> {
    private static final Logger logger = Logger.getLogger(RaceStartedAndFinishedReceiver.class.getName());
    private final IRaceStartStopTimesChangeListener listener;

    public RaceStartedAndFinishedReceiver(DynamicTrackedRegatta trackedRegatta, IEvent tractracEvent,
            Simulator simulator, DomainFactory domainFactory, IEventSubscriber eventSubscriber,
            IRaceSubscriber raceSubscriber) {
        super(domainFactory, tractracEvent, trackedRegatta, simulator, eventSubscriber, raceSubscriber);
        listener = new IRaceStartStopTimesChangeListener() {
            @Override
            public void gotTrackingStartStopTime(IRace race, IStartStopData startStopData) {
                enqueue(new Triple<IRace, IStartStopData, IStartStopData>(race, startStopData, null));
            }
            
            @Override
            public void gotRaceStartStopTime(IRace race, IStartStopData startStopData) {
                enqueue(new Triple<IRace, IStartStopData, IStartStopData>(race, null, startStopData));
            }
        };
    }

    @Override
    public void subscribe() {
        getRaceSubscriber().subscribeRaceTimesChanges(listener);
        startThread();
    }
    
    @Override
    protected void unsubscribe() {
        getRaceSubscriber().unsubscribeRaceTimesChanges(listener);
    }

    /**
     * The B component is tracking start/stop times; the C component is race start/end times
     */
    @Override
    protected void handleEvent(Triple<IRace, IStartStopData, IStartStopData> event) {
        System.out.print("StartStop");
        DynamicTrackedRace trackedRace = getTrackedRace(event.getA());
        if (trackedRace != null) {
            IStartStopData startEndTrackingTimesData = event.getB();
            if (startEndTrackingTimesData != null) {
                final long startTrackingTime = startEndTrackingTimesData.getStartTime();
                TimePoint startOfTracking = getSimulator() == null ?
                        new MillisecondsTimePoint(startTrackingTime) :
                            getSimulator().advance(new MillisecondsTimePoint(startTrackingTime));
                if (startTrackingTime > 0) {
                    trackedRace.setStartOfTrackingReceived(startOfTracking);
                }
                final long endTrackingTime = startEndTrackingTimesData.getStopTime();
                TimePoint endOfTracking = getSimulator() == null ?
                        new MillisecondsTimePoint(endTrackingTime) :
                            getSimulator().advance(new MillisecondsTimePoint(endTrackingTime));
                if (endTrackingTime > 0) {
                    trackedRace.setEndOfTrackingReceived(endOfTracking);
                }
            }
            IStartStopData startEndRaceTimesData = event.getC();
            if (startEndRaceTimesData != null) {
                final long startTime = startEndRaceTimesData.getStartTime();
                TimePoint startOfRace = getSimulator() == null ?
                        new MillisecondsTimePoint(startTime) :
                            getSimulator().advance(new MillisecondsTimePoint(startTime));
                if (startTime > 0) {
                    trackedRace.setStartTimeReceived(startOfRace);
                }
                // Note that end of race can't currently be set on a tracked race
            }
        } else {
            logger.warning("Couldn't find tracked race for race " + event.getA().getName()
                    + ". Dropping start/stop event " + event);
        }
    }

}
