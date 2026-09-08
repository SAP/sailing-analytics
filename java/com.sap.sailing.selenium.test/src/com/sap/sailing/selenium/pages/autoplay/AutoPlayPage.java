package com.sap.sailing.selenium.pages.autoplay;

import java.io.UnsupportedEncodingException;
import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Function;
import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.HostPage;

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
    
    @FindBy(how = BySeleniumId.class, using = "IdleNextUpView")
    private WebElement idleNextUpView;
    
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
        driver.get(root + "gwt/AutoPlay.html?" + getGWTCodeServerAndLocale()); //$NON-NLS-1$
        
        return new AutoPlayPage(driver);
    }
    
    private AutoPlayPage(WebDriver driver) {
        super(driver);
    }
    
    public AutoPlayConfiguration getAutoPlayConfiguration() {
        return new AutoPlayConfiguration(this.driver, autoPlayStartView);
    }
    
    public AutoPlayConfiguration getAutoPlayClassic() {
        return new AutoPlayConfiguration(this.driver, autoPlayStartView);
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

    public AutoPlayLeaderboardView goToAutoPlayClassicUrl(WebDriver driver, String url) {
        driver.get(url); //$NON-NLS-1$
        final WebElement leaderboardViewElement = new WebDriverWait(driver, Duration.ofSeconds(30)).until(new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                final WebElement leaderboardViewElement = findElementOrNullBySeleniumId("LeaderboardView");
                if (leaderboardViewElement != null) {
                    if (isElementEntirelyVisible(leaderboardViewElement)) {
                        return leaderboardViewElement;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        });
        return new AutoPlayLeaderboardView(driver, leaderboardViewElement);
    }

    public AutoPlayUpcomingView goToAutoPlaySixtyInchUrl(WebDriver webDriver, String url) {
        driver.get(url); //$NON-NLS-1$
        return new AutoPlayUpcomingView(driver, idleNextUpView);
    }
}
