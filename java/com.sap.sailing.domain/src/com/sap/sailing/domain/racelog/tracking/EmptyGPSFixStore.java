package com.sap.sailing.domain.racelog.tracking;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sailing.domain.racelogtracking.DeviceMapping;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.WithID;

public enum EmptyGPSFixStore implements GPSFixStore {
    INSTANCE;

    @Override
    public void storeFix(DeviceIdentifier device, GPSFix fix) {
    }

    @Override
    public void loadCompetitorTrack(DynamicGPSFixTrack<Competitor, GPSFixMoving> track,
            RegattaLog log, Competitor competitor) {
    }

    @Override
    public void loadMarkTrack(DynamicGPSFixTrack<Mark, GPSFix> track,
            RegattaLog log, Mark mark) {
    }

    @Override
    public void addListener(FixReceivedListener<GPSFix> listener, DeviceIdentifier device) {
    }

    @Override
    public void removeListener(FixReceivedListener<GPSFix> listener) {
    }

    @Override
    public void loadCompetitorTrack(
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track,
            DeviceMapping<Competitor> mapping) {

    }

    @Override
    public void loadMarkTrack(DynamicGPSFixTrack<Mark, GPSFix> track,
            DeviceMapping<Mark> mapping) {

    }

    @Override
    public TimeRange getTimeRangeCoveredByFixes(DeviceIdentifier device) {
        return null;
    }

    @Override
    public long getNumberOfFixes(DeviceIdentifier device) {
        return 0;
    }

    @Override
    public void loadTrack(DynamicGPSFixTrack<WithID, ?> track, DeviceMapping<WithID> mapping)
            throws NoCorrespondingServiceRegisteredException, TransformationException {
    }

    @Override
    public void loadCompetitorTrack(DynamicGPSFixTrack<Competitor, GPSFixMoving> track, RegattaLog log,
            Competitor competitor, TimePoint start, TimePoint end) throws TransformationException {
    }

    @Override
    public void loadMarkTrack(DynamicGPSFixTrack<Mark, GPSFix> track, RegattaLog log, Mark mark,
            TimePoint start, TimePoint end) throws TransformationException, NoCorrespondingServiceRegisteredException {
    }
}
