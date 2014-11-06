package com.sap.sailing.gwt.home.client.place.event.seriesanalytics;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.client.app.HomePlacesNavigator;
import com.sap.sailing.gwt.home.client.place.event.header.CompactEventHeader;
import com.sap.sailing.gwt.home.client.place.event.oldcompetitorcharts.OldCompetitorCharts;
import com.sap.sailing.gwt.home.client.place.event.oldleaderboard.OldLeaderboard;
import com.sap.sailing.gwt.home.client.place.event.oldmultileaderboard.OldMultiLeaderboard;
import com.sap.sailing.gwt.home.client.place.event.regattaleaderboard.EventRegattaLeaderboardResources;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;

public class EventSeriesAnalytics extends Composite implements LeaderboardUpdateListener {
    private static EventSeriesAnalyticsUiBinder uiBinder = GWT.create(EventSeriesAnalyticsUiBinder.class);

    interface EventSeriesAnalyticsUiBinder extends UiBinder<Widget, EventSeriesAnalytics> {
    }

    @UiField OldLeaderboard oldOverallLeaderboardComposite;
    @UiField OldCompetitorCharts oldCompetitorChartsComposite;
    @UiField OldMultiLeaderboard oldRegattaLeaderboardsComposite;
    @UiField(provided=true) CompactEventHeader eventHeader;
    
    @UiField SpanElement title;
    @UiField DivElement liveRaceDiv;

    @UiField HTMLPanel overallLeaderboardTabPanel;
    @UiField HTMLPanel regattaLeaderboardsTabPanel;
    @UiField HTMLPanel competitorChartsTabPanel;
    private HTMLPanel activeTabPanel;
    private Widget activeContentWidget;
    private Anchor activeAnchor;

    @UiField Anchor overallLeaderboardAnchor;
    @UiField Anchor regattaLeaderboardsAnchor;
    @UiField Anchor competitorChartsAnchor;
    
    private EventSeriesAnalyticsDataManager eventSeriesAnalyticsManager;
    private Timer autoRefreshTimer;
        
    public EventSeriesAnalytics(EventDTO event, String leaderboardName, Timer timerForClientServerOffset, HomePlacesNavigator placeNavigator) {
        eventHeader = new CompactEventHeader(event, leaderboardName, placeNavigator);
    
        EventRegattaLeaderboardResources.INSTANCE.css().ensureInjected();
        EventSeriesAnalyticsResources.INSTANCE.css().ensureInjected();
        
        initWidget(uiBinder.createAndBindUi(this));
        
        String seriesTitle = leaderboardName;
        if(seriesTitle.endsWith(LeaderboardNameConstants.OVERALL)) {
            seriesTitle = seriesTitle.substring(0, seriesTitle.length() - LeaderboardNameConstants.OVERALL.length());
            seriesTitle = seriesTitle.trim();
        }
        title.setInnerText(seriesTitle);

        activeAnchor = overallLeaderboardAnchor;
        activeTabPanel = overallLeaderboardTabPanel;
        activeContentWidget = null;
        activeAnchor.addStyleName(SharedResources.INSTANCE.mainCss().navbar_buttonactive());

        regattaLeaderboardsTabPanel.setVisible(false);
        competitorChartsTabPanel.setVisible(false);
        liveRaceDiv.getStyle().setVisibility(Visibility.HIDDEN);
    }
    
    public void createSeriesAnalyticsViewer(SailingServiceAsync sailingService, AsyncActionsExecutor asyncActionsExecutor, 
            Timer timer, LeaderboardSettings leaderboardSettings, String preselectedLeaderboardName, RegattaAndRaceIdentifier preselectedRace,
            String leaderboardGroupName, String metaLeaderboardName, ErrorReporter errorReporter,
            UserAgentDetails userAgent, boolean showRaceDetails,  
            boolean autoExpandLastRaceColumn, boolean showSeriesLeaderboards) {
        this.autoRefreshTimer = timer;
        eventSeriesAnalyticsManager = new EventSeriesAnalyticsDataManager(sailingService, asyncActionsExecutor, timer, errorReporter, userAgent);
        eventSeriesAnalyticsManager.createOverallLeaderboardPanel(leaderboardSettings, preselectedRace, leaderboardGroupName, metaLeaderboardName, showRaceDetails, autoExpandLastRaceColumn);
        eventSeriesAnalyticsManager.createMultiLeaderboardPanel(leaderboardSettings, null /*preSelectedLeaderboardName */, preselectedRace, leaderboardGroupName, metaLeaderboardName, showRaceDetails, autoExpandLastRaceColumn);

        DetailType initialDetailType = DetailType.OVERALL_RANK;
        List<DetailType> availableDetailsTypes = new ArrayList<DetailType>();
        availableDetailsTypes.add(DetailType.OVERALL_RANK);
        availableDetailsTypes.add(DetailType.REGATTA_TOTAL_POINTS_SUM);

        eventSeriesAnalyticsManager.createMultiCompetitorChart(metaLeaderboardName, DetailType.OVERALL_RANK);
       
        oldOverallLeaderboardComposite.setLeaderboard(eventSeriesAnalyticsManager.getLeaderboardPanel(), timer);
        oldRegattaLeaderboardsComposite.setMultiLeaderboard(eventSeriesAnalyticsManager.getMultiLeaderboardPanel(), timer);

        oldCompetitorChartsComposite.setChart(eventSeriesAnalyticsManager.getMultiCompetitorChart(), availableDetailsTypes, initialDetailType);
        eventSeriesAnalyticsManager.hideCompetitorChart();

        eventSeriesAnalyticsManager.getLeaderboardPanel().addLeaderboardUpdateListener(this);
        eventSeriesAnalyticsManager.getMultiLeaderboardPanel().addLeaderboardUpdateListener(this);
        
        activeContentWidget = eventSeriesAnalyticsManager.getLeaderboardPanel();
    }

    @Override
    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
        if(activeTabPanel == overallLeaderboardTabPanel) {
            StringMessages stringMessages = StringMessages.INSTANCE;
            
            if(leaderboard != null) {
                List<Pair<RaceColumnDTO, FleetDTO>> liveRaces = leaderboard.getLiveRaces(autoRefreshTimer.getLiveTimePointInMillis());
                boolean hasLiveRace = !liveRaces.isEmpty();
                if (hasLiveRace) {
                    String liveRaceText = "";
                    if(liveRaces.size() == 1) {
                        Pair<RaceColumnDTO, FleetDTO> liveRace = liveRaces.get(0);
                        liveRaceText = stringMessages.raceIsLive("'" + liveRace.getA().getRaceColumnName() + "'");
                    } else {
                        String raceNames = "";
                        for (Pair<RaceColumnDTO, FleetDTO> liveRace : liveRaces) {
                            raceNames += "'" + liveRace.getA().getRaceColumnName() + "', ";
                        }
                        // remove last ", "
                        raceNames = raceNames.substring(0, raceNames.length() - 2);
                        liveRaceText = stringMessages.racesAreLive(raceNames);
                    }
                    liveRaceDiv.setInnerText(liveRaceText);
                } else {
                    liveRaceDiv.setInnerText("");
                }
                liveRaceDiv.getStyle().setVisibility(hasLiveRace ? Visibility.VISIBLE : Visibility.HIDDEN);
                
                oldOverallLeaderboardComposite.updatedLeaderboard(leaderboard, hasLiveRace);
            }
        } else if(activeTabPanel == regattaLeaderboardsTabPanel) {
            oldRegattaLeaderboardsComposite.updatedMultiLeaderboard(leaderboard, false);
        }
    }

    @Override
    public void currentRaceSelected(RaceIdentifier raceIdentifier, RaceColumnDTO raceColumn) {
    }
    
    @UiHandler("overallLeaderboardAnchor")
    void overallLeaderboardTabClicked(ClickEvent event) {
        setActiveTabPanel(overallLeaderboardTabPanel, eventSeriesAnalyticsManager.getLeaderboardPanel(), overallLeaderboardAnchor);
    }

    @UiHandler("regattaLeaderboardsAnchor")
    void regattaLeaderboardsTabClicked(ClickEvent event) {
        setActiveTabPanel(regattaLeaderboardsTabPanel, eventSeriesAnalyticsManager.getMultiLeaderboardPanel(), regattaLeaderboardsAnchor);
    }

    @UiHandler("competitorChartsAnchor")
    void competitorChartsTabClicked(ClickEvent event) {
        setActiveTabPanel(competitorChartsTabPanel, eventSeriesAnalyticsManager.getMultiCompetitorChart(), competitorChartsAnchor);
        DetailType selectedChartDetailType = oldCompetitorChartsComposite.getSelectedChartDetailType();
        eventSeriesAnalyticsManager.showCompetitorChart(selectedChartDetailType);
    }

    private void setActiveTabPanel(HTMLPanel newActivePanel, Widget newActiveContentWidget, Anchor newActiveAnchor) {
        if(activeTabPanel != null) {
            activeTabPanel.setVisible(false);
            activeAnchor.removeStyleName(SharedResources.INSTANCE.mainCss().navbar_buttonactive());
        }
        if(activeContentWidget != null) {
            activeContentWidget.setVisible(false);
        }
        
        activeTabPanel = newActivePanel;
        activeContentWidget = newActiveContentWidget;
        activeAnchor = newActiveAnchor;
        activeTabPanel.setVisible(true);
        activeContentWidget.setVisible(true);
        activeAnchor.addStyleName(SharedResources.INSTANCE.mainCss().navbar_buttonactive());
    }
}
