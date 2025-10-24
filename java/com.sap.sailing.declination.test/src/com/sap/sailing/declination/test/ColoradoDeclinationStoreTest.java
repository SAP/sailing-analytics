package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.declination.impl.ColoradoImporter;

@Disabled("currently, http://magcalc.geomag.info/ seems down")
public class ColoradoDeclinationStoreTest extends DeclinationStoreTest<ColoradoImporter> {
    @Override
    @BeforeEach
    public void setUp() {
        importer = new ColoradoImporter();
        super.setUp();
    }
}
