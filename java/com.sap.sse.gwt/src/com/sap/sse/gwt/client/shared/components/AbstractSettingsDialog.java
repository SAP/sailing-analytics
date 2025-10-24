package com.sap.sse.gwt.client.shared.components;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Color;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public abstract class AbstractSettingsDialog<SettingsType extends Settings> extends DataEntryDialog<SettingsType> {
    private SettingsDialogComponent<SettingsType> settingsDialogComponent;

    private LinkWithSettingsGenerator<SettingsType> linkWithSettingsGenerator;
    private ShareLinkAnchor shareAnchor;

    private SimplePanel sp;
    
    protected AbstractSettingsDialog(final String shortName, SettingsDialogComponent<SettingsType> dialogComponent,
            StringMessages stringMessages, boolean animationEnabled, LinkWithSettingsGenerator<SettingsType> linkWithSettingsGenerator, final DialogCallback<SettingsType> callback) {
        super(stringMessages.settingsForComponent(shortName), null, stringMessages.ok(), stringMessages.cancel(),
                dialogComponent.getValidator(), animationEnabled, callback != null ? callback : new NoOpDialogCallback<SettingsType>());
        this.settingsDialogComponent = dialogComponent;
        sp =  new SimplePanel();
        this.linkWithSettingsGenerator = linkWithSettingsGenerator;
        if (linkWithSettingsGenerator != null) {
            shareAnchor = new ShareLinkAnchor(stringMessages.sharedSettingsLink(), getLeftButtonPannel());
            shareAnchor.setEnabled(true);
            Scheduler.get().scheduleDeferred(() -> onChange(getResult()));
        }
        sp.setWidget(dialogComponent.getAdditionalWidget(this));
    }
    
    void setDialogComponent(SettingsDialogComponent<SettingsType> dialog){
        sp.setWidget(dialog.getAdditionalWidget(this));
        settingsDialogComponent = dialog;
        setValidator(dialog.getValidator());
        validateAndUpdate();
    }
    
    @Override
    protected void onChange(SettingsType result) {
        if (linkWithSettingsGenerator != null) {
            String link = linkWithSettingsGenerator.createUrl(result);
            shareAnchor.setHref(link);
        }
    }
    
    @Override
    protected void onInvalidStateChanged(boolean invalidState) {
        super.onInvalidStateChanged(invalidState);
        if (linkWithSettingsGenerator != null) {
            shareAnchor.setEnabled(!invalidState);
        }
    }

    @Override
    protected Widget getAdditionalWidget() {
        return sp;
    }

    @Override
    public SettingsType getResult() {
        return settingsDialogComponent.getResult();
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return settingsDialogComponent.getFocusWidget();
    }
    
    private class ShareLinkAnchor implements HasEnabled {

        private final FlowPanel container = new FlowPanel();
        private final Anchor anchor;
        private final Label placeholder;

        private ShareLinkAnchor(String anchorText, FlowPanel parent) {
            this.container.getElement().getStyle().setMargin(0.5, Unit.EM);
            this.container.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
            this.anchor = createEnabledAnchor(anchorText);
            this.placeholder = createDisabledPlaceholder(anchorText);
            container.add(anchor);
            container.add(placeholder);
            parent.add(container);
        }

        private Anchor createEnabledAnchor(String anchorText) {
            Anchor anchor = new Anchor(anchorText);
            anchor.ensureDebugId("ShareAnchor");
            anchor.setTarget("_blank");
            anchor.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
            return anchor;
        }

        private Label createDisabledPlaceholder(String anchorText) {
            Label placeholder = new Label(anchorText);
            placeholder.ensureDebugId("ShareAnchorDiabled");
            placeholder.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
            placeholder.getElement().getStyle().setColor(Color.LIGHT_GRAY.getAsHtml());
            placeholder.getElement().getStyle().setCursor(Cursor.DEFAULT);
            return placeholder;
        }

        private void setHref(String href) {
            this.anchor.setHref(href);
        }

        @Override
        public boolean isEnabled() {
            return anchor.isVisible() && !placeholder.isVisible();
        }

        @Override
        public void setEnabled(boolean enabled) {
            anchor.setVisible(enabled);
            placeholder.setVisible(!enabled);
        }

    }

    private static class NoOpDialogCallback<SettingsType extends Settings> implements DialogCallback<SettingsType> {
        @Override
        public void ok(SettingsType editedObject) {
        }
        @Override
        public void cancel() {
        }
    }
}
