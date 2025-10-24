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
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateHandle;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CountryOverview;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
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
    
    private static List<ORCCertificate> availableCerts;
    private static boolean certificateExists;
    
    static {
        certificateExists = false;
        availableCerts = new ArrayList<>();
        final ORCPublicCertificateDatabase db = new ORCPublicCertificateDatabaseImpl();
        CountryOverview countryWithMostValidCertificates;
        try {
            countryWithMostValidCertificates = StreamSupport
                    .stream(db.getCountriesWithValidCertificates().spliterator(), /* parallel */ false)
                    .max((c1, c2) -> c1.getCertCount() - c2.getCertCount()).get();
            Iterable<CertificateHandle> certificateHandles = db.search(countryWithMostValidCertificates.getIssuingCountry(),
                    countryWithMostValidCertificates.getVPPYear(), null, null, null, null, /* includeInvalid */ false);
            final List<CertificateHandle> randomSubset = new ArrayList<>();
            Util.addAll(certificateHandles, randomSubset);
            Collections.shuffle(randomSubset);
            Iterable<ORCCertificate> orcCertificates = db.getCertificates(randomSubset.subList(0, NUMBER_OF_CERTIFICATES_TO_PROBE));
            orcCertificates.forEach(availableCerts::add);
            if (orcCertificates.iterator().hasNext()) {
                certificateExists = true;
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Problem trying to fetch certificates tests");
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