package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.communication.user.profile.CompetitorSuggestionResult;
import com.sap.sailing.gwt.home.communication.user.profile.FavoriteBoatClassesDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoriteCompetitorsDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoritesResult;
import com.sap.sailing.gwt.home.communication.user.profile.GetCompetitorSuggestionAction;
import com.sap.sailing.gwt.home.communication.user.profile.GetFavoritesAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveFavoriteBoatClassesAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveFavoriteCompetitorsAction;
import com.sap.sailing.gwt.home.shared.app.ClientFactoryWithDispatch;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedMultiSelectionBoatClassDataProvider;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedMultiSelectionCompetitorDataProvider;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionBoatClassDataProvider;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionCompetitorDataProvider;
import com.sap.sailing.gwt.ui.client.refresh.ErrorAndBusyClientFactory;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public class UserPreferencesPresenter<C extends ClientFactoryWithDispatch & ErrorAndBusyClientFactory>
        implements UserPreferencesView.Presenter {

    private final SuggestedMultiSelectionCompetitorDataProvider competitorDataProvider = 
            new SuggestedMultiSelectionCompetitorDataProviderImpl();
    private final SuggestedMultiSelectionBoatClassDataProvider boatClassDataProvider = 
            new SuggestedMultiSelectionBoatClassDataProviderImpl();
    private final C clientFactory;

    public UserPreferencesPresenter(C clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public void loadPreferences() {
        clientFactory.getDispatch().execute(new GetFavoritesAction(), new AsyncCallback<FavoritesResult>() {
            @Override
            public void onFailure(Throwable caught) {
                clientFactory.createErrorView("Error while loading notification preferences!", caught);
            }

            @Override
            public void onSuccess(FavoritesResult result) {
                initFavoriteCompetitors(result.getFavoriteCompetitors());
                initFavoriteBoatClasses(result.getFavoriteBoatClasses());
            }
        });
    }
    
    @Override
    public SuggestedMultiSelectionCompetitorDataProvider getFavoriteCompetitorsDataProvider() {
        return competitorDataProvider;
    }
    
    @Override
    public SuggestedMultiSelectionBoatClassDataProvider getFavoriteBoatClassesDataProvider() {
        return boatClassDataProvider;
    }

    private void initFavoriteCompetitors(FavoriteCompetitorsDTO favoriteCompetitors) {
        competitorDataProvider.initNotifications(favoriteCompetitors.isNotifyAboutResults());
        competitorDataProvider.initSelectedItems(favoriteCompetitors.getSelectedCompetitors());
    }

    private void initFavoriteBoatClasses(FavoriteBoatClassesDTO favoriteBoatClasses) {
        boatClassDataProvider.initNotifications(favoriteBoatClasses.isNotifyAboutUpcomingRaces(),
                favoriteBoatClasses.isNotifyAboutResults());
        boatClassDataProvider.initSelectedItems(favoriteBoatClasses.getSelectedBoatClasses());
    }

    private class SuggestedMultiSelectionBoatClassDataProviderImpl
            extends AbstractSuggestedMultiSelectionBoatClassDataProvider {
        @Override
        protected void persist(FavoriteBoatClassesDTO favorites) {
            clientFactory.getDispatch().execute(new SaveFavoriteBoatClassesAction(favorites), new SaveAsyncCallback());
        }
    }

    private class SuggestedMultiSelectionCompetitorDataProviderImpl
            extends AbstractSuggestedMultiSelectionCompetitorDataProvider {

        @Override
        protected void getSuggestions(Iterable<String> queryTokens, int limit,
                final SuggestionItemsCallback<SimpleCompetitorWithIdDTO> callback) {
            clientFactory.getDispatch().execute(new GetCompetitorSuggestionAction(queryTokens, limit),
                    new AsyncCallback<CompetitorSuggestionResult>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            Window.alert("Error while loading competitor suggestion");
                        }

                        @Override
                        public void onSuccess(CompetitorSuggestionResult result) {
                            callback.setSuggestionItems(result.getValues());
                        }
                    });
        }

        @Override
        protected void persist(FavoriteCompetitorsDTO favorites) {
            clientFactory.getDispatch().execute(new SaveFavoriteCompetitorsAction(favorites), new SaveAsyncCallback());
        }
    }

    private class SaveAsyncCallback implements AsyncCallback<VoidResult> {
        @Override
        public void onFailure(Throwable caught) {
            clientFactory.createErrorView("Error while saving notification preferences!", caught);
        }

        @Override
        public void onSuccess(VoidResult result) {
        }
    }

}
