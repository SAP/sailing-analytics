package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.gwt.ui.simulator.util.ToolTip;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.gwt.client.player.Timer;

/**
 * A google map overlay based on a HTML5 canvas for drawing a wind field. The overlay covers the whole map and displays
 * the wind objects inside it.
 * 
 * @author Nidhi Sawhney(D054070)
 * 
 */
public class WindLineGuidesCanvasOverlay extends FullCanvasOverlay implements TimeListenerWithStoppingCriteria {
 
    /** The wind field that is to be displayed in the overlay */
    private WindFieldDTO windFieldDTO;
    
    /**
     * Map containing the windfield for easy retrieval with key as time point.
     */
    private SortedMap<Long, List<SimulatorWindDTO>> timePointWindDTOMap;

    /** The points where ToolTip is to be displayed */
    private Map<ToolTip, SimulatorWindDTO> windFieldPoints;
    private String arrowColor = "Black";
    private String arrowHeadColor = "Blue";

    private int xRes;
    private Timer timer;

    private static Logger logger = Logger.getLogger(WindLineGuidesCanvasOverlay.class.getName());

    public WindLineGuidesCanvasOverlay(MapWidget map, int zIndex, final Timer timer, int xRes, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        
        this.timer = timer;
        this.xRes = xRes;
        
        windFieldDTO = null;
        windFieldPoints = new HashMap<ToolTip, SimulatorWindDTO>();
        
        timePointWindDTOMap = new TreeMap<Long, List<SimulatorWindDTO>>();        
    }

    public WindLineGuidesCanvasOverlay(MapWidget map, int zIndex, CoordinateSystem coordinateSystem) {
        this(map, zIndex, null, 0, coordinateSystem);
    }

    @Override
    public void addToMap() {
        super.addToMap();

        if (timer != null) {
            timer.addTimeListener(this);
        }
    }

    @Override
    public void removeFromMap() {
        super.removeFromMap();
        if (timer != null) {
            timer.removeTimeListener(this);
        }
    }
    
    public void setWindField(final WindFieldDTO windField) {
        this.windFieldDTO = windField;
        timePointWindDTOMap.clear();
        if (windField != null) {
            for(final SimulatorWindDTO w : windField.getMatrix()) {
                if (!timePointWindDTOMap.containsKey(w.timepoint.asMillis())) {
                    timePointWindDTOMap.put(w.timepoint.asMillis(), new LinkedList<SimulatorWindDTO>());
                }
                timePointWindDTOMap.get(w.timepoint.asMillis()).add(w);
            }
        }
    }

    public void setArrowColor(final String arrowColor, final String arrowHeadColor) {
        this.arrowColor = arrowColor;
        this.arrowHeadColor = arrowHeadColor;
    }

    @Override
    protected void draw() {
        super.draw();
        if (getMapProjection() != null && windFieldDTO != null) {
            clear();
            drawWindField();
        }
    }

    private void clear() {
        canvas.getContext2d().clearRect(0.0 /*canvas.getAbsoluteLeft()*/, 0.0/*canvas.getAbsoluteTop()*/,
                canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
        windFieldPoints.clear();
    }

    protected void drawWindField() {
        if (timer != null) {
            timeChanged(timer.getTime(), null);
        } else {
            drawWindField(windFieldDTO.getMatrix());
        }
    }

    protected void drawScaledArrow(final SimulatorWindDTO windDTO, final double angle, final int index, final double pxLength, final boolean drawHead) {

        final double aWidth = 1.0;
        drawArrow(windDTO, angle, pxLength, aWidth, index, drawHead);

    }

    protected void drawWindField(final List<SimulatorWindDTO> windDTOList) {
        final boolean drawHead = false;
        clear();
        final Context2d context2d = canvas.getContext2d();
        context2d.setGlobalAlpha(0.4);

        if (windDTOList != null && windDTOList.size() > 0) {
            Iterator<SimulatorWindDTO> windDTOIter = windDTOList.iterator();
            final SimulatorWindDTO w0 = windDTOIter.next();
            final SimulatorWindDTO w1 = windDTOIter.next();
            final LatLng pg0 = coordinateSystem.toLatLng(w0.position);
            final LatLng pg1 = coordinateSystem.toLatLng(w1.position);
            final Point px0 = getMapProjection().fromLatLngToDivPixel(pg0);
            final Point px1 = getMapProjection().fromLatLngToDivPixel(pg1);
            final double dx = px0.getX()-px1.getX();
            final double dy = px0.getY()-px1.getY();
            final double pxLength = Math.sqrt( dx*dx + dy*dy );
            logger.fine("pxLength = "+pxLength);

            windDTOIter = windDTOList.iterator();
            int index = 0;
            while (windDTOIter.hasNext()) {
                SimulatorWindDTO windDTO = windDTOIter.next();
                //System.out.println("wind angle: "+index+", "+windDTO.trueWindBearingDeg);
                if (((index % xRes) > 0)&&((index % xRes) < (xRes-1))) {
                    AbstractBearing dbi = new DegreeBearingImpl(windDTO.trueWindBearingDeg);
                    drawScaledArrow(windDTO, dbi.getRadians(), index, pxLength, drawHead);
                }
                index++;
            }
            final String title = "Wind Field at " + windDTOList.size() + " points.";
            getCanvas().setTitle(title);
        }
    }

    protected void drawArrow(final SimulatorWindDTO windDTO, final double angle, final double length, final double weight, final int index, final boolean drawHead) {
        final String msg = "Wind @ P" + index + ": time : " + windDTO.timepoint + " speed: " + windDTO.trueWindSpeedInKnots + "knots "
                + windDTO.trueWindBearingDeg;
        logger.fine(msg);
        final Context2d context2d = canvas.getContext2d();
        context2d.setGlobalAlpha(0.2);
        final Position position = windDTO.position;
        final LatLng positionLatLng = coordinateSystem.toLatLng(position);
        final Point canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        final double x = canvasPositionInPx.getX() - getWidgetPosLeft();
        final double y = canvasPositionInPx.getY() - getWidgetPosTop();
        windFieldPoints.put(new ToolTip(x, y), windDTO);
        final double dx = length * Math.sin(angle);
        final double dy = -length * Math.cos(angle);
        final double x1 = x + dx / 2;
        final double y1 = y + dy / 2;
        drawLine(x - dx / 2, y - dy / 2, x1, y1, weight, arrowColor);
        final double theta = Math.atan2(-dy, dx);
        final double hLength = Math.max(6.,6.+(10./(60.-10.))*Math.max(length-6.,0));
        logger.finer("headlength: "+hLength+", arrowlength: "+length);
        if (drawHead) {
            drawHead(x1, y1, theta, hLength, weight);
        }
        context2d.setGlobalAlpha(0.4);
    }

    protected void drawHead(final double x, final double y, final double theta, final double headLength, final double weight) {

        double t = theta + (Math.PI / 4);
        if (t > Math.PI) {
            t -= 2 * Math.PI;
        }
        double t2 = theta - (Math.PI / 4);
        if (t2 <= (-Math.PI)) {
            t2 += 2 * Math.PI;
        }

        final double x1 = (x - Math.cos(t) * headLength);
        final double y1 = (y + Math.sin(t) * headLength);
        final double x1o = (x + Math.cos(t) * weight/2);
        final double y1o = (y - Math.sin(t) * weight/2);
        final double x2 = (x - Math.cos(t2) * headLength);
        final double y2 = (y + Math.sin(t2) * headLength);
        final double x2o = (x + Math.cos(t2) * weight/2);
        final double y2o = (y - Math.sin(t2) * weight/2);
        drawLine(x1o, y1o, x1, y1, weight, arrowHeadColor);
        drawLine(x2o, y2o, x2, y2, weight, arrowHeadColor);

    }

    @Override
    public void timeChanged(final Date newDate, Date oldDate) {

        List<SimulatorWindDTO> windDTOToDraw = new ArrayList<SimulatorWindDTO>();

        final SortedMap<Long, List<SimulatorWindDTO>> headMap = (timePointWindDTOMap.headMap(newDate.getTime()+1));

        if (!headMap.isEmpty()) {
            windDTOToDraw = headMap.get(headMap.lastKey());
        }
        logger.info("In WindFieldCanvasOverlay.drawWindField drawing " + windDTOToDraw.size() + " points" + " @ "
                + newDate);

        drawWindField(windDTOToDraw);

    }

    @Override
    public boolean shallStop() {
        if (!this.isVisible() || timePointWindDTOMap == null || timer == null || timePointWindDTOMap.isEmpty()) {
            return true;
        }
        if (timePointWindDTOMap.lastKey() < timer.getTime().getTime()) {
            return true;
        } else {
            return false;
        }
    }

    public void setTimer(final Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return timer;
    }
}
