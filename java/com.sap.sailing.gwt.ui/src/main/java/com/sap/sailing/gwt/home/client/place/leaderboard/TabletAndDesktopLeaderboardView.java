package com.sap.sailing.gwt.home.client.place.leaderboard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.home.client.app.HomePlacesNavigator;
import com.sap.sailing.gwt.home.client.place.event.regattaanalytics.RegattaAnalytics;
import com.sap.sailing.gwt.home.client.place.event.seriesanalytics.EventSeriesAnalytics;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;

public class TabletAndDesktopLeaderboardView extends Composite implements AnalyticsView {
    private static LeaderboardPageViewUiBinder uiBinder = GWT.create(LeaderboardPageViewUiBinder.class);

    interface LeaderboardPageViewUiBinder extends UiBinder<Widget, TabletAndDesktopLeaderboardView> {
    }

    @UiField(provided=true) RegattaAnalytics regattaAnalytics;
    @UiField(provided=true) EventSeriesAnalytics seriesAnalytics;
    
    public TabletAndDesktopLeaderboardView(EventDTO event, String leaderboardName, Timer timerForClientServerOffset, HomePlacesNavigator placeNavigator) {
        regattaAnalytics = new RegattaAnalytics(event, leaderboardName, timerForClientServerOffset, placeNavigator);
        seriesAnalytics = new EventSeriesAnalytics(event, leaderboardName, timerForClientServerOffset, placeNavigator);
        
        regattaAnalytics.setVisible(false);
        seriesAnalytics.setVisible(false);
        
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    public void createRegattaAnalyticsViewer(final SailingServiceAsync sailingService, final AsyncActionsExecutor asyncActionsExecutor,
            final Timer timer, final LeaderboardSettings leaderboardSettings, final RegattaAndRaceIdentifier preselectedRace,
            final String leaderboardGroupName, String leaderboardName, final ErrorReporter errorReporter,
            final UserAgentDetails userAgent, boolean showRaceDetails,  
            boolean autoExpandLastRaceColumn, boolean showOverallLeaderboard) {
        regattaAnalytics.createRegattaAnalyticsViewer(sailingService, asyncActionsExecutor, timer, leaderboardSettings, preselectedRace,
                leaderboardGroupName, leaderboardName, errorReporter, userAgent, showRaceDetails, autoExpandLastRaceColumn, showOverallLeaderboard);
        regattaAnalytics.setVisible(true);
    }

    public void createSeriesAnalyticsViewer(SailingServiceAsync sailingService, AsyncActionsExecutor asyncActionsExecutor, 
            Timer timer, LeaderboardSettings leaderboardSettings, String preselectedLeaderboardName, RegattaAndRaceIdentifier preselectedRace,
            String leaderboardGroupName, String metaLeaderboardName, ErrorReporter errorReporter,
            UserAgentDetails userAgent, boolean showRaceDetails, boolean autoExpandLastRaceColumn, boolean showSeriesLeaderboards) {
        seriesAnalytics.createSeriesAnalyticsViewer(sailingService, asyncActionsExecutor, timer, leaderboardSettings, preselectedLeaderboardName,
                preselectedRace, leaderboardGroupName, metaLeaderboardName, errorReporter, userAgent, showRaceDetails, autoExpandLastRaceColumn, showSeriesLeaderboards);
        seriesAnalytics.setVisible(true);
    }
}
