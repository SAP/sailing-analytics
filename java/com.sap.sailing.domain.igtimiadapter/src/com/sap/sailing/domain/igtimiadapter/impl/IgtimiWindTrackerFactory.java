package com.sap.sailing.domain.igtimiadapter.impl;

import java.util.WeakHashMap;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.WindTracker;
import com.sap.sailing.domain.tracking.WindTrackerFactory;

public class IgtimiWindTrackerFactory implements WindTrackerFactory {
    private static final Logger logger = Logger.getLogger(IgtimiWindTrackerFactory.class.getName());
    private final IgtimiConnectionFactoryImpl connectionFactory;
    private final WeakHashMap<RaceDefinition, WindTracker> trackersForRace;
    
    public IgtimiWindTrackerFactory(IgtimiConnectionFactoryImpl connectionFactory) {
        trackersForRace = new WeakHashMap<>();
        this.connectionFactory = connectionFactory;
    }

    @Override
    public WindTracker createWindTracker(DynamicTrackedRegatta trackedRegatta, RaceDefinition race,
            boolean correctByDeclination) throws Exception {
        DynamicTrackedRace trackedRace = trackedRegatta.getTrackedRace(race);
        IgtimiWindTracker windTracker = new IgtimiWindTracker(trackedRace, connectionFactory, this);
        trackersForRace.put(race, windTracker);
        return windTracker;
    }

    @Override
    public WindTracker getExistingWindTracker(RaceDefinition race) {
        return trackersForRace.get(race);
    }

    void windTrackerStopped(RaceDefinition race, IgtimiWindTracker igtimiWindTracker) {
        WindTracker removedTracker = trackersForRace.remove(race);
        if (removedTracker != igtimiWindTracker) {
            logger.warning("Expected to remove wind tracker "+igtimiWindTracker+" but did remove "+removedTracker);
        }
    }

    WindSource getWindSource(String deviceSerialNumber) {
        return new WindSourceWithAdditionalID(WindSourceType.EXPEDITION, deviceSerialNumber);
    }

}
