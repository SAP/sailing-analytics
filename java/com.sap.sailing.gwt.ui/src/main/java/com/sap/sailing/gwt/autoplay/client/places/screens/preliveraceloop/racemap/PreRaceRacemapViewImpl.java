package com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap.statistik.PreRaceStatisticsBox;
import com.sap.sailing.gwt.home.communication.event.sixtyinch.GetSixtyInchStatisticDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sse.gwt.client.panels.ResizableFlowPanel;

public class PreRaceRacemapViewImpl extends ResizeComposite implements PreRaceRacemapView {
    private static PreRaceRacemapViewImplUiBinder uiBinder = GWT.create(PreRaceRacemapViewImplUiBinder.class);

    @UiField
    ResizableFlowPanel racemap;

    @UiField
    ResizableFlowPanel raceInfoHolder;

    @UiField
    Label bottomInfoPanel;
    PreRaceStatisticsBox statistics = new PreRaceStatisticsBox(false);

    private NumberFormat compactFormat = NumberFormat.getFormat("#.0");

    interface PreRaceRacemapViewImplUiBinder extends UiBinder<Widget, PreRaceRacemapViewImpl> {
    }

    public PreRaceRacemapViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
    }


    @Override
    public void showErrorNoLive(PreRaceRacemapPresenterImpl slide7PresenterImpl, AcceptsOneWidget panel, Throwable error) {
        panel.setWidget(new Label("Could not load RaceMap: " + error.getMessage()));
    }

    @Override
    public void startingWith(Slide7Presenter p, AcceptsOneWidget panel, RaceMap raceMap) {
        panel.setWidget(this);
        racemap.add(raceMap);

        raceInfoHolder.add(statistics);
    }


    @Override
    public void updateStatistic(GetSixtyInchStatisticDTO result, String url, String windSpeed, String windDegree) {
        // google maps api workaround
        racemap.onResize();

        statistics.clear();
        statistics.addItem(PreRaceStatisticsBox.ICON_COMPETITORS, StringMessages.INSTANCE.competitors(),
                result.getCompetitors());
        statistics.addItem(PreRaceStatisticsBox.ICON_WIND_FIX, StringMessages.INSTANCE.wind(), windSpeed);
        // statistics.addItem(PreRaceStatisticsBox.ICON_WIND, StringMessages.INSTANCE.averageDirection(), windDegree);

        statistics.addItem(PreRaceStatisticsBox.ICON_LEGS, StringMessages.INSTANCE.legs(), result.getLegs());
        statistics.addItem(PreRaceStatisticsBox.ICON_SUM_MILES, StringMessages.INSTANCE.estimatedDistance(),
                compactFormat.format(result.getDistance().getSeaMiles()) + " " + StringMessages.INSTANCE.seaMiles());
        statistics.addItem(PreRaceStatisticsBox.ICON_TIME,
                StringMessages.INSTANCE.estimatedTime(),
                compactFormat.format(result.getDuration().asMinutes()) + " " + StringMessages.INSTANCE.minutes());
        
        statistics.addItem(PreRaceStatisticsBox.ICON_RACEVIEWER, StringMessages.INSTANCE.url(), url);

    }

    @Override
    public void nextRace(RegattaAndRaceIdentifier race) {
        bottomInfoPanel
                .setText(StringMessages.INSTANCE.next() + " " + race.getRegattaName() + " " + race.getRaceName());
    }

}
