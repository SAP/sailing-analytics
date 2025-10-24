package com.sap.sse.security.persistence.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

public class CORSFilterConfigurationsPersistenceTest extends AbstractSecurityPersistenceTest {
    @Test
    public void testStoringAndLoadingCORSFilterWithWildcard() {
        final Map<String, Pair<Boolean, Set<String>>> configs = new HashMap<>();
        final String TESTSERVER = "testserver";
        configs.put(TESTSERVER, new Pair<>(true, Collections.emptySet()));
        mof.storeCORSFilterConfigurationIsWildcard(TESTSERVER);
        assertEquals(configs, dof.loadCORSFilterConfigurationsForReplicaSetNames());
    }

    @Test
    public void testStoringAndLoadingCORSFilterOneWithWildcardAnotherWithOriginList() {
        final Map<String, Pair<Boolean, Set<String>>> configs = new HashMap<>();
        final String TESTSERVER1 = "testserver1";
        final String TESTSERVER2 = "testserver2";
        configs.put(TESTSERVER1, new Pair<>(true, Collections.emptySet()));
        configs.put(TESTSERVER2, new Pair<>(false, Util.asSet(Arrays.asList("https://www.example.com", "https://www.anotherexample.de"))));
        mof.storeCORSFilterConfigurationIsWildcard(TESTSERVER1);
        mof.storeCORSFilterConfigurationAllowedOrigins(TESTSERVER2, Util.toArray(configs.get(TESTSERVER2).getB(), new String[0]));
        assertEquals(configs, dof.loadCORSFilterConfigurationsForReplicaSetNames());
    }
}
