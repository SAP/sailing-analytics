package com.sap.sailing.selenium.pages.autoplay;

import java.io.UnsupportedEncodingException;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.HostPage;
import com.sap.sailing.selenium.test.autoplay.AutoPlayConfiguration;

/**
 * <p>The page object representing the autoplay startpage.</p>
 * 
 * @author
 *   C5163874
 */
public class AutoPlayPage extends HostPage {
    private static final String PAGE_TITLE = "SAP Sailing Analytics AutoPlay"; //$NON-NLS-1$
    
    @FindBy(how = BySeleniumId.class, using = "AutoPlayStartView")
    private WebElement autoPlayStartView;
    
    /**
     * <p>Goes to the autoplay page and returns the representing page object.</p>
     * 
     * @param driver
     *   The web driver to use.
     * @param root
     *   The context root of the application.
     * @return
     *   The page object for the administration console.
     * @throws UnsupportedEncodingException 
     */
    public static AutoPlayPage goToPage(WebDriver driver, String root) {
        driver.get(root + "gwt/AutoPlay.html?" + getGWTCodeServer()); //$NON-NLS-1$
        
        return new AutoPlayPage(driver);
    }
    
    private AutoPlayPage(WebDriver driver) {
        super(driver);
    }
    
    public AutoPlayConfiguration getAutoPlayConfiguration() {
        return new AutoPlayConfiguration(this.driver, findElementBySeleniumId(context, "AutoPlayStartView"));
    }
    
    @Override
    protected void initElements() {
        super.initElements();
        
        // Wait for the initial loading of the data
        waitForAjaxRequestsExecuted("loadEventsData", 1);
    }
    
    /**
     * <p>Verifies that the current page is the autoplay page by checking the title of the page.</p>
     */
    @Override
    protected void verify() {
        if (!PAGE_TITLE.equals(this.driver.getTitle())) {
            throw new IllegalStateException("This is not the autoplay page: " + this.driver.getTitle()); //$NON-NLS-1$
        }
    }
}
