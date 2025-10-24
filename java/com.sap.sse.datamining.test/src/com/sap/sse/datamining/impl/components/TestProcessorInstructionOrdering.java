package com.sap.sse.datamining.impl.components;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.PriorityQueue;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.test.util.components.NullProcessorInstruction;

public class TestProcessorInstructionOrdering {

    @Test
    public void testOrderInPriorityQueue() {
        Queue<NullProcessorInstruction<?>> priorityQueue = new PriorityQueue<>();

        priorityQueue.add(new NullProcessorInstruction<>(null, ProcessorInstructionPriority.createRetrievalPriority(0)));
        priorityQueue.add(new NullProcessorInstruction<>(null, ProcessorInstructionPriority.createRetrievalPriority(1)));
        priorityQueue.add(new NullProcessorInstruction<>(null, ProcessorInstructionPriority.Grouping));
        priorityQueue.add(new NullProcessorInstruction<>(null, ProcessorInstructionPriority.Extraction));
        priorityQueue.add(new NullProcessorInstruction<>(null, ProcessorInstructionPriority.Filtration));
        priorityQueue.add(new NullProcessorInstruction<>(null, ProcessorInstructionPriority.Aggregation));

        assertThat(priorityQueue.poll().getPriority(), is(0));
        assertThat(priorityQueue.poll().getPriority(), is(1));
        assertThat(priorityQueue.poll().getPriority(), is(1));
        assertThat(priorityQueue.poll().getPriority(), is(2));
        assertThat(priorityQueue.poll().getPriority(), is(Integer.MAX_VALUE - 1));
        assertThat(priorityQueue.poll().getPriority(), is(Integer.MAX_VALUE));
    }

}
