package com.sap.sailing.gwt.home.shared.partials.multiselection;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.home.shared.partials.checkboxtile.CheckBoxTile;
import com.sap.sailing.gwt.home.shared.partials.filter.AbstractSuggestBoxFilter;
import com.sap.sailing.gwt.home.shared.partials.labeledbox.LabeledBox;
import com.sap.sailing.gwt.home.shared.places.user.profile.preferences.BoatClassSelectionPresenter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public class BoatClassDisplayImpl implements IsWidget, BoatClassSelectionPresenter.Display {
    public final LabeledBox selectionUi;
    private final FlowPanel childUi;
    private final CheckBoxTile upcomingRacesUi;
    private final CheckBoxTile resultsUi;
    private final SuggestedMultiSelection<BoatClassDTO> filterUi;
    private final BoatClassSelectionPresenter presenter;

    @Override
    public Widget asWidget() {
        return selectionUi;
    }

    @Override
    public boolean getNotifyAboutUpcomingRaces() {
        return upcomingRacesUi.getValue();
    }

    @Override
    public boolean getNotifyAboutResults() {
        return resultsUi.getValue();
    }

    @Override
    public Collection<BoatClassDTO> getSelection() {
        return filterUi.getSelection();
    }

    public BoatClassDisplayImpl(final BoatClassSelectionPresenter presenter) {
        this.presenter = presenter;
        presenter.addDisplay(this);
        upcomingRacesUi = composeUpcomingRacesTile();
        resultsUi = composeResultsTile();
        filterUi = composeFilter();
        childUi = new FlowPanel();
        childUi.add(upcomingRacesUi);
        childUi.add(resultsUi);
        childUi.add(filterUi);
        final String title = StringMessages.INSTANCE.favoriteBoatClasses();
        selectionUi = new LabeledBox(title, childUi);
    }

    private SuggestedMultiSelection<BoatClassDTO> composeFilter() {
        final SuggestedMultiSelection.WidgetFactory<BoatClassDTO> widgetFactory = new SuggestedMultiSelection.WidgetFactory<BoatClassDTO>() {
            @Override
            public IsWidget generateItemDescriptionWidget(BoatClassDTO item) {
                return new SuggestedMultiSelectionBoatClassItemDescription(item);
            }

            @Override
            public AbstractSuggestBoxFilter<BoatClassDTO, BoatClassDTO> generateSuggestionSearchBar(
                    Consumer<BoatClassDTO> selectionCallback) {
                final String text = StringMessages.INSTANCE.add(StringMessages.INSTANCE.boatClass());
                return new SuggestedMultiSelection.SelectableSuggestion<BoatClassDTO>(presenter, selectionCallback, text);
            }
        };
        final AsyncCallback<VoidResult> callbackWrappedWithToastNotification = new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(StringMessages.INSTANCE.failedToModifyFavoredBoatClasses(), NotificationType.ERROR);
            }

            @Override
            public void onSuccess(VoidResult result) {
                Notification.notify(StringMessages.INSTANCE.succesfullyModifiedFavoredBoatClasses(), NotificationType.SUCCESS);
            }
        };
        presenter.setSelectionPersistenceCallback(callbackWrappedWithToastNotification);
        return new SuggestedMultiSelection<>(presenter, widgetFactory);
    }

    private CheckBoxTile composeUpcomingRacesTile() {
        final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle = (isNowTrue, callback) -> {
            presenter.setNotifyAboutUpcomingRaces(isNowTrue, wrapCallbackWithToastResponse(isNowTrue, callback,
                    StringMessages.INSTANCE.failedToSetStatusOfUpdatesOnUpcomingRacesForYourFavoredBoatClasses(),
                    StringMessages.INSTANCE.youWillNowReceiveNotificationsForFavoriteBoatClassUpcomingRaces(),
                    StringMessages.INSTANCE.youWillNotReceiveUpdatesOnUpcomingRacesForYourFavoredBoatClassesAnymore()));
        };
        final String title = StringMessages.INSTANCE.notificationAboutUpcomingRaces();
        return new CheckBoxTile(title, false, onToggle);
    }

    private CheckBoxTile composeResultsTile() {
        final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle = (isNowTrue, callback) -> {
            presenter.setNotifyAboutResults(isNowTrue, wrapCallbackWithToastResponse(isNowTrue, callback,
                    StringMessages.INSTANCE.failedToSetStatusOfUpdatesOnNewResultsForYourFavoredBoatClasses(),
                    StringMessages.INSTANCE.youWillNowReceiveUpdatesOnNewResultsForYourFavoredBoatClasses(),
                    StringMessages.INSTANCE.youWillNotReceiveNotificationsForFavoriteBoatClassNewResultsAnymore()));
        };
        final String title = StringMessages.INSTANCE.notificationAboutNewResults();
        return new CheckBoxTile(title, false, onToggle);
    }

    private AsyncCallback<VoidResult> wrapCallbackWithToastResponse(final boolean isNowTrue,
            final AsyncCallback<VoidResult> callback, final String failText, final String passAndTrue,
            final String passAndFalse) {
        final AsyncCallback<VoidResult> callbackWrappedWithToastNotification = new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(failText, NotificationType.ERROR);
                if (callback != null) {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(VoidResult result) {
                final String message = isNowTrue ? passAndTrue : passAndFalse;
                Notification.notify(message, NotificationType.SUCCESS);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        };
        return callbackWrappedWithToastNotification;
    }

    @Override
    public void setSelectedItems(Iterable<BoatClassDTO> selectedItems) {
        filterUi.setSelectedItems(selectedItems);
    }

    @Override
    public void initResults(boolean notifyAboutUpcomingRaces, boolean notifyAboutResults, Collection<BoatClassDTO> selection) {
        upcomingRacesUi.setValue(notifyAboutUpcomingRaces);
        resultsUi.setValue(notifyAboutResults);
        filterUi.setSelectedItems(selection);
    }
}
