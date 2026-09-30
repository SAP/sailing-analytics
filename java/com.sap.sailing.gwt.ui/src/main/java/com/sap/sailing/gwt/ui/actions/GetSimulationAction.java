package com.sap.sailing.gwt.ui.actions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.racemap.RaceSimulationOverlay;
import com.sap.sse.gwt.client.async.RetryableActionResult;
import com.sap.sse.gwt.client.async.RetryableAsyncAction;

/**
 * An asynchronous action to retrieve simulation results from the sailing analytics server which are then shown on the
 * {@link RaceSimulationOverlay} of {@link RaceMap}
 * 
 * @author Christopher Ronnewinkel (D036654)
 * 
 */
public class GetSimulationAction extends RetryableAsyncAction<SimulatorResultsDTO> {
    private final SailingServiceAsync sailingService;
    private final LegIdentifier legIdentifier;
    
    public GetSimulationAction(SailingServiceAsync sailingService, LegIdentifier legIdentifier) {
        this.sailingService = sailingService;
        this.legIdentifier = legIdentifier;
    }

    @Override
    public void executeOnce(AsyncCallback<RetryableActionResult<SimulatorResultsDTO>> callback) {
        sailingService.getSimulatorResults(legIdentifier, callback);
    }

    @Override
    protected int getMaximumNumberOfRetries() {
        return 100;
    }
}