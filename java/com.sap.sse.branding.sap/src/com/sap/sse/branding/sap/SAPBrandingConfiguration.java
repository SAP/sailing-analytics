package com.sap.sse.branding.sap;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

import com.sap.sse.branding.shared.BrandingConfiguration;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class SAPBrandingConfiguration implements BrandingConfiguration {
    public static final String ID = "SAP";
    private String defaultBrandingLogoURL;
    private String greyTransparentLogoURL;
    private final ResourceBundleStringMessages sailingServerStringMessages;
    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/SAPBrandingStringMessages";
    
    /**
     * The following path consists of the "Web-ContextPath" from the bundle's MANIFEST.MF, followed by the
     * images folder name and hence constitutes the URL path to which a "/" and the image file name has to
     * be appended to obtain a URL path to an image.
     */
    private static final String IMAGES_ROOT = "/sap-branding/images";

    public SAPBrandingConfiguration() {
        sailingServerStringMessages = ResourceBundleStringMessages.create(STRING_MESSAGES_BASE_NAME, getClass().getClassLoader(),
                StandardCharsets.UTF_8.name());
    }
    
    private static String image(String fileName) {
        return IMAGES_ROOT + "/" + fileName;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDefaultBrandingLogoURL(Optional<String> locale) {
        return defaultBrandingLogoURL;
    }

    @Override
    public String getGreyTransparentLogoURL(Optional<String> locale) {
        return greyTransparentLogoURL;
    }

    @Override
    public String getBrandTitle(Optional<String> locale) {
        return "SAP";
    }
    
    @Override
    public String getSolutionsInSailingImageURL() {
        return image("solutions-sap-in-sailing.jpg");
    }

    @Override
    public String getSoutionsInSailingTrimmedImageURL() {
        return image("solutions-sap-trimmed.png");
    }

    @Override
    public String getSailingRaceManagerAppImageURL() {
        return image("solutions-sap-sailing-race-manager.png");
    }

    @Override
    public String getSailingRaceManagerAppTrimmedImageURL() {
        return image("solutions-race.png");
    }

    @Override
    public String getSailInSightAppImageURL() {
        return image("solutions-sap-sailing-insight.png");
    }
    
    @Override
    public String getSailingSimulatorImageURL() {
        return image("solutions-simulator.png");
    }

    @Override
    public String getSailingSimulatorTrimmedImageURL() {
        return image("solutions-simulator-trimmed.png");
    }

    @Override
    public String getBuoyPingerAppImageURL() {
        return image("solutions-sap-sailing-buoy-pinger.png");
    }

    @Override
    public String getSailingAnalyticsImageURL() {
        return image("solutions-sap.png");
    }

    @Override
    public String getSailingAnalyticsReadMoreText(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "sailingAnalyticsReadMore");
    }
    
    @Override
    public String getSailingAnalyticsSailing(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "sailingAnalyticsSailing");
    }

    @Override
    public String getFooterCopyright() {
        return "@ 2011-2025 SAP Sailing Analytics";
    }

    @Override
    public String getFooterPrivacyLink() {
        return "https://www.sap.com/about/legal/privacy.html?campaigncode=CRM-XH21-OSP-Sailing";
    }

    @Override
    public String getFooterJobsLink() {
        return "https://jobs.sapsailing.com";
    }

    @Override
    public String getFooterSupportLink() {
        return "https://support.sapsailing.com";
    }

    @Override
    public String getSportsOn(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "sportsOn");
    }

    @Override
    public String getFollowSports(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "followSports");
    }

    @Override
    public String getFacebookLink() {
        return "https://www.facebook.com/SAP";
    }

    @Override
    public String getxLink() {
        return "https://x.com/sap";
    }

    @Override
    public String getInstagramLink() {
        return "https://www.instagram.com/sap/";
    }
    @Override
    public String getWelcomeToSailingAnalytics(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "welcomeToSailingAnalytics");
    }

    @Override
    public String getWelcomeToSailingAnalyticsBody(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l->Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "welcomeToSailingAnalyticsBody");
    }
    
    @Override
    public String getMoreLoginInformationNotificationsURL() {
        return image("notifications.png");
    }
    
    @Override
    public String getMoreLoginInformationSettingsURL() {
        return image("settings.png");
    }
    
    @Override
    public String getMoreLoginInformationSailorProfilesURL() {
        return image("sailorprofiles.png");
    }
    
    @Override
    public String getMoreLoginInformationSimulatorURL() {
        return image("simulator.png");
    }
    @Override
    public String getInSailingContent(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l -> Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "inSailingContent");
    }
    @Override
    public String getFollowGitHub(Optional<String> locale) {
        return sailingServerStringMessages.get(locale.map(l -> Locale.forLanguageTag(l)).orElse(Locale.ENGLISH), "followGitHub");
    }
    @Override
    public String getGitHubLink() {
        return "https://github.com/SAP/sailing-analytics";
    }
}
