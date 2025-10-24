package com.sap.sse.landscape.aws.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.ReverseProxy;

public class ReverseProxyTargetGroupHealthCheckPathTest {
    private final static String TARGET_GROUP_ARN = "arn:aws:elasticloadbalancing:eu-west-1:017363970217:targetgroup/S-BYC-m/141f1fbae1ac783f";
    
    @Test
    public void testHealthCheckPath() {
        final String healthCheckPath = String.format(AbstractApacheReverseProxy.TARGET_GROUP_STATUS, TARGET_GROUP_ARN);
        assertTrue(healthCheckPath.endsWith("?arn="+TARGET_GROUP_ARN));
    }
    
    @Test
    public <ApplicationProcessT extends ApplicationProcess<String, ApplicationProcessMetrics, ApplicationProcessT>> void testHealthCheckPathOnReverseProxy() {
        final ReverseProxy<?, ?, ?, ?> reverseProxy = new ApacheReverseProxy<String, ApplicationProcessMetrics, ApplicationProcessT>(null, null);
        assertTrue(reverseProxy.getTargetGroupHealthCheckPath(TARGET_GROUP_ARN).endsWith("?arn="+TARGET_GROUP_ARN));
    }
}
