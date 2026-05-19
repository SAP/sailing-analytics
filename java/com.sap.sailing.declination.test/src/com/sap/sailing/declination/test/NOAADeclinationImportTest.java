package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.declination.impl.NOAAImporterForTesting;

@Disabled("Disabled because NOAA server seems down; 2026-05-19")
public class NOAADeclinationImportTest extends DeclinationImportTest<NOAAImporterForTesting> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporterForTesting();
    }
}
