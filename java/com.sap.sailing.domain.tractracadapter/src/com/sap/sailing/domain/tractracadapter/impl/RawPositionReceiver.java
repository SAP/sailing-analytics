package com.sap.sailing.domain.tractracadapter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.maptrack.client.io.TypeController;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.tracking.DynamicTrackedEvent;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.util.Util.Triple;
import com.tractrac.clientmodule.Race;
import com.tractrac.clientmodule.RaceCompetitor;
import com.tractrac.clientmodule.data.CompetitorPositionRawData;
import com.tractrac.clientmodule.data.ICallbackData;

public class RawPositionReceiver extends AbstractReceiverWithQueue<RaceCompetitor, CompetitorPositionRawData, Boolean> {
    private static final Logger logger = Logger.getLogger(RawPositionReceiver.class.getName());

    public RawPositionReceiver(DynamicTrackedEvent trackedEvent, com.tractrac.clientmodule.Event tractracEvent, DomainFactory domainFactory) {
        super(domainFactory, tractracEvent, trackedEvent);
    }
    
    /**
     * Obtains the listener and starts a thread of this object which will block for events received.
     */
    @Override
    public Iterable<TypeController> getTypeControllersAndStart() {
        ICallbackData<RaceCompetitor, CompetitorPositionRawData> positionListener = new ICallbackData<RaceCompetitor, CompetitorPositionRawData>() {
            public void gotData(RaceCompetitor tracked,
                    CompetitorPositionRawData record, boolean isLiveData) {
                enqueue(new Triple<RaceCompetitor, CompetitorPositionRawData, Boolean>(tracked, record, isLiveData));
            }
        };
        List<TypeController> listeners = new ArrayList<TypeController>();
        for (Race race : getTracTracEvent().getRaceList()) {
            TypeController listener = CompetitorPositionRawData.subscribe(race,
                positionListener, /* fromTime */0 /* means ALL */, /* toTime */Long.MAX_VALUE);
            listeners.add(listener);
        }
        setAndStartThread(new Thread(this, getClass().getName()));
        return listeners;
    }

    @Override
    protected void handleEvent(Triple<RaceCompetitor, CompetitorPositionRawData, Boolean> event) {
        System.out.print("P");
        Race race = event.getA().getRace();
        DynamicTrackedRace trackedRace = getTrackedRace(race);
        if (trackedRace != null) {
            GPSFixMoving fix = getDomainFactory().createGPSFixMoving(event.getB());
            Competitor competitor = getDomainFactory().getOrCreateCompetitor(event.getA().getCompetitor());
            trackedRace.recordFix(competitor, fix);
        } else {
            logger.warning("Couldn't find tracked race for race " + race.getName()
                    + ". Dropping raw position event " + event);
        }
    }
}
