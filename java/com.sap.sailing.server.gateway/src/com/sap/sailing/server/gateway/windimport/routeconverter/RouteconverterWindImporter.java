package com.sap.sailing.server.gateway.windimport.routeconverter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.server.gateway.windimport.AbstractWindImporter;
import com.sap.sailing.server.trackfiles.RouteConverterGPSFixImporterFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import slash.common.io.NotClosingUnderlyingInputStream;

public class RouteconverterWindImporter extends AbstractWindImporter {
    private static final Logger logger = Logger.getLogger(RouteconverterWindImporter.class.getName());

    @Override
    protected WindSource getDefaultWindSource(UploadRequest uploadRequest) {
        final WindSource windSource;
        final String sourceName;
        logger.info("Importing Routeconverter wind data from "+uploadRequest.files);
        if (uploadRequest.files != null && !uploadRequest.files.isEmpty()) {
            sourceName = uploadRequest.files.toString();
        } else {
            sourceName = "Routeconverter Wind Import";
        }
        windSource = new WindSourceWithAdditionalID(WindSourceType.WEB, sourceName + "@" + MillisecondsTimePoint.now());
        return windSource;
    }

    @Override
    protected Map<WindSource, Iterable<Wind>> importWind(WindSource defaultWindSource,
            Map<InputStream, Pair<String, Charset>> inputStreamsAndFilenamesAndCharsets) throws Exception {
        final Map<WindSource, Iterable<Wind>> result = new HashMap<>();
        if (inputStreamsAndFilenamesAndCharsets != null) {
            for (final Entry<InputStream, Pair<String, Charset>> inputStreamAndFileName : inputStreamsAndFilenamesAndCharsets.entrySet()) {
                logger.info("Reading Routeconverter wind data from "+inputStreamAndFileName.getValue());
                final Map<WindSource, ? extends Iterable<Wind>> windFixesFromStream = readWind(inputStreamAndFileName.getValue().getA(), inputStreamAndFileName.getKey(),
                        inputStreamAndFileName.getValue().getB());
                result.putAll(windFixesFromStream); // we assume that no WindSource appears in more than one stream; otherwise the last one found replaces earlier occurrences
            }
        }
        return result;
    }

    private Map<WindSource, ? extends Iterable<Wind>> readWind(String filename, InputStream inputStream, Charset charset) throws Exception {
        try {
            final Map<WindSource, List<Wind>> result = new HashMap<>();
            final GPSFixImporter importer = RouteConverterGPSFixImporterFactory.INSTANCE.createRouteConverterGPSFixImporter();
            final GPSFixImporter.Callback callback = new GPSFixImporter.Callback() {
                @Override
                public void addFix(GPSFix fix, TrackFileImportDeviceIdentifier device) {
                    if (fix instanceof GPSFixMoving) { // only with COG/SOG can we turn this into a Wind fix
                        final Wind wind = new WindImpl(fix.getPosition(), fix.getTimePoint(), ((GPSFixMoving) fix).getSpeed());
                        final WindSource windSource = WindSource.fromTypeAndId(device.getTrackName());
                        if (windSource.getType().canBeStored()) {
                            result.computeIfAbsent(windSource, ws->new ArrayList<>()).add(wind);
                        }
                    }
                }
            };
            if (filename.toLowerCase().endsWith("zip")) {
                logger.info("Routeconverter file "+filename+" is a ZIP file");
                final ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                ZipEntry entry;
                while ((entry=zipInputStream.getNextEntry()) != null) {
                    final String entryName = entry.getName();
                    final int extensionStart = entryName.lastIndexOf('.');
                    if (extensionStart == -1 || Util.contains(importer.getSupportedFileExtensions(), entryName.substring(extensionStart+1))) {
                        logger.info("Reading Routeconverter wind data from "+filename+"'s ZIP entry "+entry.getName());
                        importer.importFixes(new NotClosingUnderlyingInputStream(zipInputStream), charset, callback, /* inferSpeedAndBearing */ false, entry.getName());
                    }
                }
            } else {
                importer.importFixes(inputStream, charset, callback, /* inferSpeedAndBearing */ false, filename);
            }
            return result;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "An exception occurred while trying to close the input stream from which we're reading wind", e);
            }
        }
    }
}
