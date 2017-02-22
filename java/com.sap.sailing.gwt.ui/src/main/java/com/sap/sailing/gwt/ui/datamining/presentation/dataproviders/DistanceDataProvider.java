package com.sap.sailing.gwt.ui.datamining.presentation.dataproviders;

import java.util.LinkedHashMap;
import java.util.function.Function;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;

public class DistanceDataProvider extends AbstractNumericDataProvider<Distance> {
    
    private static final String CENTRAL_ANGLE_RADIAN = "CentralAngleRadian";
    private static final String CENTRAL_ANGLE_DEGREE = "CentralAngleDegree";
    private static final String KILOMETERS = "Kilometers";
    private static final String METERS = "Meters";
    private static final String NAUTICAL_MILES = "NauticalMiles";
    private static final String SEA_MILES = "SeaMiles";
    private static final String GEOGRAPHICAL_MILES = "GeographicalMiles";

    public DistanceDataProvider() {
        super(Distance.class, getMappings());
    }
    
    private static LinkedHashMap<String, Function<Distance, Number>> getMappings() {
        LinkedHashMap<String, Function<Distance, Number>> mappings = new LinkedHashMap<>();
        mappings.put(GEOGRAPHICAL_MILES, distance->distance.getGeographicalMiles());
        mappings.put(SEA_MILES, distance->distance.getSeaMiles());
        mappings.put(NAUTICAL_MILES, distance->distance.getNauticalMiles());
        mappings.put(METERS, distance->distance.getMeters());
        mappings.put(KILOMETERS, distance->distance.getKilometers());
        mappings.put(CENTRAL_ANGLE_DEGREE, distance->distance.getCentralAngleDeg());
        mappings.put(CENTRAL_ANGLE_RADIAN, distance->distance.getCentralAngleRad());
        return mappings;
    }

    @Override
    public String getDefaultDataKeyFor(QueryResultDTO<?> result) {
        return METERS;
    }

    @Override
    public String getLocalizedNameForDataKey(StringMessages stringMessages, String dataKey) {
        switch (dataKey) {
        case CENTRAL_ANGLE_RADIAN:
            return stringMessages.centralAngleInRadian();
        case CENTRAL_ANGLE_DEGREE:
            return stringMessages.centralAngleInDegree();
        case KILOMETERS:
            return stringMessages.kilometers();
        case METERS:
            return stringMessages.meters();
        case NAUTICAL_MILES:
            return stringMessages.nauticalMiles();
        case SEA_MILES:
            return stringMessages.seaMiles();
        case GEOGRAPHICAL_MILES:
            return stringMessages.geographicalMiles();
        }
        throw new IllegalArgumentException("The given data key '" + dataKey + "' isn't valid");
    }
}
