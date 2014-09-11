package com.sap.sailing.gwt.ui.actions;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sse.gwt.client.async.AsyncAction;

/**
 * An asynchronous action to retrieve simulation results from the sailing analytics server that are to be shown on the
 * {@link RaceMap}
 * 
 * @author Christopher Ronnewinkel (D036654)
 * 
 */
public class GetSimulationAction implements AsyncAction<SimulatorResultsDTO> {
    private final SailingServiceAsync sailingService;
    
    private final RegattaAndRaceIdentifier raceIdentifier;
    private final Date from;
    
    public GetSimulationAction(SailingServiceAsync sailingService, RegattaAndRaceIdentifier raceIdentifier, Date from) {
        this.sailingService = sailingService;
        this.raceIdentifier = raceIdentifier;
        this.from = from;
    }

    @Override
    public void execute(AsyncCallback<SimulatorResultsDTO> callback) {
        sailingService.getSimulatorResults(raceIdentifier, from, callback);
    }
}