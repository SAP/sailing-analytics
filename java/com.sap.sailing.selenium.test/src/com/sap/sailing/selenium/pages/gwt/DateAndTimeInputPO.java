package com.sap.sailing.selenium.pages.gwt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public class DateAndTimeInputPO extends PageArea {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, y");
    private static final DateFormat TIME_FORMAT_MINUTES = new SimpleDateFormat("h:mm a");
    private static final DateFormat TIME_FORMAT_SECONDS = new SimpleDateFormat("h:mm:ss a");
    private static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat ISO_TIME_FORMAT_MINUTES = new SimpleDateFormat("HH:mm");
    private static final DateFormat ISO_TIME_FORMAT_SECONDS = new SimpleDateFormat("HH:mm:ss");

    @FindBy(how = BySeleniumId.class, using = "dateInput")
    private WebElement dateInput;

    @FindBy(how = BySeleniumId.class, using = "timeInput")
    private WebElement timeInput;

    private DateAndTimeInputPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    /**
     * Sets the provided {@link Date date value} to the underlying {@link WebElement}.
     * 
     * @param date
     *            the {@link Date date value} to set to the input field
     * @param enterSeconds
     *            <code>true</code> to enter seconds fragment as well, <code>false</code> otherwise
     * 
     * @see DateFormat#format(Date)
     * @see WebElement#sendKeys(CharSequence...)
     */
    public void setValue(Date date, boolean enterSeconds) {
        // TODO implement variant for browsers using datetime-local instead of two fields
        if ("time".equals(timeInput.getAttribute("type"))) {
            this.setValueNative(timeInput, date, enterSeconds ? ISO_TIME_FORMAT_SECONDS : ISO_TIME_FORMAT_MINUTES);
        } else {
            this.setValue(timeInput, date, enterSeconds ? TIME_FORMAT_SECONDS : TIME_FORMAT_MINUTES);
        }
        
        if ("date".equals(dateInput.getAttribute("type"))) {
            this.setValueNative(dateInput, date, ISO_DATE_FORMAT);
        } else {
            this.setValue(dateInput, date, DATE_FORMAT);
        }
    }

    private void setValue(WebElement input, Date date, DateFormat format) {
        final String value = format.format(date);
        input.clear();
        input.sendKeys(value);
        input.sendKeys("\t"); // ensure popup closing!
    }
    
    private void setValueNative(WebElement input, Date date, DateFormat format) {
        final String value = format.format(date);
        final JavascriptExecutor javascriptExecutor = ((JavascriptExecutor) driver);
        javascriptExecutor.executeScript("arguments[0].value = arguments[1];", input, value);
    }

    public static DateAndTimeInputPO create(WebDriver driver, WebElement element) {
        return new DateAndTimeInputPO(driver, element);
    }
}
