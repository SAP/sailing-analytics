package com.sap.sailing.gwt.ui.client;

import java.util.Date;
import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.TimePanelCssResources.TimePanelCss;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.util.IntHolder;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.TimeRangeActionsExecutor;
import com.sap.sse.gwt.client.controls.slider.SliderBar;
import com.sap.sse.gwt.client.controls.slider.TimeSlider;
import com.sap.sse.gwt.client.controls.slider.TimeSlider.BarOverlay;
import com.sap.sse.gwt.client.player.PlayStateListener;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.TimeRangeChangeListener;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.TimeZoomChangeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.UserService;

public class TimePanel<T extends TimePanelSettings> extends AbstractCompositeComponent<T> implements TimeListener, TimeZoomChangeListener,
    TimeRangeChangeListener, PlayStateListener, RequiresResize {
    protected final Timer timer;
    protected final TimeRangeWithZoomProvider timeRangeProvider;
    
    /**
     * Tells whether the user shall be enabled to run a replay with the timer auto-advancing while the race is still live.
     * Live races are often watched by many. If in addition to that several users run the live race based on their own time line,
     * too many re-calculations will be triggered as the live data will constantly invalidate many caches. Therefore, if scaling
     * to many concurrent users is an issue, the capability to replay a live race with a non-live timing may be restricted using
     * this attribute.
     */
    private final boolean canReplayWhileLiveIsPossible;
    
    private final IntegerBox playSpeedBox;
    private final Label timeDelayLabel;
    private final Label timeLabel;
    private final Label dateLabel;
    private final Label timeToStartLabel;
    private final FlowPanel timeToStartControlPanel;
    private final Label playModeLabel;
    protected final TimeSlider timeSlider;
    private final Button backToLivePlayButton;
    protected final StringMessages stringMessages;
    protected final DateTimeFormat dateFormatter = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_FULL); 
    protected final DateTimeFormat timeFormatter = DateTimeFormat.getFormat("HH:mm:ss");
    protected final DateTimeFormat timeFormatterDetailed = DateTimeFormat.getFormat("HH:mm:ss:SS"); 
    private final ImageResource playSpeedImg;
    private final ImageResource playModeLiveActiveImg;
    private final ImageResource playModeReplayActiveImg;
    private final ImageResource playModeInactiveImg;
    private final Button playPauseButton;
    private final Image playModeImage;
    protected Date lastReceivedDataTimepoint;
    private final Button slowDownButton;
    private final Button speedUpButton;
    private final Button toggleAdvancedModeButton;
    private final Button resetZoomButton;

    private final FlowPanel controlsPanel;
    private final SimplePanel timePanelSlider;
    private final FlowPanel timePanelSliderFlowWrapper;
    private final FlowPanel playControlPanel;
    private final FlowPanel timePanelInnerWrapper;

    private final SecuredDTO raceDTO;

    /** 
     * the minimum time the slider extends its time when the end of the slider is reached
     */
    private long MINIMUM_AUTO_ADVANCE_TIME_IN_MS = 5 * 60 * 1000; // 5 minutes
    private boolean advancedModeShown;
    
    private static ClientResources resources = GWT.create(ClientResources.class);
    protected static TimePanelCss timePanelCss = TimePanelCssResources.INSTANCE.css();

    private final boolean forcePaddingRightToAlignToCharts;
    private UserService userService;

    /**
     * @param forcePaddingRightToAlignToCharts
     *            if <code>true</code>, the right padding will always be set such that the time panel lines up with
     *            charts such as the competitor chart or the wind chart shown above it, otherwise the padding depends on
     *            the flag set by {@link #setLiveGenerallyPossible(boolean)}
     * @param pendingServerOperationsCountSupplier
     *            an optional {@link AsyncActionsExecutor} that, if not {@code null}, will be used to display the
     *            {@link AsyncActionsExecutor#getNumberOfPendingActions() number of pending actions} in the advanced
     *            sub-panel of this time panel. Updates are handled using the
     *            {@link AsyncActionsExecutor#addListener(com.sap.sse.gwt.client.async.AsyncActionsExecutor.Listener)
     *            listener} support.
     */
    public TimePanel(Component<?> parent, ComponentContext<?> context, Timer timer,
            TimeRangeWithZoomProvider timeRangeProvider,
            StringMessages stringMessages,
            boolean canReplayWhileLiveIsPossible, boolean forcePaddingRightToAlignToCharts, UserService userService,
            final SecuredDTO raceDTO, AsyncActionsExecutor pendingServerOperationsCountSupplier,
            TimeRangeActionsExecutor<?, ?, ?> pendingTimeRangeActionsSupplier) {
        super(parent, context);
        this.raceDTO = raceDTO;
        this.userService = userService;
        this.timer = timer;
        this.timeRangeProvider = timeRangeProvider;
        this.stringMessages = stringMessages;
        this.canReplayWhileLiveIsPossible = canReplayWhileLiveIsPossible;
        this.forcePaddingRightToAlignToCharts = forcePaddingRightToAlignToCharts;
        timer.addTimeListener(this);
        timer.addPlayStateListener(this);
        timeRangeProvider.addTimeRangeChangeListener(this);
        timePanelInnerWrapper = new FlowPanel();
        timePanelInnerWrapper.setStyleName("timePanelInnerWrapper");
        timePanelInnerWrapper.setSize("100%", "100%");
        // slider panel
        timePanelSlider = new SimplePanel();
        timePanelSliderFlowWrapper = new FlowPanel();
        timePanelSlider.setStyleName("timePanelSlider");
        timePanelSlider.getElement().getStyle().setPaddingLeft(66, Unit.PX);
        timePanelSliderFlowWrapper.add(timePanelSlider);
        // play/pause
        playSpeedImg = resources.timesliderPlaySpeedIcon();
        playPauseButton = new Button("");
        // play mode visualization
        playModeLiveActiveImg = resources.timesliderPlayStateLiveActiveIcon();
        playModeReplayActiveImg = resources.timesliderPlayStateReplayActiveIcon();
        playModeInactiveImg = resources.timesliderPlayStateLiveInactiveIcon();
        playModeImage = new Image(playModeInactiveImg);
        // time slider
        timeSlider = new TimeSlider();
        timeSlider.setEnabled(true);
        timeSlider.setLabelFormatter(new SliderBar.LabelFormatter() {
            final DateTimeFormat timeWithMinutesFormatter = DateTimeFormat.getFormat("HH:mm"); 
            @Override
            public String formatLabel(SliderBar slider, Double value, Double previousValue) {
                Date date = new Date(value.longValue());
                return timeWithMinutesFormatter.format(date); 
            }
        });
        timeSlider.ensureDebugId("timeSlider");
        timeSlider.addValueChangeHandler(new ValueChangeHandler<Double>() {
            @Override
            public void onValueChange(ValueChangeEvent<Double> newValue) {
                if (timeSlider.getCurrentValue() != null) {
                    if (TimePanel.this.timer.getPlayMode() == PlayModes.Live) {
                        // if canReplayWhileLive==false, playStateChanged(...) will ensure that the timer is paused when
                        // reaching Replay mode
                        TimePanel.this.timer.setPlayMode(PlayModes.Replay);
                    }
                    TimePanel.this.timer.setTime(timeSlider.getCurrentValue().longValue());
                }
            }
        });
        timePanelInnerWrapper.add(timePanelSliderFlowWrapper);
        timePanelSlider.add(timeSlider);
        controlsPanel = new FlowPanel();
        controlsPanel.setStyleName("timePanel-controls");
        timePanelInnerWrapper.add(controlsPanel);
        // play button control
        playControlPanel = new FlowPanel();
        playControlPanel.setStyleName("timePanel-controls-play");
        controlsPanel.add(playControlPanel);
        toggleAdvancedModeButton = createToggleAdvancedModeButton();
        playControlPanel.add(toggleAdvancedModeButton);
        playPauseButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                switch(TimePanel.this.timer.getPlayState()) {
                    case Paused:
                        TimePanel.this.timer.play();
                        break;
                    case Playing:
                        TimePanel.this.timer.pause();
                        break;
                }
            }
        });
        playPauseButton.setTitle(stringMessages.startStopPlaying());
        playPauseButton.setStyleName("playPauseButton");
        playControlPanel.add(playPauseButton);
        backToLivePlayButton = new Button(stringMessages.raceIsInLiveTimePanelMode());
        backToLivePlayButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                TimePanel.this.timer.setPlayMode(PlayModes.Live);
                TimePanel.this.timer.play();
            }
        });
        backToLivePlayButton.setStyleName("backToLivePlayButton");
        backToLivePlayButton.setTitle(stringMessages.backToLive());
        playControlPanel.add(backToLivePlayButton);

        // current date and time control
        FlowPanel timeControlPanel = new FlowPanel();
        timeControlPanel.setStyleName("timePanel-controls-time");
        dateLabel = new Label();
        timeLabel = new Label();
        timeControlPanel.add(dateLabel);
        timeControlPanel.add(timeLabel);
        dateLabel.getElement().setClassName("dateLabel");
        timeLabel.getElement().setClassName("timeLabel");
        // time to start control
        timeToStartControlPanel = new FlowPanel();
        timeToStartControlPanel.setStyleName("timePanel-controls-timeToStart");
        timeToStartLabel = new Label();
        timeToStartControlPanel.add(timeToStartLabel);
        timeToStartLabel.getElement().setClassName("timeToStartLabel");
        FlowPanel playModeControlPanel = new FlowPanel();
        playModeControlPanel.setStyleName("timePanel-controls-playmode");
        playModeControlPanel.add(playModeImage);
        playModeImage.getElement().getStyle().setFloat(Style.Float.LEFT);
        playModeLabel = new Label();
        playModeControlPanel .add(playModeLabel);
        playModeLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        playModeLabel.getElement().setClassName("playModeLabel");
        // play speed controls
        FlowPanel playSpeedControlPanel = new FlowPanel();
        playSpeedControlPanel.setStyleName("timePanel-controls-playSpeed");
        playSpeedBox = new IntegerBox();
        playSpeedBox.setVisibleLength(3);
        playSpeedBox.setValue((int)timer.getPlaySpeedFactor()); // Christopher: initialize play speed box according to play speed factor
        playSpeedBox.setTitle(stringMessages.playSpeedHelp());
        playSpeedBox.addValueChangeHandler(new ValueChangeHandler<Integer>() {
            @Override
            public void onValueChange(ValueChangeEvent<Integer> event) {
                Integer newPlaySpeedFactor = playSpeedBox.getValue() == null ? 0 : playSpeedBox.getValue();
                TimePanel.this.timer.setPlaySpeedFactor(newPlaySpeedFactor);
            }
        });
        Image playSpeedImage = new Image(playSpeedImg);
        playSpeedControlPanel.add(playSpeedImage);
        playSpeedControlPanel.add(playSpeedBox);
        slowDownButton = new Button("-1");
        slowDownButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                playSpeedBox.setValue(playSpeedBox.getValue() == null ? 0 : playSpeedBox.getValue() - 1);
                TimePanel.this.timer.setPlaySpeedFactor(playSpeedBox.getValue());
            }
        });
        slowDownButton.setTitle(stringMessages.slowPlaySpeedDown());
        slowDownButton.addStyleName("timePanelButton-SlowDown");
        playSpeedControlPanel.add(slowDownButton);
        speedUpButton = new Button("+1");
        speedUpButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                playSpeedBox.setValue(playSpeedBox.getValue() == null ? 0 : playSpeedBox.getValue() + 1);
                TimePanel.this.timer.setPlaySpeedFactor(playSpeedBox.getValue());
            }
        });
        speedUpButton.setTitle(stringMessages.speedPlaySpeedUp());
        speedUpButton.addStyleName("timePanelButton-SpeedUp");
        playSpeedControlPanel.add(speedUpButton);
        playSpeedImage.getElement().getStyle().setFloat(Style.Float.LEFT);
        playSpeedImage.getElement().getStyle().setPadding(3, Style.Unit.PX);
        playSpeedImage.getElement().getStyle().setMarginRight(3, Style.Unit.PX);
        playSpeedBox.getElement().getStyle().setFloat(Style.Float.LEFT);
        playSpeedBox.getElement().getStyle().setPadding(2, Style.Unit.PX);
        speedUpButton.addStyleName("timePanelButton");
        slowDownButton.addStyleName("timePanelButton");
        // time delay
        FlowPanel timeDelayPanel = new FlowPanel();
        timeDelayPanel.setStyleName("timePanel-controls-timeDelay");
        timeDelayLabel = new Label();
        timeDelayPanel.add(timeDelayLabel);
        timeDelayLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        timeDelayLabel.getElement().getStyle().setPadding(3, Style.Unit.PX);
        timePanelCss.ensureInjected();
        controlsPanel.add(createSettingsButton());
        initWidget(timePanelInnerWrapper);
        playStateChanged(timer.getPlayState(), timer.getPlayMode());
        controlsPanel.add(playSpeedControlPanel);
        controlsPanel.add(playModeControlPanel);
        controlsPanel.add(timeDelayPanel);
        controlsPanel.add(timeControlPanel);
        controlsPanel.add(timeToStartControlPanel);
        resetZoomButton = new Button(stringMessages.resetZoom());
        resetZoomButton.setEnabled(false);
        resetZoomButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                timeRangeProvider.resetTimeZoom();
            }
        });
        controlsPanel.add(resetZoomButton);
        if (pendingServerOperationsCountSupplier != null) {
            final IntHolder pendingServerOperationsCount = new IntHolder(0);
            final IntHolder pendingTimeRangeActionsCount = new IntHolder(0);
            final Label pendingServerOperationsCounter = new Label(getPendingServerOperationsCounterLabelText(pendingServerOperationsCountSupplier));
            pendingServerOperationsCounter.addStyleName("timePanel-controls-pendingServerOperationsCounter");
            controlsPanel.add(pendingServerOperationsCounter);
            pendingServerOperationsCountSupplier.addListener(
                    newPendingOperationsCount->{
                        pendingServerOperationsCount.value = newPendingOperationsCount;
                        updatePendingOperationsLabel(pendingServerOperationsCountSupplier, pendingServerOperationsCount,
                                pendingTimeRangeActionsCount, pendingServerOperationsCounter);
                    });
            if (pendingTimeRangeActionsSupplier != null) {
                pendingTimeRangeActionsSupplier.addListener(newPendingTimeRangeActions->{
                    pendingTimeRangeActionsCount.value = newPendingTimeRangeActions;
                    updatePendingOperationsLabel(pendingServerOperationsCountSupplier, pendingServerOperationsCount,
                            pendingTimeRangeActionsCount, pendingServerOperationsCounter);
                });
            }
        }
        hideControlsPanel();
    }

    private void updatePendingOperationsLabel(AsyncActionsExecutor pendingServerOperationsCountSupplier,
            final IntHolder pendingServerOperationsCount, final IntHolder pendingTimeRangeActionsCount,
            final Label pendingServerOperationsCounter) {
        final int EXPECTED_MAX_NUMBER_OF_PENDING_TIME_RANGE_ACTIONS = 10;
        final int numberOfPendingServerOperations = pendingServerOperationsCount.value+pendingTimeRangeActionsCount.value;
        pendingServerOperationsCounter.setText(stringMessages.pendingServerOperations(numberOfPendingServerOperations));
        pendingServerOperationsCounter.getElement().getStyle().setProperty("backgroundSize",
                (numberOfPendingServerOperations > 0
                ? "max(calc("+(100*numberOfPendingServerOperations/(AsyncActionsExecutor.DEFAULT_MAX_PENDING_CALLS+EXPECTED_MAX_NUMBER_OF_PENDING_TIME_RANGE_ACTIONS))+"% - 30px), 10px)"
                : "0") + " 100%");
    }

    private String getPendingServerOperationsCounterLabelText(AsyncActionsExecutor pendingServerOperationsCountSupplier) {
        return stringMessages.pendingServerOperations(pendingServerOperationsCountSupplier.getNumberOfPendingActions());
    }
    
    public TimeRangeWithZoomProvider getTimeRangeProvider() {
        return timeRangeProvider;
    }

    private Button createSettingsButton() {
        Button settingsButton = SettingsDialog.<T>createSettingsButton(this, stringMessages);
        settingsButton.setStyleName(timePanelCss.settingsButtonStyle());
        settingsButton.addStyleName(timePanelCss.settingsButtonBackgroundImage());
        return settingsButton;
    }
    
    private Button createToggleAdvancedModeButton() {
        Button toggleAdvancedModeButton = new Button();
        toggleAdvancedModeButton.setStyleName("TimePanel-ShowExtended-Button");
        toggleAdvancedModeButton.addStyleDependentName("Closed");
        return toggleAdvancedModeButton;
    }
    
    public boolean toggleAdvancedMode() {
        if (advancedModeShown) {
            hideControlsPanel();
        } else {
            showControlsPanel();
        }
        this.advancedModeShown = !advancedModeShown;
        return this.advancedModeShown;
    }

    protected void hideControlsPanel() {
        controlsPanel.remove(playControlPanel);
        timePanelInnerWrapper.remove(controlsPanel);
        timePanelSliderFlowWrapper.insert(playControlPanel, 0);
        playControlPanel.setStyleName("timePanel-timeslider-play");
    }

    protected void showControlsPanel() {
        timePanelInnerWrapper.add(controlsPanel);
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if (timeRangeProvider.isZoomed()) {
            timeSlider.setCurrentValue(Double.valueOf(newTime.getTime()), false);
        } else {
            if (getFromTime() == null || getToTime() == null) {
                initializeTimeRange(oldTime, newTime);
            }
            if (getFromTime() != null && getToTime() != null) {
                // handle the case where time advances beyond slider's end.
                if (newTime.after(getToTime())) {
                    // in live mode, expand the range
                    switch (timer.getPlayMode()) {
                    case Live:
                        Date newMaxTime = new Date(newTime.getTime());
                        if (newMaxTime.getTime() - getToTime().getTime() < MINIMUM_AUTO_ADVANCE_TIME_IN_MS) {
                            newMaxTime.setTime(getToTime().getTime() + MINIMUM_AUTO_ADVANCE_TIME_IN_MS); 
                        }
                        setMinMax(getFromTime(), newMaxTime, /* fireEvent */ false); // no event because we guarantee that time is between min/max
                        break;
                    case Replay:
                        // in replay, stop playing when the end of the range has been reached
                        timer.pause();
                        break;
                    }
                }
                timeSlider.setCurrentValue(Double.valueOf(newTime.getTime()), false);
            }
        }
        dateLabel.setText(getDateLabelText(newTime));
        timeLabel.setText(getTimeLabelText(newTime));
        String timeToStartLabelText = getTimeToStartLabelText(newTime);
        if (timeToStartLabelText != null && !timeToStartLabelText.isEmpty()) {
            timeToStartControlPanel.setVisible(true);
            timeToStartLabel.setText(getTimeToStartLabelText(newTime));
        } else {
            timeToStartControlPanel.setVisible(false);
            timeToStartLabel.setText("");
        }
    }

    /**
     * When the time range is not yet properly initialized and we get a time change, adjust the time range so that old and
     * new time are in range. A margin of one minute is used before and after. This method assumes that at least one of the
     * two time points passed as parameters has a value other than <code>null</code>. Should the other be <code>null</code>,
     * it is ignored. No assumptions are made about the temporal relation of the two time points to each other; in other
     * words, <code>oldTime</code> may also be after <code>newTime<code> which could, e.g., be the case for a timer running
     * backwards. 
     */
    private void initializeTimeRange(Date oldTime, Date newTime) {
        TimePoint from = Util.getEarliestOfTimePoints(getFromTime()==null?null:new MillisecondsTimePoint(getFromTime()), oldTime==null?null:new MillisecondsTimePoint(oldTime));
        from = Util.getEarliestOfTimePoints(from, newTime==null?null:new MillisecondsTimePoint(newTime));
        TimePoint to = Util.getEarliestOfTimePoints(getToTime()==null?null:new MillisecondsTimePoint(getToTime()), oldTime==null?null:new MillisecondsTimePoint(oldTime));
        from = Util.getEarliestOfTimePoints(to, newTime==null?null:new MillisecondsTimePoint(newTime));
        timeRangeProvider.setTimeRange(from==null?null:from.asDate(), to==null?null:to.asDate());
    }

    protected String getTimeToStartLabelText(Date time) {
        return null;
    }

    protected String getTimeLabelText(Date time) {
        final String timeLabelText;
        DateTimeFormat formatter = timeFormatter;
        if (timer.getRefreshInterval() < 1000) {
            formatter = timeFormatterDetailed;
        }
        if (lastReceivedDataTimepoint == null) {
            timeLabelText = formatter.format(time);
        } else {
            timeLabelText = formatter.format(time) + " (" + formatter.format(lastReceivedDataTimepoint) + ")";
        }
        return timeLabelText;
    }

    protected String getDateLabelText(Date time) {
        return dateFormatter.format(time);
    }

    protected Date getFromTime() {
        return timeRangeProvider.getFromTime();
    }
    
    protected Date getToTime() {
        return timeRangeProvider.getToTime();
    }
    
    /**
     * @param min must not be <code>null</code>
     * @param max must not be <code>null</code>
     */
    public void setMinMax(Date min, Date max, boolean fireEvent) {
        assert min != null && max != null;
        final Optional<Pair<Double, Double>> changed =
                timeSlider.setMinAndMaxValue(Double.valueOf(min.getTime()), Double.valueOf(max.getTime()), fireEvent);
        if (changed.isPresent()) {
            final Double changedMin = changed.get().getA();
            final Double changedMax = changed.get().getB();
            if (!timeRangeProvider.isZoomed()) {
                timeRangeProvider.setTimeRange(new Date(changedMin.longValue()), new Date(changedMax.longValue()), this);
            }
            int numSteps = timeSlider.getElement().getClientWidth();
            if (numSteps > 0) {
                timeSlider.setStepSize(numSteps, fireEvent);
            } else {
                timeSlider.setStepSize(1000, fireEvent);
            }
            // Christopher: following setCurrentValue requires stepsize to be set <> 0 (otherwise division by zero; NaN)
            if (timeSlider.getCurrentValue() == null) {
                timeSlider.setCurrentValue(changedMin, fireEvent);
            }
        }
    }

    public void resetTimeSlider() {
        timeSlider.clearMarkers();
        timeSlider.redraw();
    }
    
    /**
     * Makes sure that the invariant regarding {@link #canReplayWhileLiveIsPossible} is fulfilled. The timer will be
     * {@link Timer#pause() paused} if the state is {@link PlayStates#Playing} and the the play mode is
     * {@link PlayModes#Replay}.
     * 
     */
    @Override
    public void playStateChanged(PlayStates playState, PlayModes playMode) {
        boolean liveModeToBeMadePossible = isLiveModeToBeMadePossible();
        setLiveGenerallyPossible(liveModeToBeMadePossible);
        setJumpToLiveEnablement(liveModeToBeMadePossible && playMode != PlayModes.Live);
        switch (playState) {
        case Playing:
            playPauseButton.getElement().addClassName("playPauseButtonPause");
            playPauseButton.setVisible(true);
            if (playMode == PlayModes.Live) {
                playModeImage.setResource(playModeLiveActiveImg);
            } else {
                playModeImage.setResource(playModeReplayActiveImg);
            }
            break;
        case Paused:
            updatePlayPauseButtonsVisibility(playMode);
            playPauseButton.getElement().removeClassName("playPauseButtonPause");
            playModeImage.setResource(playModeInactiveImg);
            break;
        }
        
        switch (playMode) {
        case Live:
            playModeLabel.setText(stringMessages.playModeLive());
            timeDelayLabel.setText(stringMessages.timeDelay() + ": " + timer.getLivePlayDelayInMillis() / 1000 + " s");
            timeDelayLabel.setVisible(true);
            timeSlider.setEnabled(true);
            playSpeedBox.setEnabled(false);
            slowDownButton.setEnabled(false);
            speedUpButton.setEnabled(false);
            break;
        case Replay:
            timeDelayLabel.setVisible(false);
            timeDelayLabel.setText("");
            playModeLabel.setText(stringMessages.playModeReplay());
            timeSlider.setEnabled(true);
            playSpeedBox.setEnabled(true);
            slowDownButton.setEnabled(true);
            speedUpButton.setEnabled(true);
            if (!canReplayWhileLiveIsPossible() && playState == PlayStates.Playing && isLiveModeToBeMadePossible()) {
                // can't leave the timer playing when race is still live and canReplayWhileLiveIsPossible==false 
                timer.pause();
            }
            break;
        }
    }

    public void updatePlayPauseButtonsVisibility(PlayModes playMode) {
        playPauseButton.setVisible(playMode == PlayModes.Live || canReplayWhileLiveIsPossible() || !isLiveModeToBeMadePossible());
    }
    
    @Override
    public void playSpeedFactorChanged(double newPlaySpeedFactor) {
        // nothing to do
    }
    
    @Override
    public void onTimeZoomChanged(Date zoomStartTimepoint, Date zoomEndTimepoint) {
        resetZoomButton.setEnabled(true);
    }

    @Override
    public void onTimeZoomReset() {
        resetZoomButton.setEnabled(false);
    }

    @Override
    public void onTimeRangeChanged(Date fromTime, Date toTime) {
        setMinMax(fromTime, toTime, true);
    }

    protected boolean isLiveModeToBeMadePossible() {
        return false;
    }
    
    /**
     * Enables the {@link #backToLivePlayButton} if and only if <code>enabled</code> is <code>true</code>.
     */
    protected void setJumpToLiveEnablement(boolean enabled) {
        backToLivePlayButton.setEnabled(enabled);
        if (enabled) {
            backToLivePlayButton.setText(stringMessages.backToLiveTimePanelMode());
            backToLivePlayButton.removeStyleDependentName("Inactive");
            backToLivePlayButton.addStyleDependentName("Active");
        } else {
            backToLivePlayButton.setText(stringMessages.raceIsInLiveTimePanelMode());
            backToLivePlayButton.removeStyleDependentName("Active");
            backToLivePlayButton.addStyleDependentName("Inactive");
        }
    }
    
    /**
     * Iff <code>possible</code>, makes the {@link #backToLivePlayButton} button visible. Furthermore, if
     * <code>possible==false</code> and {@link #canReplayWhileLiveIsPossible}<code>==false</code> and the timer is in
     * state {@link PlayStates#Paused} and in mode {@link PlayModes#Replay}, the play/pause button will be shown.
     */
    protected void setLiveGenerallyPossible(boolean possible) {
        backToLivePlayButton.setVisible(possible);
        updateTimeSliderPadding(possible);
        updatePlayPauseButtonsVisibility(timer.getPlayMode());
    }

    private void updateTimeSliderPadding(boolean backToLivePlayButtonVisible) {
        Style timePanelStyle = timePanelSlider.getElement().getStyle();
        if (backToLivePlayButtonVisible || forcePaddingRightToAlignToCharts) {
            timePanelStyle.setPaddingRight(66, Unit.PX);
        } else {
            timePanelStyle.clearPaddingRight();
        }
        timeSlider.onResize();
    }

    @SuppressWarnings("unchecked")
    public T getSettings() {
        TimePanelSettings result = new TimePanelSettings(timer.getRefreshInterval());
        return (T) result;
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public SettingsDialogComponent<T> getSettingsDialogComponent(T settings) {
        return new TimePanelSettingsDialogComponent<T>(settings, stringMessages, userService, raceDTO);
    }

    @Override
    public void updateSettings(T newSettings) {
        timer.setRefreshInterval(newSettings.getRefreshInterval());
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.timePanelName();
    }
    
    @Override
    public void onResize() {
        // handle what is required by @link{ProvidesResize}
        Widget child = getWidget();
        if (child instanceof RequiresResize) {
            ((RequiresResize) child).onResize();
        }
        timeSlider.onResize();
    }

    public Date getLastReceivedDataTimepoint() {
        return lastReceivedDataTimepoint;
    }

    public void setLastReceivedDataTimepoint(Date lastReceivedDataTimepoint) {
        this.lastReceivedDataTimepoint = lastReceivedDataTimepoint;
    }

    protected boolean canReplayWhileLiveIsPossible() {
        return canReplayWhileLiveIsPossible;
    }
    
    public Button getAdvancedToggleButton() {
        return this.toggleAdvancedModeButton;
    }

    @Override
    public String getDependentCssClassName() {
        return "timePanel";
    }

    public Button getBackToLiveButton() {
        return backToLivePlayButton;
    }

    @Override
    public String getId() {
        return "TimePanel";
    }

    public void setBarOverlays(Iterable<BarOverlay> overlays) {
        timeSlider.setBarOverlays(overlays);
    }

}
