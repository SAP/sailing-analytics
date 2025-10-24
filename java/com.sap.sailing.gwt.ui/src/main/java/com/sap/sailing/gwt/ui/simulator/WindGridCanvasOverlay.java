package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.gwt.ui.simulator.util.WindGridColorPalette;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.player.Timer;

/**
 * A google map overlay based on a HTML5 canvas for representing a wind field as a heat map. The overlay covers the
 * whole map and colors the cells of the field based on the trueWindSpeedInKnots
 * 
 * @author Nidhi Sawhney(D054070)
 * 
 */
public class WindGridCanvasOverlay extends FullCanvasOverlay implements TimeListenerWithStoppingCriteria {
    /** The wind field that is to be displayed in the overlay */
    private WindFieldDTO windFieldDTO;
    
    /**
     * Map containing the windfield for easy retrieval with key as time point.
     */
    private SortedMap<Long, List<SimulatorWindDTO>> timePointWindDTOMap;

    private final Timer timer;

    private int xRes;
    private int yRes;
    private SimulatorWindDTO[][] windMatrix;
    private Map<Util.Pair<Integer, Integer>, GridCell> gridCellMap;
    private WindGridColorPalette colorPalette;

    private static Logger logger = Logger.getLogger(WindFieldCanvasOverlay.class.getName());

    private class GridCell {
        public Position bottomLeft;
        public Position bottomRight;
        public Position topLeft;
        public Position topRight;

        public double windSpeedInKnots;

        public GridCell(final Position bl, final Position br, final Position tl, final Position tr, final Double windSpeedInKnots) {
            this.bottomLeft = bl;
            this.bottomRight = br;
            this.topLeft = tl;
            this.topRight = tr;
            this.windSpeedInKnots = windSpeedInKnots;
        }
    }

    private class SortByWindSpeed implements Comparator<SimulatorWindDTO> {
        @Override
        public int compare(final SimulatorWindDTO w1, final SimulatorWindDTO w2) {
            return Double.compare(w1.trueWindSpeedInKnots, w2.trueWindSpeedInKnots);
        }
    }

    private class SortByLatitude implements Comparator<SimulatorWindDTO> {
        @Override
        public int compare(final SimulatorWindDTO w1, final SimulatorWindDTO w2) {
            return Double.compare(w1.position.getLatDeg(), w2.position.getLatDeg());
        }
    }

    private class SortByLongitude implements Comparator<SimulatorWindDTO> {
        @Override
        public int compare(final SimulatorWindDTO w1, final SimulatorWindDTO w2) {
            return Double.compare(w1.position.getLngDeg(), w2.position.getLngDeg());
        }
    }

    public WindGridCanvasOverlay(MapWidget map, int zIndex, final Timer timer, final int xRes, final int yRes, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.timer = timer;
        this.xRes = xRes;
        this.yRes = yRes;

        windFieldDTO = null;
        timePointWindDTOMap = new TreeMap<Long, List<SimulatorWindDTO>>();
        colorPalette = null;
    }

    public WindGridCanvasOverlay(MapWidget map, int zIndex, CoordinateSystem coordinateSystem) {
        this(map, zIndex, null, 0, 0, coordinateSystem);
    }

    public void setWindField(final WindFieldDTO wl) {
        this.windFieldDTO = wl;

        timePointWindDTOMap.clear();
        if (wl != null) {
            for (final SimulatorWindDTO w : wl.getMatrix()) {
                if (!timePointWindDTOMap.containsKey(w.timepoint.asMillis())) {
                    timePointWindDTOMap.put(w.timepoint.asMillis(), new LinkedList<SimulatorWindDTO>());
                }
                timePointWindDTOMap.get(w.timepoint.asMillis()).add(w);
            }

            final SortByWindSpeed windSpeedSorter = new SortByWindSpeed();
            final double maxSpeed = Collections.max(wl.getMatrix(), windSpeedSorter).trueWindSpeedInKnots;
            final double minSpeed = Collections.min(wl.getMatrix(), windSpeedSorter).trueWindSpeedInKnots;
            logger.fine("minSpeed: " + minSpeed + " maxSpeed: " + maxSpeed);

            colorPalette = new WindGridColorPalette(minSpeed, maxSpeed);
            logger.fine("Color minSpeed: " + colorPalette.getColor(minSpeed));
            logger.fine("Color maxSpeed: " + colorPalette.getColor(maxSpeed));

            /**
             * Get the wind at first time point to capture the positions on the grid.
             */
            final Long firstTimePoint = timePointWindDTOMap.firstKey();
            final SortedMap<Long, List<SimulatorWindDTO>> headMap = (timePointWindDTOMap.headMap(firstTimePoint + 1));
            List<SimulatorWindDTO> windDTOToDraw;
            if (!headMap.isEmpty()) {
                windDTOToDraw = headMap.get(headMap.lastKey());
                createPositionGrid(windDTOToDraw);
            }
        }
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

    @Override
    public void timeChanged(final Date newTime, Date oldTime) {
        List<SimulatorWindDTO> windDTOToDraw = new ArrayList<SimulatorWindDTO>();

        final SortedMap<Long, List<SimulatorWindDTO>> headMap = (timePointWindDTOMap.headMap(newTime.getTime() + 1));

        if (!headMap.isEmpty()) {
            windDTOToDraw = headMap.get(headMap.lastKey());
        }
        logger.info("In WindGridCanvasOverlay.timeChanged drawing " + windDTOToDraw.size() + " points" + " @ " + newTime);

        drawWindGrid(windDTOToDraw);
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

    @Override
    protected void draw() {
        super.draw();
        if (getMapProjection() != null && windFieldDTO != null) {
            clear();
            drawWindGrid();
        }
    }

    private void clear() {
        canvas.getContext2d().clearRect(0.0 /* canvas.getAbsoluteLeft() */, 0.0/* canvas.getAbsoluteTop() */,
                canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
    }

    protected void drawWindGrid() {

        if (timer != null) {
            timeChanged(timer.getTime(), null);

        } else {
            drawWindGrid(windFieldDTO.getMatrix());
        }

    }

    protected void drawWindGrid(final List<SimulatorWindDTO> windDTOList) {
        clear();
        if (windDTOList != null && windDTOList.size() > 1) {
            if (windDTOList.size() != xRes * yRes) {
                logger.warning("Error in WindGridCanvasOverlay wind field is not rectangular.");
                return;
            }
            // createPositionGrid(windDTOList);
            // createGridCell();
            updatePositionGrid(windDTOList);
            drawGridCell();

            final String title = "Wind Grid at " + windDTOList.size() + " points.";
            getCanvas().setTitle(title);
        }
    }

    /**
     * 
     * @param windDTOList
     * @return the horizontal pixel distance between the first two points in the list
     */
    @SuppressWarnings("unused")
    private double getGridWidth(final List<SimulatorWindDTO> windDTOList) {
        if (windDTOList.size() > 1) {
            final SortByLatitude sortLatitude = new SortByLatitude();

            Collections.sort(windDTOList, sortLatitude);
            final SimulatorWindDTO windDTO1 = windDTOList.get(0);
            final SimulatorWindDTO windDTO2 = windDTOList.get(1);

            final LatLng positionLatLng1 = LatLng.newInstance(windDTO1.position.getLatDeg(), windDTO1.position.getLngDeg());
            final Point canvasPositionInPx1 = getMapProjection().fromLatLngToDivPixel(positionLatLng1);

            final LatLng positionLatLng2 = LatLng.newInstance(windDTO2.position.getLatDeg(), windDTO2.position.getLngDeg());
            final Point canvasPositionInPx2 = getMapProjection().fromLatLngToDivPixel(positionLatLng2);

            return canvasPositionInPx2.getX() - canvasPositionInPx1.getX();
        }
        return 0;
    }

    /**
     * 
     * @param windDTOList
     * @return the horizontal pixel distance between the first two points in the list
     */
    @SuppressWarnings("unused")
    private double getGridHeight(final List<SimulatorWindDTO> windDTOList) {
        if (windDTOList.size() > 1) {
            final SortByLongitude sortLongitude = new SortByLongitude();

            Collections.sort(windDTOList, sortLongitude);
            final SimulatorWindDTO windDTO1 = windDTOList.get(0);
            final SimulatorWindDTO windDTO2 = windDTOList.get(1);

            final LatLng positionLatLng1 = LatLng.newInstance(windDTO1.position.getLatDeg(), windDTO1.position.getLngDeg());
            final Point canvasPositionInPx1 = getMapProjection().fromLatLngToDivPixel(positionLatLng1);

            final LatLng positionLatLng2 = LatLng.newInstance(windDTO2.position.getLatDeg(), windDTO2.position.getLngDeg());
            final Point canvasPositionInPx2 = getMapProjection().fromLatLngToDivPixel(positionLatLng2);

            return canvasPositionInPx2.getY() - canvasPositionInPx1.getY();
        }
        return 0;
    }

    private void createPositionGrid(final List<SimulatorWindDTO> windDTOList) {
        if (windDTOList.size() != xRes * yRes) {
            logger.warning("Error in WindGridCanvasOverlay wind field is not rectangular.");
            this.windMatrix = null;
            return;
        }
        this.windMatrix = new SimulatorWindDTO[yRes + 2][xRes];
        final Iterator<SimulatorWindDTO> windDTOIter = windDTOList.iterator();

        for (int i = 1; i < yRes + 1; ++i) {
            for (int j = 0; j < xRes; ++j) {
                windMatrix[i][j] = windDTOIter.next();
            }
        }
        extendPositionGrid();

        createGridCell();
    }

    private void updatePositionGrid(final List<SimulatorWindDTO> windDTOList) {
        if (windDTOList.size() != xRes * yRes) {
            logger.warning("Error in WindGridCanvasOverlay wind field is not rectangular.");
            return;
        }
        final Iterator<SimulatorWindDTO> windDTOIter = windDTOList.iterator();

        for (int i = 0; i < yRes; ++i) {
            for (int j = 0; j < xRes; ++j) {
                windMatrix[i][j] = windDTOIter.next();
            }
        }
        updateGridCell();
    }

    /**
     * Create extra row before the first and after the last row to ensure the start and end points are covered by the
     * grid cells.
     */
    private void extendPositionGrid() {

        final int numRow = windMatrix.length;

        if (numRow < 4) {
            return;
        }
        /*
         * Row before the first row
         */
        for (int j = 0; j < xRes; ++j) {
            final Position p1 = windMatrix[1][j].position;
            final Position p2 = windMatrix[2][j].position;
            final Position position = new DegreePosition(2 * p1.getLatDeg() - p2.getLatDeg(), 2 * p1.getLngDeg() - p2.getLngDeg());
            final SimulatorWindDTO windDTO = new SimulatorWindDTO();
            // Only the position of this windDTO is used
            windDTO.position = position;
            windDTO.trueWindSpeedInKnots = 0.0;
            windMatrix[0][j] = windDTO;
        }

        /*
         * Row after the last row
         */
        for (int j = 0; j < xRes; ++j) {
            final Position p1 = windMatrix[numRow - 2][j].position;
            final Position p2 = windMatrix[numRow - 3][j].position;
            final Position position = new DegreePosition(2 * p1.getLatDeg() - p2.getLatDeg(), 2 * p1.getLngDeg() - p2.getLngDeg());
            final SimulatorWindDTO windDTO = new SimulatorWindDTO();
            // Only the position of this windDTO is used
            windDTO.position = position;
            // windDTO.trueWindSpeedInKnots = 0.0;
            windMatrix[numRow - 1][j] = windDTO;
        }
    }

    private void createGridCell() {
        if (windMatrix != null) {
            final int numRow = windMatrix.length;
            if (numRow >= 4) {
                final int numCol = windMatrix[0].length;
                if (numCol >= 2) {
                    gridCellMap = new HashMap<Util.Pair<Integer, Integer>, GridCell>();
                    for (int i = 1; i < numRow - 1; ++i) {
                        for (int j = 1; j < numCol - 1; ++j) {
                            final Position bl = getCenter(windMatrix[i - 1][j - 1].position, windMatrix[i - 1][j].position,
                                    windMatrix[i][j].position, windMatrix[i][j - 1].position);
                            final Position tl = getCenter(windMatrix[i][j - 1].position, windMatrix[i][j].position,
                                    windMatrix[i + 1][j - 1].position, windMatrix[i + 1][j].position);
                            final Position br = getCenter(windMatrix[i - 1][j].position, windMatrix[i - 1][j + 1].position,
                                    windMatrix[i][j].position, windMatrix[i][j + 1].position);
                            final Position tr = getCenter(windMatrix[i][j].position, windMatrix[i][j + 1].position,
                                    windMatrix[i + 1][j].position, windMatrix[i + 1][j + 1].position);
                            final GridCell cell = new GridCell(bl, br, tl, tr, windMatrix[i - 1][j].trueWindSpeedInKnots);
                            final Util.Pair<Integer, Integer> cellPair = new Util.Pair<Integer, Integer>(i, j);
                            gridCellMap.put(cellPair, cell);
                            // drawGridCell(cell);
                        }
                    }
                }
            }

        }
    }

    private void updateGridCell() {
        if (windMatrix != null) {
            final int numRow = windMatrix.length;
            if (numRow >= 4) {
                final int numCol = windMatrix[0].length;
                if (numCol >= 2) {
                    for (int i = 1; i < numRow - 1; ++i) {
                        for (int j = 1; j < numCol - 1; ++j) {
                            final Util.Pair<Integer, Integer> cellPair = new Util.Pair<Integer, Integer>(i, j);
                            final GridCell cell = gridCellMap.get(cellPair);
                            cell.windSpeedInKnots = windMatrix[i - 1][j].trueWindSpeedInKnots;
                        }
                    }
                }
            }
        }

    }

    private void drawGridCell() {
        if (gridCellMap != null & !gridCellMap.isEmpty()) {
            for (final Entry<Util.Pair<Integer, Integer>, GridCell> cell : gridCellMap.entrySet()) {
                drawGridCell(cell.getValue());
            }
        }
    }

    private void drawGridCell(final GridCell cell) {
        LatLng positionLatLng = coordinateSystem.toLatLng(cell.bottomLeft);
        final Point blPoint = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        positionLatLng = coordinateSystem.toLatLng(cell.bottomRight);
        final Point brPoint = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        positionLatLng = coordinateSystem.toLatLng(cell.topLeft);
        final Point tlPoint = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        positionLatLng = coordinateSystem.toLatLng(cell.topRight);
        final Point trPoint = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        /*
         * Uncomment to see the center of the grid for debug drawCircle(blPoint.getX()-this.getWidgetPosLeft(),
         * blPoint.getY()-this.getWidgetPosTop(),2,"red"); drawCircle(brPoint.getX()-this.getWidgetPosLeft(),
         * brPoint.getY()-this.getWidgetPosTop(),2,"red"); drawCircle(tlPoint.getX()-this.getWidgetPosLeft(),
         * tlPoint.getY()-this.getWidgetPosTop(),2,"red"); drawCircle(trPoint.getX()-this.getWidgetPosLeft(),
         * trPoint.getY()-this.getWidgetPosTop(),2,"red");
         */
        final Context2d context2d = canvas.getContext2d();
        context2d.setLineWidth(1);
        context2d.setStrokeStyle("Black");
        context2d.setGlobalAlpha(0.5f);
        context2d.setFillStyle(colorPalette.getColor(cell.windSpeedInKnots));

        context2d.beginPath();
        context2d.moveTo(blPoint.getX() - this.getWidgetPosLeft(), blPoint.getY() - this.getWidgetPosTop());
        context2d.lineTo(brPoint.getX() - this.getWidgetPosLeft(), brPoint.getY() - this.getWidgetPosTop());
        context2d.lineTo(trPoint.getX() - this.getWidgetPosLeft(), trPoint.getY() - this.getWidgetPosTop());
        context2d.lineTo(tlPoint.getX() - this.getWidgetPosLeft(), tlPoint.getY() - this.getWidgetPosTop());
        context2d.lineTo(blPoint.getX() - this.getWidgetPosLeft(), blPoint.getY() - this.getWidgetPosTop());
        context2d.closePath();

        context2d.fill();
        // context2d.stroke(); // Dont show the lines
    }

    private Position getCenter(final Position a, final Position b, final Position c, final Position d) {
        // center longitudes on position a, to ensure consistency on date line
        double aLng = (a.getLngDeg() + 180) % 360.0 - 180;
        double bLng = (b.getLngDeg() - aLng + 180) % 360.0 - 180;
        double cLng = (c.getLngDeg() - aLng + 180) % 360.0 - 180;
        double dLng = (d.getLngDeg() - aLng + 180) % 360.0 - 180;
        final Position center = new DegreePosition((a.getLatDeg() + b.getLatDeg() + c.getLatDeg() + d.getLatDeg()) / 4.0, aLng + (bLng + cLng + dLng) / 4.0);
        return center;
    }

    public Position[] getGridCorners() {
        if (gridCellMap != null && !gridCellMap.isEmpty()) {
            final Position[] corners = new Position[4];
            final int numRow = windMatrix.length;
            final int numCol = windMatrix[0].length;
            final Util.Pair<Integer, Integer> cellPair1 = new Util.Pair<Integer, Integer>(1, 1);
            corners[0] = gridCellMap.get(cellPair1).bottomLeft;

            final Util.Pair<Integer, Integer> cellPair2 = new Util.Pair<Integer, Integer>(1, numCol-2);
            corners[1] = gridCellMap.get(cellPair2).bottomRight;

            final Util.Pair<Integer, Integer> cellPair3 = new Util.Pair<Integer, Integer>(numRow-2, numCol-2);
            corners[2] = gridCellMap.get(cellPair3).topRight;

            final Util.Pair<Integer, Integer> cellPair4 = new Util.Pair<Integer, Integer>(numRow-2, 1);
            corners[3] = gridCellMap.get(cellPair4).topLeft;

            return corners;
        }
        return null;
    }
}
