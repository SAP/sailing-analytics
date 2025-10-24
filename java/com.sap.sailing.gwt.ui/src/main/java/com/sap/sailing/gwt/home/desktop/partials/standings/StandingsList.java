package com.sap.sailing.gwt.home.desktop.partials.standings;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.LabelType;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.GetMiniLeaderboardDTO;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.MiniLeaderboardItemDTO;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.refresh.RefreshableWidget;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;

public class StandingsList extends Widget implements RefreshableWidget<GetMiniLeaderboardDTO> {
    interface MyUiBinder extends UiBinder<Element, StandingsList> {
    }
    
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private static final StringMessages i18n = StringMessages.INSTANCE;
    
    @UiField AnchorElement headerLinkUi;
    @UiField SpanElement headerTitleUi;
    @UiField SpanElement headerLabelUi;
    @UiField DivElement headerArrowUi;
    @UiField DivElement itemContainerUi;
    @UiField DivElement noResultsUi;
    @UiField DivElement scoreInformationUi;
    
    private final boolean finished;
    private final FlagImageResolver flagImageResolver;

    public StandingsList(boolean finished, PlaceNavigation<?> headerNavigation, FlagImageResolver flagImageResolver) {
        this.finished = finished;
        this.flagImageResolver = flagImageResolver;
        StandingsResources.INSTANCE.css().ensureInjected();
        setElement(uiBinder.createAndBindUi(this));
        setVisible(false);
        if(headerNavigation == null) {
            headerArrowUi.removeFromParent();
        } else {
            headerNavigation.configureAnchorElement(headerLinkUi);
        }
    }

    @Override
    public void setData(GetMiniLeaderboardDTO data) {
        String headerText = finished ? i18n.results() : i18n.latestRegattaStandings();
        int itemCount = data.getItems().size();
        if (itemCount > 0 && data.getTotalCompetitorCount() > itemCount) {
            headerText += " (" + i18n.topN(itemCount) + ")";
        }
        headerTitleUi.setInnerText(headerText);
        itemContainerUi.removeAllChildren();
        scoreInformationUi.removeAllChildren();
        getElement().getStyle().clearDisplay();
        updateScoreInformation(data);
        
        LabelTypeUtil.renderLabelTypeOrHide(headerLabelUi, data.isLive() ? LabelType.LIVE : LabelType.NONE);
        if(data.getItems().isEmpty()) {
            noResultsUi.getStyle().clearDisplay();
            return;
        }
        noResultsUi.getStyle().setDisplay(Display.NONE);
        
        boolean showRaceCounts = data.hasDifferentRaceCounts();
        for (MiniLeaderboardItemDTO item : data.getItems()) {
            itemContainerUi.appendChild(new StandingsListCompetitor(item, showRaceCounts, flagImageResolver).getElement());
        }
    }

    private void updateScoreInformation(GetMiniLeaderboardDTO data) {
        scoreInformationUi.removeAllChildren();
        if(data.getItems().isEmpty() || (data.getScoreCorrectionText() == null && data.getLastScoreUpdate() == null)) {
            scoreInformationUi.getStyle().setDisplay(Display.NONE);
            return;
        }
        scoreInformationUi.getStyle().clearDisplay();
        
        if (data.getScoreCorrectionText() != null) {
            DivElement scoreCorrectionText = Document.get().createDivElement();
            scoreCorrectionText.setInnerText(data.getScoreCorrectionText());
            scoreInformationUi.appendChild(scoreCorrectionText);
        }
        if (data.getLastScoreUpdate() != null) {
            DivElement lastUpdateElement = Document.get().createDivElement();
            String lastUpdate = DateAndTimeFormatterUtil.formatLongDateAndTimeGMT(data.getLastScoreUpdate());
            lastUpdateElement.setInnerText(StringMessages.INSTANCE.lastScoreUpdate() + ": " + lastUpdate);
            scoreInformationUi.appendChild(lastUpdateElement);
        }
    }
}
