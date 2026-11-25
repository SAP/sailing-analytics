package com.sap.sailing.gwt.managementconsole.partials.contextmenu;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
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

    private HandlerRegistration registration;

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

    /**
     * Field to indicate the ContextItem, which should be the primary item. In this case e.g. ENTER key pressed event
     * will activate the item action (click). If primaryItemIndex is not set no primary item is defined (default).
     */
    private Integer primaryItemIndex;

    public ContextMenu() {
        initWidget(uiBinder.createAndBindUi(this));
        this.local_res.style().ensureInjected();
        this.menuPopup = new MenuPopup();
        registration = Event.addNativePreviewHandler(new NativePreviewHandler() {
            @Override
            public void onPreviewNativeEvent(NativePreviewEvent event) {
                switch (event.getTypeInt()) {
                case Event.ONKEYDOWN:
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                        menuPopup.hide();
                    } else if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                        ContextMenuItem primaryMenuItem = getPrimaryItem();
                        if (primaryMenuItem != null) {
                            primaryMenuItem.click();
                        }
                    }
                    break;
                }
            }
        });
    }

    public void setHeaderWidget(final IsWidget widget) {
        this.headerContainer.setWidget(widget);
    }

    public void addItem(final String label, final String iconStyle, final ClickHandler handler) {
        addItem(label, iconStyle, handler, /* enabled */ true, /* tooltip */ null);
    }

    public void addItem(final String label, final String iconStyle, final ClickHandler handler, final boolean enabled,
            final String tooltip) {
        final ContextMenuItem item = new ContextMenuItem(label, iconStyle);
        item.addClickHandler(event -> menuPopup.hide());
        item.addClickHandler(handler);
        item.setEnabled(enabled);
        if (tooltip != null) {
            item.setTooltip(tooltip);
        }
        itemsContainer.add(item);
    }

    public void show() {
        Scheduler.get().scheduleDeferred(() -> {
            menuPopup.center();
            menuPopup.setPopupActive(true);
        });
        final HandlerRegistration resizeHandler = Window
                .addResizeHandler(event -> Scheduler.get().scheduleDeferred(() -> menuPopup.center()));
        menuPopup.addCloseHandler(event -> {
            onClose(event, resizeHandler);
        });
    }

    private void onClose(CloseEvent<PopupPanel> event, HandlerRegistration resizeHandler) {
        resizeHandler.removeHandler();
        registration.removeHandler();
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

    /**
     * Returns the ContextMenuItem which is set as primary, if set (this is optional).
     * 
     * @return the primary ContextMenuItem or null if primaryItemIndex is not defined.
     */
    private ContextMenuItem getPrimaryItem() {
        ContextMenuItem primaryItem = null;
        if (primaryItemIndex != null && itemsContainer.getWidgetCount() - 1 > primaryItemIndex) {
            Object primaryWidget = itemsContainer.getWidget(primaryItemIndex);
            if (primaryWidget instanceof ContextMenuItem) {
                primaryItem = (ContextMenuItem) primaryWidget;
            }
        }
        return primaryItem;
    }

    /**
     * Setter of the index of an optional primary item.
     * 
     * @param index
     *            index of the {@link ContextMenuItem}. Can be null to remove primary item definition.
     */
    public void setPrimaryItemIndex(Integer index) {
        this.primaryItemIndex = index;
        for (int i = 0; i < itemsContainer.getWidgetCount(); i++) {
            itemsContainer.getWidget(i).removeStyleName(local_res.style().primaryItem());
            if (i == primaryItemIndex) {
                itemsContainer.getWidget(i).addStyleName(local_res.style().primaryItem());
            }
        }
    }

}