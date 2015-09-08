package com.sap.sse.datamining.impl.components.management;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.QueryState;
import com.sap.sse.datamining.components.management.DataMiningQueryManager;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.shared.DataMiningSession;

public abstract class SingleQueryPerKeyManager<T> implements DataMiningQueryManager {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final ConcurrentMap<T, Query<?>> queryMap;

    public SingleQueryPerKeyManager() {
        queryMap = new ConcurrentHashMap<>();
    }

    @Override
    public <ResultType> QueryResult<ResultType> runNewAndAbortPrevious(DataMiningSession session, Query<ResultType> query) {
        if (session == null || query == null) {
            //Forbidding null Queries ensures the functionality of registerNewQuery
            throw new NullPointerException();
        }
        
        Iterable<T> keys = getKeysFor(session, query);
        validate(keys);
        
        abortPreviousQueries(keys);
        registerNewQuery(keys, query);
        logger.info("Running query " + query + ", that has been registered for the keys " + keys);
        QueryResult<ResultType> result = query.run();
        unregisterQuery(keys, query);
        return result;
    }

    protected abstract <ResultType> Iterable<T> getKeysFor(DataMiningSession session, Query<ResultType> query);
    
    private void validate(Iterable<T> keys) {
        int size = 0;
        for (T key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("Null key was created for query.");
            }
            size++;
        }
        
        if (size == 0) {
            throw new IllegalArgumentException("Unable to create keys for query.");
        }
    }
    
    private void abortPreviousQueries(Iterable<T> keys) {
        for (T key : keys) {
            if (queryMap.containsKey(key)) {
                Query<?> previousQuery = queryMap.get(key);
                if (previousQuery.getState() == QueryState.RUNNING) {
                    logger.info("Aborting query " + previousQuery + ", because a new query for the key " + key + " has been requested");
                    previousQuery.abort();
                }
                queryMap.remove(key, previousQuery);
            }
        }
    }

    private <ResultType> void registerNewQuery(Iterable<T> keys, Query<ResultType> query) {
        for (T key : keys) {
            Query<?> previousValue = queryMap.putIfAbsent(key, query);
            if (previousValue != null) {
                throw new UnsupportedOperationException("There's allready a query for the key: " + key);
            }
        }
    }

    private void unregisterQuery(Iterable<T> keys, Query<?> finishedQuery) {
        for (T key : keys) {
            queryMap.remove(key, finishedQuery);
        }
    }

}
