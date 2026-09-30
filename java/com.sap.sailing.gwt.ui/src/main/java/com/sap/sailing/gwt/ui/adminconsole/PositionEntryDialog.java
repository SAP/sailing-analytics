package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Date;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.DataEntryDialogWithDateTimeBox;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.controls.datetime.DateAndTimeInput;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DoubleBox;

public class PositionEntryDialog extends DataEntryDialogWithDateTimeBox<Pair<Position, TimePoint>> {
    private final DoubleBox lat;
    private final DoubleBox lng;
    private final DateAndTimeInput timePointBox;
    private final StringMessages stringMessages;
    
    private static final double ERROR_VAL = Double.MIN_VALUE;

    public PositionEntryDialog(String title, final StringMessages stringMessages, DialogCallback<Pair<Position, TimePoint>> callback) {
        super(title, title, stringMessages.save(), stringMessages.cancel(),
                new DataEntryDialog.Validator<Pair<Position, TimePoint>>() {
                    @Override
                    public String getErrorMessage(Pair<Position, TimePoint> valueToValidate) {
                        if (valueToValidate.getA().getLatDeg() == ERROR_VAL) {
                            return stringMessages.pleaseEnterA(stringMessages.latitude());
                        }
                        if (valueToValidate.getA().getLatDeg() > 90 || valueToValidate.getA().getLatDeg() < -90) {
                            return stringMessages.pleaseEnterAValidValueFor(stringMessages.latitude(), "-90.0 - 90.0");
                        }
                        if (valueToValidate.getA().getLngDeg() == ERROR_VAL) {
                            return stringMessages.pleaseEnterA(stringMessages.longitude());
                        }
                        if (valueToValidate.getA().getLngDeg() > 180 || valueToValidate.getA().getLngDeg() < -180) {
                            return stringMessages.pleaseEnterAValidValueFor(stringMessages.latitude(), "-180.0 - 180.0");
                        }
                        return null;
                    }
        }, true, callback);
        this.stringMessages = stringMessages;
        lat = createDoubleBox(10);
        lng = createDoubleBox(10);
        timePointBox = createDateTimeBox(new Date(), Accuracy.SECONDS);
    }

    @Override
    protected Pair<Position, TimePoint> getResult() {
        Double latDeg = lat.getValue();
        Double lngDeg = lng.getValue();
        return new Pair<Position, TimePoint>(new DegreePosition(latDeg == null ? ERROR_VAL : latDeg, lngDeg == null ? ERROR_VAL : lngDeg),
                /* time point */ timePointBox.getValue() == null ? null : new MillisecondsTimePoint(timePointBox.getValue()));
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        Grid grid = new Grid(3, 2);
        grid.setWidget(0, 0, new Label(stringMessages.latitude() + " (" + stringMessages.degreesShort() + ")"));
        grid.setWidget(0, 1, lat);
        grid.setWidget(1, 0, new Label(stringMessages.longitude() + " (" + stringMessages.degreesShort() + ")"));
        grid.setWidget(1, 1, lng);
        grid.setWidget(2, 0, new Label(stringMessages.time()));
        grid.setWidget(2, 1, timePointBox);
        return grid;
    }

}
