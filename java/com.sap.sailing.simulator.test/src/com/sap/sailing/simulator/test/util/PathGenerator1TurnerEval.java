package com.sap.sailing.simulator.test.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import org.junit.jupiter.api.Assertions;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.impl.PathGenerator1Turner;
import com.sap.sailing.simulator.impl.PolarDiagramCSV;
import com.sap.sailing.simulator.impl.SimulationParametersImpl;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGenerator1TurnerEval {

    private static final String RESOURCES = "resources/";

    // private static final String POLAR_DIAGRAM_49_CSV_FILE_PATH = "resources/PolarDiagram49.csv";
    // private static final String POLAR_DIAGRAM_49_BETHWAITE_CSV_FILE_PATH = "resources/PolarDiagram49Bethwaite.csv";
    // private static final String POLAR_DIAGRAM_49_ORC_CSV_FILE_PATH = "resources/PolarDiagram49ORC.csv";
    private static final String POLAR_DIAGRAM_49_STG_CSV_FILE_PATH = "resources/PolarDiagram49STG.csv";
    // private static final String POLAR_DIAGRAM_505_STG_CSV_FILE_PATH = "resources/PolarDiagram505STG.csv";

    private SimulationParameters _simulationParameters = null;
    private PolarDiagram _polarDiagram = null;
    private WindFieldGenerator _windField = null;
    private PathGenerator1Turner _pathGenerator = null;

    public static void main(String[] args) throws IOException {
        System.out.println("Collection of test methods for evaluating path segments.");
    }

    public void initialize() throws IOException, ClassNotFoundException {

        this._polarDiagram = new PolarDiagramCSV(POLAR_DIAGRAM_49_STG_CSV_FILE_PATH);
        this._simulationParameters = new SimulationParametersImpl(null, this._polarDiagram, null, null,
                SailingSimulatorConstants.ModeMeasured, true, true);
        this._pathGenerator = new PathGenerator1Turner(this._simulationParameters);

        this._windField = readWindFieldGeneratorFromExternalFile("windField.dat");
    }

    public void test_get1Turner_allCourse() {

        Position start = new DegreePosition(53.97207883355604, 10.890463283282314);
        Position end = new DegreePosition(53.967186999999996, 10.891352);
        TimePoint startTime = new MillisecondsTimePoint(1360533600140L);
        boolean leftSide = true;
        int stepMax = 300;
        long timeStep = 6666;

        TimedPositionWithSpeed result = this._pathGenerator.get1Turner(this._windField, this._polarDiagram, start, end,
                startTime, leftSide, stepMax, timeStep);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getPosition());
        Assertions.assertNotNull(result.getSpeed());
        Assertions.assertNotNull(result.getSpeed().getBearing());
        Assertions.assertNotNull(result.getTimePoint());

        Assertions.assertEquals(3.375, result.getSpeed().getKnots(), 0.001);
        Assertions.assertEquals(131.919809, result.getSpeed().getBearing().getDegrees(), 0.000001);
        Assertions.assertEquals(53.969782, result.getPosition().getLatDeg(), 0.000001);
        Assertions.assertEquals(10.894810, result.getPosition().getLngDeg(), 0.000001);
        Assertions.assertEquals(1360533820118L, result.getTimePoint().asMillis());
    }

    public void test_get1Turner_segment1() {

        Position start = new DegreePosition(53.97042597845473, 10.895004272460938);
        Position end = new DegreePosition(53.969066999999995, 10.893665);
        TimePoint startTime = new MillisecondsTimePoint(1317552751000L);
        boolean leftSide = true;
        int stepMax = 300;
        long timeStep = 1000;

        TimedPositionWithSpeed result = this._pathGenerator.get1Turner(this._windField, this._polarDiagram, start, end,
                startTime, leftSide, stepMax, timeStep);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getPosition());
        Assertions.assertNotNull(result.getSpeed());
        Assertions.assertNotNull(result.getSpeed().getBearing());
        Assertions.assertNotNull(result.getTimePoint());

        Assertions.assertEquals(3.375, result.getSpeed().getKnots(), 0.001);
        Assertions.assertEquals(131.917200, result.getSpeed().getBearing().getDegrees(), 0.000001);
        Assertions.assertEquals(53.970290, result.getPosition().getLatDeg(), 0.000001);
        Assertions.assertEquals(10.895261, result.getPosition().getLngDeg(), 0.000001);
        Assertions.assertEquals(1317552764000L, result.getTimePoint().asMillis());
    }

    public void test_get1Turner_segment2() {

        Position start = new DegreePosition(53.969066999999995, 10.893665);

        Position end = new DegreePosition(53.96954246675255, 10.894231796264648);

        TimePoint startTime = new MillisecondsTimePoint(1317552751000L);
        boolean leftSide = true;
        int stepMax = 300;
        long timeStep = 1000;

        TimedPositionWithSpeed result = this._pathGenerator.get1Turner(this._windField, this._polarDiagram, end, start,
                startTime, leftSide, stepMax, timeStep);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getPosition());
        Assertions.assertNotNull(result.getSpeed());
        Assertions.assertNotNull(result.getSpeed().getBearing());
        Assertions.assertNotNull(result.getTimePoint());

        Assertions.assertEquals(3.375, result.getSpeed().getKnots(), 0.001);
        Assertions.assertEquals(131.916561, result.getSpeed().getBearing().getDegrees(), 0.000001);
        Assertions.assertEquals(53.969532, result.getPosition().getLatDeg(), 0.000001);
        Assertions.assertEquals(10.894251, result.getPosition().getLngDeg(), 0.000001);
        Assertions.assertEquals(1317552752000L, result.getTimePoint().asMillis());
    }

    public WindFieldGenerator readWindFieldGeneratorFromExternalFile(final String fileName) {
        WindFieldGenerator result = null;

        try {
            final InputStream file = new FileInputStream(getFile(fileName));
            final InputStream buffer = new BufferedInputStream(file);
            final ObjectInput input = new ObjectInputStream(buffer);

            try {
                result = (WindFieldGenerator) input.readObject();
            } finally {
                input.close();
                buffer.close();
                file.close();
            }
        } catch (final ClassNotFoundException ex) {
            System.err.println("[ERROR][PathGenerator1Turner][readFromExternalFile][ClassNotFoundException] "
                    + ex.getMessage());
            result = null;
        } catch (final IOException ex) {
            System.err.println("[ERROR][PathGenerator1Turner][readFromExternalFile][IOException]  " + ex.getMessage());
            result = null;
        }

        return result;
    }

    private File getFile(String fileName) {
        return new File(RESOURCES + fileName);
    }

}
