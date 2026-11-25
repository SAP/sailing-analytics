package com.sap.sailing.gwt.settings.client.whatsnew;

import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.settings.SettingsToJsonSerializerGWT;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.security.ui.client.UserService;

/**
 * This is a factory class to create a WhatsNewDialog, if the change log changed for more than
 * {@value #THRESHOLD_WHATS_NEW} characters since the last login of the current user.
 */
public abstract class AbstractWhatsNewDialogFactory<S extends AbstractWhatsNewSettings> {
    private static final long THRESHOLD_WHATS_NEW = 10;
    private static final Logger LOG = Logger.getLogger(AbstractWhatsNewDialogFactory.class.getName());

    private final UserService userService;

    protected AbstractWhatsNewDialogFactory(final UserService userService) {
        this.userService = userService;
    }

    protected void register() {
        userService.addUserStatusEventHandler((user, preAuth) -> {
            if (user != null) {
                showWhatsNewDialogIfNecessaryAndUpdatePreference();
            }
        }, false);
    }
    
    protected final void showWhatsNewDialogIfNecessaryAndUpdatePreference() {
        if (!isUserAlreadyNotified() && ClientConfiguration.getInstance().isBrandingActive()) {
            currentCharacterCount((final int charCount) -> userService.getPreference(getPrefName(),
                    new MarkedAsyncCallback<>(new ServiceCallback<String>() {
                @Override
                public void onSuccess(final String result) {
                    final SettingsToJsonSerializerGWT serializer = new SettingsToJsonSerializerGWT();
                    final DialogCallback<Void> dialogCallback = new DialogCallback<Void>() {
                        @Override
                        public void ok(final Void editedObject) {
                            updateOrCreatePreference(charCount, serializer, new ServiceCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    // open release notes only after the preference has been updated; otherwise, moving back to AdminConsole
                                    // quickly may still find the preference not updated
                                    openReleaseNotes();
                                }
                            });
                        }

                        @Override
                        public void cancel() {
                            updateOrCreatePreference(charCount, serializer, new ServiceCallback<Void>());
                        }
                    };
                    if (result != null) {
                        // deserialize whats-new-setting
                        final S pref = serializer.deserialize(getInstanceForDeserialization(), result);
                        if (pref.getNumberOfCharsOnLastLogin() + THRESHOLD_WHATS_NEW < charCount) {
                            // check if length change is over threshold
                            showDialog(dialogCallback);
                        }
                    } else {
                        // create preference
                        showDialog(dialogCallback);
                    }
                }
            })));
        }
    }

    private void updateOrCreatePreference(final long charCount, final SettingsToJsonSerializerGWT serializer,
            final ServiceCallback<Void> callback) {
        final String serializedSetting = serializer.serializeToString(getInstanceForSerialization(charCount));
        userService.setPreference(getPrefName(), serializedSetting, new MarkedAsyncCallback<>(callback));
    }

    protected abstract void currentCharacterCount(IntConsumer callback);

    protected abstract String getPrefName();

    protected abstract S getInstanceForDeserialization();

    protected abstract S getInstanceForSerialization(Long charCount);

    protected abstract boolean isUserAlreadyNotified();

    protected abstract void showDialog(DialogCallback<Void> callback);

    protected abstract void openReleaseNotes();

    protected static class ServiceCallback<R> implements AsyncCallback<R> {
        @Override
        public void onSuccess(final R result) {
        }

        @Override
        public final void onFailure(final Throwable caught) {
            LOG.log(Level.SEVERE, caught.getMessage(), caught);
        }

    }
}
