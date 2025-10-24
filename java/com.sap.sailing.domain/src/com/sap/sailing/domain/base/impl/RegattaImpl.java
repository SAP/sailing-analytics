package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterBoatEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogBoatDeregistrator;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogBoatsInLogAnalyzer;
import com.sap.sailing.domain.abstractlog.shared.analyzing.CompetitorDeregistrator;
import com.sap.sailing.domain.abstractlog.shared.analyzing.CompetitorsAndBoatsInLogAnalyzer;
import com.sap.sailing.domain.abstractlog.shared.analyzing.CompetitorsInLogAnalyzer;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaListener;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.AbstractLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.CompetitorProviderFromRaceColumnsAndRegattaLike;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.regattalike.BaseRegattaLikeImpl;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.regattalike.RegattaAsRegattaLikeIdentifier;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.regattalike.RegattaLikeListener;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.RaceExecutionOrderProvider;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.util.impl.RaceColumnListeners;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.NamedImpl;
import com.sap.sse.metering.CPUMeter;

/**
 * A regatta with series with race columns and fleets, a scoring scheme and various other data, many of which are relevant
 * for scoring. A regatta has a single boat class. It implements the {@link IsRegattaLike} interface and for this uses a
 * delegate pattern with the {@link BaseRegattaLikeImpl} which it shares with {@link FlexibleLeaderboardImpl}. That
 * delegate provides the {@link RegattaLog} for this regatta.
 *
 * @author Axel Uhl (D043530)
 *
 */
public class RegattaImpl extends NamedImpl implements Regatta, RaceColumnListener {

    /**
     * Used during master data import to handle connection to correct RaceLogStore
     */
    private static transient ThreadLocal<MasterDataImportInformation> ongoingMasterDataImportInformation = new ThreadLocal<MasterDataImportInformation>() {
        @Override
        protected MasterDataImportInformation initialValue() {
            return null;
        };
    };

    public static void setOngoingMasterDataImport(MasterDataImportInformation information) {
        ongoingMasterDataImportInformation.set(information);
    }

    private static final Logger logger = Logger.getLogger(RegattaImpl.class.getName());
    private static final long serialVersionUID = 6509564189552478869L;
    private ConcurrentMap<String, RaceDefinition> races;
    private final BoatClass boatClass;
    private transient Set<RegattaListener> regattaListeners;
    private List<? extends Series> series;
    private final RaceColumnListeners raceColumnListeners;
    private final ScoringScheme scoringScheme;
    private TimePoint startDate;
    private TimePoint endDate;
    private final Serializable id;
    private transient RaceLogStore raceLogStore;
    private final IsRegattaLike regattaLikeHelper;
    private final RankingMetricConstructor rankingMetricConstructor;
    private Double buoyZoneRadiusInHullLengths;

    /**
     * A synchronized list; synchronize on the object monitor before iterating or modifying
     */
    private List<CourseArea> courseAreas;

    private RegattaConfiguration configuration;
    private RaceExecutionOrderCache raceExecutionOrderCache;

    private String registrationLinkSecret;

    /**
     * Regattas may be constructed as implicit default regattas in which case they won't need to be stored durably and
     * don't contain valuable information worth being preserved; or they are constructed explicitly with series and race
     * columns in which case this data needs to be protected. This flag indicates whether the data of this regatta needs
     * to be maintained persistently.
     *
     * @see #isPersistent
     */
    private final boolean persistent;

    /**
     * When a regatta has well-managed {@link TrackedRace#getStartOfRace() start} and
     * {@link TrackedRace#getFinishedTime() finish times} it can make sense to drive the tracking infrastructure based
     * on these times. This may include automatically starting the tracking for a race a certain number of minutes
     * before the race starts, and finishing the tracking some time after the race has finished. This capability can be
     * activated using this flag. Tracking connectors can optionally evaluate it and take measures to drive their
     * {@link RaceTracker} and adjust start and end of tracking times accordingly.
     * <p>
     *
     * See also
     * <a href="https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=3588">https://bugzilla.sapsailing.com/bugzilla/
     * show_bug.cgi?id=3588</a>.
     */
    private boolean controlTrackingFromStartAndFinishTimes;

    private boolean autoRestartTrackingUponCompetitorSetChange;

    private boolean canBoatsOfCompetitorsChangePerRace;

    private CompetitorRegistrationType competitorRegistrationType;

    /**
     * Defaults to <code>true</code>. See {@link Regatta#useStartTimeInference()}.
     */
    private boolean useStartTimeInference;

    private transient CompetitorProviderFromRaceColumnsAndRegattaLike competitorsProvider;

    private AbstractLogEventAuthor regattaLogEventAuthorForRegatta = new LogEventAuthorImpl(
            AbstractLeaderboardImpl.class.getName(), 0);

    private transient CPUMeter cpuMeter;
    
    /**
     * Constructs a regatta with an empty {@link RaceLogStore} and with
     * {@link Regatta#isControlTrackingFromStartAndFinishTimes()} and
     * {@link #isAutoRestartTrackingUponCompetitorSetChange()} set to {@code false}.
     */
    public RegattaImpl(String name, BoatClass boatClass, boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType,
            TimePoint startDate, TimePoint endDate,
            Iterable<? extends Series> series, boolean persistent, ScoringScheme scoringScheme, Serializable id,
            CourseArea courseArea, RankingMetricConstructor rankingMetricConstructor, String registrationLinkSecret) {
        this(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, name, boatClass, canBoatsOfCompetitorsChangePerRace, competitorRegistrationType,
                startDate, endDate, series,
                persistent, scoringScheme, id, courseArea, 0.0, /* useStartTimeInference */true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, rankingMetricConstructor, registrationLinkSecret);
    }

    /**
     * Constructs a regatta with a single default series with empty race column list, and a single default fleet which
     * is not {@link #isPersistent() marked for persistence}. The regatta is created with
     * {@link Regatta#isControlTrackingFromStartAndFinishTimes()} and
     * {@link #isAutoRestartTrackingUponCompetitorSetChange()} set to {@code false}.
     *
     * @param trackedRegattaRegistry
     *            used to find the {@link TrackedRegatta} for this column's series' {@link Series#getRegatta() regatta}
     *            in order to re-associate a {@link TrackedRace} passed to {@link #setTrackedRace(Fleet, TrackedRace)}
     *            with this column's series' {@link TrackedRegatta}, and the tracked race's {@link RaceDefinition} with
     *            this column's series {@link Regatta}, respectively. If <code>null</code>, the re-association won't be
     *            carried out.
     */
    public RegattaImpl(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, String name, BoatClass boatClass,
            boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType,
            TimePoint startDate, TimePoint endDate, TrackedRegattaRegistry trackedRegattaRegistry,
            ScoringScheme scoringScheme, Serializable id, CourseArea courseArea, String registrationLinkSecret) {
        this(raceLogStore, regattaLogStore, name, boatClass, canBoatsOfCompetitorsChangePerRace, competitorRegistrationType,
                startDate, endDate, trackedRegattaRegistry, scoringScheme,
                id, courseArea, /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false,
                OneDesignRankingMetric::new, registrationLinkSecret);
    }

    /**
     * Constructs a regatta with a single default series with empty race column list, and a single default fleet which
     * is not {@link #isPersistent() marked for persistence}.
     * @param trackedRegattaRegistry
     *            used to find the {@link TrackedRegatta} for this column's series' {@link Series#getRegatta() regatta}
     *            in order to re-associate a {@link TrackedRace} passed to {@link #setTrackedRace(Fleet, TrackedRace)}
     *            with this column's series' {@link TrackedRegatta}, and the tracked race's {@link RaceDefinition} with
     *            this column's series {@link Regatta}, respectively. If <code>null</code>, the re-association won't be
     *            carried out.
     */
    public RegattaImpl(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, String name, BoatClass boatClass,
            boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType,
            TimePoint startDate, TimePoint endDate, TrackedRegattaRegistry trackedRegattaRegistry,
            ScoringScheme scoringScheme, Serializable id, CourseArea courseArea,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange,
            RankingMetricConstructor rankingMetricConstructor, String registrationLinkSecret) {
        this(raceLogStore, regattaLogStore, name, boatClass, canBoatsOfCompetitorsChangePerRace, competitorRegistrationType,
                startDate, endDate, Collections.singletonList(new SeriesImpl(LeaderboardNameConstants.DEFAULT_SERIES_NAME,
                /* isMedal */false, /* isFleetsCanRunInParallel */ true, Collections
                        .singletonList(new FleetImpl(LeaderboardNameConstants.DEFAULT_FLEET_NAME)),
                /* race column names */new ArrayList<String>(), trackedRegattaRegistry)), /* persistent */false,
                scoringScheme, id, courseArea, /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */true, controlTrackingFromStartAndFinishTimes,
                autoRestartTrackingUponCompetitorSetChange, rankingMetricConstructor, registrationLinkSecret);
    }

    public <S extends Series> RegattaImpl(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, String name,
            BoatClass boatClass, boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType,
            TimePoint startDate, TimePoint endDate, Iterable<S> series, boolean persistent,
            ScoringScheme scoringScheme, Serializable id, CourseArea courseArea, Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange,
            RankingMetricConstructor rankingMetricConstructor, String registrationLinkSecret) {
        this(raceLogStore, regattaLogStore, name,
            boatClass, canBoatsOfCompetitorsChangePerRace, competitorRegistrationType,
            startDate, endDate, series, persistent,
            scoringScheme, id, courseArea==null?Collections.emptySet():Collections.singleton(courseArea),
            buoyZoneRadiusInHullLengths, useStartTimeInference,
            controlTrackingFromStartAndFinishTimes, autoRestartTrackingUponCompetitorSetChange,
            rankingMetricConstructor, registrationLinkSecret);
    }

    /**
     * @param series
     *            all {@link Series} in this iterable will have their {@link Series#setRegatta(Regatta) regatta set} to
     *            this new regatta.
     */
    public <S extends Series> RegattaImpl(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, String name,
            BoatClass boatClass, boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType,
            TimePoint startDate, TimePoint endDate, Iterable<S> series, boolean persistent,
            ScoringScheme scoringScheme, Serializable id, Iterable<CourseArea> courseAreas, Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange,
            RankingMetricConstructor rankingMetricConstructor, String registrationLinkSecret) {
        super(name);
        this.cpuMeter = CPUMeter.create();
        this.registrationLinkSecret = registrationLinkSecret;
        this.rankingMetricConstructor = rankingMetricConstructor;
        this.useStartTimeInference = useStartTimeInference;
        this.controlTrackingFromStartAndFinishTimes = controlTrackingFromStartAndFinishTimes;
        this.autoRestartTrackingUponCompetitorSetChange = autoRestartTrackingUponCompetitorSetChange;
        this.id = id;
        this.raceLogStore = raceLogStore;
        races = new ConcurrentHashMap<>();
        regattaListeners = new HashSet<RegattaListener>();
        raceColumnListeners = new RaceColumnListeners();
        this.boatClass = boatClass;
        this.canBoatsOfCompetitorsChangePerRace = canBoatsOfCompetitorsChangePerRace;
        this.startDate = startDate;
        this.endDate = endDate;
        final List<S> seriesList = new ArrayList<S>();
        for (final S s : series) {
            seriesList.add(s);
        }
        this.series = seriesList;
        for (Series s : series) {
            linkToRegattaAndConnectRaceLogsAndAddListeners(s, /* load race logs */ true);
        }
        this.persistent = persistent;
        this.scoringScheme = scoringScheme;
        this.courseAreas = Collections.synchronizedList(new ArrayList<>());
        Util.addAll(courseAreas, this.courseAreas);
        this.configuration = null;
        this.buoyZoneRadiusInHullLengths = buoyZoneRadiusInHullLengths;
        this.regattaLikeHelper = new BaseRegattaLikeImpl(new RegattaAsRegattaLikeIdentifier(this), regattaLogStore) {
            private static final long serialVersionUID = 8546222568682770206L;

            @Override
            public RaceColumn getRaceColumnByName(String raceColumnName) {
                for (final Series series : getSeries()) {
                    for (final RaceColumn raceColumn : series.getRaceColumns()) {
                        if (raceColumn.getName().equals(raceColumnName)) {
                            return raceColumn;
                        }
                    }
                }
                return null;
            }

            @Override
            public void setFleetsCanRunInParallelToTrue() {
                RegattaImpl.this.setFleetsCanRunInParallelToTrue();
            }
        };
        this.regattaLikeHelper.addListener(new RegattaLogEventAdditionForwarder(raceColumnListeners));
        this.raceExecutionOrderCache = new RaceExecutionOrderCache();
        this.competitorRegistrationType = competitorRegistrationType;
    }

    @Override
    public RankingMetricConstructor getRankingMetricConstructor() {
        // if an old version was successfully de-serialized, this field may be null; default to OneDesignRankingMetric
        return rankingMetricConstructor == null ? OneDesignRankingMetric::new : rankingMetricConstructor;
    }

    private void registerRaceLogsOnRaceColumns(Series series, boolean loadRaceLogs) {
        for (RaceColumn raceColumn : series.getRaceColumns()) {
            setRaceLogInformationOnRaceColumn(raceColumn, loadRaceLogs);
        }
    }

    private void setRaceLogInformationOnRaceColumn(RaceColumn raceColumn, boolean loadRaceLogs) {
        final RegattaLikeIdentifier regattaLikeIdentifier = new RegattaAsRegattaLikeIdentifier(this);
        if (loadRaceLogs) {
            raceColumn.setRaceLogInformationAndLoad(raceLogStore, regattaLikeIdentifier);
        } else {
            raceColumn.setRaceLogInformation(raceLogStore, regattaLikeIdentifier);
        }
    }

    @Override
    public Serializable getId() {
        return id;
    }

    @Override
    public CPUMeter getCPUMeter() {
        return cpuMeter;
    }

    public static String getDefaultName(String baseName, String boatClassName) {
        return baseName.replace('/', '_') + (boatClassName == null ? "" : " (" + boatClassName + ")").replace('/', '_');
    }

    @Override
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * When de-serializing, a possibly remote {@link #raceLogStore} is ignored because it is transient. Instead, an
     * {@link EmptyRaceLogStore} is used for the de-serialized instance. A new {@link RaceLogInformation} is assembled
     * for this empty race log and applied to all columns. Make sure to call {@link #initializeSeriesAfterDeserialize()}
     * after the object graph has been de-serialized.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.cpuMeter = CPUMeter.create();
        regattaListeners = new HashSet<RegattaListener>();
        MasterDataImportInformation masterDataImportInformation = ongoingMasterDataImportInformation.get();
        if (masterDataImportInformation != null) {
            raceLogStore = masterDataImportInformation.getRaceLogStore();
            races = new ConcurrentHashMap<>();
        } else {
            raceLogStore = EmptyRaceLogStore.INSTANCE;
        }
        // re-establish the transient listener on regattaLikeHelper that is responsible for forwarding the
        // regatta log events
        this.regattaLikeHelper.addListener(new RegattaLogEventAdditionForwarder(raceColumnListeners));
    }

    protected Object readResolve() throws ObjectStreamException {
        raceExecutionOrderCache.triggerUpdate(); // now we're fully initialized and the cache can do its job
        return this;
    }

    /**
     * {@link RaceColumnListeners} may not be de-serialized (yet) when the regatta is de-serialized. To avoid
     * re-registering empty objects most probably leading to a {link NullPointerException} one needs to initialize all
     * listeners after all objects have been read.
     */
    public void initializeSeriesAfterDeserialize() {
        for (final Series series : getSeries()) {
            // the following also transitively invokes setRaceLogInformation(raceLogStore, getRegattaLikeIdentifier()) on all race columns
            linkToRegattaAndConnectRaceLogsAndAddListeners(series, /* load race logs */ false);
            if (series.getRaceColumns() == null) {
                logger.warning("Race Columns were null during deserialization. This should not happen.");
            }
        }
    }

    @Override
    public Iterable<? extends Series> getSeries() {
        final Iterable<? extends Series> result;
        if (series != null) {
            result = Collections.unmodifiableCollection(series);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Series getSeriesByName(String name) {
        for (Series s : getSeries()) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public Iterable<RaceDefinition> getAllRaces() {
        return races.values();
    }

    @Override
    public RegattaIdentifier getRegattaIdentifier() {
        return new RegattaName(getName());
    }

    @Override
    public RegattaAndRaceIdentifier getRaceIdentifier(RaceDefinition race) {
        return new RegattaNameAndRaceName(getName(), race.getName());
    }

    @Override
    public RaceDefinition getRaceByName(String raceName) {
        return races.get(raceName);
    }

    @Override
    public void addRace(RaceDefinition race) {
        logger.info("Adding race " + race.getName() + " to regatta " + getName() + " (" + hashCode() + ")");
        if (getBoatClass() != null && race.getBoatClass() != getBoatClass()) {
            throw new IllegalArgumentException("Boat class " + race.getBoatClass()
                    + " doesn't match regatta's boat class " + getBoatClass());
        }
        races.put(race.getName(), race);
        synchronized (regattaListeners) {
            for (RegattaListener l : regattaListeners) {
                l.raceAdded(this, race);
            }
        }
    }

    @Override
    public void removeRace(RaceDefinition race) {
        logger.info("Removing race " + race.getName() + " from regatta " + getName() + " (" + hashCode() + ")");
        races.remove(race.getName());
        synchronized (regattaListeners) {
            for (RegattaListener l : regattaListeners) {
                l.raceRemoved(this, race);
            }
        }
    }

    @Override
    public BoatClass getBoatClass() {
        return boatClass;
    }

    @Override
    public CompetitorProviderFromRaceColumnsAndRegattaLike getOrCreateCompetitorsProvider() {
        if (competitorsProvider == null) {
            competitorsProvider = new CompetitorProviderFromRaceColumnsAndRegattaLike(this);
        }
        return competitorsProvider;
    }

    @Override
    public Pair<Iterable<RaceDefinition>, Iterable<Competitor>> getAllCompetitorsWithRaceDefinitionsConsidered() {
        if (competitorsProvider == null) {
            competitorsProvider = new CompetitorProviderFromRaceColumnsAndRegattaLike(this);
        }
        final Pair<Iterable<RaceDefinition>, Iterable<Competitor>> allCompetitorsWithRaceDefinitionsConsidered = competitorsProvider.getAllCompetitorsWithRaceDefinitionsConsidered();
        Set<Competitor> newResult = null;
        Set<RaceDefinition> newRaceDefinitions = null;
        final Iterable<RaceDefinition> racesConsideredSoFar = allCompetitorsWithRaceDefinitionsConsidered.getA();
        for (final RaceDefinition race : getAllRaces()) {
            if (!Util.contains(racesConsideredSoFar, race)) {
                if (newResult == null) {
                    newRaceDefinitions = new HashSet<>();
                    Util.addAll(allCompetitorsWithRaceDefinitionsConsidered.getA(), newRaceDefinitions);
                    newResult = new HashSet<>();
                    Util.addAll(allCompetitorsWithRaceDefinitionsConsidered.getB(), newResult);
                }
                Util.addAll(race.getCompetitors(), newResult);
                newRaceDefinitions.add(race);
            }
        }
        return newResult == null ? allCompetitorsWithRaceDefinitionsConsidered : new Pair<>(newRaceDefinitions, newResult);
    }

    @Override
    public Iterable<Competitor> getAllCompetitors() {
        return getAllCompetitorsWithRaceDefinitionsConsidered().getB();
    }

    @Override
    public Iterable<Boat> getAllBoats() {
        final Set<Boat> result = new HashSet<>();
        final Set<RaceDefinition> allRaces = new HashSet<>();
        Util.addAll(getAllRaces(), allRaces);
        for (final RaceColumn rc : getRaceColumns()) {
            for (final Fleet fleet : rc.getFleets()) {
                RaceDefinition raceDefinition = rc.getRaceDefinition(fleet);
                if (raceDefinition != null) {
                    allRaces.add(raceDefinition);
                }
            }
        }
        for (final RaceDefinition raceDefinition : allRaces) {
            Util.addAll(raceDefinition.getBoats(), result);
        }
        final RegattaLog regattaLog = getRegattaLog();
        // If no race exists, the regatta log-provided boat registrations will not have
        // been considered yet; add them:
        final Map<Competitor, Boat> regattaLogProvidedCompetitorsAndBoats = new CompetitorsAndBoatsInLogAnalyzer<>(regattaLog).analyze();
        Util.addAll(regattaLogProvidedCompetitorsAndBoats.values(), result);
        return result;
    }

    @Override
    public void addRegattaListener(RegattaListener listener) {
        synchronized (regattaListeners) {
            regattaListeners.add(listener);
        }
    }

    @Override
    public void removeRegattaListener(RegattaListener listener) {
        synchronized (regattaListeners) {
            regattaListeners.remove(listener);
        }
    }

    @Override
    public void trackedRaceLinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        raceColumnListeners.notifyListenersAboutTrackedRaceLinked(raceColumn, fleet, trackedRace);
    }

    @Override
    public void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        raceColumnListeners.notifyListenersAboutTrackedRaceUnlinked(raceColumn, fleet, trackedRace);
    }

    @Override
    public void isMedalRaceChanged(RaceColumn raceColumn, boolean newIsMedalRace) {
        raceColumnListeners.notifyListenersAboutIsMedalRaceChanged(raceColumn, newIsMedalRace);
    }

    @Override
    public void isFleetsCanRunInParallelChanged(RaceColumn raceColumn, boolean newIsFleetsCanRunInParallel) {
        raceColumnListeners.notifyListenersAboutIsFleetsCanRunInParallelChanged(raceColumn, newIsFleetsCanRunInParallel);
    }

    @Override
    public void isStartsWithZeroScoreChanged(RaceColumn raceColumn, boolean newIsStartsWithZeroScore) {
        raceColumnListeners.notifyListenersAboutIsStartsWithZeroScoreChanged(raceColumn, newIsStartsWithZeroScore);
    }

    @Override
    public void isFirstColumnIsNonDiscardableCarryForwardChanged(RaceColumn raceColumn,
            boolean firstColumnIsNonDiscardableCarryForward) {
        raceColumnListeners.notifyListenersAboutIsFirstColumnIsNonDiscardableCarryForwardChanged(raceColumn,
                firstColumnIsNonDiscardableCarryForward);
    }

    @Override
    public void hasSplitFleetContiguousScoringChanged(RaceColumn raceColumn, boolean hasSplitFleetContiguousScoring) {
        raceColumnListeners.notifyListenersAboutHasSplitFleetContiguousScoringChanged(raceColumn,
                hasSplitFleetContiguousScoring);
    }

    @Override
    public void oneAlwaysStaysOneChanged(RaceColumn raceColumn, boolean oneAlwaysStaysOne) {
        raceColumnListeners.notifyListenersAboutOneAlwaysStaysOneChanged(raceColumn, oneAlwaysStaysOne);
    }

    @Override
    public void hasCrossFleetMergedRankingChanged(RaceColumn raceColumn, boolean hasCrossFleetMergedRanking) {
        raceColumnListeners.notifyListenersAboutHasCrossFleetMergedRankingChanged(raceColumn,
                hasCrossFleetMergedRanking);
    }

    @Override
    public boolean canAddRaceColumnToContainer(RaceColumn raceColumn) {
        return raceColumnListeners.canAddRaceColumnToContainer(raceColumn);
    }

    @Override
    public void raceColumnAddedToContainer(RaceColumn raceColumn) {
        setRaceLogInformationOnRaceColumn(raceColumn, /* loadRaceLogs */ true);
        raceColumnListeners.notifyListenersAboutRaceColumnAddedToContainer(raceColumn);
    }

    @Override
    public void raceColumnRemovedFromContainer(RaceColumn raceColumn) {
        for (Fleet fleet : raceColumn.getFleets()) {
            RaceLogIdentifier identifier = raceColumn.getRaceLogIdentifier(fleet);
            raceLogStore.removeRaceLog(identifier);
        }
        raceColumnListeners.notifyListenersAboutRaceColumnRemovedFromContainer(raceColumn);
    }

    @Override
    public void raceColumnMoved(RaceColumn raceColumn, int newIndex) {
        raceColumnListeners.notifyListenersAboutRaceColumnMoved(raceColumn, newIndex);
    }

    @Override
    public void raceColumnNameChanged(RaceColumn raceColumn, String oldName, String newName) {
        raceColumnListeners.notifyListenersAboutRaceColumnNameChanged(raceColumn, oldName, newName);
    }

    @Override
    public void factorChanged(RaceColumn raceColumn, Double oldFactor, Double newFactor) {
        raceColumnListeners.notifyListenersAboutFactorChanged(raceColumn, oldFactor, newFactor);
    }

    @Override
    public void competitorDisplayNameChanged(Competitor competitor, String oldDisplayName, String displayName) {
        raceColumnListeners.notifyListenersAboutCompetitorDisplayNameChanged(competitor, oldDisplayName, displayName);
    }

    @Override
    public void resultDiscardingRuleChanged(ResultDiscardingRule oldDiscardingRule,
            ResultDiscardingRule newDiscardingRule) {
        raceColumnListeners.notifyListenersAboutResultDiscardingRuleChanged(oldDiscardingRule, newDiscardingRule);
    }

    @Override
    public void maximumNumberOfDiscardsChanged(Integer oldMaximumNumberOfDiscards, Integer newMaximumNumberOfDiscards) {
        raceColumnListeners.notifyListenersAboutMaximumNumberOfDiscardsChanged(oldMaximumNumberOfDiscards, newMaximumNumberOfDiscards);
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public void addRaceColumnListener(RaceColumnListener listener) {
        raceColumnListeners.addRaceColumnListener(listener);
    }

    @Override
    public void removeRaceColumnListener(RaceColumnListener listener) {
        raceColumnListeners.removeRaceColumnListener(listener);
    }

    @Override
    public void raceLogEventAdded(RaceColumn raceColumn, RaceLogIdentifier raceLogIdentifier, RaceLogEvent event) {
        raceColumnListeners.notifyListenersAboutRaceLogEventAdded(raceColumn, raceLogIdentifier, event);
    }

    @Override
    public void regattaLogEventAdded(RegattaLogEvent event) {
        raceColumnListeners.notifyListenersAboutRegattaLogEventAdded(event);
    }

    @Override
    public ScoringScheme getScoringScheme() {
        return scoringScheme;
    }

    @Override
    public TimePoint getStartDate() {
        return startDate;
    }

    @Override
    public void setStartDate(TimePoint startDate) {
        this.startDate = startDate;
    }

    @Override
    public TimePoint getEndDate() {
        return endDate;
    }

    @Override
    public void setEndDate(TimePoint endDate) {
        this.endDate = endDate;
    }

    @Override
    public Double getBuoyZoneRadiusInHullLengths() {
        return buoyZoneRadiusInHullLengths;
    }

    @Override
    public void setBuoyZoneRadiusInHullLengths(Double buoyZoneRadiusInHullLengths) {
        this.buoyZoneRadiusInHullLengths = buoyZoneRadiusInHullLengths;
    }

    @Override
    public Iterable<CourseArea> getCourseAreas() {
        return courseAreas;
    }

    @Override
    public void setCourseAreas(Iterable<CourseArea> newCourseAreas) {
        synchronized (this.courseAreas) {
            this.courseAreas.clear();
            Util.addAll(newCourseAreas, this.courseAreas);
        }
    }

    @Override
    public boolean isControlTrackingFromStartAndFinishTimes() {
        return controlTrackingFromStartAndFinishTimes;
    }

    @Override
    public boolean isAutoRestartTrackingUponCompetitorSetChange() {
        return autoRestartTrackingUponCompetitorSetChange;
    }

    @Override
    public void setControlTrackingFromStartAndFinishTimes(boolean controlTrackingFromStartAndFinishTimes) {
        if (controlTrackingFromStartAndFinishTimes != this.controlTrackingFromStartAndFinishTimes) {
            this.controlTrackingFromStartAndFinishTimes = controlTrackingFromStartAndFinishTimes;
            synchronized (regattaListeners) {
                for (RegattaListener l : regattaListeners) {
                    l.controlTrackingFromStartAndFinishTimesChanged(this, controlTrackingFromStartAndFinishTimes);
                }
            }
        }
    }

    @Override
    public void setAutoRestartTrackingUponCompetitorSetChange(boolean autoRestartTrackingUponCompetitorSetChange) {
        if (autoRestartTrackingUponCompetitorSetChange != this.autoRestartTrackingUponCompetitorSetChange) {
            this.autoRestartTrackingUponCompetitorSetChange = autoRestartTrackingUponCompetitorSetChange;
            synchronized (regattaListeners) {
                for (RegattaListener l : regattaListeners) {
                    l.autoRestartTrackingUponCompetitorSetChangeChanged(this, autoRestartTrackingUponCompetitorSetChange);
                }
            }
        }
    }

    @Override
    public void setUseStartTimeInference(boolean useStartTimeInference) {
        if (useStartTimeInference != this.useStartTimeInference) {
            this.useStartTimeInference = useStartTimeInference;
            synchronized (regattaListeners) {
                for (RegattaListener l : regattaListeners) {
                    l.useStartTimeInferenceChanged(this, useStartTimeInference);
                }
            }
        }
    }

    @Override
    public RegattaConfiguration getRegattaConfiguration() {
        return configuration;
    }

    @Override
    public void setRegattaConfiguration(RegattaConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return whether this regatta defines its local per-series result discarding rules; if so, any leaderboard based
     *         on the regatta has to respect this and has to use a result discarding rule implementation that keeps
     *         discards local to each series rather than spreading them across the entire leaderboard.
     */
    @Override
    public boolean definesSeriesDiscardThresholds() {
        for (Series s : series) {
            if (s.definesSeriesDiscardThresholds()) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return getId() + " " + getName() + " " + getScoringScheme().getType().name();
    }

    @Override
    public void addSeries(Series seriesToAdd) {
        Series existingSeries = getSeriesByName(seriesToAdd.getName());
        if (existingSeries == null) {
            linkToRegattaAndConnectRaceLogsAndAddListeners(seriesToAdd, /* load race logs */ true);
            synchronized (this.series) {
                ArrayList<Series> newSeriesList = new ArrayList<Series>();
                for (Series seriesObject : this.series) {
                    newSeriesList.add(seriesObject);
                }
                newSeriesList.add(seriesToAdd);
                this.series = newSeriesList;
            }
        }
    }

    private void linkToRegattaAndConnectRaceLogsAndAddListeners(Series seriesToAdd, boolean loadRaceLogs) {
        seriesToAdd.setRegatta(this);
        seriesToAdd.addRaceColumnListener(this);
        registerRaceLogsOnRaceColumns(seriesToAdd, loadRaceLogs);
    }

    @Override
    public void removeSeries(Series series) {
        Series existingSeries = getSeriesByName(series.getName());
        if (existingSeries != null) {
            final List<RaceColumnInSeries> raceColumns = new ArrayList<RaceColumnInSeries>();
            Util.addAll(series.getRaceColumns(), raceColumns);
            for (RaceColumn column : raceColumns) {
                for (Fleet fleet : column.getFleets()) {
                    column.removeRaceIdentifier(fleet);
                }
                series.removeRaceColumn(column.getName());
            }
            series.removeRaceColumnListener(this);
            synchronized (this.series) {
                ArrayList<Series> newSeriesList = new ArrayList<Series>();
                for (Series seriesObject : this.series) {
                    if (!seriesObject.getName().equals(series.getName())) {
                        newSeriesList.add(seriesObject);
                    }
                }
                this.series = newSeriesList;
            }
        }
    }

    @Override
    public boolean useStartTimeInference() {
        return useStartTimeInference;
    }

    @Override
    public boolean canBoatsOfCompetitorsChangePerRace() {
        return canBoatsOfCompetitorsChangePerRace;
    }

    /**
     * Changes whether the competitors use the same boat for the whole regatta or change the boat used during the competition.
     * Actually this should never be called as this should not change once set, but we need it to migrate older regattas to the new model.
     */
    protected void setCanBoatsOfCompetitorsChangePerRace(boolean canBoatsOfCompetitorsChangePerRace) {
        this.canBoatsOfCompetitorsChangePerRace = canBoatsOfCompetitorsChangePerRace;
    }

    @Override
    public void setCompetitorRegistrationType(CompetitorRegistrationType competitorRegistrationType) {
        this.competitorRegistrationType = competitorRegistrationType;
    }

    @Override
    public CompetitorRegistrationType getCompetitorRegistrationType() {
           return this.competitorRegistrationType == null ? CompetitorRegistrationType.CLOSED : this.competitorRegistrationType;
    }

    @Override
    public RegattaLog getRegattaLog() {
        return regattaLikeHelper.getRegattaLog();
    }

    @Override
    public RegattaLikeIdentifier getRegattaLikeIdentifier() {
        return regattaLikeHelper.getRegattaLikeIdentifier();
    }

    @Override
    public void addListener(RegattaLikeListener listener) {
        regattaLikeHelper.addListener(listener);
    }

    @Override
    public void removeListener(RegattaLikeListener listener) {
        regattaLikeHelper.removeListener(listener);
    }

    @Override
    public Double getTimeOnTimeFactor(Competitor competitor, Optional<Runnable> changeCallback) {
        return regattaLikeHelper.getTimeOnTimeFactor(competitor, changeCallback);
    }

    @Override
    public Duration getTimeOnDistanceAllowancePerNauticalMile(Competitor competitor, Optional<Runnable> changeCallback) {
        return regattaLikeHelper.getTimeOnDistanceAllowancePerNauticalMile(competitor, changeCallback);
    }

    @Override
    public RaceExecutionOrderProvider getRaceExecutionOrderProvider() {
        return raceExecutionOrderCache;
    }

    private class RaceExecutionOrderCache extends AbstractRaceExecutionOrderProvider {
        private static final long serialVersionUID = 1658153438012186894L;

        public RaceExecutionOrderCache() {
            super();
            addRaceColumnListener(this);
        }

        @Override
        protected Map<Fleet, Iterable<? extends RaceColumn>> getRaceColumnsOfSeries() {
            final Map<Fleet, Iterable<? extends RaceColumn>> result = new HashMap<>();
            final Iterable<? extends Series> mySeries = getSeries();
            if (mySeries != null) {
                boolean concurrentlyModified = false;
                do {
                    try {
                        for (Series currentSeries : mySeries) {
                            if (currentSeries.getFleets() != null) {
                                for (Fleet fleet : currentSeries.getFleets()) {
                                    if (currentSeries.getRaceColumns() != null) {
                                        result.put(fleet, currentSeries.getRaceColumns());
                                    }
                                }
                            }
                        }
                    } catch (ConcurrentModificationException e) {
                        // getSeries() returns a live collection, and Series.getRaceColumns() does so, too.
                        // In the unlikely event of a modification is applied to either of these structures while
                        // iterating, an exception will be thrown. We catch and log it here and try again.
                        logger.log(Level.INFO,
                                "Got a ConcurrentModificationException while trying to update the RaceExecutionOrderCache", e);
                        concurrentlyModified = true;
                    }
                } while (concurrentlyModified);
            }
            return result;
        }
    }

    @Override
    public RaceColumn getRaceColumnByName(String raceColumnName) {
        return regattaLikeHelper.getRaceColumnByName(raceColumnName);
    }

    @Override
    public IsRegattaLike getRegattaLike() {
        return this;
    }

    @Override
    public RaceLog getRacelog(String raceColumnName, String fleetName) {
        final RaceLog result;
        final RaceColumn raceColumn = getRaceColumnByName(raceColumnName);
        if (raceColumn == null) {
            result = null;
        } else {
            final Fleet fleet = raceColumn.getFleetByName(fleetName);
            if (fleet == null) {
                result = null;
            } else {
                result = raceColumn.getRaceLog(fleet);
            }
        }
        return result;
    }

    @Override
    public Iterable<? extends RaceColumn> getRaceColumns() {
        // special handling of no series and single series case to avoid somewhat expensive mapping iterables
        final Iterable<? extends RaceColumn> result;
        if (series.isEmpty()) {
            result = Collections.emptySet();
        } else if (series.size() == 1) {
            result = series.get(0).getRaceColumns();
        } else {
            result = Util.concat(Util.map(getSeries(), series->Util.map(series.getRaceColumns(), rc->(RaceColumn) rc)));
        }
        return result;
    }

    @Override
    public Iterable<Competitor> getCompetitorsRegisteredInRegattaLog() {
        RegattaLog regattaLog = getRegattaLog();
        CompetitorsInLogAnalyzer<RegattaLog, RegattaLogEvent, RegattaLogEventVisitor> analyzer = new CompetitorsInLogAnalyzer<>(
                regattaLog);
        return analyzer.analyze();
    }

    @Override
    public void registerCompetitor(Competitor competitor) {
        registerCompetitors(Collections.singletonList(competitor));
    }

    @Override
    public void registerCompetitors(Iterable<Competitor> competitors) {
        RegattaLog regattaLog = getRegattaLike().getRegattaLog();
        TimePoint now = MillisecondsTimePoint.now();
        for (Competitor competitor: competitors) {
            regattaLog.add(new RegattaLogRegisterCompetitorEventImpl(now, now, regattaLogEventAuthorForRegatta,
                    UUID.randomUUID(), competitor));
        }
    }

    @Override
    public void deregisterCompetitor(Competitor competitor) {
        deregisterCompetitors(Collections.singleton(competitor));
    }

    @Override
    public void deregisterCompetitors(Iterable<Competitor> competitors) {
        RegattaLog regattaLog = getRegattaLike().getRegattaLog();
        CompetitorDeregistrator<RegattaLog, RegattaLogEvent, RegattaLogEventVisitor> deregisterer = new CompetitorDeregistrator<>(regattaLog, competitors, regattaLogEventAuthorForRegatta);
        deregisterer.deregister(deregisterer.analyze());
    }

    @Override
    public void setFleetsCanRunInParallelToTrue() {
        for (Series series : this.series) {
            series.setIsFleetsCanRunInParallel(true);
        }
    }

    // boat functions
    @Override
    public Iterable<Boat> getBoatsRegisteredInRegattaLog() {
        RegattaLog regattaLog = getRegattaLog();
        RegattaLogBoatsInLogAnalyzer<RegattaLog, RegattaLogEvent, RegattaLogEventVisitor> analyzer = new RegattaLogBoatsInLogAnalyzer<>(
                regattaLog);
        return analyzer.analyze();
    }

    @Override
    public void registerBoat(Boat boat) {
        registerBoats(Collections.singleton(boat));
    }

    @Override
    public void registerBoats(Iterable<Boat> boats) {
        RegattaLog regattaLog = getRegattaLike().getRegattaLog();
        TimePoint now = MillisecondsTimePoint.now();

        for (Boat boat : boats) {
            regattaLog.add(new RegattaLogRegisterBoatEventImpl(now, now, regattaLogEventAuthorForRegatta,
                    UUID.randomUUID(), boat));
        }
    }

    @Override
    public void deregisterBoat(Boat boat) {
        deregisterBoats(Collections.singleton(boat));
    }

    @Override
    public void deregisterBoats(Iterable<Boat> boats) {
        RegattaLog regattaLog = getRegattaLike().getRegattaLog();
        RegattaLogBoatDeregistrator<RegattaLog, RegattaLogEvent, RegattaLogEventVisitor> deregisterer = new RegattaLogBoatDeregistrator<>(regattaLog, boats, regattaLogEventAuthorForRegatta);
        deregisterer.deregister(deregisterer.analyze());
    }

    @Override
    public String getRegistrationLinkSecret() {
        return registrationLinkSecret;
    }

    @Override
    public void setRegistrationLinkSecret(String registrationLinkSecret) {
        this.registrationLinkSecret = registrationLinkSecret;
    }

}
