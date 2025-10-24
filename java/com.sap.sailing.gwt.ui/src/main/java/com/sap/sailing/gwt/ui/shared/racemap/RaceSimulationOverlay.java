package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.shiro.authz.UnauthorizedException;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.TextMetrics;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.LegIdentifierImpl;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.ui.actions.GetSimulationAction;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.shared.PathDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.gwt.ui.simulator.util.ColorPalette;
import com.sap.sailing.gwt.ui.simulator.util.ColorPaletteGenerator;
import com.sap.sse.common.Duration;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;

/**
 * A Google Maps overlay based on an HTML5 canvas for drawing simulation results on the {@link RaceMap}.
 * 
 * @author Christopher Ronnewinkel (D036654)
 * 
 */
public class RaceSimulationOverlay extends FullCanvasOverlay {

    public static final String GET_SIMULATION_CATEGORY = "getSimulation";
    private final String textColor = "Black";
    private final String textFont = "10pt 'Open Sans'";
    private int xOffset = 0;
    private int yOffset = 0; //150;
    private double rectWidth = 20;
    private double rectHeight = 20;
    private final StringMessages stringMessages;
    private final RegattaAndRaceIdentifier raceIdentifier;
    private final SailingServiceAsync sailingService;
    private final AsyncActionsExecutor asyncActionsExecutor;
    private final ColorPalette colors;
    private final PathNameFormatter pathNameFormatter;
    private SimulatorResultsDTO simulationResult;
    private Boolean[] visiblePaths;
    private PathDTO racePath;
    private int raceLeg = 0;
    private long requestedSimulationVersion = 0;
    private Canvas simulationLegend;
    private final Runnable disableRaceSimulator;
    
    public RaceSimulationOverlay(MapWidget map, int zIndex, RegattaAndRaceIdentifier raceIdentifier,
            SailingServiceAsync sailingService, StringMessages stringMessages,
            AsyncActionsExecutor asyncActionsExecutor, CoordinateSystem coordinateSystem,
            Runnable disableRaceSimulator) {
        super(map, zIndex, coordinateSystem);
        this.raceIdentifier = raceIdentifier;
        this.sailingService = sailingService;
        this.stringMessages = stringMessages;
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.disableRaceSimulator = disableRaceSimulator;
        this.colors = new ColorPaletteGenerator();
        this.pathNameFormatter = new PathNameFormatter(stringMessages);
        getCanvas().getElement().setId("race-simulation-overlay");
    }
    
    public void updateLeg(int newLeg, boolean clearCanvas, long newVersion) {
        if ((newLeg != raceLeg || (newLeg == raceLeg && newVersion > this.getVersion())) && this.isVisible()) {
            if (newLeg != raceLeg) {
                raceLeg = newLeg;
                requestedSimulationVersion = 0;                
            } else {
                requestedSimulationVersion = newVersion;
            }
            if (clearCanvas) {
                this.clearCanvas();
            }
            this.simulate(newLeg);
        }
    }

    public LegIdentifier getLegIdentifier() {
        return new LegIdentifierImpl(raceIdentifier, raceLeg);
    }
    
    public long getVersion() {
        if (this.simulationResult == null) {
            return 0;
        } else {
            return this.simulationResult.getVersion();
        }
    }
    
    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (simulationLegend != null) {
            simulationLegend.setVisible(isVisible);
        }
    }

    @Override
    protected void drawCenterChanged() {
        draw();
    }

    @Override
    protected void draw() {
        if (getMapProjection() != null) {
            super.setCanvasSettings();
            drawPaths();
        } else {
            logger.info("map projection of "+this+" was null");
        }
    }    

    private void createSimulationLegend(MapWidget map) {
        simulationLegend = Canvas.createIfSupported();
        simulationLegend.addStyleName("MapSimulationLegend");
        simulationLegend.setTitle(stringMessages.simulationLegendTooltip());
        simulationLegend.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                int clickPixelY = event.getRelativeY(simulationLegend.getElement());
                int legendRow = clickPixelY / ((int) rectHeight);
                int pathRow = legendRow - (racePath!=null ? 1 : 0);
                //Window.alert("clickPixelY: " + clickPixelY + "\nlegendRow: " + legendRow);
                visiblePaths[pathRow] = !visiblePaths[pathRow];
                clearCanvas();
                drawPaths();
            }
        });
        map.setControls(ControlPosition.RIGHT_BOTTOM, simulationLegend);
        simulationLegend.getParent().addStyleName("MapSimulationLegendParentDiv");
    }
    
    public void clearCanvas() {
        // clear paths
        double w = this.getCanvas().getOffsetWidth();
        double h = this.getCanvas().getOffsetHeight();
        final Context2d g = this.getCanvas().getContext2d();
        g.clearRect(0, 0, w, h);
        // clear legend
        if (simulationLegend != null) {
            w = simulationLegend.getOffsetWidth();
            h = simulationLegend.getOffsetHeight();
            final Context2d legend = simulationLegend.getContext2d();
            legend.clearRect(0, 0, w, h);
        }
    }
    
    public void drawPaths() {
        // check availability of simulation results
        if (simulationResult == null) {
            return;
        }
        if (simulationResult.getPaths() == null) {
            return;
        }
        // draw legend
        if (simulationLegend == null) {
            createSimulationLegend(map);
        }
        drawLegend(simulationLegend);
        // calibrate canvas
        setCanvasSettings();
        // draw paths
        Context2d ctxt = canvas.getContext2d();
        PathDTO[] paths = simulationResult.getPaths();
        boolean first = true;
        int colorIdx = paths.length;
        for (PathDTO path : paths) {
            colorIdx--;
            if (!visiblePaths[colorIdx] || (path.getMixedLeg())) {
                continue;
            }
            List<SimulatorWindDTO> points = path.getPoints();
            ctxt.setLineWidth(3.0);
            ctxt.setGlobalAlpha(0.7);
            ctxt.setStrokeStyle(this.colors.getColor(colorIdx));
            ctxt.beginPath();
            for (SimulatorWindDTO point : points) {
                Point px = getMapProjection().fromLatLngToContainerPixel(coordinateSystem.toLatLng(point.position));
                if (first) {
                    ctxt.moveTo(px.getX(), px.getY());
                    first = false;
                } else {
                    ctxt.lineTo(px.getX(), px.getY());
                }
            }
            ctxt.stroke();
            final SimulatorWindDTO start = points.get(0);
            final Duration timeStep = simulationResult.getTimeStep();
            for (SimulatorWindDTO point : points) {
                if (start.timepoint.until(point.timepoint).asMillis() % timeStep.asMillis() != 0) {
                    continue;
                }
                Point px = getMapProjection().fromLatLngToContainerPixel(coordinateSystem.toLatLng(point.position));
                ctxt.beginPath();
                ctxt.arc(px.getX(), px.getY(), 1.5, 0, 2 * Math.PI);
                ctxt.closePath();
                ctxt.stroke();
            }
        }
    }

    public void drawLegend(Canvas canvas) {
        if (this.simulationResult == null) {
            return;
        }
        int index = 0;
        Context2d context2d = canvas.getContext2d();
        context2d.setFont(textFont);
        TextMetrics txtmet;
        txtmet = context2d.measureText("00:00:00");
        double timewidth = txtmet.getWidth();
        double txtmaxwidth = 0.0;
        if (racePath != null) {
            txtmet = context2d.measureText(stringMessages.raceLeader());
            txtmaxwidth = Math.max(txtmaxwidth, txtmet.getWidth());
        }
        boolean containsTimeOut = false;
        boolean containsMixedLeg = false;
        PathDTO[] paths = this.simulationResult.getPaths();
        for (PathDTO path : paths) {
            if (path.getAlgorithmTimedOut()) {
                containsTimeOut = true;
            }
            if (path.getMixedLeg()) {
                containsMixedLeg = true;
            }
            txtmet = context2d.measureText(pathNameFormatter.format(path));
            txtmaxwidth = Math.max(txtmaxwidth, txtmet.getWidth());
        }
        double newwidth = 0;
        double deltaTime = 0;
        double deltaMixedLeg = 0;
        double deltaTimeOut = 0;
        double mixedLegWidth = 0;
        if (containsMixedLeg) {
            txtmet = context2d.measureText(stringMessages.mixedLegText());
            mixedLegWidth = txtmet.getWidth();
            newwidth = Math.max(timewidth, mixedLegWidth);
        }
        double timeOutWidth = 0;
        if (containsTimeOut) {
            txtmet = context2d.measureText(stringMessages.algorithmTimeOutText());
            timeOutWidth = txtmet.getWidth();
            newwidth = Math.max(newwidth, timeOutWidth);
        }
        if (containsMixedLeg||containsTimeOut) {
            deltaTime = newwidth - timewidth;
            deltaMixedLeg = newwidth - mixedLegWidth;
            deltaTimeOut = newwidth - timeOutWidth;
            timewidth = newwidth;
        }
        //canvas.setSize(xOffset + rectWidth + txtmaxwidth + timewidth + 10.0+"px", rectHeight*(paths.length+1)+"px");
        int canvasWidth = (int)Math.ceil(xOffset + rectWidth + txtmaxwidth + timewidth + 10.0 + 10.0);
        int canvasHeight = (int)Math.ceil(yOffset + rectHeight*(paths.length + (racePath == null? 0 : 1)));
        setCanvasSize(canvas, canvasWidth, canvasHeight);
        if (racePath != null) {
            drawRectangleWithText(context2d, xOffset, yOffset, null, stringMessages.raceLeader(),
                getFormattedTime(racePath.getPathTime()), txtmaxwidth, timewidth, deltaTime, true);
        }
        for (PathDTO path : paths) {
            String timeText = (path.getMixedLeg() ? stringMessages.mixedLegText() : (path.getAlgorithmTimedOut() ? stringMessages.algorithmTimeOutText() : getFormattedTime(path.getPathTime())));
            drawRectangleWithText(context2d, xOffset, yOffset + (paths.length-index-(racePath==null?1:0)) * rectHeight, this.colors.getColor(paths.length-1-index),
                pathNameFormatter.format(path), timeText, txtmaxwidth, timewidth, (path.getMixedLeg()?deltaMixedLeg:(path.getAlgorithmTimedOut()?deltaTimeOut:deltaTime)), visiblePaths[paths.length-1-index]);
            index++;
        }
    }

    protected void setCanvasSize(Canvas canvas, int canvasWidth, int canvasHeight) {
        canvas.setWidth(String.valueOf(canvasWidth));
        canvas.setHeight(String.valueOf(canvasHeight));
        canvas.setCoordinateSpaceWidth(canvasWidth);
        canvas.setCoordinateSpaceHeight(canvasHeight);
    }
    
    protected void drawRectangle(Context2d context2d, double x, double y, String color) {
        context2d.setFillStyle(color);
        context2d.setLineWidth(3);
        context2d.fillRect(x, y, rectWidth, rectHeight);
    }

    protected void drawRectangleWithText(Context2d context2d, double x, double y, String color, String text, String time, double textmaxwidth, double timewidth, double xdelta, boolean visible) {
        double offset = 3.0;
        double crossOffset = 5.0;
        context2d.setFont(textFont);
        if (color != null) {
            drawRectangle(context2d, x, y, color);
        }
        if ((visible) && (color != null)) {
            context2d.setGlobalAlpha(1.0);
            context2d.setLineWidth(3.0);
            context2d.setStrokeStyle("white");
            context2d.beginPath();
            context2d.moveTo(x + crossOffset,y + crossOffset);
            context2d.lineTo(x + rectWidth - crossOffset, y + rectHeight - crossOffset);
            context2d.stroke();
            context2d.beginPath();
            context2d.moveTo(x + crossOffset, y + rectHeight - crossOffset);
            context2d.lineTo(x + rectWidth - crossOffset,y + crossOffset);
            context2d.stroke();
            context2d.setStrokeStyle("black");            
        }
        context2d.setGlobalAlpha(0.80);
        context2d.setFillStyle("white");
        context2d.fillRect(x + (color==null?0:rectWidth), y, 20.0 + textmaxwidth + timewidth + (color==null?rectWidth:0), rectHeight);
        context2d.setGlobalAlpha(1.0);
        context2d.setFillStyle(textColor);
        context2d.fillText(text, x + rectWidth + 5.0, y + 12.0 + offset);
        context2d.fillText(time, x + rectWidth + textmaxwidth + xdelta + 15.0, y + 12.0 + offset);
    }
    
    protected String getFormattedTime(long pathTime) {
        TimeZone gmt = TimeZone.createTimeZone(0);
        Date timeDiffDate = new Date(pathTime);
        String pathTimeStr = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.HOUR24_MINUTE_SECOND).format(
                timeDiffDate, gmt);
        return pathTimeStr;
    }
    
    public void simulate(int leg) {
        LegIdentifier legIdentifier = new LegIdentifierImpl(raceIdentifier, leg);
        GetSimulationAction getSimulation = new GetSimulationAction(sailingService, legIdentifier);
        asyncActionsExecutor.execute(getSimulation, GET_SIMULATION_CATEGORY,
                new MarkedAsyncCallback<>(new AsyncCallback<SimulatorResultsDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(stringMessages.errorFetchingSimulationData(caught.getMessage()),
                                NotificationType.ERROR);
                        if (caught instanceof UnauthorizedException) {
                            disableRaceSimulator.run();
                        }
                    }

                    @Override
                    public void onSuccess(SimulatorResultsDTO result) {
                        // store results
                        if (result != null) {
                            if ((result.getPaths() != null) && (result.getVersion() >= requestedSimulationVersion) && (result.getLeg() == raceLeg)) {
                                simulationResult = result;
                                PathDTO[] paths = result.getPaths();
                                if (result.getLegDuration().compareTo(Duration.NULL) > 0) {
                                    racePath = new PathDTO("0#Race Leader");
                                    List<SimulatorWindDTO> racePathPoints = new ArrayList<SimulatorWindDTO>();
                                    racePathPoints.add(new SimulatorWindDTO(null, 0, 0, paths[0].getPoints().get(0).timepoint));
                                    racePathPoints.add(new SimulatorWindDTO(null, 0, 0, paths[0].getPoints().get(0).timepoint.plus(result.getLegDuration())));
                                    racePath.setPoints(racePathPoints);
                                } else {
                                    racePath = null;
                                }
                                if (visiblePaths == null || visiblePaths.length != simulationResult.getPaths().length) {
                                    visiblePaths = new Boolean[simulationResult.getPaths().length];
                                    Arrays.fill(visiblePaths, Boolean.TRUE);
                                    visiblePaths[1] = Boolean.FALSE; // hide left-opportunist by default
                                    visiblePaths[2] = Boolean.FALSE; // hide right-opportunist by default
                                }
                                clearCanvas();
                                if (getMapProjection() != null) {
                                    drawPaths();
                                }
                            }
                        } else {
                            raceLeg = 0;
                            simulationResult = null;
                            visiblePaths = null;
                            clearCanvas();
                        }
                    }
                }));
    }
}
