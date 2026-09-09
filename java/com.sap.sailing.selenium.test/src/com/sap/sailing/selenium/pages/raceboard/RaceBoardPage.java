package com.sap.sailing.selenium.pages.raceboard;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.BooleanSupplier;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.HostPageWithAuthentication;
import com.sap.sailing.selenium.pages.PageObject;
import com.sap.sailing.selenium.pages.leaderboard.LeaderboardSettingsDialogPO;
import com.sap.sailing.selenium.pages.timeslider.TimeSliderPO;

/**
 * {@link PageObject} representing the SAP Sailing home page.
 */
public class RaceBoardPage extends HostPageWithAuthentication {
    
    @FindBy(how = BySeleniumId.class, using = "moreOptionsButton")
    private WebElement moreOptionsButton;
    
    @FindBy(how = BySeleniumId.class, using = "raceMapSettingsButton")
    private WebElement raceMapSettingsButton;
    
    @FindBy(how=BySeleniumId.class, using = "dataByContainer")
    private WebElement dataByContainer;
    
    private boolean doneInit;
    
    /**
     * Navigates to the given home URL and provides the corresponding {@link PageObject}.
     * 
     * @param driver
     *            the {@link WebDriver} to use
     * @param url
     *            the desired destination URL
     * @return the {@link PageObject} for the home page
     * 
     *         Allows to use any url, because else some use cases for the RaceBoard are problematic to reach, for mor
     *         generic cases, a more specialized goTo Version is recommended, to reduce hardcoded strings
     */
    public static RaceBoardPage goToRaceboardUrl(WebDriver driver, String url) {
        String urlWithCodeServerAndLocale = url + "&" + getGWTCodeServerAndLocale();
        // workaround for hostpage removing query from url, required for evenntid in this case
        driver.get(urlWithCodeServerAndLocale);
        return new RaceBoardPage(driver);
    }
    
    private RaceBoardPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected void waitForAjaxRequests(int timeout, int polling) {
        // since the raceboard continually loads new data, we cannot wait for it to finish initially
        if (doneInit) {
            super.waitForAjaxRequests(timeout, polling);
        }
    }

    @Override
    protected void initElements() {
        super.initElements();
        doneInit = true;
        waitUntil(() -> moreOptionsButton.isDisplayed());
    }

    public MapSettingsPO openMapSettings() {
        waitUntil(() -> moreOptionsButton.isDisplayed());
        moreOptionsButton.click();
        clickWhenInteractable(() -> findElementBySeleniumId("raceMapSettingsButton"));
        waitUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                WebElement settingsDialog = null;
                try {
                    settingsDialog = findElementBySeleniumId("raceMapSettings");
                    boolean exists = settingsDialog != null;
                    // exists, and was actually rendered (to apply the values)
                    return (exists && settingsDialog.isDisplayed());
                } catch(Exception e) {
                }
                return false;
            }
        });
        return new MapSettingsPO(driver, findElementBySeleniumId("raceMapSettings"));
    }
    
    public LeaderboardSettingsDialogPO openLeaderboardSettingsDialog() {
        waitUntil(() -> moreOptionsButton.isDisplayed());
        moreOptionsButton.click();
        WebElement leaderboardSettingsButton = null;
        try {
            leaderboardSettingsButton = findElementBySeleniumId("leaderboardSettingsButton");
            if(leaderboardSettingsButton == null || !leaderboardSettingsButton.isDisplayed()) {
                leaderboardSettingsButton = null;
            }
        } catch(Exception e) {}
        if(leaderboardSettingsButton == null) {
            WebElement toggleButtonElement = findElementBySeleniumId("SplitLayoutPanelToggleButton-leaderboard");
            waitUntil(new BooleanSupplier() {

                @Override
                public boolean getAsBoolean() {
                    return toggleButtonElement.isDisplayed();
                }
            });
            findElementBySeleniumId("SplitLayoutPanelToggleButton-leaderboard").click();
            leaderboardSettingsButton = findElementBySeleniumId("leaderboardSettingsButton");
        }
        final WebElement finalLeaderboardSettingsButton = leaderboardSettingsButton;
        waitUntil(new BooleanSupplier() {

            @Override
            public boolean getAsBoolean() {
                return finalLeaderboardSettingsButton.isDisplayed();
            }
        });
        leaderboardSettingsButton.click();
        return new LeaderboardSettingsDialogPO(driver, findElementBySeleniumId("LeaderboardSettingsDialog"));
    }
    
    public boolean isRaceBoardLogoExisting() {
        return !driver.findElements(new BySeleniumId("raceBoardSapLogo")).isEmpty();
    }
    
    public WebElement getDataByContainer() {
        return dataByContainer;
    }
    
    public static RaceBoardPage goToRaceboardUrl(WebDriver webDriver,String context, String leaderboardName, String regattaName,
            String raceName, boolean whitelabel) throws UnsupportedEncodingException {
        return goToRaceboardUrl(webDriver, context, leaderboardName, regattaName, raceName, null, whitelabel);
    }

    public static RaceBoardPage goToRaceboardUrl(WebDriver webDriver,String context, String leaderboardName, String regattaName,
            String raceName, String raceMode, boolean whitelabel) throws UnsupportedEncodingException {
        // structure of an event link: "gwt/RaceBoard.html?leaderboardName=BMW+Cup+(J80)&regattaName=BMW+Cup+(J80)&raceName=BMW+Cup+Race+1&canReplayDuringLiveRaces=true"
        String escapedLeaderBoardName = URLEncoder.encode(leaderboardName,"UTF-8");
        String escapedRegattaName = URLEncoder.encode(regattaName, "UTF-8");
        String escapedRaceName = URLEncoder.encode(raceName, "UTF-8");
        String url = context + "gwt/RaceBoard.html?leaderboardName=" + escapedLeaderBoardName + "&regattaName="
                + escapedRegattaName + "&raceName=" + escapedRaceName;
        if(raceMode != null) {
            url += "&mode=" + URLEncoder.encode(raceMode, "UTF-8");
        }
        if (whitelabel) {
            url += "&whitelabel";
        }
        return goToRaceboardUrl(webDriver, url);
    }
    
    public TimeSliderPO getTimeSlider() {
        WebElement timeSliderElement = findElementBySeleniumId(this.driver, "timeSlider");
        return new TimeSliderPO(this.driver, timeSliderElement);
    }
}
