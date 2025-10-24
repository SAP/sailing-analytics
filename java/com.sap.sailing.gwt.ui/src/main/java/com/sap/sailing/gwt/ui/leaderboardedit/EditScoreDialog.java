package com.sap.sailing.gwt.ui.leaderboardedit;

import java.util.Arrays;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboardedit.EditScoreDialog.ScoreCorrectionUpdate;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DoubleBox;

public class EditScoreDialog extends DataEntryDialog<ScoreCorrectionUpdate> {
    private final ListBox maxPointsBox;
    private final DoubleBox totalPointsBox;
    private final DoubleBox incrementalScoreOffsetInPointsBox;
    private final StringMessages stringMessages;
    
    public static class ScoreCorrectionUpdate {
        private final Double correctedScore;
        private final Double incrementalScoreCorrectionInPoints;
        private final MaxPointsReason maxPointsReason;
        
        public ScoreCorrectionUpdate(Double correctedScore, Double incrementalScoreCorrectionInPoints,
                MaxPointsReason maxPointsReason) {
            super();
            this.correctedScore = correctedScore;
            this.incrementalScoreCorrectionInPoints = incrementalScoreCorrectionInPoints;
            this.maxPointsReason = maxPointsReason;
        }
        public Double getCorrectedScore() {
            return correctedScore;
        }
        public Double getIncrementalScoreCorrectionInPoints() {
            return incrementalScoreCorrectionInPoints;
        }
        public MaxPointsReason getMaxPointsReason() {
            return maxPointsReason;
        }
    }
    
    public EditScoreDialog(StringMessages stringMessages, String competitorName, String raceColumnName,
            MaxPointsReason oldMaxPointsReason, Double oldTotalPoints, Double oldIncrementalScoreOffsetInPoints,
            DialogCallback<ScoreCorrectionUpdate> callback) {
        super(stringMessages.correctScore(), stringMessages.correctScoreFor(competitorName, raceColumnName),
                stringMessages.ok(), stringMessages.cancel(), /* validator */ null, /* animationEnabled */ true,
                callback);
        this.stringMessages = stringMessages;
        maxPointsBox = createListBox(/* isMultipleSelect */ false);
        maxPointsBox.addItem("");
        for (MaxPointsReason maxPointsReason : MaxPointsReason.getLexicographicalValues()) {
            maxPointsBox.addItem(maxPointsReason.name());
        }
        if (oldMaxPointsReason == null) {
            maxPointsBox.setSelectedIndex(0);
        } else {
            maxPointsBox.setSelectedIndex(
                    1 + Arrays.asList(MaxPointsReason.getLexicographicalValues()).indexOf(oldMaxPointsReason));
        }
        totalPointsBox = createDoubleBox(/* visibleLength */ 5);
        if (oldTotalPoints != null) {
            totalPointsBox.setValue(oldTotalPoints);
        }
        incrementalScoreOffsetInPointsBox = createDoubleBox(/* visibleLength */ 5);
        if (oldIncrementalScoreOffsetInPoints != null) {
            incrementalScoreOffsetInPointsBox.setValue(oldIncrementalScoreOffsetInPoints);
        }
    }

    @Override
    protected ScoreCorrectionUpdate getResult() {
        final MaxPointsReason maxPointsReason;
        if ("".equals(maxPointsBox.getItemText(maxPointsBox.getSelectedIndex()))) {
            maxPointsReason = null;
        } else {
            maxPointsReason = MaxPointsReason.valueOf(maxPointsBox.getItemText(maxPointsBox.getSelectedIndex()));
        }
        final Double totalScore = totalPointsBox.getValue();
        final Double incrementalScoreCorrectionInPoints = incrementalScoreOffsetInPointsBox.getValue();
        return new ScoreCorrectionUpdate(totalScore, incrementalScoreCorrectionInPoints, maxPointsReason);
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid grid = new Grid(3, 2);
        grid.setWidget(0, 0, new Label(stringMessages.penaltyOrRedress()));
        grid.setWidget(0, 1, maxPointsBox);
        grid.setWidget(1, 0, new Label(stringMessages.totalScore()));
        grid.setWidget(1, 1, totalPointsBox);
        grid.setWidget(2, 0, new Label(stringMessages.incrementalScoreCorrectionInPoints()));
        grid.setWidget(2, 1, incrementalScoreOffsetInPointsBox);
        return grid;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return totalPointsBox;
    }
    
    @Override
    public void show() {
        super.show();
        totalPointsBox.selectAll();
    }
}
