package com.sap.sailing.gwt.home.desktop.partials.standings;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface StandingsResources extends ClientBundle {
    public static final StandingsResources INSTANCE = GWT.create(StandingsResources.class);

    @Source("Standings.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String regattalistitem();
        String regattalistitem_table();
        String regattalistitem_tableborderTop();
        String regattalistitem_steps();
        String regattalistitem_steps_step();
        String regattalistitem_steps_stepinactive();
        String regattalistitem_steps_step_infowrapper();
        String regattalistitem_steps_step_name();
        String regattalistitem_steps_step_check();
        String regattalistitem_steps_step_progress();
        String regattalistitem_steps_step_fleets();
        String regattalistitem_steps_step_fleets_fleet();
        String regattalistitem_steps_step_fleets_fleet_progress();
        String regattalistitem_steps_button();
        String button();
        String race();
        String race_item();
        String race_fleetcorner();
        String race_fleetcorner_icon();
        String race_itemrank();
        String race_itemshort();
        String race_itemlong();
        String race_itemwind();
        String race_itemwindflag();
        String race_item_windflag();
        String race_itemname();
        String race_itemwinner();
        String race_item_flag();
        String race_item_position();
        String race_item_winner();
        String race_itemcenter();
        String race_itemright();
        String race_itemright_button();
        String race_itemstatus();
        String race_itemstatus_container();
        String race_itemstatus_container_text();
        String race_itemstatus_container_progressbar();
        String race_itemstatus_container_progressbar_progress();
        String racewinners();
        String race_fleetcornerblue();
        String race_fleetcornerred();
        String race_fleetcornersilver();
        String race_fleetcornergold();
        String race_fleetcorneryellow();
        String standings();
        String standings_title();
        String standings_description();
        String standings_list();
        String standings_listhidden();
        String standings_list_content();
        String small10();
        String standings_list_content_picture();
        String standings_list_content_picture_image();
        String standings_list_content_rank();
        String standings_list_content_medal();
        String standings_list_content_flag();
        String race_itemwithbutton();
        String regattaheader();
    }
}
