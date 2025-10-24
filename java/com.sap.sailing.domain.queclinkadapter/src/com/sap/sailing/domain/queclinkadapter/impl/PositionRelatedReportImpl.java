package com.sap.sailing.domain.queclinkadapter.impl;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.queclinkadapter.PositionRelatedReport;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;

public class PositionRelatedReportImpl implements PositionRelatedReport {
    private final Byte hdop;
    private final SpeedWithBearing cogAndSog;
    private final Distance altitude;
    private final Position position;
    private final TimePoint validityTime;
    private final Short mobileCountryCode;
    private final Short mobileNetworkCode;
    private final Integer locationAreaCode;
    private final Integer cellId;
    private final Distance odometer;

    public PositionRelatedReportImpl(Byte hdop, SpeedWithBearing cogAndSog, Distance altitude, Position position,
            TimePoint validityTime, Short mobileCountryCode, Short mobileNetworkCode, Integer locationAreaCode,
            Integer cellId, Distance odometer) {
        this.hdop = hdop;
        this.cogAndSog = cogAndSog;
        this.altitude = altitude;
        this.position = position;
        this.validityTime = validityTime;
        this.mobileCountryCode = mobileCountryCode;
        this.mobileNetworkCode = mobileNetworkCode;
        this.locationAreaCode = locationAreaCode;
        this.cellId = cellId;
        this.odometer = odometer;
    }

    @Override
    public byte getHdop() {
        return hdop;
    }

    @Override
    public SpeedWithBearing getCogAndSog() {
        return cogAndSog;
    }

    @Override
    public Distance getAltitude() {
        return altitude;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public TimePoint getValidityTime() {
        return validityTime;
    }

    @Override
    public short getMobileCountryCode() {
        return mobileCountryCode;
    }

    @Override
    public short getMobileNetworkCode() {
        return mobileNetworkCode;
    }

    @Override
    public int getLocationAreaCode() {
        return locationAreaCode;
    }

    @Override
    public int getCellId() {
        return cellId;
    }

    @Override
    public Distance getOdometer() {
        return odometer;
    }
}

