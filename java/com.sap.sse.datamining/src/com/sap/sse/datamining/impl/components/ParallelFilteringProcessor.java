package com.sap.sse.datamining.impl.components;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;

/**
 * Filters the given input elements by a {@link FilterCriterion}, so that only elements, that match the 
 * filter criterion are forwarded to the result receivers.
 * 
 * @author Lennart Hensler (D054527)
 *
 * @param <InputType> The type of the data to filter.
 * 
 * @see com.sap.sse.datamining.impl.components com.sap.sse.datamining.impl.components for general information about Processors.
 */
public class ParallelFilteringProcessor<InputType> extends AbstractParallelProcessor<InputType, InputType> {

    private final FilterCriterion<InputType> filterCriterion;
    private final AtomicInteger filteredDataAmount;

    public ParallelFilteringProcessor(Class<InputType> inputType, ExecutorService executor, Collection<Processor<InputType, ?>> resultReceivers, FilterCriterion<InputType> filterCriterion) {
        super(inputType, inputType, executor, resultReceivers);
        this.filterCriterion = filterCriterion;
        filteredDataAmount = new AtomicInteger();
    }

    @Override
    protected ProcessorInstruction<InputType> createInstruction(final InputType element) {
        return new AbstractProcessorInstruction<InputType>(this, ProcessorInstructionPriority.Filtration) {
            @Override
            public InputType computeResult() {
                if (filterCriterion.matches(element)) {
                    return element;
                } else {
                    filteredDataAmount.incrementAndGet();
                    return ParallelFilteringProcessor.super.createInvalidResult();
                }
            }
        };
    }

    @Override
    protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
        int retrievedDataAmount = additionalDataBuilder.getRetrievedDataAmount();
        additionalDataBuilder.setRetrievedDataAmount(retrievedDataAmount - filteredDataAmount.get());
    }
}
