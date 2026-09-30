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
import com.sap.sailing.polars.jaxrs.client.FileBasedPolarDataClient;

public class PolarDataResourceTest {
    private static final Logger logger = Logger.getLogger(PolarDataResourceTest.class.getName());
    private static final double[] SPEED_FUNCTIONS_COEFFS_DOWNWIND = 
            new double[]{ 0, 1.2454692253503623, 0.13300922896236855, -0.006811462395111101 };
    private static final double[] ANGLE_FUNCTION_COEFFS_DOWNWIND = 
            new double[]{ 146.08526498113133, -8.612243308300094, 1.2273401844320233, -0.0411405074199962 };
    private static final String BOAT_CLASS = "GC32";

    private PolarDataServiceImpl polarService;
    private DomainFactory domainFactory;

    @BeforeEach
    public void setUp() throws IOException, ParseException, ClassNotFoundException, InterruptedException {
        polarService = new PolarDataServiceImpl();
        domainFactory = new DomainFactoryImpl(/* raceLogResolver */ null);
        final FileBasedPolarDataClient client = new FileBasedPolarDataClient(new File("resources/polar_data"), polarService, domainFactory);
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
     * Test to check if client importing data correctly. Using {@link FileBasedPolarDataClient} which use {@link File}
     * polar_data.json as source
     * 
     * @throws NotEnoughDataHasBeenAddedException
     */
    @Test
    public void testImportingFromMockFile() throws NotEnoughDataHasBeenAddedException {
        BoatClass boatClass = domainFactory.getOrCreateBoatClass(BOAT_CLASS);
        PolynomialFunction angleDownwindFunction = new PolynomialFunction(ANGLE_FUNCTION_COEFFS_DOWNWIND);
        PolynomialFunction speedDownwindFunction = new PolynomialFunction(SPEED_FUNCTIONS_COEFFS_DOWNWIND);
        assertThat(polarService.getSpeedRegressionsPerAngle().size(), is(36));
        assertThat(polarService.getCubicRegressionsPerCourse().size(), is(2));
        assertThat(polarService.getFixCountPerBoatClass().get(boatClass), is(248239L));
        // presuming that if downwind functions & regression collections' size are correct then any other thing is
        // imported correctly
        assertThat(polarService.getAngleRegressionFunction(boatClass, LegType.DOWNWIND), is(angleDownwindFunction));
        assertThat(polarService.getSpeedRegressionFunction(boatClass, LegType.DOWNWIND), is(speedDownwindFunction));
    }
}