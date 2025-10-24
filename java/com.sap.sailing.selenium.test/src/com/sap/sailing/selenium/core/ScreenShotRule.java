package com.sap.sailing.selenium.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class ScreenShotRule implements TestExecutionExceptionHandler {
    private static final Logger logger = Logger.getLogger(ScreenShotRule.class.getName());

    private static final String NOT_SUPPORTED_IMAGE = "/com/sap/sailing/selenium/resources/not-supported.png"; //$NON-NLS-1$
    private static final String ATTACHMENT_FORMAT = "[[ATTACHMENT|%s]]"; //$NON-NLS-1$
    /**
     * <p>File extension for screenshots captured with a Selenium web driver.</p>
     */
    static final String SCREENSHOT_FILE_EXTENSION = ".png"; //$NON-NLS-1$
    
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable cause) throws Throwable {
        captureScreenshots(context);
        throw cause;
    }

    /**
     * <p>Captures a screen shot and saves the picture as an PNG file under the given file name. The complete path to
     *   the stored picture consists of the screenshot folder, as defined in the test environment, and the given
     *   filename with "png" as file extension.</p>
     * 
     * <p>If the used web driver does not support the capturing of screenshots an alternative picture is used instead
     *   of the screenshot.</p>
     * 
     * @param filename
     *   The file name under which the screenshot should be saved.
     * @throws IOException
     *   if an I/O error occurs.
     */
    private void captureScreenshots(ExtensionContext context) {
        final TestEnvironment environment;
        if (context.getRequiredTestInstance() instanceof AbstractSeleniumTest) {
            AbstractSeleniumTest testInstance = (AbstractSeleniumTest) context.getRequiredTestInstance();
            environment = testInstance.getEnvironment();
        } else {
            throw new IllegalStateException("The test instance must be of type AbstractSeleniumTest to use the ScreenShotRule.");
        }
        final File screenshotFolder = environment.getScreenshotFolder();
        if (screenshotFolder != null) {
            environment.getWindowManager().forEachOpenedWindow(window -> {
                try {
                    window.switchToWindow();
                    final String filename = UUID.randomUUID().toString();
                    WebDriver driver = window.getWebDriver();
                    if (RemoteWebDriver.class.equals(driver.getClass())) {
                        driver = new Augmenter().augment(driver);
                    }
                    InputStream source = getScreenshotNotSupportedImage();
                    if (driver instanceof TakesScreenshot) {
                        source = new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
                    }
                    try {
                        final File destinationDir = new File(screenshotFolder, context.getRequiredTestClass().getName());
                        destinationDir.mkdirs();
                        final File destination = new File(destinationDir, filename + SCREENSHOT_FILE_EXTENSION); //$NON-NLS-1$
                        final Path path = destination.toPath();
                        Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
                        // ATTENTION: Do not remove this line because it is needed for the JUnit Attachment Plugin!
                        System.out.println(String.format(ATTACHMENT_FORMAT, destination.getCanonicalFile().toURI()));
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not capture screenshot for window: " + window.getWindowHandle(), e);
                }
            });
        }
    }
    
    private InputStream getScreenshotNotSupportedImage() {
        return AbstractSeleniumTest.class.getResourceAsStream(NOT_SUPPORTED_IMAGE);
    }
}
