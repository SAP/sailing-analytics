package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.Placemark;
import com.sap.sse.common.CountryCodeFactory;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;

/**
 * Used to define a populated place in the world.<br />
 * Used by {@link ReverseGeocoder}.
 * @author Lennart Hensler (D054527)
 *
 */
public class PlacemarkImpl implements Placemark {
    private static final long serialVersionUID = -7287453946921815463L;
    
    private String name;
    private String countryCode;
    private Position position;
    private long population;
    
    PlacemarkImpl() {}
    
    /**
     * Creates a new Placemark with the given parameters as attributes.
     */
    public PlacemarkImpl(String name, String countryCode, Position position, long population) {
        this.name = name;
        this.countryCode = countryCode;
        this.position = position;
        this.population = population;
    }
    
    /**
     * Creates a new Placemark with the given parameters as attributes and a <code>population</code> of <code>0</code>;
     */
    public PlacemarkImpl(String name, String countryCode, String countryName, Position position, String type) {
        this(name, countryCode, position, 0);
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }
    
    public Position getPosition() {
        return position;
    }

    public long getPopulation() {
        return population;
    }


    @Override
    public Distance distanceFrom(Position position) {
        return this.position.getDistance(position);
    }
    @Override
    public Distance distanceFrom(double latDeg, double lngDeg) {
        Position p = new DegreePosition(latDeg, lngDeg);
        return distanceFrom(p);
    }

    @Override
    public String getCountryName() {
        return CountryCodeFactory.INSTANCE.getFromTwoLetterISOName(countryCode).getName();
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[");
        
        b.append(name + ", ");
        b.append(countryCode + ", ");
        b.append(position.toString() + ", ");
        b.append(population + "]");
        
        return b.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((countryCode == null) ? 0 : countryCode.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (population ^ (population >>> 32));
        result = prime * result + ((position == null) ? 0 : position.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PlacemarkImpl other = (PlacemarkImpl) obj;
        if (countryCode == null) {
            if (other.countryCode != null)
                return false;
        } else if (!countryCode.equals(other.countryCode))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (population != other.population)
            return false;
        if (position == null) {
            if (other.position != null)
                return false;
        } else if (!position.equals(other.position))
            return false;
        return true;
    }

}
