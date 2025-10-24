package com.sap.sailing.domain.igtimiadapter.impl;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

public class DeviceImpl extends HasIdImpl implements Device {
    private static final long serialVersionUID = 7224992550721569935L;
    private final String serialNumber;
    private String name;
    private TimePoint lastHeartbeat;
    private String remoteAddress;
    
    public DeviceImpl(long id, String serialNumber) {
        this(id, serialNumber, /* name */ null);
    }

    public DeviceImpl(long id, String serialNumber, String name) {
        super(id);
        this.serialNumber = serialNumber;
        this.name = name;
    }

    @Override
    public String getSerialNumber() {
        return serialNumber;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Pair<TimePoint, String> getLastHeartbeat() {
        return lastHeartbeat == null ? null : new Pair<>(lastHeartbeat, remoteAddress);
    }

    @Override
    public void setLastHeartbeat(TimePoint timePointOfLastHeartbeat, String remoteAddress) {
        this.lastHeartbeat = timePointOfLastHeartbeat;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(new TypeRelativeObjectIdentifier(getSerialNumber()));
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.IGTIMI_DEVICE;
    }
}
