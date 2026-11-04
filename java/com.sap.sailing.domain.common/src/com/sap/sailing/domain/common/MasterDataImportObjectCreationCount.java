package com.sap.sailing.domain.common;

import java.io.Serializable;
import java.util.UUID;

public interface MasterDataImportObjectCreationCount extends Serializable {
    int getLeaderboardCount();

    int getLeaderboardGroupCount();

    int getEventCount();
    
    int getRegattaCount();

    int getMediaTrackCount();

    Iterable<String> getNamesOfOverwrittenRegattaNames();

    int getTrackedRacesCount();

    /**
     * IDs of all imported events, including events that already existed. Unlike the creation counts, these identify
     * the content whose remote references can be removed after archiving. {@code null} means an older producer did
     * not supply this information; an empty iterable means no events were imported.
     */
    Iterable<UUID> getEventIdsImported();
}
