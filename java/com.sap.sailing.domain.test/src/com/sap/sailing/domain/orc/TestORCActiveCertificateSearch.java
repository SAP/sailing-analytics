package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateFamily;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateHandle;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
import com.sap.sse.common.Util;

/**
 * Active-certificate search coverage against the key-based {@code activecerts} endpoint of the ORC public certificate
 * API. The {@code activecerts} endpoint only ever serves <em>active</em> certificates, so instead of pinning a fixed
 * reference number (which would expire and need replacing every year) these tests pick a live, currently active
 * certificate as a "seed" and then run the search variants against that seed's own values. This keeps the tests
 * self-maintaining as the ORC database evolves.
 * <p>
 * These tests are deliberately isolated in their own class so that the (comparatively large) migration to the key-based
 * API can be reviewed without this live-fixture coverage complicating the diff of the pre-existing tests in
 * {@link TestORCPublicCertificateDatabase}, whose reference-number download / update coverage keeps using the
 * inactive SOULMATE certificate reachable only through the {@code DownBoatRMS_byrefno} endpoint.
 */
@ExtendWith(FailIfNoValidOrcCertificateRule.class)
public class TestORCActiveCertificateSearch {
    private ORCPublicCertificateDatabase db;

    @BeforeEach
    public void setUp() {
        db = new ORCPublicCertificateDatabaseImpl();
    }

    /**
     * Picks a live, currently active certificate handle to use as the seed for the search tests. It searches the
     * current VPP year for the country of one of the certificates the {@link FailIfNoValidOrcCertificateRule} probed,
     * so the seed is guaranteed to be an active certificate that the {@code activecerts} endpoint can find again.
     */
    private CertificateHandle pickSeedHandle() throws Exception {
        final List<CertificateHandle> seeds = new ArrayList<>();
        for (final ORCCertificate cert : FailIfNoValidOrcCertificateRule.getAvailableCerts()) {
            for (final int year : new int[] { LocalDate.now().getYear(), LocalDate.now().getYear() - 1 }) {
                if (seeds.isEmpty()) {
                    final Iterable<CertificateHandle> handles = db.search(cert.getIssuingCountry(), year,
                            /* referenceNumber */ null, /* yachtName */ null, /* sailNumber */ null,
                            /* boatClassName */ null);
                    for (final CertificateHandle handle : handles) {
                        if (seeds.isEmpty() && handle.getFamily() != CertificateFamily.ORC_LIGHT) {
                            seeds.add(handle);
                        }
                    }
                }
            }
        }
        assertTrue(!seeds.isEmpty(), "Could not find any active certificate to seed the search tests from.");
        return seeds.get(0);
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testSimpleSearchByRefNo() throws Exception {
        final CertificateHandle seed = pickSeedHandle();
        final Iterable<CertificateHandle> handles = db.search(/* country */ null, /* year */ null,
                seed.getReferenceNumber(), /* yachtName */ null, /* sailNumber */ null, /* boatClassName */ null);
        boolean found = false;
        for (final CertificateHandle handle : handles) {
            found = found || seed.getReferenceNumber().equals(handle.getReferenceNumber());
        }
        assertTrue(found, "No active certificate found for seed reference number " + seed.getReferenceNumber());
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testSearchByYachtNameAndSailNumberAndYear() throws Exception {
        final CertificateHandle seed = pickSeedHandle();
        final int seedYear = Instant.ofEpochMilli(seed.getIssueDate().asMillis()).atOffset(ZoneOffset.UTC)
                .toLocalDate().getYear();
        final Iterable<CertificateHandle> handles = db.search(/* country */ null, seedYear, /* referenceNumber */ null,
                seed.getYachtName(), seed.getSailNumber(), /* boatClassName */ null);
        boolean found = false;
        for (final CertificateHandle handle : handles) {
            found = found || seed.getReferenceNumber().equals(handle.getReferenceNumber());
        }
        assertTrue(found, "Expected to find reference number " + seed.getReferenceNumber()
                + " when searching by its own yacht name " + seed.getYachtName() + ", sail number "
                + seed.getSailNumber() + " and year " + seedYear);
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testSearchByYachtName() throws Exception {
        final CertificateHandle seed = pickSeedHandle();
        final Iterable<CertificateHandle> handles = db.search(/* country */ null, /* year */ null,
                /* referenceNumber */ null, seed.getYachtName(), /* sailNumber */ null, /* boatClassName */ null);
        assertTrue(!Util.isEmpty(handles), "Expected at least one active certificate for yacht name "
                + seed.getYachtName());
        boolean found = false;
        for (final CertificateHandle handle : handles) {
            found = found || seed.getReferenceNumber().equals(handle.getReferenceNumber());
        }
        assertTrue(found, "Expected the seed reference number " + seed.getReferenceNumber()
                + " among the results for yacht name " + seed.getYachtName());
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testCertificateHandleAPI() throws Exception {
        final CertificateHandle seed = pickSeedHandle();
        final CertificateHandle handle = db.getCertificateHandle(seed.getReferenceNumber());
        assertNotNull(handle, "No certificate handle for reference number " + seed.getReferenceNumber());
        assertEquals(seed.getReferenceNumber(), handle.getReferenceNumber());
        assertNotNull(handle.getIssueDate(), "Certificate handle for " + seed.getReferenceNumber()
                + " carries no issue date");
        assertTrue(handle.getIssueDate().asMillis() > 0, "Certificate handle for " + seed.getReferenceNumber()
                + " carries a non-positive issue date");
    }
}
