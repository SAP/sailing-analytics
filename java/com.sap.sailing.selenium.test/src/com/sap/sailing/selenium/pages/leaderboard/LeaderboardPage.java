package com.sap.sailing.selenium.pages.leaderboard;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.HostPage;
import com.sap.sailing.selenium.pages.gwt.CheckBoxPO;

/**
 * <p>The page object representing the leaderboard.</p>
 * 
 * @author
 *   D049941
 */
public class LeaderboardPage extends HostPage {
    
    private static final String LEADERBOAR_PARAMETER_NAME = "name"; //$NON-NLS-1$
    private static final String SHOW_RACE_DETAILS_PARAMETER_NAME = "showRaceDetails"; //$NON-NLS-1$
    private static final String REFRESH_INTERVAL_MILLIS = "refreshIntervalMillis"; //$NON-NLS-1$
    
    private static final String AUTO_REFRESH_ENABLED_STRING = "Pause automatic refresh"; //$NON-NLS-1$
    
    private static final String AUTO_REFRESH_DISABLED_STRING = "Refresh automatically";  //$NON-NLS-1$
    
    public static LeaderboardPage goToPage(WebDriver driver, String root, String leaderboard) {
        return goToPage(driver, root, leaderboard, false);
    }
    
    /**
     * <p>Goes to the specified leaderboard and returns the representing page object.</p>
     * 
     * @param driver
     *   The web driver to use.
     * @param root
     *   The context root of the application.
     * @param leaderboard
     *   The name of the leaderboard.
     * @return
     *   The page object for the administration console.
     */
    public static LeaderboardPage goToPage(WebDriver driver, String root, String leaderboard, boolean raceDetails) {
        return goToPage(driver, root, leaderboard, raceDetails, /* autoRefreshIntervalInMillis */ null);
    }
    
    public static LeaderboardPage goToPage(WebDriver driver, String root, String leaderboard, boolean raceDetails, Long autoRefreshIntervalInMillis) {
        driver.get(root + "gwt/Leaderboard.html?" + getLeaderboard(leaderboard) + //$NON-NLS-1$
                "&" + getGWTCodeServerAndLocale() + "&" + getShowRaceDeatails(raceDetails) +
                (autoRefreshIntervalInMillis==null ? "" : ("&" + getAutoRefreshIntervalMillis(autoRefreshIntervalInMillis)))); //$NON-NLS-1$
        
        return new LeaderboardPage(driver, leaderboard);
    }
    
    public static LeaderboardPage goToPage(WebDriver driver, String href) {
        driver.get(href);
        return new LeaderboardPage(driver);
    }
    
    private static String getLeaderboard(String leaderboard) {
        return LEADERBOAR_PARAMETER_NAME + "=" + leaderboard; //$NON-NLS-1$
    }
    
    private static String getShowRaceDeatails(boolean raceDetails) {
        return SHOW_RACE_DETAILS_PARAMETER_NAME + "=" + raceDetails; //$NON-NLS-1$
    }
    
    private static String getAutoRefreshIntervalMillis(long autoRefreshIntervalInMillis) {
        return REFRESH_INTERVAL_MILLIS + "=" + autoRefreshIntervalInMillis; //$NON-NLS-1$
    }
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardDisplayCheckBox")
    private WebElement leaderboardDisplayCheckBox;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardSettingsButton")
    private WebElement leaderboardSettingsButton;
    
    @FindBy(how = BySeleniumId.class, using = "ShowChartsDisplayCheckBox")
    private WebElement competitorChartsDisplayCheckBox;
    
    @FindBy(how = BySeleniumId.class, using = "ShowChartsSettingsButton")
    private WebElement competitorChartsSettingsButton;
    
    @FindBy(how = BySeleniumId.class, using = "PlayAndPauseAnchor")
    private WebElement playAndPauseAnchor;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardCellTable")
    private WebElement leaderboardCellTable;
    
    private LeaderboardPage(WebDriver driver) {
        super(driver);
    }
    
    private LeaderboardPage(WebDriver driver, String leaderboard) {
        this(driver);
        
        if(!this.driver.getTitle().equals(leaderboard)) {
            throw new IllegalStateException("This is not the leaderboard"); //$NON-NLS-1$
        }
    }
    
    public boolean isAutoRefreshEnabled() {
        String playPauseAnchorTitle = this.playAndPauseAnchor.getAttribute("title");
        if (AUTO_REFRESH_ENABLED_STRING.equals(playPauseAnchorTitle)) {
            return true;
        }
        if (AUTO_REFRESH_DISABLED_STRING.equals(playPauseAnchorTitle)) {
            return false;
        }
        throw new RuntimeException("Can not determine auto refresh state"); //$NON-NLS-1$
    }
    
    public void refresh() {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        Long finishedCalls = (Long) executor.executeScript(
                "return window.PENDING_AJAX_CALLS.numberOfFinishedCalls(arguments[0])", "loadLeaderboardData");
        // this.playAndPauseAnchor.click();
        new Actions(driver).moveToElement(playAndPauseAnchor).click().build().perform();
        waitForAjaxRequestsExecuted("loadLeaderboardData", finishedCalls.intValue() + 1);
        new Actions(driver).moveToElement(playAndPauseAnchor).click().build().perform();
    }
    
    public CompetitorChartsSettingsDialogPO getCompetitorChartsSettings() {
        this.competitorChartsSettingsButton.click();
        return new CompetitorChartsSettingsDialogPO(this.driver,
                findElementBySeleniumId(this.driver, "CompetitorChartsSettingsDialog"));
    }
    
    public LeaderboardSettingsDialogPO getLeaderboardSettings() {
        scrollToTop();
        this.leaderboardSettingsButton.click();
        return new LeaderboardSettingsDialogPO(this.driver,
                findElementBySeleniumId(this.driver, "LeaderboardSettingsDialog"));
    }
    
    public boolean isOverallLeaderboardVisible() {
        boolean selected = false;
        try {
            WebElement element = findElementBySeleniumId("SeriesLeaderboardDisplayCheckBox");
            CheckBoxPO checkbox = new CheckBoxPO(this.driver, element);
            selected = checkbox.isSelected();
        } catch(Exception e) {}
        
        int leaderboardsCount = findElementsBySeleniumId("LeaderboardCellTable").size();
        Assertions.assertEquals(selected ? 2 : 1, leaderboardsCount);
        return selected;
    }
    
    public LeaderboardSettingsDialogPO getOverallLeaderboardSettings() {
        WebElement overallLeaderboardSettingsButton = findElementBySeleniumId("SeriesLeaderboardSettingsButton");
        overallLeaderboardSettingsButton.click();
        return new LeaderboardSettingsDialogPO(this.driver,
                findElementBySeleniumId(this.driver, "SeriesLeaderboardSettingsDialog"));
    }
    
    public LeaderboardTablePO getLeaderboardTable() {
        return new LeaderboardTablePO(this.driver, this.leaderboardCellTable);
    }

    public void showCompetitorChart(String chart) {
        this.competitorChartsSettingsButton.click();
        CompetitorChartsSettingsDialogPO dialog = new CompetitorChartsSettingsDialogPO(this.driver,
                findElementBySeleniumId(this.driver, "CompetitorChartsSettingsDialog"));
        dialog.selectChartType(chart);
        
        CheckBoxPO checkbox = new CheckBoxPO(this.driver, this.competitorChartsDisplayCheckBox);
        checkbox.setSelected(true);
    }
    
    public void hideCompetitorChart() {
        CheckBoxPO checkbox = new CheckBoxPO(this.driver, this.competitorChartsDisplayCheckBox);
        checkbox.setSelected(false);
    }
    
//    public LineChart getCompetitorChart() {
//        return new LineChart(this.driver, findElementBySeleniumId("CompetitorChart"));
//    }
    
    public boolean isCompetitorChartVisible() {
        CheckBoxPO checkbox = new CheckBoxPO(this.driver, this.competitorChartsDisplayCheckBox);
        boolean selected = checkbox.isSelected();
        WebElement element = null;
        try {
            element = findElementBySeleniumId("CompetitorChart");
        } catch(Exception e) {}
        Assertions.assertEquals(selected, element != null);
        return selected;
    }
    
    @Override
    protected void initElements() {
        super.initElements();
        
        // Wait for the initial loading of the data
        waitForAjaxRequestsExecuted("loadLeaderboardData", 1);
    }
}
