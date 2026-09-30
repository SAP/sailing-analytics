package com.sap.sailing.geocoding;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.Placemark;
import com.sap.sailing.geocoding.impl.ReverseGeocoderImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public interface ReverseGeocoder {
    final ReverseGeocoder INSTANCE = new ReverseGeocoderImpl();
    final double POSITION_CACHE_DISTANCE_LIMIT_IN_KM = 5.0;

    /**
     * Returns the nearest {@link Placemark} towards the given {@link Position}.
     * 
     * @param position The position where to search
     * @return The nearest {@link Placemark} towards the given {@link Position} or null if there is none within 300km
     * @throws IOException
     * @throws ParseException
     */
    Placemark getPlacemarkNearest(Position position) throws IOException, ParseException;

    /**
     * Returns a list of {@link Placemark Placemarks} near the given {@link Position}.
     * 
     * @param position The position where to search
     * @param radius The search radius
     * @return A list of {@link Placemark Placemarks} near the given {@link Position} or null if there are none within 300km
     * @throws IOException
     * @throws ParseException
     */
    List<Placemark> getPlacemarksNear(Position position, double radius) throws IOException, ParseException;

    /**
     * Searches for {@link Placemark Placemarks} near the given {@link Position} via
     * {@link ReverseGeocoder#getPlacemarkNearest(double, double) getPlacemarkNearest} and sorts them with
     * <code>comp</code>. Returns the last element in the sorted list.<br /><br />
     * The interface {@link Placemark} contains some Comparators for Placemarks.
     * 
     * @param position The position where to search
     * @param radius The search radius
     * @param comp Sorts the list
     * @return The last element in the sorted list
     * @throws IOException
     * @throws ParseException
     */
    Placemark getPlacemarkLast(Position position, double radius, Comparator<Placemark> comp) throws IOException,
            ParseException;

    /**
     * Searches for {@link Placemark Placemarks} near the given {@link Position} via
     * {@link ReverseGeocoder#getPlacemarkNearest(double, double) getPlacemarkNearest} and sorts them with
     * <code>comp</code>. Returns the first element in the sorted list.<br /><br />
     * The interface {@link Placemark} contains some Comparators for Placemarks.
     * 
     * @param position The position where to search      
     * @param radius The search radius                   
     * @param comp Sorts the list                        
     * @return The first element in the sorted list
     * @throws IOException
     * @throws ParseException
     */
    Placemark getPlacemarkFirst(Position position, double radius, Comparator<Placemark> comp) throws IOException,
            ParseException;
    
    /**
     * Actually not a "reverse" geocoding but a name search that emits placemarks sorted with the comparator
     * and returning the first element in the sorting order or {@code null} if the result is empty.
     */
    Placemark getPlacemark(String name, Comparator<Placemark> comp) throws IOException, ParseException;

    /**
     * Tries to obtain a {@link TimeZone} for a given location and time point. The implementation will first look for a
     * resolved GMT offset specific to the location/time point; if that is not found, the raw GMT offset will be looked
     * up in the response, and from all {@link TimeZone#getAvailableIDs() available time zones} one that has this offset
     * will be searched. If nothing is found, {@code null} is returned.
     * 
     * @param position for which location to look for a time zone
     * @param timePoint for which time point to look up the time zone; this may help resolving DST offsets
     */
    TimeZone getTimeZone(Position position, TimePoint timePoint)
            throws MalformedURLException, IOException, ParseException;
}
