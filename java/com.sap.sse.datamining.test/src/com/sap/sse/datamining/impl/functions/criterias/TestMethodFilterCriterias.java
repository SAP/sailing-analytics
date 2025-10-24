package com.sap.sse.datamining.impl.functions.criterias;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;

public class TestMethodFilterCriterias {

    @Test
    public void testMethodIsCorrectDimensionFilterCriteria() {
        FilterCriterion<Method> filterCriteria = new MethodIsValidDimensionFilterCriterion();
        
        Method dimension = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("dimension");
        assertThat(filterCriteria.matches(dimension), is(true));
        
        Method illegalDimension = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("illegalDimension");
        assertThat(filterCriteria.matches(illegalDimension), is(false));
        
        Method unmarkedMethod = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("unmarkedMethod");
        assertThat(filterCriteria.matches(unmarkedMethod), is(false));
    }

    @Test
    public void testMethodIsCorrectSideEffectFreeValueFilterCriteria() {
        FilterCriterion<Method> filterCriteria = new MethodIsValidStatisticFilterCriterion();
        
        Method sideEffectFreeValue = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("sideEffectFreeValue");
        assertThat(filterCriteria.matches(sideEffectFreeValue), is(true));
        
        Method unmarkedMethod = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("unmarkedMethod");
        assertThat(filterCriteria.matches(unmarkedMethod), is(false));
    }

    @Test
    public void testMethodIsCorrectExternalLibraryFunctionFilterCriteria() {
        FilterCriterion<Method> filterCriteria = new MethodIsValidExternalFunctionFilterCriterion();
        
        Method foo = FunctionTestsUtil.getMethodFromExternalLibraryClass("foo");
        assertThat(filterCriteria.matches(foo), is(true));
        
        Method fooVoid = FunctionTestsUtil.getMethodFromExternalLibraryClass("fooVoid");
        assertThat(filterCriteria.matches(fooVoid), is(false));
    }

}
