package com.sap.sailing.polars.mining;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.LegType;
import com.sap.sse.common.Bearing;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.factories.FunctionFactory;
import com.sap.sse.datamining.functions.Function;

/**
 * Needed in the context of the datamining pipeline for the polar backend.
 * It provides the information about grouping classes.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarDataDimensionCollectionFactory {

    private static void addTackAndLegTypeDimensions(Collection<Function<?>> dimensions, FunctionFactory functionFactory)
            throws NoSuchMethodException {
        Function<LegType> legTypeFunction = functionFactory
                .createMethodWrappingFunction(LegTypePolarClusterKey.class.getMethod("getLegType",
                        new Class<?>[0]));
        dimensions.add(legTypeFunction);
    }

    private static void addPolarBaseDimension(Collection<Function<?>> dimensions, FunctionFactory functionFactory)
            throws NoSuchMethodException {
        Function<BoatClass> boatClassFunction = functionFactory
                .createMethodWrappingFunction(BasePolarClusterKey.class.getMethod("getBoatClass",
                        new Class<?>[0]));
        dimensions.add(boatClassFunction);
    }

    public static Collection<Function<?>> getCubicRegressionPerCourseClusterKeyDimensions()
            throws NoSuchMethodException {
        Collection<Function<?>> dimensions = new ArrayList<>();
        FunctionFactory functionFactory = new FunctionFactory();
        addTackAndLegTypeDimensions(dimensions, functionFactory);
        addPolarBaseDimension(dimensions, functionFactory);
        return dimensions;
    }

    public static Collection<Function<?>> getSpeedRegressionPerAngleClusterClusterKeyDimensions()
            throws NoSuchMethodException {
        Collection<Function<?>> dimensions = new ArrayList<>();
        FunctionFactory functionFactory = new FunctionFactory();

        addPolarBaseDimension(dimensions, functionFactory);

        Function<Cluster<Bearing>> angleDiffTrueWindToBoatFunction = functionFactory
                .createMethodWrappingFunction(AngleClusterPolarClusterKey.class.getMethod("getAngleCluster",
                        new Class<?>[0]));
        dimensions.add(angleDiffTrueWindToBoatFunction);

        return dimensions;
    }

}
