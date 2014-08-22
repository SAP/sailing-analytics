package com.sap.sse.datamining.shared.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods marked with this annotation will be used as dimensions for the data mining framework. The method will be called,
 * if the dimension value of a data element is requested.<br />
 * The marked method has to match the following conditions or the data mining could fail:
 * <ul>
 *      <li>Is side effect free</li>
 *      <li>Is a member of an atomic data type (like GPSFix or TrackedLeg) <b>or</b><br />
 *          has an atomic data type as parameter</li>
 * </ul>
 * 
 * The return value of a marked method should be:
 * <ul>
 *      <li>a primitive type or wrapper class.</li>
 *      <li>classes that implement <code>equals()</code>, <code>hashCode()</code> and <code>toString()</code>.
 * </ul>
 * Otherwise the grouping could become incorrect and the result presentation will be unreadable.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dimension {
    
    public String messageKey();
    
    public int ordinal() default Integer.MAX_VALUE;

}
