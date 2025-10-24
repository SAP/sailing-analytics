package com.sap.sailing.gwt.ui.adminconsole.whatsnew;

import java.util.function.IntConsumer;

import com.google.gwt.user.client.Window;
import com.sap.sailing.gwt.settings.client.whatsnew.AbstractWhatsNewDialogFactory;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.i18n.StringMessages;

/**
 * Implementation of {@link AbstractWhatsNewDialogFactory} for the AdminConsole.html change log.
 */
public final class WhatsNewDialogFactory extends AbstractWhatsNewDialogFactory<WhatsNewSettings> {

    /** Automatically shows the whats-new dialog after login, if necessary. */
    public static void register(final UserService userService, final SailingServiceAsync sailingService) {
        new WhatsNewDialogFactory(userService, sailingService).register();
    }

    private static final String RELEASE_NOTES_PATH = "/release_notes_admin.html";
    private static boolean userNotified = false;

    private final SailingServiceAsync sailingService;

    private WhatsNewDialogFactory(final UserService userService, final SailingServiceAsync sailingService) {
        super(userService);
        this.sailingService = sailingService;
    }

    @Override
    protected void currentCharacterCount(final IntConsumer callback) {
        sailingService.getAdminConsoleChangeLogSize(new ServiceCallback<Integer>() {
            @Override
            public void onSuccess(final Integer result) {
                callback.accept(result.intValue());
            }
        });
    }

    @Override
    protected String getPrefName() {
        return WhatsNewSettings.PREF_NAME;
    }

    @Override
    protected WhatsNewSettings getInstanceForDeserialization() {
        return new WhatsNewSettings(null);
    }

    @Override
    protected WhatsNewSettings getInstanceForSerialization(final Long charCount) {
        return new WhatsNewSettings(charCount);
    }

    @Override
    protected boolean isUserAlreadyNotified() {
        return userNotified;
    }

    @Override
    protected void showDialog(final DialogCallback<Void> dialogCallback) {
        final DataEntryDialog<Void> dialog = new DataEntryDialog<Void>(StringMessages.INSTANCE.whatsNewDialogTitle(),
                StringMessages.INSTANCE.whatsNewDialogMessage(), StringMessages.INSTANCE.showChangelog(),
                StringMessages.INSTANCE.cancel(), null, dialogCallback) {
            @Override
            protected Void getResult() {
                return null;
            }
        };
        userNotified = true;
        dialog.ensureDebugId("AdminWhatsNewDialog");
        dialog.show();
    }

    @Override
    protected void openReleaseNotes() {
        Window.open(RELEASE_NOTES_PATH, "_blank", /* features */ null);
    }
}
