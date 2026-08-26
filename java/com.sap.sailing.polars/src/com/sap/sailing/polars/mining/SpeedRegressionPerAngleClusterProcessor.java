package com.sap.sailing.polars.mining;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarsChangedListener;
import com.sap.sailing.polars.impl.CubicEquation;
import com.sap.sailing.polars.regression.IncrementalLeastSquares;
import com.sap.sailing.polars.regression.impl.IncrementalAnyOrderLeastSquaresImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.factories.GroupKeyFactory;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;

/**
 * Holds one speed regression per BoatClass, WindSpeed, Beat Angle Range combination and provides means for adding and
 * accessing data. Data is added incrementally in and not all historic fixes need to be maintained. This allows for a
 * very space efficient structure.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class SpeedRegressionPerAngleClusterProcessor implements
        Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, Void>, Serializable {

    private static final long serialVersionUID = 3279917556091599077L;

    private static final Logger logger = Logger.getLogger(CubicRegressionPerCourseProcessor.class.getName());

    private final Map<GroupKey, IncrementalLeastSquares> regressions = new HashMap<>();

    private final Map<BoatClass, Long> fixCountPerBoatClass = new HashMap<>();

    private final ClusterGroup<Bearing> angleClusterGroup;

    /**
     * FIXME make sure listeners and replication interact correctly
     */
    private transient ConcurrentMap<BoatClass, Set<PolarsChangedListener>> listeners;

    private boolean isFinished;

    private boolean isAborted;

    public SpeedRegressionPerAngleClusterProcessor(ClusterGroup<Bearing> angleClusterGroup) {
        this.angleClusterGroup = angleClusterGroup;
    }

    public SpeedRegressionPerAngleClusterProcessor filterToBoatClasses(Iterable<BoatClass> boatClasses) {
        final Set<BoatClass> allowedBoatClasses = Util.asSet(boatClasses);
        final SpeedRegressionPerAngleClusterProcessor filteredProcessor = new SpeedRegressionPerAngleClusterProcessor(angleClusterGroup);
        synchronized (regressions) {
            for (Map.Entry<GroupKey, IncrementalLeastSquares> entry : regressions.entrySet()) {
                GroupKey key = entry.getKey();
                BoatClass boatClass = extractBoatClass(key);
                if (boatClass != null && allowedBoatClasses.contains(boatClass)) {
                    filteredProcessor.regressions.put(key, entry.getValue());
                }
            }
        }
        synchronized (fixCountPerBoatClass) {
            for (Map.Entry<BoatClass, Long> entry : fixCountPerBoatClass.entrySet()) {
                if (allowedBoatClasses.contains(entry.getKey())) {
                    filteredProcessor.fixCountPerBoatClass.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return filteredProcessor;
    }

    private BoatClass extractBoatClass(GroupKey key) {
        final BoatClass result;
        if (key.hasSubKeys()) {
            // In the compound key, BoatClass is the first dimension (index 0)
            GroupKey boatClassKey = key.getKeys().get(0);
            if (boatClassKey instanceof GenericGroupKey) {
                Object value = ((GenericGroupKey<?>) boatClassKey).getValue();
                if (value instanceof BoatClass) {
                    result = (BoatClass) value;
                } else {
                    result = null;
                }
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public boolean canProcessElements() {
        return true;
    }

    @Override
    public void processElement(GroupedDataEntry<GPSFixMovingWithPolarContext> element) {
        GroupKey key = element.getKey();
        IncrementalLeastSquares regression;
        BoatClass boatClass = element.getDataEntry().getBoatClass();
        synchronized (fixCountPerBoatClass) {
            if (!fixCountPerBoatClass.containsKey(boatClass)) {
                fixCountPerBoatClass.put(boatClass, 0L);
            }
            fixCountPerBoatClass.put(boatClass, fixCountPerBoatClass.get(boatClass) + 1);
        }
        synchronized (regressions) {
            regression = regressions.get(key);
            if (regression == null) {
                regression = new IncrementalAnyOrderLeastSquaresImpl(3, false);
                regressions.put(key, regression);
            }
        }
        GPSFixMovingWithPolarContext fix = element.getDataEntry();
        regression.addData(fix.getWind().getObject().getKnots(), fix.getBoatSpeed().getObject().getKnots());
        Set<PolarsChangedListener> listenersForBoatClass = listeners.get(fix.getBoatClass());
        if (listenersForBoatClass != null) {
            for (PolarsChangedListener listener : listenersForBoatClass) {
                listener.polarsChanged();
            }
        }
    }

    /**
     * There are angle clusters (size defined in the data mining pipeline construction), which each have their own
     * regression for boatspeed over windspeed. We don't know the thresholds or centers of the angle clusters here, so
     * we roughly interpolate by taking 10 values from angle-5 deg to angle+5 deg and average the speeds.
     * 
     * At the time of writing the size of each angle range is 5� so this method provides a pretty smooth interpolation.
     */
    public SpeedWithConfidence<Void> estimateBoatSpeed(BoatClass boatClass, Speed windSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        double speedSum = 0;
        double numberOfSpeeds = 0;
        long fixCount = 0;
        for (int i = -2; i <= 2; i++) {
            GroupKey key = createGroupKey(boatClass, new DegreeBearingImpl(Math.abs(trueWindAngle.getDegrees()) + i));
            if (regressions.containsKey(key)) {
                IncrementalLeastSquares incrementalLeastSquares = regressions.get(key);
                fixCount = fixCount + incrementalLeastSquares.getNumberOfAddedPoints();
                if (fixCount > 10) {
                    speedSum += incrementalLeastSquares.getOrCreatePolynomialFunction().value(windSpeed.getKnots());
                    numberOfSpeeds++;
                }
            }
        }
        fixCount = (long) (fixCount / numberOfSpeeds);
        long fixCountOverall = 0;
        if (numberOfSpeeds < 2 || fixCount < 10) {
            throw new NotEnoughDataHasBeenAddedException("Not enough data has been added to Per Course Regressions");
        } else {
            synchronized (fixCountPerBoatClass) {
                fixCountOverall = fixCountPerBoatClass.get(boatClass);
            }
        }
        Speed speed = new KnotSpeedImpl(speedSum / numberOfSpeeds);
        return new SpeedWithConfidenceImpl<Void>(speed, Math.min(1, 5.0 * ((double) fixCount / fixCountOverall)), null);
    }
    
    public Pair<List<Speed>, Double> estimateWindSpeeds(BoatClass boatClass, Speed boatSpeed, Bearing trueWindAngle, double referenceTwsKnots)
            throws NotEnoughDataHasBeenAddedException {
        List<Speed> windSpeeds = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            GroupKey key = createGroupKey(boatClass, new DegreeBearingImpl(Math.abs(trueWindAngle.getDegrees()) + i));
            if (regressions.containsKey(key)) {
                IncrementalLeastSquares incrementalLeastSquares = regressions.get(key);
                long fixesCount = incrementalLeastSquares.getNumberOfAddedPoints();
                if (fixesCount > 10) {
                    double[] coefficients = incrementalLeastSquares.getOrCreatePolynomialFunction().getCoefficients();
                    CubicEquation equation = new CubicEquation(coefficients[3], coefficients[2], coefficients[1],
                            -boatSpeed.getKnots());
                    double bestWindSpeedCandidateInKnots = Double.MAX_VALUE;
                    double bestOptimalSpeedDifference = Double.MAX_VALUE;
                    double[] windSpeedCandidates = equation.solve();
                    for (double windSpeedCandidateInKnots : windSpeedCandidates) {
                        if (windSpeedCandidateInKnots > 2 && windSpeedCandidateInKnots < 20) {
                            double optimalSpeedDifference = Math.abs(referenceTwsKnots - windSpeedCandidateInKnots);
                            if (optimalSpeedDifference < bestOptimalSpeedDifference) {
                                bestOptimalSpeedDifference = optimalSpeedDifference;
                                bestWindSpeedCandidateInKnots = windSpeedCandidateInKnots;
                            }
                        }
                    }
                    if (bestWindSpeedCandidateInKnots != Double.MAX_VALUE) {
                        windSpeeds.add(new KnotSpeedImpl(bestWindSpeedCandidateInKnots));
                    }
                }
            }
        }
        return new Pair<>(windSpeeds, Math.min(1, 0.5));
    }

    private GroupKey createGroupKey(final BoatClass boatClass, final Bearing angle) {
        AngleClusterPolarClusterKey key = new AngleClusterPolarClusterKey() {
            @Override
            public BoatClass getBoatClass() {
                return boatClass;
            }

            @Override
            public Cluster<Bearing> getAngleCluster() {
                return angleClusterGroup.getClusterFor(angle);
            }
        };
        GroupKey compoundKey;
        try {
            compoundKey = GroupKeyFactory.createNestingCompoundKeyFor(key, PolarDataDimensionCollectionFactory
                    .getSpeedRegressionPerAngleClusterClusterKeyDimensions());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return compoundKey;
    }

    @Override
    public void onFailure(Throwable failure) {
        logger.severe("Polar Data Mining Pipe failed.");
        throw new RuntimeException("Polar Data Miner failed.", failure);
    }


    /**
     * Allows direct access to angle regression functions for debugging purposes.
     * 
     * For actual usage of the data please use
     * {@link #estimateBoatSpeed(BoatClass, Speed, Bearing)}.
     */
    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, double trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        GroupKey key = createGroupKey(boatClass, new DegreeBearingImpl(trueWindAngle));
        PolynomialFunction polynomialFunction;
        if (regressions.containsKey(key)) {
            polynomialFunction = regressions.get(key).getOrCreatePolynomialFunction();
        } else {
            throw new NotEnoughDataHasBeenAddedException();
        }
        return polynomialFunction;
    }

    public void setListeners(ConcurrentMap<BoatClass, Set<PolarsChangedListener>> listeners) {
        this.listeners = listeners;
    }

    Set<BoatClass> getAvailableBoatClasses() {
        return Collections.unmodifiableSet(fixCountPerBoatClass.keySet());
    }

    @Override
    public Class<GroupedDataEntry<GPSFixMovingWithPolarContext>> getInputType() {
        @SuppressWarnings("unchecked")
        final Class<GroupedDataEntry<GPSFixMovingWithPolarContext>> result = (Class<GroupedDataEntry<GPSFixMovingWithPolarContext>>) (Class<?>) GroupedDataEntry.class;
        return result;
    }

    @Override
    public Class<Void> getResultType() {
        // No result type here, since this is a special case of a processor. It's the end of the pipe so to say.
        return Void.class;
    }

    @Override
    public void finish() throws InterruptedException {
        isFinished = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void abort() {
        isAborted = true;
    }

    @Override
    public boolean isAborted() {
        return isAborted;
    }

    @Override
    public AdditionalResultDataBuilder getAdditionalResultData(AdditionalResultDataBuilder additionalDataBuilder) {
        return additionalDataBuilder;
    }

    public ClusterGroup<Bearing> getAngleCluster() {
        return angleClusterGroup;
    }

    public Map<GroupKey, IncrementalAnyOrderLeastSquaresImpl> getRegressionsImpl() {
        synchronized (regressions) {
            Map<GroupKey, IncrementalAnyOrderLeastSquaresImpl> map = new HashMap<>();
            regressions.keySet().stream().forEach(key -> map.put(key, (IncrementalAnyOrderLeastSquaresImpl) regressions.get(key)));
            return Collections.unmodifiableMap(map);
        }
    }

    public Map<BoatClass, Long> getFixCountPerBoatClass() {
        synchronized (fixCountPerBoatClass) {
            return Collections.unmodifiableMap(fixCountPerBoatClass);
        }
    }
}
