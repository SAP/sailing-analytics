package com.sap.sailing.selenium.pages.adminconsole.connectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.resultimporturls.ResultImportUrlsAddDialogPO;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;
import com.sap.sailing.selenium.pages.gwt.ListBoxPO;

public class ResultImportUrlsPanelPO extends PageArea {

    @FindBy(how = BySeleniumId.class, using = "urlProviderListBox")
    private WebElement urlProviderListBox;

    @FindBy(how = BySeleniumId.class, using = "AddUrlButton")
    private WebElement addButton;

    @FindBy(how = BySeleniumId.class, using = "RemoveUrlButton")
    private WebElement removeButton;

    @FindBy(how = BySeleniumId.class, using = "WrappedTable")
    private WebElement urlTable;

    public ResultImportUrlsPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void selectUrlProviderByLabel(String label) {
        ListBoxPO.create(driver, urlProviderListBox).selectOptionByLabel(label);
    }

    public void addUrl(String url) {
        waitUntil(addButton::isEnabled);
        this.addButton.click();
        waitForElementBySeleniumId(driver, "ResultImportUrlAddDialog", DEFAULT_WAIT_TIMEOUT_SECONDS);
        WebElement dialog = findElementBySeleniumId(driver, "ResultImportUrlAddDialog");
        ResultImportUrlsAddDialogPO dialogPO = new ResultImportUrlsAddDialogPO(this.driver, dialog);
        dialogPO.setUrl(url);
        dialogPO.clickOkButtonOrThrow();
        waitForElementNotExistsBySeleniumId(driver, "ResultImportUrlAddDialog");
    }

    public void removeUrl(String url) {
        DataEntryPO testUrlEntry = findUrl(url);
        assertNotNull(testUrlEntry);
        testUrlEntry.select();
        pushRemoveButton();
        driver.switchTo().alert().accept();
        waitUntil(()-> findUrl(url) == null);
    }

    private void pushRemoveButton() {
        waitUntil(() -> removeButton.isEnabled());
        this.removeButton.click();
    }

    public void removeWithInlineButton(String url) {
        waitUntil(() -> findUrl(url) != null);
        DataEntryPO testUrlEntry = findUrl(url);
        testUrlEntry.clickActionImage("DELETE");
        driver.switchTo().alert().accept();
        waitUntil(() -> findUrl(url) == null);
    }

    private CellTablePO<DataEntryPO> getUserTable() {
        return new GenericCellTablePO<>(this.driver, this.urlTable, DataEntryPO.class);
    }

    public DataEntryPO findUrl(final String url) {
        final CellTablePO<DataEntryPO> table = getUserTable();
        for (DataEntryPO entry : table.getEntries()) {
            String name;
            try {
                name = entry.getColumnContent("URL");
            } catch (StaleElementReferenceException e) {
                // entry is not existing any more but must not break iteration
                name = null;
            }
            if (url.equals(name)) {
                return entry;
            }
        }
        return null;
    }
}
