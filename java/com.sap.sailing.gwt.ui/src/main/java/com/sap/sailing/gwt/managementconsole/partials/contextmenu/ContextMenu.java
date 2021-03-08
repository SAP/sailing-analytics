package com.sap.sailing.gwt.managementconsole.partials.contextmenu;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * {@link Composite} wrapping a {@link PopupPanel} representing a context menu.
 */
public class ContextMenu extends Composite {

    interface ContextMenuUiBinder extends UiBinder<Widget, ContextMenu> {
    }

    private static ContextMenuUiBinder uiBinder = GWT.create(ContextMenuUiBinder.class);
    private static final int FADING_DURATION_MILLIS = 500;

    static String getFadingDuration() {
        return FADING_DURATION_MILLIS + "ms";
    }

    @UiField
    ContextMenuResources local_res;

    @UiField
    SimplePanel headerContainer;

    @UiField
    FlowPanel itemsContainer;

    private final MenuPopup menuPopup;

    public ContextMenu() {
        initWidget(uiBinder.createAndBindUi(this));
        this.local_res.style().ensureInjected();
        this.menuPopup = new MenuPopup();
    }

    public void setHeaderWidget(final IsWidget widget) {
        this.headerContainer.setWidget(widget);
    }

    public void addItem(final String label, final String iconStyle, final ClickHandler handler) {
        final ContextMenuItem item = new ContextMenuItem(label, iconStyle);
        item.addClickHandler(event -> menuPopup.hide());
        item.addClickHandler(handler);
        itemsContainer.add(item);
    }

    public void show() {
        Scheduler.get().scheduleDeferred(() -> {
            menuPopup.center();
            menuPopup.setPopupActive(true);
        });
        final HandlerRegistration resizeHandler = Window
                .addResizeHandler(event -> Scheduler.get().scheduleDeferred(() -> menuPopup.center()));
        menuPopup.addCloseHandler(event -> resizeHandler.removeHandler());
    }

    private class MenuPopup extends PopupPanel {

        private MenuPopup() {
            setGlassEnabled(true);
            setGlassStyleName(local_res.style().contextMenuGlass());

            Event.sinkEvents(getGlassElement(), Event.ONCLICK);
            Event.setEventListener(getGlassElement(), event -> {
                MenuPopup.this.hide();
                event.stopPropagation();
                event.preventDefault();
            });

            addStyleName(local_res.style().contextMenu());
            setWidget(ContextMenu.this);
        }

        @Override
        public void hide(final boolean autoClosed) {
            setPopupActive(false);
            Scheduler.get().scheduleFixedPeriod(() -> {
                MenuPopup.super.hide(autoClosed);
                return false;
            }, FADING_DURATION_MILLIS);
        }

        private void setPopupActive(final boolean active) {
            setStyleName(getElement(), local_res.style().active(), active);
            setStyleName(getGlassElement(), local_res.style().active(), active);
        }

    }

}