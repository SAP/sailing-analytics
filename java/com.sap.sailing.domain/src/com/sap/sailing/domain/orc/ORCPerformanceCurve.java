package com.sap.sailing.domain.orc;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;

public interface ORCPerformanceCurve {

    Speed getImpliedWind(Duration time);
    
    Duration getCalculatedTime(ORCPerformanceCurve referenceBoat);
 
    Duration getAllowancePerCourse(Speed impliedWind);
}
