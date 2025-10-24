package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Receives GPS tracks from a race. One test (if not ignored) stores these tracks in the resources/
 * folder for later (fast, off-line and reproducible) use by other tests. The other tests apply
 * smoothening and ensure that smoothening filters out serious outliers but doesn't alter good
 * tracks that don't have outliers.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class TrackSmootheningTest extends StoredTrackBasedTest {
    private final Map<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> tracks;

    public TrackSmootheningTest() throws URISyntaxException, MalformedURLException {
        tracks = new HashMap<Competitor, DynamicGPSFixTrack<Competitor,GPSFixMoving>>();
    }
    
    /**
     * Sets up a single listener so that the rather time-consuming race setup is received only once, and all
     * tests in this class share a single feed execution. The listener fills in the first event received
     * into {@link #firstTracked} and {@link #firstData}. All events are converted into {@link GPSFixMovingImpl}
     * objects and appended to the {@link DynamicTrackedRace}s.
     */
    @BeforeEach
    public void setupListener() throws InterruptedException, FileNotFoundException, IOException {
        tracks.putAll(loadTracks());
    }

    protected String getExpectedEventName() {
        return "Kieler Woche";
    }

    private DynamicGPSFixTrack<Competitor, GPSFixMoving> getTrackByCompetitorName(String name) {
        for (Map.Entry<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> e : tracks.entrySet()) {
            if (e.getKey().getName().equals(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    protected void assertOutlierInTrack(DynamicGPSFixTrack<Competitor, GPSFixMoving> track) {
        track.lockForRead();
        try {
            GPSFixMoving outlier = getAnyOutlier(track.getRawFixes());
            assertNotNull(outlier); // assert that we found an outlier
        } finally {
            track.unlockAfterRead();
        }
    }

    protected void assertNoOutlierInSmoothenedTrack(DynamicGPSFixTrack<Competitor, GPSFixMoving> track) {
        track.lockForRead();
        try {
            Iterable<GPSFixMoving> fixes = track.getFixes();
            GPSFixMoving outlier = getAnyOutlier(fixes);
            assertNull(outlier, "Found unexpected outlier " + outlier + " in smoothened track"); // assert that we did not find an outlier
        } finally {
            track.unlockAfterRead();
        }
    }

    protected GPSFixMoving getAnyOutlier(Iterable<GPSFixMoving> fixes) {
        TimePoint lastTimePoint = null;
        GPSFixMoving lastFix = null;
        GPSFixMoving outlier = null;
        for (GPSFixMoving fix : fixes) {
            if (lastTimePoint != null) {
                TimePoint thisTimePoint = fix.getTimePoint();
                long intervalInMillis = thisTimePoint.asMillis()-lastTimePoint.asMillis();
                Distance distanceFromLast = lastFix.getPosition().getDistance(fix.getPosition());
                Speed speedBetweenFixes = distanceFromLast.inTime(intervalInMillis);
                if (speedBetweenFixes.getKnots() > 100) {
                    // then it's not an olympic-class sports boat but a GPS jump
                    outlier = fix;
                }
            }
            lastTimePoint = fix.getTimePoint();
            lastFix = fix;
        }
        return outlier;
    }

    @Test
    public void checkLoadedTracksCount() throws InterruptedException, FileNotFoundException, IOException {
        assertEquals(36, tracks.size());
    }
    
    @Test
    public void assertBirknersEquatorJump() {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrackByCompetitorName("Birkner");
        assertNotNull(track);
        assertOutlierInTrack(track);
        assertNoOutlierInSmoothenedTrack(track);
    }

    @Test
    public void assertPlattnersKielerFoerdeJump() {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrackByCompetitorName("Dr.Plattner");
        assertNotNull(track);
        assertOutlierInTrack(track);
        assertNoOutlierInSmoothenedTrack(track);
    }

}
