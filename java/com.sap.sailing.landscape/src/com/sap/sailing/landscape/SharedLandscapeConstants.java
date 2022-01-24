package com.sap.sailing.landscape;

public interface SharedLandscapeConstants {
    /**
     * If no specific domain name is provided, e.g., when creating a new application replica set, this will be
     * the default domain name appended to the host name which, in turn, may be derived, e.g., from the application
     * replica set's name.
     */
    String DEFAULT_DOMAIN_NAME = "sapsailing.com";
    
    /**
     * This is the region of the load balancer handling the default traffic for {@code *.sapsailing.com}. It is also
     * called the "dynamic" load balancer because adding, removing or changing any hostname-based rule in its HTTPS
     * listener's rule set takes effect immediately and is hence suited well for short-lived events that will be
     * archived after a short period of time.<p>
     * 
     * Care must be taken not to attempt a "dynamic" load balancer set-up for replica sets launched in regions
     * other than the one identified by this region constant because otherwise the {@code *.sapsailing.com} default
     * Route53 DNS record would be adjusted to point to that region's dynamic load balancer instead, making the
     * actual default load balancer and its default rule routing to the central reverse proxy and from there to
     * the landing page and the archive server inactive.<p>
     * 
     * A future set-up may look different, though, with "dynamic" load balancers grouped in an AWS Global Accelerator
     * which handles cross-region traffic automatically, based on where load balancers are available. Archive servers
     * may be replicated into multiple regions, and so may reverse proxy configurations that handle re-write rules
     * for archived events. If such a state is reached, "dynamic" load balancing may potentially be used regardless
     * the region.
     */
    String REGION_WITH_DEFAULT_LOAD_BALANCER = "eu-west-1";

    /**
     * The tag value used to identify host images that can be launched in order to run one or more Sailing Analytics
     * server processes on it.
     */
    String IMAGE_TYPE_TAG_VALUE_SAILING = "sailing-analytics-server";
    
    /**
     * The tag attached to hosts running zero or more Sailing Analytics processes. Can be used to discover
     * application replica sets in a landscape.
     */
    String SAILING_ANALYTICS_APPLICATION_HOST_TAG = "sailing-analytics-server";

    String ARCHIVE_SERVER_APPLICATION_HOST_TAG_VALUE = "ARCHIVE";

    /**
     * Value of the {@link #SAILSAILING_ANALYTICS_APPLICATION_HOST_TAGING_ANALYTICS_APPLICATION_HOST_TAG} tag
     * for hosts expected to run more than one dedicated application process.
     */
    String MULTI_PROCESS_INSTANCE_TAG_VALUE = "___multi___";

    /**
     * Default value for the {@code Name} tag for shared instances expected to run multiple application processes.
     */
    String MULTI_PROCESS_INSTANCE_DEFAULT_NAME = "SL Multi-Server";
}
