package com.sap.sailing.selenium.core.impl;

import java.io.File;
import java.util.function.Supplier;

import org.openqa.selenium.WebDriver;

import com.sap.sailing.selenium.core.TestEnvironment;
import com.sap.sailing.selenium.core.WindowManager;

/**
 * <p>Default implementation of the test environment.</p>
 * 
 * @author
 *   D049941
 */
public class TestEnvironmentImpl implements TestEnvironment {
    private WindowManager manager;
    private String root;
    private File screenshotsFolder;
    
    /**
     * <p>Creates a new description of the test environment in which a test is executed.</p>
     * 
     * @param driver
     *   The web driver used for the execution of the test.
     * @param root
     *   The context root (base URL) against the tests should be executed.
     * @param screenshotsFolder
     *   The folder where screenshots should be stored.
     */
    public TestEnvironmentImpl(Supplier<WebDriver> webDriverFactory, String root, File screenshotsFolder) {
        this.root = root;
        this.screenshotsFolder = screenshotsFolder;
        this.manager = new WindowManager(webDriverFactory);
    }
    
    @Override
    public WebDriver getWebDriver() {
        return this.manager.getDefaultWebDriver();
    }

    @Override
    public WindowManager getWindowManager() {
        return this.manager;
    }

    @Override
    public String getContextRoot() {
        return this.root;
    }
    
    @Override
    public File getScreenshotFolder() {
        return this.screenshotsFolder;
    }

    /**
     * <p>Closes the test environment. This quits the web driver, closing every associated window.</p>
     */
    public void close() {
        this.manager.closeAllWindows();
        this.root = null;
        this.screenshotsFolder = null;
    }
}
