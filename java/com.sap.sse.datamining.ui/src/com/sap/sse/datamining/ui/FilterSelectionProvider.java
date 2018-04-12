package com.sap.sse.datamining.ui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;

public interface FilterSelectionProvider
        extends DataMiningComponentProvider<SerializableSettings>, DataRetrieverChainDefinitionChangedListener {

    public void addSelectionChangedListener(FilterSelectionChangedListener listener);

    public HashMap<DataRetrieverLevelDTO, HashMap<FunctionDTO, HashSet<? extends Serializable>>> getSelection();

    public void clearSelection();

    void setHighestRetrieverLevelWithFilterDimension(FunctionDTO dimension, Serializable groupKey);

}