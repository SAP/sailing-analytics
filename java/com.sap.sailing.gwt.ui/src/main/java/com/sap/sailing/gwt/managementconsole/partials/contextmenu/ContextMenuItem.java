package com.sap.sailing.gwt.managementconsole.partials.contextmenu;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;

/**
 * {@link Composite} representing a context menu entry.
 */
class ContextMenuItem extends Composite implements HasClickHandlers {

    interface ContextMenuItemUiBinder extends UiBinder<Anchor, ContextMenuItem> {
    }

    private static ContextMenuItemUiBinder uiBinder = GWT.create(ContextMenuItemUiBinder.class);

    @UiField
    Element icon, text;

    private final Anchor control;

    ContextMenuItem(final String label, final String iconStyle) {
        initWidget(control = uiBinder.createAndBindUi(this));
        icon.addClassName(iconStyle);
        text.setInnerText(label);
    }

    @Override
    public HandlerRegistration addClickHandler(final ClickHandler handler) {
        return control.addClickHandler(handler);
    }

    public void click() {
        control.getElement().<ButtonElement> cast().click();
    }

}