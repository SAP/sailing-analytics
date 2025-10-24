package com.sap.sailing.domain.vakarosadapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.server.trackfiles.impl.CompressedStreamsUtil;
import com.sap.sailing.server.trackfiles.impl.ExpeditionImportFileHandler;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class VakarosGPSFixImporter implements GPSFixImporter {

    private static final Logger logger = Logger.getLogger(VakarosGPSFixImporter.class.getName());

    private static final List<String> supportedExpeditionLogFileExtensions = Arrays.asList("csv", "vak", "zip", "gz");

    static final String TIMESTAMP_COLUMN_HEADING = "timestamp";
    private static final String LAT_COLUMN_HEADING = "latitude";
    private static final String LON_COLUMN_HEADING = "longitude";
    private static final String SOG_COLUMN_HEADING = "sog_kts";
    private static final String COG_COLUMN_HEADING = "cog";
    private static final String HDG_COLUMN_HEADING = "hdg_true";
    
    private static final String VAKAROS_TYPE = "Vakaros";

    @Override
    public boolean importFixes(InputStream inputStream, Charset charset, Callback callback,
            boolean inferSpeedAndBearing, final String sourceName)
            throws FormatNotSupportedException, IOException {
        final TrackFileImportDeviceIdentifier gpsDevice = new TrackFileImportDeviceIdentifierImpl(sourceName, getType() + "@" + new Date());
        final AtomicBoolean importedFixes = new AtomicBoolean(false);
        CompressedStreamsUtil.handlePotentiallyCompressedFiles(sourceName, inputStream,
                charset, new ExpeditionImportFileHandler(Arrays.asList("vak", "csv", "log", "txt")) {
                    @Override
                    protected void handleExpeditionFile(String fileName, InputStream stream, Charset charset) throws IOException {
                        final BufferedReader br = new BufferedReader(new InputStreamReader(stream, charset));
                        final String headerLine = br.readLine();
                        final VakarosExtendedDataImporterImpl importer = new VakarosExtendedDataImporterImpl();
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
                                            if (columns.containsKey(HDG_COLUMN_HEADING)) {
                                                try {
                                                    optionalTrueHeading = new DegreeBearingImpl(Double.parseDouble(columnValues[columns.get(HDG_COLUMN_HEADING)]));
                                                } catch (NumberFormatException e) {
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
                                            callback.addFix(fix, gpsDevice);
                                            importedFixes.set(true);
                                        });
                            }
                        });
                    }
                });
        return importedFixes.get();
    }
    
    @Override
    public Iterable<String> getSupportedFileExtensions() {
        return supportedExpeditionLogFileExtensions;
    }

    @Override
    public String getType() {
        return VAKAROS_TYPE;
    }
}
