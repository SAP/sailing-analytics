package com.sap.sailing.gwt.autoplay.client.places.startclassic.old;

import com.google.gwt.user.client.ui.Widget;

public interface PlayerView {
    Widget asWidget();
    
    public void clearContent();
    public void setContent(Widget contentWidget);
}
