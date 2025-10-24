package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.lang.Nullable;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinder;

/**
 * Encapsulates the collection that holds some metadata for each device that has ever sent fixes to this
 * store, such as the last fix received, the time range between oldest and newest fix, as well as the total
 * number of fixes received from that device.<p>
 * 
 * The updates to the collection are handled asynchronously, in a background executor, so as to not impede
 * throughput for the actual fixes. The updates, recorded in the form of {@link MetadataUpdate} objects,
 * can be merged to reduce the effective number of updates required to the collection, e.g., in case fast
 * fix inserts outperform the slower background updates to the metadata collection.<p>
 * 
 * Reads from the collection wait for the last batch of updates to complete so that they return a state that
 * corresponds to the time point when the read was triggered.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class MetadataCollection extends MongoFixHandler {
    private static final Logger logger = Logger.getLogger(MetadataCollection.class.getName());
    private final MongoCollection<Document> metadataCollection;
    private final ConcurrentMap<DeviceIdentifier, MetadataUpdater> metadataUpdaters;
    private final WriteConcern writeConcern;
    private final ReadConcern readConcern;

    /**
     * Allows for causally-consistent access to the store; helpful, e.g., during test cases. May be {@code null}.
     */
    @Nullable
    private final ClientSession clientSession;

    public MetadataCollection(MongoObjectFactoryImpl mongoOF,
            TypeBasedServiceFinder<FixMongoHandler<?>> fixServiceFinder,
            TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceServiceFinder, ReadConcern readConcern,
            WriteConcern writeConcern, ClientSession clientSession) {
        super(fixServiceFinder, deviceServiceFinder);
        this.metadataCollection = mongoOF.getGPSFixMetadataCollection();
        this.metadataUpdaters = new ConcurrentHashMap<>();
        this.readConcern = readConcern;
        this.writeConcern = writeConcern;
        this.clientSession = clientSession;
    }

    /**
     * First {@link #waitForPendingMetadataUpdates(DeviceIdentifier) waits for any pending metadata updates},
     * then loads and returns them.
     */
    private Document findMetadataObject(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        waitForPendingMetadataUpdates(device);
        return findMetadataObjectInternal(device);
    }

    /**
     * Doesn't {@link #waitForPendingMetadataUpdates(DeviceIdentifier) wait for pending updates} but obtains the
     * metadata object directly from the DB
     */
    private Document findMetadataObjectInternal(DeviceIdentifier device) throws TransformationException {
        Bson query = com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.getDeviceQuery(deviceServiceFinder, device);
        final MongoCollection<Document> metadataCollectionWithReadConcern = metadataCollection.withReadConcern(readConcern);
        Document result = (clientSession == null
                ? metadataCollectionWithReadConcern.find(query) 
                : metadataCollectionWithReadConcern.find(clientSession, query)).first();
        return result;
    }

    TimeRange getTimeRangeCoveredByFixes(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        final Document resultDocument = findMetadataObject(device);
        return getTimeRangeCoveredByFixesInternal(resultDocument);
    }

    private TimeRange getTimeRangeCoveredByFixesInternal(DeviceIdentifier device) throws TransformationException {
        return getTimeRangeCoveredByFixesInternal(findMetadataObjectInternal(device));
    }
    
    /**
     * Doesn't {@link #waitForPendingMetadataUpdates(DeviceIdentifier) wait for pending updates} but tells the current
     * status as loaded from the DB
     */
    private TimeRange getTimeRangeCoveredByFixesInternal(final Document resultDocument) {
        final TimeRange result;
        if (resultDocument == null) {
            result = null;
        } else {
            result = DomainObjectFactoryImpl.loadTimeRange(resultDocument, FieldNames.TIMERANGE);
        }
        return result;
    }
    
    protected long getNumberOfFixes(DeviceIdentifier device) throws TransformationException, NoCorrespondingServiceRegisteredException {
        final Document resultDocument = findMetadataObject(device);
        final long result;
        if (resultDocument == null) {
            result = 0;
        } else {
            result = ((Number) resultDocument.get(FieldNames.NUM_FIXES.name())).longValue();
        }
        return result;
    }
    
    <FixT extends Timed> Map<DeviceIdentifier, FixT> getFixLastReceived(Iterable<DeviceIdentifier> forDevices)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        Map<DeviceIdentifier, FixT> result = new HashMap<>();
        for (final DeviceIdentifier deviceIdentifier : forDevices) {
            final Document metadataDoc = findMetadataObject(deviceIdentifier);
            if (metadataDoc != null) {
                final Document lastFixForDeviceDbObject = (Document) metadataDoc.get(FieldNames.LAST_FIX_RECEIVED.name());
                if (lastFixForDeviceDbObject != null) {
                    final Timed lastFixForDevice = loadFix(lastFixForDeviceDbObject);
                    @SuppressWarnings("unchecked")
                    final FixT lastFixForDeviceTyped = (FixT) lastFixForDevice;
                    result.put(deviceIdentifier, lastFixForDeviceTyped);
                }
            }
        }
        return result;
    }

    protected synchronized <FixT extends Timed> void enqueueMetadataUpdate(DeviceIdentifier device, final Object dbDeviceId,
            final int nrOfTotalFixes, TimeRange fixesTimeRange, FixT latestFix) throws TransformationException {
        final MetadataUpdater metadataUpdaterForDevice = metadataUpdaters.computeIfAbsent(device, d->new MetadataUpdater(this, device));
        metadataUpdaterForDevice.enqueueMetadataUpdate(device, dbDeviceId, nrOfTotalFixes, fixesTimeRange, latestFix);
    }
    
    private synchronized void waitForPendingMetadataUpdates(DeviceIdentifier device) {
        final MetadataUpdater metadataUpdaterForDevice = metadataUpdaters.get(device);
        if (metadataUpdaterForDevice != null) {
            metadataUpdaterForDevice.waitForPendingUpdates();
        }
    }

    <FixT extends Timed> void update(MetadataUpdate<FixT> update) throws TransformationException, NoCorrespondingServiceRegisteredException {
        logger.fine(()->"Updating sensor fix store metadata with "+update);
        final TimeRange oldTimeRange = getTimeRangeCoveredByFixesInternal(update.getDevice());
        final TimeRange newTimeRange = oldTimeRange == null ? update.getFixesTimeRange() : update.getFixesTimeRange() == null ? oldTimeRange : oldTimeRange.extend(update.getFixesTimeRange());
        final Document updateOperation = new Document();
        final Document newMetadata = new Document();
        newMetadata.put(FieldNames.DEVICE_ID.name(), update.getDbDeviceId());
        if (update.getLatestFix() != null) {
            newMetadata.put(FieldNames.LAST_FIX_RECEIVED.name(), storeFixToDocument(new Document(), update.getLatestFix()));
        }
        MongoObjectFactoryImpl.storeTimeRange(newTimeRange, newMetadata, FieldNames.TIMERANGE);
        updateOperation.append("$set", newMetadata);
        updateOperation.append("$inc", new Document(FieldNames.NUM_FIXES.name(), update.getNrOfTotalFixes()));
        logger.fine(()->"Updating sensor fix store metadata with update operation "+updateOperation);
        final MongoCollection<Document> metadataCollectionWithWriteConcern = metadataCollection.withWriteConcern(writeConcern);
        final Bson deviceQuery = com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.getDeviceQuery(deviceServiceFinder, update.getDevice());
        final UpdateOptions upsertOption = new UpdateOptions().upsert(true);
        if (clientSession == null) {
            metadataCollectionWithWriteConcern.updateOne(deviceQuery, updateOperation, upsertOption);
        } else {
            metadataCollectionWithWriteConcern.updateOne(clientSession, deviceQuery, updateOperation, upsertOption);
        }
    }
}
