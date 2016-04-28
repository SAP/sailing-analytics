package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Set;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;

public class CompetitorInvitationHelper {
    
    private StringMessages stringMessages;
    private SailingServiceAsync sailingService;
    private ErrorReporter errorReporter;

    public CompetitorInvitationHelper(SailingServiceAsync sailingService, StringMessages stringMessages, ErrorReporter errorReporter) {
        this.stringMessages = stringMessages;
        this.sailingService = sailingService;
        this.errorReporter = errorReporter;
    }

    public void inviteCompetitors(Set<CompetitorDTO> competitors, String leaderboardName) {
        if (competitors.size() == 0){
            Window.alert(stringMessages.selectAtLeastOneCompetitorForInvitation());
        } else {
            boolean emailProvidedForAll = isEmailProvidedForAll(competitors);

            if (emailProvidedForAll) {
                openChooseEventDialogAndSendMails(competitors, leaderboardName);
            } else {
                Window.alert(stringMessages.notAllCompetitorsProvideEmail());
            }
        }
    }
    
    private boolean isEmailProvidedForAll(Iterable<CompetitorDTO> allCompetitors) {
        for (CompetitorDTO competitor : allCompetitors) {
            if (!competitor.hasEmail()) {
                return false;
            }
        }

        return true;
    }
    
    private void openChooseEventDialogAndSendMails(final Set<CompetitorDTO> competitors, final String leaderboardName) {
        new SelectEventAndHostnameDialog(sailingService, stringMessages, errorReporter, leaderboardName, new DialogCallback<Pair<EventDTO, String>>() {

            @Override
            public void ok(Pair<EventDTO, String> result) {
                sailingService.inviteCompetitorsForTrackingViaEmail(result.getB(), result.getA(), leaderboardName,
                        competitors,
                        stringMessages.appstoreSapSailingInsight(),
                                stringMessages.playstoreTrackingApp(),
                        getLocaleInfo(), new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(stringMessages.sendingMailsFailed() + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                Window.alert(stringMessages.sendingMailsSuccessful());
                            }
                        });
            }

            @Override
            public void cancel() {
                
            }
        }).show();
    }
    
    
    private String getLocaleInfo() {
        return LocaleInfo.getCurrentLocale().getLocaleName();
    }
}
