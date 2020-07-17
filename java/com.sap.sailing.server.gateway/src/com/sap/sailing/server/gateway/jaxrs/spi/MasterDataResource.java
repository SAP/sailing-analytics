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

import javax.ws.rs.GET;
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
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.masterdataimport.TopLevelMasterData;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.PublicReadableActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;

@Path("/v1/masterdata/leaderboardgroups")
public class MasterDataResource extends AbstractSailingServerResource {
    
    private static final Logger logger = Logger.getLogger(MasterDataResource.class.getName());
    
    @GET
    @Produces("application/x-java-serialized-object")
    public Response getMasterDataByLeaderboardGroups(@QueryParam("uuids[]") List<UUID> requestedLeaderboardGroupsUuids,
            @QueryParam("compress") Boolean compress, @QueryParam("exportWind") Boolean exportWind,
            @QueryParam("exportDeviceConfigs") Boolean exportDeviceConfigs,
            @QueryParam("exportTrackedRacesAndStartTracking") Boolean exportTrackedRacesAndStartTracking)
            throws UnsupportedEncodingException {
        final SecurityService securityService = getSecurityService();
        User user = securityService.getCurrentUser();
        securityService.checkCurrentUserServerPermission(ServerActions.CAN_EXPORT_MASTERDATA);
        final long startTime = System.currentTimeMillis();
        logger.info("Masterdataexport has started; requesting user: "+user);
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
        Map<UUID, LeaderboardGroup> allLeaderboardGroups = getService().getLeaderboardGroups();
        Set<LeaderboardGroup> groupsToExport = new HashSet<LeaderboardGroup>();
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
        Set<RaceTrackingConnectivityParameters> connectivityParametersToRestore = new HashSet<>();
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
        Set<DeviceConfiguration> raceManagerDeviceConfigurations = new HashSet<>();
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
        ArrayList<Event> events = new ArrayList<>();
        for (Event event : getService().getAllEvents()) {
            events.add(event);
        }
        ArrayList<MediaTrack> mediaTracks = new ArrayList<>();
        for (MediaTrack mediaTrack : getService().getAllMediaTracks()) {
            mediaTracks.add(mediaTrack);
        }
        Map<String, Regatta> regattaRaceIds = new HashMap<>();
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
        ResponseBuilder resp = Response.ok(streamingOutput);
        if (compress) {
            resp.header("Content-Encoding", "gzip");
        }
        final Response builtResponse = resp.build();
        long timeToExport = System.currentTimeMillis() - startTime;
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

    private void writeObjects(final List<Serializable> competitorIds, final TopLevelMasterData masterData,
            ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(competitorIds);
        objectOutputStream.writeObject(masterData.getAllRegattas());
        objectOutputStream.writeObject(masterData);
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
