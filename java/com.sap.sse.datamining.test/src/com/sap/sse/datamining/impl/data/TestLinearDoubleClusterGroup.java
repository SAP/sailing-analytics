package com.sap.sse.datamining.impl.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.data.Cluster;

public class TestLinearDoubleClusterGroup {
    
    private LinearDoubleClusterGroup hardPercentageClusterGroup;
    private LinearDoubleClusterGroup softLinearClusterGroup;
    
    @BeforeEach
    public void initialize() {
        hardPercentageClusterGroup = new LinearDoubleClusterGroup(0.0, 1.0, 0.1, true);
        softLinearClusterGroup = new LinearDoubleClusterGroup(0, 100, 10, false);
    }

    @Test
    public void testGetClusterForValueOutOfBounds() {
        Cluster<Double> cluster = hardPercentageClusterGroup.getClusterFor(-0.01);
        assertThat(cluster, nullValue());
        
        cluster = hardPercentageClusterGroup.getClusterFor(1.01);
        assertThat(cluster, nullValue());
        
        cluster = softLinearClusterGroup.getClusterFor(-0.1);
        Cluster<Double> expectedCluster = createCluster(softLinearClusterGroup.getStepSize(), ComparisonStrategy.LOWER_THAN);
        assertThat(cluster, is(expectedCluster));
        
        cluster = softLinearClusterGroup.getClusterFor(100.1);
        expectedCluster = createCluster(softLinearClusterGroup.getUpperGroupBound() - softLinearClusterGroup.getStepSize(), ComparisonStrategy.GREATER_EQUALS_THAN);
        assertThat(cluster, is(expectedCluster));
    }
    
    @Test
    public void testGetClusterForBoundaryValues() {
        Cluster<Double> cluster = hardPercentageClusterGroup.getClusterFor(0.0);
        Cluster<Double> expectedCluster = createCluster(hardPercentageClusterGroup.getLowerGroupBound(), ComparisonStrategy.GREATER_EQUALS_THAN,
                hardPercentageClusterGroup.getLowerGroupBound() + hardPercentageClusterGroup.getStepSize(), ComparisonStrategy.LOWER_THAN);
        assertThat(cluster, is(expectedCluster));
        
        cluster = hardPercentageClusterGroup.getClusterFor(1.0);
        expectedCluster = createCluster(hardPercentageClusterGroup.getUpperGroupBound() - hardPercentageClusterGroup.getStepSize(), ComparisonStrategy.GREATER_EQUALS_THAN,
                                        hardPercentageClusterGroup.getUpperGroupBound(), ComparisonStrategy.LOWER_EQUALS_THAN);
        assertThat(cluster, is(expectedCluster));
        
        cluster = softLinearClusterGroup.getClusterFor(0.0);
        expectedCluster = createCluster(softLinearClusterGroup.getStepSize(), ComparisonStrategy.LOWER_THAN);
        assertThat(cluster, is(expectedCluster));
        
        cluster = softLinearClusterGroup.getClusterFor(100.0);
        expectedCluster = createCluster(softLinearClusterGroup.getUpperGroupBound() - softLinearClusterGroup.getStepSize(), ComparisonStrategy.GREATER_EQUALS_THAN);
        assertThat(cluster, is(expectedCluster));
    }
    
    @Test
    public void testGetClusterFor() {
        LinearDoubleClusterGroup clusterGroup = softLinearClusterGroup;
        double value = (clusterGroup.getUpperGroupBound() + clusterGroup.getStepSize()) / 2;
        Cluster<Double> cluster = clusterGroup.getClusterFor(value);
        Cluster<Double> expectedCluster = createCluster(clusterGroup.getUpperGroupBound() / 2, ComparisonStrategy.GREATER_EQUALS_THAN,
                (clusterGroup.getUpperGroupBound() / 2) + clusterGroup.getStepSize(), ComparisonStrategy.LOWER_THAN);
        assertThat(cluster, is(expectedCluster));
    }

    private Cluster<Double> createCluster(double lowerBoundValue, ComparisonStrategy lowerComparisonStrategy, double upperBoundValue, ComparisonStrategy upperComparisonStrategy) {
        ComparableClusterBoundary<Double> lowerBound = new ComparableClusterBoundary<>(lowerBoundValue, lowerComparisonStrategy);
        ComparableClusterBoundary<Double> upperBound = new ComparableClusterBoundary<>(upperBoundValue, upperComparisonStrategy);
        return new ClusterWithLowerAndUpperBoundaries<>(lowerBound, upperBound);
    }
    
    private Cluster<Double> createCluster(double boundValue, ComparisonStrategy comparisonStrategy) {
        return new ClusterWithSingleBoundary<>(new ComparableClusterBoundary<>(boundValue, comparisonStrategy));
    }

}
