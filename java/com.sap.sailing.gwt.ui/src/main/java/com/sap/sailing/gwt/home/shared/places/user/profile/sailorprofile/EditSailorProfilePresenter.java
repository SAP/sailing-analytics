package com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile;

import java.util.Collection;

import com.google.gwt.place.shared.PlaceController;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.app.ClientFactoryWithDispatch;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedCompetitorMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.dataprovider.StatefulSailorProfileDataProvider;
import com.sap.sailing.gwt.ui.client.refresh.ErrorAndBusyClientFactory;
import com.sap.sse.gwt.client.mvp.ClientFactory;

/**
 * Reusable implementation of {@link EditSailorProfileView.Presenter} which handles the sailor profiles. It only require
 * an appropriate client factory which implements // * {@link ClientFactoryWithDispatch},
 * {@link ErrorAndBusyClientFactory} and {@link ClientFactory}.
 * 
 * @param <C>
 *            the provided client factory type
 */
public class EditSailorProfilePresenter<C extends ClientFactoryWithDispatch & ErrorAndBusyClientFactory & ClientFactory>
        implements EditSailorProfileView.Presenter {

    private final C clientFactory;

    private final StatefulSailorProfileDataProvider sailorProfileDataProvider;

    public EditSailorProfilePresenter(C clientFactory) {
        this.clientFactory = clientFactory;
        this.sailorProfileDataProvider = new StatefulSailorProfileDataProvider(clientFactory,
                new SuggestedMultiSelectionCompetitorDataProviderImpl(clientFactory));
    }

    private class SuggestedMultiSelectionCompetitorDataProviderImpl extends
            AbstractSuggestedCompetitorMultiSelectionPresenter<SuggestedMultiSelectionPresenter.Display<SimpleCompetitorWithIdDTO>> {

        private SuggestedMultiSelectionCompetitorDataProviderImpl(ClientFactoryWithDispatch clientFactory) {
            super(clientFactory);
        }

        @Override
        protected void persist(Collection<SimpleCompetitorWithIdDTO> selectedItem) {
            /** persistence is done in {@link StatefulSailorProfileDataProvider} */
        }
    }

    @Override
    public StatefulSailorProfileDataProvider getDataProvider() {
        return sailorProfileDataProvider;
    }

    @Override
    public PlaceController getPlaceController() {
        return clientFactory.getPlaceController();
    }
}
