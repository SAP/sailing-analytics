package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;

public interface Report extends NonCommand {
    @Override
    default Direction getDirection() {
        return Direction.RESP;
    }
}
