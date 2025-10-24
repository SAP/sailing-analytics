package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sailing.domain.queclinkadapter.impl.ServerAcknowledgementImpl;

public interface ServerAcknowledgement extends NonCommand {
    MessageFactory FACTORY = ServerAcknowledgementImpl::createFromParameters;
    
    @Override
    default Direction getDirection() {
        return Direction.SACK;
    }
}
