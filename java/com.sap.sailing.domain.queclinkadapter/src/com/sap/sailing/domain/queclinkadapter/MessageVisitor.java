package com.sap.sailing.domain.queclinkadapter;

public interface MessageVisitor<T> {

    T visit(FRIReport friReport);

    T visit(HBDAcknowledgement hbdAcknowledgement);

    T visit(HBDServerAcknowledgement hbdServerAcknowledgement);

    T visit(ServerAcknowledgement serverAcknowledgement);

    T visit(PDPReport pdpReportImpl);
}
