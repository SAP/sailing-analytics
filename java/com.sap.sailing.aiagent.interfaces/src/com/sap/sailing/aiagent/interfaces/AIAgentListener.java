package com.sap.sailing.aiagent.interfaces;

import com.sap.sailing.domain.base.Event;
import com.sap.sse.aicore.Credentials;

public interface AIAgentListener {
    void stoppedCommentingOnEvent(Event e);
    void startedCommentingOnEvent(Event e);
    void credentialsUpdated(Credentials credentials);
}
