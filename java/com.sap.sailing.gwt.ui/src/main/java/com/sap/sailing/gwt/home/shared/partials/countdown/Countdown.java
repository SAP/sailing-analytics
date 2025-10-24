package com.sap.sailing.gwt.home.shared.partials.countdown;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.common.client.SharedResources.MainCss;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewRaceTickerStageDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewRegattaTickerStageDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewTickerStageDTO;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.countdown.CountdownResources.LocalCss;
import com.sap.sailing.gwt.home.shared.partials.countdowntimer.CountdownTimer;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.LinkUtil;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.security.ui.client.UserService;

public class Countdown extends Composite {

    private static final StringMessages I18N = StringMessages.INSTANCE;
    private static final LocalCss CSS = CountdownResources.INSTANCE.css();
    private static final MainCss MAIN_CSS = SharedResources.INSTANCE.mainCss();

    private static CountdownUiBinder uiBinder = GWT.create(CountdownUiBinder.class);

    interface CountdownUiBinder extends UiBinder<Widget, Countdown> {
    }

    @UiField SimplePanel tickerContainer;
    @UiField HeadingElement countdownTitle;
    @UiField DivElement countdownInfo;
    @UiField HeadingElement infoTitle;
    @UiField(provided = true) NavigationAnchor navigationButton;
    @UiField DivElement image;
    @UiField(provided = true) MediaMenuIcon imageMenuButton;
    
    public Countdown(CountdownNavigationProvider navigationProvider, UserService takedownNoticeService) {
        CSS.ensureInjected();
        this.navigationButton = new NavigationAnchor(navigationProvider);
        this.imageMenuButton = new MediaMenuIcon(takedownNoticeService, "takedownRequestForImageOnEventStage");
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    public void setData(EventOverviewTickerStageDTO data) {
        final String stageImageUrl;
        if (data.getStageImageUrl() != null) {
            stageImageUrl = data.getStageImageUrl();
        } else {
            stageImageUrl = SharedHomeResources.INSTANCE.defaultStageEventTeaserImage().getSafeUri().asString();
        }
        image.getStyle().setBackgroundImage("url(\"" + stageImageUrl + "\")");
        navigationButton.removeStyleName(MAIN_CSS.buttonred());
        navigationButton.removeStyleName(MAIN_CSS.buttonprimary());
        if (data instanceof EventOverviewRaceTickerStageDTO) {
            this.updateUi(I18N.nextRaceStartingIn(), data.getTickerInfo());
            this.navigationButton.linkToRaceViewer((EventOverviewRaceTickerStageDTO) data);
        } else if (data instanceof EventOverviewRegattaTickerStageDTO) {
            if (data.getStartTime() != null) {
                this.updateUi(I18N.startingIn(data.getTickerInfo()), data.getTickerInfo());
            }
            this.navigationButton.linkToRegatta((EventOverviewRegattaTickerStageDTO) data);
        } else {
            this.updateUi(data.getTickerInfo() != null && data.getStartTime() != null ? I18N.startingIn(data.getTickerInfo()) : null, null);
        }
        if (data.getStartTime() != null) {
            this.tickerContainer.setWidget(new CountdownTimer(data.getStartTime(), true));
        } else {
            this.tickerContainer.setWidget(null);
        }
        imageMenuButton.setData(data.getTickerInfo(), stageImageUrl);
    }

    private void updateUi(String title, String info) {
        this.countdownTitle.setInnerText(title);
        this.infoTitle.setInnerText(info);
        this.countdownInfo.getStyle().setDisplay(info != null ? Display.BLOCK : Display.NONE);
    }

    @UiHandler("navigationButton")
    void handleNavigationButtonClick(ClickEvent event) {
        this.navigationButton.onClick(event);
    }

    private class NavigationAnchor extends Anchor {
        private final CountdownNavigationProvider navigationProvider;
        private PlaceNavigation<?> currentPlaceNavigation;

        private NavigationAnchor(CountdownNavigationProvider navigationProvider) {
            this.navigationProvider = navigationProvider;
        }

        private void linkToRaceViewer(EventOverviewRaceTickerStageDTO data) {
            String url = ""; // TODO implement correctly
            // String url = navigationProvider.getRaceViewerURL(data.getRegattaAndRaceIdentifier());
            this.update(MAIN_CSS.buttonprimary(), MAIN_CSS.buttonred(), I18N.watchNow(), null, url);
        }

        private void linkToRegatta(EventOverviewRegattaTickerStageDTO data) {
            PlaceNavigation<?> nav = navigationProvider.getRegattaNavigation(data.getRegattaIdentifier().getRegattaName());
            if (nav != null) {
                this.update(MAIN_CSS.buttonred(), MAIN_CSS.buttonprimary(), I18N.regattaDetails(), nav, nav.getTargetUrl());
            }
        }
        
        private void update(String removeStyle, String addStyle, String text, PlaceNavigation<?> navigation, String url) {
            this.removeStyleName(removeStyle);
            this.addStyleName(addStyle);
            this.setText(text);
            this.currentPlaceNavigation = navigation;
            this.setHref(url);
        }

        private void onClick(ClickEvent event) {
            if (LinkUtil.handleLinkClick(event.getNativeEvent().<Event> cast())) {
                if (currentPlaceNavigation != null) {
                    event.preventDefault();
                    currentPlaceNavigation.goToPlace();
                }
            }
        }
    }
    
    public interface CountdownNavigationProvider {
        PlaceNavigation<?> getRegattaNavigation(String regattaName);
        // String getRaceViewerURL(RegattaAndRaceIdentifier raceIdentifier);
    }
}
