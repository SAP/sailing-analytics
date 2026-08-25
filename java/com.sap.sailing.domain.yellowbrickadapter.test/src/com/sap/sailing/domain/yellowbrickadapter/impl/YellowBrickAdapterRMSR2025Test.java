package com.sap.sailing.domain.yellowbrickadapter.impl;

import org.junit.jupiter.api.BeforeEach;

public class YellowBrickAdapterRMSR2025Test extends AbstractYellowBrickAdapterTest {
    @BeforeEach
    public void setUp() {
        super.setUp();
        raceName = "rmsr2025";
        expectedTimePointOfLastFix = "2025-10-25T05:00:00Z";
        expectedNumberOfCompetitors = 118;
        expectedCompetitorName = "SR Antibes Milou";
        expectedCompetitorNumberOfPositions = 1843;
    }
}