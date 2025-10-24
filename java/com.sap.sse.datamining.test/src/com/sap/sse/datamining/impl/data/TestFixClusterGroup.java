package com.sap.sse.datamining.impl.data;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterBoundary;
import com.sap.sse.datamining.data.ClusterGroup;

public class TestFixClusterGroup {

    private static final Comparator<Integer> comparator = new ComparableComparator<Integer>();
    private Cluster<Integer> clusterLowerThanZero;
    private Cluster<Integer> clusterFromZeroToTen;
    private Cluster<Integer> clusterFromElevenToTwenty;
    private Cluster<Integer> clusterFromTwentyOneToThirty;
    private Cluster<Integer> clusterGreaterThanThirty;

    @Test
    public void testClusterGroupWithClosedBoundaries() {
        ClusterGroup<Integer> groupWithClosedBoundaries = createGroupWithClosedBoundaries();

        assertThat(groupWithClosedBoundaries.getClusterFor(0), is(clusterFromZeroToTen));
        assertThat(groupWithClosedBoundaries.getClusterFor(10), is(clusterFromZeroToTen));

        assertThat(groupWithClosedBoundaries.getClusterFor(11), is(clusterFromElevenToTwenty));
        assertThat(groupWithClosedBoundaries.getClusterFor(20), is(clusterFromElevenToTwenty));

        assertThat(groupWithClosedBoundaries.getClusterFor(21), is(clusterFromTwentyOneToThirty));
        assertThat(groupWithClosedBoundaries.getClusterFor(30), is(clusterFromTwentyOneToThirty));
        
        assertThat(groupWithClosedBoundaries.getClusterFor(-1), nullValue());
        assertThat(groupWithClosedBoundaries.getClusterFor(31), nullValue());
    }

    private ClusterGroup<Integer> createGroupWithClosedBoundaries() {
        Collection<Cluster<Integer>> clusters = new ArrayList<>();
        clusters.add(clusterFromZeroToTen);
        clusters.add(clusterFromElevenToTwenty);
        clusters.add(clusterFromTwentyOneToThirty);
        return new FixClusterGroup<>(clusters);
    }
    
    @Test
    public void testClusterGroupWithOpenBoundaries() {
        ClusterGroup<Integer> groupWithOpenBoundaries = createGroupWithOpenBoundaries();

        assertThat(groupWithOpenBoundaries.getClusterFor(-1), is(clusterLowerThanZero));
        assertThat(groupWithOpenBoundaries.getClusterFor(31), is(clusterGreaterThanThirty));
    }
    
    private ClusterGroup<Integer> createGroupWithOpenBoundaries() {
        Collection<Cluster<Integer>> clusters = new ArrayList<>();
        clusters.add(clusterLowerThanZero);
        clusters.add(clusterFromZeroToTen);
        clusters.add(clusterFromElevenToTwenty);
        clusters.add(clusterFromTwentyOneToThirty);
        clusters.add(clusterGreaterThanThirty);
        return new FixClusterGroup<>(clusters);
    }

    @BeforeEach
    public void initializeClusters() {
        ClusterBoundary<Integer> lowerThanZero = new ComparatorClusterBoundary<Integer>(0, ComparisonStrategy.LOWER_THAN, comparator);
        clusterLowerThanZero = new ClusterWithSingleBoundary<>(lowerThanZero);
        
        ClusterBoundary<Integer> greaterEqualsThanZero = new ComparatorClusterBoundary<Integer>(0, ComparisonStrategy.GREATER_EQUALS_THAN, new ComparableComparator<Integer>());
        ClusterBoundary<Integer> lowerEqualsThanTen = new ComparatorClusterBoundary<Integer>(10, ComparisonStrategy.LOWER_EQUALS_THAN, new ComparableComparator<Integer>());
        clusterFromZeroToTen = new ClusterWithLowerAndUpperBoundaries<>(greaterEqualsThanZero, lowerEqualsThanTen);

        ClusterBoundary<Integer> greaterThanTen = new ComparatorClusterBoundary<Integer>(10, ComparisonStrategy.GREATER_THAN, new ComparableComparator<Integer>());
        ClusterBoundary<Integer> lowerEqualsThanTwenty = new ComparatorClusterBoundary<Integer>(20, ComparisonStrategy.LOWER_EQUALS_THAN, new ComparableComparator<Integer>());
        clusterFromElevenToTwenty = new ClusterWithLowerAndUpperBoundaries<>(greaterThanTen, lowerEqualsThanTwenty);

        ClusterBoundary<Integer> greaterThanTwenty = new ComparatorClusterBoundary<Integer>(20, ComparisonStrategy.GREATER_THAN, new ComparableComparator<Integer>());
        ClusterBoundary<Integer> lowerEqualsThanThirty = new ComparatorClusterBoundary<Integer>(30, ComparisonStrategy.LOWER_EQUALS_THAN, new ComparableComparator<Integer>());
        clusterFromTwentyOneToThirty = new ClusterWithLowerAndUpperBoundaries<>(greaterThanTwenty, lowerEqualsThanThirty);

        ClusterBoundary<Integer> greaterThanThirty = new ComparatorClusterBoundary<Integer>(30, ComparisonStrategy.GREATER_THAN, comparator);
        clusterGreaterThanThirty = new ClusterWithSingleBoundary<>(greaterThanThirty);
    }

}
