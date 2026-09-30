package com.sap.sailing.datamining;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterBoundary;
import com.sap.sse.datamining.data.ClusterFormatter;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.impl.data.ClusterWithLowerAndUpperBoundaries;
import com.sap.sse.datamining.impl.data.ClusterWithSingleBoundary;
import com.sap.sse.datamining.impl.data.ComparableClusterBoundary;
import com.sap.sse.datamining.impl.data.ComparisonStrategy;
import com.sap.sse.datamining.impl.data.FixClusterGroup;
import com.sap.sse.datamining.impl.data.LinearDoubleClusterGroup;
import com.sap.sse.datamining.impl.data.LocalizedCluster;
import com.sap.sse.datamining.impl.data.PercentageClusterFormatter;

public class SailingClusterGroups {
    
    private final ClusterGroup<Speed> windStrengthInBeaufortClusterGroup;
    
    private final ClusterGroup<Double> percentageClusterGroup;
    private final ClusterFormatter<Double> percentageClusterFormatter;
    
    public SailingClusterGroups() {
        Collection<Cluster<Speed>> clusters = new ArrayList<>();
        
        Speed lowerBoundWindSpeed = new KnotSpeedImpl(0.0);
        ClusterBoundary<Speed> lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        Speed upperBoundWindSpeed = new KnotSpeedImpl(1.0);
        ClusterBoundary<Speed> upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft0", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(1.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(4.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft1", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(4.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(7.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft2", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(7.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(11.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft3", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(11.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(16.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft4", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(16.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(22.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft5", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(22.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(28.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft6", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(28.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(34.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft7", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(34.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(41.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft8", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(41.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(48.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft9", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(48.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(56.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft10", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(56.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        upperBoundWindSpeed = new KnotSpeedImpl(64.0);
        upperBound = new ComparableClusterBoundary<Speed>(upperBoundWindSpeed, ComparisonStrategy.LOWER_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft11", new ClusterWithLowerAndUpperBoundaries<Speed>(lowerBound, upperBound)));
        
        lowerBoundWindSpeed = new KnotSpeedImpl(64.0);
        lowerBound = new ComparableClusterBoundary<Speed>(lowerBoundWindSpeed, ComparisonStrategy.GREATER_EQUALS_THAN);
        clusters.add(new LocalizedCluster<Speed>("Bft12", new ClusterWithSingleBoundary<Speed>(lowerBound)));
        
        windStrengthInBeaufortClusterGroup = new FixClusterGroup<Speed>(clusters);
        
        percentageClusterGroup = new LinearDoubleClusterGroup(0.0, 1.0, 0.1, true);
        percentageClusterFormatter = new PercentageClusterFormatter();
    }
    
    public ClusterGroup<Speed> getWindStrengthInBeaufortClusterGroup() {
        return windStrengthInBeaufortClusterGroup;
    }

    public ClusterGroup<Double> getPercentageClusterGroup() {
        return percentageClusterGroup;
    }
    
    public ClusterFormatter<Double> getPercentageClusterFormatter() {
        return percentageClusterFormatter;
    }

}
