package com.sap.sailing.selenium.pages.gwt;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.By.ByXPath;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.CompositeAction;

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
        Action action = getSelectAction();
        action.perform();
    }
    
    public void deselect() {
        if (isSelected()) {
            Action action = getModifiedSelectAction();
            action.perform();
        }
    }
    
    public void appendToSelection() {
        if (!isSelected()) {
            Action action = getModifiedSelectAction();
            action.perform();
        }
    }
    
    protected Action getSelectAction() {
        final Actions actions = new Actions(this.driver);
        final WebElement elementForSelect = getElementForSelect();
        moveToUpperLeftCorner(actions, elementForSelect);
        actions.click();
        
        final CompositeAction compositeAction = new CompositeAction();
        compositeAction.addAction(new Action() {
            @Override
            public void perform() {
                scrollToView(elementForSelect);
            }
        });
        compositeAction.addAction(actions.build());
        return compositeAction;
    }
    
    /**
     * It seems that the JavaDoc of {@link Actions#moveToElement(WebElement, int, int)} is not consistent to the
     * WebDriver specification. The specification says the int params are relative to the center of the element, while
     * the JavaDoc says it's relative to the upper-left corner. GeckoDriver implements the specification. This method
     * ensures consistent behavior no matter if the implementation is a legacy one or one that conforms to the
     * specification.
     */
    private void moveToUpperLeftCorner(Actions actions, WebElement webElement) {
        actions.moveToElement(webElement);
        final Dimension size = webElement.getSize();
        actions.moveByOffset(-size.width / 2 + 1, -size.height / 2 + 1);
    }
    
    /**
     * <p>Returns the element on which to click to select or deselect the data entry in the table. The default
     *   implementation returns the first column or the row if there are no columns. It may be necessary to overwrite
     *   this method for the case the first column contains a widget that should not be clicked accidentally.</p>
     * 
     * @return
     *   The element on which to click to select or deselect the data entry in the table.
     */
    private WebElement getElementForSelect() {
        final int columnToUseForSelection;
        if (this.columns.size() > 1) {
            // could be a selection checkbox column in the first column, so use second column to be on the safe side:
            columnToUseForSelection = 1;
        } else {
            columnToUseForSelection = 0;
        }
        return (this.columns.isEmpty() ? getWebElement() : this.columns.get(columnToUseForSelection));
    }
    
    protected Action getModifiedSelectAction() {
        Actions actions = new Actions(this.driver);
        final WebElement elementToSelect;
        final boolean controlClick;
        if (table.getColumnHeaders().get(0).equals("\u2713")) {
            // it's a checkbox column
            elementToSelect = this.columns.isEmpty() ? getWebElement() : this.columns.get(0);
            controlClick = false;
        } else {
            elementToSelect = getElementForSelect();
            controlClick = true;
        }
        if (controlClick) {
            actions.keyDown(Keys.CONTROL);
        }
        moveToUpperLeftCorner(actions, elementToSelect);
        actions.moveToElement(elementToSelect, 1, 1);
        actions.click();
        if (controlClick) {
            actions.keyUp(Keys.CONTROL);
        }
        final CompositeAction compositeAction = new CompositeAction();
        compositeAction.addAction(new Action() {
            @Override
            public void perform() {
                scrollToView(elementToSelect);
            }
        });
        compositeAction.addAction(actions.build());
        return compositeAction;
    }
    
    protected CompositeAction getModifiedCompositeActionAction() {
        return new CompositeAction() {
            @Override
            public void perform() {
                Actions actions = new Actions(driver);
                actions.keyDown(Keys.CONTROL);
                actions.perform();
                
                super.perform();
                
                actions = new Actions(driver);
                actions.keyUp(Keys.CONTROL);
                actions.perform();
            }
        };
    }

    public void clickActionImage(String actionName) {
        this.context.findElement(By.xpath(".//td/div/div[@name=\"" + actionName + "\"]/img")).click();
    }

    public boolean hasActionImage(final String actionName) {
        return !this.context.findElements(By.xpath(".//td/div/div[@name=\"" + actionName + "\"]/img")).isEmpty();
    }

    public WebElement clickActionImage(String actionName, String targetSeleniumId) {
        clickActionImage(actionName);
        return findElementBySeleniumId(driver, targetSeleniumId);
    }
}
