package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateFamily;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateHandle;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
import com.sap.sse.common.Util;

@ExtendWith(FailIfNoValidOrcCertificateRule.class)
public class TestORCPublicCertificateDatabase {
    private static final Logger logger = Logger.getLogger(TestORCPublicCertificateDatabase.class.getName());
    // A known reference number for the boat SOULMATE. The certificate is no longer active, so it can only be reached
    // through the DownBoatRMS_byrefno endpoint (getCertificate / searchForUpdate), not through the active-certificate
    // search. The former active-certificate search tests that used to key on this boat now live in
    // TestORCActiveCertificateSearch against a currently active certificate.
    private static final String SOULMATE_REF_NO = "FRA00013881";
    private static final String SOULMATE_YACHT_NAME = "SOULMATE";
    private static final String SOULMATE_BOAT_CLASS = "MOSQUITO 85";
    private static final String SOULMATE_SAIL_NUMBER = "DEN-   13";
    private static final double SOULMATE_GPH_SECONDS = 662.4;

    private ORCPublicCertificateDatabase db;
    private Map<String, Date> dateComparisonMap = new LinkedHashMap<String, Date>();
    private List<String> dateFailureCases = Arrays.asList("2019-02-21T10:44GMT+2","2019-02-21T10:38+0800","2019-02-21T10:38+08:00",
            "2019-02-21T10:38-08","2019-02-21T10:38Z","2019-02-21T10z","2019-02-21T10:38z");
    
    @BeforeEach
    public void setUp() {
        db = new ORCPublicCertificateDatabaseImpl();
        /**
         * By default GregorianCalendar month starts from 0 as January and so on. In below cases 1 specifies the date
         * month as February.
         */
        dateComparisonMap.put("2019-02-21T10:38:59Z", Date.from(ZonedDateTime.parse("2019-02-21T10:38:59Z").toInstant()));
        //                dateComparisonMap.put("2019-02-21T12:00:00.000GMT+2", Date.from(ZonedDateTime.of(2019, 2, 21, 10, 38, 59, 0, ZoneId.systemDefault()).toInstant()));
        //        //        dateComparisonMap.put("2019-02-21T15:00:00.000 GMT-2", Date.from(ZonedDateTime.of(2019, 2, 21, 10, 38, 59, 0, ZoneId.systemDefault()).toInstant()));
        dateComparisonMap.put("2019-02-21T10:38:59Z", Date.from(ZonedDateTime.parse("2019-02-21T10:38:59Z").toInstant()));
        dateComparisonMap.put("2019-02-21T10:38:55.000-0800", 
                Date.from(ZonedDateTime.parse("2019-02-21T10:38:55.000-0800", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).toInstant())
                );
        dateComparisonMap.put(
                "2019-02-21T10:38:32.000+08:00", 
                Date.from(ZonedDateTime.parse("2019-02-21T10:38:32.000+08:00").toInstant())
                );
        dateComparisonMap.put(
                "2019-02-21T10:38:09.000-06", 
                Date.from(ZonedDateTime.parse("2019-02-21T10:38:09.000-06", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSx")).toInstant())
                );
        dateComparisonMap.put(
                "2019-02-21T10:38:22.000Z", 
                Date.from(ZonedDateTime.parse("2019-02-21T10:38:22.000Z").toInstant())
                );
        dateComparisonMap.put(
                "2019-02-21T10:38:00.000z", 
                Date.from(ZonedDateTime.parse("2019-02-21T10:38:00.000z").toInstant())
                );
        dateComparisonMap.put(
                "2019-02-21T10:38:46z", 
                Date.from(ZonedDateTime.parse("2019-02-21T10:38:46z").toInstant())
                );
        dateComparisonMap.put(
                "2019-02-21T10:38:17", 
                Date.from(LocalDateTime.parse("2019-02-21T10:38:17").toInstant(ZoneOffset.UTC))
                );
        dateComparisonMap.put(
                "2019-02-21T10:38:33.000", 
                Date.from(LocalDateTime.parse("2019-02-21T10:38:33.000").toInstant(ZoneOffset.UTC))
                );
    }

    @Test
    public void testSimpleSearchByRefNo() throws Exception {
        final ORCCertificate certificate = db.getCertificate(SOULMATE_REF_NO, CertificateFamily.ORC);
        assertSoulmate(SOULMATE_REF_NO, certificate);
    }

    @Test
    public void testCertificateHandleAPI() throws Exception {
        final CertificateHandle handle = db.getCertificateHandle(SOULMATE_REF_NO);
        assertSoulmate(SOULMATE_REF_NO, handle);
    }

    @Test
    public void testCertificateUpdate() throws Exception {
        final ORCCertificate oldCertificate = db.getCertificate(SOULMATE_REF_NO, CertificateFamily.ORC);
        assertSoulmate(SOULMATE_REF_NO, oldCertificate);
        final ORCCertificate result = db.searchForUpdate(oldCertificate);
        if (result != null) {
            assertEquals(oldCertificate.getIssuingCountry(), result.getIssuingCountry());
            assertFalse(oldCertificate.getIssueDate().after(result.getIssueDate()));
        } else {
            logger.warning("Couldn't find an update to certificate with reference number "+SOULMATE_REF_NO+
                    " anymore. Consider updating this test case to using a different certificate that still has a valid update.");
        }
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testGetCertificate() throws Exception {
        Collection<ORCCertificate> certificates = FailIfNoValidOrcCertificateRule.getAvailableCerts();
        // some certificates seem to be lacking a GPH value; can't use those for this test; we're explicitly excluding LITE certificates already...
        for (final ORCCertificate cert : certificates) {
            if (cert.getGPH() != null) {
                Iterable<CertificateHandle> certHandles = db.search(/* country */ null, LocalDate.now().getYear(), /* referenceNumber */ null, cert.getBoatName(),
                        cert.getSailNumber(), /*
                                               * boat class name; could be set to cert.getBoatClassName() but there are
                                               * deviations in ORC DBs and query API, so leaving null:
                                               */ null);
                if (Util.isEmpty(certHandles)) {
                    // there were certs; get one from the previous year
                    certHandles = db.search(null, LocalDate.now().getYear()-1, null, cert.getBoatName(),
                            cert.getSailNumber(), /*
                                                   * boat class name; could be set to cert.getBoatClassName() but there are
                                                   * deviations in ORC DBs and query API, so leaving null:
                                                   */ null);
                }
                if (Util.isEmpty(certHandles)) {
                    // there were certs; try searching by reference number
                    certHandles = db.search(null, LocalDate.now().getYear(), cert.getReferenceNumber(), /* boat name may have deviated due to special characters */ null,
                            cert.getSailNumber(), /*
                                                   * boat class name; could be set to cert.getBoatClassName() but there are
                                                   * deviations in ORC DBs and query API, so leaving null:
                                                   */ null);
                }
                if (Util.isEmpty(certHandles)) {
                    // still nothing? Then try by reference number in previous year:
                    certHandles = db.search(null, LocalDate.now().getYear()-1, cert.getReferenceNumber(), /* boat name may have deviated due to special characters */ null,
                            cert.getSailNumber(), /*
                                                   * boat class name; could be set to cert.getBoatClassName() but there are
                                                   * deviations in ORC DBs and query API, so leaving null:
                                                   */ null);
                }
                Optional<CertificateHandle> certificateHandle = Optional.ofNullable(certHandles.iterator().hasNext() ? certHandles.iterator().next() : null);
                assertTrue(certificateHandle.isPresent(), "No certificate found for handle "+certificateHandle+
                                " extracted from certificates "+certificates);
                final String referenceNumber = certificateHandle.get().getReferenceNumber();
                final CertificateHandle handle = db.getCertificateHandle(referenceNumber);
                final ORCCertificate result = db.getCertificate(referenceNumber, handle.getFamily());
                assertNotNull(result, "Unable to load certificate for reference number "+referenceNumber+" from handle "+certificateHandle);
                // The active-certificate search result handle carries no GPH; the GPH is only populated on the full
                // certificate download, so assert that the downloaded certificate carries a GPH rather than comparing
                // it to the handle's (absent) GPH.
                assertNotNull(result.getGPH(), "No GPH on downloaded certificate "+referenceNumber);
                // Use some tolerance as we found differences as much as 5s between the dxtDate in the handle coming from the search result
                // and the IssueDate field in the JSON. Both suggest to report millisecond accuracy, but dxtDate always seems to have the
                // milliseconds as "000" explaining many sub-second differences. But in some cases differences were significantly bigger.
                assertEquals(handle.getIssueDate().asMillis(), result.getIssueDate().asMillis(), 10000.0, "Issue dates of certificate with reference number "+referenceNumber+
                                " varies between current year result handle ("+handle.getIssueDate()+") and certificate ("+
                                result.getIssueDate()+").");
                assertEquals(handle.getSailNumber(), result.getSailNumber());
                return;
            } else {
                logger.info("No valid GPH found in certificate "+cert.getBoatName()+" with sail number "+cert.getSailNumber());
            }
        }
        fail("No certificate found with a valid GPH; that seems very suspicious and lets this test case fail.");
    }
    
    @FailIfNoValidOrcCertificates
    @Test
    public void testParallelFuzzySearch() throws InterruptedException, ExecutionException {
        int year = LocalDate.now().getYear();
        ArrayList<Future<Set<ORCCertificate>>> futures = new ArrayList<Future<Set<ORCCertificate>>>();
        boolean isYearFound = false;
        for (ORCCertificate orcCertificate : FailIfNoValidOrcCertificateRule.getAvailableCerts()) {
            futures.add(db.search(orcCertificate.getBoatName(), orcCertificate.getSailNumber(),
                    new BoatClassImpl(orcCertificate.getBoatClassName(), true)));
        }
        for (Future<Set<ORCCertificate>> futureResult : futures) {
            isYearFound = isYearFound || assertFoundYear(futureResult.get(), year) || assertFoundYear(futureResult.get(), year-1);
        }
        assertTrue(isYearFound);
    }
    
    @Test
    public void testParseDateSuccessCases() {
        for (String date : dateComparisonMap.keySet()) {
            Date convertedDate = db.parseDate(date);
            assertEquals(convertedDate, dateComparisonMap.get(date));
        }
    }
    
    @Test
    public void testShould() throws Exception {
        assertThrows(DateTimeParseException.class, ()->{
            for (String dateString : dateFailureCases) {
                db.parseDate(dateString);
                Assertions.fail(dateString + " is parsable");
            }
        });
    }

    private void assertSoulmate(final String referenceNumber, final ORCCertificate certificate) {
        assertNotNull(certificate, "Unable to load certificate with reference number " + referenceNumber);
        assertEquals(referenceNumber, certificate.getReferenceNumber());
        assertEquals(SOULMATE_YACHT_NAME, certificate.getBoatName());
        assertEquals(SOULMATE_BOAT_CLASS, certificate.getBoatClassName());
        assertEquals(SOULMATE_SAIL_NUMBER, certificate.getSailNumber());
        assertEquals("FRA", certificate.getIssuingCountry().getThreeLetterIOCCode());
        assertEquals(SOULMATE_GPH_SECONDS, certificate.getGPH().asSeconds(), 0.05);
    }

    private void assertSoulmate(final String referenceNumber, final CertificateHandle handle) {
        assertNotNull(handle, "Unable to load certificate handle with reference number " + referenceNumber);
        assertEquals(referenceNumber, handle.getReferenceNumber());
        assertEquals(SOULMATE_YACHT_NAME, handle.getYachtName());
        assertEquals(SOULMATE_BOAT_CLASS, handle.getBoatClassName());
        assertEquals(SOULMATE_SAIL_NUMBER, handle.getSailNumber());
        assertEquals("FRA", handle.getIssuingCountry().getThreeLetterIOCCode());
        assertEquals(SOULMATE_GPH_SECONDS, handle.getGPH(), 0.05);
    }

    private boolean assertFoundYear(final Set<ORCCertificate> certificates, int year) {
        return certificates.stream().map(cert -> {
            LocalDate certDate = Instant.ofEpochMilli(cert.getIssueDate().asMillis()).atOffset(ZoneOffset.UTC)
                    .toLocalDate();
            return certDate.getYear();
        }).anyMatch(cy -> cy == year);
    }
}
