package com.sap.sailing.domain.igtimiadapter.datatypes;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.TimePoint;

public class Log extends Fix {
    private static final long serialVersionUID = 1105936972187917331L;
    private final String logMessage;
    private final int priority;
    
    public Log(TimePoint timePoint, Sensor sensor, String logMessage, int priority) {
        super(sensor, timePoint);
        this.logMessage = logMessage;
        this.priority = priority;
    }

    public String getLogMessage() {
        return logMessage;
    }
    
    public int getPriority() {
        return priority;
    }

    @Override
    protected String localToString() {
        return "LOG: "+getLogMessage();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
