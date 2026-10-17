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
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateHandle;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
import com.sap.sse.common.Util;

@ExtendWith(FailIfNoValidOrcCertificateRule.class)
public class TestORCPublicCertificateDatabase {
    private static final Logger logger = Logger.getLogger(TestORCPublicCertificateDatabase.class.getName());

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

    /**
     * Picks a live reference number from the certificates probed by {@link FailIfNoValidOrcCertificateRule} and returns
     * the corresponding {@link CertificateHandle}. Fails the test when no seed handle can be obtained, which would
     * indicate a broken search rather than a legitimate empty result.
     */
    private CertificateHandle pickSeedHandle() throws Exception {
        final Collection<ORCCertificate> probed = FailIfNoValidOrcCertificateRule.getAvailableCerts();
        assertFalse(probed.isEmpty(), "No ORC certificates could be probed; cannot seed the search tests.");
        final ORCCertificate seedCertificate = probed.iterator().next();
        final CertificateHandle handle = db.getCertificateHandle(seedCertificate.getReferenceNumber());
        assertNotNull(handle, "Unable to obtain a handle for seed reference number " + seedCertificate.getReferenceNumber());
        return handle;
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testSimpleSearchByRefNo() throws Exception {
        final String referenceNumber = pickSeedHandle().getReferenceNumber();
        final Iterable<CertificateHandle> result = db.search(/* country */ null, /* yearOfIssuance */ null,
                /* referenceNumber */ referenceNumber, /* yachtName */ null, /* sailNumber */ null,
                /* boatClassName */ null, /* includeInvalid */ true);
        assertEquals(1, Util.size(result));
        assertEquals(referenceNumber, result.iterator().next().getReferenceNumber());
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testSearchByYachtNameAndSailNumberAndYear() throws Exception {
        final CertificateHandle seed = pickSeedHandle();
        final Iterable<CertificateHandle> result = db.search(/* country */ null,
                /* yearOfIssuance */ LocalDate.now().getYear(), /* referenceNumber */ null,
                /* yachtName */ seed.getYachtName(), /* sailNumber */ seed.getSailNumber(), /* boatClassName */ null,
                /* includeInvalid */ true);
        boolean found = false;
        for (final CertificateHandle handle : result) {
            if (handle.getReferenceNumber().equals(seed.getReferenceNumber())) {
                found = true;
            }
        }
        assertTrue(found, "Search by yacht name and sail number did not return the seed certificate " + seed.getReferenceNumber());
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testSearchByYachtName() throws Exception {
        final CertificateHandle seed = pickSeedHandle();
        final Iterable<CertificateHandle> result = db.search(/* country */ null, /* yearOfIssuance */ null,
                /* referenceNumber */ null, /* yachtName */ seed.getYachtName(), /* sailNumber */ null,
                /* boatClassName */ null, /* includeInvalid */ true);
        boolean found = false;
        for (final CertificateHandle handle : result) {
            if (handle.getReferenceNumber().equals(seed.getReferenceNumber())) {
                found = true;
            }
        }
        assertTrue(found, "Search by yacht name did not return the seed certificate " + seed.getReferenceNumber());
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testCertificateHandleAPI() throws Exception {
        final String referenceNumber = pickSeedHandle().getReferenceNumber();
        final CertificateHandle result = db.getCertificateHandle(referenceNumber);
        assertEquals(referenceNumber, result.getReferenceNumber());
        assertNotNull(result.getIssueDate());
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testCertificateUpdate() throws Exception {
        final CertificateHandle oldHandle = pickSeedHandle();
        final ORCCertificate result = db.searchForUpdate(oldHandle);
        if (result != null) {
            assertEquals(oldHandle.getIssuingCountry(), result.getIssuingCountry());
            assertFalse(oldHandle.getIssueDate().after(result.getIssueDate()));
        } else {
            logger.warning("Couldn't find an update to certificate with reference number "+oldHandle.getReferenceNumber()+
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
                final Iterable<CertificateHandle> certHandles = db.search(/* country */ cert.getIssuingCountry(),
                        LocalDate.now().getYear(), /* referenceNumber */ null, cert.getBoatName(), cert.getSailNumber(),
                        /*
                         * boat class name; could be set to cert.getBoatClassName() but there are deviations in ORC DBs
                         * and query API, so leaving null:
                         */ null, /* includeInvalid */ false);
                final Optional<CertificateHandle> certificateHandle = Optional
                        .ofNullable(certHandles.iterator().hasNext() ? certHandles.iterator().next() : null);
                assertTrue(certificateHandle.isPresent(), "No certificate found for handle "+certificateHandle+
                                " extracted from certificates "+certificates);
                final String referenceNumber = certificateHandle.get().getReferenceNumber();
                final CertificateHandle handle = db.getCertificateHandle(referenceNumber);
                final ORCCertificate result = db.getCertificate(referenceNumber, handle.getFamily());
                assertNotNull(result, "Unable to load certificate for reference number "+referenceNumber+" from handle "+certificateHandle);
                // The activecerts search result handle carries no GPH; the GPH is only populated on the full certificate
                // download, so assert against the downloaded certificate's GPH instead.
                assertNotNull(result.getGPH(), "No GPH on downloaded certificate "+referenceNumber);
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

    private boolean assertFoundYear(final Set<ORCCertificate> certificates, int year) {
        return certificates.stream().map(cert -> {
            LocalDate certDate = Instant.ofEpochMilli(cert.getIssueDate().asMillis()).atOffset(ZoneOffset.UTC)
                    .toLocalDate();
            return certDate.getYear();
        }).anyMatch(cy -> cy == year);
    }
}
