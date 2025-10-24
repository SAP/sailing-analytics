package com.sap.sailing.gwt.ui.leaderboardedit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.ui.client.SailNumberCanonicalizerAndMatcher;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.BulkScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO.ScoreCorrectionEntryDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class MatchAndApplyScoreCorrectionsDialog extends DataEntryDialog<BulkScoreCorrectionDTO> {
    private final LeaderboardDTO leaderboard;
    private final Map<CompetitorDTO, String> defaultOfficialSailIDsForCompetitors;
    private final TreeSet<String> allOfficialSailIDs;
    private final Map<RaceColumnDTO, String> raceColumnToOfficialRaceNameOrNumber;
    private final RegattaScoreCorrectionDTO regattaScoreCorrection;
    private final Map<CompetitorDTO, CheckBox> competitorCheckboxes;
    private final Map<RaceColumnDTO, CheckBox> raceColumnCheckboxes;
    private final Map<Util.Pair<CompetitorDTO, RaceColumnDTO>, CheckBox> cellCheckboxes;
    private final Grid grid;
    private final Map<RaceColumnDTO, ListBox> raceNameOrNumberChoosers;
    private final Map<CompetitorDTO, ListBox> officialSailIDChoosers;
    private final CheckBox allAllCheckbox;

    public MatchAndApplyScoreCorrectionsDialog(EditableLeaderboardPanel leaderboardPanel, StringMessages stringMessages,
            SailingServiceWriteAsync sailingServiceWrite, ErrorReporter errorReporter, RegattaScoreCorrectionDTO result) {
        super(stringMessages.assignRaceNumbersToRaceColumns(), stringMessages.assignRaceNumbersToRaceColumns(),
                stringMessages.ok(), stringMessages.cancel(), new Validator(), new Callback(leaderboardPanel,
                        sailingServiceWrite, stringMessages, errorReporter));
        this.regattaScoreCorrection = result;
        this.leaderboard = leaderboardPanel.getLeaderboard();
        this.allOfficialSailIDs = new TreeSet<>();
        for (final Map<String, ScoreCorrectionEntryDTO> raceResultsBySailNumber : result.getScoreCorrectionsByRaceNameOrNumber().values()) {
            allOfficialSailIDs.addAll(raceResultsBySailNumber.keySet());
        }
        this.defaultOfficialSailIDsForCompetitors = new HashMap<>();
        for (final Entry<String, CompetitorDTO> e : new SailNumberCanonicalizerAndMatcher().mapCompetitorsAndInitializeAllOfficialRaceIDs(leaderboard.competitors, allOfficialSailIDs).entrySet()) {
            defaultOfficialSailIDsForCompetitors.put(e.getValue(), e.getKey());
        }
        this.raceColumnToOfficialRaceNameOrNumber = createRaceColumnNameToOfficialRaceNameOrNumberSuggestion(leaderboard, result);
        competitorCheckboxes = new HashMap<>();
        for (final CompetitorDTO competitor : leaderboard.competitors) {
            CheckBox checkbox = createCheckbox(stringMessages.selectAll());
            checkbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    for (RaceColumnDTO raceColumn : MatchAndApplyScoreCorrectionsDialog.this.leaderboard.getRaceList()) {
                        cellCheckboxes.get(new Util.Pair<>(competitor, raceColumn)).setValue(event.getValue());
                    }
                }
            });
            competitorCheckboxes.put(competitor, checkbox);
        }
        raceColumnCheckboxes = new HashMap<RaceColumnDTO, CheckBox>();
        for (final RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
            CheckBox checkbox = createCheckbox(stringMessages.selectAll());
            checkbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    for (CompetitorDTO competitor : MatchAndApplyScoreCorrectionsDialog.this.leaderboard.competitors) {
                        cellCheckboxes.get(new Util.Pair<CompetitorDTO, RaceColumnDTO>(competitor, raceColumn)).setValue(event.getValue());
                    }
                }
            });
            raceColumnCheckboxes.put(raceColumn, checkbox);
        }
        cellCheckboxes = new HashMap<>();
        for (final CompetitorDTO competitor : leaderboard.competitors) {
            for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                cellCheckboxes.put(new Util.Pair<>(competitor, raceColumn), createCheckbox(stringMessages.apply()));
            }
        }
        allAllCheckbox = createCheckbox(stringMessages.selectAll());
        allAllCheckbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                for (CompetitorDTO competitor : leaderboard.competitors) {
                    competitorCheckboxes.get(competitor).setValue(event.getValue());
                    for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                        cellCheckboxes.get(new Util.Pair<>(competitor, raceColumn)).setValue(event.getValue());
                    }
                }
                for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                    raceColumnCheckboxes.get(raceColumn).setValue(event.getValue());
                }
            }
        });
        raceNameOrNumberChoosers = new HashMap<RaceColumnDTO, ListBox>();
        officialSailIDChoosers = new HashMap<>();
        grid = new Grid(leaderboard.competitors.size()+1, leaderboard.getRaceList().size()+1);
        fillRaceNameOrNumberChoosers();
        fillOfficialSailIDChoosers();
    }

    private void fillRaceNameOrNumberChoosers() {
        final Set<String> entries = regattaScoreCorrection.getScoreCorrectionsByRaceNameOrNumber().keySet();
        for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
            ListBox listbox = createListBoxWithGridUpdateChangeHandler(entries, /* select */ raceColumnToOfficialRaceNameOrNumber.get(raceColumn));
            raceNameOrNumberChoosers.put(raceColumn, listbox);
        }
    }

    private ListBox createListBoxWithGridUpdateChangeHandler(Set<String> entries, String selectedItem) {
        ListBox result = createListBox(/* isMultipleSelect */ false);
        result.addItem("");
        int i=1;
        int selectionIndex = -1;
        for (String entry : entries) {
            result.addItem(entry, entry);
            if (selectedItem != null && selectedItem.equals(entry)) {
                selectionIndex = i;
            }
            i++;
        }
        if (selectionIndex != -1) {
            result.setSelectedIndex(selectionIndex);
        }
        result.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                updateGridContents(grid);
            }
        });
        return result;
    }

    private void fillOfficialSailIDChoosers() {
        for (CompetitorDTO competitor : leaderboard.competitors) {
            ListBox listbox = createListBoxWithGridUpdateChangeHandler(allOfficialSailIDs, /* selection */
                    defaultOfficialSailIDsForCompetitors.get(competitor));
            officialSailIDChoosers.put(competitor, listbox);
        }
    }

    /**
     * The default suggestion made for the mapping of leaderboard race column names to the official race name/number scheme
     * is by their ordering.
     */
    private Map<RaceColumnDTO, String> createRaceColumnNameToOfficialRaceNameOrNumberSuggestion(
            LeaderboardDTO leaderboard, RegattaScoreCorrectionDTO regattaScoreCorretion) {
        Map<RaceColumnDTO, String> result = new HashMap<RaceColumnDTO, String>();
        Iterator<RaceColumnDTO> raceColumnIter = leaderboard.getRaceList().iterator();
        Iterator<String> officialRaceNameOrNumberIter = regattaScoreCorretion.getScoreCorrectionsByRaceNameOrNumber().keySet().iterator();
        while (raceColumnIter.hasNext() && officialRaceNameOrNumberIter.hasNext()) {
            result.put(raceColumnIter.next(), officialRaceNameOrNumberIter.next());
        }
        return result;
    }

    private String getCompetitorIdentifyingText(CompetitorDTO competitor) {
        final String competitorIdentifyingText;
        if (competitor.hasBoat()) {
            competitorIdentifyingText = ((CompetitorWithBoatDTO) competitor).getSailID();
        } else {
            competitorIdentifyingText = competitor.getShortName();
        }
        return competitorIdentifyingText;
    }

    @Override
    protected BulkScoreCorrectionDTO getResult() {
        BulkScoreCorrectionDTO result = new BulkScoreCorrectionDTO(leaderboard.getName());
        for (CompetitorDTO competitor : leaderboard.competitors) {
            for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                Util.Pair<CompetitorDTO, RaceColumnDTO> key = new Util.Pair<>(competitor, raceColumn);
                CheckBox cellCheckbox = cellCheckboxes.get(key);
                if (cellCheckbox.getValue()) {
                    // apply the score correction of the cell:
                    String raceNameOrNumber = getSelectedString(raceNameOrNumberChoosers, raceColumn);
                    String officialSailID = getSelectedString(officialSailIDChoosers, competitor);
                    if (officialSailID != null && raceNameOrNumber != null) {
                        ScoreCorrectionEntryDTO officialCorrectionEntry = regattaScoreCorrection
                                .getScoreCorrectionsByRaceNameOrNumber().get(raceNameOrNumber).get(officialSailID);
                        // the competitor may not have an official result for this race, for example, if in case of split fleets
                        // the competitor raced in a different fleet
                        if (officialCorrectionEntry != null) {
                            result.addMaxPointsReasonUpdate(competitor, raceColumn,
                                    officialCorrectionEntry.getMaxPointsReason());
                            if (officialCorrectionEntry.getScore() != null) {
                                double officialTotalPointsWithFactorApplied = officialCorrectionEntry.getScore().doubleValue();
                                double officialTotalPoints = ScoringSchemeType.getUnscaledScore(raceColumn.getEffectiveFactor(), officialTotalPointsWithFactorApplied, raceColumn.isOneAlwaysStaysOne());
                                result.addScoreUpdate(competitor, raceColumn, officialTotalPoints);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        // Create a grid that hosts races on X and competitors on Y with the leaderboard ones as fixed entries and
        // the correction ones as selectable drop-downs. The grid cells show the current leaderboard entries
        // and the suggested correction plus a checkbox each. Each row and each column has a checkbox as well.
        // If a row/column checkbox is toggled, it sets all checkboxes in the row/column to the new state.
        // When OKing the dialog, for all ticked cells the corrections are applied.
        updateGridContents(grid);
        return grid;
    }

    private void updateGridContents(Grid grid) {
        grid.clear();
        grid.setWidget(0, 0, allAllCheckbox);
        int c = 1;
        for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
            VerticalPanel vp = new VerticalPanel();
            vp.add(new Label(raceColumn.getName()));
            vp.add(raceNameOrNumberChoosers.get(raceColumn));
            vp.add(raceColumnCheckboxes.get(raceColumn));
            grid.setWidget(0, c, vp);
            c++;
        }
        int row=1;
        for (CompetitorDTO competitor : leaderboard.competitors) {
            String officialSailID = getSelectedString(officialSailIDChoosers, competitor);
            int column = 0;
            VerticalPanel vp = new VerticalPanel();
            vp.add(new Label(getCompetitorIdentifyingText(competitor)+" "+competitor.getName()));
            vp.add(this.officialSailIDChoosers.get(competitor));
            vp.add(competitorCheckboxes.get(competitor));
            grid.setWidget(row, column++, vp);
            LeaderboardRowDTO leaderboardRow = leaderboard.rows.get(competitor);
            for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                LeaderboardEntryDTO entry = leaderboardRow.fieldsByRaceColumnName.get(raceColumn.getName());
                String raceNameOrNumber = getSelectedString(raceNameOrNumberChoosers, raceColumn);
                VerticalPanel cell = new VerticalPanel();
                cell.add(new Label(entry.totalPoints+"/"+entry.netPoints+"/"+entry.reasonForMaxPoints+
                        (entry.discarded?"/discarded":"")));
                if (officialSailID != null && raceNameOrNumber != null) {
                    ScoreCorrectionEntryDTO officialCorrectionEntry =
                        regattaScoreCorrection.getScoreCorrectionsByRaceNameOrNumber()
                        .get(raceNameOrNumber).get(officialSailID);
                    final Double officialTotalPointsWithFactorApplied = officialCorrectionEntry == null ? null :
                        officialCorrectionEntry.isDiscarded() ? Double.valueOf(0) : officialCorrectionEntry.getScore();
                    final Double officialTotalPoints = officialCorrectionEntry == null ? null :
                        officialCorrectionEntry.getScore() == null ? null :
                        ScoringSchemeType.getUnscaledScore(raceColumn.getEffectiveFactor(), officialCorrectionEntry.getScore(), raceColumn.isOneAlwaysStaysOne());
                    final MaxPointsReason officialMaxPointsReason = officialCorrectionEntry == null ? null :
                        officialCorrectionEntry.getMaxPointsReason();
                    SafeHtmlBuilder sb = new SafeHtmlBuilder();
                    // entries not considered "different' in case we haven't got an entry for the competitor/race at all;
                    // those non-existing "entries" will be ignored by getResults anyhow
                    boolean entriesDiffer = officialCorrectionEntry != null &&
                           (((officialTotalPoints == null && entry.totalPoints != null) || (officialTotalPoints != null && (entry.totalPoints == null || !Double.valueOf(entry.netPoints).equals(officialTotalPointsWithFactorApplied)))) ||
                            ((officialTotalPointsWithFactorApplied == null && entry.netPoints != null) || (officialTotalPointsWithFactorApplied != null && (entry.netPoints == null || !Double.valueOf(entry.netPoints).equals(officialTotalPointsWithFactorApplied)))) ||
                            ((officialMaxPointsReason == null && entry.reasonForMaxPoints != MaxPointsReason.NONE) ||
                                    officialMaxPointsReason != null && officialMaxPointsReason != entry.reasonForMaxPoints));
                    if (entriesDiffer) {
                        sb.appendHtmlConstant("<span style=\"color: #0000FF;\"><b>");
                    }
                    if (officialTotalPoints == null) {
                        sb.appendEscaped("null");
                    } else {
                        sb.append(officialTotalPoints);
                    }
                    sb.appendEscaped("/");
                    if (officialTotalPointsWithFactorApplied == null) {
                        sb.appendEscaped("null");
                    } else {
                        sb.append(officialTotalPointsWithFactorApplied);
                    }
                    sb.appendEscaped("/");
                    if (officialMaxPointsReason != null) {
                        sb.appendEscaped(officialMaxPointsReason.name());
                    } else {
                        sb.appendEscaped(MaxPointsReason.NONE.name());
                    }
                    if (officialCorrectionEntry != null && officialCorrectionEntry.isDiscarded()) {
                        sb.appendEscaped("/discarded");
                    }
                    if (entriesDiffer) {
                        sb.appendHtmlConstant("</b></span>");
                    }
                    cell.add(new HTML(sb.toSafeHtml()));
                }
                cell.add(cellCheckboxes.get(new Util.Pair<>(competitor, raceColumn)));
                grid.setWidget(row, column++, cell);
            }
            row++;
        }
    }

    /**
     * @return <code>null</code> if the empty string was selected
     */
    private <T> String getSelectedString(Map<T, ListBox> choosersByT, T t) {
        final ListBox chooser = choosersByT.get(t);
        return Util.hasLength(chooser.getSelectedValue()) ? chooser.getSelectedValue() : null;
    }

    private static class Validator implements DataEntryDialog.Validator<BulkScoreCorrectionDTO> {
        @Override
        public String getErrorMessage(BulkScoreCorrectionDTO valueToValidate) {
            // so far, nothing can go wrong :-)
            return null;
        }
    }

    private static class Callback implements DialogCallback<BulkScoreCorrectionDTO> {
        private final SailingServiceWriteAsync sailingServiceWrite;
        private final StringMessages stringMessages;
        private final ErrorReporter errorReporter;
        private final EditableLeaderboardPanel leaderboardPanel;
        
        public Callback(EditableLeaderboardPanel leaderboardPanel, SailingServiceWriteAsync sailingServiceWrite, StringMessages stringMessages, ErrorReporter errorReporter) {
            super();
            this.leaderboardPanel = leaderboardPanel;
            this.sailingServiceWrite = sailingServiceWrite;
            this.stringMessages = stringMessages;
            this.errorReporter = errorReporter;
        }

        @Override
        public void cancel() {
            // user has canceled the dialog
        }

        @Override
        public void ok(final BulkScoreCorrectionDTO result) {
            leaderboardPanel.addBusyTask();
            sailingServiceWrite.updateLeaderboardScoreCorrectionsAndMaxPointsReasons(result, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    leaderboardPanel.removeBusyTask();
                    errorReporter.reportError(stringMessages.errorUpdatingScoresForLeaderboard(result.getLeaderboardName(),
                            caught.getMessage()));
                }

                @Override
                public void onSuccess(Void result) {
                    leaderboardPanel.removeBusyTask();
                    Notification.notify(stringMessages.successfullyUpdatedScores(), NotificationType.INFO);
                    leaderboardPanel.loadCompleteLeaderboard(/* showProgress */ true); // reload leaderboard contents to reflect changes
                }
            });
        }
    }
}
