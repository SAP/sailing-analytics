package com.sap.sailing.domain.common.impl;

import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.common.MasterDataImportObjectCreationCount;


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
public class MasterDataImportObjectCreationCountImpl implements MasterDataImportObjectCreationCount {
    private static final long serialVersionUID = -3677005970701170818L;
    private int leaderboardCount = 0;
    private int leaderboardGroupCount = 0;
    private int eventCount = 0;
    private int regattaCount = 0;
    private int mediaTrackCount = 0;
    private int trackedRacesCount = 0;
    private Set<String> createdLeaderboards = new HashSet<>(); 
    private Set<String> createdLeaderboardGroups = new HashSet<>(); 
    private Set<String> createdEvents = new HashSet<>(); 
    private Set<String> createdRegattas = new HashSet<>(); 
    private Set<String> createdTrackedRaces = new HashSet<>();
    private Set<String> overwrittenRegattas = new HashSet<>();

    //For GWT serialization
    public MasterDataImportObjectCreationCountImpl() {};

    public void addOneLeaderboard(String name) {
        createdLeaderboards.add(name);
        leaderboardCount++;
    }
    
    public void addOneLeaderboardGroup(String name) {
        createdLeaderboardGroups.add(name);
        leaderboardGroupCount++;
    }
    
    public void addOneEvent(String idAsString) {
        createdEvents.add(idAsString);
        eventCount++;
    }

    public void addOneRegatta(String idAsString) {
        createdRegattas.add(idAsString);
        regattaCount++;
    }
    
    public void addOneTrackedRace(String idAsString) {
        createdTrackedRaces.add(idAsString);
        trackedRacesCount++;
    }
    
    public void addOneMediaTrack() {
    	mediaTrackCount++;
    }

    public void add(MasterDataImportObjectCreationCountImpl toAdd) {
        leaderboardCount = leaderboardCount + toAdd.leaderboardCount;
        leaderboardGroupCount = leaderboardGroupCount + toAdd.leaderboardGroupCount;
        eventCount = eventCount + toAdd.eventCount;
        regattaCount = regattaCount + toAdd.regattaCount;
        trackedRacesCount = trackedRacesCount + toAdd.trackedRacesCount;
        createdEvents.addAll(toAdd.createdEvents);
        createdLeaderboardGroups.addAll(toAdd.createdLeaderboardGroups);
        createdLeaderboards.addAll(toAdd.createdLeaderboards);
        createdRegattas.addAll(toAdd.createdRegattas);
        createdTrackedRaces.addAll(toAdd.createdTrackedRaces);
    }
    
    public boolean alreadyAddedLeaderboardWithName(String name) {
        return createdLeaderboards.contains(name);
    }
    
    public boolean alreadyAddedLeaderboardGroupWithName(String name) {
        return createdLeaderboardGroups.contains(name);
    }
    
    public boolean alreadyAddedEventWithId(String idAsString) {
        return createdEvents.contains(idAsString);
    }
    
    public boolean alreadyAddedRegattaWithId(String idAsString) {
        return createdRegattas.contains(idAsString);
    }
    
    public boolean alreadyAddedTrackedRaceWithId(String idAsString) {
        return createdTrackedRaces.contains(idAsString);
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
    public Iterable<String> getOverwrittenRegattaNames() {
        return overwrittenRegattas;
    }

    public void addOverwrittenRegattaName(String name) {
        overwrittenRegattas.add(name);
    }
}
