package com.sap.sse.datamining.impl.data;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Comparator;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterBoundary;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TestClustersWithBoundaries {

    private static final char INFINITE = '\u221e';
    
    private static final ResourceBundleStringMessages stringMessages = TestsUtil.getTestStringMessages();

    private static final Comparator<Integer> comparator = new ComparableComparator<Integer>();
    private static final ClusterBoundary<Integer> lowerBound = new ComparatorClusterBoundary<Integer>(0, ComparisonStrategy.GREATER_EQUALS_THAN, comparator);
    private static final ClusterBoundary<Integer> upperBound = new ComparatorClusterBoundary<Integer>(10, ComparisonStrategy.LOWER_THAN, comparator);
    
    @Test
    public void testLocalizedCluster() {
        Cluster<Integer> cluster = new ClusterWithLowerAndUpperBoundaries<>(lowerBound, upperBound);
        Cluster<Integer> localizedCluster = new LocalizedCluster<Integer>("TestCluster", cluster);
        
        assertThat(localizedCluster.asLocalizedString(Locale.ENGLISH, stringMessages), is("[0 - 10[ Test Cluster English"));
        assertThat(localizedCluster.asLocalizedString(Locale.GERMAN, stringMessages), is("[0 - 10[ Test Cluster Deutsch"));
    }


    @Test
    public void testClusterWithBoundaries() {
        Cluster<Integer> cluster = new ClusterWithLowerAndUpperBoundaries<>(lowerBound, upperBound);

        assertThat(cluster.isInRange(0), is(true));
        assertThat(cluster.isInRange(3), is(true));
        assertThat(cluster.isInRange(7), is(true));

        assertThat(cluster.isInRange(-1), is(false));
        assertThat(cluster.isInRange(10), is(false));
        
        assertThat(cluster.asLocalizedString(Locale.ENGLISH, stringMessages), is("[0 - 10["));
        assertThat(cluster.asLocalizedString(Locale.GERMAN, stringMessages), is("[0 - 10["));
    }
    
    @Test
    public void testClusterWithSingleBoundary() {
        Cluster<Integer> cluster = new ClusterWithSingleBoundary<>(lowerBound);

        assertThat(cluster.isInRange(0), is(true));
        assertThat(cluster.isInRange(3), is(true));
        assertThat(cluster.isInRange(Integer.MAX_VALUE), is(true));
        assertThat(cluster.isInRange(-1), is(false));

        assertThat(cluster.asLocalizedString(Locale.ENGLISH, stringMessages), is("[0 - " + INFINITE));
        assertThat(cluster.asLocalizedString(Locale.GERMAN, stringMessages), is("[0 - " + INFINITE));
        
        cluster = new ClusterWithSingleBoundary<>(upperBound);

        assertThat(cluster.isInRange(Integer.MIN_VALUE), is(true));
        assertThat(cluster.isInRange(9), is(true));
        assertThat(cluster.isInRange(10), is(false));
        assertThat(cluster.isInRange(11), is(false));

        assertThat(cluster.asLocalizedString(Locale.ENGLISH, stringMessages), is("-" + INFINITE + " - 10["));
        assertThat(cluster.asLocalizedString(Locale.GERMAN, stringMessages), is("-" + INFINITE + " - 10["));
    }

}
