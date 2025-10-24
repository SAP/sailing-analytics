package com.sap.sailing.domain.igtimiadapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.igtimi.IgtimiData.ApparentWindAngle;
import com.igtimi.IgtimiData.ApparentWindSpeed;
import com.igtimi.IgtimiData.CourseOverGround;
import com.igtimi.IgtimiData.Data;
import com.igtimi.IgtimiData.DataMsg;
import com.igtimi.IgtimiData.DataPoint;
import com.igtimi.IgtimiData.GNSS_Position;
import com.igtimi.IgtimiData.GNSS_Quality;
import com.igtimi.IgtimiData.GNSS_Sat_Count;
import com.igtimi.IgtimiData.Heading;
import com.igtimi.IgtimiData.HeadingMagnetic;
import com.igtimi.IgtimiData.SpeedOverGround;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWA;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWS;
import com.sap.sailing.domain.igtimiadapter.datatypes.BatteryLevel;
import com.sap.sailing.domain.igtimiadapter.datatypes.COG;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsAltitude;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsLatLong;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsQualityHdop;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsQualitySatCount;
import com.sap.sailing.domain.igtimiadapter.datatypes.HDG;
import com.sap.sailing.domain.igtimiadapter.datatypes.HDGM;
import com.sap.sailing.domain.igtimiadapter.datatypes.Log;
import com.sap.sailing.domain.igtimiadapter.datatypes.SOG;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sun.jersey.core.util.Base64;

/**
 * Can convert JSON messages from the original Riot API, such as resource data or web socket
 * text messages, as well as binary Protocol Buffer (protobuf) messages in {@link Msg} format
 * into {@link Fix} objects.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class FixFactory {
    public Iterable<Fix> createFixes(JSONObject sensorsJson) throws InvalidProtocolBufferException {
        final List<Fix> result = new ArrayList<>();
        for (final Entry<Object, Object> e : sensorsJson.entrySet()) {
            final String deviceSerialNumber = (String) e.getKey();
            final JSONArray messagesAsBase64 = (JSONArray) e.getValue();
            Util.addAll(createFixesForTypes(deviceSerialNumber, messagesAsBase64), result);
        }
        return result;
    }

    private Iterable<Fix> createFixesForTypes(String deviceSerialNumber, JSONArray messagesAsBase64) throws InvalidProtocolBufferException {
        final List<Fix> result = new ArrayList<>();
        for (final Object messageAsBase64 : messagesAsBase64) {
            final Msg msg = Msg.parseFrom(Base64.decode(messageAsBase64.toString()));
            Util.addAll(createFixes(msg), result);
        }
        return result;
    }
    
    public Iterable<Fix> createFixes(Msg message) {
        final List<Fix> fixes = new ArrayList<>(); // the fixes extracted from the message
        MsgVisitor.accept(message, new MsgVisitor() {
            @Override
            public void handleData(Data data) {
                for (final DataMsg dataMsg : data.getDataList()) {
                    final String serialNumber = dataMsg.getSerialNumber();
                    for (final DataPoint dataPoint : dataMsg.getDataList()) {
                        DataPointVisitor.accept(dataPoint, new DataPointVisitor<Void>() {
                            @Override
                            public Void handleAwa(ApparentWindAngle awa) {
                                fixes.add(new AWA(TimePoint.of(awa.getTimestamp()), getSensor(serialNumber), new DegreeBearingImpl(awa.getValue())));
                                return null;
                            }
    
                            @Override
                            public Void handleAws(ApparentWindSpeed aws) {
                                fixes.add(new AWS(TimePoint.of(aws.getTimestamp()), getSensor(serialNumber), new KilometersPerHourSpeedImpl(aws.getValue())));
                                return null;
                            }
    
                            @Override
                            public Void handleCog(CourseOverGround cog) {
                                fixes.add(new COG(TimePoint.of(cog.getTimestamp()), getSensor(serialNumber), new DegreeBearingImpl(cog.getValue())));
                                return null;
                            }
    
                            @Override
                            public Void handleHdg(Heading hdg) {
                                fixes.add(new HDG(TimePoint.of(hdg.getTimestamp()), getSensor(serialNumber), new DegreeBearingImpl(hdg.getValue())));
                                return null;
                            }
    
                            @Override
                            public Void handleHdgm(HeadingMagnetic hdgm) {
                                fixes.add(new HDGM(TimePoint.of(hdgm.getTimestamp()), getSensor(serialNumber), new DegreeBearingImpl(hdgm.getValue())));
                                return null;
                            }
    
                            @Override
                            public Void handlePos(GNSS_Position pos) {
                                final Sensor sensor = getSensor(serialNumber);
                                fixes.add(new GpsLatLong(TimePoint.of(pos.getTimestamp()), sensor, new DegreePosition(pos.getLatitude(), pos.getLongitude())));
                                fixes.add(new GpsAltitude(TimePoint.of(pos.getTimestamp()), sensor, new MeterDistance(pos.getAltitude())));
                                return null;
                            }
    
                            @Override
                            public Void handleSatq(GNSS_Quality hdop) {
                                fixes.add(new GpsQualityHdop(TimePoint.of(hdop.getTimestamp()), getSensor(serialNumber), new MeterDistance(hdop.getValue())));
                                return null;
                            }
    
                            @Override
                            public Void handleSatc(GNSS_Sat_Count satCount) {
                                fixes.add(new GpsQualitySatCount(TimePoint.of(satCount.getTimestamp()), getSensor(serialNumber), satCount.getValue()));
                                return null;
                            }
    
                            @Override
                            public Void handleNum(com.igtimi.IgtimiData.Number num) {
                                // This is expected to represent the battery state of charge (SOC) in percent
                                fixes.add(new BatteryLevel(TimePoint.of(num.getTimestamp()), getSensor(serialNumber), num.getValue()));
                                return null;
                            }
    
                            @Override
                            public Void handleSog(SpeedOverGround sog) {
                                fixes.add(new SOG(TimePoint.of(sog.getTimestamp()), getSensor(serialNumber), new KilometersPerHourSpeedImpl(sog.getValue())));
                                return null;
                            }
                            
                            @Override
                            public Void handleLog(com.igtimi.IgtimiData.Log log) {
                                fixes.add(new Log(TimePoint.of(log.getTimestamp()), getSensor(serialNumber), log.getMessage(), log.getPriority()));
                                return null;
                            }
                        });
                    }
                }
            }
        });
        return fixes;
    }
    
    private Sensor getSensor(String serialNumber) {
        return Sensor.create(serialNumber, /* sub-device */ 0);
    }

}
