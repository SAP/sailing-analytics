package com.sap.sailing.polars.clusters.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.polars.mining.BearingClusterGroup;
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
    }
}
