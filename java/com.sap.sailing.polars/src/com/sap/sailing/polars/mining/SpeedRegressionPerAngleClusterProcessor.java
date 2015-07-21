package com.sap.sailing.polars.mining;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarsChangedListener;
import com.sap.sailing.polars.regression.IncrementalLeastSquares;
import com.sap.sailing.polars.regression.impl.IncrementalAnyOrderLeastSquaresImpl;
import com.sap.sse.datamining.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.factories.GroupKeyFactory;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.shared.GroupKey;

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
    private transient ConcurrentHashMap<BoatClass, Set<PolarsChangedListener>> listeners;

    private final Set<BoatClass> availableBoatClasses = new HashSet<BoatClass>();
    
    public SpeedRegressionPerAngleClusterProcessor(ClusterGroup<Bearing> angleClusterGroup) {
        this.angleClusterGroup = angleClusterGroup;
    }
    
    @Override
        public boolean canProcessElements() {
            // TODO Auto-generated method stub
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
        availableBoatClasses.add(boatClass);
        Set<PolarsChangedListener> listenersForBoatClass = listeners.get(fix.getBoatClass());
        if (listenersForBoatClass != null) {
            for (PolarsChangedListener listener : listenersForBoatClass) {
                listener.polarsChanged();
            }
        }
    }
    
    /**
     * There are angle clusters (size defined in the data mining pipeline construction), which each have their own 
     * regression for boatspeed over windspeed. 
     * We don't know the thresholds or centers of the angle clusters here, so we roughly interpolate by taking 10 
     * values from angle-5 deg to angle+5 deg and average the speeds.
     */
    public SpeedWithConfidence<Void> estimateBoatSpeed(BoatClass boatClass, Speed windSpeed, Bearing trueWindAngle) throws NotEnoughDataHasBeenAddedException {
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
                    .getSpeedRegressionPerAngleClusterClusterKeyDimensions().iterator());
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
    
    public void setListeners(ConcurrentHashMap<BoatClass, Set<PolarsChangedListener>> listeners) {
        this.listeners = listeners;
    }
    
    Set<BoatClass> getAvailableBoatClasses() {
        return availableBoatClasses;
    }
    
    @Override
    public Class<GroupedDataEntry<GPSFixMovingWithPolarContext>> getInputType() {
     // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<Void> getResultType() {
        // No result type here, since this is a special case of a processor. It's the end of the pipe so to say.
        return null;
    }

    
    @Override
    public void finish() throws InterruptedException {
        // Nothing to do here
    }
    
    @Override
        public boolean isFinished() {
            return false;
        }

    @Override
    public void abort() {
        // TODO Auto-generated method stub
    }
    
    @Override
        public boolean isAborted() {
            // TODO Auto-generated method stub
            return false;
        }

    @Override
    public AdditionalResultDataBuilder getAdditionalResultData(AdditionalResultDataBuilder additionalDataBuilder) {
        // TODO Auto-generated method stub
        return null;
    }

    public ClusterGroup<Bearing> getAngleCluster() {
        return angleClusterGroup;
    }

}
