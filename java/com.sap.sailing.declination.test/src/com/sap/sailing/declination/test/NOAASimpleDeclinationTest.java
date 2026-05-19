package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.declination.impl.NOAAImporter;

@Disabled("Disabled because NOAA server seems down; 2026-05-19")
public class NOAASimpleDeclinationTest extends SimpleDeclinationTest<NOAAImporter> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporter();
    }
}
