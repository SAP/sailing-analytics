package com.sap.sailing.gwt.managementconsole.app;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import com.sap.sailing.gwt.managementconsole.places.dashboard.DashboardPlace;
import com.sap.sailing.gwt.managementconsole.places.event.create.CreateEventPlace;
import com.sap.sailing.gwt.managementconsole.places.event.edit.EditEventPlace;
import com.sap.sailing.gwt.managementconsole.places.event.media.EventMediaPlace;
import com.sap.sailing.gwt.managementconsole.places.event.overview.EventOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.create.CreateEventSeriesPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.events.EventSeriesEventsPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.overview.EventSeriesOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.create.AddRegattaPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.showcase.ShowcasePlace;

@WithTokenizers({ ShowcasePlace.Tokenizer.class, DashboardPlace.Tokenizer.class,
        EventSeriesOverviewPlace.Tokenizer.class, EventSeriesEventsPlace.Tokenizer.class,
        EventOverviewPlace.Tokenizer.class, EventMediaPlace.Tokenizer.class, RegattaOverviewPlace.Tokenizer.class,
        CreateEventPlace.Tokenizer.class, EditEventPlace.Tokenizer.class, AddRegattaPlace.Tokenizer.class,
        CreateEventSeriesPlace.Tokenizer.class })
public interface ManagementConsolePlaceHistoryMapper extends PlaceHistoryMapper {
}
