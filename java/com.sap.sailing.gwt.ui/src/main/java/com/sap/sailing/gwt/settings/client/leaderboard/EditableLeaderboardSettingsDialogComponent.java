package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public class EditableLeaderboardSettingsDialogComponent
        extends LeaderboardSettingsDialogComponent<EditableLeaderboardSettings> {

    private final Map<String, CheckBox> raceColumnCheckboxes;
    private final List<String> raceAllRaceColumnNames;
    private RaceColumnSelectionStrategies activeRaceColumnSelectionStrategy;
    private CheckBox showCarryColumnCheckbox = new CheckBox();

    public EditableLeaderboardSettingsDialogComponent(EditableLeaderboardSettings initialSettings,
            List<String> allRaceColumnNames, StringMessages stringMessages, Iterable<DetailType> availableDetailTypes, 
            boolean canBoatInfoBeShown, PaywallResolver paywallResolver, AbstractLeaderboardDTO leaderboardDTO) {
        super(initialSettings, stringMessages, availableDetailTypes, canBoatInfoBeShown, paywallResolver, leaderboardDTO);
        this.activeRaceColumnSelectionStrategy = initialSettings.getActiveRaceColumnSelectionStrategy();
        this.raceAllRaceColumnNames = allRaceColumnNames;
        raceColumnCheckboxes = new LinkedHashMap<>();
    }

    @Override
    public EditableLeaderboardSettings getResult() {
        List<DetailType> maneuverDetailsToShow = getSelected(maneuverDetailCheckboxes, 
                initialSettings.getManeuverDetailsToShow());
        List<DetailType> overallDetailsToShow = getSelected(overallDetailCheckboxes, 
                initialSettings.getOverallDetailsToShow());
        List<DetailType> raceDetailsToShow = getSelected(raceDetailCheckboxes, initialSettings.getRaceDetailsToShow());
        List<DetailType> legDetailsToShow = getSelected(legDetailCheckboxes, initialSettings.getLegDetailsToShow());
        List<String> namesOfRaceColumnsToShow = null;
        if (activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.EXPLICIT) {
            namesOfRaceColumnsToShow = new ArrayList<String>();
            for (Map.Entry<String, CheckBox> entry : raceColumnCheckboxes.entrySet()) {
                if (entry.getValue().getValue()) {
                    namesOfRaceColumnsToShow.add(entry.getKey());
                }
            }
        }
        Long delayBetweenAutoAdvancesValue = refreshIntervalInSecondsBox.getValue();
        Integer lastNRacesToShowValue = activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.LAST_N
                ? numberOfLastRacesToShowBox.getValue() : null;
        Long delayBetweenAutoAdvancesInMilliseconds = 1000l * (delayBetweenAutoAdvancesValue == null 
                ? 0l : delayBetweenAutoAdvancesValue.longValue());
        final EditableLeaderboardSettings newSettings = new EditableLeaderboardSettings(maneuverDetailsToShow,
                legDetailsToShow, raceDetailsToShow, overallDetailsToShow, namesOfRaceColumnsToShow,
                /* numberOfLastRacesToShow */ lastNRacesToShowValue,
                delayBetweenAutoAdvancesInMilliseconds,
                activeRaceColumnSelectionStrategy,
                /* showAddedScores */ showAddedScoresCheckBox.getValue().booleanValue(),
                showCompetitorShortNameColumnCheckBox.getValue(), showCompetitorFullNameColumnCheckBox.getValue(),
                showCompetitorBoatInfoColumnCheckBox.getValue(), isCompetitorNationalityColumnVisible.getValue(), 
                showCarryColumnCheckbox.getValue(), new SecurityChildSettingsContext(leaderboardDTO, paywallResolver));
        return newSettings;
    }

    private FlowPanel createSelectedRacesPanel(DataEntryDialog<?> dialog) {
        FlowPanel selectedRacesPanel = new FlowPanel();
        selectedRacesPanel.ensureDebugId("RaceSelectionSettingsPanel");
        selectedRacesPanel.addStyleName("SettingsDialogComponent");
        selectedRacesPanel.add(dialog.createHeadline(stringMessages.selectedRaces(), true));
        // race selection strategy elements
        HorizontalPanel racesSelectionStrategyPanel = new HorizontalPanel();
        selectedRacesPanel.add(racesSelectionStrategyPanel);

        FlowPanel selectedRacesContent = new FlowPanel();
        selectedRacesContent.addStyleName("dialogInnerContent");
        selectedRacesPanel.add(selectedRacesContent);
        
        showCarryColumnCheckbox = createCheckbox(dialog, stringMessages.showCarryColumn(), 
                initialSettings.getShowCarryColumn(), stringMessages.showCarryColumnTitle());

        // Attention: We need to consider that there are regattas with more than 30 races
        int racesCount = raceAllRaceColumnNames.size();
        if (racesCount > 0) {
            final FlowPanel explicitRaceSelectionContent = new FlowPanel();
            explicitRaceSelectionContent.ensureDebugId("ExplicitRaceSelectionPanel");
            final FlowPanel lastNRacesSelectionContent = new FlowPanel();
            lastNRacesSelectionContent.ensureDebugId("MostCurrentRacesSelectionPanel");

            String radioButtonGroupName = "raceSelectionStrategyGroup";
            Label raceSelectionWayLabel = new Label(stringMessages.chooseTheWayYouSelectRaces() + ":");
            raceSelectionWayLabel.getElement().getStyle().setPaddingRight(5, Unit.PX);
            racesSelectionStrategyPanel.add(raceSelectionWayLabel);
            explicitRaceColumnSelectionRadioBtn = dialog.createRadioButton(radioButtonGroupName,
                    stringMessages.selectFromAllRaces());
            explicitRaceColumnSelectionRadioBtn.ensureDebugId("ExplicitRaceSelectionRadioButton");
            racesSelectionStrategyPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
            explicitRaceColumnSelectionRadioBtn
                    .setValue(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.EXPLICIT);
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
            if (racesCount % maxRacesPerRow != 0) {
                rowCount++;
            }
            Grid grid = new Grid(rowCount, maxRacesPerRow);
            List<String> namesOfRaceColumnsToShow = initialSettings.getNamesOfRaceColumnsToShow();
            final boolean noRaceSelected = namesOfRaceColumnsToShow == null || Util.isEmpty(namesOfRaceColumnsToShow);
            for (String raceColumnName : raceAllRaceColumnNames) {
                CheckBox checkbox = createCheckbox(dialog, raceColumnName,
                        noRaceSelected || Util.contains(namesOfRaceColumnsToShow, raceColumnName), null);
                raceColumnCheckboxes.put(raceColumnName, checkbox);
                grid.setWidget(rowIndex, columnIndex++, checkbox);
                if (columnIndex == maxRacesPerRow) {
                    rowIndex++;
                    columnIndex = 0;
                }
            }
            explicitRaceSelectionContent.add(grid);
            explicitRaceSelectionContent
                    .setVisible(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.EXPLICIT);
            selectedRacesContent.add(explicitRaceSelectionContent);

            lastNRacesColumnSelectionRadioBtn = dialog.createRadioButton(radioButtonGroupName,
                    stringMessages.selectANumberOfRaces());
            lastNRacesColumnSelectionRadioBtn.ensureDebugId("MostCurrentRacesSelectionRadioButton");
            lastNRacesColumnSelectionRadioBtn
                    .setValue(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.LAST_N);
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
            Integer numberOfLastRacesToShow = initialSettings.getNumberOfLastRacesToShow();
            numberOfLastRacesToShowBox = dialog
                    .createIntegerBox(numberOfLastRacesToShow != null ? numberOfLastRacesToShow : racesCount, 3);
            numberOfLastRacesToShowBox.ensureDebugId("NumberOfMostCurrentRacesIntegerBox");
            hPanel.add(numberOfLastRacesToShowBox);
            dialog.alignAllPanelWidgetsVertically(hPanel, HasVerticalAlignment.ALIGN_MIDDLE);
            lastNRacesSelectionContent
                    .setVisible(activeRaceColumnSelectionStrategy == RaceColumnSelectionStrategies.LAST_N);
            selectedRacesContent.add(lastNRacesSelectionContent);
        } else {
            selectedRacesContent.add(new Label(stringMessages.noRacesYet()));
        }
        return selectedRacesPanel;
    }
    
    protected FlowPanel createShowCarryColumnDetailPanel(DataEntryDialog<?> dialog) {
        FlowPanel showCarryColumnDialog = new FlowPanel();
        showCarryColumnDialog.ensureDebugId("ShowCarryColumnSettingsPanel");
        showCarryColumnDialog.add(dialog.createHeadline(stringMessages.showCarryColumn(), true));
        showCarryColumnDialog.addStyleName("SettingsDialogComponent showCarryColumnSettings");
        FlowPanel showCarryColumnDialogContent = new FlowPanel();
        showCarryColumnDialogContent.addStyleName("dialogInnerContent");
       
        FlowPanel showCarryColumnDialogContentSecondLine = new FlowPanel();
        showCarryColumnDialogContentSecondLine.addStyleName("dialogInnerContent");
       
        showCarryColumnCheckbox = dialog.createCheckbox(stringMessages.showCarryColumnCheckbox());
        showCarryColumnCheckbox.setTitle(stringMessages.showCarryColumnTitle());
        showCarryColumnCheckbox.setValue(initialSettings.getShowCarryColumn());
        showCarryColumnDialogContentSecondLine.add(showCarryColumnCheckbox);
        showCarryColumnDialog.add(showCarryColumnDialogContent);
        showCarryColumnDialog.add(showCarryColumnDialogContentSecondLine);
        return showCarryColumnDialog;
    }
    

    @Override
    public Widget getAdditionalWidget(DataEntryDialog<?> dialog) {
        FlowPanel dialogPanel = new FlowPanel();
        dialogPanel.ensureDebugId("EditableLeaderboardSettingsPanel");
        dialogPanel.add(createSelectedRacesPanel(dialog));
        dialogPanel.add(createOverallDetailPanel(dialog));
        dialogPanel.add(createRaceDetailPanel(dialog));
        dialogPanel.add(createRaceStartAnalysisPanel(dialog));
        dialogPanel.add(createLegDetailsPanel(dialog));
        dialogPanel.add(createManeuverDetailsPanel(dialog));
        dialogPanel.add(createTimingDetailsPanel(dialog));
        dialogPanel.add(createShowCarryColumnDetailPanel(dialog));
        return dialogPanel;
    }

    @Override
    public Validator<EditableLeaderboardSettings> getValidator() {
        return new Validator<EditableLeaderboardSettings>() {
            @Override
            public String getErrorMessage(EditableLeaderboardSettings valueToValidate) {
                final String result;
                if (valueToValidate.getLegDetailsToShow().isEmpty()) {
                    result = stringMessages.selectAtLeastOneLegDetail();
                } else if (valueToValidate.getDelayBetweenAutoAdvancesInMilliseconds() < 1000) {
                    result = stringMessages.chooseUpdateIntervalOfAtLeastOneSecond();
                } else if (valueToValidate.getActiveRaceColumnSelectionStrategy() == RaceColumnSelectionStrategies.LAST_N
                        && (numberOfLastRacesToShowBox.getValue() == null || numberOfLastRacesToShowBox.getValue() < 0)) {
                    result = stringMessages.numberOfRacesMustBeNonNegativeNumber();
                } else {
                    result = null;
                }
                return result;
            }
        };
    }
}
