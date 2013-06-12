package com.sap.sailing.domain.racelog;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.impl.Util.Triple;

public abstract interface RaceLogFinishPositioningEvent extends RaceLogEvent {
    
    List<Triple<Serializable, String, MaxPointsReason>> getPositionedCompetitors();
}
