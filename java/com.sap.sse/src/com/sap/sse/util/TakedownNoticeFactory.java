package com.sap.sse.util;

import java.util.Locale;

import com.sap.sse.common.Util;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TakedownNoticeFactory {
    /**
     * Produces a translated message from the context's
     * {@link TakedownNoticeRequestContext#getContextDescriptionMessageKeyAndParameters() message key and parameters}.
     * The message key is expected to be known by {@code stringMessages}, and the number of parameters are expected to
     * match the number of placeholders defined by the corresponding message.
     */
    public String getLocalizedMessage(TakedownNoticeRequestContext context, Locale locale, ResourceBundleStringMessages stringMessages) {
        final String messageKey = context.getContextDescriptionMessageKey();
        return stringMessages.get(locale, messageKey, context.getContentUrl(),
                context.getContextDescriptionMessageParameter(), context.getUsername(),
                context.getNatureOfClaim().name(), context.getReportingUserComment(),
                Util.joinStrings(", ", context.getSupportingURLs()));
    }
}
