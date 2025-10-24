package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.declination.impl.NOAAImporter;

public class NOAADeclinationServiceTest extends DeclinationServiceTest<NOAAImporter> {
    @Override
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporter();
        super.setUp();
    }
}
