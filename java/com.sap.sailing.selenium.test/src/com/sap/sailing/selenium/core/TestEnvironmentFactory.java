package com.sap.sailing.selenium.core;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.sap.sailing.selenium.core.TestEnvironmentConfiguration.DriverDefinition;
import com.sap.sailing.selenium.core.impl.TestEnvironmentImpl;

public class TestEnvironmentFactory {
    private static final Logger logger = Logger.getLogger(TestEnvironmentFactory.class.getName());

    /**
     * Number of attempts to construct a {@link WebDriver} before giving up. Browser session creation (in particular
     * for headless Firefox via geckodriver/Marionette) can fail intermittently under load with a
     * {@link org.openqa.selenium.SessionNotCreatedException}, so transient failures are retried a few times.
     */
    private static final int MAX_WEB_DRIVER_CREATION_ATTEMPTS = 3;

    /**
     * Delay between two consecutive {@link WebDriver} creation attempts.
     */
    private static final long WEB_DRIVER_CREATION_RETRY_DELAY_MILLIS = 2000L;

    public static TestEnvironmentImpl create(final DriverDefinition def) throws Exception {
        final String driverClassname = def.getDriver();
        final Map<String, Object> capabilities = def.getCapabilities();
        @SuppressWarnings("unchecked")
        final Class<WebDriver> clazz = (Class<WebDriver>) Class.forName(driverClassname);
        final Constructor<WebDriver> constructor = clazz.getConstructor(Capabilities.class);
        // The class and constructor are resolved eagerly so configuration mistakes fail fast, but the WebDriver
        // (and thus the browser process) is created lazily by the WindowManager on first access. Tests that never
        // touch a browser (e.g. pure REST-API tests) therefore never start one and cannot be affected by browser
        // session startup flakiness.
        final Supplier<WebDriver> webDriverFactory = () -> createWebDriver(constructor, capabilities, driverClassname);
        return new TestEnvironmentImpl(webDriverFactory, TestEnvironmentConfiguration.getInstance().getContextRoot(),
                new File(TestEnvironmentConfiguration.getInstance().getScreenshotsFolder()));
    }

    /**
     * Constructs a fresh {@link WebDriver} using the given constructor, retrying transient browser session startup
     * failures up to {@link #MAX_WEB_DRIVER_CREATION_ATTEMPTS} times. A new instance is returned on every call so
     * that callers requiring a separate browser (e.g. {@link WindowManager#withExtraBrowser}) genuinely obtain one.
     */
    private static WebDriver createWebDriver(final Constructor<WebDriver> constructor,
            final Map<String, Object> capabilities, final String driverClassname) {
        WebDriver result = null;
        WebDriverException lastTransientFailure = null;
        for (int attempt = 1; attempt <= MAX_WEB_DRIVER_CREATION_ATTEMPTS && result == null; attempt++) {
            try {
                result = constructor.newInstance(new DesiredCapabilities(capabilities));
            } catch (final InvocationTargetException e) {
                // Only browser/session startup problems are considered transient and worth retrying; any other
                // cause is a genuine defect and is rethrown immediately.
                if (e.getCause() instanceof WebDriverException) {
                    lastTransientFailure = (WebDriverException) e.getCause();
                    logger.log(Level.WARNING, "Attempt " + attempt + " of " + MAX_WEB_DRIVER_CREATION_ATTEMPTS
                            + " to create WebDriver " + driverClassname + " failed", lastTransientFailure);
                    if (attempt < MAX_WEB_DRIVER_CREATION_ATTEMPTS) {
                        sleepBeforeRetry();
                    }
                } else {
                    throw new RuntimeException("Could not create WebDriver " + driverClassname, e.getCause());
                }
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException("Could not create WebDriver " + driverClassname, e);
            }
        }
        if (result == null) {
            throw new RuntimeException("Could not create WebDriver " + driverClassname + " after "
                    + MAX_WEB_DRIVER_CREATION_ATTEMPTS + " attempts", lastTransientFailure);
        }
        return result;
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(WEB_DRIVER_CREATION_RETRY_DELAY_MILLIS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry WebDriver creation", e);
        }
    }
}
