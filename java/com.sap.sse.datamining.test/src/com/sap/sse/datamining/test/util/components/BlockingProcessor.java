package com.sap.sse.datamining.test.util.components;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.impl.components.AbstractParallelProcessor;
import com.sap.sse.datamining.impl.components.AbstractProcessorInstruction;

public abstract class BlockingProcessor<InputType, ResultType> extends AbstractParallelProcessor<InputType, ResultType> {
    private final long timeToBlockInMillis;

    public BlockingProcessor(Class<InputType> inputType, Class<ResultType> resultType,
                             ExecutorService executor, Collection<Processor<ResultType, ?>> resultReceivers,
                             long timeToBlockInMillis) {
        super(inputType, resultType, executor, resultReceivers);
        this.timeToBlockInMillis = timeToBlockInMillis;
    }

    @Override
    protected ProcessorInstruction<ResultType> createInstruction(InputType element) {
        return new AbstractProcessorInstruction<ResultType>() {
            @Override
            public ResultType computeResult() throws Exception {
                Thread.sleep(timeToBlockInMillis);
                return createResult(element);
            }
        };
    }
    
    protected abstract ResultType createResult(InputType element);

    @Override
    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
    }
    
}