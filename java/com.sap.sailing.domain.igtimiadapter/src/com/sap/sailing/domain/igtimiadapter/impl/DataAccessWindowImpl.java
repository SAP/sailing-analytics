package com.sap.sailing.domain.igtimiadapter.impl;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;

public class DataAccessWindowImpl extends HasIdImpl implements DataAccessWindow {
    private static final long serialVersionUID = -7076166985273850220L;
    private final TimePoint startTime;
    private final TimePoint endTime;
    private final String deviceSerialNumber;

    public DataAccessWindowImpl(long id, TimePoint startTime, TimePoint endTime, String deviceSerialNumber) {
        super(id);
        this.startTime = startTime;
        this.endTime = endTime;
        this.deviceSerialNumber = deviceSerialNumber;
    }

    @Override
    public TimePoint getStartTime() {
        return startTime;
    }

    @Override
    public TimePoint getEndTime() {
        return endTime;
    }

    @Override
    public String getDeviceSerialNumber() {
        return deviceSerialNumber;
    }

    @Override
    public String toString() {
        return "DAW "+getId()+" for device "+getDeviceSerialNumber()+" from "+getStartTime()+" to "+getEndTime();
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(DataAccessWindow.createTypeRelativeObjectIdentifier(
                getDeviceSerialNumber(), getStartTime(), getEndTime()));
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.IGTIMI_DATA_ACCESS_WINDOW;
    }

    @Override
    public String getName() {
        return "Data Access Window for device "+getDeviceSerialNumber()+" from "+getStartTime().asMillis()+" to "+getEndTime().asMillis();
    }
}
