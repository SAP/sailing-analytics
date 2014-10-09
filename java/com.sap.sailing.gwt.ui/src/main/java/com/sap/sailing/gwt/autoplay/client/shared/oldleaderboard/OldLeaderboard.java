package com.sap.sailing.gwt.autoplay.client.shared.oldleaderboard;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.common.client.i18n.TextMessages;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.common.client.DateAndTimeFormatterUtil;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;

public class OldLeaderboard extends Composite implements LeaderboardUpdateListener {
    private static LeaderboardUiBinder uiBinder = GWT.create(LeaderboardUiBinder.class);

    interface LeaderboardUiBinder extends UiBinder<Widget, OldLeaderboard> {
    }

    @UiField HTMLPanel oldLeaderboardPanel;
    @UiField HTMLPanel contentPanel;

    @UiField ParagraphElement lastScoringUpdateTimeDiv;
    @UiField ParagraphElement lastScoringUpdateTextDiv;
    @UiField ParagraphElement lastScoringCommentDiv;
    @UiField ParagraphElement scoringSchemeDiv;

    private LeaderboardPanel leaderboardPanel;

    public OldLeaderboard(LeaderboardPanel leaderboardPanel) {
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
            scoringSchemeDiv.setInnerText(leaderboard.scoringScheme != null ? ScoringSchemeTypeFormatter.format(leaderboard.scoringScheme, StringMessages.INSTANCE) : "");
            if (leaderboard.getTimePointOfLastCorrectionsValidity() != null) {
                Date lastCorrectionDate = leaderboard.getTimePointOfLastCorrectionsValidity();
                String lastUpdate = DateAndTimeFormatterUtil.defaultDateFormatter.render(lastCorrectionDate) + ", "
                        + DateAndTimeFormatterUtil.longTimeFormatter.render(lastCorrectionDate);
                lastScoringUpdateTimeDiv.setInnerText(lastUpdate);
                lastScoringUpdateTextDiv.setInnerText(TextMessages.INSTANCE.eventRegattaLeaderboardLastScoreUpdate());
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
