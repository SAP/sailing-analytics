package com.sap.sailing.gwt.ui.actions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.racemap.RaceSimulationOverlay;
import com.sap.sse.gwt.client.async.AsyncAction;

/**
 * An asynchronous action to retrieve simulation results from the sailing analytics server which are then shown on the
 * {@link RaceSimulationOverlay} of {@link RaceMap}
 * 
 * @author Christopher Ronnewinkel (D036654)
 * 
 */
public class GetSimulationAction implements AsyncAction<SimulatorResultsDTO> {
    private final SailingServiceAsync sailingService;
    private final LegIdentifier legIdentifier;
    
    public GetSimulationAction(SailingServiceAsync sailingService, LegIdentifier legIdentifier) {
        this.sailingService = sailingService;
        this.legIdentifier = legIdentifier;
    }

    @Override
    public void execute(AsyncCallback<SimulatorResultsDTO> callback) {
        sailingService.getSimulatorResults(legIdentifier, callback);
    }
}