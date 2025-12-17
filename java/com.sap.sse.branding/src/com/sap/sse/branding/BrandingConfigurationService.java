package com.sap.sse.branding;

import java.util.Map;
import java.util.Optional;

import com.sap.sse.branding.shared.BrandingConfiguration;

/**
 * Describes all the aspects of branding as it may show in various places, specifically in the UI.
 * This includes whether branding is active, but also various logos to be displayed in different contexts,
 * as well as links to imprints and privacy statements.<p>
 * 
 * This is a service interface that can be implemented by different OSGi bundles, allowing for different
 * branding configurations. They may even be deployed at the same time and switched at runtime. If no branding
 * configuration is found or selected, no reference to any branding will be made ("whitelabeled" mode).<p>
 * 
 * For web pages to react to the branding configuration, JSP (Java Server Pages) can be used. Use {@link ClienctConfigurationListener}
 * in your web bundle's {@code web.xml} file as follows to get branding properties mapped into the JSP context:
 * <pre>
 *     &lt;listener&gt;
 *        &lt;listener-class>com.sap.sse.branding.ClientConfigurationListener&lt;/listener-class&gt;
 *     &lt;/listener&gt;
 * </pre>
 * Needless to say that your web bundle then must import the {@code com.sap.sse.branding} package.<p>
 * 
 * 
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface BrandingConfigurationService {
    /**
     * Branding bundles have to specify an attribute with this name in their service registration for the
     * {@link BrandingConfigurationService} interface in the OSGi service registry. A branding implementation can then
     * be looked by by filtering for {@code (com.sap.sse.branding=SAP)} (assuming the value of this constant is
     * {@code "com.sap.sse.branding"} and you're looking for a branding implementation that identifies itself as "SAP").<p>
     * 
     * At the same time, this is the name of the system property that can be used to set a start-up branding
     * configuration for the local instance.
     */
    String BRANDING_ID_PROPERTY_NAME = "com.sap.sse.branding";
    
    /**
     * This is the name of the property holding the map of JSP properties that are set by the {@link ClientConfigurationListener}. The keys of
     * the map found under this property name are defined, e.g., by {@link BrandingConfigurationProperty#BRAND_TITLE_WITH_TRAILING_SPACE_JSP_PROPERTY_NAME} or
     * {@link BrandingConfigurationProperty#BRANDING_ACTIVE_JSP_PROPERTY_NAME} and their respective {@link BrandingConfigurationProperty#getPropertyName()}.
     */
    String JSP_PROPERTY_NAME_PREFIX = "clientConfigurationContext";
    
    public static enum BrandingConfigurationProperty {
        /**
         * The name of the JSP property that contains the brand title with a trailing space, so it can be concatenated
         * with, e.g., the string "Sailing Analytics" to produce "{Your Brand Name} Sailing Analytics".
         */
        BRAND_TITLE_WITH_TRAILING_SPACE_JSP_PROPERTY_NAME("brandTitle"),
        
        /**
         * The name of the JSP property, that indicates whether debranding/whitelabeling is active. If this is
         * {@code "true"}, the brand title will be empty, the property identified by
         * {@link #DASH_WHITELABELED_JSP_PROPERTY_NAME} will be {@code "-whitelabeled"}, and the property identified by
         * {@link #BRANDING_ACTIVE_JSP_PROPERTY_NAME} will be {@code "false"}. If this is {@code "false"}, the brand
         * title will be filled, the property identified by {@link #DASH_WHITELABELED_JSP_PROPERTY_NAME} will be empty,
         * and the property identified by {@link #BRANDING_ACTIVE_JSP_PROPERTY_NAME} will be {@code "true"}.
         */
        DEBRANDING_ACTIVE_JSP_PROPERTY_NAME("debrandingActive"),
        
        /**
         * Opposite of {@link #DEBRANDING_ACTIVE_JSP_PROPERTY_NAME}.
         */
        BRANDING_ACTIVE_JSP_PROPERTY_NAME("brandingActive"),
        
        /**
         * The name of the JSP property, whose value is either empty (if branding is not active) or
         * {@code "-whitelabeled"} (if branding is active). It may be used, e.g., to produce an image URL that is
         * different for whitelabeled and branded versions of the product.
         */
        DASH_WHITELABELED_JSP_PROPERTY_NAME("whitelabeled"),
        
        /**
         * The name of the JSP property that contains the URL for an image representing your brand in the context
         * of sailing, e.g., a photo of a boat carrying your brand logo. This is used in the presentation of the
         * different elements of the Sailing Analytics solutions.
         */
        SOLUTIONS_IN_SAILING_IMAGE_URL("solutionsInSailingImageURL"),
        
        SOLUTIONS_IN_SAILING_TRIMMED_IMAGE_URL("solutionsInSailingTrimmedImageURL"),
        
        SAILING_RACE_MANAGER_APP_IMAGE_URL("sailingRaceManagerAppImageURL"),
        
        SAILING_RACE_MANAGER_APP_TRIMMED_IMAGE_URL("sailingRaceManagerAppTrimmedImageURL"),
        
        SAIL_IN_SIGHT_APP_IMAGE_URL("sailInSightAppImageURL"),
        
        BUOY_PINGER_APP_IMAGE_URL("buoyPingerAppImageURL"),
        
        SAILING_SIMULATOR_IMAGE_URL("sailingSimulatorImageURL"),
        
        SAILING_SIMULATOR_TRIMMED_IMAGE_URL("sailingSimulatorTrimmedImageURL"),
        
        SAILING_ANALYTICS_IMAGE_URL("sailingAnalyticsImageURL"),
        
        SAILING_ANALYTICS_READ_MORE_TEXT("sailingAnalyticsReadMoreText"),
        
        SAILING_ANALYTICS_SAILING("sailingAnalyticsSailing"),
        
        FOOTER_COPYRIGHT("footerCopyright"),
        
        FOOTER_PRIVACY_LINK("footerPrivacyLink"),
        
        FOOTER_JOBS_LINK("footerJobsLink"),
        
        FOOTER_SUPPORT_LINK("footerSupportLink"),
        
        FOOTER_WHATS_NEW_LINK("footerWhatsNewLink"),
        
        FOOTER_LEGAL_LINK("footerLegalLink"),
        
        SPORTS_ON("sportsOn"),
        
        FOLLOW_SPORTS("followSports"),
        
        X_LINK("xLink"),
        
        FACEBOOK_LINK("facebookLink"),
        
        INSTAGRAM_LINK("instagramLink"),
        
        WELCOME_TO_SAILING_ANALYTICS("welcomeToSailingAnalytics"),
        
        WELCOME_TO_SAILING_ANALYTICS_BODY("welcomeToSailingAnalyticsBody"),
        
        MORE_LOGIN_INFORMATION_SIMULATOR_URL("moreLoginInformationSimulatorURL"),

        MORE_LOGIN_INFORMATION_SAILOR_PROFILES_URL("moreLoginInformationSailorProfilesURL"),

        MORE_LOGIN_INFORMATION_SETTINGS_URL("moreLoginInformationSettingsURL"),

        MORE_LOGIN_INFORMATION_NOTIFICATIONS_URL("moreLoginInformationNotificationsURL"),
        
        FOLLOW_GITHUB("followGitHub"),
        
        GITHUB_LINK("gitHubLink"),


        /**
         * If you place the value of the property identified by this constant into a {@code script} tag in a HTML/JSP page, it will
         * set the {@code document.clientConfigurationContext} object to a JSON object that contains all the branding fields. 
         */
        SCRIPT_FOR_CLIENT_CONFIGURATION_CONTEXT_TO_DOCUMENT_JSP_PROPERTY_NAME("scriptForClientConfigurationContextToDocument");

        private BrandingConfigurationProperty(String propertyName) {
            this.propertyName = propertyName;
        }
        
        public String getPropertyName() {
            return propertyName;
        }

        private final String propertyName;
    }
    
    boolean isBrandingActive();
    
    BrandingConfiguration getActiveBrandingConfiguration();
    
    /**
     * Looks for a {@link BrandingConfiguration} that was registered with the OSGi service registry
     * using the {@link BrandingConfigurationService#BRANDING_ID_PROPERTY_NAME} property with the value
     * as specified by the {@code brandingConfigurationId} parameter and sets it as the active configuration.
     * This will then be the configuration returned by {@link #getActiveBrandingConfiguration()}. If no such
     * configuration is found, branding is effectively deactivated and the {@link #isBrandingActive()} method
     * will return {@code false}.<p>
     * 
     * @return the branding configuration found under the given ID, or {@code null} if no such configuration
     * was found in the OSGi service registry. Note that in case {@code null} is returned and the branding
     * configuration with the ID specified is added to the registry only later, {@link #getActiveBrandingConfiguration()}
     * can still return that configuration if it is called after the configuration was added.<p>
     */
    BrandingConfiguration setActiveBrandingConfigurationById(String brandingConfigurationId);
    
    /**
     * Uses the {@link #getActiveBrandingConfiguration() active branding configuration} to produce a map of the
     * branding-related properties that can be used in JSP contexts. The keys of the map are constructed using the
     * {@link #JSP_PROPERTY_NAME_PREFIX} and the property names defined in
     * {@link BrandingConfigurationProperty#getPropertyName()}. When branding is not active, the map will contain only
     * the following entries: {@link BrandingConfigurationProperty#BRANDING_ACTIVE_JSP_PROPERTY_NAME},
     * {@link BrandingConfigurationProperty#DEBRANDING_ACTIVE_JSP_PROPERTY_NAME},
     * {@link BrandingConfigurationProperty#BRAND_TITLE_WITH_TRAILING_SPACE_JSP_PROPERTY_NAME}, and
     * {@link BrandingConfigurationProperty#DASH_WHITELABELED_JSP_PROPERTY_NAME}. Otherwise, all properties from
     * {@link BrandingConfigurationProperty} will be included.
     * <p>
     * @param locale if present, the locale to use for the branding configuration properties, otherwise the default locale
     */
    Map<BrandingConfigurationProperty, Object> getBrandingConfigurationProperties(Optional<String> locale);
    
    /**
     * Same as {@link #getBrandingConfigurationProperties(Optional)} but returns a map where the keys are
     * the property names as defined in {@link BrandingConfigurationProperty#getPropertyName()}.
     */
    Map<String, Object> getBrandingConfigurationPropertiesForJspContext(Optional<String> locale);
}
