package com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sse.gwt.client.ImageOnFlowPanelHelper;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.panels.ResizableFlowPanel;
import com.sap.sse.gwt.common.CommonSharedResources;

public class PreLiveRaceLeaderboardWithImageViewImpl extends ResizeComposite implements PreLeaderboardWithImageView {
    private static IdleLeaderBoardWithFlagsViewImplUiBinder uiBinder = GWT
            .create(IdleLeaderBoardWithFlagsViewImplUiBinder.class);

    interface IdleLeaderBoardWithFlagsViewImplUiBinder extends UiBinder<Widget, PreLiveRaceLeaderboardWithImageViewImpl> {
    }

    @UiField
    ResizableFlowPanel leaderBoardHolder;

    @UiField
    ResizableFlowPanel infoHolder;

    @UiField
    Label bottomInfoPanel;

    @UiField
    FlowPanel image;
    @UiField
    Label subline1;
    @UiField
    Label subline2;
    @UiField
    Label subline3;
    @UiField(provided = true)
    MediaMenuIcon takedownButton;

    private ImageProvider provider;

    public interface ImageProvider {
        String getImageUrl(CompetitorDTO marked);
        
        /**
         * A key for String messages, used to explain to an administrator in which context an image for which
         * removal from the site is to be requested has occurred. The message is expected to take one parameter
         * which will be filled by the {@link CompetitorDTO#getName()} result. 
         */
        String getTakedownNoticeContextKey();
    }

    public PreLiveRaceLeaderboardWithImageViewImpl(ImageProvider provider, TakedownNoticeService takedownNoticeService) {
        CommonSharedResources.INSTANCE.mainCss().ensureInjected();
        takedownButton = new MediaMenuIcon(takedownNoticeService, provider.getTakedownNoticeContextKey());
        initWidget(uiBinder.createAndBindUi(this));
        this.provider = provider;
    }

    @Override
    public void startingWith(Slide1Presenter p, AcceptsOneWidget panel) {
        panel.setWidget(this);
    }

    @Override
    public void setLeaderBoard(SingleRaceLeaderboardPanel leaderboardPanel) {
        leaderBoardHolder.add(leaderboardPanel);
    }

    @Override
    public void onCompetitorSelect(CompetitorDTO selected) {
        final String imageUrl = provider.getImageUrl(selected);
        ImageOnFlowPanelHelper.setImage(image, imageUrl);
        takedownButton.setData(selected.getName(), imageUrl);
        final String boatName;
        if (selected.hasBoat() && (boatName = ((CompetitorWithBoatDTO) selected).getBoat().getName()) != null) {
            subline1.setText(boatName);
        } else {
            subline1.setText("");
        }
        if (selected.getName() != null) {
            subline2.setText(selected.getName());
        } else {
            subline2.setText("");
        }

        subline3.setText("");
    }
    
    @Override
    public void scrollLeaderBoardToTop() {
        leaderBoardHolder.getElement().setScrollTop(0);
    }

    @Override
    public void onStop() {
    }

    @Override
    public void nextRace(RegattaAndRaceIdentifier race) {
        bottomInfoPanel
                .setText(StringMessages.INSTANCE.next() + " " + race.getRegattaName() + " " + race.getRaceName());
    }

}
