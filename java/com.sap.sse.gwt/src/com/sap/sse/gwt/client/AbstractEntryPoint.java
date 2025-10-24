package com.sap.sse.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.debug.client.DebugInfo;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.async.PendingAjaxCallMarker;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;
import com.sap.sse.gwt.shared.DebugConstants;

public abstract class AbstractEntryPoint<S extends StringMessages> implements EntryPoint, ErrorReporter, WindowSizeDetector  {
    protected UserAgentDetails userAgent;
    private S stringMessages;
    private ErrorReporter errorReporter;

    @Override
    public final void onModuleLoad() {
        this.stringMessages = createStringMessages();
        this.errorReporter = new DefaultErrorReporter<StringMessages>(stringMessages);
        this.userAgent = new UserAgentDetails(Window.Navigator.getUserAgent());
        if (DebugInfo.isDebugIdEnabled()) {
            PendingAjaxCallBundle bundle = GWT.create(PendingAjaxCallBundle.class);
            TextResource script = bundle.ajaxSemaphoreJS();
            JavaScriptInjector.inject(script.getText());
            DebugInfo.setDebugIdAttribute(DebugConstants.DEBUG_ID_ATTRIBUTE, false);
            DebugInfo.setDebugIdPrefix(DebugConstants.DEBUG_ID_PREFIX);
        }
        doOnModuleLoad();
        if (DebugInfo.isDebugIdEnabled()) {
            PendingAjaxCallMarker.decrementPendingAjaxCalls(MarkedAsyncCallback.CATEGORY_GLOBAL);
        }
    }
    
    /**
     * Provides the concrete implementation the opportunity to specify a concrete subclass of the {@link StringMessages} type to
     * use for i18n support.
     */
    protected abstract S createStringMessages();
    
    protected abstract void doOnModuleLoad();
    
    /**
     * Sets the size of the tab panel when the tab panel is attached to the DOM
     */
    public static void setTabPanelSize(final ResizeComposite tabPanel, final String width, final String height) {
        tabPanel.addAttachHandler(new Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                tabPanel.getElement().getParentElement().getStyle().setProperty("width", width);
                tabPanel.getElement().getParentElement().getStyle().setProperty("height", height);
            }
        });
    }

    @Override
    public void reportError(String message) {
        errorReporter.reportError(message);
    }
    
    @Override
    public void reportError(String title, String message) {
        errorReporter.reportError(title, message);
    }

    @Override
    public void reportError(String message, boolean silentMode) {
        errorReporter.reportError(message, silentMode);
    }
    
    @Override
    public void reportPersistentInformation(String message) {
        errorReporter.reportPersistentInformation(message);
    }

    @Override
    public Widget getPersistentInformationWidget() {
        return errorReporter.getPersistentInformationWidget();
    }
    
    @Override
    public boolean isSmallWidth() {
        int width = Window.getClientWidth();
        return width <= 720;
    }

    protected S getStringMessages() {
        return stringMessages;
    }
}
