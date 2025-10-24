package com.sap.sse.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.test.data.impl.SimpleClassWithMarkedMethods;
import com.sap.sse.datamining.test.data.impl.Test_ExternalLibraryClass;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TestMethodWrappingFunction {
    
    private Method dimensionMethod;
    private Method sideEffectFreeValueMethod;
    private Method externalLibraryMethod;

    @BeforeEach
    public void initializeMethods() {
        dimensionMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "dimension");
        sideEffectFreeValueMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "sideEffectFreeValue");
        externalLibraryMethod = FunctionTestsUtil.getMethodFromClass(Test_ExternalLibraryClass.class, "foo");
    }
    
    @Test
    public void testMethodReturnTypeAndGivenReturnTypeDoesntMatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            Function<String> function = new MethodWrappingFunction<>(externalLibraryMethod, String.class);
        });
    }
    
    @Test
    public void testIsDimension() {
        Function<?> dimension = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(dimensionMethod);
        assertThat(dimension.isDimension(), is(true));

        Function<?> sideEffectFreeValue = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(sideEffectFreeValueMethod);
        assertThat(sideEffectFreeValue.isDimension(), is(false));

        Function<?> externalLibraryFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(externalLibraryMethod);
        assertThat(externalLibraryFunction.isDimension(), is(false));
    }
    
    @Test
    public void testGetLocalizedName() {
        ResourceBundleStringMessages stringMessages = TestsUtil.getTestStringMessages();

        Function<?> dimension = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(dimensionMethod);
        String expectedEnglishName = "dimension-english";
        assertThat(dimension.getLocalizedName(Locale.ENGLISH, stringMessages), is(expectedEnglishName));
        String expectedGermanName = "dimension-deutsch";
        assertThat(dimension.getLocalizedName(Locale.GERMAN, stringMessages), is(expectedGermanName));

        Function<?> sideEffectFreeValue = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(sideEffectFreeValueMethod);
        expectedEnglishName = "sideEffectFreeValue-english";
        assertThat(sideEffectFreeValue.getLocalizedName(Locale.ENGLISH, stringMessages), is(expectedEnglishName));
        expectedGermanName = "sideEffectFreeValue-deutsch";
        assertThat(sideEffectFreeValue.getLocalizedName(Locale.GERMAN, stringMessages), is(expectedGermanName));

        Function<?> externalLibraryFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(externalLibraryMethod);
        String expectedName = "foo()";
        assertThat(externalLibraryFunction.getLocalizedName(Locale.ENGLISH, stringMessages), is(expectedName));
        assertThat(externalLibraryFunction.getLocalizedName(Locale.GERMAN, stringMessages), is(expectedName));
    }

}
