package com.sap.sailing.domain.base.impl;

import java.io.Serializable;

import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.racelog.RaceLogCourseAreaChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.domain.racelog.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogPassChangeEvent;
import com.sap.sailing.domain.racelog.RaceLogPathfinderEvent;
import com.sap.sailing.domain.racelog.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.racelog.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogStartTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogWindFixEvent;

public class RaceColumnRaceLogReplicator implements RaceLogEventVisitor, Serializable {
    private static final long serialVersionUID = 4752330236765192592L;
    
    private final RaceColumn raceColumn;
    private final RaceLogIdentifier identifier;
    
    public RaceColumnRaceLogReplicator(RaceColumn raceColumn, RaceLogIdentifier identifier) {
        this.raceColumn = raceColumn;
        this.identifier = identifier;
    }
        
    private void notifyOnAdd(RaceLogEvent event) {
        raceColumn.getRaceColumnListeners().notifyListenersAboutRaceLogEventAdded(
                raceColumn, 
                identifier, 
                event);
    }
    
    @Override
    public void visit(RaceLogCourseAreaChangedEvent event) {
        notifyOnAdd(event);
    }
    
    @Override
    public void visit(RaceLogStartTimeEvent event) {
        notifyOnAdd(event);
    }
    
    @Override
    public void visit(RaceLogRaceStatusEvent event) {
        notifyOnAdd(event);
    }
    
    @Override
    public void visit(RaceLogPassChangeEvent event) {
        notifyOnAdd(event);
    }
    
    @Override
    public void visit(RaceLogFlagEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogCourseDesignChangedEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogFinishPositioningListChangedEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogFinishPositioningConfirmedEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogPathfinderEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogGateLineOpeningTimeEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogStartProcedureChangedEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogProtestStartTimeEvent event) {
        notifyOnAdd(event);
    }

    @Override
    public void visit(RaceLogWindFixEvent event) {
        notifyOnAdd(event);
    }
}
