package com.sap.sailing.domain.orc;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateFamily;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateHandle;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.CountryCodeFactory;
import com.sap.sse.common.Util;

/***
 * An {@link IgnoreInvalidOrcCerticatesRule} execution depends on
 * {@link FailIfNoValidOrcCertificates} annotation on any method in a test class containing
 * {@link org.junit.rules.TestRule} annotation with current class implementation. When any test class added
 * {@link FailIfNoValidOrcCertificateRule} rule then before executing all of it's test method, Junit will execute the
 * evaluate() method of {@link FailIfNoValidOrcCertificateRule} class. This method first check whether the current test
 * method contains the {@link FailIfNoValidOrcCertificates} annotation, if yes then it will check for any certificate
 * available to parse on ORC Website. If at least one certificate is available then this method continues execution of
 * the current test method; otherwise it will let the test fail because the searching for certificates may be broken, or
 * the unlikely corner case applies where temporarily at the beginning of a new calendar year no valid certificate
 * exists at all in the ORC database.
 * 
 * @author Usman Ali
 *
 */

public class FailIfNoValidOrcCertificateRule implements BeforeTestExecutionCallback {
    private static final Logger logger = Logger.getLogger(FailIfNoValidOrcCertificateRule.class.getName());
    private static final int NUMBER_OF_CERTIFICATES_TO_PROBE = 3;
    private static final String PROBE_COUNTRY_IOC_CODE = "GER";
    
    private static List<ORCCertificate> availableCerts;
    private static boolean certificateExists;
    
    static {
        certificateExists = false;
        availableCerts = new ArrayList<>();
        final ORCPublicCertificateDatabase db = new ORCPublicCertificateDatabaseImpl();
        try {
            final CountryCode probeCountry = CountryCodeFactory.INSTANCE
                    .getFromThreeLetterIOCName(PROBE_COUNTRY_IOC_CODE);
            final Iterable<CertificateHandle> certificateHandles = Util.filter(
                    db.search(probeCountry, /* yearOfIssuance */ null, /* referenceNumber */ null,
                            /* yachtName */ null, /* sailNumber */ null, /* boatClassName */ null,
                            /* includeInvalid */ false),
                    certHandle -> certHandle.getFamily() != CertificateFamily.ORC_LIGHT); // exclude LITE certificates
            final List<CertificateHandle> randomSubset = new ArrayList<>();
            Util.addAll(certificateHandles, randomSubset);
            Collections.shuffle(randomSubset);
            final List<CertificateHandle> probeHandles = randomSubset.subList(0,
                    Math.min(NUMBER_OF_CERTIFICATES_TO_PROBE, randomSubset.size()));
            final Iterable<ORCCertificate> orcCertificates = db.getCertificates(probeHandles);
            orcCertificates.forEach(availableCerts::add);
            if (orcCertificates.iterator().hasNext()) {
                certificateExists = true;
            }
        } catch (final Exception ex) {
            logger.log(Level.WARNING, "Problem trying to fetch certificates tests", ex);
        }
    }

    
    public FailIfNoValidOrcCertificateRule() {
        logger.info("FailIfNoValidOrcCertificateRule created");
    }

    public static Collection<ORCCertificate> getAvailableCerts() {
        return Collections.unmodifiableCollection(availableCerts);
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        Optional<AnnotatedElement> testElement = context.getElement();
        final boolean hasAnnotation = testElement
                .map(el -> el.isAnnotationPresent(FailIfNoValidOrcCertificates.class))
                .orElse(false);
        if (hasAnnotation && !certificateExists) {
            logger.warning("No certificates found. Are we at the beginning of a new year (January)? Then this may be okay. Otherwise, please check what's up!");
            Assumptions.assumeTrue(false, "Skipping test: no valid ORC certificates found.");
        }
    }
}
