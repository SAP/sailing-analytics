package com.sap.sailing.server.masterdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.MasterDataImportInformation;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.DataImportSubProgress;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.MasterDataImportObjectCreationCountImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.masterdataimport.TopLevelMasterData;
import com.sap.sailing.domain.masterdataimport.WindTrackMasterData;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.tracking.DummyTrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.ImportMasterDataOperation;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Timed;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache.ResolveListener;

public class MasterDataImporter {
    private final static Logger logger = Logger.getLogger(MasterDataImporter.class.getName());
    private static final int BATCH_SIZE_FOR_IMPORTING_FIXES = 5000;
    private static final int BATCH_SIZE_FOR_IMPORTING_WIND_FIXES = 5000;
    /**
     * Overall-progress milestones for a master-data import, in the {@code [0, 1]} range reported through
     * {@link RacingEventService#createOrUpdateDataImportProgressWithReplication}. They are laid out as a single
     * monotonically increasing schedule so the progress bar never jumps backwards (see bug6227): transfer of the
     * object graph ({@link #PROGRESS_TRANSFER_STARTED} to {@link #PROGRESS_TRANSFER_COMPLETED}), then the two
     * streamed top-level sections that usually dominate the import time, each given an equal-width band: the wind tracks fill
     * {@link #PROGRESS_TRANSFER_COMPLETED} to {@link #PROGRESS_WIND_TRACKS_DONE} and the sensor fixes fill
     * {@link #PROGRESS_WIND_TRACKS_DONE} to {@link #PROGRESS_SENSOR_FIXES_DONE}. Next, the graph is applied and the
     * races are loaded before publishing {@link #PROGRESS_COMPLETE} (see bug6335). The per-item band positions are
     * computed as {@code start + width * fractionDone}
     * from these milestones (see {@link #importWindTracks} and {@link #importRaceLogTrackingGPSFixes}) rather than
     * from repeated literals, so the two band widths cannot drift apart from the milestones that bound them.
     */
    private static final double PROGRESS_TRANSFER_STARTED = 0.03;
    private static final double PROGRESS_TRANSFER_COMPLETED = 0.3;
    private static final double PROGRESS_WIND_TRACKS_DONE = 0.5;
    private static final double PROGRESS_SENSOR_FIXES_DONE = 0.7;
    private static final double PROGRESS_COMPLETE = 1.0;
    private static final double PROGRESS_WIND_TRACKS_BAND_WIDTH = PROGRESS_WIND_TRACKS_DONE - PROGRESS_TRANSFER_COMPLETED;
    private static final double PROGRESS_SENSOR_FIXES_BAND_WIDTH = PROGRESS_SENSOR_FIXES_DONE - PROGRESS_WIND_TRACKS_DONE;
    private final DomainFactory baseDomainFactory;
    private final RacingEventService racingEventService;
    private final User user;
    private final UserGroup tenant;

    public MasterDataImporter(DomainFactory baseDomainFactory, RacingEventService racingEventService,
            User user, UserGroup tenant) {
        this.baseDomainFactory = baseDomainFactory;
        this.racingEventService = racingEventService;
        this.user = user;
        this.tenant = tenant;
    }

    /**
     * Reads a master-data export back in. This is the <em>reading</em> end of the master-data wire format; the
     * <em>writing</em> end is
     * {@code com.sap.sailing.server.gateway.jaxrs.spi.MasterDataResource.writeObjects} in the
     * {@code com.sap.sailing.server.gateway} bundle. The two ends are not independently versioned: they must be
     * changed together and stay in lock-step, object-for-object, because the objects are consumed here positionally in
     * exactly the order the writer produced them. The sequence read here is, and must remain: the {@code competitorIds}
     * {@link List}; {@link TopLevelMasterData#getAllRegattas() all regattas} (before the graph, so regattas are
     * deserialized before series); the fix-free and wind-free {@link TopLevelMasterData} graph; then the wind section
     * (see {@link #importWindTracks(ObjectInputStream, UUID)}: an {@code int} count, the streamed
     * {@code WindTrackMasterData} objects, a {@code null} sentinel); then the sensor-fix section (see
     * {@link #importRaceLogTrackingGPSFixes(ObjectInputStream, UUID, int)}: per-device sections each terminated by a
     * {@code null} fix, and a {@code null} device to end the section). Any change to what, how many, or in which order
     * objects are read here has to be mirrored in {@code MasterDataResource.writeObjects} (and its
     * {@code writeWindTracks} / {@code writeRaceLogTrackingFixes} helpers), and vice versa; there is deliberately no
     * mixed-version compatibility between an old writer and a new reader or the other way round (see bug6227).
     */
    public Map<LeaderboardGroup, ? extends Iterable<Event>> importFromStream(InputStream inputStream, UUID importOperationId, boolean override)
            throws IOException, ClassNotFoundException {
        logger.info("Preparing master data import stream for import "+importOperationId);
        ObjectInputStreamResolvingAgainstCache<DomainFactory> objectInputStream = racingEventService
                .getBaseDomainFactory()
                .createObjectInputStreamResolvingAgainstThisFactory(inputStream, new ResolveListener() {
                    @Override
                    public void onNewObject(Object result) {
                        if (result instanceof Boat || result instanceof Competitor) {
                            QualifiedObjectIdentifier id = ((WithQualifiedObjectIdentifier) result).getIdentifier();
                            logger.info("Adopting " + id + " from Masterdataimport to " + user.getName() + " and group "
                                    + (tenant==null ? "null" : tenant.getName()));
                            racingEventService.getSecurityService().setOwnershipIfNotSet(id, user, tenant);
                        }
                    }

                    @Override
                    public void onResolvedObject(Object result) {
                    }
                }, /* classLoaderCache */ new HashMap<>());
        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, PROGRESS_TRANSFER_STARTED,
                DataImportSubProgress.TRANSFER_STARTED, 0.5);
        RaceLogStore raceLogStore = MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(
                racingEventService.getMongoObjectFactory(), racingEventService.getDomainObjectFactory());
        RegattaImpl.setOngoingMasterDataImport(new MasterDataImportInformation(raceLogStore));
        final ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        final TopLevelMasterData topLevelMasterData;
        Thread.currentThread().setContextClassLoader(racingEventService.getDeserializationClassLoader());
        try {
            // Writer counterpart: MasterDataResource.writeObjects writes these three objects (competitorIds, all
            // regattas, then the fix-free and wind-free TopLevelMasterData graph) in exactly this order; the wind and
            // sensor-fix sections that follow the graph are read below by importWindTracks / importRaceLogTrackingGPSFixes.
            logger.info("Reading competitor IDs for import "+importOperationId);
            @SuppressWarnings("unchecked")
            final List<Serializable> competitorIds = (List<Serializable>) objectInputStream.readObject();
            if (override) {
                setAllowCompetitorsDataToBeReset(competitorIds);
            }
            logger.info("Reading regattas for import "+importOperationId);
            // Deserialize Regattas to make sure that Regattas are deserialized before Series
            objectInputStream.readObject();
            logger.info("Reading master data for import "+importOperationId);
            topLevelMasterData = (TopLevelMasterData) objectInputStream.readObject();
        } finally {
            RegattaImpl.setOngoingMasterDataImport(null);
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
        // in order to restore all listeners we need to initialize the regatta
        // after the whole object graph has been restored
        for (Regatta regatta : topLevelMasterData.getAllRegattas()) {
            RegattaImpl regattaImpl = (RegattaImpl) regatta;
            regattaImpl.initializeSeriesAfterDeserialize();
            // master data import from older system, generate a uuid for this.
            if (regatta.getRegistrationLinkSecret() == null) {
                logger.info("Generated missing registrationLinkSecret for " + this + " while importing MasterData");
                regatta.setRegistrationLinkSecret(UUID.randomUUID().toString());
            }
        }
        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, PROGRESS_TRANSFER_COMPLETED,
                DataImportSubProgress.TRANSFER_COMPLETED, 0.5);
        // The wind tracks and sensor fixes are streamed as top-level objects after the serialized object graph
        // (see bug6227), but must be persisted before applying ImportMasterDataOperation: that operation starts
        // race loading, which must find the imported wind in the store already (see bug6335). Because the wind and
        // sensor-fix imports usually dominate the overall time, an equal-width band is reserved for each: overall
        // progress rises monotonically from PROGRESS_TRANSFER_COMPLETED through the wind band
        // (PROGRESS_TRANSFER_COMPLETED -> PROGRESS_WIND_TRACKS_DONE, 0.3 -> 0.5) and the sensor-fix band
        // (PROGRESS_WIND_TRACKS_DONE -> PROGRESS_SENSOR_FIXES_DONE, 0.5 -> 0.7). Each band advances by its width
        // multiplied by the fraction of wind tracks or device sections imported. Applying the graph and loading
        // the races then advances progress from 0.7 to 0.95, leaving PROGRESS_COMPLETE for the true end of the import.
        // The single terminal "Done" marker and the operation's result are published only here, with replication,
        // after both fix imports, race loading, and adding the imported event IDs to the result. Thus, a client
        // polling either the primary or a replica sees DataImportProgress.getResult() turn non-null only at genuine
        // completion, rather than prematurely when the object-graph operation returns (see bug6227 and bug6322).
        final ClassLoader oldContextClassLoaderForStreamedData = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(racingEventService.getDeserializationClassLoader());
        try {
            racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, PROGRESS_TRANSFER_COMPLETED,
                    DataImportSubProgress.IMPORT_WIND_TRACKS, /* subProgressPct */ 0);
            logger.info("Reading wind tracks for import "+importOperationId);
            importWindTracks(objectInputStream, importOperationId);
            racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, PROGRESS_WIND_TRACKS_DONE,
                    DataImportSubProgress.IMPORT_SENSOR_FIXES, /* subProgressPct */ 0);
            logger.info("Reading GPS/sensor fixes for import "+importOperationId);
            importRaceLogTrackingGPSFixes(objectInputStream, importOperationId,
                    topLevelMasterData.getRaceLogTrackingDeviceRanges().size());
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoaderForStreamedData);
        }
        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, PROGRESS_SENSOR_FIXES_DONE,
                DataImportSubProgress.IMPORT_WAIT, /* subProgressPct */ 0);
        final MasterDataImportObjectCreationCountImpl creationCount = applyMasterDataImportOperation(topLevelMasterData,
                importOperationId, override);
        final Set<UUID> eventIdsImported = new HashSet<>();
        for (final Iterable<Event> events : topLevelMasterData.getEventForLeaderboardGroup().values()) {
            for (final Event event : events) {
                eventIdsImported.add(event.getId());
            }
        }
        creationCount.setEventIdsImported(eventIdsImported);
        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, PROGRESS_COMPLETE,
                DataImportSubProgress.WAITING_FOR_TRACKED_RACES_TO_FINISH_LOADING, /* subProgressPct */ 1.0, creationCount);
        logger.info("Done importing master data into " + racingEventService);
        return topLevelMasterData.getEventForLeaderboardGroup();
    }

    /**
     * Reads and stores the {@link WindTrackMasterData} objects that the exporter streamed as top-level objects after
     * the {@link TopLevelMasterData} and before the sensor-fix section (see bug6227). Each {@link WindTrackMasterData}
     * is read on its own so that at most one of them (with its potentially large {@link WindTrack}) is held in memory
     * at a time, rather than the whole set being materialized inside the deserialized object graph and retained in the
     * {@link ObjectInputStream} handle table. The wire framing is an {@code int} count of the wind tracks that
     * follow, then the {@link WindTrackMasterData} objects, then a {@code null} sentinel in place of the next one; the
     * count is used only to advance {@link DataImportSubProgress#IMPORT_WIND_TRACKS wind sub-progress} per track,
     * whereas the {@code null} sentinel remains the authority on where the section actually ends (so a count that a
     * concurrent export-side change might have made stale can never cause the reader to over- or under-run the
     * stream). The expected count has to be sent explicitly because, unlike the sensor-fix device count, it cannot be
     * recovered from the deserialized graph: the wind tracks are held only {@code transient}ly on the export side and
     * never travel inside the {@link TopLevelMasterData} graph.
     * <p>
     * Sub-progress for the {@link DataImportSubProgress#IMPORT_WIND_TRACKS} phase is advanced once per wind track,
     * driven by that leading count, mirroring the per-device advancement of
     * {@link #importRaceLogTrackingGPSFixes(ObjectInputStream, UUID, int) the sensor-fix section}: overall progress
     * rises across the {@link #PROGRESS_TRANSFER_COMPLETED}-to-{@link #PROGRESS_WIND_TRACKS_DONE} band as
     * {@code PROGRESS_TRANSFER_COMPLETED + PROGRESS_WIND_TRACKS_BAND_WIDTH * windTracksDone / expectedWindTrackCount}. A
     * finer per-fix granularity is intentionally not attempted because the streaming format carries no per-track fix
     * count.
     * <p>
     * As with {@link #importRaceLogTrackingGPSFixes(ObjectInputStream, UUID, int) the sensor-fix section}, this only
     * ever runs on the primary/master: a replica receives wind data through the tracked-race loading replication, not
     * through this stream, and only ever gets the {@link TopLevelMasterData#copyAndStripOffDataNotNeededOnReplicas()
     * stripped} {@link ImportMasterDataOperation}. A non-{@code null}
     * {@link RacingEventService#getMasterDescriptor() master descriptor} therefore means a replica has erroneously
     * reached this stream-based path, which is a broken routing state; it is reported by throwing an
     * {@link IllegalStateException} rather than silently draining, so the fault surfaces instead of being hidden. This
     * mirrors the logic that previously lived in {@code ImportMasterDataOperation.createWindTracks}, moved here so that
     * the large wind tracks travel through the streaming path instead of the operation's object graph.
     */
    private void importWindTracks(final ObjectInputStream objectInputStream, final UUID importOperationId)
            throws IOException, ClassNotFoundException {
        if (racingEventService.getMasterDescriptor() != null) {
            throw new IllegalStateException(
                    "Master data import from an ObjectInputStream reached a replica RacingEventService; such requests "
                            + "must always be routed to the primary/master. The primary loads the tracked races with "
                            + "their fixes and those transactions are replicated; a replica only ever receives the "
                            + "stripped ImportMasterDataOperation, never the wind-track stream.");
        } else {
            final int expectedWindTrackCount = objectInputStream.readInt();
            int windTracksDone = 0;
            Object nextHeader = objectInputStream.readObject();
            while (nextHeader != null) {
                final WindTrackMasterData windMasterData = (WindTrackMasterData) nextHeader;
                final DummyTrackedRace trackedRaceWithNameAndId = new DummyTrackedRace(windMasterData.getRaceName(),
                        windMasterData.getRaceId());
                final WindTrack windTrackToWriteTo = racingEventService.getWindStore().getWindTrack(
                        windMasterData.getRegattaName(), trackedRaceWithNameAndId, windMasterData.getWindSource(), 0,
                        -1);
                final List<Wind> fixesToAdd = new ArrayList<>(BATCH_SIZE_FOR_IMPORTING_WIND_FIXES);
                Object nextFix = objectInputStream.readObject();
                while (nextFix != null) {
                    final Wind fix = (Wind) nextFix;
                    final Wind existingFix = windTrackToWriteTo.getFirstRawFixAtOrAfter(fix.getTimePoint());
                    if (existingFix == null || !existingFix.equals(fix)) {
                        fixesToAdd.add(fix);
                        if (fixesToAdd.size() == BATCH_SIZE_FOR_IMPORTING_WIND_FIXES) {
                            windTrackToWriteTo.add(fixesToAdd);
                            fixesToAdd.clear();
                        }
                    } else {
                        logger.fine("Didn't add wind fix in import, because equal fix was already there.");
                    }
                    nextFix = objectInputStream.readObject();
                }
                if (!fixesToAdd.isEmpty()) {
                    windTrackToWriteTo.add(fixesToAdd);
                }
                windTracksDone++;
                logger.info("Done with wind track #"+windTracksDone+" of "+expectedWindTrackCount);
                if (expectedWindTrackCount > 0) {
                    final double fractionDone = (double) windTracksDone / expectedWindTrackCount;
                    racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId,
                            PROGRESS_TRANSFER_COMPLETED + PROGRESS_WIND_TRACKS_BAND_WIDTH * fractionDone,
                            DataImportSubProgress.IMPORT_WIND_TRACKS, fractionDone);
                }
                nextHeader = objectInputStream.readObject();
            }
        }
    }

    /**
     * Reads and stores the race-log tracking fixes that the exporter streamed as top-level objects after the
     * {@link TopLevelMasterData} (see bug6227). The fixes are read incrementally and stored in batches of
     * {@link #BATCH_SIZE_FOR_IMPORTING_FIXES} directly into the master's {@link SensorFixStore}, so that at most one
     * batch is held in memory at a time. The wire framing uses {@code null} sentinels: each device section is
     * terminated by a {@code null} fix, and the whole section is terminated by a {@code null} device.
     * <p>
     * This method only ever runs on the primary/master: master data import requests carrying a serialized stream must
     * always be routed to the primary, never to a replica. A replica never receives the fix stream at all; it receives
     * only the {@link TopLevelMasterData#copyAndStripOffDataNotNeededOnReplicas() stripped}
     * {@link ImportMasterDataOperation} (which contains no fixes, mappings, or connectivity parameters), and it obtains
     * the tracking data by having the primary load the tracked races with their fixes, whose transactions are then
     * replicated. A non-{@code null} {@link RacingEventService#getMasterDescriptor() master descriptor} therefore means
     * a replica has erroneously reached this stream-based path, which is a broken routing state; it is reported by
     * throwing an {@link IllegalStateException} rather than silently draining, so the fault surfaces instead of being
     * hidden.
     * <p>
     * Sub-progress for the {@link DataImportSubProgress#IMPORT_SENSOR_FIXES} phase is advanced once per device section,
     * driven by {@code numberOfDeviceSections} (the number of entries in
     * {@link TopLevelMasterData#getRaceLogTrackingDeviceRanges()}, which equals the number of device sections the
     * exporter writes: the exporter streams each device exactly once over its merged, non-overlapping ranges). A finer
     * per-fix granularity is intentionally not attempted because the streaming format carries no fix count.
     */
    private void importRaceLogTrackingGPSFixes(final ObjectInputStream objectInputStream, final UUID importOperationId,
            final int numberOfDeviceSections) throws IOException, ClassNotFoundException {
        if (racingEventService.getMasterDescriptor() != null) {
            throw new IllegalStateException(
                    "Master data import from an ObjectInputStream reached a replica RacingEventService; such requests "
                            + "must always be routed to the primary/master. The primary loads the tracked races with "
                            + "their fixes and those transactions are replicated; a replica only ever receives the "
                            + "stripped ImportMasterDataOperation, never the fix stream.");
        } else {
            final SensorFixStore store = racingEventService.getSensorFixStore();
            int devicesDone = 0;
            Object device = objectInputStream.readObject();
            while (device != null) {
                final DeviceIdentifier deviceIdentifier = (DeviceIdentifier) device;
                logger.info("Importing fixes for device "+deviceIdentifier+" for import "+importOperationId+" ("+devicesDone+"/"+numberOfDeviceSections+")");
                final Collection<Timed> fixesToAddAsBatch = new ArrayList<>(BATCH_SIZE_FOR_IMPORTING_FIXES);
                Object fix = objectInputStream.readObject();
                while (fix != null) {
                    Timed fixToAdd = (Timed) fix;
                    if (fixToAdd instanceof VeryCompactGPSFixMovingImpl) {
                        final VeryCompactGPSFixMovingImpl gpsFix = (VeryCompactGPSFixMovingImpl) fixToAdd;
                        fixToAdd = new GPSFixMovingImpl(gpsFix.getPosition(), fixToAdd.getTimePoint(), gpsFix.getSpeed(),
                                gpsFix.getOptionalTrueHeading());
                    } else if (fixToAdd instanceof VeryCompactGPSFixImpl) {
                        final VeryCompactGPSFixImpl gpsFix = (VeryCompactGPSFixImpl) fixToAdd;
                        fixToAdd = new GPSFixImpl(gpsFix.getPosition(), fixToAdd.getTimePoint());
                    }
                    fixesToAddAsBatch.add(fixToAdd);
                    if (fixesToAddAsBatch.size() == BATCH_SIZE_FOR_IMPORTING_FIXES) {
                        storeFixes(store, deviceIdentifier, fixesToAddAsBatch);
                    }
                    fix = objectInputStream.readObject();
                }
                if (!fixesToAddAsBatch.isEmpty()) {
                    storeFixes(store, deviceIdentifier, fixesToAddAsBatch);
                }
                device = objectInputStream.readObject();
                devicesDone++;
                if (numberOfDeviceSections > 0) {
                    final double fractionDone = (double) devicesDone / numberOfDeviceSections;
                    racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId,
                            PROGRESS_WIND_TRACKS_DONE + PROGRESS_SENSOR_FIXES_BAND_WIDTH * fractionDone,
                            DataImportSubProgress.IMPORT_SENSOR_FIXES, fractionDone);
                }
            }
        }
    }

    private void storeFixes(final SensorFixStore store, final DeviceIdentifier device,
            final Collection<Timed> fixesToAddAsBatch) {
        try {
            store.storeFixes(device, fixesToAddAsBatch, /* returnManeuverUpdate */ false, /* returnLiveDelay */ false);
        } catch (final NoCorrespondingServiceRegisteredException e) {
            logger.severe("Failed to store race log tracking fixes while importing.");
        }
        fixesToAddAsBatch.clear();
    }

    private void setAllowCompetitorsDataToBeReset(List<Serializable> competitorIds) {
        CompetitorAndBoatStore store = baseDomainFactory.getCompetitorAndBoatStore();
        for (Serializable id : competitorIds) {
            Competitor competitor = baseDomainFactory.getExistingCompetitorById(id);
            if (competitor != null) {
                store.allowCompetitorResetToDefaults(competitor);
            }
        }
    }

    /**
     * Replicates a stripped-down version of the {@code topLevelMasterData} to any replica attached. We assume here that
     * the {@link #racingEventService} provided to this imported is the "master" instance of this service. This must be
     * guaranteed by any service invoking this method, be it a REST API or a GWT RPC; they all need to ensure that their
     * request has previously been routed to the master node for the {@link RacingEventService}. The reason for this is
     * that the {@link TopLevelMasterData} object used for replication will have all tracking data stripped off which
     * helps reducing the object size to make it very likely for the operation to fit into a RabbitMQ replication
     * message, and because transmitting the tracking data to a replica this way would be redundant because it will get
     * replicated as soon as the master node starts loading those races.
     * 
     * @param topLevelMasterData
     *            the full master data with all tracking data attached; for replication, a stripped-down
     *            {@link TopLevelMasterData#copyAndStripOffDataNotNeededOnReplicas() copy} will be created. The
     *            full version will be applied to the {@link #racingEventService} locally.
     */
    private MasterDataImportObjectCreationCountImpl applyMasterDataImportOperation(TopLevelMasterData topLevelMasterData,
            UUID importOperationId, boolean override) {
        ImportMasterDataOperation strippedOpForReplicas = new ImportMasterDataOperation(
                topLevelMasterData.copyAndStripOffDataNotNeededOnReplicas(), importOperationId, override, user,
                tenant);
        // replicate explicitly first and let isRequiresExplicitTransitiveReplication return false; see also bug5574
        racingEventService.replicate(strippedOpForReplicas);
        ImportMasterDataOperation op = new ImportMasterDataOperation(topLevelMasterData, importOperationId, override,
                user, tenant);
        return racingEventService.apply(op);
    }

}
