package com.sap.sailing.gwt.home.mobile.partials.solutions;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class Solutions extends Composite {

    interface MyUiBinder extends UiBinder<Widget, Solutions> {
    }
    
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField StringMessages i18n;

    @UiField SolutionsItem brandInSailingItem;
    @UiField SolutionsItem sailingAnalyticsItem;
    @UiField SolutionsItem sailingRaceManager;
    @UiField SolutionsItem sapSailInSight;
    @UiField SolutionsItem sailingBuoyPinger;
    @UiField SolutionsItem strategySimulator;
    @UiField AnchorElement sailingAnalyticsDetailsAnchor;
    @UiField AnchorElement raceManagerAppDetailsAnchor;
    @UiField AnchorElement sailInSightAppDetailsAnchor;
    @UiField AnchorElement buoyPingerAppDetailsAnchor;
    @UiField AnchorElement simulatorAppDetailsAnchor;
    
    public Solutions(MobilePlacesNavigator placesNavigator) {
        initWidget(uiBinder.createAndBindUi(this));
        brandInSailingItem.setImageUrl(ClientConfiguration.getInstance().getSolutionsInSailingImageURL());
        sailingAnalyticsItem.setImageUrl(ClientConfiguration.getInstance().getSoutionsInSailingTrimmedImageURL());
        sailingRaceManager.setImageUrl(ClientConfiguration.getInstance().getSailingRaceManagerAppTrimmedImageURL());
        sapSailInSight.setImageUrl(ClientConfiguration.getInstance().getSailInSightAppImageURL());
        sailingBuoyPinger.setImageUrl(ClientConfiguration.getInstance().getBuoyPingerAppImageURL());
        strategySimulator.setImageUrl(ClientConfiguration.getInstance().getSailingSimulatorTrimmedImageURL());
        sailingAnalyticsDetailsAnchor.setInnerText(ClientConfiguration.getInstance().getSailingAnalyticsReadMoreText(Optional.empty()));
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.SailingAnalytics, sailingAnalyticsDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.RaceManagerApp, raceManagerAppDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.InSightApp, sailInSightAppDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.BuoyPingerApp, buoyPingerAppDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.SailingSimulator, simulatorAppDetailsAnchor);
    }
    
    private void initWhatsNewLink(MobilePlacesNavigator placesNavigator, WhatsNewNavigationTabs tab, AnchorElement anchor) {
        placesNavigator.getWhatsNewNavigation(tab).configureAnchorElement(anchor);
    }

}
