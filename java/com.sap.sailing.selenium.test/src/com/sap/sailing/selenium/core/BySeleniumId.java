package com.sap.sailing.selenium.core;

import java.text.MessageFormat;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 * <p>A mechanism to locate elements by the value of the "selenium-id" attribute.</p>
 *  
 * @author
 *   D049941
 */
public class BySeleniumId extends By {
    /**
     * <p>XPath expression for the search of an element with a given selenium identifier. The search is performed in the
     *   specified context (web driver or web element) and returns the first matching element which is one or more
     *   levels deep in the current context.</p>
     */
    private static final MessageFormat BY_SELENIUM_ID = new MessageFormat(".//*[@selenium-id=\"{0}\"]"); //$NON-NLS-1$
    
    private String id;
    
    /**
     * <p>Creates a new location mechanism which searches for an element or a list of elements with the given
     *   identifier.</p>
     * 
     * @param id
     *   The value of the "selenium-id" attribute to search for.
     */
    public BySeleniumId(String id) {
        super();
        
        this.id = id;
    }
    

    // TODO [D049941]: Verify that the element closest to the context is returned in the case that several elements
    //                 are found!
    @Override
    public WebElement findElement(SearchContext context) {
        return super.findElement(context);
    }
    
    @Override
    public List<WebElement> findElements(SearchContext context) {
        return context.findElements(By.xpath(BY_SELENIUM_ID.format(new Object[] {this.id})));
    }
    
    @Override
    public String toString() {
      return "BySeleniumId: " + this.id;
    }

}
