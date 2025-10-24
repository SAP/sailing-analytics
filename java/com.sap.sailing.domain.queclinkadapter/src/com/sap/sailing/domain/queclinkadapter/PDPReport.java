package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.queclinkadapter.impl.PDPReportImpl;

public interface PDPReport extends MessageWithDeviceOrigin, Report {
    MessageFactory FACTORY = PDPReportImpl::createFromParameters;
}
