package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.queclinkadapter.impl.HBDAcknowledgementImpl;

public interface HBDAcknowledgement extends HBDMessage, MessageWithDeviceOrigin, Acknowledgement {
    MessageFactory FACTORY = HBDAcknowledgementImpl::createFromParameters;
}
