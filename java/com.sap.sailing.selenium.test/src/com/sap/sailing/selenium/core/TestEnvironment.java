package com.sap.sailing.selenium.core;

import java.net.URL;

import org.openqa.selenium.WebDriver;

/**
 * <p>Description of the test environment in which a test is executed by the {@link Selenium} runner. The environment is
 *   injected by the runner to fields annotated with {@link Managed} and consists of the web driver which is used, a
 *   folder where screenshots should be stored and a context root (base URL) against the tests is executed.</p>
 * 
 * @author
 *   D049941
 */
public interface TestEnvironment {
    /**
     * <p>Returns the web driver used for the execution of the test.</p>
     * 
     * @return
     *   The web driver used for the execution of the test.
     */
    public WebDriver getWebDriver();
    
    public WindowManager getWindowManager();
    
    /**
     * <p>Returns the context root (base URL) against the tests should be executed. The context root identifies the
     *   web application and usually consists of a protocol definition, the host and a path.</p>
     * 
     * @return
     *   The context root (base URL) against the tests should be executed.
     */
    public String getContextRoot();
    
    /**
     * <p>Returns the folder where screenshots, captured during the test, should be stored</p>
     * 
     * @return
     *   The folder where screenshots should be stored.
     */
    public URL getScreenshotFolder();
}
