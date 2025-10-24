package com.sap.sailing.gwt.ui.simulator;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.geometrylib.SphericalUtils;
import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.gwt.client.player.Timer;

/**
 * This class implements the layer which displays the optimal path on the path. Currently there is a single path to be
 * displayed.
 * 
 * @author D054070
 * 
 */
public class PathCanvasOverlay extends WindFieldCanvasOverlay implements Named {

    /**
     * Generated serial version id.
     */
    private static final long serialVersionUID = -6284996043723173190L;

    private static int MinimumPxDistanceBetweenArrows = 60;

    private LatLng startPoint;
    private LatLng endPoint;
    private boolean totalTimeIsGiven = false;
    private long totalTimeMilliseconds = 0;

    private String name;
    private boolean algorithmTimedOut;
    private boolean mixedLeg;

    private String pathColor = "Green";

    /**
     * Whether or not to display the wind directions for the points on the optimal path.
     */
    public boolean displayWindAlongPath = true;

    public PathCanvasOverlay(MapWidget map, int zIndex, String name, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.name = name;
    }

    public PathCanvasOverlay(MapWidget map, int zIndex, String name, Timer timer, WindFieldGenParamsDTO windParams, boolean algorithmTimedOut, boolean mixedLeg, CoordinateSystem coordinateSystem) {
        super(map, zIndex, timer, windParams, coordinateSystem);
        this.name = name;
        this.algorithmTimedOut = algorithmTimedOut;
        this.mixedLeg = mixedLeg;
    }

    public PathCanvasOverlay(MapWidget map, int zIndex, String name, long totalTimeMilliseconds, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.name = name;
        this.totalTimeIsGiven = true;
        this.totalTimeMilliseconds = totalTimeMilliseconds;
    }

    public PathCanvasOverlay(MapWidget map, int zIndex, String name, long totalTimeMilliseconds, String color, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.name = name;
        this.totalTimeIsGiven = true;
        this.totalTimeMilliseconds = totalTimeMilliseconds;
        this.pathColor = color;
    }

    public PathCanvasOverlay(MapWidget map, int zIndex, String name, Timer timer, WindFieldGenParamsDTO windParams, long totalTimeMilliseconds, CoordinateSystem coordinateSystem) {
        super(map, zIndex, timer, windParams, coordinateSystem);
        this.name = name;
        this.totalTimeIsGiven = true;
        this.totalTimeMilliseconds = totalTimeMilliseconds;
    }

    public PathCanvasOverlay(MapWidget map, int zIndex, String name, Timer timer, WindFieldGenParamsDTO windParams, long totalTimeMilliseconds, String color, CoordinateSystem coordinateSystem) {
        super(map, zIndex, timer, windParams, coordinateSystem);
        this.name = name;
        this.totalTimeIsGiven = true;
        this.totalTimeMilliseconds = totalTimeMilliseconds;
        this.pathColor = color;
    }

    public void setTotalTimeMilliseconds(long totalTimeMilliseconds) {
        this.totalTimeIsGiven = true;
        this.totalTimeMilliseconds = totalTimeMilliseconds;
    }

    public long getTotalTimeMilliseconds() {
        return this.totalTimeMilliseconds;
    }

    public void setRaceCourse(LatLng startPoint, LatLng endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    @Override
    protected void drawWindField(List<SimulatorWindDTO> windDTOList) {
        int numPoints = windDTOList.size();
        if (numPoints < 1) {
            return;
        }
        Duration totalDuration = windDTOList.get(0).timepoint.until(windDTOList.get(numPoints - 1).timepoint);
        double distance = SphericalUtils.computeDistanceBetween(startPoint, endPoint) / Mile.METERS_PER_NAUTICAL_MILE;
        if (windDTOList != null && windDTOList.size() > 0) {
            Context2d context2d = canvas.getContext2d();
            context2d.setGlobalAlpha(0.8);
            Iterator<SimulatorWindDTO> windDTOIter = windDTOList.iterator();
            SimulatorWindDTO prevWindDTO = null;
            while (windDTOIter.hasNext()) {
                SimulatorWindDTO windDTO = windDTOIter.next();
                if (prevWindDTO != null) {
                    drawLine(prevWindDTO, windDTO);
                }
                prevWindDTO = windDTO;
            }
            windDTOIter = windDTOList.iterator();
            int index = 0;
            final TimePoint startTime = windDTOList.get(0).timepoint;
            prevWindDTO = null; // For the last time arrow was displayed
            while (windDTOIter.hasNext()) {
                SimulatorWindDTO windDTO = windDTOIter.next();
                if (displayWindAlongPath) {
                    if (checkPointsAreFarEnough(windDTO,prevWindDTO)) {
                        AbstractBearing dbi = new DegreeBearingImpl(windDTO.trueWindBearingDeg);
                        drawScaledArrow(windDTO, dbi.getRadians(), index, true);
                        prevWindDTO = windDTO;
                    }
                }
                index++;
                long timeStep = windParams.getTimeStep().asMillis();
                if (startTime.until(windDTO.timepoint).asMillis() % timeStep == 0) {
                    drawPoint(windDTO);
                }
            }
            context2d.setGlobalAlpha(1.0);
            final Date timeDiffDate = new Date(totalDuration.asMillis());
            TimeZone gmt = TimeZone.createTimeZone(0);
            getCanvas().setTitle(StringMessages.INSTANCE.pathCanvasOverlayTitle(numPoints,
                    NumberFormat.getFormat("0.00").format(distance),
                    DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.HOUR24_MINUTE_SECOND).format(timeDiffDate, gmt)));
        }
    }

    private void drawLine(SimulatorWindDTO p1, SimulatorWindDTO p2) {
        double weight = 3.0;
        LatLng positionLatLng = coordinateSystem.toLatLng(p1.position);
        Point canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        double x1 = canvasPositionInPx.getX() - this.getWidgetPosLeft();
        double y1 = canvasPositionInPx.getY() - this.getWidgetPosTop();
        positionLatLng = coordinateSystem.toLatLng(p2.position);
        canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        double x2 = canvasPositionInPx.getX() - this.getWidgetPosLeft();
        double y2 = canvasPositionInPx.getY() - this.getWidgetPosTop();
        // Context2d context2d = canvas.getContext2d();
        // context2d.setShadowBlur(weight);
        drawLine(x1, y1, x2, y2, weight, pathColor);
        // context2d.setShadowBlur(0.0);
        // System.out.print("x1:"+x1+" y1:"+y1+" x2:"+x2+" y2:"+y2+"\n");
    }

    private void drawPoint(SimulatorWindDTO p) {
        double weight = 3.0;
        LatLng positionLatLng = coordinateSystem.toLatLng(p.position);
        Point canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        double x1 = canvasPositionInPx.getX() - this.getWidgetPosLeft();
        double y1 = canvasPositionInPx.getY() - this.getWidgetPosTop();
        drawCircle(x1, y1, weight / 2., pathColor);
    }

    /**
     * 
     * @return true if the pixel distance between the two points is greater than
     * a threshold. returns true if either of the points is null
     */
    private boolean checkPointsAreFarEnough(SimulatorWindDTO p1, SimulatorWindDTO p2) {
        if (p1 == null || p2 == null) {
            return true;
        }
        LatLng positionLatLng = coordinateSystem.toLatLng(p1.position);
        Point canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        double x1 = canvasPositionInPx.getX();
        double y1 = canvasPositionInPx.getY();
        positionLatLng = coordinateSystem.toLatLng(p2.position);
        canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        double x2 = canvasPositionInPx.getX();
        double y2 = canvasPositionInPx.getY();
        double pxDistance = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
        if (pxDistance >= MinimumPxDistanceBetweenArrows) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean getAlgorithmTimedOut() {
        return algorithmTimedOut;
    }

    public boolean getMixedLeg() {
        return mixedLeg;
    }
    
    public long getPathDurationMillis() {
        if (totalTimeIsGiven) {
            return totalTimeMilliseconds;
        } else {
            List<SimulatorWindDTO> windDTOList = windFieldDTO.getMatrix();
            int numPoints = windDTOList.size();
            final Duration totalTime = windDTOList.get(0).timepoint.until(windDTOList.get(numPoints - 1).timepoint);
            return totalTime.asMillis();
        }
    }

    public String getPathColor() {
        return pathColor;
    }

    public void setPathColor(String pathColor) {
        this.pathColor = pathColor;
    }
}
