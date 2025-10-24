package com.sap.sailing.domain.queclinkadapter.impl;

import java.text.ParseException;

import com.sap.sailing.domain.queclinkadapter.HBDAcknowledgement;
import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sse.common.TimePoint;

public class HBDAcknowledgementImpl extends MessageWithDeviceOriginImpl implements HBDAcknowledgement {
    public HBDAcknowledgementImpl(int protocolVersion, short countNumber, String imei, String deviceName, TimePoint sendTime) {
        super(MessageType.HBD, Direction.ACK, protocolVersion, imei, deviceName, sendTime, countNumber);
    }
    
    @Override
    public String[] getParameters() {
        return new String[] {
                MessageParserImpl.formatProtocolVersionHex(getProtocolVersion()),
                getImei(),
                getDeviceName(),
                getSendTime() == null ? "" : MessageParserImpl.formatAsYYYYMMDDHHMMSS(getSendTime()),
                MessageParserImpl.formatCountNumberHex(getCountNumber())
        };
    }
    
    public static HBDAcknowledgement createFromParameters(String[] parameterList) throws ParseException {
        return new HBDAcknowledgementImpl(MessageParserImpl.parseProtocolVersionHex(parameterList[0]),
                MessageParserImpl.parseCountNumberHex(parameterList[4]),
                /* imei */ parameterList[1],
                /* deviceName */ parameterList[2],
                /* sendTime */ MessageParserImpl.parseTimeStamp(parameterList[3]));
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
