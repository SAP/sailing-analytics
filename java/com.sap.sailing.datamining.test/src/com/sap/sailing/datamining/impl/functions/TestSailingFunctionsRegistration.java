package com.sap.sailing.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.datamining.data.HasGPSFixContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.test.util.OpenFunctionManager;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.common.Moving;
import com.sap.sse.common.Named;
import com.sap.sse.common.Speed;
import com.sap.sse.datamining.factories.FunctionFactory;
import com.sap.sse.datamining.functions.Function;

public class TestSailingFunctionsRegistration {

    private static FunctionFactory functionFactory = new FunctionFactory();
    
    private OpenFunctionManager functionRegistry;
    
    @BeforeEach
    public void setUpFunctionRegistryAndProvider() {
        functionRegistry = new OpenFunctionManager();
        functionRegistry.registerAllClasses(Activator.getDefault().getClassesWithMarkedMethods());
    }
    
    @Test
    public void testImportantRegisteredDimensions() throws NoSuchMethodException, SecurityException {
        Collection<Function<?>> expectedDimensions = createExpectedImportantRegisteredDimensions();
        Collection<Function<?>> providedDimensions = functionRegistry.getDimensions();
        for (Function<?> dimension : expectedDimensions) {
            assertThat("The expected dimension '" + dimension + "' wasn't provided.",
                       providedDimensions.contains(dimension), is(true));
        }
    }

    private Collection<Function<?>> createExpectedImportantRegisteredDimensions() throws NoSuchMethodException, SecurityException {
        Collection<Function<?>> expectedDimensions = new HashSet<>();
        
        Method getRegattaMethod = HasTrackedRaceContext.class.getMethod("getRegatta", new Class<?>[0]);
        Function<?> getRegatta = functionFactory.createMethodWrappingFunction(getRegattaMethod);
        Method getNameMethod = Named.class.getMethod("getName", new Class<?>[0]);
        Function<?> getName = functionFactory.createMethodWrappingFunction(getNameMethod);
        expectedDimensions.add(functionFactory.createCompoundFunction(Arrays.asList(getRegatta, getName)));
        
        Method getYearMethod = HasTrackedRaceContext.class.getMethod("getYear", new Class<?>[0]);
        expectedDimensions.add(functionFactory.createMethodWrappingFunction(getYearMethod));
        
        Method getCompetitorMethod = HasTrackedLegOfCompetitorContext.class.getMethod("getCompetitor", new Class<?>[0]);
        Function<?> getCompetitor = functionFactory.createMethodWrappingFunction(getCompetitorMethod);
        Method getNationalityMethod = Competitor.class.getMethod("getNationality", new Class<?>[0]);
        Function<?> getNationality = functionFactory.createMethodWrappingFunction(getNationalityMethod);
        Method getAcronymMethod = Nationality.class.getMethod("getThreeLetterIOCAcronym", new Class<?>[0]);
        Function<?> getAcronym = functionFactory.createMethodWrappingFunction(getAcronymMethod);
        expectedDimensions.add(functionFactory.createCompoundFunction(Arrays.asList(getCompetitor, getNationality, getAcronym)));
        
        return expectedDimensions;
    }
    
    @Test
    public void testImportantRegisteredStatistics() throws NoSuchMethodException, SecurityException {
        Collection<Function<?>> expectedStatistics = createExpectedImportantRegisteredStatistics();
        Collection<Function<?>> providedStatistics = functionRegistry.getAllStatistics();
        for (Function<?> statistic : expectedStatistics) {
            assertThat("The expected statistic '" + statistic + "' wasn't provided.",
                       providedStatistics.contains(statistic), is(true));
        }
    }

    private Collection<Function<?>> createExpectedImportantRegisteredStatistics() throws NoSuchMethodException, SecurityException {
        Collection<Function<?>> expectedStatistics = new HashSet<>();
        
        Method getDistanceTraveledMethod = HasTrackedLegOfCompetitorContext.class.getMethod("getDistanceTraveled", new Class<?>[0]);
        expectedStatistics.add(functionFactory.createMethodWrappingFunction(getDistanceTraveledMethod));
        
        Method getGPSFixMethod = HasGPSFixContext.class.getMethod("getGPSFix", new Class<?>[0]);
        Function<?> getGPSFix = functionFactory.createMethodWrappingFunction(getGPSFixMethod);
        Method getSpeedMethod = Moving.class.getMethod("getSpeed", new Class<?>[0]);
        Function<?> getSpeed = functionFactory.createMethodWrappingFunction(getSpeedMethod);
        Method getKnotsMethod = Speed.class.getMethod("getKnots", new Class<?>[0]);
        Function<?> getKnots = functionFactory.createMethodWrappingFunction(getKnotsMethod);
        expectedStatistics.add(functionFactory.createCompoundFunction(Arrays.asList(getGPSFix, getSpeed, getKnots)));
        
        return expectedStatistics;
    }

}
