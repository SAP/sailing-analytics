package com.sap.sailing.gwt.managementconsole.places.event.overview;

import java.util.List;

import com.google.gwt.user.client.ui.RequiresResize;
import com.sap.sailing.gwt.common.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.managementconsole.mvp.View;
import com.sap.sailing.gwt.managementconsole.partials.contextmenu.HasContextMenuView;
import com.sap.sailing.gwt.managementconsole.places.event.EventPresenter;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;


public interface EventOverviewView
        extends View<EventOverviewView.Presenter>, HasContextMenuView<EventMetadataDTO>, RequiresResize {

    void renderEvents(List<EventMetadataDTO> events);

    interface Presenter extends com.sap.sailing.gwt.managementconsole.mvp.Presenter, EventPresenter {

        void reloadEventList();

        void navigateToCreateEvent();

        void editEvent(EventMetadataDTO event);

        void advancedSettings(ManagementConsoleResources app_res, EventMetadataDTO event);

        void deleteEvent(EventMetadataDTO event);

        boolean canDeleteEvent(EventMetadataDTO event);
    }

}