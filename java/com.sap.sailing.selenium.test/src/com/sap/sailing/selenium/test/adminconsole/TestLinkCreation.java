package com.sap.sailing.selenium.test.adminconsole;

import java.net.SocketException;
import java.net.URL;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.connectors.SmartphoneTrackingEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.racemanagementapp.DeviceConfigurationCreateDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.racemanagementapp.DeviceConfigurationDetailsAreaPO;
import com.sap.sailing.selenium.pages.adminconsole.racemanagementapp.DeviceConfigurationQRCodeDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.racemanagementapp.RaceManagementAppPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaDetailsCompositePO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegistrationLinkWithQRCodeDialogPO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.home.HomePage;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sailing.selenium.test.adminconsole.smartphonetracking.AddCompetitorWithBoatDialogPO;
import com.sap.sailing.selenium.test.adminconsole.smartphonetracking.AddDeviceMappingsDialogPO;
import com.sap.sailing.selenium.test.adminconsole.smartphonetracking.MapDevicesDialogPO;
import com.sap.sailing.selenium.test.adminconsole.smartphonetracking.RegisterCompetitorsDialogPO;

/**
 * There are various link creation logic in AdminConsole. This test is to cover them.
 * <p>
 * 
 * The reply from branch.io regarding the test failures in connection with "localhost" addresses was this:
 * <p>
 * 
 * "It looks like the engineering team confirmed there was an update to security and this behavior is expected. Branch
 * recently applied the new WAF rule, and does not allow passing the localhost as a query string due to avoid widely
 * known risks/issues/phishing for security reasons. Since WAF does not have access to the link data, the error does not
 * raise when localhost is set an encapsulated link data.<p>
 * 
 * It's recommend that clients use a hosted pre-prod environment for their internal testing, rather than localhost"<p>
 * 
 * Therefore, we've installed a DNS CNAME alias for the test-qr-code-place.sapsailing.com domain name to point to
 * 127.0.0.1. This domain name is used in the test to replace localhost in the URLs and is accepted in URL parameters
 * by branch.io.
 */
public class TestLinkCreation extends AbstractSeleniumTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestLinkCreation.class);

    private static final String DEVICE_CONFIG_NAME = "Test";
    private static final String BMW_CUP_EVENT = "BMW Cup";
    private static final String BMW_CUP_BOAT_CLASS = "J80";
    private static final String AUDI_CUP_BOAT_CLASS = "J70";
    private static final String BMW_CUP_REGATTA = "BMW Cup (J80)"; //$NON-NLS-1$
    private static final String AUDI_CUP_REGATTA = "Audi Business Cup (J70)"; //$NON-NLS-1$
    private static final String BMW_CUP_EVENTS_DESC = "BMW Cup Description";
    private static final String BMW_VENUE = "Somewhere";
    private static final Date BMW_START_EVENT_TIME = DatatypeConverter.parseDateTime("2012-04-08T10:09:00-05:00")
            .getTime();
    private static final Date BMW_STOP_EVENT_TIME = DatatypeConverter.parseDateTime("2017-04-08T10:50:00-05:00")
            .getTime();
    private static final String CUSTOM_COURSE_AREA = "Custom X";
    private static final String INVITATION_URL_BASE = "https://sailinsight30-app.sapsailing.com/publicInvite?regatta_name=Audi+Business+Cup+(J70)+(J70)";
    private static final String INVITATION_QR_CODE_BASE = "https://sailinsight30-app.sapsailing.com/publicInvite?regatta_name=Audi%20Business%20Cup%20(J70)%20(J70)";
    private static final String EXPECTED_PUPLIC_INVITE_QR_CODE_TITLE = "Welcome to the public regatta Audi Business Cup (J70) (J70)";
    private static final String EXPECTED_RACE_MANAGER_APP_QR_CODE_TITLE = "Welcome to the Race Manager App registration";
    private static final String EXPECTED_DEVICE_REGISTRATION_QR_CODE_TITLE = "Welcome Competitor Test to the regatta Audi Business Cup (J70) (J70)";
    private static final String EXPECTED_QR_LINK_TEXT = "Please scan this QR Code with your mobile device to proceed with the registration";

    private static final String CHECK_RACE_APP_URL_REGEX = "^https:\\/\\/racemanager-app.sapsailing.com\\/invite\\"
            + "?server_url=http.*localhost.*(\\d{0,5})?" + "&device_config_identifier=" + DEVICE_CONFIG_NAME
            + "&device_config_uuid=(\\w|\\d|-)*";

    private static final String DEVICE_REGISTRATION_URL_REGEX = ".*https:\\/\\/sailinsight30-app.sapsailing.com\\/invite\\?checkinUrl=.*";
    private static final String BRANCH_IO_COMPLIANT_LOCALHOST_ALIAS = "branch-io-localhost-alias";

    @Override
    @BeforeEach
    public void setUp() {
        boolean checkUrlIsWorking;
        String prodCheckUrl = "https://www.sapsailing.com/gwt/status";
        try {
            LOG.debug("Testing check URL from productive server");
            (new URL(prodCheckUrl)).openStream().close();
            checkUrlIsWorking = true;
            LOG.info("Productive server {} is accessible.", prodCheckUrl);
        } catch (Exception ex) {
            checkUrlIsWorking = false;
            LOG.warn("Productive server {} is NOT accessible. Skip tests which are requirering live connection.",
                    prodCheckUrl);
        }
        Assumptions.assumeTrue(checkUrlIsWorking, "Execute link creation test only if productive server is online (www.sapsailing.com).");
        clearState(getContextRoot());
        super.setUp();
    }

    /**
     * Test the creation of an invitation link.
     * <p>
     * Please notice, that the test checks the created invitation link by calling it and checking over the redirects
     * from branch.io over production environment (my.sapsailing.com) back to localhost. The link back to localhost need
     * an additional confirmation on production server.
     */
    @SeleniumTestCase
    public void testRegattaOverviewInvitationLinkCreation() throws SocketException {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        // create an event and regatta
        EventConfigurationPanelPO events = adminConsole.goToEvents();
        events.createEventWithDefaultLeaderboardGroupRegattaAndDefaultLeaderboard(BMW_CUP_EVENT, BMW_CUP_EVENTS_DESC,
                BMW_VENUE, BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, /* isPublic */ true, BMW_CUP_REGATTA,
                BMW_CUP_BOAT_CLASS, BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, /* useOverallLeaderboard */ false,
                CUSTOM_COURSE_AREA);
        RegattaStructureManagementPanelPO regattas = adminConsole.goToRegattaStructure();
        RegattaDescriptor regattaDescriptor = new RegattaDescriptor(AUDI_CUP_REGATTA, AUDI_CUP_BOAT_CLASS);
        regattas.createRegattaAndAddToEvent(regattaDescriptor, BMW_CUP_EVENT, new String[] { CUSTOM_COURSE_AREA });
        // extract secret and event ID
        RegattaEditDialogPO editRegatta = regattas.getRegattaList().editRegatta(regattaDescriptor);
        String secret = editRegatta.getSecret();
        String selectedEventId = editRegatta.getSelectedEventId();
        editRegatta.clickOkButtonOrThrow();
        // create expected link URL based on the secret and event ID
        RegattaDetailsCompositePO regattaDetails = regattas.getRegattaDetails();
        RegistrationLinkWithQRCodeDialogPO registrationLinkWithQRCode = regattaDetails.configureRegistrationURL();
        // check URL
        String createdInvitationUrl = registrationLinkWithQRCode.getRegistrationLinkUrl();
        Assertions.assertTrue(createdInvitationUrl.startsWith(INVITATION_URL_BASE));
        Assertions.assertTrue(createdInvitationUrl.contains("secret=" + secret));
        Assertions.assertTrue(createdInvitationUrl.contains("event_id=" + selectedEventId));
        Assertions.assertTrue(createdInvitationUrl.contains("server=http%3A%2F%2Flocalhost%3A"));
        // "localhost" is not accepted by branch.io, so we need to replace by some artificial server;
        // that server will not have to be resolved during the test, so it can be a random string
        final String localNonLoopbackAddress = getNonLoopbackLocalhostAddress();
        final String nonLocalhostCreatedInvitationUrl = createdInvitationUrl.replace("localhost", localNonLoopbackAddress);
        registrationLinkWithQRCode.clickOkButtonOrThrow();
        HomePage.goToHomeUrl(getWebDriver(), nonLocalhostCreatedInvitationUrl);
        Wait<WebDriver> wait = new WebDriverWait(getWebDriver(), 30);
        // Confirm dialog will be shown on sapsailing.com to redirect back to localhost or non-default domain
//      wait.until(ExpectedConditions.numberOfElementsToBe(
//              By.xpath("//button[contains(text(), 'Yes')] | //button[contains(text(), 'Ja')]"), 1)).get(0).click();
        // here we are back on localhost (Home.html#QRCodePlace)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(text(), '" + EXPECTED_PUPLIC_INVITE_QR_CODE_TITLE + "')]")));
        WebElement qrCodeLink = wait.until(ExpectedConditions
                .presenceOfElementLocated(By.xpath("//a[contains(text(), '" + EXPECTED_QR_LINK_TEXT + "')]")));
        Assertions.assertTrue(qrCodeLink.getAttribute("href").startsWith(INVITATION_QR_CODE_BASE));
        Assertions.assertTrue(qrCodeLink.getAttribute("href").contains("secret=" + secret));
        Assertions.assertTrue(qrCodeLink.getAttribute("href").contains("server=http%3A%2F%2F"+localNonLoopbackAddress+"%3A"));
    }
    
    /**
     * @return an alias hostname for localhost; branch.io does not accept localhost nor 127.0.0.1 nor any
     * local IP address such as 192.168.x.x or 10.x.x.x. The way we solve this here is to establish a Route53
     * CNAME alias DNS record for the test-qr-code-place.sapsailing.com domain name to point to 127.0.0.1.
     * branch.io is not smart enough to check the DNS record and will accept this domain name.
     */
    private String getNonLoopbackLocalhostAddress() throws SocketException {
        return BRANCH_IO_COMPLIANT_LOCALHOST_ALIAS + "." + SharedLandscapeConstants.DEFAULT_DOMAIN_NAME;
    }

    /**
     * Testing the generation of an invitation QR code for the Race Manager App.
     */
    @SeleniumTestCase
    public void testRaceManagerAppInvitationLink() throws SocketException {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        RaceManagementAppPanelPO raceManagerApp = adminConsole.goToRaceManagerApp();
        DeviceConfigurationCreateDialogPO createDeviceConfiguration = raceManagerApp.createDeviceConfiguration();
        createDeviceConfiguration.setDeviceName(DEVICE_CONFIG_NAME);
        createDeviceConfiguration.clickOkButtonOrThrow();
        DeviceConfigurationDetailsAreaPO deviceConfigurationDetails = raceManagerApp.getDeviceConfigurationDetails();
        DeviceConfigurationQRCodeDialogPO qrCodeDialog = deviceConfigurationDetails.openQRCodeDialog();
        Matcher<String> matcher = Matchers.matchesRegex(CHECK_RACE_APP_URL_REGEX);
        String createdInvitationUrl = qrCodeDialog.getUrl();
        MatcherAssert.assertThat("Check URL", matcher.matches(createdInvitationUrl));
        // "localhost" is not accepted by branch.io, so we need to replace by some artificial server;
        // that server will not have to be resolved during the test, so it can be a random string
        final String localNonLoopbackAddress = getNonLoopbackLocalhostAddress();
        final String nonLocalhostCreatedInvitationUrl = createdInvitationUrl.replace("localhost", localNonLoopbackAddress);
        HomePage.goToHomeUrl(getWebDriver(), nonLocalhostCreatedInvitationUrl);
        Wait<WebDriver> wait = new WebDriverWait(getWebDriver(), 30);
        // Confirm dialog will be shown on sapsailing.com to redirect back to localhost or non-default domain
//      wait.until(ExpectedConditions.numberOfElementsToBe(
//              By.xpath("//button[contains(text(), 'Yes')] | //button[contains(text(), 'Ja')]"), 1)).get(0).click();
        // here we are back on localhost (Home.html#QRCodePlace)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(text(), '" + EXPECTED_RACE_MANAGER_APP_QR_CODE_TITLE + "')]")));
        WebElement qrCodeLink = wait.until(ExpectedConditions
                .presenceOfElementLocated(By.xpath("//a[contains(text(), '" + EXPECTED_QR_LINK_TEXT + "')]")));
        Matcher<String> matcherWithNonLocalhostAddress = Matchers.matchesRegex(CHECK_RACE_APP_URL_REGEX.replace("localhost", getNonLoopbackLocalhostAddress()));
        MatcherAssert.assertThat("Check URL", matcherWithNonLocalhostAddress.matches(qrCodeLink.getAttribute("href")));
    }

    /**
     * Device registration via QR code test.
     */
    @SeleniumTestCase
    public void testDeviceRegistation() throws InterruptedException, SocketException {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        // create an event and regatta
        EventConfigurationPanelPO events = adminConsole.goToEvents();
        events.createEventWithDefaultLeaderboardGroupRegattaAndDefaultLeaderboard(BMW_CUP_EVENT, BMW_CUP_EVENTS_DESC,
                BMW_VENUE, BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, /* isPublic */ true, BMW_CUP_REGATTA,
                BMW_CUP_BOAT_CLASS, BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, /* useOverallLeaderboard */ false,
                CUSTOM_COURSE_AREA);
        RegattaStructureManagementPanelPO regattas = adminConsole.goToRegattaStructure();
        RegattaDescriptor regattaDescriptor = new RegattaDescriptor(AUDI_CUP_REGATTA, AUDI_CUP_BOAT_CLASS);
        regattas.createRegattaAndAddToEvent(regattaDescriptor, BMW_CUP_EVENT, new String[] { CUSTOM_COURSE_AREA });
        SmartphoneTrackingEventManagementPanelPO smartphoneTrackingPanel = adminConsole.goToSmartphoneTrackingPanel();
        DataEntryPO aLeaderboard = smartphoneTrackingPanel.getLeaderboardTable().getEntries().get(0);
        // Add a competitor with boat as precondition for adding devices
        RegisterCompetitorsDialogPO registerCompetitorsDialogPO = smartphoneTrackingPanel
                .pushCompetitorRegistrationsActionButton(aLeaderboard);
        AddCompetitorWithBoatDialogPO addCompetitorWithBoatDialogPO = registerCompetitorsDialogPO
                .openAddCompetitorWithBoatDialog();
        addCompetitorWithBoatDialogPO.addCompetitorWithBoat();
        Wait<WebDriver> wait = new WebDriverWait(getWebDriver(), 30);
        // wait until competitor is shown in the table. Sometimes test breaks here if dialog was closed to early
        wait.until((s) -> registerCompetitorsDialogPO.getCompetitorTable().getEntries().size() > 0);
        // now close the dialog
        registerCompetitorsDialogPO.clickOkButtonOrThrow();
        // Add device
        aLeaderboard = smartphoneTrackingPanel.getLeaderboardTable().getEntries().get(0);
        MapDevicesDialogPO mapDevicesDialog = smartphoneTrackingPanel.pushMapDevicesActionButton(aLeaderboard);
        AddDeviceMappingsDialogPO addDeviceMappingsDialog = mapDevicesDialog.addMapping();
        // check URL based on boat selection
        addDeviceMappingsDialog.getBoatsTable().getEntries().get(0).select();
        String boatUrlPattern = DEVICE_REGISTRATION_URL_REGEX + "boat_id.*";
        Matcher<String> boatMatcher = Matchers.matchesRegex(boatUrlPattern);
        MatcherAssert.assertThat("Check URL",
                boatMatcher.matches(addDeviceMappingsDialog.getQrCodeUrl(boatUrlPattern)));
        // check URL based on competitor selection
        addDeviceMappingsDialog.getCompetitorTable().getEntries().get(0).select();
        String competitorUrlPattern = DEVICE_REGISTRATION_URL_REGEX + "competitor_id.*";
        Matcher<String> competitorMatcher = Matchers.matchesRegex(competitorUrlPattern);
        String qrCodeUrl = addDeviceMappingsDialog.getQrCodeUrl(competitorUrlPattern);
        MatcherAssert.assertThat("Check URL", competitorMatcher.matches(qrCodeUrl));
        // "localhost" is not accepted by branch.io, so we need to replace by some artificial server;
        // that server will not have to be resolved during the test, so it can be a random string
        final String localNonLoopbackAddress = getNonLoopbackLocalhostAddress();
        final String nonLocalhostQRCodeUrl = qrCodeUrl.replace("localhost", localNonLoopbackAddress);
        // check redirect
        getWebDriver().get(nonLocalhostQRCodeUrl);
        // Confirm dialog will be shown on sapsailing.com to redirect back to localhost or non-default domain
//        wait.until(ExpectedConditions.numberOfElementsToBe(
//                By.xpath("//button[contains(text(), 'Yes')] | //button[contains(text(), 'Ja')]"), 1)).get(0).click();
        // here we are back on localhost (Home.html#QRCodePlace)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(text(), '" + EXPECTED_DEVICE_REGISTRATION_QR_CODE_TITLE + "')]")));
        WebElement qrCodeLink = wait.until(ExpectedConditions
                .presenceOfElementLocated(By.xpath("//a[contains(text(), '" + EXPECTED_QR_LINK_TEXT + "')]")));
        Matcher<String> matcher = Matchers.matchesRegex(competitorUrlPattern);
        MatcherAssert.assertThat("Check URL", matcher.matches(qrCodeLink.getAttribute("href")));
    }
}
