package com.sap.sailing.selenium.pages.adminconsole.advanced;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public class LocalServerPO extends PageArea {

    /**
     * Ajax category the server-configuration-update RPC is marked with in {@code LocalServerManagementPanel}. The value
     * is a shared contract with the product code and must match its {@code CATEGORY_SERVER_CONFIGURATION_UPDATE}.
     */
    private static final String CATEGORY_SERVER_CONFIGURATION_UPDATE = "updateServerConfiguration";

    public LocalServerPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    @FindBy(how = BySeleniumId.class, using = "isSelfServiceServerCheckbox-input")
    private WebElement isSelfServiceServerCheckbox;

    @FindBy(how = BySeleniumId.class, using = "isPublicServerCheckbox-input")
    private WebElement isPublicServerCheckbox;

    @FindBy(how = BySeleniumId.class, using = "isStandaloneServerCheckbox-input")
    private WebElement isStandaloneServerCheckbox;
    
    @FindBy(how = BySeleniumId.class, using = "bearerTokenAbusePanel")
    private WebElement bearerTokenAbusePanel;
    
    @FindBy(how = BySeleniumId.class, using = "userCreationAbusePanel")
    private WebElement userCreationAbusePanel;

    public IpBlocklistPanelPO getBearerTokenAbusePO() {
        final WebElement wrappedTable = bearerTokenAbusePanel.findElement(new BySeleniumId("wrappedTable"));
        return new IpBlocklistPanelPO(this.driver, wrappedTable);
    }

    public IpBlocklistPanelPO getUserCreationAbusePO() {
        final WebElement wrappedTable = userCreationAbusePanel.findElement(new BySeleniumId("wrappedTable"));
        return new IpBlocklistPanelPO(this.driver, wrappedTable);
    }

    public void setSelfServiceServer(final boolean selfService) {
        if (selfService != isSelfServiceServerCheckbox.isSelected()) {
            clickAndAwaitServerConfigurationUpdated(isSelfServiceServerCheckbox, selfService);
        }
    }

    public void setPublicServer(final boolean publicServer) {
        if (publicServer != isPublicServerCheckbox.isSelected()) {
            clickAndAwaitServerConfigurationUpdated(isPublicServerCheckbox, publicServer);
        }
    }

    public void setStandaloneServer(final boolean standalone) {
        if (standalone != isStandaloneServerCheckbox.isSelected()) {
            clickAndAwaitServerConfigurationUpdated(isStandaloneServerCheckbox, standalone);
        }
    }

    /**
     * Clicks the given checkbox and blocks until the server-configuration-update RPC triggered by the checkbox's
     * value-change handler has completed. In Selenium 4 {@link WebElement#click()} is a W3C command that may return
     * before the browser has run the element's value-change handler, so we cannot observe an in-flight-RPC flag right
     * after the click: the RPC may not have been fired yet. Instead we baseline the monotonically increasing
     * finished-call count for {@link #CATEGORY_SERVER_CONFIGURATION_UPDATE} (via
     * {@link #getNumberOfFinishedAjaxRequests(String)}) <em>before</em> clicking, click, wait for the selection state
     * to become visible (a cheap sanity barrier confirming the click's default action ran), and finally wait until the
     * finished-call count has advanced by one. Because the count only ever increases, this is immune both to the
     * click-return gap and to a boolean flag being toggled back to {@code false} between two polls.
     */
    private void clickAndAwaitServerConfigurationUpdated(final WebElement checkbox,
            final boolean expectedSelectionState) {
        final int finishedCallsBaseline = getNumberOfFinishedAjaxRequests(CATEGORY_SERVER_CONFIGURATION_UPDATE);
        checkbox.click();
        createFluentWait(driver).until(ExpectedConditions.elementSelectionStateToBe(checkbox, expectedSelectionState));
        waitForAjaxRequestsExecuted(CATEGORY_SERVER_CONFIGURATION_UPDATE, finishedCallsBaseline + 1);
    }
}
