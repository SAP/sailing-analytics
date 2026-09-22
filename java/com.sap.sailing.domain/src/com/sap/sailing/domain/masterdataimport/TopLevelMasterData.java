package com.sap.sailing.domain.masterdataimport;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;

/**
 * Holds all information needed for a master data import.
 * 
 * @author Frederik Petersen (D054528)
 * 
 */
public class TopLevelMasterData implements Serializable {

    private static final long serialVersionUID = -6820731812478920411L;
    private final Map<RegattaIdentifier, Set<String>> raceIdStringsForRegatta;
    private final Set<MediaTrack> filteredMediaTracks;
    private final Set<LeaderboardGroup> leaderboardGroups;
    private final Map<LeaderboardGroup, Set<Event>> eventForLeaderboardGroup;
    /**
     * Merged, per-{@link DeviceIdentifier} time ranges over which the exporter streams sensor fixes as separate
     * top-level stream objects (see bug6227). A device mapped in several regattas (or by several open-ended
     * {@link RegattaLogDeviceMappingEvent}s) would otherwise appear as several independent mappings, making the
     * exporter re-stream that device's fixes once per mapping. Folding all of a device's mapping intervals into a
     * single {@link MultiTimeRange} (via {@link MultiTimeRange#union(TimeRange)}) lets the exporter stream each
     * device exactly once over its coalesced, non-overlapping sub-ranges. The mapping end from
     * {@link RegattaLogDeviceMappingEvent#getToInclusive()} is <em>inclusive</em>, whereas {@link TimeRange#to()} is
     * <em>exclusive</em>; each mapping's inclusive end is therefore converted to an exclusive range end by adding one
     * {@link TimePoint} resolution unit (see {@link #addRangeIfMappingEvent}), so a single-instant mapping such as a
     * pinged mark ({@code from == toInclusive}) becomes a non-empty range instead of an empty one that
     * {@link MultiTimeRange} would silently drop. The exporter loads these ranges with {@code toIsInclusive == false}.
     */
    private final Map<DeviceIdentifier, MultiTimeRange> raceLogTrackingDeviceRanges;
    private transient SensorFixStore sensorFixStore;
    /**
     * The wind tracks to export. These are held only transiently on the export side and streamed as separate
     * top-level stream objects (one {@link WindTrackMasterData} at a time, with the serialization handle table reset
     * between them; see bug6227), so the potentially large {@link WindTrackMasterData#getWindTrack() wind tracks} are
     * never all materialized inside this object graph nor retained in the reading stream's handle table.
     */
    private final transient Set<WindTrackMasterData> windTrackMasterDataForStreaming;
    private final Iterable<DeviceConfiguration> deviceConfigurations;
    private final Set<RaceTrackingConnectivityParameters> connectivityParametersToRestore;

    public TopLevelMasterData(final Set<LeaderboardGroup> groupsToExport, final Iterable<Event> allEvents,
            final Map<String, Regatta> regattaForRaceIdString, final Iterable<MediaTrack> allMediaTracks,
            SensorFixStore sensorFixStore, boolean exportWind,
            Iterable<DeviceConfiguration> raceManagerDeviceConfigurations, final Set<RaceTrackingConnectivityParameters> connectivityParametersToRestore) {
        this(groupsToExport, createEventMap(groupsToExport, allEvents),
                convertToRaceIdStringsForRegattaMap(regattaForRaceIdString),
                filterMediaTracks(allMediaTracks, groupsToExport), collectRaceLogTrackingDeviceRanges(groupsToExport),
                exportWind ? fillWindMap(groupsToExport) : Collections.emptySet(), raceManagerDeviceConfigurations,
                connectivityParametersToRestore);
        this.sensorFixStore = sensorFixStore;
    }

    private TopLevelMasterData(final Set<LeaderboardGroup> leaderboardGroups,
            Map<LeaderboardGroup, Set<Event>> eventForLeaderboardGroup,
            final Map<RegattaIdentifier, Set<String>> raceIdStringsForRegatta,
            final Set<MediaTrack> filteredMediaTracks,
            Map<DeviceIdentifier, MultiTimeRange> raceLogTrackingDeviceRanges,
            Set<WindTrackMasterData> windTrackMasterDataForStreaming,
            Iterable<DeviceConfiguration> deviceConfigurations,
            final Set<RaceTrackingConnectivityParameters> connectivityParametersToRestore) {
        this.raceIdStringsForRegatta = raceIdStringsForRegatta;
        this.leaderboardGroups = leaderboardGroups;
        this.raceLogTrackingDeviceRanges = raceLogTrackingDeviceRanges;
        this.windTrackMasterDataForStreaming = windTrackMasterDataForStreaming;
        this.deviceConfigurations = deviceConfigurations;
        this.eventForLeaderboardGroup = eventForLeaderboardGroup;
        this.filteredMediaTracks = filteredMediaTracks;
        this.connectivityParametersToRestore = connectivityParametersToRestore;
    }

    public TopLevelMasterData copyAndStripOffDataNotNeededOnReplicas() {
        return new TopLevelMasterData(leaderboardGroups, eventForLeaderboardGroup, raceIdStringsForRegatta, filteredMediaTracks,
                /* strip off raceLogTrackingDeviceRanges */ Collections.emptyMap(),
                /* strip off windTrackMasterDataForStreaming */ Collections.emptySet(),
                /* strip off device configurations */ Collections.emptySet(),
                /* strip off connectivity params */ Collections.emptySet());
    }

    /**
     * Collects, per {@link DeviceIdentifier}, the merged {@link MultiTimeRange} over which sensor fixes are to be
     * streamed during master data export (see bug6227). Device mappings live only in the regatta log;
     * {@link RegattaLogDeviceMappingEvent} is a {@link RegattaLogEvent} and can never appear in a
     * {@code RaceLog}, so only the regatta logs need to be scanned. Every mapping event for a given device is folded
     * into that device's accumulated {@link MultiTimeRange} via {@link MultiTimeRange#union(TimeRange)} so the
     * exporter streams each device exactly once over its coalesced, non-overlapping sub-ranges rather than once per
     * mapping event. The <em>inclusive</em> mapping end from {@link RegattaLogDeviceMappingEvent#getToInclusive()} is
     * converted to an <em>exclusive</em> {@link TimeRange} end in {@link #addRangeIfMappingEvent}; the exporter then
     * loads fixes with {@code toIsInclusive == false}.
     */
    private static Map<DeviceIdentifier, MultiTimeRange> collectRaceLogTrackingDeviceRanges(
            Set<LeaderboardGroup> groupsToExport) {
        final Map<DeviceIdentifier, MultiTimeRange> deviceRanges = new HashMap<>();
        for (Regatta regatta : getAllRegattas(groupsToExport)) {
            final RegattaLog regattaLog = regatta.getRegattaLog();
            try {
                regattaLog.lockForRead();
                for (RegattaLogEvent logEvent : regattaLog.getRawFixes()) {
                    addRangeIfMappingEvent(deviceRanges, logEvent);
                }
            } finally {
                regattaLog.unlockAfterRead();
            }
        }
        return deviceRanges;
    }

    private static void addRangeIfMappingEvent(Map<DeviceIdentifier, MultiTimeRange> deviceRanges,
            AbstractLogEvent<?> logEvent) {
        if (logEvent instanceof RegattaLogDeviceMappingEvent<?>) {
            final RegattaLogDeviceMappingEvent<?> mappingEvent = (RegattaLogDeviceMappingEvent<?>) logEvent;
            final DeviceIdentifier device = mappingEvent.getDevice();
            final TimePoint from = mappingEvent.getFrom();
            final TimePoint toInclusive = mappingEvent.getToInclusive();
            // RegattaLogDeviceMappingEvent.getToInclusive() is an INCLUSIVE end, whereas TimeRange.to() is EXCLUSIVE.
            // As mandated by that method's Javadoc, we bridge the gap by adding one TimePoint resolution unit to the
            // inclusive end to obtain a valid exclusive TimeRange end that still includes the mapping's last instant.
            // This is what makes a single-instant PING mapping ([t, t] inclusive) a non-empty range [t, t+resolution)
            // rather than an empty range that MultiTimeRange would silently discard (see bug6227). We ask the TimePoint
            // itself for its resolution (TimePoint.getResolution(), currently one millisecond for all implementations),
            // so this stays correct should a finer-resolution TimePoint ever be introduced; the exporter loads these
            // ranges with toIsInclusive == false to match.
            final TimePoint toExclusive = toInclusive == null ? null : toInclusive.plus(toInclusive.getResolution());
            final TimeRange mappingRange = TimeRange.create(from, toExclusive);
            final MultiTimeRange existing = deviceRanges.get(device);
            final MultiTimeRange merged = existing == null ? MultiTimeRange.of(mappingRange)
                    : existing.union(mappingRange);
            deviceRanges.put(device, merged);
        }
    }

    /**
     * Workaround to look for the events connected to RegattaLeadeboards. There should be a proper connection between
     * regatta and event soon. TODO See bugs 1896 and 1532
     */
    private static Map<LeaderboardGroup, Set<Event>> createEventMap(Set<LeaderboardGroup> groupsToExport,
            Iterable<Event> allEvents) {
        Map<LeaderboardGroup, Set<Event>> eventsForLeaderboardGroup = new HashMap<LeaderboardGroup, Set<Event>>();
        Map<CourseArea, Event> eventForCourseArea = new HashMap<CourseArea, Event>();
        for (Event event : allEvents) {
            for (CourseArea courseArea : event.getVenue().getCourseAreas()) {
                eventForCourseArea.put(courseArea, event);
            }
        }
        for (LeaderboardGroup leaderboardGroup : groupsToExport) {
            HashSet<Event> eventSet = new HashSet<Event>();
            eventsForLeaderboardGroup.put(leaderboardGroup, eventSet);
            for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                for (final CourseArea courseArea : leaderboard.getCourseAreas()) {
                    final Event event = eventForCourseArea.get(courseArea);
                    if (event != null) {
                        eventSet.add(event);
                    }
                }
            }
        }
        return eventsForLeaderboardGroup;
    }

    private static Map<RegattaIdentifier, Set<String>> convertToRaceIdStringsForRegattaMap(
            Map<String, Regatta> regattaForRaceIdString) {
        Map<RegattaIdentifier, Set<String>> raceIdStringsForRegatta = new HashMap<RegattaIdentifier, Set<String>>();
        for (Entry<String, Regatta> entry : regattaForRaceIdString.entrySet()) {
            Regatta regatta = entry.getValue();
            Set<String> raceIds = raceIdStringsForRegatta.get(regatta.getRegattaIdentifier());
            if (raceIds == null) {
                raceIds = new HashSet<String>();
                raceIdStringsForRegatta.put(regatta.getRegattaIdentifier(), raceIds);
            }
            raceIds.add(entry.getKey());
        }
        return raceIdStringsForRegatta;
    }

    private static Set<WindTrackMasterData> fillWindMap(Set<LeaderboardGroup> groupsToExport) {
        Set<WindTrackMasterData> windTrackMasterDataSet = new HashSet<WindTrackMasterData>();
        for (LeaderboardGroup group : groupsToExport) {
            for (Leaderboard leaderboard : group.getLeaderboards()) {
                for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                    for (Fleet fleet : raceColumn.getFleets()) {
                        addWindTracksToSetIfExistantAndSourceCanBeStored(windTrackMasterDataSet, raceColumn, fleet);
                    }
                }
            }
        }
        return windTrackMasterDataSet;
    }

    private static void addWindTracksToSetIfExistantAndSourceCanBeStored(Set<WindTrackMasterData> windTrackMasterDataSet,
            RaceColumn raceColumn, Fleet fleet) {
        TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
        if (trackedRace != null) {
            Iterable<WindSource> windSources = trackedRace.getWindSources();
            String raceName = trackedRace.getRace().getName();
            Serializable raceId = trackedRace.getRace().getId();
            String regattaName = trackedRace.getTrackedRegatta().getRegatta().getName();
            if (windSources != null) {
                for (WindSource source : windSources) {
                    if (source.canBeStored()) {
                        windTrackMasterDataSet.add(new WindTrackMasterData(source.getType(), source.getId(),
                                trackedRace.getOrCreateWindTrack(source), regattaName, raceName, raceId));
                    }
                }
            }
        }
    }

    public Iterable<MediaTrack> getFilteredMediaTracks() {
        return this.filteredMediaTracks;
    }

    public Collection<LeaderboardGroup> getLeaderboardGroups() {
        return leaderboardGroups;
    }

    public Set<WindTrackMasterData> getWindTrackMasterDataForStreaming() {
        return windTrackMasterDataForStreaming;
    }

    public void setMasterDataExportFlagOnRaceColumns(boolean flagValue) {
        // collect all leaderboard groups for all events that will be touched during serialization
        final Set<LeaderboardGroup> allLeaderboardGroups = new HashSet<>();
        allLeaderboardGroups.addAll(leaderboardGroups);
        for (Entry<LeaderboardGroup, Set<Event>> i : eventForLeaderboardGroup.entrySet()) {
            for (Event e : i.getValue()) {
                Util.addAll(e.getLeaderboardGroups(), allLeaderboardGroups);
            }
        }
        for (LeaderboardGroup group : allLeaderboardGroups) {
            for (Leaderboard leaderboard : group.getLeaderboards()) {
                for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                    raceColumn.setMasterDataExportOngoingThreadFlag(true);
                }
            }
        }
    }

    public Map<RegattaIdentifier, Set<String>> getRaceIdStringsForRegatta() {
        return raceIdStringsForRegatta;
    }

    public Map<LeaderboardGroup, Set<Event>> getEventForLeaderboardGroup() {
        return eventForLeaderboardGroup;
    }

    public Iterable<Event> getAllEvents() {
        Map<UUID, Event> allEventsInMasterData = new HashMap<>();
        for (Set<Event> events : eventForLeaderboardGroup.values()) {
            for (Event e : events) {
                allEventsInMasterData.put(e.getId(), e);
            }
        }
        return allEventsInMasterData.values();
    }
    
    public Iterable<Regatta> getAllRegattas() {
        return getAllRegattas(leaderboardGroups);
    }

    private static Iterable<Regatta> getAllRegattas(Iterable<LeaderboardGroup> groupsToExport) {
        Set<Regatta> regattas = new HashSet<>();
        for (LeaderboardGroup leaderboardGroup : groupsToExport) {
            for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                if (leaderboard instanceof RegattaLeaderboard) {
                    RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                    regattas.add(regattaLeaderboard.getRegatta());
                }
            }
        }
        return regattas;
    }

    /**
     * Copies only those media tracks from {@code allMediaTracks} to {@code filteredMediaTracks} which are 
     * assigned to races related to any of  exported {@link #leaderboardGroups}.
     */
    private static Set<MediaTrack> filterMediaTracks(final Iterable<MediaTrack> allMediaTracks, final Set<LeaderboardGroup> leaderboardGroups) {
        final Set<MediaTrack> result = new HashSet<>();
        final Set<RaceIdentifier> raceIdentitifiersForMediaExport = collectRaceIdentifiersForMediaExport(leaderboardGroups);
        for (MediaTrack mediaTrack : allMediaTracks) {
            for (RegattaAndRaceIdentifier raceIdentifier : mediaTrack.assignedRaces) {
                if (raceIdentitifiersForMediaExport.contains(raceIdentifier)) {
                    result.add(mediaTrack);
                }
            }
        }
        return result;
    }

    /**
     * Returns the set of races (in the form of {@link RaceIdentifier}s) related to the exported
     * {@link #leaderboardGroups}.
     */
    private static Set<RaceIdentifier> collectRaceIdentifiersForMediaExport(final Set<LeaderboardGroup> leaderboardGroups) {
        final Set<RaceIdentifier> raceIdentifiers = new HashSet<>();
        for (LeaderboardGroup leaderboardGroup : leaderboardGroups) {
            for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                    for (Fleet fleet : raceColumn.getFleets()) {
                        RaceIdentifier raceIdentifier = raceColumn.getRaceIdentifier(fleet);
                        if (raceIdentifier != null) {
                            raceIdentifiers.add(raceIdentifier);
                        }
                    }
                }
            }
        }
        return raceIdentifiers;
    }

    public Map<DeviceIdentifier, MultiTimeRange> getRaceLogTrackingDeviceRanges() {
        return raceLogTrackingDeviceRanges;
    }

    public SensorFixStore getSensorFixStore() {
        return sensorFixStore;
    }

    public Iterable<DeviceConfiguration> getDeviceConfigurations() {
        return deviceConfigurations;
    }

    public Set<RaceTrackingConnectivityParameters> getConnectivityParametersToRestore() {
        return connectivityParametersToRestore;
    }
}
