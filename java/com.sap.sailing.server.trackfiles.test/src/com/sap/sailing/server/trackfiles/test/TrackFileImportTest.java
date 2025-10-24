package com.sap.sailing.server.trackfiles.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.trackimport.GPSFixImporter.Callback;
import com.sap.sailing.server.trackfiles.impl.RouteConverterGPSFixImporterImpl;

public class TrackFileImportTest {
    private boolean callbackCalled = false;

    @BeforeEach
    public void setup() {
        callbackCalled = false;
    }

    @Test
    public void testGpx() throws IOException, FormatNotSupportedException {
        try (InputStream in = getClass().getResourceAsStream("/Cardiff Race17 - COMPETITORS.gpx")) {
            new RouteConverterGPSFixImporterImpl().importFixes(in, Charset.defaultCharset(), new Callback() {
                @Override
                public void addFix(GPSFix fix, TrackFileImportDeviceIdentifier device) {
                    if (fix instanceof GPSFixMoving) {
                        callbackCalled = true;
                    }
                }
            }, false, "source");
            assertTrue(callbackCalled);
        }
    }

    @Test
    public void testKmlOnlyLatLong() throws IOException, FormatNotSupportedException {
        try (InputStream in = getClass().getResourceAsStream("/Cardiff Race22 - COMPETITORS.kml")) {
            new RouteConverterGPSFixImporterImpl().importFixes(in, Charset.defaultCharset(), new Callback() {
                @Override
                public void addFix(GPSFix fix, TrackFileImportDeviceIdentifier device) {
                    if (fix instanceof GPSFix) {
                        callbackCalled = true;
                    }
                }
            }, false, "source");
            assertTrue(callbackCalled);
        }
    }
}
