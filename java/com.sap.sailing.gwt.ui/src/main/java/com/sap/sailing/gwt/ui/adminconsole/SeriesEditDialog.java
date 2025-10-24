package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.controls.listedit.GenericStringListInlineEditorComposite;
import com.sap.sse.gwt.client.controls.listedit.StringListEditorComposite;
import com.sap.sse.gwt.client.controls.listedit.StringListInlineEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class SeriesEditDialog extends DataEntryDialog<SeriesDescriptor> {
    private TextBox seriesNameTextBox;
    private CheckBox isMedalCheckbox;
    private CheckBox fleetsCanRunInParallelCheckbox;
    private CheckBox startWithZeroScoreCheckbox;
    private CheckBox hasSplitFleetContiguousScoringCheckbox;
    private CheckBox hasCrossFleetMergedRankingCheckbox;
    private CheckBox firstColumnIsNonDiscardableCarryForwardCheckbox;
    private CheckBox oneAlwaysStaysOneCheckbox;
    private IntegerBox maximumNumberOfDiscardsBox;
    private CheckBox useSeriesResultDiscardingThresholdsCheckbox;
    private final StringMessages stringMessages;
    private VerticalPanel additionalWidgetPanel;
    private final SeriesDTO selectedSeries;
    private final RegattaDTO regatta;
    private final DiscardThresholdBoxes discardThresholdBoxes;
    private StringListEditorComposite raceNamesEditor;

    private static class RaceDialogValidator implements Validator<SeriesDescriptor> {
        private StringMessages stringMessages;
        private RegattaDTO regatta;

        public RaceDialogValidator(RegattaDTO regatta, StringMessages stringMessages) {
            this.stringMessages = stringMessages;
            this.regatta = regatta;
        }

        @Override
        public String getErrorMessage(SeriesDescriptor valueToValidate) {
            Set<String> raceColumnNamesOfOtherSeries = new HashSet<String>();
            SeriesDTO seriesToValidate = valueToValidate.getSeries();
            List<RaceColumnDTO> raceColumnsToValidate = valueToValidate.getRaces();
            String errorMessage = null;
            for (SeriesDTO seriesDTO: regatta.series) {
                if (!seriesDTO.getName().equals(seriesToValidate.getName())) {
                    for (RaceColumnDTO raceColumn : seriesDTO.getRaceColumns()) {
                        raceColumnNamesOfOtherSeries.add(raceColumn.getName());
                    }
                }
            }
            boolean raceColumnNameNotEmpty = true;
            RaceColumnDTO wrongRaceColumn = null;
            for (RaceColumnDTO raceColumn : raceColumnsToValidate) {
                raceColumnNameNotEmpty = raceColumn.getName() != null && raceColumn.getName().length() > 0;
                if (!raceColumnNameNotEmpty) {
                    wrongRaceColumn = raceColumn;
                    break;
                }
            }
            boolean raceColumnUniqueInSeries = true;
            boolean raceColumnUniqueInRegatta = true;
            HashSet<String> setToFindDuplicates = new HashSet<String>();
            for (RaceColumnDTO raceColumn : raceColumnsToValidate) {
                if (!setToFindDuplicates.add(raceColumn.getName())) {
                    raceColumnUniqueInSeries = false;
                    wrongRaceColumn = raceColumn;
                    break;
                } else if(raceColumnNamesOfOtherSeries.contains(raceColumn.getName())) {
                    raceColumnUniqueInRegatta = false;
                    wrongRaceColumn = raceColumn;
                    break;
                }
            }
            if (!raceColumnNameNotEmpty) {
                errorMessage = stringMessages.race() + " " + wrongRaceColumn.getName() + ": "
                        + stringMessages.pleaseEnterAName();
            } else if (!raceColumnUniqueInSeries) {
                errorMessage = stringMessages.race() + " " + wrongRaceColumn.getName() + ": "
                        + stringMessages.raceWithThisNameAlreadyExists();
            } else if (!raceColumnUniqueInRegatta) {
                errorMessage = stringMessages.race() + " " + wrongRaceColumn.getName() + ": "
                        + stringMessages.raceWithThisNameAlreadyExistsInRegatta();
            } else {
                errorMessage = DiscardThresholdBoxes.getErrorMessage(valueToValidate.getResultDiscardingThresholds(), stringMessages);
            }
            return errorMessage;
        }
    }

    public SeriesEditDialog(RegattaDTO regatta, SeriesDTO selectedSeries, StringMessages stringMessages,DialogCallback<SeriesDescriptor> callback) {
        super(stringMessages.actionEditSeries(), null, stringMessages.ok(), stringMessages.cancel(),
                new RaceDialogValidator(regatta, stringMessages), callback);
        this.regatta = regatta;
        this.selectedSeries = selectedSeries;
        this.stringMessages = stringMessages;
        discardThresholdBoxes = new DiscardThresholdBoxes(this, selectedSeries.getDiscardThresholds(), stringMessages);
    }

    @Override
    protected SeriesDescriptor getResult() {
        SeriesDTO selectedSeries = getSelectedSeries();
        List<RaceColumnDTO> races = new ArrayList<RaceColumnDTO>();
        for (String name : raceNamesEditor.getValue()) {
            RaceColumnDTO raceColumnDTO = findRaceColumnInSeriesByName(selectedSeries, name);
            if (raceColumnDTO == null) {
                raceColumnDTO = new RaceColumnInSeriesDTO(name, selectedSeries.getName(), regatta.getName(), selectedSeries.isOneAlwaysStaysOne());
            }
            races.add(raceColumnDTO);
        }
        return new SeriesDescriptor(selectedSeries, seriesNameTextBox.getValue(), races, isMedalCheckbox.getValue(),
                fleetsCanRunInParallelCheckbox.getValue(),
                useSeriesResultDiscardingThresholdsCheckbox.getValue() ? discardThresholdBoxes.getDiscardThresholds()
                        : null, startWithZeroScoreCheckbox.getValue(),
                firstColumnIsNonDiscardableCarryForwardCheckbox.getValue(), hasSplitFleetContiguousScoringCheckbox.getValue(),
                hasCrossFleetMergedRankingCheckbox.getValue(), maximumNumberOfDiscardsBox.getValue(), oneAlwaysStaysOneCheckbox.getValue());
    }

    private RaceColumnDTO findRaceColumnInSeriesByName(SeriesDTO series, String raceColumnName) {
        RaceColumnDTO result = null;
        if (series != null) {
            for (RaceColumnDTO raceColumn : series.getRaceColumns()) {
                if (raceColumn.getName().equals(raceColumnName)) {
                    result = raceColumn;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    protected Widget getAdditionalWidget() {
        additionalWidgetPanel = new VerticalPanel();
        Widget additionalWidget = super.getAdditionalWidget();
        if (additionalWidget != null) {
            additionalWidgetPanel.add(additionalWidget);
        }
        HorizontalPanel seriesPanel = new HorizontalPanel();
        seriesPanel.setSpacing(3);
        String seriesName = getSelectedSeries().getName();
        seriesPanel.add(new Label(stringMessages.series() + ": " + seriesName));
        additionalWidgetPanel.add(seriesPanel);

        seriesNameTextBox = createTextBox(seriesName);
        additionalWidgetPanel.add(seriesNameTextBox);

        isMedalCheckbox = createCheckbox(stringMessages.medalSeries());
        isMedalCheckbox.ensureDebugId("MedalSeriesCheckbox");
        isMedalCheckbox.setValue(selectedSeries.isMedal());
        additionalWidgetPanel.add(isMedalCheckbox);

        fleetsCanRunInParallelCheckbox = createCheckbox(stringMessages.canFleetsRunInParallel());
        fleetsCanRunInParallelCheckbox.ensureDebugId("FleetsCanRaceInParallelSeriesCheckbox");
        fleetsCanRunInParallelCheckbox.setValue(selectedSeries.isFleetsCanRunInParallel());
        additionalWidgetPanel.add(fleetsCanRunInParallelCheckbox);

        startWithZeroScoreCheckbox = createCheckbox(stringMessages.startsWithZeroScore());
        startWithZeroScoreCheckbox.ensureDebugId("StartsWithZeroScoreCheckbox");
        startWithZeroScoreCheckbox.setValue(selectedSeries.isStartsWithZeroScore());
        additionalWidgetPanel.add(startWithZeroScoreCheckbox);

        hasSplitFleetContiguousScoringCheckbox = createCheckbox(stringMessages.hasSplitFleetContiguousScoring());
        hasSplitFleetContiguousScoringCheckbox.ensureDebugId("HasSplitFleetContiguousScoringCheckbox");
        hasSplitFleetContiguousScoringCheckbox.setValue(selectedSeries.hasSplitFleetContiguousScoring());
        additionalWidgetPanel.add(hasSplitFleetContiguousScoringCheckbox);

        hasCrossFleetMergedRankingCheckbox = createCheckbox(stringMessages.hasCrossFleetMergedRanking());
        hasCrossFleetMergedRankingCheckbox.ensureDebugId("HasCrossFleetMergedRankingCheckbox");
        hasCrossFleetMergedRankingCheckbox.setValue(selectedSeries.hasCrossFleetMergedRanking());
        additionalWidgetPanel.add(hasCrossFleetMergedRankingCheckbox);

        firstColumnIsNonDiscardableCarryForwardCheckbox = createCheckbox(stringMessages.firstRaceIsNonDiscardableCarryForward());
        firstColumnIsNonDiscardableCarryForwardCheckbox.ensureDebugId("StartsWithNonDiscardableCarryForwardCheckbox");
        firstColumnIsNonDiscardableCarryForwardCheckbox.setValue(selectedSeries.isFirstColumnIsNonDiscardableCarryForward());
        additionalWidgetPanel.add(firstColumnIsNonDiscardableCarryForwardCheckbox);

        oneAlwaysStaysOneCheckbox = createCheckbox(stringMessages.oneAlwaysStaysOne());
        oneAlwaysStaysOneCheckbox.ensureDebugId("OneAlwaysStaysOneCheckbox");
        oneAlwaysStaysOneCheckbox.setValue(selectedSeries.isOneAlwaysStaysOne());
        additionalWidgetPanel.add(oneAlwaysStaysOneCheckbox);

        final HorizontalPanel maximumNumberOfDiscardsPanel = new HorizontalPanel();
        maximumNumberOfDiscardsPanel.add(new Label(stringMessages.maximumNumberOfDiscards()));
        maximumNumberOfDiscardsBox = createIntegerBox(selectedSeries.getMaximumNumberOfDiscards(), /* visibleLength */ 3);
        maximumNumberOfDiscardsPanel.add(maximumNumberOfDiscardsBox);
        additionalWidgetPanel.add(maximumNumberOfDiscardsPanel);

        useSeriesResultDiscardingThresholdsCheckbox = createCheckbox(stringMessages.seriesDefinesResultDiscardingRule());
        useSeriesResultDiscardingThresholdsCheckbox.ensureDebugId("DefinesResultDiscardingRulesCheckbox");
        useSeriesResultDiscardingThresholdsCheckbox.setValue(selectedSeries.getDiscardThresholds() != null);
        useSeriesResultDiscardingThresholdsCheckbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                discardThresholdBoxes.getWidget().setVisible(event.getValue());
            }
        });
        additionalWidgetPanel.add(useSeriesResultDiscardingThresholdsCheckbox);

        Widget discardThresholdBoxesWidget = discardThresholdBoxes.getWidget();
        discardThresholdBoxesWidget.ensureDebugId("");
        discardThresholdBoxesWidget.setVisible(useSeriesResultDiscardingThresholdsCheckbox.getValue());
        additionalWidgetPanel.add(discardThresholdBoxesWidget);

        raceNamesEditor = new StringListInlineEditorComposite(getExistingRacesOfSeries(), new RaceNamesEditorUi(regatta, stringMessages, IconResources.INSTANCE.removeIcon(), seriesName));
        raceNamesEditor.ensureDebugId("RaceNamesStringListEditorComposite");
        raceNamesEditor.addValueChangeHandler(new ValueChangeHandler<Iterable<String>>() {
            @Override
            public void onValueChange(ValueChangeEvent<Iterable<String>> event) {
                validateAndUpdate();
            }
        });

        TabPanel tabPanel = new TabPanel();
        tabPanel.setWidth("100%");
        tabPanel.add(raceNamesEditor, stringMessages.races());
        tabPanel.selectTab(0);
        additionalWidgetPanel.add(tabPanel);

        return additionalWidgetPanel;
    }

    private List<String> getExistingRacesOfSeries() {
        List<String> names = new ArrayList<String>();
        SeriesDTO selectedSeries = getSelectedSeries();
        if(selectedSeries != null && !selectedSeries.getRaceColumns().isEmpty()) {
            for(RaceColumnDTO raceColumn: selectedSeries.getRaceColumns()) {
                names.add(raceColumn.getName());
            }
        }
        return names;
    }

    private SeriesDTO getSelectedSeries() {
        return selectedSeries;
    }

    private class RaceNamesEditorUi extends GenericStringListInlineEditorComposite.ExpandedUi<String> {
        private final RegattaDTO regatta;

        private final ListBox addRacesFromListBox;
        private final ListBox addRacesToListBox;
        private final TextBox raceNamePrefixTextBox;
        private final Button addRacesBtn;

        private final String seriesName;
        private final Label addRacesHintLabel;

        public RaceNamesEditorUi(RegattaDTO regatta, StringMessages stringMessages, ImageResource removeImage, String seriesName) {
            super(stringMessages, removeImage, /* suggest values */ Collections.emptySet(), stringMessages.enterRaceName(), 40);

            this.seriesName = seriesName;
            this.regatta = regatta;

            this.addRacesFromListBox = createListBox(false);
            this.addRacesFromListBox.ensureDebugId("AddRacesFromListBox");

            this.addRacesToListBox = createListBox(false);
            this.addRacesToListBox.ensureDebugId("AddRacesToListBox");

            this.raceNamePrefixTextBox = createTextBox(null);
            this.raceNamePrefixTextBox.ensureDebugId("RaceNamePrefixTextBox");

            this.addRacesBtn = new Button(stringMessages.add());
            this.addRacesBtn.ensureDebugId("AddRacesButton");

            this.addRacesHintLabel = new Label("");
        }

        @Override
        protected StringMessages getStringMessages() {
            return (StringMessages) super.getStringMessages();
        }

        private List<String> resolveRaceNamesToAdd() {
            List<String> result = new ArrayList<String>();
            String racePrefix = raceNamePrefixTextBox.getText();
            int to = addRacesToListBox.getSelectedIndex() + 1;
            int from = addRacesFromListBox.getSelectedIndex() + 1;
            int racesToCreate = to - from + 1;
            if(racesToCreate > 0) {
                for(int i = from; i <= to; i++) {
                    String raceName = racePrefix + i;
                    result.add(raceName);
                }
            }

            return result;
        }

        public void updateHintLabel() {
            List<String> resolveRaceNamesToAdd = resolveRaceNamesToAdd();
            String hintText = "Hint: 'Add' will create the races: ";
            for(String raceName: resolveRaceNamesToAdd) {
                hintText += raceName + " ";
            }
            addRacesHintLabel.setText(hintText);
        }

        public void updateFromToListboxesSelection() {
            int nextNumber = calculateNextValidRaceNumber(raceNamePrefixTextBox.getValue());
            addRacesFromListBox.setSelectedIndex(nextNumber-1);
            addRacesToListBox.setSelectedIndex(nextNumber-1);
        }

        private int calculateNextValidRaceNumber(String prefix) {
            int maxNumber = 0;
            List<String> allRaces = new ArrayList<String>();
            for (SeriesDTO seriesDTO: regatta.series) {
                if(seriesName.equals(seriesDTO.getName())) {
                    allRaces.addAll(context.getValue());
                } else {
                    for (RaceColumnDTO raceColumn : seriesDTO.getRaceColumns()) {
                        allRaces.add(raceColumn.getName());
                    }
                }
            }
            for(String name: allRaces) {
                if(prefix != null && !prefix.isEmpty()) {
                    if(name.startsWith(prefix)) {
                        String withoutPrefix = name.substring(prefix.length(), name.length());
                        try {
                            int number = Integer.parseInt(withoutPrefix);
                            if(number > maxNumber) {
                                maxNumber = number;
                            }
                        } catch ( NumberFormatException nbe) {
                            // do nothing
                        }
                    }
                } else {
                    try {
                        int number = Integer.parseInt(name);
                        if(number > maxNumber) {
                            maxNumber = number;
                        }
                    } catch ( NumberFormatException nbe) {
                        // do nothing
                    }
                }
            }
            return maxNumber+1;
        }

        @Override
        protected Widget createAddWidget() {
            VerticalPanel vPanel = new VerticalPanel();
            HorizontalPanel addRacesPanel = new HorizontalPanel();
            addRacesPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
            addRacesPanel.setSpacing(5);
            addRacesPanel.add(new Label(getStringMessages().addRaces()));
            for(int i = 1; i <= 100; i++) {
                addRacesFromListBox.addItem("" + i);
                addRacesToListBox.addItem("" + i);
            }
            updateFromToListboxesSelection();
            addRacesFromListBox.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    updateHintLabel();
                }
            });
            addRacesToListBox.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    updateHintLabel();
                }
            });
            addRacesPanel.add(addRacesFromListBox);
            addRacesPanel.add(new Label(getStringMessages().to()));
            addRacesPanel.add(addRacesToListBox);
            addRacesPanel.add(new Label(getStringMessages().withNamePrefix()));
            raceNamePrefixTextBox.setWidth("20px");
            if (LeaderboardNameConstants.DEFAULT_SERIES_NAME.equals(seriesName)) {
                raceNamePrefixTextBox.setText("R");
            } else {
                raceNamePrefixTextBox.setText(seriesName.substring(0, 1).toUpperCase());
            }
            raceNamePrefixTextBox.addValueChangeHandler(new ValueChangeHandler<String>() {
                @Override
                public void onValueChange(ValueChangeEvent<String> event) {
                    updateHintLabel();
                }
            });
            addRacesPanel.add(raceNamePrefixTextBox);
            addRacesBtn.addStyleName("inlineButton");
            addRacesBtn.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    SeriesDTO selectedSeries = getSelectedSeries();
                    if (selectedSeries != null) {
                        List<String> raceNamesToAdd = resolveRaceNamesToAdd();
                        for (String raceToAdd : raceNamesToAdd) {
                            addValue(raceToAdd);
                        }
                        validateAndUpdate();
                    } else {
                        Notification.notify(getStringMessages().pleaseSelectASeriesFirst(), NotificationType.ERROR);
                    }
                }
            });
            addRacesPanel.add(addRacesBtn);
            vPanel.add(addRacesPanel);
            addRacesHintLabel.getElement().getStyle().setColor("gray");
            vPanel.add(addRacesHintLabel);
            updateFromToListboxesSelection();
            updateHintLabel();
            return vPanel;
        }

        @Override
        public void onRowAdded() {
            updateFromToListboxesSelection();
            updateHintLabel();
        }

        @Override
        public void onRowRemoved(int rowIndex) {
            updateFromToListboxesSelection();
            updateHintLabel();
        }
    }
}
