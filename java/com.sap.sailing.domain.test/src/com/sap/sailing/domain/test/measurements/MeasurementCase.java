package com.sap.sailing.domain.test.measurements;

import java.util.HashSet;
import java.util.Set;

/**
 * Obtain an instance of this class by invoking {@link MeasurementXMLFile#addCase(String)}, then add
 * measurements using the {@link #addMeasurement(Measurement)} method.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class MeasurementCase {
    private final String name;
    private final Set<Measurement> measurements;

    MeasurementCase(String name) {
        super();
        this.name = name;
        measurements = new HashSet<>();
    }
    
    String getName() {
        return name;
    }

    public void addMeasurement(Measurement measurement) {
        measurements.add(measurement);
    }
    
    Iterable<Measurement> getMeasurements() {
        return measurements;
    }
}
