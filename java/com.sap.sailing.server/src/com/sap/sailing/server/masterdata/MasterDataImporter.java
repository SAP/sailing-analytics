package com.sap.sailing.server.masterdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.sap.sailing.domain.common.MasterDataImportObjectCreationCount;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.masterdataimport.TopLevelMasterData;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.ImportMasterDataOperation;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache.ResolveListener;

public class MasterDataImporter {
    private final static Logger logger = Logger.getLogger(MasterDataImporter.class.getName());
    private static final int BATCH_SIZE_FOR_IMPORTING_FIXES = 5000;
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

    public Map<LeaderboardGroup, ? extends Iterable<Event>> importFromStream(InputStream inputStream, UUID importOperationId, boolean override)
            throws IOException, ClassNotFoundException {
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
        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, 0.03,
                DataImportSubProgress.TRANSFER_STARTED, 0.5);
        RaceLogStore raceLogStore = MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(
                racingEventService.getMongoObjectFactory(), racingEventService.getDomainObjectFactory());
        RegattaImpl.setOngoingMasterDataImport(new MasterDataImportInformation(raceLogStore));
        final ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        final TopLevelMasterData topLevelMasterData;
        Thread.currentThread().setContextClassLoader(racingEventService.getDeserializationClassLoader());
        try {
            @SuppressWarnings("unchecked")
            final List<Serializable> competitorIds = (List<Serializable>) objectInputStream.readObject();
            if (override) {
                setAllowCompetitorsDataToBeReset(competitorIds);
            }
            // Deserialize Regattas to make sure that Regattas are deserialized before Series
            objectInputStream.readObject();
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
        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, 0.3,
                DataImportSubProgress.TRANSFER_COMPLETED, 0.5);
        applyMasterDataImportOperation(topLevelMasterData, importOperationId, override);
        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, 0.7,
                DataImportSubProgress.IMPORT_SENSOR_FIXES, 0);
        final ClassLoader oldContextClassLoaderForFixes = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(racingEventService.getDeserializationClassLoader());
        try {
            importRaceLogTrackingGPSFixes(objectInputStream, importOperationId,
                    Util.size(topLevelMasterData.getRaceLogTrackingFixMappings()));
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoaderForFixes);
        }
        return topLevelMasterData.getEventForLeaderboardGroup();
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
     * driven by {@code numberOfDeviceSections} (the number of {@link TopLevelMasterData#getRaceLogTrackingFixMappings()
     * fix mapping descriptors}, which equals the number of device sections the exporter writes). A finer per-fix
     * granularity is intentionally not attempted because the streaming format carries no fix count.
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
                    racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, 0.7,
                            DataImportSubProgress.IMPORT_SENSOR_FIXES, (double) devicesDone / numberOfDeviceSections);
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
    private MasterDataImportObjectCreationCount applyMasterDataImportOperation(TopLevelMasterData topLevelMasterData,
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
