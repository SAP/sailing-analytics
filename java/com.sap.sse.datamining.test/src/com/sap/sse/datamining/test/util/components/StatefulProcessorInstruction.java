package com.sap.sse.datamining.test.util.components;

import com.sap.sse.datamining.components.ProcessorInstructionHandler;
import com.sap.sse.datamining.impl.components.AbstractProcessorInstruction;
import com.sap.sse.datamining.impl.components.ProcessorInstructionPriority;

public abstract class StatefulProcessorInstruction<ResultType> extends AbstractProcessorInstruction<ResultType> {
    
    private volatile boolean runWasCalled;
    private volatile boolean computeResultWasCalled;
    private volatile boolean computeResultWasFinished;

    public StatefulProcessorInstruction(ProcessorInstructionHandler<ResultType> processor, int priority) {
        super(processor, priority);
    }

    public StatefulProcessorInstruction(ProcessorInstructionHandler<ResultType> handler,
            ProcessorInstructionPriority priority) {
        super(handler, priority);
    }

    public StatefulProcessorInstruction(ProcessorInstructionHandler<ResultType> handler) {
        super(handler);
    }
    
    @Override
    public void run() {
        runWasCalled = true;
        super.run();
    }

    @Override
    protected ResultType computeResult() throws Exception {
        computeResultWasCalled = true;
        ResultType result = internalComputeResult();
        computeResultWasFinished = true;
        return result;
    }

    protected abstract ResultType internalComputeResult() throws Exception;

    public boolean runWasCalled() {
        return runWasCalled;
    }

    public boolean computeResultWasCalled() {
        return computeResultWasCalled;
    }

    public boolean computeResultWasFinished() {
        return computeResultWasFinished;
    }
}
