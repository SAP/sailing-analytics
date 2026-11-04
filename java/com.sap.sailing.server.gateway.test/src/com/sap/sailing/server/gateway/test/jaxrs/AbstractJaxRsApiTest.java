package com.sap.sailing.server.gateway.test.jaxrs;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.mockito.Mockito;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneUuidServiceFinderFactory;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.resultimport.impl.ScoreCorrectionProviderImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;

public abstract class AbstractJaxRsApiTest {
    protected RacingEventService racingEventService;
    protected MongoDBService service;
    protected SecurityService securityService;
    protected DummyEventsRessource eventsResource;
    protected DummyRegattasResource regattasResource;
    protected DummyLeaderboardGroupsResource leaderboardGroupsResource;
    protected DummyLeaderboardsResource leaderboardsResource;
    protected DummyBoatsResource boatsResource;
    protected DummyCompetitorsResource competitorsResource;
    private ScoreCorrectionProvider xrrScoreCorrectionProvider;

    protected static SimpleDateFormat TIMEPOINT_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public void setUp() throws Exception {
        service = MongoDBConfiguration.getDefaultTestConfiguration().getService();
        service.getDB().drop();
        xrrScoreCorrectionProvider = new ScoreCorrectionProviderImpl(mock(ResultDocumentProvider.class), ParserFactory.INSTANCE);
        racingEventService = Mockito.spy(new RacingEventServiceImpl(/* clearPersistentCompetitorStore */ true,
                /* sensorFixStore */ null, new MockSmartphoneUuidServiceFinderFactory(), /* restoreTrackedRaces */ false));
        SecurityManager securityManager = Mockito.mock(org.apache.shiro.mgt.SecurityManager.class);
        Subject fakeSubject = Mockito.mock(Subject.class);
        Mockito.doReturn(true).when(fakeSubject).isAuthenticated();
        SecurityUtils.setSecurityManager(securityManager);
        Mockito.doReturn(fakeSubject).when(securityManager).createSubject(Mockito.any());
        securityService = SecurityServiceMockFactory.mockSecurityService();
        OwnershipAnnotation mockedOwnership = Mockito.mock(OwnershipAnnotation.class);
        Ownership ownership = Mockito.mock(Ownership.class);
        Mockito.doReturn(mockedOwnership).when(securityService).getOwnership(Mockito.any());
        Mockito.doReturn(ownership).when(mockedOwnership).getAnnotation();
        eventsResource = spyResource(new DummyEventsRessource());
        doReturn(getSecurityService()).when(eventsResource).getSecurityService();
        regattasResource = spyResource(new DummyRegattasResource());
        doReturn(getSecurityService()).when(regattasResource).getSecurityService();
        leaderboardGroupsResource = spyResource(new DummyLeaderboardGroupsResource());
        doReturn(getSecurityService()).when(leaderboardGroupsResource).getSecurityService();
        leaderboardsResource = spyResource(new DummyLeaderboardsResource());
        doReturn(getSecurityService()).when(leaderboardsResource).getSecurityService();
        doReturn(Optional.of(xrrScoreCorrectionProvider)).when(leaderboardsResource).getScoreCorrectionProvider("ISAF XML Regatta Result (XRR) Importer");
        boatsResource = spyResource(new DummyBoatsResource());
        doReturn(getSecurityService()).when(boatsResource).getSecurityService();
        competitorsResource = spyResource(new DummyCompetitorsResource());
        doReturn(getSecurityService()).when(competitorsResource).getSecurityService();
        doReturn(racingEventService).when(competitorsResource).getService();
        doReturn(RaceLogTrackingAdapterFactory.INSTANCE.getAdapter(racingEventService.getBaseDomainFactory()))
                .when(leaderboardsResource).getRaceLogTrackingAdapter();
    }

    public ThreadState setUser(User user) {
        Subject subject = Mockito.mock(Subject.class);
        if (user == null) {
            Mockito.when(subject.isAuthenticated()).thenReturn(false);
            Mockito.when(subject.getPrincipal()).thenReturn(null);
        } else {
            Mockito.when(subject.isAuthenticated()).thenReturn(true);
            Mockito.when(subject.getPrincipal()).thenReturn(user.getName());
        }
        Mockito.when(securityService.getCurrentUser()).thenReturn(user);
        ThreadState subjectThreadState = new SubjectThreadState(subject);
        subjectThreadState.bind();
        return subjectThreadState;
    }

    public SecurityService getSecurityService() {
        return securityService;
    }
    
    protected <T extends AbstractSailingServerResource> T spyResource(T resource) {
        T spyResource = spy(resource);
        doReturn(racingEventService).when(spyResource).getService();
        return spyResource;
    }    
    
    protected String decodeResponseFromByteArray(Response response) throws UnsupportedEncodingException {
        byte[] entity = (byte[]) response.getEntity();
        return new String(entity, "UTF-8");
    }

    protected TimePoint parseTimepointFromJsonNumber(Long timePointAsJsonNumber) throws ParseException {
        TimePoint result = null;
        if (timePointAsJsonNumber != null) {
            result = new MillisecondsTimePoint(timePointAsJsonNumber);
        }
        return result;
    }

    protected TimePoint parseTimepointFromJsonString(String timePointAsJsonString) throws ParseException {
        TimePoint result = null;
        if (timePointAsJsonString != null && !timePointAsJsonString.isEmpty()) {
            Date date = TIMEPOINT_FORMATTER.parse(timePointAsJsonString);
            result = new MillisecondsTimePoint(date);
        }
        return result;
    }
     
    protected List<Competitor> createCompetitors(int numberOfCompetitorsToCreate) {
        List<Competitor> result = new ArrayList<Competitor>();
        for (int i = 1; i <= numberOfCompetitorsToCreate; i++) {
            String competitorName = "C" + i;
            Competitor competitor = new CompetitorImpl(Integer.valueOf(i), competitorName, "KYC"+i, Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                                    new PersonImpl(competitorName, new NationalityImpl("GER"),
                                            /* dateOfBirth */ null, "This is famous "+competitorName)),
                                            new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                                            /* dateOfBirth */null, "This is Rigo, the coach")), 
                                    /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null); 
            result.add(competitor);
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    protected Callable<String> getAnyCallable() {
        return Mockito.any(Callable.class);
    }
}
