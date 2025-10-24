package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.tracking.impl.CrossTrackErrorCache;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.common.TimePoint;

public class DynamicTrackedRaceImplSerializationTest extends AbstractSerializationTest {
    @Test
    public void testCrossTrackErrorCacheStaysListenerThroughSerialization() throws ClassNotFoundException, IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("Hasso");
        final Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(competitor, competitor.getBoat());
        final DynamicTrackedRaceImpl dtr = TrackBasedTest.createTestTrackedRace("Test", "Test", "470", competitorsAndBoats,
                /* timePointForFixes */ TimePoint.now(), /* useMarkPassingCalculator */ false);
        final DynamicTrackedRaceImpl dtrClone = cloneBySerialization(dtr, DomainFactory.INSTANCE);
        dtrClone.initializeAfterDeserialization();
        assertNotNull(dtrClone);
        final Field crossTrackErrorCacheField = TrackedRaceImpl.class.getDeclaredField("crossTrackErrorCache");
        crossTrackErrorCacheField.setAccessible(true);
        final CrossTrackErrorCache cted = (CrossTrackErrorCache) crossTrackErrorCacheField.get(dtr);
        final CrossTrackErrorCache ctedClone = (CrossTrackErrorCache) crossTrackErrorCacheField.get(dtrClone);
        assertNotNull(cted);
        assertNotNull(ctedClone);
        final Method getListeners = DynamicTrackedRaceImpl.class.getDeclaredMethod("getListeners");
        getListeners.setAccessible(true);
        final Set<?> dtrListeners = (Set<?>) getListeners.invoke(dtr);
        final Set<?> dtrCloneListeners = (Set<?>) getListeners.invoke(dtrClone);
        assertTrue(dtrListeners.contains(cted));
        assertTrue(dtrCloneListeners.contains(ctedClone));
    }
}
