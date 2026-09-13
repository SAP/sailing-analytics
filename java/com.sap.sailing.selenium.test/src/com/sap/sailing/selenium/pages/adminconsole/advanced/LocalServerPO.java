package com.sap.sailing.selenium.pages.adminconsole.advanced;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public class LocalServerPO extends PageArea {

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
            clickAndAwaitSelectionState(isSelfServiceServerCheckbox, selfService);
            awaitServerConfigurationUpdated();
        }
    }

    public void setPublicServer(final boolean publicServer) {
        if (publicServer != isPublicServerCheckbox.isSelected()) {
            clickAndAwaitSelectionState(isPublicServerCheckbox, publicServer);
            awaitServerConfigurationUpdated();
        }
    }

    public void setStandaloneServer(final boolean standalone) {
        if (standalone != isStandaloneServerCheckbox.isSelected()) {
            clickAndAwaitSelectionState(isStandaloneServerCheckbox, standalone);
            awaitServerConfigurationUpdated();
        }
    }

    /**
     * Clicks the given checkbox and blocks until the Selenium driver observes the checkbox in the
     * {@code expectedSelectionState}. In Selenium 4 {@link WebElement#click()} is a W3C command that may return before
     * the browser has run the element's value-change handler; without this barrier a subsequent poll of the
     * {@code updating} attribute in {@link #awaitServerConfigurationUpdated()} could read the pre-click state and return
     * immediately. Once the new selection state is visible, the value-change handler has fired and the server
     * configuration update RPC is in flight, so {@link #awaitServerConfigurationUpdated()} observes a consistent flag.
     */
    private void clickAndAwaitSelectionState(final WebElement checkbox, final boolean expectedSelectionState) {
        checkbox.click();
        createFluentWait(driver).until(ExpectedConditions.elementSelectionStateToBe(checkbox, expectedSelectionState));
    }

    private void awaitServerConfigurationUpdated() {
        while (isServerConfigurationUpdating()) {
        }
    }

    public boolean isServerConfigurationUpdating() {
        final String updating = isSelfServiceServerCheckbox.getAttribute("updating");
        return "true".equals(updating);
    }
}
