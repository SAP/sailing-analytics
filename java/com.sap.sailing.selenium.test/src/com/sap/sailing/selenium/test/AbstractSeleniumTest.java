package com.sap.sailing.selenium.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatchman;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sailing.selenium.core.Managed;
import com.sap.sailing.selenium.core.SeleniumRunner;
import com.sap.sailing.selenium.core.TestEnvironment;
import com.sap.sailing.selenium.core.WindowManager;

/**
 * <p>Abstract base class for unit tests with Selenium. This class is already annotated as required to get executed
 *   with the Selenium runner. Furthermore there is a rule, which automatically captures a screenshot if a test fails,
 *   since it is helpful to see how a page looked in the case a test failed.</p>
 * 
 * @author
 *   D049941
 */
@RunWith(SeleniumRunner.class)
public abstract class AbstractSeleniumTest {
    private static final Logger logger = Logger.getLogger(AbstractSeleniumTest.class.getName());
    
    /**
     * <p>File extension for screenshots captured with a Selenium web driver.</p>
     */
    private static final String SCREENSHOT_FILE_EXTENSION = ".png"; //$NON-NLS-1$
    
    private static final String NOT_SUPPORTED_IMAGE = "/com/sap/sailing/selenium/resources/not-supported.png"; //$NON-NLS-1$
    
    private static final String ATTACHMENT_FORMAT = "[[ATTACHMENT|%s]]"; //$NON-NLS-1$
    
    private static final String CLEAR_STATE_URL = "sailingserver/test-support/clearState"; //$NON-NLS-1$
    
    private static final String LOGIN_URL = "security/api/restsecurity/login";
    
    private static final int CLEAR_STATE_SUCCESFUL_STATUS_CODE = 204;

    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    
    /**
     * <p></p>
     * 
     * @param contextRoot
     * 
     * @return
     *   <code>true</code> if the state was reseted successfully and <code>false</code> otherwise.
     */
    protected void clearState(String contextRoot) {
        logger.info("clearing server state");
        try {
            URL url = new URL(contextRoot + CLEAR_STATE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.connect();
            if (connection.getResponseCode() != CLEAR_STATE_SUCCESFUL_STATUS_CODE) {
                throw new RuntimeException(connection.getResponseMessage());
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
    
    @Before
    public void setUpAuthenticatedSession() {
        logger.info("Authenticating session...");
        Cookie sessionCookie = authenticate(getContextRoot());
        getWebDriver().get(getContextRoot() + "index.html"); // initialize web driver so setting a cookie for the local domain is possible
        getWebDriver().manage().addCookie(sessionCookie);
        logger.info("...obtained session cookie "+sessionCookie);
    }
    
    /**
     * Obtains a session cookie for a session authenticated using the default admin user.
     * 
     * @return the cookie that represents the authenticated session or <code>null</code> if the session
     * couldn't successfully be authenticated
     */
    protected Cookie authenticate(String contextRoot) {
        try {
            Cookie result = null;
            URL url = new URL(contextRoot + LOGIN_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.connect();
            connection.getOutputStream().write("username=admin&password=admin".getBytes());
            if (connection.getResponseCode() != 200) {
                throw new RuntimeException(connection.getResponseMessage());
            }
            List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.startsWith(SESSION_COOKIE_NAME + "=")) {
                        String cookieValue = cookie.substring(cookie.indexOf('=')+1, cookie.indexOf(';'));
                        result = new Cookie(SESSION_COOKIE_NAME, cookieValue, /* domain */ "localhost", /* path */ "/", MillisecondsTimePoint.now().plus(Duration.ONE_WEEK).asDate());
                    }
                }
            }
            return result;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
    
    // TODO: Change to TestWatcher if we support a higher version (4.11) of JUnit.
    //private class ScreenShotRule extends TestWatcher {
    //    ScreenShotFilenameGenerator generator;
    //    
    //    public ScreenShotRule(ScreenShotFilenameGenerator generator) {
    //        this.generator = generator;
    //    }
    //    
    //    protected void failed(Throwable error, Description description) {
    //        captureScreenshot(this.generator.getFilePath(description));
    //    }
    //}
    
    private class ScreenShotRule extends TestWatchman {
        @Override
        public void failed(Throwable cause, FrameworkMethod method) {
            try {
                captureScreenshot(UUID.randomUUID().toString());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
    
    /**
     * <p>Rule for capturing of a screenshot if a test fails.</p>
     */
    @Rule
    public final ScreenShotRule takeScreenshoot = new ScreenShotRule(/*generator*/);

    /**
     * <p>The test environment used for the execution of the the tests.</p>
     */
    @Managed
    protected TestEnvironment environment;
    
    /**
     * <p>Returns the context root (base URL) against the tests are executed. The context root identifies a web
     *   application and usually consists of a protocol definition, the host and a path.</p>
     * 
     * @return
     *   The context root against the tests are executed.
     */
    protected String getContextRoot() {
        return this.environment.getContextRoot();
    }
    
    /**
     * <p>Returns the web driver to use for the execution of the tests.</p>
     * 
     * @return
     *   The web driver to use for the execution of the tests
     */
    protected WebDriver getWebDriver() {
        return this.environment.getWebDriver();
    }
    
    /**
     * <p>Returns the window manager for the used web driver, which can be used to open new windows and switching
     *   between multiple windows.</p>
     * 
     * @return
     *   The window manager for the used web driver.
     */
    protected WindowManager getWindowManager() {
        return this.environment.getWindowManager();
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
    protected void captureScreenshot(String filename) {
        File screenshotFolder = this.environment.getScreenshotFolder();
        if (screenshotFolder != null) {
            WebDriver driver = getWebDriver();
            if (RemoteWebDriver.class.equals(driver.getClass())) {
                driver = new Augmenter().augment(driver);
            }
            InputStream source = getScreenshotNotSupportedImage();
            if (driver instanceof TakesScreenshot) {
                source = new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
            }
            try {
                File destinationDir = new File(screenshotFolder, getClass().getName());
                destinationDir.mkdirs();
                File destination = new File(destinationDir, filename + SCREENSHOT_FILE_EXTENSION); //$NON-NLS-1$
                Path path = destination.toPath();
                Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
                // ATTENTION: Do not remove this line because it is needed for the JUnit Attachment Plugin!
                System.out.println(String.format(ATTACHMENT_FORMAT, destination.getCanonicalFile().toURI()));
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }
    
    private InputStream getScreenshotNotSupportedImage() {
        return AbstractSeleniumTest.class.getResourceAsStream(NOT_SUPPORTED_IMAGE);
    }
}
