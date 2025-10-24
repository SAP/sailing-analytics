package com.sap.sailing.domain.vakarosadapter.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.sensordata.BravoExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.BaseDoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.trackimport.GPSFixImporter.Callback;
import com.sap.sailing.domain.vakarosadapter.VakarosExtendedDataImporterImpl;
import com.sap.sailing.domain.vakarosadapter.VakarosGPSFixImporter;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class TestReadingVakarosFiles {
    private final static double EPSILON = 0.000000001;
    private VakarosGPSFixImporter gpsFixImporter;
    private VakarosExtendedDataImporterImpl sensorFixImporter;

    @BeforeEach
    public void setUp() {
        gpsFixImporter = new VakarosGPSFixImporter();
        sensorFixImporter = new VakarosExtendedDataImporterImpl();
    }
    
    @Test
    public void testReadFileWithLoadValues() throws FormatNotSupportedException, IOException, ParseException {
        final String sourceName = "/GDF-8-11-2024.csv.gz";
        final Map<TrackFileImportDeviceIdentifier, List<GPSFix>> gpsFixes = new HashMap<>();
        final Map<TrackFileImportDeviceIdentifier, List<DoubleVectorFix>> sensorFixes = new HashMap<>();
        readGPSFixesAndAssertNotEmpty(sourceName, gpsFixes);
        readSensorFixesAndAssertNotEmpty(sourceName, sensorFixes);
        // check for presence of fixes corresponding to line:
        //   timestamp,latitude,longitude,sog_kts,cog,hdg_true,roll,pitch,load_GDF2,load_GDF1
        //   2024-11-08T08:38:56.077,41.7387700,12.2457483,0.000,113.1,125.4,-1.7,4.6,1.40,141.20
        final TimePoint timePoint = TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse("2024-11-08T08:38:56.077"+"Z"));
        boolean foundGPSFix = false;
        for (final GPSFix fix : gpsFixes.values().iterator().next()) {
            final GPSFixMoving fixMoving = (GPSFixMoving) fix;
            if (fix.getTimePoint().equals(timePoint) &&
                Math.abs(fix.getPosition().getLatDeg() - 41.7387700) < EPSILON &&
                Math.abs(fix.getPosition().getLngDeg() - 12.2457483) < EPSILON &&
                Math.abs(fixMoving.getSpeed().getKnots() - 0.000) < EPSILON &&
                Math.abs(fixMoving.getSpeed().getBearing().getDegrees() - 113.1) < EPSILON &&
                Math.abs(fixMoving.getOptionalTrueHeading().getDegrees() - 125.4) < EPSILON) {
                foundGPSFix = true;
                break;
            }
        }
        assertTrue(foundGPSFix);
        boolean foundSensorFix = false;
        for (final DoubleVectorFix sensorFix : sensorFixes.values().iterator().next()) {
            if (sensorFix.getTimePoint().equals(timePoint) &&
                Math.abs(sensorFix.get(BravoExtendedSensorDataMetadata.HEEL.getColumnIndex()) - -1.7) < EPSILON &&
                Math.abs(sensorFix.get(BravoExtendedSensorDataMetadata.PITCH.getColumnIndex()) - 4.6) < EPSILON &&
                Math.abs(sensorFix.get(BravoExtendedSensorDataMetadata.EXPEDITION_KICKER_TENSION.getColumnIndex()) - 1.4) < EPSILON &&
                Math.abs(sensorFix.get(BravoExtendedSensorDataMetadata.FORESTAY_LOAD.getColumnIndex()) - 0.1412) < EPSILON) {
                foundSensorFix = true;
                break;
            }
        }
        assertTrue(foundSensorFix);
    }

    private void readGPSFixesAndAssertNotEmpty(final String sourceName,
            final Map<TrackFileImportDeviceIdentifier, List<GPSFix>> gpsFixes)
            throws FormatNotSupportedException, IOException {
        final Callback callback = new Callback() {
            @Override
            public void addFix(GPSFix newFix, TrackFileImportDeviceIdentifier device) {
                gpsFixes.computeIfAbsent(device, d->new ArrayList<>()).add(newFix);
            }
        };
        gpsFixImporter.importFixes(getClass().getResourceAsStream(sourceName), Charset.forName("UTF-8"), callback, /* inferSpeedAndBearing */ false, sourceName);
        assertFalse(gpsFixes.isEmpty());
        for (final List<GPSFix> gpsFixList : gpsFixes.values()) {
            assertFalse(gpsFixList.isEmpty());
        }
    }
    
    private void readSensorFixesAndAssertNotEmpty(final String sourceName,
            final Map<TrackFileImportDeviceIdentifier, List<DoubleVectorFix>> sensorFixes)
            throws FormatNotSupportedException, IOException {
        final com.sap.sailing.domain.trackimport.BaseDoubleVectorFixImporter.Callback callback = new BaseDoubleVectorFixImporter.Callback() {
            @Override
            public void addFixes(Iterable<DoubleVectorFix> fixes, TrackFileImportDeviceIdentifier device) {
                Util.addAll(fixes, sensorFixes.computeIfAbsent(device, d->new ArrayList<>()));
            }
        };
        sensorFixImporter.importFixes(getClass().getResourceAsStream(sourceName), Charset.forName("UTF-8"), callback, /* filename */ sourceName, sourceName, /* downsample */ false);
        assertFalse(sensorFixes.isEmpty());
        for (final List<DoubleVectorFix> sensorFixList : sensorFixes.values()) {
            assertFalse(sensorFixList.isEmpty());
        }
    }
    
    @Test
    public void testReadFileWithoutLoadValues() throws FormatNotSupportedException, IOException {
        final String sourceName = "/GDF-13-12-2024.csv.zip";
        final Map<TrackFileImportDeviceIdentifier, List<GPSFix>> gpsFixes = new HashMap<>();
        final Map<TrackFileImportDeviceIdentifier, List<DoubleVectorFix>> sensorFixes = new HashMap<>();
        readGPSFixesAndAssertNotEmpty(sourceName, gpsFixes);
        readSensorFixesAndAssertNotEmpty(sourceName, sensorFixes);
    }
}
