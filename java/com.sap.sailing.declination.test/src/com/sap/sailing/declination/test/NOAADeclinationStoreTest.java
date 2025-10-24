package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.declination.impl.NOAAImporter;

public class NOAADeclinationStoreTest extends DeclinationStoreTest<NOAAImporter> {
    @Override
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporter();
        super.setUp();
    }
}
