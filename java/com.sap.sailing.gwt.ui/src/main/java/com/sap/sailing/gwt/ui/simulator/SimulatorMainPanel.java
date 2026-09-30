package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.ui.client.SimulatorServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.TimePanel;
import com.sap.sailing.gwt.ui.client.TimePanelSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.IdentityCoordinateSystem;
import com.sap.sailing.gwt.ui.shared.BoatClassDTOsAndNotificationMessage;
import com.sap.sailing.gwt.ui.shared.PolarDiagramDTOAndNotificationMessage;
import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.WindPatternDTO;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternDisplay;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternFormatter;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternSetting;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternSetting.SettingName;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.controls.slider.SliderBar;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomModel;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.security.ui.client.UserService;

public class SimulatorMainPanel extends SimplePanel {

    private static final String RADIOBOX_GROUP_MAP_DISPLAY_OPTIONS = "Map Display Options";

    private class ResizableFlowPanel extends FlowPanel implements RequiresResize {
        @Override
        public void onResize() {
            if (simulatorMap != null && simulatorMap.getMap() != null) {
                simulatorMap.getMap().triggerResize();
            }
        }
    }
	
    public boolean macroWeather;
    
    private DockLayoutPanel mainPanel;
    private FlowPanel leftPanel;
    private ResizableFlowPanel rightPanel;
    private VerticalPanel windPanel;

    private Button updateButton;
    private Button polarDiagramButton;
    private Button courseInputButton;

    private RadioButton summaryButton;
    private RadioButton replayButton;
    private RadioButton windDisplayButton;

    private Map<String, WindPatternDisplay> patternDisplayMap;
    private Map<String, Panel> patternPanelMap;

    private WindPatternDisplay currentWPDisplay;
    private Panel currentWPPanel;

    private ListBox patternSelector;
    private PatternSelectorHandler patternSelectorHandler;
    private Map<String, WindPatternDTO> patternNameDTOMap;

    private ListBox raceSelector;
    private ListBox legSelector;
    private ListBox competitorSelector;
    private ListBox boatClassSelector;
    private ListBox directionSelector;

    private WindFieldGenParamsDTO windParams;
    private Timer timer;
    private SimpleBusyIndicator busyIndicator;
    private static Logger logger = Logger.getLogger(SimulatorMainPanel.class.getName());

    private SimulatorMap simulatorMap;
    private StringMessages stringMessages;
    private SimulatorServiceAsync simulatorSvc;
    private ErrorReporter errorReporter;
    private boolean autoUpdate;
    private char mode;
    private char event;

    private TimePanel<TimePanelSettings> timePanel;
    private FlowPanel fullTimePanel;
    
    private CheckBox isOmniscient;
    private CheckBox isOpportunistic;
    private CheckBox isPathPolylineFreeMode;
    private Chart chart;

    private boolean warningAlreadyShown = false;
    private DialogBox polarDiagramDialogBox;
    private Button polarDiagramDialogCloseButton;
    private VerticalPanel polarDiv;
    private BoatClassDTO[] boatClasses = new BoatClassDTO[0];
    
    private final WindPatternFormatter formatter;

    private class WindControlCapture implements ValueChangeHandler<Double> {
        private SliderBar sliderBar;
        private WindPatternSetting<?> setting;

        public WindControlCapture(SliderBar sliderBar, WindPatternSetting<?> setting) {
            this.sliderBar = sliderBar;
            this.setting = setting;
        }

        @Override
        public void onValueChange(ValueChangeEvent<Double> arg0) {
            sliderBar.setTitle(SimulatorMainPanel.formatSliderValue(sliderBar.getCurrentValue()));
            logger.info("Slider value : " + arg0.getValue());
            setting.setValue(arg0.getValue());
            if (setting.getSettingName() == SettingName.BASE_BEARING_IN_DEGREES) {
            	simulatorMap.clearOverlays();
            	//System.out.println("Wind Base Bearing: "+setting.getValue());
            	simulatorMap.getRegattaAreaCanvasOverlay().updateRaceCourse(1, (Double) setting.getValue());
            	simulatorMap.getRaceCourseCanvasOverlay().draw();
                simulatorMap.getWindNeedleCanvasOverlay().draw();
            }
            if (setting.getSettingName() == SettingName.RACE_COURSE_DIFF_IN_DEGREES) {
            	simulatorMap.clearOverlays();
            	//System.out.println("Wind Base Bearing: "+setting.getValue());
            	simulatorMap.getRegattaAreaCanvasOverlay().updateRaceCourse(2, (Double) setting.getValue());
            	simulatorMap.getRaceCourseCanvasOverlay().draw();
                simulatorMap.getWindNeedleCanvasOverlay().draw();
            }
            if (autoUpdate) {
                update();
            }
        }
    }

    private class PatternSelectorHandler implements ChangeHandler {

        private class PatternRetriever implements AsyncCallback<WindPatternDisplay> {

            private String windPattern;

            public PatternRetriever(String windPattern) {
                this.windPattern = windPattern;
            }

            @Override
            public void onFailure(Throwable message) {
                errorReporter.reportError(stringMessages.errorReceivingWindPattern(message.getMessage()));
            }

            @Override
            public void onSuccess(WindPatternDisplay display) {
                logger.info(display.getSettings().toString());
                patternDisplayMap.put(windPattern, display);
                currentWPDisplay = display;
                Panel wPanel = getWindControlPanel();
                if (currentWPPanel != null) {
                    currentWPPanel.removeFromParent();
                }
                currentWPPanel = wPanel;
                patternPanelMap.put(windPattern, currentWPPanel);
                windPanel.add(currentWPPanel);
                currentWPPanel.setWidth("100%");
                currentWPPanel.getElement().setClassName("currentWPPanel");
            }
        }

        @Override
        public void onChange(ChangeEvent arg0) {
            String windPattern = patternSelector.getItemText(patternSelector.getSelectedIndex());
            logger.info(windPattern);
            if (patternDisplayMap.containsKey(windPattern)) {
                currentWPDisplay = patternDisplayMap.get(windPattern);
                if (currentWPPanel != null) {
                    currentWPPanel.removeFromParent();
                }
                currentWPPanel = patternPanelMap.get(windPattern);
                windPanel.add(currentWPPanel);

            } else {
                WindPatternDTO pattern = patternNameDTOMap.get(windPattern);
                simulatorSvc.getWindPatternDisplay(pattern, new PatternRetriever(windPattern));
            }
            simulatorMap.removeOverlays();
        }
    }

    public static String formatSliderValue(double value) {
        return NumberFormat.getFormat("0.0").format(value);
    }

    SimulatorMainPanel(SimulatorServiceAsync svc, StringMessages stringMessages, ErrorReporter errorReporter,
            int xRes, int yRes, int border, StreamletParameters streamletPars, boolean autoUpdate, char mode,
            char event, boolean showGrid, boolean showLines, char seedLines, boolean showArrows,
            boolean showLineGuides, boolean showStreamlets, boolean showMapControls, UserService userService) {
       super();
       this.formatter = new WindPatternFormatter(stringMessages);
        this.macroWeather = streamletPars.macroWeather;
        this.simulatorSvc = svc;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.autoUpdate = autoUpdate;
        this.mode = mode;
        this.event = event;
        this.isOmniscient = new CheckBox(this.stringMessages.omniscient(), true);
        this.isOmniscient.setValue(true);
        this.isOmniscient.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent evnet) {
            	boolean selectedValue = isOmniscient.getValue();
            	simulatorMap.getWindParams().showOmniscient = selectedValue;
            }
        });
        this.isOpportunistic = new CheckBox(this.stringMessages.opportunistic(), true);
        this.isOpportunistic.setValue(true);
        this.isOpportunistic.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent evnet) {
            	boolean selectedValue = isOpportunistic.getValue();
            	simulatorMap.getWindParams().showOpportunist = selectedValue;
            }
        });
        this.isPathPolylineFreeMode = new CheckBox(this.stringMessages.freemode(), true);
        this.isPathPolylineFreeMode.setValue(true);
        this.setSize("100%", "100%");
        leftPanel = new FlowPanel();
        rightPanel = new ResizableFlowPanel();
        patternSelector = new ListBox();
        patternSelectorHandler = new PatternSelectorHandler();
        patternSelector.addChangeHandler(patternSelectorHandler);
        patternSelector.setWidth("215px");
        patternNameDTOMap = new HashMap<String, WindPatternDTO>();
        patternDisplayMap = new HashMap<String, WindPatternDisplay>();
        patternPanelMap = new HashMap<String, Panel>();
        currentWPDisplay = null;
        currentWPPanel = null;
        this.raceSelector = new ListBox();
        this.raceSelector.setWidth("215px");
        this.legSelector = new ListBox();
        this.legSelector.setWidth("215px");
        this.competitorSelector = new ListBox();
        this.competitorSelector.setWidth("215px");
        this.boatClassSelector = new ListBox();
        this.boatClassSelector.setWidth("215px");
        directionSelector = new ListBox();
        directionSelector.setWidth("215px");
        windParams = new WindFieldGenParamsDTO();
        windParams.setMode(mode);
        windParams.setShowArrows(showArrows);
        windParams.setShowGrid(showGrid);
        windParams.setShowLines(showLines);
        windParams.setSeedLines(seedLines);
        windParams.setShowLineGuides(showLineGuides);
        windParams.setShowStreamlets(showStreamlets);
        this.setDefaultTimeSettings();
        timer = new Timer(PlayModes.Replay, 1000l);
        TimeRangeWithZoomProvider timeRangeProvider = new TimeRangeWithZoomModel();
        initTimer();
        timer.setTime(windParams.getStartTime().getTime());
        int secondsTimeStep = (int) windParams.getTimeStep().asSeconds();
        timer.setPlaySpeedFactor(secondsTimeStep);
        timePanel = new TimePanel<TimePanelSettings>(null, null, timer, timeRangeProvider, stringMessages, false,
                /*
                 * isScreenLargeEnoughToOfferChartSupport: no wind or competitor chart is shown; use full horizontal
                 * extension of time panel
                 */ false, userService, /*TODO: raceDTO is needed for permission check */null,
                /* pending server operations count supplier */ null, /* timeRangeActionsExecutor */ null);
        busyIndicator = new SimpleBusyIndicator(false, 0.8f);
        simulatorMap = new SimulatorMap(simulatorSvc, stringMessages, errorReporter, xRes, yRes, border, streamletPars,
                timer, timePanel, windParams, busyIndicator, mode, this, showMapControls,
                new IdentityCoordinateSystem());
        simulatorMap.setSize("100%", "100%");
        this.rightPanel.add(this.simulatorMap);
        createOptionsPanelTop();
        createOptionsPanel();
        fullTimePanel = this.createTimePanel();
        final Button toggleButton = timePanel.getAdvancedToggleButton();
        toggleButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                boolean advancedModeShown = timePanel.toggleAdvancedMode();
                if (advancedModeShown) {
                    mainPanel.setWidgetSize(fullTimePanel, 96);
                    toggleButton.removeStyleDependentName("Closed");
                    toggleButton.addStyleDependentName("Open");
                } else {
                    mainPanel.setWidgetSize(fullTimePanel, 67);
                    toggleButton.addStyleDependentName("Closed");
                    toggleButton.removeStyleDependentName("Open");
                }
            }
        });
        mainPanel = new DockLayoutPanel(Unit.PX);
        mainPanel.setSize("100%", "100%");        
        mainPanel.addWest(leftPanel, 470);
        mainPanel.addSouth(fullTimePanel, 67);
        mainPanel.setWidgetHidden(fullTimePanel, true);
        createMapOptionsPanel(); // add map-options to mainPanel-North
        mainPanel.add(rightPanel);
        this.setWidget(mainPanel);        
        this.polarDiagramDialogBox = this.createPolarDiagramDialogBox();
    }

    public native void setMapInstance(Object mapInstance) /*-{
        $wnd.swarmMap = mapInstance;
    }-*/;
    
    public native void setCanvasProjectionInstance(Object instance) /*-{
        $wnd.swarmCanvasProjection = instance;
    }-*/;

    public native void startStreamlets() /*-{
        if ($wnd.swarmAnimator) {
            $wnd.swarmUpdData = true;
            $wnd.updateStreamlets($wnd.swarmUpdData);
        } else {
            $wnd.initStreamlets($wnd.swarmMap);
        }
    }-*/;

    public void setDefaultTimeSettings() {
        Date defaultNow = new Date();
    	DateTimeFormat fmt = DateTimeFormat.getFormat("Z"); 
        NumberFormat df = NumberFormat.getDecimalFormat();
        int utcOffSet = (int) df.parse(fmt.format(defaultNow))/100; // default time zone offset versus UTC
        long epochTime = (defaultNow.getTime()/86400000)*86400000 - utcOffSet*3600000; // today, midnight in default time zone

        Date startTime = new Date(epochTime);
        Duration timeStep = new MillisecondsDurationImpl(15 * 1000);
        Date endTime = new Date(startTime.getTime() + 10 * 60 * 1000);
        windParams.setDefaultTimeSettings(startTime, timeStep, endTime);
    }

    public void showTimePanel(boolean visible) {
        mainPanel.setWidgetHidden(fullTimePanel, !visible);
        mainPanel.forceLayout(); // trigger onResize() on child panels, relevant for map resize & center
    }
    
    private void createOptionsPanelTop() {
        HorizontalPanel optionsPanel = new HorizontalPanel();
        optionsPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        optionsPanel.setTitle(stringMessages.optionsBar());
        optionsPanel.getElement().setClassName("optionsPanel");
        Label options = new Label(stringMessages.optionsBar());
        options.getElement().setClassName("sectorHeadline");
        optionsPanel.setSize("100%", "45px");
        optionsPanel.add(options);
        optionsPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        HorizontalPanel buttonPanel = new HorizontalPanel();
        initPolarDiagramButton();
        buttonPanel.add(polarDiagramButton);        
        initUpdateButton();
        buttonPanel.add(updateButton);
        optionsPanel.add(buttonPanel);        
        leftPanel.add(optionsPanel);
    }

    private void createOptionsPanel() {
        FlowPanel controlPanel = new FlowPanel();
        FlowPanel controlPanelInnerWrapper = new FlowPanel();
        controlPanelInnerWrapper.getElement().setClassName("controlPanelInnerWrapper");
        controlPanel.setTitle(stringMessages.controlSettings());
        controlPanel.getElement().setClassName("controlPanel");
        controlPanel.getElement().setId("masterPanelLeft");
        controlPanel.add(controlPanelInnerWrapper);
        createWindSetup(controlPanelInnerWrapper);
        this.createSailingSetup(controlPanelInnerWrapper);
        leftPanel.add(controlPanel);

    }

    private void createWindSetup(Panel controlPanel) {
        windPanel = new VerticalPanel();

        controlPanel.add(windPanel);
        windPanel.getElement().setClassName("windPanel");
        String windSetup = stringMessages.windSetup();
        Label windSetupLabel = new Label(windSetup);
        windSetupLabel.getElement().setClassName("innerHeadline");
        windPanel.add(windSetupLabel);

        HorizontalPanel hp = new HorizontalPanel();

        Label pattern = new Label(stringMessages.pattern());
        hp.add(pattern);

        simulatorSvc.getWindPatterns(mode, new AsyncCallback<List<WindPatternDTO>>() {
            @Override
            public void onFailure(Throwable message) {
                errorReporter.reportError(stringMessages.errorReceivingWindPattern(message.getMessage()));
            }

            @Override
            public void onSuccess(List<WindPatternDTO> patterns) {
                for (WindPatternDTO p : patterns) {
                    if ((mode != SailingSimulatorConstants.ModeFreestyle)||(!p.getName().equals("MEASURED"))) {
                        String displayName = formatter.formatPattern(p.getPattern());
                        patternSelector.addItem(displayName);
                        patternNameDTOMap.put(displayName, p);
                    }
                }
                if (mode == SailingSimulatorConstants.ModeMeasured) {
                    patternSelector.setItemSelected(patternSelector.getItemCount()-1, true);
                    patternSelectorHandler.onChange(null);
                } else {
                    patternSelector.setItemSelected(0, true);
                    patternSelectorHandler.onChange(null);
                }
            }
        });
        hp.add(patternSelector);
        windPanel.add(hp);
        hp.getElement().setClassName("choosePattern");
    }

    private Panel getWindControlPanel() {
        assert (currentWPDisplay != null);
        VerticalPanel windControlPanel = new VerticalPanel();
        windControlPanel.getElement().setClassName("windControLPanel");
        for (WindPatternSetting<?> s : currentWPDisplay.getSettings()) {
            switch (s.getDisplayWidgetType()) {
            case SLIDERBAR:
                @SuppressWarnings("unused")
                Panel sliderPanel = getSliderPanel(windControlPanel, s);
                break;
            case LISTBOX:
                logger.info("We have a listbox " + s);
                break;
            default:
                break;
            }
        }
        return windControlPanel;
    }

    public WindPatternDisplay getWindPatternDisplay() {
    	return currentWPDisplay;
    }
    
    private Panel getSliderPanel(Panel parentPanel, WindPatternSetting<?> s) {
        String labelName = formatter.formatSetting(s.getSettingName());
        double minValue = (Double) s.getMin();
        double maxValue = (Double) s.getMax();
        double defaultValue = (Double) s.getDefault();
        double stepSize = (Double) s.getResolution();
        FlowPanel vp = new FlowPanel();
        vp.getElement().setClassName("sliderWrapper");
        Label label = new Label(labelName);
        label.setWordWrap(true);
        vp.add(label);
        label.getElement().setClassName("sliderLabel");
        SliderBar sliderBar = new SliderBar(minValue, maxValue);
        sliderBar.getElement().getStyle().setProperty("width", "216px");
        sliderBar.setStepSize(stepSize, false);
        sliderBar.setNumTicks(s.getSteps());
        sliderBar.setNumTickLabels(1);
        sliderBar.setEnabled(true);
        WindControlCapture handler = new WindControlCapture(sliderBar, s);
        sliderBar.addValueChangeHandler(handler);
        sliderBar.setLabelFormatter(new SliderBar.LabelFormatter() {
            @Override
            public String formatLabel(SliderBar slider, Double value, Double previousValue) {
                return String.valueOf(Math.round(value));
                //return String.valueOf((value));
            }
        });
        sliderBar.setCurrentValue(defaultValue);
        //sliderBar.setTitle(String.valueOf(Math.round(sliderBar.getCurrentValue())));
        sliderBar.setTitle(SimulatorMainPanel.formatSliderValue(sliderBar.getCurrentValue()));
        vp.add(sliderBar);
        parentPanel.add(vp);
        label.getElement().getStyle().setFloat(Style.Float.LEFT);
        sliderBar.getElement().getStyle().setFloat(Style.Float.RIGHT);
        return vp;
    }

    private void createMapOptionsPanel() {
        HorizontalPanel mapOptions = new HorizontalPanel();
        mapOptions.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        mapOptions.setSize("100%", "45px");
        mapOptions.setTitle(stringMessages.maps());
        mapOptions.getElement().setClassName("mapOptions");
        Label mapsLabel = new Label(stringMessages.maps());
        mapsLabel.getElement().setClassName("sectorHeadline");
        mapOptions.add(mapsLabel);
        mapOptions.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        if ((mode != SailingSimulatorConstants.ModeMeasured) && (mode != SailingSimulatorConstants.ModeEvent)) {
            initCourseInputButton();
            mapOptions.add(courseInputButton);
        }
        mainPanel.addNorth(mapOptions, 45);
        initDisplayOptions(mapOptions);
        if (mode == SailingSimulatorConstants.ModeEvent) {
            summaryButton.setValue(true);
            replayButton.setValue(false);
            windDisplayButton.setValue(false);
        }
    }

    // initialize timer with a default time span based on windParams
    private void initTimer() {
        Date startDate = windParams.getStartTime();
        if (timePanel != null) {
            timePanel.setMinMax(startDate, windParams.getEndTime(), false);
        }
    }

    private void initCourseInputButton() {
        courseInputButton = new Button(stringMessages.startEnd());
        courseInputButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
                simulatorMap.reset();
                // TODO fix so that resetting the course displays the current
                // selection
                // For now disable all button & force user to select
                summaryButton.setValue(false);
                replayButton.setValue(false);
                windDisplayButton.setValue(false);
            }
        });
    }

    private Panel createRaceDirectionSelector() {
        Label raceDirectionLabel = new Label(stringMessages.raceDirection());
        raceDirectionLabel.getElement().setClassName("boatClassLabel");
        HorizontalPanel hp = new HorizontalPanel();
        hp.getElement().setClassName("boatClassPanel");
        hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        hp.add(raceDirectionLabel);
        if (directionSelector != null) {
            directionSelector.addItem(stringMessages.upWind());
            directionSelector.addItem(stringMessages.downWind());
            this.directionSelector.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent evnet) {
                    int selectedIndex = directionSelector.getSelectedIndex();
                    setRaceCourseDirection(selectedIndex);
                }
            });
            hp.add(directionSelector);
        }
        return hp;
    }

    private Panel createStrategySelector() {
        Label label = new Label(stringMessages.strategies());
        label.getElement().getStyle().setFloat(Style.Float.LEFT);
        FlowPanel fp = new FlowPanel();
        fp.add(label);
        VerticalPanel vp = new VerticalPanel();
        vp.getElement().getStyle().setProperty("width", "215px");
        vp.add(this.isOmniscient);
        vp.add(this.isOpportunistic);
        vp.getElement().getStyle().setFloat(Style.Float.RIGHT);
        fp.add(vp);
        return fp;
    }

    private Panel createPathPolylineModeSelector() {
        Label label = new Label(this.stringMessages.whatIfCourse());
        label.getElement().getStyle().setFloat(Style.Float.LEFT);
        FlowPanel fp = new FlowPanel();
        fp.add(label);
        VerticalPanel vp = new VerticalPanel();
        vp.getElement().getStyle().setProperty("width", "215px");
        vp.add(this.isPathPolylineFreeMode);
        vp.getElement().getStyle().setFloat(Style.Float.RIGHT);
        fp.add(vp);
        return fp;
    }

    public boolean isPathPolylineFreeMode() {
        return (!this.isPathPolylineFreeMode.getValue());
    }

    private FlowPanel createTimePanel() {
        FlowPanel timeLineInnerBgPanel = new FlowPanel();
        timeLineInnerBgPanel.addStyleName("timeLineInnerBgPanel");
        timeLineInnerBgPanel.add(timePanel);
        FlowPanel timeLineInnerPanel = new FlowPanel();
        timeLineInnerPanel.add(timeLineInnerBgPanel);
        timeLineInnerPanel.addStyleName("timeLineInnerPanel");
        FlowPanel fullTimePanel = new FlowPanel();
        fullTimePanel.add(timeLineInnerPanel);
        fullTimePanel.addStyleName("timeLinePanel");
        return fullTimePanel;
    }

    private void createSailingSetup(Panel controlPanel) {
        VerticalPanel sailingPanel = new VerticalPanel();
        controlPanel.add(sailingPanel);
        sailingPanel.getElement().setClassName("sailingPanel");
        String sailingSetup = stringMessages.sailingSetup();
        Label sailingSetupLabel = new Label(sailingSetup);
        sailingSetupLabel.getElement().setClassName("innerHeadline");
        sailingPanel.add(sailingSetupLabel);
        sailingPanel.add(this.getBoatClassesSelector());
        if (this.mode == SailingSimulatorConstants.ModeMeasured) {
            sailingPanel.add(this.getRacesSelector());
            sailingPanel.add(this.getCompetitorsSelector());
            sailingPanel.add(this.getLegsSelector());
        }
        Panel raceDirection = createRaceDirectionSelector();
        sailingPanel.add(raceDirection);
        Panel strategySelector = createStrategySelector();
        sailingPanel.add(strategySelector);
        if (this.mode == SailingSimulatorConstants.ModeMeasured) {
            Panel pathPolylineModeSelector = this.createPathPolylineModeSelector();
            sailingPanel.add(pathPolylineModeSelector);
        }
        this.polarDiagramDialogCloseButton = new Button(stringMessages.close());
        this.polarDiagramDialogCloseButton.getElement().setId("closeButton");
        this.polarDiagramDialogCloseButton.getElement().getStyle().setProperty("marginTop", "10px");
        this.polarDiv = new VerticalPanel();
        this.polarDiv.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        this.polarDiv.getElement().getStyle().setProperty("border", "0px");
        this.polarDiv.getElement().setClassName("polarDiv");
        this.polarDiv.setVisible(false);
        sailingPanel.add(this.polarDiv);
    }

    private void setRaceCourseDirection(final int selectedDirection) {
    	switch(selectedDirection) {
    	case 0:
    		simulatorMap.setRaceCourseDirection(SailingSimulatorConstants.LegTypeUpwind);
    		break;
    	case 1:
    		simulatorMap.setRaceCourseDirection(SailingSimulatorConstants.LegTypeDownwind);
    		break;
    	default:
    		simulatorMap.setRaceCourseDirection(SailingSimulatorConstants.LegTypeUpwind);
    	}
    }    
    
    private void loadPolarDiagramData(final int selectedBoatClass) {
        this.simulatorSvc.getBoatClasses(new AsyncCallback<BoatClassDTOsAndNotificationMessage>() {
            @Override
            public void onFailure(Throwable error) {
                errorReporter.reportError(stringMessages.errorLoadingBoatClasses(error.getMessage()));
            }
            @Override
            public void onSuccess(BoatClassDTOsAndNotificationMessage boatClassesAndMsg) {
                String notificationMessage = boatClassesAndMsg.getNotificationMessage();
                if (Util.hasLength(notificationMessage) && !warningAlreadyShown) {
                    errorReporter.reportError(boatClassesAndMsg.getNotificationMessage(), true);
                    warningAlreadyShown = true;
                }
                boatClasses = boatClassesAndMsg.getBoatClassDTOs();
                chart.setChartTitleText(boatClasses[selectedBoatClass].getName());
            }
        });
        if (this.chart != null) {
            this.polarDiv.remove(this.chart);
        }
        this.chart = new Chart()
        .setType(Series.Type.LINE)
        .setChartTitleText("Polar diagram test")
        .setWidth(450)
        .setHeight(500)
        .setOption("/chart/polar", true)
        .setOption("pane/startAngle", 0)
        .setOption("pane/endAngle", 360)
        .setOption("exporting/enableImages", true)
        .setOption("plotOptions/line/lineWidth", 1)
        .setOption("plotOptions/line/marker/enabled", false)
        .setMarginRight(2);
        this.simulatorSvc.getPolarDiagram(5.0, selectedBoatClass, new AsyncCallback<PolarDiagramDTOAndNotificationMessage>() {
            @Override
            public void onFailure(Throwable error) {
                errorReporter.reportError(stringMessages.errorLoadingPolarDataForBoatClass(boatClasses[selectedBoatClass].getName(), error.getMessage()));
            }
            
            @Override
            public void onSuccess(PolarDiagramDTOAndNotificationMessage polar) {
                if (polar == null) {
                    errorReporter.reportError(stringMessages.errorLoadingPolarDataForBoatClass(boatClasses[selectedBoatClass].getName(), ""));
                } else {
                    String notificationMessage = polar.getNotificationMessage();
                    if (Util.hasLength(notificationMessage) && warningAlreadyShown == false) {
                        errorReporter.reportError(polar.getNotificationMessage(), true);
                        warningAlreadyShown = true;
                    }
                    Number[][] Nseries = polar.getPolarDiagramDTO().getNumberSeries();
                    int[] windSpeedCatalog = new int[] { 6, 8, 10, 12, 14, 16, 20 };
                    PolarChartColorRange cc = new PolarChartColorRange(Nseries.length + 1);
                    ArrayList<String> windSpeedColor = cc.GetColors();
                    Series ser = chart.createSeries();
                    for (int i = 0; i < Nseries.length; i++) {
                        ser = chart.createSeries();
                        ser.setName("" + windSpeedCatalog[i] + " kn");
                        ser.setPoints(Nseries[i]);
                        ser.setOption("color", windSpeedColor.get(i));
                        chart.addSeries(ser);
                    }
                    chart.getXAxis().setTickInterval(10);
                    chart.getYAxis().setMin(0);
                    chart.setOption("plotOptions/series/pointInterval", 360.0 / (Nseries[0].length));
                    chart.getXAxis().setLabels(new XAxisLabels().setFormatter(new AxisLabelsFormatter() {
                        @Override
                        public String format(AxisLabelsData axisLabelsData) {
                            String labelD = "";
                            if (axisLabelsData.getValueAsLong() % 30 == 0) {
                                labelD = axisLabelsData.getValueAsLong() + "\u00B0";
                            }
                            return labelD;
                        }
                    }));
                    polarDiv.add(chart);
                    polarDiv.add(polarDiagramDialogCloseButton);
                }
            }
        });
    }

    private void initUpdateButton() {
        this.updateButton = new Button(stringMessages.simulateButton());
        this.updateButton.getElement().getStyle().setProperty("marginLeft", "6px");
        if (mode == SailingSimulatorConstants.ModeEvent) {
        	this.updateButton.setEnabled(true);
        }
        this.updateButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
                update();
            }
        });
    }

    public void setUpdateButtonEnabled(boolean enabled) {
    	this.updateButton.setEnabled(enabled);
    }
    
    private void update() {
    	if (this.windParams.isShowStreamlets()) {
    	    this.setMapInstance(this.simulatorMap.getMap().getJso());
    	    this.setCanvasProjectionInstance(this.simulatorMap.getRegattaAreaCanvasOverlay().getMapProjection());
    	}
        int selectedLegIndex = legSelector.getSelectedIndex();
        SimulatorUISelectionDTO selection = new SimulatorUISelectionDTO(getSelectedBoatClassIndex(),
                raceSelector.getSelectedIndex(), competitorSelector.getSelectedIndex(), legSelector.getSelectedIndex());
        if (windDisplayButton.getValue()) {
            showTimePanel(true);
            simulatorMap.refreshView(SimulatorMap.ViewName.WINDDISPLAY, currentWPDisplay, selection, true);
        } else if (summaryButton.getValue()) {
            showTimePanel(false);
            simulatorMap.getRegattaAreaCanvasOverlay().draw();
            simulatorMap.refreshView(SimulatorMap.ViewName.SUMMARY, currentWPDisplay, selection, true);
        } else if (replayButton.getValue()) {
            showTimePanel(true);
            simulatorMap.refreshView(SimulatorMap.ViewName.REPLAY, currentWPDisplay, selection, true);
        } else {
            if (mode == SailingSimulatorConstants.ModeMeasured) {
                if (selectedLegIndex % 2 != 0) {
                    errorReporter.reportError(stringMessages.downwindLegsNotSupported());
                } else {
                    simulatorMap.removePolyline();
                    showTimePanel(false);
                    simulatorMap.refreshView(SimulatorMap.ViewName.SUMMARY, currentWPDisplay, selection, true);
                }
            }
        }
    }

    private void initPolarDiagramButton() {
    	this.polarDiagramButton = new Button(stringMessages.polarDiagramButton());
    	this.polarDiagramButton.addClickHandler(new ClickHandler() {
    		@Override
    		public void onClick(ClickEvent arg0) {
    		    boolean checked = polarDiv.isVisible();
    		    if (!checked) {
    		        polarDiv.setVisible(true);
    		        //TODO: change the hardcoded values below...
    		        polarDiagramDialogBox.setPopupPositionAndShow(new PositionCallback() {
    		            @Override
    		            public void setPosition(int offsetWidth, int offsetHeight) {
    		                int width = (Window.getClientWidth() - 492)/2;
    		                int height = (Window.getClientHeight() - 608)/2;
    		                polarDiagramDialogBox.setPopupPosition(width, height);
    		            }
    		        });
    		        polarDiagramDialogCloseButton.setFocus(true);
    		    }
    		}
    	});
    }	

    private void initDisplayOptions(Panel mapOptions) {
    	this.summaryButton = new RadioButton(RADIOBOX_GROUP_MAP_DISPLAY_OPTIONS, stringMessages.summary());
        this.summaryButton.getElement().setClassName("MapDisplayOptions");
        this.summaryButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
                showTimePanel(false);
                SimulatorUISelectionDTO selection = new SimulatorUISelectionDTO(getSelectedBoatClassIndex(), raceSelector.getSelectedIndex(),
                        competitorSelector.getSelectedIndex(), legSelector.getSelectedIndex());
                simulatorMap.refreshView(SimulatorMap.ViewName.SUMMARY, currentWPDisplay, selection, false);
            }
        });
        this.replayButton = new RadioButton(RADIOBOX_GROUP_MAP_DISPLAY_OPTIONS, stringMessages.replay());
        this.replayButton.getElement().setClassName("MapDisplayOptions");
        this.replayButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
                showTimePanel(true);
                SimulatorUISelectionDTO selection = new SimulatorUISelectionDTO(getSelectedBoatClassIndex(), raceSelector.getSelectedIndex(),
                        competitorSelector.getSelectedIndex(), legSelector.getSelectedIndex());
                simulatorMap.refreshView(SimulatorMap.ViewName.REPLAY, currentWPDisplay, selection, false);
            }
        });
        this.windDisplayButton = new RadioButton(RADIOBOX_GROUP_MAP_DISPLAY_OPTIONS, stringMessages.windDisplay());
        this.windDisplayButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
                showTimePanel(true);
                SimulatorUISelectionDTO selection = new SimulatorUISelectionDTO(getSelectedBoatClassIndex(), raceSelector.getSelectedIndex(),
                        competitorSelector.getSelectedIndex(), legSelector.getSelectedIndex());
                simulatorMap.refreshView(SimulatorMap.ViewName.WINDDISPLAY, currentWPDisplay, selection, false);
            }
        });
        HorizontalPanel p = new HorizontalPanel();
        p.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        p.add(busyIndicator);
        p.add(windDisplayButton);
        p.add(summaryButton);
        p.add(replayButton);
        // windDisplayButton.setValue(true);
        mapOptions.add(p);
    }

    private DialogBox createPolarDiagramDialogBox() {
        final DialogBox dialogBox = new DialogBox();
        dialogBox.getElement().getStyle().setZIndex(10); // put polardiagram on-top of sapsailing header and parameter sliders
        dialogBox.setText("Polar Diagram");
        dialogBox.setAnimationEnabled(true);
        dialogBox.setAutoHideEnabled(false);
        dialogBox.setModal(false);
        dialogBox.setWidget(this.polarDiv);
        this.polarDiagramDialogCloseButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
            	polarDiv.setVisible(false);
                dialogBox.hide();
            }
        });
        return dialogBox;
    }

    public char getEvent() {
    	return this.event;
    }
    
    public int getSelectedBoatClassIndex() {
        return Integer.valueOf(boatClassSelector.getSelectedValue());
    }

    public int getSelectedRaceIndex() {
        return this.raceSelector.getSelectedIndex();
    }

    public int getSelectedLegIndex() {
        return this.legSelector.getSelectedIndex();
    }

    public int getSelectedCompetitorIndex() {
        return this.competitorSelector.getSelectedIndex();
    }

    private void loadRaceData(int selectedRaceIndex) {
        if (selectedRaceIndex < 0) {
            selectedRaceIndex = 0;
        }
        this.loadCompetitors(selectedRaceIndex);
        this.loadLegs(selectedRaceIndex);
    }

    private void loadLegData(int selectedLegIndex) {
    }

    private void loadCompetitorData(int selectedCompetitorIndex) {
    }

    private Panel getRacesSelector() {
        Label raceLabel = new Label(this.stringMessages.raceLabel());
        raceLabel.getElement().setClassName("boatClassLabel");
        HorizontalPanel panel = new HorizontalPanel();
        panel.getElement().setClassName("boatClassPanel");
        panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        panel.add(raceLabel);
        this.simulatorSvc.getRacesNames(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable error) {
                errorReporter.reportError(stringMessages.errorLoadingRaceNames(error.getMessage()));
            }

            @Override
            public void onSuccess(List<String> response) {
                for (String raceName : response) {
                    raceSelector.addItem(raceName);
                }
                raceSelector.setItemSelected(0, true); // first race
                loadRaceData(0);
            }
        });
        this.raceSelector.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent arg0) {
                loadRaceData(raceSelector.getSelectedIndex());
            }
        });
        panel.add(this.raceSelector);
        return panel;
    }

    private Panel getLegsSelector() {
        Label legLabel = new Label(this.stringMessages.legLabel());
        legLabel.getElement().setClassName("boatClassLabel");
        HorizontalPanel panel = new HorizontalPanel();
        panel.getElement().setClassName("boatClassPanel");
        panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        panel.add(legLabel);
        this.loadLegs(this.raceSelector.getSelectedIndex());
        this.legSelector.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent arg0) {
                loadLegData(legSelector.getSelectedIndex());
            }
        });
        panel.add(this.legSelector);
        return panel;
    }

    private Panel getCompetitorsSelector() {
        Label competitorLabel = new Label(this.stringMessages.competitorLabel());
        competitorLabel.getElement().setClassName("boatClassLabel");
        HorizontalPanel panel = new HorizontalPanel();
        panel.getElement().setClassName("boatClassPanel");
        panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        panel.add(competitorLabel);
        this.loadCompetitors(this.raceSelector.getSelectedIndex());
        this.competitorSelector.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent arg0) {
                loadCompetitorData(competitorSelector.getSelectedIndex());
            }
        });
        panel.add(this.competitorSelector);
        return panel;
    }

    private Panel getBoatClassesSelector() {
        Label boatClassLabel = new Label(this.stringMessages.boatClass());
        boatClassLabel.getElement().setClassName("boatClassLabel");
        HorizontalPanel panel = new HorizontalPanel();
        panel.getElement().setClassName("boatClassPanel");
        panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        panel.add(boatClassLabel);
        this.simulatorSvc.getBoatClasses(new AsyncCallback<BoatClassDTOsAndNotificationMessage>() {
            @Override
            public void onFailure(Throwable error) {
                errorReporter.reportError(stringMessages.errorLoadingBoatClasses(error.getMessage()));
            }

            @Override
            public void onSuccess(BoatClassDTOsAndNotificationMessage response) {
                String notificationMessage = response.getNotificationMessage();
                if (Util.hasLength(notificationMessage) && !warningAlreadyShown) {
                    errorReporter.reportError(response.getNotificationMessage(), true);
                    warningAlreadyShown = true;
                }
                boatClasses = response.getBoatClassDTOs();
                final Map<BoatClassDTO, Integer> boatClassIndices = new HashMap<>();
                for (int i = 0; i < boatClasses.length; ++i) {
                    final BoatClassDTO boatClass = boatClasses[i];
                    boatClassIndices.put(boatClass, i);
                    
                }
                int[] index = new int[1];
                boatClassIndices.keySet().stream().sorted().forEach(bc->{
                    final Integer boatClassIndex = boatClassIndices.get(bc);
                    boatClassSelector.addItem(bc.getName(), ""+boatClassIndex);
                    if (bc.getName().equals("49er STG")) {
                        boatClassSelector.setSelectedIndex(index[0]);
                        loadPolarDiagramData(boatClassIndex);
                    }
                    index[0]++;
                });
            }
        });
        this.boatClassSelector.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent evnet) {
                int selectedIndex = getSelectedBoatClassIndex();
                loadPolarDiagramData(selectedIndex);
            }
        });
        panel.add(boatClassSelector);
        return panel;
    }

    private void loadLegs(int selectedRaceIndex) {
        this.simulatorSvc.getLegsNames(selectedRaceIndex, new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable error) {
                errorReporter.reportError(stringMessages.errorLoadingLegInformation(error.getMessage()));
            }

            @Override
            public void onSuccess(List<String> response) {
                legSelector.clear();
                int index = 0;
                for (String legName : response) {
                    legSelector.addItem(legName + ((index % 2 == 0) ? " (" + stringMessages.upWind() + ")"
                            : " (" + stringMessages.downWind() + ")"));
                    index++;
                }
                legSelector.setItemSelected(0, true); // first leg
                loadLegData(0);
            }
        });
    }

    private void loadCompetitors(int selectedRaceIndex) {
        this.simulatorSvc.getCompetitorsNames(selectedRaceIndex, new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable error) {
                errorReporter.reportError(stringMessages.errorLoadingCompetitors(error.getMessage()));
            }

            @Override
            public void onSuccess(List<String> response) {
                competitorSelector.clear();
                for (String competitorName : response) {
                    competitorSelector.addItem(competitorName);
                }
                competitorSelector.setItemSelected(0, true); // first competitor
                loadCompetitorData(0);
            }
        });
    }
}
