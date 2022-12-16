package com.sap.sse.landscape.aws.impl;

import com.sap.sse.common.impl.NamedImpl;
import com.sap.sse.landscape.AvailabilityZone;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsLandscape;

public class AwsAvailabilityZoneImpl extends NamedImpl implements AwsAvailabilityZone {
    private static final long serialVersionUID = 7081008193504850891L;
    private final String id;
    private final AwsRegion region;
    
    public AwsAvailabilityZoneImpl(String id, String name, AwsRegion region) {
        super(name);
        this.id = id;
        this.region = region;
    }
    
    public AwsAvailabilityZoneImpl(software.amazon.awssdk.services.ec2.model.AvailabilityZone az, AwsLandscape<?> landscape) {
        this(az.zoneId(), az.zoneName(), new AwsRegion(az.regionName(), landscape));
    }
    
    @Override
    public int hashCode() {
        return getId().hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        return getId().equals(((AvailabilityZone) other).getId());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public AwsRegion getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return super.toString() == null ? getId() : super.toString();
    }
}
