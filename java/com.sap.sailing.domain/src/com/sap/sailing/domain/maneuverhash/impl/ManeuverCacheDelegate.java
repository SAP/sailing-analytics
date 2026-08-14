package com.sap.sailing.domain.maneuverhash.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.maneuverhash.ManeuverCache;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.maneuverhash.SerializableManeuverCache;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sailing.domain.windestimation.IncrementalWindEstimation;

public class ManeuverCacheDelegate implements SerializableManeuverCache {
    private static final long serialVersionUID = 19872309587435L;
    private final TrackedRaceImpl race;
    private static final Logger logger = Logger.getLogger(ManeuverCacheDelegate.class.getName());
    private transient ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry;
    private volatile transient ManeuverCache cacheToUse;
    
    public ManeuverCacheDelegate(TrackedRaceImpl race,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        super();
        this.race = race;
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
        this.cacheToUse = createUpdatableManeuverCache();
    }    
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.cacheToUse = (ManeuversFromDatabase) ois.readObject();
    }
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(new ManeuversFromDatabase(getAllKnownManeuvers()));
    }
    
    @Override
    public void ensureFilled() {
        if (cacheToUse.canBeUpdated()) {
            for (final Competitor competitor : race.getShuffledCompetitors()) {
                cacheToUse.triggerUpdate(competitor);
            }
        }
    }

    @Override
    public void setManeuverRaceFingerprintRegistry(ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
    }

    private Map<Competitor, List<Maneuver>> getAllKnownManeuvers() {
        final Map<Competitor, List<Maneuver>> result = new HashMap<>();
        for (final Competitor competitor : race.getRace().getCompetitors()) {
            final List<Maneuver> maneuversForCompetitor = get(competitor, /* waitForLatest */ false);
            if (maneuversForCompetitor != null) {
                result.put(competitor, maneuversForCompetitor);
            }
        }
        return result;
    }

    @Override
    public void resume() {
        final ManeuverRaceFingerprint fingerprint;
        if (maneuverRaceFingerprintRegistry != null) {
            logger.info("Compare maneuver fingerprints for race "+race.getRaceIdentifier());
            race.waitForAllRaceLogsAttached();
            fingerprint = maneuverRaceFingerprintRegistry.getManeuverRaceFingerprint(race.getRaceIdentifier());
        } else {
            fingerprint = null;
        }
        // Self-heal check (bug6241): if the fingerprint matches but the loaded maneuvers cannot
        // possibly produce a maneuver-based wind estimate, treat the DB record as a stale cache
        // miss and fall through to the compute path. Two poisoned states are recognised:
        //
        //   * "all empty" -- every competitor's list is null or empty. This is the original
        //     failure mode: the maneuver detector completed with no wind context at all and the
        //     storage step wrote empty lists.
        //
        //   * "no classifiable maneuvers" -- lists are non-empty but every stored maneuver is
        //     typed as UNKNOWN or PENALTY_CIRCLE (i.e. none of TACK/JIBE/HEAD_UP/BEAR_AWAY is
        //     present anywhere in the race). This happens when the detector ran while
        //     getWind(pos, at) still returned null everywhere so createManeuverFromManeuverCurveAndWind
        //     hit the "wind == null" branch (ManeuverDetectorImpl line ~699) and typed every
        //     spot as UNKNOWN. Feeding such records back through
        //     IncrementalWindEstimation.alreadyClassifiedManeuversAvailable produces zero wind
        //     fixes because IncrementalMstHmmWindEstimationForTrackedRace.mapManeuverType maps
        //     UNKNOWN and PENALTY_CIRCLE to null -- the whole batch gets filtered out and the
        //     MBE wind track stays empty. That's what surfaces to callers as hasWindData=false.
        //
        // Either case is unambiguous evidence that the DB record was produced without a working
        // wind estimator. The compute path now sequences its storage after the wind estimator
        // has settled (see computeAndStore / retypeAndStoreAfterWindEstimationSettled), so
        // recomputing here overwrites the poison and heals the DB.
        final boolean useDbLoad;
        final Map<Competitor, List<Maneuver>> loadedManeuvers;
        if (fingerprint != null && fingerprint.matches(race)) {
            loadedManeuvers = maneuverRaceFingerprintRegistry.loadManeuvers(race, race.getRace().getCourse());
            if (isAllEmpty(loadedManeuvers)) {
                logger.info("Maneuver fingerprints match for race "+race.getRaceIdentifier()
                        +" but stored maneuvers are empty for every competitor; treating as stale cache miss and re-computing (see bug6241)");
                useDbLoad = false;
            } else if (hasNoClassifiableManeuver(loadedManeuvers)) {
                logger.info("Maneuver fingerprints match for race "+race.getRaceIdentifier()
                        +" but no stored maneuver is classifiable as TACK/JIBE/HEAD_UP/BEAR_AWAY;"
                        +" the DB record cannot yield a maneuver-based wind estimate. Treating as stale"
                        +" cache miss and re-computing (see bug6241)");
                useDbLoad = false;
            } else {
                logger.info("Maneuver fingerprints match for race "+race.getRaceIdentifier()+"; loading from DB instead of computing");
                useDbLoad = true;
            }
        } else {
            loadedManeuvers = null;
            useDbLoad = false;
        }
        if (useDbLoad) {
            cacheToUse = new ManeuversFromDatabase(loadedManeuvers);
        } else {
            new Thread(this::computeAndStore, "Waiting for maneuvers for "+race.getName()+" after having resumed to store the results in registry")
                    .start();
        }
    }

    /**
     * Returns {@code true} iff {@code maneuvers} is {@code null}, contains no entries, or contains
     * only entries whose value is {@code null} or an empty list. Used by {@link #resume()} to
     * self-heal from a previously-persisted "computed and empty" verdict (see bug6241).
     */
    private boolean isAllEmpty(Map<Competitor, List<Maneuver>> maneuvers) {
        boolean allEmpty = true;
        if (maneuvers != null) {
            for (final List<Maneuver> forCompetitor : maneuvers.values()) {
                if (forCompetitor != null && !forCompetitor.isEmpty()) {
                    allEmpty = false;
                    break;
                }
            }
        }
        return allEmpty;
    }

    /**
     * Returns {@code true} iff none of the stored maneuvers across all competitors is typed as
     * {@link ManeuverType#TACK TACK}, {@link ManeuverType#JIBE JIBE}, {@link ManeuverType#HEAD_UP
     * HEAD_UP} or {@link ManeuverType#BEAR_AWAY BEAR_AWAY}. Those four are the types that the
     * maneuver-based wind estimator can turn into a wind fix; anything else
     * ({@link ManeuverType#PENALTY_CIRCLE PENALTY_CIRCLE}, {@link ManeuverType#UNKNOWN UNKNOWN},
     * {@code null}) is filtered out in {@code IncrementalMstHmmWindEstimationForTrackedRace
     * .mapManeuverType}, so a stored record consisting only of those non-classifiable types
     * would yield zero wind fixes when fed back via {@code alreadyClassifiedManeuversAvailable}.
     * <p>
     *
     * Assumes the caller has already established that {@link #isAllEmpty} is {@code false}:
     * this method exists to catch the second poisoned state where the lists are non-empty but
     * every entry is unclassifiable (typically all-{@link ManeuverType#UNKNOWN UNKNOWN} from a
     * detection pass that had no wind context; see the {@link #resume()} rationale).
     */
    private boolean hasNoClassifiableManeuver(Map<Competitor, List<Maneuver>> maneuvers) {
        boolean noneClassifiable = true;
        if (maneuvers != null) {
            outer:
            for (final List<Maneuver> forCompetitor : maneuvers.values()) {
                if (forCompetitor != null) {
                    for (final Maneuver maneuver : forCompetitor) {
                        if (maneuver != null && isClassifiableType(maneuver.getType())) {
                            noneClassifiable = false;
                            break outer;
                        }
                    }
                }
            }
        }
        return noneClassifiable;
    }

    /**
     * The four maneuver types that {@code IncrementalMstHmmWindEstimationForTrackedRace
     * .mapManeuverType} maps to a non-{@code null} {@code ManeuverTypeForClassification}, i.e.
     * the ones that can feed a wind fix. Keep this list in sync with that mapping.
     */
    private boolean isClassifiableType(ManeuverType type) {
        return type == ManeuverType.TACK || type == ManeuverType.JIBE
                || type == ManeuverType.HEAD_UP || type == ManeuverType.BEAR_AWAY;
    }

    /**
     * Runs the maneuver detector via the smart-future cache, then -- once a wind estimation has
     * been installed on the tracked race and its inference has produced any wind fixes it can --
     * recalculates each competitor's maneuvers so they get re-typed using the estimator's fixes
     * (which since bug6274 are visible to the typing step via {@code trackedRace.getWind}).
     * Finally snapshots the re-typed maneuvers and persists them via the fingerprint registry.
     * <p>
     *
     * This deferred-store choreography (bug6241) is what prevents the DB from being poisoned with
     * empty / UNKNOWN-typed maneuvers on the first server run for a race that has no other wind
     * source: on subsequent server starts the fingerprint match then loads a properly-typed
     * maneuver list, which fed through {@code feedAlreadyKnownManeuversToWindEstimation} produces
     * wind fixes without needing to redetect.
     */
    private void computeAndStore() {
        logger.info("Maneuver fingerprints do not match for race "+race.getRaceIdentifier()+"; NOT loading from DB");
        if (!cacheToUse.canBeUpdated()) {
            cacheToUse = createUpdatableManeuverCache();
        }
        cacheToUse.resume();
        if (maneuverRaceFingerprintRegistry != null) {
            // First blocking pass: let the detector complete its initial run. This is what emits
            // ManeuverSpots to the wind estimator via newManeuverSpotsDetected. Spots get typed
            // using whatever wind is currently available -- typically nothing on the first pass
            // for races with no non-estimation wind source.
            for (final Competitor competitor : race.getRace().getCompetitors()) {
                cacheToUse.get(competitor, /* waitForLatest */ true);
            }
            // Sequence the persistence step after the wind estimator has been installed and has
            // finished the inference kicked off by our spots. runWhenWindEstimationInstalled
            // fires synchronously here if the estimator was installed while we were computing;
            // otherwise it registers a callback that fires once setWindEstimation is called with
            // a non-null argument, and cancels silently if the race is removed before that
            // happens. Because the setWindEstimation call typically happens on a shared
            // background-executor thread and the follow-up work (waitUntilDone, recalculate,
            // get(waitForLatest=true)) then blocks waiting for other tasks on the *same* shared
            // pool, running it on the setWindEstimation caller's thread risks starving the pool
            // (fatal with pool size 1). Detach onto a dedicated thread so the caller of
            // setWindEstimation returns immediately. See bug6241 for the rationale.
            race.runWhenWindEstimationInstalled(() ->
                    new Thread(this::retypeAndStoreAfterWindEstimationSettled,
                            "Retyping and storing maneuvers after wind estimation for "+race.getName())
                            .start());
        }
    }

    /**
     * Second phase of {@link #computeAndStore()}: sequences maneuver-cache re-typing so that the
     * stored maneuvers reflect the wind fixes the estimator produces from the spots emitted by
     * the detector.
     * <p>
     *
     * The choreography is two-round: the first round of {@link ManeuverCache#recalculate}
     * triggers a fresh full-scan pass of the detector for each competitor (their per-competitor
     * detectors were {@link TrackedRaceImpl#setWindEstimation cleared} when the wind estimator
     * was installed, so the fresh detector emits spots into the now-non-null
     * {@code WindEstimationInteraction}). The estimator asynchronously processes those spots and
     * publishes wind fixes on its {@code MANEUVER_BASED_ESTIMATION} track. Once the estimator
     * has drained (via {@code waitUntilDone}), a second round of recalculation re-types the
     * spots using the newly-available wind and captures the classified maneuvers, which are then
     * persisted. See bug6241; without this sequencing the initial recalc runs while the
     * estimator has queued but not yet processed the spots, so typing still sees no wind and
     * produces UNKNOWN maneuvers which then get persisted permanently.
     */
    private void retypeAndStoreAfterWindEstimationSettled() {
        // Round 1: trigger fresh detection per competitor. Their detector cache was cleared when
        // setWindEstimation ran, so new detectors are built with a non-null wind-estimation
        // interaction and emit spots to the estimator during this pass.
        for (final Competitor competitor : race.getRace().getCompetitors()) {
            cacheToUse.recalculate(competitor);
            cacheToUse.get(competitor, /* waitForLatest */ true);
        }
        // Wait for the estimator to drain the spots emitted in Round 1 and produce its wind
        // fixes. Those fixes flow into the tracked race's MANEUVER_BASED_ESTIMATION track and
        // become visible to getWind(pos, at) lookups.
        final IncrementalWindEstimation windEstimation = race.getWindEstimation();
        boolean estimatorSettled = true;
        if (windEstimation != null) {
            try {
                windEstimation.waitUntilDone();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                estimatorSettled = false;
                logger.log(Level.WARNING, "Interrupted while waiting for wind estimation to finish inference for race "
                        +race.getRaceIdentifier()+"; skipping maneuver re-type and store");
            }
        }
        if (estimatorSettled) {
            // Round 2: recalculate per competitor. This time the detector's
            // lastManeuverDetectionResult is populated from Round 1 and no new raw fixes have
            // arrived, so it takes the re-type branch of detectManeuverSpots (line 150 of
            // IncrementalManeuverDetectorImpl) and re-types the existing spots using the current
            // wind -- which now includes the estimator's fixes.
            final Map<Competitor, List<Maneuver>> maneuvers = new HashMap<>();
            for (final Competitor competitor : race.getRace().getCompetitors()) {
                cacheToUse.recalculate(competitor);
                maneuvers.put(competitor, cacheToUse.get(competitor, /* waitForLatest */ true));
            }
            maneuverRaceFingerprintRegistry.storeManeuvers(race.getRaceIdentifier(),
                    ManeuverRaceFingerprintFactory.INSTANCE.createFingerprint(race),
                    maneuvers, race.getRace().getCourse());
        }
    }

    @Override
    public List<Maneuver> get(Competitor competitor, boolean waitForLatest) {
        return cacheToUse.get(competitor, waitForLatest);
    }

    @Override
    public void suspend() {
        cacheToUse.suspend();
    }

    @Override
    public void recalculate(Competitor competitor) {
        cacheToUse.recalculate(competitor);
    }

    @Override
    public void triggerUpdate(Competitor competitor) {
        if (!cacheToUse.canBeUpdated()) {
            logger.warning("Received a maneuver cache update trigger for competitor "+competitor.getName()+" but current cache cannot be updated; switching to an updatable cache");
            cacheToUse = createUpdatableManeuverCache();
        }
        cacheToUse.triggerUpdate(competitor);
    }

    private ManeuverCache createUpdatableManeuverCache() {
        return new ManeuversFromSmartFutureCache((DynamicTrackedRaceImpl) race);
    }

    /**
     * Reflects the current {@link #cacheToUse inner cache}. Returns {@code true} when the delegate is
     * currently backed by a compute-capable cache (a {@link ManeuversFromSmartFutureCache}) and
     * {@code false} when it is currently backed by a passive {@link ManeuversFromDatabase}. The value
     * switches at {@link #resume()} time depending on whether the fingerprint matched (DB-load) or not
     * (compute), and again in {@link #triggerUpdate(Competitor)} if an update is requested on a
     * currently-passive cache -- see the comment in {@code triggerUpdate}.
     * <p>
     *
     * Callers use this to distinguish the two states, e.g., {@code TrackedRaceImpl
     * .feedAlreadyKnownManeuversToWindEstimation} only feeds when the delegate is in the DB-load
     * state (bug6241). Prior to bug6241 this method returned {@code true} unconditionally, which was
     * a semantic bug: the delegate always <em>can</em> switch to a compute-capable cache on demand,
     * but that's not what the {@link ManeuverCache#canBeUpdated} contract asks -- it asks whether
     * the cache currently accepts {@link #triggerUpdate} without a mode switch.
     */
    @Override
    public boolean canBeUpdated() {
        return cacheToUse.canBeUpdated();
    }
}
