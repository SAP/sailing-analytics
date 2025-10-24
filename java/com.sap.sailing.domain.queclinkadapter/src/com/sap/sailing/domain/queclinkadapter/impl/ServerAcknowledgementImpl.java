package com.sap.sailing.domain.queclinkadapter.impl;

import java.text.ParseException;

import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sailing.domain.queclinkadapter.ServerAcknowledgement;

public class ServerAcknowledgementImpl extends MessageImpl implements ServerAcknowledgement {
    private final short countNumber;
    
    public ServerAcknowledgementImpl(short countNumber) {
        super(/* type */ null, Direction.SACK);
        this.countNumber = countNumber;
    }
    
    public static ServerAcknowledgement createFromParameters(String[] parameterList) throws ParseException {
        return new ServerAcknowledgementImpl(MessageParserImpl.parseCountNumberHex(parameterList[0]));
    }

    @Override
    public String getPrefix() {
        return String.format("+%s:", getDirection());
    }

    @Override
    public String[] getParameters() {
        return new String[] { String.format("%4h", countNumber) };
    }
        
    public short getCountNumber() {
        return countNumber;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
