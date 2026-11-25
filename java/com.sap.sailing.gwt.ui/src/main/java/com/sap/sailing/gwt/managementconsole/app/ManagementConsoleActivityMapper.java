package com.sap.sailing.gwt.managementconsole.app;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.sap.sailing.gwt.managementconsole.places.dashboard.DashboardActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.dashboard.DashboardPlace;
import com.sap.sailing.gwt.managementconsole.places.event.create.CreateEventActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.event.create.CreateEventPlace;
import com.sap.sailing.gwt.managementconsole.places.event.edit.EditEventActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.event.edit.EditEventPlace;
import com.sap.sailing.gwt.managementconsole.places.event.media.EventMediaActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.event.media.EventMediaPlace;
import com.sap.sailing.gwt.managementconsole.places.event.overview.EventOverviewActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.event.overview.EventOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.create.CreateEventSeriesActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.eventseries.create.CreateEventSeriesPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.events.EventSeriesEventsActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.eventseries.events.EventSeriesEventsPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.overview.EventSeriesOverviewActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.eventseries.overview.EventSeriesOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.create.AddRegattaActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.regatta.create.AddRegattaPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.showcase.ShowcaseActivityProxy;
import com.sap.sailing.gwt.managementconsole.places.showcase.ShowcasePlace;
import com.sap.sse.gwt.client.mvp.AbstractActivityProxy;

/**
 * The management console application's {@link ActivityMapper} implementation providing the {@link Activity activity} to
 * start depending on the provided {@link Place place} implementation.
 *
 * <p>
 * <b>Note:</b> Indeed, this mapper provides an {@link AbstractActivityProxy} instance which wraps the actual activity
 * in order to benefit from the <a href="http://www.gwtproject.org/doc/latest/DevGuideCodeSplitting.html">GWT code
 * splitting</a> feature. *
 * </p>
 *
 * @see ActivityMapper#getActivity(Place)
 */
public class ManagementConsoleActivityMapper implements ActivityMapper {

    private final ManagementConsoleClientFactory clientFactory;

    public ManagementConsoleActivityMapper(final ManagementConsoleClientFactory clientFactory) {
        super();
        this.clientFactory = clientFactory;
    }

    @Override
    public Activity getActivity(final Place place) {
        if (place instanceof ShowcasePlace) {
            return new ShowcaseActivityProxy(clientFactory, (ShowcasePlace) place);
        } else if (place instanceof DashboardPlace) {
            return new DashboardActivityProxy(clientFactory, (DashboardPlace) place);
        } else if (place instanceof EventSeriesOverviewPlace) {
            return new EventSeriesOverviewActivityProxy(clientFactory, (EventSeriesOverviewPlace) place);
        } else if (place instanceof EventSeriesEventsPlace) {
            return new EventSeriesEventsActivityProxy(clientFactory, (EventSeriesEventsPlace) place);
        } else if (place instanceof EventOverviewPlace) {
            return new EventOverviewActivityProxy(clientFactory, (EventOverviewPlace) place);
        } else if (place instanceof EventMediaPlace) {
            return new EventMediaActivityProxy(clientFactory, (EventMediaPlace) place);
        } else if (place instanceof RegattaOverviewPlace) {
            return new RegattaOverviewActivityProxy(clientFactory, (RegattaOverviewPlace) place);
        } else if (place instanceof CreateEventPlace) {
            return new CreateEventActivityProxy(clientFactory, (CreateEventPlace) place);
        } else if (place instanceof EditEventPlace) {
            return new EditEventActivityProxy(clientFactory, (EditEventPlace) place);
        } else if (place instanceof CreateEventSeriesPlace) {
            return new CreateEventSeriesActivityProxy(clientFactory, (CreateEventSeriesPlace) place);
        } else if (place instanceof AddRegattaPlace) {
            return new AddRegattaActivityProxy(clientFactory, (AddRegattaPlace) place);
        }
        return null;
    }
}
