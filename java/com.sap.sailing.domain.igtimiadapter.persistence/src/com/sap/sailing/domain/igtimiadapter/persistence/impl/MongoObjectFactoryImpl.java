package com.sap.sailing.domain.igtimiadapter.persistence.impl;

import org.bson.Document;
import org.bson.types.Binary;

import com.igtimi.IgtimiData.DataMsg;
import com.igtimi.IgtimiData.DataPoint;
import com.igtimi.IgtimiDevice.DeviceManagement;
import com.igtimi.IgtimiDevice.DeviceManagementResponse;
import com.igtimi.IgtimiStream.Msg;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.DataPointTimePointExtractor;
import com.sap.sailing.domain.igtimiadapter.DataPointVisitor;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.persistence.MongoObjectFactory;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private final MongoDatabase db;
    private final MongoCollection<Document> messagesCollection;

    public MongoObjectFactoryImpl(MongoDatabase db) {
        this.db = db;
        this.messagesCollection = getOrCreateMessagesCollection(db);
    }
    
    static MongoCollection<Document> getOrCreateMessagesCollection(MongoDatabase database) {
        final MongoCollection<Document> messagesCollection = database.getCollection(CollectionNames.IGTIMI_MESSAGES.name());
        final Document index = new Document()
            .append(FieldNames.IGTIMI_MESSAGES_DEVICE_SERIAL_NUMBER.name(), 1)
            .append(FieldNames.IGTIMI_MESSAGES_TIMESTAMP.name(), 1);
        final IndexOptions indexOptions = new IndexOptions().name("fixbydevandtime").background(false);
        messagesCollection.createIndex(index, indexOptions);
        return messagesCollection;
    }

    @Override
    public void clear(ClientSession clientSessionOrNull) {
        final MongoCollection<Document> devicesCollection = db.getCollection(CollectionNames.IGTIMI_DEVICES.name()).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        final MongoCollection<Document> dataAccessWindowsCollections = db.getCollection(CollectionNames.IGTIMI_DATA_ACCESS_WINDOWS.name()).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        if (clientSessionOrNull == null) {
            devicesCollection.drop();
            dataAccessWindowsCollections.drop();
            messagesCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).drop();
        } else {
            devicesCollection.drop(clientSessionOrNull);
            dataAccessWindowsCollections.drop(clientSessionOrNull);
            messagesCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).drop(clientSessionOrNull);
        }
    }

    
    @Override
    public void storeDevice(Device device, ClientSession clientSessionOrNull) {
        final Document filter = new Document(FieldNames.IGTIMI_DEVICES_ID.name(), device.getId());
        final Document update = new Document();
        update.put(FieldNames.IGTIMI_DEVICES_ID.name(), device.getId());
        update.put(FieldNames.IGTIMI_DEVICES_NAME.name(), device.getName());
        update.put(FieldNames.IGTIMI_DEVICES_SERIAL_NUMBER.name(), device.getSerialNumber());
        update.put(FieldNames.IGTIMI_DEVICES_LAST_HEARTBEAT_MILLIS.name(), device.getLastHeartbeat() == null ? null : device.getLastHeartbeat().getA() == null ? null : device.getLastHeartbeat().getA().asMillis());
        update.put(FieldNames.IGTIMI_DEVICES_REMOTE_ADDRESS.name(), device.getLastHeartbeat() == null ? null : device.getLastHeartbeat().getB());
        final MongoCollection<Document> deviceCollection = db.getCollection(CollectionNames.IGTIMI_DEVICES.name()).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        if (clientSessionOrNull == null) {
            deviceCollection.replaceOne(filter, update, new ReplaceOptions().upsert(true));
        } else {
            deviceCollection.replaceOne(clientSessionOrNull, filter, update, new ReplaceOptions().upsert(true));
        }
    }

    @Override
    public void removeDevice(long deviceId, ClientSession clientSessionOrNull) {
        final Document filter = new Document(FieldNames.IGTIMI_DEVICES_ID.name(), deviceId);
        final MongoCollection<Document> deviceCollection = db.getCollection(CollectionNames.IGTIMI_DEVICES.name()).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        if (clientSessionOrNull == null) {
            deviceCollection.deleteOne(filter);
        } else {
            deviceCollection.deleteOne(clientSessionOrNull, filter);
        }
    }
    
    @Override
    public void storeDataAccessWindow(DataAccessWindow daw, ClientSession clientSessionOrNull) {
        final Document filter = new Document(FieldNames.IGTIMI_DATA_ACCESS_WINDOWS_ID.name(), daw.getId());
        final Document update = new Document();
        update.put(FieldNames.IGTIMI_DATA_ACCESS_WINDOWS_ID.name(), daw.getId());
        update.put(FieldNames.IGTIMI_DATA_ACCESS_WINDOWS_DEVICE_SERIAL_NUMBER.name(), daw.getDeviceSerialNumber());
        update.put(FieldNames.IGTIMI_DATA_ACCESS_WINDOWS_START_TIME_MILLIS.name(), daw.getStartTime() == null ? null : daw.getStartTime().asMillis());
        update.put(FieldNames.IGTIMI_DATA_ACCESS_WINDOWS_END_TIME_MILLIS.name(), daw.getEndTime() == null ? null : daw.getEndTime().asMillis());
        final MongoCollection<Document> dataAccessWindowsCollection = db.getCollection(CollectionNames.IGTIMI_DATA_ACCESS_WINDOWS.name()).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        if (clientSessionOrNull == null) {
            dataAccessWindowsCollection.replaceOne(filter, update, new ReplaceOptions().upsert(true));
        } else {
            dataAccessWindowsCollection.replaceOne(clientSessionOrNull, filter, update, new ReplaceOptions().upsert(true));
        }
    }

    @Override
    public void removeDataAccessWindow(long dawId, ClientSession clientSessionOrNull) {
        final Document filter = new Document(FieldNames.IGTIMI_DEVICES_ID.name(), dawId);
        final MongoCollection<Document> dataAccessWindowsCollection = db.getCollection(CollectionNames.IGTIMI_DATA_ACCESS_WINDOWS.name()).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        if (clientSessionOrNull == null) {
            dataAccessWindowsCollection.deleteOne(filter);
        } else {
            dataAccessWindowsCollection.deleteOne(clientSessionOrNull, filter);
        }
    }

    @Override
    public void storeMessage(String serialNumber, Msg message, ClientSession clientSessionOrNull) {
        if (Util.hasLength(serialNumber)) {
            final TimePoint timePointFromMessage = extractTimePointFromMessage(message);
            final TimePoint effectiveTimePoint = timePointFromMessage == null ? TimePoint.now() : timePointFromMessage;
            final Document doc = new Document(FieldNames.IGTIMI_MESSAGES_TIMESTAMP.name(), effectiveTimePoint.asDate());
            doc.put(FieldNames.IGTIMI_MESSAGES_DEVICE_SERIAL_NUMBER.name(), serialNumber);
            doc.put(FieldNames.IGTIMI_MESSAGES_PROTOBUF_MESSAGE.name(), new Binary(message.toByteArray()));
            if (clientSessionOrNull == null) {
                messagesCollection.insertOne(doc);
            } else {
                messagesCollection.insertOne(clientSessionOrNull, doc);
            }
        }
    }

    /**
     * Returns the first timestamp found in any of the messages; looking for data messages with data points inside
     * 
     * @return the first timestamp found in any data point message inside a data message, or {@code null} if no such
     *         message can be found nested in {@code message}
     */
    private TimePoint extractTimePointFromMessage(Msg message) {
        if (message.hasData()) {
            for (final DataMsg data : message.getData().getDataList()) {
                for (final DataPoint dataPoint : data.getDataList()) {
                    return DataPointVisitor.accept(dataPoint, new DataPointTimePointExtractor());
                }
            }
        } else if (message.hasDeviceManagement()) {
            final DeviceManagement deviceManagement = message.getDeviceManagement();
            if (deviceManagement.hasResponse()) {
                final DeviceManagementResponse response = deviceManagement.getResponse();
                return TimePoint.of(response.getTimestamp());
            }
        }
        return null;
    }
}
