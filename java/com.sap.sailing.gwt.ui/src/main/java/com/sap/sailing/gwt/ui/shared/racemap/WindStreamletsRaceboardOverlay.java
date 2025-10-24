package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.TextMetrics;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.gwt.ui.actions.GetWindInfoAction;
import com.sap.sailing.gwt.ui.client.NumberFormatterFactory;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.gwt.ui.simulator.StreamletParameters;
import com.sap.sailing.gwt.ui.simulator.racemap.MovingCanvasOverlay;
import com.sap.sailing.gwt.ui.simulator.streamlets.Swarm;
import com.sap.sailing.gwt.ui.simulator.streamlets.VectorField;
import com.sap.sailing.gwt.ui.simulator.streamlets.WindInfoForRaceVectorField;
import com.sap.sse.common.ColorMapperChangedListener;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.mutationobserver.ElementStyleMutationObserver;
import com.sap.sse.gwt.client.mutationobserver.ElementStyleMutationObserver.DomStyleMutationCallback;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;

/**
 * A Google Maps overlay based on an HTML5 canvas for drawing a wind field. The {@link VectorField} implementation used
 * is the {@link WindInfoForRaceVectorField} which takes a {@link WindInfoForRaceDTO} as its basis.
 * 
 * @author Christopher Ronnewinkel (D036654)
 * @author Axel Uhl (D043530)
 * 
 */
public class WindStreamletsRaceboardOverlay extends MovingCanvasOverlay implements ColorMapperChangedListener, TimeListener {
    public static final String LOAD_WIND_STREAMLET_DATA_CATEGORY = "loadWindStreamletData";
    private static final int animationIntervalMillis = 40;
    private static final long RESOLUTION_IN_MILLIS = 5000;
    private static final int WIND_FETCH_INTERVAL_IN_MILLIS = 10000;
    private static final int CHECK_WIND_SOURCE_INTERVAL_IN_MILLIS = 60000;

    private boolean visible = false;
    private final Timer timer;
    private Swarm swarm;
    private final WindInfoForRaceVectorField windField;
    private final WindInfoForRaceDTO windInfoForRace;
    private final RegattaAndRaceIdentifier raceIdentifier;
    private final SailingServiceAsync sailingService;
    private final StringMessages stringMessages;
    private final AsyncActionsExecutor asyncActionsExecutor;
    private final Scheduler scheduler;
    private Canvas streamletLegend;
    private boolean firstColoring = true;
    private boolean colored;
    private final NumberFormat numberFormatOneDecimal = NumberFormatterFactory.getDecimalFormat(1);
    private ElementStyleMutationObserver observer;
    private boolean dragging = false;
    private boolean isAttached = false, startObserverWhenAttached = false;
    private boolean timeChangedSinceLastUpdate = true;

    public WindStreamletsRaceboardOverlay(MapWidget map, int zIndex, final Timer timer,
            RegattaAndRaceIdentifier raceIdentifier, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, StringMessages stringMessages,
            CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.scheduler = Scheduler.get();
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.stringMessages = stringMessages;
        this.raceIdentifier = raceIdentifier;
        this.sailingService = sailingService;
        this.windInfoForRace = new WindInfoForRaceDTO();
        windInfoForRace.raceIsKnownToStartUpwind = true; // default
        windInfoForRace.windSourcesToExclude = new HashSet<>();
        windInfoForRace.windTrackInfoByWindSource = new HashMap<>();
        this.windField = new WindInfoForRaceVectorField(windInfoForRace,
                /* frames per second */ 1000.0 / animationIntervalMillis, coordinateSystem);
        this.timer = timer;
        this.timer.addTimeListener(this);
        getCanvas().getElement().setId("swarm-display");
        createStreamletLegend(map);
    }

    private void createStreamletLegend(MapWidget map) {
        streamletLegend = Canvas.createIfSupported();
        streamletLegend.addStyleName("MapStreamletLegend");
        map.setControls(ControlPosition.LEFT_BOTTOM, streamletLegend);
        int canvasWidth = 500;
        int canvasHeight = 60;
        streamletLegend.getParent().addStyleName("MapStreamletLegendParentDiv");
        streamletLegend.setWidth(String.valueOf(canvasWidth));
        streamletLegend.setHeight(String.valueOf(canvasHeight));
        streamletLegend.setCoordinateSpaceWidth(canvasWidth);
        streamletLegend.setCoordinateSpaceHeight(canvasHeight);
    }

    public void drawLegend() {
        if (streamletLegend.isVisible()) {
            streamletLegend.getContext2d().clearRect(0, 0, streamletLegend.getCoordinateSpaceWidth(),
                    streamletLegend.getCoordinateSpaceHeight());
            if (swarm != null && swarm.isColored()) {
                final double x = 100;
                final double y = 16;
                final double w = 1;
                final double h = 20;
                final double speed_max = swarm.getValueRange().getMaxRight();
                final double speed_min = swarm.getValueRange().getMinLeft();
                final double speed_spread = speed_max - speed_min;
                final int scale_spread;
                if (speed_spread < 0.5) {
                    scale_spread = 300;
                } else if (speed_spread < 1) {
                    scale_spread = 100;
                } else {
                    scale_spread = 50;
                }
                final int maxIdx = 300;
                Context2d context2d = streamletLegend.getContext2d();
                context2d.setFillStyle("rgba(0,0,0,.3)");
                context2d.setLineWidth(1.0);
                context2d.beginPath();
                context2d.fillRect(x - 10.0, y - 16.0, w * maxIdx + 20.0, 56.0);
                context2d.closePath();
                context2d.stroke();
                context2d.setFillStyle("white");
                String label = stringMessages.windSpeedInKnots();
                TextMetrics txtmet;
                txtmet = context2d.measureText(label);
                context2d.fillText(label, x + (w * maxIdx - txtmet.getWidth()) / 2.0, y - 5.0);
                for (int idx = 0; idx <= maxIdx; idx++) {
                    final double speedSteps = speed_min + idx * (speed_spread) / maxIdx;
                    context2d.setFillStyle(swarm.getColorMapper().getColor(speedSteps));
                    context2d.beginPath();
                    context2d.fillRect(x + idx * w, y, w, h);
                    context2d.closePath();
                    context2d.stroke();
                    if (idx % scale_spread == 0) {
                        context2d.setStrokeStyle("white");
                        context2d.setLineWidth(1.0);
                        context2d.beginPath();
                        context2d.moveTo(x + idx * w, y + h);
                        context2d.lineTo(x + idx * w, y + h + 7.0);
                        context2d.closePath();
                        context2d.stroke();
                        context2d.setFillStyle("white");
                        label = numberFormatOneDecimal.format(speedSteps);
                        txtmet = context2d.measureText(label);
                        context2d.fillText(label, x + idx * w - txtmet.getWidth() / 2.0, y + h + 8.0 + 8.0);
                    }
                }
            }
        }
    }

    public void startStreamlets() {
        scheduleWindDataRefresh();
        if (swarm == null) {
            setCanvasSettings();
            this.swarm = new Swarm(this, map, timer, windField, new StreamletParameters());
        }
        initCanvasOrigin();
        this.swarm.setColors(colored);
        this.swarm.start(animationIntervalMillis);
        this.swarm.getColorMapper().addListener(this);
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        timeChangedSinceLastUpdate = true;
    }

    private void scheduleWindDataRefresh() {
        scheduler.scheduleFixedPeriod(new RepeatingCommand() {
            @Override
            public boolean execute() {
                if (timeChangedSinceLastUpdate) {
                    updateWindField();
                    timeChangedSinceLastUpdate = false;
                }
                return visible;
            }
        }, WIND_FETCH_INTERVAL_IN_MILLIS);
        scheduler.scheduleFixedPeriod(new RepeatingCommand() {
            @Override
            public boolean execute() {
                updateWindSourcesToObserve(/* runWhenDone */ null);
                return visible;
            }
        }, CHECK_WIND_SOURCE_INTERVAL_IN_MILLIS);
        // Now run things once, first updating the wind sources so that when streamlets are enabled, wind can be queried quickly:
        updateWindSourcesToObserve(new Runnable() {
            @Override
            public void run() {
                updateWindField();
            }
        });
    }

    private void stopStreamlets() {
        if (this.swarm != null) {
            this.swarm.stop();
            this.swarm.getColorMapper().removeListener(this);
            swarm = null;
        }
    }

    /**
     * Check which wind sources are available for the race identified by {@link #raceIdentifier}. For any wind source
     * not yet observed (contained in the keys of {@link #windInfoForRace}'s
     * {@link WindInfoForRaceDTO#windTrackInfoByWindSource windTrackInfoByWindSource} map, the wind source is added to
     * that map unless it's the {@link WindSourceType#COMBINED} wind source or the wind source is marked as excluded.
     */
    private void updateWindSourcesToObserve(final Runnable runWhenDone) {
        sailingService.getWindSourcesInfo(raceIdentifier,
                new MarkedAsyncCallback<>(new AsyncCallback<WindInfoForRaceDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(stringMessages.errorFetchingWindStreamletData(caught.getMessage()),
                                NotificationType.WARNING);
                    }

                    @Override
                    public void onSuccess(WindInfoForRaceDTO result) {
                        windInfoForRace.raceIsKnownToStartUpwind = result.raceIsKnownToStartUpwind;
                        windInfoForRace.windSourcesToExclude = result.windSourcesToExclude;
                        for (Entry<WindSource, WindTrackInfoDTO> e : result.windTrackInfoByWindSource.entrySet()) {
                            if (!windInfoForRace.windTrackInfoByWindSource.containsKey(e.getKey())
                                    && !Util.contains(result.windSourcesToExclude, e.getKey())
                                    && e.getKey().getType() != WindSourceType.COMBINED) {
                                windInfoForRace.windTrackInfoByWindSource.put(e.getKey(), e.getValue());
                            }
                        }
                        if (runWhenDone != null) {
                            runWhenDone.run();
                        }
                    }
                }));
    }

    /**
     * Fetches data for the {@link Timer#getTime() current time} set in the {@link #timer}. For each wind source to observe,
     * a single averaged fix for the current time is requested which is then used to replace the vector field's contents.
     * This way, the vector field has constant memory footprint, wind queries are fast and independent of race length,
     * and wind track time resolution.
     */
    private void updateWindField() {
        final Set<String> windSourceTypeNames = windInfoForRace.windTrackInfoByWindSource.keySet().stream().map(
                windSource->windSource.getType().name()).collect(Collectors.toSet());
        GetWindInfoAction getWind = new GetWindInfoAction(sailingService, raceIdentifier, timer.getTime(), RESOLUTION_IN_MILLIS,
                /* number of fixes */ 1, windSourceTypeNames, /* onlyUpToNewestEvent */ true);
        asyncActionsExecutor.execute(getWind, LOAD_WIND_STREAMLET_DATA_CATEGORY,
                new MarkedAsyncCallback<>(new AsyncCallback<WindInfoForRaceDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(stringMessages.errorFetchingWindStreamletData(caught.getMessage()),
                                NotificationType.WARNING);
                    }

                    @Override
                    public void onSuccess(WindInfoForRaceDTO result) {
                        if (result != null) {
                            windField.updateWindInfo(result);
                        }
                    }
                }));
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    protected void onAttach() {
        isAttached = true;
        if (startObserverWhenAttached) {
            Scheduler.get().scheduleDeferred(() -> addObserverIfNecessary());
        }
    }

    @Override
    public void setVisible(boolean isVisible) {
        if (getCanvas() != null) {
            if (isVisible) {
                this.startStreamlets();
                if (colored) {
                    this.streamletLegend.setVisible(true);
                }
                swarm.setColors(colored);
                if (isAttached) {
                    Scheduler.get().scheduleDeferred(() -> addObserverIfNecessary());
                } else {
                    startObserverWhenAttached = true;
                }
                this.visible = isVisible;
            } else {
                if (swarm != null && this.swarm.isColored()) {
                    this.streamletLegend.setVisible(false);
                }
                this.stopStreamlets();
                removeObserverIfPresent();
                this.visible = isVisible;
            }
        }
    }

    public void setColors(boolean isColored) {
        this.colored = isColored;
        if ((isColored) && (firstColoring)) {
            firstColoring = false;
        }
        if (visible) {
            this.streamletLegend.setVisible(isColored);
        }
        if (swarm != null) {
            swarm.setColors(isColored);
        }
    }

    @Override
    public void removeFromMap() {
        this.setVisible(false);
    }

    @Override
    protected void drawCenterChanged() {
    }

    /** removes the mutation observer if one is present */
    protected void removeObserverIfPresent() {
        if (observer != null) {
            observer.disconnect();
            observer = null;
        }
    }

    /**
     * adds a mutation observer to the map canvas to react on style changes, if the transform property changes because
     * the user pans the map around
     */
    private void addObserverIfNecessary() {
        if (ElementStyleMutationObserver.isSupported() && observer == null) {
            observer = new ElementStyleMutationObserver(new DomStyleMutationCallback() {

                @Override
                public void onStyleChanged() {
                    if (isVisible()) {
                        setCanvasSettings();
                    }
                }
            });
            observer.observe(super.canvas.getElement().getParentElement().getParentElement());
        }
    }

    public void onDragStart() {
        dragging = true;
        swarm.pause(500 / animationIntervalMillis); // pause animation for two seconds
    }

    public void onDragEnd() {
        dragging = false;
    }

    public void onBoundsChanged(boolean zoomChanged) {
        if (swarm != null) {
            if (dragging) {
                swarm.pause(300/animationIntervalMillis);
            } else {
                int swarmPause = ((zoomChanged ? 1000 : 300)/animationIntervalMillis);
                swarm.onBoundsChanged(zoomChanged, swarmPause);
            }
        }
    }

    @Override
    public void onColorMappingChanged() {
        drawLegend();
    }
}
