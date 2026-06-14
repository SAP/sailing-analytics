package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.declination.impl.NOAAImporterForTesting;

public class NOAADeclinationImportTest extends DeclinationImportTest<NOAAImporterForTesting> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporterForTesting();
    }
}
