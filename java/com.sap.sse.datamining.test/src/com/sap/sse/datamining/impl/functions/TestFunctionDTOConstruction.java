package com.sap.sse.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.test.data.impl.SimpleClassWithMarkedMethods;
import com.sap.sse.datamining.test.data.impl.Test_ExternalLibraryClass;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TestFunctionDTOConstruction {
    
    private static final ResourceBundleStringMessages stringMessages = TestsUtil.getTestStringMessages();

    private Method dimensionMethod;
    private Method sideEffectFreeValueMethod;
    private Method externalLibraryMethod;
    private Method incrementMethod;

    @BeforeEach
    public void initializeMethods() {
        dimensionMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "dimension");
        sideEffectFreeValueMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "sideEffectFreeValue");
        externalLibraryMethod = FunctionTestsUtil.getMethodFromClass(Test_ExternalLibraryClass.class, "foo");
        incrementMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "increment", int.class);
    }

    @Test
    public void testDimensionDTOConstruction() {
        Function<?> dimension = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(dimensionMethod);
        
        FunctionDTO expectedDimensionDTO = createExpectedDimensionDTO(dimension);
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension), is(expectedDimensionDTO));
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension, stringMessages, Locale.ENGLISH), is(expectedDimensionDTO));
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension, stringMessages, Locale.GERMAN), is(expectedDimensionDTO));
    }

    public FunctionDTO createExpectedDimensionDTO(Function<?> dimension) {
        String functionName = dimension.getSimpleName();
        String sourceTypeName = dimension.getDeclaringType().getName();
        String returnTypeName = dimension.getReturnType().getName();
        List<String> parameterTypeNames = new ArrayList<>();

        String displayName = functionName;
        boolean isDimension = true;
        int ordinal = 0;
        
        return new FunctionDTO(isDimension, functionName, sourceTypeName, returnTypeName, parameterTypeNames, displayName, ordinal);
    }
    
    @Test
    public void testNullarySideEffectFreeValueDTOConstruction() {
        Function<?> sideEffectFreeValue = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(sideEffectFreeValueMethod);
        
        FunctionDTO expectedDTO = createExpectedNullarySideEffectFreeValueDTO(sideEffectFreeValue);
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(sideEffectFreeValue, stringMessages, Locale.ENGLISH), is(expectedDTO));
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(sideEffectFreeValue, stringMessages, Locale.GERMAN), is(expectedDTO));
    }

    private FunctionDTO createExpectedNullarySideEffectFreeValueDTO(Function<?> sideEffectFreeValue) {
        String functionName = sideEffectFreeValue.getSimpleName();
        String sourceTypeName = sideEffectFreeValue.getDeclaringType().getName();
        String returnTypeName = sideEffectFreeValue.getReturnType().getName();
        List<String> parameterTypeNames = new ArrayList<>();

        String displayName = functionName;
        boolean isDimension = false;
        int ordinal = 0;
        
        return new FunctionDTO(isDimension, functionName, sourceTypeName, returnTypeName, parameterTypeNames, displayName, ordinal);
    }
    
    @Test
    public void testExternalLibraryFunctionDTOConstruction() {
        Function<?> externalLibraryFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(externalLibraryMethod);
        
        FunctionDTO expectedDTO = createExpectedExternalLibraryFunctionDTO(externalLibraryFunction);
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(externalLibraryFunction, stringMessages, Locale.ENGLISH), is(expectedDTO));
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(externalLibraryFunction, stringMessages, Locale.GERMAN), is(expectedDTO));
    }

    private FunctionDTO createExpectedExternalLibraryFunctionDTO(Function<?> externalLibraryFunction) {
        String functionName = externalLibraryFunction.getSimpleName();
        String sourceTypeName = externalLibraryFunction.getDeclaringType().getName();
        String returnTypeName = externalLibraryFunction.getReturnType().getName();
        List<String> parameterTypeNames = new ArrayList<>();

        String displayName = functionName;
        boolean isDimension = false;
        int ordinal = 0;
        
        return new FunctionDTO(isDimension, functionName, sourceTypeName, returnTypeName, parameterTypeNames, displayName, ordinal);
    }
    
    @Test
    public void testFunctionDTOConstructionForMethodWithParameters() {
        Function<?> increment = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(incrementMethod);
        
        FunctionDTO expectedDTO = createFunctionDTOWithParameters(increment);
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(increment, stringMessages, Locale.ENGLISH), is(expectedDTO));
        assertThat(FunctionTestsUtil.getDTOFactory().createFunctionDTO(increment, stringMessages, Locale.GERMAN), is(expectedDTO));
    }

    private FunctionDTO createFunctionDTOWithParameters(Function<?> increment) {
        String functionName = increment.getSimpleName();
        String sourceTypeName = increment.getDeclaringType().getName();
        String returnTypeName = increment.getReturnType().getName();
        List<String> parameterTypeNames = new ArrayList<>();
        parameterTypeNames.add(int.class.getName());

        String displayName = functionName;
        boolean isDimension = false;
        int ordinal = 0;
        
        return new FunctionDTO(isDimension, functionName, sourceTypeName, returnTypeName, parameterTypeNames, displayName, ordinal);
    }
    
    @Test
    public void testThatLocalizedFunctionDTOAndUnlocalizedAreEqual() {
        Function<?> dimension = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(dimensionMethod);
        
        FunctionDTO unlocalizedDimensionDTO = FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension);
        FunctionDTO englishDimensionDTO = FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension, TestsUtil.getTestStringMessages(), Locale.ENGLISH);
        FunctionDTO germanDimensionDTO = FunctionTestsUtil.getDTOFactory().createFunctionDTO(dimension, TestsUtil.getTestStringMessages(), Locale.GERMAN);
        
        assertThat(unlocalizedDimensionDTO, is(englishDimensionDTO));
        assertThat(englishDimensionDTO, is(germanDimensionDTO));
    }

}
