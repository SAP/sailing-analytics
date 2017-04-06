package com.sap.sailing.gwt.autoplay.client.place.sixtyinch.slides.slide6;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.communication.event.sixtyinch.GetSixtyInchStatisticDTO;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;

public interface PreRaceRacemapView {
    void startingWith(Slide7Presenter p, AcceptsOneWidget panel, RaceMap raceMap);

    public interface Slide7Presenter {
    }

    void showErrorNoLive(PreRaceRacemapPresenterImpl slide7PresenterImpl, AcceptsOneWidget panel, Throwable error);

    void updateStatistic(GetSixtyInchStatisticDTO result, String url, String windData, String windDegree);

}
