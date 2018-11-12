package com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile;

import java.util.Collection;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.app.ClientFactoryWithDispatch;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedCompetitorMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.dataprovider.SailorProfileDataProvider;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.dataprovider.SailorProfileDataProviderImpl;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.dataprovider.SailorProfilesCompetitorSelectionPresenter;
import com.sap.sailing.gwt.ui.client.refresh.ErrorAndBusyClientFactory;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.client.mvp.ClientFactory;

/**
 * Reusable implementation of {@link EditSailorProfileDetailsView.Presenter} which handles the sailor profiles. It only
 * require an appropriate client factory which implements {@link ClientFactoryWithDispatch},
 * {@link ErrorAndBusyClientFactory} and {@link ClientFactory}.
 * 
 * @param <C>
 *            the provided client factory type
 */
public class EditSailorProfilePresenter implements EditSailorProfileDetailsView.Presenter {

    private final ClientFactoryWithDispatchAndErrorAndUserService clientFactory;
    private ServerInfoDTO cachedServerInfo;

    private final SailorProfileDataProvider sailorProfileDataProvider;
    private final SailorProfilesCompetitorSelectionPresenter sailorProfilesCompetitorSelectionPresenter;

    public EditSailorProfilePresenter(ClientFactoryWithDispatchAndErrorAndUserService clientFactory) {
        this.clientFactory = clientFactory;
        this.sailorProfileDataProvider = new SailorProfileDataProviderImpl(clientFactory);
        this.sailorProfilesCompetitorSelectionPresenter = new SailorProfilesCompetitorSelectionPresenter(
                new SuggestedMultiSelectionCompetitorDataProviderImpl(clientFactory), this.sailorProfileDataProvider);
        this.sailorProfileDataProvider.setCompetitorSelectionPresenter(this.sailorProfilesCompetitorSelectionPresenter);
    }

    private class SuggestedMultiSelectionCompetitorDataProviderImpl extends
            AbstractSuggestedCompetitorMultiSelectionPresenter<SuggestedMultiSelectionPresenter.Display<SimpleCompetitorWithIdDTO>> {

        private SuggestedMultiSelectionCompetitorDataProviderImpl(ClientFactoryWithDispatch clientFactory) {
            super(clientFactory);
        }

        @Override
        protected void persist(Collection<SimpleCompetitorWithIdDTO> selectedItem) {
            /** persistence is done in {@link SailorProfileDataProvider} */
        }
    }

    @Override
    public SailorProfileDataProvider getDataProvider() {
        return sailorProfileDataProvider;
    }

    @Override
    public PlaceController getPlaceController() {
        return clientFactory.getPlaceController();
    }

    @Override
    public void getServerInfo(AsyncCallback<ServerInfoDTO> callback) {
        if (cachedServerInfo != null) {
            callback.onSuccess(cachedServerInfo);
        } else {
            clientFactory.getSailingService().getServerInfo(new AsyncCallback<ServerInfoDTO>() {

                @Override
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }

                @Override
                public void onSuccess(ServerInfoDTO result) {
                    cachedServerInfo = result;
                    callback.onSuccess(result);
                }
            });
        }
    }

    public ClientFactoryWithDispatchAndErrorAndUserService getClientFactory() {
        return clientFactory;
    }

    @Override
    public SailorProfilesCompetitorSelectionPresenter getCompetitorPresenter() {
        return sailorProfilesCompetitorSelectionPresenter;
    }
}
