package com.sap.sailing.server.gateway.expeditionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.gateway.trackfiles.impl.ExpeditionCourseInferrer;
import com.sap.sailing.server.gateway.trackfiles.impl.ExpeditionStartData;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ExpeditionStartDataTest {
    private static final SimpleDateFormat TIMEPOINT_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Test
    public void readFile() throws IOException, FormatNotSupportedException, ParseException {
        final String filename = "2018Apr28_05.csv.gz";
        final ExpeditionStartData result = new ExpeditionCourseInferrer(
                RaceLogTrackingAdapterFactory.INSTANCE.getAdapter(DomainFactory.INSTANCE)).getStartData(
                        getClass().getResourceAsStream(filename), filename, Charset.forName("UTF-8"));
        assertNotNull(result);
        final Set<TimePoint> startTimes = new HashSet<>();
        Util.addAll(result.getStartTimes(), startTimes);
        assertTrue(startTimes.contains(new MillisecondsTimePoint(TIMEPOINT_FORMATTER.parse("2018-04-28T14:45:00.086+0200"))));
        assertTrue(startTimes.contains(new MillisecondsTimePoint(TIMEPOINT_FORMATTER.parse("2018-04-28T14:35:40.041+0200"))));
        assertTrue(startTimes.contains(new MillisecondsTimePoint(TIMEPOINT_FORMATTER.parse("2018-04-28T14:14:59.424+0200"))));
        assertTrue(startTimes.contains(new MillisecondsTimePoint(TIMEPOINT_FORMATTER.parse("2018-04-28T15:30:00.518+0200"))));
        assertTrue(startTimes.contains(new MillisecondsTimePoint(TIMEPOINT_FORMATTER.parse("2018-04-28T14:24:59.731+0200"))));
        assertEquals(34, Util.size(result.getStartLinePortFixes()));
        assertEquals(34, Util.size(result.getStartLineStarboardFixes()));
    }
}
