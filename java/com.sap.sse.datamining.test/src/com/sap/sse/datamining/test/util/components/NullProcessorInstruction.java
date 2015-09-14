package com.sap.sse.datamining.test.util.components;

import com.sap.sse.datamining.impl.components.AbstractProcessorInstruction;
import com.sap.sse.datamining.impl.components.ProcessorInstructionPriority;

public class NullProcessorInstruction<ResultType> extends AbstractProcessorInstruction<ResultType> {

    public NullProcessorInstruction(ProcessorInstructionPriority priority) {
        super(priority);
    }

    @Override
    protected ResultType computeResult() throws Exception {
        return null;
    }

}
