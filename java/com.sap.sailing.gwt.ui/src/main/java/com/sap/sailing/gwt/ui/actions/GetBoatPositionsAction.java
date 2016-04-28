package com.sap.sailing.gwt.ui.actions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.CompactBoatPositionsDTO;

public class GetBoatPositionsAction extends AbstractGetMapRelatedDataAction<CompactBoatPositionsDTO> {
    public GetBoatPositionsAction(SailingServiceAsync sailingService,
            RegattaAndRaceIdentifier raceIdentifier, Map<CompetitorDTO, Date> from,
            Map<CompetitorDTO, Date> to, boolean extrapolate) {
        super(sailingService, raceIdentifier, from, to, extrapolate);
    }
    
    @Override
    public void execute(final AsyncCallback<CompactBoatPositionsDTO> callback) {
        Map<String, Date> fromByCompetitorIdAsString = new HashMap<String, Date>();
        for (Map.Entry<CompetitorDTO, Date> fromEntry : getFrom().entrySet()) {
            fromByCompetitorIdAsString.put(fromEntry.getKey().getIdAsString(), fromEntry.getValue());
        }
        Map<String, Date> toByCompetitorIdAsString = new HashMap<String, Date>();
        for (Map.Entry<CompetitorDTO, Date> toEntry : getTo().entrySet()) {
            toByCompetitorIdAsString.put(toEntry.getKey().getIdAsString(), toEntry.getValue());
        }
        getSailingService().getBoatPositions(getRaceIdentifier(), fromByCompetitorIdAsString, toByCompetitorIdAsString,
                isExtrapolate(), new AsyncCallback<CompactBoatPositionsDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }

                    @Override
                    public void onSuccess(CompactBoatPositionsDTO result) {
                        callback.onSuccess(result);
                    }
                });
    }
}
