package com.sap.sailing.domain.yellowbrickadapter.impl;

import org.junit.jupiter.api.BeforeEach;

public class YellowBrickAdapterRMSR2019Test extends AbstractYellowBrickAdapterTest {
    @BeforeEach
    public void setUp() {
        super.setUp();
        raceName = "rmsr2019";
        expectedTimePointOfLastFix = "2019-10-26T23:55:01Z";
        expectedNumberOfCompetitors = 113;
        expectedCompetitorName = "JYS Jarhead";
        expectedCompetitorNumberOfPositions = 2009;
    }
}
