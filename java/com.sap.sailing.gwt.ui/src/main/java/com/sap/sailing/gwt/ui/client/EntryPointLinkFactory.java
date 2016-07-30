package com.sap.sailing.gwt.ui.client;

import java.util.Collections;
import java.util.Map;

import com.sap.sse.gwt.client.AbstractEntryPointLinkFactory;

public class EntryPointLinkFactory extends AbstractEntryPointLinkFactory {
    public static String createRaceBoardLink(Map<String, String> parameters) {
        return createEntryPointLink("/gwt/RaceBoard.html", parameters);
    }

    public static String createSimulatorLink(Map<String, String> parameters) {
        return createEntryPointLink("/gwt/Simulator.html", parameters);
    }

    public static String createLeaderboardLink(Map<String, String> parameters) {
        return createEntryPointLink("/gwt/Leaderboard.html", parameters);
    }
    
    public static String createLeaderboardEditingLink(Map<String, String> parameters) {
        return createEntryPointLink("/gwt/LeaderboardEditing.html", parameters);
    }
    
    public static String createLeaderboardTabLink(String eventId, String regattaId, Map<String, String> parameters) {
        return createEntryPointLink("/gwt/Home.html#/regatta/leaderboard/:eventId=" + eventId + "&regattaId=" + regattaId, parameters);
    }
    
    public static String createLeaderboardPlaceLink(String eventId, String leaderboardName) {
        final Map<String, String> emptyParams = Collections.emptyMap();
        return createEntryPointLink("/gwt/Home.html#EventPlace:eventId="+eventId+
                "&navigationTab=Regatta&leaderboardName=" +leaderboardName, emptyParams);

    }
    
    public static String createLeaderboardGroupLink(Map<String, String> parameters) {
        return createEntryPointLink("/gwt/Spectator.html", parameters);
    }
    
    public static String createEventPlaceLink(String eventId, Map<String, String> parameters) {
        return createEntryPointLink("/gwt/Home.html#/event/:eventId="+eventId, parameters);
    }
    
    public static String createDashboardLink(Map<String, String> parameters) {
        return createEntryPointLink("/dashboards/RibDashboard.html", parameters);
    }
}
