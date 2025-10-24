package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.queclinkadapter.impl.FRIReportImpl;

public interface FRIReport extends MessageWithDeviceOrigin, Report {
    MessageFactory FACTORY = FRIReportImpl::createFromParameters;

    Byte getReportId();

    Byte getReportType();

    byte getNumberOfFixes();
    
    /**
     * @return an array of size {@link #getNumberOfFixes()}
     */
    PositionRelatedReport[] getPositionRelatedReports();

    Byte getBatteryPercentage();

    IOStatus getIoStatus();
}
