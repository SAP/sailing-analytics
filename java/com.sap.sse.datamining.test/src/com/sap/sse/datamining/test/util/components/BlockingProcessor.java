package com.sap.sse.datamining.test.util.components;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractSimpleParallelProcessor;

public class BlockingProcessor<InputType, ResultType> extends AbstractSimpleParallelProcessor<InputType, ResultType> {
    private final long timeToBlockInMillis;

    public BlockingProcessor(Class<InputType> inputType, ExecutorService executor, Collection<Processor<ResultType>> resultReceivers, long timeToBlockInMillis) {
        super(inputType, executor, resultReceivers);
        this.timeToBlockInMillis = timeToBlockInMillis;
    }

    @Override
    protected Callable<ResultType> createInstruction(InputType element) {
        return new Callable<ResultType>() {
            @Override
            public ResultType call() throws Exception {
                Thread.sleep(timeToBlockInMillis);
                return null;
            }
        };
    }

    @Override
    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
    }
    
}