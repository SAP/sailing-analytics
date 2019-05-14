package com.sap.sailing.domain.orc;

import java.util.Map;


/**
 * Extracts ORC-Certificates from different sources and different formats. Returns ORC-Certificate objects for given
 * identification (sail number).
 * 
 * @author Daniel Lisunkin (i505543)
 * 
 **/
public interface ORCCertificateImporter {

    /**
     * Returns an ORCCertificate object to a given sail number. If there is no certificate for the given id, the method
     * returns {@link null}.
     **/
    //TODO Better input specification on identification of certificate.
    ORCCertificate getCertificate(String sailnumber);

    /**
     * Returns a map of ORCCertificate objects to a given array of sail numbers.
     **/
    Map<String, ORCCertificate> getCertificates(String[] sailnumbers);

}
