package com.sap.sailing.selenium.pages.gwt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.PageObject;

/**
 * {@link PageObject} implementation for better date time boxes.
 */
public class BetterDateTimeBoxPO extends PageArea {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat timeFormat = new SimpleDateFormat("hh:mm:ss");

    @FindBy(how = BySeleniumId.class, using = "datebox")
    private WebElement datebox;

    @FindBy(how = BySeleniumId.class, using = "timebox")
    private WebElement timebox;

    /**
     * @see TextBoxPO#TextBoxPO(WebDriver, WebElement)
     */
    protected BetterDateTimeBoxPO(WebDriver driver, WebElement element, DateFormat timeFormat) {
        super(driver, element);
    }

    /**
     * Sets the underlying {@link WebElement}s text by formatting the given {@link Date} using the internal date format
     * (<code>dd/MM/yyyy hh:mm</code> or <code>dd/MM/yyyy hh:mm:ss</code>).
     * 
     * @param date
     *            the {@link Date} object to set
     * 
     * @see DateFormat#format(Date)
     * @see #setText(String)
     */
    public void setDate(Date date) {
        String datein = dateFormat.format(date);
        datebox.clear();
        datebox.sendKeys(datein);
        String timein = timeFormat.format(date);
        timebox.clear();
        timebox.sendKeys(timein);
    }

    public static BetterDateTimeBoxPO create(WebDriver driver, WebElement startTimeTimeBox, boolean b) {
        return new BetterDateTimeBoxPO(driver, startTimeTimeBox, null);
    }

    public static BetterDateTimeBoxPO create(WebDriver driver, WebElement startDateTimeBox) {
        return create(driver, startDateTimeBox, true);
    }
}
