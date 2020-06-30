package com.sap.sailing.gwt.ui.spectator;

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.settings.client.spectator.SpectatorContextDefinition;
import com.sap.sailing.gwt.settings.client.spectator.SpectatorSettings;
import com.sap.sailing.gwt.ui.client.AbstractSailingReadEntryPoint;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.shared.panels.SimpleWelcomeWidget;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;
import com.sap.sse.gwt.shared.ClientConfiguration;

/**
 * 
 * @author Lennart Hensler (D054527)
 *
 */
public class SpectatorEntryPoint extends AbstractSailingReadEntryPoint implements RegattaRefresher {

    @Override
    protected void doOnModuleLoad() {
        super.doOnModuleLoad();

        final String groupNameParam = new SettingsToUrlSerializer()
                .deserializeFromCurrentLocation(new SpectatorContextDefinition()).getLeaderboardGroupName();
        final String groupIdParam = new SettingsToUrlSerializer()
                .deserializeFromCurrentLocation(new SpectatorContextDefinition()).getLeaderboardGroupId();

        final SpectatorSettings settings = new SettingsToUrlSerializer()
                .deserializeFromCurrentLocation(new SpectatorSettings());
        RootPanel rootPanel = RootPanel.get();
        FlowPanel groupAndFeedbackPanel = new FlowPanel();
        boolean embedded = settings.isEmbedded();
        if (groupIdParam == null && groupNameParam == null) {
            FlowPanel groupOverviewPanel = new FlowPanel();
            groupOverviewPanel.addStyleName("contentOuterPanel");
            // DON'T DELETE -> the EventOverviewPanel will replace the LeaderboardGroupOverviewPanel later on
            // EventOverviewPanel eventOverviewPanel = new EventOverviewPanel(sailingServiceWrite, this, stringMessages,
            // showRaceDetails);
            // groupOverviewPanel.add( eventOverviewPanel);
            LeaderboardGroupOverviewPanel leaderboardGroupOverviewPanel = new LeaderboardGroupOverviewPanel(
                    getSailingService(), this, getStringMessages(), settings.isShowRaceDetails());
            groupOverviewPanel.add(leaderboardGroupOverviewPanel);
            setHeader(null, embedded);
            rootPanel.add(groupOverviewPanel);
        } else {
            LeaderboardGroupPanel groupPanel = new LeaderboardGroupPanel(getSailingService(), getStringMessages(), this,
                    groupIdParam, groupNameParam, this::setHeader, settings.getViewMode(), embedded,
                    settings.isShowRaceDetails(), settings.isCanReplayDuringLiveRaces(), settings.isShowMapControls());
            groupAndFeedbackPanel.add(groupPanel);
            if (!embedded) {
                groupPanel.setWelcomeWidget(new SimpleWelcomeWidget(getStringMessages().welcomeToSailingAnalytics(),
                        getStringMessages().welcomeToSailingAnalyticsBody()));
                if (ClientConfiguration.getInstance().isBrandingActive()) {
                    SimplePanel feedbackPanel = new SimplePanel();
                    feedbackPanel.getElement().getStyle().setProperty("clear", "right");
                    feedbackPanel.addStyleName("feedbackPanel");
                    Anchor feedbackLink = new Anchor(new SafeHtmlBuilder()
                            .appendHtmlConstant("<img src=\"/gwt/images/feedbackPanel-bg.png\"/>").toSafeHtml());
                    // TODO set image
                    feedbackLink.setHref("mailto:sailing_analytics%40sap.com?subject=[SAP Sailing] Feedback");
                    feedbackPanel.add(feedbackLink);
                    groupAndFeedbackPanel.add(feedbackPanel);
                }
            }
            rootPanel.add(groupAndFeedbackPanel);
        }
        fillRegattas();
    }

    private void setHeader(final String groupNameParam, final boolean embedded) {
        if (!embedded) {
            String title = groupNameParam != null ? groupNameParam : getStringMessages().overview();
            Window.setTitle(title);
            SAPSailingHeaderWithAuthentication header = getHeader(title);
            RootPanel.get().add(header);
        } else {
            RootPanel.getBodyElement().getStyle().setPadding(0, Unit.PX);
            RootPanel.getBodyElement().getStyle().setPaddingTop(20, Unit.PX);
        }
    }

    private SAPSailingHeaderWithAuthentication getHeader(String title) {
        SAPSailingHeaderWithAuthentication header = new SAPSailingHeaderWithAuthentication(title);
        new FixedSailingAuthentication(getUserService(), header.getAuthenticationMenuView());
        header.getElement().getStyle().setPosition(Position.FIXED);
        header.getElement().getStyle().setTop(0, Unit.PX);
        header.getElement().getStyle().setWidth(100, Unit.PCT);
        return header;
    }

    @Override
    public void fillRegattas() {
        
    }
    
}
