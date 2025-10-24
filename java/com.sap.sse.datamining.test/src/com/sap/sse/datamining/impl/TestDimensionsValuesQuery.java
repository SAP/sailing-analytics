package com.sap.sse.datamining.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.components.DataRetrieverLevel;
import com.sap.sse.datamining.impl.components.SimpleDataRetrieverChainDefinition;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.test.data.Test_HasLegOfCompetitorContext;
import com.sap.sse.datamining.test.data.Test_HasRaceContext;
import com.sap.sse.datamining.test.domain.Test_Boat;
import com.sap.sse.datamining.test.domain.Test_Competitor;
import com.sap.sse.datamining.test.domain.Test_Named;
import com.sap.sse.datamining.test.domain.Test_Race;
import com.sap.sse.datamining.test.domain.Test_Regatta;
import com.sap.sse.datamining.test.util.ComponentTestsUtil;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.datamining.test.util.components.TestLegOfCompetitorWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRaceWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRegattaRetrievalProcessor;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TestDimensionsValuesQuery {

    private static final ResourceBundleStringMessages stringMessages = TestsUtil.getTestStringMessagesWithProductiveMessages();
    private static final Locale locale = Locale.ENGLISH;
    
    private DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> dataRetrieverChainDefinition;
    private Collection<Test_Regatta> dataSource;
    
    //Test_HasRaceContext dimensions
    private Function<String> dimensionRegattaName;
    private Function<String> dimensionRaceName;
    private Function<String> dimensionBoatClassName;
    private Function<Integer> dimensionYear;

    //Test_HasLegContext dimensions
    private Function<Integer> dimensionLegNumber;
    private Function<String> dimensionCompetitorName;
    private Function<String> dimensionCompetitorSailID;
    
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void initializeDataRetrieverChain() {
        dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Test_HasLegOfCompetitorContext.class, "TestRetrieverChain");
        Class<Processor<Collection<Test_Regatta>, Test_Regatta>> regattaRetrieverClass = (Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) TestRegattaRetrievalProcessor.class;
        dataRetrieverChainDefinition.startWith(regattaRetrieverClass, Test_Regatta.class, "regatta");
        
        Class<Processor<Test_Regatta, Test_HasRaceContext>> raceRetrieverClass = 
                (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) TestRaceWithContextRetrievalProcessor.class;
        dataRetrieverChainDefinition.addAfter(regattaRetrieverClass,
                                               raceRetrieverClass,
                                               Test_HasRaceContext.class, "race");
        
        Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>> legRetrieverClass = 
                (Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>>)(Class<?>) TestLegOfCompetitorWithContextRetrievalProcessor.class;
        dataRetrieverChainDefinition.endWith(raceRetrieverClass,
                                               legRetrieverClass,
                                               Test_HasLegOfCompetitorContext.class, "legOfCompetitor");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testDimensionsValuesQuery() throws InterruptedException, ExecutionException {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        server.addStringMessages(stringMessages);
        server.registerDataSourceProvider(new AbstractDataSourceProvider<Collection>(Collection.class) {
            @Override
            public Collection<?> getDataSource() {
                return dataSource;
            }
        });
        
        Collection<Function<?>> raceDimensions = new ArrayList<>();
        raceDimensions.add(dimensionRegattaName);
        raceDimensions.add(dimensionRaceName);
        raceDimensions.add(dimensionBoatClassName);
        raceDimensions.add(dimensionYear);
        Map<DataRetrieverLevel<?, ?>, Map<Function<?>, Collection<?>>> filterSelection = new HashMap<>();
        Query<HashSet<Object>> dimensionsValueQuery = server.createDimensionValuesQuery(dataRetrieverChainDefinition, dataRetrieverChainDefinition.getDataRetrieverLevel(/*race*/ 1), raceDimensions, new HashMap<>(), filterSelection, locale);
        
        Map<GroupKey, HashSet<Object>> expectedRaceResultData = buildExpectedRaceResultData();
        QueryResult<HashSet<Object>> result = dimensionsValueQuery.run();
        ConcurrencyTestsUtil.verifyResultData(result.getResults(), expectedRaceResultData);
        
        Collection<Function<?>> legDimensions = new ArrayList<>();
        legDimensions.add(dimensionLegNumber);
        legDimensions.add(dimensionCompetitorName);
        legDimensions.add(dimensionCompetitorSailID);
        dimensionsValueQuery = server.createDimensionValuesQuery(dataRetrieverChainDefinition, dataRetrieverChainDefinition.getDataRetrieverLevel(/*leg*/ 2), legDimensions, new HashMap<>(), filterSelection, locale);

        Map<GroupKey, HashSet<Object>> expectedLegResultData = buildExpectedLegResultData();
        result = dimensionsValueQuery.run();
        ConcurrencyTestsUtil.verifyResultData(result.getResults(), expectedLegResultData);
    }
    
    private Map<GroupKey, HashSet<Object>> buildExpectedRaceResultData() {
        Map<GroupKey, HashSet<Object>> expectedResultData = new HashMap<>();

        //Add empty sets for Test_HasRaceContext dimensions
        GroupKey dimensionRegattaNameGroupKey = new GenericGroupKey<FunctionDTO>(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimensionRegattaName, stringMessages, locale));
        expectedResultData.put(dimensionRegattaNameGroupKey, new HashSet<Object>());
        GroupKey dimensionRaceNameGroupKey = new GenericGroupKey<FunctionDTO>(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimensionRaceName, stringMessages, locale));
        expectedResultData.put(dimensionRaceNameGroupKey, new HashSet<Object>());
        GroupKey dimensionBoatClassNameGroupKey = new GenericGroupKey<FunctionDTO>(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimensionBoatClassName, stringMessages, locale));
        expectedResultData.put(dimensionBoatClassNameGroupKey, new HashSet<Object>());
        GroupKey dimensionYearGroupKey = new GenericGroupKey<FunctionDTO>(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimensionYear, stringMessages, locale));
        expectedResultData.put(dimensionYearGroupKey, new HashSet<Object>());
        
        for (Test_Regatta regatta : dataSource) {
            expectedResultData.get(dimensionRegattaNameGroupKey).add(regatta.getName());
            expectedResultData.get(dimensionYearGroupKey).add(regatta.getYear());
            expectedResultData.get(dimensionBoatClassNameGroupKey).add(regatta.getBoatClass().getName());
            
            for (Test_Race race : regatta.getRaces()) {
                expectedResultData.get(dimensionRaceNameGroupKey).add(race.getName());
            }
        }
        return expectedResultData;
    }
    
    private Map<GroupKey, HashSet<Object>> buildExpectedLegResultData() {
        Map<GroupKey, HashSet<Object>> expectedResultData = new HashMap<>();

        //Add empty sets for Test_HasLegContext dimensions
        GroupKey dimensionLegNumberGroupKey = new GenericGroupKey<FunctionDTO>(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimensionLegNumber, stringMessages, locale));
        expectedResultData.put(dimensionLegNumberGroupKey, new HashSet<Object>());
        GroupKey dimensionCompetitorNameGroupKey = new GenericGroupKey<FunctionDTO>(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimensionCompetitorName, stringMessages, locale));
        expectedResultData.put(dimensionCompetitorNameGroupKey, new HashSet<Object>());
        GroupKey dimensionCompetitorSailIDGroupKey = new GenericGroupKey<FunctionDTO>(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimensionCompetitorSailID, stringMessages, locale));
        expectedResultData.put(dimensionCompetitorSailIDGroupKey, new HashSet<Object>());
        
        for (Test_Regatta regatta : dataSource) {
            for (Test_Race race : regatta.getRaces()) {
                for (int legNumber = 1; legNumber <= race.getLegs().size(); legNumber++) {
                    expectedResultData.get(dimensionLegNumberGroupKey).add(legNumber);
                }
                
                for (Test_Competitor competitor : race.getCompetitors()) {
                    expectedResultData.get(dimensionCompetitorNameGroupKey).add(competitor.getTeam().getName());
                    expectedResultData.get(dimensionCompetitorSailIDGroupKey).add(competitor.getBoat().getSailID());
                }
            }
        }
        return expectedResultData;
    }

    @BeforeEach
    public void initializeDimensions() throws NoSuchMethodException, SecurityException {
        Method getNameMethod = Test_Named.class.getMethod("getName", new Class<?>[0]);
        Function<?> getName = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getNameMethod);
        
        Method getRegattaMethod = Test_HasRaceContext.class.getMethod("getRegatta", new Class<?>[0]);
        Function<?> getRegatta = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getRegattaMethod);
        dimensionRegattaName = FunctionTestsUtil.getFunctionFactory().createCompoundFunction(Arrays.asList(getRegatta, getName));

        Method getRaceMethod = Test_HasRaceContext.class.getMethod("getRace", new Class<?>[0]);
        Function<?> getRace = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getRaceMethod);
        dimensionRaceName = FunctionTestsUtil.getFunctionFactory().createCompoundFunction(Arrays.asList(getRace, getName));
        
        Method getBoatClassMethod = Test_HasRaceContext.class.getMethod("getBoatClass", new Class<?>[0]);
        Function<?> getBoatClass = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getBoatClassMethod);
        dimensionBoatClassName = FunctionTestsUtil.getFunctionFactory().createCompoundFunction(Arrays.asList(getBoatClass, getName));
        
        Method getYearMethod = Test_HasRaceContext.class.getMethod("getYear", new Class<?>[0]);
        dimensionYear = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getYearMethod);
        
        Method getLegNumberMethod = Test_HasLegOfCompetitorContext.class.getMethod("getLegNumber", new Class<?>[0]);
        dimensionLegNumber = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getLegNumberMethod);
        
        Method getCompetitorMethod = Test_HasLegOfCompetitorContext.class.getMethod("getCompetitor", new Class<?>[0]);
        Function<?> getCompetitor = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getCompetitorMethod);
        
        Method getTeamMethod = Test_Competitor.class.getMethod("getTeam", new Class<?>[0]);
        Function<?> getTeam = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getTeamMethod);
        dimensionCompetitorName = FunctionTestsUtil.getFunctionFactory().createCompoundFunction(Arrays.asList(getCompetitor, getTeam, getName));
        
        Method getBoatMethod = Test_Competitor.class.getMethod("getBoat", new Class<?>[0]);
        Function<?> getBoat = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getBoatMethod);
        Method getSailIDMethod = Test_Boat.class.getMethod("getSailID", new Class<?>[0]);
        Function<?> getSailID = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getSailIDMethod);
        dimensionCompetitorSailID = FunctionTestsUtil.getFunctionFactory().createCompoundFunction(Arrays.asList(getCompetitor, getBoat, getSailID));
    }
    
    @BeforeEach
    public void initializeDataSource() {
        dataSource = ComponentTestsUtil.createExampleDataSource();
    }

}
