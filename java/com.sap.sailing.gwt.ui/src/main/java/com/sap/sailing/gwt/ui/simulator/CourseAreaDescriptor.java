package com.sap.sailing.gwt.ui.simulator;

import com.google.gwt.maps.client.base.LatLng;

/**
 * A helper class to describe a course area on a map.
 */
public class CourseAreaDescriptor {

    private String name;
    private double radius;
    private LatLng centerPos;
    private LatLng edgePos;
    private String color;
    private String colorText;
 
    public CourseAreaDescriptor(String name, LatLng centerPos, double radius, String color) {
        this(name, centerPos, radius, color, color);
    }

    public CourseAreaDescriptor(String name, LatLng centerPos, double radius, String color, String colorText) {
        this.name = name;
        this.radius = radius;
        this.centerPos = centerPos;
        this.edgePos = getEdgePoint(centerPos, radius);
        this.color = color;
        this.colorText = colorText;
    }

    // TODO: replace with existing function
    private LatLng getEdgePoint(LatLng pos, double dist) {
        double lat1 = pos.getLatitude() / 180. * Math.PI;
        double lon1 = pos.getLongitude() / 180. * Math.PI;

        double brng = 0.0;

        double R = 6371;
        double d = 1.852 * dist;
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R) + Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
        double lon2 = lon1
                + Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
                        Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
        lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI; // normalize to -180° ... +180°*/

        double lat2deg = lat2 / Math.PI * 180;
        double lon2deg = lon2 / Math.PI * 180;

        LatLng result = LatLng.newInstance(lat2deg, lon2deg);

        return result;
    }

    public String getName() {
        return name;
    }

    public double getRadius() {
        return radius;
    }

    public double getRadiusInMeters() {
        return (radius*1852.0);
    }

    public LatLng getCenterPos() {
        return centerPos;
    }

    public LatLng getEdgePos() {
        return edgePos;
    }

    public String getColor() {
        return color;
    }

    public String getColorText() {
        return colorText;
    }

}
