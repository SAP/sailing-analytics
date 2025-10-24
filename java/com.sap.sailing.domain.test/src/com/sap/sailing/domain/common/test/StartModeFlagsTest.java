package com.sap.sailing.domain.common.test;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.racelog.Flags;

public class StartModeFlagsTest {
    @Test
    public void testStartModeFlags() {
        Arrays.asList(Flags.getStartModeFlags()).contains(Flags.PAPA);
    }
}
