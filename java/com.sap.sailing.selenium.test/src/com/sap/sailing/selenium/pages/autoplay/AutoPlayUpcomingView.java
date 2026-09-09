package com.sap.sailing.selenium.pages.autoplay;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.pages.PageArea;

public class AutoPlayUpcomingView extends PageArea {
    /**
     * The upcoming view alternates with the leaderboard view. If the leaderboard is shown we need to wait a little bit
     * longer to see the upcoming view again.
     */
    private static final int TIMEOUT_FOR_UPCOMING_VIEW = 300;

    public AutoPlayUpcomingView(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public String getText() {
        return new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_FOR_UPCOMING_VIEW)).until(new Function<WebDriver, String>() {
            @Override
            public String apply(WebDriver t) {
                try {
                    final List<WebElement> elements = findElementsBySeleniumId("upComingDataLabel");
                    final String result;
                    if (!elements.isEmpty()) {
                        final WebElement upComingDataLabel = elements.get(0);
                        final String currentRawText = upComingDataLabel.getText();
                        result = currentRawText == null || currentRawText.trim().isEmpty() ? null : currentRawText;
                    } else {
                        result = null;
                    }
                    return result;
                } catch (StaleElementReferenceException e) {
                    // AutoPlay might switch the views while calling getText
                    // While the element is out of the viewport you can't read the text
                    // In this case it needs to be checked in the next loop again
                    return null;
                }
            }
        });
    }

}
