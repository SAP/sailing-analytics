package com.sap.sailing.gwt.ui.simulator.streamlets;

import com.google.gwt.maps.client.base.Point;

public class Vector {
    public double x;
    public double y;

    public Vector() {
    }

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector(Point pixel) {
        this(pixel.getX(), pixel.getY());
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public Vector setLength(double length) {
        double current = this.length();
        if (current > 0) {
            double scale = length / current;
            this.x *= scale;
            this.y *= scale;
        }
        return this;
    }
    
    @Override
    public String toString() {
        return "{x: "+x+", y: "+y+"}";
    }
}
