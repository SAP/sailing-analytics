package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoriteBoatClassesDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoriteCompetitorsDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoritesResult;
import com.sap.sailing.gwt.home.communication.user.profile.GetFavoritesAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveFavoriteBoatClassesAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveFavoriteCompetitorsAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveMiscEmailPreferences;
import com.sap.sailing.gwt.home.shared.app.ClientFactoryWithDispatch;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedBoatClassMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedCompetitorMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.MiscellaneousDisplayImpl;
import com.sap.sailing.gwt.ui.client.refresh.ErrorAndBusyClientFactory;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

/**
 * Reusable implementation of {@link UserPreferencesView.Presenter} which handles the selections and notification
 * toggles of a {@link UserPreferencesView}. It only require an appropriate client factory which implements
 * {@link ClientFactoryWithDispatch} and {@link ErrorAndBusyClientFactory}.
 * 
 * @param <C>
 *            the provided client factory type
 */
public class UserPreferencesPresenter<C extends ClientFactoryWithDispatch & ErrorAndBusyClientFactory>
        implements UserPreferencesView.Presenter {

    private final BoatClassSelectionPresenter boatClassSelectionPresenter = new BoatClassSelectionPresenterImpl();
    final CompetitorSelectionPresenter competitorPresenter;
    private final MiscPreferencesPresenter miscPresenter = new MiscPresenterImpl(); 
    private final C clientFactory;

    public UserPreferencesPresenter(C clientFactory) {
        this.clientFactory = clientFactory;
        competitorPresenter = new CompetitorSelectionPresenterImpl(clientFactory);
    }

    @Override
    public void loadPreferences() {
        final AsyncCallback<FavoritesResult> callback = new AsyncCallback<FavoritesResult>() {
            @Override
            public void onFailure(Throwable caught) {
                clientFactory.createErrorView("Error while loading notification preferences!", caught);
            }

            @Override
            public void onSuccess(FavoritesResult result) {
                final boolean isNotify = result.getFavoriteCompetitors().isNotifyAboutResults();
                final Collection<SimpleCompetitorWithIdDTO> selection = result.getFavoriteCompetitors().getSelectedCompetitors();
                competitorPresenter.initResults(isNotify, selection);
                final FavoriteBoatClassesDTO favoriteBoatClasses = result.getFavoriteBoatClasses();
                boatClassSelectionPresenter.initNotifications(favoriteBoatClasses.isNotifyAboutUpcomingRaces(),
                        favoriteBoatClasses.isNotifyAboutResults(), favoriteBoatClasses.getSelectedBoatClasses());
                miscPresenter.initDidOptOutOfFeatureAndCommunityEmails(result.getDidOptOutOfFeatureAndCommunityEmails());
            }
        };
        clientFactory.getDispatch().execute(new GetFavoritesAction(), callback);
    }
    
    @Override
    public MiscPreferencesPresenter getMiscPresenter() {
        return miscPresenter;
    }
    
    private class MiscPresenterImpl implements MiscPreferencesPresenter {
        MiscellaneousDisplayImpl display;

        @Override
        public void registerDisplay(MiscellaneousDisplayImpl display) {
            this.display = display;
        }

        @Override
        public void initDidOptOutOfFeatureAndCommunityEmails(final boolean didOptOutOfFeatureAndCommunityEmails) {
            if (display != null) {
                display.setDidOptOutOfFeatureAndCommunityEmails(didOptOutOfFeatureAndCommunityEmails, false);
            }
        }
        
        @Override
        public void updateDidOptOutOfFeatureAndCommunityEmails(final boolean didOptOutOfFeatureAndCommunityEmails, final AsyncCallback<VoidResult> callback) {
            clientFactory.getDispatch().execute(new SaveMiscEmailPreferences(didOptOutOfFeatureAndCommunityEmails), callback);
        }
    }
    
    @Override
    public BoatClassSelectionPresenter getFavoriteBoatClassesDataProvider() {
        return boatClassSelectionPresenter;
    }

    private class BoatClassSelectionPresenterImpl
            extends AbstractSuggestedBoatClassMultiSelectionPresenter<BoatClassSelectionPresenter.Display>
            implements BoatClassSelectionPresenter {

        private AsyncCallback<VoidResult> selectionCallback;

        @Override
        public void initNotifications(boolean notifyAboutUpcomingRaces, boolean notifyAboutResults, Collection<BoatClassDTO> selection) {
            this.displays.forEach(display -> {
                display.initResults(notifyAboutUpcomingRaces, notifyAboutResults, selection);
            });
        }

        @Override
        public void setNotifyAboutUpcomingRaces(boolean notifyAboutUpcomingRaces, AsyncCallback<VoidResult> callback) {
            if (!this.displays.isEmpty()) {
                final BoatClassSelectionPresenter.Display display = (BoatClassSelectionPresenter.Display) this.displays
                        .toArray()[0];
                final Collection<BoatClassDTO> selectedItems = display.getSelection();
                persistResults(notifyAboutUpcomingRaces, display.getNotifyAboutResults(), callback, selectedItems);
            }
        }

        @Override
        public void setNotifyAboutResults(boolean notifyAboutResults, AsyncCallback<VoidResult> callback) {
            if (!this.displays.isEmpty()) {
                final BoatClassSelectionPresenter.Display display = (BoatClassSelectionPresenter.Display) this.displays
                        .toArray()[0];
                final Collection<BoatClassDTO> selectedItems = display.getSelection();
                persistResults(display.getNotifyAboutUpcomingRaces(), notifyAboutResults, callback, selectedItems);
            }
        }

        @Override
        protected void persist(Collection<BoatClassDTO> selectedItems) {
            if (!this.displays.isEmpty()) {
                final BoatClassSelectionPresenter.Display display = (BoatClassSelectionPresenter.Display) this.displays
                        .toArray()[0];
                if (selectionCallback != null) {
                    persistResults(display.getNotifyAboutUpcomingRaces(), display.getNotifyAboutResults(),
                            selectionCallback, selectedItems);
                }
            }
        }

        @Override
        public void persistResults(boolean notifyAboutUpcomingRaces, boolean notifyAboutResults,
                AsyncCallback<VoidResult> callback, Collection<BoatClassDTO> latestSelectedItems) {
            final FavoriteBoatClassesDTO favorites = new FavoriteBoatClassesDTO(latestSelectedItems,
                    notifyAboutUpcomingRaces, notifyAboutResults);
            clientFactory.getDispatch().execute(new SaveFavoriteBoatClassesAction(favorites), callback);
        }

        @Override
        public void setSelectionPersistenceCallback(AsyncCallback<VoidResult> selectionCallback) {
            this.selectionCallback = selectionCallback;
        }
    }

    private class CompetitorSelectionPresenterImpl
            extends AbstractSuggestedCompetitorMultiSelectionPresenter<CompetitorSelectionPresenter.Display>
            implements CompetitorSelectionPresenter {
        private AsyncCallback<VoidResult> selectionCallback;
        
        private CompetitorSelectionPresenterImpl(ClientFactoryWithDispatch clientFactory) {
            super(clientFactory);
        }

        @Override
        protected final void persist(Collection<SimpleCompetitorWithIdDTO> selectedItems) {
            if (!this.displays.isEmpty()) {
                final CompetitorSelectionPresenter.Display display = (CompetitorSelectionPresenter.Display) this.displays
                        .toArray()[0];
                if (selectionCallback != null) {
                    persistResults(display.getIsNotify(), selectionCallback, selectedItems);
                }
            }
        }

        @Override
        public void persistResults(boolean notifyAboutResults, AsyncCallback<VoidResult> callback,
                Collection<SimpleCompetitorWithIdDTO> latestSelectedItems) {
            final FavoriteCompetitorsDTO favorites = new FavoriteCompetitorsDTO(latestSelectedItems,
                    notifyAboutResults);
            clientFactory.getDispatch().execute(new SaveFavoriteCompetitorsAction(favorites), callback);
        }

        @Override
        public void initResults(boolean notifyAboutResults, Collection<SimpleCompetitorWithIdDTO> latestSelectedItems) {
            this.displays.forEach(display -> display.initResults(notifyAboutResults, latestSelectedItems));
        }

        @Override
        public void setSelectionPersistenceCallback(AsyncCallback<VoidResult> selectionCallback)  {
            this.selectionCallback = selectionCallback;
        }
    }

    @Override
    public CompetitorSelectionPresenter getFavoriteCompetitorsDataProvider() {
        return competitorPresenter;
    }
}
