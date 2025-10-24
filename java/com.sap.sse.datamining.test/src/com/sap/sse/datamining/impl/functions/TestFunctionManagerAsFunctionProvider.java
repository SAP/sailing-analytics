package com.sap.sse.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.factories.DataMiningDTOFactory;
import com.sap.sse.datamining.factories.FunctionFactory;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.components.DataRetrieverLevel;
import com.sap.sse.datamining.impl.components.SimpleDataRetrieverChainDefinition;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.test.data.Test_HasLegOfCompetitorContext;
import com.sap.sse.datamining.test.data.Test_HasLegOfCompetitorContextImpl;
import com.sap.sse.datamining.test.data.Test_HasRaceContext;
import com.sap.sse.datamining.test.data.Test_HasRaceContextImpl;
import com.sap.sse.datamining.test.data.impl.SimpleClassWithMarkedMethods;
import com.sap.sse.datamining.test.data.impl.Test_ExternalLibraryClass;
import com.sap.sse.datamining.test.domain.Test_Leg;
import com.sap.sse.datamining.test.domain.Test_Named;
import com.sap.sse.datamining.test.domain.Test_Regatta;
import com.sap.sse.datamining.test.util.ExpectedFunctionRegistryUtil;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.datamining.test.util.components.TestLegOfCompetitorWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRaceWithContextRetrievalProcessor;
import com.sap.sse.datamining.test.util.components.TestRegattaRetrievalProcessor;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TestFunctionManagerAsFunctionProvider {
    
    private static final ResourceBundleStringMessages stringMessages = TestsUtil.getTestStringMessagesWithProductiveMessages();
    
    private ExpectedFunctionRegistryUtil functionRegistryUtil;
    private ModifiableDataMiningServer server;
    
    @BeforeEach
    public void initializeDataMiningServer() throws NoSuchMethodException, SecurityException {
        functionRegistryUtil = new ExpectedFunctionRegistryUtil();
        server = TestsUtil.createNewServer();
        server.addStringMessages(TestsUtil.getTestStringMessages());
        
        Collection<Class<?>> internalClassesToScan = new HashSet<>();
        internalClassesToScan.add(Test_HasRaceContext.class);
        internalClassesToScan.add(Test_HasLegOfCompetitorContext.class);
        server.registerAllClasses(internalClassesToScan);
        
        Collection<Class<?>> externalClassesToScan = new HashSet<>();
        externalClassesToScan.add(Test_ExternalLibraryClass.class);
        server.registerAllWithExternalFunctionPolicy(externalClassesToScan);
    }
    
    @Test
    public void testGetIdentityFunction() {
        assertThat(server.getIdentityFunction(), is(new IdentityFunction()));
    }
    
    @Test
    public void testGetAllStatistics() {
        Collection<Function<?>> expectedFunctions = functionRegistryUtil.getAllExpectedStatistics();
        assertThat(server.getAllStatistics(), is(expectedFunctions));
    }

    @Test
    public void testGetDimensionsForType() {
        Collection<Function<?>> expectedDimensions = functionRegistryUtil.getExpectedDimensionsFor(Test_HasRaceContext.class);
        assertThat(server.getDimensionsFor(Test_HasRaceContext.class), is(expectedDimensions));
        assertThat(server.getDimensionsFor(Test_HasRaceContextImpl.class), is(expectedDimensions));

        expectedDimensions = functionRegistryUtil.getExpectedDimensionsFor(Test_HasLegOfCompetitorContext.class);
        assertThat(server.getDimensionsFor(Test_HasLegOfCompetitorContext.class), is(expectedDimensions));
        assertThat(server.getDimensionsFor(Test_HasLegOfCompetitorContextImpl.class), is(expectedDimensions));
    }
    
    @Test
    public void testGetDimensionsForDataRetrieverChainDefinition() {
        DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> dataRetrieverChainDefinition = createDataRetrieverChainDefinition();

        List<? extends DataRetrieverLevel<?, ?>> dataRetrieverLevels = dataRetrieverChainDefinition.getDataRetrieverLevels();
        Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> expectedDimensions = new HashMap<>();
        for (DataRetrieverLevel<?, ?> dataRetrieverLevel : dataRetrieverLevels) {
            expectedDimensions.put(dataRetrieverLevel, server.getDimensionsFor(dataRetrieverLevel.getRetrievedDataType()));
        }
        final Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> dimensionsMappedByLevel = server.getDimensionsMappedByLevelFor(dataRetrieverChainDefinition);
        assertThat(dimensionsMappedByLevel, is(expectedDimensions));
    }
    
    @Test
    public void testGetReducedDimensionsForDataRetrieverChainDefinition() {
        DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> retrieverChain = createDataRetrieverChainDefinition();

        Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> expectedDimensions = server.getDimensionsMappedByLevelFor(retrieverChain);
        for (DataRetrieverLevel<?, ?> retrieverLevel : expectedDimensions.keySet()) {
            if (retrieverLevel.getRetrievedDataType().equals(Test_HasLegOfCompetitorContext.class)) {
                Collection<Function<?>> reducedLegDimensions = new HashSet<>();
                for (Function<?> dimension : expectedDimensions.get(retrieverLevel)) {
                    if (!(dimension instanceof ConcatenatingCompoundFunction) ||
                        !((ConcatenatingCompoundFunction<?>) dimension).getSimpleFunctions().get(0).getReturnType().equals(Test_HasRaceContext.class)) {
                        reducedLegDimensions.add(dimension);
                    }
                }
                expectedDimensions.put(retrieverLevel, reducedLegDimensions);
                break;
            }
        }
        final Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> reducedDimensionsMappedByLevel = server.getReducedDimensionsMappedByLevelFor(retrieverChain).getReducedDimensions();
        assertThat(reducedDimensionsMappedByLevel, is(expectedDimensions));
    }
    
    @SuppressWarnings("unchecked")
    public DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> createDataRetrieverChainDefinition() {
        DataRetrieverChainDefinition<Collection<Test_Regatta>, Test_HasLegOfCompetitorContext> dataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>((Class<Collection<Test_Regatta>>)(Class<?>) Collection.class, Test_HasLegOfCompetitorContext.class, "TestRetrieverChain");
        Class<Processor<Collection<Test_Regatta>, Test_Regatta>> regattaRetrieverClass = (Class<Processor<Collection<Test_Regatta>, Test_Regatta>>)(Class<?>) TestRegattaRetrievalProcessor.class;
        dataRetrieverChainDefinition.startWith(regattaRetrieverClass, Test_Regatta.class, "regatta");
        
        Class<Processor<Test_Regatta, Test_HasRaceContext>> raceRetrieverClass = 
                (Class<Processor<Test_Regatta, Test_HasRaceContext>>)(Class<?>) TestRaceWithContextRetrievalProcessor.class;
        dataRetrieverChainDefinition.addAfter(regattaRetrieverClass,
                                               raceRetrieverClass,
                                               Test_HasRaceContext.class, "race");
        
        Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>> legRetrieverClass = 
                (Class<Processor<Test_HasRaceContext, Test_HasLegOfCompetitorContext>>)(Class<?>) TestLegOfCompetitorWithContextRetrievalProcessor.class;
        dataRetrieverChainDefinition.addAfter(raceRetrieverClass,
                                               legRetrieverClass,
                                               Test_HasLegOfCompetitorContext.class, "legOfCompetitor");
        
        return dataRetrieverChainDefinition;
    }
    
    @Test
    public void testGetStatisticsForType() {
        Collection<Function<?>> expectedStatistics = functionRegistryUtil.getExpectedStatisticsFor(Test_HasLegOfCompetitorContext.class);
        assertThat(server.getStatisticsFor(Test_HasLegOfCompetitorContext.class), is(expectedStatistics));
        assertThat(server.getStatisticsFor(Test_HasLegOfCompetitorContextImpl.class), is(expectedStatistics));
    }
    
    @Test
    public void testGetAllFunctionsForType() {
        Collection<Function<?>> expectedFunctions = functionRegistryUtil.getExpectedFunctionsFor(Test_HasLegOfCompetitorContext.class);
        assertThat(server.getFunctionsFor(Test_HasLegOfCompetitorContext.class), is(expectedFunctions));
        assertThat(server.getFunctionsFor(Test_HasLegOfCompetitorContextImpl.class), is(expectedFunctions));
    }
    
    @Test
    public void testGetAllFunctionsForObject() {
        assertThat(Util.size(server.getFunctionsFor(Object.class)), is(0));
    }
    
    @SuppressWarnings("unchecked") // Hamcrest requires type matching of actual and expected type, so the Functions have to be specific (without <?>)
    @Test
    public void testGetFunctionForDimensionDTO() throws NoSuchMethodException, SecurityException {
        FunctionFactory functionFactory = FunctionTestsUtil.getFunctionFactory();
        DataMiningDTOFactory dtoFactory = FunctionTestsUtil.getDTOFactory();
        
        Method getRegattaMethod = Test_HasRaceContext.class.getMethod("getRace", new Class<?>[0]);
        Function<?> getRegatta = functionFactory.createMethodWrappingFunction(getRegattaMethod);
        Method getNameMethod = Test_Named.class.getMethod("getName", new Class<?>[0]);
        Function<?> getName = functionFactory.createMethodWrappingFunction(getNameMethod);
        Function<Object> getRegattaName = functionFactory.createCompoundFunction(Arrays.asList(getRegatta, getName));
        
        FunctionDTO getRegattaNameDTO_English = dtoFactory.createFunctionDTO(getRegattaName, stringMessages, Locale.ENGLISH);
        Function<Object> providedFunction = (Function<Object>) server.getFunctionForDTO(getRegattaNameDTO_English);
        assertThat(providedFunction, is(getRegattaName));
        FunctionDTO getRegattaNameDTO_German = dtoFactory.createFunctionDTO(getRegattaName, stringMessages, Locale.GERMAN);
        providedFunction = (Function<Object>) server.getFunctionForDTO(getRegattaNameDTO_German);
        assertThat(providedFunction, is(getRegattaName));
        FunctionDTO getRegattaNameDTO = dtoFactory.createFunctionDTO(getRegattaName);
        providedFunction = (Function<Object>) server.getFunctionForDTO(getRegattaNameDTO);
        assertThat(providedFunction, is(getRegattaName));
    }
    
    @SuppressWarnings("unchecked") // Hamcrest requires type matching of actual and expected type, so the Functions have to be specific (without <?>)
    @Test
    public void testGetFunctionForStatisticDTO() {
        FunctionFactory functionFactory = FunctionTestsUtil.getFunctionFactory();
        DataMiningDTOFactory dtoFactory = FunctionTestsUtil.getDTOFactory();
        
        Method getLegMethod = FunctionTestsUtil.getMethodFromClass(Test_HasLegOfCompetitorContext.class, "getLeg");
        Function<?> getLeg = functionFactory.createMethodWrappingFunction(getLegMethod);
        Method getDistanceTraveledMethod = FunctionTestsUtil.getMethodFromClass(Test_Leg.class, "getDistanceTraveled");
        Function<?> getDistanceTraveled = functionFactory.createMethodWrappingFunction(getDistanceTraveledMethod);
        Function<Object> getLegDistanceTraveled = functionFactory.createCompoundFunction(Arrays.asList(getLeg, getDistanceTraveled));
        
        FunctionDTO getLegDistanceTraveledDTO_English = dtoFactory.createFunctionDTO(getLegDistanceTraveled, stringMessages, Locale.ENGLISH);
        Function<Object> providedFunction = (Function<Object>) server.getFunctionForDTO(getLegDistanceTraveledDTO_English);
        assertThat(providedFunction, is(getLegDistanceTraveled));
        FunctionDTO getLegDistanceTraveledDTO_German = dtoFactory.createFunctionDTO(getLegDistanceTraveled, stringMessages, Locale.GERMAN);
        providedFunction = (Function<Object>) server.getFunctionForDTO(getLegDistanceTraveledDTO_German);
        assertThat(providedFunction, is(getLegDistanceTraveled));
        FunctionDTO getLegDistanceTraveledDTO = dtoFactory.createFunctionDTO(getLegDistanceTraveled);
        providedFunction = (Function<Object>) server.getFunctionForDTO(getLegDistanceTraveledDTO);
        assertThat(providedFunction, is(getLegDistanceTraveled));
    }
    
    @SuppressWarnings("unchecked") // Hamcrest requires type matching of actual and expected type, so the Functions have to be specific (without <?>)
    @Test
    public void testGetFunctionForExternalFunctionDTO() {
        FunctionFactory functionFactory = FunctionTestsUtil.getFunctionFactory();
        DataMiningDTOFactory dtoFactory = FunctionTestsUtil.getDTOFactory();
        
        Method fooMethod = FunctionTestsUtil.getMethodFromClass(Test_ExternalLibraryClass.class, "foo");
        Function<Object> foo = functionFactory.createMethodWrappingFunction(fooMethod);
        
        FunctionDTO fooDTO_English = dtoFactory.createFunctionDTO(foo, stringMessages, Locale.ENGLISH);
        Function<Object> providedFunction = (Function<Object>) server.getFunctionForDTO(fooDTO_English);
        assertThat(providedFunction, is(foo));
        FunctionDTO fooDTO_German = dtoFactory.createFunctionDTO(foo, stringMessages, Locale.GERMAN);
        providedFunction = (Function<Object>) server.getFunctionForDTO(fooDTO_German);
        assertThat(providedFunction, is(foo));
        FunctionDTO fooDTO = dtoFactory.createFunctionDTO(foo);
        providedFunction = (Function<Object>) server.getFunctionForDTO(fooDTO);
        assertThat(providedFunction, is(foo));
    }
    
    @Test
    public void testGetFunctionForUnregisteredDTO() {
        Method dimensionMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "dimension");
        Function<Object> dimension = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(dimensionMethod);
        FunctionDTO dimensionDTO_English = FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension, stringMessages, Locale.ENGLISH);
        assertThat(server.getFunctionForDTO(dimensionDTO_English), is(nullValue()));
        FunctionDTO dimensionDTO_German = FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension, stringMessages, Locale.GERMAN);
        assertThat(server.getFunctionForDTO(dimensionDTO_German), is(nullValue()));
        FunctionDTO dimensionDTO = FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension);
        assertThat(server.getFunctionForDTO(dimensionDTO), is(nullValue()));

        Method sideEffectFreeValueMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "sideEffectFreeValue");
        Function<Object> sideEffectFreeValue = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(sideEffectFreeValueMethod);
        FunctionDTO sideEffectFreeValueDTO_English = FunctionTestsUtil.getDTOFactory().createFunctionDTO(sideEffectFreeValue, stringMessages, Locale.ENGLISH);
        assertThat(server.getFunctionForDTO(sideEffectFreeValueDTO_English), is(nullValue()));
        FunctionDTO sideEffectFreeValueDTO_German = FunctionTestsUtil.getDTOFactory().createFunctionDTO(sideEffectFreeValue, stringMessages, Locale.GERMAN);
        assertThat(server.getFunctionForDTO(sideEffectFreeValueDTO_German), is(nullValue()));
        FunctionDTO sideEffectFreeValueDTO = FunctionTestsUtil.getDTOFactory().createFunctionDTO(sideEffectFreeValue);
        assertThat(server.getFunctionForDTO(sideEffectFreeValueDTO), is(nullValue()));
    }
    
    @Test
    public void testGetFunctionForIdentityDTO() {
        DataMiningDTOFactory dtoFactory = FunctionTestsUtil.getDTOFactory();
        IdentityFunction identityFunction = new IdentityFunction();
        
        FunctionDTO identityFunctionDTO_English = dtoFactory.createFunctionDTO(identityFunction, stringMessages, Locale.ENGLISH); 
        assertThat(server.getFunctionForDTO(identityFunctionDTO_English), is(identityFunction));
        
        FunctionDTO identityFunctionDTO_German = dtoFactory.createFunctionDTO(identityFunction, stringMessages, Locale.GERMAN); 
        assertThat(server.getFunctionForDTO(identityFunctionDTO_German), is(identityFunction));
        
        FunctionDTO identityFunctionDTO = dtoFactory.createFunctionDTO(identityFunction); 
        assertThat(server.getFunctionForDTO(identityFunctionDTO), is(identityFunction));
    }
    
    @Test
    public void testGetFunctionForNullDTO() {
        assertThat(server.getFunctionForDTO(null), is(nullValue()));
    }
    
    @Test
    public void testGetFunctionForDTOWithNonExistingClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            FunctionDTO functionDTO = new FunctionDTO(false, "Not relevant", "Impossible Class", "Impossible Class", new ArrayList<String>(), "Not relevant", 0);
            server.getFunctionForDTO(functionDTO);
        });
    }

}
