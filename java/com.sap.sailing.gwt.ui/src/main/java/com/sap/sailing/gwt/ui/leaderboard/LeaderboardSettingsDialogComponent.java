package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LongBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.gwt.ui.client.DataEntryDialog;
import com.sap.sailing.gwt.ui.client.DataEntryDialog.Validator;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSettings.RaceColumnSelectionStrategies;
import com.sap.sailing.gwt.ui.shared.RaceColumnDTO;
import com.sap.sailing.gwt.ui.shared.components.SettingsDialogComponent;

public class LeaderboardSettingsDialogComponent implements SettingsDialogComponent<LeaderboardSettings> {
    private final Iterable<RaceColumnDTO> raceColumnSelection;
    private final Map<RaceColumnDTO, CheckBox> raceColumnCheckboxes;
    private final List<RaceColumnDTO> raceAllRaceColumns;
    private final List<DetailType> maneuverDetailSelection;
    private final Map<DetailType, CheckBox> maneuverDetailCheckboxes;
    private final List<DetailType> legDetailSelection;
    private final Map<DetailType, CheckBox> legDetailCheckboxes;
    private final List<DetailType> raceDetailSelection;
    private final Map<DetailType, CheckBox> raceDetailCheckboxes;
    private final List<DetailType> overallDetailSelection;
    private final Map<DetailType, CheckBox> overallDetailCheckboxes;
    private final StringMessages stringMessages;
    private LongBox refreshIntervalInSecondsBox;
    private LongBox delayInSecondsBox;
    private final boolean autoExpandPreSelectedRace;
    private final long delayBetweenAutoAdvancesInMilliseconds;
    private final long delayInMilliseconds;
    private final Integer numberOfLastRacesToShow;
    private RaceColumnSelectionStrategies activeRaceColumnSelectionStrategy;
    private RadioButton explicitRaceColumnSelectionRadioBtn;
    private RadioButton lastNRacesColumnSelectionRadioBtn;
    private IntegerBox numberOfLastRacesToShowBox;

    public LeaderboardSettingsDialogComponent(List<DetailType> maneuverDetailSelection,
            List<DetailType> legDetailSelection, List<DetailType> raceDetailSelection, List<DetailType> overallDetailSelection,
            List<RaceColumnDTO> raceAllRaceColumns, Iterable<RaceColumnDTO> raceColumnSelection, RaceColumnSelection raceColumnSelectionStrategy,
            boolean autoExpandPreSelectedRace, long delayBetweenAutoAdvancesInMilliseconds, long delayInMilliseconds, StringMessages stringMessages) {
        this.raceAllRaceColumns = raceAllRaceColumns;
        this.numberOfLastRacesToShow = raceColumnSelectionStrategy.getNumberOfLastRaceColumnsToShow();
        this.activeRaceColumnSelectionStrategy = raceColumnSelectionStrategy.getType();
        this.maneuverDetailSelection = maneuverDetailSelection;
        maneuverDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        this.raceColumnSelection = raceColumnSelection;
        raceColumnCheckboxes = new LinkedHashMap<RaceColumnDTO, CheckBox>();
        this.legDetailSelection = legDetailSelection;
        legDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        this.raceDetailSelection = raceDetailSelection;
        raceDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        this.overallDetailSelection = overallDetailSelection;
        overallDetailCheckboxes = new LinkedHashMap<DetailType, CheckBox>();
        this.stringMessages = stringMessages;
        this.autoExpandPreSelectedRace = autoExpandPreSelectedRace;
        this.delayBetweenAutoAdvancesInMilliseconds = delayBetweenAutoAdvancesInMilliseconds;
        this.delayInMilliseconds = delayInMilliseconds;
    }

    @Override
    public Widget getAdditionalWidget(DataEntryDialog<?> dialog) {
        FlowPanel dialogPanel = new FlowPanel();
        dialogPanel.add(createSelectedRacesPanel(dialog));
        dialogPanel.add(createOverallDetailPanel(dialog));
        dialogPanel.add(createRaceDetailPanel(dialog));
        dialogPanel.add(createLegDetailsPanel(dialog));
        dialogPanel.add(createManeuverDetailsPanel(dialog));
        dialogPanel.add(createTimingDetailsPanel(dialog));
        return dialogPanel;
    }

    private FlowPanel createManeuverDetailsPanel(DataEntryDialog<?> dialog) {
        FlowPanel meneuverPanel = new FlowPanel();
        meneuverPanel.add(dialog.createHeadline(stringMessages.maneuverTypes(), true));
        meneuverPanel.addStyleName("SettingsDialogComponent");
        FlowPanel meneuverContent = new FlowPanel();
        meneuverContent.addStyleName("dialogInnerContent");
        List<DetailType> currentMeneuverDetailSelection = maneuverDetailSelection;
        for (DetailType detailType : ManeuverCountRaceColumn.getAvailableManeuverDetailColumnTypes()) {
            CheckBox checkbox = dialog.createCheckbox(DetailTypeFormatter.format(detailType, stringMessages));
            checkbox.setValue(currentMeneuverDetailSelection.contains(detailType));
            maneuverDetailCheckboxes.put(detailType, checkbox);
            meneuverContent.add(checkbox);
        }
        meneuverPanel.add(meneuverContent);
        return meneuverPanel;
    }

    private FlowPanel createTimingDetailsPanel(DataEntryDialog<?> dialog) {
        FlowPanel timingPanel = new FlowPanel();
        refreshIntervalInSecondsBox = dialog.createLongBox(delayBetweenAutoAdvancesInMilliseconds / 1000l, 4);
        delayInSecondsBox = dialog.createLongBox(delayInMilliseconds / 1000l, 4);
        
        timingPanel.add(dialog.createHeadline(stringMessages.timing(), true));
        timingPanel.addStyleName("SettingsDialogComponent");

        FlowPanel timingContent = new FlowPanel();
        timingPanel.add(timingContent);
        
        timingContent.addStyleName("dialogInnerContent");
        Label delayLabel = new Label(stringMessages.delayInSeconds() + ":");
        delayLabel.getElement().getStyle().setPaddingRight(5, Unit.PX);
        
        timingContent.add(delayLabel);
        timingContent.add(delayInSecondsBox);

        Label refreshIntervalLabel = new Label(stringMessages.refreshInterval() + ":");
        refreshIntervalLabel.getElement().getStyle().setPaddingRight(5, Unit.PX);
        refreshIntervalLabel.getElement().getStyle().setPaddingLeft(25, Unit.PX);
        timingContent.add(refreshIntervalLabel);
        timingContent.add(refreshIntervalInSecondsBox);

        for(int i = 0; i < timingContent.getWidgetCount(); i++) {
            timingContent.getWidget(i).getElement().getStyle().setFloat(Float.LEFT);
        }

        return timingPanel;
    }

    private FlowPanel createRaceDetailPanel(DataEntryDialog<?> dialog) {
        FlowPanel raceDetailDialog = new FlowPanel();
        raceDetailDialog.add(dialog.createHeadline(stringMessages.raceDetailsToShow(), true));
        raceDetailDialog.addStyleName("SettingsDialogComponent");
        FlowPanel raceDetailDialogContent = new FlowPanel();
        raceDetailDialogContent.addStyleName("dialogInnerContent");
        List<DetailType> currentRaceDetailSelection = raceDetailSelection;
        for (DetailType type : LeaderboardPanel.getAvailableRaceDetailColumnTypes()) {
            CheckBox checkbox = dialog.createCheckbox(DetailTypeFormatter.format(type, stringMessages));
            checkbox.setValue(currentRaceDetailSelection.contains(type));
            raceDetailCheckboxes.put(type, checkbox);
            raceDetailDialogContent.add(checkbox);
        }
        raceDetailDialog.add(raceDetailDialogContent);
        return raceDetailDialog;
    }

    private FlowPanel createOverallDetailPanel(DataEntryDialog<?> dialog) {
        FlowPanel overallDetailDialog = new FlowPanel();
        overallDetailDialog.add(dialog.createHeadline(stringMessages.overallDetailsToShow(), true));
        overallDetailDialog.addStyleName("SettingsDialogComponent overallDetailSettings");
        FlowPanel overallDetailDialogContent = new FlowPanel();
        overallDetailDialogContent.addStyleName("dialogInnerContent");
        List<DetailType> currentOverallDetailSelection = overallDetailSelection;
        for (DetailType type : LeaderboardPanel.getAvailableOverallDetailColumnTypes()) {
            CheckBox checkbox = dialog.createCheckbox(DetailTypeFormatter.format(type, stringMessages));
            checkbox.setValue(currentOverallDetailSelection.contains(type));
            overallDetailCheckboxes.put(type, checkbox);
            overallDetailDialogContent.add(checkbox);
        }
        overallDetailDialog.add(overallDetailDialogContent);
        return overallDetailDialog;
    }

    private FlowPanel createLegDetailsPanel(DataEntryDialog<?> dialog) {
        FlowPanel legDetailsToShow = new FlowPanel();
        legDetailsToShow.add(dialog.createHeadline(stringMessages.legDetailsToShow(), true));
        legDetailsToShow.addStyleName("SettingsDialogComponent");
        FlowPanel legDetailsContent = new FlowPanel();
        legDetailsContent.addStyleName("dialogInnerContent");
        List<DetailType> currentLegDetailSelection = legDetailSelection;
        for (DetailType type : LegColumn.getAvailableLegDetailColumnTypes()) {
            CheckBox checkbox = dialog.createCheckbox(DetailTypeFormatter.format(type, stringMessages));
            checkbox.setValue(currentLegDetailSelection.contains(type));
            legDetailCheckboxes.put(type, checkbox);
            legDetailsContent.add(checkbox);
        }
        legDetailsToShow.add(legDetailsContent);
        return legDetailsToShow;
    }

    private FlowPanel createSelectedRacesPanel(DataEntryDialog<?> dialog) {
        FlowPanel selectedRacesPanel = new FlowPanel();
        selectedRacesPanel.addStyleName("SettingsDialogComponent");
        selectedRacesPanel.add(dialog.createHeadline(stringMessages.selectedRaces(), true));
        // race selection strategy elements
        HorizontalPanel racesSelectionStrategyPanel = new HorizontalPanel();
        selectedRacesPanel.add(racesSelectionStrategyPanel);

        FlowPanel selectedRacesContent = new FlowPanel();
        selectedRacesContent.addStyleName("dialogInnerContent");
        selectedRacesPanel.add(selectedRacesContent);
        
        // Attention: We need to consider that there are regattas with more than 30 races
        int racesCount = raceAllRaceColumns.size();
        if (racesCount > 0) {
            final FlowPanel explicitRaceSelectionContent = new FlowPanel();
            final FlowPanel lastNRacesSelectionContent = new FlowPanel();
            String radioButtonGroupName = "raceSelectionStrategyGroup";
            Label raceSelectionWayLabel = new Label(stringMessages.chooseTheWayYouSelectRaces() + ":");
            raceSelectionWayLabel.getElement().getStyle().setPaddingRight(5, Unit.PX);
            racesSelectionStrategyPanel.add(raceSelectionWayLabel);
            explicitRaceColumnSelectionRadioBtn = dialog.createRadioButton(radioButtonGroupName, stringMessages.selectFromAllRaces());
            racesSelectionStrategyPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
            explicitRaceColumnSelectionRadioBtn.setValue(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.EXPLICIT);
            explicitRaceColumnSelectionRadioBtn.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    explicitRaceSelectionContent.setVisible(true);
                    lastNRacesSelectionContent.setVisible(false);
                    activeRaceColumnSelectionStrategy = RaceColumnSelectionStrategies.EXPLICIT;
                }
            });
            racesSelectionStrategyPanel.add(explicitRaceColumnSelectionRadioBtn);

            // content of explicit race selection
            int maxRacesPerRow = 10;
            int rowIndex = 0;
            int columnIndex = 0;
            int rowCount = racesCount / maxRacesPerRow;
            if(racesCount % maxRacesPerRow != 0) {
                rowCount++;
            }
            Grid grid = new Grid(rowCount, maxRacesPerRow);
            for (RaceColumnDTO expandableSortableColumn : raceAllRaceColumns) {
                CheckBox checkbox = dialog.createCheckbox(expandableSortableColumn.getRaceColumnName());
                checkbox.setValue(Util.contains(raceColumnSelection, expandableSortableColumn));
                raceColumnCheckboxes.put(expandableSortableColumn, checkbox);
                grid.setWidget(rowIndex, columnIndex++, checkbox);
                if(columnIndex == maxRacesPerRow) {
                    rowIndex++;
                    columnIndex = 0;
                }
            }
            explicitRaceSelectionContent.add(grid);
            explicitRaceSelectionContent.setVisible(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.EXPLICIT);
            selectedRacesContent.add(explicitRaceSelectionContent);
            
            lastNRacesColumnSelectionRadioBtn = dialog.createRadioButton(radioButtonGroupName, stringMessages.selectANumberOfRaces());
            lastNRacesColumnSelectionRadioBtn.setValue(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.LAST_N);
            lastNRacesColumnSelectionRadioBtn.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    explicitRaceSelectionContent.setVisible(false);
                    lastNRacesSelectionContent.setVisible(true);
                    activeRaceColumnSelectionStrategy = RaceColumnSelectionStrategies.LAST_N;
                }
            });
            racesSelectionStrategyPanel.add(lastNRacesColumnSelectionRadioBtn);
            dialog.alignAllPanelWidgetsVertically(racesSelectionStrategyPanel, HasVerticalAlignment.ALIGN_MIDDLE);
            
            // content of 'number of races' selection
            HorizontalPanel hPanel = new HorizontalPanel();
            lastNRacesSelectionContent.add(hPanel);
            Label numberOfLastRacesLabel = new Label(stringMessages.numberOfLastNRaces() + ":");
            numberOfLastRacesLabel.getElement().getStyle().setPaddingRight(10, Unit.PX);
            hPanel.add(numberOfLastRacesLabel);
            numberOfLastRacesToShowBox = dialog.createIntegerBox(numberOfLastRacesToShow != null ? numberOfLastRacesToShow : racesCount, 3);
            hPanel.add(numberOfLastRacesToShowBox);
            dialog.alignAllPanelWidgetsVertically(hPanel, HasVerticalAlignment.ALIGN_MIDDLE);
            lastNRacesSelectionContent.setVisible(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.LAST_N);
            selectedRacesContent.add(lastNRacesSelectionContent);
        } else {
            selectedRacesContent.add(new Label(stringMessages.noRacesYet()));
        }
        return selectedRacesPanel;
    }

    @Override
    public LeaderboardSettings getResult() {
        List<DetailType> maneuverDetailsToShow = new ArrayList<DetailType>();
        for (Map.Entry<DetailType, CheckBox> entry : maneuverDetailCheckboxes.entrySet()) {
            if (entry.getValue().getValue()) {
                maneuverDetailsToShow.add(entry.getKey());
            }
        }
        List<DetailType> overallDetailsToShow = new ArrayList<DetailType>();
        for (Map.Entry<DetailType, CheckBox> entry : overallDetailCheckboxes.entrySet()) {
            if (entry.getValue().getValue()) {
                overallDetailsToShow.add(entry.getKey());
            }
        }
        List<DetailType> raceDetailsToShow = new ArrayList<DetailType>();
        for (Map.Entry<DetailType, CheckBox> entry : raceDetailCheckboxes.entrySet()) {
            if (entry.getValue().getValue()) {
                raceDetailsToShow.add(entry.getKey());
            }
        }
        List<DetailType> legDetailsToShow = new ArrayList<DetailType>();
        for (Map.Entry<DetailType, CheckBox> entry : legDetailCheckboxes.entrySet()) {
            if (entry.getValue().getValue()) {
                legDetailsToShow.add(entry.getKey());
            }
        }
        List<String> namesOfRaceColumnsToShow = null;
        if (activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.EXPLICIT) {
            namesOfRaceColumnsToShow = new ArrayList<String>();
            for (Map.Entry<RaceColumnDTO, CheckBox> entry : raceColumnCheckboxes.entrySet()) {
                if (entry.getValue().getValue()) {
                    namesOfRaceColumnsToShow.add(entry.getKey().getRaceColumnName());
                }
            }
        }
        Long delayBetweenAutoAdvancesValue = refreshIntervalInSecondsBox.getValue();
        Long delayInSecondsValue = delayInSecondsBox.getValue();
        Integer lastNRacesToShowValue = activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.LAST_N ?
                numberOfLastRacesToShowBox.getValue() : null;
        return new LeaderboardSettings(maneuverDetailsToShow, legDetailsToShow, raceDetailsToShow,
                overallDetailsToShow, namesOfRaceColumnsToShow, /* nameOfRacesToShow */null,
                lastNRacesToShowValue,
                autoExpandPreSelectedRace, 1000l * (delayBetweenAutoAdvancesValue == null ? 0l : delayBetweenAutoAdvancesValue.longValue()),
                1000 * (delayInSecondsValue == null ? 0 : delayInSecondsValue.longValue()),
                null, true, /* updateUponPlayStateChange */ true, activeRaceColumnSelectionStrategy);
    }

    @Override
    public Validator<LeaderboardSettings> getValidator() {
        return new Validator<LeaderboardSettings>() {
            @Override
            public String getErrorMessage(LeaderboardSettings valueToValidate) {
                if (valueToValidate.getLegDetailsToShow().isEmpty()) {
                    return stringMessages.selectAtLeastOneLegDetail();
                } else if (valueToValidate.getDelayBetweenAutoAdvancesInMilliseconds() < 1000) {
                    return stringMessages.chooseUpdateIntervalOfAtLeastOneSecond();
                } else if (valueToValidate.getActiveRaceColumnSelectionStrategy() == RaceColumnSelectionStrategies.LAST_N
                        && (numberOfLastRacesToShowBox.getValue() == null || numberOfLastRacesToShowBox.getValue() < 0)) {
                    return stringMessages.numberOfRacesMustBeNonNegativeNumber();
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public FocusWidget getFocusWidget() {
        return delayInSecondsBox;
    }
}
