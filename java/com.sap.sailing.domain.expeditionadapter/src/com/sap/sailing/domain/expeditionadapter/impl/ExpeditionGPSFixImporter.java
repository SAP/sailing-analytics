package com.sap.sailing.domain.expeditionadapter.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.server.trackfiles.impl.CompressedStreamsUtil;
import com.sap.sailing.server.trackfiles.impl.ExpeditionExtendedDataImporterImpl;
import com.sap.sailing.server.trackfiles.impl.ExpeditionImportFileHandler;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class ExpeditionGPSFixImporter implements GPSFixImporter {
    private static final Logger logger = Logger.getLogger(ExpeditionGPSFixImporter.class.getName());

    private static final String LAT_COLUMN_HEADING = ExpeditionExtendedDataImporterImpl.COL_NAME_LAT;
    private static final String LON_COLUMN_HEADING = ExpeditionExtendedDataImporterImpl.COL_NAME_LON;
    private static final String COG_COLUMN_HEADING = "cog";
    private static final String SOG_COLUMN_HEADING = "sog";
    private static final String HDT_COLUMN_HEADING = "hdt";

    @Override
    public boolean importFixes(InputStream inputStream, Charset charset, Callback callback,
            boolean inferSpeedAndBearing, final String sourceName)
            throws FormatNotSupportedException, IOException {
        TrackFileImportDeviceIdentifier device = new TrackFileImportDeviceIdentifierImpl(sourceName, getType() + "@" + new Date());
        final AtomicBoolean importedFixes = new AtomicBoolean(false);
        CompressedStreamsUtil.handlePotentiallyCompressedFiles(sourceName, inputStream,
                charset, getExpeditionImportFileHandler(callback, device, importedFixes));
        return importedFixes.get();
    }

    protected ExpeditionImportFileHandler getExpeditionImportFileHandler(Callback callback,
            TrackFileImportDeviceIdentifier device, final AtomicBoolean importedFixes) {
        return new ExpeditionImportFileHandler() {
            @Override
            protected void handleExpeditionFile(String fileName, InputStream stream, Charset charset) throws IOException {
                final BufferedReader br = new BufferedReader(new InputStreamReader(stream, charset));
                final String headerLine = br.readLine();
                final ExpeditionExtendedDataImporterImpl importer = new ExpeditionExtendedDataImporterImpl();
                final Map<String, Integer> columnDefinitions = importer.parseHeader(headerLine);
                final AtomicInteger lineNr = new AtomicInteger(0);
                br.lines().forEach(line -> {
                    if (!line.trim().isEmpty()) {
                        importer.parseLine(lineNr.incrementAndGet(), fileName, line,
                                columnDefinitions, (timePoint, columnValues, columns) -> {
                                    final double latDeg = Double
                                            .parseDouble(columnValues[columns.get(LAT_COLUMN_HEADING)]);
                                    final double lonDeg = Double
                                            .parseDouble(columnValues[columns.get(LON_COLUMN_HEADING)]);
                                    final double cogDeg = Double
                                            .parseDouble(columnValues[columns.get(COG_COLUMN_HEADING)]);
                                    final double sogKnots = Double
                                            .parseDouble(columnValues[columns.get(SOG_COLUMN_HEADING)]);
                                    final DegreePosition position = new DegreePosition(latDeg, lonDeg);
                                    Bearing optionalTrueHeading;
                                    if (columns.containsKey(HDT_COLUMN_HEADING)) {
                                        try {
                                            optionalTrueHeading = new DegreeBearingImpl(Double.parseDouble(columnValues[columns.get(HDT_COLUMN_HEADING)]))
                                                    .add(DeclinationService.INSTANCE.getDeclination(timePoint, position, /* timeout ms */ 1000).getBearingCorrectedTo(timePoint));
                                        } catch (NumberFormatException | IOException | ParseException e) {
                                            logger.log(Level.WARNING, "Problem obtaining declination for Expedition fix heading", e);
                                            optionalTrueHeading = null;
                                        }
                                    } else {
                                        optionalTrueHeading = null;
                                    }
                                    final GPSFixMoving fix = new GPSFixMovingImpl(
                                            position, timePoint,
                                            new KnotSpeedWithBearingImpl(sogKnots,
                                                    new DegreeBearingImpl(cogDeg)), optionalTrueHeading);
                                    callback.addFix(fix, device);
                                    importedFixes.set(true);
                                });
                    }
                });
            }
        };
    }

    @Override
    public Iterable<String> getSupportedFileExtensions() {
        return getExpeditionImportFileHandler(null, null, null).getSupportedFileExtensions();
    }

    @Override
    public String getType() {
        return GPSFixImporter.EXPEDITION_TYPE;
    }
}
