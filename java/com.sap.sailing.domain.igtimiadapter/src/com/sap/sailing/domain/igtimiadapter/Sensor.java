package com.sap.sailing.domain.igtimiadapter;

import com.sap.sailing.domain.igtimiadapter.impl.SensorImpl;

public interface Sensor {
    static Sensor create(String deviceSerialNumber, long subDeviceId) {
        return new SensorImpl(deviceSerialNumber, subDeviceId);
    }
    
    /**
     * The ID called "serial number" by Igtimi. This identifies the microcontroller-like device that can mange several sensors
     * such as wind, GPS or heart rate. Together with the type code and the optional {@link #getSensorId() sub-device ID} as used
     * for heart rate sensors the individual sensor is uniquely defined.
     */
    String getDeviceSerialNumber();
    
    /**
     * By default, the sensor ID is 0, particularly if it represents a sensor built into the device, such as a built-in GPS.
     * In case of multiple sensors of the same kind, the non-zero ID will then help to keep the individual sensors attached
     * to the same device apart. Example: multiple heart rate sensors.
     */
    long getSensorId();
}
