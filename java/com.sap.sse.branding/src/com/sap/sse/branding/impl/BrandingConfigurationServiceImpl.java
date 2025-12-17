package com.sap.sse.branding.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sap.sse.branding.BrandingConfigurationService;
import com.sap.sse.branding.shared.BrandingConfiguration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.ServiceTrackerFactory;

public class BrandingConfigurationServiceImpl implements BrandingConfigurationService, ServiceTrackerCustomizer<BrandingConfiguration, BrandingConfiguration> {
    private static final Logger logger = Logger.getLogger(BrandingConfigurationServiceImpl.class.getName());

    private final ServiceTracker<BrandingConfiguration, BrandingConfiguration> brandingConfigurationTracker;
    
    private final BundleContext bundleContext;
    
    private final ConcurrentMap<Pair<String, String>, Map<String, Object>> brandingConfigurationsByIdAndLocale;
    
    /**
     * Starts out as {@code null}. Will be set by {@link #setActiveBrandingConfigurationById(String)} if the
     * configuration by that ID is found through the {@link #brandingConfigurationTracker}. Furthermore,
     * a {@link ServiceTrackerCustomizer} is used to update this field whenever the service currently in
     * use is removed or modified, or a new service matching the ID is added.
     */
    private BrandingConfiguration activeBrandingConfiguration;
    
    private Filter filterForActiveBrandingConfigurationId;
    
    public BrandingConfigurationServiceImpl(BundleContext bundleContext) {
        super();
        this.bundleContext = bundleContext;
        brandingConfigurationTracker = ServiceTrackerFactory.createAndOpen(bundleContext, BrandingConfiguration.class, this);
        brandingConfigurationsByIdAndLocale = new ConcurrentHashMap<>();
    }
    
    BrandingConfigurationServiceImpl(BundleContext bundleContext, ServiceTracker<BrandingConfiguration, BrandingConfiguration> brandingConfigurationTracker) {
        // for testing only
        this.bundleContext = bundleContext;
        this.brandingConfigurationTracker = brandingConfigurationTracker;
        this.brandingConfigurationsByIdAndLocale = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isBrandingActive() {
        return activeBrandingConfiguration != null;
    }

    @Override
    public BrandingConfiguration setActiveBrandingConfigurationById(String brandingConfigurationId) {
        filterForActiveBrandingConfigurationId = createFilterForBrandingConfigurationId(brandingConfigurationId);
        activeBrandingConfiguration = filterForActiveBrandingConfigurationId == null ? null :
            brandingConfigurationTracker.getServiceReferences() == null ? null :
            Arrays.asList(brandingConfigurationTracker.getServiceReferences())
                .stream()
                .filter(ref -> filterForActiveBrandingConfigurationId.match(ref.getProperties()))
                .findFirst()
                .map(ref -> brandingConfigurationTracker.getService(ref))
                .orElse(null);
        if (activeBrandingConfiguration == null) {
            logger.warning("Couldn't find a branding configuration with ID " + brandingConfigurationId +
                    " in the OSGi service registry. Branding is effectively deactivated.");
        } else {
            logger.info("Found active branding configuration: " + activeBrandingConfiguration.getId()+
                    " with object ID "+System.identityHashCode(activeBrandingConfiguration));
        }
        return activeBrandingConfiguration;
    }

    private Filter createFilterForBrandingConfigurationId(String brandingConfigurationId) {
        try {
            return brandingConfigurationId == null ? null :
                bundleContext.createFilter(
                    String.format("(&(%s=%s)(%s=%s))",
                            BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME, ""+brandingConfigurationId,
                            Constants.OBJECTCLASS, BrandingConfiguration.class.getName()));
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Internal error: Invalid filter syntax for BrandingConfigurationService", e);
        }
    }
    
    @Override
    public BrandingConfiguration getActiveBrandingConfiguration() {
        return activeBrandingConfiguration;
    }


    @Override
    public Map<BrandingConfigurationProperty, Object> getBrandingConfigurationProperties(Optional<String> locale) {
        final BrandingConfiguration brandingConfiguration = getActiveBrandingConfiguration();
        final Map<BrandingConfigurationProperty, Object> map = new HashMap<>();
        final String title;
        final String whitelabeled;
        if (brandingConfiguration != null) {
            title = brandingConfiguration.getBrandTitle(locale)+" ";
            whitelabeled = "";
        } else {
            title = "";
            whitelabeled = "-whitelabeled";
        }
        map.put(BrandingConfigurationProperty.BRAND_TITLE_WITH_TRAILING_SPACE_JSP_PROPERTY_NAME, title);
        map.put(BrandingConfigurationProperty.SOLUTIONS_IN_SAILING_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSolutionsInSailingImageURL());
        map.put(BrandingConfigurationProperty.SOLUTIONS_IN_SAILING_TRIMMED_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSoutionsInSailingTrimmedImageURL());
        map.put(BrandingConfigurationProperty.SAILING_RACE_MANAGER_APP_TRIMMED_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSailingRaceManagerAppTrimmedImageURL());
        map.put(BrandingConfigurationProperty.SAILING_RACE_MANAGER_APP_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSailingRaceManagerAppImageURL());
        map.put(BrandingConfigurationProperty.SAIL_IN_SIGHT_APP_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSailInSightAppImageURL());
        map.put(BrandingConfigurationProperty.SAILING_SIMULATOR_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSailingSimulatorImageURL());
        map.put(BrandingConfigurationProperty.SAILING_SIMULATOR_TRIMMED_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSailingSimulatorTrimmedImageURL());
        map.put(BrandingConfigurationProperty.SAIL_IN_SIGHT_APP_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSailInSightAppImageURL());
        map.put(BrandingConfigurationProperty.BUOY_PINGER_APP_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getBuoyPingerAppImageURL());
        map.put(BrandingConfigurationProperty.SAILING_ANALYTICS_IMAGE_URL, brandingConfiguration == null ? "" : brandingConfiguration.getSailingAnalyticsImageURL());
        map.put(BrandingConfigurationProperty.FOOTER_COPYRIGHT, brandingConfiguration == null ? "" : brandingConfiguration.getFooterCopyright());
        map.put(BrandingConfigurationProperty.FOOTER_PRIVACY_LINK, brandingConfiguration == null ? "" : brandingConfiguration.getFooterPrivacyLink());
        map.put(BrandingConfigurationProperty.FOOTER_JOBS_LINK, brandingConfiguration == null ? "" : brandingConfiguration.getFooterJobsLink());
        map.put(BrandingConfigurationProperty.FOOTER_SUPPORT_LINK, brandingConfiguration == null ? "" : brandingConfiguration.getFooterSupportLink());
        map.put(BrandingConfigurationProperty.MORE_LOGIN_INFORMATION_SIMULATOR_URL, brandingConfiguration == null ? "" : brandingConfiguration.getMoreLoginInformationSimulatorURL());
        map.put(BrandingConfigurationProperty.MORE_LOGIN_INFORMATION_SAILOR_PROFILES_URL, brandingConfiguration == null ? "" : brandingConfiguration.getMoreLoginInformationSailorProfilesURL());
        map.put(BrandingConfigurationProperty.MORE_LOGIN_INFORMATION_SETTINGS_URL, brandingConfiguration == null ? "" : brandingConfiguration.getMoreLoginInformationSettingsURL());
        map.put(BrandingConfigurationProperty.MORE_LOGIN_INFORMATION_NOTIFICATIONS_URL, brandingConfiguration == null ? "" : brandingConfiguration.getMoreLoginInformationNotificationsURL());
        map.put(BrandingConfigurationProperty.SAILING_ANALYTICS_READ_MORE_TEXT, brandingConfiguration == null ? "" : brandingConfiguration.getSailingAnalyticsReadMoreText(locale));
        map.put(BrandingConfigurationProperty.SPORTS_ON, brandingConfiguration == null ? "" : brandingConfiguration.getSportsOn(locale));
        map.put(BrandingConfigurationProperty.FOLLOW_SPORTS, brandingConfiguration == null ? "" : brandingConfiguration.getFollowSports(locale));
        map.put(BrandingConfigurationProperty.X_LINK, brandingConfiguration == null ? "" : brandingConfiguration.getxLink());
        map.put(BrandingConfigurationProperty.FACEBOOK_LINK, brandingConfiguration == null ? "" : brandingConfiguration.getFacebookLink());
        map.put(BrandingConfigurationProperty.INSTAGRAM_LINK, brandingConfiguration == null ? "" : brandingConfiguration.getInstagramLink());
        map.put(BrandingConfigurationProperty.SAILING_ANALYTICS_SAILING, brandingConfiguration == null ? "" : brandingConfiguration.getSailingAnalyticsSailing(locale));
        map.put(BrandingConfigurationProperty.WELCOME_TO_SAILING_ANALYTICS, brandingConfiguration == null ? "" : brandingConfiguration.getWelcomeToSailingAnalytics(locale));
        map.put(BrandingConfigurationProperty.WELCOME_TO_SAILING_ANALYTICS_BODY, brandingConfiguration == null ? "" : brandingConfiguration.getWelcomeToSailingAnalyticsBody(locale));
        map.put(BrandingConfigurationProperty.FOLLOW_GITHUB, brandingConfiguration == null ? "" : brandingConfiguration.getFollowGitHub(locale));
        map.put(BrandingConfigurationProperty.GITHUB_LINK, brandingConfiguration == null ? "" : brandingConfiguration.getGitHubLink());
        map.put(BrandingConfigurationProperty.DEBRANDING_ACTIVE_JSP_PROPERTY_NAME, !isBrandingActive());
        map.put(BrandingConfigurationProperty.BRANDING_ACTIVE_JSP_PROPERTY_NAME, isBrandingActive());
        map.put(BrandingConfigurationProperty.DASH_WHITELABELED_JSP_PROPERTY_NAME, whitelabeled);
        map.put(BrandingConfigurationProperty.SCRIPT_FOR_CLIENT_CONFIGURATION_CONTEXT_TO_DOCUMENT_JSP_PROPERTY_NAME, generateScriptForClientConfigurationContext(map));
        return map;
    }

    @Override
    public Map<String, Object> getBrandingConfigurationPropertiesForJspContext(Optional<String> locale) {
        return brandingConfigurationsByIdAndLocale.computeIfAbsent(
                new Pair<>(locale.orElse(null), isBrandingActive() ? getActiveBrandingConfiguration().getId() : null),
                key -> computeBrandingConfigurationProperties(locale));
    }

    private Map<String, Object> computeBrandingConfigurationProperties(Optional<String> requestLocale) {
        final Map<String, Object> brandingProperties = new HashMap<>(); // no concurrency control needed within single request
        getBrandingConfigurationProperties(requestLocale).forEach((k, v) -> {
            brandingProperties.put(k.getPropertyName(), v);
        });
        return brandingProperties;
    }

    private Object generateScriptForClientConfigurationContext(Map<BrandingConfigurationProperty, Object> map) {
        final StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("document.clientConfigurationContext=");
        final JSONObject jsonObject = new JSONObject();
        for (final Entry<BrandingConfigurationProperty, Object> brandingConfigurationPropertyAndValue : map.entrySet()) {
            jsonObject.put(brandingConfigurationPropertyAndValue.getKey().getPropertyName(), brandingConfigurationPropertyAndValue.getValue());
        }
        scriptBuilder.append(jsonObject.toJSONString());
        scriptBuilder.append(";");
        return scriptBuilder.toString();
    }

    @Override
    public BrandingConfiguration addingService(ServiceReference<BrandingConfiguration> reference) {
        final BrandingConfiguration service = evictCachedPropertiesForBrandingServiceReference(reference);
        logger.info("Adding branding configuration service with ID: " + reference.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME) +
                " and object ID " + System.identityHashCode(service));
        if (service != null && filterForActiveBrandingConfigurationId != null && filterForActiveBrandingConfigurationId.match(reference)) {
            logger.info("Added branding configuration service with ID " + reference.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME) +
                    " is selected as the active one.");
            activeBrandingConfiguration = service;
        }
        return service;
    }

    private BrandingConfiguration evictCachedPropertiesForBrandingServiceReference(
            ServiceReference<BrandingConfiguration> reference) {
        final String brandingId = (String) reference.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME);
        final BrandingConfiguration service = bundleContext.getService(reference);
        for (final Iterator<Pair<String, String>> i=brandingConfigurationsByIdAndLocale.keySet().iterator(); i.hasNext(); ) {
            final Pair<String, String> cachedProperties = i.next();
            if (Util.equalsWithNull(cachedProperties.getB(), brandingId)) {
                logger.info("Removing cached branding properties from service with ID: " + brandingId + " and object ID " + System.identityHashCode(service));
                // remove the cached properties for this ID, so that they will be recomputed
                i.remove();
            }
        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<BrandingConfiguration> reference, BrandingConfiguration service) {
        evictCachedPropertiesForBrandingServiceReference(reference);
        logger.info("Modified branding configuration service with ID: " + reference.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME) +
                " and object ID " + System.identityHashCode(service));
        if (service != null && filterForActiveBrandingConfigurationId != null && filterForActiveBrandingConfigurationId.match(reference)) {
            logger.info("Modified branding configuration service with ID " + reference.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME) +
                    ", updating active branding configuration.");
            activeBrandingConfiguration = service;
        }
    }

    @Override
    public void removedService(ServiceReference<BrandingConfiguration> reference, BrandingConfiguration service) {
        evictCachedPropertiesForBrandingServiceReference(reference);
        logger.info("Removed branding configuration service with ID: " + reference.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME) +
                " and object ID " + System.identityHashCode(service));
        if (service == activeBrandingConfiguration) {
            logger.info("The active branding configuration service with ID " + reference.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME) +
                    " was removed, setting active branding configuration to null.");
            activeBrandingConfiguration = null;
        }
    }
}