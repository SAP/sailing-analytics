package com.sap.sse.datamining.impl.components.management;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.AdditionalQueryData;
import com.sap.sse.datamining.DataMiningServer;
import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.QueryState;
import com.sap.sse.datamining.components.management.DataMiningQueryManager;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.factories.FunctionFactory;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.AdditionalDimensionValuesQueryData;
import com.sap.sse.datamining.impl.AdditionalOtherQueryData;
import com.sap.sse.datamining.impl.AdditionalStatisticQueryData;
import com.sap.sse.datamining.shared.DataMiningSession;
import com.sap.sse.datamining.shared.impl.UUIDDataMiningSession;
import com.sap.sse.datamining.test.data.Test_HasLegOfCompetitorContext;
import com.sap.sse.datamining.test.data.Test_HasRaceContext;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;

public class TestServerQueryManagement {
    
    private static final FunctionFactory functionFactory = new FunctionFactory();
    private DataMiningServer server;
    
    @BeforeEach
    public void initializeServer() {
        server = TestsUtil.createNewServer();
    }
    
    @Test
    public void testStatisticQueryManagementWithoutConflict() {
        DataMiningSession firstQuerySession = new UUIDDataMiningSession(UUID.randomUUID());
        DataMiningSession secondQuerySession = new UUIDDataMiningSession(UUID.randomUUID());
        AdditionalQueryData additionalData = new AdditionalStatisticQueryData();
        
        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        runQueriesAndVerifyNoConflict(firstQuerySession, firstQuery, secondQuerySession, secondQuery);
    }
    
    @Test
    public void testDimensionValuesQueryManagementWithoutConflictsDueToDifferentDimensions() {
        DataMiningSession session = new UUIDDataMiningSession(UUID.randomUUID());

        Collection<Function<?>> firstQueryDimensions = new ArrayList<>();
        Method getYear = FunctionTestsUtil.getMethodFromClass(Test_HasRaceContext.class, "getYear");
        firstQueryDimensions.add(functionFactory.createMethodWrappingFunction(getYear));
        AdditionalQueryData firstQueryAdditionalData = new AdditionalDimensionValuesQueryData(firstQueryDimensions);
        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, firstQueryAdditionalData);

        Collection<Function<?>> secondQueryDimensions = new ArrayList<>();
        Method getLegNumber = FunctionTestsUtil.getMethodFromClass(Test_HasLegOfCompetitorContext.class, "getLegNumber");
        secondQueryDimensions.add(functionFactory.createMethodWrappingFunction(getLegNumber));
        AdditionalQueryData secondQueryAdditionalData = new AdditionalDimensionValuesQueryData(secondQueryDimensions);
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, secondQueryAdditionalData);
        
        runQueriesAndVerifyNoConflict(session, firstQuery, session, secondQuery);
    }

    @Test
    public void testDimensionValuesQueryManagementWithoutConflictsDueToDifferentSessions() {
        DataMiningSession firstQuerySession = new UUIDDataMiningSession(UUID.randomUUID());
        DataMiningSession secondQuerySession = new UUIDDataMiningSession(UUID.randomUUID());
        Collection<Function<?>> dimensions = new ArrayList<>();
        Method getYear = FunctionTestsUtil.getMethodFromClass(Test_HasRaceContext.class, "getYear");
        dimensions.add(functionFactory.createMethodWrappingFunction(getYear));
        
        AdditionalQueryData firstQueryAdditionalData = new AdditionalDimensionValuesQueryData(dimensions);
        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, firstQueryAdditionalData);
        AdditionalQueryData secondQueryAdditionalData = new AdditionalDimensionValuesQueryData(dimensions);
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, secondQueryAdditionalData);
        
        runQueriesAndVerifyNoConflict(firstQuerySession, firstQuery, secondQuerySession, secondQuery);
    }
    
    @Test 
    public void testOtherQueryManagement() {
        DataMiningSession session = new UUIDDataMiningSession(UUID.randomUUID());
        AdditionalQueryData additionalData = new AdditionalOtherQueryData();
        
        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        runQueriesAndVerifyNoConflict(session, firstQuery, session, secondQuery);
    }
    
    private void runQueriesAndVerifyNoConflict(DataMiningSession firstQuerySession, ControllablePseudoQuery<?> firstQuery, DataMiningSession secondQuerySession, ControllablePseudoQuery<?> secondQuery) {
        Thread firstQueryWorker = new Thread(new ServerQueryWorker(firstQuerySession, firstQuery));
        Thread secondQueryWorker = new Thread(new ServerQueryWorker(secondQuerySession, secondQuery));
        
        firstQueryWorker.start();
        ConcurrencyTestsUtil.sleepFor(75);
        assertThat(server.getNumberOfRunningQueries(), is(1));
        secondQueryWorker.start();
        ConcurrencyTestsUtil.sleepFor(75);
        assertThat(server.getNumberOfRunningQueries(), is(2));
        assertThat(firstQuery.getState(), not(QueryState.ABORTED));
        
        firstQuery.enableProcess();
        secondQuery.enableProcess();
        ConcurrencyTestsUtil.sleepFor(110);
        assertThat(server.getNumberOfRunningQueries(), is(0));
        assertThat(firstQuery.getState(), is(QueryState.NORMAL));
        assertThat(secondQuery.getState(), is(QueryState.NORMAL));
    }
    
    @Test
    public void testAbortingRandomQueryOfNullManager() {
        DataMiningQueryManager manager = new NullDataMiningQueryManager();

        AdditionalQueryData additionalData = new AdditionalOtherQueryData();
        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        runQueriesAbortRandomOneAndVerifyAbortion(manager, firstQuery, secondQuery);
    }
    
    @Test
    public void testAbortingRandomQueryOfStrategyPerQueryTypeManager() {
        DataMiningQueryManager manager = new StrategyPerQueryTypeManager();

        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, new AdditionalStatisticQueryData());
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, new AdditionalOtherQueryData());
        runQueriesAbortRandomOneAndVerifyAbortion(manager, firstQuery, secondQuery);
    }

    private void runQueriesAbortRandomOneAndVerifyAbortion(DataMiningQueryManager manager,
            final ControllablePseudoQuery<Double> firstQuery, final ControllablePseudoQuery<Double> secondQuery) {
        Thread firstQueryWorker = new Thread(new ManagerQueryWorker(manager, new UUIDDataMiningSession(UUID.randomUUID()), firstQuery));
        firstQueryWorker.setDaemon(true);
        Thread secondQueryWorker = new Thread(new ManagerQueryWorker(manager, new UUIDDataMiningSession(UUID.randomUUID()), secondQuery));
        secondQueryWorker.setDaemon(true);
        firstQueryWorker.start();
        secondQueryWorker.start();

        ConcurrencyTestsUtil.sleepFor(75);
        assertThat(manager.getNumberOfRunningQueries(), is(2));
        manager.abortRandomQuery();
        ConcurrencyTestsUtil.sleepFor(75);
        assertThat(manager.getNumberOfRunningQueries(), is(1));
        if (!(firstQuery.getState() == QueryState.ABORTED || secondQuery.getState() == QueryState.ABORTED)) {
            fail("One query has to be aborted.");
        }
    }
    
    @Test
    public void testAbortingAllQueryOfNullManager() {
        DataMiningQueryManager manager = new NullDataMiningQueryManager();

        AdditionalQueryData additionalData = new AdditionalOtherQueryData();
        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        runQueriesAbortAllAndVerifyAbortion(manager, firstQuery, secondQuery);
    }
    
    @Test
    public void testAbortingAllQueryOfStrategyPerQueryTypeManager() {
        DataMiningQueryManager manager = new StrategyPerQueryTypeManager();

        final ControllablePseudoQuery<Double> firstQuery = new ControllablePseudoQuery<Double>(Double.class, new AdditionalStatisticQueryData());
        final ControllablePseudoQuery<Double> secondQuery = new ControllablePseudoQuery<Double>(Double.class, new AdditionalOtherQueryData());
        runQueriesAbortAllAndVerifyAbortion(manager, firstQuery, secondQuery);
    }

    private void runQueriesAbortAllAndVerifyAbortion(DataMiningQueryManager manager,
            final ControllablePseudoQuery<Double> firstQuery, final ControllablePseudoQuery<Double> secondQuery) {
        Thread firstQueryWorker = new Thread(new ManagerQueryWorker(manager, new UUIDDataMiningSession(UUID.randomUUID()), firstQuery));
        firstQueryWorker.setDaemon(true);
        Thread secondQueryWorker = new Thread(new ManagerQueryWorker(manager, new UUIDDataMiningSession(UUID.randomUUID()), secondQuery));
        secondQueryWorker.setDaemon(true);
        firstQueryWorker.start();
        secondQueryWorker.start();

        ConcurrencyTestsUtil.sleepFor(75);
        assertThat(manager.getNumberOfRunningQueries(), is(2));
        manager.abortAllQueries();
        ConcurrencyTestsUtil.sleepFor(100);
        assertThat(manager.getNumberOfRunningQueries(), is(0));
        if (firstQuery.getState() != QueryState.ABORTED && secondQuery.getState() != QueryState.ABORTED) {
            fail("All queries have to be aborted.");
        }
    }
    
    @Test
    public void testControllablePseudoQuery() {
        AdditionalQueryData additionalData = new AdditionalOtherQueryData();
        
        final ControllablePseudoQuery<Double> normalQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        assertThat(normalQuery.getState(), is(QueryState.NOT_STARTED));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                normalQuery.run();
            }
        });
        thread.start();
        ConcurrencyTestsUtil.sleepFor(50);
        assertThat(normalQuery.getState(), is(QueryState.RUNNING));
        normalQuery.enableProcess();
        ConcurrencyTestsUtil.sleepFor(110);
        assertThat(normalQuery.getState(), is(QueryState.NORMAL));
        normalQuery.abort();
        ConcurrencyTestsUtil.sleepFor(50);
        assertThat(normalQuery.getState(), is(QueryState.NORMAL));
        
        final Query<Double> abortedQuery = new ControllablePseudoQuery<Double>(Double.class, additionalData);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                abortedQuery.run();
            }
        });
        thread.start();
        abortedQuery.abort();
        ConcurrencyTestsUtil.sleepFor(110);
        assertThat(abortedQuery.getState(), is(QueryState.ABORTED));
    }
    
    private static class ControllablePseudoQuery<ResultType> implements Query<ResultType> {

        private QueryState state;
        private final Class<ResultType> resultType;
        private final AdditionalQueryData additionalData;
        
        private boolean processEnabled;
        private boolean aborted;

        public ControllablePseudoQuery(Class<ResultType> resultType, AdditionalQueryData additionalData) {
            state = QueryState.NOT_STARTED;
            this.resultType = resultType;
            this.additionalData = additionalData;
        }

        @Override
        public QueryState getState() {
            return state;
        }
        
        @Override
        public Class<ResultType> getResultType() {
            return resultType;
        }

        @Override
        public AdditionalQueryData getAdditionalData() {
            return additionalData;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends AdditionalQueryData> T getAdditionalData(Class<T> additionalDataType) {
            if (additionalDataType.isAssignableFrom(getAdditionalData().getClass())) {
                return (T) getAdditionalData();
            }
            return null;
        }
        
        public void enableProcess() {
            processEnabled = true;
        }

        @Override
        public QueryResult<ResultType> run() {
            state = QueryState.RUNNING;
            while (!processEnabled && !aborted) {
                ConcurrencyTestsUtil.sleepFor(100);
            }
            if (processEnabled) {
                state = QueryState.NORMAL;
            } else if (aborted) {
                state = QueryState.ABORTED;
            } else {
                throw new IllegalStateException("The query shouldn't finish, without being aborted "
                        + "or enabled process");
            }
            return null;
        }

        @Override
        public QueryResult<ResultType> run(long timeout, TimeUnit unit) throws TimeoutException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void abort() {
            aborted = true;
        }
        
    }
    
    private class ServerQueryWorker implements Runnable {
        
        private final DataMiningSession session;
        private final Query<?> query;

        public ServerQueryWorker(DataMiningSession session, Query<?> query) {
            this.session = session;
            this.query = query;
        }

        @Override
        public void run() {
            server.runNewQueryAndAbortPreviousQueries(session, query);
        }
        
    }
    
    private class ManagerQueryWorker implements Runnable {
        
        private final DataMiningQueryManager manager;
        private final DataMiningSession session;
        private final Query<?> query;

        public ManagerQueryWorker(DataMiningQueryManager manager, DataMiningSession session, Query<?> query) {
            this.manager = manager;
            this.session = session;
            this.query = query;
        }

        @Override
        public void run() {
            manager.runNewAndAbortPrevious(session, query);
        }
        
    }

    @Test
    public void testNullSessionOrQuery() {
        try {
            server.runNewQueryAndAbortPreviousQueries(null, null);
            fail("Expected a NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            DataMiningSession session = new UUIDDataMiningSession(UUID.randomUUID());
            server.runNewQueryAndAbortPreviousQueries(session , null);
            fail("Expected a NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
