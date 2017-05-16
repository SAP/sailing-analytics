package com.sap.sailing.domain.test;

import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * See bug 4125 which handles race status updates more sophisticatedly.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TrackedRaceStatusUpdateTest extends TrackBasedTest {
    private Competitor competitor;
    private DynamicTrackedRace trackedRace;
    
    @Before
    public void setUp() {
        competitor = createCompetitor("Test Competitor");
        trackedRace = createTestTrackedRace("Test Regatta", "Test Race", "505", Collections.singleton(competitor), MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false);
    }
    
    @Test
    public void testSetPreparedStatus() {
        assertSame(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        trackedRace.onStatusChanged(new TrackingDataLoader() {}, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.PREPARED, 0.0));
        assertSame(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
    }
}
