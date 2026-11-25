package com.sap.sailing.selenium.pages.timeslider;

import java.time.Duration;
import java.util.function.Function;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageObject;

/**
 * {@link PageObject} representing the SAP Sailing home page.
 */
public class TimeSliderPO extends PageObject {
    
    @FindBy(how = BySeleniumId.class, using = "sliderKnob")
    private WebElement sliderKnob;
    
    @FindBy(how=BySeleniumId.class, using = "sliderBarMarker-E")
    private WebElement raceEndMarker;
    
    @FindBy(how=BySeleniumId.class, using = "sliderBarMarker-S")
    private WebElement startMarker;
    
    @FindBy(how=BySeleniumId.class, using = "sliderBarMarker-F")
    private WebElement finishMarker;
    
    private TimeSliderPO(WebDriver driver) {
        super(driver);
    }

    public TimeSliderPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    @Override
    protected void initElements() {
        super.initElements();
        WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(300));
        webDriverWait.until(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver t) {
                try {
                    return sliderKnob.isDisplayed() && startMarker.isDisplayed();
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }
    
    public String getSliderKnobTime() {
        return sliderKnob.getAttribute("title");
    }
    
    public String getEndMarkerTime() {
        return raceEndMarker.getAttribute("title");
    }
    
    public String getStartMarkerTime() {
        return startMarker.getAttribute("title");
    }
    
    public String getFinishMarkerTime() {
        return finishMarker.getAttribute("title");
    }
}
