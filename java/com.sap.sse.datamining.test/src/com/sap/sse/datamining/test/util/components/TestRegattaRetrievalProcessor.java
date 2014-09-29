package com.sap.sse.datamining.test.util.components;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractSimpleRetrievalProcessor;
import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Regatta;

public class TestRegattaRetrievalProcessor extends
        AbstractSimpleRetrievalProcessor<Collection<Test_Regatta>, Test_Regatta> {

    @SuppressWarnings("unchecked")
    public TestRegattaRetrievalProcessor(ExecutorService executor, Collection<Processor<Test_Regatta>> resultReceivers) {
        super((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, executor, resultReceivers);
    }

    @Override
    protected Iterable<Test_Regatta> retrieveData(Collection<Test_Regatta> regattas) {
        return regattas;
    }

}
