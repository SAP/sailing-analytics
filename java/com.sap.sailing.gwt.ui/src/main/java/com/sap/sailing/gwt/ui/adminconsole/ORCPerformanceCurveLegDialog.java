package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.gwt.ui.adminconsole.CourseManagementWidget.LegGeometrySupplier;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.dialog.DoubleBox;

/**
 * An editor allowing users to specify the ORC Polar Curve leg type and distance, and in case of a
 * {@link ORCPerformanceCurveLegTypes#TWA}-type leg the angle between the leg's rhumb line and the true wind
 * direction ("leg TWA"). The result is a {@link ORCPerformanceCurveLegImpl} object.<p>
 * 
 * If the user chooses the "empty type" then {@code null} will be the result. The caller should then make an
 * effort to revoke any previously recorded leg information so that again a tracking-based leg adapter will
 * effectively be used.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ORCPerformanceCurveLegDialog extends AbstractORCPerformanceCurveLegDialog<ORCPerformanceCurveLegImpl> {
    private final Button fetchTrackingBasedDistanceAndTwaButton;
    private final ListBox legTypeBox;
    private final DoubleBox distanceInNauticalMilesBox;
    private final DoubleBox twdBox;
    private final DoubleBox legDirectionBox;
    private final DoubleBox twaBox;
    private final StringMessages stringMessages;
    private final ListDataProvider<WaypointDTO> waypointList;
    private final WaypointDTO forLegEndingAt;
    private final LegGeometrySupplier legGeometrySupplier;
    
    public ORCPerformanceCurveLegDialog(StringMessages stringMessages, WaypointDTO forLegEndingAt,
            ListDataProvider<WaypointDTO> waypointList, ORCPerformanceCurveLegImpl orcLegParametersSoFar,
            LegGeometrySupplier legGeometrySupplier,
            Validator<ORCPerformanceCurveLegImpl> validator, DialogCallback<ORCPerformanceCurveLegImpl> callback) {
        super(stringMessages.orcPerformanceCurveLeg(), stringMessages
                .orcPerformanceCurveLegName(waypointList.getList().indexOf(forLegEndingAt), forLegEndingAt.getName()),
                stringMessages, validator, callback);
        this.stringMessages = stringMessages;
        this.forLegEndingAt = forLegEndingAt;
        this.waypointList = waypointList;
        this.legGeometrySupplier = legGeometrySupplier;
        fetchTrackingBasedDistanceAndTwaButton = new Button(stringMessages.orcPerformanceCurveFetchTrackedLegGeometry());
        fetchTrackingBasedDistanceAndTwaButton.addClickHandler(e->fetchTrackingBasedDistanceAndTwa());
        legTypeBox = createLegTypeBox(orcLegParametersSoFar);
        distanceInNauticalMilesBox = createDoubleBox(/* visibleLength */ 5);
        twdBox = createDoubleBox(/* visibleLength */ 5);
        legDirectionBox = createDoubleBox(/* visibleLength */ 5);
        twaBox = createDoubleBox(/* visibleLength */ 5);
        twdBox.addValueChangeHandler(e->updateTwaBoxFromTwdAndLegDirection(twdBox, legDirectionBox, twaBox));
        legDirectionBox.addValueChangeHandler(e->updateTwaBoxFromTwdAndLegDirection(twdBox, legDirectionBox, twaBox));
        if (orcLegParametersSoFar != null) {
            distanceInNauticalMilesBox.setValue(orcLegParametersSoFar.getLength().getNauticalMiles());
            if (orcLegParametersSoFar.getTwa() != null) {
                twaBox.setValue(orcLegParametersSoFar.getTwa().getDegrees());
            }
        }
        legTypeBox.addChangeHandler(e->updateFetchTrackingBasedDistanceAndTwaButtonEnabledState());
        updateFetchTrackingBasedDistanceAndTwaButtonEnabledState();
    }
    
    private void fetchTrackingBasedDistanceAndTwa() {
        final int zeroBasedLegIndex = waypointList.getList().indexOf(forLegEndingAt)-1;
        legGeometrySupplier.getLegGeometry(new int[] { zeroBasedLegIndex }, new ORCPerformanceCurveLegTypes[] { getSelectedLegType() },
                new AsyncCallback<ORCPerformanceCurveLegImpl[]>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log(caught.getMessage());
                        Notification.notify(caught.getMessage(), NotificationType.WARNING);
                    }

                    @Override
                    public void onSuccess(ORCPerformanceCurveLegImpl[] legGeometries) {
                        twaBox.setValue(legGeometries == null ? null : legGeometries[0].getTwa().getDegrees());
                        distanceInNauticalMilesBox.setValue(legGeometries == null ? null : legGeometries[0].getLength().getNauticalMiles());
                        validateAndUpdate();
                    }
        });
    }

    private ORCPerformanceCurveLegTypes getSelectedLegType() {
        final String selectedValue = legTypeBox.getSelectedValue();
        return selectedValue == null || selectedValue.equals("null") ? null : ORCPerformanceCurveLegTypes.valueOf(selectedValue);
    }
    
    private void updateFetchTrackingBasedDistanceAndTwaButtonEnabledState() {
        final boolean twaIsSelected = getSelectedLegType() == ORCPerformanceCurveLegTypes.TWA;
        twaBox.setEnabled(twaIsSelected);
    }

    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(6, 2);
        int row = 0;
        result.setWidget(row, 0, new Label(stringMessages.legType()));
        result.setWidget(row++, 1, legTypeBox);
        result.setWidget(row, 0, new Label(stringMessages.distanceInNauticalMiles()));
        result.setWidget(row++, 1, distanceInNauticalMilesBox);
        result.setWidget(row, 0, new Label(stringMessages.twdInDegrees()));
        result.setWidget(row++, 1, twdBox);
        result.setWidget(row, 0, new Label(stringMessages.legDirectionInDegrees()));
        result.setWidget(row++, 1, legDirectionBox);
        result.setWidget(row, 0, new Label(stringMessages.legTwaInDegrees()));
        result.setWidget(row++, 1, twaBox);
        result.setWidget(row, 0, fetchTrackingBasedDistanceAndTwaButton);
        return result;
    }

    @Override
    public FocusWidget getInitialFocusWidget() {
        return legTypeBox;
    }

    @Override
    protected ORCPerformanceCurveLegImpl getResult() {
        final ORCPerformanceCurveLegImpl result;
        final ORCPerformanceCurveLegTypes selectedLegType = getSelectedLegType();
        if (selectedLegType == null) {
            result = null;
        } else if (selectedLegType == ORCPerformanceCurveLegTypes.TWA) {
            result = new ORCPerformanceCurveLegImpl(
                    distanceInNauticalMilesBox.getValue() == null ? null
                            : new NauticalMileDistance(distanceInNauticalMilesBox.getValue()),
                    twaBox.getValue() == null ? null : new DegreeBearingImpl(twaBox.getValue()));
        } else {
            result = new ORCPerformanceCurveLegImpl(distanceInNauticalMilesBox.getValue() == null ? null
                    : new NauticalMileDistance(distanceInNauticalMilesBox.getValue()), selectedLegType);
        }
        return result;
    }
}
