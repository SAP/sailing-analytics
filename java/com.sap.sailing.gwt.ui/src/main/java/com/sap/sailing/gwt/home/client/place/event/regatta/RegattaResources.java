package com.sap.sailing.gwt.home.client.place.event.regatta;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface RegattaResources extends ClientBundle {
    public static final RegattaResources INSTANCE = GWT.create(RegattaResources.class);

    @Source("com/sap/sailing/gwt/home/client/place/event/regatta/Regatta.css")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String regatta();
        String regatta_intro_details_itemhideoncompact();
        String regatta_intro();
        String regatta_intro_image();
        String regatta_intro_header();
        String regatta_intro_header_name();
        String regatta_intro_header_namelink();
        String regatta_intro_header_details();
        String regatta_intro_header_details_item();
        String regatta_intro_header_details_item_value();
        String regatta_intro_header_details_label();
        String regatta_intro_phases();
        String regatta_intro_phases_phase();
        String regatta_intro_phases_phase_name();
        String regatta_intro_phases_phase_races();
        String regatta_intro_phases_phase_races_race();
        String regatta_intro_phases_leaderboardlink();
        String regatta_intro_scheduled();
        String regatta_intro_results();
        String regatta_intro_results_competitor();
        String regatta_intro_results_competitor_name();
        String regatta_intro_fleets();
        String regatta_intro_fleets_fleet();
        String regatta_intro_fleets_fleet_item();
        String regatta_intro_fleets_fleet_itemfirst();
        String regatta_intro_fleets_fleet_item_value();
        String regatta_intro_fleets_fleet_item_flag();
        String regatta_intro_fleets_fleet_live();
    }
}
