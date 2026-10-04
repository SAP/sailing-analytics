package com.sap.sailing.server.gateway.jaxrs.spi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.shiro.authz.AuthorizationException;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.impl.CompetitorSerializationCustomizer;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.masterdataimport.TopLevelMasterData;
import com.sap.sailing.domain.masterdataimport.WindTrackMasterData;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.interfaces.MasterDataImportConstants;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.TransformationException;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.PublicReadableActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;

@Path(MasterDataImportConstants.MASTER_DATA_RESOURCE_BASE_URL)
public class MasterDataResource extends AbstractSailingServerResource {
    private static final Logger logger = Logger.getLogger(MasterDataResource.class.getName());
    /**
     * Number of fixes written between {@link ObjectOutputStream#reset()} calls while streaming a single device's
     * race-log tracking fixes; see {@link #writeRaceLogTrackingFixes(TopLevelMasterData, ObjectOutputStream)} for why
     * the handle table must be cleared within a device section and not only between devices (bug6227).
     */
    private static final int FIXES_PER_HANDLE_TABLE_RESET = 1000;

    @POST
    @Produces("application/x-java-serialized-object")
    public Response getMasterDataByLeaderboardGroups(@QueryParam(MasterDataImportConstants.QUERY_PARAM_UUIDS) List<UUID> requestedLeaderboardGroupsUuids,
            @QueryParam(MasterDataImportConstants.QUERY_PARAM_COMPRESS) Boolean compress, @QueryParam(MasterDataImportConstants.QUERY_PARAM_EXPORT_WIND) Boolean exportWind,
            @QueryParam(MasterDataImportConstants.QUERY_PARAM_EXPORT_DEVICE_CONFIGS) Boolean exportDeviceConfigs,
            @QueryParam(MasterDataImportConstants.QUERY_PARAM_EXPORT_TRACKED_RACES_AND_START_TRACKING) Boolean exportTrackedRacesAndStartTracking)
            throws UnsupportedEncodingException {
        final SecurityService securityService = getSecurityService();
        final User user = securityService.getCurrentUser();
        securityService.checkCurrentUserServerPermission(ServerActions.CAN_EXPORT_MASTERDATA);
        final long startTime = System.currentTimeMillis();
        logger.info("Masterdataexport has started; requesting user: "+user.getName());
        if (compress == null) {
            compress = false;
        }
        if (exportWind == null) {
            exportWind = true;
        }
        if (exportDeviceConfigs == null) {
            exportDeviceConfigs = false;
        }
        if (exportTrackedRacesAndStartTracking == null) {
            exportTrackedRacesAndStartTracking = false;
        }
        logger.info(String.format("Masterdataexport gzip compression is turned %s", compress ? "on" : "off"));
        final Map<UUID, LeaderboardGroup> allLeaderboardGroups = getService().getLeaderboardGroups();
        final Set<LeaderboardGroup> groupsToExport = new HashSet<LeaderboardGroup>();
        if (requestedLeaderboardGroupsUuids.isEmpty()) {
            // Add all visible LeaderboardGroups.
            // The request will not fail due to missing LeaderboardGroup READ permissions.
            for (LeaderboardGroup group : allLeaderboardGroups.values()) {
                if (securityService.hasCurrentUserReadPermission(group)) {
                    groupsToExport.add(group);
                }
            }
        } else {
            // Add all requested LeaderboardGroups.
            // The request will fail due to missing LeaderboardGroup READ permissions.
            for (UUID uuid : requestedLeaderboardGroupsUuids) {
                LeaderboardGroup group = allLeaderboardGroups.get(uuid);
                if (group != null) {
                    if (!securityService.hasCurrentUserReadPermission(group)) {
                        throw new AuthorizationException(
                                "No permission to read leaderboard group with uuid'" + uuid + "'");
                    }
                    groupsToExport.add(group);
                }
            }
        }
        final List<Serializable> competitorIds = new ArrayList<Serializable>();
        final Set<RaceTrackingConnectivityParameters> connectivityParametersToRestore = new HashSet<>();
        for (LeaderboardGroup lg : groupsToExport) {
            for (Leaderboard leaderboard : lg.getLeaderboards()) {
                // All Leaderboards/Regattas contained in the LeaderboardGroup need to be visible
                // to ensure consistency during import. A partial/pruned import is not intended to take place.
                if (!securityService.hasCurrentUserReadPermission(leaderboard)
                        || ((leaderboard instanceof RegattaLeaderboard) && !securityService
                                .hasCurrentUserReadPermission(((RegattaLeaderboard) leaderboard).getRegatta()))) {
                    throw new AuthorizationException(
                            "No permission to read all leaderboards and regattas of leaderboard group '" + lg.getName()
                                    + "'");
                }
                for (Competitor competitor : leaderboard.getAllCompetitors()) {
                    // All competitors reachable by Leaderboards contained in a LeaderboardGroup
                    // need to be readable (READ or READ_PUBLIC) to allow the import.
                    // Pruning of personal data (email) is done during serialization if the user has READ_PUBLIC but no
                    // READ permission.
                    if (!securityService.hasCurrentUserOneOfExplicitPermissions(competitor,
                            PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                        throw new AuthorizationException("No permission to read competitor " + competitor.getId()
                                + " for leaderboard '" + leaderboard.getName() + "'");
                    }
                    competitorIds.add(competitor.getId());
                }
                for (Boat boat : leaderboard.getAllBoats()) {
                    // All boats reachable by Leaderboards contained in a LeaderboardGroup
                    // need to be readable (READ or READ_PUBLIC) to allow the import.
                    if (!securityService.hasCurrentUserOneOfExplicitPermissions(boat,
                            PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                        throw new AuthorizationException("No permission to read boat " + boat.getId()
                                + " for leaderboard '" + leaderboard.getName() + "'");
                    }
                }
                if (exportTrackedRacesAndStartTracking) {
                    for (TrackedRace trackedRace : leaderboard.getTrackedRaces()) {
                        securityService.checkCurrentUserReadPermission(trackedRace);
                        connectivityParametersToRestore.add(getService().getConnectivityParametersByRace(trackedRace.getRace()));
                    }
                }
            }
        }
        final Set<DeviceConfiguration> raceManagerDeviceConfigurations = new HashSet<>();
        if (exportDeviceConfigs) {
            for (DeviceConfiguration deviceConfig : getAllDeviceConfigs()) {
                // DeviceConfiguration are explicitly filtered by their permissions
                // because no filtering based on the selected LeaderboardGroups is done.
                // This is the only way to allow importing DeviceConfigurations at all.
                if (securityService.hasCurrentUserReadPermission(deviceConfig)) {
                    raceManagerDeviceConfigurations.add(deviceConfig);
                }
            }
        }
        final ArrayList<Event> events = new ArrayList<>();
        for (Event event : getService().getAllEvents()) {
            events.add(event);
        }
        final ArrayList<MediaTrack> mediaTracks = new ArrayList<>();
        for (MediaTrack mediaTrack : getService().getAllMediaTracks()) {
            mediaTracks.add(mediaTrack);
        }
        final Map<String, Regatta> regattaRaceIds = new HashMap<>();
        for (Entry<String, Regatta> regattaRaceMap : getService().getPersistentRegattasForRaceIDs().entrySet()) {
            regattaRaceIds.put(regattaRaceMap.getKey(), regattaRaceMap.getValue());
        }
        final TopLevelMasterData masterData = new TopLevelMasterData(groupsToExport,
                events, regattaRaceIds, mediaTracks,
                getService().getSensorFixStore(), exportWind, raceManagerDeviceConfigurations, connectivityParametersToRestore);
        // Checking permissions after filtering of Events to be transferred.
        for (Event event: masterData.getAllEvents()) {
            if (!securityService.hasCurrentUserReadPermission(event)) {
                throw new AuthorizationException("No permission to read event " + event.getId());
            }
        }
        // Checking permissions after filtering of MediaTracks to be transferred.
        for (MediaTrack mediaTrack : masterData.getFilteredMediaTracks()) {
            if (!securityService.hasCurrentUserReadPermission(mediaTrack)) {
                throw new AuthorizationException("No permission to read media track " + mediaTrack.dbId);
            }
        }
        final StreamingOutput streamingOutput;
        if (compress) {
            streamingOutput = new CompressingStreamingOutput(masterData, competitorIds, startTime, securityService);
        } else {
            streamingOutput = new NonCompressingStreamingOutput(masterData, competitorIds, startTime, securityService);
        }
        final ResponseBuilder resp = Response.ok(streamingOutput);
        if (compress) {
            resp.header("Content-Encoding", "gzip");
        }
        final Response builtResponse = resp.build();
        final long timeToExport = System.currentTimeMillis() - startTime;
        logger.info(String.format("Took %s ms to start masterdataexport-streaming.", timeToExport));
        return builtResponse;
    }
    
    private Iterable<DeviceConfiguration> getAllDeviceConfigs() {
        return getService().getAllDeviceConfigurations();
    }

    private abstract class AbstractStreamingOutput implements StreamingOutput {
        private final TopLevelMasterData masterData;
        private final List<Serializable> competitorIds;
        private final long startTime;
        private final SecurityService securityService;

        protected AbstractStreamingOutput(TopLevelMasterData masterData, List<Serializable> competitorIds,
                long startTime, SecurityService securityService) {
            super();
            this.masterData = masterData;
            this.competitorIds = competitorIds;
            this.startTime = startTime;
            this.securityService = securityService;
        }
        
        protected abstract OutputStream wrapOutputStream(OutputStream outputStream) throws IOException;

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            CompetitorSerializationCustomizer
            .doWithCustomizer(c -> {
                return !securityService.hasCurrentUserReadPermission(c) && c.getEmail() != null;
            }, () -> {
                try {
                    ObjectOutputStream objectOutputStream = null;
                    try {
                        OutputStream gzipOrNot = wrapOutputStream(output);
                        OutputStream outputStreamWithByteCounter = new ByteCountOutputStreamDecorator(gzipOrNot);
                        objectOutputStream = new ObjectOutputStream(outputStreamWithByteCounter);
                        masterData.setMasterDataExportFlagOnRaceColumns(true);
                        // Actual start of streaming
                        writeObjects(competitorIds, masterData, objectOutputStream);
                    } finally {
                        objectOutputStream.close();
                        masterData.setMasterDataExportFlagOnRaceColumns(false);
                    }
                    long timeToExport = System.currentTimeMillis() - startTime;
                    logger.info(String.format("Took %s ms to finish masterdataexport", timeToExport));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private class NonCompressingStreamingOutput extends AbstractStreamingOutput {
        protected NonCompressingStreamingOutput(TopLevelMasterData masterData, List<Serializable> competitorIds,
                long startTime, SecurityService securityService) {
            super(masterData, competitorIds, startTime, securityService);
        }

        @Override
        protected OutputStream wrapOutputStream(OutputStream outputStream) {
            return outputStream;
        }
    }

    private class CompressingStreamingOutput extends AbstractStreamingOutput {
        protected CompressingStreamingOutput(TopLevelMasterData masterData, List<Serializable> competitorIds,
                long startTime, SecurityService securityService) {
            super(masterData, competitorIds, startTime, securityService);
        }

        @Override
        protected OutputStream wrapOutputStream(OutputStream outputStream) throws IOException {
            return new GZIPOutputStream(outputStream);
        }
    }

    /**
     * Writes the master-data export onto a single {@link ObjectOutputStream} (itself wrapped in gzip). This is the
     * <em>writing</em> end of the master-data wire format; the <em>reading</em> end is
     * {@code com.sap.sailing.server.masterdata.MasterDataImporter.importFromStream} in the {@code com.sap.sailing.server}
     * bundle. The two ends are not independently versioned: they must be changed together and stay in lock-step,
     * object-for-object, because the reader consumes the objects positionally in exactly the order written here. The
     * sequence is, and must remain:
     * <ol>
     * <li>the {@code competitorIds} {@link List} (read back by {@code MasterDataImporter} to reset competitor data);</li>
     * <li>{@link TopLevelMasterData#getAllRegattas() all regattas} (written before the graph so that regattas are
     * deserialized before series);</li>
     * <li>the fix-free and wind-free {@link TopLevelMasterData} graph;</li>
     * <li>the wind section written by {@link #writeWindTracks(TopLevelMasterData, ObjectOutputStream)}: an
     * {@code int} count, then the streamed {@code WindTrackMasterData} objects, then a {@code null} sentinel;</li>
     * <li>the sensor-fix section written by {@link #writeRaceLogTrackingFixes(TopLevelMasterData, ObjectOutputStream)}:
     * per device a device header, its fixes, and a {@code null} sentinel, then a {@code null} sentinel for the whole
     * section.</li>
     * </ol>
     * Any change to what, how many, or in which order objects are written here has to be mirrored in
     * {@code MasterDataImporter.importFromStream} (and the corresponding {@code importWindTracks} /
     * {@code importRaceLogTrackingGPSFixes} helpers), and vice versa; there is deliberately no mixed-version
     * compatibility between an old writer and a new reader or the other way round (see bug6227).
     */
    private void writeObjects(final List<Serializable> competitorIds, final TopLevelMasterData masterData,
            ObjectOutputStream objectOutputStream) throws IOException {
        // Reader counterpart: MasterDataImporter.importFromStream reads these three objects back in this exact order.
        objectOutputStream.writeObject(competitorIds);
        objectOutputStream.writeObject(masterData.getAllRegattas());
        objectOutputStream.writeObject(masterData);
        // Reader counterpart: MasterDataImporter.importWindTracks reads the Integer count, the WindTrackMasterData
        // objects, and the terminating null sentinel written by writeWindTracks below.
        writeWindTracks(masterData, objectOutputStream);
        // Reader counterpart: MasterDataImporter.importRaceLogTrackingGPSFixes reads the per-device sections and the
        // terminating null sentinels written by writeRaceLogTrackingFixes below.
        writeRaceLogTrackingFixes(masterData, objectOutputStream);
    }

    /**
     * Streams the {@link WindTrackMasterData} objects as top-level stream objects, one after another, after the
     * (wind-free) {@code masterData} has been written (see bug6227). A season's {@link WindTrackMasterData#getWindTrack()
     * wind tracks} can grow large; keeping them inside the {@link TopLevelMasterData} object graph would retain every
     * one of them in the reading stream's handle table for back-reference resolution. Writing each as a top-level
     * object and clearing the serialization handle table via {@link ObjectOutputStream#reset()} between them means at
     * most one {@link WindTrackMasterData} is retained at a time. The section is still framed by a trailing
     * {@code null} sentinel, but it is preceded by the number of {@link WindTrackMasterData} objects that follow so
     * that the importer can drive a {@link com.sap.sailing.domain.common.tracking.DataImportSubProgress#IMPORT_WIND_TRACKS
     * wind sub-progress} per track (the wind tracks are held only {@code transient}ly and never travel inside the
     * {@link TopLevelMasterData} graph, so unlike the sensor-fix device count this expected count cannot be recovered
     * from the deserialized graph on the reader side and has to be sent explicitly). The count is written as a
     * bare {@code int} via {@link ObjectOutputStream#writeInt(int)} (not as a boxed {@link Integer}), so it carries no
     * class descriptor and occupies no handle-table entry; it is a snapshot of the export-side set that is iterated
     * right after, so it always matches the number of objects actually streamed. The
     * {@link ObjectOutputStream#reset()} is issued after each object; {@code TC_RESET} is an independent stream token
     * the reader consumes transparently.
     */
    private void writeWindTracks(final TopLevelMasterData masterData, final ObjectOutputStream objectOutputStream)
            throws IOException {
        final Set<WindTrackMasterData> windTrackMasterDataForStreaming = masterData.getWindTrackMasterDataForStreaming();
        objectOutputStream.writeInt(windTrackMasterDataForStreaming.size());
        for (final WindTrackMasterData windTrackMasterData : windTrackMasterDataForStreaming) {
            objectOutputStream.writeObject(windTrackMasterData);
            objectOutputStream.reset();
        }
        objectOutputStream.writeObject(null);
    }

    /**
     * Streams the race-log tracking fixes as top-level stream objects, one device section after another, after the
     * (fix-free) {@code masterData} has been written. See bug6227: writing each fix as a top-level object and clearing
     * the serialization handle table via {@link ObjectOutputStream#reset()} means no fix object is retained for
     * back-reference resolution, allowing the fixes to be streamed straight from the {@link SensorFixStore} without
     * ever materializing them all in memory. Each device is streamed exactly once over its merged, non-overlapping
     * {@link MultiTimeRange} sub-ranges (see {@link TopLevelMasterData#getRaceLogTrackingDeviceRanges()}); a device
     * mapped in several regattas therefore no longer has its fixes streamed several times. The mapping ends carried in
     * the ranges are already exclusive (each mapping's inclusive end was converted to an exclusive range end by adding
     * one {@link com.sap.sse.common.TimePoint} resolution unit in
     * {@link TopLevelMasterData#getRaceLogTrackingDeviceRanges()}), so each sub-range is loaded with
     * {@code toIsInclusive == false}; an open sub-range end ({@code null}) streams up to the end of the device's
     * fixes. {@link ObjectOutputStream#reset()} is called both
     * after every {@link #FIXES_PER_HANDLE_TABLE_RESET} fixes within a device section and once more at the end of each
     * device section. A device's merged {@link MultiTimeRange} can span an entire season, so resetting only per device
     * would let the handle table accumulate that device's whole season of fixes (and everything transitively reachable
     * from them) before the first reset, which is what exhausted the heap (bug6227). Resetting every
     * {@link #FIXES_PER_HANDLE_TABLE_RESET} fixes instead bounds the retained handle set to that many fixes regardless
     * of how long a device's ranges are. A reset discards the handle table, so the object written after it must
     * re-emit the full class descriptor hierarchy of the fix type (several hundred bytes) instead of back-referencing
     * it; batching the reset amortizes that descriptor re-emission (and the reader's class resolution) over
     * {@link #FIXES_PER_HANDLE_TABLE_RESET} fixes rather than paying it per fix, and the repeated descriptors compress
     * well in the gzip stream. The framing uses {@code null} sentinels (never fix counts, which a concurrent write
     * could invalidate): each device section is terminated by a {@code null} fix, and the whole section is terminated
     * by a {@code null} device. {@code TC_RESET} is an independent stream token that the reader consumes transparently,
     * so its position relative to the {@code null} sentinels does not affect framing.
     */
    private void writeRaceLogTrackingFixes(final TopLevelMasterData masterData,
            final ObjectOutputStream objectOutputStream) throws IOException {
        final SensorFixStore sensorFixStore = masterData.getSensorFixStore();
        try {
            for (final Entry<DeviceIdentifier, MultiTimeRange> deviceRange : masterData.getRaceLogTrackingDeviceRanges()
                    .entrySet()) {
                final DeviceIdentifier device = deviceRange.getKey();
                objectOutputStream.writeObject(device);
                final int[] fixesSinceLastReset = new int[] { 0 };
                for (final TimeRange range : deviceRange.getValue()) {
                    sensorFixStore.loadFixes(fix -> {
                        try {
                            objectOutputStream.writeObject(fix);
                            fixesSinceLastReset[0]++;
                            if (fixesSinceLastReset[0] >= FIXES_PER_HANDLE_TABLE_RESET) {
                                objectOutputStream.reset();
                                fixesSinceLastReset[0] = 0;
                            }
                        } catch (final IOException e) {
                            throw new WriteFixException(e);
                        }
                    }, device, range.from(), range.to(), false);
                }
                objectOutputStream.writeObject(null);
                objectOutputStream.reset();
            }
            objectOutputStream.writeObject(null);
        } catch (final WriteFixException e) {
            throw e.getCause();
        } catch (final NoCorrespondingServiceRegisteredException | TransformationException e) {
            throw new IOException("Failed to stream race log tracking fixes during master data export", e);
        }
    }

    /**
     * Unchecked carrier for an {@link IOException} thrown from within the {@link java.util.function.Consumer} passed to
     * {@link SensorFixStore#loadFixes}, which cannot itself throw a checked exception.
     */
    private static final class WriteFixException extends RuntimeException {
        private static final long serialVersionUID = -7057078255514958124L;

        private WriteFixException(final IOException cause) {
            super(cause);
        }

        @Override
        public IOException getCause() {
            return (IOException) super.getCause();
        }
    }

    private class ByteCountOutputStreamDecorator extends FilterOutputStream {

        private long byteCount = 0;

        public ByteCountOutputStreamDecorator(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b) throws IOException {
            byteCount++;
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            byteCount = byteCount + len;
            super.write(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            byteCount++;
            super.write(b);
        }

        @Override
        public void close() throws IOException {
            logger.info(String.format("Uncompressed data size of masterdataexport: %s bytes", byteCount));
            super.close();
        }

    }

}
