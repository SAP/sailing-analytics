package com.sap.sailing.aiagent.impl;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.aiagent.impl.rules.Rule;
import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.aicore.AICore;
import com.sap.sse.aicore.ChatSession;
import com.sap.sse.util.ServiceTrackerFactory;

/**
 * When activated, starts to track the {@link RacingEventService} using an OSGi {@link ServiceTracker} and maintains an
 * {@link AICore} instance to obtain {@link ChatSession}s from. It creates and registers an {@link AIAgent} with the
 * service registry. The agent can then be told to monitor one or more {@link Event}(s) or {@link Regatta}s and will
 * then attach itself as a {@link RaceChangeListener} to the races contained.
 * <p>
 * 
 * Note: {@link Rule} "registrations" currently happen through code in the
 * {@link RaceListener#RaceListener(AIAgentImpl, com.sap.sailing.domain.leaderboard.Leaderboard, com.sap.sailing.domain.base.RaceColumn, com.sap.sailing.domain.base.Fleet, com.sap.sailing.domain.tracking.TrackedRace)}
 * constructor.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class Activator implements BundleActivator {
    private static final String MODEL_NAME_SYSTEM_PROPERTY_NAME = "sap.sailing.aiagent.modelname";

    private static final Logger logger = Logger.getLogger(Activator.class.getName());

    private static BundleContext context;
    private static Activator instance;
    
    private static final String SYSTEM_PROMPT =
            "You are an experienced sailing commentator with your own sailing history. " +
            "You have been hired to write intelligent live commentary for people who follow the race tracking. " +
            "Your comments will appear in a side bar of a race viewer and therefore have to be exciting, yet very concise. " +
            "Make good use also of information about prior races to put the live performance into context.";
    
    private ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;
    private AIAgent aiAgent;

    public Activator() {
        instance = this;
    }
    
    static BundleContext getContext() {
        return context;
    }
    
    public static Activator getInstance() {
        return instance;
    }

    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        racingEventServiceTracker = ServiceTrackerFactory.createAndOpen(context, RacingEventService.class);
        final AICore aiCore = AICore.getDefault();
        if (aiCore != null) { // since we now also support AICore instances without Credentials, this case is the most likely
            aiAgent = new AIAgentImpl(racingEventServiceTracker, aiCore, System.getProperty(MODEL_NAME_SYSTEM_PROPERTY_NAME), SYSTEM_PROMPT);
            logger.info("Created AI Agent "+aiAgent);
            bundleContext.registerService(AIAgent.class, aiAgent, /* properties */ null);
        } else {
            logger.warning("Didn't find any AICore service; is the com.sap.sse.aicore bundle missing?");
        }
    }
    
    public RacingEventService getRacingEventService() {
        return racingEventServiceTracker.getService();
    }
    
    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
    }
}
