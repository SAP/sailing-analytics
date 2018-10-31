package com.sap.sailing.gwt.home.shared.partials.dialog.whatsnew;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewResources;
import com.sap.sailing.gwt.home.shared.partials.dialog.ConfirmDialogPanel;
import com.sap.sailing.gwt.home.shared.partials.dialog.DialogResources;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.settings.SettingsToJsonSerializerGWT;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.i18n.StringMessages;

/**
 * This is a factory class to create a WhatsNewDialog, if the changelog changed for more than
 * {@link #THRESHOLD_WHATS_NEW} characters since the last login of the current user.
 */
public final class WhatsNewDialogFactory {

    private static final long THRESHOLD_WHATS_NEW = 10;
    private static final Logger LOG = Logger.getLogger(WhatsNewDialogFactory.class.getName());

    private WhatsNewDialogFactory() {
    }

    /** automatically shows the whats-new dialog after login, if necessary */
    public static void registerWithUserService(UserService userService, PlaceController placeController) {
        userService.addUserStatusEventHandler((userDTO, b) -> {
            if (userDTO != null) {
                showWhatsNewDialogIfNecessary(userService, placeController);
            }
        }, false);
    }

    private static void showWhatsNewDialog(PlaceController placeController) {
        DialogResources.INSTANCE.css().ensureInjected();
        PopupPanel dialog = new PopupPanel();
        ConfirmDialogPanel confirmDialogPanel = new ConfirmDialogPanel(StringMessages.INSTANCE.whatsNewDialogMessage(),
                StringMessages.INSTANCE.whatsNewDialogTitle(), new DialogCallback<Void>() {
                    @Override
                    public void ok(Void editedObject) {
                        placeController.goTo(new WhatsNewPlace(WhatsNewNavigationTabs.SailingAnalytics));
                    }

                    @Override
                    public void cancel() {
                    }
                }, dialog);
        dialog.setWidget(confirmDialogPanel);
        confirmDialogPanel.addStyleName(DialogResources.INSTANCE.css().dialog());

        confirmDialogPanel.setButtonLabels(StringMessages.INSTANCE.showChangelog(), StringMessages.INSTANCE.cancel());

        dialog.addStyleName(DialogResources.INSTANCE.css().backgroundPanel());
        dialog.show();

        dialog.addDomHandler(e -> dialog.hide(), ClickEvent.getType());
        dialog.sinkEvents(Event.ONCLICK | Event.ONTOUCHEND);
    }

    /**
     * Shows a dialog, if the changelog changed for more than {@link #THRESHOLD_WHATS_NEW} characters since the last
     * login of the current user.
     */
    private static void showWhatsNewDialogIfNecessary(UserService userService, PlaceController placeController) {
        final long linesInWhatsChangedDocument = WhatsNewResources.INSTANCE.getSailingAnalyticsNotesHtml().getText()
                .length();
        userService.getPreference(WhatsNewSettings.PREF_NAME, new AsyncCallback<String>() {

            @Override
            public void onSuccess(String result) {
                SettingsToJsonSerializerGWT settingsToJsonSerializerGWT = new SettingsToJsonSerializerGWT();
                if (result != null) {
                    // deserialize whats-new-setting
                    WhatsNewSettings pref = settingsToJsonSerializerGWT.deserialize(new WhatsNewSettings(), result);

                    if (pref.getNumberOfCharsOnLastLogin() > linesInWhatsChangedDocument + THRESHOLD_WHATS_NEW) {
                        // check if length change is over threshold
                        updateOrCreatePreference(linesInWhatsChangedDocument, settingsToJsonSerializerGWT);
                        WhatsNewDialogFactory.showWhatsNewDialog(placeController);
                    }
                } else {
                    // create preference
                    updateOrCreatePreference(linesInWhatsChangedDocument, settingsToJsonSerializerGWT);
                    WhatsNewDialogFactory.showWhatsNewDialog(placeController);
                }
            }

            private void updateOrCreatePreference(long linesInWhatsChangedDocument,
                    SettingsToJsonSerializerGWT settingsToJsonSerializerGWT) {
                String serializedSetting = settingsToJsonSerializerGWT
                        .serializeToString(new WhatsNewSettings(linesInWhatsChangedDocument));
                userService.setPreference(WhatsNewSettings.PREF_NAME, serializedSetting, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        LOG.log(Level.SEVERE, caught.getMessage(), caught);
                    }

                    @Override
                    public void onSuccess(Void result) {
                    }
                });
            }

            @Override
            public void onFailure(Throwable caught) {
                LOG.log(Level.SEVERE, caught.getMessage(), caught);
            }
        });
    }

}
