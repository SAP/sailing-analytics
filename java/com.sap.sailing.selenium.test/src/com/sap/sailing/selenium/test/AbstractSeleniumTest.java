package com.sap.sailing.selenium.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.TestEnvironment;
import com.sap.sailing.selenium.core.UseSeleniumExtensions;
import com.sap.sailing.selenium.core.WindowManager;
import com.sap.sailing.selenium.pages.PageObject;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * <p>Abstract base class for unit tests with Selenium. This class is already annotated as required to get executed
 *   with the Selenium runner. Furthermore there is a rule, which automatically captures a screenshot if a test fails,
 *   since it is helpful to see how a page looked in the case a test failed.</p>
 * 
 * @author
 *   D049941
 */
@UseSeleniumExtensions
@TestInstance(Lifecycle.PER_METHOD)
public abstract class AbstractSeleniumTest {
    private static final Logger logger = Logger.getLogger(AbstractSeleniumTest.class.getName());
    
    private static final String CLEAR_STATE_URL = "sailingserver/test-support/clearState"; //$NON-NLS-1$
    
    private static final String SWITCH_WHITELABEL_URL = "sailingserver/test-support/switch/whitelabel/"; //$NON-NLS-1$
    
    private static final String OBTAIN_ACCESS_TOKEN_URL = "security/api/restsecurity/access_token";
    
    private static final String CREATE_SESSION_URL = "sailingserver/test-support/createSession";
    
    private static final int CLEAR_STATE_SUCCESFUL_STATUS_CODE = 204;

    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    
    /**
     * <p></p>
     * 
     * @param contextRoot
     * @param headless if true, page inits are not required for some kind of tests (e.g. Rest-API)
     * 
     * @return
     *   <code>true</code> if the state was reseted successfully and <code>false</code> otherwise.
     */
    protected void clearState(String contextRoot, boolean headless) {
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
        setWhitelabel(false, contextRoot);
        if (!headless) {
            // To be able to access LocalStorage we need to load a page having the target origin
            getWebDriver().get(contextRoot);
            final String notificationTimeoutKey = "sse.notification.customTimeOutInSeconds";
            final String notificationTimeoutValue = Integer.toString(PageObject.DEFAULT_WAIT_TIMEOUT_SECONDS);
            if (getWebDriver() instanceof WebStorage) {
                // clear local storage
                final WebStorage webStorage = (WebStorage) getWebDriver();
                webStorage.getLocalStorage().clear();
                // extending the timeout of notifications to 100s to prevent timing failures
                webStorage.getLocalStorage().setItem(notificationTimeoutKey, notificationTimeoutValue);
            } else {
                // Fallback solution for IE
                ((JavascriptExecutor) getWebDriver()).executeScript("window.localStorage.clear();");
                ((JavascriptExecutor) getWebDriver()).executeScript("window.localStorage.setItem(\""
                        + notificationTimeoutKey + "\", \"" + notificationTimeoutValue + "\");");
            }
            try {
                // In IE 11 we sometimes see the problem that IE somehow automatically changes the zoom level to 75%.
                // Selenium tests with InternetExplorerDriver fail if the zoom level is not set to 100% due to the fact
                // that coordinates determined aren't correct.
                // With this we enforce a zoom level of 100% before running a test.
                // To make this work correctly you also need to set InternetExplorerDriver.IGNORE_ZOOM_SETTING to true
                // (this should be pre-configured in local-test-environment.xml when activating IE driver)
                getWebDriver().findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.CONTROL, "0"));
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * Resets the state for running tests in a clean state. In most cases of UI test also the state of the web page
     * needs to get reset. In some other cases (e.g. only Rest-API calls are involved in the test) an initialization of
     * the web page is not required. If so the method {@link #clearState(String, boolean)} can be called.
     */
    protected void clearState(String contextRoot) {
        clearState(contextRoot, false);
    }

    protected void setWhitelabel(boolean status, String contextRoot) {
        try {
            URL url = new URL(contextRoot + SWITCH_WHITELABEL_URL + Boolean.toString(status));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.connect();
            if (connection.getResponseCode() != 200) {
                throw new RuntimeException(connection.getResponseMessage());
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
//        clearBrowserCache(getWebDriver());
    }
    
    public void clearBrowserCache(WebDriver driver) {
        WebDriverWait webDriverWait = new WebDriverWait(driver, 1L);
        WebElement clearBrowsingButon = webDriverWait.until(d -> d.findElement(By.cssSelector("* /deep/ #clearBrowsingDataConfirm")));
        clearBrowsingButon.click();
    }
    
    protected boolean getWhitelabel(String contextRoot) {
        try {
            URL url = new URL(contextRoot + SWITCH_WHITELABEL_URL + "status");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != 200) {
                throw new RuntimeException(connection.getResponseMessage());
            }
            String response = (String)connection.getContent();
            return Boolean.valueOf(response);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
    
    protected void setUpAuthenticatedSession(WebDriver webDriver) {
        setUpAuthenticatedSession(webDriver, "admin", "admin");
    }
    
    protected void clearSession(WebDriver webDriver) {
        webDriver.manage().deleteCookieNamed("JSESSIONID");
    }
    
    protected void setUpAuthenticatedSession(WebDriver webDriver, String username, String password) {
        // To be able to set a cookie we need to load a page having the target origin
        webDriver.get(getContextRoot());
        logger.info("Authenticating session...");
        Cookie sessionCookie;
        try {
            sessionCookie = authenticate(getContextRoot(), username, password);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        webDriver.get(getContextRoot() + "index.html"); // initialize web driver so setting a cookie for the local domain is possible
        final Cookie cookieWithoutDomain = new Cookie(sessionCookie.getName(), sessionCookie.getValue(), null, sessionCookie.getPath(), sessionCookie.getExpiry(), sessionCookie.isSecure(), sessionCookie.isHttpOnly());
        webDriver.manage().addCookie(cookieWithoutDomain);
        logger.info("...obtained session cookie "+sessionCookie);
    }

    /**
     * If subclasses want to clear the state using {@link #clearState(String)}, they must re-define this method and
     * first invoke {@link #clearState(String)} before calling this implementation using <code>super.setUp()</code>.
     * This is important because {@link #clearState(String)} will also clear all session state that has been constructed
     * by {@link #setUpAuthenticatedSession()}.
     */
    @BeforeEach
    public void setUp() {
        setUpAuthenticatedSession(getWebDriver());
    }
    
    /**
     * Obtains a session cookie for a session authenticated using the default admin user.
     * 
     * @return the cookie that represents the authenticated session or <code>null</code> if the session
     * couldn't successfully be authenticated
     */
    protected Cookie authenticate(String contextRoot, String username, String password) throws JSONException {
        try {
            Cookie result = null;
            URL accessTokenUrl = new URL(contextRoot + OBTAIN_ACCESS_TOKEN_URL);
            HttpURLConnection connection = (HttpURLConnection) accessTokenUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.connect();
            connection.getOutputStream().write(("username=" + username + "&password=" + password).getBytes());
            final JSONObject jsonResponse = new JSONObject(new JSONTokener(new InputStreamReader((InputStream) connection.getContent())));
            final String accessToken = jsonResponse.getString("access_token");
            URL createSessionUrl = new URL(contextRoot + CREATE_SESSION_URL);
            HttpURLConnection adminConsoleConnection = (HttpURLConnection) createSessionUrl.openConnection();
            adminConsoleConnection.setRequestProperty("Authorization", "Bearer "+accessToken);
            adminConsoleConnection.setRequestMethod("GET");
            adminConsoleConnection.connect();
            List<String> cookies = adminConsoleConnection.getHeaderFields().get("Set-Cookie");
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
    
    /**
     * <p>The test environment used for the execution of the the tests.</p>
     */
    protected TestEnvironment environment;
    
    public void setEnvironment(TestEnvironment environment) {
        this.environment = environment;
    }

    public TestEnvironment getEnvironment() {
        return environment;
    }

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

}
