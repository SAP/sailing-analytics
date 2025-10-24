package com.sap.sse.security.subscription.chargebee;

import java.util.logging.Logger;

import com.sap.sse.common.Util;

/**
 * Subscription configuration. The system will get these information from server application startup arguments:
 * {@code chargebee.site}, {@code chargebee.apikey}
 * 
 * @author Tu Tran
 */
public class ChargebeeConfiguration {
    private static final Logger logger = Logger.getLogger(ChargebeeConfiguration.class.getName());

    private static final String CHARGEBEE_SITE = "chargebee.site";
    private static final String CHARGEBEE_APIKEY = "chargebee.apikey";

    private static ChargebeeConfiguration instance;

    private final String site;
    private final String apiKey;

    protected ChargebeeConfiguration(String site, String apiKey) {
        this.site = site;
        this.apiKey = apiKey;
    }

    public static ChargebeeConfiguration getInstance() {
        if (instance == null) {
            final String site = System.getProperty(CHARGEBEE_SITE);
            final String apiKey = System.getProperty(CHARGEBEE_APIKEY);
            logger.info("Chargebee site: " + site);
            if (Util.hasLength(site) && Util.hasLength(apiKey)) {
                instance = new ChargebeeConfiguration(site, apiKey);
                logger.info("Activating Chargebee Configuration and service");
            } else {
                logger.info("No "+CHARGEBEE_SITE+" and "+CHARGEBEE_APIKEY+" system properties provided. Not activating Chargebee service");
            }
        }
        return instance;
    }

    public String getSite() {
        return site;
    }

    public String getApiKey() {
        return apiKey;
    }
}
