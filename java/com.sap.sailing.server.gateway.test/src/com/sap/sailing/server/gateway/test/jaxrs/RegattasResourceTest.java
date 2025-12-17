package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimedLockImpl;
import com.sap.sse.rest.StreamingOutputUtil;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.impl.User;

public class RegattasResourceTest extends AbstractJaxRsApiTest {
    private final String boatClassName = "49er";
    private final String closedRegattaNamePart = "TestRegatta";
    private final String openRegattaNamePart = "TestOpenRegatta";
    private final String closedRegattaName = RegattaImpl.getDefaultName(closedRegattaNamePart, boatClassName);
    private final String openRegattaName = RegattaImpl.getDefaultName(openRegattaNamePart, boatClassName);
    private final String deviceUuid = "00000000-1111-2222-3333-444444444444";
    private final String deviceUuid2 = "00000000-1111-2222-3333-444444444445";
    private final String secret = "ABCDEF";
    private final String competitorName1 = "Max Mustermann";
    private final String competitorShortName1 = "MM";
    private final String competitorName2 = "Test Competitor";
    private final String competitorShortName2 = "TC";
    private final String flagImageUri = "file://flag.jpg";
    private final String teamImageUri = "file://team.jpg";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        List<Series> series = new ArrayList<Series>();
        List<Fleet> fleets = new ArrayList<Fleet>();
        List<String> raceColumnNames = new ArrayList<String>();
        fleets.add(new FleetImpl("Fleet1"));
        fleets.add(new FleetImpl("Fleet2"));
        final Calendar cal = new GregorianCalendar();
        cal.set(2014, 5, 6, 10, 00);
        final TimePoint startDate = new MillisecondsTimePoint(cal.getTime());
        cal.set(2014, 5, 8, 16, 00);
        final TimePoint endDate = new MillisecondsTimePoint(cal.getTime());
        Series testSeries = new SeriesImpl("TestSeries", /* isMedal */false, /* isFleetsCanRunInParallel */ true,
                fleets, raceColumnNames, /* trackedRegattaRegistry */ null);
        series.add(testSeries);
        final UUID closedRegattaUuid = UUID.randomUUID();
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.REGATTA,
                Regatta.getTypeRelativeObjectIdentifier(closedRegattaName), closedRegattaName, new Callable<Regatta>() {
                    @Override
                    public Regatta call() throws Exception {
                        return racingEventService.createRegatta(closedRegattaName, boatClassName,
                                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                                /* registrationLinkSecret */ null, startDate, endDate, closedRegattaUuid, series,
                                /* persistent */ true,
                                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                                /* course area ID */ (Serializable) null,
                                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
                    }
                });
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.REGATTA,
                Regatta.getTypeRelativeObjectIdentifier(openRegattaName), openRegattaName, new Callable<Regatta>() {
                    @Override
                    public Regatta call() throws Exception {
                        return racingEventService.createRegatta(openRegattaName, boatClassName,
                                /* canBoatsOfCompetitorsChangePerRace */ true,
                                CompetitorRegistrationType.OPEN_UNMODERATED, secret, startDate, endDate,
                                UUID.randomUUID(), series, /* persistent */ true,
                                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                                /* course area ID */ (Serializable) null,
                                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
                    }
                });
        testSeries.addRaceColumn("R1", /* trackedRegattaRegistry */ null);
        testSeries.addRaceColumn("R2", /* trackedRegattaRegistry */ null);
        Course course = new CourseImpl("emptyCourse", Collections.emptySet());
        // get the same instance! of the boat class object, as else addRace will fail
        BoatClass boatClass = racingEventService.getBaseDomainFactory().getOrCreateBoatClass(boatClassName);
        racingEventService.addRace(new RegattaName(closedRegattaName),
                new RaceDefinitionImpl("Race 1", course, boatClass));
    }

    @Test
    public void testGetRegattas() throws Exception {
        Response regattasResponse = regattasResource.getRegattas();
        String jsonString = StreamingOutputUtil.getEntityAsString(regattasResponse.getEntity());
        Object obj = JSONValue.parse(jsonString);
        JSONArray array = (JSONArray) obj;
        assertTrue(array.size() == 2);
        JSONObject firstElement = (JSONObject) array.get(0);
        String jsonName = (String) firstElement.get("name");
        String jsonBoatClass = (String) firstElement.get("boatclass");
        assertTrue(closedRegattaName.equals(jsonName) || openRegattaName.equals(jsonName));
        assertTrue(boatClassName.equals(jsonBoatClass));
    }

    @Test
    public void testNullCheckForTrackedRaceInGetManeuvers() throws Exception {
        Response response = regattasResource.getManeuvers(closedRegattaName, "Race 1", null, null);
        // the current race is not tracked, expect an error
        assertTrue(response.getStatus() != 200);
    }

    @Test
    public void testGetRegatta() throws Exception {
        Response regattaResponse = regattasResource.getRegatta(closedRegattaName, null);
        String jsonString = StreamingOutputUtil.getEntityAsString(regattaResponse.getEntity());
        assertNotNull(jsonString);
        String readRegattaName = (String) ((JSONObject) JSONValue.parse(jsonString)).get("name");
        assertEquals(closedRegattaName, readRegattaName);
    }

    @Test
    public void testCompetitorRegistrationByAdmin() throws Exception {
        doReturn(securityService).when(regattasResource).getService(SecurityService.class);
        doReturn(true).when(securityService).hasCurrentUserUpdatePermission(Mockito.any());
        User user = new UserImpl("admin", "noreply@sapsailing.com", null, new ArrayList<Account>(0), null, new TimedLockImpl());
        setUser(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        Response response = regattasResource.createAndAddCompetitor(closedRegattaName, boatClassName, null, "GER",
                "#F00", flagImageUri, teamImageUri, null, null, null, competitorName1, competitorShortName1, null,
                deviceUuid, null);
        assertTrue(response.getStatus() == Status.OK.getStatusCode(),
                response.getStatus() + ": " + StreamingOutputUtil.getEntityAsString(response.getEntity()));
        assertTrue(regattasResource.getService() == racingEventService);
        response = regattasResource.createAndAddCompetitor(closedRegattaName, boatClassName, null, "GER", "#0F0",
                flagImageUri, teamImageUri, null, null, null, competitorName2, competitorShortName2, null, deviceUuid2,
                null);
        assertTrue(response.getStatus() == Status.OK.getStatusCode(),
                response.getStatus() + ": " + StreamingOutputUtil.getEntityAsString(response.getEntity()));
        Regatta regatta = racingEventService.getRegattaByName(closedRegattaName);
        Iterator<Competitor> cit = regatta.getAllCompetitors().iterator();
        Competitor readCompetitor = cit.next();
        assertNotNull(readCompetitor);
        assertTrue(competitorName1.equals(readCompetitor.getName()) || competitorName2.equals(readCompetitor.getName()));
        readCompetitor = cit.next();
        assertNotNull(readCompetitor);
        assertTrue(competitorName1.equals(readCompetitor.getName()) || competitorName2.equals(readCompetitor.getName()));
        Iterator<Competitor> citForRemove = regatta.getAllCompetitors().iterator();
        citForRemove.forEachRemaining(competitor -> regattasResource.removeCompetitor(closedRegattaName,
                competitor.getId().toString(), null));
        Iterator<Competitor> citAfterRemove = regatta.getAllCompetitors().iterator();
        assertTrue(!citAfterRemove.hasNext(), "Competitors still exist after remove.");
    }

    @Test
    public void testCompetitorRegistrationAnonymousOnOpenRegatta() throws Exception {
        doReturn(securityService).when(regattasResource).getService(SecurityService.class);
        setUser(null);
        Response response = regattasResource.createAndAddCompetitor(openRegattaName, boatClassName, null, "GER", "#F00",
                flagImageUri, teamImageUri, null, null, null, competitorName1, competitorShortName1, null, deviceUuid,
                secret);
        assertTrue(response.getStatus() == Status.OK.getStatusCode(),
                response.getStatus() + ": " + StreamingOutputUtil.getEntityAsString(response.getEntity()));
        assertTrue(regattasResource.getService() == racingEventService);

        Regatta regatta = racingEventService.getRegattaByName(openRegattaName);
        testResponseOfOpenRegattaCompetitorRegistration(regatta);
    }

    @Test
    public void testCompetitorRegistrationAnonymousOnOpenRegattaWrongSecret() throws Exception {
        doReturn(securityService).when(regattasResource).getService(SecurityService.class);
        setUser(null);
        Response response = regattasResource.createAndAddCompetitor(openRegattaName, boatClassName, null, "GER", "#F00",
                flagImageUri, teamImageUri, null, null, null, competitorName1, competitorShortName1, null, deviceUuid,
                "WRONGSECRET");
        assertTrue(response.getStatus() == Status.FORBIDDEN.getStatusCode(),
                response.getStatus() + ": " + StreamingOutputUtil.getEntityAsString(response.getEntity()));
    }

    @Test
    public void testCompetitorRegistrationAuthenticatedOnOpenRegatta() throws Exception {
        doReturn(securityService).when(regattasResource).getService(SecurityService.class);
        User user = new UserImpl("max", "noreply@sapsailing.com", null, new ArrayList<Account>(0), null, new TimedLockImpl());
        setUser(user);
        Regatta regatta = racingEventService.getRegattaByName(openRegattaName);
        Response response = regattasResource.createAndAddCompetitor(openRegattaName, boatClassName, null, "GER", "#F00",
                flagImageUri, teamImageUri, null, null, null, competitorName1, competitorShortName1, null, deviceUuid,
                secret);
        assertTrue(response.getStatus() == Status.OK.getStatusCode(),
                response.getStatus() + ": " + StreamingOutputUtil.getEntityAsString(response.getEntity()));
        assertTrue(regattasResource.getService() == racingEventService);
        regatta = racingEventService.getRegattaByName(openRegattaName);
        testResponseOfOpenRegattaCompetitorRegistration(regatta);
    }

    private void testResponseOfOpenRegattaCompetitorRegistration(final Regatta regatta) throws URISyntaxException {
        Iterator<Competitor> cit = regatta.getAllCompetitors().iterator();
        Competitor readCompetitor = cit.next();
        assertNotNull(readCompetitor);

        // should have one device registration event of type RegattaLogDeviceCompetitorMappingEventImpl...
        assertEquals(1, regatta.getRegattaLog().getUnrevokedEvents().stream()
                .filter(e -> e instanceof RegattaLogDeviceCompetitorMappingEvent).count());
        // ...with requested device id
        assertEquals(deviceUuid,
                ((RegattaLogDeviceCompetitorMappingEvent) regatta.getRegattaLog().getUnrevokedEvents().stream()
                        .filter(e -> e instanceof RegattaLogDeviceCompetitorMappingEvent).findFirst().get()).getDevice()
                                .getStringRepresentation());
        assertEquals(Color.RED, readCompetitor.getColor());
        assertEquals(competitorName1, readCompetitor.getName());
        assertEquals(competitorShortName1, readCompetitor.getShortName());
        assertEquals(new URI(flagImageUri), readCompetitor.getFlagImage());
        assertEquals(new URI(teamImageUri), readCompetitor.getTeam().getImage());

        // Same deviceUuid for registration should fail
        Response response = regattasResource.createAndAddCompetitor(regatta.getName(), boatClassName, null, "GER",
                "#F00", flagImageUri, teamImageUri, null, null, null, "XXX", "XXX", null, deviceUuid, secret);
        assertTrue(response.getStatus() == Status.FORBIDDEN.getStatusCode(), "Reponse http status for duplicate competitor device should be forbidden (403) but is "
                        + response.getStatus());

        // Remove competitor registration
        Response removeResponse = regattasResource.removeCompetitor(regatta.getName(),
                readCompetitor.getId().toString(), secret);
        assertEquals(200, removeResponse.getStatus(), "Remove of competitor failed");
    }
}
