package com.sap.sailing.grib.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MeterPerSecondSpeedWithDegreeBearingImpl;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;

public class SpeedAndDirectionWindField extends AbstractGribWindFieldImpl {
    private static final Logger logger = Logger.getLogger(SpeedAndDirectionWindField.class.getName());
    private static final int WIND_DIRECTION_PARAMETER_ID = 31;
    private static final int WIND_DIRECTION_GRIB2_DISCIPLINE = 0;
    private static final int WIND_DIRECTION_GRIB2_PARAMETER_CATEGORY = 2;
    private static final int WIND_DIRECTION_GRIB2_PARAMETER_NUMBER = 0;

    private static final int WIND_SPEED_PARAMETER_ID = 32;
    private static final int WIND_SPEED_GRIB2_DISCIPLINE = 0;
    private static final int WIND_SPEED_GRIB2_PARAMETER_CATEGORY = 2;
    private static final int WIND_SPEED_GRIB2_PARAMETER_NUMBER = 1;
    
    private static final VariableSpecification windDirectionVariableSpecification =
            new CompositeVariableSpecification(new Grib1VariableSpecification(WIND_DIRECTION_PARAMETER_ID),
                                               new Grib2VariableSpecification(new int[] { WIND_DIRECTION_GRIB2_DISCIPLINE, WIND_DIRECTION_GRIB2_PARAMETER_CATEGORY, WIND_DIRECTION_GRIB2_PARAMETER_NUMBER }));

    private static final VariableSpecification windSpeedVariableSpecification =
            new CompositeVariableSpecification(new Grib1VariableSpecification(WIND_SPEED_PARAMETER_ID),
                                               new Grib2VariableSpecification(new int[] { WIND_SPEED_GRIB2_DISCIPLINE, WIND_SPEED_GRIB2_PARAMETER_CATEGORY, WIND_SPEED_GRIB2_PARAMETER_NUMBER }));

    public SpeedAndDirectionWindField(FeatureDataset... dataSets) {
        super(/* baseConfidence */ 0.5, dataSets);
    }

    @Override
    public WindWithConfidence<TimePoint> getWind(TimePoint timePoint, Position position) throws IOException {
        Triple<Double, TimePoint, Position> directionComponentInDegreesTrue = null;
        Triple<Double, TimePoint, Position> speedComponentInMetersPerSecond = null;
        for (final FeatureDataset dataSet : getDataSets()) {
            if (dataSet instanceof GridDataset) {
                for (final GridDatatype grid : ((GridDataset) dataSet).getGrids()) {
                    if (windDirectionVariableSpecification.matches(grid.getVariable())) {
                        assert isDegreesTrue(getUnit(grid.getVariable()).get());
                        directionComponentInDegreesTrue = getValue(grid, timePoint, position);
                    } else if (windSpeedVariableSpecification.matches(grid.getVariable())) {
                        assert isMetersPerSecond(getUnit(grid.getVariable()).get());
                        speedComponentInMetersPerSecond = getValue(grid, timePoint, position);
                    }
                    if (directionComponentInDegreesTrue != null && speedComponentInMetersPerSecond != null) {
                        break;
                    }
                }
            }
        }
        final Wind wind;
        final double confidence;
        if (directionComponentInDegreesTrue != null && speedComponentInMetersPerSecond != null) {
            confidence = getTimeConfidence(timePoint, directionComponentInDegreesTrue.getB());
            wind = createWindFixFromDirectionAndSpeed(directionComponentInDegreesTrue.getC(), directionComponentInDegreesTrue.getB(),
                    speedComponentInMetersPerSecond.getA(),
                    // we're getting the "from" direction from the GRIB file and need to convert to "to" here
                    directionComponentInDegreesTrue.getA());
        } else {
            wind = null;
            confidence = 0;
        }
        return new WindWithConfidenceImpl<TimePoint>(wind, confidence*getBaseConfidence(), timePoint, /* useSpeed */ true);
    }
    
    private Wind createWindFixFromDirectionAndSpeed(Position position, TimePoint timePoint, double speedInMetersPerSecond, double fromTrueDirectionInDeg) {
        return new WindImpl(position, timePoint,
                new MeterPerSecondSpeedWithDegreeBearingImpl(speedInMetersPerSecond,
                        // we're getting the "from" direction from the GRIB file and need to convert to "to" here
                        new DegreeBearingImpl(fromTrueDirectionInDeg).reverse()));
    }

    /**
     * Checks whether the data set has a wind speed variable (GRIB parameter #32) and a wind direction variable
     * (GRIB parameter #31).
     */
    public static boolean handles(FeatureDataset... dataSets) {
        return hasVariable(windDirectionVariableSpecification, dataSets) && hasVariable(windSpeedVariableSpecification, dataSets);
    }

    @Override
    public Iterable<Wind> getAllWindFixes() throws IOException {
        final List<Wind> result = new ArrayList<>();
        GridDatatype directionGrid = null;
        GridDatatype speedGrid = null;
        for (final FeatureDataset dataSet : getDataSets()) {
            for (Iterator<GridDatatype> i=((GridDataset) dataSet).getGrids().iterator(); i.hasNext() && (directionGrid==null || speedGrid==null); ) {
                final GridDatatype grid = i.next();
                if (windDirectionVariableSpecification.matches(grid.getVariable())) {
                    assert isDegreesTrue(getUnit(grid.getVariable()).get());
                    directionGrid = grid;
                } else if (windSpeedVariableSpecification.matches(grid.getVariable())) {
                    assert isMetersPerSecond(getUnit(grid.getVariable()).get());
                    speedGrid = grid;
                }
            }
        }
        if (directionGrid != null && speedGrid != null) {
            final GridDatatype finalSpeedGrid = speedGrid;
            final Map<Integer, Array> speedGridDataCache = new HashMap<>();
            for (final Wind wind : foreach(directionGrid, (Array directionGridData, int timeIndex, Index index, TimePoint timePoint, Position position)->{
                try {
                    final Wind wind;
                    Array speedGridData = speedGridDataCache.get(timeIndex);
                    if (speedGridData == null) {
                        speedGridData = finalSpeedGrid.readVolumeData(timeIndex);
                        speedGridDataCache.put(timeIndex, speedGridData);
                    }
                    double speedInMetersPerSecond = speedGridData.getDouble(index);
                    double trueDirectionFromInDeg = directionGridData.getDouble(index);
                    if (!Double.isNaN(speedInMetersPerSecond) && !Double.isNaN(trueDirectionFromInDeg)) {
                        wind = createWindFixFromDirectionAndSpeed(position, timePoint, speedInMetersPerSecond, trueDirectionFromInDeg);
                    } else {
                        wind = null;
                    }
                    return wind;
                } catch (Exception e) {
                    logger.log(Level.INFO, "Exception trying to compute wind from speed and direction", e);
                    return null;
                }
            })) {
                if (wind != null) {
                    result.add(wind);
                }
            };
        }
        return result;
    }

}
