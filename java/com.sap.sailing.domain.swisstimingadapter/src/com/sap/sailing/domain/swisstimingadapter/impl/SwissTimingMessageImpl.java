package com.sap.sailing.domain.swisstimingadapter.impl;

import com.sap.sailing.domain.swisstimingadapter.SwissTimingMessage;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

public class SwissTimingMessageImpl implements SwissTimingMessage {
    private final String raceID;
    private final int packetID;
    private final TimePoint timestamp;
    private final int gpsID;
    private final Position position;
    private final Speed speed;
    private final int numberOfSatellites;
    private final int batteryPercent;
    private final int length;
    
    public SwissTimingMessageImpl(String raceID, int packetID, TimePoint timestamp, int gpsID, Position position, Speed speed,
            int numberOfSatellites, int batteryPercent, int length) {
        super();
        this.raceID = raceID;
        this.packetID = packetID;
        this.timestamp = timestamp;
        this.gpsID = gpsID;
        this.position = position;
        this.speed = speed;
        this.numberOfSatellites = numberOfSatellites;
        this.batteryPercent = batteryPercent;
        this.length = length;
    }
    
    @Override
    public boolean isValid() {
        return numberOfSatellites > 0 && batteryPercent > 0;
    }

    @Override
    public String getRaceID() {
        return raceID;
    }

    @Override
    public int getPacketID() {
        return packetID;
    }

    @Override
    public TimePoint getTimestamp() {
        return timestamp;
    }

    @Override
    public int getGpsID() {
        return gpsID;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public Speed getSpeed() {
        return speed;
    }

    @Override
    public int getNumberOfSatellites() {
        return numberOfSatellites;
    }

    @Override
    public int getBatteryPercent() {
        return batteryPercent;
    }

    @Override
    public int length() {
        return length;
    }
    
    @Override
    public String toString() {
        return "#"+getPacketID()+"/"+getGpsID()+" "+getTimestamp()+": "+getPosition()+", "+getSpeed()+". "+getNumberOfSatellites()+" satellites, battery: "+
                getBatteryPercent()+"%";
    }
}
