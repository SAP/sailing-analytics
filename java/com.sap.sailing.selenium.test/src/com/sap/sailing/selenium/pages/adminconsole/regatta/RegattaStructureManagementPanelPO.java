package com.sap.sailing.selenium.pages.adminconsole.regatta;

import java.time.Duration;
import java.util.function.Function;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.common.ConfirmDialogPO;

public class RegattaStructureManagementPanelPO extends PageArea {
    public static final String DEFAULT_SERIES_NAME = "DefaultSelenium"; //$NON-NLS-1$
    
    @FindBy(how = BySeleniumId.class, using = "AddRegattaButton")
    WebElement addRegattaButton;
    
    @FindBy(how = BySeleniumId.class, using = "RemoveRegattaButton")
    WebElement removeRegattaButton;
    
    @FindBy(how = BySeleniumId.class, using = "RegattaListComposite")
    WebElement regattaList;
    
    @FindBy(how = BySeleniumId.class, using = "RegattaDetailsComposite")
    WebElement regattaDetails;
    
    public RegattaStructureManagementPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public RegattaCreateDialogPO startRegattaCreation() {
        this.addRegattaButton.click();
        WebElement dialog = findElementBySeleniumId(this.driver, "RegattaCreateDialog"); //$NON-NLS-1$
        return new RegattaCreateDialogPO(this.driver, dialog);
    }
    
    /**
     * <p>Creates a regatta with 1 series (named "Default").</p>
     * 
     * @param regatta
     */
    public void createRegatta(RegattaDescriptor regatta, boolean withDefaultLeaderboard) {
        RegattaCreateDialogPO createRegattaDialog = startRegattaCreation();
        createRegattaDialog.setRegattaName(regatta.getName()+" ("+regatta.getBoatClass()+")");
        createRegattaDialog.setBoatClass(regatta.getBoatClass());
        createRegattaDialog.setCompetitorRegistrationType(regatta.getCompetitorRegistrationType());
        SeriesCreateDialogPO addSeriesDialog = createRegattaDialog.addSeries();
        addSeriesDialog.setSeriesName(DEFAULT_SERIES_NAME);
        addSeriesDialog.pressOk();
        // QUESTION: How do we handle an error (here or in the dialog)?
        createRegattaDialog.pressOk();
        DefaultRegattaLeaderboardCreateDialogPO createDefaultRegattaLeaderboardDialog = createDefaultRegattaLeaderboard();
        if (withDefaultLeaderboard) {
            createDefaultRegattaLeaderboardDialog.pressOk();
        } else {
            createDefaultRegattaLeaderboardDialog.pressCancel();
        }
        
    }
    
    public void createRegattaAndAddToEvent(RegattaDescriptor regatta, String event, String[] courseAreaNames) {
        RegattaCreateDialogPO createRegattaDialog = startRegattaCreation();
        createRegattaDialog.setRegattaName(regatta.getName()+" ("+regatta.getBoatClass()+")");
        createRegattaDialog.setBoatClass(regatta.getBoatClass());
        createRegattaDialog.setCompetitorRegistrationType(regatta.getCompetitorRegistrationType());
        createRegattaDialog.setEventAndCourseArea(event, courseAreaNames);
        createRegattaDialog.setCompetitorRegistrationType(CompetitorRegistrationType.OPEN_UNMODERATED);
        createRegattaDialog.pressOk();
        createDefaultRegattaLeaderboard().pressOk();
        waitForPO(ConfirmDialogPO::new, EventConfigurationPanelPO.ID_LINK_LEADERBORAD_TO_GROUP_DIALOG, 5).pressOk();
    }
    
    private DefaultRegattaLeaderboardCreateDialogPO createDefaultRegattaLeaderboard() {
        final WebElement dialog = waitForElementBySeleniumId(this.driver, "CreateDefaultRegattaLeaderboardDialog", 10); //$NON-NLS-1$
        return new DefaultRegattaLeaderboardCreateDialogPO(this.driver, dialog);
    }

    public RegattaListCompositePO getRegattaList() {
        return new RegattaListCompositePO(this.driver, this.regattaList);
    }
    
    public RegattaDetailsCompositePO getRegattaDetails(RegattaDescriptor regatta) {
        RegattaListCompositePO regattaList = getRegattaList();
        regattaList.selectRegatta(regatta);
        
        return getRegattaDetails();
    }
    
    public RegattaDetailsCompositePO getRegattaDetails() {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_TIMEOUT_SECONDS)).until(new Function<WebDriver, RegattaDetailsCompositePO>() {
            @Override
            public RegattaDetailsCompositePO apply(WebDriver t) {
                if (regattaDetails.isDisplayed()) {
                    return new RegattaDetailsCompositePO(driver, regattaDetails);
                }
                return null;
            }
        });
    }
}
