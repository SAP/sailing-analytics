package com.sap.sse.landscape.aws.impl;

import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.aws.AwsAutoScalingGroup;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;

public class AwsAutoScalingGroupImpl implements AwsAutoScalingGroup {
    private static final long serialVersionUID = -6244055510377604441L;
    private final AutoScalingGroup autoScalingGroup;
    private final LaunchTemplate launchTemplate;
    private final LaunchTemplateVersion launchTemplateDefaultVersion;
    private final Region region;

    public AwsAutoScalingGroupImpl(AutoScalingGroup autoScalingGroup, LaunchTemplate launchTemplate, LaunchTemplateVersion launchTemplateDefaultVersion, Region region) {
        super();
        this.autoScalingGroup = autoScalingGroup;
        this.launchTemplate = launchTemplate;
        this.launchTemplateDefaultVersion = launchTemplateDefaultVersion;
        this.region = region;
    }

    @Override
    public AutoScalingGroup getAutoScalingGroup() {
        return autoScalingGroup;
    }
    
    @Override
    public LaunchTemplate getLaunchTemplate() {
        return launchTemplate;
    }

    @Override
    public LaunchTemplateVersion getLaunchTemplateDefaultVersion() {
        return launchTemplateDefaultVersion;
    }

    @Override
    public Region getRegion() {
        return region;
    }
}
