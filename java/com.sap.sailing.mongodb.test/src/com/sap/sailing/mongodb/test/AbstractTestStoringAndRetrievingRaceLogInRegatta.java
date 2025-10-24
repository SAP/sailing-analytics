package com.sap.sailing.mongodb.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class AbstractTestStoringAndRetrievingRaceLogInRegatta extends RaceLogMongoDBTest {

    protected final String raceColumnName = "My.First$Race$1";
    protected final String regattaName = "TestRegatta";
    protected final String yellowFleetName = "Yellow";
    protected final String seriesName = "Qualifying";
    protected MongoObjectFactory mongoObjectFactory = null;
    protected DomainObjectFactory domainObjectFactory = null;
    protected Regatta regatta = null;
    protected AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);

    public AbstractTestStoringAndRetrievingRaceLogInRegatta() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    public void setUp() {
        now = MillisecondsTimePoint.now();
        mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        regatta = createRegattaAndAddRaceColumns(1, regattaName, boatClass, true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
    }
    
    private Regatta createRegattaAndAddRaceColumns(final int numberOfQualifyingRaces, final String regattaBaseName, BoatClass boatClass, boolean persistent, ScoringScheme scoringScheme) {
        Regatta regatta = createRegattaWithoutRaceColumns(regattaBaseName, boatClass, persistent, scoringScheme, null);
        regatta.getSeriesByName(seriesName).addRaceColumn(raceColumnName, /* trackedRegattaRegistry */ null);
        return regatta;
    }
    
    private Regatta createRegattaWithoutRaceColumns(final String regattaBaseName, BoatClass boatClass,
            boolean persistent, ScoringScheme scoringScheme, CourseArea courseArea) {
        List<Series> series = createSeriesForTestRegatta();
        Regatta regatta = new RegattaImpl(getRaceLogStore(), getRegattaLogStore(), RegattaImpl.getDefaultName(regattaBaseName,
                boatClass == null ? null : boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, series, persistent, scoringScheme, "123",
                courseArea, /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false,
                OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    private List<Series> createSeriesForTestRegatta() {
        List<Series> series = new ArrayList<Series>();
        // -------- qualifying series ------------
        List<String> emptyRaceColumnNames = Collections.emptyList();
        List<Fleet> qualifyingFleets = createQualifyingFleets();
        Series qualifyingSeries = new SeriesImpl(seriesName, /* isMedal */false, /* isFleetsCanRunInParallel */ true, qualifyingFleets,
                emptyRaceColumnNames, /* trackedRegattaRegistry */ null);
        series.add(qualifyingSeries);
        return series;
    }

    abstract protected List<Fleet> createQualifyingFleets();
}
