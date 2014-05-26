package com.sap.sailing.gwt.ui.spectator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.sap.sailing.gwt.ui.client.AbstractEntryPoint;
import com.sap.sailing.gwt.ui.client.GlobalNavigationPanel;
import com.sap.sailing.gwt.ui.client.LogoAndTitlePanel;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.RemoteServiceMappingConstants;
import com.sap.sailing.gwt.ui.client.SailingService;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.shared.panels.SimpleWelcomeWidget;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardViewConfiguration;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.shared.GwtHttpRequestUtils;

/**
 * 
 * @author Lennart Hensler (D054527)
 *
 */
public class SpectatorEntryPoint extends AbstractEntryPoint implements RegattaRefresher {
    private final SailingServiceAsync sailingService = GWT.create(SailingService.class);
    
    @Override
    protected void doOnModuleLoad() {
        super.doOnModuleLoad();

        EntryPointHelper.registerASyncService((ServiceDefTarget) sailingService, RemoteServiceMappingConstants.sailingServiceRemotePath);

        String groupParamValue = Window.Location.getParameter("leaderboardGroupName");
        String viewModeParamValue = Window.Location.getParameter("viewMode");
        final boolean canReplayDuringLiveRaces = GwtHttpRequestUtils.getBooleanParameter(
                RaceBoardViewConfiguration.PARAM_CAN_REPLAY_DURING_LIVE_RACES, /* defaultValue */ false);
        boolean showRaceDetails = Window.Location.getParameter("showRaceDetails") != null
                && Window.Location.getParameter("showRaceDetails").equalsIgnoreCase("true");
        final String groupName;
        if (groupParamValue == null || groupParamValue.isEmpty()) {
            groupName = null;
        } else {
            groupName = groupParamValue;
            Window.setTitle(groupName);
            sailingService.getLeaderboardGroupByName(groupName, false /*withGeoLocationData*/, new AsyncCallback<LeaderboardGroupDTO>() {
                @Override
                public void onFailure(Throwable t) {
                    reportError(stringMessages.noLeaderboardGroupWithNameFound(groupName));
                }
                @Override
                public void onSuccess(LeaderboardGroupDTO group) {                }
            });
        }
        String root = Window.Location.getParameter("root");
        //Check if the root contains an allowed value
        if (root != null) {
            root = (root.equals("leaderboardGroupPanel") || root.equals("overview")) ? root : null;
        }
        
        RootPanel rootPanel = RootPanel.get();
        FlowPanel groupAndFeedbackPanel = new FlowPanel();
        boolean embedded = Window.Location.getParameter("embedded") != null
                && Window.Location.getParameter("embedded").equalsIgnoreCase("true");
        if (!embedded) {
            String title = groupName != null ? groupName : stringMessages.overview();
            LogoAndTitlePanel logoAndTitlePanel = new LogoAndTitlePanel(title, stringMessages, this);
            logoAndTitlePanel.addStyleName("LogoAndTitlePanel");
            FlowPanel globalNavigationPanel = new GlobalNavigationPanel(stringMessages, true, null, null);
            logoAndTitlePanel.add(globalNavigationPanel);

            rootPanel.add(logoAndTitlePanel);
        } else {
            RootPanel.getBodyElement().getStyle().setPadding(0, Unit.PX);
            RootPanel.getBodyElement().getStyle().setPaddingTop(20, Unit.PX);
        }
        if (groupName == null) {
            FlowPanel groupOverviewPanel = new FlowPanel();
            groupOverviewPanel.addStyleName("contentOuterPanel");
            // DON'T DELETE -> the EventOverviewPanel will replace the LeaderboardGroupOverviewPanel later on
//            EventOverviewPanel eventOverviewPanel = new EventOverviewPanel(sailingService, this, stringMessages, showRaceDetails);
//            groupOverviewPanel.add( eventOverviewPanel);
            LeaderboardGroupOverviewPanel leaderboardGroupOverviewPanel = new LeaderboardGroupOverviewPanel(sailingService, this, stringMessages, showRaceDetails);
            groupOverviewPanel.add(leaderboardGroupOverviewPanel);
            rootPanel.add(groupOverviewPanel);
        } else {
            LeaderboardGroupPanel groupPanel = new LeaderboardGroupPanel(sailingService, stringMessages, this,
                    groupName, root, viewModeParamValue, embedded, showRaceDetails, canReplayDuringLiveRaces);
            groupAndFeedbackPanel.add(groupPanel);
            if (!embedded) {
                groupPanel.setWelcomeWidget(new SimpleWelcomeWidget(stringMessages.welcomeToSailingAnalytics(),
                        stringMessages.welcomeToSailingAnalyticsBody()));
                SimplePanel feedbackPanel = new SimplePanel();
                feedbackPanel.getElement().getStyle().setProperty("clear", "right");
                feedbackPanel.addStyleName("feedbackPanel");
                Anchor feedbackLink = new Anchor(new SafeHtmlBuilder().appendHtmlConstant(
                        "<img class=\"linkNoBorder\" src=\"/gwt/images/feedbackPanel-bg.png\"/>").toSafeHtml());// TODO set image
                feedbackLink.setHref("mailto:sailing_analytics%40sap.com?subject=[SAP Sailing] Feedback");
                feedbackLink.addStyleName("feedbackLink");
                feedbackPanel.add(feedbackLink);
                groupAndFeedbackPanel.add(feedbackPanel);
            }
            rootPanel.add(groupAndFeedbackPanel);
        }
        fillRegattas();
    }

    @Override
    public void fillRegattas() {
        
    }
    
}
