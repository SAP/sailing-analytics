package com.sap.sailing.polars.clusters.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.polars.mining.BearingClusterGroup;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.datamining.data.Cluster;

public class BearingClusterGroupTest {
    
    @Test
    public void testBearingClusterGroupConstruction() {
        BearingClusterGroup group = new BearingClusterGroup(-180, 180, 5);
        for (int i = -179; i < 180; i++) {
            Cluster<Bearing> cluster = group.getClusterFor(new DegreeBearingImpl(i));
            assertThat(cluster, notNullValue());
        }
        
        Cluster<Bearing> cluster1 = group.getClusterFor(new DegreeBearingImpl(-179));
        Cluster<Bearing> cluster2 = group.getClusterFor(new DegreeBearingImpl(-178));
        
        assertTrue(cluster1.equals(cluster2));
        
        cluster1 = group.getClusterFor(new DegreeBearingImpl(-90));
        cluster2 = group.getClusterFor(new DegreeBearingImpl(-91));
        
        assertTrue(cluster1.equals(cluster2));
    }
}
