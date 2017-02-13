package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;

public class RegattaLogCompetitorRegistrationDialog extends AbstractCompetitorRegistrationsDialog {

    public RegattaLogCompetitorRegistrationDialog(String boatClass, SailingServiceAsync sailingService,
            StringMessages stringMessages, ErrorReporter errorReporter, boolean editable, String leaderboardName,
            com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback<Set<CompetitorDTO>> callback) {
        super(sailingService, stringMessages, errorReporter, editable, callback, leaderboardName, boatClass, /* validator */ null);
    }

    @Override
    protected void setRegisteredCompetitors() {
        if (showOnlyCompetitorsOfLog()) {
            sailingService.getCompetitorRegistrationsInRegattaLog(leaderboardName,
                    new AsyncCallback<Collection<CompetitorDTO>>() {
                        @Override
                        public void onSuccess(Collection<CompetitorDTO> registeredCompetitors) {
                            move(allCompetitorsTable, registeredCompetitorsTable, registeredCompetitors);
                            validateAndUpdate();
                        }

                        @Override
                        public void onFailure(Throwable reason) {
                            errorReporter.reportError("Could not load already registered competitors: "
                                    + reason.getMessage());
                        }
                    });

        } else {
            sailingService.getCompetitorRegistrationsForLeaderboard(leaderboardName,
                    new AsyncCallback<Collection<CompetitorDTO>>() {
                        @Override
                        public void onSuccess(Collection<CompetitorDTO> registeredCompetitors) {
                            move(allCompetitorsTable, registeredCompetitorsTable, registeredCompetitors);
                            validateAndUpdate();
                        }

                        @Override
                        public void onFailure(Throwable reason) {
                            errorReporter.reportError("Could not load already registered competitors: "
                                    + reason.getMessage());
                        }
                    });
        }
    }

    @Override
    public void addAdditionalWidgets(FlowPanel mainPanel) {
    }
}
