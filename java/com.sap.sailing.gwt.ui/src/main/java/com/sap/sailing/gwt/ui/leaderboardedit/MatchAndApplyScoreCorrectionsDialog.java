package com.sap.sailing.gwt.ui.leaderboardedit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.BulkScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO.ScoreCorrectionEntryDTO;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class MatchAndApplyScoreCorrectionsDialog extends DataEntryDialog<BulkScoreCorrectionDTO> {
    private static final RegExp sailIdPattern = RegExp.compile("^([A-Z][A-Z][A-Z])\\s*[^0-9]*([0-9]*)$");
    
    private final LeaderboardDTO leaderboard;
    private final Map<CompetitorDTO, String> defaultOfficialSailIDsForCompetitors;
    private final Set<String> allOfficialSailIDs;
    private final Map<RaceColumnDTO, String> raceColumnToOfficialRaceNameOrNumber;
    private final RegattaScoreCorrectionDTO regattaScoreCorrection;
    private final Map<CompetitorDTO, CheckBox> competitorCheckboxes;
    private final Map<RaceColumnDTO, CheckBox> raceColumnCheckboxes;
    private final Map<Pair<CompetitorDTO, RaceColumnDTO>, CheckBox> cellCheckboxes;
    private final Grid grid;
    private final Map<RaceColumnDTO, ListBox> raceNameOrNumberChoosers;
    private final Map<CompetitorDTO, ListBox> officialSailIDChoosers;
    private final CheckBox allAllCheckbox;

    public MatchAndApplyScoreCorrectionsDialog(EditableLeaderboardPanel leaderboardPanel, StringMessages stringMessages,
            SailingServiceAsync sailingService, ErrorReporter errorReporter, RegattaScoreCorrectionDTO result) {
        super(stringMessages.assignRaceNumbersToRaceColumns(), stringMessages.assignRaceNumbersToRaceColumns(),
                stringMessages.ok(), stringMessages.cancel(), new Validator(), new Callback(leaderboardPanel,
                        sailingService, stringMessages, errorReporter));
        this.regattaScoreCorrection = result;
        this.leaderboard = leaderboardPanel.getLeaderboard();
        this.allOfficialSailIDs = new LinkedHashSet<String>();
        this.defaultOfficialSailIDsForCompetitors = new HashMap<CompetitorDTO, String>();
        mapCompetitorsAndInitializeAllOfficialRaceIDs(leaderboard, result);
        this.raceColumnToOfficialRaceNameOrNumber = createRaceColumnNameToOfficialRaceNameOrNumberSuggestion(leaderboard, result);
        competitorCheckboxes = new HashMap<CompetitorDTO, CheckBox>();
        for (final CompetitorDTO competitor : leaderboard.competitors) {
            CheckBox checkbox = createCheckbox(stringMessages.selectAll());
            checkbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    for (RaceColumnDTO raceColumn : MatchAndApplyScoreCorrectionsDialog.this.leaderboard.getRaceList()) {
                        cellCheckboxes.get(new Pair<CompetitorDTO, RaceColumnDTO>(competitor, raceColumn)).setValue(event.getValue());
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
                        cellCheckboxes.get(new Pair<CompetitorDTO, RaceColumnDTO>(competitor, raceColumn)).setValue(event.getValue());
                    }
                }
            });
            raceColumnCheckboxes.put(raceColumn, checkbox);
        }
        cellCheckboxes = new HashMap<Pair<CompetitorDTO, RaceColumnDTO>, CheckBox>();
        for (final CompetitorDTO competitor : leaderboard.competitors) {
            for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                cellCheckboxes.put(new Pair<CompetitorDTO, RaceColumnDTO>(competitor, raceColumn), createCheckbox(stringMessages.apply()));
            }
        }
        allAllCheckbox = createCheckbox(stringMessages.selectAll());
        allAllCheckbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                for (CompetitorDTO competitor : leaderboard.competitors) {
                    competitorCheckboxes.get(competitor).setValue(event.getValue());
                    for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                        cellCheckboxes.get(new Pair<CompetitorDTO, RaceColumnDTO>(competitor, raceColumn)).setValue(event.getValue());
                    }
                }
                for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                    raceColumnCheckboxes.get(raceColumn).setValue(event.getValue());
                }
            }
        });
        raceNameOrNumberChoosers = new HashMap<RaceColumnDTO, ListBox>();
        officialSailIDChoosers = new HashMap<CompetitorDTO, ListBox>();
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
            result.addItem(entry);
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

    /**
     * Maps the sail IDs contained in <code>result</code> to the {@link CompetitorDTO}s contained in <code>leaderboard</code>.
     * The match making ignores all whitespaces in the sail IDs on both sides. If the {@link CompetitorDTO#sailID} does not start
     * with a letter it is assumed the country code is missing. In this case, the {@link CompetitorDTO#threeLetterIocCountryCode} is
     * prepended before comparing to <code>result</code>'s sail IDs. The sail ID number is extracted by trimming and using all
     * trailing digits.
     * 
     * @return a map mapping the sailIDs as found in <code>result</code> to the {@link CompetitorDTO}s used in <code>leaderboard</code>;
     * values may be <code>null</code> if no competitor was found for the sail ID in the leaderboard
     */
    private void mapCompetitorsAndInitializeAllOfficialRaceIDs(LeaderboardDTO leaderboard,
            RegattaScoreCorrectionDTO regattaScoreCorrection) {
        Map<String, CompetitorDTO> canonicalizedLeaderboardSailIDToCompetitors = canonicalizeLeaderboardSailIDs(leaderboard);
        List<String> allOfficialSailIDsAsSortableList = new ArrayList<String>();
        for (Map<String, ScoreCorrectionEntryDTO> scoreCorrectionsBySailID : regattaScoreCorrection.getScoreCorrectionsByRaceNameOrNumber().values()) {
            for (String officialSailID : scoreCorrectionsBySailID.keySet()) {
                allOfficialSailIDsAsSortableList.add(officialSailID);
                String canonicalizedResultSailID = canonicalizeSailID(officialSailID, /* defaultNationality */ null);
                CompetitorDTO competitor = canonicalizedLeaderboardSailIDToCompetitors.get(canonicalizedResultSailID);
                defaultOfficialSailIDsForCompetitors.put(competitor, officialSailID);
            }
        }
        Collections.sort(allOfficialSailIDsAsSortableList);
        allOfficialSailIDs.addAll(allOfficialSailIDsAsSortableList);
    }
    
    /**
     * Try to match three-letter country code and number, optionally separated by whitespaces. If there is no match,
     * use the first 20 characters of the sailID.
     */
    private String canonicalizeSailID(String sailID, String defaultNationality) {
        String result = null;
        MatchResult m = sailIdPattern.exec(sailID.trim());
        if (sailIdPattern.test(sailID.trim())) {
            String iocCode = m.getGroup(1);
            if (iocCode != null) {
                iocCode = iocCode.toUpperCase();
            }
            if (defaultNationality != null && (iocCode == null || iocCode.trim().length() == 0)) {
                iocCode = defaultNationality.toUpperCase();
            }
            if (iocCode != null && iocCode.trim().length() > 0) {
                String number = m.getGroup(2);
                result = iocCode + number;
            }
        }
        if (result == null) {
            result = sailID.substring(0, Math.min(20, sailID.length()));
        }
        return result;
    }

    private Map<String, CompetitorDTO> canonicalizeLeaderboardSailIDs(LeaderboardDTO leaderboard) {
        Map<String, CompetitorDTO> result = new HashMap<String, CompetitorDTO>();
        for (CompetitorDTO competitor : leaderboard.competitors) {
            String canonicalizedSailID = canonicalizeSailID(competitor.getSailID().trim(), competitor.getThreeLetterIocCountryCode().trim());
            if (canonicalizedSailID != null) {
                result.put(canonicalizedSailID, competitor);
            }
        }
        return result;
    }

    @Override
    protected BulkScoreCorrectionDTO getResult() {
        BulkScoreCorrectionDTO result = new BulkScoreCorrectionDTO(leaderboard.name);
        for (CompetitorDTO competitor : leaderboard.competitors) {
            for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                Pair<CompetitorDTO, RaceColumnDTO> key = new Pair<CompetitorDTO, RaceColumnDTO>(competitor, raceColumn);
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
                                double officialTotalPoints = officialCorrectionEntry.getScore().doubleValue();
                                double officialNetPoints = officialTotalPoints / raceColumn.getEffectiveFactor();
                                result.addScoreUpdate(competitor, raceColumn, officialNetPoints);
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
            vp.add(new Label(competitor.getSailID()+" "+competitor.getName()));
            vp.add(this.officialSailIDChoosers.get(competitor));
            vp.add(competitorCheckboxes.get(competitor));
            grid.setWidget(row, column++, vp);
            LeaderboardRowDTO leaderboardRow = leaderboard.rows.get(competitor);
            for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                LeaderboardEntryDTO entry = leaderboardRow.fieldsByRaceColumnName.get(raceColumn.getName());
                String raceNameOrNumber = getSelectedString(raceNameOrNumberChoosers, raceColumn);
                VerticalPanel cell = new VerticalPanel();
                cell.add(new Label(entry.netPoints+"/"+entry.totalPoints+"/"+entry.reasonForMaxPoints+
                        (entry.discarded?"/discarded":"")));
                if (officialSailID != null && raceNameOrNumber != null) {
                    ScoreCorrectionEntryDTO officialCorrectionEntry =
                        regattaScoreCorrection.getScoreCorrectionsByRaceNameOrNumber()
                        .get(raceNameOrNumber).get(officialSailID);
                    final Double officialTotalPoints = officialCorrectionEntry == null ? null :
                        officialCorrectionEntry.isDiscarded() ? new Double(0) : officialCorrectionEntry.getScore();
                    final Double officialNetPoints = officialCorrectionEntry == null ? null :
                        officialCorrectionEntry.getScore() == null ? null :
                        officialCorrectionEntry.getScore() / raceColumn.getEffectiveFactor();
                    final MaxPointsReason officialMaxPointsReason = officialCorrectionEntry == null ? null :
                        officialCorrectionEntry.getMaxPointsReason();
                    SafeHtmlBuilder sb = new SafeHtmlBuilder();
                    // entries not considered "different' in case we haven't got an entry for the competitor/race at all;
                    // those non-existing "entries" will be ignored by getResults anyhow
                    boolean entriesDiffer = officialCorrectionEntry != null &&
                           (((officialNetPoints == null && entry.netPoints != null) || (officialNetPoints != null && (entry.netPoints == null || !new Double(entry.netPoints).equals(officialNetPoints)))) ||
                            ((officialTotalPoints == null && entry.totalPoints != null) || (officialTotalPoints != null && (entry.totalPoints == null || !new Double(entry.totalPoints).equals(officialTotalPoints)))) ||
                            ((officialMaxPointsReason == null && entry.reasonForMaxPoints != MaxPointsReason.NONE) ||
                                    officialMaxPointsReason != null && officialMaxPointsReason != entry.reasonForMaxPoints));
                    if (entriesDiffer) {
                        sb.appendHtmlConstant("<span style=\"color: #0000FF;\"><b>");
                    }
                    if (officialNetPoints == null) {
                        sb.appendEscaped("null");
                    } else {
                        sb.append(officialNetPoints);
                    }
                    sb.appendEscaped("/");
                    if (officialTotalPoints == null) {
                        sb.appendEscaped("null");
                    } else {
                        sb.append(officialTotalPoints);
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
                cell.add(cellCheckboxes.get(new Pair<CompetitorDTO, RaceColumnDTO>(competitor, raceColumn)));
                grid.setWidget(row, column++, cell);
            }
            row++;
        }
    }

    /**
     * @return <code>null</code> if the empty string was selected
     */
    private <T> String getSelectedString(Map<T, ListBox> choosersByT, T t) {
        String result = null;
        ListBox chooser = choosersByT.get(t);
        int selectedIndex = chooser.getSelectedIndex();
        if (selectedIndex != -1) {
            result = chooser.getItemText(selectedIndex);
            if (result.length() == 0) {
                result = null;
            }
        }
        return result;
    }

    private static class Validator implements DataEntryDialog.Validator<BulkScoreCorrectionDTO> {
        @Override
        public String getErrorMessage(BulkScoreCorrectionDTO valueToValidate) {
            // so far, nothing can go wrong :-)
            return null;
        }
    }

    private static class Callback implements DialogCallback<BulkScoreCorrectionDTO> {
        private final SailingServiceAsync sailingService;
        private final StringMessages stringMessages;
        private final ErrorReporter errorReporter;
        private final EditableLeaderboardPanel leaderboardPanel;
        
        public Callback(EditableLeaderboardPanel leaderboardPanel, SailingServiceAsync sailingService, StringMessages stringMessages, ErrorReporter errorReporter) {
            super();
            this.leaderboardPanel = leaderboardPanel;
            this.sailingService = sailingService;
            this.stringMessages = stringMessages;
            this.errorReporter = errorReporter;
        }

        @Override
        public void cancel() {
            // user has canceled the dialog
        }

        @Override
        public void ok(final BulkScoreCorrectionDTO result) {
            leaderboardPanel.getBusyIndicator().setBusy(true);
            sailingService.updateLeaderboardScoreCorrectionsAndMaxPointsReasons(result, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    leaderboardPanel.getBusyIndicator().setBusy(false);
                    errorReporter.reportError(stringMessages.errorUpdatingScoresForLeaderboard(result.getLeaderboardName(),
                            caught.getMessage()));
                }

                @Override
                public void onSuccess(Void result) {
                    Window.setStatus(stringMessages.successfullyUpdatedScores());
                    leaderboardPanel.timeChanged(/* time point is ignored */ null, null); // reload leaderboard contents to reflect changes
                    // leaderboard panel sets busy indicator to non-busy after done with updating
                }
            });
        }
    }
}
