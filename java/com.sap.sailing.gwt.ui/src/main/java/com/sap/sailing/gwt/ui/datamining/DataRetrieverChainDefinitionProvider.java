package com.sap.sailing.gwt.ui.datamining;

import java.util.HashMap;

import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.gwt.client.shared.components.CompositeSettings;

public interface DataRetrieverChainDefinitionProvider extends DataMiningComponentProvider<CompositeSettings> {
    
    public DataRetrieverChainDefinitionDTO getDataRetrieverChainDefinition();

    public void addDataRetrieverChainDefinitionChangedListener(DataRetrieverChainDefinitionChangedListener listener);
    
    public void applyQueryDefinition(StatisticQueryDefinitionDTO queryDefinition);

    /**
     * @return The settings of the {@link #getDataRetrieverChainDefinition() current retriever definition} or an 
     *         empty Map, if the retriever definition doesn't have any settings;
     */
    public HashMap<DataRetrieverLevelDTO, SerializableSettings> getRetrieverSettings();

}
