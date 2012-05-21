package com.sap.sailing.domain.tracking.impl;

import java.util.Collections;
import java.util.Map;

import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;

public class EmptyWindStore implements WindStore {
    public static EmptyWindStore INSTANCE = new EmptyWindStore();
    
    @Override
    public WindTrack getWindTrack(TrackedRegatta trackedRegatta, TrackedRace trackedRace, WindSource windSource,
            long millisecondsOverWhichToAverage, long delayForWindEstimationCacheInvalidation) {
        switch (windSource.getType()) {
        case COURSE_BASED:
            return new CourseBasedWindTrackImpl(trackedRace, millisecondsOverWhichToAverage, WindSourceType.COURSE_BASED.getBaseConfidence());
        case TRACK_BASED_ESTIMATION:
            return new TrackBasedEstimationWindTrackImpl(trackedRace, millisecondsOverWhichToAverage,
                    WindSourceType.TRACK_BASED_ESTIMATION.getBaseConfidence(), delayForWindEstimationCacheInvalidation);
        default:
            return new WindTrackImpl(millisecondsOverWhichToAverage, windSource.getType().useSpeed());
        }
    }

    @Override
    public Map<? extends WindSource, ? extends WindTrack> loadWindTracks(TrackedRegatta trackedRegatta,
            TrackedRace trackedRace, long millisecondsOverWhichToAverageWind) {
        return Collections.emptyMap();
    }

}
