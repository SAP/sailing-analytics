package com.sap.sailing.gwt.managementconsole.places.event.overview;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.managementconsole.partials.contextmenu.ContextMenu;
import com.sap.sailing.gwt.managementconsole.places.event.overview.partials.EventCard;
import com.sap.sailing.gwt.managementconsole.places.event.overview.partials.EventInfo;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class EventOverviewViewImpl extends Composite implements EventOverviewView {

    private Presenter presenter;

    interface EventOverviewViewImplUiBinder extends UiBinder<Widget, EventOverviewViewImpl> {
    }

    private static EventOverviewViewImplUiBinder uiBinder = GWT.create(EventOverviewViewImplUiBinder.class);

    @UiField
    EventOverviewResources local_res;

    @UiField
    ManagementConsoleResources app_res;

    @UiField
    StringMessages i18n;

    @UiField
    Element headerContainer;

    @UiField
    ScrollPanel scrollContainer;

    @UiField
    FlowPanel cards;

    @UiField
    Anchor addEventAnchor, filterEventAnchor, searchEventAnchor;

    public EventOverviewViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
        local_res.style().ensureInjected();
        app_res.icons().ensureInjected();
        addEventAnchor.addClickHandler(e -> presenter.navigateToCreateEvent());
    }

    @Override
    public void setPresenter(final Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void renderEvents(final List<EventMetadataDTO> events) {
        cards.clear();
        events.stream().map(event -> new EventCard(event, presenter)).forEach(cards::add);
    }

    @Override
    public void showContextMenu(final EventMetadataDTO event) {
        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setHeaderWidget(new EventInfo(event));
        contextMenu.addItem(i18n.edit(), app_res.icons().iconSettings(), e -> presenter.editEvent(event));
        contextMenu.addItem(i18n.advanced(), app_res.icons().iconSettings(), e -> presenter.advancedSettings(app_res, event));
        final boolean canDelete = presenter.canDeleteEvent(event);
        contextMenu.addItem(i18n.delete(), app_res.icons().iconDelete(), e -> presenter.deleteEvent(event), canDelete,
                canDelete ? null : i18n.noPermissionToDeleteEvent());
        contextMenu.show();
    }

    @Override
    public void onResize() {
        final int scrollContainerHeight = getElement().getOffsetHeight() - headerContainer.getOffsetHeight();
        scrollContainer.getElement().getStyle().setHeight(scrollContainerHeight, Unit.PX);
    }

}
