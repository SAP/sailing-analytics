package com.sap.sailing.domain.common;

import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.datamining.annotations.Connector;

public interface Moving {
    @Connector(messageKey="")
    SpeedWithBearing getSpeed();
}
