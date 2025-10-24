package com.sap.sailing.gwt.home.mobile.partials.minileaderboard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.FontStyle;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.LabelType;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.GetMiniLeaderboardDTO;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.MiniLeaderboardItemDTO;
import com.sap.sailing.gwt.home.mobile.partials.section.MobileSection;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent;
import com.sap.sailing.gwt.home.mobile.partials.toggleButton.BigButton;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.refresh.RefreshableWidget;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;

public class MinileaderboardBox extends Composite implements RefreshableWidget<GetMiniLeaderboardDTO> {
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, MinileaderboardBox> {
    }
    
    private final StringMessages I18N = StringMessages.INSTANCE;

    @UiField MobileSection itemContainerUi;
    @UiField SectionHeaderContent headerUi;
    @UiField BigButton showLeaderboardButtonUi;
    
    private PlaceNavigation<?> placeNavigation = null;

    private boolean isOverall;

    private final FlagImageResolver flagImageResolver;
    
    public MinileaderboardBox(boolean isOverall, FlagImageResolver flagImageResolver) {
        this.isOverall = isOverall;
        this.flagImageResolver = flagImageResolver;
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    @UiHandler("showLeaderboardButtonUi")
    void onShowLeaderboardClick(ClickEvent event) {
        if(placeNavigation != null) {
            placeNavigation.goToPlace();
        }
    }
    
    public void setAction(String infoText, final PlaceNavigation<?> placeNavigation) {
        headerUi.setInfoText(infoText);
        headerUi.setClickAction(placeNavigation);
        this.placeNavigation = placeNavigation;
    }
    
    @Override
    public void setData(final GetMiniLeaderboardDTO data) {
        String headerText = isOverall ? I18N.overallStandings() : I18N.results();
        int itemCount = data.getItems().size();
        boolean showLeaderboardButton = false;
        if (itemCount > 0 && data.getTotalCompetitorCount() > itemCount) {
            headerText += " (" + StringMessages.INSTANCE.topN(itemCount) + ")";
            showLeaderboardButton = true;
        }
        headerUi.setSectionTitle(headerText);
        itemContainerUi.clearContent();
        if (data.getItems().isEmpty()) {
            itemContainerUi.addContent(getNoResultsInfoWidget());
            return;
        }
        headerUi.setLabelType(data.isLive() ? LabelType.LIVE : LabelType.NONE);
        if (data.getScoreCorrectionText() != null || data.getLastScoreUpdate() != null) {
            itemContainerUi.addContent(getScoreInformation(data));
        }
        boolean showRaceCounts = data.hasDifferentRaceCounts();
        for (MiniLeaderboardItemDTO item : data.getItems()) {
            itemContainerUi.addContent(new MinileaderboardBoxItem(item, showRaceCounts, flagImageResolver));
        }
        if (showRaceCounts) {
            itemContainerUi.addContent(new MinileaderboardBoxItemLegend());
        }
        showLeaderboardButtonUi.setVisible(showLeaderboardButton);
    }
    
    private Widget getNoResultsInfoWidget() {
        Label label = new Label(StringMessages.INSTANCE.noResults());
        label.getElement().getStyle().setPadding(1, Unit.EM);
        label.getElement().getStyle().setFontWeight(FontWeight.BOLD);
        label.getElement().getStyle().setFontStyle(FontStyle.ITALIC);
        return label;
    }
    
    private Widget getScoreInformation(GetMiniLeaderboardDTO data) {
        FlowPanel scoreInformation = new FlowPanel();
        if (data.getScoreCorrectionText() != null) {
            scoreInformation.add(new Label(data.getScoreCorrectionText()));
        }
        if (data.getLastScoreUpdate() != null) {
            String lastUpdate = DateAndTimeFormatterUtil.formatLongDateAndTimeGMT(data.getLastScoreUpdate());
            scoreInformation.add(new Label(StringMessages.INSTANCE.lastScoreUpdate() + ": " + lastUpdate));
        }
        scoreInformation.getElement().getStyle().setBackgroundColor("#f2f2f2");
        scoreInformation.getElement().getStyle().setProperty("borderTop", "1px solid #ccc");
        scoreInformation.getElement().getStyle().setFontSize(0.866666666666667, Unit.EM);
        scoreInformation.getElement().getStyle().setPadding(1, Unit.EM);
        if (data.isLive()) {
            scoreInformation.getElement().getStyle().setColor("#ff0000");
        }
        return scoreInformation;
    }
}
