package com.sap.sailing.selenium.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * <p>Models a condition that checks for pending Ajax calls. The condition is used in tests to be able able to tell
 *   exactly when an asynchronous request has finished. The condition checks for the counter of a specified category as
 *   introduced by {@code PendingAjaxCallBundle} and {@code PendingAjaxCallMarker}.</p>
 * 
 * @author
 *   D049941
 */
public class AjaxCallsComplete implements ExpectedCondition<Boolean> {
    private static final Logger logger = Logger.getLogger(AjaxCallsComplete.class.getName());
    
    /**
     * <p>The key for the global category as defined in the class
     *   <code>com.sap.sailing.gwt.ui.client.MarkedAsyncCallback</code>.</p>
     */
    public static final String CATEGORY_GLOBAL = ""; //$NON-NLS-1$
    
    private static final String JAVASCRIPT = "return " + AjaxCallsExecuted.PENDING_AJAX_CALLS + " && (" + AjaxCallsExecuted.PENDING_AJAX_CALLS + ".numberOfPendingCalls(arguments[0]) === 0)"; //$NON-NLS-1$
    
    private String category;
    
    /**
     * <p>Creates a new condition that checks the global category of Ajax calls.</p>
     */
    public AjaxCallsComplete() {
        this(CATEGORY_GLOBAL);
    }
    
    /**
     * <p>Creates a new condition that checks the given category of Ajax calls.</p>
     * 
     * @param category
     *   The category of Ajax calls to check.
     */
    public AjaxCallsComplete(String category) {
        this.category = category;
    }
    
    /**
     * <p>Determines if all pending Ajax call have been completed. Returns a boolean representing {@code true} if so
     *   and boolean representing {@code false} otherwise.</p>
     * 
     * @param driver
     *   The web driver to use.
     * @return
     *   A boolean representing {@code true} if all pending Ajax calls have been completed and {@code false} otherwise.
     */
    @Override
    public Boolean apply(WebDriver driver) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        boolean result = false;
        try {
            result = Boolean.TRUE.equals(executor.executeScript(JAVASCRIPT, this.category));
        } catch (WebDriverException e) {
            logger.log(Level.WARNING, "Exception looking for PENDING_AJAX_CALLS variable. Telling caller that not all AJAX calls have finished yet.", e);
        }
        return result;
    }
}
