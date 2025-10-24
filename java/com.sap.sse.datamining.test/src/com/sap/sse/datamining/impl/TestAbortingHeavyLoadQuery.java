package com.sap.sse.datamining.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.QueryState;
import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.components.ProcessorInstructionHandler;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.impl.components.AbstractParallelProcessor;
import com.sap.sse.datamining.impl.components.AbstractProcessorInstruction;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.ProcessorInstructionPriority;
import com.sap.sse.datamining.impl.components.aggregators.ParallelGroupedDataCollectingAsSetProcessor;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.data.QueryResultState;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.test.util.components.StatefulBlockingInstruction;
import com.sap.sse.datamining.test.util.components.StatefulProcessorInstruction;

/**
 * Integration test aborting a query with simulated heavy load instructions. Uses a highly customized processor chain
 * (based on the processors/instructions used in production) that allows detailed introspection and logging of the
 * execution process. The test is used to verify the behavior of processors, if such a heavy load query is aborted
 * during the process. For example that no instructions are scheduled after the query was aborted and the behavior of
 * already scheduled instructions that may or may not have been started.<br>
 * <br>
 * The test is configurable regarding the number of threads for the executor, the number of data elements/heavy load
 * instructions, the duration of heavy load instructions, the time to wait before the query is aborted and the time
 * given to the executor to finish the remaining instructions. Note that the test can fail for some configurations, due
 * to race conditions that would be very hard to check for.<br>
 * <br>
 * The test optionally records the process in order of the execution (using a concurrent message queue), which is
 * printed to the console before assertions are performed. This isn't used by the assertions, but can be used to get a
 * better understanding of what happens during the execution.<br>
 * <br>
 * See {@link #initialize()} for more details about the processor chain.
 * 
 * @author Lennart Hensler
 */
public class TestAbortingHeavyLoadQuery {
    
    // Test Configuration ----------------------------------------------------------------------------------------
    /** 
     * Number of threads in the executor. Determines the number of elements (and thus heavy load instructions)
     * retrieved for each group. This means that each group should have a runtime of {@value #HeavyLoadInstructionTotalDuration}ms,
     * since the single instructions are executed concurrently.
     */
    private static final int ExecutorPoolSize = Math.max(3, Runtime.getRuntime().availableProcessors());
    
    /** The number of groups contained in the initial data source. */
    private static final int DataSourceSize = 2000;
    private static final String GroupKeyPrefix = "G";
    
    /** The time a step of a heavy load instruction blocks the executing thread (using {@link Thread#sleep(long)}). */
    private static final long HeavyLoadInstructionStepDuration = 50;
    /** The number of steps a heavy load instruction performs */
    private static final int HeavyLoadInstructionNumberOfSteps = 10;
    /** The total time a heavy load instruction blocks the executing thread (using {@link Thread#sleep(long)}). */
    private static final long HeavyLoadInstructionTotalDuration = HeavyLoadInstructionStepDuration * HeavyLoadInstructionNumberOfSteps;
    
    /** The number of milliseconds to wait before {@link Query#abort()} is called. */
    private static final long AbortQueryDelay = (long) (HeavyLoadInstructionTotalDuration * 5.45);
    /** The time given to the executor to complete all unfinished instructions */
    private static final long TerminationTimeout = HeavyLoadInstructionStepDuration * 2;
    //------------------------------------------------------------------------------------------------------------
    
    // Execution Recording Configuration -------------------------------------------------------------------------
    /** Enables concurrent logging of the query execution and prints the current record before assertions. */
    private static final boolean RecordExecution = true;
    private static final SimpleDateFormat DateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    private ConcurrentLinkedQueue<String> executionRecord;
    //------------------------------------------------------------------------------------------------------------
    
    // Test State - Initialized in initialize() ------------------------------------------------------------------
    private ExecutorService executor;
    private Query<HashSet<Integer>> query;
    private Collection<Processor<?, ?>> processors;
    private Set<StatefulProcessorInstruction<?>> unfinishedInstructions;
    //------------------------------------------------------------------------------------------------------------
    
    @Test
    public void testAbortingHeavyLoadQuery() throws InterruptedException, ExecutionException {
        // Executing query in separate thread
        RunnableFuture<QueryResult<HashSet<Integer>>> queryTask = new FutureTask<>(() -> {
            logExecution("Starting query execution");
            long start = System.currentTimeMillis();
            QueryResult<HashSet<Integer>> result = query.run();
            long duration = System.currentTimeMillis() - start;
            logExecution("Finished query in " + duration + "ms");
            return result;
        });
        Thread worker = new Thread(queryTask, "Worker");
        worker.start();
        do {
            Thread.sleep(10);
        } while (query.getState() != QueryState.RUNNING);
        // Aborting query and waiting for completion
        Thread.sleep(AbortQueryDelay);
        logExecution("Aborting query");
        query.abort();
        QueryResult<HashSet<Integer>> result = queryTask.get();
        // Checking query, result and processor chain state
        assertThat(query.getState(), is(QueryState.ABORTED));
        assertThat(result.getState(), is(QueryResultState.ABORTED));
        assertTrue(result.isEmpty(), "The result is not empty");
        for (Processor<?, ?> processor : processors) {
            assertTrue(processor.isAborted(), "Processor wasn't aborted");
        }
        // Execution of unfinished instructions
        executor.shutdown();
        boolean terminated = executor.awaitTermination(TerminationTimeout, TimeUnit.MILLISECONDS);
        printExecutionRecord();
        assertTrue(terminated, "The executor didn't terminate in the given time");
        // The query execution returned, so all processors have received the abort signal (checked below).
        // The number of unfinished instructions mustn't change after this point
        int unfinishedInstructionsCount = unfinishedInstructions.size();
        Set<StatefulProcessorInstruction<?>> runningInstructions = new HashSet<>();
        Set<StatefulProcessorInstruction<?>> notStartedInstructions = new HashSet<>();
        for (StatefulProcessorInstruction<?> instruction : unfinishedInstructions) {
            if (instruction.runWasCalled()) { // could be that computeResult still wasn't called because the instruction was aborted
                runningInstructions.add(instruction);
            } else {
                notStartedInstructions.add(instruction);
            }
        }
        logExecution(unfinishedInstructionsCount + " unfinished instructions left - " +
                     runningInstructions.size() + " running, " + notStartedInstructions.size() + " not started");
        printExecutionRecord();
        assertThat("Number of unfinished instructions changed", runningInstructions.size() + notStartedInstructions.size(), is(unfinishedInstructionsCount));
        // Verify that the collection of unfinished instructions didn't change
        assertThat("Number of unfinished instructions changed", unfinishedInstructions.size(), is(unfinishedInstructionsCount));
        for (StatefulProcessorInstruction<?> instruction : unfinishedInstructions) {
            boolean previouslyContained = runningInstructions.contains(instruction) || notStartedInstructions.contains(instruction);
            assertTrue(previouslyContained, "A new instruction has been scheduled after aborting the query");
        }
        for (StatefulProcessorInstruction<?> instruction : runningInstructions) {
            assertTrue(unfinishedInstructions.contains(instruction), "A previously running instruction was removed from unfinished instructions");
        }
        for (StatefulProcessorInstruction<?> instruction : notStartedInstructions) {
            assertTrue(unfinishedInstructions.contains(instruction), "A previously unstarted instruction was removed from unfinished instructions");
        }
        // Checking state of unfinished instructions
        for (StatefulProcessorInstruction<?> instruction : runningInstructions) {
            assertTrue(instruction.computeResultWasCalled() == instruction.computeResultWasFinished(), "computeResult() of a running unfinished instruction didn't finish");
            if (instruction instanceof StatefulBlockingInstruction) {
                StatefulBlockingInstruction<?> blockingInstruction = (StatefulBlockingInstruction<?>) instruction;
                assertTrue(!blockingInstruction.computeResultWasCalled() || blockingInstruction.computeResultWasAborted(), "computeResult() of a running heavy load instruction wasn't aborted");
            }
        }
        for (StatefulProcessorInstruction<?> instruction : notStartedInstructions) {
            assertTrue(instruction.runWasCalled(), "run() of an unstarted unfinished instruction wasn't called");
            assertFalse(instruction.computeResultWasCalled(), "computeResult() of an unstarted unfinished instruction was called");
            assertFalse(instruction.computeResultWasFinished(), "computeResult() of an unstarted unfinished instruction was finished");
        }
    }

    private void printExecutionRecord() {
        if (RecordExecution) {
            Collection<String> snapshot = new ArrayList<>(executionRecord);
            executionRecord.clear();
            for (String string : snapshot) {
                System.out.println(string);
            }
        }
    }

    private void logExecution(String message) {
        if (RecordExecution) {
            String timeString = DateFormatter.format(new Date());
            executionRecord.add(timeString + " - " + Thread.currentThread().getName() + ": " + message);
        }
    }

    /**
     * Initializes the {@link ExecutorService}, the execution record, helper collections (e.g. the processors in the
     * chain or the unfinished instructions) and most importantly the processor chain for the query, which works as
     * follows:
     * <ol>
     *   <li>
     *     The initial data source is a collection of strings from {@value #GroupKeyPrefix}<code>n</code> to
     *     {@value #GroupKeyPrefix}<code>n-1</code>, denoting a group. <code>n</code> is specified in {@link #DataSourceSize}.
     *   </li>
     *   <li>
     *     <code>x</code> (specified by {@link #ExecutorPoolSize}) {@link Element elements} are retrieved for each
     *     group, where the elements name is set to the received string and the elements value is the current value
     *     of <code>x</code>.
     *   </li>
     *   <li>
     *     A heavy load instruction for each element is scheduled, which blocks the running thread for
     *     {@value #HeavyLoadInstructionTotalDuration}ms.
     *   </li>
     *   <li>
     *     Each element is grouped by its name and its value is used as value for the {@link GroupedDataEntry}.
     *   </li>
     *   <li>The grouped data is collected as set.</li>
     * </ol>
     * This results in <code>n * x</code> data elements (and heavy load instructions), with <code>x</code> instructions
     * executed concurrently.<br>
     * <br>
     * Each processor creates {@link StatefulProcessorInstruction} or uses {@link StatefulInstructionWrapper}, which allows
     * to verify if <code>instruction.run()</code> was called, its computation started and its computation finished.<br>
     * <br>
     * A concurrent set ({@link ConcurrentHashMap#newKeySet()}) is used to track the unfinished instructions. An instruction
     * is added to the set upon its construction and is removed when the instruction finished callback method is called.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    public void initialize() {
        if (RecordExecution) {
            executionRecord = new ConcurrentLinkedQueue<>();
        }
        executor = new DataMiningExecutorService(ExecutorPoolSize);
        processors = new ArrayList<>();
        unfinishedInstructions = ConcurrentHashMap.newKeySet();
        
        Class<Iterable<String>> dataSourceType = (Class<Iterable<String>>)(Class<?>) Iterable.class;
        Class<GroupedDataEntry<Integer>> groupedType = (Class<GroupedDataEntry<Integer>>)(Class<?>) GroupedDataEntry.class;
        Class<HashSet<Integer>> resultType = (Class<HashSet<Integer>>)(Class<?>) HashSet.class;
        
        Collection<String> dataSource = new ArrayList<>(DataSourceSize);
        for (int i = 0; i < DataSourceSize; i++) {
            dataSource.add(GroupKeyPrefix + i);
        }
        query = new ProcessorQuery<HashSet<Integer>, Iterable<String>>(dataSource, resultType) {
            @Override
            protected Processor<Iterable<String>, ?> createChainAndReturnFirstProcessor(Processor<Map<GroupKey, HashSet<Integer>>, Void> resultReceiver) {
                Processor<GroupedDataEntry<Integer>, Map<GroupKey, HashSet<Integer>>> aggregator = new ParallelGroupedDataCollectingAsSetProcessor<Integer>(executor, Collections.singleton(resultReceiver)) {
                    @Override
                    protected ProcessorInstruction<Map<GroupKey, HashSet<Integer>>> createInstruction(GroupedDataEntry<Integer> element) {
                        AbstractProcessorInstruction<Map<GroupKey, HashSet<Integer>>> instruction = (AbstractProcessorInstruction<Map<GroupKey, HashSet<Integer>>>) super.createInstruction(element);
                        StatefulProcessorInstruction<Map<GroupKey, HashSet<Integer>>> statefulInstruction = new StatefulInstructionWrapper<>(instruction);
                        unfinishedInstructions.add(statefulInstruction);
                        return statefulInstruction;
                    }
                    
                    @Override
                    protected void storeElement(GroupedDataEntry<Integer> element) {
                        logExecution("Storing " + element);
                        super.storeElement(element);
                    }
                    
                    @Override
                    public void afterInstructionFinished(ProcessorInstruction<Map<GroupKey, HashSet<Integer>>> instruction) {
                        super.afterInstructionFinished(instruction);
                        if (canProcessElements()) {
                            unfinishedInstructions.remove(instruction);
                        }
                    }
                };
                
                Processor<Element, GroupedDataEntry<Integer>> grouper = new AbstractParallelProcessor<Element, GroupedDataEntry<Integer>>(Element.class, groupedType, executor, Collections.singleton(aggregator)) {
                    @Override
                    protected ProcessorInstruction<GroupedDataEntry<Integer>> createInstruction(Element element) {
                        StatefulProcessorInstruction<GroupedDataEntry<Integer>> instruction = new StatefulProcessorInstruction<GroupedDataEntry<Integer>>(this, ProcessorInstructionPriority.Grouping) {
                            @Override
                            protected GroupedDataEntry<Integer> internalComputeResult() throws Exception {
                                logExecution("Grouping " + element);
                                return new GroupedDataEntry<>(new GenericGroupKey<>(element.getName()), element.getValue());
                            }
                        };
                        unfinishedInstructions.add(instruction);
                        return instruction;
                    }
                    
                    @Override
                    public void afterInstructionFinished(ProcessorInstruction<GroupedDataEntry<Integer>> instruction) {
                        super.afterInstructionFinished(instruction);
                        if (canProcessElements()) {
                            unfinishedInstructions.remove(instruction);
                        }
                    }
                    
                    @Override
                    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) { }
                };
                
                Processor<Element, Element> heavyLoadProcessor = new AbstractParallelProcessor<Element, Element>(Element.class, Element.class, executor, Collections.singleton(grouper)) {
                    @Override
                    protected ProcessorInstruction<Element> createInstruction(Element element) {
                        StatefulProcessorInstruction<Element> instruction = new HeavyLoadInstruction(this,
                                ProcessorInstructionPriority.Extraction, HeavyLoadInstructionStepDuration,
                                HeavyLoadInstructionNumberOfSteps, element, TestAbortingHeavyLoadQuery.this::logExecution);
                        unfinishedInstructions.add(instruction);
                        return instruction;
                    }
                    
                    @Override
                    public void afterInstructionFinished(ProcessorInstruction<Element> instruction) {
                        super.afterInstructionFinished(instruction);
                        if (canProcessElements()) {
                            unfinishedInstructions.remove(instruction);
                        }
                    }
                    
                    @Override
                    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) { }
                };
                
                Processor<String, Element> retriever1 = new AbstractRetrievalProcessor<String, Element>(String.class, Element.class, executor, Collections.singleton(heavyLoadProcessor), 1, "") {
                    @Override
                    protected ProcessorInstruction<Element> createInstruction(String element) {
                        AbstractProcessorInstruction<Element> instruction = (AbstractProcessorInstruction<Element>) super.createInstruction(element);
                        StatefulProcessorInstruction<Element> statefulInstruction = new StatefulInstructionWrapper<>(instruction);
                        unfinishedInstructions.add(statefulInstruction);
                        return statefulInstruction;
                    }
                    
                    @Override
                    protected Iterable<Element> retrieveData(String element) {
                        int count = ExecutorPoolSize;
                        logExecution("Retrieving " + count + " elements for " + element);
                        Collection<Element> data = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            data.add(new Element(element, i));
                        }
                        return data;
                    }
                    
                    @Override
                    public void instructionSucceeded(Element result) {
                        logExecution("Group retrieval finished");
                        super.instructionSucceeded(result);
                    }
                    
                    @Override
                    public void afterInstructionFinished(ProcessorInstruction<Element> instruction) {
                        super.afterInstructionFinished(instruction);
                        if (canProcessElements()) {
                            unfinishedInstructions.remove(instruction);
                        }
                    }
                };
                Processor<Iterable<String>, String> retriever0 = new AbstractRetrievalProcessor<Iterable<String>, String>(dataSourceType, String.class, executor, Collections.singleton(retriever1), 0, "") {
                    @Override
                    protected ProcessorInstruction<String> createInstruction(Iterable<String> element) {
                        AbstractProcessorInstruction<String> instruction = (AbstractProcessorInstruction<String>) super.createInstruction(element);
                        StatefulProcessorInstruction<String> statefulInstruction = new StatefulInstructionWrapper<>(instruction);
                        unfinishedInstructions.add(statefulInstruction);
                        return statefulInstruction;
                    }
                    
                    @Override
                    protected Iterable<String> retrieveData(Iterable<String> element) {
                        logExecution("Retrieving data from data source");
                        return element;
                    }
                    
                    @Override
                    public void instructionSucceeded(String result) {
                        logExecution("Data source retrieval finished");
                        super.instructionSucceeded(result);
                    }
                    
                    @Override
                    public void afterInstructionFinished(ProcessorInstruction<String> instruction) {
                        super.afterInstructionFinished(instruction);
                        if (canProcessElements()) {
                            unfinishedInstructions.remove(instruction);
                        }
                    }
                };
                processors.add(retriever0);
                processors.add(retriever1);
                processors.add(heavyLoadProcessor);
                processors.add(grouper);
                processors.add(aggregator);
                return retriever0;
            }
        };
    }
    
    private static class HeavyLoadInstruction extends StatefulBlockingInstruction<Element> {

        private final Consumer<String> recorder;

        public HeavyLoadInstruction(ProcessorInstructionHandler<Element> handler, ProcessorInstructionPriority priority,
                long stepDuration, int numberOfSteps, Element result, Consumer<String> recorder) {
            super(handler, priority, stepDuration, numberOfSteps, result);
            this.recorder = recorder;
        }
        
        @Override
        public void run() {
            recorder.accept("Executing heavy load instruction for " + result);
            super.run();
        }
        
        @Override
        protected void actionBeforeBlock() {
            recorder.accept("Starting work for heavy load instruction for " + result);
        }
        
        @Override
        protected void actionBeforeAbort() {
            recorder.accept("Aborting heavy load instruction for " + result);
        }
        
        @Override
        protected void actionAfterBlock() {
            recorder.accept("Finished heavy load instruction for " + result);
        }
        
    }
    
    private static class StatefulInstructionWrapper<ResultType> extends StatefulProcessorInstruction<ResultType> {
        
        private static Method computeResult;
        
        private final AbstractProcessorInstruction<ResultType> instruction;

        public StatefulInstructionWrapper(AbstractProcessorInstruction<ResultType> instruction) {
            super(instruction.getHandler(), instruction.getPriority());
            this.instruction = instruction;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected ResultType internalComputeResult() throws Exception {
            if (computeResult == null) {
                computeResult = AbstractProcessorInstruction.class.getDeclaredMethod("computeResult");
                computeResult.setAccessible(true);
            }
            return (ResultType) computeResult.invoke(instruction);
        }

    }
    
    @AfterEach
    public void resetComputeResultAccessibility() throws SecurityException, NoSuchMethodException {
        AbstractProcessorInstruction.class.getDeclaredMethod("computeResult").setAccessible(false);
    }
    
    /**
     * Simple data type consisting of a name and value.
     */
    private static class Element {
        
        private final String name;
        private final int value;
        
        public Element(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return getName() + "-" + getValue();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + value;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Element other = (Element) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (value != other.value)
                return false;
            return true;
        }
        
    }

}
