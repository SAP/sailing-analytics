package com.sap.sailing.server.operationaltransformation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.DataImportSubProgress;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.MasterDataImportObjectCreationCountImpl;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.masterdataimport.TopLevelMasterData;
import com.sap.sailing.domain.masterdataimport.WindTrackMasterData;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.MongoRegattaLogStoreFactory;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.regattalike.HasRegattaLike;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.DummyTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.server.interfaces.DataImportLockWithProgress;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public class ImportMasterDataOperation extends
        AbstractRacingEventServiceOperation<MasterDataImportObjectCreationCountImpl> {

    private static final long serialVersionUID = 3131715325307370303L;

    private static final Logger logger = Logger.getLogger(ImportMasterDataOperation.class.getName());
    
    private static final int BATCH_SIZE_FOR_IMPORTING_FIXES = 5000;

    private final TopLevelMasterData masterData;

    private final MasterDataImportObjectCreationCountImpl creationCount;

    private final boolean override;

    private final UUID importOperationId;

    private DataImportProgress progress;

    private User user;

    private UserGroup tenant;
    
    private final Set<RaceTrackingConnectivityParameters> connectivityParametersToRestore;
    
    public ImportMasterDataOperation(TopLevelMasterData topLevelMasterData, UUID importOperationId, boolean override,
            User user, UserGroup tenant) {
        this.creationCount = new MasterDataImportObjectCreationCountImpl();
        this.masterData = topLevelMasterData;
        this.override = override;
        this.importOperationId = importOperationId;
        this.user = user;
        this.tenant = tenant;
        this.connectivityParametersToRestore = masterData.getConnectivityParametersToRestore();
    }

    /**
     * Operations of this type are expected to be explicitly sent out <em>before</em> the operation is applied locally on
     * the master server. This is important because otherwise tracking-related operations may be sent out before the
     * structures for regattas, events, etc. have been replicated. This also holds for the "reverse" replication direction
     * from a replica to a master. See also bug5574.
     */
    @Override
    public boolean isRequiresExplicitTransitiveReplication() {
        return false;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public MasterDataImportObjectCreationCountImpl internalApplyTo(RacingEventService toState) throws Exception {
        logger.info("Starting to import master data into "+toState);
        final DataImportLockWithProgress dataImportLock = toState.getDataImportLock();
        SecurityService securityService = toState.getSecurityService();
        if (securityService == null) {
            throw new IllegalStateException("Cannot import data, security service could not be resolved");
        }
        this.progress = dataImportLock.getProgress(importOperationId);
        progress.setCurrentSubProgress(DataImportSubProgress.IMPORT_WAIT);
        LockUtil.lockForWrite(dataImportLock);
        try {
            progress.setCurrentSubProgress(DataImportSubProgress.IMPORT_LEADERBOARD_GROUPS);
            progress.setCurrentSubProgressPct(0);
            int numOfGroupsToImport = masterData.getLeaderboardGroups().size();
            int i = 0;
            for (LeaderboardGroup leaderboardGroup : masterData.getLeaderboardGroups()) {
                createLeaderboardGroupWithAllRelatedObjects(toState, leaderboardGroup, securityService);
                i++;
                progress.setCurrentSubProgressPct((double) i / numOfGroupsToImport);
            }
            progress.setCurrentSubProgress(DataImportSubProgress.UPDATE_EVENT_LEADERBOARD_GROUP_LINKS);
            progress.setOverAllProgressPct(0.4);
            progress.setCurrentSubProgressPct(0);
            final Iterable<Event> allEvents = masterData.getAllEvents();
            int numOfEventsToHandle = Util.size(allEvents);
            int eventCounter = 0;
            for (Event e : allEvents) {
                updateLinksToLeaderboardGroups(toState, e);
                eventCounter++;
                progress.setCurrentSubProgressPct((double) eventCounter / numOfEventsToHandle);
            }
            progress.setCurrentSubProgress(DataImportSubProgress.IMPORT_WIND_TRACKS);
            progress.setOverAllProgressPct(0.5);
            progress.setCurrentSubProgressPct(0);
            createWindTracks(toState);
            progress.setCurrentSubProgress(DataImportSubProgress.IMPORT_SENSOR_FIXES);
            progress.setOverAllProgressPct(0.7);
            progress.setCurrentSubProgressPct(0);
            importRaceLogTrackingGPSFixes(toState);
            if (masterData.getDeviceConfigurations() != null) {
                importDeviceConfigurations(toState);
            }
            Collection<MediaTrack> allMediaTracksToImport = masterData.getFilteredMediaTracks();
            for (MediaTrack trackToImport : allMediaTracksToImport) {
                ensureOwnership(trackToImport.getIdentifier(), securityService);
            }
            toState.mediaTracksImported(allMediaTracksToImport, creationCount, override);
            progress.setCurrentSubProgress(DataImportSubProgress.IMPORT_TRACKED_RACES);
            progress.setOverAllProgressPct(0.8);
            progress.setCurrentSubProgressPct(0);
            final Iterable<TrackedRace> trackedRacesToWaitForLoadingComplete = importTrackedRaces(toState, securityService);            
            progress.setCurrentSubProgress(DataImportSubProgress.WAITING_FOR_TRACKED_RACES_TO_FINISH_LOADING);
            progress.setOverAllProgressPct(0.9);
            progress.setCurrentSubProgressPct(0);
            waitForTrackedRacesToFinishLoading(trackedRacesToWaitForLoadingComplete);
            dataImportLock.getProgress(importOperationId).setResult(creationCount);
            progress.setOverAllProgressPct(1.0);
            logger.info("Done importing master data into "+toState);
            return creationCount;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during execution of ImportMasterDataOperation", e);
            throw new RuntimeException("Error during execution of ImportMasterDataOperation", e);
        } finally {
            LockUtil.unlockAfterWrite(dataImportLock);
        }
    }

    private void waitForTrackedRacesToFinishLoading(Iterable<TrackedRace> trackedRacesToWaitForLoadingComplete) throws InterruptedException {
        final int toLoad = Util.size(trackedRacesToWaitForLoadingComplete);
        logger.info("Waiting for race loading to complete for "+toLoad+" races...");;
        final Set<TrackedRace> racesStillLoading = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Util.addAll(trackedRacesToWaitForLoadingComplete, racesStillLoading);
        final Object sync = new Object(); // on this object we synchronize the waiting and unblocking process
        for (final TrackedRace trackedRace : trackedRacesToWaitForLoadingComplete) {
            trackedRace.addListener(new AbstractRaceChangeListener() {
                @Override
                public void statusChanged(TrackedRaceStatus newStatus, TrackedRaceStatus oldStatus) {
                    if ((oldStatus.getStatus() == TrackedRaceStatusEnum.PREPARED || oldStatus.getStatus() == TrackedRaceStatusEnum.LOADING)
                            && newStatus.getStatus().getOrder() > TrackedRaceStatusEnum.LOADING.getOrder()) {
                        logger.info("Race "+trackedRace+" has finished loading");
                        synchronized (progress) {
                            progress.setCurrentSubProgressPct((double) (toLoad - racesStillLoading.size()) / (double) toLoad);
                        }
                        racesStillLoading.remove(trackedRace);
                        synchronized (sync) {
                            if (racesStillLoading.isEmpty()) {
                                sync.notifyAll();
                            }
                        }
                        trackedRace.removeListener(this);
                    }
                }
            });
        }
        for (Iterator<TrackedRace> i = racesStillLoading.iterator(); i.hasNext(); ) {
            final TrackedRace trackedRace = i.next();
            if (trackedRace.getStatus().getStatus().getOrder() > TrackedRaceStatusEnum.LOADING.getOrder()) {
                logger.info("Race "+trackedRace+" has finished loading");
                i.remove();
                progress.setCurrentSubProgressPct((double) (toLoad - racesStillLoading.size()) / (double) toLoad);
            }
        }
        synchronized (sync) {
            while (!racesStillLoading.isEmpty()) {
                sync.wait();
            }
        }
        logger.info("All races imported have finished loading");
    }

    private void importDeviceConfigurations(RacingEventService toState) {
        if (toState.getMasterDescriptor() == null) { // don't do this on a replica's RacingEventService; device config removals/additions are replicated by RacingEventService
            Iterable<DeviceConfiguration> newConfigs = masterData.getDeviceConfigurations();
            for (DeviceConfiguration config : newConfigs) {
                if (toState.getDeviceConfigurationById(config.getId()) != null) {
                    if (override) {
                        logger.info(String.format(
                                "Device configuration [%s] with name \"%s\" already exists. Overwrite because override flag is set.",
                                config.getId(), config.getName()));
                        toState.removeDeviceConfiguration(config.getId());
                        toState.createOrUpdateDeviceConfiguration(config);
                        // FIXME ownership here!
                    } else {
                        logger.info(String
                                .format("Device configuration [%s] with name \"%s\" already exists. Not overwriting because override flag is not set.",
                                        config.getId(), config.getName()));
                    }
                } else {
                    toState.createOrUpdateDeviceConfiguration(config);
                }
            }
        }
    }

    /**
     * Ensures that all links from <code>eventReceived</code> to its leaderboard groups are established also on the
     * local event after import as long as those leaderboard groups are part of the actual import. For this subset of
     * leaderboard groups, equality of ordering is established between the <code>eventReceived</code>'s leaderboard
     * group sequence and the local event's leaderboard group sequence. This may require temporarily removing
     * leaderboard groups from the local event and re-adding them at the end which may change the ordering with respect
     * to other, non-imported leaderboard groups.
     * <p>
     * 
     * Loops over the imported event's leaderboard groups and for those part of the import tries to find by ID each of
     * them in the local event's leaderboard group sequence. If not found, it is appended at the end. If found after the
     * position of the previous leaderboard group handled, it is left in place. Otherwise, it is removed and added again
     * at the end.
     */
    private void updateLinksToLeaderboardGroups(RacingEventService racingEventService, Event eventReceived) {
        boolean changed = false;
        int positionOfLastLeaderboardGroupFoundInLocalEvent = -1;
        Event eventAfterImport = racingEventService.getEvent(eventReceived.getId());
        Collection<LeaderboardGroup> leaderboardGroupsReceived = masterData.getLeaderboardGroups();
        for (LeaderboardGroup lgInEventReceived : eventReceived.getLeaderboardGroups()) {
            if (leaderboardGroupsReceived.contains(lgInEventReceived)) {
                // it shall also be referenced by eventAfterImport, with a position that shall be greater than
                // positionOfLastLeaderboardGroupFoundInLocalEvent.
                int pos = 0;
                boolean found = false;
                for (LeaderboardGroup importedLg : eventAfterImport.getLeaderboardGroups()) {
                    if (importedLg.getId().equals(lgInEventReceived.getId())) {
                        found = true;
                        if (pos < positionOfLastLeaderboardGroupFoundInLocalEvent) {
                            // need to move lgInEventReceived; move to end
                            eventAfterImport.removeLeaderboardGroup(importedLg);
                            eventAfterImport.addLeaderboardGroup(importedLg);
                            positionOfLastLeaderboardGroupFoundInLocalEvent = Util.size(eventAfterImport.getLeaderboardGroups())-1;
                            changed = true;
                        } else {
                            positionOfLastLeaderboardGroupFoundInLocalEvent = pos;
                        }
                        break;
                    }
                    pos++;
                }
                if (!found) {
                    eventAfterImport.addLeaderboardGroup(racingEventService.getLeaderboardGroupByID(lgInEventReceived.getId()));
                    positionOfLastLeaderboardGroupFoundInLocalEvent = Util.size(eventAfterImport.getLeaderboardGroups())-1;
                    changed = true;
                }
            }
        }
        if (changed) {
            racingEventService.getMongoObjectFactory().storeEvent(eventAfterImport);
        }
    }

    private void createLeaderboardGroupWithAllRelatedObjects(final RacingEventService toState,
            LeaderboardGroup leaderboardGroup, SecurityService securityService) {
        Map<String, Leaderboard> existingLeaderboards = toState.getLeaderboards();
        List<String> leaderboardNames = new ArrayList<String>();
        createCourseAreasAndEvents(toState, leaderboardGroup, securityService);
        createRegattas(toState, leaderboardGroup, securityService);
        for (final Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
            leaderboardNames.add(leaderboard.getName());
            if (existingLeaderboards.containsKey(leaderboard.getName())) {
                if (creationCount.alreadyAddedLeaderboardWithName(leaderboard.getName())) {
                    // Has already been added by this operation
                    continue;
                } else if (override) {
                    for (RaceColumn raceColumn : existingLeaderboards.get(leaderboard.getName()).getRaceColumns()) {
                        for (Fleet fleet : raceColumn.getFleets()) {
                            final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                            if (trackedRace != null) {
                                raceColumn.releaseTrackedRace(fleet);
                            }
                        }
                    }
                    if (toState.getLeaderboardByName(leaderboard.getName()) != null) {
                        toState.removeLeaderboard(leaderboard.getName());
                    }
                    logger.info(String.format("Leaderboard with name %1$s already existed and has been overridden.",
                            leaderboard.getName()));
                } else {
                    logger.info(String.format("Leaderboard with name %1$s already exists and hasn't been overridden.",
                            leaderboard.getName()));
                    continue;
                }
            }
            if (leaderboard != null) {
                toState.addLeaderboard(leaderboard);
                storeRaceLogEvents(leaderboard, toState.getMongoObjectFactory(), toState.getDomainObjectFactory(), override);
                storeRegattaLogEvents(leaderboard, toState.getMongoObjectFactory(), toState.getDomainObjectFactory(), override);
                ensureOwnership(leaderboard.getIdentifier(), securityService);
                creationCount.addOneLeaderboard(leaderboard.getName());
                relinkTrackedRacesIfPossible(toState, leaderboard);
                toState.updateStoredLeaderboard(leaderboard);
            }
        }
        LeaderboardGroup existingLeaderboardGroup = toState.getLeaderboardGroupByID(leaderboardGroup.getId());
        if (existingLeaderboardGroup != null && override) {
            logger.info(String.format("Leaderboard Group with ID %1$s already existed and will be overridden.", leaderboardGroup.getId()));
            toState.removeLeaderboardGroup(leaderboardGroup.getId());
            existingLeaderboardGroup = null;
        }
        if (existingLeaderboardGroup == null) {
            toState.addLeaderboardGroupWithoutReplication(leaderboardGroup);
            creationCount.addOneLeaderboardGroup(leaderboardGroup.getName());
            if (leaderboardGroup.getOverallLeaderboard() != null) {
                ensureOwnership(leaderboardGroup.getOverallLeaderboard().getIdentifier(), securityService);
            }
            ensureOwnership(leaderboardGroup.getIdentifier(), securityService);
        } else {
            logger.info(String.format("Leaderboard Group with name %1$s already exists and hasn't been overridden.",
                    leaderboardGroup.getName()));
        }
    }

    /**
     * Ensures that the race log events are stored to the receiving instance's database. The race logs have been
     * received in serialized form on the {@link RaceColumn} objects, but the database doesn't yet know about them. This
     * method uses a <code>MongoRaceLogStoreVisitor</code> to store all race log events to the database.
     */
    private void storeRaceLogEvents(Leaderboard leaderboard, MongoObjectFactory mongoObjectFactory,
            DomainObjectFactory domainObjectFactory, boolean override) {
        RaceLogStore mongoRaceLogStore = MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory,
                domainObjectFactory);
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                RaceLog log = raceColumn.getRaceLog(fleet);
                if (log != null) {
                    RaceLogIdentifier identifier = raceColumn.getRaceLogIdentifier(fleet);
                    RaceLog currentPersistedLog = mongoRaceLogStore.getRaceLog(identifier, true);
                    if (currentPersistedLog.isEmpty()) {
                        addAllImportedEvents(mongoObjectFactory, mongoRaceLogStore, log, identifier);
                    } else if (override) {
                        // Clear existing race log
                        mongoRaceLogStore.removeRaceLog(identifier);
                        addAllImportedEvents(mongoObjectFactory, mongoRaceLogStore, log, identifier);
                    }
                }
            }
        }
    }

    private void addAllImportedEvents(MongoObjectFactory mongoObjectFactory, RaceLogStore mongoRaceLogStore,
            final RaceLog log, RaceLogIdentifier identifier) {
        final RaceLogEventVisitor storeVisitor = MongoRaceLogStoreFactory.INSTANCE
                .getMongoRaceLogStoreVisitor(identifier, mongoObjectFactory);
        log.lockForRead();
        try {
            for (RaceLogEvent event : log.getRawFixes()) {
                event.accept(storeVisitor);
            }
        } finally {
            log.unlockAfterRead();
        }
        // Make sure listener is added to race log
        mongoRaceLogStore.addImportedRaceLog(log, identifier);
    }

    /**
     * Ensures that the regatta log events are stored to the receiving instance's database. The {@code leaderboard}
     * potentially {@link HasRegattaLike has} an attached RegattaLog, which then must be stored in the database.
     * @see #storeRaceLogEvents(Leaderboard, MongoObjectFactory)
     */
    private void storeRegattaLogEvents(Leaderboard leaderboard, MongoObjectFactory mongoObjectFactory, DomainObjectFactory domainObjectFactory,
            boolean override) {
        RegattaLogStore regattaLogStore = MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStore(mongoObjectFactory, domainObjectFactory);
        if (leaderboard instanceof HasRegattaLike) {
            IsRegattaLike regattaLike = ((HasRegattaLike) leaderboard).getRegattaLike();
            RegattaLog log = regattaLike.getRegattaLog();
            RegattaLikeIdentifier identifier = regattaLike.getRegattaLikeIdentifier();
            RegattaLogEventVisitor storeVisitor = MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStoreVisitor(
                    identifier, mongoObjectFactory);
            RegattaLog currentPersistedLog = regattaLogStore.getRegattaLog(identifier, true);
            if (currentPersistedLog.isEmpty()) {
                addAllImportedRegattaEvents(regattaLogStore, log, identifier, storeVisitor);
            } else if (override) {
                //Clear existing regatta log
                regattaLogStore.removeRegattaLog(identifier);
                addAllImportedRegattaEvents(regattaLogStore, log, identifier, storeVisitor);
            }
        }
    }

    private void addAllImportedRegattaEvents(RegattaLogStore regattaLogStore, final RegattaLog log,
            RegattaLikeIdentifier identifier, RegattaLogEventVisitor storeVisitor) {
        log.lockForRead();
        try {
            for (RegattaLogEvent event : log.getRawFixes()) {
                event.accept(storeVisitor);
            }
        } finally {
            log.unlockAfterRead();
        }
        regattaLogStore.addImportedRegattaLog(log, identifier);
    }

    private void relinkTrackedRacesIfPossible(RacingEventService toState, Leaderboard newLeaderboard) {
        if (newLeaderboard instanceof FlexibleLeaderboard) {
            for (RaceColumn raceColumn : newLeaderboard.getRaceColumns()) {
                for (Fleet fleet : raceColumn.getFleets()) {
                    RaceIdentifier raceIdentifier = raceColumn.getRaceIdentifier(fleet);
                    if (raceIdentifier != null) {
                        DynamicTrackedRace trackedRace = toState
                                .getTrackedRace((RegattaAndRaceIdentifier) raceIdentifier);
                        raceColumn.setTrackedRace(fleet, trackedRace);
                        // in case the TrackedRace wasn't found (see also bug 5982), at least record the race identifier
                        raceColumn.setRaceIdentifier(fleet, raceIdentifier);
                    }
                }
            }
        }
    }

    private void createWindTracks(RacingEventService toState) {
        if (toState.getMasterDescriptor() == null) { // don't do this on a replica's RacingEventService; wind data will be received through the tracked race loading replication
            int numOfWindTracks = masterData.getWindTrackMasterData().size();
            int i = 0;
            for (WindTrackMasterData windMasterData : masterData.getWindTrackMasterData()) {
                DummyTrackedRace trackedRaceWithNameAndId = new DummyTrackedRace(windMasterData.getRaceName(), windMasterData.getRaceId());
                WindTrack windTrackToWriteTo = toState.getWindStore().getWindTrack(windMasterData.getRegattaName(), trackedRaceWithNameAndId, windMasterData.getWindSource(), 0, -1);
                final WindTrack windTrackToReadFrom = windMasterData.getWindTrack();
                final List<Wind> fixesToAdd = new ArrayList<>();
                windTrackToReadFrom.lockForRead();
                try {
                    for (Wind fix : windTrackToReadFrom.getRawFixes()) {
                        Wind existingFix = windTrackToWriteTo.getFirstRawFixAtOrAfter(fix.getTimePoint());
                        if (existingFix == null || !existingFix.equals(fix)) {
                            fixesToAdd.add(fix);
                        } else {
                            logger.fine("Didn't add wind fix in import, because equal fix was already there.");
                        }
                    }
                } finally {
                    windTrackToReadFrom.unlockAfterRead();
                }
                windTrackToWriteTo.add(fixesToAdd);
                i++;
                progress.setCurrentSubProgressPct((double) i / numOfWindTracks);
                progress.setOverAllProgressPct(0.5 + (0.3) * ((double) i / numOfWindTracks));
            }
        }
    }
    
    private void importRaceLogTrackingGPSFixes(RacingEventService toState) {
        if (toState.getMasterDescriptor() == null) { // don't do this on a replica's RacingEventService; tracking data will be received through the tracked race loading replication
            Map<DeviceIdentifier, Set<Timed>> raceLogTrackingFixes = masterData.getRaceLogTrackingFixes();
            if (raceLogTrackingFixes != null) {
                SensorFixStore store = toState.getSensorFixStore();
                int i = 0;
                final int numberOfDevices = raceLogTrackingFixes.size();
                for (Entry<DeviceIdentifier, Set<Timed>> entry : raceLogTrackingFixes.entrySet()) {
                    DeviceIdentifier device = entry.getKey();
                    final Collection<Timed> fixesToAddAsBatch = new ArrayList<>(BATCH_SIZE_FOR_IMPORTING_FIXES);
                    for (Timed fixToAdd : entry.getValue()) {
                        if (fixToAdd instanceof VeryCompactGPSFixMovingImpl) {
                            VeryCompactGPSFixMovingImpl gpsFix = (VeryCompactGPSFixMovingImpl) fixToAdd;
                            fixToAdd = new GPSFixMovingImpl(gpsFix.getPosition(), fixToAdd.getTimePoint(),
                                    ((VeryCompactGPSFixMovingImpl) fixToAdd).getSpeed(), gpsFix.getOptionalTrueHeading());
                        } else if (fixToAdd instanceof VeryCompactGPSFixImpl) {
                            VeryCompactGPSFixImpl gpsFix = (VeryCompactGPSFixImpl) fixToAdd;
                            fixToAdd = new GPSFixImpl(gpsFix.getPosition(), fixToAdd.getTimePoint());
                        } 
                        fixesToAddAsBatch.add(fixToAdd);
                        if (fixesToAddAsBatch.size() == BATCH_SIZE_FOR_IMPORTING_FIXES) {
                            storeFixes(store, device, fixesToAddAsBatch);
                        }
                    }
                    if (!fixesToAddAsBatch.isEmpty()) {
                        storeFixes(store, device, fixesToAddAsBatch);
                    }
                    i++;
                    progress.setCurrentSubProgressPct((double) i / numberOfDevices);
                }
            }
        }
    }

    private void storeFixes(SensorFixStore store, DeviceIdentifier device, final Collection<Timed> fixesToAddAsBatch) {
        try {
            store.storeFixes(device, fixesToAddAsBatch, /* returnManeuverUpdate */ false, /* returnLiveDelay */ false);
            fixesToAddAsBatch.clear();
        } catch (NoCorrespondingServiceRegisteredException e) {
            logger.severe("Failed to store race log tracking fixes while importing.");
        }
    }

    private void createRegattas(RacingEventService toState, LeaderboardGroup leaderboardGroup,
            SecurityService securityService) {
        Iterable<Leaderboard> leaderboards = leaderboardGroup.getLeaderboards();
        for (Leaderboard leaderboard : leaderboards) {
            if (leaderboard instanceof RegattaLeaderboard) {
                RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                Regatta regatta = regattaLeaderboard.getRegatta();
                Regatta existingRegatta = toState.getRegatta(regatta.getRegattaIdentifier());
                if (existingRegatta != null) {
                    if (creationCount.alreadyAddedRegattaWithId(existingRegatta.getId().toString())) {
                        // Already added earlier in this import process
                        continue;
                    } else if (override) {
                        logger.info(String
                                .format("Regatta with name %1$s already existed and has been overridden. All it's tracked races were stopped and removed.",
                                        regatta.getRegattaIdentifier()));
                        try {
                            TrackedRegatta trackedRegatta = toState.getTrackedRegatta(existingRegatta);
                            List<TrackedRace> toRemove = new ArrayList<TrackedRace>();
                            if (trackedRegatta != null) {
                                trackedRegatta.lockTrackedRacesForRead();
                                try {
                                    for (TrackedRace race : trackedRegatta.getTrackedRaces()) {
                                        toRemove.add(race);
                                    }
                                } finally {
                                    trackedRegatta.unlockTrackedRacesAfterRead();
                                }
                                for (TrackedRace raceToRemove : toRemove) {
                                    trackedRegatta.removeTrackedRace(raceToRemove, Optional.of(toState
                                            .getThreadLocalTransporterForCurrentlyFillingFromInitialLoadOrApplyingOperationReceivedFromMaster()));
                                    RaceDefinition race = existingRegatta.getRaceByName(raceToRemove
                                            .getRaceIdentifier().getRaceName());
                                    if (race != null) {
                                        try {
                                            toState.removeRace(existingRegatta, race);
                                        } catch (IOException | InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }
                            toState.stopTrackingAndRemove(existingRegatta);
                            creationCount.addOverwrittenRegattaName(existingRegatta.getName());
                            toState.removeRegatta(existingRegatta);
                        } catch (IOException | InterruptedException e) {
                            logger.warning(String.format(
                                    "Regatta with name %1$s could not be deleted due to an error.",
                                    regatta.getRegattaIdentifier()));
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        logger.info(String.format("Regatta with name %1$s already exists and hasn't been overridden.",
                                regatta.getRegattaIdentifier()));
                        continue;
                    }
                }
                toState.addRegattaWithoutReplication(regatta);
                Set<String> raceIdStrings = masterData.getRaceIdStringsForRegatta().get(regatta.getRegattaIdentifier());
                if (raceIdStrings != null) {
                    for (String raceIdAsString : raceIdStrings) {
                        if (!override && toState.getRememberedRegattaForRace(raceIdAsString) != null) {
                            logger.info(String
                                    .format("Persistent regatta wasn't set for race id %1$s, because override was not turned on.",
                                            raceIdAsString));
                        } else {
                            toState.setRegattaForRace(regatta, raceIdAsString);
                        }
                    }
                }
                ensureOwnership(regatta.getIdentifier(), securityService);
                creationCount.addOneRegatta(regatta.getId().toString());
            }
        }
    }

    private void createCourseAreasAndEvents(RacingEventService toState, LeaderboardGroup leaderboardGroup,
            SecurityService securityService) {
        for (Event event : masterData.getEventForLeaderboardGroup().get(leaderboardGroup)) {
            UUID id = event.getId();
            Event existingEvent = toState.getEvent(id);
            if (existingEvent != null && override && !creationCount.alreadyAddedEventWithId(id.toString())) {
                logger.info(String.format("Event with ID %1$s already existed and will be overridden.", event.getId()));
                toState.removeEvent(existingEvent.getId());
                existingEvent = null;
            }
            if (existingEvent == null) {
                toState.addEventWithoutReplication(event);
                ensureOwnership(event.getIdentifier(), securityService);
                creationCount.addOneEvent(event.getId().toString());
            } else {
                logger.info(String.format("Event with name %1$s already exists and hasn't been overridden.",
                        event.getName()));
            }
        }
    }

    private void ensureOwnership(QualifiedObjectIdentifier identifier, SecurityService securityService) {
        logger.info("Trying to adopt " + identifier + " from Masterdataimport to " + user.getName() +
                " and group " + (tenant==null?"null":tenant.getName())+" if orphaned");
        securityService.setOwnershipIfNotSet(identifier, user, tenant);
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<?> serverOp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<?> clientOp) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * in order to restore all listeners we need to initialize the regatta after the whole object graph has been
     * restored. This applies to all replicas that receive this operation "over the wire".
     * 
     * Fixes bug2023
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        for (Regatta regatta : masterData.getAllRegattas()) {
            RegattaImpl regattaImpl = (RegattaImpl) regatta;
            regattaImpl.initializeSeriesAfterDeserialize();
        }
    }
    
    /**
     * Starts the tracking of imported tracked races.
     * 
     * @return the set of tracked races for which loading/tracking has started. A caller may use those, e.g., to observe
     *         their loading/tracking status. Races that didn't start loading/tracking within a
     *         {@link RaceTracker#TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS timeout} will not be returned as
     *         part of this set since waiting for them is considered futile.
     */
    private Iterable<TrackedRace> importTrackedRaces(RacingEventService toState, SecurityService securityService) throws Exception {
        final Set<TrackedRace> result = new HashSet<>();
        // only start importing / loading tracked races content if not running on a replica
        if (connectivityParametersToRestore != null && toState.getMasterDescriptor() == null) {
            int i = 0;
            final int numberOfConnectivityParamsToRestore = connectivityParametersToRestore.size();
            for (RaceTrackingConnectivityParameters param : connectivityParametersToRestore) {
                if (param != null) {
                    final RaceTrackingConnectivityParameters paramToStartTracking = toState
                            .getRaceTrackingConnectivityParamsServiceFinder().findService(param.getTypeIdentifier())
                            .resolve(param);
                    RaceHandle raceHandle = toState.addRace(/* default */ null, paramToStartTracking, /* do not wait */ -1);
                    final RaceDefinition race = raceHandle.getRace(RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS);
                    if (race != null) {
                        final DynamicTrackedRace trackedRace = raceHandle.getTrackedRegatta().getTrackedRace(race); // wait/block for tracked race to show up
                        result.add(trackedRace);
                        ensureOwnership(trackedRace.getIdentifier(), securityService);
                        creationCount.addOneTrackedRace(race.getId().toString());
                    } else {
                        logger.severe("Race for handle "+raceHandle+" didn't show in "+RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS+"ms; ownership not set");
                    }
                }
                i++;
                progress.setCurrentSubProgressPct((double) i / numberOfConnectivityParamsToRestore);
            }
        }
        return result;
    }
}
