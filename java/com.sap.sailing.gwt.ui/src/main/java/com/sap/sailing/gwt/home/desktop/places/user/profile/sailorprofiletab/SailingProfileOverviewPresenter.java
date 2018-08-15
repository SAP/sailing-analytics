package com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab;

import java.util.UUID;

import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.EditSailorProfileView;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sse.gwt.client.mvp.ClientFactory;
import com.sap.sse.security.ui.authentication.app.NeedsAuthenticationContext;
import com.sap.sse.security.ui.authentication.decorator.NotLoggedInPresenter;

public interface SailingProfileOverviewPresenter extends NotLoggedInPresenter, NeedsAuthenticationContext {
    EditSailorProfileView.Presenter getSharedSailorProfilePresenter();

    ClientFactory getClientFactory();

    FlagImageResolver getFlagImageResolver();

    void removeSailorProfile(UUID uuid);
}
