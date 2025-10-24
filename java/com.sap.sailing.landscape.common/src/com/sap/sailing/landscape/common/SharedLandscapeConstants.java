package com.sap.sailing.landscape.common;

import java.util.Arrays;
import java.util.Collections;

import com.sap.sse.common.Util;

public interface SharedLandscapeConstants {
    /**
     * If no specific domain name is provided, e.g., when creating a new application replica set, this will be
     * the default domain name appended to the host name which, in turn, may be derived, e.g., from the application
     * replica set's name.
     */
    String DEFAULT_DOMAIN_NAME = "sapsailing.com";

    /**
     * Servers in any of these domains we want to trust. This can and shall be used, e.g., to guard server-side requests
     * to URLs that may have been provided through an API or UI by some potentially untrusted client or user.
     */
    Iterable<String> TRUSTED_DOMAINS = Collections.unmodifiableCollection(Arrays.asList(new String[] {
        DEFAULT_DOMAIN_NAME, "sailing.omegatiming.com", "localhost", "127.0.0.1"
    }));
    
    /**
     * Checks that {@code domain} equals one of {@link #TRUSTED_DOMAINS} or is a sub-domain of any of these
     */
    static boolean isTrustedDomain(String domain) {
        while (Util.hasLength(domain)) {
            if (Util.contains(TRUSTED_DOMAINS, domain)) {
                return true;
            } else {
                final int indexOfSubdomainSeparator = domain.indexOf('.');
                domain = indexOfSubdomainSeparator >= 0 ? domain.substring(indexOfSubdomainSeparator+1) : "";
            }
        }
        return false;
    }
    
    /**
     * The default name of the replica set in the landscape we use to receive data from Igtimi WindBot devices.
     */
    String IGTIMI_DEFAULT_RIOT_REPLICA_SET_NAME = "wind";
    
    /**
     * The default host name under which at port {@link #IGTIMI_DEFAULT_RIOT_PORT} the Riot server can be reached by the
     * Igtimi WindBot devices. Constructed from the {@link #IGTIMI_DEFAULT_RIOT_REPLICA_SET_NAME default Riot replica
     * set name} and the {@link #DEFAULT_DOMAIN_NAME default domain name}.
     */
    String IGTIMI_DEFAULT_RIOT_HOSTNAME = IGTIMI_DEFAULT_RIOT_REPLICA_SET_NAME + "." + DEFAULT_DOMAIN_NAME;
    
    /**
     * The default value for the property whose name is given by {@link #IGTIMI_BASE_URL_PROPERTY_NAME}; ends with a
     * slash (/)
     */
    String IGTIMI_BASE_URL_DEFAULT = "https://" + IGTIMI_DEFAULT_RIOT_REPLICA_SET_NAME + "." + DEFAULT_DOMAIN_NAME + "/";
    
    /**
     * The port of our default Igtimi "Riot" server implementation that WindBot devices may send their data to.
     * WindBots can be configured in their {@code config.ini} file with a line like
     * <pre>
     *   riot server_address &lt;hostname&gt; &lt;port&gt;
     * </pre>
     * Where the hostname should by default be the one used in the {@link #IGTIMI_BASE_URL_DEFAULT}, and the
     * port should be the value of this constant.
     */
    int IGTIMI_DEFAULT_RIOT_PORT = 6000;
    
    /**
     * If a shared security realm is to be used for a domain then this constant tells the name of the application
     * replica set that by default manages the shared security information. Other replicables that are to be shared
     * through the same realm, such as landscape management data or shared sailing data such as course templates or mark
     * properties inventories, can be shared from the same application replica set.
     * <p>
     * 
     * To obtain the full host name used by the application replica set, append "." and the
     * {@link #DEFAULT_DOMAIN_NAME}.
     */
    String DEFAULT_SECURITY_SERVICE_REPLICA_SET_NAME = "security-service";
    
    String RABBIT_IN_DEFAULT_REGION_HOSTNAME = "rabbit.internal."+DEFAULT_DOMAIN_NAME;

    String DEFAULT_REGION = "eu-west-1";
    
    /**
     * We maintain a DNS entry for "rabbit.internal.sapsailing.com" (see {@link #RABBIT_IN_DEFAULT_REGION_HOSTNAME}) in this region
     */
    String REGION_WITH_RABBITMQ_DNS_HOSTNAME = DEFAULT_REGION;
    
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
    String REGION_WITH_DEFAULT_LOAD_BALANCER = DEFAULT_REGION;
    
    /**
     * Tag name used to identify instances on which a RabbitMQ installation is running. The tag value is currently interpreted to
     * be the port number (usually 5672) on which the RabbitMQ endpoint can be reached.
     */
    String RABBITMQ_TAG_NAME = "RabbitMQEndpoint";
    
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

    String ARCHIVE_SERVER_APPLICATION_REPLICA_SET_NAME = "ARCHIVE";

    /**
     * Value of the {@link #SAILING_ANALYTICS_APPLICATION_HOST_TAG} tag
     * for hosts expected to run more than one dedicated application process.
     */
    String MULTI_PROCESS_INSTANCE_TAG_VALUE = "___multi___";

    /**
     * Default value for the {@code Name} tag for shared instances expected to run multiple application processes.
     */
    String MULTI_PROCESS_INSTANCE_DEFAULT_NAME = "SL Multi-Server";

    String DEFAULT_DEDICATED_INSTANCE_TYPE_NAME = "C5_2_XLARGE";
    
    String DEFAULT_SHARED_INSTANCE_TYPE_NAME = "I3_2_XLARGE";

    /**
     * Tells how to size process heaps on shared instances by default, based on the instance's physical memory.
     * Harmonizes with the {@link #DEFAULT_SHARED_INSTANCE_TYPE_NAME} and the expected approximate memory requirements
     * of a typical process instance.
     */
    int DEFAULT_NUMBER_OF_PROCESSES_IN_MEMORY = 4;

    /**
     * The most appropriate instance type for a disposable reverse proxy.
     */
    String DEFAULT_REVERSE_PROXY_INSTANCE_TYPE = "T3_MEDIUM";

    String DEFAULT_DISPOSABLE_REVERSE_PROXY_INSTANCE_NAME = "DisposableReverseProxy";

    /**
     * Hostname of failover archive. It is defined in {@code root@sapsailing.com:/etc/httpd/conf.d/001-events.conf} and
     * points to the IP address defined as {@code ARCHIVE_FAILOVER_IP} in
     * {@code root@sapsailing.com:/etc/httpd/conf.d/000-macros.conf}. Both files are maintained in the {@code httpdConf}
     * Git repository that currently has its headless home under {@code httpdConf@sapsailing.com:repo.git} and is used
     * in particular to synchronize configuration changes across central and disposable reverse proxies.
     * See also <a href="...">the Wiki article about this.</a>
     */
    String ARCHIVE_FAILOVER_ADDRESS = "archive-failover." +DEFAULT_DOMAIN_NAME;
}
