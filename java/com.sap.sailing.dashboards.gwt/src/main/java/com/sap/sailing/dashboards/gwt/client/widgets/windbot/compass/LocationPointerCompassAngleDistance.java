package com.sap.sailing.dashboards.gwt.client.widgets.windbot.compass;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.dashboards.gwt.client.device.Compass;
import com.sap.sailing.dashboards.gwt.client.device.CompassListener;
import com.sap.sailing.dashboards.gwt.client.device.Location;
import com.sap.sailing.dashboards.gwt.client.device.LocationListener;
import com.sap.sailing.dashboards.gwt.client.device.Orientation;
import com.sap.sailing.dashboards.gwt.client.device.OrientationListener;
import com.sap.sailing.dashboards.gwt.client.device.OrientationType;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreePosition;

/**
 * The classes task is to notify its listeners about changes in distance and angle from the actual device to a pointed
 * location. It receives location and compass heading changes from the classes {@link Location} and {@link Compass} and
 * has a public method {@link #windBotPositionChanged(DegreePosition)} to get changes of the wind bots position the
 * application receives from the backend.
 * 
 * @author Alexander Ries (D062114)
 *
 */
public class LocationPointerCompassAngleDistance implements LocationListener, CompassListener, OrientationListener {

    private List<LocationPointerCompassAngleDistanceListener> listeners;
    private double latDevice;
    private double lonDevice;
    private double latBot;
    private double lonBot;

    /**
     * The angle from the users device to the pointing location. Needs to get cached because it depends on location and
     * compass events from the device.
     * */
    private double angle;
    private double compassHeading;
    private static Double degreesToRadiansFactor = Math.PI / 180.0;

    public LocationPointerCompassAngleDistance() {
        listeners = new ArrayList<LocationPointerCompassAngleDistanceListener>();
        Compass.getInstance().addListener(this);
        Location.getInstance().addListener(this);
        Orientation.getInstance().addListener(this);
    }
    
    public void triggerOrientationRead(){
        Orientation.getInstance().triggerDeviceOrientationRead();
    }

    private double getAngleBetweenGPSPoints(double lat1, double lng1, double lat2, double lng2) {
        Double phi1 = lat1 * degreesToRadiansFactor;
        Double phi2 = lat2 * degreesToRadiansFactor;
        Double lam1 = lng1 * degreesToRadiansFactor;
        Double lam2 = lng2 * degreesToRadiansFactor;
        double angle = Math.atan2(Math.sin(lam2 - lam1) * Math.cos(phi2),
                Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lam2 - lam1))
                * 180 / Math.PI;
        if (angle < 0) {
            angle = 360 + angle;
        }
        return Math.abs(angle);
    }

    private float getDistanceBetweenGPSPoints(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);
        return dist;
    }

    public void addListener(LocationPointerCompassAngleDistanceListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LocationPointerCompassAngleDistanceListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenerAboutAngleChange(double angle) {
        for (LocationPointerCompassAngleDistanceListener listener : listeners) {
            listener.angleChanged(angle);
        }
    }

    private void notifyListenerAboutAngleAndDistanceChange(double angle, double distance) {
        for (LocationPointerCompassAngleDistanceListener listener : listeners) {
            listener.angleAndDistanceChanged(angle, distance);
        }
    }

    private void calulateNewAngleAndDistance() {
        angle = getAngleBetweenGPSPoints(latDevice, lonDevice, latBot, lonBot);
        double distance = getDistanceBetweenGPSPoints(latDevice, lonDevice, latBot, lonBot);
        notifyListenerAboutAngleAndDistanceChange(calulateNewAngleFromPositionAngleAndCompassHeading(this.angle, this.compassHeading), distance);
    }

    @Override
    public void compassHeadingChanged(double compassHeading) {
        this.compassHeading = compassHeading;
        notifyListenerAboutAngleChange(calulateNewAngleFromPositionAngleAndCompassHeading(this.angle, this.compassHeading));
    }
    
    private double calulateNewAngleFromPositionAngleAndCompassHeading(double positionAngle, double compassAngle){
        double compassWidgetAngle;
        if (compassHeading > angle) {
            compassWidgetAngle = 360 + (angle - compassHeading);
        } else {
            compassWidgetAngle = angle - compassHeading;
        }
        compassWidgetAngle = compassWidgetAngle % 360;
        return compassWidgetAngle;
    }

    @Override
    public void locationChanged(double latDeg, double longDeg) {
        latDevice = latDeg;
        lonDevice = longDeg;
        calulateNewAngleAndDistance();
    }

    public void windBotPositionChanged(Position positionDTO) {
        latBot = positionDTO.getLatDeg();
        lonBot = positionDTO.getLngDeg();
        calulateNewAngleAndDistance();
    }

    private void setAngleOffsetAtListeners(double offset) {
        for (LocationPointerCompassAngleDistanceListener listener : listeners) {
            listener.setAngleOffset(offset);
        }
    }

    @Override
    public void orientationChanged(Pair<OrientationType, Double> orientation) {
        setAngleOffsetAtListeners(orientation.getB().doubleValue());
    }
}
