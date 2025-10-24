package com.sap.sse.datamining.test.util.components;

import com.sap.sse.datamining.components.ProcessorInstructionHandler;
import com.sap.sse.datamining.impl.components.ProcessorInstructionPriority;

public class StatefulBlockingInstruction<ResultType> extends StatefulProcessorInstruction<ResultType> {
    
    protected final long stepDuration;
    protected final int numberOfSteps;
    protected final ResultType result;
    
    private boolean computeResultWasAborted;

    public StatefulBlockingInstruction(ProcessorInstructionHandler<ResultType> handler, long stepDuration, int numberOfSteps) {
        this(handler, 0, stepDuration, numberOfSteps, null);
    }
    
    public StatefulBlockingInstruction(ProcessorInstructionHandler<ResultType> handler, ProcessorInstructionPriority priority, long stepDuration, int numberOfSteps, ResultType result) {
        this(handler, priority.asIntValue(), stepDuration, numberOfSteps, result);
    }
    
    public StatefulBlockingInstruction(ProcessorInstructionHandler<ResultType> handler, int priority, long stepDuration, int numberOfSteps, ResultType result) {
        super(handler, priority);
        this.stepDuration = stepDuration;
        this.numberOfSteps = numberOfSteps;
        this.result = result;
    }

    @Override
    protected ResultType internalComputeResult() throws Exception {
        if (getTotalBlockDuration() > 0) {
            actionBeforeBlock();
            for (int i = 0; !checkIsAborted() && i < numberOfSteps; i++) {
                Thread.sleep(stepDuration);
            }
            actionAfterBlock();
        }
        return result;
    }
    
    private boolean checkIsAborted() {
        final boolean result = isAborted();
        if (result) {
            actionBeforeAbort();
            computeResultWasAborted = true;
        }
        return result;
    }
    
    protected void actionBeforeBlock() { }
    protected void actionBeforeAbort() { }
    protected void actionAfterBlock() { }
    
    public long getTotalBlockDuration() {
        return stepDuration * numberOfSteps;
    }
    
    public boolean computeResultWasAborted() {
        return computeResultWasAborted;
    }
}