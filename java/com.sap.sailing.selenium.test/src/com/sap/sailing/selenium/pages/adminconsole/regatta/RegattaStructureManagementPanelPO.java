package com.sap.sailing.selenium.pages.adminconsole.regatta;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;

public class RegattaStructureManagementPanelPO extends PageArea {
    public static final String DEFAULT_SERIES_NAME = "Default"; //$NON-NLS-1$
    
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
    public void createRegatta(RegattaDescriptor regatta) {
        RegattaCreateDialogPO createRegattaDialog = startRegattaCreation();
        createRegattaDialog.setRegattaName(regatta.getName()+" ("+regatta.getBoatClass()+")");
        createRegattaDialog.setBoatClass(regatta.getBoatClass());
        SeriesCreateDialogPO addSeriesDialog = createRegattaDialog.addSeries();
        addSeriesDialog.setSeriesName(DEFAULT_SERIES_NAME);
        addSeriesDialog.pressOk();
        // QUESTION: How do we handle an error (here or in the dialog)?
        createRegattaDialog.pressOk();
        DefaultRegattaLeaderboardCreateDialogPO createDefaultRegattaLeaderboardDialog = createDefaultRegattaLeaderboard();
        createDefaultRegattaLeaderboardDialog.pressCancel();
    }
    
    private DefaultRegattaLeaderboardCreateDialogPO createDefaultRegattaLeaderboard() {
            WebElement dialog = findElementBySeleniumId(this.driver, "CreateDefaultRegattaDialog"); //$NON-NLS-1$
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
        if (this.regattaDetails.isDisplayed()) {
            return new RegattaDetailsCompositePO(this.driver, this.regattaDetails);
        }
        return null;
    }
}
