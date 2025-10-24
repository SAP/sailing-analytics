package com.sap.sailing.aiagent.interfaces;

import com.sap.sailing.domain.base.Event;
import com.sap.sse.aicore.AICore;
import com.sap.sse.aicore.ChatSession;
import com.sap.sse.aicore.Credentials;

public interface AIAgent {
    void startCommentingOnEvent(Event event);

    void stopCommentingOnEvent(Event event);

    void stopCommentingOnAllEvents();
    
    void addListener(AIAgentListener listener);
    
    void removeListener(AIAgentListener listener);
    
    Iterable<Event> getCommentingOnEvents();

    String getModelName();

    /**
     * The underlying {@link AICore} facade to SAP AI Core may or may not hold authentication {@link Credentials}.
     * This method tells whether {@link Credentials} are currently set on the {@link AICore} instance. Note, however,
     * that this doesn't tell about their <em>validity</em>.
     */
    boolean hasCredentials();
    
    /**
     * Allows clients to set/update the credentials used for {@link AICore} requests, such as
     * creating the underlying chat session used to generate AI comments for races. Using {@code null}
     * will "unset" the credentials, most likely disallowing the use of the service due to
     * authentication problems. Also, setting the credentials to {@code null} will let
     * {@link #hasCredentials()} return {@code false}.<p>
     * 
     * If non-{@code null} {@code credentials} are provided, a new {@link ChatSession} is created using
     * {@link AICore#createChatSession(com.sap.sse.aicore.Deployment)} after resolving the desired model
     * name again, defaulting to a model with a default name if with the new credentials the desired
     * model cannot be found.
     */
    void setCredentials(Credentials credentials);
}
