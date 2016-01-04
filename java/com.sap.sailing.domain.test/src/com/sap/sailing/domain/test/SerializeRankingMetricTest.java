package com.sap.sailing.domain.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelog.tracking.EmptyGPSFixStore;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.ranking.TimeOnTimeAndDistanceRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;

public class SerializeRankingMetricTest extends AbstractSerializationTest {
    @Test
    public void testTimeOnTimeAndDistanceRankingMetricSerialization() throws ClassNotFoundException, IOException {
        final BoatClass _49er = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");
        final TrackedRace trackedRace = new DynamicTrackedRaceImpl(new DynamicTrackedRegattaImpl(
                new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, "Regatta", /* boatClass */ _49er,
                        /* startDate */ null, /* endDate */ null, /* trackedRegattaRegistry */ null,
                        new LowPoint(), UUID.randomUUID(), /* courseArea */ null)),
                new RaceDefinitionImpl("Race", new CourseImpl("Course", Collections.<Waypoint>emptyList()) , 
                        /* boatClass */ _49er, Collections.<Competitor>emptySet()),
                Collections.<Sideline>emptyList(), EmptyWindStore.INSTANCE, EmptyGPSFixStore.INSTANCE, 0l, 0l, 0l,
                false, TimeOnTimeAndDistanceRankingMetric::new, null);
        RankingMetric clone = cloneBySerialization(trackedRace.getRankingMetric(), DomainFactory.INSTANCE);
        assertNotNull(clone);
        assertTrue(clone instanceof TimeOnTimeAndDistanceRankingMetric);
    }
}
