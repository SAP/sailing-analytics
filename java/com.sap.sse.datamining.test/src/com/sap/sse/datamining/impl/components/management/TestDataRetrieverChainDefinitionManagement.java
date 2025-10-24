package com.sap.sse.datamining.impl.components.management;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.datamining.impl.components.SimpleDataRetrieverChainDefinition;
import com.sap.sse.datamining.impl.components.SingleDataRetrieverChainDefinition;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.LocalizedTypeDTO;
import com.sap.sse.datamining.test.data.Test_HasLegOfCompetitorContext;
import com.sap.sse.datamining.test.data.Test_HasRaceContext;
import com.sap.sse.datamining.test.data.Test_HasRaceContextImpl;
import com.sap.sse.datamining.test.domain.Test_Race;
import com.sap.sse.datamining.test.domain.Test_Regatta;
import com.sap.sse.datamining.test.domain.Test_Series;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.datamining.test.util.components.TestLegOfCompetitorWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRaceWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRegattaRetrievalProcessor;

public class TestDataRetrieverChainDefinitionManagement {
    
    private DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_Regatta> regattaRetrieverChainDefinition;
    private DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasRaceContext> raceRetrieverChainDefinition;
    private DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> legRetrieverChainDefinition;
    private ModifiableDataMiningServer server;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void initializeRetrieverChainsAndRegistry() {
        regattaRetrieverChainDefinition = new SingleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Test_Regatta.class, "TestRaceRetrieverChain");
        regattaRetrieverChainDefinition.startWith(TestRegattaRetrievalProcessor.class, Test_Regatta.class, "regatta");
        
        raceRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(regattaRetrieverChainDefinition, Test_HasRaceContext.class, "TestRaceRetrieverChain");
        raceRetrieverChainDefinition.endWith(TestRegattaRetrievalProcessor.class, TestRaceWithContextRetrievalProcessor.class, Test_HasRaceContext.class, "race");

        legRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(raceRetrieverChainDefinition, Test_HasLegOfCompetitorContext.class, "TestLegRetrieverChain");
        legRetrieverChainDefinition.endWith(TestRaceWithContextRetrievalProcessor.class, TestLegOfCompetitorWithContextRetrievalProcessor.class, Test_HasLegOfCompetitorContext.class, "legOfCompetitor");
        
        server = TestsUtil.createNewServer();
        server.registerDataRetrieverChainDefinition(raceRetrieverChainDefinition);
        server.registerDataRetrieverChainDefinition(legRetrieverChainDefinition);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDataRetrieverChainDefinitions() {
        Class<Collection<Test_Regatta>> dataSourceType = (Class<Collection<Test_Regatta>>)(Class<?>) Collection.class;
        
        Collection<DataRetrieverChainDefinition<?, ?>> expectedRetrieverChainDefinitions = new HashSet<>();
        expectedRetrieverChainDefinitions.add(raceRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitions(dataSourceType, Test_HasRaceContext.class), is(expectedRetrieverChainDefinitions));
        
        expectedRetrieverChainDefinitions = new HashSet<>();
        expectedRetrieverChainDefinitions.add(legRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitions(dataSourceType, Test_HasLegOfCompetitorContext.class), is(expectedRetrieverChainDefinitions));
        
        expectedRetrieverChainDefinitions = new HashSet<>();
        expectedRetrieverChainDefinitions.add(raceRetrieverChainDefinition);
        expectedRetrieverChainDefinitions.add(legRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitionsBySourceType(dataSourceType), is(expectedRetrieverChainDefinitions));
        assertThat(server.getDataRetrieverChainDefinitions(), is(expectedRetrieverChainDefinitions));
        
        expectedRetrieverChainDefinitions = new HashSet<>();
        expectedRetrieverChainDefinitions.add(legRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitionsByDataType(Test_HasLegOfCompetitorContext.class), is(expectedRetrieverChainDefinitions));
        
        expectedRetrieverChainDefinitions = new HashSet<>();
        expectedRetrieverChainDefinitions.add(raceRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitionsByDataType(Test_HasRaceContext.class), is(expectedRetrieverChainDefinitions));
    }
    
    @Test
    public void testGetDataRetrieverChainDefinitionForDTO() {
        DataRetrieverChainDefinitionDTO raceRetrieverChainDTO = TestsUtil.getDTOFactory().createDataRetrieverChainDefinitionDTO(raceRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitionForDTO(raceRetrieverChainDTO), is(raceRetrieverChainDefinition));

        DataRetrieverChainDefinitionDTO legRetrieverChainDTO = TestsUtil.getDTOFactory().createDataRetrieverChainDefinitionDTO(legRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitionForDTO(legRetrieverChainDTO), is(legRetrieverChainDefinition));
        
        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<DataRetrieverLevelDTO>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, TestRegattaRetrievalProcessor.class.getName(), new LocalizedTypeDTO("Not relevant", "Not relevant"), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, TestSeriesRetrievalProcessor.class.getName(), new LocalizedTypeDTO("Not relevant", "Not relevant"), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(2, TestRaceFromSeriesRetrievalProcessor.class.getName(), new LocalizedTypeDTO(Test_HasRaceContext.class.getName(), "Not relevant"), null));
        DataRetrieverChainDefinitionDTO dtoWithMoreLevels = new DataRetrieverChainDefinitionDTO("Not relevant", Collection.class.getName(), retrieverLevels);
        assertThat(server.getDataRetrieverChainDefinitionForDTO(dtoWithMoreLevels), nullValue());
        
        retrieverLevels = new ArrayList<DataRetrieverLevelDTO>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, TestRaceWithContextRetrievalProcessor.class.getName(), new LocalizedTypeDTO(Test_Race.class.getName(), "Race"), null));
        DataRetrieverChainDefinitionDTO dtoWithLessLevels = new DataRetrieverChainDefinitionDTO("Not relevant", Test_Regatta.class.getName(), retrieverLevels);
        assertThat(server.getDataRetrieverChainDefinitionForDTO(dtoWithLessLevels), nullValue());
        
        retrieverLevels = new ArrayList<DataRetrieverLevelDTO>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, TestRegattaRetrievalProcessor.class.getName(), new LocalizedTypeDTO("Not relevant", "Not relevant"), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, TestRaceFromSeriesRetrievalProcessor.class.getName(), new LocalizedTypeDTO(Test_HasRaceContext.class.getName(), "Not relevant"), null));
        DataRetrieverChainDefinitionDTO dtoWithUnmatchingLevels = new DataRetrieverChainDefinitionDTO("Not relevant", Collection.class.getName(), retrieverLevels);
        assertThat(server.getDataRetrieverChainDefinitionForDTO(dtoWithUnmatchingLevels), nullValue());
        
        assertThat(server.getDataRetrieverChainDefinitionForDTO(null), nullValue());
        
        retrieverLevels = new ArrayList<DataRetrieverLevelDTO>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, "Not relevant", new LocalizedTypeDTO("Impossible Class", "Impossible Class"), null));
        DataRetrieverChainDefinitionDTO errorDTO = new DataRetrieverChainDefinitionDTO("Impossible Class", "Impossible Class", retrieverLevels);
        try {
            server.getDataRetrieverChainDefinitionForDTO(errorDTO);
            fail("An IllegalArgumentException is expected.");
        } catch (IllegalArgumentException e) {
            // Exception is expected
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveDataRetrieverChainDefinition() {
        Class<Collection<Test_Regatta>> dataSourceType = (Class<Collection<Test_Regatta>>)(Class<?>) Collection.class;

        Date beforeUnregistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterDataRetrieverChainDefinition(legRetrieverChainDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeUnregistration), is(true));
        
        beforeUnregistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterDataRetrieverChainDefinition(legRetrieverChainDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeUnregistration), is(false));
        
        Collection<DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext>> expectedEmptyDataRetrieverChainDefinitions = new HashSet<>();
        assertThat(server.getDataRetrieverChainDefinitions(dataSourceType, Test_HasLegOfCompetitorContext.class), is(expectedEmptyDataRetrieverChainDefinitions));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingDataRetrieverChainDefinitionTwice() {
        Class<Collection<Test_Regatta>> dataSourceType = (Class<Collection<Test_Regatta>>)(Class<?>) Collection.class;

        Date beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerDataRetrieverChainDefinition(legRetrieverChainDefinition);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(false));
        
        Collection<DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext>> expectedDataRetrieverChainDefinitions = new HashSet<>();
        expectedDataRetrieverChainDefinitions.add(legRetrieverChainDefinition);
        assertThat(server.getDataRetrieverChainDefinitions(dataSourceType, Test_HasLegOfCompetitorContext.class), is(expectedDataRetrieverChainDefinitions));
    }

    @Test
    public void testGetDataRetrieverChainDefinitionsForNotRegisteredChain() {
        Collection<DataRetrieverChainDefinition<Test_Regatta, Test_HasRaceContextImpl>> expectedEmptyDataRetrieverChainDefinitions = new HashSet<>();
        assertThat(server.getDataRetrieverChainDefinitions(Test_Regatta.class, Test_HasRaceContextImpl.class), is(expectedEmptyDataRetrieverChainDefinitions));
    }
    
    private class TestSeriesRetrievalProcessor extends AbstractRetrievalProcessor<Test_Regatta, Test_Series> {

        public TestSeriesRetrievalProcessor(ExecutorService executor,
                Collection<Processor<Test_Series, ?>> resultReceivers, int retrievalLevel, String retrievedDataTypeMessageKey) {
            super(Test_Regatta.class, Test_Series.class, executor, resultReceivers, retrievalLevel, retrievedDataTypeMessageKey);
        }

        @Override
        protected Iterable<Test_Series> retrieveData(Test_Regatta regatta) {
            return regatta.getSeries();
        }

    }
    
    private class TestRaceFromSeriesRetrievalProcessor extends AbstractRetrievalProcessor<Test_Series, Test_HasRaceContext> {

        public TestRaceFromSeriesRetrievalProcessor(ExecutorService executor,
                Collection<Processor<Test_HasRaceContext, ?>> resultReceivers, int retrievalLevel, String retrievedDataTypeMessageKey) {
            super(Test_Series.class, Test_HasRaceContext.class, executor, resultReceivers, retrievalLevel, retrievedDataTypeMessageKey);
        }

        @Override
        protected Iterable<Test_HasRaceContext> retrieveData(Test_Series series) {
            Collection<Test_HasRaceContext> racesWithContext = new ArrayList<>();
            for (Test_Race race : series.getRaces()) {
                Test_Regatta regatta = series.getRegatta();
                racesWithContext.add(new Test_HasRaceContextImpl(regatta, race, regatta.getBoatClass(), regatta.getYear()));
            }
            return racesWithContext;
        }

    }

}
