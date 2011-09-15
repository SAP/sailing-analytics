package com.sap.sailing.domain.base;

public interface Bearing {

    double getDegrees();

    double getRadians();

    /**
     * adds or subtracts 180 degrees to obtain the opposite bearing
     */
    Bearing reverse();
    
    Bearing add(Bearing diff);
    
    /**
     * Computes the bearing different that needs to be {@link #add(Bearing) added} to this bearing
     * to result in <code>b</code>. The absolute number of degrees of the resulting bearing is
     * guaranteed to be less or equal to 180. For example (simplifying bearings as the number
     * of degrees they represent), <code>180.getDegreesTo(182)==2</code>, and
     * <code>180.getDegreesTo(178)==-2</code>, and <code>10.getDegreesTo(-10)==-20</code>,
     * and <code>350.getDegreesTo(10)==20</code>, and <code>10.getDegreesTo(350)==-20</code>
     */
    Bearing getDifferenceTo(Bearing b);

    /**
     * Finds the middle between this bearing and the <code>other</code> bearing. For degree differences
     * up to and including 180 degrees, the arithmetic mean between the angles is used to construct the
     * resulting bearing. Otherwise, the reverse of the arithmetic mean is returned.
     */
    Bearing middle(Bearing other);

}
