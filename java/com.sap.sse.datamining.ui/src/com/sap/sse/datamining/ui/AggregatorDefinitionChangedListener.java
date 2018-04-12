package com.sap.sse.datamining.ui;

import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;

public interface AggregatorDefinitionChangedListener {

    public void aggregatorDefinitionChanged(AggregationProcessorDefinitionDTO newAggregatorDefinition);

}
