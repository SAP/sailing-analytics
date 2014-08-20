package com.sap.sailing.gwt.ui.datamining;

import com.sap.sailing.datamining.shared.QueryDefinitionDeprecated;
import com.sap.sailing.gwt.ui.client.shared.components.Component;
import com.sap.sse.datamining.shared.components.AggregatorType;
import com.sap.sse.datamining.shared.dto.FunctionDTO;

public interface StatisticProvider extends Component<Object> {
    
    public FunctionDTO getStatisticToCalculate();
    public AggregatorType getAggregatorType();

    public void addStatisticChangedListener(StatisticChangedListener listener);
    
    public void applyQueryDefinition(QueryDefinitionDeprecated queryDefinition);

}
