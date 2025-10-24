package com.sap.sailing.selenium.core;

import java.lang.reflect.Field;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

/**
 * <p>The factory for producing {@code SeleniumElementLocator}. For each call to {@link #createLocator} a new element
 *   locator will be returned, as expected by the interface.</p>
 * 
 * @author
 *   D049941
 */
public class SeleniumElementLocatorFactory implements ElementLocatorFactory {
    private SearchContext context;
    private final int timeOut;
    private final int interval;

    /**
     * <p>Creates a new factory which uses the given context and timeout for the search. For the search an interval of
     *   250ms is used.</p>
     * 
     * @param context
     *   The context for search of the element or the list of elements.
     * @param timeOut
     *   The maximum amount of time to wait for the element or the list of elements.
     */
    public SeleniumElementLocatorFactory(SearchContext context, int timeOut) {
        this(context, timeOut, 250);
    }

    /**
     * <p>Creates a new factory which uses the given context, timeout and interval for the search.</p>
     * 
     * @param context
     *   The context for search of an element or a list of elements.
     * @param timeOutSeconds
     *   The maximum amount of time to wait for the element or the list of elements in seconds
     * @param intervalMilliseconds
     *   The frequency with which to check the search context in milliseconds
     */
    public SeleniumElementLocatorFactory(SearchContext context, int timeOutSeconds, int intervalMilliseconds) {
        this.context = context;
        this.timeOut = timeOutSeconds;
        this.interval = intervalMilliseconds;
    }

    /**
     * <p>Returns a new element locator for the given field.</p>
     * 
     * @param field
     *   The field for which an element or an element list should be lazily locate.
     * @return
     *   The element locator for the search.
     */
    @Override
    public ElementLocator createLocator(Field field) {
        if (!Annotations.isAnnotationPresent(field)) {
            return null;
        }
        
        return new SeleniumElementLocator(this.context, field, this.timeOut, this.interval);
    }
}
