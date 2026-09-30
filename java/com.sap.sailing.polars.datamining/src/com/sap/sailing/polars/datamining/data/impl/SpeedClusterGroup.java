package com.sap.sailing.polars.datamining.data.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sailing.domain.common.impl.WindSpeedSteppingWithMaxDistance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterBoundary;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.impl.data.ClusterWithLowerAndUpperBoundaries;
import com.sap.sse.datamining.impl.data.ComparableClusterBoundary;
import com.sap.sse.datamining.impl.data.ComparisonStrategy;
import com.sap.sse.datamining.impl.data.FixClusterGroup;

public class SpeedClusterGroup extends FixClusterGroup<Speed> {
    private static final long serialVersionUID = -3428022721991223921L;
    
    /**
     * A {@link SpeedClusterGroup} lets the user set the level mids for all its clusters. The Boundaries will
     * automatically be determined. They are in the middle between each level mid but only if the distance between level
     * mid and boundary is smaller or equal {@code maxDistance}
     * 
     * 
     * @param messageKey
     * @param levelMidsInKnots
     *            sorted low -> high. E.g. [2,4,6,10,15,20,30]
     * @param maxDistanceInKnots
     *            the clusters will max span <-maxDistanceInKnots-|mid|-maxDistanceinKnots->
     */
    public SpeedClusterGroup(double[] levelMidsInKnots, double maxDistanceInKnots) {
        super(createClustersForLevelMids(levelMidsInKnots, maxDistanceInKnots));
    }

    private static Collection<Cluster<Speed>> createClustersForLevelMids(double[] levelMidsInKnots,
            double maxDistanceInKnots) {
        ArrayList<Cluster<Speed>> clusterList = new ArrayList<Cluster<Speed>>();
        for (int index = 0; index < levelMidsInKnots.length; index++) {
            ClusterBoundary<Speed> lowerBoundary = createLowerBoundary(levelMidsInKnots, maxDistanceInKnots, index);
            ClusterBoundary<Speed> upperBoundary = createUpperBoundary(levelMidsInKnots, maxDistanceInKnots, index);
            Cluster<Speed> cluster = new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBoundary, upperBoundary);
            clusterList.add(cluster);
        }
        return clusterList;
    }

    private static ClusterBoundary<Speed> createUpperBoundary(double[] levelMidsInKnots, double maxDistanceInKnots,
            int index) {
        ClusterBoundary<Speed> upperBoundary;
        double upperBoundaryValue;
        if (index == levelMidsInKnots.length - 1) {
            upperBoundaryValue = levelMidsInKnots[index] + maxDistanceInKnots;
        } else {
            double biggestPossibleUpperBoundary = levelMidsInKnots[index] + maxDistanceInKnots;
            double midBetweenCurrentLevelMidAndUpperLevelMid = levelMidsInKnots[index]
                    + (0.5 * (levelMidsInKnots[index + 1] - levelMidsInKnots[index]));
            upperBoundaryValue = biggestPossibleUpperBoundary < midBetweenCurrentLevelMidAndUpperLevelMid ? biggestPossibleUpperBoundary
                    : midBetweenCurrentLevelMidAndUpperLevelMid;
        }
        upperBoundary = new ComparableClusterBoundary<Speed>(new KnotSpeedImpl(upperBoundaryValue),
                ComparisonStrategy.LOWER_THAN);
        return upperBoundary;
    }

    private static ClusterBoundary<Speed> createLowerBoundary(double[] levelMidsInKnots, double maxDistanceInKnots,
            int index) {
        ClusterBoundary<Speed> lowerBoundary;
        double lowerBoundaryValue;
        if (index > 0) {
            double lowestPossibleLowerBoundary = levelMidsInKnots[index] - maxDistanceInKnots;
            double midBetweenLowerLevelMidAndCurrentLevelMid = levelMidsInKnots[index - 1]
                    + (0.5 * (levelMidsInKnots[index] - levelMidsInKnots[index - 1]));
            lowerBoundaryValue = lowestPossibleLowerBoundary > midBetweenLowerLevelMidAndCurrentLevelMid ? lowestPossibleLowerBoundary
                    : midBetweenLowerLevelMidAndCurrentLevelMid;
        } else {
            double lowestPossibleLowerBoundary = levelMidsInKnots[index] - maxDistanceInKnots;
            lowerBoundaryValue = lowestPossibleLowerBoundary <= 0 ? 0 : lowestPossibleLowerBoundary;
        }
        lowerBoundary = new ComparableClusterBoundary<Speed>(new KnotSpeedImpl(lowerBoundaryValue),
                ComparisonStrategy.GREATER_EQUALS_THAN);
        return lowerBoundary;
    }
    
    public static ClusterGroup<Speed> createSpeedClusterGroupFrom(WindSpeedSteppingWithMaxDistance windStepping) {
        double maxDistance = windStepping.getMaxDistance();
        double[] rawIntegerStepping = windStepping.getRawStepping();
        double[] rawDoubleLevelMids = new double[rawIntegerStepping.length];
        for (int i = 0; i < rawIntegerStepping.length; i++) {
            rawDoubleLevelMids[i] = rawIntegerStepping[i];
        }

        return new SpeedClusterGroup(rawDoubleLevelMids, maxDistance);
    }


}
