package com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.competitors;

import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayPresenterConfigured;

public class PreRaceCompetitorsPresenterImpl extends AutoPlayPresenterConfigured<AbstractPreRaceCompetitorsPlace>
        implements PreRaceCompetitorsView.PreRaceCompetitorsPresenter {
    static final int DELAY_NEXT = 2000;
    private PreRaceCompetitorsView view;
    private Timer selectionScroll;

    public PreRaceCompetitorsPresenterImpl(AbstractPreRaceCompetitorsPlace place, AutoPlayClientFactory clientFactory,
            PreRaceCompetitorsView slide4ViewImpl) {
        super(place, clientFactory);
        this.view = slide4ViewImpl;
        selectionScroll = new Timer() {
            @Override
            public void run() {
                view.move();
            }
        };
    }

    @Override
    public void startConfigured(AcceptsOneWidget panel) {
        view.startingWith(this, panel);
        List<CompetitorWithBoatDTO> data = null;
        view.setCompetitors(data, getClientFactory().getUserService());
        selectionScroll.scheduleRepeating(DELAY_NEXT);
    }
}
