package com.sap.sse.gwt.client.context.data;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Access custom information for GWT client from static browser page.
 * 
 * @see com.sap.sse.gwt.client.context.impl.ClientConfigurationContextDataFactoryImpl
 * @see com.sap.sse.gwt.shared.ClientConfiguration
 * @author Georg Herdt
 *
 */
public class ClientConfigurationContextDataJSO extends JavaScriptObject {
    protected ClientConfigurationContextDataJSO() {
    }

    public final native boolean isDebrandingActive() /*-{
        return this.debrandingActive;
    }-*/;

    public final native String getId() /*-{
        return this.id;
    }-*/;

    public final native String getBrandTitle() /*-{
        return this.brandTitle;
    }-*/;

    public final native String getDefaultBrandingLogoURL() /*-{
        return this.defaultBrandingLogoURL;
    }-*/;

    public final native String getGreyTransparentLogoURL() /*-{
        return this.greyTransparentLogoURL;
    }-*/;

    public final native String getSoutionsInSailingImageURL() /*-{
        return this.solutionsInSailingImageURL;
    }-*/;

    public final native String getSolutionsInSailingTrimmedImageURL() /*-{
        return this.solutionsInSailingTrimmedImageURL;
    }-*/;

    public final native String getSailingRaceManagerAppTrimmedImageURL() /*-{
        return this.sailingRaceManagerAppTrimmedImageURL;
    }-*/;

    public final native String getSailingSimulatorTrimmedImageURL() /*-{
        return this.sailingSimulatorTrimmedImageURL;
    }-*/;

    public final native String getSailInSightAppImageURL() /*-{
        return this.sailInSightAppImageURL;
    }-*/;

    public final native String getSailingRaceManagerAppImageURL() /*-{
        return this.sailingRaceManagerAppImageURL;
    }-*/;

    public final native String getSailingSimulatorImageURL() /*-{
        return this.sailingSimulatorImageURL;
    }-*/;

    public final native String getBuoyPingerAppImageURL() /*-{
        return this.buoyPingerAppImageURL;
    }-*/;

    public final native String getSailingAnalyticsImageURL() /*-{
        return this.sailingAnalyticsImageURL;
    }-*/;

    public final native String getSailingAnalyticsReadMoreText() /*-{
        return this.sailingAnalyticsReadMoreText;
    }-*/;
    public final native String getSailingAnalyticsSailing() /*-{
        return this.sailingAnalyticsSailing;
    }-*/;
    public final native String getFooterCopyright() /*-{
        return this.footerCopyright;
    }-*/;
    public final native String getFooterPrivacyLink() /*-{
        return this.footerPrivacyLink;
    }-*/;
    public final native String getFooterJobsLink() /*-{
        return this.footerJobsLink;
    }-*/;
    public final native String getFooterSupportLink() /*-{
        return this.footerSupportLink;
    }-*/;
    public final native String getSportsOn() /*-{
        return this.sportsOn;
    }-*/;
    public final native String getFollowSports() /*-{
        return this.followSports;
    }-*/;
    public final native String getFacebookLink() /*-{
        return this.facebookLink;
    }-*/;
    public final native String getxLink() /*-{
        return this.xLink;
    }-*/;
    public final native String getInstagramLink() /*-{
        return this.instagramLink;
    }-*/;
    public final native String getWelcomeToSailingAnalytics()/*-{
        return this.welcomeToSailingAnalytics;
    }-*/;
    public final native String getWelcomeToSailingAnalyticsBody()/*-{
        return this.welcomeToSailingAnalyticsBody;
    }-*/;
    public final native String getMoreLoginInformationNotificationsURL() /*-{
        return this.moreLoginInformationNotificationsURL;
    }-*/;
    public final native String getMoreLoginInformationSettingsURL() /*-{
        return this.moreLoginInformationSettingsURL;
    }-*/;
    public final native String getMoreLoginInformationSailorProfilesURL() /*-{
        return this.moreLoginInformationSailorProfilesURL;
    }-*/;
    public final native String getMoreLoginInformationSimulatorURL() /*-{
        return this.moreLoginInformationSimulatorURL;
    }-*/;
    public final native String getFollowGitHub() /*-{
        return this.followGitHub;
    }-*/;
    public final native String getGitHubLink() /*-{
        return this.gitHubLink;
    }-*/;
}