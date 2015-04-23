package com.sap.sailing.domain.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.junit.Before;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class StoredTrackBasedTestWithTrackedRace extends StoredTrackBasedTest {
    @Before
    public void setUp() throws FileNotFoundException, IOException {
        Map<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> tracks = loadTracks();
        setTrackedRace(createTestTrackedRace("Kieler Woche", "505 Race 2", "505", tracks.keySet(),
                new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime())));
        copyTracks(tracks);
    }
    
    private void copyTracks(Map<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> tracks) {
        for (Map.Entry<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> e : tracks.entrySet()) {
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrackedRace().getTrack(e.getKey());
            e.getValue().lockForRead();
            try {
                for (GPSFixMoving fix : e.getValue().getRawFixes()) {
                    track.addGPSFix(fix);
                }
            } finally {
                e.getValue().unlockAfterRead();
            }
            List<MarkPassing> markPassings = new ArrayList<MarkPassing>();
            // add a mark passing for the start gate at the very beginning to make sure everyone is on a valid leg
            markPassings.add(new MarkPassingImpl(track.getFirstRawFix().getTimePoint(), getTrackedRace().getRace()
                    .getCourse().getWaypoints().iterator().next(), e.getKey()));
            getTrackedRace().updateMarkPassings(e.getKey(), markPassings);
        }
    }

    protected Competitor getCompetitorByName(String name) {
        for (Competitor c : getTrackedRace().getRace().getCompetitors()) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

}
