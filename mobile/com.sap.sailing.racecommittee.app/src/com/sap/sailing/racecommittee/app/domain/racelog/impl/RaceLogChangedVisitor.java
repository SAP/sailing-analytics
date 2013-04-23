package com.sap.sailing.racecommittee.app.domain.racelog.impl;

import com.sap.sailing.domain.racelog.RaceLogCourseAreaChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.domain.racelog.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogPassChangeEvent;
import com.sap.sailing.domain.racelog.RaceLogPathfinderEvent;
import com.sap.sailing.domain.racelog.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.racelog.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogStartTimeEvent;
import com.sap.sailing.racecommittee.app.domain.racelog.RaceLogChangedListener;

public class RaceLogChangedVisitor implements RaceLogEventVisitor {

    private RaceLogChangedListener listener;

    public RaceLogChangedVisitor(RaceLogChangedListener listener) {
        this.listener = listener;
    }

    public void visit(RaceLogFlagEvent event) {
        listener.eventAdded(event);
    }

    public void visit(RaceLogPassChangeEvent event) {
        listener.eventAdded(event);
    }

    public void visit(RaceLogRaceStatusEvent event) {
        listener.eventAdded(event);
    }

    public void visit(RaceLogStartTimeEvent event) {
        listener.eventAdded(event);
    }

    public void visit(RaceLogCourseAreaChangedEvent event) {
        listener.eventAdded(event);
    }

    public void visit(RaceLogCourseDesignChangedEvent event) {
        listener.eventAdded(event);
    }

    @Override
    public void visit(RaceLogFinishPositioningListChangedEvent event) {
        listener.eventAdded(event);
    }

    @Override
    public void visit(RaceLogFinishPositioningConfirmedEvent event) {
        listener.eventAdded(event);        
    }

    @Override
    public void visit(RaceLogPathfinderEvent event) {
        listener.eventAdded(event);
    }

    @Override
    public void visit(RaceLogGateLineOpeningTimeEvent event) {
        listener.eventAdded(event);
    }

    @Override
    public void visit(RaceLogStartProcedureChangedEvent event) {
        listener.eventAdded(event);
    }

}
