package com.sap.sailing.selenium.pages.home.event;

import java.text.MessageFormat;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.pages.HostPage;
import com.sap.sailing.selenium.pages.HostPageWithAuthentication;
import com.sap.sailing.selenium.pages.PageObject;

public class EventPage extends HostPageWithAuthentication {
    
    private static final String ID_HOME_WHATS_NEW_DIALOG = "HomeWhatsNewDialog";
    
    private static final MessageFormat REGATTA_LIST_ITEM_BY_REGATTA_NAME = new MessageFormat(
            ".//span[@selenium-id=\"RegattaNameSpan\" and text()=\"{0}\"]/ancestor::div[@selenium-id=\"RegattaListItemPanel\"]");
    private static final String EVENT_HEADER_IDENTIFIER = "EventHeaderPanel";
    
    /**
     * Navigates to the given event URL and provides the corresponding {@link PageObject}.
     * 
     * @param driver the {@link WebDriver} to use
     * @param url the desired destination URL
     * @return the {@link PageObject} for the event page
     */
    public static EventPage goToEventUrl(WebDriver driver, String url) {
        return HostPage.goToUrl(EventPage::new, driver, url);
    }

    protected EventPage(WebDriver driver) {
        super(driver);
    }
    
    @Override
    protected void initElements() {
        super.initElements();
        waitForElement(EVENT_HEADER_IDENTIFIER);
    }
    
    public EventHeaderPO getEventHeader() {
        return getPO(EventHeaderPO::new, EVENT_HEADER_IDENTIFIER);
    }
    
    public RegattaListItemPO getRegattaListItem(String regattaName) {
        String xpathExpression = REGATTA_LIST_ITEM_BY_REGATTA_NAME.format(new Object[]{regattaName});
        return new RegattaListItemPO(driver, driver.findElement(By.xpath(xpathExpression)));
    }
    
    public void checkWhatsNewDialog() {
        WebElement whatsNew = this.waitForElementBySeleniumId(driver, ID_HOME_WHATS_NEW_DIALOG, 1);
        if (whatsNew != null) {
            whatsNew.click();
        }
    }
    
    public MediaPO selectMedia() {
        WebElement eventTabBar = waitForElementBySeleniumId(driver, "eventTabBar", 1);
        eventTabBar.findElement(By.linkText("Media")).click();
        return waitForPO(MediaPO::new, "tabContentPanelUi");
    }

}
