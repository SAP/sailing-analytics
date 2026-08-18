package com.sap.sailing.domain.orc;

import java.io.IOException;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * An interface to {@code data.orc.org}, mostly the search functionality at
 * {@code https://data.orc.org/public/WPub.dll?action=SrchCert&xslp=scert.php}. It can search for certificates by a
 * number of criteria, among them the issuing country, sail number, ORC reference number, or boat name. Those search
 * results are represented as {@link CertificateHandle} objects. Such handles can also be obtained for certificates that
 * are not currently valid. See the {@code includeInvalid} parameter of the
 * {@link #search(CountryCode, Integer, String, String, String, String, boolean)} method. However, handles for
 * {@link CertificateHandle#isValid() invalid} certificates cannot be resolved into {@link ORCCertificate} objects
 * through {@link #getCertificates(Iterable)} or {@link #getCertificates(CertificateHandle...)} because the database
 * returns full certificates only if they are valid.
 * <p>
 * 
 * As the web site emits XML documents as search results, those can be parsed and turned into handles to certificates.
 * An example output (excerpt):
 * 
 * <pre>
&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;?xml-stylesheet type="text/xsl" href="public.php?content=ListCert.php" ?&gt;
&lt;ROOT&gt;
  &lt;DATA&gt;
    &lt;ROW RowNum="1"&gt;
      &lt;CountryId&gt;GRE&lt;/CountryId&gt;
      &lt;SSSID&gt;61576&lt;/SSSID&gt;
      &lt;DatInGID&gt;{8C8F3E75-4926-4A30-B0C8-08D36774B1F4}&lt;/DatInGID&gt;
      &lt;dxtID&gt;4793&lt;/dxtID&gt;
      &lt;RefNo&gt;GRE00000701&lt;/RefNo&gt;
      &lt;dxtName&gt;GR1329.dxt&lt;/dxtName&gt;
      &lt;YachtName&gt;*AXION ESTI&lt;/YachtName&gt;
      &lt;SailNo&gt;GRE-1329&lt;/SailNo&gt;
      &lt;Class&gt;SUN ODYSSEY 32&lt;/Class&gt;
      &lt;Designer&gt;&lt;/Designer&gt;
      &lt;Builder&gt;JEANNEAU&lt;/Builder&gt;
      &lt;dxtDate&gt;2009-05-12T23:09:58.000&lt;/dxtDate&gt;
      &lt;CertType&gt;3&lt;/CertType&gt;
      &lt;VPPVer&gt;&lt;/VPPVer&gt;
      &lt;VPPYear&gt;2009&lt;/VPPYear&gt;
      &lt;IsOd&gt;False&lt;/IsOd&gt;
      &lt;GPH&gt;786.1&lt;/GPH&gt;
      &lt;Age&gt;1993&lt;/Age&gt;
      &lt;Override&gt;&lt;/Override&gt;
      &lt;Provisional&gt;False&lt;/Provisional&gt;
      &lt;Selected&gt;False&lt;/Selected&gt;
      &lt;CanSelect&gt;False&lt;/CanSelect&gt;
    &lt;/ROW&gt;
    &lt;ROW RowNum="2"&gt;
      &lt;CountryId&gt;GRE&lt;/CountryId&gt;
      &lt;SSSID&gt;15709&lt;/SSSID&gt;
      &lt;DatInGID&gt;{C790AAFA-9861-435B-BEBE-0606BAFE2721}&lt;/DatInGID&gt;
      &lt;dxtID&gt;&lt;/dxtID&gt;
      &lt;RefNo&gt;&lt;/RefNo&gt;
      &lt;dxtName&gt;GR1329.dat&lt;/dxtName&gt;
      &lt;YachtName&gt;*AXION ESTI&lt;/YachtName&gt;
      &lt;SailNo&gt;GRE-1329&lt;/SailNo&gt;
      &lt;Class&gt;SUN ODYSSEY 32&lt;/Class&gt;
      &lt;Designer&gt;&lt;/Designer&gt;
      &lt;Builder&gt;JEANNEAU&lt;/Builder&gt;
      &lt;dxtDate&gt;2008-09-18T15:29:36.000&lt;/dxtDate&gt;
      &lt;CertType&gt;0&lt;/CertType&gt;
      &lt;VPPVer&gt;&lt;/VPPVer&gt;
      &lt;VPPYear&gt;2008&lt;/VPPYear&gt;
      &lt;IsOd&gt;False&lt;/IsOd&gt;
      &lt;GPH&gt;&lt;/GPH&gt;
      &lt;Age&gt;1998&lt;/Age&gt;
      &lt;Override&gt;&lt;/Override&gt;
      &lt;Provisional&gt;False&lt;/Provisional&gt;
      &lt;Selected&gt;False&lt;/Selected&gt;
      &lt;CanSelect&gt;False&lt;/CanSelect&gt;
    &lt;/ROW&gt;
  &lt;/ROOT&gt;
&lt;/DATA&gt;
 * </pre>
 * 
 * This also shows that some query results do not provide a valid link to a downloadable copy of the certificate. Those
 * are automatically removed from the results here.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ORCPublicCertificateDatabase {
    ORCPublicCertificateDatabase INSTANCE = new ORCPublicCertificateDatabaseImpl();
    
    public enum CertificateFamily {
        UNKNOWN(0, ""), ORC(1, "ORC"), SUPER_YACHT(2, "SY"), DOUBLE_HANDED(3, "DH"), MULTI_HULL(4, "Mu"), NON_SPINNAKER(5, "NS"), ORC_LIGHT(6, "LITE");
        
        private final int familyId;
        private final String familyQueryParamValue;
        private final static Map<Integer, CertificateFamily> familyById;
        
        static {
            familyById = new HashMap<>();
            for (final CertificateFamily family : values()) {
                familyById.put(family.getFamilyId(), family);
            }
        }
        
        private CertificateFamily(int familyId, String familyQueryParamValue) {
            this.familyId = familyId;
            this.familyQueryParamValue = familyQueryParamValue;
        }

        public int getFamilyId() {
            return familyId;
        }

        public String getFamilyQueryParamValue() {
            return familyQueryParamValue;
        }
        
        public static CertificateFamily fromId(int familyId) {
            return familyById.get(familyId);
        }
    }
    
    /**
     * Data about valid certificates in a country, as obtained, e.g., from http://data.orc.org/public/WPub.dll/RMS. Such
     * a record, in its original XML representation, looks like this:
     * 
     * <pre>
      &lt;CountryId&gt;AUS&lt;/CountryId&gt;
      &lt;Family&gt;&lt;/Family&gt;
      &lt;CertType&gt;3&lt;/CertType&gt;
      &lt;VPPYear&gt;2020&lt;/VPPYear&gt;
      &lt;CertCount&gt;161&lt;/CertCount&gt;
      &lt;CountryName&gt;Australia&lt;/CountryName&gt;
      &lt;LastUpdate&gt;2021-01-08T08:12:57.000Z&lt;/LastUpdate&gt;
      &lt;CertName&gt;Club&lt;/CertName&gt;
      &lt;RMSCode&gt;CLUB&lt;/RMSCode&gt;
     * </pre>
     * 
     * Mappings of family and type work as follows:
     * 
     * Used for parsing the ORC public API (WPub.dll) RMS/JSON outputs. Note: Family 6 (ORC Light) certificates
     * generally lack GPH and Performance Curve data required for PCS calculations.
     *
     * <table border="1">
     * <caption>ORC Family and Certificate Type Codes</caption>
     * <tr>
     * <th>Family ID</th>
     * <th>Family Name</th>
     * <th>certType</th>
     * <th>Certificate Name</th>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>ORC Standard</td>
     * <td>2</td>
     * <td>International (ORCi)</td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>ORC Standard</td>
     * <td>3</td>
     * <td>Club</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>Super Yacht</td>
     * <td>7</td>
     * <td>ORCsy</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>Double Handed</td>
     * <td>8</td>
     * <td>DH International</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>Double Handed</td>
     * <td>9</td>
     * <td>DH Club</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>Multihull</td>
     * <td>15</td>
     * <td>Multihull International</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>Multihull</td>
     * <td>16</td>
     * <td>Multihull Club</td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td>Non Spinnaker</td>
     * <td>10</td>
     * <td>NS International</td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td>Non Spinnaker</td>
     * <td>11</td>
     * <td>NS Club</td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td>ORC Light</td>
     * <td>13</td>
     * <td>Light (Standard)</td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td>ORC Light</td>
     * <td>14</td>
     * <td>Light (Double Handed)</td>
     * </tr>
     * </table>
     * 
     * @author Axel Uhl (D043530)
     */
    public interface CountryOverview {
        CountryCode getIssuingCountry();
        CertificateFamily getFamily();
        Integer getCertType();
        Integer getVPPYear();
        int getCertCount();
        TimePoint getLastUpdate();
        String getCertName();
        String getRMSCode();
    }
    
    /**
     * Equality and hash code of such handles is based on the {@link #getReferenceNumber() reference number} only.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    public interface CertificateHandle {
        CountryCode getIssuingCountry();
        String getFileId();
        Double getGPH();
        String getSSSID();
        UUID getDatInGID();
        String getReferenceNumber();
        CertificateFamily getFamily();
        String getYachtName();
        String getSailNumber();
        String getBoatClassName();
        String getDesigner();
        String getBuilder();
        Integer getYearBuilt();
        TimePoint getIssueDate();
        Integer getCertType();
        Boolean isOd();
        Boolean isProvisional();
        Boolean isValid();
    }
    
    /**
     * Searches for certificates based on various criteria. Pass {@code null} for a criterion to not restrict search
     * results based on that criterion. You can use "%" as wildcards in the {@code yachtName}, {@code sailNumber} and
     * {@code boatClassName} parameters.
     * <p>
     * 
     * @param includeInvalid
     *            If {@code true}, handles may have {@link CertificateHandle#isValid()}{@code == false}, meaning that
     *            such a handle cannot be resolved to a {@link ORCCertificate} using any of
     *            {@link #getCertificate(String)}, {@link #getCertificates(Iterable)} and
     *            {@link #getCertificates(CertificateHandle...)}. If {@code false}, only valid certificate handles
     *            will be returned that should all be resolvable into an {@link ORCCertificate} with any of the methods
     *            listed above.
     * 
     * @return never {@code null}; an object which may be {@link Util#isEmpty() empty}.
     */
    Iterable<CertificateHandle> search(CountryCode country, Integer yearOfIssuance, String referenceNumber,
            String yachtName, String sailNumber, String boatClassName, boolean includeInvalid) throws Exception;
    
    Iterable<ORCCertificate> getCertificates(Iterable<CertificateHandle> handles) throws Exception;
    
    default Iterable<ORCCertificate> getCertificates(CertificateHandle... handles) throws Exception {
        return getCertificates(Arrays.asList(handles));
    }
    
    default CertificateHandle getCertificateHandle(String referenceNumber) throws Exception {
        return search(null, null, referenceNumber, null, null, null, /* includeInvalid */ true).iterator().next();
    }
    
    ORCCertificate searchForUpdate(ORCCertificate certificate) throws Exception;
    
    ORCCertificate searchForUpdate(CertificateHandle certificateHandle) throws Exception;
    
    /**
     * Searches for a default {@link CertificateFamily#ORC} certificate by the given {@code referenceNumber}.
     * Note that this may not find certificates of other families.
     */
    default ORCCertificate getCertificate(String referenceNumber) throws Exception {
        return getCertificate(referenceNumber, CertificateFamily.ORC);
    }

    ORCCertificate getCertificate(String referenceNumber, CertificateFamily family) throws Exception;

    default ORCCertificate getCertificate(CertificateHandle certificateHandle) throws Exception {
        final Iterable<ORCCertificate> certificates = getCertificates(certificateHandle);
        return certificates.iterator().hasNext() ? certificates.iterator().next() : null;
    }

    /**
     * Creates a lookup job that is forked into the background. The job will first try an exact lookup
     * with the parameters as specified. If one or more results are obtained this way, they are returned. If no
     * certificate is found, a relaxation and rotation process starts, under the following assumptions:
     * <ul>
     * <li>The {@code sailNumber} may be noted in a different way from the way the ORC database has it represented;
     * typical variations include the addition or removal of space and dash characters between the nationality letters
     * and the actual numeric digits, e.g. "DEN- 13" instead of "DEN13" or "DEN 13".</li>
     * <li>The boat class name used here may differ from the boat class name used by the ORC database. We will try the
     * different boat class alias names to keep looking for a match.</li>
     * <li>Yacht name and sail number may have been swapped in how the boat was specified. We'll try swapping them in
     * the search request</li>
     * </ul>
     * 
     * @param yachtName may be {@code null}
     * @param sailNumber may be {@code null}
     * @param boatClass may be {@code null}
     * 
     * @return an object from which the caller can obtain the set of certificates found by invoking
     *         {@link Future#get()}. The result set will be the smallest, most concise non-empty set found, or it will
     *         be empty in case no match was found at all, regardless the rotation and alterations applied to the search
     *         parameters.
     */
    Future<Set<ORCCertificate>> search(String yachtName, String sailNumber, BoatClass boatClass);
    
    Date parseDate(final String dateString) throws DateTimeParseException;

    Iterable<CountryOverview> getCountriesWithValidCertificates()
            throws SAXException, IOException, ParserConfigurationException, DOMException, ParseException;
}
