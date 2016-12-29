package com.sap.sailing.server.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.tracking.AbstractRaceTrackerBaseImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.WindStore;

public class RaceTrackerMock extends AbstractRaceTrackerBaseImpl {
    
    private Long id;
    private Regatta regatta;
    private Set<RaceDefinition> raceDefinitions;
    private boolean isTracking;
    
    public RaceTrackerMock() {
        super(null);
    }
    
    
    
    public RaceTrackerMock(Long id, Regatta regatta, Set<RaceDefinition> raceDefinitions, boolean isTracking) {
        super(null);
        this.id = id;
        this.regatta = regatta;
        this.raceDefinitions = raceDefinitions;
        this.isTracking = isTracking;
    }

    public void setIsTracking(boolean isTracking){
        this.isTracking = isTracking;
    }
    
    public boolean getIsTracking(){
        return isTracking;
    }

    public RaceTrackerMock(Long id) {
        super(null);
        this.id = id;
    }

    @Override
    protected void onStop(boolean preemptive) throws MalformedURLException, IOException, InterruptedException {
        isTracking = false;
    }

    @Override
    public Regatta getRegatta() {
        return regatta;
    }

    @Override
    public Set<RaceDefinition> getRaces() {
        return raceDefinitions;
    }

    @Override
    public RaceHandle getRaceHandle() {
        throw new RuntimeException("No race Handle in RaceTrackerMock");
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WindStore getWindStore() {
        throw new RuntimeException("No wind store in racetracker mock");
    }

    @Override
    public Object getID() {
        return id;
    }

    @Override
    public Set<RegattaAndRaceIdentifier> getRaceIdentifiers() {
        // TODO Auto-generated method stub
        return null;
    }
}
