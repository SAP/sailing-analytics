package com.sap.sailing.selenium.core;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * <p></p>
 * 
 * @author
 *   Riccardo Nimser (D049941)
 */
public class WindowManager {
    private WebDriverWindow defaultWindow;
    private final Set<WebDriverWindow> allWindows = new HashSet<>();
    private WebDriver driver;

    private final Supplier<WebDriver> webDriverFactory;
    
    /**
     * <p></p>
     * 
     * @param driver
     *   
     */
    public WindowManager(Supplier<WebDriver> webDriverFactory) {
        this.webDriverFactory = webDriverFactory;
    }
    
    public WebDriver getDefaultWebDriver() {
        if (this.driver == null) {
            this.driver = webDriverFactory.get();
            this.defaultWindow = new ManagedWebDriverWindow(this.driver, this.driver.getWindowHandle());
            setWindowMaximized(this.driver);
        }
        return this.driver;
    }
    
    /**
     * This is the old, currently unused code superseded by {@link #withExtraWindow(WebDriver, BiConsumer)}.
     */
    public void withExtraWindow(BiConsumer<WebDriverWindow, WebDriverWindow> defaultAndExtraWindow) {
        // ensures that a default window exists
        getDefaultWebDriver();
        final WebDriver extraDriver = webDriverFactory.get();
        final WebDriverWindow extraWindow = new ManagedWebDriverWindow(extraDriver, extraDriver.getWindowHandle());
        extraWindow.switchToWindow();
        setWindowMaximized(extraDriver);
        defaultWindow.switchToWindow();
        defaultAndExtraWindow.accept(defaultWindow, extraWindow);
        try {
            // quit is explicitly not called in a finally block to ensure that both windows are still open
            // when trying to create screenshots in case an error occurs
            extraWindow.close();
            extraDriver.quit();
        } catch (Exception e) {
            // This call may fail depending on the WebDriver being used
        }
    }

    /**
     * New version of {@link #withExtraWindow(BiConsumer)} that uses the WebDriver passed as parameter and uses
     * a single {@link WebDriver} for both windows.
     */
    public void withExtraWindow(WebDriver driver, BiConsumer<WebDriverWindow, WebDriverWindow> defaultAndExtraWindow) {
        String originalWindowHandle = driver.getWindowHandle();
        // Open a new window using JavaScript
        ((JavascriptExecutor) driver).executeScript("window.open('about:blank','_blank');");
        // Wait until the new window appears
        new WebDriverWait(driver, /* seconds */ 5).until(d -> d.getWindowHandles().size() > 1);
        // Identify the new window handle
        String extraWindowHandle = driver.getWindowHandles().stream()
            .filter(handle -> !handle.equals(originalWindowHandle))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("New window did not open"));
        final WebDriverWindow defaultWindow = new ManagedWebDriverWindow(driver, originalWindowHandle);
        final WebDriverWindow extraWindow = new ManagedWebDriverWindow(driver, extraWindowHandle);
        try {
            extraWindow.switchToWindow();
            setWindowMaximized(driver);
            // Switch back to default window before calling the test logic
            defaultWindow.switchToWindow();
            // Run the test logic
            defaultAndExtraWindow.accept(defaultWindow, extraWindow);
        } finally {
            // Close the extra window and switch back to the original
            try {
                extraWindow.switchToWindow();
                driver.close();
            } catch (Exception ignored) {}
            // This call may fail depending on the WebDriver being used
            try {
                defaultWindow.switchToWindow();
            } catch (Exception ignored) {
                // This call may fail depending on the WebDriver being used
            }
        }
    }
    
    private boolean isDriverAlive(WebDriver driver) {
        try {
            driver.getWindowHandles();
            return true;
        } catch (NoSuchSessionException | SessionNotCreatedException e) {
            return false;
        }
    }
    
    private void setWindowMaximized(WebDriver driver) {
        try {
            driver.manage().window().maximize();
        } catch (Exception e) {
            // Depending on the combination of OS and WebDriver implementation this may fail
            // e.g. chrome with xvfb can't do this successfully.
            try {
                // Trying to set a proper screen size as fallback that should usable with all modern screens
                driver.manage().window().setSize(new Dimension(1440, 900));
            } catch (Exception exc) {
                // In this case we just can't change the window
            }
        }
    }
    
    public void forEachOpenedWindow(Consumer<WebDriverWindow> windowConsumer) {
        new HashSet<>(this.allWindows).forEach(windowConsumer);
    }
    
    public void closeAllWindows() {
        forEachOpenedWindow(WebDriverWindow::close);
    }
    
    private class ManagedWebDriverWindow extends WebDriverWindow {
        protected ManagedWebDriverWindow(WebDriver driver, String handle) {
            super(driver, handle);
            allWindows.add(this);
        }
        @Override
        public void close() {
            allWindows.remove(this);
            final WebDriver myDriver = driver;
            if (isDriverAlive(myDriver)) {
                try {
                    super.close();
                } catch (WebDriverException e) {
                    // If the window is already closed, we ignore the exception
                }
            }
        }
    }
}
