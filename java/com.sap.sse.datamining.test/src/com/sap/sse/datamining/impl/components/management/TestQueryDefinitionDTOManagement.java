package com.sap.sse.datamining.impl.components.management;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.exceptions.DataMiningComponentAlreadyRegisteredForKeyException;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.PredefinedQueryIdentifier;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.ModifiableStatisticQueryDefinitionDTO;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;

public class TestQueryDefinitionDTOManagement {

    private static StatisticQueryDefinitionDTO pseudoQueryDefinition;
    private static StatisticQueryDefinitionDTO differentQueryDefinition;

    @BeforeAll
    public static void initializeQueryDefinitions() {
        FunctionDTO statisticToCalculate = new FunctionDTO(false, "Test", "Test", "Test", new ArrayList<String>(), "Test", 0);
        AggregationProcessorDefinitionDTO aggregatorDefinition = new AggregationProcessorDefinitionDTO("Test", "Test", "Test", "Test");
        DataRetrieverChainDefinitionDTO dataRetrieverChainDefinition = new DataRetrieverChainDefinitionDTO("Test", "Test", new ArrayList<DataRetrieverLevelDTO>());
        pseudoQueryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statisticToCalculate, aggregatorDefinition, dataRetrieverChainDefinition);

        statisticToCalculate = new FunctionDTO(false, "Different", "Different", "Different", new ArrayList<String>(), "Different", 0);
        aggregatorDefinition = new AggregationProcessorDefinitionDTO("Different", "Different", "Different", "Different");
        dataRetrieverChainDefinition = new DataRetrieverChainDefinitionDTO("Different", "Different", new ArrayList<DataRetrieverLevelDTO>());
        differentQueryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statisticToCalculate, aggregatorDefinition, dataRetrieverChainDefinition);
    }

    @Test
    public void testQueryDefinitionRegistration() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        
        Date beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), pseudoQueryDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(true));
        
        beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), pseudoQueryDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(false));
    }
    
    @Test
    public void testQueryDefinitionRegistrationWithConflict() {
        assertThrows(DataMiningComponentAlreadyRegisteredForKeyException.class, () -> {
            ModifiableDataMiningServer server = TestsUtil.createNewServer();
            server.registerPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), pseudoQueryDefinition);
            server.registerPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), differentQueryDefinition);
        });
    }

    @Test
    public void testQueryDefinitionUnregistration() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        server.registerPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), pseudoQueryDefinition);
        
        Date beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), pseudoQueryDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(true));
        
        beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), pseudoQueryDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(false));
    }
    
    @Test
    public void testGetByIdentifier() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        server.registerPredefinedQueryDefinition(new PredefinedQueryIdentifier("Test", "Description"), pseudoQueryDefinition);
        server.registerPredefinedQueryDefinition(new PredefinedQueryIdentifier("Different", "Description"), differentQueryDefinition);

        assertThat(server.getPredefinedQueryDefinitionDTO(new PredefinedQueryIdentifier("Test", "Description")), is(pseudoQueryDefinition));
        assertThat(server.getPredefinedQueryDefinitionDTO(new PredefinedQueryIdentifier("Different", "Description")), is(differentQueryDefinition));
        
        assertThat(server.getPredefinedQueryDefinitionDTO(new PredefinedQueryIdentifier("Test", "")), is(pseudoQueryDefinition));
        assertThat(server.getPredefinedQueryDefinitionDTO(new PredefinedQueryIdentifier("Different", "")), is(differentQueryDefinition));
    }

}
