package com.sap.sailing.gwt.home.shared.partials.multiselection;

import java.util.function.BiConsumer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.sap.sailing.gwt.home.shared.partials.checkboxtile.CheckBoxTile;
import com.sap.sailing.gwt.home.shared.partials.labeledbox.LabeledBox;
import com.sap.sailing.gwt.home.shared.places.user.profile.preferences.MiscPreferencesPresenter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public class MiscellaneousDisplayImpl {
    public final LabeledBox selectionUi;
    private final CheckBoxTile featureAndCommunityUpdates;

    public MiscellaneousDisplayImpl(final MiscPreferencesPresenter presenter) {
        presenter.registerDisplay(this);
        final String securityUpdatesTitle = StringMessages.INSTANCE.securityUpdates();
        final CheckBoxTile securityUpdates = new CheckBoxTile(securityUpdatesTitle, true, null);
        featureAndCommunityUpdates = composeFeatureAndCommunityUpdatesTile(presenter);
        final FlowPanel tileList = new FlowPanel();
        tileList.add(securityUpdates);
        tileList.add(featureAndCommunityUpdates);
        final String boxTitle = StringMessages.INSTANCE.miscellaneous();
        selectionUi = new LabeledBox(boxTitle, tileList);
    }
    
    public void setDidOptOutOfFeatureAndCommunityEmails(final boolean didOptOutOfFeatureAndCommunityEmails, final boolean fireChangeHandlers) {
        featureAndCommunityUpdates.setValue(didOptOutOfFeatureAndCommunityEmails, fireChangeHandlers);
    }

    private AsyncCallback<VoidResult> wrapCallbackWithToastResponse(final boolean didOptOutOfFeatureAndCommunityEmails,
            final AsyncCallback<VoidResult> callback) {
        final AsyncCallback<VoidResult> callbackWrappedWithToastNotification = new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                final String failText = StringMessages.INSTANCE.couldNotToggleFeatureAndCommunityUpdates();
                Notification.notify(failText, NotificationType.ERROR);
                if (callback != null) {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(VoidResult result) {
                final String passAndTrue = StringMessages.INSTANCE.optedOutOfFeatureAndCommunityUpdates();
                final String passAndFalse = StringMessages.INSTANCE.optedInToFeatureAndCommunityUpdates();
                final String message = didOptOutOfFeatureAndCommunityEmails ? passAndTrue : passAndFalse;
                Notification.notify(message, NotificationType.SUCCESS);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        };
        return callbackWrappedWithToastNotification;
    }

    private CheckBoxTile composeFeatureAndCommunityUpdatesTile(final MiscPreferencesPresenter presenter) {
        final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle = (didOptOutOfFeatureAndCommunityEmails, callback) -> {
            final AsyncCallback<VoidResult> wrappedCallback = wrapCallbackWithToastResponse(didOptOutOfFeatureAndCommunityEmails, callback);
            presenter.updateDidOptOutOfFeatureAndCommunityEmails(didOptOutOfFeatureAndCommunityEmails, wrappedCallback);
        };
        final String title = StringMessages.INSTANCE.optOutOfFeatureAndCommunityUpdates();
        return new CheckBoxTile(title, false, onToggle);
    }
}