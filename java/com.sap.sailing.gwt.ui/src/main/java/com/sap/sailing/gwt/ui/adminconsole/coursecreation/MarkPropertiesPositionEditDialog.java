package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DoubleBox;

public class MarkPropertiesPositionEditDialog extends DataEntryDialog<Position> {

    private final DoubleBox latDoubleBox;
    private final DoubleBox lngDoubleBox;
    private final StringMessages stringMessages;

    private static final double ERROR_VAL = Double.MIN_VALUE;

    public MarkPropertiesPositionEditDialog(final StringMessages stringMessages, Position positionToEdit,
            DialogCallback<Position> callback) {
        super(stringMessages.edit() + " " + stringMessages.markProperties() + ": " + stringMessages.setPosition(), null,
                stringMessages.ok(), stringMessages.cancel(), new Validator<Position>() {
                    @Override
                    public String getErrorMessage(Position valueToValidate) {
                        if (valueToValidate.getLatDeg() == ERROR_VAL) {
                            return stringMessages.pleaseEnterA(stringMessages.latitude());
                        }
                        if (valueToValidate.getLngDeg() == ERROR_VAL) {
                            return stringMessages.pleaseEnterA(stringMessages.longitude());
                        }
                        return null;
                    }
                }, /* animationEnabled */true, callback);
        this.ensureDebugId("MarkPropertiesPositionEditDialog");
        this.stringMessages = stringMessages;
        this.latDoubleBox = createDoubleBox(10);
        this.lngDoubleBox = createDoubleBox(10);
    }

    @Override
    protected Position getResult() {
        return new DegreePosition(latDoubleBox.getValue() != null ? latDoubleBox.getValue() : ERROR_VAL,
                        lngDoubleBox.getValue() != null ? lngDoubleBox.getValue() : ERROR_VAL);
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(stringMessages.latitude() + " (" + stringMessages.degreesShort() + ")"));
        grid.setWidget(0, 1, latDoubleBox);
        grid.setWidget(1, 0, new Label(stringMessages.longitude() + " (" + stringMessages.degreesShort() + ")"));
        grid.setWidget(1, 1, lngDoubleBox);
        return grid;
    }
}
