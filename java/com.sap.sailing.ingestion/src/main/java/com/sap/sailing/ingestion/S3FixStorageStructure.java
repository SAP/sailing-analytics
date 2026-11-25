package com.sap.sailing.ingestion;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.impl.TimeRangeImpl;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

/**
 * This class defines the organization structure of fixes on the S3 storage and is the foundation for all the AWS Lambda
 * operations. The basic structure distinguishes single fixes which are inserted by the {@link FixIngestionLambda} and
 * collection items which are created by the {@link FixCombinationLambda} by combining single fixes which are all in the
 * same defined time range. It also facilitates the creation of list requests to obtain fixes of a requested time range.
 * <br>
 * <br>
 * On S3, "files" are represented by keys and a defined delimiter can be used to imitate a folder structure to organize
 * "files" hierarchically. The structure of all the keys on the S3 is the following:
 * <ol>
 * <li>Prefix (single fix or collection item)</li>
 * <li>Device identifier of tracker</li>
 * <li>Time of fix or collection</li>
 * </ol>
 * 
 * @author Kevin Wiesner
 *
 */
public class S3FixStorageStructure {
    private static final String SINGLE_FIX_PREFIX = "ingestion";
    private static final String COLLECTION_PREFIX = "collection";
    private static final String S3_DELIMITER = "/";
    private static final Duration COLLECTION_DURATION = Duration.ONE_MINUTE.times(10l);
    private static final int SINGLE_FIX_BATCH_SIZE = 10;

    /**
     * Returns the basic prefix for a single fix.
     * 
     * @return singleFixPrefix
     */
    public String getSingleFixPrefix() {
        return combineElementsToPrefix(SINGLE_FIX_PREFIX);
    }

    /**
     * Generates a key for a single fix based on a given {@link DeviceIdentifier} and {@link TimePoint}.
     * 
     * @param deviceIdentifier
     * @param timePointOfSingleFix
     * @return generatedKey
     */
    public String generateKeyForSingleFix(final DeviceIdentifier deviceIdentifier,
            final TimePoint timePointOfSingleFix) {
        return generateS3KeyForFile(SINGLE_FIX_PREFIX, deviceIdentifier, timePointOfSingleFix);
    }

    /**
     * Returns a boolean to determine whether a given {@link GPSFixMoving} object lies within the current collection time frame.
     * This is useful to avoid to constantly add fixes to a single collection but to wait for a {@link TimeRange} of a
     * collection to end and add all single fixes in one operation to a collection.
     * 
     * @param fixToCheck
     * @return isInCurrentCollectionFrame
     */
    public Boolean isFixInCurrentCollectionFrame(final GPSFixMoving fixToCheck) {
        final TimePoint fixTimePoint = fixToCheck.getTimePoint();
        final TimeRange currentTimeRange = assignTimePointToCollectionTimeUnit(TimePoint.now());
        return currentTimeRange.includes(fixTimePoint);
    }

    /**
     * Creates a {@link S3FixStorageListRequest} object to facilitate the access to remaining single fixes in a defined
     * {@link TimeRange}. By using the instance variables of the returning object, it is possible to create a
     * {@link ListObjectsV2Request} to get the requested single fixes. The response to the created request would be structured
     * in batches limited by the maxKeys property of the request, it is the task of the code using the request to end
     * the process of accessing new batches when the desired {@link TimeRange} is covered by the single fixes received.
     * 
     * @param deviceIdentifier
     * @param timeRange
     * @return listObjectRequestFacilitator
     */
    public S3FixStorageListRequest getSingleFixesRequestForTimeRange(final DeviceIdentifier deviceIdentifier,
            final TimeRange timeRange) {
        final String keyStartAfter = generateKeyForSingleFix(deviceIdentifier, timeRange.from().minusResolution());
        final String keyToEnd = generateKeyForSingleFix(deviceIdentifier, timeRange.to());
        final String commonPrefix = getCommonPrefix(keyStartAfter, keyToEnd);
        return new S3FixStorageListRequest(commonPrefix, keyStartAfter, SINGLE_FIX_BATCH_SIZE);
    }

    /**
     * Returns the basic prefix for a collection item.
     * 
     * @return collectionPrefix
     */
    public String getCollectionPrefix() {
        return combineElementsToPrefix(COLLECTION_PREFIX);
    }

    /**
     * Generates a key for a collection based on a given {@link DeviceIdentifier} and {@link TimeRange}.
     * 
     * @param deviceIdentifier
     * @param timeRangeOfFixCollection
     * @return generatedKey
     */
    public String generateKeyForCollection(final DeviceIdentifier deviceIdentifier,
            final TimeRange timeRangeOfFixCollection) {
        return generateS3KeyForFile(COLLECTION_PREFIX, deviceIdentifier, timeRangeOfFixCollection.from());
    }

    /**
     * Maps a given fix to a collection {@link TimeRange}. Based on the defined COLLECTION_DURATION, it defines all {@link TimeRange}
     * results beginning at timestamp(0).
     * 
     * @param fixToAssign
     * @return timeRangeForFixCollection
     */
    public TimeRange assignFixToCollectionTimeUnit(final GPSFixMoving fixToAssign) {
        return assignTimePointToCollectionTimeUnit(fixToAssign.getTimePoint());
    }

    /**
     * Creates a {@link S3FixStorageListRequest} object to facilitate the access to the collection items in a defined {@link TimeRange}.
     * By using the instance variables of the returning object, it is possible to create a {@link ListObjectsV2Request} to get
     * the requested collection items. The response to the created request would be structured in one batch which
     * contains all the relevant collection items. Is has to be considered that due to the fact that there could be a
     * collection {@link TimeRange} without a collection item, it is possible that the batch would contain collection items
     * which are out of the scope of the desired {@link TimeRange}.
     * 
     * @param deviceIdentifier
     * @param timeRange
     * @return listObjectRequestFacilitator
     */
    public S3FixStorageListRequest getCollectionRequestForTimeRange(final DeviceIdentifier deviceIdentifier,
            final TimeRange timeRange) {
        final TimeRange collectionAdjustedTimeRangeStart = assignTimePointToCollectionTimeUnit(
                timeRange.from().minus(COLLECTION_DURATION));
        final TimeRange collectionAdjustedTimeRangeEnd = assignTimePointToCollectionTimeUnit(timeRange.to());
        final String keyStartAfter = generateKeyForCollection(deviceIdentifier, collectionAdjustedTimeRangeStart);
        final String keyToEnd = generateKeyForCollection(deviceIdentifier, collectionAdjustedTimeRangeEnd);
        final String commonPrefix = getCommonPrefix(keyStartAfter, keyToEnd);
        final Duration requestDuration = collectionAdjustedTimeRangeStart.to()
                .until(collectionAdjustedTimeRangeEnd.to());
        final int maxKeys = (int) requestDuration.divide(COLLECTION_DURATION);
        return new S3FixStorageListRequest(commonPrefix, keyStartAfter, maxKeys);
    }

    /**
     * Extracts a {@link DeviceIdentifier} from a given key. The key has to follow the described structure of fixes on the
     * storage. Currently, the {@link DeviceIdentifier} can only be a {@link UUID}.
     * 
     * @param key
     * @return deviceIdentifier
     */
    public DeviceIdentifier getIdentifierFromKey(final String key) {
        // TODO handle different device identifiers - improve handling
        String deviceIdentifierString = "";
        String[] keySplitAfterPrefix;
        if (key.contains(COLLECTION_PREFIX)) {
            keySplitAfterPrefix = key.split(COLLECTION_PREFIX);
        } else {
            keySplitAfterPrefix = key.split(SINGLE_FIX_PREFIX);
        }
        deviceIdentifierString = String.join("", keySplitAfterPrefix).split(S3_DELIMITER)[1];
        final UUID deviceUuid = UUID.fromString(deviceIdentifierString);
        final DeviceIdentifier deviceIdentifier = new SmartphoneUUIDIdentifierImpl(deviceUuid);
        return deviceIdentifier;
    }

    /**
     * Helper function to obtain the common prefix of two given keys.
     * 
     * @param key1
     * @param key2
     * @return commonPrefix
     */
    private String getCommonPrefix(final String key1, final String key2) {
        final StringBuilder prefix = new StringBuilder();
        int commonIndex = 0;
        while (commonIndex != key1.length() && commonIndex != key2.length()
                && key1.charAt(commonIndex) == key2.charAt(commonIndex)) {
            prefix.append(key1.charAt(commonIndex));
            commonIndex += 1;
        }
        return prefix.toString();
    }

    /**
     * Uses a given prefix, {@link DeviceIdentifier} and a {@link TimePoint} to create a key based on the determined storage structure.
     * 
     * @param prefix
     * @param deviceIdentifier
     * @param timePoint
     * @return generatedKey
     */
    private String generateS3KeyForFile(final String prefix, final DeviceIdentifier deviceIdentifier,
            final TimePoint timePoint) {
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(timePoint.asMillis());
        final String collectionName = String.format("%02d:%02d:%02d.%03d-UTC.json", calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND));
        final String year = String.format("%04d", calendar.get(Calendar.YEAR));
        final String month = String.format("%02d", calendar.get(Calendar.MONTH)+1);
        final String dayOfMonth = String.format("%02d", calendar.get(Calendar.DATE));
        final String generatedKey = combineElementsToKey(prefix, deviceIdentifier.getStringRepresentation(), year,
                month, dayOfMonth, collectionName);
        return generatedKey;
    }

    /**
     * Helper function to map a given {@link TimePoint} to a structured {@link TimeRange}.
     * 
     * @param timePointToAssign
     * @return mappedTimeRange
     */
    private TimeRange assignTimePointToCollectionTimeUnit(final TimePoint timePointToAssign) {
        final TimePoint rangeStart = timePointToAssign
                .minus(timePointToAssign.asMillis() % COLLECTION_DURATION.asMillis());
        final TimePoint rangeEnd = rangeStart.plus(COLLECTION_DURATION.minus(1));
        return new TimeRangeImpl(rangeStart, rangeEnd);
    }

    /**
     * Helper function to combine elements of a path to a key using the defined class delimiter.
     * 
     * @param pathElements
     * @return key
     */
    private String combineElementsToKey(final String... pathElements) {
        return String.join(S3_DELIMITER, pathElements);
    }

    /**
     * Helper function to combine elements of a path to a prefix using the defined class delimiter.
     * 
     * @param pathElements
     * @return prefix
     */
    private String combineElementsToPrefix(final String... pathElements) {
        return combineElementsToKey(pathElements) + S3_DELIMITER;
    }
}
