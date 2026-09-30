package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.moxieapps.gwt.highcharts.client.Axis;
import org.moxieapps.gwt.highcharts.client.BaseChart;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.ChartSubtitle;
import org.moxieapps.gwt.highcharts.client.ChartTitle;
import org.moxieapps.gwt.highcharts.client.Color;
import org.moxieapps.gwt.highcharts.client.Credits;
import org.moxieapps.gwt.highcharts.client.PlotLine.DashStyle;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.ToolTip;
import org.moxieapps.gwt.highcharts.client.ToolTipData;
import org.moxieapps.gwt.highcharts.client.ToolTipFormatter;
import org.moxieapps.gwt.highcharts.client.XAxis;
import org.moxieapps.gwt.highcharts.client.events.ChartClickEvent;
import org.moxieapps.gwt.highcharts.client.events.ChartClickEventHandler;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEvent;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEventHandler;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;
import org.moxieapps.gwt.highcharts.client.plotOptions.LinePlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;

import com.google.gwt.ajaxloader.client.Properties.TypeException;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapOptions;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.LatLngBounds;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.maps.client.events.click.ClickMapEvent;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.mousedown.MouseDownMapEvent;
import com.google.gwt.maps.client.events.mousedown.MouseDownMapHandler;
import com.google.gwt.maps.client.events.mousemove.MouseMoveMapEvent;
import com.google.gwt.maps.client.events.mousemove.MouseMoveMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapEvent;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.events.mouseup.MouseUpMapEvent;
import com.google.gwt.maps.client.events.mouseup.MouseUpMapHandler;
import com.google.gwt.maps.client.events.resize.ResizeMapEvent;
import com.google.gwt.maps.client.events.resize.ResizeMapHandler;
import com.google.gwt.maps.client.events.rightclick.RightClickMapEvent;
import com.google.gwt.maps.client.events.rightclick.RightClickMapHandler;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.maps.client.overlays.PolylineOptions;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.FixType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.charts.MarkPositionService.MarkTrackDTO;
import com.sap.sailing.gwt.ui.client.shared.racemap.BoundsUtil;
import com.sap.sailing.gwt.ui.client.shared.racemap.CourseMarkOverlay;
import com.sap.sailing.gwt.ui.client.shared.racemap.FixOverlay;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.raceboard.SideBySideComponentViewer;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.settings.AbstractSettings;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

public class EditMarkPositionPanel extends AbstractRaceChart<AbstractSettings> implements RequiresResize, SelectionChangeEvent.Handler, HasAvailabilityCheck {
    protected static final int FIX_OVERLAY_Z_ORDER = 230;
    private final RaceMap raceMap;
    private final SingleRaceLeaderboardPanel leaderboardPanel;
    private final MarksPanel marksPanel;
    private Series markSeries;
    private LinePlotOptions markSeriesPlotOptions;
    private final Label noMarkSelectedLabel;
    private MapWidget map;
    private List<HandlerRegistration> courseMarkClickHandlers;
    
    private Map<MarkDTO, Pair<Date, Date>> marksFromToTimes;
    private Date raceFromTime;
    private Date raceToTime;
    
    private Map<MarkDTO, SortedMap<GPSFixDTO, FixOverlay>> marks;
    private MarkDTO selectedMark;
    private Map<MarkDTO, Polyline> polylines;
    private SideBySideComponentViewer sideBySideComponentViewer;
    
    private FixPositionChooser currentFixPositionChooser;
    private List<OverlayClickHandler> overlayClickHandlers;
    private final MarkPositionService markPositionService;
    
    private final RaceIdentifierToLeaderboardRaceColumnAndFleetMapper raceIdentifierToLeaderboardRaceColumnAndFleetMapper;
    protected boolean nonTrackingWarningWasDisplayed;

    private final Set<MarkDTO> marksCurrentlyRequestedViaRemoteCall = new HashSet<>();
    private final SailingServiceWriteAsync sailingServiceWrite;

    public EditMarkPositionPanel(Component<?> parent, ComponentContext<?> context, final RaceMap raceMap,
            final SingleRaceLeaderboardPanel leaderboardPanel,
            RegattaAndRaceIdentifier selectedRaceIdentifier, String leaderboardName, final StringMessages stringMessages,
            SailingServiceAsync sailingService, Timer timer, TimeRangeWithZoomProvider timeRangeWithZoomProvider,
            AsyncActionsExecutor asyncActionsExecutor, ErrorReporter errorReporter, SailingServiceWriteAsync sailingServiceWrite) {
        super(parent, context, sailingService, selectedRaceIdentifier, timer, timeRangeWithZoomProvider, stringMessages,
                asyncActionsExecutor, errorReporter);
        this.markPositionService = new MarkPositionServiceForSailingService(sailingServiceWrite);
        this.raceIdentifierToLeaderboardRaceColumnAndFleetMapper = new RaceIdentifierToLeaderboardRaceColumnAndFleetMapper();
        this.raceMap = raceMap;
        this.leaderboardPanel = leaderboardPanel;
        this.sailingServiceWrite = sailingServiceWrite;
        this.polylines = new HashMap<>();
        this.marksPanel = new MarksPanel(this, context, stringMessages);
        this.noMarkSelectedLabel = new Label(stringMessages.pleaseSelectAMark());
        this.noMarkSelectedLabel.setStyleName("abstractChartPanel-importantMessageOfChart");
        this.courseMarkClickHandlers = new ArrayList<>();
        this.marksFromToTimes = new HashMap<>();
        this.getEntryWidget().setTitle(stringMessages.editMarkPositions());
        this.setVisible(false);
        this.overlayClickHandlers = new ArrayList<>();
        chart = new Chart()
                .setPersistent(true)
                .setZoomType(BaseChart.ZoomType.X)
                .setMarginLeft(65)
                .setMarginRight(65)
                .setWidth100()
                .setHeight100()
                .setBorderWidth(0)
                .setBorderRadius(0)
                .setBackgroundColor(new Color("#FFFFFF"))
                .setPlotBackgroundColor("#f8f8f8")
                .setPlotBorderWidth(0)
                .setCredits(new Credits().setEnabled(false))
                .setChartTitle(new ChartTitle().setText(stringMessages.markFixes()))
                .setChartSubtitle(new ChartSubtitle().setText(stringMessages.clickAndDragToZoomIn()))
                .setPersistent(true);
        chart.setClickEventHandler(new ChartClickEventHandler() {
            @Override
            public boolean onClick(ChartClickEvent chartClickEvent) {
                return EditMarkPositionPanel.this.onClick(chartClickEvent);
            }
        });
        chart.setSelectionEventHandler(new ChartSelectionEventHandler() {
            @Override
            public boolean onSelection(ChartSelectionEvent chartSelectionEvent) {
                return EditMarkPositionPanel.this.onXAxisSelectionChange(chartSelectionEvent);
            }
        });
        chart.getXAxis().setType(Axis.Type.DATE_TIME)
                .setMaxZoom(60 * 1000) // 1 minute
                .setAxisTitleText(stringMessages.time());
        chart.getXAxis().setLabels(new XAxisLabels().setFormatter(new AxisLabelsFormatter() {
            @Override
            public String format(AxisLabelsData axisLabelsData) {
                return dateFormatHoursMinutes.format(new Date(axisLabelsData.getValueAsLong()));
            }
        }));
        chart.getYAxis().setAxisTitleText(stringMessages.distanceFromAveragePosition() + " (" + stringMessages.metersUnit() + ")").setOption("labels/enabled", false);
        timePlotLine = chart.getXAxis().createPlotLine().setColor("#656565").setWidth(1)
                .setDashStyle(DashStyle.SOLID);
        markSeriesPlotOptions = new LinePlotOptions().setSelected(true).setShowInLegend(false).setLineWidth(1).setColor("#000")
                .setHoverStateLineWidth(1).setEnableMouseTracking(true);
        markSeries = chart.createSeries().setType(Series.Type.LINE).setYAxis(0)
                .setPlotOptions(markSeriesPlotOptions).setName(stringMessages.distanceFromAveragePosition());
        chart.addSeries(markSeries, false, false);
        chart.setToolTip(new ToolTip().setEnabled(true).setFormatter(new ToolTipFormatter() {
            @Override
            public String format(ToolTipData toolTipData) {
                final String seriesName = toolTipData.getSeriesName();
                if (seriesName.equals(stringMessages.time())) {
                    return "<b>" + seriesName + ":</b> " + dateFormat.format(new Date(toolTipData.getXAsLong()))
                            + "<br/>(" + stringMessages.clickChartToSetTime() + ")";
                } else {
                    return "<b>" + (dateFormat.format(new Date(toolTipData.getXAsLong()))) + "</b>: "
                                 + NumberFormat.getFormat("0.0").format(toolTipData.getYAsDouble()) + " m";
                }
            }
        }));
    }
    
    private void canRemoveMarkFix(MarkDTO mark, GPSFixDTO fix, AsyncCallback<Boolean> callback) {
        markPositionService.canRemoveMarkFix(
                raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(selectedRaceIdentifier),
                mark, fix, callback);
    }
    
    private static class Pixel extends JavaScriptObject {
        protected Pixel() {
        }
        
        private native int getX() /*-{
			return this.x;
        }-*/;
        private native int getY() /*-{
			return this.y;
        }-*/;
    }
    
    private class OverlayClickHandler {
        private final PopupPanel popup;
        private final MenuItem moveMenuItem;
        
        private final FixOverlay overlay;
        private FixOverlay moveOverlay;
        
        private final MouseDownMapHandler mouseDownHandler;
        private HandlerRegistration mouseDownHandlerRegistration;
        private final RightClickMapHandler rightClickHandler;
        private HandlerRegistration rightClickHandlerRegistration;
        
        private int mouseDownX;
        private int mouseDownY;
        private boolean mouseDown;
        private boolean dragging;
        
        //Currently the big problem with the OverlayClickHandler is that you cannot reliably detect if users use a touch device
        //and even if they don't at first they could switch from touch to mouse or the other way around. Thus, both input methods
        //are always present. Touch users should not accidentally drag fixes, because there is a buffer.
        public OverlayClickHandler(final MarkDTO mark, final GPSFixDTO fix, final FixOverlay overlay) {
            this.overlay = overlay;
            final Polyline polyline = polylines.get(mark);
            
            map.addResizeHandler(new ResizeMapHandler() {
                @Override
                public void onEvent(ResizeMapEvent event) {
                    onResizeMap(event);
                }
            });
            rightClickHandler = new RightClickMapHandler() {
                @Override
                public void onEvent(RightClickMapEvent event) {
                    onRightClick(event);
                }
            };
            mouseDownHandler = new MouseDownMapHandler() {
                @Override
                public void onEvent(MouseDownMapEvent event) {
                    try {
                        if (event.getProperties().getNumber("which") == 1) {
                            mouseDownX = event.getProperties().getNumber("pageX").intValue() - map.getAbsoluteLeft();
                            mouseDownY = event.getProperties().getNumber("pageY").intValue() - map.getAbsoluteTop();
                            mouseDown = true;
                            dragging = false;
                            MapOptions options = MapOptions.newInstance(false);
                            options.setDraggable(false);
                            map.setOptions(options);
                        }
                    } catch (TypeException e) {
                        GWT.log("Exception trying to obtain pageX or pageY property on event "+event, e);
                    }
                }
            };
            map.addMouseMoveHandler(new MouseMoveMapHandler() {
                @Override
                public void onEvent(MouseMoveMapEvent event) {
                    if (mouseDown) {
                        int currentX = 0;
                        int currentY = 0;
                        try {
                            Pixel pixel = event.getProperties().getObject("pixel").cast();
                            currentX = pixel.getX();
                            currentY = pixel.getY();
                        } catch (TypeException e) {
                            GWT.log("Exception trying to obtain pixel property on event "+event, e);
                        }
                        final int index = getIndexOfFixInPolyline(mark, fix);
                        if (!dragging && Math.sqrt(Math.pow(currentX - mouseDownX, 2) +
                                Math.pow(currentY - mouseDownY, 2)) > 15) {
                            dragging = true;
                            overlay.setVisible(false);
                            setRedPoint(index);
                            moveOverlay = new FixOverlay(map, overlay.getZIndex(), overlay.getGPSFixDTO(), overlay.getType(),
                                    "#f00", raceMap.getCoordinateSystem(), stringMessages.dragToChangePosition());
                            moveOverlay.addMouseUpHandler(new MouseUpMapHandler() {
                                @Override
                                public void onEvent(MouseUpMapEvent event) {
                                    editMarkFix(mark, fix, fix.position);
                                    overlay.setVisible(true);
                                    resetPointColor(index);
                                    moveOverlay.removeFromMap();
                                    moveOverlay = null;
                                    
                                    MapOptions options = MapOptions.newInstance(false);
                                    options.setDraggable(true);
                                    map.setOptions(options);
                                    mouseDown = false;
                                }
                            });
                        }
                        if (dragging) {
                            fix.position = raceMap.getCoordinateSystem().getPosition(event.getMouseEvent().getLatLng());
                            overlay.setGPSFixDTO(fix);
                            moveOverlay.setGPSFixDTO(fix);
                            updateRedPoint(index);
                            polyline.getPath().setAt(index, event.getMouseEvent().getLatLng());
                        }
                    }
                }
            });
            overlay.addMouseUpHandler(new MouseUpMapHandler() {
                @Override
                public void onEvent(MouseUpMapEvent event) {
                    if (mouseDown) {
                        if (!dragging) {
                            onClick(event);
                        }
                        MapOptions options = MapOptions.newInstance(false);
                        options.setDraggable(true);
                        map.setOptions(options);
                        mouseDown = false;
                    }
                }
            });
            map.addMouseOutMoveHandler(new MouseOutMapHandler() {
                @Override
                public void onEvent(MouseOutMapEvent event) {
                    if (mouseDown) {
                        if (dragging) {
                            editMarkFix(mark, fix, fix.position);
                            overlay.setVisible(true);
                            resetPointColor(getIndexOfFixInPolyline(mark, fix));
                            moveOverlay.removeFromMap();
                            moveOverlay = null;
                        }
                        MapOptions options = MapOptions.newInstance(false);
                        options.setDraggable(true);
                        map.setOptions(options);
                        mouseDown = false;
                    }
                }
            });
            
            popup = new PopupPanel(true);
            popup.setStyleName("EditMarkPositionPopup");
            final MenuBar menu = new MenuBar(true);
            moveMenuItem = new MenuItem(stringMessages.moveFix(), new ScheduledCommand() {
                @Override
                public void execute() {
                    overlay.setVisible(false);
                    if (currentFixPositionChooser == null) {
                        OverlayClickHandler.this.unregisterAll();
                        checkIfTracking(() -> {
                            currentFixPositionChooser = new FixPositionChooser(EditMarkPositionPanel.this,
                                    stringMessages, map, getIndexOfFixInPolyline(mark, fix), polyline.getPath(),
                                    overlay, new Callback<Position, Exception>() {
                                        @Override
                                        public void onFailure(Exception reason) {
                                            overlay.setVisible(true);
                                            OverlayClickHandler.this.registerAll();
                                            resetCurrentFixPositionChooser();
                                        }

                                        @Override
                                        public void onSuccess(Position result) {
                                            editMarkFix(mark, fix, result);
                                            overlay.setVisible(true);
                                            OverlayClickHandler.this.registerAll();
                                            resetCurrentFixPositionChooser();
                                        }
                                    });
                        });
                    }
                    popup.hide();
                }
            });
            moveMenuItem.setTitle(stringMessages.useATouchOptimizedUI());
            menu.addItem(moveMenuItem);
            canRemoveMarkFix(mark, fix, new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError(stringMessages.errorCommunicatingWithServer()+": "+caught.getMessage(), /* silent */ true);
                    addDeleteItemAndShowPopup(false);
                }

                @Override
                public void onSuccess(Boolean result) {
                    addDeleteItemAndShowPopup(result);
                }
                
                private void addDeleteItemAndShowPopup(boolean enableDelete) {
                    Runnable r = () -> {
                        final MenuItem delete = new MenuItem(stringMessages.deleteFix(), new ScheduledCommand() {
                            @Override
                            public void execute() {
                                if (enableDelete) {
                                    removeMarkFix(mark, fix);
                                    popup.hide();
                                }
                            }
                        });
                        if (!enableDelete) {
                            delete.setEnabled(false);
                            delete.setTitle(stringMessages.theDeletionOfThisFix());
                        }
                        menu.addItem(delete);
                        popup.setWidget(menu);
                    };
                    if (enableDelete) {
                        checkIfTracking(r);
                    } else {
                        r.run();
                    }
                }
            });
        }
        
        private int getIndexOfFixInPolyline(MarkDTO mark, GPSFixDTO fix) {
            int index;
            if (marks == null) {
                index = -1;
            } else {
                index = 0;
                for (GPSFixDTO fixToCompare : marks.get(mark).keySet()) {
                    if (fixToCompare.timepoint.equals(fix.timepoint))
                        return index;
                    index++;
                }
            }
            return index;
        }
        
        public void onClick(MouseUpMapEvent event) {
            setPopupPosition();
            moveMenuItem.setVisible(true);
            popup.show();
        }
        
        public OverlayClickHandler register() {
            if (mouseDownHandlerRegistration == null) {
                mouseDownHandlerRegistration = overlay.addMouseDownHandler(mouseDownHandler);
            }
            if (rightClickHandlerRegistration == null) {
                rightClickHandlerRegistration = overlay.addRightClickHandler(rightClickHandler);
            }
            return this;
        }
        
        public OverlayClickHandler unregister() {
            if (mouseDownHandlerRegistration != null) {
                mouseDownHandlerRegistration.removeHandler();
                mouseDownHandlerRegistration = null;
            }
            if (rightClickHandlerRegistration != null) {
                rightClickHandlerRegistration.removeHandler();
                rightClickHandlerRegistration = null;
            }
            return this;
        }
        
        public void unregisterAll() {
            for (OverlayClickHandler handler : overlayClickHandlers) {
                handler.unregister();
            }
        }
        
        public void registerAll() {
            for (OverlayClickHandler handler : overlayClickHandlers) {
                handler.register();
            }
        }
        
        private void setPopupPosition() {
            popup.setPopupPosition(overlay.getCanvas().getAbsoluteLeft() + (int) overlay.getFixScaleAndSize().getB().getWidth() / 2 + 5, 
                    overlay.getCanvas().getAbsoluteTop() + (int) overlay.getFixScaleAndSize().getB().getHeight() / 2 + 5);
        }
        
        public void onResizeMap(ResizeMapEvent event) {
            if (popup.isVisible()) {
                popup.hide();
            }
        }
        
        public void onRightClick(RightClickMapEvent event) {
            setPopupPosition();
            moveMenuItem.setVisible(false);
            popup.show();
        }
    }
    
    public void setRedPoint(Point[] points, int index) {
        points[index].setMarker(new Marker().setFillColor("#f00"));
    }
    
    public void setRedPoint(int index) {
        Point[] points = markSeries.getPoints();
        if (points.length > index) {
            setRedPoint(points, index);
            setSeriesPoints(markSeries, points, /* manageZoom */ true);
        }
    }
    
    public void updateRedPoint(int index) {
        if (selectedMark != null && marks != null) {
            Point[] points = getSeriesPoints(marks.get(selectedMark).keySet());
            if (points.length > index) {
                setRedPoint(points, index);
                setSeriesPoints(markSeries, points, /* manageZoom */ true);
                chart.redraw();
            }
        }
    }
    
    public void resetPointColor(int index) {
        Point[] points = markSeries.getPoints();
        if (points.length > index && selectedMark != null) {
            points[index].setMarker(new Marker().setFillColor(selectedMark.color==null?null:selectedMark.color.getAsHtml()));
            setSeriesPoints(markSeries, points, /* manageZoom */ true);
        }
    }
        
    private void createMarkTrackUi(MarkDTO mark, Iterable<GPSFixDTO> track) {
        Date fromTime = timeRangeWithZoomProvider.getFromTime();
        Date toTime = timeRangeWithZoomProvider.getToTime();
        PolylineOptions options = PolylineOptions.newInstance();
        if (map != null) {
            options.setMap(map);
        }
        options.setStrokeWeight(1);
        options.setVisible(false);
        final Polyline polyline = Polyline.newInstance(options); // Line can not be dashed at the moment, because line symbols are not supported
        polylines.put(mark, polyline);
        
        SortedMap<GPSFixDTO, FixOverlay> fixOverlayMap = marks.get(mark);
        
        for (final GPSFixDTO fix : track) {
            final FixOverlay overlay = new FixOverlay(map, FIX_OVERLAY_Z_ORDER, fix, FixType.BUOY,
                    mark.color==null?null:mark.color.getAsHtml(), raceMap.getCoordinateSystem(), stringMessages.dragToChangePosition());
            fixOverlayMap.put(fix, overlay);
            overlay.setVisible(false);
            overlayClickHandlers.add(new OverlayClickHandler(mark, fix, overlay).register());
            if (fromTime.after(fix.timepoint)) {
                fromTime = fix.timepoint;
            } else if (toTime.before(fix.timepoint)) {
                toTime = fix.timepoint;
            }
        }
        updatePolylinePoints(mark);
        marksFromToTimes.put(mark, new Pair<Date, Date>(fromTime, toTime));
        onResize();
    }
    
    private void loadData() {
        if (selectedRaceIdentifier != null && marks == null) {
            setWidget(chart);
            showLoading(stringMessages.loadingMarkFixes());
            markPositionService.getMarksInTrackedRace(raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(selectedRaceIdentifier), 
                    new AsyncCallback<Iterable<MarkDTO>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            hideLoading();
                            errorReporter.reportError(stringMessages.errorCommunicatingWithServer()+": "+caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Iterable<MarkDTO> marks) {
                            EditMarkPositionPanel.this.marks = new HashMap<>();
                            for (MarkDTO mark : marks) {
                                SortedMap<GPSFixDTO, FixOverlay> fixOverlayMap = new TreeMap<>(new Comparator<GPSFixDTO>() {
                                    @Override
                                    public int compare(GPSFixDTO o1, GPSFixDTO o2) {
                                        return o1.timepoint.compareTo(o2.timepoint);
                                    }
                                });
                                EditMarkPositionPanel.this.marks.put(mark, fixOverlayMap);
                            }
                            marksPanel.updateMarks(marks);
                            onSelectionChange(null);
                            hideLoading();
                        }
                    });
        }
    }
    
    public void addMarkFix(final MarkDTO mark, final Date timepoint, final Position fixPosition) {
        final GPSFixDTOWithSpeedWindTackAndLegType fix = new GPSFixDTOWithSpeedWindTackAndLegType(timepoint, fixPosition, null, /* optionalTrueHeading */ null, new WindDTO(), null, null, false);
        markPositionService.addMarkFix(
                raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(selectedRaceIdentifier),
                mark, fix, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorCommunicatingWithServer()+": "+caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        FixOverlay overlay = new FixOverlay(map, FIX_OVERLAY_Z_ORDER, fix, FixType.BUOY,
                                mark.color==null?null:mark.color.getAsHtml(),
                                raceMap.getCoordinateSystem(), stringMessages.dragToChangePosition());
                        overlayClickHandlers.add(new OverlayClickHandler(mark, fix, overlay).register());
                        marks.get(mark).put(fix, overlay);
                        updatePolylinePoints(mark);
                        setSeriesPoints(mark);
                        onResize();
                        showNotification(stringMessages.fixSuccessfullyAdded(), NotificationType.SUCCESS);
                    }
                });
    }
    
    private void editMarkFix(final MarkDTO mark, final GPSFixDTO fix, final Position newPosition) {
        markPositionService.editMarkFix(
                raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(selectedRaceIdentifier),
                mark, fix, newPosition, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorCommunicatingWithServer()+": "+caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        fix.position = newPosition;
                        marks.get(mark).get(fix).setGPSFixDTO(fix);
                        updatePolylinePoints(mark);
                        setSeriesPoints(mark);
                        onResize();
                        showNotification(stringMessages.fixPositionSuccessfullyEdited(), NotificationType.SUCCESS);
                        timer.setTime(timer.getTime().getTime()+1);
                    }
                });
    }
    
    private void removeMarkFix(final MarkDTO mark, final GPSFixDTO fix) {
        markPositionService.removeMarkFix(
                raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(selectedRaceIdentifier),
                mark, fix, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorCommunicatingWithServer()+": "+caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        CanvasOverlayV3 overlay = marks.get(mark).remove(fix);
                        overlay.removeFromMap();
                        updatePolylinePoints(mark);
                        setSeriesPoints(mark);
                        onResize();
                        showNotification(stringMessages.fixSuccessfullyRemoved(), NotificationType.SUCCESS);
                    }
                });
    }
    
    private void updatePolylinePoints(MarkDTO mark) {
        MVCArray<LatLng> path = polylines.get(mark).getPath();
        path.clear();
        for (GPSFixDTO fix : marks.get(mark).keySet()) {
            path.push(LatLng.newInstance(fix.position.getLatDeg(), fix.position.getLngDeg()));
        }
    }
    
    public Point[] getSeriesPoints(Collection<GPSFixDTO> fixes) {
        double latAverage = 0;
        double lngAverage = 0;
        for (GPSFixDTO fix : fixes) {
            latAverage += fix.position.getLatDeg();
            lngAverage += fix.position.getLngDeg();
        }
        latAverage /= fixes.size();
        lngAverage /= fixes.size();
        Position averagePosition = new DegreePosition(latAverage, lngAverage);
        
        Point[] points = new Point[fixes.size()];
        int i = 0;
        for (GPSFixDTO fix : fixes) {
            final double metersFromAverage = fix.position.getDistance(averagePosition).getMeters();
            points[i] = new Point(fix.timepoint.getTime(), metersFromAverage);
            i++;
        }
        return points;
    }
    
    private void setSeriesPoints(MarkDTO mark) {
        setSeriesPoints(markSeries, getSeriesPoints(marks.get(mark).keySet()), /* manageZoom */ true);
    }
    
    public void setSeriesPoints(Point[] points) {
        setSeriesPoints(markSeries, points, /* manageZoom */ true);
    }

    public void setVisible(boolean visible) {
        if (visible && !isVisible()) {
            checkIfTracking(() -> {});
        }
        if (map == null) {
            map = raceMap.getMap();
            if (map != null) {
                setMap(map);
            }
        }
        if (visible) {
            if (sideBySideComponentViewer != null) {
                sideBySideComponentViewer.setLeftComponent(marksPanel);
                sideBySideComponentViewer.setLeftComponentToggleButtonVisible(false);
            }
            for (final CourseMarkOverlay overlay : raceMap.getCourseMarkOverlays().values()) {
                courseMarkClickHandlers.add(overlay.addClickHandler(new ClickMapHandler() {
                    @Override
                    public void onEvent(ClickMapEvent event) {
                        marksPanel.select(overlay.getMark());
                    }
                }));
            }
            raceMap.unregisterAllCourseMarkInfoWindowClickHandlers();
        } else {
            cancelFixPositionChooserAndNotification();
            marksPanel.deselectMark();
            selectedMark = null;
            if (sideBySideComponentViewer != null) {
                sideBySideComponentViewer.setLeftComponent(leaderboardPanel);
                sideBySideComponentViewer.setLeftComponentToggleButtonVisible(true);
            }
            for (HandlerRegistration registration : courseMarkClickHandlers) {
                registration.removeHandler();
            }
            raceMap.unregisterAllCourseMarkInfoWindowClickHandlers();
            raceMap.registerAllCourseMarkInfoWindowClickHandlers();
        }
        super.setVisible(visible);
    }

    private void cancelFixPositionChooserAndNotification() {
        if (currentFixPositionChooser != null) {
            currentFixPositionChooser.cancel();
            currentFixPositionChooser = null;
            if (notificationTimer.isRunning()) {
                notificationTimer.run();
                notificationTimer.cancel();
            }
        }
    }

    private void checkIfTracking(Runnable continuation) {
        if (nonTrackingWarningWasDisplayed) {
            continuation.run();
        } else {
            sailingService.checkIfRaceIsTracking(selectedRaceIdentifier, new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(stringMessages.serverError(), Notification.NotificationType.ERROR);
                    continuation.run();
                }

                @Override
                public void onSuccess(Boolean result) {
                    if (Boolean.FALSE.equals(result)) {
                        Notification.notify(stringMessages.positionEditOnNonTrackingRace(), Notification.NotificationType.WARNING);
                        nonTrackingWarningWasDisplayed = true;
                    }
                    continuation.run();
                }
            });
        }
    }
    
    @Override
    public void onResize() {
        if (selectedMark != null) {
            LatLngBounds bounds = null;
            if (marks != null) {
                for (GPSFixDTO fix : marks.get(selectedMark).keySet()) {
                    if (bounds == null) {
                        bounds = BoundsUtil.getAsBounds(raceMap.getCoordinateSystem().toLatLng(fix.position));
                    } else {
                        bounds = bounds.extend(raceMap.getCoordinateSystem().toLatLng(fix.position));
                    }
                }
            }
        }
        chart.setSizeToMatchContainer();
        chart.redraw();
    }
    
    public void redrawChart() {
        chart.redraw();
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.editMarkPositions();
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public SettingsDialogComponent<AbstractSettings> getSettingsDialogComponent(AbstractSettings settings) {
        return null;
    }

    @Override
    public void updateSettings(AbstractSettings newSettings) {
    }

    @Override
    public String getDependentCssClassName() {
        return null;
    }

    public void setMap(final MapWidget map) {
        this.map = map;
        for (Map.Entry<MarkDTO, Polyline> polyline : polylines.entrySet()) {
            polyline.getValue().setMap(map);
        }
    }
    
    public Date getTimepoint() {
       return timer.getTime();
    }

    @Override
    protected Button createSettingsButton() {
        Button settingsButton = SettingsDialog.createSettingsButton(this, stringMessages);
        settingsButton.setVisible(false);
        return settingsButton;
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if (isVisible()) {
            loadData();
            updateTimePlotLine(newTime);
        }
    }
    
    public void setComponentViewer(SideBySideComponentViewer sideBySideComponentViewer) {
        this.sideBySideComponentViewer = sideBySideComponentViewer;
    }
    
    private void selectMark(MarkDTO mark) {
        if (selectedMark != mark) {
            cancelFixPositionChooserAndNotification();
        }
        selectedMark = mark;
        if (selectedMark != null) {
            if (marksFromToTimes.get(selectedMark) != null) {
                // For some reason the time slider does not change with this method only if you comment out line 430 and 432 in TimePanel it works
                timeRangeWithZoomProvider.setTimeRange(marksFromToTimes.get(selectedMark).getA(), marksFromToTimes.get(selectedMark).getB());
            }
            setWidget(chart);
            markSeries.remove();
            markSeries.setPlotOptions(markSeriesPlotOptions.setMarker(
                    new Marker().setFillColor(selectedMark.color != null ? selectedMark.color.getAsHtml() : "#efab00")
                    .setLineColor("#fff").setLineWidth(2)));
            chart.addSeries(markSeries);
            setSeriesPoints(selectedMark);
            onResize(); // redraw chart
            hideAllCourseMarkOverlaysExceptSelected();
            raceMap.hideAllHelplines();
            if (marks != null) {
                for (final Entry<MarkDTO, SortedMap<GPSFixDTO, FixOverlay>> e : marks.entrySet()) {
                    for (CanvasOverlayV3 overlay : e.getValue().values()) {
                        // show only tail of selected mark, hide all others
                        overlay.setVisible(selectedMark.equals(e.getKey()));
                    }
                }
            }
            for (final Entry<MarkDTO, Polyline> e : polylines.entrySet()) {
                e.getValue().setVisible(selectedMark.equals(e.getKey()));
            }
        } else {
            if (raceFromTime != null && raceToTime != null) {
                timeRangeWithZoomProvider.setTimeRange(raceFromTime, raceToTime);
            }
            setWidget(noMarkSelectedLabel);
            showAllCourseMarkOverlays();
            raceMap.showAllHelplinesToShow();
            hideAllFixOverlays();
            hideAllPolylines();
        }
    }

    @Override
    public void onSelectionChange(SelectionChangeEvent event) {
        final MarkDTO mark = marksPanel.getSelectedMark();
        retrieveAndSelectMarkIfNecessary(mark, null);
    }

    protected void retrieveAndSelectMarkIfNecessary(final MarkDTO mark, final Runnable callback) {
        if (mark != null && (marks.get(mark) == null || marks.get(mark).isEmpty())) {
            
            if (marksCurrentlyRequestedViaRemoteCall.add(mark)) {
                markPositionService.getMarkTrack(
                        raceIdentifierToLeaderboardRaceColumnAndFleetMapper
                                .getLeaderboardNameAndRaceColumnNameAndFleetName(selectedRaceIdentifier),
                        mark.getIdAsString(), new AsyncCallback<MarkTrackDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                marksCurrentlyRequestedViaRemoteCall.remove(mark);
                                errorReporter.reportError(
                                        stringMessages.errorCommunicatingWithServer() + ": " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(MarkTrackDTO result) {
                                marksCurrentlyRequestedViaRemoteCall.remove(mark);
                                createMarkTrackUi(mark, result.getFixes());
                                selectMark(mark);
                                if (callback != null) {
                                    callback.run();
                                }
                            }
                        });
            }
            else {
                // remote call of the same mark already in progress -> ignore this request
            }
        } else {
            selectMark(mark);
            if (callback != null) {
                callback.run();
            }
        }
    }
    
    public void showAllCourseMarkOverlays() {
        for (final CourseMarkOverlay overlay : raceMap.getCourseMarkOverlays().values()) {
            overlay.setVisible(true);
        }
    }
    
    public void hideAllCourseMarkOverlaysExceptSelected() { 
        for (Map.Entry<String, CourseMarkOverlay> overlay : raceMap.getCourseMarkOverlays().entrySet()) {
            overlay.getValue().setVisible(overlay.getKey().equals(selectedMark.getIdAsString()));
        }
    }
    
    public void hideAllFixOverlays() {
        if (marks != null) {
            for (Map.Entry<MarkDTO, SortedMap<GPSFixDTO, FixOverlay>> mark : marks.entrySet()) {
                for (CanvasOverlayV3 overlay : mark.getValue().values()) {
                    overlay.setVisible(false);
                }
            }
        }
    }
    
    public void hideAllPolylines() {
        for (Map.Entry<MarkDTO, Polyline> polyline : polylines.entrySet()) {
            polyline.getValue().setVisible(false);
        }
    }

    /**
     * Call {@link #resetCurrentFixPositionChooser()} after the fix position chooser is done.
     * 
     * @throws MultipleFixPositionChooserException
     *             Is thrown when another fix position chooser is still open
     */
    public void createFixPositionChooserToAddFixToMark(MarkDTO mark, Callback<Position, Exception> callback) {
        if (currentFixPositionChooser != null) {
            return;
        }
        checkIfTracking(() -> {
            int index = 0;
            for (GPSFixDTO fix : marks.get(mark).keySet()) {
                if (fix.timepoint.after(timer.getTime()))
                    break;
                index++;
            }
            Polyline polyline = polylines.get(mark);
            currentFixPositionChooser = new FixPositionChooser(EditMarkPositionPanel.this, stringMessages, map, index,
                    polyline != null ? polyline.getPath() : null, map.getCenter(), raceMap.getCoordinateSystem(),
                    callback);
        });
    }
    
    public void resetCurrentFixPositionChooser() {
        currentFixPositionChooser = null;
    }
    
    private com.google.gwt.user.client.Timer notificationTimer;
    
    public enum NotificationType {
        INFO,
        ERROR,
        SUCCESS
    }
    
    public void showNotification(String message) {
        showNotification(message, NotificationType.INFO);
    }
    
    public void showNotification(String message, NotificationType type) {
        if (notificationTimer != null && notificationTimer.isRunning()) {
            notificationTimer.run();
        }
        final HTMLPanel text = new HTMLPanel("<div style = \" color: " + (type == NotificationType.ERROR ? "#c00" : type == NotificationType.SUCCESS ? "#0c0" : "#000") + "\">" + message + "</div>");
        text.setStyleName("EditMarkPositionNotification");
        map.setControls(ControlPosition.TOP_CENTER, text);

        notificationTimer = new com.google.gwt.user.client.Timer(){
            @Override
            public void run() {
                text.removeFromParent();
            }
        };
        notificationTimer.schedule(5000);
    }

    public boolean hasFixAtTimePoint(MarkDTO mark, Date timePoint) {
        boolean result = false;
        for (GPSFixDTO fix : marks.get(mark).keySet()) {
            if (fix.timepoint.equals(timePoint)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    public List<GPSFixDTO> getMarkFixes() {
        if (selectedMark != null) {
            List<GPSFixDTO> set = new ArrayList<>();
            set.addAll(marks.get(selectedMark).keySet());
            return set;
        } else {
            return null;
        }
    }
    
    public XAxis getXAxis() {
        return chart.getXAxis();
    }

    public void setLeaderboard(LeaderboardDTO leaderboard) {
        raceIdentifierToLeaderboardRaceColumnAndFleetMapper.setLeaderboard(leaderboard);
    }

    @Override
    public AbstractSettings getSettings() {
        return null;
    }

    @Override
    public String getId() {
        return "EditMarkPositionPanel";
    }

    @Override
    public void checkBackendAvailability(Consumer<Boolean> callback) {
        HasAvailabilityCheck.validateBackendAvailabilityAndExecuteBusinessLogic(sailingServiceWrite, callback,
                stringMessages);
    }
}
