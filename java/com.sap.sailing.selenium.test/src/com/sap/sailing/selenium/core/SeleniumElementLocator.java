package com.sap.sailing.selenium.core;

import java.lang.reflect.Field;

import java.util.List;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.pagefactory.ElementLocator;

import org.openqa.selenium.support.ui.FluentWait;

import com.google.common.base.Function;

/**
 * <p>An element locator, which will lazily locate and wait for an element or an element list to appear, by polling the
 *   UI on a regular basis. It is possible to define the maximum amount of time to wait for the element or the list of
 *   elements, as well as the frequency with which to check the UI.</p>
 * 
 * <p>This class is designed for use with the page factory to make the usage of page objects as simple and easy as
 *   possible and understands the annotations {@link org.openqa.selenium.support.CacheLookup}, {@link FindBy} and
 *   {@link FindBys}.</p>
 * 
 * @author
 *   D049941
 */
public class SeleniumElementLocator implements ElementLocator {
    private final SearchContext context;
    private final FluentWait<SearchContext> wait;
    private final boolean useCache;

    private final By by;

    private WebElement element;
    private List<WebElement> elements;

    /**
     * <p>Creates a new element locator for the given field using the specified context and timeout for the search.</p>
     * 
     * @param context
     *   The context for search of the element or the list of elements.
     * @param field
     *   The field for which an element or an element list should be lazily locate.
     * @param timeOut
     *   The maximum amount of time to wait for the element or the list of elements.
     */
    public SeleniumElementLocator(SearchContext context, Field field, int timeOut) {
        this(context, field, timeOut, 250);
    }

    /**
     * <p>Creates a new element locator for the given field using the specified context, timeout and polling interval
     *   for the search.</p>
     * 
     * @param context
     *   The context for search of the element or the list of elements.
     * @param field
     *   The field for which an element or an element list should be lazily locate.
     * @param timeOut
     *   The maximum amount of time to wait for the element or the list of elements.
     * @param interval
     *   The frequency with which to check the search context.
     */
    public SeleniumElementLocator(SearchContext context, Field field, int timeOut, int interval) {
        this.context = context;

        this.wait = new FluentWait<>(this.context);
        this.wait.withTimeout(timeOut, TimeUnit.SECONDS);
        this.wait.pollingEvery(interval, TimeUnit.MILLISECONDS);
        this.wait.ignoring(NoSuchElementException.class);

        Annotations annotations = new Annotations(field);
        this.useCache = annotations.isLookupCached();
        this.by = annotations.buildBy();
    }

    /**
     * <p>Searches and returns the context for the first matching element using the mechanism defined on the field. The
     *   element returned will be present on the DOM and is actually visible. You can override
     *   {@link #isElementUsable(WebElement)} if it is important to you to define another condition.</p></p>
     * 
     * @return
     *   The first matching element in the search context, or null if nothing matches.
     */
    @Override
    public WebElement findElement() {
        if (this.useCache && this.element != null) {
            return this.element;
        }

        this.element = this.wait.until(new Function<SearchContext, WebElement>() {
            @Override
            public WebElement apply(SearchContext context) {
                WebElement element = context.findElement(SeleniumElementLocator.this.getBy());

                return (isElementUsable(element) ? element : null);
            }
        });

        return this.element;
    }

    /**
     * <p>Searches and returns the context for the list of all matching elements using the mechanism defined on the
     *   field. The elements returned will be present on the DOM and are actually visible. You can override
     *   {@link #isElementUsable(WebElement)} if it is important to you to define another condition.</p>
     * 
     * @return
     *   A list of all matching elements in the search context, or an empty list if nothing matches.
     */
    @Override
    public List<WebElement> findElements() {
        if (this.useCache && this.elements != null) {
            return this.elements;
        }

        this.elements = this.wait.until(new Function<SearchContext, List<WebElement>>() {
            @Override
            public List<WebElement> apply(SearchContext context) {
                return context.findElements(SeleniumElementLocator.this.getBy());
            }
        });

        return this.elements;
    }

    /**
     * <p>Returns the mechanism used to locate elements within the search context.</p>
     * 
     * @return
     *   The mechanism used to locate elements within the search context.
     */
    public By getBy() {
        return this.by;
    }
    
    /**
     * <p>By default, elements are considered as "found" if they are in the DOM and displayed. You can override this
     *   method in order to change whether or not an element is considered as loaded.</p>
     * 
     * @param element
     *   The element to use.
     * @return
     *   {@code true} if the element is usable and {@code false} otherwise.
     */
    protected boolean isElementUsable(WebElement element) {
        return element.isDisplayed();
    }
}
