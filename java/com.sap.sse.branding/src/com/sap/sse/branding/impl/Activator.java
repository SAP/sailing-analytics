package com.sap.sse.branding.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import com.sap.sse.branding.BrandingConfigurationService;

public class Activator implements BundleActivator {
    private static BrandingConfigurationService defaultBrandingConfigurationService;

    public static BrandingConfigurationService getDefaultBrandingConfigurationService() {
        return defaultBrandingConfigurationService;
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        defaultBrandingConfigurationService = new BrandingConfigurationServiceImpl(context);
        final String brandingConfigurationId = System.getProperty(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME);
        defaultBrandingConfigurationService.setActiveBrandingConfigurationById(brandingConfigurationId);
        context.registerService(BrandingConfigurationService.class.getName(), defaultBrandingConfigurationService, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
