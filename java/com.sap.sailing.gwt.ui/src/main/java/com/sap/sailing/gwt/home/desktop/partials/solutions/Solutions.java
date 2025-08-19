package com.sap.sailing.gwt.home.desktop.partials.solutions;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class Solutions extends Composite {
    interface SolutionsUiBinder extends UiBinder<Widget, Solutions> {
    }
    
    private static SolutionsUiBinder uiBinder = GWT.create(SolutionsUiBinder.class);

    private static final HyperlinkImpl HYPERLINK_IMPL = GWT.create(HyperlinkImpl.class);

    @UiField StringMessages i18n;
    @UiField Anchor sapInSailingAnchor;
    @UiField Anchor sailingAnalyticsAnchor;
    @UiField Anchor raceAnchor;
    @UiField Anchor inSightAnchor;
    @UiField Anchor buoyPingerAnchor;
    @UiField Anchor simulatorAnchor;

    @UiField DivElement sapInSailingDiv;
    @UiField DivElement brandInSailingInnerDiv;
    @UiField DivElement sailingAnalyticsDiv;
    @UiField DivElement sailingAnalyticsDivGridAlternator;
    @UiField DivElement raceDiv;
    @UiField DivElement raceDivGridAlternator;
    @UiField DivElement inSightDiv;
    @UiField DivElement inSightDivGridAlternator;
    @UiField DivElement buoyPingerDiv;
    @UiField DivElement buoyPingerDivGridAlternator;
    @UiField DivElement simulatorDiv;
    @UiField DivElement simulatorDivGridAlternator;

    @UiField Anchor sailingAnalyticsDetailsAnchor;
    @UiField Anchor raceCommitteeAppDetailsAnchor;
    @UiField Anchor inSightAppDetailsAnchor;
    @UiField Anchor buoyPingerAppDetailsAnchor;
    @UiField Anchor simulatorAppDetailsAnchor;
    
    private final PlaceNavigation<SolutionsPlace> sapInSailingNavigation;
    private final PlaceNavigation<SolutionsPlace> sailingAnalyticsNavigation; 
    private final PlaceNavigation<SolutionsPlace> raceCommitteeAppNavigation; 
    private final PlaceNavigation<SolutionsPlace> inSightAppNavigation;
    private final PlaceNavigation<SolutionsPlace> buoyPingerAppNavigation;
    private final PlaceNavigation<SolutionsPlace> sailingSimulatorNavigation; 
    
    private final SolutionsNavigationTabs navigationTab;
    
    private final PlaceNavigation<WhatsNewPlace> sailingAnalyticsDetailsNavigation;
    private final PlaceNavigation<WhatsNewPlace> raceCommitteeAppDetailsNavigation;
    private final PlaceNavigation<WhatsNewPlace> buoyPingerAppDetailsNavigation;
    private final PlaceNavigation<WhatsNewPlace> simulatorAppDetailsNavigation;
    private final DesktopPlacesNavigator placesNavigator;
    
    public Solutions(SolutionsNavigationTabs navigationTab, DesktopPlacesNavigator placesNavigator) {
        this.navigationTab = navigationTab;
        this.placesNavigator = placesNavigator;
        SolutionsResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        sailingAnalyticsDivGridAlternator.getStyle().setBackgroundImage("url(\'"+ClientConfiguration.getInstance().getSailingAnalyticsImageURL()+"\')");
        brandInSailingInnerDiv.getStyle().setBackgroundImage("url(\'"+ClientConfiguration.getInstance().getSolutionsInSailingImageURL()+"\')");
        inSightDivGridAlternator.getStyle().setBackgroundImage("url(\'"+ClientConfiguration.getInstance().getSailInSightAppImageURL()+"\')");
        simulatorDivGridAlternator.getStyle().setBackgroundImage("url(\'"+ClientConfiguration.getInstance().getSailingSimulatorImageURL()+"\')");
        raceDivGridAlternator.getStyle().setBackgroundImage("url(\'"+ClientConfiguration.getInstance().getSailingRaceManagerAppImageURL()+"\')");
        buoyPingerDivGridAlternator.getStyle().setBackgroundImage("url(\'"+ClientConfiguration.getInstance().getBuoyPingerAppImageURL()+"\')");
        sailingAnalyticsDetailsAnchor.setText(ClientConfiguration.getInstance().getSailingAnalyticsReadMoreText(Optional.empty()));

        sailingAnalyticsDetailsNavigation = placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.SailingAnalytics);
        raceCommitteeAppDetailsNavigation =  placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.RaceManagerApp);
        buoyPingerAppDetailsNavigation =  placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.BuoyPingerApp);
        simulatorAppDetailsNavigation =  placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.SailingSimulator);
        
        sailingAnalyticsDetailsAnchor.setHref(sailingAnalyticsDetailsNavigation.getTargetUrl());
        raceCommitteeAppDetailsAnchor.setHref(raceCommitteeAppDetailsNavigation.getTargetUrl());
        inSightAppDetailsAnchor.setHref("https://sail-insight.com");
        buoyPingerAppDetailsAnchor.setHref(buoyPingerAppDetailsNavigation.getTargetUrl());
        simulatorAppDetailsAnchor.setHref(simulatorAppDetailsNavigation.getTargetUrl());

        sapInSailingNavigation = placesNavigator.getSolutionsNavigation(SolutionsNavigationTabs.SapInSailing);
        sailingAnalyticsNavigation = placesNavigator.getSolutionsNavigation(SolutionsNavigationTabs.SailingAnalytics);
        raceCommitteeAppNavigation = placesNavigator.getSolutionsNavigation(SolutionsNavigationTabs.RaceManagerApp);
        inSightAppNavigation = placesNavigator.getSolutionsNavigation(SolutionsNavigationTabs.InSightApp);
        buoyPingerAppNavigation = placesNavigator.getSolutionsNavigation(SolutionsNavigationTabs.BuoyPingerApp);
        sailingSimulatorNavigation = placesNavigator.getSolutionsNavigation(SolutionsNavigationTabs.SailingSimulator);

        sapInSailingAnchor.setHref(sapInSailingNavigation.getTargetUrl());
        sailingAnalyticsAnchor.setHref(sailingAnalyticsNavigation.getTargetUrl());
        raceAnchor.setHref(raceCommitteeAppNavigation.getTargetUrl());
        inSightAnchor.setHref(inSightAppNavigation.getTargetUrl());
        buoyPingerAnchor.setHref(buoyPingerAppNavigation.getTargetUrl());
        simulatorAnchor.setHref(sailingSimulatorNavigation.getTargetUrl());
    }

    @Override
    protected void onLoad() {
        Scheduler.get().scheduleDeferred(new Command() {
            public void execute() {
                scrollToView(navigationTab);
            }
        });
    }

    @UiHandler("sapInSailingAnchor")
    public void scrollTosapInSailingAnchor(ClickEvent e) {
        scrollToView(SolutionsNavigationTabs.SapInSailing);
        handleClickEventWithLocalNavigation(e, sapInSailingNavigation);
    }
    @UiHandler("sailingAnalyticsAnchor")
    public void scrollToSailingAnalytics(ClickEvent e) {
        scrollToView(SolutionsNavigationTabs.SailingAnalytics);
        handleClickEventWithLocalNavigation(e, sailingAnalyticsNavigation);
    }
    
    @UiHandler("raceAnchor")
    public void scrollToRace(ClickEvent e) {
        scrollToView(SolutionsNavigationTabs.RaceManagerApp);
        handleClickEventWithLocalNavigation(e, raceCommitteeAppNavigation);
    }

    @UiHandler("inSightAnchor")
    public void scrollToInSight(ClickEvent e) {
        scrollToView(SolutionsNavigationTabs.InSightApp);
        handleClickEventWithLocalNavigation(e, inSightAppNavigation);
    }

    @UiHandler("buoyPingerAnchor")
    public void scrollToBuoyPinger(ClickEvent e) {
        scrollToView(SolutionsNavigationTabs.BuoyPingerApp);
        handleClickEventWithLocalNavigation(e, buoyPingerAppNavigation);
    }

    @UiHandler("simulatorAnchor")
    public void scrollToSimulator(ClickEvent e) {
        scrollToView(SolutionsNavigationTabs.SailingSimulator);
        handleClickEventWithLocalNavigation(e, sailingSimulatorNavigation);
    }

    @UiHandler("sailingAnalyticsDetailsAnchor")
    public void sailingAnalyticsDetailsClicked(ClickEvent e) {
        handleClickEventWithPlaceController(e, sailingAnalyticsDetailsNavigation);
    }

    @UiHandler("simulatorAppDetailsAnchor")
    public void simulatorAppDetailsAnchorClicked(ClickEvent e) {
        handleClickEventWithPlaceController(e, simulatorAppDetailsNavigation);
    }

    private void scrollToView(SolutionsNavigationTabs navigationTab) {
        final int actualScrollLeft = Window.getScrollLeft();
        if (navigationTab == null) {
            sapInSailingDiv.scrollIntoView();
            Window.scrollTo(actualScrollLeft, 0);
        } else {
            switch (navigationTab) {
            case BuoyPingerApp:
                Window.scrollTo(actualScrollLeft, buoyPingerDiv.getAbsoluteTop());
                break;
            case InSightApp:
                Window.scrollTo(actualScrollLeft, inSightDiv.getAbsoluteTop());
                break;
            case SapInSailing:
                Window.scrollTo(actualScrollLeft, 0);
                break;
            case SailingAnalytics:
                Window.scrollTo(actualScrollLeft, sailingAnalyticsDiv.getAbsoluteTop());
                break;
            case RaceManagerApp:
                Window.scrollTo(actualScrollLeft, raceDiv.getAbsoluteTop());
                break;
            case SailingSimulator:
                Window.scrollTo(actualScrollLeft, simulatorDiv.getAbsoluteTop());
                break;
            }
        }
    }
    
    private void handleClickEventWithLocalNavigation(ClickEvent e, PlaceNavigation<?> placeNavigation) {
        if (HYPERLINK_IMPL.handleAsClick((Event) e.getNativeEvent())) {
            // don't use the placecontroller for navigation here as we want to avoid a page reload
            History.newItem(placeNavigation.getHistoryUrl(), false);
            e.preventDefault();
         }
    }
    
    private void handleClickEventWithPlaceController(ClickEvent e, PlaceNavigation<?> placeNavigation) {
        if (HYPERLINK_IMPL.handleAsClick((Event) e.getNativeEvent())) {
            placesNavigator.goToPlace(placeNavigation);
            e.preventDefault();
         }
    }
}
