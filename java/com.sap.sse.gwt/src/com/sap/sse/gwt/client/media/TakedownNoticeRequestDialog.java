package com.sap.sse.gwt.client.media;

import java.util.Collections;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.common.media.NatureOfClaim;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.gwt.client.controls.GenericListBox;
import com.sap.sse.gwt.client.controls.listedit.StringListEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * A dialog that is shown to the user who wants to file a take-down notice for a media file found
 * on the site, such as a video or an image.<p>
 * 
 * Upon dialog construction, basic contextual information such as the media URL, message key and
 * message parameter must be provided. The remaining information that is required for the construction
 * of the result {@link TakedownNoticeRequestContext} object is collected by the dialog.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TakedownNoticeRequestDialog extends DataEntryDialog<TakedownNoticeRequestContext> {
    private static TakedownNoticeService serviceToUseForTakedownNotice;
    private final String contextDescriptionMessageKey;
    private final String contextDescriptionMessageParameter;
    private final String contentUrl;
    private final GenericListBox<NatureOfClaim> natureOfClaimListBox;
    private final TextArea reportingUserCommentTextArea;
    private final StringListEditorComposite supportingURLsEditor;
    private final String username;
    private final StringMessages stringMessages;
    
    /**
     * Invoke this method to install a JavaScript callback function
     * {@code showTakedownNoticeRequestDialog(contextDescriptionMessageKey, contextDescriptionMessageParameter, contentUrl, username)}
     * into the current page (the {@code $wnd} document) that can be invoked with arguments which will be forwarded to a
     * JSNI call to the
     * {@link #TakedownNoticeRequestDialog(String, String, String, String, StringMessages, DialogCallback)} constructor
     * and will then display the dialog and, if confirmed, uses the {@link TakedownNoticeRequestContext dialog result}
     * to then carry out the request by a call to the server.<p>
     * 
     * To use this in an {@code onclick} callback function of an element defined by a {@code @Template}, you have to
     * "park" the parameters in attributes of the element and access them from within the {@code onclick} handler
     * using the {@code getAttribute} method, e.g., like this:
     * 
     * <pre>
     *   &ltdiv class='{0}'
     *          takedown-contextDescriptionMessageKey='{1}'
     *          takedown-contextDescriptionMessageParameter='{2}'
     *          takedown-contentUrl='{3}' takedown-username='{4}'
     *          onclick=\"showTakedownNoticeRequestDialog(this.getAttribute('takedown-contextDescriptionMessageKey'), this.getAttribute('takedown-contextDescriptionMessageParameter'), this.getAttribute('takedown-contentUrl'), this.getAttribute('takedown-username'))\"&gt;⋯&lt;/div&gt;
     * </pre>
     */
    public static void ensureJSFunctionInstalled(TakedownNoticeService serviceToUseForTakedownNotice) {
        TakedownNoticeRequestDialog.serviceToUseForTakedownNotice = serviceToUseForTakedownNotice;
        ensureJSFunctionInstalled();
    }
    
    private static native void ensureJSFunctionInstalled() /*-{
        if ($wnd.showTakedownNoticeRequestDialog == null) {
            $wnd.showTakedownNoticeRequestDialog = $entry(function(contextDescriptionMessageKey, contextDescriptionMessageParameter, contentUrl, username) {
                @com.sap.sse.gwt.client.media.TakedownNoticeRequestDialog::showTakedownNoticeRequestDialog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(contextDescriptionMessageKey, contextDescriptionMessageParameter, contentUrl, username);
            });
        }
    }-*/;
    
    private static void showTakedownNoticeRequestDialog(String contextDescriptionMessageKey, String contextDescriptionMessageParameter,
            String contentUrl, String username) {
        if (!Util.hasLength(username.trim()) || !serviceToUseForTakedownNotice.isEmailAddressOfCurrentUserValidated()) {
            Notification.notify(StringMessages.INSTANCE.mustBeLoggedInAndWithValidatedEmail(), NotificationType.ERROR);
        } else {
            final TakedownNoticeRequestDialog dialog = new TakedownNoticeRequestDialog(contextDescriptionMessageKey, contextDescriptionMessageParameter, contentUrl, username, StringMessages.INSTANCE, new DialogCallback<TakedownNoticeRequestContext>() {
                @Override
                public void ok(TakedownNoticeRequestContext editedObject) {
                    serviceToUseForTakedownNotice.fileTakedownNotice(editedObject);
                }
                
                @Override
                public void cancel() {
                }
            });
            dialog.show();
        }
    }
    
    public TakedownNoticeRequestDialog(String contextDescriptionMessageKey, String contextDescriptionMessageParameter,
            String contentUrl, String username, StringMessages stringMessages,
            DialogCallback<TakedownNoticeRequestContext> callback) {
        super(stringMessages.takedownNoticeDialogTitle(), stringMessages.takedownNoticeDialogMessage(contentUrl),
                stringMessages.ok(), stringMessages.cancel(), /* validator */ null, callback);
        this.stringMessages = stringMessages;
        this.username = username;
        this.reportingUserCommentTextArea = createTextArea("");
        this.natureOfClaimListBox = createGenericListBox(this::getDisplayString, /* isMultipleSelect */ false);
        for (final NatureOfClaim noc : NatureOfClaim.values()) {
            this.natureOfClaimListBox.addItem(noc);
        }
        this.supportingURLsEditor = new StringListEditorComposite(/* initial values */ Collections.emptySet(), stringMessages,
                IconResources.INSTANCE.removeIcon(), /* suggestValues */ Collections.emptySet());
        this.contextDescriptionMessageKey = contextDescriptionMessageKey;
        this.contextDescriptionMessageParameter = contextDescriptionMessageParameter;
        this.contentUrl = contentUrl;
        getDialogBox().getElement().getStyle().setProperty("overflowWrap", "anywhere"); // allow long URLs to be wrapped, especially on small phone screens...
        getDialogBox().setWidth(""+(Math.min(Window.getClientWidth(), 600))+"px");
        getStatusLabel().getElement().getStyle().setProperty("height", "0px"); // hide status because it isn't used here
    }
    
    public String getDisplayString(NatureOfClaim natureOfClaim) {
        switch (natureOfClaim) {
        case COPYRIGHT_INFRINGEMENT:
            return stringMessages.natureOfClaim_CopyrightInfringement();
        case DEFAMATORY_CONTENT:
            return stringMessages.natureOfClaim_DefamatoryContent();
        case INAPPROPRIATE:
            return stringMessages.natureOfClaim_Inappropriate();
        case VIOLENCE:
            return stringMessages.natureOfClaim_Violence();
        case NONE:
            return stringMessages.natureOfClaim_None();
        case OTHER:
            return stringMessages.natureOfClaim_Other();
        }
        throw new RuntimeException("unknown NatureOfClaim: "+natureOfClaim);
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(3, 2);
        result.setStyleName("table-with-spacing");
        int row = 0;
        result.setWidget(row, 0, new Label(stringMessages.natureOfTakedownClaim()));
        result.setWidget(row++, 1, natureOfClaimListBox);
        result.setWidget(row,  0, new Label(stringMessages.comment()));
        result.setWidget(row++, 1, reportingUserCommentTextArea);
        result.setWidget(row, 0, new Label(stringMessages.supportingURLs()));
        result.setWidget(row++, 1, supportingURLsEditor);
        return result;
    }

    @Override
    protected TakedownNoticeRequestContext getResult() {
        return new TakedownNoticeRequestContext(contextDescriptionMessageKey, contextDescriptionMessageParameter,
                contentUrl, Window.Location.createUrlBuilder().buildString(), natureOfClaimListBox.getValue(),
                reportingUserCommentTextArea.getValue(), supportingURLsEditor.getValue(), username);
    }
}
