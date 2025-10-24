package com.sap.sailing.domain.queclinkadapter;

import java.util.Arrays;

import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sse.common.Util;

public interface Message {
    MessageType getType();

    Direction getDirection();

    String[] getParameters();

    char getTypeSeparator();

    String getPrefix();

    default String getMessageString() {
        return String.format("%s%c%s$", getPrefix(), getTypeSeparator(),
                Util.joinStrings(",", Util.map(Arrays.asList(getParameters()), p->p==null?"":p)));
    }
    
    <T> T accept(MessageVisitor<T> visitor);
}
