package com.sap.sailing.domain.racelogtracking.impl;

import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.tracking.BravoExtendedFix;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.impl.BravoExtendedFixImpl;
import com.sap.sailing.domain.racelog.tracking.SensorFixMapper;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.CompetitorBravoFixTrackImpl;

/**
 * {@link SensorFixMapper} implementation to produce {@link BravoExtendedFix}es from {@link DoubleVectorFix}es
 * interpreted as "Expedition" data. The data has significant overlap with what we typically receive from the
 * "Bravo" devices. Ultimately, the naming may need to be adjusted, better reflecting that the "Bravo" extended
 * tracks now assume general responsibility for a broader set of sensor values that may be assigned to a
 * competitor, beyond just GPS tracking data.
 */
public class ExpeditionExtendedDataFixMapper implements SensorFixMapper<BravoFix, DynamicSensorFixTrack<Competitor, BravoFix>, Competitor> {

    @Override
    public DynamicSensorFixTrack<Competitor, BravoFix> getTrack(DynamicTrackedRace race, Competitor key) {
        return race.getOrCreateSensorTrack(key, BravoFixTrack.TRACK_NAME, 
                () -> new CompetitorBravoFixTrackImpl(key, BravoFixTrack.TRACK_NAME, /* hasExtendedFixes */ true,
                        race.getTrack(key)));
    }
    
    @Override
    public BravoExtendedFix map(DoubleVectorFix fix) {
        return new BravoExtendedFixImpl(fix);
    }
    
    @Override
    public boolean isResponsibleFor(Class<? extends RegattaLogDeviceMappingEvent<?>> eventType) {
        return RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl.class.isAssignableFrom(eventType)
                || RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl.class.isAssignableFrom(eventType);
    }
}