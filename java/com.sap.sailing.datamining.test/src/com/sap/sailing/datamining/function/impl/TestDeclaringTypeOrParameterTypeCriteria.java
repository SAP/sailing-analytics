package com.sap.sailing.datamining.function.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.datamining.ConcurrentFilterCriteria;
import com.sap.sailing.datamining.factories.FunctionFactory;
import com.sap.sailing.datamining.function.Function;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeInterface;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeWithContext;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeWithContextImpl;
import com.sap.sailing.datamining.test.function.test_classes.ExtendingInterface;
import com.sap.sailing.datamining.test.function.test_classes.ExternalLibraryClass;
import com.sap.sailing.datamining.test.util.FunctionTestsUtil;

public class TestDeclaringTypeOrParameterTypeCriteria {

    private Function<?> getSpeedInKnotsValue;
    private Function<?> getRaceNameLengthValue;
    private Function<?> getRegattaNameDimension;
    
    private Function<?> libraryFunction;
    
    @Before
    public void setUpFunctions() {
        getSpeedInKnotsValue = FunctionFactory.createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(DataTypeInterface.class, "getSpeedInKnots"));
        getRaceNameLengthValue = FunctionFactory.createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(ExtendingInterface.class, "getRaceNameLength"));
        getRegattaNameDimension = FunctionFactory.createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(DataTypeWithContext.class, "getRegattaName"));
        
        libraryFunction = FunctionFactory.createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(ExternalLibraryClass.class, "foo"));
    }

    @Test
    public void testMatchingTypeWithBigHierarchy() {
        ConcurrentFilterCriteria<Function<?>> criteria = new DeclaringTypeOrParameterTypeCriteria(DataTypeWithContextImpl.class);

        assertThat(criteria.matches(getSpeedInKnotsValue), is(true));
        assertThat(criteria.matches(getRaceNameLengthValue), is(true));
        assertThat(criteria.matches(getRegattaNameDimension), is(true));

        assertThat(criteria.matches(libraryFunction), is(false));
    }

}
