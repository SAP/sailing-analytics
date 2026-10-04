package com.sap.sailing.polars.mining;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarsChangedListener;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util;
import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.factories.GroupKeyFactory;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;

/**
 * Groups incoming fixes by boatclass and legtype into {@link AngleAndSpeedRegression} instances and
 * provides access methods to regression data.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class CubicRegressionPerCourseProcessor implements
        Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, Void>, Serializable {

    private static final long serialVersionUID = 3059764273262382648L;

    private static final Logger logger = Logger.getLogger(CubicRegressionPerCourseProcessor.class.getName());

    private final Map<GroupKey, AngleAndSpeedRegression> regressions = new HashMap<>();

    /**
     * FIXME Make sure replication and listeners interact correctly
     */
    private transient ConcurrentMap<BoatClass, Set<PolarsChangedListener>> listeners;

    private boolean isFinished;

    private boolean isAborted;

    public CubicRegressionPerCourseProcessor filterToBoatClasses(Iterable<BoatClass> boatClasses) {
        final Set<BoatClass> allowedBoatClasses = Util.asSet(boatClasses);
        final CubicRegressionPerCourseProcessor filteredProcessor = new CubicRegressionPerCourseProcessor();
        for (final Map.Entry<GroupKey, AngleAndSpeedRegression> entry : regressions.entrySet()) {
            GroupKey key = entry.getKey();
            BoatClass boatClass = extractBoatClass(key);
            if (boatClass != null && allowedBoatClasses.contains(boatClass)) {
                filteredProcessor.regressions.put(key, entry.getValue());
            }
        }
        return filteredProcessor;
    }

    private BoatClass extractBoatClass(GroupKey key) {
        final BoatClass result;
        if (key.hasSubKeys()) {
            // In the compound key, BoatClass is the second dimension (index 1)
            GroupKey boatClassKey = key.getKeys().get(1);
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
        GPSFixMovingWithPolarContext fix = element.getDataEntry();
        if (fix.getLegType() == LegType.UPWIND || fix.getLegType() == LegType.DOWNWIND) {
            GroupKey key = element.getKey();
            AngleAndSpeedRegression regression;
            synchronized (regressions) {
                regression = regressions.get(key);
                if (regression == null) {
                    regression = new AngleAndSpeedRegression();
                    regressions.put(key, regression);
                }
            }
            regression.addData(fix.getWind(), fix.getAbsoluteAngleToTheWind(), fix.getBoatSpeed());
            Set<PolarsChangedListener> listenersForBoatClass = listeners.get(fix.getBoatClass());
            if (listenersForBoatClass != null) {
                for (PolarsChangedListener listener : listenersForBoatClass) {
                    listener.polarsChanged();
                }
            }
        }
    }

    /**
     * Returns speed and twa for a given scenario, if data is available in the regression.
     */
    public SpeedWithBearingWithConfidence<Void> getAverageSpeedAndCourseOverGround(BoatClass boatClass,
            Speed windSpeed, LegType legType) throws NotEnoughDataHasBeenAddedException {
        GroupKey key = createGroupKey(boatClass, legType);
        SpeedWithBearingWithConfidence<Void> estimatedSpeedAndAngle = null;
        if (regressions.containsKey(key)) {
            estimatedSpeedAndAngle = regressions.get(key).estimateSpeedAndAngle(windSpeed);
        } else {
            throw new NotEnoughDataHasBeenAddedException("Not enough data has been added to Per Course Regressions");
        }
        return estimatedSpeedAndAngle;
    }

    /**
     * Returns windspeed and windangle candidates for a given scenario, if enough data is available.
     */
    public Set<SpeedWithBearingWithConfidence<Void>> estimateTrueWindSpeedAndAngleCandidates(BoatClass boatClass,
            Speed speedOverGround, LegType legType, Tack tack) {
        GroupKey key = createGroupKey(boatClass, legType);
        Set<SpeedWithBearingWithConfidence<Void>> result = new HashSet<>();
        if (regressions.containsKey(key)) {
            try {
                result = regressions.get(key).estimateTrueWindSpeedAndAngleCandidates(speedOverGround, legType, tack);
            } catch (NotEnoughDataHasBeenAddedException e) {
                // Return empty result
            }
        } else {
            // Return empty result;
        }
        return result;
    }

    private GroupKey createGroupKey(final BoatClass boatClass, final LegType legType) {
        LegTypePolarClusterKey key = new LegTypePolarClusterKey() {
            @Override
            public BoatClass getBoatClass() {
                return boatClass;
            }

            @Override
            public LegType getLegType() {
                return legType;
            }
        };
        GroupKey compoundKey;
        try {
            compoundKey = GroupKeyFactory.createNestingCompoundKeyFor(key, PolarDataDimensionCollectionFactory
                    .getCubicRegressionPerCourseClusterKeyDimensions());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return compoundKey;
    }

    /**
     * Allows direct access to speed regression functions for debugging purposes.
     * 
     * For actual usage of the data please use
     * {@link #getAverageSpeedAndCourseOverGround(BoatClass, Speed, LegType)}
     * and
     * {@link #estimateTrueWindSpeedAndAngleCandidates(BoatClass, Speed, LegType, Tack)}.
     */
    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        GroupKey key = createGroupKey(boatClass, legType);
        PolynomialFunction polynomialFunction;
        if (regressions.containsKey(key)) {
            polynomialFunction = regressions.get(key).getSpeedRegressionFunction();
        } else {
            throw new NotEnoughDataHasBeenAddedException();
        }
        return polynomialFunction;
    }

    /**
     * Allows direct access to angle regression functions for debugging purposes.
     * 
     * For actual usage of the data please use
     * {@link #getAverageSpeedAndCourseOverGround(BoatClass, Speed, LegType)}
     * and
     * {@link #estimateTrueWindSpeedAndAngleCandidates(BoatClass, Speed, LegType, Tack)}.
     */
    public PolynomialFunction getAngleRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        GroupKey key = createGroupKey(boatClass, legType);
        PolynomialFunction polynomialFunction;
        if (regressions.containsKey(key)) {
            polynomialFunction = regressions.get(key).getAngleRegressionFunction();
        } else {
            throw new NotEnoughDataHasBeenAddedException();
        }
        return polynomialFunction;
    }

    @Override
    public void onFailure(Throwable failure) {
        failure.printStackTrace();
        logger.severe("Polar Data Mining Pipe failed. Cause: " + failure.getMessage());
        throw new RuntimeException("Polar Data Miner failed.", failure);
    }

    public void setListeners(ConcurrentMap<BoatClass, Set<PolarsChangedListener>> listeners) {
        this.listeners = listeners;
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

    public Map<GroupKey, AngleAndSpeedRegression> getRegressions() {
        synchronized (regressions) {
            return Collections.unmodifiableMap(regressions);
        }
    }
    
    public void updateRegressions(Map<GroupKey, AngleAndSpeedRegression> regressionsToUpdate) {
        synchronized (regressions) {
            regressions.putAll(regressionsToUpdate);
        }
    }
    
}
