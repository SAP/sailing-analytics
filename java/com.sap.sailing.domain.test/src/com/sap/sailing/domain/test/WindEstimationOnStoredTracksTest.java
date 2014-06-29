package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sse.common.Util;

public class WindEstimationOnStoredTracksTest extends StoredTrackBasedTestWithTrackedRace {
    // ---------------- BearingWithConfidenceCluster -----------------
    
    @Test
    public void testSuccessfulTrackedRaceCreation() throws FileNotFoundException, IOException {
        assertNotNull(getTrackedRace());
        assertEquals(Util.size(getTrackedRace().getRace().getCompetitors()), loadTracks().size());
    }

    @Test
    public void testSimpleWindEstimation() throws NoWindException, FileNotFoundException, IOException {
        Map<String, Position> markPositions = new HashMap<>();
        markPositions.put("Left lee gate buoy", new DegreePosition(54.4680424, 10.234451));
        markPositions.put("Right lee gate buoy", new DegreePosition(54.4681424, 10.234451));
        markPositions.put("Windward mark", new DegreePosition(54.4680424, 10.034451));

        Competitor hasso = getCompetitorByName("Dr.Plattner");
        GPSFixTrack<Competitor, GPSFixMoving> hassosTrack = getTrackedRace().getTrack(hasso);
        TimePoint start = hassosTrack.getFirstRawFix().getTimePoint();
        TimePoint stop = hassosTrack.getLastRawFix().getTimePoint();
        TimePoint somewhenOnAnUpwind = new MillisecondsTimePoint(start.asMillis() + (stop.asMillis()-start.asMillis())*3/4);
        for (Mark mark : getTrackedRace().getMarks()) {
            getTrackedRace().getOrCreateTrack(mark).addGPSFix(
                    new GPSFixImpl(markPositions.get(mark.getName()), new MillisecondsTimePoint(0)));
            getTrackedRace().getOrCreateTrack(mark).addGPSFix(
                    new GPSFixImpl(markPositions.get(mark.getName()), somewhenOnAnUpwind));
        }
        getTrackedRace().recordWind(new WindImpl(markPositions.get("Windward mark"), new MillisecondsTimePoint(0),
                new KnotSpeedWithBearingImpl(
                /* speedInKnots */14.7, new DegreeBearingImpl(90))), new WindSourceImpl(WindSourceType.WEB));
        getTrackedRace().recordWind(new WindImpl(markPositions.get("Windward mark"), somewhenOnAnUpwind,
                new KnotSpeedWithBearingImpl(
                /* speedInKnots */14.7, new DegreeBearingImpl(90))), new WindSourceImpl(WindSourceType.WEB));
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(somewhenOnAnUpwind);
        assertNotNull(estimatedWindDirection);
    }
    
}
