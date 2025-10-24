package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import static com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.storeDeviceId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.lang.Nullable;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sailing.domain.persistence.racelog.tracking.MongoSensorFixStore;
import com.sap.sailing.domain.racelog.tracking.FixReceivedListener;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

/**
 * At the moment, the timerange covered by the fixes for a device, and the number of fixes for a device are stored in a
 * metadata collection. Should be changed, see bug 1982.
 * 
 * @author Fredrik Teschke
 *
 */
public class MongoSensorFixStoreImpl extends MongoFixHandler implements MongoSensorFixStore {
    private static final Logger logger = Logger.getLogger(MongoSensorFixStoreImpl.class.getName());
    private final MongoCollection<Document> fixesCollection;
    private final MetadataCollection metadataCollection;
    private final MongoObjectFactoryImpl mongoOF;
    
    /**
     * Allows for causally-consistent access to the store; helpful, e.g., during test cases. May be {@code null}.
     */
    @Nullable
    private final ClientSession clientSession;
    
    /**
     * The write concern to use for updating the fixes and metadata collections. Tests can use this, e.g., to work with
     * {@link WriteConcern#MAJORITY} in order to ensure they can read their own writes.
     */
    private final WriteConcern writeConcern;
    
    /**
     * The read concern to use for reading/loading fixes and metadata. Tests can use this, e.g., to work with
     * {@link ReadConcern#MAJORITY} in order to ensure they can read their own writes.
     */
    private final ReadConcern readConcern;
    
    /**
     * Lock object to be used when accessing {@link #listeners}.
     */
    private final NamedReentrantReadWriteLock listenersLock = new NamedReentrantReadWriteLock("Listeners collection lock of " + MongoSensorFixStoreImpl.class.getName(), false);
    private final Map<DeviceIdentifier, Set<FixReceivedListener<? extends Timed>>> listeners = new HashMap<>();

    public MongoSensorFixStoreImpl(MongoObjectFactory mongoObjectFactory, DomainObjectFactory domainObjectFactory,
            TypeBasedServiceFinderFactory serviceFinderFactory) {
        this(mongoObjectFactory, domainObjectFactory, serviceFinderFactory, ReadConcern.DEFAULT,
                WriteConcern.UNACKNOWLEDGED, /* clientSession */ null, /* metadataCollectionClientSession */ null);
    }
    
    public MongoSensorFixStoreImpl(MongoObjectFactory mongoObjectFactory, DomainObjectFactory domainObjectFactory,
            TypeBasedServiceFinderFactory serviceFinderFactory, ReadConcern readConcern, WriteConcern writeConcern,
            ClientSession clientSession, ClientSession metadataCollectionClientSession) {
        super(serviceFinderFactory != null ? createFixServiceFinder(serviceFinderFactory) : null,
              serviceFinderFactory != null ? serviceFinderFactory.createServiceFinder(DeviceIdentifierMongoHandler.class) : null);
        mongoOF = (MongoObjectFactoryImpl) mongoObjectFactory;
        fixesCollection = mongoOF.getGPSFixCollection(clientSession);
        metadataCollection = new MetadataCollection(mongoOF, fixServiceFinder, deviceServiceFinder, readConcern, writeConcern, metadataCollectionClientSession);
        this.writeConcern = writeConcern;
        this.readConcern = readConcern;
        this.clientSession = clientSession;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static TypeBasedServiceFinder<FixMongoHandler<?>> createFixServiceFinder(
            TypeBasedServiceFinderFactory serviceFinderFactory) {
        return (TypeBasedServiceFinder) serviceFinderFactory.createServiceFinder(FixMongoHandler.class);
    }

    @Override
    public <FixT extends Timed> boolean loadOldestFix(Consumer<FixT> consumer, DeviceIdentifier device, TimeRange timeRangeToLoad) throws NoCorrespondingServiceRegisteredException, TransformationException {
        return loadFixes(consumer, device, timeRangeToLoad.from(), timeRangeToLoad.to(), false, () -> false, (d) -> {
        }, true, true);
    }
    
    @Override
    public <FixT extends Timed> boolean loadYoungestFix(Consumer<FixT> consumer, DeviceIdentifier device, TimeRange timeRangeToLoad) throws NoCorrespondingServiceRegisteredException, TransformationException {
        return loadFixes(consumer, device, timeRangeToLoad.from(), timeRangeToLoad.to(), /* inclusive */ false, () -> false, (d) -> {
        }, /* ascending */ false, /* only one result */ true);
    }
    
    @Override
    public <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier device, TimePoint from,
            TimePoint to, boolean inclusive) throws NoCorrespondingServiceRegisteredException, TransformationException {
        loadFixes(consumer, device, from, to, inclusive, () -> false, /* progress consumer */ d -> {});
    }
    
    @Override
    public <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier device, TimePoint from,
            TimePoint to, boolean inclusive, BooleanSupplier isPreemptiveStopped, Consumer<Double> progressConsumer)
                    throws NoCorrespondingServiceRegisteredException, TransformationException {
        loadFixes(consumer, device, from, to, inclusive, isPreemptiveStopped, progressConsumer, true, false);
    }

    private <FixT extends Timed> boolean loadFixes(Consumer<FixT> consumer, DeviceIdentifier device, TimePoint from,
            TimePoint to, boolean inclusive, BooleanSupplier isPreemptiveStopped, Consumer<Double> progressConsumer,
            boolean ascending, boolean onlyOneResult)
            throws NoCorrespondingServiceRegisteredException, TransformationException {
        progressConsumer.accept(0d);
        final TimePoint loadFixesFrom = from == null ? TimePoint.BeginningOfTime : from;
        final TimePoint loadFixesTo = to == null ? TimePoint.EndOfTime : to;
        Bson dbDeviceId = com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.getDeviceQuery(deviceServiceFinder, device);
        final List<Bson> filters = new ArrayList<>();
        filters.add(dbDeviceId);
        filters.add(Filters.gte(FieldNames.TIME_AS_MILLIS.name(), loadFixesFrom.asMillis()));
        if (inclusive) {
            filters.add(Filters.lte(FieldNames.TIME_AS_MILLIS.name(), loadFixesTo.asMillis()));
        } else {
            filters.add(Filters.lt(FieldNames.TIME_AS_MILLIS.name(), loadFixesTo.asMillis()));
        }
        Bson query = Filters.and(filters);
        final MongoCollection<Document> fixesCollectionWithReadConcern = fixesCollection.withReadConcern(readConcern);
        FindIterable<Document> result = clientSession == null ? fixesCollectionWithReadConcern.find(query) : fixesCollectionWithReadConcern.find(clientSession, query);
        result.batchSize(100000).sort(new Document(FieldNames.TIME_AS_MILLIS.name(), ascending ? 1 : -1));
        if (onlyOneResult) {
            result.limit(1);
        }
        boolean fixLoaded = false;
        final Duration totalDurationToLoad = loadFixesFrom.until(loadFixesTo);
        // Given that fixes are recorded with a rate of 10/s we update the progress every 10*60*5=3000 fixes.
        final Duration minimumDurationBetweenProgressUpdates = Util.max(totalDurationToLoad.divide(20), Duration.ONE_MINUTE.times(5));
        TimePoint nextProgressUpdateAt = ascending
                ? loadFixesFrom.plus(minimumDurationBetweenProgressUpdates)
                : loadFixesTo.minus(minimumDurationBetweenProgressUpdates);
        for (Document fixObject : result) {
            try {
                FixT fix = loadFix(fixObject);
                consumer.accept(fix);
                fixLoaded = true;
                TimePoint fixTimePoint = fix.getTimePoint();
                if (ascending ? fixTimePoint.after(nextProgressUpdateAt) : fixTimePoint.before(nextProgressUpdateAt)) {
                    final Duration durationAlreadyLoaded = ascending ? loadFixesFrom.until(fixTimePoint) : fixTimePoint.until(loadFixesTo);
                    progressConsumer.accept(durationAlreadyLoaded.divide(totalDurationToLoad));
                    nextProgressUpdateAt = ascending
                            ? fixTimePoint.plus(minimumDurationBetweenProgressUpdates)
                            : fixTimePoint.minus(minimumDurationBetweenProgressUpdates);
                    if (isPreemptiveStopped.getAsBoolean()) {
                        logger.log(Level.WARNING, "Exiting because of preemtive stop requested " + fixObject);
                        return fixLoaded;
                    }
                }
            } catch (TransformationException e) {
                logger.log(Level.WARNING, "Could not read fix from MongoDB: " + fixObject);
            } catch (ClassCastException e) {
                String type = (String) fixObject.get(FieldNames.GPSFIX_TYPE.name());
                logger.log(Level.WARNING,
                        "Unexpected fix type (" + type + ") encountered when trying to load track for " + device);
            }
        }
        progressConsumer.accept(1d);
        
        return fixLoaded;
    }

    /**
     * Store fixes in batches, reducing metadata storage update.
     * 
     * @return the identifiers of those races in which new maneuvers have been discovered since the last update for the
     *         competitor / boat to which the device is mapped and/or information about the race's live delay, if
     *         requested; always a valid, non-{@code null} but potentially empty collection.
     */
    @Override
    public <FixT extends Timed> Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> storeFixes(DeviceIdentifier device,
            Iterable<FixT> fixes, boolean returnManeuverChanges, boolean returnLiveDelay) {
        final Set<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> racesWithManeuverChangesOrLiveDelay = new HashSet<>();
        if (!Util.isEmpty(fixes)) {
            try {
                final Object dbDeviceId = storeDeviceId(deviceServiceFinder, device);
                final int nrOfTotalFixes = Util.size(fixes);
                final ArrayList<Document> dbFixes = new ArrayList<>(nrOfTotalFixes);
                TimeRange newTimeRange = null;
                FixT latestFix = null;
                for (FixT fix : fixes) {
                    if (latestFix == null || latestFix.getTimePoint().before(fix.getTimePoint())) {
                        latestFix = fix;
                    }
                    Document entry = new Document().append(FieldNames.DEVICE_ID.name(), dbDeviceId);
                    storeFixToDocument(entry, fix);
                    mongoOF.storeTimed(fix, entry);
                    dbFixes.add(entry);
                    TimePoint fixTP = fix.getTimePoint();
                    final TimeRangeImpl fixTimeRange = new TimeRangeImpl(fixTP, fixTP, /* toIsInclusive */ true);
                    newTimeRange = newTimeRange == null ? fixTimeRange : newTimeRange.extend(fixTimeRange);
                }
                final MongoCollection<Document> fixesCollectionWithWriteConcern = fixesCollection.withWriteConcern(writeConcern);
                if (clientSession == null) {
                    fixesCollectionWithWriteConcern.insertMany(dbFixes);
                } else {
                    fixesCollectionWithWriteConcern.insertMany(clientSession, dbFixes);
                }
                metadataCollection.enqueueMetadataUpdate(device, dbDeviceId, nrOfTotalFixes, newTimeRange, latestFix);
            } catch (TransformationException e) {
                logger.log(Level.WARNING, "Could not store fix in MongoDB", e);
            }
            Util.addAll(notifyListeners(device, fixes, returnManeuverChanges, returnLiveDelay), racesWithManeuverChangesOrLiveDelay);
        }
        return racesWithManeuverChangesOrLiveDelay;
    }

    @Override
    public <FixT extends Timed> void storeFix(DeviceIdentifier device, FixT fix) {
        storeFixes(device, Collections.singletonList(fix), /* returnManeuverUpdate */ false, /* returnLiveDelay */ false);
    }

    private <FixT extends Timed> Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> notifyListeners(DeviceIdentifier device,
            Iterable<FixT> fixes, boolean returnManeuverChanges, boolean returnLiveDelay) {
        final Set<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> raceWithChangedManeuver = new HashSet<>();
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Map<DeviceIdentifier, Set<FixReceivedListener<FixT>>> listenersWithFixType = (Map) listeners;
        final Set<FixReceivedListener<FixT>> listenersToInform = LockUtil.executeWithReadLockAndResult(listenersLock, () -> {
            return new HashSet<>(Util.<DeviceIdentifier, Set<FixReceivedListener<FixT>>> get(
                    listenersWithFixType, device, Collections.emptySet()));
        });
        for (FixT fix : fixes) {
            for (FixReceivedListener<FixT> listener : listenersToInform) {
                final Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> racesWithManeuverChangeFromListener = listener.fixReceived(device, fix, returnManeuverChanges, returnLiveDelay);
                Util.addAll(racesWithManeuverChangeFromListener, raceWithChangedManeuver);
            }
        }
        return raceWithChangedManeuver;
    }

    @Override
    public void addListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device) {
        LockUtil.executeWithWriteLock(listenersLock, () -> Util.addToValueSet(listeners, device, listener));
    }

    @Override
    public void removeListener(FixReceivedListener<? extends Timed> listener) {
        LockUtil.executeWithWriteLock(listenersLock, () -> Util.removeFromAllValueSets(listeners, listener));
    }

    @Override
    public void removeListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device) {
        LockUtil.executeWithWriteLock(listenersLock, () -> Util.removeFromValueSet(listeners, device, listener));
    }

    @Override
    public TimeRange getTimeRangeCoveredByFixes(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        return getMetadataCollection().getTimeRangeCoveredByFixes(device);
    }

    private MetadataCollection getMetadataCollection() {
        return metadataCollection;
    }

    @Override
    public long getNumberOfFixes(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        return metadataCollection.getNumberOfFixes(device);
    }

    @Override
    public <FixT extends Timed> Map<DeviceIdentifier, FixT> getFixLastReceived(Iterable<DeviceIdentifier> forDevices)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        return metadataCollection.getFixLastReceived(forDevices);
    }
}
