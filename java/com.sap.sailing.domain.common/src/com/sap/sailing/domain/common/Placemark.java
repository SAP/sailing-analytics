package com.sap.sailing.domain.common;

import java.io.Serializable;
import java.util.Comparator;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;

/**
 * Used to define a populated place in the world.<br />
 * Used by {@link ReverseGeocoder}.
 * @author Lennart Hensler (D054527)
 *
 */
public interface Placemark extends Serializable {
    
    /**
     * @return The name of the Placemark
     */
    String getName();
    
    /**
     * @return The 2 letters country code of the Placemark
     */
    String getCountryCode();
    
    /**
     * @return The {@link Position} of the Placemark
     */
    Position getPosition();
    
    /**
     * @return The population of the Placemark
     */
    long getPopulation();

    /**
     * @param position The {@link Position} from which the distance is calculated
     * @return The {@link Distance} between the given {@link Position} and the Placemark
     */
    Distance distanceFrom(Position position);
    
    /**
     * Creates a new {@link Position} out of the given coordinates and calculates the distance between {@link Position} and
     * the Placemark via {@link Placemark#distanceFrom(Position) distanceFrom(Position)}.
     * @param latDeg Latitude in degrees
     * @param lngDeg Longitude in degrees
     * @return The {@link Distance} between the given coordinates and the Placemark
     */
    Distance distanceFrom(double latDeg, double lngDeg);
    
    /**
     * @return The name of the country where the Placemark is located
     */
    String getCountryName();
    
    
    
    
    
    
    /**
     * Sorts the Placemarks by Population from low to high.
     * @author Lennart Hensler (D054527)
     *
     */
    public class ByPopulation implements Comparator<Placemark> {
        @Override
        public int compare(Placemark p1, Placemark p2) {
            return p1.getPopulation() > p2.getPopulation() ? 1 : p1.getPopulation() < p2.getPopulation() ? -1 : 0;
        }
    }
    
    /**
     * Sorts the Placemarks by distance to a {@link Position} from near to far.<br /><br />
     * 
     * If you use this for {@link ReverseGeocoder#getPlacemarkLast(Position, float, Comparator) getPlacemarkBest(Position position,...)} to 
     * get the nearest Placemark for <code>position</code> you can use {@link ReverseGeocoder#getPlacemarkNearest(Position) getPlacemark} instead.
     * <code>getPlacemark</code> returns per default the nearest Placemark for a Position.
     * @author Lennart Hensler (D054527)
     *
     */
    public class ByDistance implements Comparator<Placemark> {
        
        private Position position;
        
        public ByDistance(Position position) {
            this.position = position;
        }
        
        @Override
        public int compare(Placemark p1, Placemark p2) {
            Distance d1 = p1.distanceFrom(position);
            Distance d2 = p2.distanceFrom(position);
            
            return d1.compareTo(d2);
        }
    }
    
    public class ByPopulationDistanceRatio implements Comparator<Placemark> {
        
        private Position position;
        private double populationFactor = 1.0;
        private double distanceFactor = 1.0;

        public ByPopulationDistanceRatio(Position position) {
            this.position = position;
        }

        @Override
        public int compare(Placemark p1, Placemark p2) {
            double r1 = (p1.getPopulation() * populationFactor) / (p1.distanceFrom(position).getKilometers() * distanceFactor);
            double r2 = (p2.getPopulation() * populationFactor) / (p2.distanceFrom(position).getKilometers() * distanceFactor);

            return r1 > r2 ? 1 : r1 < r2 ? -1 : 0;
        }
        
    }
    
}
