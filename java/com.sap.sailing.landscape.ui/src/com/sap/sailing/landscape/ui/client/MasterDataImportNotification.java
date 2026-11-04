package com.sap.sailing.landscape.ui.client;

import com.sap.sailing.domain.common.MasterDataImportObjectCreationCount;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;

/**
 * Shared completion notification for master data imports, including those performed while archiving a replica set.
 */
public final class MasterDataImportNotification {
    private MasterDataImportNotification() {
    }

    public static void notifySuccess(final MasterDataImportObjectCreationCount creationCount,
            final StringMessages stringMessages) {
        notifySuccess(creationCount, stringMessages, /* additionalInformation */ null);
    }

    /**
     * Adds optional context, such as archiving steps that continue after the import has completed.
     */
    public static void notifySuccess(final MasterDataImportObjectCreationCount creationCount,
            final StringMessages stringMessages, final String additionalInformation) {
        final StringBuilder message = new StringBuilder(stringMessages.importSuccess(
                creationCount.getLeaderboardGroupCount(), creationCount.getLeaderboardCount(),
                creationCount.getEventCount(), creationCount.getRegattaCount(), creationCount.getMediaTrackCount(),
                creationCount.getTrackedRacesCount()));
        final Iterable<String> overwrittenRegattas = creationCount.getNamesOfOverwrittenRegattaNames();
        if (!Util.isEmpty(overwrittenRegattas)) {
            message.append("\n\n").append(stringMessages.importSuccessOverwriteInfo());
            for (final String regattaName : overwrittenRegattas) {
                message.append("\n").append(regattaName);
            }
        }
        if (additionalInformation != null && !additionalInformation.isEmpty()) {
            message.append("\n\n").append(additionalInformation);
        }
        Notification.notify(message.toString(), NotificationType.SUCCESS);
    }
}
