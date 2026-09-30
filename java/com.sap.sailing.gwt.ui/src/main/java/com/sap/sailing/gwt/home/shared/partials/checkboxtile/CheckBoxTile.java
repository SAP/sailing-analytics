package com.sap.sailing.gwt.home.shared.partials.checkboxtile;

import java.util.function.BiConsumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

/**
 * @param onToggle
 *            if null, toggle will be disabled
 */
public final class CheckBoxTile extends Composite implements HasValue<Boolean> {
    private static CheckBoxTileUiBinder uiBinder = GWT.create(CheckBoxTileUiBinder.class);

    interface CheckBoxTileUiBinder extends UiBinder<Widget, CheckBoxTile> {
    }

    @UiField
    CheckBoxTileResources res;
    @UiField
    Label labelUi;
    @UiField
    CheckBox toggleButtonUi;
    private DivElement loadingOverlay;

    public CheckBoxTile(final String label, final boolean initialState,
            final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle) {
        super();
        CheckBoxTileResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        labelUi.setText(label);
        initToggleButtonUi(initialState, onToggle);
    }

    private void initToggleButtonUi(final boolean initialState,
            final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle) {
        toggleButtonUi.setValue(initialState);
        if (onToggle == null) {
            toggleButtonUi.setEnabled(false);
        }
        toggleButtonUi.getElement().getStyle().setProperty("position", "relative");
        final ValueChangeHandler<Boolean> loadingHandler = value -> {
            final Boolean newlyToggledValue = value.getValue();
            overlayLoadingSpinner();
            final AsyncCallback<VoidResult> callback = new AsyncCallback<VoidResult>() {
                @Override
                public void onFailure(Throwable caught) {
                    // undo failed toggle, false arg enforces silence of change handlers on this call
                    toggleButtonUi.setValue(!newlyToggledValue, false);
                    hideLoadingSpinner();
                }

                @Override
                public void onSuccess(VoidResult result) {
                    hideLoadingSpinner();
                }
            };
            onToggle.accept(newlyToggledValue, callback);
        };
        toggleButtonUi.addValueChangeHandler(loadingHandler);
    }

    private void overlayLoadingSpinner() {
        toggleButtonUi.setEnabled(false);
        if (loadingOverlay == null) {
            initLoadingOverlay();
        }
        loadingOverlay.getStyle().setProperty("display", "flex");
    }

    private void initLoadingOverlay() {
        // add elements
        loadingOverlay = Document.get().createDivElement();
        loadingOverlay.addClassName(res.css().loadingOverlay());
        final DivElement spinner = Document.get().createDivElement();
        spinner.addClassName(res.css().spinner());
        // add to canvas
        loadingOverlay.appendChild(spinner);
        toggleButtonUi.getElement().appendChild(loadingOverlay);
    }

    private void hideLoadingSpinner() {
        if (loadingOverlay != null) {
            loadingOverlay.getStyle().setProperty("display", "none");
            toggleButtonUi.setEnabled(true);
        }
    }

    @Override
    public void setValue(Boolean b) {
        toggleButtonUi.setValue(b);
    }

    @Override
    public Boolean getValue() {
        return toggleButtonUi.getValue();
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
        return toggleButtonUi.addValueChangeHandler(handler);
    }
    
    @Override
    public void setValue(Boolean value, boolean fireEvents) {
        toggleButtonUi.setValue(value, fireEvents);
    }
}
