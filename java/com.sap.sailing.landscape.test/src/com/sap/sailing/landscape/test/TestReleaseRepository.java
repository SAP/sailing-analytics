package com.sap.sailing.landscape.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.sap.sailing.landscape.SailingReleaseRepository;
import com.sap.sse.common.Util;

public class TestReleaseRepository {
    @Test
    public void testForAtLeastOneRelease() {
        assertFalse(Util.isEmpty(SailingReleaseRepository.INSTANCE));
    }

    @Test
    public void testForAtLeastOneMasterRelease() {
        assertNotNull(SailingReleaseRepository.INSTANCE.getLatestMasterRelease());
    }
}
