package com.sap.sailing.domain.orc;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateFamily;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateHandle;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CountryOverview;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
import com.sap.sse.common.Util;

/***
 * A {@link FailIfNoValidOrcCertificateRule} execution depends on the {@link FailIfNoValidOrcCertificates} annotation on
 * the test method. Before executing an annotated test method, this extension checks whether at least one certificate
 * could be probed from the ORC public certificate database. If at least one certificate is available the test method is
 * executed; otherwise the test is skipped via a JUnit assumption, because the unlikely corner case may apply where
 * temporarily at the beginning of a new calendar year no valid certificate exists at all in the ORC database.
 * <p>
 * The probe is performed once in the static initializer. If it fails because the {@code ORC_API_KEY} environment
 * variable is not configured, the resulting {@link java.io.IOException} is captured and re-thrown from
 * {@link #beforeTestExecution(ExtensionContext)} for {@link FailIfNoValidOrcCertificates annotated} test methods so
 * that the suite FAILS loudly on misconfiguration rather than being silently skipped. Test methods that do not carry
 * the annotation (such as pure offline parsing tests) are unaffected by a missing key and run regardless.
 *
 * @author Usman Ali
 *
 */

public class FailIfNoValidOrcCertificateRule implements BeforeTestExecutionCallback {
    private static final Logger logger = Logger.getLogger(FailIfNoValidOrcCertificateRule.class.getName());
    private static final int NUMBER_OF_CERTIFICATES_TO_PROBE = 3;

    private static final List<ORCCertificate> availableCerts;
    private static final boolean certificateExists;
    private static final RuntimeException probeFailure;

    static {
        final List<ORCCertificate> probedCerts = new ArrayList<>();
        boolean probedCertificateExists = false;
        RuntimeException capturedProbeFailure = null;
        final ORCPublicCertificateDatabase db = new ORCPublicCertificateDatabaseImpl();
        try {
            final CountryOverview countryWithMostValidCertificates = StreamSupport
                    .stream(db.getCountriesWithValidCertificates().spliterator(), /* parallel */ false)
                    .max((c1, c2) -> c1.getCertCount() - c2.getCertCount()).get();
            final Iterable<CertificateHandle> certificateHandles = Util.filter(
                    db.search(countryWithMostValidCertificates.getIssuingCountry(),
                            countryWithMostValidCertificates.getVPPYear(), null, null, null, null),
                    certHandle -> certHandle.getFamily() != CertificateFamily.ORC_LIGHT); // exclude LITE certificates
            final List<CertificateHandle> randomSubset = new ArrayList<>();
            Util.addAll(certificateHandles, randomSubset);
            Collections.shuffle(randomSubset);
            final int probeCount = Math.min(NUMBER_OF_CERTIFICATES_TO_PROBE, randomSubset.size());
            final Iterable<ORCCertificate> orcCertificates = db.getCertificates(randomSubset.subList(0, probeCount));
            orcCertificates.forEach(probedCerts::add);
            if (orcCertificates.iterator().hasNext()) {
                probedCertificateExists = true;
            }
        } catch (final java.io.IOException ex) {
            logger.log(Level.WARNING, "Problem trying to fetch certificates for tests", ex);
            capturedProbeFailure = new IllegalStateException(
                    "Unable to access the ORC public certificate API; is the ORC_API_KEY environment variable set?",
                    ex);
        } catch (final Exception ex) {
            logger.log(Level.WARNING, "Problem trying to fetch certificates for tests", ex);
        }
        availableCerts = probedCerts;
        certificateExists = probedCertificateExists;
        probeFailure = capturedProbeFailure;
    }

    public FailIfNoValidOrcCertificateRule() {
        logger.info("FailIfNoValidOrcCertificateRule created");
    }

    public static Collection<ORCCertificate> getAvailableCerts() {
        return Collections.unmodifiableCollection(availableCerts);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        final Optional<AnnotatedElement> testElement = context.getElement();
        final boolean hasAnnotation = testElement
                .map(el -> el.isAnnotationPresent(FailIfNoValidOrcCertificates.class))
                .orElse(false);
        if (hasAnnotation) {
            if (probeFailure != null) {
                throw probeFailure;
            } else if (!certificateExists) {
                logger.warning("No certificates found. Are we at the beginning of a new year (January)? Then this may be okay. Otherwise, please check what's up!");
                Assumptions.assumeTrue(false, "Skipping test: no valid ORC certificates found.");
            }
        }
    }
}
