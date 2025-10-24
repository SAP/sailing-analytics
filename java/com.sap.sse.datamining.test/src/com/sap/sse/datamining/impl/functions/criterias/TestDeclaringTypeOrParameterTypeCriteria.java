package com.sap.sse.datamining.impl.functions.criterias;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.test.data.impl.DataTypeInterface;
import com.sap.sse.datamining.test.data.impl.DataTypeWithContext;
import com.sap.sse.datamining.test.data.impl.DataTypeWithContextImpl;
import com.sap.sse.datamining.test.data.impl.ExtendingInterface;
import com.sap.sse.datamining.test.data.impl.Test_ExternalLibraryClass;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;

public class TestDeclaringTypeOrParameterTypeCriteria {

    private Function<?> getSpeedInKnotsValue;
    private Function<?> getRaceNameLengthValue;
    private Function<?> getRegattaNameDimension;
    
    private Function<?> libraryFunction;
    
    @BeforeEach
    public void setUpFunctions() {
        getSpeedInKnotsValue = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(DataTypeInterface.class, "getSpeedInKnots"));
        getRaceNameLengthValue = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(ExtendingInterface.class, "getRaceNameLength"));
        getRegattaNameDimension = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(DataTypeWithContext.class, "getRegattaName"));
        
        libraryFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(Test_ExternalLibraryClass.class, "foo"));
    }

    @Test
    public void testMatchingTypeWithBigHierarchy() {
        FilterCriterion<Function<?>> criteria = new IsDeclaringTypeFilterCriterion(DataTypeWithContextImpl.class);

        assertThat(criteria.matches(getSpeedInKnotsValue), is(true));
        assertThat(criteria.matches(getRaceNameLengthValue), is(true));
        assertThat(criteria.matches(getRegattaNameDimension), is(true));

        assertThat(criteria.matches(libraryFunction), is(false));
    }
    
    @Test
    public void testGetElementType() {
        FilterCriterion<Function<?>> criteria = new IsDeclaringTypeFilterCriterion(DataTypeWithContextImpl.class);
        assertThat(criteria.getElementType().equals(Function.class), is(true));
    }

}
