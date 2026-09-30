package com.sap.sailing.landscape.ui.shared;

import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;

public class ReverseProxyDTO extends AwsInstanceDTO implements Named {
    private static final long serialVersionUID = 1576177375197043472L;

    @Deprecated
    ReverseProxyDTO() {} 
    
    private String name;
    private String amiId;
    private String health;
    private boolean isDisposable = false;

    public ReverseProxyDTO(String instanceId, String instanceType, String privateIpAddress,
            String publicIpAddress, String region, TimePoint launchTimePoint, boolean shared, String name, String imageId,
            String healthInTargetGroup, boolean isDisposable, AvailabilityZoneDTO availabilityZoneDTO) {
        super(instanceId, instanceType, privateIpAddress, publicIpAddress, region, launchTimePoint, shared, availabilityZoneDTO);
        this.name = name;
        this.amiId = imageId;
        this.health = healthInTargetGroup;
        this.isDisposable = isDisposable;
    }
    
    public String getName() {
        return name;
    }

    public String getImageId() {
        return amiId;
    }

    public String getHealth() {
        return health;
    }

    public boolean isDisposable() {
        return isDisposable;
    }
}
