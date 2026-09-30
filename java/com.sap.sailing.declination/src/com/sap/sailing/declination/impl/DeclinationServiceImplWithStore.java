package com.sap.sailing.declination.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.quadtree.QuadTree;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class DeclinationServiceImplWithStore implements DeclinationService {
    private final Distance defaultMaxDistance;
    private final DeclinationStore persistentStore;
    private final DeclinationImporter declinationImporter;
    
    /**
     * Keys are years
     */
    private final Map<Integer, QuadTree<Declination>> yearStore;
    
    /**
     * Keys are years
     */
    private final Map<Integer, QuadTree<Declination>> importerCache;
    
    public DeclinationServiceImplWithStore(Distance defaultMaxDistance, DeclinationImporter declinationImporter) {
        this.declinationImporter = declinationImporter;
        this.defaultMaxDistance = defaultMaxDistance;
        this.yearStore = new HashMap<Integer, QuadTree<Declination>>();
        this.persistentStore = new DeclinationStore(declinationImporter);
        this.importerCache = new HashMap<Integer, QuadTree<Declination>>();
    }

    @Override
    public Declination getDeclination(TimePoint timePoint, Position position,
            long timeoutForOnlineFetchInMilliseconds) throws IOException, ParseException {
        return getDeclination(timePoint, position, defaultMaxDistance, timeoutForOnlineFetchInMilliseconds);
    }

    @Override
    public Declination getDeclination(TimePoint timePoint, Position position, Distance maxDistance,
            long timeoutForOnlineFetchInMilliseconds) throws IOException, ParseException {
        Calendar cal = new GregorianCalendar();
        cal.setTime(timePoint.asDate());
        Declination result = null;
        double minDistance = Double.MAX_VALUE;
        int step = -1;
        int year = cal.get(Calendar.YEAR);
        QuadTree<Declination> set;
        while ((set = getYearStore(year)) != null) {
            Declination resultForYear;
            synchronized (set) {
                resultForYear = set.get(position);
            }
            Distance spatialDistance = resultForYear.getPosition().getDistance(position);
            // consider result only if it's closer than maxDistance
            if (spatialDistance.compareTo(maxDistance) <= 0) {
                double distance = timeAndSpaceDistance(spatialDistance, resultForYear.getTimePoint(), timePoint);
                if (distance < minDistance) {
                    result = resultForYear;
                    minDistance = distance;
                }
            }
            year += step;
            step = -step - (int) Math.signum(step); // alternate around the original year until no more stored declinations are found
        }
        if (result == null) {
            QuadTree<Declination> importerCacheForYear = importerCache.get(year);
            if (importerCacheForYear != null) {
                synchronized (importerCacheForYear) {
                    result = importerCacheForYear.get(position);
                }
                if (result.getPosition().getDistance(position).compareTo(maxDistance) <= 0) {
                    return result;
                    // else it's further away from the requested position as demanded by maxDistance
                }
            }
            result = declinationImporter.getDeclination(position, timePoint, timeoutForOnlineFetchInMilliseconds);
            if (result != null) {
                if (importerCacheForYear == null) {
                    importerCacheForYear = new QuadTree<Declination>();
                    importerCache.put(year, importerCacheForYear);
                }
                synchronized (importerCacheForYear) {
                    importerCacheForYear.put(result.getPosition(), result);
                }
            }
        }
        return result;
    }

    private QuadTree<Declination> getYearStore(int year) throws IOException, ParseException {
        QuadTree<Declination> result = yearStore.get(year);
        if (result == null) {
            synchronized (this) {
                // make sure we only trigger one invocation of persistentStore.getStoredDeclinations(year) even for multiple threads
                result = yearStore.get(year);
                if (result == null) {
                    result = persistentStore.getStoredDeclinations(year);
                    if (result != null) {
                        yearStore.put(year, result);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Computes a measure for a "distance" based on time and space, between two positions and time points records. Being
     * six months off is deemed to be as bad as being sixty nautical miles off.
     */
    static double timeAndSpaceDistance(Distance spatialDistance, TimePoint t1, TimePoint t2) {
        double nauticalMileDistance = spatialDistance.getNauticalMiles();
        long millisDistance = Math.abs(t1.asMillis()-t2.asMillis());
        return ((double) millisDistance)/1000. /*s*/ / 3600. /*h*/ / 24. /*days*/ / 186. /*six months*/ +
                nauticalMileDistance/60.;
    }

}
