package com.sap.sailing.gwt.autoplay.client.nodes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.autoplay.client.nodes.base.FiresPlaceNode;
import com.sap.sailing.gwt.autoplay.client.places.screens.afterliveraceloop.flags.RaceEndWithCompetitorFlagsPlace;
import com.sap.sailing.gwt.home.communication.event.sixtyinch.GetSixtyInchStatisticAction;
import com.sap.sailing.gwt.home.communication.event.sixtyinch.GetSixtyInchStatisticDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class RaceEndWithCompetitorsFlagsNode extends FiresPlaceNode {
    private final AutoPlayClientFactory cf;

    public RaceEndWithCompetitorsFlagsNode(AutoPlayClientFactory cf) {
        this.cf = cf;
    }

    public void onStart() {
        RaceEndWithCompetitorFlagsPlace place = new RaceEndWithCompetitorFlagsPlace();

        RegattaAndRaceIdentifier lastRace = cf.getAutoPlayCtx().getLastRace();
        place.setLifeRace(lastRace);
        setPlaceToGo(place);

        cf.getDispatch().execute(new GetSixtyInchStatisticAction(lastRace.getRaceName(), lastRace.getRegattaName()),
                new AsyncCallback<GetSixtyInchStatisticDTO>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log("error getting data! " + caught.getMessage());
                        caught.printStackTrace();
                    }

                    @Override
                    public void onSuccess(GetSixtyInchStatisticDTO result) {
                        place.setStatistic(result);
                        getBus().fireEvent(new AutoPlayHeaderEvent(lastRace.getRegattaName(),
                                StringMessages.INSTANCE.results() + " " + lastRace.getRaceName()));
                        firePlaceChangeAndStartTimer();
                    }
                });
    };
}
