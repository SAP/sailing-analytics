package com.sap.sse.datamining.impl.components.management;

import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.test.domain.Test_Competitor;
import com.sap.sse.datamining.test.domain.Test_Named;
import com.sap.sse.datamining.test.domain.Test_Race;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.datamining.test.util.components.aggregators.TestAnyDataTypeAggregationProcessorDummy;
import com.sap.sse.datamining.test.util.components.aggregators.TestCompetitorAggregationProcessorDummy;
import com.sap.sse.datamining.test.util.components.aggregators.TestNamedAggregationProcessorDummy;
import com.sap.sse.datamining.test.util.components.aggregators.TestRaceAggregationProcessorDummy;

public class TestAggregationProcessorManagement {
    
    private static AggregationProcessorDefinition<Object, Double> anyDataAggregatorDefinition;
    private static AggregationProcessorDefinition<Test_Named, Test_Named> namedAggregatorDefinition;
    private static AggregationProcessorDefinition<Test_Race,Test_Race> raceAggregatorDefinition;
    private static AggregationProcessorDefinition<Test_Competitor,Test_Competitor> competitorAggregatorDefinition;

    @BeforeAll
    public static void initializeAggregationProcessorDefinitions() {
        anyDataAggregatorDefinition = new SimpleAggregationProcessorDefinition<>(Object.class, Double.class, "AnyDataAggregator",
                TestAnyDataTypeAggregationProcessorDummy.class);
        namedAggregatorDefinition = new SimpleAggregationProcessorDefinition<>(Test_Named.class, Test_Named.class,
                "NamedAggregator", TestNamedAggregationProcessorDummy.class);
        raceAggregatorDefinition = new SimpleAggregationProcessorDefinition<>(Test_Race.class, Test_Race.class,
                "RaceAggregator", TestRaceAggregationProcessorDummy.class);
        competitorAggregatorDefinition = new SimpleAggregationProcessorDefinition<>(Test_Competitor.class, Test_Competitor.class,
                "CompetitorAggregator", TestCompetitorAggregationProcessorDummy.class); 
    }

    @Test
    public void testAggregationProcessorRegistration() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        
        Date beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerAggregationProcessor(anyDataAggregatorDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), Matchers.is(true));
        
        beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerAggregationProcessor(anyDataAggregatorDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), Matchers.is(false));
    }

    @Test
    public void testAggregationProcessorUnregistration() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        server.registerAggregationProcessor(anyDataAggregatorDefinition);
        
        Date beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterAggregationProcessor(anyDataAggregatorDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), Matchers.is(true));
        
        beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterAggregationProcessor(anyDataAggregatorDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), Matchers.is(false));
    }
    
    @Test
    public void testGetByExtractedType() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        server.registerAggregationProcessor(anyDataAggregatorDefinition);
        server.registerAggregationProcessor(namedAggregatorDefinition);
        server.registerAggregationProcessor(raceAggregatorDefinition);
        server.registerAggregationProcessor(competitorAggregatorDefinition);
        
        Collection<AggregationProcessorDefinition<?, ?>> expectedDefinitions = new HashSet<>();
        expectedDefinitions.add(anyDataAggregatorDefinition);
        expectedDefinitions.add(namedAggregatorDefinition);
        expectedDefinitions.add(raceAggregatorDefinition);
        assertThat(server.getAggregationProcessorDefinitions(Test_Race.class), Matchers.is(expectedDefinitions));
        
        expectedDefinitions = new HashSet<>();
        expectedDefinitions.add(anyDataAggregatorDefinition);
        expectedDefinitions.add(namedAggregatorDefinition);
        assertThat(server.getAggregationProcessorDefinitions(Test_Named.class), Matchers.is(expectedDefinitions));
        
        expectedDefinitions = new HashSet<>();
        expectedDefinitions.add(anyDataAggregatorDefinition);
        assertThat(server.getAggregationProcessorDefinitions(Object.class), Matchers.is(expectedDefinitions));
    }
    
    @Test
    public void testGetByExtractedTypeAndMessageKey() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        server.registerAggregationProcessor(anyDataAggregatorDefinition);
        server.registerAggregationProcessor(namedAggregatorDefinition);
        server.registerAggregationProcessor(raceAggregatorDefinition);
        server.registerAggregationProcessor(competitorAggregatorDefinition);

        assertThat(server.getAggregationProcessorDefinition(Test_Race.class, "RaceAggregator"), Matchers.is(raceAggregatorDefinition));
        assertThat(server.getAggregationProcessorDefinition(Test_Competitor.class, "CompetitorAggregator"), Matchers.is(competitorAggregatorDefinition));
    }
    
    @Test
    public void testGetByDTO() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        server.registerAggregationProcessor(anyDataAggregatorDefinition);
        server.registerAggregationProcessor(namedAggregatorDefinition);
        server.registerAggregationProcessor(raceAggregatorDefinition);
        
        AggregationProcessorDefinitionDTO anyDataAggregatorDefinitionDTO = TestsUtil.getDTOFactory().createAggregationProcessorDefinitionDTO(anyDataAggregatorDefinition);
        assertThat(server.getAggregationProcessorDefinitionForDTO(anyDataAggregatorDefinitionDTO), Matchers.is(anyDataAggregatorDefinition));
        
        AggregationProcessorDefinitionDTO namedDataAggregatorDefinitionDTO = TestsUtil.getDTOFactory().createAggregationProcessorDefinitionDTO(namedAggregatorDefinition);
        assertThat(server.getAggregationProcessorDefinitionForDTO(namedDataAggregatorDefinitionDTO), Matchers.is(namedAggregatorDefinition));
        
        AggregationProcessorDefinitionDTO competitorAggregatorDefinitionDTO = TestsUtil.getDTOFactory().createAggregationProcessorDefinitionDTO(competitorAggregatorDefinition);
        assertThat(server.getAggregationProcessorDefinitionForDTO(competitorAggregatorDefinitionDTO), nullValue());
        
        assertThat(server.getAggregationProcessorDefinitionForDTO(null), nullValue());
        
        AggregationProcessorDefinitionDTO aggregatorDTO = new AggregationProcessorDefinitionDTO("Not relevant", "Impossible Class", "Impossible Class", "Not relevant");
        try {
            server.getAggregationProcessorDefinitionForDTO(aggregatorDTO);
            fail("An IllegalArgumentException is expected.");
        } catch (IllegalArgumentException e) {
            // Exception is expected
        }
    }

}
