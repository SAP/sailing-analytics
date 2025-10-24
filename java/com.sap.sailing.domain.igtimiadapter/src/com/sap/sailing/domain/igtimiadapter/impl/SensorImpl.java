package com.sap.sailing.domain.igtimiadapter.impl;

import com.sap.sailing.domain.igtimiadapter.Sensor;

public class SensorImpl implements Sensor {
    private final String deviceSerialNumber;
    private final long subDeviceId;
    
    public SensorImpl(String deviceSerialNumber, long subDeviceId) {
        super();
        this.deviceSerialNumber = deviceSerialNumber;
        this.subDeviceId = subDeviceId;
    }
    
    @Override
    public String getDeviceSerialNumber() {
        return deviceSerialNumber;
    }
    
    @Override
    public long getSensorId() {
        return subDeviceId;
    }
    
    @Override
    public String toString() {
        return "Device "+getDeviceSerialNumber()+", sensor "+getSensorId();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deviceSerialNumber == null) ? 0 : deviceSerialNumber.hashCode());
        result = prime * result + (int) (subDeviceId ^ (subDeviceId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SensorImpl other = (SensorImpl) obj;
        if (deviceSerialNumber == null) {
            if (other.deviceSerialNumber != null)
                return false;
        } else if (!deviceSerialNumber.equals(other.deviceSerialNumber))
            return false;
        if (subDeviceId != other.subDeviceId)
            return false;
        return true;
    }

}
