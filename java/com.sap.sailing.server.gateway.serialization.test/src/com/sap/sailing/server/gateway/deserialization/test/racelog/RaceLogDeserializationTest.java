package com.sap.sailing.server.gateway.deserialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogEventDeserializer;

public class RaceLogDeserializationTest {
    private RaceLog parsedRaceLog;
    
    @BeforeEach
    public void setUp() throws IOException, ParseException {
        parsedRaceLog = deserializeFromFile();
    }
    
    private static RaceLog deserializeFromFile() throws IOException, ParseException {
        InputStream raceLogStream = RaceLogDeserializationTest.class.getResourceAsStream("/racelog.json");
        RaceLogDeserializer deserializer = new RaceLogDeserializer(RaceLogEventDeserializer.create(DomainFactory.INSTANCE));
        
        Object parsedRaceLog = JSONValue.parseWithException(new InputStreamReader(raceLogStream));
        JSONObject parsedRaceLogJson = Helpers.toJSONObjectSafe(parsedRaceLog);
        
        return deserializer.deserialize(parsedRaceLogJson);
    }
    
    @Test
    public void testNextStatusDeserialization() throws IOException, ParseException {
        parsedRaceLog.lockForRead();
        for (RaceLogEvent event : parsedRaceLog.getFixes()) {
            if (event instanceof RaceLogRaceStatusEvent) {
                RaceLogRaceStatus status = ((RaceLogRaceStatusEvent) event).getNextStatus();
                if (status == null) {
                    System.out.println(event);
                }
                assertNotNull(status);
            }
        }
        parsedRaceLog.unlockAfterRead();
    }
}
