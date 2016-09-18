package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import static com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl.storeDeviceId;
import static com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl.storeTimeRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.FieldNames;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.DeviceIdentifierMongoHandler;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sailing.domain.persistence.racelog.tracking.MongoSensorFixStore;
import com.sap.sailing.domain.racelog.tracking.FixReceivedListener;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
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
public class MongoSensorFixStoreImpl implements MongoSensorFixStore {
    private static final Logger logger = Logger.getLogger(MongoSensorFixStoreImpl.class.getName());
    private final TypeBasedServiceFinder<FixMongoHandler<?>> fixServiceFinder;
    private final TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceServiceFinder;
    private final DBCollection fixesCollection;
    private final DBCollection metadataCollection;
    private final MongoObjectFactoryImpl mongoOF;
    /**
     * Lock object to be used when accessing {@link #listeners}.
     */
    private final NamedReentrantReadWriteLock listenersLock = new NamedReentrantReadWriteLock("Listeners collection lock of " + MongoSensorFixStoreImpl.class.getName(), false);
    private final Map<DeviceIdentifier, Set<FixReceivedListener<? extends Timed>>> listeners = new HashMap<>();

    public MongoSensorFixStoreImpl(MongoObjectFactory mongoObjectFactory, DomainObjectFactory domainObjectFactory,
            TypeBasedServiceFinderFactory serviceFinderFactory) {
        mongoOF = (MongoObjectFactoryImpl) mongoObjectFactory;
        if (serviceFinderFactory != null) {
            fixServiceFinder = createFixServiceFinder(serviceFinderFactory);
            deviceServiceFinder = serviceFinderFactory.createServiceFinder(DeviceIdentifierMongoHandler.class);
        } else {
            fixServiceFinder = null;
            deviceServiceFinder = null;
        }
        fixesCollection = mongoOF.getGPSFixCollection();
        metadataCollection = mongoOF.getGPSFixMetadataCollection();

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private TypeBasedServiceFinder<FixMongoHandler<?>> createFixServiceFinder(
            TypeBasedServiceFinderFactory serviceFinderFactory) {
        return (TypeBasedServiceFinder) serviceFinderFactory.createServiceFinder(FixMongoHandler.class);
    }

    private <T extends Timed> T loadFix(DBObject object)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        String type = (String) object.get(FieldNames.GPSFIX_TYPE.name());
        DBObject fixObject = (DBObject) object.get(FieldNames.GPSFIX.name());
        return this.<T> findService(type).transformBack(fixObject);
    }

    @Override
    public <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier device, TimePoint from,
            TimePoint to, boolean inclusive) throws NoCorrespondingServiceRegisteredException, TransformationException {
        final TimePoint loadFixesFrom = from == null ? TimePoint.BeginningOfTime : from;
        final TimePoint loadFixesTo = to == null ? TimePoint.EndOfTime : to;
        
        Object dbDeviceId = storeDeviceId(deviceServiceFinder, device);
        final QueryBuilder queryBuilder = QueryBuilder.start(FieldNames.DEVICE_ID.name()).is(dbDeviceId)
                .and(FieldNames.TIME_AS_MILLIS.name());
        if (inclusive) {
            queryBuilder.greaterThanEquals(loadFixesFrom.asMillis()).and(FieldNames.TIME_AS_MILLIS.name())
                    .lessThanEquals(loadFixesTo.asMillis());
        } else {
            queryBuilder.greaterThan(loadFixesFrom.asMillis()).and(FieldNames.TIME_AS_MILLIS.name()).lessThan(loadFixesTo.asMillis());
        }
        DBObject query = queryBuilder.get();
        DBCursor result = fixesCollection.find(query);
        for (DBObject fixObject : result) {
            try {
                @SuppressWarnings("unchecked")
                FixT fix = (FixT) loadFix(fixObject);
                consumer.accept(fix);
            } catch (TransformationException e) {
                logger.log(Level.WARNING, "Could not read fix from MongoDB: " + fixObject);
            } catch (ClassCastException e) {
                String type = (String) fixObject.get(FieldNames.GPSFIX_TYPE.name());
                logger.log(Level.WARNING,
                        "Unexpected fix type (" + type + ") encountered when trying to load track for " + device);
            }
        }
    }

    /**
     * Store fixes in batches, reducing metadata storage update.
     */
    @Override
    public <FixT extends Timed> void storeFixes(DeviceIdentifier device, Iterable<FixT> fixes) {
        if (Util.isEmpty(fixes)) {
            return;
        }
        try {
            final Object dbDeviceId = storeDeviceId(deviceServiceFinder, device);
            final int nrOfTotalFixes = Util.size(fixes);
            final ArrayList<DBObject> dbFixes = new ArrayList<>(nrOfTotalFixes);

            TimePoint newFrom = null;
            TimePoint newTo = null;
            for (FixT fix : fixes) {
                String type = fix.getClass().getName();
                FixMongoHandler<FixT> mongoHandler = findService(type);
                Object fixObject = mongoHandler.transformForth(fix);
                DBObject entry = new BasicDBObjectBuilder().add(FieldNames.DEVICE_ID.name(), dbDeviceId)
                        .add(FieldNames.GPSFIX_TYPE.name(), type).add(FieldNames.GPSFIX.name(), fixObject).get();
                mongoOF.storeTimed(fix, entry);
                dbFixes.add(entry);
                TimePoint fixTP = fix.getTimePoint();
                if (newFrom == null || newFrom.after(fixTP)) {
                    newFrom = fixTP;
                }
                if (newTo == null || newTo.before(fixTP)) {
                    newTo = fixTP;
                }
            }
            fixesCollection.insert(dbFixes);
            final BasicDBObject updateOperation = new BasicDBObject();
            final BasicDBObject newMetadata = new BasicDBObject();
            newMetadata.put(FieldNames.DEVICE_ID.name(), dbDeviceId);

            TimeRange oldTimeRange = getTimeRangeCoveredByFixes(device);
            if (oldTimeRange != null) {
                newFrom = oldTimeRange.from().before(newFrom) ? oldTimeRange.from() : newFrom;
                newTo = oldTimeRange.to().after(newTo) ? oldTimeRange.to() : newTo;
            }
            final TimeRange newTimeRange = new TimeRangeImpl(newFrom, newTo);

            storeTimeRange(newTimeRange, newMetadata, FieldNames.TIMERANGE);
            updateOperation.append("$set", newMetadata);
            updateOperation.append("$inc", new BasicDBObject(FieldNames.NUM_FIXES.name(), nrOfTotalFixes));
            metadataCollection.update(getDeviceQuery(device), updateOperation, /* create if not existent */ true,
                    /* update multiple */ false);
            

        } catch (TransformationException e) {
            logger.log(Level.WARNING, "Could not store fix in MongoDB");
            e.printStackTrace();
        }
        notifyListeners(device, fixes);
    }

    @Override
    public <FixT extends Timed> void storeFix(DeviceIdentifier device, FixT fix) {
        storeFixes(device, Collections.singletonList(fix));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <FixT extends Timed> void notifyListeners(DeviceIdentifier device, Iterable<FixT> fixes) {
        LockUtil.executeWithReadLock(listenersLock, () -> {
            for (FixT fix : fixes) {
                for (FixReceivedListener<FixT> listener : Util.<DeviceIdentifier, Set<FixReceivedListener<FixT>>> get(
                        (Map) listeners, device, Collections.emptySet())) {
                    listener.fixReceived(device, fix);
                }
            }
        });
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

    private DBObject getDeviceQuery(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        Object dbDeviceId = storeDeviceId(deviceServiceFinder, device);
        DBObject query = QueryBuilder.start(FieldNames.DEVICE_ID.name()).is(dbDeviceId).get();
        return query;
    }

    private DBObject findMetadataObject(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        DBObject query = getDeviceQuery(device);
        DBObject result = metadataCollection.findOne(query);
        return result;
    }

    @Override
    public TimeRange getTimeRangeCoveredByFixes(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        DBObject result = findMetadataObject(device);
        if (result == null) {
            return null;
        }
        return DomainObjectFactoryImpl.loadTimeRange(result, FieldNames.TIMERANGE);
    }

    @Override
    public long getNumberOfFixes(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        DBObject result = findMetadataObject(device);
        if (result == null) {
            return 0;
        }
        return ((Number) result.get(FieldNames.NUM_FIXES.name())).longValue();
    }

    /**
     * Try to find a service implementing specified type of objects interface using the {@link #fixServiceFinder}.
     * <p>
     * To support an additional fix type, an implementation of {@link FixMongoHandler} is required, which specifies how to
     * transform a fix forth to and back from database. This implementation needs to be
     * registered during OSGi bundle activator startup, providing respective type mapping properties.
     * </p>
     * 
     * @param type type object to find service for
     * @return the registered {@link FixMongoHandler} implementation
     * 
     * @see TypeBasedServiceFinder#findService(String)
     */
    @SuppressWarnings("unchecked")
    private <FixT extends Timed> FixMongoHandler<FixT> findService(String type) {
        return (FixMongoHandler<FixT>) fixServiceFinder.findService(type);
    }

    @Override
    public <FixT extends Timed> Map<DeviceIdentifier, FixT> getLastFix(Iterable<DeviceIdentifier> forDevices)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        Map<DeviceIdentifier, FixT> result  = new HashMap<>();
        for (final DeviceIdentifier deviceIdentifier : forDevices) {
            final DBObject deviceQuery = new BasicDBObject(FieldNames.DEVICE_ID.name()+"."+FieldNames.DEVICE_TYPE_SPECIFIC_ID.name(),
                    deviceIdentifier.getStringRepresentation());
            final DBObject orderBy = new BasicDBObject(FieldNames.GPSFIX.name()+"."+FieldNames.TIME_AS_MILLIS.name(), -1);
            DBCursor lastFixForDeviceCursor = fixesCollection.find(deviceQuery).sort(orderBy).limit(1);
            if (lastFixForDeviceCursor.hasNext()) {
                final DBObject lastFixForDeviceDbObject = lastFixForDeviceCursor.next();
                final Timed lastFixForDevice = loadFix(lastFixForDeviceDbObject);
                @SuppressWarnings("unchecked")
                final FixT lastFixForDeviceTyped = (FixT) lastFixForDevice;
                result.put(deviceIdentifier, lastFixForDeviceTyped);
            }
        }
        return result;
    }
}
