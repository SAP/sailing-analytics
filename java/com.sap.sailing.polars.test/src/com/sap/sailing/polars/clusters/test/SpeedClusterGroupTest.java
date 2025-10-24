package com.sap.sailing.polars.clusters.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.polars.datamining.data.impl.SpeedClusterGroup;
import com.sap.sse.common.Speed;
import com.sap.sse.datamining.data.Cluster;

public class SpeedClusterGroupTest {

    @Test
    public void testWithZeroLevels() {
        double[] levelMidsInKnots = {};
        SpeedClusterGroup group = new SpeedClusterGroup(levelMidsInKnots, 4);
        assertThat(group.getClusterFor(new KnotSpeedImpl(0)), nullValue());
        assertThat(group.getClusterFor(new KnotSpeedImpl(6)), nullValue());
    }

    @Test
    public void testWithOneLevelReachingZero() {
        double[] levelMidsInKnots = { 4 };
        SpeedClusterGroup group = new SpeedClusterGroup(levelMidsInKnots, 4);
        Cluster<Speed> clusterForZero = group.getClusterFor(new KnotSpeedImpl(0));
        assertThat(clusterForZero, notNullValue());
        Cluster<Speed> clusterForSix = group.getClusterFor(new KnotSpeedImpl(6));
        assertThat(clusterForSix, notNullValue());
        assertThat(clusterForSix == clusterForZero, equalTo(true));
        assertThat(group.getClusterFor(new KnotSpeedImpl(9)), nullValue());
    }

    @Test
    public void testWithOneLevelNotReachingZero() {
        double[] levelMidsInKnots = { 7 };
        SpeedClusterGroup group = new SpeedClusterGroup(levelMidsInKnots, 4);
        Cluster<Speed> clusterForNine = group.getClusterFor(new KnotSpeedImpl(9));
        assertThat(clusterForNine, notNullValue());
        Cluster<Speed> clusterForSix = group.getClusterFor(new KnotSpeedImpl(6));
        assertThat(clusterForSix, notNullValue());
        assertThat(clusterForSix == clusterForNine, equalTo(true));
        assertThat(group.getClusterFor(new KnotSpeedImpl(0)), nullValue());
    }

    @Test
    public void testWithMultipleLevelsWithNoEmptyRoomInbetween() {
        double[] levelMidsInKnots = { 4, 6, 8, 10, 12, 15 };
        SpeedClusterGroup group = new SpeedClusterGroup(levelMidsInKnots, 4);
        Cluster<Speed> clusterForFourteen = group.getClusterFor(new KnotSpeedImpl(14));
        assertThat(clusterForFourteen, notNullValue());
        Cluster<Speed> clusterForThirteen = group.getClusterFor(new KnotSpeedImpl(13));
        assertThat(clusterForThirteen, notNullValue());
        assertThat(clusterForThirteen == clusterForFourteen, equalTo(false));
        assertThat(group.getClusterFor(new KnotSpeedImpl(20)), nullValue());
    }

    @Test
    public void testWithMultipleLevelsWithRoomInbetween() {
        double[] levelMidsInKnots = { 4, 6, 15 };
        SpeedClusterGroup group = new SpeedClusterGroup(levelMidsInKnots, 4);
        Cluster<Speed> clusterForFourteen = group.getClusterFor(new KnotSpeedImpl(14));
        assertThat(clusterForFourteen, notNullValue());
        Cluster<Speed> clusterForSeven = group.getClusterFor(new KnotSpeedImpl(7));
        assertThat(clusterForSeven, notNullValue());
        assertThat(clusterForSeven == clusterForFourteen, equalTo(false));
        assertThat(group.getClusterFor(new KnotSpeedImpl(10.5)), nullValue());
    }
}
