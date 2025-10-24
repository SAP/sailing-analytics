package com.sap.sse.datamining.impl.components;

import static com.sap.sse.datamining.test.util.ConcurrencyTestsUtil.getSharedExecutor;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.components.DataRetrieverChainBuilder;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.criterias.AbstractFilterCriterion;
import com.sap.sse.datamining.test.data.Test_HasLegOfCompetitorContext;
import com.sap.sse.datamining.test.data.Test_HasRaceContext;
import com.sap.sse.datamining.test.domain.Test_Regatta;
import com.sap.sse.datamining.test.util.ComponentTestsUtil;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;
import com.sap.sse.datamining.test.util.components.TestLegOfCompetitorWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRaceWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRegattaRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.Test_RetrievalProcessorSettings;
import com.sap.sse.datamining.test.util.components.Test_RetrievalProcessorWithSettings;

public class TestDataRetrieverChainCreation {

    private DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> dataRetrieverChainDefinition;
    
    private FilterCriterion<Test_HasRaceContext> raceFilter;
    private FilterCriterion<Test_HasLegOfCompetitorContext> legOfCompetitorFilter;
    
    private int receivedLegOfCompetitorAmount = 0;
    private Processor<Test_HasLegOfCompetitorContext, Void> legReceiver = new NullProcessor<Test_HasLegOfCompetitorContext, Void>(Test_HasLegOfCompetitorContext.class, Void.class) {
        @Override
        public void processElement(Test_HasLegOfCompetitorContext element) {
            synchronized (this) {
                receivedLegOfCompetitorAmount++;
            }
        }
    };

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void initializeRetrieverChain() {
        dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Test_HasLegOfCompetitorContext.class, "TestRetrieverChain");
        dataRetrieverChainDefinition.startWith(TestRegattaRetrievalProcessor.class, Test_Regatta.class, "regatta");
        
        dataRetrieverChainDefinition.addAfter(TestRegattaRetrievalProcessor.class,
                                              TestRaceWithContextRetrievalProcessor.class,
                                              Test_HasRaceContext.class, "race");
        
        dataRetrieverChainDefinition.endWith(TestRaceWithContextRetrievalProcessor.class,
                                             TestLegOfCompetitorWithContextRetrievalProcessor.class,
                                             Test_HasLegOfCompetitorContext.class, "legOfCompetitor");
    }
    
    @BeforeEach
    public void initializeFilters() {
        raceFilter = new AbstractFilterCriterion<Test_HasRaceContext>(Test_HasRaceContext.class) {
            @Override
            public boolean matches(Test_HasRaceContext element) {
                return element.getBoatClass().getName().equals("49er");
            }
        };
        
        legOfCompetitorFilter = new AbstractFilterCriterion<Test_HasLegOfCompetitorContext>(Test_HasLegOfCompetitorContext.class) {
            @Override
            public boolean matches(Test_HasLegOfCompetitorContext element) {
                return element.getLegNumber() <= 3;
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreationWithExistingChainDefinition() {
        try {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, ?> chainClone = new SimpleDataRetrieverChainDefinition<>(dataRetrieverChainDefinition, Void.class, "TestRetrieverChain");
            assertThat(chainClone.getDataSourceType().equals(dataRetrieverChainDefinition.getDataSourceType()), is(true));
            List<DataRetrieverLevel<?, ?>> cloneDataRetrieverTypesWithInformation = (List<DataRetrieverLevel<?, ?>>) chainClone.getDataRetrieverLevels();
            assertThat(cloneDataRetrieverTypesWithInformation, is(dataRetrieverChainDefinition.getDataRetrieverLevels()));
            assertThat(chainClone, not(dataRetrieverChainDefinition));
            chainClone.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());
            fail("Expected IllegalStateException because the chain definition is already initialized.");
        } catch (IllegalStateException e) {
            // expected
        }
    }
    
    @Test
    public void testThatTheDataRetrieverChainBuilderHasToBeInitialized() {
        DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = dataRetrieverChainDefinition.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());

        try {
            chainBuilder.getCurrentRetrievedDataType();
            fail("An IllegalStateException is expected.");
        } catch (IllegalStateException e) {
        }
        try {
            chainBuilder.setFilter(null);
            fail("An IllegalStateException is expected.");
        } catch (IllegalStateException e) {
        }
        try {
            chainBuilder.setSettings(null);
            fail("An IllegalStateException is expected.");
        } catch (IllegalStateException e) {
        }
        try {
            chainBuilder.addResultReceiver(null);
            fail("An IllegalStateException is expected.");
        } catch (IllegalStateException e) {
        }
        try {
            chainBuilder.build();
            fail("An IllegalStateException is expected.");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testStepByStepDataRetrieverChainCreation() throws InterruptedException {
        DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = dataRetrieverChainDefinition.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());
        chainBuilder.stepFurther(); //Initialization
        
        assertThat(chainBuilder.getCurrentRetrievedDataType().equals(Test_Regatta.class), is(true));
        assertThat(chainBuilder.canStepFurther(), is(true));
        
        chainBuilder.stepFurther().setFilter(raceFilter);
        assertThat(chainBuilder.getCurrentRetrievedDataType().equals(Test_HasRaceContext.class), is(true));
        assertThat(chainBuilder.canStepFurther(), is(true));
        
        chainBuilder.stepFurther().setFilter(legOfCompetitorFilter).addResultReceiver(legReceiver);
        assertThat(chainBuilder.getCurrentRetrievedDataType().equals(Test_HasLegOfCompetitorContext.class), is(true));
        assertThat(chainBuilder.canStepFurther(), is(false));
        
        Processor<Collection<Test_Regatta>, ?> firstRetrieverInChain = chainBuilder.build();
        firstRetrieverInChain.processElement(ComponentTestsUtil.createExampleDataSource());
        firstRetrieverInChain.finish();
        ConcurrencyTestsUtil.sleepFor(100);
        assertThat(receivedLegOfCompetitorAmount, is(3 /*races*/ * 3 /*legs*/ * 4 /*competitors*/));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAddAfterWithoutStarting() {
        assertThrows(IllegalStateException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, ?> dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Void.class, "TestRetrieverChain");
            Class<Processor<Collection<Test_Regatta>, Test_Regatta>> regattaRetrieverClass = (Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) TestRegattaRetrievalProcessor.class;
            Class<Processor<Test_Regatta, Test_HasRaceContext>> raceRetrieverClass = 
                    (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) TestRaceWithContextRetrievalProcessor.class;
            dataRetrieverChainDefinition.addAfter(regattaRetrieverClass,
                                                   raceRetrieverClass,
                                                   Test_HasRaceContext.class, "race");
        });
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testStartingTwice() {
        assertThrows(IllegalStateException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, ?> dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Void.class, "TestRetrieverChain");
            Class<Processor<Collection<Test_Regatta>, Test_Regatta>> regattaRetrieverClass = (Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) TestRegattaRetrievalProcessor.class;
            dataRetrieverChainDefinition.startWith(regattaRetrieverClass, Test_Regatta.class, "regatta");
            dataRetrieverChainDefinition.startWith(regattaRetrieverClass, Test_Regatta.class, "regatta");
        });
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testStartingWithProcessorWithoutUsableConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, ?> dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Void.class, "TestRetrieverChain");
            dataRetrieverChainDefinition.startWith((Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) Processor.class, Test_Regatta.class, "regatta");
        });
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingProcessorWithoutUsableConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, ?> dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Void.class, "TestRetrieverChain");
            Class<Processor<Collection<Test_Regatta>, Test_Regatta>> regattaRetrieverClass = (Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) TestRegattaRetrievalProcessor.class;
            dataRetrieverChainDefinition.startWith(regattaRetrieverClass, Test_Regatta.class, "regatta");
            dataRetrieverChainDefinition.addAfter(regattaRetrieverClass,
                                                  (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) Processor.class,
                                                  Test_HasRaceContext.class, "race");
        });
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingRetrieverThatDoesntMatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, ?> dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Void.class, "TestRetrieverChain");
            Class<Processor<Collection<Test_Regatta>, Test_Regatta>> regattaRetrieverClass = (Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) TestRegattaRetrievalProcessor.class;
            dataRetrieverChainDefinition.startWith(regattaRetrieverClass, Test_Regatta.class, "regatta");
            Class<Processor<Test_Regatta, Test_HasRaceContext>> raceRetrieverClass = 
                    (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) TestRaceWithContextRetrievalProcessor.class;
            Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>> legRetrieverClass = 
                    (Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>>)(Class<?>) TestLegOfCompetitorWithContextRetrievalProcessor.class;
            dataRetrieverChainDefinition.addAfter(raceRetrieverClass,
                                                   legRetrieverClass,
                                                   Test_HasLegOfCompetitorContext.class, "legOfCompetitor");
        });
    }
    
    @Test
    public void testSettingAFilterWithWrongElementType() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = dataRetrieverChainDefinition.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());
            chainBuilder.stepFurther(); //Initialization
            chainBuilder.setFilter(raceFilter);
        });
    }
    
    @Test
    public void testSettingAResultReceiverWithWrongInputType() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = dataRetrieverChainDefinition.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());
            chainBuilder.stepFurther(); //Initialization
            chainBuilder.addResultReceiver(legReceiver);
        });
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEndingTwice() {
        assertThrows(IllegalStateException.class, () -> {
            Class<Processor<Test_Regatta, Test_HasRaceContext>> raceRetrieverClass = 
                    (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) TestRaceWithContextRetrievalProcessor.class;
            Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>> legRetrieverClass = 
                    (Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>>)(Class<?>) TestLegOfCompetitorWithContextRetrievalProcessor.class;
            dataRetrieverChainDefinition.endWith(raceRetrieverClass, legRetrieverClass, Test_HasLegOfCompetitorContext.class, "legOfCompetitor");
        });
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingRetrieverAfterCompletion() {
        assertThrows(IllegalStateException.class, () -> {
            Class<Processor<Test_Regatta, Test_HasRaceContext>> raceRetrieverClass = 
                    (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) TestRaceWithContextRetrievalProcessor.class;
            Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>> legRetrieverClass = 
                    (Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>>)(Class<?>) TestLegOfCompetitorWithContextRetrievalProcessor.class;
            dataRetrieverChainDefinition.addAfter(raceRetrieverClass, legRetrieverClass, Test_HasLegOfCompetitorContext.class, "legOfCompetitor");
        });
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testStartBuildingWithIncompleteChain() {
        assertThrows(IllegalStateException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Test_HasLegOfCompetitorContext.class, "TestRetrieverChain");
            Class<Processor<Collection<Test_Regatta>, Test_Regatta>> regattaRetrieverClass = (Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) TestRegattaRetrievalProcessor.class;
            dataRetrieverChainDefinition.startWith(regattaRetrieverClass, Test_Regatta.class, "regatta");
            Class<Processor<Test_Regatta, Test_HasRaceContext>> raceRetrieverClass = 
                    (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) TestRaceWithContextRetrievalProcessor.class;
            dataRetrieverChainDefinition.addAfter(regattaRetrieverClass,
                                                   raceRetrieverClass,
                                                   Test_HasRaceContext.class, "race");
            dataRetrieverChainDefinition.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());
        });
    }
    
    @Test
    public void testSteppingToFar() {
        assertThrows(IllegalStateException.class, () -> {
            DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = dataRetrieverChainDefinition.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());
            while (chainBuilder.canStepFurther()) {
                chainBuilder.stepFurther();
            }
            chainBuilder.stepFurther();
        });
    }
    
    @Test
    public void testRetrieverChainWithSettingsCreation() {
        DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> chainWithSettings = 
                new SimpleDataRetrieverChainDefinition<>(dataRetrieverChainDefinition, Test_HasLegOfCompetitorContext.class, "TestRetrieverChain");
        chainWithSettings.endWith(TestLegOfCompetitorWithContextRetrievalProcessor.class, Test_RetrievalProcessorWithSettings.class, Test_HasLegOfCompetitorContext.class,
                Test_RetrievalProcessorSettings.class, new Test_RetrievalProcessorSettings("Default Settings"), "legOfCompetitor");
        
        DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = chainWithSettings.startBuilding(getSharedExecutor());
        while (chainBuilder.canStepFurther()) {
            chainBuilder.stepFurther();
        }
        
        Processor<Collection<Test_Regatta>, ?> firstRetriever = chainBuilder.build();
        Test_RetrievalProcessorWithSettings lastRetriever = getLastRetriever(firstRetriever);
        assertThat(lastRetriever.getSettings().getValue(), is("Default Settings"));
        
        Test_RetrievalProcessorSettings settings = new Test_RetrievalProcessorSettings("Settings Value");
        chainBuilder.setSettings(settings);
        
        firstRetriever = chainBuilder.build();
        lastRetriever = getLastRetriever(firstRetriever);
        assertThat(lastRetriever.getSettings().getValue(), is("Settings Value"));
    }
    
    private Test_RetrievalProcessorWithSettings getLastRetriever(Processor<Collection<Test_Regatta>, ?> firstRetriever) {
        TestRaceWithContextRetrievalProcessor raceRetriever = (TestRaceWithContextRetrievalProcessor) ((TestRegattaRetrievalProcessor) firstRetriever).getResultReceivers()[0];
        TestLegOfCompetitorWithContextRetrievalProcessor legRetriever = (TestLegOfCompetitorWithContextRetrievalProcessor) raceRetriever.getResultReceivers()[0];
        Test_RetrievalProcessorWithSettings lastRetriever = (Test_RetrievalProcessorWithSettings) legRetriever.getResultReceivers()[0];
        return lastRetriever;
    }

    @Test
    public void testRetrieverChainDefinitionWithSettingsButWithoutDefaultSettings() {
        assertThrows(NullPointerException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> chainWithSettings = 
                    new SimpleDataRetrieverChainDefinition<>(dataRetrieverChainDefinition, Test_HasLegOfCompetitorContext.class, "TestRetrieverChain");
            chainWithSettings.endWith(TestLegOfCompetitorWithContextRetrievalProcessor.class, Test_RetrievalProcessorWithSettings.class, Test_HasLegOfCompetitorContext.class,
                    Test_RetrievalProcessorSettings.class, null, "legOfCompetitor");
        });
    }
    
    @Test
    public void testRetrieverChainWithWrongSettingsCreation() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> chainWithSettings = 
                    new SimpleDataRetrieverChainDefinition<>(dataRetrieverChainDefinition, Test_HasLegOfCompetitorContext.class, "TestRetrieverChain");
            chainWithSettings.endWith(TestLegOfCompetitorWithContextRetrievalProcessor.class, Test_RetrievalProcessorWithSettings.class, Test_HasLegOfCompetitorContext.class,
                    Test_RetrievalProcessorSettings.class, new Test_RetrievalProcessorSettings("Default Settings"), "legOfCompetitor");
            
            DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = chainWithSettings.startBuilding(getSharedExecutor());
            while (chainBuilder.canStepFurther()) {
                chainBuilder.stepFurther();
            }
            chainBuilder.setSettings(new WrongSettings());
        });
    }
    
    @Test
    public void testRetrieverWithNoSettingsButSettedSettingsCreated() {
        assertThrows(IllegalStateException.class, () -> {
            DataRetrieverChainBuilder<Collection<Test_Regatta>> chainBuilder = dataRetrieverChainDefinition.startBuilding(ConcurrencyTestsUtil.getSharedExecutor());
            chainBuilder.stepFurther(); // Initialization
            chainBuilder.setSettings(new WrongSettings());
        });
    }
    
    public class WrongSettings extends SerializableSettings {
        private static final long serialVersionUID = 8114560334614499761L;
        
    }

}
