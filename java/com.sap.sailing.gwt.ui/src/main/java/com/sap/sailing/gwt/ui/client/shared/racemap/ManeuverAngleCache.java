package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.BearingWithConfidenceDTO;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.KnotSpeedImpl;

/**
 * Cache for maneuver angles for different boat classes, maneuver types and wind speeds.
 *
 * @author Tim Hessenmüller (D062243)
 */
public class ManeuverAngleCache {
    private static final Long TTL_MILLIS = 600_000L;
    private static final Integer WIND_BUCKET_RESOLUTION = 1;
    private static final Integer WIND_BUCKET_INITIAL_CAP = 8;

    private final class Key {
        public final BoatClassDTO boatClass;
        public final ManeuverType maneuverType;

        public Key(BoatClassDTO boatClass, ManeuverType maneuverType) {
            this.boatClass = boatClass;
            this.maneuverType = maneuverType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((boatClass == null) ? 0 : boatClass.hashCode());
            result = prime * result + ((maneuverType == null) ? 0 : maneuverType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Key))
                return false;
            Key other = (Key) obj;
            if (boatClass == null) {
                if (other.boatClass != null)
                    return false;
            } else if (!boatClass.equals(other.boatClass))
                return false;
            if (maneuverType != other.maneuverType)
                return false;
            return true;
        }
    }

    private final SailingServiceAsync sailingService;
    private final HashMap<Key, ArrayList<Triple<Long, Bearing, Double>>> cache = new HashMap<>();
    /**
     * Set of request that have been sent and are still being processed.
     */
    private final HashSet<Triple<BoatClassDTO, ManeuverType, Integer>> requestSet = new HashSet<>();
    /**
     * Maneuver angle to return when no data is available or {@link #overrideAngle} is set.
     */
    private Bearing defaultAngle;
    private boolean overrideAngle;

    public ManeuverAngleCache(SailingServiceAsync sailingService, Bearing defaultAngle) {
        this(sailingService, defaultAngle, false);
    }

    public ManeuverAngleCache(SailingServiceAsync sailingService, Bearing defaultAngle, boolean overrideAngle) {
        this.sailingService = sailingService;
        this.defaultAngle = defaultAngle;
        this.overrideAngle = overrideAngle;
    }

    /**
     * Sets the default maneuver angle to use when there is not enough data available or when {@link #isOverrideAngle()}
     * is {@code true}.
     * @see #getDefaultAngle()
     */
    public void setDefaultAngle(Bearing defaultAngle) {
        this.defaultAngle = defaultAngle;
    }

    /**
     * @see #setDefaultAngle(Bearing)
     */
    public Bearing getDefaultAngle() {
        return defaultAngle;
    }

    /**
     * When set to {@code true} {@link #getManeuverAngle(BoatClassDTO, ManeuverType, Speed)} will always return the
     * value of {@link #getDefaultAngle()} and no requests against the server will be made.
     * @see #isOverrideAngle()
     */
    public void setOverrideAngle(boolean override) {
        this.overrideAngle = override;
    }

    /**
     * @see #setOverrideAngle(boolean)
     */
    public boolean isOverrideAngle() {
        return overrideAngle;
    }

    /**
     * <p>Fetches the maneuver angle for a given boat class, maneuver type and wind speed.</p>
     * <p>The wind speed key is split over buckets of size {@link #WIND_BUCKET_RESOLUTION}. If no data for the requested
     * bucket is available a value from the closest adjacent bucket will be returned and a request to the server will be
     * fired off.</p>
     * <p> If any of the parameters are {@code null} or there is no data available {@link #getDefaultAngle()} will be
     * returned.<br/>
     * If {@link #isOverrideAngle()} is {@code true} {@link #getDefaultAngle()} will always be returned.</p>
     */
    public Bearing getManeuverAngle(BoatClassDTO boatClass, ManeuverType maneuverType, Speed windSpeed) {
        Triple<Long, Bearing, Double> entry = null;
        if (!overrideAngle) {
            final Key key = new Key(boatClass, maneuverType);
            final List<Triple<Long, Bearing, Double>> windBuckets = cache.get(key);
            if (windBuckets != null) {
                final int index = bucketIndex(windSpeed);
                if (index < windBuckets.size()) {
                    entry = windBuckets.get(index);
                }
                if (entry == null || entry.getA() < System.currentTimeMillis()) {
                    // We have no data or it has expired
                    callGetManeuverAngle(boatClass, maneuverType, index);
                }
                if (entry == null) {
                    // Try to find the closest cached value to return now while we wait for the server
                    entry = getClosestEntry(windBuckets, index);
                }
            } else {
                callGetManeuverAngle(boatClass, maneuverType, bucketIndex(windSpeed));
            }
        }
        Bearing result = defaultAngle;
        if (entry != null) {
            result = entry.getB();
        }
        return result;
    }

    /**
     * Attempts to find the closest value (by speed difference) to a given wind speed.
     * @param windBuckets {@link List} to search in
     * @param windSpeed {@link Speed} to start the search from
     * @return the closest entry or {@code null} if no other value exists
     */
    private Triple<Long, Bearing, Double> getClosestEntry(List<Triple<Long, Bearing, Double>> windBuckets,
            int bucketIndex) {
        final int size = windBuckets.size();
        Triple<Long, Bearing, Double> result = null;
        int downIndex = bucketIndex - 1;
        int upIndex = bucketIndex + 1;
        while (downIndex >= 0 || upIndex < size) {
            if (downIndex >= 0 && downIndex < size) {
                result = windBuckets.get(downIndex);
                if (result != null) {
                    break;
                }
            }
            downIndex--;
            if (upIndex < size) {
                result = windBuckets.get(upIndex);
                if (result != null) {
                    break;
                }
            }
            upIndex++;
        }
        return result;
    }

    /**
     * Performs the request to the server if there is no identical request already on the way.
     * @param boatClass {@link BoatClassDTO}
     * @param maneuverType {@link ManeuverType}
     * @param windSpeedBucket {@code int} index of bucket
     */
    private void callGetManeuverAngle(BoatClassDTO boatClass, ManeuverType maneuverType, int windSpeedBucket) {
        final Triple<BoatClassDTO, ManeuverType, Integer> requestKey = new Triple<>(boatClass, maneuverType, windSpeedBucket);
        if (!requestSet.contains(requestKey)) {
            final Speed windSpeed = bucketAvgSpeed(windSpeedBucket);
            GWT.log("callGetManeuverAngle: " + boatClass + ", " + maneuverType + ", " + windSpeed);
            sailingService.getManeuverAngle(boatClass, maneuverType, windSpeed,
                    new AsyncCallback<BearingWithConfidenceDTO>() {
                        @Override
                        public void onSuccess(BearingWithConfidenceDTO result) {
                            if (result != null) {
                                final int index = windSpeedBucket;
                                final Key key = new Key(boatClass, maneuverType);
                                final ArrayList<Triple<Long, Bearing, Double>> windBuckets = cache.computeIfAbsent(key,
                                        k -> new ArrayList<>(Math.max(WIND_BUCKET_INITIAL_CAP, index + 1)));
                                final long expiry = System.currentTimeMillis() + TTL_MILLIS; // TODO Adjust TTL by
                                                                                             // confidence
                                if (index >= windBuckets.size()) {
                                    windBuckets.ensureCapacity(index + 1);
                                    windBuckets.addAll(windBuckets.size(), Collections.nCopies(index - windBuckets.size() + 1, null));
                                }
                                windBuckets.set(index, new Triple<>(expiry, result.getBearing(), result.getConfidence()));
                            }
                            requestSet.remove(requestKey);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            //TODO Throttle requests?
                            GWT.log("[ERROR] callGetManeuverAngle:", caught);
                            requestSet.remove(requestKey);
                        }
                    });
            requestSet.add(requestKey);
        }
    }

    /**
     * Calculates the bucket index from a wind speed.
     * @param windSpeed {@link Speed} wind speed
     * @return {@code int} bucket index
     */
    private static int bucketIndex(Speed windSpeed) {
        return ((int) Math.floor(windSpeed.getKnots())) / WIND_BUCKET_RESOLUTION;
    }

    /**
     * Calculates the wind speed for the middle of a bucket.
     * @param bucketIndex {@code int} bucket index
     * @return {@link Speed} middle wind speed
     */
    private static Speed bucketAvgSpeed(int bucketIndex) {
        double speed = bucketIndex * WIND_BUCKET_RESOLUTION + WIND_BUCKET_RESOLUTION / 2.0;
        return new KnotSpeedImpl(speed);
    }

    @Override
    public String toString() {
        return "ManeuverAngleCache [defaultAngle=" + defaultAngle + ", overrideAngle="
                + overrideAngle + "]";
    }
}
