package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.List;

import org.moxieapps.gwt.highcharts.client.Color;
import org.moxieapps.gwt.highcharts.client.PlotLine;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.PlotLine.DashStyle;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.maps.client.events.center.CenterChangeMapEvent;
import com.google.gwt.maps.client.events.center.CenterChangeMapHandler;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.sap.sailing.domain.common.FixType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.client.shared.racemap.FixOverlay;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sse.common.Position;

public class FixPositionChooser {
    private final Callback<Position, Exception> callback;
    private final MapWidget map;
    private final boolean newFix;
    private final int polylineFixIndex;
    private final MVCArray<LatLng> polylinePath;
    private final LatLng startPos;
    private final FixOverlay overlay;
    private final CoordinateSystem coordinateSystem;
    private FixOverlay moveOverlay;
    private HandlerRegistration centerChangeHandlerRegistration;
    private MenuBar menu;
    private final EditMarkPositionPanel editMarkPositionPanel;
    private final StringMessages stringMessages;
    private final PlotLine redTimeLine;
    
    /**
     * Use this constructor when there is already a fix with an overlay. 
     * This constructor will automatically assume that an existing fix is moved.
     * @param editMarkPositionPanel
     * @param map
     * @param polylineFixIndex Position of the fix in the polyline path
     * @param polylinePath
     * @param overlay
     * @param callback
     */
    public FixPositionChooser(final EditMarkPositionPanel editMarkPositionPanel, final StringMessages stringMessages, final MapWidget map, final int polylineFixIndex, final MVCArray<LatLng> polylinePath, final FixOverlay overlay, 
            final Callback<Position, Exception> callback) {
        this(editMarkPositionPanel, stringMessages, false, map, polylineFixIndex, polylinePath, overlay, overlay.getLatLngPosition(), overlay.getCoordinateSystem(), 
                stringMessages.confirmMove(), callback);
    }
    
    /**
     * Use this constructor when you want to add a fix and not move one.
     * This constructor will automatically assume that there is no existing fix.
     */
    public FixPositionChooser(final EditMarkPositionPanel editMarkPositionPanel, final StringMessages stringMessages, final MapWidget map, final int polylineFixIndex, final MVCArray<LatLng> polylinePath, final LatLng startPos, 
            final CoordinateSystem coordinateSystem, final Callback<Position, Exception> callback) {
        this(editMarkPositionPanel, stringMessages, true, map, polylineFixIndex, polylinePath, null, startPos, coordinateSystem, 
                stringMessages.confirmNewFix(), callback);
    }
    
    // The problem of not knowing if it is a touchscreen or mouse the user is currently using, applies to this class too.
    // The touch optimized input is currently implemented, because mouse users can use it more easily than the other way around.
    private FixPositionChooser(final EditMarkPositionPanel editMarkPositionPanel, final StringMessages stringMessages, final boolean newFix, final MapWidget map, final int polylineFixIndex, final MVCArray<LatLng> polylinePath, 
            final FixOverlay overlay, final LatLng startPos, final CoordinateSystem coordinateSystem, final String confirmButtonText, 
            final Callback<Position, Exception> callback) {
        this.callback = callback;
        this.stringMessages = stringMessages;
        this.map = map;
        this.newFix = newFix;
        this.polylineFixIndex = polylineFixIndex;
        this.polylinePath = polylinePath;
        this.overlay = overlay;
        this.startPos = startPos;
        this.coordinateSystem = coordinateSystem;
        this.editMarkPositionPanel = editMarkPositionPanel;
        this.editMarkPositionPanel.showNotification(stringMessages.selectAFixPositionBy());
        this.redTimeLine = editMarkPositionPanel.getXAxis().createPlotLine().setColor(new Color(255, 0, 0)).setWidth(1.5).setDashStyle(DashStyle.SOLID);
        setupUIOverlay(confirmButtonText);
    }
    
    private void setupUIOverlay(final String confirmButtonText) {
        final GPSFixDTO fix;
        if (overlay != null) {
            final GPSFixDTO oldFix = overlay.getGPSFixDTO();
            fix = new GPSFixDTO(oldFix.timepoint, oldFix.position);
            this.moveOverlay = new FixOverlay(map, overlay.getZIndex(), fix, overlay.getType(), "#f00", coordinateSystem, stringMessages.dragToChangePosition());
        } else {
            fix = new GPSFixDTOWithSpeedWindTackAndLegType(editMarkPositionPanel.getTimepoint(), coordinateSystem.getPosition(startPos), null, /* optionalTrueHeading */ null, new WindDTO(), null, null, false);
            this.moveOverlay = new FixOverlay(map, EditMarkPositionPanel.FIX_OVERLAY_Z_ORDER+1, fix, FixType.BUOY, "#f00", coordinateSystem, stringMessages.dragToChangePosition());
        }
        redTimeLine.setValue(fix.timepoint.getTime());
        editMarkPositionPanel.getXAxis().addPlotLines(redTimeLine);
        map.panTo(startPos);
        if (polylinePath != null && newFix) {
            polylinePath.insertAt(polylineFixIndex, map.getCenter());
        }
        setRedPointInChart(fix);
        centerChangeHandlerRegistration = map.addCenterChangeHandler(new CenterChangeMapHandler() {
            @Override
            public void onEvent(CenterChangeMapEvent event) {
                fix.position = coordinateSystem.getPosition(map.getCenter());
                moveOverlay.setGPSFixDTO(fix);
                if (polylinePath != null) {
                    polylinePath.setAt(polylineFixIndex, map.getCenter());
                }
                setRedPointInChart(fix);
            }
        });
        menu = new MenuBar(/* vertical */ false);
        menu.setStyleName("EditMarkPositionConfirmCancelButtons");
        MenuItem confirm = new MenuItem(confirmButtonText, new ScheduledCommand() {
            @Override
            public void execute() {
                destroyUIOverlay();
                cleanupChart();
                callback.onSuccess(coordinateSystem.getPosition(map.getCenter()));
            }
        });
        MenuItem cancel = new MenuItem(stringMessages.cancel(), new ScheduledCommand() {
            @Override
            public void execute() {
                cancel();
            }
        });
        menu.addItem(confirm);
        menu.addItem(cancel);
        map.setControls(ControlPosition.BOTTOM_LEFT, menu);
    }
    
    private void destroyUIOverlay() {
        moveOverlay.removeFromMap();
        centerChangeHandlerRegistration.removeHandler();
        menu.removeFromParent();
    }
    
    private void cleanupChart() {
        editMarkPositionPanel.getXAxis().removePlotLine(redTimeLine);
        editMarkPositionPanel.updateRedPoint(polylineFixIndex);
        editMarkPositionPanel.resetPointColor(polylineFixIndex);
        editMarkPositionPanel.redrawChart();
    }
    
    private void setRedPointInChart(GPSFixDTO fix) {
        List<GPSFixDTO> fixes = editMarkPositionPanel.getMarkFixes();
        if (fixes != null) {
            if (newFix) {
                fixes.add(polylineFixIndex, fix);
            } else {
                fixes.set(polylineFixIndex, fix);
            }
            Point[] points = editMarkPositionPanel.getSeriesPoints(fixes);
            editMarkPositionPanel.setRedPoint(points, polylineFixIndex);
            editMarkPositionPanel.setSeriesPoints(points);
            editMarkPositionPanel.redrawChart();
        }
    }
    
    public void cancel() {
        destroyUIOverlay();
        cleanupChart();
        if (polylinePath != null) {
            if (newFix) {
                polylinePath.removeAt(polylineFixIndex);
            } else {
                polylinePath.setAt(polylineFixIndex, overlay.getLatLngPosition());
            }
        }
        callback.onFailure(null);
    }
}
