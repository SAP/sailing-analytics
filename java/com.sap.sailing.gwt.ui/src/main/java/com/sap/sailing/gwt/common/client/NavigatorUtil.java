package com.sap.sailing.gwt.common.client;

import com.google.gwt.core.client.GWT;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;

public class NavigatorUtil {
    /**
     * Note that this method will check whether the browser does support the copyToClipboard feature first and will
     * display a warning notification if it does not, to prevent JS error messages in the console. Please use
     * {@link clientHasNavigatorCopyToClipboardSupport} to check for browser support, if case specific handling is
     * required. On a successful copy an info notification is displayed.
     * 
     * @param String
     *            text: Text to share.
     */
    public static void copyToClipboard(String text) {
        if (nativeClientHasNavigatorCopyToClipboardSupport()) {
            nativeCopyToClipboard(text);
            Notification.notify(StringMessages.INSTANCE.sharingLinkCopied(), NotificationType.INFO);
        } else {
            GWT.log("This browser does not support copying to clipboard");
            Notification.notify(StringMessages.INSTANCE.browserDoesNotSupportCopyToClipboard(),
                    NotificationType.WARNING);
        }
    }
    
    
    /**
     * Note that this method will check whether the browser does support the sharing feature first and will display a
     * warning notification if it does not, to prevent JS error messages in the console. Please use
     * {@link clientHasNavigatorShareSupport} to check for browser support, if case specific handling is required.
     * 
     * @param String
     *            url: URL to share.
     * @param String
     *            text: Optional text to share.
     */
    public static void shareUrl(String url, String text) {
        if (nativeClientHasNavigatorShareSupport()) {
            if (text != null) {
                nativeShareUrlAndText(url, text);
            } else {
                nativeShareUrl(url);
            }
        } else {
            GWT.log("This browser does not support native sharing");
            Notification.notify(StringMessages.INSTANCE.browserDoesNotSupportNativeSharing(), NotificationType.WARNING);
        }
    }
    
    public static boolean clientHasNavigatorShareSupport(){
        return nativeClientHasNavigatorShareSupport();
    }
    
    public static boolean clientHasNavigatorCopyToClipboardSupport() {
        return nativeClientHasNavigatorCopyToClipboardSupport();
    }
    
    private static native void nativeCopyToClipboard(String text) /*-{
        window.focus();
        navigator.clipboard.writeText(text);
    }-*/;

    private static native void nativeShareUrl(String url) /*-{
        window.focus();
        navigator.share({
            url : url,
        });
    }-*/;
    
    private static native void nativeShareUrlAndText(String url, String text) /*-{
        window.focus();
        navigator.share({
            url : url,
            text : text
        });
    }-*/;

    private static native boolean nativeClientHasNavigatorShareSupport() /*-{
        window.focus();
        if (navigator && navigator.share) {
            return true;
        } else {
            return false;
        }
    }-*/;

    private static native boolean nativeClientHasNavigatorCopyToClipboardSupport() /*-{
        window.focus();
        if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
            return true;
        } else {
            return false;
        }
    }-*/;
}
