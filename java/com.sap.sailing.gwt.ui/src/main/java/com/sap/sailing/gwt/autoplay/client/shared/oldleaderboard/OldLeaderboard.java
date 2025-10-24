package com.sap.sailing.gwt.autoplay.client.shared.oldleaderboard;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.MultiRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;

public class OldLeaderboard extends Composite implements LeaderboardUpdateListener {
    private static LeaderboardUiBinder uiBinder = GWT.create(LeaderboardUiBinder.class);

    interface LeaderboardUiBinder extends UiBinder<Widget, OldLeaderboard> {
    }

    @UiField HTMLPanel oldLeaderboardPanel;
    @UiField HTMLPanel contentPanel;

    @UiField DivElement lastScoringUpdateTimeDiv;
    @UiField DivElement lastScoringUpdateTextDiv;
    @UiField DivElement lastScoringCommentDiv;
    @UiField DivElement scoringSchemeDiv;

    private MultiRaceLeaderboardPanel leaderboardPanel;
    private final StringMessages stringmessages;

    public OldLeaderboard(MultiRaceLeaderboardPanel leaderboardPanel, StringMessages stringmessages) {
        this.stringmessages = stringmessages;
        this.leaderboardPanel = leaderboardPanel;

        OldLeaderboardResources.INSTANCE.css().ensureInjected();

        initWidget(uiBinder.createAndBindUi(this));

        oldLeaderboardPanel.add(this.leaderboardPanel);
    }

    public Widget getContentWidget() {
        return contentPanel;
    }

    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
        if(leaderboard != null) {
            lastScoringCommentDiv.setInnerText(leaderboard.getComment() != null ? leaderboard.getComment() : "");
            scoringSchemeDiv.setInnerText(leaderboard.scoringScheme != null
                    ? ScoringSchemeTypeFormatter.getDescription(leaderboard.scoringScheme, stringmessages) : "");
            if (leaderboard.getTimePointOfLastCorrectionsValidity() != null) {
                Date lastCorrectionDate = leaderboard.getTimePointOfLastCorrectionsValidity();
                String lastUpdate = DateAndTimeFormatterUtil.defaultDateFormatterUTC.render(lastCorrectionDate) + ", "
                        + DateAndTimeFormatterUtil.longTimeFormatterUTC.render(lastCorrectionDate);
                lastScoringUpdateTimeDiv.setInnerText(lastUpdate);
                lastScoringUpdateTextDiv.setInnerText(stringmessages.eventRegattaLeaderboardLastScoreUpdate());
            } else {
                lastScoringUpdateTimeDiv.setInnerText("");
                lastScoringUpdateTextDiv.setInnerText("");
            }
        }
    }

    @Override
    public void currentRaceSelected(RaceIdentifier raceIdentifier, RaceColumnDTO raceColumn) {
    }
}
