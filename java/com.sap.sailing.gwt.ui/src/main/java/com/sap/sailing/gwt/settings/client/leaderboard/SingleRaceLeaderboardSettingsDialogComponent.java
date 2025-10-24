package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.List;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public class SingleRaceLeaderboardSettingsDialogComponent
        extends LeaderboardSettingsDialogComponent<SingleRaceLeaderboardSettings> {

    private CheckBox showRaceRankColumn;
    
    public SingleRaceLeaderboardSettingsDialogComponent(SingleRaceLeaderboardSettings initialSettings,
            StringMessages stringMessages, Iterable<DetailType> availableDetailTypes, PaywallResolver paywallResolver, AbstractLeaderboardDTO leaderboarDTO) {
        super(initialSettings, stringMessages, availableDetailTypes, /* canBoatInfoBeShown */ true, paywallResolver, leaderboarDTO);
    }

    @Override
    public SingleRaceLeaderboardSettings getResult() {
        List<DetailType> maneuverDetailsToShow = getSelected(maneuverDetailCheckboxes, initialSettings.getManeuverDetailsToShow());
        List<DetailType> overallDetailsToShow = getSelected(overallDetailCheckboxes, initialSettings.getOverallDetailsToShow());
        List<DetailType> raceDetailsToShow = getSelected(raceDetailCheckboxes, initialSettings.getRaceDetailsToShow());
        List<DetailType> legDetailsToShow = getSelected(legDetailCheckboxes, initialSettings.getLegDetailsToShow());
        Long delayBetweenAutoAdvancesValue = refreshIntervalInSecondsBox.getValue();
        final SingleRaceLeaderboardSettings newSettings = new SingleRaceLeaderboardSettings(maneuverDetailsToShow,
                legDetailsToShow, raceDetailsToShow, overallDetailsToShow,
                1000l * (delayBetweenAutoAdvancesValue == null ? 0l : delayBetweenAutoAdvancesValue.longValue()), 
                /* showAddedScores */ showAddedScoresCheckBox.getValue().booleanValue(),
                showCompetitorShortNameColumnCheckBox.getValue(), showCompetitorFullNameColumnCheckBox.getValue(),
                showCompetitorBoatInfoColumnCheckBox.getValue(), isCompetitorNationalityColumnVisible.getValue(),
                showRaceRankColumn.getValue(), new SecurityChildSettingsContext(leaderboardDTO, paywallResolver));
        return newSettings;
    }
    

    @Override
    public Widget getAdditionalWidget(DataEntryDialog<?> dialog) {
        FlowPanel dialogPanel = new FlowPanel();
        dialogPanel.ensureDebugId("LeaderboardSettingsPanel");
        dialogPanel.add(createOverallDetailPanel(dialog));
        dialogPanel.add(createRaceDetailPanel(dialog));
        dialogPanel.add(createRaceStartAnalysisPanel(dialog));
        dialogPanel.add(createLegDetailsPanel(dialog));
        dialogPanel.add(createManeuverDetailsPanel(dialog));
        dialogPanel.add(createTimingDetailsPanel(dialog));
        return dialogPanel;
    }
    
    @Override
    protected FlowPanel createOverallDetailPanel(DataEntryDialog<?> dialog) {
        final FlowPanel detailPanel = super.createOverallDetailPanel(dialog);
        showRaceRankColumn = createCheckbox(dialog, stringMessages.showRaceRankColumn(), initialSettings.isShowRaceRankColumn(), null);
        final FlowPanel overallDetailDialogContentThirdLine = new FlowPanel();
        overallDetailDialogContentThirdLine.addStyleName("dialogInnerContent");
        overallDetailDialogContentThirdLine.add(showRaceRankColumn);
        detailPanel.add(overallDetailDialogContentThirdLine);
        return detailPanel;
    }

    @Override
    public Validator<SingleRaceLeaderboardSettings> getValidator() {
        return new Validator<SingleRaceLeaderboardSettings>() {
            @Override
            public String getErrorMessage(SingleRaceLeaderboardSettings valueToValidate) {
                final String result;
                if (valueToValidate.getLegDetailsToShow().isEmpty()) {
                    result = stringMessages.selectAtLeastOneLegDetail();
                } else if (valueToValidate.getDelayBetweenAutoAdvancesInMilliseconds() < 1000) {
                    result = stringMessages.chooseUpdateIntervalOfAtLeastOneSecond();
                } else {
                    result = null;
                }
                return result;
            }
        };
    }
}
