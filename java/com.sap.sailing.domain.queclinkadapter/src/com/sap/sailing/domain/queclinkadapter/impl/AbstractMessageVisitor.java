package com.sap.sailing.domain.queclinkadapter.impl;

import com.sap.sailing.domain.queclinkadapter.FRIReport;
import com.sap.sailing.domain.queclinkadapter.HBDAcknowledgement;
import com.sap.sailing.domain.queclinkadapter.HBDServerAcknowledgement;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sailing.domain.queclinkadapter.PDPReport;
import com.sap.sailing.domain.queclinkadapter.ServerAcknowledgement;

public class AbstractMessageVisitor<T> implements MessageVisitor<T> {

    @Override
    public T visit(FRIReport friReport) {
        return null;
    }

    @Override
    public T visit(HBDAcknowledgement hbdAcknowledgement) {
        return null;
    }

    @Override
    public T visit(HBDServerAcknowledgement hbdServerAcknowledgement) {
        return null;
    }

    @Override
    public T visit(ServerAcknowledgement serverAcknowledgement) {
        return null;
    }

    @Override
    public T visit(PDPReport pdpReportImpl) {
        return null;
    }
}
