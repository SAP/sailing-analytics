package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.maps.client.overlays.PolylineOptions;

public class ColorlineOptions {
    private ColorlineMode colorMode = ColorlineMode.MONOCHROMATIC;

    private boolean clickable = true;
    private boolean draggable = false;
    private boolean editable  = false;
    private boolean geodesic  = true;
    private boolean visible   = true;

    private int strokeWeight = 1;
    private double strokeOpacity = 1.0;

    private int zIndex = 0;

    private ColorlineColorProvider colorProvider;

    public ColorlineOptions() {
    }

    public ColorlineOptions(ColorlineColorProvider colorProvider) {
        this.colorProvider = colorProvider;
    }

    public ColorlineOptions(PolylineOptions options, ColorlineColorProvider colorProvider) {
        clickable = options.getClickable();
        geodesic = options.getGeodesic();
        visible = options.getVisible();
        strokeWeight = options.getStrokeWeight();
        strokeOpacity = options.getStrokeOpacity();
        zIndex = options.getZindex();
        this.colorProvider = colorProvider;
    }

    public ColorlineOptions(ColorlineOptions options) {
        colorMode = options.getColorMode();
        colorProvider = options.getColorProvider();
        clickable = options.getClickable();
        draggable = options.getDraggable();
        editable = options.getEditable();
        geodesic = options.getGeodesic();
        visible = options.getVisible();
        strokeWeight = options.getStrokeWeight();
        strokeOpacity = options.getStrokeOpacity();
        zIndex = options.getZIndex();
    }

    public Polyline newPolylineInstance(int fixIndexInTail) {
        if (colorProvider == null) {
            throw new IllegalStateException("A ColorProvider must be set prior to creating new Polylines.");
        }
        return newPolylineInstance(colorProvider.getColor(fixIndexInTail));
    }

    public Polyline newPolylineInstance(String strokeColor) {
        Polyline line = Polyline.newInstance(newPolylineOptionsInstance(strokeColor));
        line.setEditable(editable);
        return line;
    }

    public PolylineOptions newPolylineOptionsInstance(String strokeColor) {
        PolylineOptions opt = PolylineOptions.newInstance();
        opt.setStrokeColor(strokeColor);
        opt.setClickable(clickable);
        opt.setGeodesic(geodesic);
        opt.setVisible(visible);
        opt.setStrokeWeight(strokeWeight);
        opt.setStrokeOpacity(strokeOpacity);
        opt.setZindex(zIndex);
        return opt;
    }

    public ColorlineMode getColorMode() {
        return colorMode;
    }

    public void setColorMode(ColorlineMode colorMode) {
        this.colorMode = colorMode;
    }

    public ColorlineColorProvider getColorProvider() {
        return colorProvider;
    }

    public void setColorProvider(ColorlineColorProvider colorProvider) {
        this.colorProvider = colorProvider;
    }

    public boolean getClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean getDraggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public boolean getEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean getGeodesic() {
        return geodesic;
    }

    public void setGeodesic(boolean geodesic) {
        this.geodesic = geodesic;
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public int getStrokeWeight() {
        return strokeWeight;
    }

    public void setStrokeWeight(int strokeWeight) {
        this.strokeWeight = strokeWeight;
    }

    public double getStrokeOpacity() {
        return strokeOpacity;
    }

    public void setStrokeOpacity(double strokeOpacity) {
        this.strokeOpacity = strokeOpacity;
    }
}
