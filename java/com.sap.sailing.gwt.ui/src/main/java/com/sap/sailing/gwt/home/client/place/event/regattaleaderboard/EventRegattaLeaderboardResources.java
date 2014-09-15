package com.sap.sailing.gwt.home.client.place.event.regattaleaderboard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface EventRegattaLeaderboardResources extends ClientBundle {
    public static final EventRegattaLeaderboardResources INSTANCE = GWT.create(EventRegattaLeaderboardResources.class);

    @Source("com/sap/sailing/gwt/home/client/place/event/regattaleaderboard/EventRegattaLeaderboard.css")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String leaderboardcompetitor();
        String leaderboardcompetitor_rank();
        String leaderboardcompetitor_image();
        String leaderboardcompetitor_nationality();
        String leaderboardcompetitor_itemcenter();
        String regattaleaderboard();
        String regattaleaderboard_meta();
        String regattaleaderboard_meta_title();
        String regattaleaderboard_meta_status();
        String regattaleaderboard_meta_statuslive();
        String regattaleaderboard_meta_navigation();
        String regattaleaderboard_meta_scoring();
        String regattaleaderboard_meta_update();
        String regattaleaderboard_meta_scoring_comment();
        String regattaleaderboard_meta_update_comment();
        String regattaleaderboard_meta_scoring_type();
        String regattaleaderboard_meta_update_type();
        String regattaleaderboard_meta_scoring_timestamp();
        String regattaleaderboard_meta_update_timestamp();
        String regattaleaderboard_meta_scoring_text();
        String regattaleaderboard_meta_update_text();
        String regattaleaderboard_meta_reload();
        String regattaleaderboard_meta_settings_button();
        String regattaleaderboard_meta_settings();
        String regattaleaderboard_table();
        String regattaleaderboard_header();
        String regattaleaderboard_header_item();
        String regattaleaderboard_header_itemrank();
        String regattaleaderboard_header_itemcompetitor();
        String regattaleaderboard_header_itemname();
        String regattaleaderboard_header_itemdata();
        String regattaleaderboard_header_itemdata_element();
        String regattaleaderboard_header_itemdata_elementascending();
        String regattaleaderboard_header_itemdata_elementdescending();
        String regattaleaderboard_header_itempoints();
        String regattaleaderboard_header_item_name();
        String regattaleaderboard_header_itemdata_element_name();
        String regattaleaderboard_header_itemdescending();
        String regattaleaderboard_header_itemascending();
        String regattaleaderboard_header_table();
        String regattaleaderboard_body();
        String regattaleaderboard_data_element();
        String leaderboardcompetitor_item();
        String leaderboardcompetitor_itemcentercenter();
        String leaderboardcompetitor_item_table();
        String leaderboardcompetitor_itemcenter_table();
        String leaderboardcompetitorshort();
    }
}
