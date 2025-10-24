package com.sap.sse.gwt.client.media;

import com.sap.sse.common.media.TakedownNoticeRequestContext;

public interface TakedownNoticeService {
    void fileTakedownNotice(TakedownNoticeRequestContext takedownNoticeRequestContext);

    /**
     * @return {@code true} if and only if a user is currently logged in and that user's e-mail address has been
     *         validated successfully
     */
    boolean isEmailAddressOfCurrentUserValidated();

    /**
     * @return {@code null} if no current user is logged in, or the current user's name
     */
    String getCurrentUserName();
}
