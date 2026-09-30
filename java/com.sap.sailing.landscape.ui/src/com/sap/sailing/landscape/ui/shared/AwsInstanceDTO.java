package com.sap.sailing.landscape.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.TimePoint;

public class AwsInstanceDTO implements IsSerializable {
    private String instanceId;
    private String instanceType;
    private AvailabilityZoneDTO availabilityZoneDTO;
    private String privateIpAddress;
    private String publicIpAddress;
    private String region;
    private TimePoint launchTimePoint;
    private boolean shared;
    @Deprecated
    AwsInstanceDTO() {} // for GWT RPC serialization only
    
    public AwsInstanceDTO(String instanceId, String instanceType, String privateIpAddress, String publicIpAddress, String region, TimePoint launchTimePoint, boolean shared, AvailabilityZoneDTO azDTO) {
        super();
        this.instanceId = instanceId;
        this.instanceType = instanceType;
        this.availabilityZoneDTO = azDTO;
        this.privateIpAddress = privateIpAddress;
        this.publicIpAddress = publicIpAddress;
        this.region = region;
        this.launchTimePoint = launchTimePoint;
        this.shared = shared;
    }
    public String getAvailabilityZoneId() {
        return availabilityZoneDTO.getAzId();
    }
    public String getInstanceType() {
        return instanceType;
    }
    public String getInstanceId() {
        return instanceId;
    }
    public AvailabilityZoneDTO getAvailabilityZoneDTO() {
        return availabilityZoneDTO;
    }
    public String getAvailabilityZoneName() {
        return availabilityZoneDTO.getAzName();
    }
    public String getRegion() {
        return region;
    }
    public TimePoint getLaunchTimePoint() {
        return launchTimePoint;
    }
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }
    public String getPublicIpAddress() {
        return publicIpAddress;
    }
    public boolean isShared() {
        return shared;
    }
}
