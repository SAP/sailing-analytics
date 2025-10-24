package com.sap.sse.datamining.impl.components;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.test.util.components.aggregators.TestAggregationProcessorDummyWithInvalidConstructor;

public class TestAggregationProcessorDefinition {

    @Test
    public void testCreationWithInvalidConstructor() {
        try {
            new SimpleAggregationProcessorDefinition<>(Object.class, Double.class, "InvalidAggregatorDefinition", TestAggregationProcessorDummyWithInvalidConstructor.class);
            fail("Expected IllegalArgumentException for invalid constructor");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

}
