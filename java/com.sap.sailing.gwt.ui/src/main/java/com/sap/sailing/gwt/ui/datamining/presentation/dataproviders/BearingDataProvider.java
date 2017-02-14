package com.sap.sailing.gwt.ui.datamining.presentation.dataproviders;

import java.util.LinkedHashMap;
import java.util.function.Function;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;

public class BearingDataProvider extends AbstractDataProvider<Bearing> {
    
    private static final String DEGREES = "Angle in Degrees";
    private static final String RADIANS = "Angle in Radians";

    public BearingDataProvider() {
        super(Bearing.class, getMappings());
    }
    
    private static LinkedHashMap<String, Function<Bearing, Number>> getMappings() {
        LinkedHashMap<String, Function<Bearing, Number>> mappings = new LinkedHashMap<>();
        mappings.put(DEGREES, bearing->bearing.getDegrees());
        mappings.put(RADIANS, bearing->bearing.getRadians());
        return mappings;
    }

    @Override
    public String getDefaultDataKeyFor(QueryResultDTO<?> result) {
        return DEGREES;
    }
}
