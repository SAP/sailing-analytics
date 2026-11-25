package com.sap.sse.landscape.aws.impl;

import java.util.Optional;

import com.sap.sse.common.Duration;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.ReverseProxy;

public abstract class AbstractApacheReverseProxy<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
        implements ReverseProxy<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> {
    /**
     * The path the target group will ping to check the health of the reverse proxy.
     */
    protected static final String INTERNAL_SERVER_STATUS = "internal-server-status";
    protected static final String TARGET_GROUP_STATUS = "cgi-bin/reverseProxyHealthcheck.sh?arn=%s";
    /**
     * five minutes of timeout for most network-related actions
     */
    protected static final Optional<Duration> TIMEOUT = Optional.of(Duration.ONE_MINUTE.times(5));

    private final AwsLandscape<ShardingKey> landscape;

    public AbstractApacheReverseProxy(AwsLandscape<ShardingKey> landscape) {
        this.landscape = landscape;
    }

    protected AwsLandscape<ShardingKey> getLandscape() {
        return landscape;
    }

    @Override
    public String getHealthCheckPath() {
        return "/" + INTERNAL_SERVER_STATUS;
    }

    @Override
    public String getTargetGroupHealthCheckPath(String targetGroupArn) {
        return "/" + String.format(TARGET_GROUP_STATUS, targetGroupArn);
    }
}
