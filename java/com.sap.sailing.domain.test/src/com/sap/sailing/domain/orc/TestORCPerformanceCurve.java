package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveCourseImpl;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.orc.impl.ORCCertificatesJsonImporter;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveImpl;
import com.sap.sailing.domain.orc.impl.ORCPublicCertificateDatabaseImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * 
 * @author Daniel Lisunkin {i505543)
 *
 */
@ExtendWith(FailIfNoValidOrcCertificateRule.class)
public class TestORCPerformanceCurve {
    private static final Logger logger = Logger.getLogger(TestORCPerformanceCurve.class.getName());
    
    private static ORCPerformanceCurveCourse alturaCourse;
    private static ORCCertificatesCollection importerLocal;
    private static ORCCertificatesCollection importerWithSpecificBins;
    private static ORCCertificatesCollection importerOnline;
    
    private static final String RESOURCES = "resources/orc/";
    
    @BeforeAll
    public static void initialize() throws IOException, ParseException, DOMException, SAXException,
            ParserConfigurationException, java.text.ParseException {
        List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2.23), new DegreeBearingImpl(10)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2.00), new DegreeBearingImpl(170)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(0.97), new DegreeBearingImpl(0)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.03), new DegreeBearingImpl(15)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.03), new DegreeBearingImpl(165)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.17), new DegreeBearingImpl(180)));
        alturaCourse = new ORCPerformanceCurveCourseImpl(legs); // this course is the same course as seen in the Altura "IMS Explanation" sheet
        // Local File:
        File fileGER = new File(RESOURCES + "GER2019.json");
        importerLocal = new ORCCertificatesJsonImporter().read(new FileInputStream(fileGER));
        // Local File new format with specific TWA/TWS bins:
        File fileWithSpecificBins = new File(RESOURCES + "newFormatCertificate.json");
        importerWithSpecificBins = new ORCCertificatesJsonImporter().read(new FileInputStream(fileWithSpecificBins));
        // Online File:
        final CountryCode countryWithMostValidCertificates = StreamSupport
                .stream(ORCPublicCertificateDatabaseImpl.INSTANCE.getCountriesWithValidCertificates().spliterator(),
                        /* parallel */ false)
                .max((c1, c2) -> c1.getCertCount() - c2.getCertCount()).get().getIssuingCountry();
        logger.info("Trying to read country certificates for "+countryWithMostValidCertificates);
        importerOnline = new ORCCertificatesJsonImporter().read(new URL("https://data.orc.org/public/WPub.dll?action=DownBoatRMS&CountryId="+
                countryWithMostValidCertificates.getThreeLetterIOCCode()+"&ext=json").openStream());
    }
    
    @Test
    public void testLagrangeInterpolation60() throws FunctionEvaluationException {
        ORCCertificate certificate = importerLocal.getCertificateById("GER20040783");
        testAllowancePerLeg(certificate, 60.0, 498.4);
    }
    
    @Test
    public void testLagrangeInterpolation62_5() throws FunctionEvaluationException {
        ORCCertificate certificate = importerLocal.getCertificateById("GER20040783");
        testAllowancePerLeg(certificate, 62.5, 492.2, 425.0, 403.6, 393.1, 386.7, 382.6, 371.4);
    }
    
    @Test
    public void testLagrangeInterpolation98_3() throws FunctionEvaluationException {
        ORCCertificate certificate = importerLocal.getCertificateById("GER20040783");
        testAllowancePerLeg(certificate, 98.3, 483.6, 418.7, 394.8, 377.9, 360.2, 345.0, 321.7);
    }
    
    @Test
    public void testLagrangeInterpolation120() throws FunctionEvaluationException {
        ORCCertificate certificate = importerLocal.getCertificateById("GER20040783");
        testAllowancePerLeg(certificate, 120.0, 506);
    }
    
    @Test
    public void testLagrangeInterpolation138_7() throws FunctionEvaluationException {
        ORCCertificate certificate = importerLocal.getCertificateById("GER20040783");
        testAllowancePerLeg(certificate, 138.7, 588.1, 468.4, 413.8, 382.6, 355.1, 326.4, 275.4);
    }
    
    private void testAllowancePerLeg(ORCCertificate certificate, double twa, double... expectedAllowancesPerTrueWindSpeed) throws FunctionEvaluationException {
        Double accuracy = 0.1;
        final ORCPerformanceCurveCourse course = new ORCPerformanceCurveCourseImpl(Arrays.asList(new ORCPerformanceCurveLegImpl(ORCCertificate.NAUTICAL_MILE, new DegreeBearingImpl(twa))));
        final ORCPerformanceCurve performanceCurve = new ORCPerformanceCurveImpl(certificate, course);
        for (int i=0; i<expectedAllowancesPerTrueWindSpeed.length; i++) {
            assertEquals(expectedAllowancesPerTrueWindSpeed[i], performanceCurve.getAllowancePerCourse(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS[i]).asSeconds(), accuracy);
        }
    }
    
    @Test
    public void testSimpleConstructedCourse() throws FunctionEvaluationException {
        ORCCertificate certificate = (ORCCertificate) importerLocal.getCertificateById("GER20040783");
        List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.0), new DegreeBearingImpl(0)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.0), new DegreeBearingImpl(30)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.0), new DegreeBearingImpl(60)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.0), new DegreeBearingImpl(120)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.0), new DegreeBearingImpl(180)));
        ORCPerformanceCurveCourse simpleCourse = new ORCPerformanceCurveCourseImpl(legs);
        ORCPerformanceCurve performanceCurve = new ORCPerformanceCurveImpl(certificate, simpleCourse);
        assertNotNull(performanceCurve);
        testAllowancePerCourse(performanceCurve, 654.4, 538.6, 485.1, 458.7, 444.1, 430.3, 404.3);
    }

    private void testAllowancePerCourse(ORCPerformanceCurve performanceCurve, double... allowancePerNauticalMileInSeconds) throws ArgumentOutsideDomainException {
        for (int i=0; i<allowancePerNauticalMileInSeconds.length; i++) {
            assertEquals(allowancePerNauticalMileInSeconds[i]*performanceCurve.getCourse().getTotalLength().getNauticalMiles(),
                    performanceCurve.getAllowancePerCourse(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS[i]).asSeconds(), 0.3);
        }
    }

    @Test
    public void testPerformanceCurveInversion() throws MaxIterationsExceededException, FunctionEvaluationException {
        Double accuracy = 0.1;
        ORCCertificate certificate = (ORCCertificate) importerLocal.getCertificateById("GER20040783");
        ORCPerformanceCurveImpl performanceCurve = (ORCPerformanceCurveImpl) new ORCPerformanceCurveImpl(certificate, alturaCourse);
        testBackwardForward(accuracy, performanceCurve, 11.5);
        testBackwardForward(accuracy, performanceCurve, 17.23);
        testBackwardForward(accuracy, performanceCurve, 18);
        testForwardBackward(accuracy, performanceCurve, 450);
        testForwardBackward(accuracy, performanceCurve, 500);
        testForwardBackward(accuracy, performanceCurve, 600);
        testForwardBackward(accuracy, performanceCurve, 700);
        testForwardBackward(accuracy, performanceCurve, 750);
    }

    private void testBackwardForward(Double accuracy, ORCPerformanceCurveImpl performanceCurve, final double impliedWindInKnots)
            throws MaxIterationsExceededException, FunctionEvaluationException, ArgumentOutsideDomainException {
        assertEquals(impliedWindInKnots, performanceCurve.getImpliedWind(performanceCurve.getAllowancePerCourse(new KnotSpeedImpl( impliedWindInKnots))).getKnots(), accuracy);
    }

    private void testForwardBackward(Double accuracy, ORCPerformanceCurveImpl performanceCurve, final double secondsPerNauticalMile)
            throws ArgumentOutsideDomainException, MaxIterationsExceededException, FunctionEvaluationException {
        final double secondsForCourse = secondsPerNauticalMile*alturaCourse.getTotalLength().getNauticalMiles();
        assertEquals(secondsForCourse,
                performanceCurve.getAllowancePerCourse(performanceCurve.getImpliedWind(
                        Duration.ONE_SECOND.times(secondsForCourse))).asSeconds(), accuracy);
    }
    
    // Tests for a Implied Wind calculation for a simple predefined course. The solutions are extracted from the provided ORC TestPCS.exe application.
    @Test
    public void testImpliedWind() throws MaxIterationsExceededException, FunctionEvaluationException {
       double accuracy = 0.0001;
       ORCCertificate certificateMoana          = importerLocal.getCertificateById("GER20040783");
       ORCCertificate certificateMilan          = importerLocal.getCertificateById("GER20041179");
       ORCCertificate certificateTutima         = importerLocal.getCertificateById("GER30010194");
       ORCCertificate certificateBank           = importerLocal.getCertificateById("GER20040647");
       ORCCertificate certificateHaspa          = importerLocal.getCertificateById("GER30010167");
       ORCCertificate certificateHalbtrocken    = importerLocal.getCertificateById("GER20040632");
       ORCPerformanceCurve performanceCurveMoana        = new ORCPerformanceCurveImpl(certificateMoana, alturaCourse);
       ORCPerformanceCurve performanceCurveMilan        = new ORCPerformanceCurveImpl(certificateMilan, alturaCourse);
       ORCPerformanceCurve performanceCurveTutima       = new ORCPerformanceCurveImpl(certificateTutima, alturaCourse);
       ORCPerformanceCurve performanceCurveBank         = new ORCPerformanceCurveImpl(certificateBank, alturaCourse);
       ORCPerformanceCurve performanceCurveHaspa        = new ORCPerformanceCurveImpl(certificateHaspa, alturaCourse);
       ORCPerformanceCurve performanceCurveHalbtrocken  = new ORCPerformanceCurveImpl(certificateHalbtrocken, alturaCourse);
       // Test for corner case and if the algorithm reacts to the boundaries of 6 and 20 kts.
       assertEquals( 6.0    , performanceCurveMoana.getImpliedWind(Duration.ONE_HOUR.times(24)).getKnots(), accuracy);
       assertEquals(20.0    , performanceCurveMoana.getImpliedWind(Duration.ONE_HOUR.divide(24)).getKnots(), accuracy);
       assertEquals(1.0, performanceCurveMilan.getAllowancePerCourse(new KnotSpeedImpl(12.80881)).asHours(), accuracy); 
       // scratch sheets and implied wind as calculated by Altura for course1 and 1:00:00 / 1:30:00 time sailed, respectively:
       //               6kts    8kts    10kts   12kts   14kts   16kts   20kts   implied wind    Altura          ORC Scorer      ORC PCS Test    SAP
       // Milan:        675.2   539.5   473.1   437.6   412.7   388.8   350.8                   12.8091135      12.80881        12.80881        12.809089
       // Moana:        775.7   627.5   549.9   512.4   493.3   473.1   435.0                   7.76029797      7.76218         7.76218         7.7602936
       
       // scratch sheet as computed by ORC Scorer:
       //               6kts    8kts    10kts   12kts   14kts   16kts   20kts
       // Milan:        675.2   539.5   473.1   437.6   412.7   388.8   350.8
       // Moana:        775.7   627.5   549.9   512.4   493.3   473.1   435.0
       assertEquals(12.80881 , performanceCurveMilan.getImpliedWind(Duration.ONE_HOUR.times(1.0)).getKnots(), accuracy);
       assertEquals(8.72668  , performanceCurveTutima     .getImpliedWind(Duration.ONE_HOUR.times(1.5)).getKnots(), accuracy);
       assertEquals(8.07591  , performanceCurveBank       .getImpliedWind(Duration.ONE_HOUR.times(1.5)).getKnots(), accuracy);
       assertEquals(7.78413  , performanceCurveHaspa      .getImpliedWind(Duration.ONE_HOUR.times(1.5)).getKnots(), accuracy);
       assertEquals(7.76218  , performanceCurveMoana      .getImpliedWind(Duration.ONE_HOUR.times(1.5)).getKnots(), accuracy);
       assertEquals(7.62407  , performanceCurveHalbtrocken.getImpliedWind(Duration.ONE_HOUR.times(2.0)).getKnots(), accuracy);
    }
    
    // Tests for a Allowance calculation for a simple predefined course and given Implied Winds. The solutions are extracted from the provided ORC TestPCS.exe
    @Test
    public void testAllowances() throws FunctionEvaluationException {
        double accuracy = 0.00001;
        ORCCertificate certificateMoana          = importerLocal.getCertificateById("GER20040783");
        ORCCertificate certificateMilan          = importerLocal.getCertificateById("GER20041179");
        ORCCertificate certificateTutima         = importerLocal.getCertificateById("GER30010194");
        ORCCertificate certificateBank           = importerLocal.getCertificateById("GER20040647");
        ORCCertificate certificateHaspa          = importerLocal.getCertificateById("GER30010167");
        ORCCertificate certificateHalbtrocken    = importerLocal.getCertificateById("GER20040632");
        ORCPerformanceCurve performanceCurveMoana        = new ORCPerformanceCurveImpl(certificateMoana, alturaCourse);
        ORCPerformanceCurve performanceCurveMilan        = new ORCPerformanceCurveImpl(certificateMilan, alturaCourse);
        ORCPerformanceCurve performanceCurveTutima       = new ORCPerformanceCurveImpl(certificateTutima, alturaCourse);
        ORCPerformanceCurve performanceCurveBank         = new ORCPerformanceCurveImpl(certificateBank, alturaCourse);
        ORCPerformanceCurve performanceCurveHaspa        = new ORCPerformanceCurveImpl(certificateHaspa, alturaCourse);
        ORCPerformanceCurve performanceCurveHalbtrocken  = new ORCPerformanceCurveImpl(certificateHalbtrocken, alturaCourse);
        assertEquals(Duration.ONE_HOUR.times(1.0).asHours(), performanceCurveMilan.getAllowancePerCourse(new KnotSpeedImpl(12.80881)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.5).asHours(), performanceCurveTutima.getAllowancePerCourse(new KnotSpeedImpl(8.72668)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.5).asHours(), performanceCurveBank.getAllowancePerCourse(new KnotSpeedImpl(8.07591)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.5).asHours(), performanceCurveHaspa.getAllowancePerCourse(new KnotSpeedImpl(7.78413)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.5).asHours(), performanceCurveMoana.getAllowancePerCourse(new KnotSpeedImpl(7.76218)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(2.0).asHours(), performanceCurveHalbtrocken.getAllowancePerCourse(new KnotSpeedImpl(7.62407)).asHours(), accuracy);
   }
    
    /**
     * Tests to make sure, that the structure of the certificate files didn't change and polar curves can be built
     */
    @FailIfNoValidOrcCertificates
    @Test
    public void testOnlineImport() throws FunctionEvaluationException {
        assertFalse(Util.isEmpty(importerOnline.getCertificateIds()));
        for (final ORCCertificate certificate : importerOnline.getCertificates()) {
            new ORCPerformanceCurveImpl(certificate, alturaCourse);
        }
    }
    
    @Test
    public void testAllowancesAndImpliedWindForSpecificBins() throws FunctionEvaluationException, MaxIterationsExceededException {
        final double highAccuracy = 0.01;
        final double allowanceAccuracy = 0.1;
        final Distance ONE_NAUTICAL_MILE = new NauticalMileDistance(1.0);
        final ORCCertificate certificateWithSpecificBins = importerWithSpecificBins.getCertificateById("N/A");
        final List<Executable> assertions = new ArrayList<>();
        for (final Bearing twa : certificateWithSpecificBins.getTrueWindAngles()) {
            final ORCPerformanceCurveCourse singleLegOneMileCourseWithTwa = createSingleLegCourseWithTwa(twa);
            final ORCPerformanceCurve performanceCurveSpecificBins = new ORCPerformanceCurveImpl(certificateWithSpecificBins, singleLegOneMileCourseWithTwa);
            for (final Speed tws : certificateWithSpecificBins.getTrueWindSpeeds()) {
                final Duration duration = certificateWithSpecificBins.getVelocityPredictionPerTrueWindSpeedAndAngle().get(tws).get(twa).getDuration(ONE_NAUTICAL_MILE);
                assertions.add(()->assertEquals(duration.asSeconds(), performanceCurveSpecificBins.getAllowancePerCourse(tws).asSeconds(), allowanceAccuracy, "mismatch for twa "+twa+", tws "+tws));
                assertions.add(()->assertEquals(tws.getKnots(), performanceCurveSpecificBins.getImpliedWind(duration).getKnots(), highAccuracy, "mismatch for twa "+twa+", tws "+tws));
            }
        }
        assertAll(assertions);
    }
    
    private ORCPerformanceCurveCourse createSingleLegCourseWithTwa(Bearing twa) {
        List<ORCPerformanceCurveLeg> list = new ArrayList<>();
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), twa));
        return new ORCPerformanceCurveCourseImpl(list);
    }
    
    // Tests for the calculations with a more complex course which contains some special leg types. (circular random or other)
    @Test
    public void testComplexCourseImpliedWind() throws FunctionEvaluationException, MaxIterationsExceededException {
        List<ORCPerformanceCurveLeg> list = new ArrayList<>();
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.5), new DegreeBearingImpl(0)));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), ORCPerformanceCurveLegTypes.WINDWARD_LEEWARD));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), ORCPerformanceCurveLegTypes.LONG_DISTANCE));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), ORCPerformanceCurveLegTypes.CIRCULAR_RANDOM));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.5), ORCPerformanceCurveLegTypes.NON_SPINNAKER));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(180)));
        ORCPerformanceCurveCourse complexCourse = new ORCPerformanceCurveCourseImpl(list);
        
        final double accuracy = 0.0001;
        ORCCertificate certificateMoana          = importerLocal.getCertificateById("GER20040783");
        ORCCertificate certificateMilan          = importerLocal.getCertificateById("GER20041179");
        ORCCertificate certificateTutima         = importerLocal.getCertificateById("GER30010194");
        ORCCertificate certificateBank           = importerLocal.getCertificateById("GER20040647");
        ORCCertificate certificateHaspa          = importerLocal.getCertificateById("GER30010167");
        ORCCertificate certificateHalbtrocken    = importerLocal.getCertificateById("GER20040632");
        ORCCertificate certificateWithSpecificBins=importerWithSpecificBins.getCertificateById("N/A");
        ORCPerformanceCurve performanceCurveMoana        = new ORCPerformanceCurveImpl(certificateMoana, complexCourse);
        ORCPerformanceCurve performanceCurveMilan        = new ORCPerformanceCurveImpl(certificateMilan, complexCourse);
        ORCPerformanceCurve performanceCurveTutima       = new ORCPerformanceCurveImpl(certificateTutima, complexCourse);
        ORCPerformanceCurve performanceCurveBank         = new ORCPerformanceCurveImpl(certificateBank, complexCourse);
        ORCPerformanceCurve performanceCurveHaspa        = new ORCPerformanceCurveImpl(certificateHaspa, complexCourse);
        ORCPerformanceCurve performanceCurveHalbtrocken  = new ORCPerformanceCurveImpl(certificateHalbtrocken, complexCourse);
        ORCPerformanceCurve performanceCurveSpecificBins = new ORCPerformanceCurveImpl(certificateWithSpecificBins, complexCourse);
        assertEquals(15.75777 , performanceCurveMilan      .getImpliedWind(Duration.ONE_HOUR.times(1.0)).getKnots(), accuracy);
        assertEquals(15.27808 , performanceCurveBank       .getImpliedWind(Duration.ONE_HOUR.times(1.25)).getKnots(), accuracy);
        assertEquals(15.10141 , performanceCurveMoana      .getImpliedWind(Duration.ONE_HOUR.times(1.25)).getKnots(), accuracy);
        assertEquals(14.44527 , performanceCurveHaspa      .getImpliedWind(Duration.ONE_HOUR.times(1.25)).getKnots(), accuracy);
        assertEquals(10.86927 , performanceCurveTutima     .getImpliedWind(Duration.ONE_HOUR.times(1.5)).getKnots(), accuracy);
        assertEquals(9.13385  , performanceCurveHalbtrocken.getImpliedWind(Duration.ONE_HOUR.times(2.0)).getKnots(), accuracy);
        assertEquals(10.83989 , performanceCurveSpecificBins.getImpliedWind(Duration.ONE_HOUR.times(2.0)).getKnots(), accuracy);
    }
    
    @Test
    public void testComplexCourseAllowances() throws FunctionEvaluationException {
        List<ORCPerformanceCurveLeg> list = new ArrayList<>();
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.5), new DegreeBearingImpl(0)));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), ORCPerformanceCurveLegTypes.WINDWARD_LEEWARD));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), ORCPerformanceCurveLegTypes.LONG_DISTANCE));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), ORCPerformanceCurveLegTypes.CIRCULAR_RANDOM));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.5), ORCPerformanceCurveLegTypes.NON_SPINNAKER));
        list.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(180)));
        ORCPerformanceCurveCourse complexCourse = new ORCPerformanceCurveCourseImpl(list);
        
        double accuracy = 0.00001;
        ORCCertificate certificateMoana          = importerLocal.getCertificateById("GER20040783");
        ORCCertificate certificateMilan          = importerLocal.getCertificateById("GER20041179");
        ORCCertificate certificateTutima         = importerLocal.getCertificateById("GER30010194");
        ORCCertificate certificateBank           = importerLocal.getCertificateById("GER20040647");
        ORCCertificate certificateHaspa          = importerLocal.getCertificateById("GER30010167");
        ORCCertificate certificateHalbtrocken    = importerLocal.getCertificateById("GER20040632");
        ORCCertificate certificateWithSpecificBins=importerWithSpecificBins.getCertificateById("N/A");
        ORCPerformanceCurve performanceCurveMoana        = new ORCPerformanceCurveImpl(certificateMoana, complexCourse);
        ORCPerformanceCurve performanceCurveMilan        = new ORCPerformanceCurveImpl(certificateMilan, complexCourse);
        ORCPerformanceCurve performanceCurveTutima       = new ORCPerformanceCurveImpl(certificateTutima, complexCourse);
        ORCPerformanceCurve performanceCurveBank         = new ORCPerformanceCurveImpl(certificateBank, complexCourse);
        ORCPerformanceCurve performanceCurveHaspa        = new ORCPerformanceCurveImpl(certificateHaspa, complexCourse);
        ORCPerformanceCurve performanceCurveHalbtrocken  = new ORCPerformanceCurveImpl(certificateHalbtrocken, complexCourse);
        ORCPerformanceCurve performanceCurveSpecificBins = new ORCPerformanceCurveImpl(certificateWithSpecificBins, complexCourse);
        
        assertEquals(Duration.ONE_HOUR.times(1.0).asHours(), performanceCurveMilan.getAllowancePerCourse(new KnotSpeedImpl(15.75777)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.25).asHours(), performanceCurveBank.getAllowancePerCourse(new KnotSpeedImpl(15.27808)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.25).asHours(), performanceCurveMoana.getAllowancePerCourse(new KnotSpeedImpl(15.10141)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.25).asHours(), performanceCurveHaspa.getAllowancePerCourse(new KnotSpeedImpl(14.44527)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(1.5).asHours(), performanceCurveTutima.getAllowancePerCourse(new KnotSpeedImpl(10.86927)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(2.0).asHours(), performanceCurveHalbtrocken.getAllowancePerCourse(new KnotSpeedImpl(9.13385)).asHours(), accuracy);
        assertEquals(Duration.ONE_HOUR.times(2.0).asHours(), performanceCurveSpecificBins.getAllowancePerCourse(new KnotSpeedImpl(10.83989)).asHours(), accuracy);
    }
    
}
