package com.sap.sailing.gwt.ui.leaderboardedit;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DoubleBox;

public class EditCarryValueDialog extends DataEntryDialog<Double> {
    private final DoubleBox carriedPointsBox;
    private final StringMessages stringMessages;
    
    public EditCarryValueDialog(StringMessages stringMessages, String competitorName, Double oldCarriedPoints, DialogCallback<Double> callback) {
        super(stringMessages.carry(), stringMessages.enterCarryValueFor(competitorName),
                stringMessages.ok(), stringMessages.cancel(), /* validator */ null, /* animationEnabled */ true,
                callback);
        this.stringMessages = stringMessages;
        carriedPointsBox = createDoubleBox(/* visibleLength */ 5);
        if (oldCarriedPoints != null) {
            carriedPointsBox.setValue(oldCarriedPoints);
        }
    }

    @Override
    protected Double getResult() {
        return carriedPointsBox.getValue();
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid grid = new Grid(1, 2);
        grid.setWidget(0, 0, new Label(stringMessages.carry()));
        grid.setWidget(0, 1, carriedPointsBox);
        return grid;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return carriedPointsBox;
    }

    @Override
    public void show() {
        super.show();
        carriedPointsBox.selectAll();
    }
}
