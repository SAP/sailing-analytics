package com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.racemapwithleaderboard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sse.common.Distance;
import com.sap.sse.gwt.client.panels.ResizableFlowPanel;

public class LiveRaceWithRacemapAndLeaderBoardViewImpl extends ResizeComposite implements LiveRaceWithRacemapAndLeaderBoardView {
    private static LifeRaceWithRacemapViewImplUiBinder uiBinder = GWT.create(LifeRaceWithRacemapViewImplUiBinder.class);

    @UiField
    ResizableFlowPanel racemap;

    @UiField
    ResizableFlowPanel leaderBoardHolder;
    
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
    
    private NumberFormat compactFormat = NumberFormat.getFormat("0.0");

    private RaceMap rawRaceMap;

    private SingleRaceLeaderboardPanel leaderboardPanel;

    interface LifeRaceWithRacemapViewImplUiBinder extends UiBinder<Widget, LiveRaceWithRacemapAndLeaderBoardViewImpl> {
    }

    public LiveRaceWithRacemapAndLeaderBoardViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
    }


    @Override
    public void showErrorNoLive(LiveRaceWithRacemapAndLeaderBoardPresenterImpl slide7PresenterImpl, AcceptsOneWidget panel, Throwable error) {
        panel.setWidget(new Label("Could not load RaceMap: " + error.getMessage()));
    }

    @Override
    public void onCompetitorSelect(CompetitorDTO marked) {
        rawRaceMap.addedToSelection(marked);
    }

    @Override
    public void scrollLeaderBoardToTop() {
        leaderboardPanel.getHeaderWidget().getElement().setScrollTop(0);
    }

    @Override
    public void onStop() {
    }

    @Override
    public void startingWith(Slide7Presenter p, AcceptsOneWidget panel, RaceMap raceMap,
            SingleRaceLeaderboardPanel leaderboardPanel) {
        panel.setWidget(this);
        rawRaceMap = raceMap;
        racemap.add(raceMap);
        this.leaderboardPanel = leaderboardPanel;
        leaderBoardHolder.add(leaderboardPanel);
    }

    @Override
    public void setStatistic(String windinfo, Distance distance, long duration) {
        statisticProperty1.setText(StringMessages.INSTANCE.windSpeed());
        if (windinfo == null || windinfo.isEmpty()) {
            statisticValue1.setText(StringMessages.INSTANCE.noDataFound());
        } else {
            statisticValue1.setText(windinfo);
        }

        statisticProperty2.setText(StringMessages.INSTANCE.distance());
        if (distance == null) {
            statisticValue2.setText(StringMessages.INSTANCE.noDataFound());
        } else {
            statisticValue2
                    .setText(compactFormat.format(distance.getNauticalMiles()) + " " + StringMessages.INSTANCE.nauticalMiles());
        }

        statisticProperty3.setText(StringMessages.INSTANCE.durationPlain());
        statisticValue3.setText(compactFormat.format(duration / 1000f / 60f) + " " + StringMessages.INSTANCE.minutes());
    }


    @Override
    public native void ensureMapVisibility() /*-{
        try {
            $wnd.dispatchEvent(new Event("resize"));
        } catch (error) {
        }
    }-*/;

}

