package com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayPresenterConfigured;
import com.sap.sailing.gwt.home.communication.event.sixtyinch.GetSixtyInchStatisticAction;
import com.sap.sailing.gwt.home.communication.event.sixtyinch.GetSixtyInchStatisticDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;

public class PreRaceRacemapPresenterImpl extends AutoPlayPresenterConfigured<PreRaceRacemapPlace>
        implements PreRaceRacemapView.Slide7Presenter {
    private PreRaceRacemapView view;
    private Timer updateStatistics;
    private GetSixtyInchStatisticDTO lastStatisticResult;
    private Timer reloadStatistics;

    public PreRaceRacemapPresenterImpl(PreRaceRacemapPlace place, AutoPlayClientFactory clientFactory,
            PreRaceRacemapView slide7ViewImpl) {
        super(place, clientFactory);
        this.view = slide7ViewImpl;
    }

    @Override
    public void startConfigured(AcceptsOneWidget panel) {
        if (getPlace().getError() != null) {
            view.showErrorNoLive(this, panel, getPlace().getError());
            return;
        }
        view.nextRace(getSlideCtx().getLifeRace());
        reloadStatistics();
        reloadStatistics = new Timer() {
            @Override
            public void run() {
                reloadStatistics();
            }
        };
        updateStatistics = new Timer() {
            @Override
            public void run() {
                if (lastStatisticResult != null && getPlace().getRaceMap().getLastCombinedWindTrackInfoDTO() != null) {
                    String windSpeed = "";
                    String windDegree = "";
                    for (WindSource windSource : getPlace().getRaceMap()
                            .getLastCombinedWindTrackInfoDTO().windTrackInfoByWindSource.keySet()) {
                        WindTrackInfoDTO windTrackInfoDTO = getPlace().getRaceMap()
                                .getLastCombinedWindTrackInfoDTO().windTrackInfoByWindSource.get(windSource);
                        switch (windSource.getType()) {
                        case COMBINED:
                            if (!windTrackInfoDTO.windFixes.isEmpty()) {
                                WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
                                double speedInKnots = windDTO.dampenedTrueWindSpeedInKnots;
                                double windFromDeg = windDTO.dampenedTrueWindBearingDeg;
                                NumberFormat numberFormat = NumberFormat.getFormat("0.0");
                                windDegree = Math.round(windFromDeg) + " " + StringMessages.INSTANCE.degreesShort();
                                windSpeed = numberFormat.format(speedInKnots) + " "
                                        + StringMessages.INSTANCE.knotsUnit();
                            }
                            break;
                        default:
                        }
                    }
                    view.updateStatistic(lastStatisticResult, getSlideCtx().getEvent().getOfficialWebsiteURL(), windSpeed,
                            windDegree);
                }
            }
        };

        updateStatistics.scheduleRepeating(1000);
        // load only slowly, to reduce jitter in display, but still allow to respect coarse course changes
        reloadStatistics.scheduleRepeating(30000);
        view.startingWith(this, panel, getPlace().getRaceMap());
    };

    @Override
    public void onStop() {
        super.onStop();
        updateStatistics.cancel();
        reloadStatistics.cancel();
    }

    private void reloadStatistics() {
        RegattaAndRaceIdentifier upcomingRace = getSlideCtx().getLifeRace();
        getClientFactory().getDispatch().execute(
                new GetSixtyInchStatisticAction(upcomingRace.getRaceName(), upcomingRace.getRegattaName()),
                new AsyncCallback<GetSixtyInchStatisticDTO>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log("error getting data! " + caught.getMessage());
                        caught.printStackTrace();
                    }

                    @Override
                    public void onSuccess(GetSixtyInchStatisticDTO result) {
                        lastStatisticResult = result;
                    }
                });

    }
}
