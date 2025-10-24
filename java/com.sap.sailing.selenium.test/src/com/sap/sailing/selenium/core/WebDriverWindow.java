package com.sap.sailing.selenium.core;

import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.openqa.selenium.WebDriverException;

public class WebDriverWindow {
    
    private WebDriver driver;
    
    private String handle;
    
    /**
     * Creates a new window for given web driver
     * 
     * @param driver
     *   WebDriver instance
     * @param url
     *   Initial URL to load
     */
    protected WebDriverWindow(WebDriver driver, String handle) {
        this.driver = driver;
        this.handle = handle;
    }
    
    public String getWindowHandle() {
        return this.handle;
    }

    public void close() {
        WebDriver driver = switchToWindow();
        driver.close();
        this.handle = null;
    }

    public WebDriver switchToWindow() {
        checkForClosed();
        TargetLocator locator = this.driver.switchTo();
        return locator.window(this.handle);
    }

    private void checkForClosed() {
        Set<String> handles = this.driver.getWindowHandles();
        if (!handles.contains(this.handle)) {
            throw new WebDriverException("Window closed or not initialized"); //$NON-NLS-1$
        }
    }
    
    public WebDriver getWebDriver() {
        return driver;
    }
}
