package com.sap.sailing.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixMovingJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixMovingJsonSerializer;
import com.sap.sse.common.TimeRange;
import com.sap.sse.shared.json.JsonDeserializationException;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.utils.IoUtils;

/**
 * This λ is used to combine single fixes (currently only fixes from the type {@link GPSFixMoving}) on the S3 created by
 * the {@link FixIngestionLambda} to collection items which bundle all single fixes for a certain device and a certain
 * {@link TimeRange} obtained by the S3 structure class {@link S3FixStorageStructure}.
 * 
 * @author Kevin Wiesner
 *
 */
public class FixCombinationLambda implements RequestStreamHandler {
    private final S3Client s3Client = S3Client.builder().region(Configuration.S3_REGION).build();
    private final GPSFixMovingJsonDeserializer deserializer = new GPSFixMovingJsonDeserializer();
    private final S3FixStorageStructure s3FixStorageStructure = new S3FixStorageStructure();
    private static final Logger logger = Logger.getLogger(FixCombinationLambda.class.getName());
 
    /**
     * Entry point for the λ on AWS. Defines the operation needed to combine single fixes to collections.
     */
    @Override
    public void handleRequest(final InputStream inputAsStream, final OutputStream outputAsStream, final Context context) {
        try {
            logger.info("FixCombination Lambda is starting");
            final Map<DeviceIdentifier, List<S3Object>> allSingleFixMetadataFromDevices = this.getMetadataOfNewFixes();
            final List<ObjectIdentifier> keysToDelete = Collections.synchronizedList(new ArrayList<ObjectIdentifier>());
            final Map<DeviceIdentifier, TreeSet<GPSFixMoving>> allSingleFixDataFromDevices = this.getDataOfNewFixes(allSingleFixMetadataFromDevices, keysToDelete);
            this.divideFixesAndCreateCollections(allSingleFixDataFromDevices);
            this.deleteFixesUsedForCollections(keysToDelete);
        } catch (S3Exception e) {
            logger.log(Level.SEVERE, e.awsErrorDetails().errorMessage());
        } finally {
            try {
                inputAsStream.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception trying to close input: " + e.getMessage());
            }
            try {
                outputAsStream.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception trying to close output: " + e.getMessage());
            }
        }
    }

    /**
     * Obtains the metadata for every single fix located on the S3.
     * 
     * @return allSingleFixMetadataFromDevices
     */
    private Map<DeviceIdentifier, List<S3Object>> getMetadataOfNewFixes() {
        final ListObjectsV2Request listSingleFixesRequest = ListObjectsV2Request.builder()
                .bucket(Configuration.S3_BUCKET_NAME)
                .prefix(s3FixStorageStructure.getSingleFixPrefix())
                .build();
        final ListObjectsV2Iterable listOfSingleFixes = s3Client.listObjectsV2Paginator(listSingleFixesRequest);
        final Map<DeviceIdentifier, List<S3Object>> allSingleFixMetadataFromDevices = listOfSingleFixes.stream()
                .flatMap(listOfSingleFixesPage -> listOfSingleFixesPage.contents().stream())
                .filter(singleFix -> singleFix.key().contains(".json"))
                .collect(Collectors.groupingBy(singleFix -> s3FixStorageStructure.getIdentifierFromKey(singleFix.key())));
        return allSingleFixMetadataFromDevices;
    }

    /**
     * Obtains the data from every single fix located on the S3 based on the given collection of metadata of single
     * fixes. It returns a {@link Map<DeviceIdentifier, TreeSet<GPSFixMoving>>} which groups the single fixes to a
     * certain {@link DeviceIdentifier} and organizes the fixes in a sorted {@link TreeSet<GPSFixMoving>} based on the
     * timestamp of a single fix.
     * 
     * @param allSingleFixMetadataFromDevices
     * @param keysToDelete
     * @return allSingleFixDataFromDevices
     */
    private Map<DeviceIdentifier, TreeSet<GPSFixMoving>> getDataOfNewFixes(
            final Map<DeviceIdentifier, List<S3Object>> allSingleFixMetadataFromDevices,
            final List<ObjectIdentifier> keysToDelete) {
        final Map<DeviceIdentifier, TreeSet<GPSFixMoving>> allSingleFixDataFromDevices = new HashMap<DeviceIdentifier, TreeSet<GPSFixMoving>>();
        allSingleFixMetadataFromDevices.entrySet().parallelStream().forEach(singleFixMetadataFromDevice -> {
            logger.info("Fetching fixes from: " + singleFixMetadataFromDevice.getKey() 
                    + " (Amount: " + Integer.toString(singleFixMetadataFromDevice.getValue().size()) + ")");
            synchronized (allSingleFixDataFromDevices) {
                allSingleFixDataFromDevices.put(singleFixMetadataFromDevice.getKey(), new TreeSet<>(new TimedComparator()));
            }
            singleFixMetadataFromDevice.getValue().parallelStream().forEach(singleFixS3Object -> {
                try {
                    final Object singleFixObject = loadObjectFromKey(singleFixS3Object.key());
                    final JSONObject singleFixJson = Helpers.toJSONObjectSafe(singleFixObject);
                    final GPSFixMoving singleFix = deserializer.deserialize(singleFixJson);
                    if (!s3FixStorageStructure.isFixInCurrentCollectionFrame(singleFix)) {
                        synchronized (allSingleFixDataFromDevices) {
                            allSingleFixDataFromDevices.get(singleFixMetadataFromDevice.getKey()).add(singleFix);
                        }
                        final ObjectIdentifier fixToDelete = ObjectIdentifier.builder().key(singleFixS3Object.key()).build();
                        keysToDelete.add(fixToDelete);
                    }
                } catch (ParseException | JsonDeserializationException e) {
                    logger.log(Level.SEVERE, "Exception for key " + singleFixS3Object.key() + " with JSON operations: " + e.getMessage());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Exception for key " + singleFixS3Object.key() + " while receiving object from S3: " + e.getMessage());
                }
            });
        });
        logger.info("Finished list of objects in S3");
        return allSingleFixDataFromDevices;
    }

    /**
     * Uses a given map of all single fix data grouped by their device to first group every set for every device into
     * time ranges for the resulting collections. If a collection already exists, it retrieves the single fixes them and
     * sorts the new fixes for the collection into them. If the resulting collection does not yet exist, it creates a
     * new sorted collection for the considered fixes.
     * 
     * @param allSingleFixDataFromDevices
     */
    private void divideFixesAndCreateCollections(Map<DeviceIdentifier, TreeSet<GPSFixMoving>> allSingleFixDataFromDevices) {
        allSingleFixDataFromDevices.entrySet().parallelStream().forEach(singleFixDataFromDevice -> {
            Map<TimeRange, TreeSet<GPSFixMoving>> timeRangeDividedFixDataFromDevice = singleFixDataFromDevice.getValue().stream()
                    .collect(Collectors.groupingBy(s3FixStorageStructure::assignFixToCollectionTimeUnit,
                            Collectors.toCollection(() -> new TreeSet<>(new TimedComparator()))));
            for(Map.Entry<TimeRange, TreeSet<GPSFixMoving>> singleTimeRangeDataOfSingleDevice: timeRangeDividedFixDataFromDevice.entrySet()) {
                final TimeRange timeRangeOfCollection = singleTimeRangeDataOfSingleDevice.getKey();
                final TreeSet<GPSFixMoving> singleFixesMappedToTimeRange = singleTimeRangeDataOfSingleDevice.getValue();
                String logAboutCollectionProcess = "Device Id: "
                        + singleFixDataFromDevice.getKey().getStringRepresentation() + "\tTimestamp: "
                        + timeRangeOfCollection.toString() + "\tAmount of new Fixes: "
                        + Integer.toString(singleFixesMappedToTimeRange.size())
                        + "\tFixes: " + singleFixesMappedToTimeRange.toString();
                TreeSet<GPSFixMoving> fixSetToStoreForTimeRangeAndDevice = null;
                final String keyForCollection = s3FixStorageStructure.generateKeyForCollection(singleFixDataFromDevice.getKey(), timeRangeOfCollection);
                try {
                    final Object existingFixesObject = loadObjectFromKey(keyForCollection);
                    final JSONArray existingFixedJsonArray = Helpers.toJSONArraySafe(existingFixesObject);
                    TreeSet<GPSFixMoving> existingFixes = new TreeSet<>(new TimedComparator());
                    Iterator<Object> existingFixesJsonArrayIterator = existingFixedJsonArray.iterator();
                    while (existingFixesJsonArrayIterator.hasNext()) {
                        final JSONObject fixJsonObject = Helpers.toJSONObjectSafe(existingFixesJsonArrayIterator.next());
                        final GPSFixMoving existingSingleFixData = deserializer.deserialize(fixJsonObject);
                        existingFixes.add(existingSingleFixData);
                    }
                    if (existingFixes.size() > singleFixesMappedToTimeRange.size()) {
                        existingFixes.addAll(singleFixesMappedToTimeRange);
                        fixSetToStoreForTimeRangeAndDevice = existingFixes;
                    } else {
                        singleFixesMappedToTimeRange.addAll(existingFixes);
                    }
                    logAboutCollectionProcess += "\tAdded data to existing collection with key " + keyForCollection;
                } catch (NoSuchKeyException e) {
                    logAboutCollectionProcess += "\tCollection with key " + keyForCollection + " does not exist, creating a new one...";
                } catch (ParseException | JsonDeserializationException e) {
                    logAboutCollectionProcess += "\tDeserializing existing collection for key " + keyForCollection + " failed: " + e.getMessage();
                } catch (IOException e) {
                    logAboutCollectionProcess += "\tFetching existing collection for key " + keyForCollection + " failed: " + e.getMessage();
                }
                final PutObjectRequest saveFixCollectionRequest = PutObjectRequest.builder()
                        .bucket(Configuration.S3_BUCKET_NAME)
                        .key(keyForCollection)
                        .build();
                if (fixSetToStoreForTimeRangeAndDevice == null)
                    fixSetToStoreForTimeRangeAndDevice = singleFixesMappedToTimeRange;
                final JSONArray fixSetToStoreForTimeRangeAndDeviceAsJSONArray = new JSONArray();
                fixSetToStoreForTimeRangeAndDevice.stream().map((fixObject) -> new GPSFixMovingJsonSerializer().serialize(fixObject)).forEach(fixSetToStoreForTimeRangeAndDeviceAsJSONArray::add);
                s3Client.putObject(saveFixCollectionRequest, RequestBody.fromString(fixSetToStoreForTimeRangeAndDeviceAsJSONArray.toJSONString()));
                logger.info(logAboutCollectionProcess);
            }
        });
        logger.info("Finished combination of fixes into collection files");
    }

    /**
     * Deletes all the specified keys (given as a list of {@link ObjectIdentifier}) from the S3.
     * 
     * @param keysToDelete
     */
    private void deleteFixesUsedForCollections(final List<ObjectIdentifier> keysToDelete) {
        logger.info("Keys to delete: " + keysToDelete.toString());
        if (keysToDelete.size() != 0) {
            final Delete deleteFixes = Delete.builder().objects(keysToDelete).build();
            final DeleteObjectsRequest deleteFixesRequest = DeleteObjectsRequest.builder()
                    .bucket(Configuration.S3_BUCKET_NAME)
                    .delete(deleteFixes)
                    .build();
            s3Client.deleteObjects(deleteFixesRequest);
            logger.info("Deleted single fix files used for collection");
        }
    }

    /**
     * Helper function to retrieve data of a given S3 key and parse it to a Object which can then be used for JSON
     * deserialization.
     * 
     * @param requestKey
     * @return fixObject
     * @throws IOException
     * @throws ParseException
     */
    private Object loadObjectFromKey(final String requestKey)
            throws IOException, ParseException {
        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(Configuration.S3_BUCKET_NAME)
                .key(requestKey)
                .build();
        final ResponseInputStream<GetObjectResponse> fixData = s3Client.getObject(getObjectRequest);
        final String strFixData = IoUtils.toUtf8String(fixData);
        fixData.close();
        final Object fixObject = JSONValue.parseWithException(strFixData);
        return fixObject;
    }
}
