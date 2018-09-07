package com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfilesDTO;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailingProfileOverviewPresenter;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailorProfileOverviewImplPresenter;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailorProfileView;
import com.sap.sailing.gwt.home.mobile.app.MobileApplicationClientFactory;
import com.sap.sailing.gwt.home.mobile.places.user.profile.AbstractUserProfileActivity;
import com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles.details.SailorProfilesDetailsViewImpl;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.ClientFactoryWithDispatchAndError;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.EditSailorProfilePresenter;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.EditSailorProfileView;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.HasLoginFormAndFactory;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.SailorProfilePlace;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sse.security.ui.authentication.AuthenticationContextEvent;

public class SailorProfilesActivity extends AbstractUserProfileActivity
        implements SailorProfilesOverviewView.Presenter, HasLoginFormAndFactory {

    private final MobileApplicationClientFactory clientFactory;
    private final SailorProfileView currentView;
    private final SailingProfileOverviewPresenter sailorProfileOverviewPresenter;

    // redundant with sailorProfileOverviewPresenter.getSharedPresenter() --> TODO: remove
    private final EditSailorProfilePresenter editSailorProfilePresenter;

    private final SailorProfilePlace place;

    public SailorProfilesActivity(SailorProfilePlace place, MobileApplicationClientFactory clientFactory) {
        super(clientFactory);
        this.place = place;
        this.clientFactory = clientFactory;
        editSailorProfilePresenter = new EditSailorProfilePresenter(clientFactory);
        if (place.getSailorProfileUuid() == null) {
            if (place.isCreateNew()) {
                this.currentView = new SailorProfilesDetailsViewImpl(this);
            } else {
                this.currentView = new SailorProfilesOverviewViewImpl(this);

            }
        } else {
            this.currentView = new SailorProfilesDetailsViewImpl(this);
        }
        sailorProfileOverviewPresenter = new SailorProfileOverviewImplPresenter(this.currentView, this,
                FlagImageResolverImpl.get(), editSailorProfilePresenter);

    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        panel.setWidget(currentView);
        if (currentView instanceof SailorProfilesViewWithAuthenticationContext) {

            SailorProfilesViewWithAuthenticationContext cView = (SailorProfilesViewWithAuthenticationContext) currentView;
            cView.setAuthenticationContext(clientFactory.getAuthenticationManager().getAuthenticationContext());
            eventBus.addHandler(AuthenticationContextEvent.TYPE, new AuthenticationContextEvent.Handler() {
                @Override
                public void onUserChangeEvent(AuthenticationContextEvent event) {
                    cView.setAuthenticationContext(event.getCtx());
                }
            });

            if (cView instanceof SailorProfilesOverviewView) {
                SailorProfilesOverviewView overview = (SailorProfilesOverviewView) cView;
                if (clientFactory.getAuthenticationManager().getAuthenticationContext().isLoggedIn()) {
                    sailorProfileOverviewPresenter.getSharedSailorProfilePresenter().getDataProvider()
                            .loadSailorProfiles(new AsyncCallback<SailorProfilesDTO>() {

                                @Override
                                public void onSuccess(SailorProfilesDTO result) {
                                    overview.setProfileList(result.getEntries());
                                }

                                @Override
                                public void onFailure(Throwable caught) {
                                    GWT.log(caught.getMessage(), caught);
                                }
                            });
                }
            } else if (cView instanceof EditSailorProfileView) {
                EditSailorProfileView detailsView = (EditSailorProfileView) cView;
                editSailorProfilePresenter.getDataProvider().setView(detailsView);
                editSailorProfilePresenter.getDataProvider().loadSailorProfile(place.getSailorProfileUuid());
            }
        }
    }

    @Override
    public EditSailorProfilePresenter getSailorProfilePresenter() {
        return editSailorProfilePresenter;
    }

    @Override
    public SailingProfileOverviewPresenter getSailorProfileOverviewPresenter() {
        return sailorProfileOverviewPresenter;
    }

    @Override
    public ClientFactoryWithDispatchAndError getClientFactory() {
        return editSailorProfilePresenter.getClientFactory();
    }

}
