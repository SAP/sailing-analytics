package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LongBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.gwt.ui.client.ManeuverTypeFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings.HelpLineTypes;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;

public class RaceMapSettingsDialogComponent implements SettingsDialogComponent<RaceMapSettings> {
    private static final int MAX_BUOY_ZONE_RADIUS_IN_METERS = 100;
    private static final int MAX_STROKE_WEIGHT = 33;
    //Initializing the lists to prevent a null pointer exception in the first validation call
    private List<Util.Pair<CheckBox, ManeuverType>> checkboxAndManeuverType = new ArrayList<Util.Pair<CheckBox, ManeuverType>>();
    private List<Util.Pair<CheckBox, ZoomTypes>> checkboxAndZoomType = new ArrayList<Util.Pair<CheckBox,ZoomTypes>>();
    private List<Util.Pair<CheckBox, HelpLineTypes>> checkboxAndHelpLineType = new ArrayList<Util.Pair<CheckBox,HelpLineTypes>>();
    private CheckBox zoomOnlyToSelectedCompetitorsCheckBox;
    private CheckBox showDouglasPeuckerPointsCheckBox;
    private CheckBox showOnlySelectedCompetitorsCheckBox;
    private CheckBox showWindStreamletOverlayCheckbox;
    private CheckBox showWindStreamletColorsCheckbox;
    private CheckBox windUpCheckbox;
    private CheckBox showSimulationOverlayCheckbox;
    private CheckBox showSelectedCompetitorsInfoCheckBox;
    private LongBox tailLengthBox;
    private DoubleBox buoyZoneRadiusBox;
    private CheckBox transparentHoverlines;
    private IntegerBox hoverlineStrokeWeight;
    
    private boolean isSimulationEnabled;
    
    private final StringMessages stringMessages;
    private final RaceMapSettings initialSettings;

    private ArrayList<CheckBox> disableOnlySelectedWhenAreFalse;
    
    public RaceMapSettingsDialogComponent(RaceMapSettings settings, StringMessages stringMessages, boolean isSimulationEnabled) {
        this.isSimulationEnabled = isSimulationEnabled;
        this.stringMessages = stringMessages;
        initialSettings = settings;
    }

    @Override
    public Widget getAdditionalWidget(DataEntryDialog<?> dialog) {
        VerticalPanel vp = new VerticalPanel();

        Label generalLabel = dialog.createHeadlineLabel(stringMessages.general());
        vp.add(generalLabel);
       
        windUpCheckbox = dialog.createCheckbox(stringMessages.windUp());
        windUpCheckbox.setValue(initialSettings.isWindUp());
        vp.add(windUpCheckbox);

        showWindStreamletOverlayCheckbox = dialog.createCheckbox(stringMessages.showWindStreamletOverlay());
        showWindStreamletOverlayCheckbox.setValue(initialSettings.isShowWindStreamletOverlay());
        vp.add(showWindStreamletOverlayCheckbox);
        
        showWindStreamletColorsCheckbox = dialog.createCheckbox(stringMessages.showWindStreamletColors());
        showWindStreamletColorsCheckbox.setEnabled(initialSettings.isShowWindStreamletOverlay());
        showWindStreamletColorsCheckbox.setValue(initialSettings.isShowWindStreamletColors());
        showWindStreamletColorsCheckbox.addStyleName("RaceMapSettingsDialogCheckboxIntended");
        vp.add(showWindStreamletColorsCheckbox);

        showWindStreamletOverlayCheckbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                showWindStreamletColorsCheckbox.setEnabled(showWindStreamletOverlayCheckbox.getValue());
            }
        });
        
        if (isSimulationEnabled) {
            showSimulationOverlayCheckbox = dialog.createCheckbox(stringMessages.showSimulationOverlay());
            showSimulationOverlayCheckbox.setValue(initialSettings.isShowSimulationOverlay());
            vp.add(showSimulationOverlayCheckbox);
        }

        Label competitorsLabel = dialog.createHeadlineLabel(stringMessages.competitors());
        vp.add(competitorsLabel);

        showOnlySelectedCompetitorsCheckBox = dialog.createCheckbox(stringMessages.showOnlySelectedCompetitors());
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
                
                //Save specific checkboxes for easier value change handling
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
                RaceMapSettingsDialogComponent.this.buoyZoneRadiusBox.setEnabled(newValue);
            }
        });
        Label buoyZoneRadiusLabel = new Label(stringMessages.radiusInMeters() + ":");
        buoyZoneRadiusLabel.getElement().getStyle().setMarginLeft(25, Unit.PX);
        buoyZoneSettingsPanel.add(buoyZoneRadiusLabel);
        buoyZoneSettingsPanel.setCellVerticalAlignment(buoyZoneRadiusLabel, HasVerticalAlignment.ALIGN_MIDDLE);
        buoyZoneRadiusBox = dialog.createDoubleBox(Double.valueOf((int) (initialSettings.getBuoyZoneRadiusInMeters())), 4);
        buoyZoneRadiusBox.setEnabled(initialSettings.getHelpLinesSettings().isVisible(HelpLineTypes.BOATTAILS));
        buoyZoneSettingsPanel.add(buoyZoneRadiusBox);
        buoyZoneSettingsPanel.setCellVerticalAlignment(buoyZoneRadiusBox, HasVerticalAlignment.ALIGN_MIDDLE);
        vp.add(buoyZoneSettingsPanel);

        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.STARTLINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.FINISHLINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.ADVANTAGELINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.COURSEMIDDLELINE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.STARTLINETOFIRSTMARKTRIANGLE));
        vp.add(createHelpLineCheckBox(dialog, HelpLineTypes.COURSEGEOMETRY));
        
        transparentHoverlines = dialog.createCheckbox(stringMessages.transparentBufferLineOnHover());
        transparentHoverlines.setValue(initialSettings.getTransparentHoverlines());
        vp.add(transparentHoverlines);
        
        HorizontalPanel hoverlineStrokeWeightPanel = new HorizontalPanel();
        Label hoverlineStrokeWeightLabel = new Label(stringMessages.bufferLineStrokeWeight() + ":");
        hoverlineStrokeWeightPanel.add(hoverlineStrokeWeightLabel);
        hoverlineStrokeWeight = dialog.createIntegerBox(initialSettings.getHoverlineStrokeWeight(), 3);
        hoverlineStrokeWeightPanel.add(hoverlineStrokeWeight);
        vp.add(hoverlineStrokeWeightPanel);
        
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
        RaceMapSettings result = new RaceMapSettings();
        for (Util.Pair<CheckBox, ManeuverType> p : checkboxAndManeuverType) {
            result.showManeuverType(p.getB(), p.getA().getValue());
        }
        RaceMapHelpLinesSettings helpLinesSettings = getHelpLinesSettings();
        result.setZoomSettings(getZoomSettings());
        result.setHelpLinesSettings(helpLinesSettings);
        result.setShowDouglasPeuckerPoints(showDouglasPeuckerPointsCheckBox.getValue());
        result.setShowOnlySelectedCompetitors(showOnlySelectedCompetitorsCheckBox.getValue());
        result.setShowWindStreamletOverlay(showWindStreamletOverlayCheckbox.getValue());
        result.setShowWindStreamletColors(showWindStreamletColorsCheckbox.getValue());
        result.setShowMapControls(initialSettings.isShowMapControls());
        if (isSimulationEnabled) {
            result.setShowSimulationOverlay(showSimulationOverlayCheckbox.getValue());
        } else {
            result.setShowSimulationOverlay(false);            
        }
        result.setWindUp(windUpCheckbox.getValue());
        result.setShowSelectedCompetitorsInfo(showSelectedCompetitorsInfoCheckBox.getValue());
        if (helpLinesSettings.isVisible(HelpLineTypes.BOATTAILS)) {
            result.setTailLengthInMilliseconds(tailLengthBox.getValue() == null ? -1 : tailLengthBox.getValue() * 1000l);
        }
        if (helpLinesSettings.isVisible(HelpLineTypes.BUOYZONE)) {
            final Double value = buoyZoneRadiusBox.getValue();
            if (value != null) {
                result.setBuoyZoneRadiusInMeters(value);
            }
        }
        result.setTransparentHoverlines(transparentHoverlines.getValue());
        result.setHoverlineStrokeWeight(hoverlineStrokeWeight.getValue());
        return result;
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
                        && (valueToValidate.getBuoyZoneRadiusInMeters() < 0 || valueToValidate.getBuoyZoneRadiusInMeters() > MAX_BUOY_ZONE_RADIUS_IN_METERS)) {
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
        return showWindStreamletOverlayCheckbox;
    }
}
