package com.sap.sse.branding.shared;

import java.util.Optional;

/**
 * Describes all branding aspects that may appear in the UI (logo, titles, links, localized texts, etc.).
 * <p>
 * <strong>Locale Parameter Conventions</strong><br>
 * Several methods accept {@code Optional<String> locale}. Implementations SHOULD interpret this value as a
 * BCP-47 language tag (e.g., {@code "en"}, {@code "de-DE"}). If {@link Optional#empty()} is provided, an
 * implementation SHOULD return a sensible default (e.g., English or system default). Implementations MAY ignore
 * the locale for non-localized assets (like image URLs).
 */
public interface BrandingConfiguration {

    /**
     * The ID by which to find or set this configuration in {@code BrandingConfigurationService#setActiveBrandingConfigurationById(String)}
     * <p>Used by the system property {@code -Dcom.sap.sse.branding=<ID>} and as the OSGi service property
     * {@code com.sap.sse.branding} to select the active branding.
     *
     * @return non-null brand ID (e.g., {@code "SAP"} or {@code "YOUR_BRAND_NAME"}).
     */
    String getId();

    /**
     * Localized brand display title (e.g., {@code "SAP"}).
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return non-null (possibly empty) title.
     */
    String getBrandTitle(Optional<String> locale);

    /**
     * Main brand logo URL used in headers/favicons or other primary placements.
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return URL string (absolute or context-relative).
     */
    String getDefaultBrandingLogoURL(Optional<String> locale);

    /**
     * Grayscale/transparent logo URL for the Race Tracking window.
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return URL string. Empty to hide/fallback.
     */
    String getGreyTransparentLogoURL(Optional<String> locale);

    /**
     * “Solutions in Sailing” section image URL.
     *
     * @return URL string. Empty to hide.
     */
    String getSolutionsInSailingImageURL();

    /**
     * Trimmed variant of the “Solutions in Sailing” image URL.
     *
     * @return URL string. Empty to hide.
     */
    String getSoutionsInSailingTrimmedImageURL();

    /**
     * Trimmed image URL for the Sailing Race Manager app tile/card.
     *
     * @return URL string. Empty to hide.
     */
    String getSailingRaceManagerAppTrimmedImageURL();

    /**
     * Image URL for the Sail In Sight app tile/card.
     *
     * @return URL string. Empty to hide.
     */
    String getSailInSightAppImageURL();

    /**
     * Image URL for the Sailing Race Manager app tile/card.
     *
     * @return URL string. Empty to hide.
     */
    String getSailingRaceManagerAppImageURL();

    /**
     * Image URL for the Sailing Simulator app tile/card.
     *
     * @return URL string. Empty to hide.
     */
    String getSailingSimulatorImageURL();

    /**
     * Trimmed image URL for the Sailing Simulator tile/card.
     *
     * @return URL string. Empty to hide.
     */
    String getSailingSimulatorTrimmedImageURL();

    /**
     * Image URL for the Buoy Pinger app tile/card.
     *
     * @return URL string. Empty to hide.
     */
    String getBuoyPingerAppImageURL();

    /**
     * Image URL for the Sailing Analytics tile/card.
     *
     * @return URL string. Empty to hide.
     */
    String getSailingAnalyticsImageURL();

    /**
     * Localized “Read more” label for the Sailing Analytics tile/card.
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return localized text. Empty to hide.
     */
    String getSailingAnalyticsReadMoreText(Optional<String> locale);

    /**
     * Localized “Sailing” label combined with the brand (e.g., “SAP Sailing”).
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return localized text. May be empty.
     */
    String getSailingAnalyticsSailing(Optional<String> locale);

    /**
     * Footer copyright text.
     *
     * @return plain text. Empty to hide.
     */
    String getFooterCopyright();

    /**
     * Footer link to the brand’s privacy statement.
     *
     * @return absolute URL. Empty to hide.
     */
    String getFooterPrivacyLink();

    /**
     * Footer link to jobs/careers.
     *
     * @return absolute URL. Empty to hide.
     */
    String getFooterJobsLink();

    /**
     * Footer link to support/help.
     *
     * @return absolute URL. Empty to hide.
     */
    String getFooterSupportLink();

    /**
     * Localized footer label inviting users to explore sports content (e.g., “Sports on …”).
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return localized text. Empty to hide.
     */
    String getSportsOn(Optional<String> locale);

    /**
     * Localized footer label inviting users to follow the brand’s sports channels (e.g., “Follow … Sports”).
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return localized text. Empty to hide.
     */
    String getFollowSports(Optional<String> locale);

    /**
     * Brand’s Facebook page URL.
     *
     * @return absolute URL. Empty to hide the icon/link.
     */
    String getFacebookLink();

    /**
     * Brand’s X (formerly Twitter) profile URL.
     *
     * @return absolute URL. Empty to hide the icon/link.
     */
    String getxLink();

    /**
     * Brand’s Instagram profile URL.
     *
     * @return absolute URL. Empty to hide the icon/link.
     */
    String getInstagramLink();

    /**
     * Localized headline for the “Welcome to Sailing Analytics” widget.
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return localized text. Empty to hide the widget.
     */
    String getWelcomeToSailingAnalytics(Optional<String> locale);

    /**
     * Localized body text for the “Welcome to Sailing Analytics” widget.
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return localized text. Empty to hide the widget body.
     */
    String getWelcomeToSailingAnalyticsBody(Optional<String> locale);

    /**
     * Image URL for “More login information” → Notifications.
     * <p>Note: This dialog may require logging out to test locally.</p>
     *
     * @return URL string. Empty to hide.
     */
    String getMoreLoginInformationNotificationsURL();

    /**
     * Image URL for “More login information” → Settings.
     * <p>Note: This dialog may require logging out to test locally.</p>
     *
     * @return URL string. Empty to hide.
     */
    String getMoreLoginInformationSettingsURL();

    /**
     * Image URL for “More login information” → Sailor Profiles.
     * <p>Note: This dialog may require logging out to test locally.</p>
     *
     * @return URL string. Empty to hide.
     */
    String getMoreLoginInformationSailorProfilesURL();

    /**
     * Image URL for “More login information” → Simulator.
     * <p>Note: This dialog may require logging out to test locally.</p>
     *
     * @return URL string. Empty to hide.
     */
    String getMoreLoginInformationSimulatorURL();

    /**
     * Localized HTML/text for “{Brand} in Sailing” (Solutions tab). Implementations return sanitized HTML.
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return HTML or plain text. Empty to hide.
     */
    String getInSailingContent(Optional<String> locale);
    
    /**
     * Localized footer label inviting users to follow the brand’s GitHub Source Code.
     *
     * @param locale see “Locale Parameter Conventions”.
     * @return localized text. Empty to hide.
     */
    String getFollowGitHub(Optional<String> locale);
    
    /**
     * Brand’s GitHub URL.
     *
     * @return absolute URL. Empty to hide the icon/link.
     */
    String getGitHubLink();
}
