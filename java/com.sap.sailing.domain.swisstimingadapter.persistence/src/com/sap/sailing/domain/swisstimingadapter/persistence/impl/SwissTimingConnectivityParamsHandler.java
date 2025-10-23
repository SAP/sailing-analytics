package com.sap.sailing.domain.swisstimingadapter.persistence.impl;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.BasicDBList;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.CrewMember;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.impl.CompetitorWithID;
import com.sap.sailing.domain.swisstimingadapter.impl.CompetitorWithoutID;
import com.sap.sailing.domain.swisstimingadapter.impl.CrewMemberImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.StartListImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingTrackingConnectivityParameters;
import com.sap.sailing.domain.swisstimingadapter.persistence.SwissTimingAdapterPersistence;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.impl.AbstractRaceTrackingConnectivityParametersHandler;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionUtils;

/**
 * Handles mapping TracTrac connectivity parameters from and to a map with {@link String} keys. The
 * "param URL" is considered the {@link #getKey(RaceTrackingConnectivityParameters) key} for these objects.<p>
 * 
 * Lives in the same package as {@link RaceTrackingConnectivityParameters} for package-private access to
 * its members.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SwissTimingConnectivityParamsHandler extends AbstractRaceTrackingConnectivityParametersHandler {
    private static final String CREW_MEMBER_POSITION = "crewMemberPosition";
    private static final String CREW_MEMBER_NATIONALITY = "crewMemberNationality";
    private static final String CREW_MEMBER_NAME = "crewMemberName";
    private static final String COMPETITOR_CREW = "competitorCrew";
    private static final String COMPETITOR_THREE_LETTER_IOC_CODE = "competitorThreeLetterIocCode";
    private static final String COMPETITOR_NAME = "competitorName";
    private static final String COMPETITOR_BOAT_ID = "competitorBoatId";
    private static final String COMPETITOR_ID_AS_STRING = "competitorId"; // the competitor ID as String
    private static final String USE_INTERNAL_MARK_PASSING_ALGORITHM = "useInternalMarkPassingAlgorithm";
    private static final String DELAY_TO_LIVE_IN_MILLIS = "delayToLiveInMillis";
    private static final String START_LIST = "startList";
    private static final String RACE_NAME = "raceName";
    private static final String RACE_ID = "raceID";
    private static final String RACE_DESCRIPTION = "raceDescription";
    private static final String PORT = "port";
    private static final String BOAT_CLASS_NAME = "boatClassName";
    private static final String HOSTNAME = "hostname";
    private static final String UPDATE_URL = "updateURL";
    private static final String API_TOKEN = "apiToken";
    private static final String EVENT_NAME = "eventName";
    private static final String MANAGE2SAIL_EVENT_URL = "manage2SailEventUrl";
    private final RaceLogStore raceLogStore;
    private final RegattaLogStore regattaLogStore;
    private final DomainFactory domainFactory;
    private final SwissTimingFactory swissTimingFactory;
    private final SecurityService securityService;

    public SwissTimingConnectivityParamsHandler(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, DomainFactory domainFactory, SecurityService securityService) {
        super();
        this.raceLogStore = raceLogStore;
        this.regattaLogStore = regattaLogStore;
        this.domainFactory = domainFactory;
        this.securityService = securityService;
        this.swissTimingFactory = SwissTimingFactory.INSTANCE;
    }

    @Override
    public Map<String, Object> mapFrom(RaceTrackingConnectivityParameters params) {
        assert params instanceof SwissTimingTrackingConnectivityParameters;
        final SwissTimingTrackingConnectivityParameters stParams = (SwissTimingTrackingConnectivityParameters) params;
        final Map<String, Object> result = getKey(params);
        result.put(BOAT_CLASS_NAME, stParams.getBoatClass()==null?null:stParams.getBoatClass().getName());
        result.put(RACE_DESCRIPTION, stParams.getRaceDescription());
        result.put(RACE_NAME, stParams.getRaceName());
        result.put(START_LIST, createStartListDBObject(stParams));
        result.put(DELAY_TO_LIVE_IN_MILLIS, stParams.getDelayToLiveInMillis());
        result.put(USE_INTERNAL_MARK_PASSING_ALGORITHM, stParams.isUseInternalMarkPassingAlgorithm());
        result.put(UPDATE_URL, stParams.getUpdateURL());
        result.put(API_TOKEN, stParams.getApiToken());
        result.put(EVENT_NAME, stParams.getEventName());
        result.put(MANAGE2SAIL_EVENT_URL, stParams.getManage2SailEventUrl());
        addWindTrackingParameters(stParams, result);
        return result;
    }

    private BasicDBList createStartListDBObject(final SwissTimingTrackingConnectivityParameters stParams) {
        final BasicDBList startListDbObject;
        if (stParams.getStartList() != null) {
            startListDbObject = new BasicDBList();
            for (final Competitor competitor : stParams.getStartList().getCompetitors()) {
                Document competitorDBObject = createCompetitorDBObject(competitor);
                startListDbObject.add(competitorDBObject);
            }
        } else {
            startListDbObject = null;
        }
        return startListDbObject;
    }

    private StartList createStartListFromDBObject(String raceId, Iterable<?> startListDBObject) {
        List<Competitor> competitors = new ArrayList<>();
        for (final Object competitorObject : startListDBObject) {
            final Document competitorDBObject = (Document) competitorObject;
            competitors.add(createCompetitorFromDBObject(competitorDBObject));
        }
        return new StartListImpl(raceId, competitors);
    }
    
    private Document createCompetitorDBObject(Competitor competitor) {
        final Document result = new Document();
        result.put(COMPETITOR_ID_AS_STRING, competitor.getIdAsString());
        result.put(COMPETITOR_BOAT_ID, competitor.getBoatID());
        result.put(COMPETITOR_NAME, competitor.getName());
        result.put(COMPETITOR_THREE_LETTER_IOC_CODE, competitor.getThreeLetterIOCCode());
        result.put(COMPETITOR_CREW, createCrewDbObject(competitor.getCrew()));
        return result;
    }

    private BasicDBList createCrewDbObject(List<CrewMember> crew) {
        final BasicDBList result;
        if (crew == null) {
            result = null;
        } else {
            result = new BasicDBList();
            for (final CrewMember crewMember : crew) {
                final Document crewMemberDBObject = new Document();
                crewMemberDBObject.put(CREW_MEMBER_NAME, crewMember.getName());
                crewMemberDBObject.put(CREW_MEMBER_NATIONALITY, crewMember.getNationality());
                crewMemberDBObject.put(CREW_MEMBER_POSITION, crewMember.getPosition());
                result.add(crewMemberDBObject);
            }
        }
        return result;
    }

    private List<CrewMember> createCrewFromDBObject(Iterable<?> crewDBObject) {
        final List<CrewMember> result;
        if (crewDBObject == null) {
            result = null;
        } else {
            result = new ArrayList<>();
            for (final Object o : crewDBObject) {
                final Document crewMemberDBObject = (Document) o;
                result.add(new CrewMemberImpl((String) crewMemberDBObject.get(CREW_MEMBER_NAME),
                                              (String) crewMemberDBObject.get(CREW_MEMBER_NATIONALITY),
                                              (String) crewMemberDBObject.get(CREW_MEMBER_POSITION)));
            }
        }
        return result;
    }

    private Competitor createCompetitorFromDBObject(Document competitorDBObject) {
        final Competitor result;
        final String competitorIdAsString = (String) competitorDBObject.get(COMPETITOR_ID_AS_STRING);
        final String boatID = (String) competitorDBObject.get(COMPETITOR_BOAT_ID);
        final String threeLetterIOCCode = (String) competitorDBObject.get(COMPETITOR_THREE_LETTER_IOC_CODE);
        final String name = (String) competitorDBObject.get(COMPETITOR_NAME);
        final Iterable<?> crewDBObject = (Iterable<?>) competitorDBObject.get(COMPETITOR_CREW);
        if (competitorIdAsString == null) {
            result = new CompetitorWithoutID(boatID, threeLetterIOCCode, name);
        } else {
            result = new CompetitorWithID(competitorIdAsString, boatID, threeLetterIOCCode, name, createCrewFromDBObject(crewDBObject));
        }
        return result;
    }
    
    @Override
    public RaceTrackingConnectivityParameters mapTo(Map<String, Object> map) throws MalformedURLException, URISyntaxException {
        return new SwissTimingTrackingConnectivityParameters(
                (String) map.get(HOSTNAME),
                ((Number) map.get(PORT)).intValue(),
                (String) map.get(RACE_ID),
                (String) map.get(RACE_NAME),
                (String) map.get(RACE_DESCRIPTION),
                domainFactory.getBaseDomainFactory().getOrCreateBoatClass((String) map.get(BOAT_CLASS_NAME)),
                map.get(START_LIST) == null ? null : createStartListFromDBObject((String) map.get(RACE_ID), (Iterable<?>) map.get(START_LIST)),
                ((Number) map.get(DELAY_TO_LIVE_IN_MILLIS)).longValue(),
                swissTimingFactory, domainFactory, raceLogStore, regattaLogStore,
                (boolean) map.get(USE_INTERNAL_MARK_PASSING_ALGORITHM), isTrackWind(map), isCorrectWindDirectionByMagneticDeclination(map),
                (String) map.get(UPDATE_URL),
                (String) map.get(API_TOKEN),
                (String) map.get(EVENT_NAME),
                (String) map.get(MANAGE2SAIL_EVENT_URL));
    }

    @Override
    public Map<String, Object> getKey(RaceTrackingConnectivityParameters params) {
        assert params instanceof SwissTimingTrackingConnectivityParameters;
        final SwissTimingTrackingConnectivityParameters stParams = (SwissTimingTrackingConnectivityParameters) params;
        final Map<String, Object> result = new HashMap<>();
        result.put(TypeBasedServiceFinder.TYPE, params.getTypeIdentifier());
        result.put(HOSTNAME, stParams.getHostname());
        result.put(PORT, stParams.getPort());
        result.put(RACE_ID, stParams.getRaceID());
        return result;
    }

    @Override
    public RaceTrackingConnectivityParameters resolve(RaceTrackingConnectivityParameters params) throws Exception {
        assert params instanceof SwissTimingTrackingConnectivityParameters;
        final SwissTimingTrackingConnectivityParameters stParams = (SwissTimingTrackingConnectivityParameters) params;
        SwissTimingTrackingConnectivityParameters result = new SwissTimingTrackingConnectivityParameters(
                stParams.getHostname(), stParams.getPort(), stParams.getRaceID(), stParams.getRaceName(),
                stParams.getRaceDescription(), stParams.getBoatClass(), stParams.getStartList(),
                stParams.getDelayToLiveInMillis(), swissTimingFactory, domainFactory, raceLogStore, regattaLogStore,
                stParams.isUseInternalMarkPassingAlgorithm(), stParams.isTrackWind(),
                stParams.isCorrectWindDirectionByMagneticDeclination(), stParams.getUpdateURL(),
                stParams.getApiToken(), stParams.getEventName(), stParams.getManage2SailEventUrl());
        final String creatorName = SessionUtils.getPrincipal().toString();
        if (result.getManage2SailEventUrl() != null) { // legacy records won't have this URL stored in their connectivity params
            final SwissTimingConfiguration swissTimingConfiguration = SwissTimingFactory.INSTANCE
                    .createSwissTimingConfiguration(result.getEventName(), result.getManage2SailEventUrl(), result.getHostname(), result.getPort(), result.getUpdateURL(),
                            result.getApiToken(), creatorName);
            SwissTimingAdapterPersistence.INSTANCE.updateSwissTimingConfiguration(swissTimingConfiguration);
            securityService.setDefaultOwnershipIfNotSet(swissTimingConfiguration.getIdentifier());
        }
        return result;
    }
}
