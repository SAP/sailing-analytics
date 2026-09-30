package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Position;

public class Request1TurnerDTO implements IsSerializable {

    public SimulatorUISelectionDTO selection = null;
    public Position oldMovedPoint = null;
    public Position newMovedPoint = null;
    public Position beforeMovedPoint = null;
    public Position edgeStart = null;
    public Position edgeEnd = null;

    public Long oldMovedPointTimePoint = 0L;
    public Double startToEndBearingDegrees = 0.0;

    public Request1TurnerDTO() {
        this.selection = null;

        this.oldMovedPoint = null;
        this.newMovedPoint = null;
        this.beforeMovedPoint = null;
        this.edgeStart = null;
        this.edgeEnd = null;

        this.oldMovedPointTimePoint = 0L;
        this.startToEndBearingDegrees = 0.0;

    }

    public Request1TurnerDTO(SimulatorUISelectionDTO selection, Position oldMovedPoint, Position newMovedPoint, Position beforeMovedPoint,
            Position edgeStart, Position edgeEnd,
            Long oldMovedPointTimePoint, Double startToEndBearingDegrees) {

        this.selection = selection;

        this.oldMovedPoint = oldMovedPoint;
        this.newMovedPoint = newMovedPoint;
        this.beforeMovedPoint = beforeMovedPoint;
        this.edgeStart = edgeStart;
        this.edgeEnd = edgeEnd;

        this.oldMovedPointTimePoint = oldMovedPointTimePoint;
        this.startToEndBearingDegrees = startToEndBearingDegrees;
    }
}
