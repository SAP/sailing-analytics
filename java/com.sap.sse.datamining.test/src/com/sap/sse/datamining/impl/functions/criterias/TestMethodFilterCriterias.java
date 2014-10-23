package com.sap.sse.datamining.impl.functions.criterias;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;

import org.junit.Test;

import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;

public class TestMethodFilterCriterias {

    @Test
    public void testMethodIsCorrectDimensionFilterCriteria() {
        FilterCriterion<Method> filterCriteria = new MethodIsCorrectDimensionFilterCriterion();
        
        Method dimension = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("dimension");
        assertThat(filterCriteria.matches(dimension), is(true));
        
        Method illegalDimension = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("illegalDimension");
        assertThat(filterCriteria.matches(illegalDimension), is(false));
        
        Method unmarkedMethod = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("unmarkedMethod");
        assertThat(filterCriteria.matches(unmarkedMethod), is(false));
    }

    @Test
    public void testMethodIsCorrectSideEffectFreeValueFilterCriteria() {
        FilterCriterion<Method> filterCriteria = new MethodIsCorrectStatisticFilterCriterion();
        
        Method sideEffectFreeValue = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("sideEffectFreeValue");
        assertThat(filterCriteria.matches(sideEffectFreeValue), is(true));
        
        Method unmarkedMethod = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("unmarkedMethod");
        assertThat(filterCriteria.matches(unmarkedMethod), is(false));
    }

    @Test
    public void testMethodIsCorrectExternalLibraryFunctionFilterCriteria() {
        FilterCriterion<Method> filterCriteria = new MethodIsCorrectExternalFunctionFilterCriterion();
        
        Method foo = FunctionTestsUtil.getMethodFromExternalLibraryClass("foo");
        assertThat(filterCriteria.matches(foo), is(true));
        
        Method fooVoid = FunctionTestsUtil.getMethodFromExternalLibraryClass("fooVoid");
        assertThat(filterCriteria.matches(fooVoid), is(false));
    }

}
