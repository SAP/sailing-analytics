package com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.competitors;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreLiveRaceLeaderboardWithImageViewImpl.ImageProvider;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class PreRaceCompetitorsViewImpl extends ResizeComposite implements PreRaceCompetitorsView {
    private static PreRaceCompetitorsViewImplUiBinder uiBinder = GWT.create(PreRaceCompetitorsViewImplUiBinder.class);

    @UiField
    LayoutPanel competitorSlider;

    private ImageProvider provider;

    interface PreRaceCompetitorsViewImplUiBinder extends UiBinder<Widget, PreRaceCompetitorsViewImpl> {
    }

    public PreRaceCompetitorsViewImpl(ImageProvider provider) {
        initWidget(uiBinder.createAndBindUi(this));
        this.provider = provider;
    }

    @Override
    public void startingWith(PreRaceCompetitorsPresenter p, AcceptsOneWidget panel) {
        panel.setWidget(this);
    }

    @Override
    public void move() {
        Widget first = competitorSlider.getWidget(0);
        competitorSlider.remove(first);
        competitorSlider.add(first);
        competitorSlider.animate(PreRaceCompetitorsPresenterImpl.DELAY_NEXT);
    }

    @Override
    public void setCompetitors(List<CompetitorWithBoatDTO> competitors, TakedownNoticeService takedownNoticeService) {
        for (CompetitorWithBoatDTO competitor : competitors) {
            Widget w = createCompetitorElement(competitor, takedownNoticeService);
            competitorSlider.add(w);
        }
        competitorSlider.forceLayout();
    }

    private Widget createCompetitorElement(CompetitorWithBoatDTO competitor, TakedownNoticeService takedownNoticeService) {
        FlowPanel competitorPanel = new FlowPanel();
        competitorPanel.add(new CompetitorViewImpl(provider, competitor, takedownNoticeService));
        return new Label(competitor.getName());
    }

}
