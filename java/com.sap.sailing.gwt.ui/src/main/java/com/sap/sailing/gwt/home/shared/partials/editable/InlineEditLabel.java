package com.sap.sailing.gwt.home.shared.partials.editable;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailorProfileResources;

public class InlineEditLabel extends Composite implements HasText {

    private static InlineEditTextfieldUiBinder uiBinder = GWT.create(InlineEditTextfieldUiBinder.class);

    interface InlineEditTextfieldUiBinder extends UiBinder<Widget, InlineEditLabel> {
    }

    public static interface TextChangeHandler {
        void onTextChanged(String text);
    }

    @UiField
    SailorProfileResources res;

    @UiField
    TextBox textBox;

    @UiField
    Label label;

    @UiField
    Label imageUi;

    private boolean state = false;

    private final List<TextChangeHandler> changeHandlers = new ArrayList<>();

    public InlineEditLabel() {
        initWidget(uiBinder.createAndBindUi(this));
        imageUi.getElement().getStyle().setBackgroundImage("url('" + res.editPencil().getSafeUri().asString() + "')");
        textBox.addKeyUpHandler((event) -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                updateState(false);
            }
        });
        label.addClickHandler((event) -> {
            updateState(true);
        });
        updateState(false, true);
    }

    @UiHandler("imageUi")
    void onClick(ClickEvent e) {
        updateState(!state);
    }

    private void updateState(boolean newState) {
        updateState(newState, false);
    }

    private void updateState(boolean newState, boolean suppressEvents) {
        if (newState) {
            textBox.setWidth((label.getOffsetWidth() + 7) + "px");
            label.setVisible(false);
            textBox.setVisible(true);
            textBox.setText(label.getText());
            textBox.setFocus(true);
        } else {
            boolean unchanged = textBox.getText().equals(label.getText());

            label.setVisible(true);
            textBox.setVisible(false);
            label.setText(textBox.getText());

            if (!suppressEvents && !unchanged) {
                changeHandlers.forEach(c -> c.onTextChanged(label.getText()));
            }
        }
        this.state = newState;
    }

    @Override
    public void setText(String text) {
        label.setText(text);
    }

    @Override
    public String getText() {
        return label.getText();
    }

    public void addTextChangeHandler(TextChangeHandler handler) {
        changeHandlers.add(handler);
    }
}
