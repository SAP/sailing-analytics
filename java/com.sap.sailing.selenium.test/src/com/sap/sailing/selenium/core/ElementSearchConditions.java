package com.sap.sailing.selenium.core;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

/**
 * <p>Canned conditions which use a search context and which are generally useful within tests.</p>
 * 
 * @author
 *   D049941
 */
public class ElementSearchConditions {
    /**
     * An expectation for checking that an element is present on the DOM of a page and visible. Visibility means that
     * the element is not only displayed but also has a height and width that is greater than 0.
     * 
     * @param locator
     *   used to find the element
     * @return
     *   the WebElement once it is located and visible
     */
    public static ElementSearchCondition<WebElement> visibilityOfElementLocated(final By locator) {
        return new ElementSearchCondition<WebElement>() {
            @Override
            public WebElement apply(SearchContext context) {
                try {
                    return elementIfVisible(findElement(context, locator));
                } catch (final NoSuchElementException | StaleElementReferenceException exception) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return "visibility of element located by " + locator; //$NON-NLS-1$
            }
        };
    }

    private static WebElement elementIfVisible(WebElement element) {
        return element.isDisplayed() ? element : null;
    }

    private static WebElement findElement(SearchContext context, By by) {
        return context.findElement(by);
    }

    private ElementSearchConditions() {
        // Utility class
    }
}
