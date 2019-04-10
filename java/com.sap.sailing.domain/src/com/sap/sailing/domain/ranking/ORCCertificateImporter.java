package com.sap.sailing.domain.ranking;

import java.util.Map;

/**
 * Extracts ORC-Certificates from different sources and different formats. Returns ORC-Certificate objects for given
 * identification (sailnumber).
 * 
 * @author Daniel Lisunkin (i505543)
 * 
 **/
public interface ORCCertificateImporter {

    /**
     * Imports and processes the given Data for accessing and creating ORCCertificates by identification. 
     * **/
    public void importData();
    
    /**
     * Returns an ORCCertificate object to a given sailnumber.
     * **/
    ORCCertificate getCertificate(String sailnumber);
    
    
    /**
     * Returns a map of ORCCertificate objects to a given array of sailnumbers.
     * **/
    Map<String, ORCCertificate> getCertificates(String[] sailnumbers);

}
