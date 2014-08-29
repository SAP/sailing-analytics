package com.sap.sailing.gwt.ui.datamining;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.sap.sailing.gwt.ui.client.shared.components.Component;
import com.sap.sse.datamining.shared.QueryDefinition;
import com.sap.sse.datamining.shared.dto.FunctionDTO;

public interface SelectionProvider<SettingsType> extends Component<SettingsType> {

    public void addSelectionChangedListener(SelectionChangedListener listener);

    public Map<FunctionDTO, Collection<? extends Serializable>> getFilterSelection();

    public void applySelection(QueryDefinition queryDefinition);

    public void clearSelection();

}