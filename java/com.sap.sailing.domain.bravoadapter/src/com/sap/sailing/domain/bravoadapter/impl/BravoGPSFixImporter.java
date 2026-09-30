package com.sap.sailing.domain.bravoadapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.sensordata.BravoExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.server.gateway.windimport.bravo.FunnyDegreeConverter;
import com.sap.sailing.server.trackfiles.impl.BravoExtendedDataImporterImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class BravoGPSFixImporter implements GPSFixImporter {
    private static final Logger logger = Logger.getLogger(BravoGPSFixImporter.class.getName());

    @Override
    public boolean importFixes(InputStream inputStream, Charset charset, Callback callback, boolean inferSpeedAndBearing, final String filename)
            throws FormatNotSupportedException, IOException {
        final AtomicBoolean importedFixes = new AtomicBoolean(false);
        TrackFileImportDeviceIdentifier device = new TrackFileImportDeviceIdentifierImpl(filename, getType() + "@" + new Date());
        new BravoExtendedDataImporterImpl().importFixes(inputStream,
                charset, (Iterable<DoubleVectorFix> fixes, TrackFileImportDeviceIdentifier deviceIdentifier)->{
                    for (final DoubleVectorFix fix : fixes) {
                        final DegreePosition position = new DegreePosition(FunnyDegreeConverter.funnyLatLng(fix.get(BravoExtendedSensorDataMetadata.LAT.getColumnIndex())),
                                FunnyDegreeConverter.funnyLatLng(fix.get(BravoExtendedSensorDataMetadata.LON.getColumnIndex())));
                        Bearing optionalTrueHeading;
                        if (fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_HDG.getColumnIndex()) != null) {
                            try {
                                optionalTrueHeading = new DegreeBearingImpl(fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_HDG.getColumnIndex())).add(
                                        DeclinationService.INSTANCE.getDeclination(fix.getTimePoint(), position, /* timeout for online lookup in millis */ 100)
                                            .getBearingCorrectedTo(fix.getTimePoint()));
                            } catch (IOException | ParseException e) {
                                logger.log(Level.WARNING, "Problem looking up magnetic declination for Bravo fix", e);
                                optionalTrueHeading = null;
                            }
                        } else {
                            optionalTrueHeading = null;
                        }
                        GPSFixMoving gpsFix = new GPSFixMovingImpl(
                                position,
                                fix.getTimePoint(),
                                new KnotSpeedWithBearingImpl(fix.get(BravoExtendedSensorDataMetadata.SOG.getColumnIndex()),
                                        new DegreeBearingImpl(fix.get(BravoExtendedSensorDataMetadata.COG.getColumnIndex()))),
                                optionalTrueHeading);
                        callback.addFix(gpsFix, device);
                        importedFixes.set(true);
                    }
                }, filename, /* sourceName */ getType(), /* downsample */ false);
        return importedFixes.get();
    }

    @Override
    public Iterable<String> getSupportedFileExtensions() {
        return Arrays.asList(new String[] { "csv", "log", "txt" });
    }

    @Override
    public String getType() {
        return "Bravo";
    }
}
