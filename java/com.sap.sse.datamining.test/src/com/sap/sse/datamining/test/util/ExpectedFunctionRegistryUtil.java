package com.sap.sse.datamining.test.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.datamining.factories.FunctionFactory;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Boat;
import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Competitor;
import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Leg;
import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Named;
import com.sap.sse.datamining.test.functions.registry.test_contexts.Test_HasLegContext;
import com.sap.sse.datamining.test.functions.registry.test_contexts.Test_HasRaceContext;
import com.sap.sse.datamining.test.functions.test_classes.Test_ExternalLibraryClass;

public class ExpectedFunctionRegistryUtil {

    private final HashMap<Class<?>, Collection<Function<?>>> expectedDimensions;
    private final HashMap<Class<?>, Collection<Function<?>>> expectedStatistics;
    private final HashMap<Class<?>, Collection<Function<?>>> expectedExternalFunctions;
    
    public ExpectedFunctionRegistryUtil() throws NoSuchMethodException, SecurityException {
        expectedDimensions = new HashMap<>();
        expectedStatistics = new HashMap<>();
        expectedExternalFunctions = new HashMap<>();
        
        buildExpectedFunctions();
    }

    private void buildExpectedFunctions() throws NoSuchMethodException, SecurityException {
        buildExpectedDimensions();
        buildExpectedStatistics();
        buildExpectedExternalFunctions();
    }

    private void buildExpectedDimensions() throws NoSuchMethodException, SecurityException {
        Method getNameMethod = Test_Named.class.getMethod("getName", new Class<?>[0]);
        Function<?> getName = FunctionFactory.createMethodWrappingFunction(getNameMethod);
        
        Method getRegattaMethod = Test_HasRaceContext.class.getMethod("getRegatta", new Class<?>[0]);
        Function<?> getRegatta = FunctionFactory.createMethodWrappingFunction(getRegattaMethod);
        addExpectedDimension(FunctionFactory.createCompoundFunction(null, Arrays.asList(getRegatta, getName)));

        Method getRaceMethod = Test_HasRaceContext.class.getMethod("getRace", new Class<?>[0]);
        Function<?> getRace = FunctionFactory.createMethodWrappingFunction(getRaceMethod);
        addExpectedDimension(FunctionFactory.createCompoundFunction(null, Arrays.asList(getRace, getName)));
        
        Method getBoatClassMethod = Test_HasRaceContext.class.getMethod("getBoatClass", new Class<?>[0]);
        Function<?> getBoatClass = FunctionFactory.createMethodWrappingFunction(getBoatClassMethod);
        addExpectedDimension(FunctionFactory.createCompoundFunction(null, Arrays.asList(getBoatClass, getName)));
        
        Method getYearMethod = Test_HasRaceContext.class.getMethod("getYear", new Class<?>[0]);
        addExpectedDimension(FunctionFactory.createMethodWrappingFunction(getYearMethod));
        
        Method getLegNumberMethod = Test_HasLegContext.class.getMethod("getLegNumber", new Class<?>[0]);
        addExpectedDimension(FunctionFactory.createMethodWrappingFunction(getLegNumberMethod));
        
        Method getCompetitorMethod = Test_HasLegContext.class.getMethod("getCompetitor", new Class<?>[0]);
        Function<?> getCompetitor = FunctionFactory.createMethodWrappingFunction(getCompetitorMethod);
        
        Method getTeamMethod = Test_Competitor.class.getMethod("getTeam", new Class<?>[0]);
        Function<?> getTeam = FunctionFactory.createMethodWrappingFunction(getTeamMethod);
        addExpectedDimension(FunctionFactory.createCompoundFunction(null, Arrays.asList(getCompetitor, getTeam, getName)));
        
        Method getBoatMethod = Test_Competitor.class.getMethod("getBoat", new Class<?>[0]);
        Function<?> getBoat = FunctionFactory.createMethodWrappingFunction(getBoatMethod);
        Method getSailIDMethod = Test_Boat.class.getMethod("getSailID", new Class<?>[0]);
        Function<?> getSailID = FunctionFactory.createMethodWrappingFunction(getSailIDMethod);
        addExpectedDimension(FunctionFactory.createCompoundFunction(null, Arrays.asList(getCompetitor, getBoat, getSailID)));
    }
    
    private void addExpectedDimension(Function<?> dimension) {
        Class<?> declaringType = dimension.getDeclaringType();
        if (!expectedDimensions.containsKey(declaringType)) {
            expectedDimensions.put(declaringType, new HashSet<Function<?>>());
        }
        expectedDimensions.get(declaringType).add(dimension);
    }

    private void buildExpectedStatistics() throws NoSuchMethodException, SecurityException {
        Method getLegMethod = Test_HasLegContext.class.getMethod("getLeg", new Class<?>[0]);
        Function<?> getLeg = FunctionFactory.createMethodWrappingFunction(getLegMethod);
        Method getDistanceTraveledMethod = Test_Leg.class.getMethod("getDistanceTraveled", new Class<?>[0]);
        Function<?> getDistanceTraveled = FunctionFactory.createMethodWrappingFunction(getDistanceTraveledMethod);
        addExpectedStatistic(FunctionFactory.createCompoundFunction(null, Arrays.asList(getLeg, getDistanceTraveled)));
    }
    
    private void addExpectedStatistic(Function<?> statistic) {
        Class<?> declaringType = statistic.getDeclaringType();
        if (!expectedStatistics.containsKey(declaringType)) {
            expectedStatistics.put(declaringType, new HashSet<Function<?>>());
        }
        expectedStatistics.get(declaringType).add(statistic);
    }

    private void buildExpectedExternalFunctions() throws NoSuchMethodException, SecurityException {
        Method fooMethod = Test_ExternalLibraryClass.class.getMethod("foo", new Class<?>[0]);
        Function<?> foo = FunctionFactory.createMethodWrappingFunction(fooMethod);
        addExpectedExternalFunction(foo);
    }
    
    private void addExpectedExternalFunction(Function<?> externalFunction) {
        Class<?> declaringType = externalFunction.getDeclaringType();
        if (!expectedExternalFunctions.containsKey(declaringType)) {
            expectedExternalFunctions.put(declaringType, new HashSet<Function<?>>());
        }
        expectedExternalFunctions.get(declaringType).add(externalFunction);
    }
    
    public Collection<Function<?>> getAllExpectedStatistics() {
        Collection<Function<?>> expectedStatistics = new HashSet<>();
        for (Collection<Function<?>> statisticsForType : this.expectedStatistics.values()) {
            expectedStatistics.addAll(statisticsForType);
        }
        return expectedStatistics;
    }
    
    public Collection<Function<?>> getExpectedDimensionsFor(Class<?> declaringType) {
        Collection<Function<?>> expectedDimensions = this.expectedDimensions.get(declaringType);
        return expectedDimensions != null ? expectedDimensions : new ArrayList<Function<?>>();
    }
    
    public Collection<Function<?>> getExpectedStatisticsFor(Class<?> declaringType) {
        Collection<Function<?>> expectedStatistics = this.expectedStatistics.get(declaringType);
        return expectedStatistics != null ? expectedStatistics : new ArrayList<Function<?>>();
    }
    
    public Collection<Function<?>> getExpectedExternalFunctionsFor(Class<?> declaringType) {
        Collection<Function<?>> expectedExternalFunctions = this.expectedExternalFunctions.get(declaringType);
        return expectedExternalFunctions != null ? expectedExternalFunctions : new ArrayList<Function<?>>();
    }

    public Set<Function<?>> getExpectedFunctionsFor(Class<?> declaringType) {
        Set<Function<?>> expectedFunctions = new HashSet<>();
        expectedFunctions.addAll(getExpectedDimensionsFor(declaringType));
        expectedFunctions.addAll(getExpectedStatisticsFor(declaringType));
        expectedFunctions.addAll(getExpectedExternalFunctionsFor(declaringType));
        return expectedFunctions;
    }

}
