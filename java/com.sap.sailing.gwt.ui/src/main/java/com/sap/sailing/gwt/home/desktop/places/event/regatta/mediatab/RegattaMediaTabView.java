package com.sap.sailing.gwt.home.desktop.places.event.regatta.mediatab;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.sap.sailing.gwt.home.communication.media.MediaDTO;
import com.sap.sailing.gwt.home.desktop.partials.media.MediaPage;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.EventRegattaView;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.EventRegattaView.Presenter;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.RegattaTabView;
import com.sap.sailing.gwt.home.shared.app.ActivityCallback;
import com.sap.sailing.gwt.home.shared.places.MediaTabView;
import com.sap.sailing.gwt.ui.client.refresh.ErrorAndBusyClientFactory;

/**
 * Created by pgtaboada on 25.11.14.
 */
public class RegattaMediaTabView extends Composite implements RegattaTabView<RegattaMediaPlace>, MediaTabView<RegattaMediaPlace, Presenter> {

    private Presenter currentPresenter;

    @Override
    public void setPresenter(EventRegattaView.Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
    }

    @Override
    public Presenter getPresenter() {
        return currentPresenter;
    }

    @Override
    public Class<RegattaMediaPlace> getPlaceClassForActivation() {
        return RegattaMediaPlace.class;
    }

    @Override
    public void start(RegattaMediaPlace myPlace, final AcceptsOneWidget contentArea) {
        ErrorAndBusyClientFactory errorAndBusyClientFactory = currentPresenter.getErrorAndBusyClientFactory();
        final MediaPage mediaPage = new MediaPage(errorAndBusyClientFactory.createBusyView(),
                currentPresenter.getEventBus(), currentPresenter.getUserService(), currentPresenter.getEventDTO());
        initWidget(mediaPage);
        currentPresenter.ensureMedia(new ActivityCallback<MediaDTO>(errorAndBusyClientFactory, contentArea) {
            @Override
            public void onSuccess(MediaDTO media) {
                contentArea.setWidget(RegattaMediaTabView.this);
                mediaPage.setMedia(media, currentPresenter.getEventDTO());
            }
        });
    }

    @Override
    public void stop() {
    }

    @Override
    public RegattaMediaPlace placeToFire() {
        return new RegattaMediaPlace(currentPresenter.getCtx());
    }
}
