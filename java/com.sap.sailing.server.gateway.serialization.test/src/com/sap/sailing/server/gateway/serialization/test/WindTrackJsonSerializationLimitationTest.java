package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.mockito.ArgumentMatchers;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.server.gateway.serialization.impl.DefaultWindTrackJsonSerializer;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

@Timeout(value=5, unit=TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
public class WindTrackJsonSerializationLimitationTest {
    private DefaultWindTrackJsonSerializer serializer;
    
    @Test
    public void testLimitAppliesToWindTrackSerialization() {
        final int MAX_FIXES = 100;
        final Random random = new Random();
        serializer = new DefaultWindTrackJsonSerializer(MAX_FIXES, TimePoint.BeginningOfTime, TimePoint.EndOfTime, new WindSourceImpl(WindSourceType.WEB));
        final WindTrack windTrack = mock(WindTrack.class);
        when(windTrack.getFixesIterator(ArgumentMatchers.any(TimePoint.class), ArgumentMatchers.anyBoolean(), ArgumentMatchers.any(TimePoint.class), ArgumentMatchers.anyBoolean()))
                .thenReturn(new Iterator<Wind>() {
                    @Override
                    public Wind next() {
                        return new WindImpl(new DegreePosition(random.nextDouble(), random.nextDouble()), TimePoint.now(),
                                new KnotSpeedWithBearingImpl(random.nextDouble(), new DegreeBearingImpl(random.nextDouble())));
                    }
                    
                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                });
        when(windTrack.getAveragedWind(ArgumentMatchers.any(Position.class), ArgumentMatchers.any(TimePoint.class)))
                .thenReturn(new WindImpl(new DegreePosition(random.nextDouble(), random.nextDouble()), TimePoint.now(),
                        new KnotSpeedWithBearingImpl(random.nextDouble(), new DegreeBearingImpl(random.nextDouble()))));
        final JSONObject json = serializer.serialize(windTrack);
        final Object windSourceKey = json.keySet().iterator().next();
        final JSONArray fixes = (JSONArray) json.get(windSourceKey);
        assertEquals(MAX_FIXES, fixes.size());
    }
}
