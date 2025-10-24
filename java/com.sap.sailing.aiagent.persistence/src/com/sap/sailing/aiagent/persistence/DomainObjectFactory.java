package com.sap.sailing.aiagent.persistence;

import java.util.UUID;

import com.mongodb.client.ClientSession;
import com.sap.sse.aicore.Credentials;

public interface DomainObjectFactory {
    Iterable<UUID> getEventsToComment(ClientSession clientSessionOrNull);

    Credentials getCredentials(ClientSession clientSessionOrNull);
}
