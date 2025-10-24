package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.declination.impl.BGSImporter;

public class BGSSimpleDeclinationTest extends SimpleDeclinationTest<BGSImporter> {
    @BeforeEach
    public void setUp() {
        importer = new BGSImporter();
    }
}
