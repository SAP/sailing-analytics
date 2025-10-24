package com.sap.sse.datamining.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.datamining.AdditionalQueryData;
import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.QueryState;
import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.impl.components.OverwritingResultDataBuilder;
import com.sap.sse.datamining.impl.data.QueryResultImpl;
import com.sap.sse.datamining.shared.AdditionalResultData;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.data.QueryResultState;
import com.sap.sse.datamining.shared.impl.NullAdditionalResultData;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public abstract class ProcessorQuery<ResultType, DataSourceType> implements Query<ResultType> {
    
    private static final Logger LOGGER = Logger.getLogger(ProcessorQuery.class.getSimpleName());
    
    private final DataSourceType dataSource;
    private final Processor<DataSourceType, ?> firstProcessor;
    private QueryState state;
    private final Class<ResultType> resultType;

    private final ProcessResultReceiver resultReceiver;
    
    private final ResourceBundleStringMessages stringMessages;
    private final Locale locale;
    private final AdditionalQueryData additionalData;

    private final Object monitorObject = new Object();
    private Thread workingThread;
    
    /**
     * Creates a query
     * <ul>
     *   <li> with no {@link AdditionalQueryData} (more exactly with {@link NullAdditionalQueryData} as additional data).</li>
     *   <li> that returns a result without {@link AdditionalResultData} (more exactly with {@link NullAdditionalResultData} as additional data).</li>
     * </ul>
     */
    public ProcessorQuery(DataSourceType dataSource, Class<ResultType> resultType) {
        this(dataSource, resultType, AdditionalQueryData.NULL_INSTANCE);
    }

    
    /**
     * Creates a query that returns a result without {@link AdditionalResultData}
     * (more exactly with {@link NullAdditionalResultData} as additional data).
     */
    public ProcessorQuery(DataSourceType dataSource, Class<ResultType> resultType, AdditionalQueryData additionalData) {
        this(dataSource, null, null, resultType, additionalData);
    }

    /**
     * Creates a query that returns a result with additional data.
     */
    public ProcessorQuery(DataSourceType dataSource, ResourceBundleStringMessages stringMessages, Locale locale, Class<ResultType> resultType, AdditionalQueryData additionalData) {
        this.dataSource = dataSource;
        this.stringMessages = stringMessages;
        this.locale = locale;
        state = QueryState.NOT_STARTED;
        this.resultType = resultType;
        this.additionalData = additionalData;

        resultReceiver = new ProcessResultReceiver();
        firstProcessor = createChainAndReturnFirstProcessor(resultReceiver);
    }

    protected abstract Processor<DataSourceType, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, ResultType>, Void> resultReceiver);
    
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
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends AdditionalQueryData> T getAdditionalData(Class<T> additionalDataType) {
        if (additionalDataType.isAssignableFrom(getAdditionalData().getClass())) {
            return (T) getAdditionalData();
        }
        return null;
    }

    @Override
    public QueryResult<ResultType> run() {
        try {
            return run(0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // This code shouldn't be reached, because the timeout is deactivated (by the value 0 as timeout)
            LOGGER.log(Level.SEVERE, "Got a TimeoutException that should never happen: ", e);
        }
        
        return null;
    }
    
    @Override
    public QueryResult<ResultType> run(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return processQuery(unit.toMillis(timeout));
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "The query processing got interrupted.", e);
        }
        
        return null;
    }

    private QueryResult<ResultType> processQuery(long timeoutInMillis) throws InterruptedException, TimeoutException {
        state = QueryState.RUNNING;
        final long startTime = System.nanoTime();
        startWorking();
        waitTillWorkIsDone(timeoutInMillis);
        final long endTime = System.nanoTime();
        
        logOccuredFailuresAndThrowSevereFailure();

        long calculationTimeInNanos = endTime - startTime;
        Map<GroupKey, ResultType> results = resultReceiver.getResult();
        QueryResultState resultState = state.asResultState();
        
        if (stringMessages != null && locale != null) {
            AdditionalResultDataBuilder additionalDataBuilder = new OverwritingResultDataBuilder();
            additionalDataBuilder = firstProcessor.getAdditionalResultData(additionalDataBuilder);
            return new QueryResultImpl<>(resultState, getResultType(), results, additionalDataBuilder.build(calculationTimeInNanos, stringMessages, locale));
        } else {
            return new QueryResultImpl<>(resultState, getResultType(), results);
        }
    }

    private void startWorking() {
        workingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    firstProcessor.processElement(dataSource);
                    firstProcessor.finish();
                } catch (InterruptedException e) {
                    if (state == QueryState.TIMED_OUT) {
                        LOGGER.log(Level.INFO, "The query processing timed out.");
                    } else if (state == QueryState.ABORTED) {
                        LOGGER.log(Level.INFO, "The query processing got aborted.");
                    } else if (state == QueryState.ERROR) {
                        LOGGER.log(Level.INFO, "A severe failure occured during the query processing.");
                    } else {
                        LOGGER.log(Level.WARNING, "The query processing got interrupted.", e);
                    }
                }
            }
        });
        workingThread.start();
    }

    private void waitTillWorkIsDone(long timeoutInMillis) throws InterruptedException, TimeoutException {
        setUpTimeoutTimer(timeoutInMillis);
        synchronized (monitorObject) {
            while (getState() == QueryState.RUNNING) {
                monitorObject.wait();
                if (processingHasToBeAborted()) {
                    firstProcessor.abort();
                    workingThread.interrupt();
                    if (state == QueryState.TIMED_OUT) {
                        throw new TimeoutException("The query processing timed out");
                    }
                    break;
                }
            }
        }
    }
    
    private boolean processingHasToBeAborted() {
        return state == QueryState.TIMED_OUT || state == QueryState.ABORTED || state == QueryState.ERROR;
    }

    private void setUpTimeoutTimer(long timeoutInMillis) {
        if (timeoutInMillis > 0) {
            Timer timeoutTimer = new Timer();
            timeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (monitorObject) {
                        state = QueryState.TIMED_OUT;
                        monitorObject.notify();
                    }
                }
            }, timeoutInMillis);
        }
    }

    private void logOccuredFailuresAndThrowSevereFailure() {
        for (Throwable failure : resultReceiver.getOccuredFailures()) {
            LOGGER.log(Level.SEVERE, "A failure occured during the processing of an instruction: ", failure);
        }
        if (state == QueryState.ERROR) {
            throw new RuntimeException("An error occured during the processing of an instruction", resultReceiver.getSevereFailure());
        }
    }
    
    @Override
    public void abort() {
        synchronized (monitorObject) {
            LOGGER.log(Level.INFO, "Aborting the query processing");
            state = state == QueryState.RUNNING || state == QueryState.NOT_STARTED ? QueryState.ABORTED : state;
            monitorObject.notify();
        }
    }

    public Processor<Map<GroupKey, ResultType>, Void> getResultReceiver() {
        return resultReceiver;
    }
    
    private class ProcessResultReceiver implements Processor<Map<GroupKey, ResultType>, Void> {
        
        private final ReentrantLock resultsLock;
        private Map<GroupKey, ResultType> results;
        private List<Throwable> occuredFailures;
        private Throwable severeFailure;
        
        public ProcessResultReceiver() {
            resultsLock = new ReentrantLock();
            results = new HashMap<>();
            occuredFailures = new ArrayList<>();
        }

        @Override
        public boolean canProcessElements() {
            return true;
        }

        @Override
        public void processElement(Map<GroupKey, ResultType> groupedAggregations) {
            resultsLock.lock();
            try {
                results.putAll(groupedAggregations);
            } finally {
                resultsLock.unlock();
            }
        }
        
        @Override
        public void onFailure(Throwable failure) {
            if (isSevereFailure(failure)) {
                severeFailure = failure;
                synchronized (monitorObject) {
                    state = QueryState.ERROR;
                    monitorObject.notify();
                }
            } else {
                state = QueryState.FAILURE;
                occuredFailures.add(failure);
            }
        }

        private boolean isSevereFailure(Throwable failure) {
            return !(failure instanceof Exception) || failure instanceof RejectedExecutionException;
        }

        @Override
        public void finish() throws InterruptedException {
            synchronized (monitorObject) {
                state = state == QueryState.RUNNING ? QueryState.NORMAL : state;
                monitorObject.notify();
            }
        }
        
        @Override
        public boolean isFinished() {
            return false;
        }
        
        @Override
        public void abort() {
            results = new HashMap<>();
            occuredFailures = new ArrayList<>();
        }
        
        @Override
        public boolean isAborted() {
            return false;
        }
        
        public Map<GroupKey, ResultType> getResult() {
            return results;
        }
        
        public List<Throwable> getOccuredFailures() {
            return occuredFailures;
        }
        
        public Throwable getSevereFailure() {
            return severeFailure;
        }

        @Override
        public AdditionalResultDataBuilder getAdditionalResultData(AdditionalResultDataBuilder additionalDataBuilder) {
            return additionalDataBuilder;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Map<GroupKey, ResultType>> getInputType() {
            return (Class<Map<GroupKey, ResultType>>)(Class<?>) Map.class;
        }

        @Override
        public Class<Void> getResultType() {
            return Void.class;
        }
        
    }

}
