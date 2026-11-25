package com.sap.sailing.selenium.pages.gwt;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.By.ByXPath;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.AttributeHelper;

public class DataEntryPO extends CellTableRowPO {
    protected static final String CELL_TAG_NAME = "td"; //$NON-NLS-1$
    
    @FindBy(how = ByXPath.class, using = "./td")
    private List<WebElement> columns;
    
    public DataEntryPO(CellTablePO<?> table, WebElement element) {
        super(table, element);
    }
    
    protected DataEntryPO() {
        super();
    }
    
    public int getNumberOfColumns() {
        return this.columns.size();
    }
    
    public String getColumnContent(int column) {
        return this.columns.get(column).getText().trim();
    }
    
    public String getColumnContent(String name) {
        return getColumnContent(this.table.getColumnIndex(name));
    }
    
    /**
     * <p>Returns an identifier for data entry that uniquely identifies the entry in the containing table. The default
     *   implementation simply returns the underlying web element, but subclasses should overwrite this method and
     *   return a more meaningful value. An example would be the String value of one ore more columns.</p>
     * 
     * @return
     *   An identifier that uniquely identifies the entry in the containing table.
     */
    public Object getIdentifier() {
        // TODO: Test if its valid to return the context (the web element)
        //       To verify this take the key of an entry and ask the table for the entry with this key!
        return this.context;
    }
    
    public boolean isSelected() {
        return AttributeHelper.isEnabled((WebElement) this.context, CellTablePO.ARIA_ROLE_SELECTED);
    }
    
    public void select() {
        final Supplier<DataEntryPO> entrySupplier = getFreshEntrySupplier();
        clickWhenInteractableAtCorner(() -> entrySupplier.get().getElementForSelect(), () -> true,
                /* controlClick */ false);
        waitUntil(() -> entrySupplier.get().isSelected());
    }

    public void deselect() {
        setSelected(false);
    }

    public void appendToSelection() {
        setSelected(true);
    }

    protected void setSelected(final boolean selected) {
        final Supplier<DataEntryPO> entrySupplier = getFreshEntrySupplier();
        final boolean[] interactionCompleted = { false };
        waitUntil(() -> {
            if (!interactionCompleted[0] && entrySupplier.get().isSelected() != selected) {
                final List<String> headers = table.getColumnHeaders();
                final boolean checkboxColumn = (!headers.isEmpty() && "\u2713".equals(headers.get(/* index */ 0)))
                        || !table.getWebElement().findElements(By.xpath("./thead/tr/th[1]//input[@type='checkbox']"))
                                .isEmpty();
                if (checkboxColumn) {
                    performWhenInteractable(() -> entrySupplier.get().getWebElement().findElement(By.xpath("./td[1]/div")),
                            (final WebElement checkbox) -> {
                                if (entrySupplier.get().isSelected() != selected) {
                                    checkbox.click();
                                }
                            });
                } else {
                    clickWhenInteractableAtCorner(() -> entrySupplier.get().getElementForSelect(),
                            () -> entrySupplier.get().isSelected() != selected, /* controlClick */ true);
                }
                interactionCompleted[0] = true;
            }
            // A successful click is sent once; wait for GWT to publish its selection instead of toggling again.
            return entrySupplier.get().isSelected() == selected;
        });
    }

    private WebElement getElementForSelect() {
        // Leave checkbox columns alone for ordinary row selection and avoid embedded links with a corner click.
        final int columnToUseForSelection = this.columns.size() > 1 ? 1 : 0;
        return this.columns.isEmpty() ? getWebElement() : this.columns.get(columnToUseForSelection);
    }

    /**
     * Retains a row identity before an interaction can replace the DOM. Typed entries use their domain identifier;
     * generic entries use their complete cell text as a conservative fallback, never a mutable row index.
     * Lookup waits through transient removal; ambiguous replacements fail instead of selecting an arbitrary row.
     */
    protected Supplier<DataEntryPO> getFreshEntrySupplier() {
        final Object identifier = getIdentifier();
        final List<String> contents = identifier instanceof WebElement ? getColumnContents() : null;
        final DataEntryPO[] current = { this };
        return () -> new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_LOOKUP_TIMEOUT))
                .ignoring(StaleElementReferenceException.class).until((final WebDriver webDriver) -> {
            try {
                current[0].getWebElement().isEnabled();
            } catch (final StaleElementReferenceException e) {
                DataEntryPO replacement = null;
                for (final DataEntryPO candidate : table.getEntries()) {
                    final boolean matches = contents == null ? Objects.equals(identifier, candidate.getIdentifier())
                            : contents.equals(candidate.getColumnContents());
                    if (matches) {
                        if (replacement != null) {
                            throw new IllegalStateException("Ambiguous replacement for table row "
                                    + (contents == null ? identifier : contents));
                        }
                        replacement = candidate;
                    }
                }
                if (replacement == null) {
                    throw new NoSuchElementException("Table row not yet available: "
                            + (contents == null ? identifier : contents));
                }
                current[0] = replacement;
            }
            return current[0];
        });
    }

    private List<String> getColumnContents() {
        final List<String> contents = new ArrayList<>();
        for (final WebElement column : columns) {
            contents.add(column.getText().trim());
        }
        return contents;
    }

    public void clickActionImage(final String actionName) {
        final Supplier<DataEntryPO> entrySupplier = getFreshEntrySupplier();
        clickWhenInteractable(() -> entrySupplier.get().getWebElement()
                .findElement(By.xpath(".//div[@name=\"" + actionName + "\"]/img")));
    }

    public boolean hasActionImage(final String actionName) {
        return !this.context.findElements(By.xpath(".//td/div/div[@name=\"" + actionName + "\"]/img")).isEmpty();
    }

    public WebElement clickActionImage(String actionName, String targetSeleniumId) {
        clickActionImage(actionName);
        return findElementBySeleniumId(driver, targetSeleniumId);
    }
}
