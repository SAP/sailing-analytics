package com.sap.sailing.gwt.regattaoverview.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface RegattaOverviewResources extends ClientBundle {
    public static final RegattaOverviewResources INSTANCE = GWT.create(RegattaOverviewResources.class);

    @Source("RegattaOverview.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String container();
        String titleLabel();
        String eventLabel();
        String venueLabel();
        String clockLabel();
        String refreshStopTime();
        String functionBar();
        String contentWrapper();
        String circle();
        String circleBlue();
        String circleYellow();
        String circleGreen();
        String circleGrey();
        String raceDetailPanel();
        String raceDetailPanel_header();
        String raceDetailPanel_content();
        String leaderboards();
        String viewerToolbar_innerElement();

        String settingsButton();

        String refreshButton();

        String refreshButton_live();

        String refreshButton_playing();
        String eventDescription();

        String button();

        String buttonLeaderboard();

        String functionBarLeft();

        String repeatedInfoLabel();
    }
}
