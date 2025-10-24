package com.sap.sailing.aiagent.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.json.simple.parser.ParseException;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sailing.aiagent.interfaces.AIAgentListener;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventListener;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.common.tagging.TagAlreadyExistsException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupListener;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.TaggingService;
import com.sap.sse.aicore.AICore;
import com.sap.sse.aicore.ChatSession;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.Deployment;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.shared.util.WeakValueCache;

public class AIAgentImpl implements AIAgent {
    private static final Logger logger = Logger.getLogger(AIAgentImpl.class.getName());
    
    private static final String DEFAULT_MODEL_NAME = "o4-mini";

    private static final String SAP_AI_CORE_TAG = "SAP AI Core on %s";
    
    private final ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;
    
    private final String desiredModelName;
    
    private String modelName;
    
    private final String systemPrompt;
    
    private ChatSession chatSession;
    
    private final ConcurrentMap<Leaderboard, RaceColumnListener> raceColumnListeners;
    
    private final ConcurrentMap<Event, EventListener> eventListeners;
    
    /**
     * A concurrent set to be backed by a {@link ConcurrentHashMap}.
     */
    private final Set<AIAgentListener> listeners;
    
    private final ConcurrentMap<Triple<String, String, String>, Set<String>> tagIdentifiersCurrentlyBeingAddedToRace;
    
    /**
     * To be accessed only through the {@code synchronized} methods {@link #lockRaceForCommenting(String, String, String)} and
     * {@link #unlockRaceAfterCommenting(String, String, String)}.
     */
    private final WeakValueCache<Triple<String, String, String>, NamedReentrantReadWriteLock> locks;

    private final AICore aiCore;

    /**
     * @param desiredModelName may be {@code null}, leading to the use of a model named according to {@link #DEFAULT_MODEL_NAME}.
     */
    public AIAgentImpl(ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker, AICore aiCore,
            String desiredModelName, String systemPrompt) throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException {
        super();
        this.aiCore = aiCore;
        this.desiredModelName = desiredModelName;
        this.systemPrompt = systemPrompt;
        this.tagIdentifiersCurrentlyBeingAddedToRace = new ConcurrentHashMap<>();
        this.raceColumnListeners = new ConcurrentHashMap<>();
        this.eventListeners = new ConcurrentHashMap<>();
        this.racingEventServiceTracker = racingEventServiceTracker;
        this.chatSession = createChatSession();
        this.locks = new WeakValueCache<>(new HashMap<>());
        this.listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }
    
    /**
     * Using the {@link #aiCore} facade to SAP AI Core, obtains the {@link Deployment}s available and tries to find
     * one that has the {@link #desiredModelName}. If not, it defaults to a model named according to {@link #DEFAULT_MODEL_NAME}.
     * This deployment is then used to create a new {@link ChatSession} which is then returned.
     */
    private ChatSession createChatSession() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final ChatSession result;
        if (aiCore.hasCredentials()) {
            final Map<String, Set<Deployment>> deploymentsByModelName = new HashMap<>();
            aiCore.getDeployments().forEach(d->Util.addToValueSet(deploymentsByModelName, d.getModelName(), d));
            logger.info("Found AI models "+deploymentsByModelName.keySet());
            if (desiredModelName == null || deploymentsByModelName.get(desiredModelName) == null || deploymentsByModelName.get(desiredModelName).isEmpty()) {
                logger.warning("Couldn't find model "+desiredModelName+"; defaulting to "+DEFAULT_MODEL_NAME);
                modelName = DEFAULT_MODEL_NAME;
            } else {
                logger.info("Found model "+desiredModelName);
                modelName = desiredModelName;
            }
            final Set<Deployment> deployments = deploymentsByModelName.get(modelName);
            final Deployment deployment = deployments.iterator().next();
            result = aiCore.createChatSession(deployment);
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public boolean hasCredentials() {
        return aiCore.hasCredentials();
    }
    
    @Override
    public void setCredentials(Credentials credentials) {
        aiCore.setCredentials(credentials);
        if (credentials != null) {
            try {
                chatSession = createChatSession();
            } catch (UnsupportedOperationException | URISyntaxException | IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            chatSession = null;
            modelName = null;
        }
        listeners.forEach(l->l.credentialsUpdated(credentials));
    }

    private RacingEventService getRacingEventService() {
        return racingEventServiceTracker.getService();
    }
    
    @Override
    public void addListener(AIAgentListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(AIAgentListener listener) {
        listeners.remove(listener);
    }

    /**
     * Checks if a tag with the {@code tagIdentifier} is already found on the race identified by
     * {@code leaderboardName}, {@code raceColumnName} and {@code fleetName}; if not, the prompt is sent to a new chat
     * session created with the LLM identified by {@link #desiredModelName}, and a new tag is added to that race using the
     * response received.
     * 
     * @param tagIdentifier
     *            used to check if there already is a tag with equal identifier; this is encoded in a tag's
     *            {@link TagDTO#getHiddenInfo() hidden info}.
     */
    public void produceCommentFromPrompt(String tag, final String prompt, final String leaderboardName,
            String raceColumnName, String fleetName, TimePoint raceTimepoint, String tagIdentifier)
            throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException,
            ParseException, RaceLogNotFoundException, ServiceNotFoundException {
        if (chatSession != null && hasCredentials()) {
            final NamedReentrantReadWriteLock lock = lockRaceForCommenting(leaderboardName, raceColumnName, fleetName);
            try {
                if (!raceHasOrIsAboutToGetTag(leaderboardName, raceColumnName, fleetName, tagIdentifier)) {
                    raceIsAboutToGetTag(leaderboardName, raceColumnName, fleetName, tagIdentifier);
                    chatSession
                        .addSystemPrompt(systemPrompt)
                        .addPrompt(prompt)
                        .setTemperature(1.0)
                        .submit(response->{
                                try {
                                    getRacingEventService().getTaggingService().addTag(leaderboardName, raceColumnName, fleetName,
                                            String.format(SAP_AI_CORE_TAG, tag), response, tagIdentifier,
                                            "/images/AI_generated_R_blk.png", /* resizedImageURL */ null, /* visibleForPublic */ true, raceTimepoint);
                                } catch (AuthorizationException | IllegalArgumentException
                                        | RaceLogNotFoundException | ServiceNotFoundException
                                        | TagAlreadyExistsException e) {
                                    logger.log(Level.SEVERE, "Error trying to add AI comment to leaderboard "+leaderboardName+", race column "+raceColumnName+", fleet "+fleetName, e);
                                } finally {
                                    final NamedReentrantReadWriteLock lock2 = lockRaceForCommenting(leaderboardName, raceColumnName, fleetName);
                                    try {
                                        raceIsNoLongerAboutToGetTag(leaderboardName, raceColumnName, fleetName, tagIdentifier);
                                    } finally {
                                        unlockRaceAfterCommenting(lock2, leaderboardName, raceColumnName, fleetName);
                                    }
                                }
                            },
                            /* exception handler */ Optional.of(ex->{
                                final NamedReentrantReadWriteLock lock3 = lockRaceForCommenting(leaderboardName, raceColumnName, fleetName);
                                try {
                                    raceIsNoLongerAboutToGetTag(leaderboardName, raceColumnName, fleetName, tagIdentifier);
                                } finally {
                                    unlockRaceAfterCommenting(lock3, leaderboardName, raceColumnName, fleetName);
                                }
                                logger.log(Level.SEVERE, "Error trying to generate AI comment", ex);
                            }));
                }
            } finally {
                unlockRaceAfterCommenting(lock, leaderboardName, raceColumnName, fleetName);
            }
        } else {
            logger.fine(()->"Trying to produce a comment skipped due to missing AI Core credentials");
        }
    }

    private void raceIsAboutToGetTag(String leaderboardName, String raceColumnName, String fleetName, String tagIdentifier) {
        Util.addToValueSet(tagIdentifiersCurrentlyBeingAddedToRace, new Triple<>(leaderboardName, raceColumnName, fleetName), tagIdentifier);
    }   
    
    private void raceIsNoLongerAboutToGetTag(String leaderboardName, String raceColumnName, String fleetName, String tagIdentifier) {
        Util.removeFromValueSet(tagIdentifiersCurrentlyBeingAddedToRace, new Triple<>(leaderboardName, raceColumnName, fleetName), tagIdentifier);
    }

    private boolean raceHasOrIsAboutToGetTag(final String leaderboardName, String raceColumnName, String fleetName,
            String tagIdentifier) throws RaceLogNotFoundException, ServiceNotFoundException {
        final Triple<String, String, String> key = new Triple<>(leaderboardName, raceColumnName, fleetName);
        return (tagIdentifiersCurrentlyBeingAddedToRace.containsKey(key) &&
                tagIdentifiersCurrentlyBeingAddedToRace.get(key).contains(tagIdentifier))
                ||
                getRacingEventService().getTaggingService().getTags(leaderboardName, raceColumnName, fleetName, /* searchSince */ null, /* returnRevokedTags */ false)
                .stream().anyMatch(existingTag->Util.equalsWithNull(existingTag.getHiddenInfo(), tagIdentifier));
    }
    
    /**
     * To be called before starting to check the {@link TaggingService} for the presence of a tag with a
     * {@link TagDTO#getHiddenInfo() hidden info / identifier} because some concurrent attempt to comment may just be in
     * between checking and updating the tags on that race.
     * <p>
     * 
     * Callers <em>must</em> make sure to call {@link #unlockRaceAfterCommenting(String, String, String)} in a
     * <tt>finally</tt> clause to avoid deadlocks at all cost.
     * <p>
     * 
     * If another thread currently holds the lock for the race identified by {@code leaderboardName},
     * {@code raceColumnName}, and {@code fleetName}, this method will block; when the other thread then
     * {@link #unlockRaceAfterCommenting(String, String, String) releases its lock}, one other thread
     * waiting for the lock will be unblocked, etc.
     * @return 
     */
    private NamedReentrantReadWriteLock lockRaceForCommenting(final String leaderboardName, String raceColumnName, String fleetName) {
        final NamedReentrantReadWriteLock lock;
        final Triple<String, String, String> lockKey = getLockKey(leaderboardName, raceColumnName, fleetName);
        synchronized (this) {
            final NamedReentrantReadWriteLock existingLock = locks.get(lockKey);
            if (existingLock == null) {
                lock = new NamedReentrantReadWriteLock("AI comments for "+leaderboardName+"/"+raceColumnName+"/"+fleetName, /* fair */ false);
                locks.put(lockKey, lock);
            } else {
                lock = existingLock;
            }
            logger.fine(()->"Locking "+lock.writeLock()+" in thread "+Thread.currentThread()+" for key "+lockKey);
        }
        LockUtil.lockForWrite(lock);
        return lock;
    }

    private Triple<String, String, String> getLockKey(final String leaderboardName, String raceColumnName,
            String fleetName) {
        return new Triple<>(leaderboardName, raceColumnName, fleetName);
    }
    
    private synchronized void unlockRaceAfterCommenting(NamedReentrantReadWriteLock lock, final String leaderboardName, String raceColumnName, String fleetName) {
        final Triple<String, String, String> lockKey = getLockKey(leaderboardName, raceColumnName, fleetName);
        logger.fine(()->"Unlocking "+lock.writeLock()+" in thread "+Thread.currentThread()+" for key "+lockKey);
        LockUtil.unlockAfterWrite(lock);
    }
    
    @Override
    public void startCommentingOnEvent(final Event event) {
        final LeaderboardGroupListener lgListener = new LeaderboardGroupListenerImpl(this);
        final EventListener eventListener = new EventListener() {
            @Override
            public void leaderboardGroupAdded(Event event, LeaderboardGroup leaderboardGroup) {
                for (final Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    if (leaderboard.isPartOfEvent(event)) {
                        addNewRaceColumnListenerToLeaderboard(leaderboard);
                    }
                }
                leaderboardGroup.addLeaderboardGroupListener(lgListener);
            }

            @Override
            public void leaderboardGroupRemoved(Event event, LeaderboardGroup leaderboardGroup) {
                leaderboardGroup.removeLeaderboardGroupListener(lgListener);
                for (final Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    removeRaceColumnListenerFromLeaderboard(leaderboard);
                }
            }
        };
        event.addEventListener(eventListener);
        eventListeners.put(event, eventListener);
        for (final Leaderboard leaderboard : event.getLeaderboards()) {
            addNewRaceColumnListenerToLeaderboard(leaderboard);
        }
        logger.info("User "+SecurityUtils.getSubject().getPrincipal()+" activated AI comments for event "+event.getName()+" with ID "+event.getId());
        listeners.forEach(l->l.startedCommentingOnEvent(event));
    }

    void addNewRaceColumnListenerToLeaderboard(final Leaderboard leaderboard) {
        final RaceColumnListener raceColumnListenerForLeaderboard = new RaceColumnListener(leaderboard, this);
        raceColumnListeners.put(leaderboard, raceColumnListenerForLeaderboard);
        leaderboard.addRaceColumnListener(raceColumnListenerForLeaderboard);
    }
    
    void removeRaceColumnListenerFromLeaderboard(final Leaderboard leaderboard) {
        final RaceColumnListener listener = raceColumnListeners.remove(leaderboard);
        if (listener != null) {
            listener.removeListener();
        }
    }
    
    @Override
    public void stopCommentingOnEvent(Event event) {
        final EventListener eventListener = eventListeners.remove(event);
        if (eventListener != null) {
            event.removeEventListener(eventListener);
            for (final Leaderboard leaderboard : event.getLeaderboards()) {
                final RaceColumnListener raceColumnListener = raceColumnListeners.get(leaderboard);
                if (raceColumnListener != null) {
                    raceColumnListener.removeListener();
                }
            }
        }
        logger.info("User "+SecurityUtils.getSubject().getPrincipal()+" de-activated AI comments for event "+event.getName()+" with ID "+event.getId());
        listeners.forEach(l->l.stoppedCommentingOnEvent(event));
    }
    
    @Override
    public void stopCommentingOnAllEvents() {
        for (final Event event : new HashSet<>(eventListeners.keySet())) {
            stopCommentingOnEvent(event);
        }
    }

    @Override
    public Iterable<Event> getCommentingOnEvents() {
        return Collections.unmodifiableCollection(eventListeners.keySet());
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
}
