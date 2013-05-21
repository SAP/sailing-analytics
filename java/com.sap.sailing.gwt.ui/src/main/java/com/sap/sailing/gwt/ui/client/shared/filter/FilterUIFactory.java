package com.sap.sailing.gwt.ui.client.shared.filter;

import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.DataEntryDialog;

/**
 * A factory interface for creating UI input widgets for a filter  
 * @author Frank
 */
public interface FilterUIFactory<FilterObjectType> {
    Widget createFilterUIWidget(DataEntryDialog<?> dataEntryDialog);

    FilterWithUI<FilterObjectType> createFilterFromUI();
}
