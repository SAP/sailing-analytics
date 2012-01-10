package com.sap.sailing.domain.common;



/**
 * A distance which can be converted to various units of measure. Can be negative.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface Distance extends Comparable<Distance> {
    static final Distance NULL = new Distance() {
        @Override
        public int compareTo(Distance o) {
            return getMeters() > o.getMeters() ? 1 : getMeters() == o.getMeters() ? 0 : -1;
        }

        @Override
        public double getGeographicalMiles() {
            return 0;
        }

        @Override
        public double getSeaMiles() {
            return 0;
        }

        @Override
        public double getNauticalMiles() {
            return 0;
        }

        @Override
        public double getMeters() {
            return 0;
        }

        @Override
        public double getKilometers() {
            return 0;
        }

        @Override
        public double getCentralAngleDeg() {
            return 0;
        }

        @Override
        public double getCentralAngleRad() {
            return 0;
        }

        @Override
        public Distance scale(double factor) {
            return this;
        }

        @Override
        public Speed inTime(long milliseconds) {
            return Speed.NULL;
        }
        
    };
    
    double getGeographicalMiles();

    double getSeaMiles();

    double getNauticalMiles();

    double getMeters();

    double getKilometers();

    double getCentralAngleDeg();

    double getCentralAngleRad();

    Distance scale(double factor);
    
    /**
     * Computes the (undirected) average speed one has traveled at when passing this distance in the number of
     * milliseconds specified. Sign-sensitive, meaning, e.g., that if this distance is negative and the time is positive
     * then the resulting speed will be negative.
     */
    Speed inTime(long milliseconds);
}
