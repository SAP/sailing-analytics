package com.sap.sse.datamining.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sse.datamining.AdditionalQueryData;
import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.QueryState;
import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.components.AbstractParallelProcessor;
import com.sap.sse.datamining.impl.components.AbstractProcessorInstruction;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.data.QueryResultState;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.datamining.test.util.components.BlockingProcessor;
import com.sap.sse.datamining.test.util.components.NullProcessor;
import com.sap.sse.datamining.test.util.components.Number;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TestProcessorQuery {
    
    private static final Logger LOGGER = Logger.getLogger(TestProcessorQuery.class.getSimpleName());
    private static final ResourceBundleStringMessages stringMessages = TestsUtil.getTestStringMessagesWithProductiveMessages();

    private boolean receivedElementOrFinished;
    private boolean receivedAbort;
    
    private QueryResult<?> resultAfterAbortion;

    @Timeout(value=2, unit=TimeUnit.SECONDS)
    @Test
    public void testQueryTimeouting() {
        receivedElementOrFinished = false;
        receivedAbort = false;
        
        Collection<Number> dataSource = new ArrayList<>();
        dataSource.add(new Number(10));
        ProcessorQuery<Double, Iterable<Number>> query = new ProcessorQuery<Double, Iterable<Number>>(
                dataSource, stringMessages, Locale.ENGLISH, Double.class, AdditionalQueryData.NULL_INSTANCE) {
            @SuppressWarnings("unchecked")
            @Override
            protected Processor<Iterable<Number>, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, Double>, Void> resultReceiver) {
                Collection<Processor<Double, ?>> resultReceivers = new ArrayList<>();
                resultReceivers.add(new AbortResultReceiver(resultReceiver));
                return new BlockingProcessor<Iterable<Number>, Double>((Class<Iterable<Number>>)(Class<?>) Iterable.class, Double.class,
                        ConcurrencyTestsUtil.getSharedExecutor(), resultReceivers, 1000) {
                            @Override
                            protected Double createResult(Iterable<Number> element) {
                                return 0.0;
                            }
                };
            }
        };
        
        try {
            query.run(500, TimeUnit.MILLISECONDS);
            fail("The previous line should throw a timeout exception");
        } catch (TimeoutException e) {
            // A timeout exception is expected
            LOGGER.log(Level.INFO, "The query timed out: ", e);
        }
        assertThat(query.getState(), is(QueryState.TIMED_OUT));
        
        ConcurrencyTestsUtil.sleepFor(1000); // Wait if a result is received
        assertThat("The processing should be aborted, but received elements", receivedElementOrFinished, is(false));
        assertThat("The processing should be aborted, but didn't receive abort", receivedAbort, is(true));
    }

    @Timeout(value=2, unit=TimeUnit.SECONDS)
    @Test
    public void testQueryAbortion() throws InterruptedException {
        receivedElementOrFinished = false;
        receivedAbort = false;

        Collection<Number> dataSource = new ArrayList<>();
        dataSource.add(new Number(10));
        ProcessorQuery<Double, Iterable<Number>> query = new ProcessorQuery<Double, Iterable<Number>>(
                dataSource, stringMessages, Locale.ENGLISH, Double.class, AdditionalQueryData.NULL_INSTANCE) {
            @SuppressWarnings("unchecked")
            @Override
            protected Processor<Iterable<Number>, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, Double>, Void> resultReceiver) {
                Collection<Processor<Double, ?>> resultReceivers = new ArrayList<>();
                resultReceivers.add(new AbortResultReceiver(resultReceiver));
                return new BlockingProcessor<Iterable<Number>, Double>((Class<Iterable<Number>>)(Class<?>) Iterable.class, Double.class,
                        ConcurrencyTestsUtil.getSharedExecutor(), resultReceivers, 1000) {
                            @Override
                            protected Double createResult(Iterable<Number> element) {
                                return 0.0;
                            }
                };
            }
        };
        
        Thread queryRunner = new Thread(new Runnable() {
            @Override
            public void run() {
                resultAfterAbortion = query.run();
            }
        });
        queryRunner.start();
        ConcurrencyTestsUtil.sleepFor(250);
        assertThat(query.getState(), is(QueryState.RUNNING));
        ConcurrencyTestsUtil.sleepFor(250);
        query.abort();
        
        ConcurrencyTestsUtil.sleepFor(1000); // Wait if a result is received
        queryRunner.join();
        assertThat(resultAfterAbortion.getState(), is(QueryResultState.ABORTED));
        assertThat("The processing should be aborted, but received elements", receivedElementOrFinished, is(false));
        assertThat("The processing should be aborted, but didn't receive abort", receivedAbort, is(true));
    }
    
    private class AbortResultReceiver extends NullProcessor<Double, Void> {
        private final Processor<Map<GroupKey, Double>, Void> queryResultReceiver;
        
        public AbortResultReceiver(Processor<Map<GroupKey, Double>, Void> queryResultReceiver) {
            super(Double.class, Void.class);
            this.queryResultReceiver = queryResultReceiver;
        }
        
        @Override
        public void processElement(Double element) {
            receivedElementOrFinished = true;
            queryResultReceiver.processElement(null);
        }
        @Override
        public void finish() throws InterruptedException {
            receivedElementOrFinished = true;
            queryResultReceiver.finish();
        }
        @Override
        public void abort() {
            receivedAbort = true;
            queryResultReceiver.abort();
        }
        
    }

    @Test
    public void testQueryWithTimeoutAndNonBlockingProcess() throws TimeoutException {
        final String keyValue = "Sum";
        ProcessorQuery<Double, Iterable<Number>> query = new ProcessorQuery<Double, Iterable<Number>>(
                createDataSource(), stringMessages, Locale.ENGLISH, Double.class, AdditionalQueryData.NULL_INSTANCE) {
            @SuppressWarnings("unchecked")
            @Override
            protected Processor<Iterable<Number>, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, Double>, Void> resultReceiver) {
                Collection<Processor<Map<GroupKey, Double>, ?>> resultReceivers = new ArrayList<>();
                resultReceivers.add(resultReceiver);
                return new AbstractParallelProcessor<Iterable<Number>, Map<GroupKey, Double>>((Class<Iterable<Number>>)(Class<?>) Iterable.class,
                                                                                                    (Class<Map<GroupKey, Double>>)(Class<?>) Map.class,
                                                                                                    ConcurrencyTestsUtil.getSharedExecutor(),
                                                                                                    resultReceivers) {
                    @Override
                    protected ProcessorInstruction<Map<GroupKey, Double>> createInstruction(final Iterable<Number> element) {
                        return new AbstractProcessorInstruction<Map<GroupKey,Double>>(this) {
                            @Override
                            public Map<GroupKey, Double> computeResult() {
                                Map<GroupKey, Double> result = new HashMap<>();
                                double sum = 0;
                                for (Number number : element) {
                                    sum += number.getValue();
                                }
                                result.put(new GenericGroupKey<String>(keyValue), sum);
                                return result;
                            }
                        };
                    }
                
                    @Override
                    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
                    }
                };
            }
        };
        
        Map<GroupKey, Double> expectedResult = new HashMap<>();
        expectedResult.put(new GenericGroupKey<String>(keyValue), 10358.0);
        assertThat(query.run(500, TimeUnit.MILLISECONDS).getResults(), is(expectedResult));
    }
    
    private Collection<Number> createDataSource() {
        Collection<Number> dataSource = new ArrayList<>();
        
        //Should be removed after filtering the data
        dataSource.add(new Number(1));
        dataSource.add(new Number(7));

        //Results in <2> = 5
        dataSource.add(new Number(10));
        dataSource.add(new Number(10));
        dataSource.add(new Number(10));
        dataSource.add(new Number(10));
        dataSource.add(new Number(10));

        //Results in <3> = 3
        dataSource.add(new Number(100));
        dataSource.add(new Number(100));
        dataSource.add(new Number(100));

        //Results in <4> = 10
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        dataSource.add(new Number(1000));
        
        return dataSource;
    }
    
    @Test
    public void testQueryWithError() {
        Query<Double> query = new ProcessorQuery<Double, Double>(0.0, Double.class) {
            @SuppressWarnings("unchecked")
            @Override
            protected Processor<Double, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, Double>, Void> resultReceiver) {
                Collection<Processor<Map<GroupKey, Double>, ?>> resultReceivers = new ArrayList<>();
                resultReceivers.add(resultReceiver);
                return new AbstractParallelProcessor<Double, Map<GroupKey, Double>>(Double.class,
                                                                                    (Class<Map<GroupKey, Double>>)(Class<?>) Map.class,
                                                                                    ConcurrencyTestsUtil.getSharedExecutor(),
                                                                                    resultReceivers) {
                    @Override
                    protected ProcessorInstruction<Map<GroupKey, Double>> createInstruction(Double element) {
                        return new AbstractProcessorInstruction<Map<GroupKey,Double>>(this) {
                            @Override
                            protected Map<GroupKey, Double> computeResult() throws Exception {
                                throw new RejectedExecutionException("This should cause an error during the query processing");
                            }
                        };
                    }
                    @Override
                    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
                    }
                };
            }
        };
        try {
            query.run();
            fail("The previous line should throw a runtime exception");
        } catch (RuntimeException e) {
            // A RuntimeException with a RejectedExecitonException as cause is expected
            if (!(e.getCause() instanceof RejectedExecutionException)) {
                throw e;
            }
            LOGGER.log(Level.INFO, "The query had an error: ", e);
        }
        assertThat(query.getState(), is(QueryState.ERROR));
    }
    
    @Test
    public void testQueryWithFailure() {
        Query<Double> query = new ProcessorQuery<Double, Double>(0.0, Double.class) {
            @SuppressWarnings("unchecked")
            @Override
            protected Processor<Double, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, Double>, Void> resultReceiver) {
                Collection<Processor<Map<GroupKey, Double>, ?>> resultReceivers = new ArrayList<>();
                resultReceivers.add(resultReceiver);
                return new AbstractParallelProcessor<Double, Map<GroupKey, Double>>(Double.class,
                                                                                    (Class<Map<GroupKey, Double>>)(Class<?>) Map.class,
                                                                                    ConcurrencyTestsUtil.getSharedExecutor(),
                                                                                    resultReceivers) {
                    @Override
                    protected ProcessorInstruction<Map<GroupKey, Double>> createInstruction(Double element) {
                        return new AbstractProcessorInstruction<Map<GroupKey,Double>>(this) {
                            @Override
                            protected Map<GroupKey, Double> computeResult() throws Exception {
                                throw new NullPointerException("This should cause a failure during the query processing");
                            }
                        };
                    }
                    @Override
                    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
                    }
                };
            }
        };
        query.run();
        assertThat(query.getState(), is(QueryState.FAILURE));
    }
    
    @Test
    public void testQueryAdditionalData() {
        AdditionalQueryData additionalData = new AdditionalStatisticQueryData();
        Query<?> query = new ProcessorQuery<Double, Double>(0.0, Double.class, additionalData) {
            @Override
            protected Processor<Double, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, Double>, Void> resultReceiver) {
                return null;
            }
        };
        assertThat(query.getAdditionalData(), is(additionalData));
        assertThat(query.getAdditionalData(AdditionalStatisticQueryData.class), is(additionalData));
        assertThat(query.getAdditionalData(AdditionalDimensionValuesQueryData.class), nullValue());
        
        additionalData = new AdditionalDimensionValuesQueryData(new ArrayList<Function<?>>());
        query = new ProcessorQuery<Double, Double>(0.0, Double.class, additionalData) {
            @Override
            protected Processor<Double, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, Double>, Void> resultReceiver) {
                return null;
            }
        };
        assertThat(query.getAdditionalData(AdditionalDimensionValuesQueryData.class), is(additionalData));
    }

}
