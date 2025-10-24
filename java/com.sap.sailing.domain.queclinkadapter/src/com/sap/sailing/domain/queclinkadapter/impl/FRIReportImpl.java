package com.sap.sailing.domain.queclinkadapter.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.queclinkadapter.FRIReport;
import com.sap.sailing.domain.queclinkadapter.IOStatus;
import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sailing.domain.queclinkadapter.PositionRelatedReport;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class FRIReportImpl extends MessageWithDeviceOriginImpl implements FRIReport {
    private final Byte reportId;
    private final Byte reportType;
    private final Byte batteryPercentage;
    private final IOStatus ioStatus;
    private final PositionRelatedReport[] positionRelatedReports;

    public FRIReportImpl(int protocolVersion, String imei, String deviceName, Byte reportId, Byte reportType,
            PositionRelatedReport[] positionRelatedReports, Byte batteryPercentage, IOStatus ioStatus,
            TimePoint sendTime, short countNumber) {
        super(MessageType.FRI, Direction.RESP, protocolVersion, imei, deviceName, sendTime, countNumber);
        this.reportId = reportId;
        this.reportType = reportType;
        this.batteryPercentage = batteryPercentage;
        this.ioStatus = ioStatus;
        this.positionRelatedReports = positionRelatedReports;
    }

    public static FRIReportImpl createFromParameters(String[] parameterList) throws ParseException {
        final byte numberOfFixes = Byte.parseByte(parameterList[5]);
        final int FIELDS_PER_FIX = 12;
        final int sendTimeIndex;
        final int countNumberIndex;
        final IOStatus ioStatus;
        if (parameterList.length > 9+numberOfFixes*FIELDS_PER_FIX) {
            // optional field "I/O Status" seems present
            ioStatus = new IOStatus(parameterList[7+numberOfFixes*FIELDS_PER_FIX]);
            sendTimeIndex = 8+numberOfFixes*FIELDS_PER_FIX;
            countNumberIndex = 9+numberOfFixes*FIELDS_PER_FIX;
        } else {
            sendTimeIndex = 7+numberOfFixes*FIELDS_PER_FIX;
            countNumberIndex = 8+numberOfFixes*FIELDS_PER_FIX;
            ioStatus = null;
        }
        final List<PositionRelatedReport> positionRelatedReports = new ArrayList<>();
        for (int i=0; i<numberOfFixes; i++) {
            final PositionRelatedReportImpl positionRelatedReport = new PositionRelatedReportImpl(
                    /* hdop */ Util.hasLength(parameterList[6+i*FIELDS_PER_FIX])?Byte.parseByte(parameterList[6+i*FIELDS_PER_FIX]):null,
                    Util.hasLength(parameterList[7+i*FIELDS_PER_FIX]) && Util.hasLength(parameterList[8+i*FIELDS_PER_FIX]) ? new KilometersPerHourSpeedWithBearingImpl(
                            Double.parseDouble(parameterList[7+i*FIELDS_PER_FIX]), new DegreeBearingImpl(Double.parseDouble(parameterList[8+i*FIELDS_PER_FIX]))) : null,
                    /* altitude */ Util.hasLength(parameterList[9+i*FIELDS_PER_FIX]) ? new MeterDistance(Double.parseDouble(parameterList[9+i*FIELDS_PER_FIX])) : null,
                    Util.hasLength(parameterList[11+i*FIELDS_PER_FIX]) && Util.hasLength(parameterList[10+i*FIELDS_PER_FIX])?new DegreePosition(Double.parseDouble(parameterList[11+i*FIELDS_PER_FIX]), Double.parseDouble(parameterList[10+i*FIELDS_PER_FIX])):null,
                    /* validity time */ MessageParserImpl.parseTimeStamp(parameterList[12+i*FIELDS_PER_FIX]),
                    /* mobileCountryCode */ Util.hasLength(parameterList[13+i*FIELDS_PER_FIX])?Short.parseShort(parameterList[13+i*FIELDS_PER_FIX]):null,
                    /* mobileNetworkCode */ Util.hasLength(parameterList[14+i*FIELDS_PER_FIX])?Short.parseShort(parameterList[14+i*FIELDS_PER_FIX]):null,
                    /* locationAreaCode */ Util.hasLength(parameterList[15+i*FIELDS_PER_FIX])?Integer.parseInt(parameterList[15+i*FIELDS_PER_FIX], 16):null,
                    /* cellId */ Util.hasLength(parameterList[16+i*FIELDS_PER_FIX])?Integer.parseInt(parameterList[16+i*FIELDS_PER_FIX], 16):null,
                    /* odometer */ Util.hasLength(parameterList[17+i*FIELDS_PER_FIX])?new MeterDistance(1000.0*Double.parseDouble(parameterList[17+i*FIELDS_PER_FIX])):null);
            if (positionRelatedReport.getPosition() != null && positionRelatedReport.getValidityTime() != null) {
                positionRelatedReports.add(positionRelatedReport);
            }
        }
        return new FRIReportImpl(MessageParserImpl.parseProtocolVersionHex(parameterList[0]),
                /* imei */ parameterList[1], /* deviceName */ parameterList[2],
                /* reportId */ Util.hasLength(parameterList[3])?Byte.parseByte(parameterList[3]):null,
                /* reportType */ Util.hasLength(parameterList[4])?Byte.parseByte(parameterList[4]):null,
                positionRelatedReports.toArray(new PositionRelatedReport[positionRelatedReports.size()]),
                /* batteryPercentage */ Util.hasLength(parameterList[6+numberOfFixes*FIELDS_PER_FIX])?Byte.parseByte(parameterList[6+numberOfFixes*FIELDS_PER_FIX]):null, ioStatus,
                /* sendTime */ MessageParserImpl.parseTimeStamp(parameterList[sendTimeIndex]),
                MessageParserImpl.parseCountNumberHex(parameterList[countNumberIndex]));
    }

    @Override
    public String[] getParameters() {
        return new String[] { MessageParserImpl.formatProtocolVersionHex(getProtocolVersion()), getImei(),
                getDeviceName(),
                getSendTime() == null ? "" : MessageParserImpl.formatAsYYYYMMDDHHMMSS(getSendTime()),
                MessageParserImpl.formatCountNumberHex(getCountNumber()) };
    }

    @Override
    public Byte getReportId() {
        return reportId;
    }

    @Override
    public Byte getReportType() {
        return reportType;
    }

    @Override
    public byte getNumberOfFixes() {
        return (byte) positionRelatedReports.length;
    }

    @Override
    public PositionRelatedReport[] getPositionRelatedReports() {
        return positionRelatedReports;
    }

    @Override
    public Byte getBatteryPercentage() {
        return batteryPercentage;
    }

    @Override
    public IOStatus getIoStatus() {
        return ioStatus;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
