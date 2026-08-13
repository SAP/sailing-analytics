package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.declination.impl.NOAAImporter;

@Disabled("NOAA seems down as of 2026-08-12")
public class NOAASimpleDeclinationTest extends SimpleDeclinationTest<NOAAImporter> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporter();
    }
}
