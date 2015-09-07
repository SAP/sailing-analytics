package com.sap.sse.datamining.functions;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import com.sap.sse.datamining.shared.annotations.Connector;
import com.sap.sse.datamining.shared.annotations.Dimension;
import com.sap.sse.datamining.shared.data.Unit;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public interface Function<ReturnType> {

    public Class<?> getDeclaringType();
    public Iterable<Class<?>> getParameters();
    public Class<ReturnType> getReturnType();
    
    /**
     * Checks if this function and the given function would return the same result or if they are {@link #equals(Object)}.
     * This can be the case, if the only difference between the two functions is a {@link Connector} to a Data Type
     * to inherit its {@link Dimension Dimensions}. 
     * 
     * @param function The function to compare.
     * @return <code>true</code>, if this function and the given function would return the same result.
     */
    public boolean isLogicalEqualTo(Function<?> function);

    public boolean isDimension();
    
    public String getSimpleName();
    
    /**
     * @return <code>true</code>, if the method {@link #getLocalizedName(Locale, ResourceBundleStringMessages) getLocalizedName} would return something
     * other than the method {@link #getSimpleName()}.
     */
    public boolean isLocalizable();
    
    /**
     * @return the localized name of this function, if it can be {@link #isLocalizable() localized} and the {@link #getSimpleName() simple name} if not.
     */
    public String getLocalizedName(Locale locale, ResourceBundleStringMessages stringMessages);
    
    /**
     * Tries to invoke the function for the given <code>instance</code> without parameters.
     * 
     * @return The result of the function or <code>null</code>, if an {@link InvocationTargetException},
     *         {@link IllegalAccessException} or {@link IllegalArgumentException} was thrown.
     */
    public ReturnType tryToInvoke(Object instance);
    
    /**
     * Tries to invoke the function for the given <code>instance</code> and the <code>parameters</code> provided
     * by the given {@link ParameterProvider}.
     * 
     * @return The result of the function or <code>null</code>, if an {@link InvocationTargetException},
     *         {@link IllegalAccessException} or {@link IllegalArgumentException} was thrown.
     */
    public ReturnType tryToInvoke(Object instance, ParameterProvider parameterProvider);
    
    public Unit getResultUnit();
    
    public int getResultDecimals();
    
    public int getOrdinal();

}
