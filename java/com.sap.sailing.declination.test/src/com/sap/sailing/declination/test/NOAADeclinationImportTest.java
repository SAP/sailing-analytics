package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.declination.impl.NOAAImporterForTesting;

@Disabled("NOAA seems down as of 2026-08-12")
public class NOAADeclinationImportTest extends DeclinationImportTest<NOAAImporterForTesting> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporterForTesting();
    }
}
