package com.sap.sse.datamining.impl.components;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.factories.ProcessorFactory;
import com.sap.sse.datamining.functions.ParameterProvider;
import com.sap.sse.datamining.functions.ParameterizedFunction;
import com.sap.sse.datamining.impl.functions.MethodWrappingFunction;
import com.sap.sse.datamining.impl.functions.SimpleParameterizedFunction;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.shared.impl.CompoundGroupKey;
import com.sap.sse.datamining.test.util.ComponentTestsUtil;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;
import com.sap.sse.datamining.test.util.components.Number;

public class TestParallelMultiDimensionalGroupingProcessor {
    
    private final static ProcessorFactory processorFactory = ComponentTestsUtil.getProcessorFactory();
    
    private Processor<Number, GroupedDataEntry<Number>> processor;
    private Collection<Processor<GroupedDataEntry<Number>, ?>> receivers;

    private GroupedDataEntry<Number> groupedElement;

    @BeforeEach
    public void intializeProcessor() throws IllegalArgumentException, NoSuchMethodException, SecurityException {
        @SuppressWarnings("unchecked")
        Processor<GroupedDataEntry<Number>, Void> receiver = new NullProcessor<GroupedDataEntry<Number>, Void>((Class<GroupedDataEntry<Number>>)(Class<?>) GroupedDataEntry.class, Void.class) {
            @Override
            public void processElement(GroupedDataEntry<Number> element) {
                groupedElement = element;
            }
        };
        
        receivers = new ArrayList<>();
        receivers.add(receiver);
        
        List<ParameterizedFunction<?>> dimensions = new ArrayList<>();
        dimensions.add(new SimpleParameterizedFunction<>(new MethodWrappingFunction<>(Number.class.getMethod("getLength", new Class<?>[0]), int.class), ParameterProvider.NULL));
        dimensions.add(new SimpleParameterizedFunction<>(new MethodWrappingFunction<>(Number.class.getMethod("getCrossSum", new Class<?>[0]), int.class), ParameterProvider.NULL));
        
        processor = ComponentTestsUtil.getProcessorFactory().createGroupingProcessor(Number.class, receivers, dimensions);
    }
    
    @Test
    public void testConstructionWithNullDimensions() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ParallelMultiDimensionsValueNestingGroupingProcessor<>(Number.class, ConcurrencyTestsUtil.getSharedExecutor(), receivers, null);
        });
    }
    
    @Test
    public void testConstructionWithEmptyDimensions() {
        assertThrows(IllegalArgumentException.class, () -> {
            List<ParameterizedFunction<?>> dimensions = new ArrayList<>();
            processorFactory.createGroupingProcessor(Number.class, receivers, dimensions);
        });
    }
    
    @Test
    public void testConstructionWithFunctionsInsteadOfDimensions() {
        assertThrows(IllegalArgumentException.class, () -> {
            List<ParameterizedFunction<?>> functions = new ArrayList<>();
            Method method = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("sideEffectFreeValue");
            functions.add(new SimpleParameterizedFunction<>(FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(method), ParameterProvider.NULL));
            processorFactory.createGroupingProcessor(Number.class, receivers, functions);
        });
    }

    @Test
    public void testGroupKeyGeneration() {
        Number number = new Number(1111);
        processor.processElement(number);
        ConcurrencyTestsUtil.sleepFor(200); //Giving the processor time to finish the instruction
        verifyGroupedElement(number);
    }

    private void verifyGroupedElement(Number originElement) {
        List<GroupKey> keys = new ArrayList<>();
        keys.add(new GenericGroupKey<Object>(originElement.getLength()));
        keys.add(new GenericGroupKey<Object>(originElement.getCrossSum()));
        GroupKey expectedKey = new CompoundGroupKey(keys);
        assertThat(groupedElement.getKey(), is(expectedKey));
    }

}
