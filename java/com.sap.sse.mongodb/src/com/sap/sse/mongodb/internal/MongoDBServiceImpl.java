package com.sap.sse.mongodb.internal;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.BsonArray;
import org.bson.BsonDocument;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ConnectionString;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sap.sse.mongodb.AlreadyRegisteredException;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;

public class MongoDBServiceImpl implements MongoDBService {

    private static final Logger logger = Logger.getLogger(MongoDBServiceImpl.class.getName());

    private MongoDBConfiguration configuration;

    private final ConcurrentMap<ConnectionString, MongoClient> mongos;
    
    private final ConcurrentMap<ConnectionString, MongoDatabase> dbs;
    
    private final Map<ClientSession, Boolean> sessionsToRefresh;
    
    private Timer timerForSessionRefresh;

    /**
     * collection name -> fully qualified class name
     */
    private final Map<String, String> registered;

    public MongoDBServiceImpl() {
        mongos = new ConcurrentHashMap<>();
        dbs = new ConcurrentHashMap<>();
        registered = new HashMap<String, String>();
        sessionsToRefresh = Collections.synchronizedMap(new WeakHashMap<>());
    }

    public MongoDBServiceImpl(MongoDBConfiguration configuration) {
        this();
        setConfiguration(configuration);
    }

    public MongoDBConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MongoDBConfiguration configuration) {
        this.configuration = configuration;
        logger.info("Used Mongo configuration: "+configuration.getMongoClientURI());
    }

    @Override
    public MongoDatabase getDB() {
        ensureConfigurationDefaultingToTest();
        try {
            return getDB(configuration);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public ClientSession startCausallyConsistentSession() {
        ensureConfigurationDefaultingToTest();
        return getMongo(getConfiguration()).startSession(ClientSessionOptions.builder().causallyConsistent(true).build());
    }
    
    @Override
    public ClientSession startAutoRefreshingSession() {
        ensureConfigurationDefaultingToTest();
        final ClientSession result = getMongo(getConfiguration()).startSession();
        synchronized (sessionsToRefresh) {
            logger.fine("Adding session with ID "+result.getServerSession().getIdentifier()+" to set of sessions to refresh");
            sessionsToRefresh.put(result, true);
            ensureTimerRunning();
            timerForSessionRefresh.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (sessionsToRefresh) {
                        if (sessionsToRefresh.isEmpty()) {
                            logger.fine("Terminating session refresh timer");
                            cancel();
                            timerForSessionRefresh.cancel();
                            timerForSessionRefresh = null;
                        } else {
                            final BsonArray sessionIdsAsList = new BsonArray();
                            for (final ClientSession session : sessionsToRefresh.keySet()) {
                                final BsonDocument sessionId = session.getServerSession().getIdentifier();
                                sessionIdsAsList.add(sessionId);
                            }
                            logger.fine("Refreshing sessions "+sessionIdsAsList);
                            getDB().runCommand(new BsonDocument("refreshSessions", sessionIdsAsList));
                        }
                    }
                }
            }, SESSION_REFERSH_INTERVAL.asMillis(), SESSION_REFERSH_INTERVAL.asMillis());
        }
        return result;
    }

    private void ensureTimerRunning() {
        synchronized (sessionsToRefresh) {
            if (timerForSessionRefresh == null) {
                logger.fine("Launching session refresh timer");
                timerForSessionRefresh = new Timer(/* isDaemon */ true);
            }
        }
    }

    private void ensureConfigurationDefaultingToTest() {
        if (configuration == null) {
            configuration = MongoDBConfiguration.getDefaultTestConfiguration();
            logger.info("Used default Mongo configuration: "+configuration.getMongoClientURI());
        }
    }
    
    private synchronized MongoDatabase getDB(MongoDBConfiguration mongoDBConfiguration) throws UnknownHostException {
        final ConnectionString connectionString = mongoDBConfiguration.getMongoClientURI();
        return dbs.computeIfAbsent(connectionString,
                k->getMongo(mongoDBConfiguration).getDatabase(mongoDBConfiguration.getMongoClientURI().getDatabase()));
    }

    private MongoClient getMongo(MongoDBConfiguration mongoDBConfiguration) {
        return getMongo(mongoDBConfiguration.getMongoClientURI());
    }

    @Override
    public MongoClient getMongo(ConnectionString mongoConnectionString) {
        MongoClient mongo = mongos.computeIfAbsent(mongoConnectionString, k-> MongoClients.create(mongoConnectionString));
        return mongo;
    }

    @Override
    public ConnectionString getMongoClientURI() {
        ensureConfigurationDefaultingToTest();
        return configuration.getMongoClientURI();
    }

    @Override
    public MongoClient getMongoClient() {
        ensureConfigurationDefaultingToTest();
        return getMongo(getConfiguration());
    }
    
    @Override
    public void registerExclusively(Class<?> registerForInterface, String collectionName)
            throws AlreadyRegisteredException {
        String fullyQualified = registerForInterface.getName();
        if (registered.keySet().contains(collectionName) && registered.get(collectionName) != fullyQualified) {
            logger.log(Level.SEVERE, "Same collection name (" + collectionName
                    + " is required in two different places - this may lead to problems: \n"
                    + " - already registered for: " + registered.get(collectionName) + "\n"
                    + " - tried to register for: " + fullyQualified);
            throw new AlreadyRegisteredException();
        }
        logger.log(Level.INFO, "Registered collection name: " + collectionName);
        registered.put(collectionName, fullyQualified);
    }
}
