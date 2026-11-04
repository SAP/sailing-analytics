package com.sap.sailing.domain.common.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sap.sailing.domain.common.MasterDataImportObjectCreationCount;
import com.sap.sse.common.Util;


/**
 * Keeps track of how many instances of domain classes are created in the process of
 * the master data import.
 * 
 * Also keeps track of names and ids of the created objects to ignore one that are 
 * listed multiple times.
 * 
 * @author Frederik Petersen (D054528)
 *
 */
public class BaseMasterDataImportObjectCreationCountImpl implements MasterDataImportObjectCreationCount {
    private static final long serialVersionUID = -3677005970701170818L;
    private int leaderboardCount = 0;
    private int leaderboardGroupCount = 0;
    private int eventCount = 0;
    private int regattaCount = 0;
    private int mediaTrackCount = 0;
    private int trackedRacesCount = 0;
    private Set<String> namedOfOverwrittenRegattas = new HashSet<>();
    private List<UUID> eventIdsImported;

    public BaseMasterDataImportObjectCreationCountImpl() {
    };
    

    public BaseMasterDataImportObjectCreationCountImpl(int leaderboardCount, int leaderboardGroupCount, int eventCount,
            int regattaCount, int mediaTrackCount, int trackedRacesCount, Iterable<String> namesOfOverwrittenRegattas) {
        super();
        this.leaderboardCount = leaderboardCount;
        this.leaderboardGroupCount = leaderboardGroupCount;
        this.eventCount = eventCount;
        this.regattaCount = regattaCount;
        this.mediaTrackCount = mediaTrackCount;
        this.trackedRacesCount = trackedRacesCount;
        Util.addAll(namesOfOverwrittenRegattas, this.namedOfOverwrittenRegattas);
    }

    public void addOneLeaderboard(String name) {
        leaderboardCount++;
    }
    
    public void addOneLeaderboardGroup(String name) {
        leaderboardGroupCount++;
    }
    
    public void addOneEvent(String idAsString) {
        eventCount++;
    }

    public void addOneRegatta(String idAsString) {
        regattaCount++;
    }
    
    public void addOneTrackedRace(String idAsString) {
        trackedRacesCount++;
    }
    
    public void addOneMediaTrack() {
    	mediaTrackCount++;
    }

    @Override
    public int getLeaderboardCount() {
        return leaderboardCount;
    }

    @Override
    public int getLeaderboardGroupCount() {
        return leaderboardGroupCount;
    }

    @Override
    public int getEventCount() {
        return eventCount;
    }

    @Override
    public int getRegattaCount() {
        return regattaCount;
    }
    
    @Override
    public int getMediaTrackCount() {
    	return mediaTrackCount;
    }
    
    @Override
    public int getTrackedRacesCount() {
        return trackedRacesCount;
    }

    @Override
    public Iterable<String> getNamesOfOverwrittenRegattaNames() {
        return namedOfOverwrittenRegattas;
    }

    public void addOverwrittenRegattaName(String name) {
        namedOfOverwrittenRegattas.add(name);
    }

    @Override
    public Iterable<UUID> getEventIdsImported() {
        return eventIdsImported;
    }

    public void setEventIdsImported(final Iterable<UUID> eventIdsImported) {
        this.eventIdsImported = new ArrayList<>();
        Util.addAll(eventIdsImported, this.eventIdsImported);
    }
}
