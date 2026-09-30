package com.sap.sailing.gwt.home.shared.partials.multiselection;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.partials.checkboxtile.CheckBoxTile;
import com.sap.sailing.gwt.home.shared.partials.filter.AbstractSuggestBoxFilter;
import com.sap.sailing.gwt.home.shared.partials.labeledbox.LabeledBox;
import com.sap.sailing.gwt.home.shared.places.user.profile.preferences.CompetitorSelectionPresenter;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public class CompetitorDisplayImpl implements IsWidget, CompetitorSelectionPresenter.Display {
    public final LabeledBox selectionUi;
    private final FlowPanel childUi;
    private final CheckBoxTile tileUi;
    private final SuggestedMultiSelection<SimpleCompetitorWithIdDTO> filterUi;
    private final CompetitorSelectionPresenter presenter;

    @Override
    public Widget asWidget() {
        return selectionUi;
    }
    
    public boolean getIsNotify() {
        return tileUi.getValue();
    }

    public CompetitorDisplayImpl(CompetitorSelectionPresenter presenter, FlagImageResolver flagImageResolver) {
        this.presenter = presenter;
        presenter.addDisplay(this);
        tileUi = composeTile();
        filterUi = composeFilter(flagImageResolver);
        childUi = new FlowPanel();
        childUi.add(tileUi);
        childUi.add(filterUi);
        final String title = StringMessages.INSTANCE.favoriteCompetitors();
        selectionUi = new LabeledBox(title, childUi);
    }

    private SuggestedMultiSelection<SimpleCompetitorWithIdDTO> composeFilter(FlagImageResolver flagImageResolver) {
        final SuggestedMultiSelection.WidgetFactory<SimpleCompetitorWithIdDTO> widgetFactory = new SuggestedMultiSelection.WidgetFactory<SimpleCompetitorWithIdDTO>() {
            @Override
            public IsWidget generateItemDescriptionWidget(SimpleCompetitorWithIdDTO item) {
                return new SuggestedMultiSelectionCompetitorItemDescription(item, flagImageResolver);
            }

            @Override
            public AbstractSuggestBoxFilter<SimpleCompetitorWithIdDTO, SimpleCompetitorWithIdDTO> generateSuggestionSearchBar(
                    Consumer<SimpleCompetitorWithIdDTO> onSuggestionSelectedCallback) {
                final String text = StringMessages.INSTANCE.add(StringMessages.INSTANCE.competitor());
                return new SuggestedMultiSelection.SelectableSuggestion<SimpleCompetitorWithIdDTO>(presenter, onSuggestionSelectedCallback,
                        text);
            }
        };
        // TODO add different messages to toast response here
        final AsyncCallback<VoidResult> selectionPersistenceCallback = new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(StringMessages.INSTANCE.failedToModifyFavoriteCompetitors(), NotificationType.ERROR);
            }

            @Override
            public void onSuccess(VoidResult result) {
                Notification.notify(StringMessages.INSTANCE.favoriteCompetitorsModifiedSuccessfully(), NotificationType.SUCCESS);
            }
        };
        presenter.setSelectionPersistenceCallback(selectionPersistenceCallback);
        return new SuggestedMultiSelection<>(presenter, widgetFactory);
    }

    private CheckBoxTile composeTile() {
        final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle = (isNowTrue, callback) -> {
            presenter.persistResults(isNowTrue, wrapCallbackWithToastResponse(isNowTrue, callback),
                    presenter.getSelection());
        };
        final String title = StringMessages.INSTANCE.notificationAboutNewResults();
        return new CheckBoxTile(title, false, onToggle);
    }

    private AsyncCallback<VoidResult> wrapCallbackWithToastResponse(final boolean isNowTrue,
            final AsyncCallback<VoidResult> callback) {
        final AsyncCallback<VoidResult> callbackWrappedWithToastNotification = new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                final String failText = StringMessages.INSTANCE
                        .failedToSetStatusOfNotificationsForFavoriteCompetitors();
                Notification.notify(failText, NotificationType.ERROR);
                if (callback != null) {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(VoidResult result) {
                final String passAndTrue = StringMessages.INSTANCE
                        .youWillNowReceiveNotificationsForFavoriteCompetitors();
                final String passAndFalse = StringMessages.INSTANCE
                        .youWillNotReceiveNotificationsForFavoriteCompetitorsAnymore();
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
    public void setSelectedItems(Iterable<SimpleCompetitorWithIdDTO> selectedItemsToSet) {
        filterUi.setSelectedItems(selectedItemsToSet);
    }

    @Override
    public void initResults(boolean notifyAboutResults, Collection<SimpleCompetitorWithIdDTO> latestSelectedItems) {
        filterUi.setSelectedItems(latestSelectedItems);
        tileUi.setValue(notifyAboutResults, false);
    }
}