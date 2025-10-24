package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LongBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.common.client.premium.SailingPremiumCheckBox;
import com.sap.sailing.gwt.ui.client.DebugIdHelper;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.leaderboard.ManeuverCountRaceColumn;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PremiumCheckBox;

public abstract class LeaderboardSettingsDialogComponent<T extends LeaderboardSettings> implements SettingsDialogComponent<T> {
    public static final String CHECK_BOX_DEBUGID_CONSTANT = "CheckBox";
    protected final Map<DetailType, CheckBox> maneuverDetailCheckboxes;
    protected final Map<DetailType, CheckBox> legDetailCheckboxes;
    protected final Map<DetailType, CheckBox> raceDetailCheckboxes;
    protected final Map<DetailType, CheckBox> overallDetailCheckboxes;
    protected final StringMessages stringMessages;
    protected LongBox refreshIntervalInSecondsBox;
    protected final boolean canBoatInfoBeShownAsOverallDetail;
    
    protected RadioButton explicitRaceColumnSelectionRadioBtn;
    protected RadioButton lastNRacesColumnSelectionRadioBtn;
    protected IntegerBox numberOfLastRacesToShowBox;
    protected CheckBox showAddedScoresCheckBox;
    protected CheckBox showCompetitorShortNameColumnCheckBox;
    protected CheckBox showCompetitorFullNameColumnCheckBox;
    protected CheckBox showCompetitorBoatInfoColumnCheckBox;
    protected CheckBox isCompetitorNationalityColumnVisible;
    protected T initialSettings;
    protected Iterable<DetailType> availableDetailTypes;
    
    protected final PaywallResolver paywallResolver;
    protected final AbstractLeaderboardDTO leaderboardDTO;

    protected LeaderboardSettingsDialogComponent(T initialSettings, StringMessages stringMessages,
            Iterable<DetailType> availableDetailTypes, boolean canBoatInfoBeShownAsOverallDetail, PaywallResolver paywallResolver, AbstractLeaderboardDTO leaderboardDTO) {
        this.initialSettings = initialSettings;
        this.stringMessages = stringMessages;
        this.canBoatInfoBeShownAsOverallDetail = canBoatInfoBeShownAsOverallDetail;
        maneuverDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        legDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        raceDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        overallDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        this.availableDetailTypes = availableDetailTypes;
        this.paywallResolver = paywallResolver;
        this.leaderboardDTO = leaderboardDTO;
    }

    protected FlowPanel createManeuverDetailsPanel(DataEntryDialog<?> dialog) {
        FlowPanel meneuverPanel = new FlowPanel();
        meneuverPanel.ensureDebugId("ManeuverSettingsPanel");
        meneuverPanel.add(dialog.createHeadline(stringMessages.maneuverTypes(), true));
        meneuverPanel.addStyleName("SettingsDialogComponent");
        FlowPanel meneuverContent = new FlowPanel();
        meneuverContent.addStyleName("dialogInnerContent");
        Collection<DetailType> currentMeneuverDetailSelection = initialSettings.getManeuverDetailsToShow();
        for (DetailType detailType : Util.retainCopy(ManeuverCountRaceColumn.getAvailableManeuverDetailColumnTypes(), availableDetailTypes)) {
            final Widget checkbox;
            if (detailType.getPremiumAction() != null) {
                checkbox = createAndRegisterPremiumCheckbox(dialog, detailType, currentMeneuverDetailSelection.contains(detailType),
                        maneuverDetailCheckboxes);
            } else {
                checkbox = createAndRegisterCheckbox(dialog, detailType, currentMeneuverDetailSelection.contains(detailType),
                        maneuverDetailCheckboxes);
            }
            meneuverContent.add(checkbox);
        }
        meneuverPanel.add(meneuverContent);
        return meneuverPanel;
    }

    protected FlowPanel createTimingDetailsPanel(DataEntryDialog<?> dialog) {
        FlowPanel timingPanel = new FlowPanel();
        timingPanel.ensureDebugId("TimingSettingsPanel");
        refreshIntervalInSecondsBox = dialog.createLongBox(
                (initialSettings.getDelayBetweenAutoAdvancesInMilliseconds()==null ?
                        LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS : initialSettings.getDelayBetweenAutoAdvancesInMilliseconds()) / 1000l, 4);
        refreshIntervalInSecondsBox.ensureDebugId("RefreshIntervalLongBox");
        timingPanel.add(dialog.createHeadline(stringMessages.timing(), true));
        timingPanel.addStyleName("SettingsDialogComponent");
        FlowPanel timingContent = new FlowPanel();
        timingPanel.add(timingContent);
        timingContent.addStyleName("dialogInnerContent");
        Label refreshIntervalLabel = new Label(stringMessages.refreshInterval() + ":");
        refreshIntervalLabel.getElement().getStyle().setPaddingRight(5, Unit.PX);
        refreshIntervalLabel.getElement().getStyle().setPaddingLeft(5, Unit.PX);
        refreshIntervalLabel.getElement().getStyle().setFloat(Float.LEFT);
        timingContent.add(refreshIntervalLabel);
        timingContent.add(refreshIntervalInSecondsBox);
        return timingPanel;
    }

    protected FlowPanel createRaceDetailPanel(DataEntryDialog<?> dialog) {
        FlowPanel raceDetailDialog = new FlowPanel();
        raceDetailDialog.ensureDebugId("RaceDetailsSettingsPanel");
        raceDetailDialog.add(dialog.createHeadline(stringMessages.raceDetailsToShow(), true));
        raceDetailDialog.addStyleName("SettingsDialogComponent");
        int detailCountInCurrentFlowPanel = 0;
        Collection<DetailType> currentRaceDetailSelection = initialSettings.getRaceDetailsToShow();
        FlowPanel raceDetailDialogContent = null;
        for (DetailType type : Util.retainCopy(DetailType.getAllRaceDetailTypes(), availableDetailTypes)) {
            if (type == DetailType.RACE_DISPLAY_BOATS && canBoatInfoBeShownAsOverallDetail) {
                // we do not need race level boats, if the boats do not change
                continue;
            }
            if (detailCountInCurrentFlowPanel % 7 == 0) {
                raceDetailDialogContent = new FlowPanel();
                raceDetailDialogContent.addStyleName("dialogInnerContent");
                raceDetailDialog.add(raceDetailDialogContent);
            }
            final Widget checkbox;
            if (type.getPremiumAction() != null) {
                checkbox = createAndRegisterPremiumCheckbox(dialog, type, currentRaceDetailSelection.contains(type),
                        raceDetailCheckboxes);
            } else {
                checkbox = createAndRegisterCheckbox(dialog, type, currentRaceDetailSelection.contains(type),
                        raceDetailCheckboxes);
            }
            raceDetailDialogContent.add(checkbox);
            detailCountInCurrentFlowPanel++;
        }
        // Make it possible to configure added points
        FlowPanel addedScoresFlowPanel = new FlowPanel();
        addedScoresFlowPanel.addStyleName("dialogInnerContent");
        showAddedScoresCheckBox = dialog.createCheckbox(stringMessages.showAddedScores());
        dialog.addTooltip(showAddedScoresCheckBox, stringMessages.showAddedScores());
        showAddedScoresCheckBox.setValue(initialSettings.isShowAddedScores());
        addedScoresFlowPanel.add(showAddedScoresCheckBox);
        raceDetailDialog.add(addedScoresFlowPanel);
        return raceDetailDialog;
    }

    protected FlowPanel createRaceStartAnalysisPanel(DataEntryDialog<?> dialog) {
        FlowPanel raceStartAnalysisDialog = new FlowPanel();
        raceStartAnalysisDialog.ensureDebugId("RaceStartAnalysisDialog");
        raceStartAnalysisDialog.add(dialog.createHeadline(stringMessages.raceStartAnalysis(), true));
        raceStartAnalysisDialog.addStyleName("SettingsDialogComponent");
        int detailCountInCurrentFlowPanel = 0;
        Collection<DetailType> currentRaceDetailSelection = initialSettings.getRaceDetailsToShow();
        FlowPanel raceStartAnalysisDialogContent = null;
        for (DetailType type : Util.retainCopy(DetailType.getRaceStartAnalysisColumnTypes(), availableDetailTypes)) {
            if (detailCountInCurrentFlowPanel % 6 == 0) {
                raceStartAnalysisDialogContent = new FlowPanel();
                raceStartAnalysisDialogContent.addStyleName("dialogInnerContent");
                raceStartAnalysisDialog.add(raceStartAnalysisDialogContent);
            }
            final Widget checkbox;
            if (type.getPremiumAction() != null) {
                checkbox = createAndRegisterPremiumCheckbox(dialog, type, currentRaceDetailSelection.contains(type),
                        raceDetailCheckboxes);
            } else {
                checkbox = createAndRegisterCheckbox(dialog, type, currentRaceDetailSelection.contains(type),
                        raceDetailCheckboxes);
            }
            raceStartAnalysisDialogContent.add(checkbox);
            detailCountInCurrentFlowPanel++;
        }
        return raceStartAnalysisDialog;
    }

    protected FlowPanel createOverallDetailPanel(DataEntryDialog<?> dialog) {
        FlowPanel overallDetailDialog = new FlowPanel();
        overallDetailDialog.ensureDebugId("OverallDetailsSettingsPanel");
        overallDetailDialog.add(dialog.createHeadline(stringMessages.overallDetailsToShow(), true));
        overallDetailDialog.addStyleName("SettingsDialogComponent overallDetailSettings");
        FlowPanel overallDetailDialogContent = new FlowPanel();
        overallDetailDialogContent.addStyleName("dialogInnerContent");
        Collection<DetailType> currentOverallDetailSelection = initialSettings.getOverallDetailsToShow();
        for (DetailType type : Util.retainCopy(DetailType.getAvailableOverallDetailColumnTypes(), availableDetailTypes)) {
            final Widget checkbox;
            if (type.getPremiumAction() != null) {
                checkbox = createAndRegisterPremiumCheckbox(dialog, type, currentOverallDetailSelection.contains(type),
                        overallDetailCheckboxes);
            } else {
                checkbox = createAndRegisterCheckbox(dialog, type, currentOverallDetailSelection.contains(type),
                        overallDetailCheckboxes);
            }
            overallDetailDialogContent.add(checkbox);
        }
        FlowPanel overallDetailDialogContentSecondLine = new FlowPanel();
        overallDetailDialogContentSecondLine.addStyleName("dialogInnerContent");
        showCompetitorShortNameColumnCheckBox = dialog.createCheckbox(stringMessages.showCompetitorShortNameColumn());
        showCompetitorShortNameColumnCheckBox.setTitle(
                stringMessages.showCompetitorShortNameColumnTooltip(stringMessages.showCompetitorNationalityColumn()));
        showCompetitorShortNameColumnCheckBox.setValue(initialSettings.isShowCompetitorShortNameColumn());
        overallDetailDialogContentSecondLine.add(showCompetitorShortNameColumnCheckBox);
        showCompetitorFullNameColumnCheckBox = dialog.createCheckbox(stringMessages.showCompetitorFullNameColumn());
        showCompetitorFullNameColumnCheckBox.setValue(initialSettings.isShowCompetitorFullNameColumn());
        overallDetailDialogContentSecondLine.add(showCompetitorFullNameColumnCheckBox);
        showCompetitorBoatInfoColumnCheckBox = dialog.createCheckbox(stringMessages.showCompetitorBoatColumn());
        showCompetitorBoatInfoColumnCheckBox.setValue(initialSettings.isShowCompetitorBoatInfoColumn());
        overallDetailDialogContentSecondLine.add(showCompetitorBoatInfoColumnCheckBox);
        showCompetitorBoatInfoColumnCheckBox.setVisible(canBoatInfoBeShownAsOverallDetail);
        isCompetitorNationalityColumnVisible = dialog.createCheckbox(stringMessages.showCompetitorNationalityColumn());
        isCompetitorNationalityColumnVisible.setTitle(stringMessages.showCompetitorNationalityColumnTooltip());
        isCompetitorNationalityColumnVisible.setValue(initialSettings.isShowCompetitorNationality());
        overallDetailDialogContentSecondLine.add(isCompetitorNationalityColumnVisible);
        overallDetailDialog.add(overallDetailDialogContent);
        overallDetailDialog.add(overallDetailDialogContentSecondLine);
        return overallDetailDialog;
    }

    protected FlowPanel createLegDetailsPanel(DataEntryDialog<?> dialog) {
        FlowPanel legDetailsToShow = new FlowPanel();
        legDetailsToShow.ensureDebugId("LegDetailsSettingsPanel");
        legDetailsToShow.add(dialog.createHeadline(stringMessages.legDetailsToShow(), true));
        legDetailsToShow.addStyleName("SettingsDialogComponent");
        FlowPanel legDetailsContent = null;
        Collection<DetailType> currentLegDetailSelection = initialSettings.getLegDetailsToShow();
        int detailCountInCurrentFlowPanel = 0;
        for (DetailType type : Util.retainCopy(DetailType.getAllLegDetailColumnTypes(), availableDetailTypes)) {
            if (detailCountInCurrentFlowPanel % 7 == 0) {
                legDetailsContent = new FlowPanel();
                legDetailsContent.addStyleName("dialogInnerContent");
                legDetailsToShow.add(legDetailsContent);
            }
            final Widget checkbox;
            if (type.getPremiumAction() != null) {
                checkbox = createAndRegisterPremiumCheckbox(dialog, type, currentLegDetailSelection.contains(type),
                        legDetailCheckboxes);
            } else {
                checkbox = createAndRegisterCheckbox(dialog, type, currentLegDetailSelection.contains(type),
                        legDetailCheckboxes);
            }
            legDetailsContent.add(checkbox);
            detailCountInCurrentFlowPanel++;
        }
        return legDetailsToShow;
    }

    private CheckBox createAndRegisterCheckbox(DataEntryDialog<?> dialog, DetailType detailType, boolean selected,
            Map<DetailType, CheckBox> registry) {
        CheckBox checkbox = createCheckbox(dialog, detailType, selected);
        registry.put(detailType, checkbox);
        return checkbox;
    }

    private PremiumCheckBox createAndRegisterPremiumCheckbox(DataEntryDialog<?> dialog, DetailType detailType, boolean selected,
            Map<DetailType, CheckBox> registry) {
        PremiumCheckBox premiumCheckbox = createPremiumCheckbox(dialog, detailType, selected);
        registry.put(detailType, premiumCheckbox.getCheckBox());
        return premiumCheckbox;
    }

    private CheckBox createCheckbox(DataEntryDialog<?> dialog, DetailType detailType, boolean selected) {
        CheckBox checkbox = createCheckbox(dialog, DetailTypeFormatter.format(detailType), selected,
                DetailTypeFormatter.getTooltip(detailType));
        checkbox.ensureDebugId(DebugIdHelper.createDebugId(detailType) + CHECK_BOX_DEBUGID_CONSTANT);
        return checkbox;
    }

    private PremiumCheckBox createPremiumCheckbox(DataEntryDialog<?> dialog, DetailType detailType, boolean selected) {
        String tooltip = DetailTypeFormatter.getTooltip(detailType);
        String premiumTooltip = stringMessages.premiumFeature();
        if (tooltip != null && !tooltip.isEmpty()) {
            premiumTooltip += ": " + tooltip;
        } 
        PremiumCheckBox premiumCheckBox = createPremiumCheckbox(dialog, DetailTypeFormatter.format(detailType), selected,
                premiumTooltip , detailType.getPremiumAction());
        premiumCheckBox.ensureDebugId(DebugIdHelper.createDebugId(detailType) + CHECK_BOX_DEBUGID_CONSTANT);
        return premiumCheckBox;
    }
    
    protected CheckBox createCheckbox(DataEntryDialog<?> dialog, String label, boolean selected, String tooltip) {
        final CheckBox checkbox = dialog.createCheckbox(label);
        checkbox.ensureDebugId(DebugIdHelper.createDebugId(label) + CHECK_BOX_DEBUGID_CONSTANT);
        checkbox.setValue(selected);
        dialog.addTooltip(checkbox, tooltip);
        return checkbox;
    }

    protected PremiumCheckBox createPremiumCheckbox(DataEntryDialog<?> dialog, String label, boolean selected, String tooltip, Action premiumAction) {
        PremiumCheckBox premiumCheckBox = new SailingPremiumCheckBox(label, premiumAction, paywallResolver, leaderboardDTO, selected);
        dialog.registerCheckbox(premiumCheckBox.getCheckBox());
        premiumCheckBox.ensureDebugId(DebugIdHelper.createDebugId(label) + CHECK_BOX_DEBUGID_CONSTANT);
        dialog.addTooltip(premiumCheckBox, tooltip);
        return premiumCheckBox;
    }

    @Override
    public FocusWidget getFocusWidget() {
        final FocusWidget result;
        if (raceDetailCheckboxes.isEmpty()) {
            if (legDetailCheckboxes.isEmpty()) {
                if (maneuverDetailCheckboxes.isEmpty()) {
                    if (overallDetailCheckboxes.isEmpty()) {
                        result = refreshIntervalInSecondsBox;
                    } else {
                        result = overallDetailCheckboxes.values().iterator().next();
                    }
                } else {
                    result = maneuverDetailCheckboxes.values().iterator().next();
                }
            } else {
                result = legDetailCheckboxes.values().iterator().next();
            }
        } else {
            result = raceDetailCheckboxes.values().iterator().next();
        }
        return result;
    }
    
    protected List<DetailType> getSelected(Map<DetailType, CheckBox> checkBoxes, Collection<DetailType> initialValues) {
        List<DetailType> selectedDetails = new ArrayList<DetailType>(initialValues);
        for (Map.Entry<DetailType, CheckBox> entry : checkBoxes.entrySet()) {
            if (entry.getValue().getValue()) {
                selectedDetails.add(entry.getKey());
            } else {
                selectedDetails.remove(entry.getKey());
            }
        }
        return selectedDetails;
    }
}
