package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;

import org.junit.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogDependentStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.tracking.StartTimeChangedListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TrackedRaceStartTimeUpdateForDependentStartTimeTest extends TrackBasedTest {
    @Test
    public void testStartTimeUpdateForRaceWithDependentStartTime() {
        final Competitor hasso = createCompetitor("Hasso");
        final RaceLog r1RaceLog = new RaceLogImpl("r1RaceLog");
        final RaceLog r2RaceLog = new RaceLogImpl("r2RaceLog");
        final RaceLogResolver raceLogResolver = new RaceLogResolver() {
            @Override
            public RaceLog resolve(SimpleRaceLogIdentifier identifier) {
                if (identifier.getRaceColumnName().equals("R1")) {
                    return r1RaceLog;
                } else if (identifier.getRaceColumnName().equals("R2")) {
                    return r2RaceLog;
                }
                return null;
            }
        };
        final TrackedRace r1 = createTestTrackedRace("Regatta", "R1", "J/70", Collections.singleton(hasso),
                MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false, raceLogResolver);
        final TrackedRace r2 = createTestTrackedRace("Regatta", "R2", "J/70", Collections.singleton(hasso),
                MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false, raceLogResolver);
        r1.attachRaceLog(r1RaceLog);
        r2.attachRaceLog(r2RaceLog);
        final TimePoint[] r1StartTime = new TimePoint[1];
        final TimePoint[] r2StartTime = new TimePoint[1];
        r1.addStartTimeChangedListener(new StartTimeChangedListener() {
            @Override
            public void startTimeChanged(TimePoint newTimePoint) throws MalformedURLException, IOException {
                r1StartTime[0] = newTimePoint;
            }
        });
        r2.addStartTimeChangedListener(new StartTimeChangedListener() {
            @Override
            public void startTimeChanged(TimePoint newTimePoint) throws MalformedURLException, IOException {
                r2StartTime[0] = newTimePoint;
            }
        });
        final AbstractLogEventAuthor author = new LogEventAuthorImpl("Axel", 0);
        final Duration startTimeDiff = Duration.ONE_MINUTE;
        r2RaceLog.add(new RaceLogDependentStartTimeEventImpl(MillisecondsTimePoint.now(), author, /* pass */ 0,
                new SimpleRaceLogIdentifierImpl(r1.getTrackedRegatta().getRegatta().getName(), "R1", "Default"), startTimeDiff));
        assertNull(r2StartTime[0]);
        final TimePoint r1StartTimeToSet = MillisecondsTimePoint.now();
        r1RaceLog.add(new RaceLogStartTimeEventImpl(r1StartTimeToSet, author, /* pass */ 0, r1StartTimeToSet));
        assertNotNull(r1StartTime[0]);
        assertEquals(r1StartTimeToSet, r1StartTime[0]);
        assertNotNull(r2StartTime[0]);
        assertEquals(r1StartTimeToSet.plus(startTimeDiff), r2StartTime[0]);
    }
}
