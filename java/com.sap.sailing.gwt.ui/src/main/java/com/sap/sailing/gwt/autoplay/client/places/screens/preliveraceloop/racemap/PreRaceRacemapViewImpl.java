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
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap.statistic.PreRaceStatisticsBox;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap.statistic.PreRaceStatisticsBoxResources;
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
    PreRaceStatisticsBox statistics = new PreRaceStatisticsBox();
    private NumberFormat compactFormat = NumberFormat.getFormat("#.0");

    interface PreRaceRacemapViewImplUiBinder extends UiBinder<Widget, PreRaceRacemapViewImpl> {
    }

    public PreRaceRacemapViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void showErrorNoLive(PreRaceRacemapPresenterImpl slide7PresenterImpl, AcceptsOneWidget panel,
            Throwable error) {
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
        statistics.addItem(PreRaceStatisticsBoxResources.INSTANCE.competitors(), StringMessages.INSTANCE.competitors(),
                result.getCompetitors());
        statistics.addItem(PreRaceStatisticsBoxResources.INSTANCE.strongestWind(), StringMessages.INSTANCE.wind(), windSpeed);
        statistics.addItem(PreRaceStatisticsBoxResources.INSTANCE.wind(), StringMessages.INSTANCE.averageDirection(), windDegree);
        statistics.addItem(PreRaceStatisticsBoxResources.INSTANCE.legs(), StringMessages.INSTANCE.legs(), result.getLegs());
        try {
            statistics.addItem(PreRaceStatisticsBoxResources.INSTANCE.sumMiles(), StringMessages.INSTANCE.estimatedDistance(),
                    compactFormat.format(result.getDistance().getNauticalMiles()) + " " + StringMessages.INSTANCE.nauticalMiles());
        } catch (Exception e) {
        }
        try {
            statistics.addItem(PreRaceStatisticsBoxResources.INSTANCE.time(), StringMessages.INSTANCE.estimatedTime(),
                    compactFormat.format(result.getDuration().asMinutes()) + " " + StringMessages.INSTANCE.minutes());
        } catch (Exception e) {
        }
        statistics.addQRItem(PreRaceStatisticsBoxResources.INSTANCE.raceviewer(), StringMessages.INSTANCE.raceViewer(), url);
    }

    @Override
    public void nextRace(RegattaAndRaceIdentifier race) {
        bottomInfoPanel
                .setText(StringMessages.INSTANCE.next() + " " + race.getRegattaName() + " " + race.getRaceName());
    }
}
