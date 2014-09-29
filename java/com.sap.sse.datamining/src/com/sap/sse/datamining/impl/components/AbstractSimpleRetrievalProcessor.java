package com.sap.sse.datamining.impl.components;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.components.Processor;

public abstract class AbstractSimpleRetrievalProcessor<InputType, ResultType> extends
        AbstractRetrievalProcessor<InputType, ResultType, ResultType> {

    public AbstractSimpleRetrievalProcessor(Class<InputType> inputType, ExecutorService executor, Collection<Processor<ResultType>> resultReceivers) {
        super(inputType, executor, resultReceivers);
    }

    @Override
    protected ResultType convertWorkingToResultType(ResultType partialElement) {
        return partialElement;
    }

}
