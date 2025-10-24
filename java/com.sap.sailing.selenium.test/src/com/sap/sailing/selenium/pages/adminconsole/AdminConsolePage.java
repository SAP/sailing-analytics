package com.sap.sailing.selenium.pages.adminconsole;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.HostPage;
import com.sap.sailing.selenium.pages.HostPageWithAuthentication;
import com.sap.sailing.selenium.pages.adminconsole.advanced.FileStoragePO;
import com.sap.sailing.selenium.pages.adminconsole.advanced.LocalServerPO;
import com.sap.sailing.selenium.pages.adminconsole.advanced.MasterDataImportPO;
import com.sap.sailing.selenium.pages.adminconsole.common.WhatsNewDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.connectors.ExpeditionDeviceConfigurationsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.connectors.ResultImportUrlsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.connectors.SmartphoneTrackingEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.igtimi.IgtimiDevicesManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardGroupConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.racemanagementapp.RaceManagementAppPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.roles.RoleDefinitionsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usergroups.UserGroupManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.UserManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.wind.WindPanelPO;

/**
 * <p>The page object representing the administration console. The console consists of multiple tabs with its content
 *   between you can switch.</p>
 * 
 * @author
 *   D049941
 */
public class AdminConsolePage extends HostPageWithAuthentication {
    private static final Logger logger = Logger.getLogger(AdminConsolePage.class.getName());
    private static final String PAGE_TITLE = "SAP Sailing Analytics Administration Console"; //$NON-NLS-1$

    private static final String ROLE_DEFINITIONS_TAB_LABEL = "Roles";
    private static final String ROLE_DEFINITIONS_TAB_IDENTIFIER = "RoleDefinitionsPanelWrapper";
    
    private static final String EVENTS_TAB_LABEL = "Events"; //$NON-NLS-1$
    private static final String EVENTS_TAB_IDENTIFIER = "EventManagement"; //$NON-NLS-1$
    
    private static final String RACE_MANAGER_APP_TAB_LABEL = "Race Manager App"; //$NON-NLS-1$
    private static final String RACE_MANAGER_APP_TAB_IDENTIFIER = "RaceCommiteeAppPanel"; //$NON-NLS-1$
    
    private static final String REGATTA_STRUCTURE_TAB_LABEL = "Regattas"; //$NON-NLS-1$
    private static final String REGATTA_STRUCTURE_TAB_IDENTIFIER = "RegattaStructureManagement"; //$NON-NLS-1$
    
    private static final String TRACTRAC_EVENTS_TAB_PARENT_LABEL = "Connectors";
    private static final String TRACTRAC_EVENTS_TAB_PARENT_IDENTIFIER = "TrackingProviderPanel";
    
    private static final String TRACTRAC_EVENTS_TAB_LABEL = "TracTrac Events"; //$NON-NLS-1$
    private static final String TRACTRAC_EVENTS_TAB_IDENTIFIER = "TracTracEventManagement"; //$NON-NLS-1$
    
    private static final String IGTIMI_DEVICES_TAB_LABEL = "Igtimi Devices"; //$NON-NLS-1$
    private static final String IGTIMI_DEVICES_TAB_IDENTIFIER = "IgtimiDevices"; //$NON-NLS-1$
    
    private static final String TRACKED_RACES_TAB_PARENT_LABEL = "Tracked races"; //$NON-NLS-1$
    private static final String TRACKED_RACES_TAB_PARENT_IDENTIFIER = "RacesPanel"; //$NON-NLS-1$
    
    private static final String TRACKED_RACES_TAB_LABEL = "Tracked races"; //$NON-NLS-1$
    private static final String TRACKED_RACES_TAB_IDENTIFIER = "TrackedRacesManagement"; //$NON-NLS-1$
    
    private static final String ADVANCED_USER_GROUP_MANAGEMENT_LABEL = "User Group Management";
    private static final String ADVANCED_USER_GROUP_MANAGEMENT_IDENTIFIER = "UserGroupManagementPanelWrapper";
    
    private static final String WIND_TAB_LABEL = "Wind"; //$NON-NLS-1$
    private static final String WIND_TAB_IDENTIFIER = "WindPanel"; //$NON-NLS-1$
    
    private static final String LEADERBOARD_CONFIGURATION_TAB_PARENT_LABEL = "Leaderboards"; //$NON-NLS-1$
    private static final String LEADERBOARD_CONFIGURATION_TAB_PARENT_IDENTIFIER = "LeaderboardPanel"; //$NON-NLS-1$
    
    private static final String LEADERBOARD_CONFIGURATION_TAB_LABEL = "Leaderboards"; //$NON-NLS-1$
    private static final String LEADERBOARD_CONFIGURATION_TAB_IDENTIFIER = "LeaderboardConfiguration"; //$NON-NLS-1$
    
    private static final String LEADERBOARD_GROUP_CONFIGURATION_TAB_LABEL = "Leaderboard groups"; //$NON-NLS-1$
    private static final String LEADERBOARD_GROUP_CONFIGURATION_TAB_IDENTIFIER = "LeaderboardGroupConfiguration"; //$NON-NLS-1$
    
    private static final String COMPETITOR_PANEL_TAB_LABEL = "Competitors"; //$NON-NLS-1$
    private static final String COMPETITOR_PANEL_TAB_IDENTIFIER = "CompetitorPanel"; //$NON-NLS-1$

    private static final String BOAT_PANEL_TAB_LABEL = "Boats"; //$NON-NLS-1$
    private static final String BOAT_PANEL_TAB_IDENTIFIER = "BoatPanel"; //$NON-NLS-1$

    private static final String SMARTPHONETRACKINGPANEL_PANEL_TAB_LABEL = "Smartphone Tracking"; //$NON-NLS-1$
    private static final String SMARTPHONETRACKINGPANEL_PANEL_TAB_IDENTIFIER = "SmartphoneTrackingPanel"; //$NON-NLS-1$

    private static final String RESULTIMPORTURLS_PANEL_TAB_LABEL = "Result import URLs"; //$NON-NLS-1$
    private static final String RESULTIMPORTURLS_PANEL_TAB_IDENTIFIER = "resultUrlsListComposite"; //$NON-NLS-1$

    private static final String USER_MANAGEMENT_PANEL_TAB_LABEL = "User Management"; //$NON-NLS-1$
    private static final String USER_MANAGEMENT_PANEL_TAB_IDENTIFIER = "UserManagementPanel"; //$NON-NLS-1$

    private static final String EXPEDITION_DEVICE_CONFIGURATION_PANEL_TAB_LABEL = "Expedition Device Configurations"; //$NON-NLS-1$
    private static final String EXPEDITION_DEVICE_CONFIGURATION_PANEL_TAB_IDENTIFIER = "ExpeditionDeviceConfigurations"; //$NON-NLS-1$

    private static final String ADVANCED_PARENT_LABEL = "Advanced";
    private static final String ADVANCED_TAB_PARENT_IDENTIFIER = "AdvancedTab";
    private static final String ADVANCED_MASTERDATA_LABEL = "Master Data Import";
    private static final String ADVANCED_MASTERDATA_IDENTIFIER = "MasterDataImport";
    private static final String ADVANCED_LOCAL_SERVER_LABEL = "Local Server";
    private static final String ADVANCED_LOCAL_SERVER_IDENTIFIER = "LocalServer";
    private static final String ADVANCED_FILE_STORAGE_LABEL = "File Storage";
    private static final String ADVANCED_FILE_STORAGE_IDENTIFIER = "fileStoragePanel";

    private static final String LEADERBOARDS_PLACE = "LeaderboardsPlace";
    private static final String EXPEDITION_DEVICE_CONFIGURATIONS_PLACE = "ExpeditionDeviceConfigurationsPlace";
    
    private static final String ID_ADMIN_WHATS_NEW_DIALOG = "AdminWhatsNewDialog";

    private static final String ADMIN_CONSOLE_PATH = "gwt/AdminConsole.html";

    /**
     * <p>Goes to the administration console and returns the representing page object.</p>
     * 
     * @param driver
     *   The web driver to use.
     * @param root
     *   The context root of the application.
     * @return
     *   The page object for the administration console.
     */
    public static AdminConsolePage goToPage(WebDriver driver, String root) {
        return HostPage.goToUrl(AdminConsolePage::new, driver, root + ADMIN_CONSOLE_PATH);
    }

    public static AdminConsolePage goToLeaderboardConfigurationPlace(WebDriver driver, String root, String ...urlParameterKeysAndValues) {
        return HostPage.goToPlace(AdminConsolePage::new, driver, root + ADMIN_CONSOLE_PATH, LEADERBOARDS_PLACE, urlParameterKeysAndValues);
    }

    public static AdminConsolePage goToExpeditionDeviceConfigurationsPlace(WebDriver driver, String root, String ...urlParameterKeysAndValues) {
        return HostPage.goToPlace(AdminConsolePage::new, driver, root + ADMIN_CONSOLE_PATH, EXPEDITION_DEVICE_CONFIGURATIONS_PLACE, urlParameterKeysAndValues);
    }

    public LeaderboardConfigurationPanelPO getLeaderboardConfigurationPanelPO () {
        WebElement content = waitForWebElement(administrationTabPanel, LEADERBOARD_CONFIGURATION_TAB_IDENTIFIER);
        return new LeaderboardConfigurationPanelPO(driver, content);
    }

    public ExpeditionDeviceConfigurationsPanelPO getExpeditionDeviceConfigurationsPanelPO () {
        WebElement content = waitForWebElement(administrationTabPanel, EXPEDITION_DEVICE_CONFIGURATION_PANEL_TAB_IDENTIFIER);
        return new ExpeditionDeviceConfigurationsPanelPO(driver, content);
    }
    
    @FindBy(how = BySeleniumId.class, using = "AdministrationTabs")
    private WebElement administrationTabPanel;
    
    private AdminConsolePage(WebDriver driver) {
        super(driver);
        cancelWhatsNewDialogIfAppearing();
    }
    
    public EventConfigurationPanelPO goToEvents() {
        return new EventConfigurationPanelPO(this.driver, goToTab(EVENTS_TAB_LABEL, EVENTS_TAB_IDENTIFIER, true));
    }
    
    public RaceManagementAppPanelPO goToRaceManagerApp() {
        return new RaceManagementAppPanelPO(this.driver, goToTab(RACE_MANAGER_APP_TAB_LABEL, RACE_MANAGER_APP_TAB_IDENTIFIER, true));
    }
    
    public UserManagementPanelPO goToUserManagement() {
        goToTab(ADVANCED_PARENT_LABEL, ADVANCED_TAB_PARENT_IDENTIFIER, true);
        return new UserManagementPanelPO(this.driver,
                goToTab(USER_MANAGEMENT_PANEL_TAB_LABEL, USER_MANAGEMENT_PANEL_TAB_IDENTIFIER, false));
    }
    
    public UserGroupManagementPanelPO goToUserGroupDefinitions() {
        goToTab(ADVANCED_PARENT_LABEL, ADVANCED_TAB_PARENT_IDENTIFIER, true);
        return new UserGroupManagementPanelPO(this.driver,
                goToTab(ADVANCED_USER_GROUP_MANAGEMENT_LABEL, ADVANCED_USER_GROUP_MANAGEMENT_IDENTIFIER, false));
    }
    
    public RoleDefinitionsPanelPO goToRoleDefinitions() {
        goToTab(ADVANCED_PARENT_LABEL, ADVANCED_TAB_PARENT_IDENTIFIER, true);
        return new RoleDefinitionsPanelPO(this.driver,
                goToTab(ROLE_DEFINITIONS_TAB_LABEL, ROLE_DEFINITIONS_TAB_IDENTIFIER, false));
    }

    public RegattaStructureManagementPanelPO goToRegattaStructure() {
        return new RegattaStructureManagementPanelPO(this.driver, goToTab(REGATTA_STRUCTURE_TAB_LABEL,
                REGATTA_STRUCTURE_TAB_IDENTIFIER, true));
    }
    
    /**
     * <p>Switch to the TracTrac Events tab and waits until the animation is finished. The returned page object,
     *   representing the tab, becomes invalid if you switch to another tab in the administration console.</p>
     * 
     * @return
     *   The page object for the TracTracEvents tab.
     */
    public TracTracEventManagementPanelPO goToTracTracEvents() {
        goToTab(TRACTRAC_EVENTS_TAB_PARENT_LABEL, TRACTRAC_EVENTS_TAB_PARENT_IDENTIFIER, true);
        return new TracTracEventManagementPanelPO(this.driver, goToTab(TRACTRAC_EVENTS_TAB_LABEL,
                TRACTRAC_EVENTS_TAB_IDENTIFIER, false));
    }
    
    public IgtimiDevicesManagementPanelPO goToIgtimi() {
        goToTab(TRACTRAC_EVENTS_TAB_PARENT_LABEL, TRACTRAC_EVENTS_TAB_PARENT_IDENTIFIER, true);
        return new IgtimiDevicesManagementPanelPO(this.driver, goToTab(IGTIMI_DEVICES_TAB_LABEL,
                IGTIMI_DEVICES_TAB_IDENTIFIER, false));
    }
    
    public TrackedRacesManagementPanelPO goToTrackedRaces() {
        goToTab(TRACKED_RACES_TAB_PARENT_LABEL, TRACKED_RACES_TAB_PARENT_IDENTIFIER, true);
        return new TrackedRacesManagementPanelPO(this.driver, goToTab(TRACKED_RACES_TAB_LABEL,
                TRACKED_RACES_TAB_IDENTIFIER, false));
    }
    
    public WindPanelPO goToWind() {
        goToTab(TRACKED_RACES_TAB_PARENT_LABEL, TRACKED_RACES_TAB_PARENT_IDENTIFIER, true);
        return new WindPanelPO(this.driver, goToTab(WIND_TAB_LABEL,
                WIND_TAB_IDENTIFIER, false));
    }
    
    public LeaderboardConfigurationPanelPO goToLeaderboardConfiguration() {
        goToTab(LEADERBOARD_CONFIGURATION_TAB_PARENT_LABEL, LEADERBOARD_CONFIGURATION_TAB_PARENT_IDENTIFIER, true);
        return new LeaderboardConfigurationPanelPO(this.driver, goToTab(LEADERBOARD_CONFIGURATION_TAB_LABEL,
                LEADERBOARD_CONFIGURATION_TAB_IDENTIFIER, false));
    }
    
    public LeaderboardGroupConfigurationPanelPO goToLeaderboardGroupConfiguration() {
        goToTab(LEADERBOARD_CONFIGURATION_TAB_PARENT_LABEL, LEADERBOARD_CONFIGURATION_TAB_PARENT_IDENTIFIER, true);
        return new LeaderboardGroupConfigurationPanelPO(this.driver, goToTab(LEADERBOARD_GROUP_CONFIGURATION_TAB_LABEL,
                LEADERBOARD_GROUP_CONFIGURATION_TAB_IDENTIFIER, false));
    }
    
    public TrackedRacesCompetitorsPanelPO goToTrackedRacesCompetitors() {
        goToTab(TRACKED_RACES_TAB_PARENT_LABEL, TRACKED_RACES_TAB_PARENT_IDENTIFIER, true);
        return new TrackedRacesCompetitorsPanelPO(this.driver, goToTab(COMPETITOR_PANEL_TAB_LABEL,
                COMPETITOR_PANEL_TAB_IDENTIFIER, false));
    }

    public TrackedRacesBoatsPanelPO goToTrackedRacesBoats() {
        goToTab(TRACKED_RACES_TAB_PARENT_LABEL, TRACKED_RACES_TAB_PARENT_IDENTIFIER, true);
        return new TrackedRacesBoatsPanelPO(this.driver, goToTab(BOAT_PANEL_TAB_LABEL,
                BOAT_PANEL_TAB_IDENTIFIER, false));
    }
    
    public MasterDataImportPO goToMasterDateImport() {
        goToTab(ADVANCED_PARENT_LABEL, ADVANCED_TAB_PARENT_IDENTIFIER, true);
        return new MasterDataImportPO(this.driver,
                goToTab(ADVANCED_MASTERDATA_LABEL, ADVANCED_MASTERDATA_IDENTIFIER, false));
    }
    
    public FileStoragePO goToFileStorage() {
        goToTab(ADVANCED_PARENT_LABEL, ADVANCED_TAB_PARENT_IDENTIFIER, true);
        return new FileStoragePO(this.driver,
                goToTab(ADVANCED_FILE_STORAGE_LABEL, ADVANCED_FILE_STORAGE_IDENTIFIER, false));
    }

    public SmartphoneTrackingEventManagementPanelPO goToSmartphoneTrackingPanel() {
        goToTab(TRACTRAC_EVENTS_TAB_PARENT_LABEL, TRACTRAC_EVENTS_TAB_PARENT_IDENTIFIER, true);
        return new SmartphoneTrackingEventManagementPanelPO(this.driver, goToTab(SMARTPHONETRACKINGPANEL_PANEL_TAB_LABEL,
                SMARTPHONETRACKINGPANEL_PANEL_TAB_IDENTIFIER, false));
    }

    public ResultImportUrlsPanelPO goToResultImportUrlsPanel() {
        goToTab(TRACTRAC_EVENTS_TAB_PARENT_LABEL, TRACTRAC_EVENTS_TAB_PARENT_IDENTIFIER, true);
        return new ResultImportUrlsPanelPO(this.driver, goToTab(RESULTIMPORTURLS_PANEL_TAB_LABEL,
                RESULTIMPORTURLS_PANEL_TAB_IDENTIFIER, false));
    }


    public ExpeditionDeviceConfigurationsPanelPO goToExpeditionDeviceConfigurationsPanel() {
        goToTab(TRACTRAC_EVENTS_TAB_PARENT_LABEL, TRACTRAC_EVENTS_TAB_PARENT_IDENTIFIER, true);
        return new ExpeditionDeviceConfigurationsPanelPO(this.driver, goToTab(EXPEDITION_DEVICE_CONFIGURATION_PANEL_TAB_LABEL,
                EXPEDITION_DEVICE_CONFIGURATION_PANEL_TAB_IDENTIFIER, false));
    }

    public LocalServerPO goToLocalServerPanel() {
        goToTab(ADVANCED_PARENT_LABEL, ADVANCED_TAB_PARENT_IDENTIFIER, true);
        return new LocalServerPO(this.driver,
                goToTab(ADVANCED_LOCAL_SERVER_LABEL, ADVANCED_LOCAL_SERVER_IDENTIFIER, false));
    }
    
    /**
     * <p>Verifies that the current page is the administration console by checking the title of the page.</p>
     */
    @Override
    protected void verify() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted while waiting for an admin console tab to appear", e);
        } // wait a bit for the page to have initialized and set its title
        if (!PAGE_TITLE.equals(this.driver.getTitle())) {
            throw new IllegalStateException("This is not the administration console: " + this.driver.getTitle()); //$NON-NLS-1$
        }
    }
    
    private WebElement goToTab(String label, final String id, boolean isVertical) {
        return goToTab(administrationTabPanel, label, id, isVertical ? TabPanelType.VERTICAL_TAB_LAYOUT_PANEL : TabPanelType.TAB_LAYOUT_PANEL);
    }
    
    private void cancelWhatsNewDialogIfAppearing() {
        try {
            waitForPO(WhatsNewDialogPO::new, ID_ADMIN_WHATS_NEW_DIALOG, 1).pressCancel();
        } catch (TimeoutException exc) {
            logger.log(Level.FINEST, "What's new dialog not found => ignoring", exc);
        }
    }

}
