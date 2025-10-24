package com.sap.sse.gwt.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FlowPanel;

public class ImageOnFlowPanelHelper {
    public static void setImage(FlowPanel image, String imageUrl) {
        image.getElement().getStyle().setBackgroundImage("url(" + imageUrl + ")");
        image.getElement().getStyle().setWidth(100, Unit.PCT);
        image.getElement().getStyle().setProperty("height", "90%");
        image.getElement().getStyle().setProperty("margin", "auto");
        image.getElement().getStyle().setProperty("backgroundPosition", "center bottom");
        image.getElement().getStyle().setProperty("backgroundSize", "contain");
        image.getElement().getStyle().setProperty("backgroundRepeat", "no-repeat");
    }
}
