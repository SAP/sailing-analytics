package com.sap.sailing.aiagent.persistence.impl;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sailing.aiagent.interfaces.AIAgentListener;
import com.sap.sailing.aiagent.persistence.DomainObjectFactory;
import com.sap.sailing.aiagent.persistence.MongoObjectFactory;
import com.sap.sailing.aiagent.persistence.PersistenceFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.common.Duration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());

    private static final Duration TIMEOUT_WAITING_FOR_AI_AGENT = Duration.ONE_MINUTE.times(5);

    private static BundleContext context;

    static BundleContext getContext() {
        return context;
    }

    public void start(BundleContext bundleContext) throws Exception {
        for (CollectionNames name : CollectionNames.values()) {
            MongoDBService.INSTANCE.registerExclusively(CollectionNames.class, name.name());
        }
        new Thread(()->{
            final ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker = ServiceTrackerFactory.createAndOpen(bundleContext, RacingEventService.class);
            final ServiceTracker<AIAgent, AIAgent> aiAgentTracker = ServiceTrackerFactory.createAndOpen(bundleContext, AIAgent.class);
            try {
                final AIAgent aiAgent = aiAgentTracker.waitForService(TIMEOUT_WAITING_FOR_AI_AGENT.asMillis());
                if (aiAgent != null) {
                    final RacingEventService racingEventService = racingEventServiceTracker.waitForService(0);
                    logger.info("Found service "+aiAgent+"; initializing it from the persistent configuration data");
                    aiAgent.stopCommentingOnAllEvents();
                    final DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory();
                    final MongoObjectFactory mof = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory();
                    final Credentials credentials = dof.getCredentials(/* clientSession */ null);
                    if (credentials != null) {
                        logger.info("Found AI Core credentials in persistent store; applying to AI Agent");
                        aiAgent.setCredentials(credentials);
                    }
                    for (final Event eventToCommentOn : racingEventService.getEventsSelectively(/* include */ true, dof.getEventsToComment(/* clientSessionOrNull */ null))) {
                        aiAgent.startCommentingOnEvent(eventToCommentOn);
                    }
                    aiAgent.addListener(new AIAgentListener() {
                        @Override
                        public void stoppedCommentingOnEvent(Event e) {
                            mof.removeEventToComment(e.getId(), /* clientSessionOrNull */ null);
                        }
    
                        @Override
                        public void startedCommentingOnEvent(Event e) {
                            mof.addEventToComment(e.getId(), /* clientSessionOrNull */ null);
                        }
                        
                        @Override
                        public void credentialsUpdated(Credentials credentials) {
                            mof.updateCredentials(credentials, /* clientSessionOrNull */ null);
                        }
                    });
                } else {
                    logger.warning("Waiting for AI Agent to show up in OSGi registry timed out after "+TIMEOUT_WAITING_FOR_AI_AGENT);
                }
            } catch (InterruptedException e) {
                logger.severe("Got interrupted while waiting for AIAgent service");
                throw new RuntimeException(e);
            }
        }, "Waiting for AIAgent to show up in OSGi registry").start();
        Activator.context = bundleContext;
    }

    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
    }
}
