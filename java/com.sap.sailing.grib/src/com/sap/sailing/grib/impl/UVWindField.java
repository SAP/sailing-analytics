package com.sap.sailing.grib.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.sap.sse.common.impl.MeterPerSecondSpeedWithDegreeBearingImpl;
import com.sap.sse.common.impl.RadianBearingImpl;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;

public class UVWindField extends AbstractGribWindFieldImpl {
    private static final Logger logger = Logger.getLogger(UVWindField.class.getName());
    private static final int U_COMPONENT_OF_WIND_PARAMETER_ID = 33;
    private static final int U_COMPONENT_OF_WIND_GRIB2_DISCIPLINE = 0;
    private static final int U_COMPONENT_OF_WIND_GRIB2_PARAMETER_CATEGORY = 2;
    private static final int U_COMPONENT_OF_WIND_GRIB2_PARAMETER_NUMBER = 2;

    private static final int V_COMPONENT_OF_WIND_PARAMETER_ID = 34;
    private static final int V_COMPONENT_OF_WIND_GRIB2_DISCIPLINE = 0;
    private static final int V_COMPONENT_OF_WIND_GRIB2_PARAMETER_CATEGORY = 2;
    private static final int V_COMPONENT_OF_WIND_GRIB2_PARAMETER_NUMBER = 3;
    
    private static final VariableSpecification uComponentOfWindVariableSpecification =
            new CompositeVariableSpecification(new Grib1VariableSpecification(U_COMPONENT_OF_WIND_PARAMETER_ID),
                                               new Grib2VariableSpecification(new int[] { U_COMPONENT_OF_WIND_GRIB2_DISCIPLINE, U_COMPONENT_OF_WIND_GRIB2_PARAMETER_CATEGORY, U_COMPONENT_OF_WIND_GRIB2_PARAMETER_NUMBER }));

    private static final VariableSpecification vComponentOfWindVariableSpecification =
            new CompositeVariableSpecification(new Grib1VariableSpecification(V_COMPONENT_OF_WIND_PARAMETER_ID),
                                               new Grib2VariableSpecification(new int[] { V_COMPONENT_OF_WIND_GRIB2_DISCIPLINE, V_COMPONENT_OF_WIND_GRIB2_PARAMETER_CATEGORY, V_COMPONENT_OF_WIND_GRIB2_PARAMETER_NUMBER }));

    public UVWindField(FeatureDataset... dataSets) {
        super(/* baseConfidence */ 0.5, dataSets);
    }

    @Override
    public WindWithConfidence<TimePoint> getWind(TimePoint timePoint, Position position) throws IOException {
        final Wind wind;
        final double confidence;
        Triple<Double, TimePoint, Position> uComponentInMetersPerSecond = null;
        Triple<Double, TimePoint, Position> vComponentInMetersPerSecond = null;
        for (final FeatureDataset dataSet : getDataSets()) {
            if (dataSet instanceof GridDataset) {
                for (final GridDatatype grid : ((GridDataset) dataSet).getGrids()) {
                    if (uComponentOfWindVariableSpecification.matches(grid.getVariable())) {
                        assert isMetersPerSecond(getUnit(grid.getVariable()).get());
                        uComponentInMetersPerSecond = getValue(grid, timePoint, position);
                    } else if (vComponentOfWindVariableSpecification.matches(grid.getVariable())) {
                        assert isMetersPerSecond(getUnit(grid.getVariable()).get());
                        vComponentInMetersPerSecond = getValue(grid, timePoint, position);
                    }
                    if (uComponentInMetersPerSecond != null && vComponentInMetersPerSecond != null) {
                        break;
                    }
                }
            }
        }
        if (uComponentInMetersPerSecond != null && vComponentInMetersPerSecond != null) {
            confidence = getTimeConfidence(timePoint, uComponentInMetersPerSecond.getB());
            wind = createWindFixFromUAndV(uComponentInMetersPerSecond.getC(), uComponentInMetersPerSecond.getB(),
                    uComponentInMetersPerSecond.getA(), vComponentInMetersPerSecond.getA());
        } else {
            wind = null;
            confidence = 0;
        }
        return new WindWithConfidenceImpl<TimePoint>(wind, confidence*getBaseConfidence(), timePoint, /* useSpeed */ true);
    }

    /**
     * Checks whether the data set has a u-component of wind (GRIB parameter #33) and a v-component of wind
     * (GRIB parameter #34).
     */
    public static boolean handles(FeatureDataset... dataSets) {
        return hasVariable(uComponentOfWindVariableSpecification, dataSets) && hasVariable(vComponentOfWindVariableSpecification, dataSets);
    }

    @Override
    public Iterable<Wind> getAllWindFixes() throws IOException {
        final List<Wind> result = new ArrayList<>();
        for (final FeatureDataset dataSet : getDataSets()) {
            GridDatatype uGrid = null;
            GridDatatype vGrid = null;
            for (Iterator<GridDatatype> i=((GridDataset) dataSet).getGrids().iterator(); i.hasNext() && (uGrid==null || vGrid==null); ) {
                final GridDatatype grid = i.next();
                if (uComponentOfWindVariableSpecification.matches(grid.getVariable())) {
                    assert isMetersPerSecond(getUnit(grid.getVariable()).get());
                    uGrid = grid;
                } else if (vComponentOfWindVariableSpecification.matches(grid.getVariable())) {
                    assert isMetersPerSecond(getUnit(grid.getVariable()).get());
                    vGrid = grid;
                }
            }
            if (uGrid != null && vGrid != null) {
                final GridDatatype finalVGrid = vGrid;
                final Map<Integer, Array> vGridDataCache = new HashMap<>();
                for (final Wind wind : foreach(uGrid, (Array uGridData, int timeIndex, Index index, TimePoint timePoint, Position position)->{
                    assert Arrays.equals(index.getShape(), uGridData.getShape());
                    try {
                        final Wind wind;
                        Array vGridData = vGridDataCache.get(timeIndex);
                        if (vGridData == null) {
                            vGridData = finalVGrid.readVolumeData(timeIndex);
                            vGridDataCache.put(timeIndex, vGridData);
                        }
                        double uComponentInMetersPerSecond = uGridData.getDouble(index);
                        double vComponentInMetersPerSecond = vGridData.getDouble(index);
                        if (!Double.isNaN(uComponentInMetersPerSecond) && !Double.isNaN(vComponentInMetersPerSecond)) {
                            wind = createWindFixFromUAndV(position, timePoint, uComponentInMetersPerSecond, vComponentInMetersPerSecond);
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
        }
        return result;
    }

    private Wind createWindFixFromUAndV(Position position, TimePoint timePoint, double uComponentInMetersPerSecond,
            double vComponentInMetersPerSecond) {
        final double atan2 = Math.atan2(uComponentInMetersPerSecond, vComponentInMetersPerSecond);
        return new WindImpl(position, timePoint,
                new MeterPerSecondSpeedWithDegreeBearingImpl(
                        Math.sqrt(uComponentInMetersPerSecond * uComponentInMetersPerSecond
                                + vComponentInMetersPerSecond * vComponentInMetersPerSecond),
                        new RadianBearingImpl(atan2 > 0 ? atan2 : 2 * Math.PI + atan2)));
    }
}
