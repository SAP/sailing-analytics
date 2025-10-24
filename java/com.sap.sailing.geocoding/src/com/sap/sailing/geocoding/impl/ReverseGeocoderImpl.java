package com.sap.sailing.geocoding.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.Placemark;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.PlacemarkImpl;
import com.sap.sailing.domain.common.quadtree.QuadTree;
import com.sap.sailing.geocoding.ReverseGeocoder;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.util.HttpUrlConnectionHelper;

public class ReverseGeocoderImpl implements ReverseGeocoder {
    /**
     * The system property name to get the comma separated list of geonames.org usernames; takes precedence over
     * the environment variable whose name is given by {@link #USERNAMES_ENV_VARIABLE_NAME}.
     */
    private static final String USERNAMES_SYSTEM_PROPERTY_NAME = "geonames.org.usernames";
    
    /**
     * The environment variable name to get the comma separated list of geonames.org usernames. Not evaluated
     * if the system property whose name is given by {@link #USERNAMES_SYSTEM_PROPERTY_NAME} is set.
     */
    private static final String USERNAMES_ENV_VARIABLE_NAME = "GEONAMES_ORG_USERNAMES";
    
    /**
     * Default username in case neither system property nor environment variable is set.
     */
    private static final String GEONAMES_DEFAULT_USER_NAME = "sailtracking0";

    private static final String DATES = "dates";
    private static final String TIMEZONE_ID = "timezoneId";
    private static final String BASE_URL = "http://api.geonames.org";
    private static final String NEARBY_PLACE_SERVICE = BASE_URL+"/findNearbyPlaceNameJSON?";
    private static final String SEARCH_BY_NAME_SERVICE = BASE_URL+"/searchJSON?";
    private static final String SEARCH_TIMEZONE_BY_POSITION = BASE_URL+"/timezoneJSON?";
    /**
     * Maximal distance in degree for the cache.<br />
     * The first number is the distance in kilometers and the second number is a needed calculation factor and mustn't
     * be changed! To change the distance just change the first number!
     */
    private final double POSITION_CACHE_DISTANCE_LIMIT = ReverseGeocoder.POSITION_CACHE_DISTANCE_LIMIT_IN_KM * 0.00899928005759539236861051115911;
    private final int XKM_RADIUS = 5;
    private final int ROWS_PER_XKM_RADIUS = 15;
    
    private final int MAX_ROW_NUMBER = 500;
    private final int MAX_RADIUS = 300;
    private final String[] usernames;

    private QuadTree<Util.Triple<Position, Double, List<Placemark>>> cache = new QuadTree<Util.Triple<Position,Double,List<Placemark>>>();;
    
    private static final Logger logger = Logger.getLogger(ReverseGeocoderImpl.class.getName());

    public ReverseGeocoderImpl() {
        final String commaSeparatedUsernames = System.getProperty(USERNAMES_SYSTEM_PROPERTY_NAME,
                Optional.ofNullable(System.getenv(USERNAMES_ENV_VARIABLE_NAME)).orElse(GEONAMES_DEFAULT_USER_NAME));
        this.usernames = commaSeparatedUsernames.split(",");
    }
    
    @Override
    public TimeZone getTimeZone(Position position, TimePoint timePoint) throws MalformedURLException, IOException, ParseException {
        TimeZone resolvedTimeZone = null;
        final JSONObject timeZoneObject = callTimezoneService(position, timePoint);
        if (timeZoneObject != null) {
            if (timeZoneObject.containsKey(TIMEZONE_ID)) {
                final String timeZoneId = timeZoneObject.get(TIMEZONE_ID).toString();
                resolvedTimeZone = TimeZone.getTimeZone(timeZoneId);
            }
            if (resolvedTimeZone == null && timeZoneObject.containsKey(DATES)) {
                final JSONArray dates = (JSONArray) timeZoneObject.get(DATES);
                if (dates.size() > 1 && ((JSONObject) dates.get(1)).containsKey("offsetToGmt")) {
                    final int offsetToGmtMillis = (int) (Double.parseDouble(((JSONObject) dates.get(1)).get("offsetToGmt").toString()) * 3600 * 1000);
                    resolvedTimeZone = getTimeZoneWithOffsetAtTime(offsetToGmtMillis, timePoint);
                }
            }
        }
        return resolvedTimeZone;
    }
    
    private TimeZone getTimeZoneWithOffsetAtTime(int offsetToGmtMillis, TimePoint timePoint) {
        for (final String tzId : TimeZone.getAvailableIDs()) {
            final TimeZone tz = TimeZone.getTimeZone(tzId);
            if (tz.getOffset(timePoint.asMillis()) == offsetToGmtMillis) {
                return tz;
            }
        }
        return null;
    }

    private JSONObject callTimezoneService(Position position, TimePoint timePoint)
            throws MalformedURLException, IOException, ParseException {
        StringBuilder url = generateRequestUrlTimezoneService(position, timePoint);
        JSONObject geonames = submitGeonamesRequestForJSONObjectResult(url);
        return geonames;
    }

    private StringBuilder generateRequestUrlTimezoneService(Position position, TimePoint timePoint) {
        StringBuilder url = new StringBuilder(SEARCH_TIMEZONE_BY_POSITION);
        url.append("lat=" + Double.toString(position.getLatDeg()));
        url.append("&lng=" + Double.toString(position.getLngDeg()));
        url.append("&radius=10" /*km*/);
        url.append("&date="+new SimpleDateFormat("yyyy-MM-dd").format(timePoint.asDate()));
        return url;
    }

    private JSONObject submitGeonamesRequestForJSONObjectResult(StringBuilder url)
            throws MalformedURLException, IOException, ParseException {
        final URLConnection connection = addUsernameParameterAndConnect(url);
        final BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));
        final JSONParser parser = new JSONParser();
        final JSONObject obj = (JSONObject) parser.parse(in);
        return obj;
    }
    
    @Override
    public Placemark getPlacemarkNearest(Position position) throws IOException, ParseException {
        Placemark p = null;
        Util.Triple<Position, Double, List<Placemark>> cachedPlacemarks = checkCache(position);
        if (cachedPlacemarks != null && cachedPlacemarks.getC() != null && !cachedPlacemarks.getC().isEmpty()) {
            p = cachedPlacemarks.getC().get(0);
        } else {
            JSONArray geonames = callNearestService(position);
            if (geonames != null && !geonames.isEmpty()) {
                p = jsonToPlacemark((JSONObject) geonames.get(0));
                if (p != null) {
                    List<Placemark> placemarks = new ArrayList<Placemark>();
                    placemarks.add(p);
                    cachePlacemarks(position, 0.0, placemarks);
                }
            }
        }
        return p;
    }

    @Override
    public List<Placemark> getPlacemarksNear(Position position, double radius) throws IOException, ParseException {
        final List<Placemark> placemarks;
        final Util.Triple<Position, Double, List<Placemark>> cachedPlacemarks = checkCache(position);
        // Calculating the search radius and the maximum number of returning Placemarks
        double limitedRadius = Math.min(radius, MAX_RADIUS);
        final int radiusInt = (int) limitedRadius;
        final int xKmRadius = radiusInt / XKM_RADIUS;
        final int maxRows = (int) Math.min(ROWS_PER_XKM_RADIUS * Math.pow(2, xKmRadius), MAX_ROW_NUMBER);
        if (cachedPlacemarks != null && cachedPlacemarks.getB() >= limitedRadius) {
            if (cachedPlacemarks.getC().size() > maxRows) {
                placemarks = cachedPlacemarks.getC().subList(0, maxRows);
            } else {
                placemarks = new ArrayList<Placemark>(cachedPlacemarks.getC());
            }
        } else {
            // Recalculating the radius and resetting the search position to keep the cache correct
            Position searchPosition = null;
            if (cachedPlacemarks != null) {
                searchPosition = cachedPlacemarks.getA();
                double distance = position.getDistance(searchPosition).getKilometers();
                limitedRadius = Math.min(MAX_RADIUS, limitedRadius + distance); 
            } else {
                searchPosition = position;
            }
            final JSONArray geonames = callNearbyService(searchPosition, limitedRadius, maxRows);
            if (geonames != null) {
                Iterator<Object> iterator = geonames.iterator();
                if (iterator.hasNext()) {
                    placemarks = new ArrayList<Placemark>();
                    while (iterator.hasNext()) {
                        JSONObject object = (JSONObject) iterator.next();
                        Placemark place = jsonToPlacemark(object);
                        if (place != null) {
                            placemarks.add(jsonToPlacemark(object));
                        }
                    }
                    // If there are no cached placemarks for the requested Position just cache them, otherwise update the cache
                    if (cachedPlacemarks == null) {
                        cachePlacemarks(searchPosition, limitedRadius, placemarks);
                    } else {
                        updateCachedPlacemarks(searchPosition, limitedRadius, placemarks);
                    }
                } else {
                    placemarks = null;
                }
            } else {
                placemarks = null;
            }
        }
        return placemarks;
    }

    @Override
    public Placemark getPlacemarkLast(Position position, double radius, Comparator<Placemark> comp) throws IOException,
            ParseException {
        List<Placemark> placemarks = getPlacemarksNearSorted(position, radius, comp);
        return placemarks == null ? null : placemarks.get(placemarks.size() - 1);
    }

    @Override
    public Placemark getPlacemarkFirst(Position position, double radius, Comparator<Placemark> comp)
            throws IOException, ParseException {
        List<Placemark> placemarks = getPlacemarksNearSorted(position, radius, comp);
        return placemarks == null ? null : placemarks.get(0);
    }
    
    @Override
    public Placemark getPlacemark(String name, Comparator<Placemark> comp) throws IOException, ParseException {
        StringBuilder url = new StringBuilder(SEARCH_BY_NAME_SERVICE);
        url.append("name=" + URLEncoder.encode(name, "UTF-8"));
        final JSONArray geonames = submitGeonamesRequestForJSONArrayResult(url);
        return geonames.stream().map(o->jsonToPlacemark((JSONObject) o)).sorted(comp).findFirst().orElse(null);
    }

    private List<Placemark> getPlacemarksNearSorted(Position position, double radius, Comparator<Placemark> comp)
            throws IOException, ParseException {
        List<Placemark> placemarks = getPlacemarksNear(position, radius);
        if (placemarks != null) {
            Collections.sort(placemarks, comp);
        }
        return placemarks;
    }

    /**
     * Returns a {@link Placemark} for a compatible JSONObject.
     * 
     * @param json
     *            The object to be converted
     * @return A {@link Placemark} or <code>null</code>, if the object doesn't contain a name, a postion or the
     *         if the population is 0
     */
    private Placemark jsonToPlacemark(JSONObject json) {
        String name = (String) json.get("toponymName");
        String countryCode = (String) json.get("countryCode");
        // Tries are necessary, because some latitude or longitude values delivered by Geonames have no decimal places
        // and are interpreted as Long
        // Casting a Long to a Double raises a ClassCastException
        Double latDeg = null;
        Object jsonLat = json.get("lat");
        if (jsonLat instanceof String) {
            latDeg = Double.valueOf((String) jsonLat);
        } else if (jsonLat instanceof Number) {
            latDeg = ((Number) jsonLat).doubleValue();
        }
        Double lngDeg = null;
        Object jsonLng = json.get("lng");
        if (jsonLng instanceof String) {
            lngDeg = Double.valueOf((String) jsonLng);
        } else if (jsonLng instanceof Number) {
            lngDeg = ((Number) jsonLng).doubleValue();
        }
        Position position = new DegreePosition(latDeg, lngDeg);
        long population = (Long) json.get("population");
        if (name != null && lngDeg != null && latDeg != null) {
            return new PlacemarkImpl(name, countryCode, position, population);
        } else {
            return null;
        }
    }

    /**
     * Caches the <code>position</code>, the <code>radius</code> and the <code>placemarks</code> at the Position
     * <code>p</code> in the cache.
     * 
     * @param position
     *            The position in the cache and the point of the search
     * @param radius
     *            The radius of the search
     * @param placemarks
     *            The results of the search
     */
    private void cachePlacemarks(Position position, Double radius, List<Placemark> placemarks) {
        Collections.sort(placemarks, new Placemark.ByDistance(position));
        if (position != null) {
            synchronized (cache) {
                cache.put(position, new Util.Triple<Position, Double, List<Placemark>>(position, radius, placemarks));
            }
        }
    }
    
    /**
     * Replaces the data at <code>cachedPoint</code> with the <code>newRadius</code> and <code>newPlacemarks</code>.
     * 
     * @param cachedPoint
     *            The position of the data which should be replaced. Has to be a Position which was cached before.<br />
     *            The parameter is as best a value from a Triple in the cache, like <code>cachedData.getA()</code>.
     * @param newRadius
     *            The new radius of the cached search results
     * @param newPlacemarks
     *            The new search results
     */
    private void updateCachedPlacemarks(Position cachedPoint, Double newRadius, List<Placemark> newPlacemarks) {
        if (cachedPoint != null) {
            synchronized (cache) {
                cache.put(cachedPoint, new Util.Triple<Position, Double, List<Placemark>>(cachedPoint, newRadius, newPlacemarks));
            }
        }
    }

    /**
     * @param position
     *            The position to be checked by the cache
     * @return The cached placemarks (with meta-data) sorted by distance towards <code>position</code> or
     *         <code>null</code>, if there's nothing cached around <code>position</code> within
     *         {@link ReverseGeocoderImpl#POSITION_CACHE_DISTANCE_LIMIT the distance limit}
     */
    private Util.Triple<Position, Double, List<Placemark>> checkCache(Position position) {
        synchronized (cache) {
            return cache.get(position, POSITION_CACHE_DISTANCE_LIMIT);
        }
    }

    private JSONArray callNearestService(Position position) throws MalformedURLException, IOException, ParseException {
        StringBuilder url = generateRequestUrlNearbyPlaceService(position);
        JSONArray geonames = submitGeonamesRequestForJSONArrayResult(url);
        return geonames;
    }

    private URLConnection addUsernameParameterAndConnect(StringBuilder url) throws MalformedURLException, IOException {
        url.append("&username=");
        url.append(getGeonamesUser());
        return HttpUrlConnectionHelper.redirectConnection(new URL(url.toString()), Duration.ONE_MINUTE, c->c.setRequestProperty("User-Agent", ""));
    }

    private JSONArray callNearbyService(Position position, double radius, int maxRows) throws MalformedURLException,
            IOException, ParseException {
        StringBuilder url = generateRequestUrlNearbyPlaceService(position, radius, maxRows);
        JSONArray geonames = submitGeonamesRequestForJSONArrayResult(url);
        return geonames;
    }
    
    private StringBuilder generateRequestUrlNearbyPlaceService(Position position) {
        StringBuilder url = new StringBuilder(NEARBY_PLACE_SERVICE);
        url.append("lat=" + Double.toString(position.getLatDeg()));
        url.append("&lng=" + Double.toString(position.getLngDeg()));
        return url;
    }
    
    private StringBuilder generateRequestUrlNearbyPlaceService(Position position, double radius, int maxRows) {
        StringBuilder url = generateRequestUrlNearbyPlaceService(position);
        url.append("&radius=" + Double.toString(radius));
        url.append("&maxRows=" + Integer.toString(maxRows));
        return url;
    }
    
    private JSONArray submitGeonamesRequestForJSONArrayResult(StringBuilder url) throws MalformedURLException, IOException, ParseException {
        final JSONObject obj = submitGeonamesRequestForJSONObjectResult(url);
        final JSONArray geonames = (JSONArray) obj.get("geonames");
        if (geonames == null) {
            logger.log(Level.WARNING, "Returning null value for geonames object: " + obj.toJSONString());
        }
        return geonames;
    }

    private String getGeonamesUser() {
        return usernames[new Random().nextInt(usernames.length)];
    }
}
