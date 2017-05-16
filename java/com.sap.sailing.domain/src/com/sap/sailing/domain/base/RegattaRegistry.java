package com.sap.sailing.domain.base;

import java.util.ConcurrentModificationException;

import com.sap.sailing.domain.common.RegattaIdentifier;

public interface RegattaRegistry {
    /**
     * @return a thread-safe copy of the regattas currently known by the service; it's safe for callers to iterate over
     *         the iterable returned, and no risk of a {@link ConcurrentModificationException} exists
     */
    Iterable<Regatta> getAllRegattas();
    
    Regatta getRegattaByName(String name);
    
    Regatta getRegatta(RegattaIdentifier regattaIdentifier);
}
