package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LongBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.gwt.common.client.premium.SailingPremiumCheckBox;
import com.sap.sailing.gwt.ui.client.ManeuverTypeFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings.HelpLineTypes;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.gwt.client.dialog.DoubleBox;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.security.ui.client.premium.PremiumCheckBox;

/**
 * The view responsible for manipulating an instance of RaceMapSettings, is handled by the RaceMapLifecycle
 */
public class RaceMapSettingsDialogComponent implements SettingsDialogComponent<RaceMapSettings> {
    private static final Distance MAX_BUOY_ZONE_RADIUS = new MeterDistance(100);
    private static final int MAX_STROKE_WEIGHT = 33;
    //Initializing the lists to prevent a null pointer exception in the first validation call
    private List<Util.Pair<CheckBox, ManeuverType>> checkboxAndManeuverType = new ArrayList<Util.Pair<CheckBox, ManeuverType>>();
    private List<Util.Pair<CheckBox, ZoomTypes>> checkboxAndZoomType = new ArrayList<Util.Pair<CheckBox,ZoomTypes>>();
    private List<Util.Pair<CheckBox, HelpLineTypes>> checkboxAndHelpLineType = new ArrayList<Util.Pair<CheckBox,HelpLineTypes>>();
    private CheckBox zoomOnlyToSelectedCompetitorsCheckBox;
    private CheckBox showDouglasPeuckerPointsCheckBox;
    private CheckBox showOnlySelectedCompetitorsCheckBox;
    private PremiumCheckBox showWindStreamletOverlayCheckbox;
    private PremiumCheckBox showWindStreamletColorsCheckbox;
    private CheckBox showSatelliteLayerCheckbox;
    private CheckBox windUpCheckbox;
    private PremiumCheckBox showSimulationOverlayCheckbox;
    private CheckBox showSelectedCompetitorsInfoCheckBox;
    private LongBox tailLengthBox;
    private DoubleBox buoyZoneRadiusInMetersBox;
    private CheckBox transparentHoverlines;
    private IntegerBox hoverlineStrokeWeight;
    private DoubleBox startCountDownFontSizeScalingBox;
    private CheckBox maneuverLossVisualizationCheckBox;
    private CheckBox windLadderCheckBox;
    private boolean hasPolar;

    private final StringMessages stringMessages;
    private final RaceMapSettings initialSettings;
    private ArrayList<CheckBox> disableOnlySelectedWhenAreFalse;
    private CheckBox showEstimatedDuration;
    
    public RaceMapSettingsDialogComponent(RaceMapSettings settings, StringMessages stringMessages,
            boolean hasPolar) {
        this.stringMessages = stringMessages;
        this.initialSettings = settings;
        this.hasPolar = hasPolar;
    }

    @Override
    public Widget getAdditionalWidget(DataEntryDialog<?> dialog) {
        VerticalPanel vp = new VerticalPanel();
        Label generalLabel = dialog.createHeadlineLabel(stringMessages.general());
        vp.add(generalLabel);
        showSatelliteLayerCheckbox = dialog.createCheckbox(stringMessages.showSatelliteLayer());
        showSatelliteLayerCheckbox.setValue(initialSettings.isShowSatelliteLayer());
        showSatelliteLayerCheckbox.getElement().setAttribute("selenium_checkbox", String.valueOf(initialSettings.isShowSatelliteLayer()));
        showSatelliteLayerCheckbox.ensureDebugId("showSatelliteLayerCheckBox");
        showSatelliteLayerCheckbox.setEnabled(!initialSettings.isWindUp());
        vp.add(showSatelliteLayerCheckbox);
        windUpCheckbox = dialog.createCheckbox(stringMessages.windUp());
        windUpCheckbox.setValue(initialSettings.isWindUp());
        windUpCheckbox.getElement().setAttribute("selenium_checkbox", String.valueOf(initialSettings.isWindUp()));
        windUpCheckbox.ensureDebugId("windUpCheckBox");
        vp.add(windUpCheckbox);
        showWindStreamletOverlayCheckbox = dialog.create(() -> new SailingPremiumCheckBox(
                stringMessages.showWindStreamletOverlay(), initialSettings.getShowWindStreamletOverlaySetting()));
        showWindStreamletOverlayCheckbox.ensureDebugId("showWindStreamletOverlayCheckBox");
        vp.add(showWindStreamletOverlayCheckbox);
        showWindStreamletColorsCheckbox = dialog.create(() -> new SailingPremiumCheckBox(
                stringMessages.showWindStreamletColors(), initialSettings.getShowWindStreamletColorsSetting()));
        showWindStreamletColorsCheckbox.setEnabled(initialSettings.isShowWindStreamletOverlay());
        showWindStreamletColorsCheckbox.addStyleName("RaceMapSettingsDialogCheckBoxIntended");
        vp.add(showWindStreamletColorsCheckbox);
        showWindStreamletOverlayCheckbox.addValueChangeHandler(
                event -> showWindStreamletColorsCheckbox.setEnabled(showWindStreamletOverlayCheckbox.getValue()));
        if (hasPolar) {
            showEstimatedDuration = dialog.createCheckbox(stringMessages.showEstimatedDuration());
            showEstimatedDuration.ensureDebugId("showEstimatedDurationCheckBox");
            showEstimatedDuration.setValue(initialSettings.isShowEstimatedDuration());
            vp.add(showEstimatedDuration);
            showSimulationOverlayCheckbox = dialog.create(() -> new SailingPremiumCheckBox(
                    stringMessages.showSimulationOverlay(), initialSettings.getShowSimulationOverlaySetting()));
            showSimulationOverlayCheckbox.ensureDebugId("showSimulationOverlayCheckBox");
            vp.add(showSimulationOverlayCheckbox);
        }
        Label competitorsLabel = dialog.createHeadlineLabel(stringMessages.competitors());
        vp.add(competitorsLabel);
        showOnlySelectedCompetitorsCheckBox = dialog.createCheckbox(stringMessages.showOnlySelectedCompetitors());
        showOnlySelectedCompetitorsCheckBox.ensureDebugId("showOnlySelectedCompetitorsCheckBox");
        showOnlySelectedCompetitorsCheckBox.setValue(initialSettings.isShowOnlySelectedCompetitors());
        vp.add(showOnlySelectedCompetitorsCheckBox);
        showSelectedCompetitorsInfoCheckBox = dialog.createCheckbox(stringMessages.showSelectedCompetitorsInfo());
        showSelectedCompetitorsInfoCheckBox.setValue(initialSettings.isShowSelectedCompetitorsInfo());
        vp.add(showSelectedCompetitorsInfoCheckBox);
        Label zoomLabel = dialog.createHeadlineLabel(stringMessages.zoom());
        vp.add(zoomLabel);
        HorizontalPanel zoomSettingsPanel = new HorizontalPanel();
        Label zoomSettingsLabel = new Label(stringMessages.autoZoomTo() + ": ");
        zoomSettingsPanel.add(zoomSettingsLabel);
        VerticalPanel zoomSettingsBoxesPanel = new VerticalPanel();
        disableOnlySelectedWhenAreFalse = new ArrayList<CheckBox>();
        for (ZoomTypes zoomType : ZoomTypes.values()) {
            if (zoomType != ZoomTypes.NONE) {
                CheckBox cb = dialog.createCheckbox(RaceMapSettingsTypeFormatter.formatZoomType(zoomType, stringMessages));
                cb.setValue(Util.contains(initialSettings.getZoomSettings().getTypesToConsiderOnZoom(), zoomType), false);
                checkboxAndZoomType.add(new Util.Pair<CheckBox, ZoomTypes>(cb, zoomType));
                zoomSettingsBoxesPanel.add(cb);
                // Save specific checkboxes for easier value change handling
                if (zoomType == ZoomTypes.BOATS || zoomType == ZoomTypes.TAILS) {
                    disableOnlySelectedWhenAreFalse.add(cb);
                    cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                        @Override
                        public void onValueChange(ValueChangeEvent<Boolean> event) {
                            zoomSettingsChanged();
                        }
                    });
                }
            }
        }
        zoomSettingsPanel.add(zoomSettingsBoxesPanel);
        vp.add(zoomSettingsPanel);
        zoomOnlyToSelectedCompetitorsCheckBox = dialog.createCheckbox(stringMessages.autoZoomSelectedCompetitors());
        zoomOnlyToSelectedCompetitorsCheckBox.setValue(initialSettings.getZoomSettings().isZoomToSelectedCompetitors());
        vp.add(zoomOnlyToSelectedCompetitorsCheckBox);
        //Run zoomSettingsChanged to set the checkboxes to their correct state
        zoomSettingsChanged();
        Label maneuversLabel = dialog.createHeadlineLabel(stringMessages.maneuverTypesToShowWhenCompetitorIsClicked());
        vp.add(maneuversLabel);
        int checkBoxCount = ManeuverType.values().length + 1; // including douglas peucker checkbox
        int gridRowsRequired = checkBoxCount / 2 + checkBoxCount % 2; 
        Grid maneuverGrid = new Grid(gridRowsRequired, 2);
        vp.add(maneuverGrid);
        int currentRowIndex = 0;
        int currentColumnIndex = 0;
        for (ManeuverType maneuverType : ManeuverType.values()) {
            CheckBox checkbox = dialog.createCheckbox(ManeuverTypeFormatter.format(maneuverType, stringMessages));
            checkbox.setValue(initialSettings.isShowManeuverType(maneuverType));
            checkboxAndManeuverType.add(new Util.Pair<CheckBox, ManeuverType>(checkbox, maneuverType));
            maneuverGrid.setWidget(currentRowIndex++, currentColumnIndex, checkbox);
            if (currentRowIndex >= gridRowsRequired) {
                currentColumnIndex = 1;
                currentRowIndex = 0; 
            }
        }
        showDouglasPeuckerPointsCheckBox = dialog.createCheckbox(stringMessages.douglasPeuckerPoints());
        showDouglasPeuckerPointsCheckBox.setValue(initialSettings.isShowDouglasPeuckerPoints());
        maneuverGrid.setWidget(currentRowIndex, currentColumnIndex, showDouglasPeuckerPointsCheckBox);
        Label helpLinesLabel = dialog.createHeadlineLabel(stringMessages.helpLines());
        vp.add(helpLinesLabel);
        // boat tail settings
        HorizontalPanel tailSettingsPanel = new HorizontalPanel();
        final CheckBox showTailsCheckBox = createHelpLineCheckBox(dialog, HelpLineTypes.BOATTAILS);
        tailSettingsPanel.add(showTailsCheckBox);
        showTailsCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> vce) {
                boolean newValue = vce.getValue();
                RaceMapSettingsDialogComponent.this.tailLengthBox.setEnabled(newValue);
            }
        });
        Label tailLengthLabel = new Label(stringMessages.lengthInSeconds() + ":");
        tailLengthLabel.getElement().getStyle().setMarginLeft(25, Unit.PX);
        tailSettingsPanel.add(tailLengthLabel);
        tailSettingsPanel.setCellVerticalAlignment(tailLengthLabel, HasVerticalAlignment.ALIGN_MIDDLE);
        tailLengthBox = dialog.createLongBox((int) (initialSettings.getTailLengthInMilliseconds() / 1000), 4);
        tailLengthBox.setEnabled(initialSettings.getHelpLinesSettings().isVisible(HelpLineTypes.BOATTAILS));
        tailSettingsPanel.add(tailLengthBox);
        tailSettingsPanel.setCellVerticalAlignment(tailLengthBox, HasVerticalAlignment.ALIGN_MIDDLE);
        vp.add(tailSettingsPanel);
        // buoy zone settings
        HorizontalPanel buoyZoneSettingsPanel = new HorizontalPanel();
        final CheckBox showBuoyZoneCheckBox = createHelpLineCheckBox(dialog, HelpLineTypes.BUOYZONE);
        buoyZoneSettingsPanel.add(showBuoyZoneCheckBox);
        showBuoyZoneCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> vce) {
                boolean newValue = vce.getValue();
                RaceMapSettingsDialogComponent.this.buoyZoneRadiusInMetersBox.setEnabled(newValue);
            }
        });
        Label buoyZoneRadiusLabel = new Label(stringMessages.radiusInMeters() + ":");
        buoyZoneRadiusLabel.getElement().getStyle().setMarginLeft(25, Unit.PX);
        buoyZoneSettingsPanel.add(buoyZoneRadiusLabel);
        buoyZoneSettingsPanel.setCellVerticalAlignment(buoyZoneRadiusLabel, HasVerticalAlignment.ALIGN_MIDDLE);
        buoyZoneRadiusInMetersBox = dialog.createDoubleBox(initialSettings.getBuoyZoneRadius().getMeters(), 4);
        buoyZoneRadiusInMetersBox.setEnabled(initialSettings.getHelpLinesSettings().isVisible(HelpLineTypes.BOATTAILS));
        buoyZoneSettingsPanel.add(buoyZoneRadiusInMetersBox);
        buoyZoneSettingsPanel.setCellVerticalAlignment(buoyZoneRadiusInMetersBox, HasVerticalAlignment.ALIGN_MIDDLE);
        vp.add(buoyZoneSettingsPanel);
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.STARTLINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.FINISHLINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.ADVANTAGELINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.COURSEMIDDLELINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.STARTLINETOFIRSTMARKTRIANGLE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.COURSEGEOMETRY));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.COURSEAREACIRCLES));
        maneuverLossVisualizationCheckBox = dialog.createCheckbox(stringMessages.maneuverLoss());
        maneuverLossVisualizationCheckBox.setValue(initialSettings.isShowManeuverLossVisualization());
        vp.add(maneuverLossVisualizationCheckBox);

        windLadderCheckBox = dialog.createCheckbox(stringMessages.showWindLadder());
        windLadderCheckBox.setValue(initialSettings.isShowWindLadder());
        vp.add(windLadderCheckBox);

        transparentHoverlines = dialog.createCheckbox(stringMessages.transparentBufferLineOnHover());
        transparentHoverlines.ensureDebugId("transparentHoverlinesCheckBox");
        transparentHoverlines.setValue(initialSettings.getTransparentHoverlines());
        vp.add(transparentHoverlines);
        HorizontalPanel hoverlineStrokeWeightPanel = new HorizontalPanel();
        Label hoverlineStrokeWeightLabel = new Label(stringMessages.bufferLineStrokeWeight() + ":");
        hoverlineStrokeWeightPanel.add(hoverlineStrokeWeightLabel);
        hoverlineStrokeWeight = dialog.createIntegerBox(initialSettings.getHoverlineStrokeWeight(), 3);
        hoverlineStrokeWeightPanel.add(hoverlineStrokeWeight);
        vp.add(hoverlineStrokeWeightPanel);
        HorizontalPanel startCountDownFontSizeScalingPanel = new HorizontalPanel();
        Label startCountDownFontSizeScalingLabel = new Label(stringMessages.startCountDownFontSizeScaling() + ":");
        startCountDownFontSizeScalingPanel.add(startCountDownFontSizeScalingLabel);
        startCountDownFontSizeScalingBox = dialog.createDoubleBox(initialSettings.getStartCountDownFontSizeScaling(), 3);
        startCountDownFontSizeScalingPanel.add(startCountDownFontSizeScalingBox);
        vp.add(startCountDownFontSizeScalingPanel);
        return vp;
    }
    
    private CheckBox createHelpLineCheckBox(DataEntryDialog<?> dialog, HelpLineTypes helpLineType) {
        CheckBox cb = dialog.createCheckbox(RaceMapSettingsTypeFormatter.formatHelpLineType(helpLineType, stringMessages));
        cb.setValue(initialSettings.getHelpLinesSettings().isVisible(helpLineType));
        checkboxAndHelpLineType.add(new Util.Pair<CheckBox, HelpLineTypes>(cb, helpLineType));
        return cb;
    }
    
    private void zoomSettingsChanged() {
        boolean disableOnlySelected = true;
        for (Util.Pair<CheckBox, ZoomTypes> pair : checkboxAndZoomType) {
            pair.getA().setEnabled(true);
            if (disableOnlySelectedWhenAreFalse.contains(pair.getA())) {
                if (pair.getA().getValue()) {
                    disableOnlySelected = false;
                }
            }
        }

        zoomOnlyToSelectedCompetitorsCheckBox.setEnabled(!disableOnlySelected);
        if (disableOnlySelected) {
            zoomOnlyToSelectedCompetitorsCheckBox.setValue(false);
        }
    }

    @Override
    public RaceMapSettings getResult() {
        Set<ManeuverType> maneuverTypesToShow = new HashSet<ManeuverType>();
        for (Util.Pair<CheckBox, ManeuverType> p : checkboxAndManeuverType) {
            if (p.getA().getValue() == true) {
                maneuverTypesToShow.add(p.getB());
            }
        }
        RaceMapHelpLinesSettings helpLinesSettings = getHelpLinesSettings();
        RaceMapZoomSettings zoomSettings = getZoomSettings();
        boolean estimatedDuration = showEstimatedDuration != null ? showEstimatedDuration.getValue() : false;
        boolean showSimulationOverlay = showSimulationOverlayCheckbox != null ? showSimulationOverlayCheckbox.getValue() : false;
        long tailLengthInMilliseconds = initialSettings.getTailLengthInMilliseconds(); 
        if (helpLinesSettings.isVisible(HelpLineTypes.BOATTAILS)) {
            tailLengthInMilliseconds = tailLengthBox.getValue() == null ? -1 : tailLengthBox.getValue() * 1000l;
        }
        Distance buoyZoneRadius = initialSettings.getBuoyZoneRadius();
        if (helpLinesSettings.isVisible(HelpLineTypes.BUOYZONE) && buoyZoneRadiusInMetersBox.getValue() != null) {
            buoyZoneRadius = new MeterDistance(buoyZoneRadiusInMetersBox.getValue());
        }
        return new RaceMapSettings(zoomSettings, helpLinesSettings,
                transparentHoverlines.getValue(), hoverlineStrokeWeight.getValue(), tailLengthInMilliseconds, windUpCheckbox.getValue(),
                buoyZoneRadius, showOnlySelectedCompetitorsCheckBox.getValue(), showSelectedCompetitorsInfoCheckBox.getValue(),
                showWindStreamletColorsCheckbox.getValue(), showWindStreamletOverlayCheckbox.getValue(), showSimulationOverlay,
                initialSettings.isShowMapControls(), maneuverTypesToShow, showDouglasPeuckerPointsCheckBox.getValue(),estimatedDuration,
                startCountDownFontSizeScalingBox.getValue(), maneuverLossVisualizationCheckBox.getValue(),
                showSatelliteLayerCheckbox.getValue(), windLadderCheckBox.getValue(), initialSettings.getPaywallResolver(), initialSettings.getSecuredDTO());
    }
    
    private RaceMapZoomSettings getZoomSettings() {
        ArrayList<ZoomTypes> zoomTypes = new ArrayList<ZoomTypes>();
        boolean noAutoZoomSelected = true;
        for (Util.Pair<CheckBox, ZoomTypes> pair : checkboxAndZoomType) {
            if (pair.getA().getValue()) {
                zoomTypes.add(pair.getB());
                noAutoZoomSelected = false;
            }
        }
        if (noAutoZoomSelected) {
            zoomTypes.add(ZoomTypes.NONE);
        }
        return new RaceMapZoomSettings(zoomTypes, zoomOnlyToSelectedCompetitorsCheckBox.getValue());
    }

    private RaceMapHelpLinesSettings getHelpLinesSettings() {
        Set<HelpLineTypes> helpLineTypes = new HashSet<HelpLineTypes>();
        for (Util.Pair<CheckBox, HelpLineTypes> pair : checkboxAndHelpLineType) {
            if (pair.getA().getValue()) {
                helpLineTypes.add(pair.getB());
            }
        }
        return new RaceMapHelpLinesSettings(helpLineTypes);
    }

    @Override
    public Validator<RaceMapSettings> getValidator() {
        return new Validator<RaceMapSettings>() {
            @Override
            public String getErrorMessage(RaceMapSettings valueToValidate) {
                String errorMessage = null;
                if (valueToValidate.getHelpLinesSettings().isVisible(HelpLineTypes.BOATTAILS) && valueToValidate.getTailLengthInMilliseconds() <= 0) {
                    errorMessage = stringMessages.tailLengthMustBePositive();
                } else if (valueToValidate.getHelpLinesSettings().isVisible(HelpLineTypes.BUOYZONE) 
                        && (valueToValidate.getBuoyZoneRadius().compareTo(Distance.NULL) < 0 || valueToValidate.getBuoyZoneRadius().compareTo(MAX_BUOY_ZONE_RADIUS) > 0)) {
                        errorMessage = stringMessages.valueMustBeBetweenMinMax(stringMessages.buoyZone(), 0, 100);
                } else if (valueToValidate.getHoverlineStrokeWeight() < 0 || valueToValidate.getHoverlineStrokeWeight() > MAX_STROKE_WEIGHT) {
                    errorMessage = stringMessages.valueMustBeBetweenMinMax(stringMessages.bufferLineStrokeWeight(), 0, MAX_STROKE_WEIGHT);
                }
                return errorMessage;
            }
        };
    }

    @Override
    public FocusWidget getFocusWidget() {
        return showWindStreamletOverlayCheckbox.getFocusWidget();
    }
}
