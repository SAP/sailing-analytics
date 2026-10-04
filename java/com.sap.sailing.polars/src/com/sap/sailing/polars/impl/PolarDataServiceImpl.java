package com.sap.sailing.polars.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.PolarSheetGenerationSettings;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.impl.PolarSheetGenerationSettingsImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.polars.PolarsChangedListener;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.polars.PolarDataOperation;
import com.sap.sailing.polars.ReplicablePolarService;
import com.sap.sailing.polars.mining.AngleAndSpeedRegression;
import com.sap.sailing.polars.mining.BearingClusterGroup;
import com.sap.sailing.polars.mining.CubicRegressionPerCourseProcessor;
import com.sap.sailing.polars.mining.PolarDataMiner;
import com.sap.sailing.polars.mining.SpeedRegressionPerAngleClusterProcessor;
import com.sap.sailing.polars.regression.impl.IncrementalAnyOrderLeastSquaresImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.confidence.BearingWithConfidence;
import com.sap.sse.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.replication.interfaces.impl.AbstractReplicableWithObjectInputStream;
import com.sap.sse.util.ClearStateTestSupport;

/**
 * Uses a custom datamining pipeline to aggregate incoming fixes in two regression based polar containers.
 * 
 * For more information on polars in SAP Sailing Analytics, please see: http://wiki.sapsailing.com/wiki/howto/misc/polars
 * 
 * @author Frederik Petersen (D054528)
 * @author Axel Uhl
 * 
 */
public class PolarDataServiceImpl extends AbstractReplicableWithObjectInputStream<PolarDataService, PolarDataOperation<?>> implements ReplicablePolarService, ClearStateTestSupport {

    private static final Logger logger = Logger.getLogger(PolarDataServiceImpl.class.getSimpleName());

    private PolarDataMiner polarDataMiner;

    private DomainFactory domainFactory;

    /**
     * See {@link #PolarDataServiceImpl(boolean)}. Preserved across
     * {@link #resetState() state resets}.
     */
    private final boolean waitForLoadingOfAllRacesToRestoreToBeStarted;

    /**
     * Convenience constructor equivalent to
     * {@code new PolarDataServiceImpl(false)}. See
     * {@link #PolarDataServiceImpl(boolean)} for the meaning of the flag; briefly, this default
     * does <em>not</em> enable the "wait until the client has finished triggering all startup
     * races" gate on {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)}. That gate
     * is only needed by clients that batch-restore many races through this service and expect
     * drain-callbacks to hold until that batch is fully queued. Ad-hoc uses and tests can rely
     * on this default.
     */
    public PolarDataServiceImpl() {
        this(/* waitForLoadingOfAllRacesToRestoreToBeStarted */ false);
    }

    /**
     * @param waitForLoadingOfAllRacesToRestoreToBeStarted
     *            when {@code true}, callbacks registered via
     *            {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} won't fire until
     *            the client has explicitly called {@link #markLoadingOfAllRacesToRestoreStarted()}
     *            <em>and</em> the race's fixes are in the loading pipeline. Constructing with
     *            {@code true} and never calling {@code markLoadingOfAllRacesToRestoreStarted()}
     *            will hold callbacks indefinitely — used by the OSGi/production wiring which
     *            makes that promise. See bug6241 and
     *            {@link PolarDataMiner#PolarDataMiner(PolarSheetGenerationSettings, CubicRegressionPerCourseProcessor, SpeedRegressionPerAngleClusterProcessor, ClusterGroup, boolean)}.
     */
    public PolarDataServiceImpl(boolean waitForLoadingOfAllRacesToRestoreToBeStarted) {
        this.waitForLoadingOfAllRacesToRestoreToBeStarted = waitForLoadingOfAllRacesToRestoreToBeStarted;
        resetState();
    }

    public PolarDataServiceImpl filterToBoatClasses(Iterable<BoatClass> boatClassesToFilterTo) {
        final PolarDataMiner filteredPolarDataMiner = polarDataMiner.filterToBoatClasses(boatClassesToFilterTo);
        // A filtered clone is a derived view, not driven by a startup-restore batch; keep the
        // default (non-gated) mode regardless of this instance's setting.
        final PolarDataServiceImpl filteredService = new PolarDataServiceImpl();
        filteredService.polarDataMiner = filteredPolarDataMiner;
        return filteredService;
    }

    @Override
    public void resetState() {
        PolarSheetGenerationSettings settings = PolarSheetGenerationSettingsImpl.createBackendPolarSettings();
        ClusterGroup<Bearing> angleClusterGroup = createAngleClusterGroup();
        CubicRegressionPerCourseProcessor cubicRegressionPerCourseProcessor = new CubicRegressionPerCourseProcessor();
        SpeedRegressionPerAngleClusterProcessor speedRegressionPerAngleClusterProcessor = new SpeedRegressionPerAngleClusterProcessor(angleClusterGroup);
        this.polarDataMiner = new PolarDataMiner(settings, cubicRegressionPerCourseProcessor,
                speedRegressionPerAngleClusterProcessor, angleClusterGroup,
                waitForLoadingOfAllRacesToRestoreToBeStarted);
    }
    
    public boolean isCurrentlyActiveOrHasQueue() {
        return polarDataMiner.isCurrentlyActiveOrHasQueue();
    }

    private ClusterGroup<Bearing> createAngleClusterGroup() {
        return new BearingClusterGroup(0, 180, 5);
    }

    @Override
    public SpeedWithConfidence<Void> getSpeed(BoatClass boatClass, Speed windSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        if (polarDataMiner == null) {
            throw new NotEnoughDataHasBeenAddedException(
                    "Polar Data Miner is currently unavailable. Maybe we are in the process of replication initial load?");
        }
        return polarDataMiner.estimateBoatSpeed(boatClass, windSpeed, trueWindAngle);
    }
    
    @Override
    public Pair<List<Speed>, Double> estimateWindSpeeds(BoatClass boatClass, Speed boatSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        if (polarDataMiner == null) {
            throw new NotEnoughDataHasBeenAddedException(
                    "Polar Data Miner is currently unavailable. Maybe we are in the process of replication initial load?");
        }
        return polarDataMiner.estimateWindSpeeds(boatClass, boatSpeed, trueWindAngle);
    }

    @Override
    public Set<SpeedWithBearingWithConfidence<Void>> getAverageTrueWindSpeedAndAngleCandidates(BoatClass boatClass,
            Speed speedOverGround, LegType legType, Tack tack) {
        return polarDataMiner.estimateTrueWindSpeedAndAngleCandidates(boatClass, speedOverGround, legType, tack);
    }

    @Override
    public SpeedWithBearingWithConfidence<Void> getAverageSpeedWithTrueWindAngle(BoatClass boatClass, Speed windSpeed,
            LegType legType, Tack tack) throws NotEnoughDataHasBeenAddedException {
        if (polarDataMiner == null) {
            throw new NotEnoughDataHasBeenAddedException(
                    "Polar Data Miner is currently unavailable. Maybe we are in the process of replication initial load?");
        }
        SpeedWithBearingWithConfidence<Void> averageSpeedAndCourseOverGround = polarDataMiner
                .getAverageSpeedAndCourseOverGround(boatClass, windSpeed, legType);
        if (tack == Tack.PORT) {
            // Negative twa
            DegreeBearingImpl bearing = new DegreeBearingImpl(-averageSpeedAndCourseOverGround.getObject().getBearing()
                    .getDegrees());
            KnotSpeedWithBearingImpl speed = new KnotSpeedWithBearingImpl(averageSpeedAndCourseOverGround.getObject()
                    .getKnots(), bearing);
            averageSpeedAndCourseOverGround = new SpeedWithBearingWithConfidenceImpl<Void>(speed,
                    averageSpeedAndCourseOverGround.getConfidence(), null);
        }
        return averageSpeedAndCourseOverGround;
    }

    @Override
    public Set<BoatClass> getAllBoatClassesWithPolarSheetsAvailable() {
        return polarDataMiner.getAvailableBoatClasses();
    }

    @Override
    public void competitorPositionChanged(final GPSFixMoving fix, final Competitor competitor,
            final TrackedRace createdTrackedRace) {
        polarDataMiner.addFix(fix, competitor, createdTrackedRace);
    }

    @Override
    public double getConfidenceForTackJibeSpeedRatio(Speed intoTackSpeed, Speed intoJibeSpeed, BoatClass boatClass) {
        return Math.min(1., 0.5 * intoJibeSpeed.getKnots() / intoTackSpeed.getKnots());
    }

    @Override
    public Pair<Double, SpeedWithBearingWithConfidence<Void>> getManeuverLikelihoodAndTwsTwa(BoatClass boatClass,
            Speed speedAtManeuverStart, double courseChangeDeg, ManeuverType maneuverType) {
        assert maneuverType == ManeuverType.TACK || maneuverType == ManeuverType.JIBE;
        SpeedWithBearingWithConfidence<Void> closestTwsTwa = getClosestTwaTws(maneuverType, speedAtManeuverStart,
                courseChangeDeg, boatClass);
        final Pair<Double, SpeedWithBearingWithConfidence<Void>> result;
        if (closestTwsTwa == null) {
            result = new Pair<>(0.0, null);
        } else {
            double targetManeuverAngle = getManeuverAngleInDegreesFromTwa(maneuverType,
                    closestTwsTwa.getObject().getBearing());
            double minDiffDeg = Math.abs(Math.abs(targetManeuverAngle) - Math.abs(courseChangeDeg));
            result = new Pair<>(1. / (1. + (minDiffDeg / 10.) * (minDiffDeg / 10.)), closestTwsTwa);
        }
        return result;
    }

    @Override
    public SpeedWithBearingWithConfidence<Void> getClosestTwaTws(ManeuverType type, Speed speedAtManeuverStart,
            double courseChangeDeg, BoatClass boatClass) {
        assert type == ManeuverType.TACK || type == ManeuverType.JIBE;
        double minDiff = Double.MAX_VALUE;
        SpeedWithBearingWithConfidence<Void> closestTwsTwa = null;
        for (SpeedWithBearingWithConfidence<Void> trueWindSpeedAndAngle : getAverageTrueWindSpeedAndAngleCandidates(
                boatClass, speedAtManeuverStart, type == ManeuverType.TACK ? LegType.UPWIND : LegType.DOWNWIND,
                type == ManeuverType.TACK ? courseChangeDeg >= 0 ? Tack.PORT : Tack.STARBOARD
                        : courseChangeDeg >= 0 ? Tack.STARBOARD : Tack.PORT)) {
            double targetManeuverAngle = getManeuverAngleInDegreesFromTwa(type,
                    trueWindSpeedAndAngle.getObject().getBearing());
            double diff = Math.abs(Math.abs(targetManeuverAngle) - Math.abs(courseChangeDeg));
            if (diff < minDiff) {
                minDiff = diff;
                closestTwsTwa = trueWindSpeedAndAngle;
            }
        }
        return closestTwsTwa;
    }

    @Override
    public double getManeuverAngleInDegreesFromTwa(ManeuverType type, Bearing twa) {
        assert type == ManeuverType.TACK || type == ManeuverType.JIBE;
        double maneuverAngle;
        if (type == ManeuverType.TACK) {
            maneuverAngle = Math.abs(twa.getDegrees() * 2);
        } else {
            maneuverAngle = (180 - Math.abs(twa.getDegrees())) * 2.0;
        }
        return maneuverAngle;
    }

    @Override
    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        return polarDataMiner.getSpeedRegressionFunction(boatClass, legType);
    }

    @Override
    public PolynomialFunction getAngleRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        return polarDataMiner.getAngleRegressionFunction(boatClass, legType);
    }

    @Override
    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, double trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        return polarDataMiner.getSpeedRegressionFunction(boatClass, Math.abs(trueWindAngle));
    }

    @Override
    public void raceFinishedLoading(TrackedRace race, Runnable callbackWhenRaceChangingToTrackingOfFinishedStatus) {
        polarDataMiner.raceFinishedLoading(race, callbackWhenRaceChangingToTrackingOfFinishedStatus);
    }

    @Override
    public void runWhenPolarLoadingFinishedFor(TrackedRace race, Runnable callback) {
        polarDataMiner.runWhenPolarLoadingFinishedFor(race, callback);
    }

    @Override
    public void raceRemoved(TrackedRace race) {
        polarDataMiner.raceRemoved(race);
    }

    @Override
    public void markLoadingOfAllRacesToRestoreStarted() {
        polarDataMiner.markLoadingOfAllRacesToRestoreStarted();
    }

    @Override
    public BearingWithConfidence<Void> getManeuverAngle(BoatClass boatClass, ManeuverType maneuverType, Speed windSpeed)
            throws NotEnoughDataHasBeenAddedException {
        if (maneuverType != ManeuverType.TACK && maneuverType != ManeuverType.JIBE) {
            throw new IllegalArgumentException("ManeuverType needs to be tack or jibe.");
        }
        LegType legType = maneuverType == ManeuverType.TACK ? LegType.UPWIND : LegType.DOWNWIND;
        if (boatClass == null || windSpeed == null) {
            throw new IllegalArgumentException("Boatclass and windspeed cannot be null.");
        }
        if (polarDataMiner == null) {
            throw new NotEnoughDataHasBeenAddedException(
                    "Polar Data Miner is currently unavailable. Maybe we are in the process of replication initial load?");
        }
        SpeedWithBearingWithConfidence<Void> speed = polarDataMiner.getAverageSpeedAndCourseOverGround(boatClass,
                windSpeed, legType);
        Bearing bearing = new DegreeBearingImpl(
                getManeuverAngleInDegreesFromTwa(maneuverType, speed.getObject().getBearing()));
        BearingWithConfidence<Void> bearingWithConfidence = new BearingWithConfidenceImpl<Void>(bearing,
                speed.getConfidence(), null);
        return bearingWithConfidence;
    }

    @Override
    public void insertExistingFixes(TrackedRace trackedRace) {
        for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
            final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
            track.lockForRead();
            try {
                for (GPSFixMoving fix : track.getFixes()) {
                    competitorPositionChanged(fix, competitor, trackedRace);
                }
            } finally {
                track.unlockAfterRead();
            }
        }
    }

    @Override
    public void registerListener(BoatClass boatClass, PolarsChangedListener listener) {
        polarDataMiner.registerListener(boatClass, listener);
    }

    @Override
    public void unregisterListener(BoatClass boatClass, PolarsChangedListener listener) {
        polarDataMiner.unregisterListener(boatClass, listener);
    }

    @Override
    public void clearReplicaState() throws MalformedURLException, IOException, InterruptedException {
        polarDataMiner = null;
    }

    @Override
    public ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is, Map<String, Class<?>> classLoaderCache) throws IOException {
        ObjectInputStream ois;
        if (domainFactory != null) {
            ois = domainFactory.createObjectInputStreamResolvingAgainstThisFactory(is, null, classLoaderCache);
        } else {
            // TODO ensure that domainfactory is set here. Otherwise there can be issues with duplicate domain objects
            logger.warning("PolarDataService didn't have a domain factory attached. Replication to this service could fail.");
            ois = new ObjectInputStream(is);
        }
        return ois;
    }

    @Override
    public void initiallyFillFromInternal(ObjectInputStream is) throws IOException, ClassNotFoundException,
            InterruptedException {
        PolarSheetGenerationSettings backendPolarSettings = (PolarSheetGenerationSettings) is.readObject();
        CubicRegressionPerCourseProcessor cubicRegressionPerCourseProcessor = (CubicRegressionPerCourseProcessor) is.readObject();
        SpeedRegressionPerAngleClusterProcessor speedRegressionPerAngleClusterProcessor = (SpeedRegressionPerAngleClusterProcessor) is.readObject();
        polarDataMiner = new PolarDataMiner(backendPolarSettings, cubicRegressionPerCourseProcessor,
                speedRegressionPerAngleClusterProcessor, speedRegressionPerAngleClusterProcessor.getAngleCluster(),
                waitForLoadingOfAllRacesToRestoreToBeStarted);
    }

    @Override
    public void serializeForInitialReplicationInternal(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(polarDataMiner.getPolarSheetGenerationSettings());
        // objectOutputStream.writeObject(polarDataMiner.getMovingAverageProcessor());
        objectOutputStream.writeObject(polarDataMiner.getCubicRegressionPerCourseProcessor());
        objectOutputStream.writeObject(polarDataMiner.getSpeedRegressionPerAngleClusterProcessor());
    }

    @Override
    public void registerDomainFactory(DomainFactory domainFactory) {
        synchronized(this) {
            this.domainFactory = domainFactory;
            this.notifyAll();
        }
    }
    
    @Override
    public void runWithDomainFactory(Consumer<DomainFactory> consumer) throws InterruptedException {
        DomainFactory myDomainFactory;
        synchronized(this) {
            myDomainFactory = domainFactory;
            while (myDomainFactory == null) {
                wait();
                myDomainFactory = domainFactory;
            }
        }
        consumer.accept(myDomainFactory);
    }

    public Map<GroupKey, AngleAndSpeedRegression> getCubicRegressionsPerCourse() {
        return polarDataMiner.getCubicRegressionPerCourseProcessor().getRegressions();
    }
    
    public Map<GroupKey, IncrementalAnyOrderLeastSquaresImpl> getSpeedRegressionsPerAngle() {
        return polarDataMiner.getSpeedRegressionPerAngleClusterProcessor().getRegressionsImpl();
    }

    @Override
    public Map<BoatClass, Long> getFixCountPerBoatClass() {
        return polarDataMiner.getSpeedRegressionPerAngleClusterProcessor().getFixCountPerBoatClass();
    }

    @Override
    public void clearState() throws Exception {
        resetState();
    }
}
