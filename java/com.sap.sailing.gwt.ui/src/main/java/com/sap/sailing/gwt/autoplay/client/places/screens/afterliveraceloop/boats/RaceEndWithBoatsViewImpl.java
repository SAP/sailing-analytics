package com.sap.sailing.gwt.autoplay.client.places.screens.afterliveraceloop.boats;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreLiveRaceLeaderboardWithImageViewImpl.ImageProvider;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.gwt.client.ImageOnFlowPanelHelper;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.panels.ResizableFlowPanel;

public class RaceEndWithBoatsViewImpl extends ResizeComposite implements RaceEndWithBoatsView {
    private static RaceEndWithBoatsViewImplUiBinder uiBinder = GWT.create(RaceEndWithBoatsViewImplUiBinder.class);

    interface RaceEndWithBoatsViewImplUiBinder extends UiBinder<Widget, RaceEndWithBoatsViewImpl> {
    }

    @UiField
    ResizableFlowPanel leaderBoardHolder;

    @UiField
    ResizableFlowPanel infoHolder;

    @UiField
    FlowPanel image1;
    @UiField
    FlowPanel image2;
    @UiField
    FlowPanel image3;
    @UiField
    Label subline1;
    @UiField
    Label subline2;
    @UiField
    Label subline3;
    @UiField
    Label statisticValue1;
    @UiField
    Label statisticProperty1;
    @UiField
    Label statisticValue2;
    @UiField
    Label statisticProperty2;
    @UiField
    Label statisticValue3;
    @UiField
    Label statisticProperty3;
    @UiField(provided = true)
    MediaMenuIcon takedownButtonImage1;
    @UiField(provided = true)
    MediaMenuIcon takedownButtonImage2;
    @UiField(provided = true)
    MediaMenuIcon takedownButtonImage3;
    
    private NumberFormat compactFormat = NumberFormat.getFormat("#.0");

    private ImageProvider provider;

    public RaceEndWithBoatsViewImpl(ImageProvider provider, TakedownNoticeService takedownNoticeService) {
        takedownButtonImage1 = new MediaMenuIcon(takedownNoticeService, provider.getTakedownNoticeContextKey());
        takedownButtonImage2 = new MediaMenuIcon(takedownNoticeService, provider.getTakedownNoticeContextKey());
        takedownButtonImage3 = new MediaMenuIcon(takedownNoticeService, provider.getTakedownNoticeContextKey());
        initWidget(uiBinder.createAndBindUi(this));
        this.provider = provider;
    }

    @Override
    public void startingWith(NextRaceWithBoatsPresenter p, AcceptsOneWidget panel) {
        panel.setWidget(this);
    }

    @Override
    public void setLeaderBoard(SingleRaceLeaderboardPanel leaderboardPanel) {
        leaderBoardHolder.add(leaderboardPanel);
    }

    private void setCompetitor(int position, Label subline, FlowPanel image, MediaMenuIcon takedownButton, CompetitorDTO c) {
        subline.setText(""+position+". " + c.getName());
        final String imageUrl = provider.getImageUrl(c);
        ImageOnFlowPanelHelper.setImage(image, provider.getImageUrl(c));
        takedownButton.setData(c.getName(), imageUrl);
    }

    @Override
    public void setFirst(CompetitorDTO c) {
        setCompetitor(1, subline1, image1, takedownButtonImage1, c);
    }

    @Override
    public void setSecond(CompetitorDTO c) {
        setCompetitor(2, subline2, image2, takedownButtonImage2, c);
    }

    @Override
    public void setThird(CompetitorDTO c) {
        setCompetitor(3, subline3, image3, takedownButtonImage3, c);
    }

    @Override
    public void setStatistic(int competitorCount, Distance distance, Duration duration) {
        statisticProperty1.setText(StringMessages.INSTANCE.competitors());
        statisticValue1.setText(String.valueOf(competitorCount));
        statisticProperty2.setText(StringMessages.INSTANCE.distance());
        if(distance == null){
            statisticValue2
            .setText(StringMessages.INSTANCE.noDataFound());
        }else{
            statisticValue2
            .setText(compactFormat.format(distance.getNauticalMiles()) + " " + StringMessages.INSTANCE.nauticalMiles());
        }
        statisticProperty3.setText(StringMessages.INSTANCE.durationPlain());
        statisticValue3.setText(compactFormat.format(duration.asMinutes()) + " " + StringMessages.INSTANCE.minutes());
    }

}
