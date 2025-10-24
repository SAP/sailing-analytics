package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Perform a few tests around race log and race log event serialization / deserialization
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SerializeRaceLogEventsTest {
    private ObjectOutputStream oos;
    private ByteArrayOutputStream bos;
    private RaceLog raceLog;
    
    @BeforeEach
    public void setUp() throws IOException {
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        raceLog = new RaceLogImpl("Test Race Log");
    }
    
    @Test
    public void testSimpleRaceLogSerialization() throws IOException, ClassNotFoundException {
        oos.writeObject(raceLog);
        ObjectInputStream ois = getObjectInputStream();
        RaceLog rl = (RaceLog) ois.readObject();
        raceLog.lockForRead();
        rl.lockForRead();
        try {
            assertEquals(Util.size(raceLog.getRawFixes()), Util.size(rl.getRawFixes()));
        } finally {
            rl.unlockAfterRead();
            raceLog.unlockAfterRead();
        }
    }

    @Test
    public void testRaceLogSerializationWithSingleEvent() throws IOException, ClassNotFoundException {
        RaceLogStartTimeEvent startTimeEvent = new RaceLogStartTimeEventImpl(MillisecondsTimePoint.now(),
                new LogEventAuthorImpl("Author Name", /* priority */0), /* passId */1, /* startTime */
                MillisecondsTimePoint.now(), /* courseAreaId */ null);
        raceLog.add(startTimeEvent);
        oos.writeObject(raceLog);
        ObjectInputStream ois = getObjectInputStream();
        RaceLog rl = (RaceLog) ois.readObject();
        raceLog.lockForRead();
        rl.lockForRead();
        try {
            assertEquals(Util.size(raceLog.getRawFixes()), Util.size(rl.getRawFixes()));
            assertEquals(((RaceLogStartTimeEvent) raceLog.getFirstRawFix()).getStartTime(), ((RaceLogStartTimeEvent) rl.getFirstRawFix()).getStartTime());
        } finally {
            rl.unlockAfterRead();
            raceLog.unlockAfterRead();
        }
    }

    private ObjectInputStream getObjectInputStream() throws IOException {
        return new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
    }
}
