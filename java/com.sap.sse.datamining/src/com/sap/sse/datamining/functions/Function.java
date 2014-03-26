package com.sap.sse.datamining.functions;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import com.sap.sse.datamining.i18n.DataMiningStringMessages;
import com.sap.sse.datamining.shared.Unit;

public interface Function<ReturnType> {

    public Class<?> getDeclaringType();
    public Iterable<Class<?>> getParameters();
    public Class<ReturnType> getReturnType();

    public boolean isDimension();
    
    public String getSimpleName();
    
    public String getLocalizedName(Locale locale, DataMiningStringMessages stringMessages);
    
    /**
     * Tries to invoke the function for the given <code>instance</code> without parameters.
     * 
     * @return The result of the function or <code>null</code>, if an {@link InvocationTargetException},
     *         {@link IllegalAccessException} or {@link IllegalArgumentException} was thrown.
     */
    public ReturnType tryToInvoke(Object instance);
    
    /**
     * Tries to invoke the function for the given <code>instance</code> and the given <code>parameters</code>.
     * 
     * @return The result of the function or <code>null</code>, if an {@link InvocationTargetException},
     *         {@link IllegalAccessException} or {@link IllegalArgumentException} was thrown.
     */
    public ReturnType tryToInvoke(Object instance, Object... parameters);
    
    public Unit getResultUnit();
    
    public int getResultDecimals();

}
