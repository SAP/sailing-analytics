package com.sap.sailing.domain.windfinderadapter.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.WindTracker;
import com.sap.sse.common.Util.Pair;

public class WindFinderPollerTest {
    @Test
    public void testPolling() throws Exception {
        final Object monitor = new Object();
        WindFinderTrackerFactoryImpl factory = new WindFinderTrackerFactoryImpl();
        factory.addReviewedSpotCollection(new ReviewedSpotsCollectionImpl("schilksee"));
        final List<Pair<Wind, WindSource>> wind = Collections.synchronizedList(new ArrayList<>());
        final RaceDefinition mockedRaceDefinition = mock(RaceDefinition.class);
        final Regatta mockedRegatta = mock(Regatta.class);
        final DynamicTrackedRegatta mockedTrackedRegatta = mock(DynamicTrackedRegatta.class);
        final DynamicTrackedRace mockedTrackedRace = mock(DynamicTrackedRace.class);
        when(mockedTrackedRace.getTrackedRegatta()).thenReturn(mockedTrackedRegatta);
        when(mockedTrackedRegatta.getRegatta()).thenReturn(mockedRegatta);
        when(mockedTrackedRegatta.getExistingTrackedRace(mockedRaceDefinition)).thenReturn(mockedTrackedRace);
        when(mockedTrackedRegatta.getTrackedRace(mockedRaceDefinition)).thenReturn(mockedTrackedRace);
        when(mockedTrackedRace.recordWind(any(Wind.class), any(WindSourceWithAdditionalID.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                wind.add(new Pair<>(invocation.getArgument(0, Wind.class), invocation.getArgument(1, WindSourceWithAdditionalID.class)));
                synchronized (monitor) {
                    monitor.notifyAll();
                }
                return true;
            }
        });
        when(mockedTrackedRace.recordWind(any(Wind.class), any(WindSourceWithAdditionalID.class), /* applyFilter */ anyBoolean())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                wind.add(new Pair<>(invocation.getArgument(0, Wind.class), invocation.getArgument(1, WindSourceWithAdditionalID.class)));
                return true;
            }
        });
        final WindTracker tracker;
        synchronized (monitor) {
            tracker = factory.createWindTracker(mockedTrackedRegatta, mockedRaceDefinition, /* correctByDeclination */ false,
                    null);
            monitor.wait(5000);
        }
        tracker.stop();
        synchronized (wind) {
            assertFalse(wind.isEmpty()); // at least one latest measurement
            Pair<Wind, WindSource> firstFix = wind.iterator().next();
            assertTrue(new HashSet<>(Arrays.asList("de15", "10044N")).contains(((WindSourceWithAdditionalID) firstFix.getB()).getId()));
        }
    }
}
