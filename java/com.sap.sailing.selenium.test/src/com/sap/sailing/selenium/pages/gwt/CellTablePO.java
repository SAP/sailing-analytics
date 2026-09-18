package com.sap.sailing.selenium.pages.gwt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.common.CSSConstants;

/**
 * <p>Abstract base implementation for a page objects representing a GWT CellTable. The abstract implementation provides
 *   common functionality like sorting and the selection of entries.</p>
 * 
 * <p>For a basic CellTable you can use the concrete implementation GenericCellTable, which use a factory to create the
 *   data entries. If you have a customized CellTable with extended functionality you should extend this class to add
 *   these functionality as a service of the page object.</p>
 * 
 * @author
 *   Riccardo Nimser (D049941)
 * @param <T>
 *   The type of the data entries the table contains.
 */
public abstract class CellTablePO<T extends DataEntryPO> extends PageArea {
    public enum SortingOrder {
        Ascending,
        Descending,
        None;
    }
    
    protected static final String TABLE_TAG_NAME = "table"; //$NON-NLS-1$
    
    protected static final String HEAD_TAG_NAME = "thead"; //$NON-NLS-1$
    
    protected static final String BODY_TAG_NAME = "tbody"; //$NON-NLS-1$
    
    protected static final String FOOT_TAG_NAME = "tfoot"; //$NON-NLS-1$
    
    protected static final String TABLE_HEADER_XPATH = "./thead/tr/th";
    
    protected static final String GWT_HEADER_XPATH = "./thead/tr//*[@__gwt_header]";
    
    private static final String SORTING_INDICATOR_XPATH = ".//div/div/img";
    
    private static final String LOADING_ANIMATION_XPATH = "./td/div/div/div/img";

    private static final String LOADING_ANIMATION_IMAGE = "data:image/gif;base64," +               //$NON-NLS-1$
            "R0lGODlhKwALAPEAAP///0tKSqampktKSiH/C05FVFNDQVBFMi4wAwEAAAAh/hpDcmVhdGVkIHdpdGggYWpheGx" +  //$NON-NLS-1$
            "vYWQuaW5mbwAh+QQJCgAAACwAAAAAKwALAAACMoSOCMuW2diD88UKG95W88uF4DaGWFmhZid93pq+pwxnLUnXh8" +  //$NON-NLS-1$
            "ou+sSz+T64oCAyTBUAACH5BAkKAAAALAAAAAArAAsAAAI9xI4IyyAPYWOxmoTHrHzzmGHe94xkmJifyqFKQ0pwL" +  //$NON-NLS-1$
            "LgHa82xrekkDrIBZRQab1jyfY7KTtPimixiUsevAAAh+QQJCgAAACwAAAAAKwALAAACPYSOCMswD2FjqZpqW9xv" +  //$NON-NLS-1$
            "4g8KE7d54XmMpNSgqLoOpgvC60xjNonnyc7p+VKamKw1zDCMR8rp8pksYlKorgAAIfkECQoAAAAsAAAAACsACwA" +  //$NON-NLS-1$
            "AAkCEjgjLltnYmJS6Bxt+sfq5ZUyoNJ9HHlEqdCfFrqn7DrE2m7Wdj/2y45FkQ13t5itKdshFExC8YCLOEBX6Ah" +  //$NON-NLS-1$
            "QAADsAAAAAAAAAAAA=";                                                                     //$NON-NLS-1$
    
    private static final String ASCENDING_IMAGE = "url(\"data:image/png;base64," +                       //$NON-NLS-1$
            "iVBORw0KGgoAAAANSUhEUgAAAAsAAAAHCAYAAADebrddAAAAiklEQVR42mNgwALyKrumFRf3iDAQAvmVXVVAxf/" +  //$NON-NLS-1$
            "zKjq341WYV95hk1fZ+R+MK8C4HqtCkLW5FZ2PQYpyK6AaKjv/5VV1OmIozq3s3AFR0AXFUNMrO5/lV7WKI6yv6m" +  //$NON-NLS-1$
            "xCksSGDyTU13Mw5JV2qeaWd54FWn0BRAMlLgPZl/NAuBKMz+dWdF0H2hwCAPwcZIjfOFLHAAAAAElFTkSuQmCC\")"; //$NON-NLS-1$
    
    private static final String DESCENDING_IMAGE = "url(\"data:image/png;base64," +                      //$NON-NLS-1$
            "iVBORw0KGgoAAAANSUhEUgAAAAsAAAAHCAYAAADebrddAAAAiklEQVR42mPIrewMya3oup5X2XkeiC/nVXRezgV" +  //$NON-NLS-1$
            "iEDu3vPMskH0BROeVdqkyJNTXcwAlDgDxfwxcAaWrOpsYYCC/qlUcKPgMLlnZBcWd/4E272BAB0DdjkDJf2AFFR" +  //$NON-NLS-1$
            "BTgfTj4uIeEQZsAKigHmE6EJd32DDgA0DF20FOyK/sqmIgBEDWAhVPwyYHAJAqZIiNwsHKAAAAAElFTkSuQmCC\")"; //$NON-NLS-1$
    public static final String ARIA_ROLE_SELECTED = "aria-selected";
    
//    @FindBy(how = ByTagName.class, using = HEAD_TAG_NAME)
//    private WebElement head;
//    
//    @FindBy(how = ByTagName.class, using = FOOT_TAG_NAME)
//    private WebElement foot;
//    
//    @FindBy(how = ByTagName.class, using = BODY_TAG_NAME)
//    private List<WebElement> bodies;
    
    /**
     * <p></p>
     * 
     * @param driver
     *   
     * @param element
     *   
     */
    public CellTablePO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    /**
     * <p>Returns the number of columns of the table. Note that the result can change if the table is updated in some
     *   way.</p>
     * 
     * @return
     *   The number of columns of the table.
     */
    public int numberOfColumns() {
        return this.context.findElements(By.xpath(TABLE_HEADER_XPATH)).size();
    }
    
    public int getColumnIndex(String header) {
        return getColumnHeaders().indexOf(header);
    }
    
    /**
     * <p>Returns a string representation for all columns.</p>
     * 
     * @return
     *   A string representation for all columns.
     */
    public List<String> getColumnHeaders() {
        final List<String> headers = new ArrayList<>();
        for (WebElement header : findGWTHeaders()) {
            headers.add(header.getText());
        }
        return headers;
    }
    
    public boolean containsColumnHeader(String headerToCheck) {
        for (String header : getColumnHeaders()) {
            if(header.contains(headerToCheck)) {
                return true;
            }
        }
        return false;
    }
    
    public void sortByColumn(int column, SortingOrder order) {
        final List<WebElement> headers = this.context.findElements(By.xpath(TABLE_HEADER_XPATH));
        final WebElement header = headers.get(column);
        final String role = header.getAttribute(GWTConstants.ROLE_ATTRIBUTE_NAME);
        if (header.isDisplayed() && GWTConstants.ROLE_BUTTON.equals(role)) {
            if (order == SortingOrder.None && getSortingOrder(column) != SortingOrder.None) {
                throw new IllegalArgumentException("Can't change sorting order back to NONE");
            }
            while (order != getSortingOrder(column)) {
                header.click();
            }
        }
    }
    
    public void sortByColumn(String header, SortingOrder order) {
        sortByColumn(getColumnIndex(header), order);
    }
    
    public SortingOrder getSortingOrder(int column) {
        final List<WebElement> headers = this.context.findElements(By.xpath(TABLE_HEADER_XPATH));
        final WebElement header = headers.get(column);
        // NOTE: We use "findElements", since "findElement" throws an exception if there is no image (not sorted)
        final List<WebElement> images = header.findElements(By.xpath(SORTING_INDICATOR_XPATH));
        final SortingOrder result;
        if (images.size() == 0) {
            result = SortingOrder.None;
        } else {
            final String image = images.get(0).getCssValue(CSSConstants.CSS_BACKGROUND_IMAGE);
            if (ASCENDING_IMAGE.equals(image)) {
                result = SortingOrder.Ascending;
            } else if (DESCENDING_IMAGE.equals(image)) {
                result = SortingOrder.Descending;
            } else {
                throw new RuntimeException("Unkown sorting indicator");
            }
        }
        return result;
    }
    
    public SortingOrder getSortingOrder(String header) {
        return getSortingOrder(getColumnIndex(header));
    }
    
    public List<T> getEntries() {
        final int maxAttempts = 5;
        List<T> result = null;
        for (int attempt = 0; result == null && attempt < maxAttempts; attempt++) {
            try {
                final List<T> entries = new ArrayList<>();
                for (final WebElement row : getRows()) {
                    entries.add(createDataEntry(row));
                }
                result = entries;
            } catch (final StaleElementReferenceException e) {
                if (attempt == maxAttempts - 1) {
                    throw e;
                }
            }
        }
        return result;
    }
    
    public T getEntry(Object identifier) {
        for (T entry : getEntries()) {
            if (Objects.equals(identifier, entry.getIdentifier())) {
                return entry;
            }
        }
        return null;
    }
    
    public List<T> getEntries(List<?> identifiers) {
        final List<T> entries = new ArrayList<>();
        for (T entry : getEntries()) {
            if (identifiers.contains(entry.getIdentifier())) {
                entries.add(entry);
            }
        }
        return entries;
    }
    
    /**
     * <p>Selects the specified entry.</p>
     * 
     * <p>Note: If the entry is not contained in the table it will not be selected.</p>
     * 
     * @param entry
     *   The entry to select.
     */
    public void selectEntry(T entry) {
        if (this.getWebElement().equals(entry.table.getWebElement())) {
            entry.select();
        }
    }
    
    /**
     * <p>Selects the specified entries.</p>
     * 
     * <p>Note: If an entry is not contained in the table it will not be selected.</p>
     * @param entryFetcher
     *   The provider to use to obtain the table entries to select
     */
    public void selectEntries(Supplier<Stream<T>> entryFetcher) {
        // First deselect all selected entries
        for (T entry : getSelectedEntries()) {
            entry.deselect();
        }
        // Select all specified entries
        entryFetcher.get().forEach(entry -> {
            if (this.getWebElement().equals(entry.table.getWebElement())) {
                entry.appendToSelection();
            }
        });
    }
    
    /**
     * <p>Selects entries based on the given Predicate.</p>
     * 
     * <p>Note: If an entry is not contained in the table it will not be selected.</p>
     */
    public void selectEntries(Predicate<T> toSelectPredicate, BooleanSupplier canAbort) {
        for (T entry : getEntries()) {
            if(toSelectPredicate.test(entry)) {
                entry.appendToSelection();
            } else {
                entry.deselect();
            }
            if (canAbort.getAsBoolean()) {
                break;
            }
        }
    }
    
    /**
     * <p>Selects entries based on the given Predicate.</p>
     * 
     * <p>Note: If an entry is not contained in the table it will not be selected.</p>
     */
    public void selectAllEntries() {
        selectEntries(e -> true, Boolean.FALSE::booleanValue);
    }
    
    /**
     * <p>Returns all currently selected entries. If no entry is selected, an empty list is returned.</p>
     * 
     * @return
     *   All currently selected entries.
     */
    public List<T> getSelectedEntries() {
        final List<T> entries = getEntries();
        final Iterator<T> iterator = entries.iterator();
        while (iterator.hasNext()) {
            T entry = iterator.next();
            if (!entry.isSelected()) {
                iterator.remove();
            }
        }
        return entries;
    }
    
    protected WebDriver getWebDriver() {
        return this.driver;
    }
    
    @Override
    protected void verify() {
        final WebElement element = getWebElement();
        final String tagName = element.getTagName();
        if (!TABLE_TAG_NAME.equalsIgnoreCase(tagName) /* || !CSSHelper.hasCSSClass(element, CELL_TABLE_CSS_CLASS) */) {
            throw new IllegalArgumentException("WebElement does not represent a CellTable");
        }
    }

    protected List<WebElement> findGWTHeaders() {
        return this.context.findElements(By.xpath("./thead/tr//*[@__gwt_header]"));
    }
    
    protected List<WebElement> getRows() {
        final List<WebElement> rows = this.context.findElements(By.xpath("./tbody/tr"));
        final Iterator<WebElement> iterator = rows.iterator();
        while (iterator.hasNext()) {
            final WebElement row = iterator.next();
            if (!row.isDisplayed() || isRowForLoadingIndicatorOrEmptyTableWidget(row)) {
                iterator.remove();
            }
        }
        return rows;
    }
    
    protected abstract T createDataEntry(WebElement element);
    
    /**
     * Checks if the row is the table row displayed for a loading indicator or an empty table.
     * @param row the row to check
     * @return
     */
    private boolean isRowForLoadingIndicatorOrEmptyTableWidget(WebElement row) {
        final List<WebElement> images = row.findElements(By.xpath(LOADING_ANIMATION_XPATH));
        final boolean result;
        if (images.size() != 1) {
            result = false;
        } else {
            final String image = images.get(0).getAttribute("src");
            result = LOADING_ANIMATION_IMAGE.equals(image);
        }
        return result;
    }
    
    public void waitForTableToShowData() {
        waitUntil(() -> {
            try {
                return !getRows().isEmpty();
            } catch (Exception e) {
                // This can fail while the table is just updating
                return false;
            }
        });
    }
}
