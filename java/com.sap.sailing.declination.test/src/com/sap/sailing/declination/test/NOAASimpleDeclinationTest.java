package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.declination.impl.NOAAImporter;

public class NOAASimpleDeclinationTest extends SimpleDeclinationTest<NOAAImporter> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporter();
    }
}
