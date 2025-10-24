package com.sap.sailing.server.trackfiles.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.sensordata.ExpeditionExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Importer for CSV data files from Expedition log files, as used, e.g., by Team Phoenix. Note that this importer so far
 * ignores the {@code downsample} parameter of the
 * {@link #importFixes(InputStream, Charset, com.sap.sailing.domain.trackimport.BaseDoubleVectorFixImporter.Callback, String, String, boolean)}
 * method.
 */
public class ExpeditionExtendedDataImporterImpl extends AbstractDoubleVectorFixImporter
        implements DoubleVectorFixImporter {
    private static final Logger logger = Logger.getLogger(ExpeditionExtendedDataImporterImpl.class.getName());
    private static final String ORIGINAL_POSITION_HEADER = "pos[ddd.dd]";
    public static final String BOAT_COL = "boat";
    public static final String COL_NAME_LAT = "lat";
    public static final String COL_NAME_LON = "lon";
    private static final String UTC_COLUMN = "utc";
    private static final String DATE_COLUMN_1 = "dd/mm/yy";
    private static final String DATE_COLUMN_1_PATTERN = "dd/MM/yy";
    private static final String DATE_COLUMN_2 = "mm/dd/yy";
    private static final String DATE_COLUMN_2_PATTERN = "MM/dd/yy";
    private static final String TIME_COLUMN = "hhmmss";
    private static final String GPS_TIME_COLUMN = "gps time"; // FIXME this has to be "GPS time" with a lowercase t but then we need to handle all these odd values...
    private static final Pattern BOAT_CHECK_PATTERN = Pattern.compile("[1-9]?[0-9]");
    private final Map<String, Integer> columnNamesInFileAndTheirValueIndexInResultingDoubleVectorFix;
    /**
     * The maximum index + 1 into the double vector fix from the
     * {@link #columnNamesInFileAndTheirValueIndexInResultingDoubleVectorFix}
     * values
     */
    private final int trackColumnCount;

    public ExpeditionExtendedDataImporterImpl() {
        this(DoubleVectorFixImporter.EXPEDITION_EXTENDED_TYPE);
    }
    
    protected ExpeditionExtendedDataImporterImpl(String fixType) {
        super(fixType);
        columnNamesInFileAndTheirValueIndexInResultingDoubleVectorFix = getColumnNamesToIndexInDoubleFix();
        trackColumnCount = columnNamesInFileAndTheirValueIndexInResultingDoubleVectorFix.values().stream()
                .max((x, y) -> Integer.compare(x, y)).get() + 1;
    }

    protected Map<String, Integer> getColumnNamesToIndexInDoubleFix() {
        return ExpeditionExtendedSensorDataMetadata.getColumnNamesToIndexInDoubleFix();
    }

    @Override
    public RegattaLogDeviceCompetitorSensorDataMappingEvent createEvent(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable id, Competitor mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint to) {
        return new RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl(createdAt, logicalTimePoint, author, id,
                mappedTo, device, from, to);
    }
    
    @Override
    public RegattaLogDeviceBoatSensorDataMappingEvent createEvent(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable id, Boat mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint to) {
        return new RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl(createdAt, logicalTimePoint, author, id,
                mappedTo, device, from, to);
    }

    @Override
    public boolean importFixes(InputStream inputStream, Charset charset, final Callback callback, String filename,
            String sourceName, boolean downsample) throws FormatNotSupportedException, IOException {
        final TrackFileImportDeviceIdentifier trackIdentifier = getTrackIdentifier(filename, sourceName);
        final AtomicBoolean importedFixes = new AtomicBoolean(false);
        CompressedStreamsUtil.handlePotentiallyCompressedFiles(filename, inputStream, charset, getFileHandler(trackIdentifier, importedFixes, callback));
        return importedFixes.get();
    }
    
    protected ExpeditionImportFileHandler getFileHandler(TrackFileImportDeviceIdentifier trackIdentifier, AtomicBoolean importedFixes, Callback callback) {
        return new ExpeditionImportFileHandler() {
            @Override
            protected void handleExpeditionFile(String fileName, InputStream inputStream, Charset charset) throws IOException, FormatNotSupportedException {
                ExpeditionExtendedDataImporterImpl.this.handleExpeditionFile(fileName, inputStream, charset, trackIdentifier, importedFixes, callback);
            }
        };
    }

    protected void handleExpeditionFile(String fileName, InputStream inputStream, Charset charset, TrackFileImportDeviceIdentifier trackIdentifier, AtomicBoolean importedFixes, Callback callback) throws IOException, FormatNotSupportedException {
        logger.fine("Start parsing Expedition file");
        final AtomicLong lineNr = new AtomicLong();
        final BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
        String headerLine = buffer.readLine();
        lineNr.incrementAndGet();
        logger.fine("Validate and parse header columns");
        final Map<String, Integer> colIndices = parseHeader(headerLine);
        validateHeader(colIndices);
        buffer.lines().forEach(line -> {
            lineNr.incrementAndGet();
            if (!line.trim().isEmpty()) {
                parseLine(lineNr.get(), fileName, line, colIndices,
                        (timePoint, lineContentTokens, columnsInFileFromHeader) -> {
                            final Double[] trackFixData = new Double[trackColumnCount];
                            for (final Entry<String, Integer> columnFromFile : columnsInFileFromHeader.entrySet()) {
                                final Integer indexInDoubleVectorFix = columnNamesInFileAndTheirValueIndexInResultingDoubleVectorFix.get(columnFromFile.getKey());
                                if (indexInDoubleVectorFix != null) {
                                    final Double value = columnFromFile.getValue() >= lineContentTokens.length ? null
                                            : lineContentTokens[columnFromFile.getValue()].trim().isEmpty() ? null
                                                    : Double.parseDouble(lineContentTokens[columnFromFile.getValue()]);
                                    trackFixData[indexInDoubleVectorFix] = value;
                                }
                            }
                            importedFixes.set(true);
                            callback.addFixes(
                                    Collections.singleton(new DoubleVectorFixImpl(timePoint, trackFixData)),
                                    trackIdentifier);
                        });
            }
        });
    }

    protected TrackFileImportDeviceIdentifierImpl getTrackIdentifier(String filename, String sourceName) {
        return new TrackFileImportDeviceIdentifierImpl(
                UUID.randomUUID(), filename, sourceName, MillisecondsTimePoint.now());
    }

    /**
     * When the header contains one or more occurrences of {@link #ORIGINAL_POSITION_HEADER}, it is substituted by the
     * two header columns {@link #COL_NAME_LAT} and {@link #COL_NAME_LON} because that's how positions are stored in
     * Expedition files: as two comma-separated values, one for latitude, another for longitude, although there is only
     * one header field.
     * <p>
     * 
     * All header column names will be stored in the resulting map's key set as lowercase.
     */
    public Map<String, Integer> parseHeader(String headerLine) {
        final String[] headerTokens = split(headerLine);
        Map<String, Integer> colIndicesInFile = new HashMap<>();
        int columnInResultingHeader = 0;
        for (int columnInHeader = 0; columnInHeader < headerTokens.length; columnInHeader++) {
            String header = headerTokens[columnInHeader];
            if (header.toLowerCase().equals(ORIGINAL_POSITION_HEADER)) {
                colIndicesInFile.put(COL_NAME_LAT.toLowerCase(), columnInResultingHeader++);
                colIndicesInFile.put(COL_NAME_LON.toLowerCase(), columnInResultingHeader++);
            } else {
                colIndicesInFile.put(header.toLowerCase(), columnInResultingHeader++);
            }
        }
        return colIndicesInFile;
    }

    /**
     * Ensures that all columns in
     * {@link #columnNamesInFileAndTheirValueIndexInResultingDoubleVectorFix}'s
     * key set are present in {@code colIndicesInFile}'s key set. If not, an
     * exception is thrown that reports the columns missing.
     */
    public void validateHeader(Map<String, Integer> colIndicesInFile) throws FormatNotSupportedException {
        final boolean dateTimeFormatOk;
        if (colIndicesInFile.containsKey(UTC_COLUMN)) {
            dateTimeFormatOk = true;
        } else if (colIndicesInFile.containsKey(DATE_COLUMN_1)
                || colIndicesInFile.containsKey(DATE_COLUMN_2)) {
            dateTimeFormatOk = colIndicesInFile.containsKey(TIME_COLUMN);
        } else {
            // assume that "GPS Time" is present:
            dateTimeFormatOk = colIndicesInFile.containsKey(GPS_TIME_COLUMN);
        }
        if (!dateTimeFormatOk) {
            final String msg = "Missing date/time headers; expect either "+UTC_COLUMN+" or "+
                    DATE_COLUMN_1+" with "+TIME_COLUMN+" or "+DATE_COLUMN_2+" with "+TIME_COLUMN+
                    " or "+GPS_TIME_COLUMN;
            logger.log(Level.SEVERE, msg);
            throw new FormatNotSupportedException(msg);
        }
    }

    public String[] split(String line) {
        return line.split("\\s*,\\s*");
    }

    /**
     * Consumes a line split by the rules of the CSV format handled here; the
     * first argument to {@link #accept(TimePoint, String[], Map) is the time
     * stamp parsed from the line, the second is the tokenized array, each
     * element representing one column in the current line; the third argument
     * maps column names as found in the header line to their 0-based index in
     * the array.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    @FunctionalInterface
    public static interface LineParserCallback {
        void accept(TimePoint timePoint, String[] columns, Map<String, Integer> header);
    }

    /**
     * Parses the CSV line and reads the double data values in the order defined
     * by the col enums.
     */    
    public void parseLine(long lineNr, String filename, String line,
            Map<String, Integer> columnsInFileFromHeader, LineParserCallback callback) {
        try {
            String[] lineContentTokens = split(line);
            final Integer boatColumnIndex = columnsInFileFromHeader.get(BOAT_COL);
            if (boatColumnIndex != null) {
                // Not all file types contain a boat column.
                // We only saw broken data in files having a boat column.
                final String boatToken = lineContentTokens[boatColumnIndex];
                if (!BOAT_CHECK_PATTERN.matcher(boatToken).matches()) {
                    logger.warning("Error, skipping line #" + lineNr + " in file " + filename + ", not a boat id: " + boatToken);
                    return;
                }
            }
            final TimePoint timePoint = getTimePointFromLine(columnsInFileFromHeader, lineContentTokens);
            if (timePoint != null) {
                callback.accept(timePoint, lineContentTokens, columnsInFileFromHeader);
            }
        } catch (Exception e) {
            logger.warning(
                    "Error parsing line #" + lineNr + " in file " + filename + " with exception: " + e.getMessage());
        }
    }

    /**
     * Obtains the time from a line from the Expedition log file. Three time sources are considered in the following
     * order of decreasing precedence:
     * <ol>
     * <li>{@code GPS Time}: the time as reported by the GPS device, in an Excel-like format as days since the
     * epoch</li>
     * <li>{@code Utc}: the time as reported by the computer Expedition was running on when recording the log, in an
     * Excel-like format as days since the epoch</li>
     * <li>{@code dd/mm/yy + hhmmss} or {@code mm/dd/yy + hhmmss}: an unknown time source, probably from the computer
     * Expedition was running on when the log was recorded; assumed to be reported in UTC</li>
     * </ol>
     * If none of the above is found, {@link null} is returned.
     */
    public TimePoint getTimePointFromLine(Map<String, Integer> columnsInFileFromHeader,
            String[] lineContentTokens) throws ParseException {
        final TimePoint timePoint;
        final String date;
        final String dateFormatPattern;
        final Integer gpsTimeColumnIndex = columnsInFileFromHeader.get(GPS_TIME_COLUMN);
        final String time_ExcelEpoch = gpsTimeColumnIndex == null ? null : lineContentTokens[gpsTimeColumnIndex];
        final Integer utcColumnIndex = columnsInFileFromHeader.get(UTC_COLUMN);
        final String utc = utcColumnIndex == null ? null : lineContentTokens[utcColumnIndex];
        if (time_ExcelEpoch != null && !time_ExcelEpoch.trim().isEmpty()) {
            if (Double.valueOf(time_ExcelEpoch) < 1 && utc != null &&
                    !utc.trim().isEmpty()) { // then it seems to be only the fractional time of the day; try to add UTC day:
                timePoint = getTimePoint(""+(Double.valueOf(utc.trim()).intValue()+Double.valueOf(time_ExcelEpoch)));
            } else {
                timePoint = getTimePoint(time_ExcelEpoch);
            }
        } else if (utc != null && !utc.trim().isEmpty()) {
            timePoint = getTimePoint(lineContentTokens[utcColumnIndex]);
        } else if (columnsInFileFromHeader.containsKey(DATE_COLUMN_1) && !lineContentTokens[columnsInFileFromHeader.get(DATE_COLUMN_1)].trim().isEmpty()
                || columnsInFileFromHeader.containsKey(DATE_COLUMN_2) && !lineContentTokens[columnsInFileFromHeader.get(DATE_COLUMN_2)].trim().isEmpty()) {
            if (columnsInFileFromHeader.containsKey(DATE_COLUMN_1)) {
                date = lineContentTokens[columnsInFileFromHeader.get(DATE_COLUMN_1)];
                dateFormatPattern = DATE_COLUMN_1_PATTERN;
            } else {
                date = lineContentTokens[columnsInFileFromHeader.get(DATE_COLUMN_2)];
                dateFormatPattern = DATE_COLUMN_2_PATTERN;
            }
            final String time = lineContentTokens[columnsInFileFromHeader.get(TIME_COLUMN)];
            final DateFormat df = new SimpleDateFormat(dateFormatPattern + "'T'HH:mm:ssX");
            final Date timestamp = df.parse(date + "T" + time + "+00:00"); // assuming UTC
            timePoint = new MillisecondsTimePoint(timestamp);
        } else {
            timePoint = null;
        }
        return timePoint;
    }

    /**
     * The 1900 Date System http://support.microsoft.com/kb/180162/en-us
     */
    private static final Calendar EXCEL_EPOCH_START = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    static {
        EXCEL_EPOCH_START.set(1899, Calendar.DECEMBER, 30, 0, 0, 0);
        EXCEL_EPOCH_START.set(Calendar.MILLISECOND, 0);
    }
    private static final BigDecimal MILLISECONDS_PER_DAY = BigDecimal.valueOf(24 * 60 * 60 * 1000);

    /**
     * Converts a value from a column such as "GPS Time" to a {@link TimePoint}.
     * The value is expected to be provided in decimal format, representing the
     * days since the {@link #EXCEL_EPOCH_START "Excel Epoch Start"}.
     */
    public static TimePoint getTimePoint(String time_ExcelEpoch) {
        final TimePoint timePoint;
        if (!time_ExcelEpoch.trim().isEmpty()) {
            BigDecimal timeStamp = new BigDecimal(time_ExcelEpoch);
            long millisecondsSinceExcelEpochStart = timeStamp.multiply(MILLISECONDS_PER_DAY).longValue();
            timePoint = new MillisecondsTimePoint(
                    EXCEL_EPOCH_START.getTimeInMillis() + millisecondsSinceExcelEpochStart);

        } else {
            timePoint = null;
        }
        return timePoint;
    }
}
