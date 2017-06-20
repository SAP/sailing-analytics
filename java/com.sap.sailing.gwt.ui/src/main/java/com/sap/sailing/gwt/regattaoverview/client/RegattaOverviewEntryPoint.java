package com.sap.sailing.gwt.regattaoverview.client;

import java.util.UUID;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.settings.client.regattaoverview.RegattaOverviewContextDefinition;
import com.sap.sailing.gwt.settings.client.regattaoverview.RegattaRaceStatesSettings;
import com.sap.sailing.gwt.ui.client.AbstractSailingEntryPoint;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;

public class RegattaOverviewEntryPoint extends AbstractSailingEntryPoint  {

    private static final SettingsToUrlSerializer serializer = new SettingsToUrlSerializer();
    
    private DockLayoutPanel containerPanel;
    private RaceDetailPanel detailPanel;
    private RegattaOverviewPanel regattaPanel;
    private final Label clockLabel = new Label();

    private final RegattaOverviewResources.LocalCss style = RegattaOverviewResources.INSTANCE.css();
    private SAPSailingHeaderWithAuthentication siteHeader;

    @Override
    public void doOnModuleLoad() {
        super.doOnModuleLoad();
        
        SharedResources.INSTANCE.mediaCss().ensureInjected();
        SharedResources.INSTANCE.mainCss().ensureInjected();
        RegattaOverviewResources.INSTANCE.css().ensureInjected();

        RootLayoutPanel rootPanel = RootLayoutPanel.get();
        containerPanel = new DockLayoutPanel(Unit.PX);
        rootPanel.add(containerPanel);
        containerPanel.addStyleName(RegattaOverviewResources.INSTANCE.css().container());
        
        clockLabel.addStyleName(style.clockLabel());

        siteHeader = new SAPSailingHeaderWithAuthentication();
        new FixedSailingAuthentication(getUserService(), siteHeader.getAuthenticationMenuView());

        siteHeader.addWidgetToRightSide(clockLabel);
        containerPanel.addNorth(siteHeader, 75);
        
        RegattaOverviewContextDefinition regattaOverviewContextDefinition = serializer
                .deserializeFromCurrentLocation(new RegattaOverviewContextDefinition());

        if (regattaOverviewContextDefinition.getEvent() == null) {
            Window.alert("Missing parameter");
            return;
        }

        createAndAddDetailPanel();
        createAndAddRegattaPanel(regattaOverviewContextDefinition);
        toggleDetailPanel(false);
        
    }
    
    private void toggleDetailPanel(boolean visibile) {
        containerPanel.setWidgetSize(detailPanel, visibile ? 125 : 0);
        containerPanel.animate(500);
    }

    private void createAndAddRegattaPanel(RegattaOverviewContextDefinition regattaOverviewContextDefinition) {
        regattaPanel = new RegattaOverviewPanel(sailingService, getUserService(), this, getStringMessages(),
                regattaOverviewContextDefinition);

        regattaPanel.addHandler(new EventDTOLoadedEvent.Handler() {
            @Override
            public void onEventDTOLoaded(EventDTOLoadedEvent e) {
                siteHeader.setHeaderTitle(e.getCurrentEvent().getName());
                siteHeader.setHeaderSubTitle(e.getCurrentEvent().venue.getName());
            }
        }, EventDTOLoadedEvent.TYPE);

        regattaPanel.addHandler(new EventTimeUpdateEvent.Handler() {
            @Override
            public void onEventDTOLoaded(EventTimeUpdateEvent e) {
                clockLabel.setText(e.getFormattedUpdatedTime());
            }
        }, EventTimeUpdateEvent.TYPE);

        Panel centerPanel = new FlowPanel();
        centerPanel.add(regattaPanel);
        ScrollPanel scrollPanel = new ScrollPanel(centerPanel);
        containerPanel.add(scrollPanel);
    }

    private void createAndAddDetailPanel() {
        detailPanel = new RaceDetailPanel(getStringMessages(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleDetailPanel(false);
            }
        });
        containerPanel.addSouth(detailPanel, 110);
    }

    public static String getUrl(UUID eventId, RegattaRaceStatesSettings settings) {
        UrlBuilder urlBuilder = serializer.serializeUrlBuilderBasedOnCurrentLocationWithCleanParameters(settings);
        serializer.serializeToUrlBuilder(new RegattaOverviewContextDefinition(eventId), urlBuilder);
        return urlBuilder.buildString();
    }
}