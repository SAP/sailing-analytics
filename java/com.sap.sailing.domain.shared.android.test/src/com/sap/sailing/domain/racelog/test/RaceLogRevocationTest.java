package com.sap.sailing.domain.racelog.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.in;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogChangedListener;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogChangedVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDenoteForTrackingEventImpl;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogRevocationTest {
    private RaceLog serverRaceLog;
    private AbstractLogEventAuthor serverAuthor = new LogEventAuthorImpl("server", 2);
    private RaceLog clientARaceLog;
    private UUID clientAUUID = UUID.randomUUID();
    private AbstractLogEventAuthor clientAAuthor = new LogEventAuthorImpl("clientA", 3);
    private RaceLog clientBRaceLog;
    private UUID clientBUUID = UUID.randomUUID();
    private AbstractLogEventAuthor clientBAuthor = new LogEventAuthorImpl("clientB", 1);
    private RaceLog clientCRaceLog;
    private UUID clientCUUID = UUID.randomUUID();
    private AbstractLogEventAuthor clientCAuthor = new LogEventAuthorImpl("clientC", 0);
    
    @BeforeEach
    public void setup() {
        serverRaceLog = new RaceLogImpl("server");
        clientARaceLog = new RaceLogImpl("clientA");
        clientBRaceLog = new RaceLogImpl("clientB");
        clientCRaceLog = new RaceLogImpl("clientC");
        
        addSendingListener(clientARaceLog, clientAUUID);
        addSendingListener(clientBRaceLog, clientBUUID);
        addSendingListener(clientCRaceLog, clientCUUID); 
    }
    
    private void addSendingListener(RaceLog raceLog, UUID uuid) {
        raceLog.addListener(new RaceLogChangedVisitor(new RaceLogChangedListener() {
            @Override
            public void eventAdded(RaceLogEvent event) {
                sendClientEvent(raceLog, event, uuid);
            }
        }));
    }
    
    private static TimePoint now() {
        return MillisecondsTimePoint.now();
    }
    
    private void sendClientEvent(RaceLog clientRaceLog, RaceLogEvent clientEvent,
            UUID clientUUID) {
        Iterable<RaceLogEvent> eventsToAdd = null;
        if (clientEvent == null) {
            eventsToAdd = serverRaceLog.getEventsToDeliver(clientUUID);
        } else {
            clientRaceLog.add(clientEvent);
            eventsToAdd = serverRaceLog.add(clientEvent, clientUUID);
        }
        for (RaceLogEvent eventToAdd : eventsToAdd) {
            clientRaceLog.add(eventToAdd);
        }
    }
    
    private void sendClientAEvent(RaceLogEvent event) {
        sendClientEvent(clientARaceLog, event, clientAUUID);
    }
    
    private void sendClientBEvent(RaceLogEvent event) {
        sendClientEvent(clientBRaceLog, event, clientBUUID);
    }
    
    private void sendClientCEvent(RaceLogEvent event) {
        sendClientEvent(clientCRaceLog, event, clientCUUID);
    }
    
    private static void assertNumUnrevoked(final RaceLog raceLog, int expectedNum) {
        raceLog.lockForRead();
        try {
            assertEquals(expectedNum, Util.size(raceLog.getUnrevokedEvents()));
        } finally {
            raceLog.unlockAfterRead();
        }
    }
    
    private static void assertNumAll(final RaceLog raceLog, int expectedNum) {
        raceLog.lockForRead();
        try {
            assertEquals(expectedNum, Util.size(raceLog.getRawFixes()));
        } finally {
            raceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void sameEventRevokedByTwoClientsBeforeSync() throws NotRevokableException {
        RaceLogDenoteForTrackingEvent event = new RaceLogDenoteForTrackingEventImpl(
                now(), serverAuthor, 0, "race", null, null);
        serverRaceLog.add(event);
        
        //sync events to clients
        sendClientAEvent(null);
        sendClientBEvent(null);
        sendClientCEvent(null);

        //they revoke the event independently
        try {
            clientARaceLog.revokeEvent(clientAAuthor, event);
        } catch (NotRevokableException e) {
            //expected
        }
        clientBRaceLog.revokeEvent(clientBAuthor, event);
        clientCRaceLog.revokeEvent(clientCAuthor, event);
        //expected, because A does not have sufficient priority
        assertThat("event not revoked due to insufficient author priority", event, is(in(clientARaceLog.getUnrevokedEvents())));
        
        //update clients
        sendClientAEvent(null);
        sendClientBEvent(null);
        sendClientCEvent(null);
        
        //check if the event has been revoked on the server and clients
        assertNumUnrevoked(clientARaceLog, 0);
        assertNumAll(clientARaceLog, 3);
        assertNumUnrevoked(clientBRaceLog, 0);
        assertNumAll(clientBRaceLog, 3);
        assertNumUnrevoked(clientCRaceLog, 0);
        assertNumAll(clientCRaceLog, 3);
        assertNumUnrevoked(serverRaceLog, 0);
        assertNumAll(serverRaceLog, 3);
    }
}
