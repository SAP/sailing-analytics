package com.sap.sailing.aiagent.persistence;

import java.util.UUID;

import com.mongodb.client.ClientSession;
import com.sap.sse.aicore.Credentials;

public interface MongoObjectFactory {
    void addEventToComment(UUID eventId, ClientSession clientSessionOrNull);
    void removeEventToComment(UUID eventId, ClientSession clientSessionOrNull);
    void clear(ClientSession clientSessionOrNull);
    void updateCredentials(Credentials credentials, ClientSession clientSessionOrNull);
}
