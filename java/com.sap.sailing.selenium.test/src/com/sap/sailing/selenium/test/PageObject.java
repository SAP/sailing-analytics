package com.sap.sailing.selenium.test;

import java.util.List;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.PageFactory;

import org.openqa.selenium.support.ui.FluentWait;

import com.sap.sailing.selenium.core.AjaxCallsComplete;
import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.core.FindBys;
import com.sap.sailing.selenium.core.SeleniumElementLocatorFactory;

/**
 * <p>Within a web app's UI there are areas that tests interact with. A page object simply models these as objects
 *   within the test code and abstracts from the actual structure (DOM) of a website. This reduces the amount of
 *   duplicated code and means that if the UI changes, the fix need only be applied in one place.</p>
 * 
 * <p>Page objects can be thought of as facing in two directions simultaneously. Facing towards the developer of a test,
 *   they represent the services offered by a particular page. Facing away from the developer, they should be the only
 *   thing that has a deep knowledge of the structure of a page (or part of a page). It's simplest to think of the
 *   methods on a page object as offering the "services" that a page offers rather than exposing the details and
 *   mechanics of the page.</p>
 * 
 * <p>Because the developer of a test should think about the services that they're interacting with rather than the
 *   implementation, page objects should seldom expose the underlying web driver or elements. To facilitate this,
 *   methods on a page object should return other page objects.</p>
 * 
 * <p>To make the development of page objects as simple and easy as possible, we also support a factory for this pattern
 *   which helps to remove some boiler-plate code from the page objects by using annotations. The factory uses the
 *   annotations on fields to lazily locate and wait for an element or an element list to appear, by polling the UI on
 *   a regular basis, and initializes the field with the element or the list of elements.</p>
 * 
 * @author
 *   D049941
 */
public class PageObject {
    /**
     * </p>The default timeout of 60 seconds for the initialization of the page object.</p>
     */
    protected static final int DEFAULT_TIMEOUT = 60;
    
    /**
     * <p>The web driver to use.</p>
     */
    protected final WebDriver driver;
    
    /**
     * <p>The context for the locating of elements.</p>
     */
    protected final SearchContext context;
    
    /**
     * <p>Creates a new page object with the given web driver. This constructor is equivalent to the two argument
     *   constructor whereby the web driver is also used as the context for the page object.</p>
     * 
     * @param driver
     *   The web driver which also acts as the context to use.
     */
    public PageObject(WebDriver driver) {
        this(driver, driver);
    }
    
    /**
     * <p>Creates a new page object with the given web driver and context.</p>
     * 
     * @param driver
     *   The web driver to use.
     * @param context
     *   The context to use.
     */
    public PageObject(WebDriver driver, SearchContext context) {
        this.driver = driver;
        this.context = context;
        
        verify();
        initElements();
    }
    
    /**
     * <p>Verifies the page object. This method can be overwritten to check that the browser is on the correct page or
     *   that the provided context represents the right element. The default implementation does nothing.</p>
     */
    protected void verify() {
    }
    
    /**
     * <p>Initialize the page object. The default implementation use a factory to lazily initialize the elements
     *   (annotated fields) of the page object using the timeout duration returned by {@link #getTimeOut()}. To get a
     *   field lazily initialized you have to annotate the field either with {@link FindBy} or with {@link FindBys}.
     *   If the element never changes (that is, that the same instance in the DOM can always be used) it is also
     *   possible to use a cache for the lookup by using the annotation {@code CacheLookup} in addition.</p>
     * 
     * <p>A simple example for the implementation of a page object can look as follows:</p>
     * 
     * <pre>
     *   public class MySearchPage extends PageObject {
     *       // The element is looked up using the id attribute and it is
     *       // never look up again once it has been used the first time
     *       &#64;FindBy(how = ById.class, using = "my-searchfield")
     *       &#64;CacheLookup
     *       private WebElement searchField;
     *       
     *       // The element is looked up using the tag name and it is
     *       // look up every time it is used
     *       &#64;FindBy(how = ByTagName.class, using = "input")
     *       private WebElement submitButton;
     *       
     *       public MySearchPage(WebDriver driver) {
     *           super(driver);
     *       }
     *       
     *       public void enterSearchText(String text) {
     *          this.searchField.clear();
     *          this.searchField.sendKeys(text);
     *       }
     *       
     *       public ResultPage performSearch() {
     *          this.submitButton.click();
     *       }
     *   }
     * </pre>
     * 
     * <p>If the implementation is not sufficient for your case, you can overwrite this method with or without calling
     *   the default implementation.</p>
     */
    protected void initElements() {
        PageFactory.initElements(new SeleniumElementLocatorFactory(this.context, getTimeOut()), this);
    }
    
    /**
     * <p>Returns the timeout duration for the initialization of the page object.</p>
     * 
     * @return
     *   The timeout duration for the initialization of the page object.
     */
    protected int getTimeOut() {
        return DEFAULT_TIMEOUT;
    }
    
    /**
     * <p>Finds and returns the first element with the specified selenium id in the search context of the page object.
     *   If multiple elements exists, the element closest to the context is returned.</p>
     * 
     * @param id
     *   The selenium id of the element.
     * @return
     *   The first matching element in the search context of the page object.
     * @see #findElementBySeleniumId(SearchContext context, String id)
     */
    protected WebElement findElementBySeleniumId(String id) {
        return findElementBySeleniumId(this.context, id);
    }
    
    /**
     * <p>Finds and returns the first element with the specified selenium id in the given search context. If multiple
     *   elements exists, the element closest to the context is returned.</p>
     * 
     * @param context
     *   The search context to use for the search.
     * @param id
     *   The selenium id of the element.
     * @return
     *   The first matching element in the given context.
     */
    protected WebElement findElementBySeleniumId(SearchContext context, String id) {
        return context.findElement(new BySeleniumId(id));
    }
    
    /**
     * <p>Finds and returns all elements within the specified selenium id in the search context of the page object.</p>
     * 
     * @param id
     *   The selenium id of the elements.
     * @return
     *   A list of all matching elements in the search context of the page object, or an empty list if nothing matches.
     * @see #findElementsBySeleniumId(SearchContext context, String id)
     */
    protected List<WebElement> findElementsBySeleniumId(String id) {
        return findElementsBySeleniumId(this.context, id);
    }
    
    /**
     * <p>Finds and returns all elements within the specified selenium id in the given search context.</p>
     * 
     * @param context
     *   The search context to use for the search.
     * @param id
     *   The selenium id of the elements.
     * @return
     *   A list of all matching elements in the given context, or an empty list if nothing matches.
     */
    protected List<WebElement> findElementsBySeleniumId(SearchContext context, String id) {
        return context.findElements(new BySeleniumId(id));
    }
    
    /**
     * <p>Waits until all pending Ajax request in the global category have been complete using a timeout of 30 seconds
     *   and a polling interval of 5 seconds. In reality, the interval may be greater as the cost of actually evaluating
     *   the condition is not factored in.</p>
     * 
     * @throws org.openqa.selenium.TimeoutException
     *   if the timeout of 30 seconds expires.
     * @see #waitForAjaxRequests(int timeout, int polling)
     */
    protected void waitForAjaxRequests() {
        waitForAjaxRequests(30, 5);
    }
    
    /**
     * <p>Waits until all pending Ajax request in the specified category have been complete using a timeout of 30
     *   seconds and a polling interval of 5 seconds. In reality, the interval may be greater as the cost of actually
     *   evaluating the condition is not factored in.</p>
     * 
     * @param category
     *   The category of Ajax requests to wait for.
     * @throws org.openqa.selenium.TimeoutException
     *   if the timeout of 30 seconds expires.
     * @see #waitForAjaxRequests(int timeout, int polling)
     */
    protected void waitForAjaxRequests(String category) {
        waitForAjaxRequests(category, 30, 5);
    }
    
    /**
     * <p>Waits until all pending Ajax request in the global category have been complete using the given timeout
     *   and polling interval. In reality, the interval may be greater as the cost of actually evaluating the condition
     *   is not factored in.</p>
     * 
     * @param timeout
     *   The timeout duration for the waiting.
     * @param polling
     *   The interval in which the check should be performed.
     * @throws org.openqa.selenium.TimeoutException
     *   if the timeout expires.
     */
    protected void waitForAjaxRequests(int timeout, int polling) {
        waitForAjaxRequests(AjaxCallsComplete.CATEGORY_GLOBAL, timeout, polling);
    }
    
    /**
     * <p>Waits until all pending Ajax request in the specified category have been complete using the given timeout
     *   and polling interval. In reality, the interval may be greater as the cost of actually evaluating the condition
     *   is not factored in.</p>
     * 
     * @param category
     *   The category of Ajax requests to wait for.
     * @param timeout
     *   The timeout duration for the waiting.
     * @param polling
     *   The interval in which the check should be performed.
     * @throws org.openqa.selenium.TimeoutException
     *   if the timeout expires.
     */
    protected void waitForAjaxRequests(String category, int timeout, int polling) {
        FluentWait<WebDriver> wait = new FluentWait<>(this.driver);
        wait.withTimeout(timeout, TimeUnit.SECONDS);
        wait.pollingEvery(polling, TimeUnit.SECONDS);
        
        wait.until(new AjaxCallsComplete(category));
    }
}
