package com.sap.sailing.domain.queclinkadapter.impl;

import java.text.ParseException;

import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sailing.domain.queclinkadapter.PDPReport;
import com.sap.sse.common.TimePoint;

public class PDPReportImpl extends MessageWithDeviceOriginImpl implements PDPReport {
    public PDPReportImpl(int protocolVersion, String imei, String deviceName, TimePoint sendTime, short countNumber) {
        super(MessageType.PDP, Direction.RESP, protocolVersion, imei, deviceName, sendTime, countNumber);
    }

    public static PDPReportImpl createFromParameters(String[] parameterList) throws ParseException {
        return new PDPReportImpl(MessageParserImpl.parseProtocolVersionHex(parameterList[0]),
                /* imei */ parameterList[1], /* deviceName */ parameterList[2],
                /* sendTime */ MessageParserImpl.parseTimeStamp(parameterList[3]),
                MessageParserImpl.parseCountNumberHex(parameterList[4]));
    }

    @Override
    public String[] getParameters() {
        return new String[] { MessageParserImpl.formatProtocolVersionHex(getProtocolVersion()), getImei(),
                getDeviceName(),
                getSendTime() == null ? "" : MessageParserImpl.formatAsYYYYMMDDHHMMSS(getSendTime()),
                MessageParserImpl.formatCountNumberHex(getCountNumber()) };
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
