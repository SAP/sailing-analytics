package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.sensordata.ExpeditionExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.trackfiles.impl.CompressedStreamsUtil;
import com.sap.sailing.server.trackfiles.impl.ExpeditionExtendedDataImporterImpl;
import com.sap.sailing.server.trackfiles.impl.ExpeditionImportFileHandler;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * From an Expedition log file extracts a {@link ExpeditionStartData} object that has all mark "ping" positions for the
 * two ends of the start line ("committee boat" and "pin end"), furthermore all time points identified as possible start
 * time points because the "time to gun" value went to zero.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ExpeditionCourseInferrer {
    private static final Logger logger = Logger.getLogger(ExpeditionCourseInferrer.class.getName());
    private static final String START_LINE_PORT_END_LAT = "port lat";
    private static final String START_LINE_PORT_END_LON = "port lon";
    private static final String START_LINE_STARBOARD_END_LAT = "stbd lat";
    private static final String START_LINE_STARBOARD_END_LON = "stbd lon";
    private static final String START_LINE_PORT_END_MARK_NAME = "start p";
    private static final String START_LINE_STARBOARD_END_MARK_NAME = "start s";
    private static final String START_LINE_CONTROL_POINT_NAME = "start";
    
    private final RaceLogTrackingAdapter raceLogTrackingAdapter;
    
    public ExpeditionCourseInferrer(RaceLogTrackingAdapter raceLogTrackingAdapter) {
        super();
        this.raceLogTrackingAdapter = raceLogTrackingAdapter;
    }

    public ExpeditionStartData getStartData(InputStream inputStream, String filenameWithSuffix, Charset charset)
            throws IOException, FormatNotSupportedException {
        final List<TimePoint> startTimeCandidates = new ArrayList<>();
        final List<GPSFix> startLinePortEndFixes = new ArrayList<>();
        final List<GPSFix> startLineStarboardEndFixes = new ArrayList<>();
        CompressedStreamsUtil.handlePotentiallyCompressedFiles(filenameWithSuffix, inputStream, charset, new ExpeditionImportFileHandler() {
            @Override
            protected void handleExpeditionFile(String fileName, InputStream inputStream, Charset charset) throws IOException, FormatNotSupportedException {
                logger.fine("Start parsing Expedition file");
                final AtomicLong lineNr = new AtomicLong();
                try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                    String headerLine = buffer.readLine();
                    lineNr.incrementAndGet();
                    logger.fine("Validate and parse header columns");
                    final ExpeditionExtendedDataImporterImpl importer = new ExpeditionExtendedDataImporterImpl();
                    final Map<String, Integer> colIndices = importer.parseHeader(headerLine);
                    importer.validateHeader(colIndices);
                    final Double lastTimeToGunValue[] = new Double[1];
                    buffer.lines().forEach(line -> {
                        lineNr.incrementAndGet();
                        if (!line.trim().isEmpty()) {
                            importer.parseLine(lineNr.get(), filenameWithSuffix, line, colIndices,
                                    (timePoint, lineContentTokens, columnsInFileFromHeader) -> {
                                        // look for a start time based on the time to gun turning negative
                                        final Double timeToGunValue = getColumnValue(lineContentTokens, columnsInFileFromHeader,
                                                ExpeditionExtendedSensorDataMetadata.EXPEDITION_TMTOGUN.getColumnName());
                                        if (lastTimeToGunValue[0] != null && lastTimeToGunValue[0] > 0 && timeToGunValue != null &&
                                                timeToGunValue <= 0) {
                                            // found a start candidate:
                                            startTimeCandidates.add(timePoint);
                                        }
                                        lastTimeToGunValue[0] = timeToGunValue;
                                        // look for start line position pings:
                                        final Double startLinePortEndLat = getColumnValue(lineContentTokens, columnsInFileFromHeader, START_LINE_PORT_END_LAT);
                                        final Double startLinePortEndLon = getColumnValue(lineContentTokens, columnsInFileFromHeader, START_LINE_PORT_END_LON);
                                        final Double startLineStarboardEndLat = getColumnValue(lineContentTokens, columnsInFileFromHeader, START_LINE_STARBOARD_END_LAT);
                                        final Double startLineStarboardEndLon = getColumnValue(lineContentTokens, columnsInFileFromHeader, START_LINE_STARBOARD_END_LON);
                                        addFixIfCoordinatesValid(startLinePortEndLat, startLinePortEndLon, timePoint, startLinePortEndFixes);
                                        addFixIfCoordinatesValid(startLineStarboardEndLat, startLineStarboardEndLon, timePoint, startLineStarboardEndFixes);
                                    });
                        }
                    });
                    buffer.close();
                }
            }
        });
        return new ExpeditionStartDataImpl(startLinePortEndFixes, startLineStarboardEndFixes, startTimeCandidates);
    }

    private void addFixIfCoordinatesValid(Double lat, Double lon, TimePoint timePoint, List<GPSFix> listToAddTo) {
        if (lat != null && lon != null) {
            listToAddTo.add(new GPSFixImpl(new DegreePosition(lat, lon), timePoint));
        }
    }
    
    private Double getColumnValue(String[] lineContentTokens, Map<String, Integer> columnsInFileFromHeader, String columnName) {
        return getColumnValue(lineContentTokens, columnsInFileFromHeader.get(columnName.toLowerCase()));
    }

    private Double getColumnValue(String[] lineContentTokens, final Integer columnIndex) {
        return columnIndex == null ? null :
            columnIndex >= lineContentTokens.length ? null
                : lineContentTokens[columnIndex].trim().isEmpty() ? null
                        : Double.parseDouble(lineContentTokens[columnIndex]);
    }

    /**
     * For the tracked races provided constructs a start line based on the {@code startData} and its
     * {@link ExpeditionStartData#getStartLinePortFixes() port} and
     * {@link ExpeditionStartData#getStartLineStarboardFixes() starboard} fixes. Course marks named
     * {@link #START_LINE_PORT_END_MARK_NAME} and {@link #START_LINE_STARBOARD_END_MARK_NAME} are looked
     * up in the tracked races. If not found, such marks are defined through the regatta log.
     */
    public void setStartLine(ExpeditionStartData startData, Iterable<DynamicTrackedRace> trackedRaces, RacingEventService racingEventService) {
        if (!Util.isEmpty(trackedRaces)) {
            for (final DynamicTrackedRace trackedRace : trackedRaces) {
                trackedRace.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 0.5));
            }
            final DynamicTrackedRace anyTrackedRace = trackedRaces.iterator().next();
            logger.info("Adding mark pings for "+START_LINE_PORT_END_MARK_NAME+" using tracked race "+anyTrackedRace.getRace().getName());
            final Mark portMark = getOrCreateMarkAndAddPings(anyTrackedRace, START_LINE_PORT_END_MARK_NAME, startData.getStartLinePortFixes(), racingEventService);
            logger.info("Adding mark pings for "+START_LINE_STARBOARD_END_MARK_NAME+" using tracked race "+anyTrackedRace.getRace().getName());
            final Mark starboardMark = getOrCreateMarkAndAddPings(anyTrackedRace, START_LINE_STARBOARD_END_MARK_NAME, startData.getStartLineStarboardFixes(), racingEventService);
            for (final DynamicTrackedRace trackedRace : trackedRaces) {
                setCourseWithStartLine(trackedRace, racingEventService, portMark, starboardMark);
            }
            for (final DynamicTrackedRace trackedRace : trackedRaces) {
                trackedRace.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
            }
        }
    }

    private void setCourseWithStartLine(DynamicTrackedRace trackedRace, RacingEventService racingEventService,
            final Mark portMark, final Mark starboardMark) {
        logger.info("Creating start line in tracked race "+trackedRace.getRace().getName());
        final ControlPoint startLine = racingEventService.getBaseDomainFactory().getOrCreateControlPointWithTwoMarks(
                UUID.randomUUID(), START_LINE_CONTROL_POINT_NAME, portMark, starboardMark, START_LINE_CONTROL_POINT_NAME);
        final CourseBase course = new CourseDataImpl("Auto-Course "+trackedRace.getRace().getName());
        course.addWaypoint(0, new WaypointImpl(startLine, PassingInstruction.Line));
        final RaceLog raceLog = trackedRace.getAttachedRaceLogs().iterator().next();
        RaceLogEvent event = new RaceLogCourseDesignChangedEventImpl(MillisecondsTimePoint.now(),
                racingEventService.getServerAuthor(), raceLog.getCurrentPassId(), course, CourseDesignerMode.ADMIN_CONSOLE);
        raceLog.add(event);
    }

    private Mark getOrCreateMarkAndAddPings(DynamicTrackedRace trackedRace, String markName, Iterable<GPSFix> markFixes, RacingEventService racingEventService) {
        final Mark mark = getOrCreateMark(trackedRace, markName, racingEventService);
        for (final GPSFix fix : markFixes) {
            recordFix(mark, fix, trackedRace, racingEventService);
        }
        return mark;
    }

    /**
     * Creates a "ping" device mapping for the {@code mark} in the regatta log that is retrieved from the tracked race's
     * regatta.
     */
    private void recordFix(Mark mark, GPSFix fix, DynamicTrackedRace trackedRace, RacingEventService racingEventService) {
        raceLogTrackingAdapter.pingMark(trackedRace.getAttachedRegattaLogs().iterator().next(), mark, fix, racingEventService);
    }

    /**
     * @param racingEventService
     *            used to determine the server author based on the user currently logged on, furthermore to get access
     *            to the {@link SharedDomainFactory} used to create a new mark in case a mark by the desired name is not
     *            found in the race or regatta.
     * @return the mark found or created
     */
    private Mark getOrCreateMark(DynamicTrackedRace trackedRace, String markName, RacingEventService racingEventService) {
        Mark mark = null;
        final Regatta regatta = trackedRace.getTrackedRegatta().getRegatta();
        outer: for (final RaceColumn raceColumn : regatta.getRaceColumns()) {
            for (final Fleet fleet : raceColumn.getFleets()) {
                final TrackedRace trackedRaceForFleet = raceColumn.getTrackedRace(fleet);
                if (trackedRaceForFleet == trackedRace) {
                    // found it; grab marks:
                    for (final Mark availableMark : raceColumn.getAvailableMarks(fleet)) {
                        if (availableMark.getName().equals(markName)) {
                            mark = availableMark;
                            break outer; // found the mark with the correct name; done
                        }
                    }
                    if (mark == null) {
                        mark = racingEventService.getBaseDomainFactory().getOrCreateMark(UUID.randomUUID(), markName, markName);
                        final TimePoint now = MillisecondsTimePoint.now();
                        RegattaLogDefineMarkEventImpl defineMarkEvent = new RegattaLogDefineMarkEventImpl(now,
                                racingEventService.getServerAuthor(), now, UUID.randomUUID(), mark);
                        regatta.getRegattaLog().add(defineMarkEvent);
                    }
                    break outer; // tracked race found, mark found or created at this point; no need to scan more columns
                }
            }
        }
        return mark;
    }
}
