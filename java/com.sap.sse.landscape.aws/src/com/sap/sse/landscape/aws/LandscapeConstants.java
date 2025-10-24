package com.sap.sse.landscape.aws;

import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceType;

public interface LandscapeConstants {
    /**
     * The key <strong>tag</strong>, indicating that an instance only acts as a reverse proxy
     * (ie. it is not hosting other services). SO it can be terminated without risk.
     */
    String DISPOSABLE_PROXY = "DisposableProxy";
    
    /**
     * The tag name for a target group, which contains all the instances running httpd. Used to mark the
     * target groups containing all of the reverse proxies, disposable and otherwise. This allows for health checks
     * to be accessed and marks which groups any new reverse proxy instances should be added to.
     */
    String ALL_REVERSE_PROXIES = "allReverseProxies";
    
    /**
     * The tag name for a target group containing only the central reverse proxy.
     */
    String JUST_CENTRAL_REVERSE_PROXY = "CentralReverseProxy";
    
    /**
     * A string that all network load balancers contain.
     */
    String NLB_ARN_CONTAINS = "loadbalancer/net";
    
    /**
     * The value of "image-type" tag, which is used to identify which image to use to deploy a disposable reverse proxy.
     */
    String IMAGE_TYPE_REVERSE_PROXY = "disposable-reverse-proxy";
    
    /**
     * Indicates an instance is a reverse proxy.
     */
    String REVERSE_PROXY_TAG_NAME = "ReverseProxy";
    
    /**
     * The tag for a security group that a reverse proxy requires.
     */
    String REVERSE_PROXY_SG_TAG = "reverse-proxy-sg";
    
    /**
     * The tag for any security groups for a mongo DB.
     */
    String MONGO_SG_TAG = "mongo-sg";
    
    /**
     * The tag for a security group for an application server.
     */
    String SAILING_APPLICATION_SG_TAG = "application-server-sg";
    
    /**
     * Indicates that an instance is suitable for co-deployment with httpd.
     */
    String INSTANCE_SUITABLE_FOR_HTTPD = "canCoDeployWithHttpd";
    
    /**
     * The tag for the central reverse proxy, which also hosts non-essential services.
     */
    String CENTRAL_REVERSE_PROXY_TAG_NAME = "CentralReverseProxy";
    
    /**
     * A tag key indicating that a subnet is not to be used for deploying instances, by autoscaling groups or by load balancers.
     */
    String NO_INSTANCE_DEPLOYMENT = "noInstanceDeployment";
    
    /**
     * The version "number" to use for a launch template in an auto-scaling group to identify the
     * "default" version. See, e.g., {@link LaunchTemplateSpecification#version()}.
     */
    String DEFAULT_LAUNCH_TEMPLATE_VERSION_NAME = "$Default";
    
    /**
     * When we use AWS Web Application Firewall (WAF) to protect a web application, we tag the web ACLs using this as a
     * tag key. The tag value then identifies the purpose, e.g., using the value of {@link #WEB_ACL_GEOBLOCKING_PURPOSE}.
     */
    String WEB_ACL_PURPOSE_TAG = "web-acl-purpose";
    
    /**
     * A possible tag value for the {@link #WEB_ACL_PURPOSE_TAG} tag key, on a WAF (Web Application Firewall) web ACL
     * indicating that the web ACL is used for geoblocking.
     */
    String WEB_ACL_GEOBLOCKING_PURPOSE = "geoblocking";

    InstanceType[] INSTANCE_TYPES_BANNED_FROM_INSTANCE_BASED_NLB_TARGET_GROUPS = new InstanceType[] { InstanceType.CC1_4_XLARGE, InstanceType.C1_MEDIUM, InstanceType.C1_XLARGE,
            InstanceType.CC2_8_XLARGE, InstanceType.CG1_4_XLARGE, InstanceType.CR1_8_XLARGE, InstanceType.G2_2_XLARGE,
            InstanceType.G2_8_XLARGE, InstanceType.HI1_4_XLARGE, InstanceType.HS1_8_XLARGE, InstanceType.M1_LARGE,
            InstanceType.M1_MEDIUM, InstanceType.M1_SMALL, InstanceType.M1_XLARGE, InstanceType.M2_2_XLARGE,
            InstanceType.M2_4_XLARGE, InstanceType.M2_XLARGE, InstanceType.M3_2_XLARGE, InstanceType.M3_LARGE,
            InstanceType.M3_MEDIUM, InstanceType.M3_XLARGE, InstanceType.T1_MICRO};
}
