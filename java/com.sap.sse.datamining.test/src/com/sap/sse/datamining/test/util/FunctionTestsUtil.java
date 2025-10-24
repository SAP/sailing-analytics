package com.sap.sse.datamining.test.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.datamining.factories.FunctionFactory;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.test.data.impl.DataTypeInterface;
import com.sap.sse.datamining.test.data.impl.DataTypeWithContext;
import com.sap.sse.datamining.test.data.impl.DataTypeWithContextImpl;
import com.sap.sse.datamining.test.data.impl.ExtendingInterface;
import com.sap.sse.datamining.test.data.impl.SimpleClassWithMarkedMethods;
import com.sap.sse.datamining.test.data.impl.Test_ExternalLibraryClass;

public class FunctionTestsUtil extends ConcurrencyTestsUtil {

    private static final FunctionFactory functionFactory = new FunctionFactory();
    
    public static FunctionFactory getFunctionFactory() {
        return functionFactory;
    }

    public static Collection<Function<?>> getMarkedMethodsOfSimpleClassWithMarkedMethod() {
        Set<Function<?>> markedMethods = new HashSet<>();
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromSimpleClassWithMarkedMethod("dimension")));
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromSimpleClassWithMarkedMethod("sideEffectFreeValue")));
        return markedMethods;
    }
    
    public static Method getMethodFromSimpleClassWithMarkedMethod(String name) {
        return getMethodFromClass(SimpleClassWithMarkedMethods.class, name);
    }

    public static Collection<Function<?>> getMethodsOfExternalLibraryClass() {
        Set<Function<?>> markedMethods = new HashSet<>();
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromExternalLibraryClass("foo")));
        return markedMethods;
    }

    public static Method getMethodFromExternalLibraryClass(String name) {
        return getMethodFromClass(Test_ExternalLibraryClass.class, name);
    }

    public static Collection<Function<?>> getMarkedMethodsOfDataTypeWithContextImplAndItsSupertypes() {
        Collection<Function<?>> markedMethods = getMarkedMethodsOfDataTypeWithContextAndItsSupertypes();
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(ExtendingInterface.class, "getRaceNameLength")));
        return markedMethods;
    }

    public static Collection<Function<?>> getMarkedMethodsOfDataTypeWithContextAndItsSupertypes() {
        Collection<Function<?>> markedMethods = new HashSet<>();
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(DataTypeInterface.class, "getSpeedInKnots")));
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(DataTypeWithContext.class, "getRegattaName")));
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(DataTypeWithContext.class, "getRaceName")));
        markedMethods.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(DataTypeWithContext.class, "getLegNumber")));
        return markedMethods;
    }
    
    public static Method getMethodFromClass(Class<?> fromClass, String methodName) {
        return getMethodFromClass(fromClass, methodName, (Class<?>[]) null);
    }

    public static Method getMethodFromClass(Class<?> fromClass, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = fromClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            fail("Failed to get the method with name '" + methodName + "' from class '" + fromClass.getSimpleName() + "'. Have the names been changed?");
        }
        return method;
    }

    public static Collection<Function<?>> getDimensionsFor(Class<?> dataType) {
        if (dataType.equals(DataTypeWithContext.class) || dataType.equals(DataTypeWithContextImpl.class)) {
            return getDimensionsForDataTypeWithContext();
        }
        if (dataType.equals(SimpleClassWithMarkedMethods.class)) {
            return getDimensionsForSimpleClassWithMarkedMethods();
        }
        throw new IllegalArgumentException("There are no dimensions for " + dataType.getSimpleName());
    }

    private static Collection<Function<?>> getDimensionsForSimpleClassWithMarkedMethods() {
        Collection<Function<?>> dimensions = new HashSet<>();
        dimensions.add(functionFactory.createMethodWrappingFunction(getMethodFromSimpleClassWithMarkedMethod("dimension")));
        return dimensions;
    }

    private static Collection<Function<?>> getDimensionsForDataTypeWithContext() {
        Collection<Function<?>> dimensions = new HashSet<>();
        dimensions.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(DataTypeWithContext.class, "getRegattaName")));
        dimensions.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(DataTypeWithContext.class, "getRaceName")));
        dimensions.add(functionFactory.createMethodWrappingFunction(getMethodFromClass(DataTypeWithContext.class, "getLegNumber")));
        return dimensions;
    }
    
    private FunctionTestsUtil() {
    }

}
