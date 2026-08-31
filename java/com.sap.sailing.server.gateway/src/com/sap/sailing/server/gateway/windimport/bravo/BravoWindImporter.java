package com.sap.sailing.server.gateway.windimport.bravo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.BaseDoubleVectorFixImporter.Callback;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.gateway.windimport.AbstractWindImporter;
import com.sap.sailing.server.trackfiles.impl.BaseBravoDataImporterImpl;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class BravoWindImporter extends AbstractWindImporter {
    private static final String BRAVO_WIND_IMPORT = "Bravo Wind Import";
    private static final String GZIP_SUFFIX = ".gz";
    private static final Logger logger = Logger.getLogger(BravoWindImporter.class.getName());
    
    @Override
    protected WindSource getDefaultWindSource(UploadRequest uploadRequest) {
        final WindSource windSource;
        final String sourceName;
        logger.info("Importing Bravo wind data from "+uploadRequest.files);
        if (uploadRequest.files != null && !uploadRequest.files.isEmpty()) {
            sourceName = uploadRequest.files.stream().map(f->f.getName()).collect(Collectors.joining(", "));
        } else {
            sourceName = BRAVO_WIND_IMPORT;
        }
        windSource = new WindSourceWithAdditionalID(WindSourceType.EXPEDITION, sourceName + "@" + MillisecondsTimePoint.now());
        return windSource;
    }

    @Override
    protected Map<WindSource, Iterable<Wind>> importWind(WindSource defaultWindSource,
            Map<InputStream, Pair<String, Charset>> inputStreamsAndFilenamesAndCharsets)
            throws IOException, InterruptedException, FormatNotSupportedException {
        final Iterable<Wind> windFixes;
        if (inputStreamsAndFilenamesAndCharsets != null && inputStreamsAndFilenamesAndCharsets.size() == 1) {
            final Pair<String, Charset> filenameAndCharset = inputStreamsAndFilenamesAndCharsets.values().iterator().next();
            logger.info("Reading Bravo wind data from "+filenameAndCharset);
            windFixes = readWind(filenameAndCharset.getA(), inputStreamsAndFilenamesAndCharsets.keySet().iterator().next(), filenameAndCharset.getB());
        } else {
            final List<Wind> windList = new LinkedList<>();
            for (final Entry<InputStream, Pair<String, Charset>> inputStreamAndFileName : inputStreamsAndFilenamesAndCharsets.entrySet()) {
                logger.info("Reading Bravo wind data from "+inputStreamAndFileName.getValue().getA());
                Util.addAll(readWind(inputStreamAndFileName.getValue().getA(), inputStreamAndFileName.getKey(), inputStreamAndFileName.getValue().getB()), windList);
            }
            windFixes = windList;
        }
        final Map<WindSource, Iterable<Wind>> result = new HashMap<>();
        result.put(defaultWindSource, windFixes);
        return result;
    }

    private static enum Fields {
        Lat, Lon, TWS, TWD;
    }
    
    private Iterable<Wind> readWind(String filename, InputStream inputStream, Charset charset)
            throws InterruptedException, IOException, FormatNotSupportedException {
        final List<Wind> result = new LinkedList<>();
        Map<String, Integer> columnsMap = new HashMap<>();
        for (final Fields field : Fields.values()) {
            columnsMap.put(field.name(), field.ordinal());
        }
        final BaseBravoDataImporterImpl importer = new BaseBravoDataImporterImpl(columnsMap, BRAVO_WIND_IMPORT);
        final Callback callback = new Callback() {
            @Override
            public void addFixes(Iterable<DoubleVectorFix> fixes, TrackFileImportDeviceIdentifier device) {
                for (final DoubleVectorFix fix : fixes) {
                    // latitude / longitude are represented in funny NMEA-like way; the value divided by 100 as
                    // a floored integer represents the full degrees; the value modulo 100 represents the decimal
                    // minutes. Example: the pair (4124.645890, 213.738670) stands for N41°24.645890 E002°13.738670
                    final Wind wind = new WindImpl(new DegreePosition(FunnyDegreeConverter.funnyLatLng(fix.get(Fields.Lat.ordinal())),
                            FunnyDegreeConverter.funnyLatLng(fix.get(Fields.Lon.ordinal()))),
                            fix.getTimePoint(), new KnotSpeedWithBearingImpl(fix.get(Fields.TWS.ordinal()),
                                    new DegreeBearingImpl(fix.get(Fields.TWD.ordinal())).reverse()));
                    result.add(wind);
                }
            }
        };
        if (filename.toLowerCase().endsWith(".zip")) {
            logger.info("Bravo file "+filename+" is a ZIP file");
            try (final ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                ZipEntry entry;
                while ((entry=zipInputStream.getNextEntry()) != null) {
                    if (entry.getName().toLowerCase().endsWith(".txt")) {
                        logger.info("Reading Bravo wind data from "+filename+"'s ZIP entry "+entry.getName());
                        importer.importFixes(zipInputStream, Charset.forName("UTF-8"), callback, entry.getName(), BRAVO_WIND_IMPORT, /* downsample */ false);
                    }
                }
            }
        } else {
            final String actualFileName;
            if (filename.toLowerCase().endsWith(GZIP_SUFFIX)) {
                inputStream = new GZIPInputStream(inputStream);
                actualFileName = filename.substring(0, filename.length()-GZIP_SUFFIX.length());
            } else {
                actualFileName = filename;
            }
            importer.importFixes(inputStream, Charset.forName("UTF-8"), callback, actualFileName, BRAVO_WIND_IMPORT, /* downsample */ false);
        }
        return result;
    }
}
