package com.sap.sailing.polars.jaxrs.api.test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.polars.impl.PolarDataServiceImpl;

public class PolarDataResourceTest {
    private static final Logger logger = Logger.getLogger(PolarDataResourceTest.class.getName());
    private static final double[] SPEED_FUNCTIONS_COEFFS_DOWNWIND = 
            new double[]{ 0, 2.2378615205516326, -0.0801870828343283, 9.81959605693472E-4 };
    private static final double[] ANGLE_FUNCTION_COEFFS_DOWNWIND = 
            new double[]{ 138.3296034098894, -5.908558698662091, 1.1870404653964215, -0.056603488833331994 };
    private static final String BOAT_CLASS = "GC32";

    private PolarDataServiceImpl polarService;
    private DomainFactory domainFactory;

    @BeforeEach
    public void setUp() throws IOException, ParseException, ClassNotFoundException, InterruptedException {
        polarService = new PolarDataServiceImpl();
        domainFactory = new DomainFactoryImpl(/* raceLogResolver */ null);
        final PolarDataClientMock client = new PolarDataClientMock(new File("resources/polar_data"), polarService, domainFactory);
        client.updatePolarDataRegressions();
        // ensure that setting the domain factory has worked
        polarService.runWithDomainFactory(domainFactory -> { 
            try {
                client.updatePolarDataRegressions();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception while trying to import polar data from file during test", e);
            }
        });
    }

    /**
     * Test to check if client importing data correctly. Using {@link PolarDataClientMock} which use {@link File}
     * polar_data.json as source
     * 
     * @throws NotEnoughDataHasBeenAddedException
     */
    @Test
    public void testImportingFromMockFile() throws NotEnoughDataHasBeenAddedException {
        BoatClass boatClass = domainFactory.getOrCreateBoatClass(BOAT_CLASS);
        PolynomialFunction angleDownwindFunction = new PolynomialFunction(ANGLE_FUNCTION_COEFFS_DOWNWIND);
        PolynomialFunction speedDownwindFunction = new PolynomialFunction(SPEED_FUNCTIONS_COEFFS_DOWNWIND);

        assertThat(polarService.getSpeedRegressionsPerAngle().size(), is(68));
        assertThat(polarService.getCubicRegressionsPerCourse().size(), is(4));
        assertThat(polarService.getFixCountPerBoatClass().get(boatClass), is(9330L));
        // presuming that if downwind functions & regression collections' size are correct then any other thing is
        // imported correctly
        assertThat(polarService.getAngleRegressionFunction(boatClass, LegType.DOWNWIND), is(angleDownwindFunction));
        assertThat(polarService.getSpeedRegressionFunction(boatClass, LegType.DOWNWIND), is(speedDownwindFunction));
    }

}