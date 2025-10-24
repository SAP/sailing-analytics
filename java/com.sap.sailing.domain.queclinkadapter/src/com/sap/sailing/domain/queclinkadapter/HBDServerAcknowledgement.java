package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.queclinkadapter.impl.HBDServerAcknowledgementImpl;

public interface HBDServerAcknowledgement extends HBDMessage, ServerAcknowledgement {
    MessageFactory FACTORY = HBDServerAcknowledgementImpl::createFromParameters;
}
