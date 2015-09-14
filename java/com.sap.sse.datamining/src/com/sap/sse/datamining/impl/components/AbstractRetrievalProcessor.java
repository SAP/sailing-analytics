package com.sap.sse.datamining.impl.components;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;

public abstract class AbstractRetrievalProcessor<InputType, ResultType> extends AbstractParallelProcessor<InputType, ResultType> {

    private final int retrievalLevel;
    private final AtomicInteger retrievedDataAmount;

    /**
     * 
     * @param inputType
     * @param resultType
     * @param executor
     * @param resultReceivers
     * @param retrievalLevel The position of this retriever in it's chain. <code>0</code> represents the first.
     */
    public AbstractRetrievalProcessor(Class<InputType> inputType, Class<ResultType> resultType,
            ExecutorService executor, Collection<Processor<ResultType, ?>> resultReceivers, int retrievalLevel) {
        super(inputType, resultType, executor, resultReceivers);
        this.retrievalLevel = retrievalLevel;
        retrievedDataAmount = new AtomicInteger();
    }

    @Override
    protected ProcessorInstruction<ResultType> createInstruction(final InputType element) {
        return new AbstractProcessorInstruction<ResultType>(ProcessorInstructionPriority.createRetrievalPriority(retrievalLevel)) {
            @Override
            public ResultType computeResult() {
                for (ResultType retrievedElement : retrieveData(element)) {
                    retrievedDataAmount.incrementAndGet();
                    forwardResultToReceivers(retrievedElement);
                }
                return createInvalidResult();
            }
        };
    }

    protected abstract Iterable<ResultType> retrieveData(InputType element);

    @Override
    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
        additionalDataBuilder.setRetrievedDataAmount(retrievedDataAmount.get());
    }

}
