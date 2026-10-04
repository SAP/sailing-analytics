package com.sap.sailing.windestimation.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.domain.windestimation.IncrementalWindEstimation;
import com.sap.sailing.polars.impl.PolarDataServiceImpl;
import com.sap.sailing.polars.jaxrs.client.FileBasedPolarDataClient;
import com.sap.sailing.windestimation.model.store.ClassPathReadOnlyModelStoreImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.shared.util.Wait;

/**
 * Regression test for issue #6274. When a race has <em>no</em> wind sources other than
 * {@link WindSourceType#MANEUVER_BASED_ESTIMATION}, the maneuver-typing step
 * (in {@code IncrementalManeuverDetectorImpl}, {@code ManeuverDetectorImpl}, and
 * {@code ManeuverDetectorWithEstimationDataSupportDecoratorImpl}) needs to be able to
 * consume the estimation track's own wind fixes to classify maneuvers as
 * {@link ManeuverType#TACK}, {@link ManeuverType#JIBE},
 * {@link ManeuverType#HEAD_UP} or {@link ManeuverType#BEAR_AWAY}. Issue #6184
 * excluded the {@link WindSourceType#MANEUVER_BASED_ESTIMATION} source from those
 * {@code trackedRace.getWind(...)} lookups, so when it is the only wind source the
 * typing step receives {@code null} and produces {@link ManeuverType#UNKNOWN}
 * maneuvers uniformly, breaking the NN+HMM bootstrap that the wind-estimation-from-
 * maneuvers design depends on.
 * <p>
 *
 * The test replicates {@link IncrementalMstHmmWindEstimationForTrackedRaceTest}'s
 * fixture (TracTrac 5O5 race with local polar data) but omits the EXPEDITION wind
 * fixes that test injects. The estimator therefore has to bootstrap from its own
 * output. Assertions:
 *
 * <ol>
 *   <li>The MANEUVER_BASED_ESTIMATION track has produced at least a handful of wind
 *       fixes (i.e. the estimator ran at all).</li>
 *   <li>A substantial fraction of detected maneuvers are classified with a
 *       non-{@link ManeuverType#UNKNOWN} type (the typing step consumed the
 *       estimation wind).</li>
 * </ol>
 *
 * On mainline (with the exclusion still in place) assertion 2 fails because every
 * detected maneuver is {@link ManeuverType#UNKNOWN}. On the {@code bug6274} branch
 * (where the exclusion is reverted) both assertions hold.
 */
public class ManeuverTypingWithOnlyEstimationWindTest extends AbstractTestWithLocal505PolarData {
    private static final Logger logger = Logger.getLogger(ManeuverTypingWithOnlyEstimationWindTest.class.getName());

    /**
     * At least this fraction of detected maneuvers must have a classified type
     * (TACK, JIBE, HEAD_UP, BEAR_AWAY) rather than {@link ManeuverType#UNKNOWN}.
     * The 5O5 Kieler Woche fixture used here has plenty of clean tacks and jibes;
     * the threshold is set well above what the broken-typing path produces
     * (which is 0) yet loosely enough not to be sensitive to non-determinism of
     * the spanning-tree/HMM inference.
     */
    private static final double MIN_TYPED_MANEUVER_RATIO = 0.5;

    private WindEstimationFactoryServiceImpl windEstimationFactoryService;
    private PolarDataServiceImpl polarDataService;

    public ManeuverTypingWithOnlyEstimationWindTest() throws Exception {
        windEstimationFactoryService = new WindEstimationFactoryServiceImpl();
        windEstimationFactoryService.clearState();
        final ClassPathReadOnlyModelStoreImpl modelStore = new ClassPathReadOnlyModelStoreImpl(
                "trained_wind_estimation_models", getClass().getClassLoader(),
                IncrementalMstHmmWindEstimationForTrackedRaceTest.modelFilesNames);
        windEstimationFactoryService.importAllModelsFromModelStore(modelStore);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        final URI storedUri = new URI("file:///"
                + new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS,
                        ReceiverType.MARKPOSITIONS });
        polarDataService = new PolarDataServiceImpl();
        final com.sap.sailing.domain.tractracadapter.DomainFactory domainFactoryImpl = getDomainFactory();
        final DomainFactory baseDomainFactory = domainFactoryImpl.getBaseDomainFactory();
        final FileBasedPolarDataClient client = new FileBasedPolarDataClient(new File("resources/polar_data"),
                polarDataService, baseDomainFactory);
        client.updatePolarDataRegressions();
        getTrackedRace().setPolarDataService(polarDataService);
        // NOTE: unlike IncrementalMstHmmWindEstimationForTrackedRaceTest, we deliberately do NOT
        // install any EXPEDITION / TRACK_BASED_ESTIMATION / COURSE_BASED wind fixes here. The
        // MANEUVER_BASED_ESTIMATION track produced by the estimator we install below is the only
        // wind source available to the maneuver-typing step. See issue #6274.
        polarDataService.insertExistingFixes(getTrackedRace());
        Wait.wait(() -> !polarDataService.isCurrentlyActiveOrHasQueue(),
                /* timeout */ Optional.of(Duration.ONE_MINUTE),
                /* sleepBetweenAttempts */ Duration.ONE_SECOND.times(5), Level.INFO,
                "Waiting for polar data service to finish computing");
        final IncrementalWindEstimation windEstimation = windEstimationFactoryService
                .createIncrementalWindEstimationTrack(getTrackedRace());
        getTrackedRace().setWindEstimation(windEstimation);
        getTrackedRace().triggerManeuverCacheRecalculationForAllCompetitors();
        getTrackedRace().waitForManeuverDetectionToFinish();
        windEstimation.waitUntilDone();
    }

    @Test
    public void testManeuversGetTypedFromEstimationWind() {
        assertTrue(windEstimationFactoryService.isReady(), "Wind estimation models are empty");
        final DynamicTrackedRaceImpl trackedRace = getTrackedRace();
        final WindTrack estimatedWindTrack = trackedRace
                .getOrCreateWindTrack(new WindSourceImpl(WindSourceType.MANEUVER_BASED_ESTIMATION));
        int estimatedFixCount = 0;
        estimatedWindTrack.lockForRead();
        try {
            for (@SuppressWarnings("unused") final Wind ignored : estimatedWindTrack.getFixes()) {
                estimatedFixCount++;
            }
        } finally {
            estimatedWindTrack.unlockAfterRead();
        }
        assertTrue(estimatedFixCount > 0,
                "Expected the maneuver-based wind estimator to have produced at least one wind fix, "
                        + "but the MANEUVER_BASED_ESTIMATION track is empty; the NN+HMM graph produced "
                        + "no output at all (unrelated to issue #6274).");
        logger.info("Estimator produced " + estimatedFixCount + " MANEUVER_BASED_ESTIMATION wind fixes.");
        int totalManeuvers = 0;
        int typedManeuvers = 0;
        for (final Competitor competitor : trackedRace.getRace().getCompetitors()) {
            for (final Maneuver maneuver : trackedRace.getManeuvers(competitor, /* waitForLatest */ false)) {
                totalManeuvers++;
                final ManeuverType type = maneuver.getType();
                if (type == ManeuverType.TACK || type == ManeuverType.JIBE
                        || type == ManeuverType.HEAD_UP || type == ManeuverType.BEAR_AWAY) {
                    typedManeuvers++;
                }
            }
        }
        assertTrue(totalManeuvers > 0,
                "Expected at least some maneuvers to have been detected for the fixture race, but found none.");
        final double typedRatio = (double) typedManeuvers / (double) totalManeuvers;
        logger.info("Typed " + typedManeuvers + " of " + totalManeuvers + " maneuvers ("
                + Math.round(100 * typedRatio) + "%).");
        assertTrue(typedRatio >= MIN_TYPED_MANEUVER_RATIO,
                "Expected at least " + Math.round(100 * MIN_TYPED_MANEUVER_RATIO)
                        + "% of maneuvers to be classified as TACK/JIBE/HEAD_UP/BEAR_AWAY, but only "
                        + typedManeuvers + "/" + totalManeuvers + " (" + Math.round(100 * typedRatio)
                        + "%) were. The fixture race has no non-MANEUVER_BASED_ESTIMATION wind sources, "
                        + "so this indicates the maneuver-typing step could not consume any wind: issue "
                        + "#6184's typing-side exclusion of the estimation track (reverted by #6274) "
                        + "prevents the bootstrap from ever producing a wind the typing step will accept.");
    }
}
