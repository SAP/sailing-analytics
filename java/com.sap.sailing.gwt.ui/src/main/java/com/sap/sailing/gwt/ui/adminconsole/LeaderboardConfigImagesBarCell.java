package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;

public class LeaderboardConfigImagesBarCell extends DefaultActionsImagesBarCell {
    public static final String ACTION_EDIT_SCORES = "ACTION_EDIT_SCORES";
    public static final String ACTION_EDIT_COMPETITORS = "ACTION_EDIT_COMPETITORS";
    public static final String ACTION_CONFIGURE_URL = "ACTION_CONFIGURE_URL";
    public static final String ACTION_OPEN_COACH_DASHBOARD = "ACTION_OPEN_COACH_DASHBOARD";
    public static final String ACTION_SHOW_REGATTA_LOG = "ACTION_SHOW_REGATTA_LOG";
    public static final String ACTION_CREATE_PAIRINGLIST = "ACTION_CREATE_PAIRINGLIST";
    public static final String ACTION_PRINT_PAIRINGLIST = "ACTION_PRINT_PAIRINGLIST";
    public static final String ACTION_COPY_PAIRINGLIST_FROM_OTHER_LEADERBOARD = "ACTION_COPY_PAIRINGLIST_FROM_OTHER_LEADERBOARD";

    private static AdminConsoleResources resources = GWT.create(AdminConsoleResources.class);
    private final StringMessages stringMessages;
    
    public LeaderboardConfigImagesBarCell(final StringMessages stringMessages) {
        super(stringMessages);
        this.stringMessages = stringMessages;
    }

    @Override
    protected Iterable<ImageSpec> getImageSpecs() {
        final StrippedLeaderboardDTO object = (StrippedLeaderboardDTO) getContext().getKey();
        final List<ImageSpec> result = new ArrayList<>();
        result.add(getUpdateImageSpec());
        result.add(new ImageSpec(ACTION_EDIT_SCORES, stringMessages.actionEditScores(), resources.scoresIcon()));
        result.add(new ImageSpec(ACTION_EDIT_COMPETITORS, stringMessages.actionEditCompetitors(), resources.competitorsIcon()));
        result.add(new ImageSpec(ACTION_CONFIGURE_URL, stringMessages.actionConfigureUrl(), resources.settingsActionIcon()));
        result.add(getDeleteImageSpec());
        result.add(getChangeOwnershipImageSpec());
        result.add(getChangeACLImageSpec());
        result.add(new ImageSpec(ACTION_OPEN_COACH_DASHBOARD, stringMessages.actionOpenDashboard(), resources.openCoachDashboard()));
        result.add(new ImageSpec(ACTION_SHOW_REGATTA_LOG, stringMessages.regattaLog(), resources.flagIcon()));
        result.add(new ImageSpec(ACTION_CREATE_PAIRINGLIST, stringMessages.pairingLists(), resources.pairingList()));
        result.add(new ImageSpec(ACTION_PRINT_PAIRINGLIST, stringMessages.print() + " " + stringMessages.pairingList(), resources.printPairingList()));
        if (object.type.isRegattaLeaderboard()) {
            result.add(new ImageSpec(ACTION_COPY_PAIRINGLIST_FROM_OTHER_LEADERBOARD,
                    stringMessages.copyPairingListFromOtherLeaderboard(), resources.copyPairingList()));
        }
        return result;
    }
}