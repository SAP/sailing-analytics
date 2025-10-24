package com.sap.sailing.domain.queclinkadapter.impl;

import java.text.ParseException;

import com.sap.sailing.domain.queclinkadapter.HBDServerAcknowledgement;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;

public class HBDServerAcknowledgementImpl extends HBDMessageImpl implements HBDServerAcknowledgement {
    public HBDServerAcknowledgementImpl(int protocolVersion, short countNumber) {
        super(Direction.SACK, protocolVersion, countNumber);
    }

    public static HBDServerAcknowledgement createFromParameters(String[] parameterList) throws ParseException {
        return new HBDServerAcknowledgementImpl(MessageParserImpl.parseProtocolVersionHex(parameterList[0]),
                MessageParserImpl.parseCountNumberHex(parameterList[1]));
    }

    @Override
    public String[] getParameters() {
        return new String[] { MessageParserImpl.formatProtocolVersionHex(getProtocolVersion()), MessageParserImpl.formatCountNumberHex(getCountNumber()) };
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
