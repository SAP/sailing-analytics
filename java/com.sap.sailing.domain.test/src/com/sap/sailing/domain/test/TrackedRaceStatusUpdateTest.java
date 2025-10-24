package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CompetitorWithBoat;
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
    private CompetitorWithBoat competitor;
    private DynamicTrackedRace trackedRace;
    
    @BeforeEach
    public void setUp() {
        competitor = createCompetitorWithBoat("Test Competitor");
        trackedRace = createTestTrackedRace("Test Regatta", "Test Race", "505", createCompetitorAndBoatsMap(competitor), MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false);
    }
    
    @Test
    public void testSetPreparedStatus() {
        assertSame(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        trackedRace.onStatusChanged(new TrackingDataLoader() {}, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.PREPARED, 0.0));
        assertSame(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
    }
}
