package com.sap.sailing.gwt.home.client.place.event.regattaraces;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface EventRegattaRacesResources extends ClientBundle {
    public static final EventRegattaRacesResources INSTANCE = GWT.create(EventRegattaRacesResources.class);

    @Source("com/sap/sailing/gwt/home/client/place/event/regattaraces/EventRegattaRaces.css")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String regattanavigation();
        String eventregattaraces();
        String eventregattaraces_phase();
        String eventregattaraces_phase_name();
        String eventregattaraces_phase_day();
        String eventregattaraces_phase_races();
        String eventregattaraces_phase_fleets();
        String eventregattaraces_phase_fleet();
        String eventregattaraces_phase_fleet_name();
        String eventregattaraces_phase_fleet_count();
        String eventregattaraces_legend();
        String eventregattaraces_legend_item();
        String eventregattaraces_legend_itemgps();
        String eventregattaraces_legend_itemwind();
        String eventregattaraces_legend_itemvideo();
        String eventregattaraces_legend_itemaudio();
        String eventregattarace();
        String eventregattarace_fleetindicator();
        String eventregattarace_details();
        String eventregattarace_name();
        String eventregattarace_nameshort();
        String eventregattarace_flag();
        String eventregattarace_wind();
        String eventregattarace_wind_strength();
        String eventregattarace_leader();
        String eventregattarace_leader_name();
        String eventregattarace_features();
        String eventregattarace_legs();
        String eventregattarace_legs_leg();
        String eventregattarace_action();
        String eventregattarace_actionwatch();
        String eventregattarace_actiondisabled();
        String eventregattarace_feature_gps();
        String eventregattarace_feature_wind();
        String eventregattarace_feature_video();
        String eventregattarace_feature_audio();
        String eventregattarace_feature_unavailable();
    }
}
