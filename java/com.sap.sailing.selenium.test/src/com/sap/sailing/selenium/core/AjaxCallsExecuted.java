package com.sap.sailing.selenium.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * <p>Models a condition that checks for the number of completed Ajax calls. The condition is used in tests where
 *   asynchronous request are triggered automatically to be able able to tell when a specified number of requests
 *   completed. The condition checks for the counter of a specified category as introduced by
 *   {@code PendingAjaxCallBundle}.</p>
 * 
 * @author
 *   D049941
 */
public class AjaxCallsExecuted implements ExpectedCondition<Boolean> {
    /**
     * <p>The key for the global category as defined in the class
     *   <code>com.sap.sailing.gwt.ui.client.MarkedAsyncCallback</code>.</p>
     */
    public static final String CATEGORY_GLOBAL = ""; //$NON-NLS-1$
    
    private static final String JAVASCRIPT = "return (window.PENDING_AJAX_CALLS.numberOfFinishedCalls(arguments[0]) >= arguments[1])"; //$NON-NLS-1$
    
    private String category;
    
    private int numberOfCalls;
    
    /**
     * <p>Creates a new condition that checks the global category of Ajax calls.</p>
     */
    public AjaxCallsExecuted(int numberOfCalls) {
        this(CATEGORY_GLOBAL, numberOfCalls);
    }
    
    /**
     * <p>Creates a new condition that checks the given category of Ajax calls.</p>
     * 
     * @param category
     *   The category of Ajax calls to check.
     */
    public AjaxCallsExecuted(String category, int numberOfCalls) {
        this.category = category;
        this.numberOfCalls = numberOfCalls;
    }
    
    /**
     * <p>Determines if the number of Ajax calls have been completed. Returns a boolean representing {@code true} if so
     *   and boolean representing {@code false} otherwise.</p>
     * 
     * @param driver
     *   The web driver to use.
     * @return
     *   A boolean representing {@code true} if the number of Ajax calls have been completed and {@code false} otherwise.
     */
    @Override
    public Boolean apply(WebDriver driver) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        
        return (Boolean) executor.executeScript(JAVASCRIPT, this.category, this.numberOfCalls);
    }
}
