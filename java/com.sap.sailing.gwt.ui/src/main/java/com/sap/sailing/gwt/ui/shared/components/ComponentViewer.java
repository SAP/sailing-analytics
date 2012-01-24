package com.sap.sailing.gwt.ui.shared.components;

import com.google.gwt.user.client.ui.Widget;

public interface ComponentViewer {

    Widget getViewerWidget();
    
    String getViewerName();

    Component<?> getRootComponent();
}
