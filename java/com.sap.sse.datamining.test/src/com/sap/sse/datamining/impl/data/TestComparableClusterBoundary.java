package com.sap.sse.datamining.impl.data;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

import com.sap.sse.datamining.data.ClusterBoundary;

public class TestComparableClusterBoundary {

    @Test
    public void testGreaterEqualsThanStrategy() {
        ClusterBoundary<Integer> greaterEqualsThan5Bound = new ComparableClusterBoundary<Integer>(5, ComparisonStrategy.GREATER_EQUALS_THAN);

        assertThat(greaterEqualsThan5Bound.contains(6), is(true));
        assertThat(greaterEqualsThan5Bound.contains(5), is(true));
        assertThat(greaterEqualsThan5Bound.contains(4), is(false));
        
        assertThat(greaterEqualsThan5Bound.toString(), is("[5"));
    }

    @Test
    public void testGreaterThanStrategy() {
        ClusterBoundary<Integer> greaterThan5Bound = new ComparableClusterBoundary<Integer>(5, ComparisonStrategy.GREATER_THAN);

        assertThat(greaterThan5Bound.contains(6), is(true));
        assertThat(greaterThan5Bound.contains(5), is(false));
        
        assertThat(greaterThan5Bound.toString(), is("]5"));
    }

    @Test
    public void testLowerEqualsThanStrategy() {
        ClusterBoundary<Integer> lowerEqualsThan5Bound = new ComparableClusterBoundary<Integer>(5, ComparisonStrategy.LOWER_EQUALS_THAN);

        assertThat(lowerEqualsThan5Bound.contains(4), is(true));
        assertThat(lowerEqualsThan5Bound.contains(5), is(true));
        assertThat(lowerEqualsThan5Bound.contains(6), is(false));
        
        assertThat(lowerEqualsThan5Bound.toString(), is("5]"));
    }

    @Test
    public void testLowerThanStrategy() {
        ClusterBoundary<Integer> lowerThan5Bound = new ComparableClusterBoundary<Integer>(5, ComparisonStrategy.LOWER_THAN);

        assertThat(lowerThan5Bound.contains(4), is(true));
        assertThat(lowerThan5Bound.contains(5), is(false));
        
        assertThat(lowerThan5Bound.toString(), is("5["));
    }

}
