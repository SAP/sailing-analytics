package com.sap.sailing.gwt.ui.adminconsole;

import java.util.stream.IntStream;

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
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.dialog.DoubleBox;

/**
 * An editor allowing users to specify the ORC Polar Curve leg type and distance for all legs currently in the
 * course. See also {@link ORCPerformanceCurveLegDialog} which does this for a single leg only.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ORCPerformanceCurveAllLegsDialog extends AbstractORCPerformanceCurveLegDialog<ORCPerformanceCurveLegImpl[]> {
    private final ListBox commonLegTypeBox;
    private final DoubleBox desiredTotalCourseDistanceInNauticalMilesBox;
    private final ListBox[] legTypeBoxes;
    private final Label totalDistanceInNauticalMilesLabel;
    private final DoubleBox[] distanceInNauticalMilesBoxes;
    private final DoubleBox[] twdBoxes;
    private final DoubleBox[] legDirectionBoxes;
    private final DoubleBox[] twaBoxes;
    private final StringMessages stringMessages;
    private final ListDataProvider<WaypointDTO> waypointList;
    private final LegGeometrySupplier legGeometrySupplier;
    private ORCPerformanceCurveLegImpl[] trackingBasedCourseGeometry;
    private final Label[] trackedTwaInDegreesLabels;
    private final Label[] trackedDistanceInNauticalMilesLabels;
    final Button spreadTotalDistanceProportionallyAcrossLegsButton;
    
    /**
     * @param orcLegParametersSoFar
     *            element at index 0 has the parameters for the first leg, so the one ending at the waypoint with
     *            zero-based index 1 and one-based index 2, respectively.
     */
    public ORCPerformanceCurveAllLegsDialog(StringMessages stringMessages, ListDataProvider<WaypointDTO> waypointList,
            ORCPerformanceCurveLegImpl[] orcLegParametersSoFar, LegGeometrySupplier legGeometrySupplier,
            Validator<ORCPerformanceCurveLegImpl[]> validator,
            DialogCallback<ORCPerformanceCurveLegImpl[]> callback) {
        super(stringMessages.orcPerformanceCurveLegs(), stringMessages.orcPerformanceCurveLegs(), stringMessages, validator, callback);
        this.stringMessages = stringMessages;
        this.waypointList = waypointList;
        this.legGeometrySupplier = legGeometrySupplier;
        commonLegTypeBox = createLegTypeBox(null);
        desiredTotalCourseDistanceInNauticalMilesBox = createDoubleBox(/* visibleLength */ 5);
        totalDistanceInNauticalMilesLabel = new Label();
        legTypeBoxes = new ListBox[waypointList.getList().size()-1];
        twdBoxes = new DoubleBox[waypointList.getList().size()-1];
        legDirectionBoxes = new DoubleBox[waypointList.getList().size()-1];
        twaBoxes = new DoubleBox[waypointList.getList().size()-1];
        distanceInNauticalMilesBoxes = new DoubleBox[waypointList.getList().size()-1];
        trackedTwaInDegreesLabels = new Label[waypointList.getList().size()-1];
        trackedDistanceInNauticalMilesLabels = new Label[waypointList.getList().size()-1];
        for (int i=0; i<waypointList.getList().size()-1; i++) {
            final int zeroBasedLegNumber = i;
            legTypeBoxes[i] = createLegTypeBox(orcLegParametersSoFar[i]);
            distanceInNauticalMilesBoxes[i] = createDoubleBox(/* visibleLength */ 5);
            distanceInNauticalMilesBoxes[i].addChangeHandler(e->updateTotalDistanceLabel());
            twdBoxes[i] = createDoubleBox(/* visibleLength */ 5);
            legDirectionBoxes[i] = createDoubleBox(/* visibleLength */ 5);
            twaBoxes[i] = createDoubleBox(/* visibleLength */ 5);
            twdBoxes[i].addValueChangeHandler(e->updateTwaBoxFromTwdAndLegDirection(twdBoxes[zeroBasedLegNumber], legDirectionBoxes[zeroBasedLegNumber], twaBoxes[zeroBasedLegNumber]));
            legDirectionBoxes[i].addValueChangeHandler(e->updateTwaBoxFromTwdAndLegDirection(twdBoxes[zeroBasedLegNumber], legDirectionBoxes[zeroBasedLegNumber], twaBoxes[zeroBasedLegNumber]));
            if (orcLegParametersSoFar != null && orcLegParametersSoFar[i] != null) {
                distanceInNauticalMilesBoxes[i].setValue(orcLegParametersSoFar[i].getLength().getNauticalMiles());
                twaBoxes[i].setValue(orcLegParametersSoFar[i].getTwa() == null ? null : orcLegParametersSoFar[i].getTwa().getDegrees());
            }
            trackedTwaInDegreesLabels[i] = new Label();
            trackedDistanceInNauticalMilesLabels[i] = new Label();
            legTypeBoxes[i].addChangeHandler(e->updateAfterLegTypeChange(zeroBasedLegNumber));
            updateAfterLegTypeChange(zeroBasedLegNumber);
        }
        spreadTotalDistanceProportionallyAcrossLegsButton = new Button(stringMessages.spreadTotalDistanceProportionallyAcrossLegs());
        desiredTotalCourseDistanceInNauticalMilesBox.addChangeHandler(e->adjustEnablednessOfSpreadButton());
        spreadTotalDistanceProportionallyAcrossLegsButton.addClickHandler(e->spreadTotalDistance());
        adjustEnablednessOfSpreadButton();
        commonLegTypeBox.addChangeHandler(e->{
            int zeroBasedLegNumber = 0;
            for (final ListBox legTypeBox : legTypeBoxes) {
                legTypeBox.setSelectedIndex(commonLegTypeBox.getSelectedIndex());
                updateAfterLegTypeChange(zeroBasedLegNumber++);
            }
            validateAndUpdate();
        });
        fetchTrackingBasedDistanceAndTwa();
    }
    
    private void adjustEnablednessOfSpreadButton() {
        spreadTotalDistanceProportionallyAcrossLegsButton.setEnabled(desiredTotalCourseDistanceInNauticalMilesBox.getValue() != null);
    }

    private void updateAfterLegTypeChange(final int zeroBasedLegNumber) {
        twaBoxes[zeroBasedLegNumber].setEnabled(getSelectedLegType(zeroBasedLegNumber)==ORCPerformanceCurveLegTypes.TWA);
        distanceInNauticalMilesBoxes[zeroBasedLegNumber].setEnabled(getSelectedLegType(zeroBasedLegNumber)!=null);
    }
    
    private void updateTotalDistanceLabel() {
        final Distance totalDistance = getEffectiveTotalDistance(getResult());
        totalDistanceInNauticalMilesLabel.setText(Util.padPositiveValue(totalDistance.getNauticalMiles(), 1, 3, /* round */ true));
    }
    
    @Override
    protected void validateAndUpdate() {
        updateTotalDistanceLabel();
        super.validateAndUpdate();
    }
    
    private Distance getEffectiveTotalDistance(ORCPerformanceCurveLegImpl[] explicitLegSpecs) {
        Distance totalDistance = Distance.NULL;
        for (int i=0; i<explicitLegSpecs.length; i++) {
            final Distance legDistance = getEffectiveLegDistance(explicitLegSpecs, i);
            totalDistance = totalDistance.add(legDistance);
        }
        return totalDistance;
    }

    protected Distance getEffectiveLegDistance(final ORCPerformanceCurveLegImpl[] explicitLegSpecs, int zeroBasedLegIndex) {
        final Distance legDistance;
        if (explicitLegSpecs[zeroBasedLegIndex] != null && explicitLegSpecs[zeroBasedLegIndex].getLength() != null) {
            legDistance = explicitLegSpecs[zeroBasedLegIndex].getLength();
        } else if (trackingBasedCourseGeometry != null && trackingBasedCourseGeometry.length > zeroBasedLegIndex && trackingBasedCourseGeometry[zeroBasedLegIndex] != null) {
            legDistance = trackingBasedCourseGeometry[zeroBasedLegIndex].getLength();
        } else {
            legDistance = Distance.NULL;
        }
        return legDistance;
    }
    
    private void fetchTrackingBasedDistanceAndTwa() {
        legGeometrySupplier.getLegGeometry(IntStream.range(0,  waypointList.getList().size()-1).toArray(), getSelectedLegTypes(),
                new AsyncCallback<ORCPerformanceCurveLegImpl[]>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log(caught.getMessage());
                        Notification.notify(caught.getMessage(), NotificationType.WARNING);
                    }

                    @Override
                    public void onSuccess(ORCPerformanceCurveLegImpl[] legGeometries) {
                        trackingBasedCourseGeometry = legGeometries;
                        for (int i=0; i<legGeometries.length; i++) {
                            if (legGeometries[i] != null && legGeometries[i].getTwa() != null) {
                                final double twaInDegrees = legGeometries[i].getTwa().getDegrees();
                                trackedTwaInDegreesLabels[i].setText((twaInDegrees<0?"-":"")+Util.padPositiveValue(Math.abs(twaInDegrees), 1, 3, /* round */ true));
                            } else {
                                trackedTwaInDegreesLabels[i].setText("");
                            }
                            if (legGeometries[i] != null && legGeometries[i].getLength() != null) {
                                trackedDistanceInNauticalMilesLabels[i].setText(Util.padPositiveValue(legGeometries[i].getLength().getNauticalMiles(), 1, 3, /* round */ true));
                            } else {
                                trackedDistanceInNauticalMilesLabels[i].setText("");
                            }
                        }
                        validateAndUpdate();
                    }
        });
    }

    private ORCPerformanceCurveLegTypes getSelectedLegType(int zeroBasedLegNumber) {
        final String selectedValue = legTypeBoxes[zeroBasedLegNumber].getSelectedValue();
        return selectedValue == null || selectedValue.equals("null") ? null : ORCPerformanceCurveLegTypes.valueOf(selectedValue);
    }
    
    private ORCPerformanceCurveLegTypes[] getSelectedLegTypes() {
        final ORCPerformanceCurveLegTypes[] result = new ORCPerformanceCurveLegTypes[legTypeBoxes.length];
        for (int i=0; i<legTypeBoxes.length; i++) {
            result[i] = getSelectedLegType(i);
        }
        return result;
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(waypointList.getList().size()+1, 9);
        int row = 0;
        result.setWidget(row, 0, new Label(stringMessages.setAllLegsToType()));
        result.setWidget(row, 1, commonLegTypeBox);
        result.setWidget(row, 2, new Label(stringMessages.totalDistance()));
        result.setWidget(row, 3, totalDistanceInNauticalMilesLabel);
        result.setWidget(row, 4, new Label(stringMessages.desiredTotalDistanceInNauticalMiles()));
        result.setWidget(row, 5, desiredTotalCourseDistanceInNauticalMilesBox);
        result.setWidget(row, 6, spreadTotalDistanceProportionallyAcrossLegsButton);
        row++;
        // column headers:
        result.setWidget(row, 1, new Label(stringMessages.legType()));
        result.setWidget(row, 2, new Label(stringMessages.distanceInNauticalMiles()));
        result.setWidget(row, 3, new Label(stringMessages.twdInDegrees()));
        result.setWidget(row, 4, new Label(stringMessages.legDirectionInDegrees()));
        result.setWidget(row, 5, new Label(stringMessages.legTwaInDegrees()));
        result.setWidget(row, 6, new Label(stringMessages.trackedDistanceInNauticalMiles()));
        result.setWidget(row, 7, new Label(stringMessages.trackedTwaInDegrees()));
        row++;
        for (int i=0; i<waypointList.getList().size()-1; i++) {
            final int index = i;
            result.setWidget(row, 0, new Label(waypointList.getList().get(i).getName()+" - "+waypointList.getList().get(i+1).getName()));
            result.setWidget(row, 1, legTypeBoxes[i]);
            result.setWidget(row, 2, distanceInNauticalMilesBoxes[i]);
            result.setWidget(row, 3, twdBoxes[i]);
            result.setWidget(row, 4, legDirectionBoxes[i]);
            result.setWidget(row, 5, twaBoxes[i]);
            result.setWidget(row, 6, trackedDistanceInNauticalMilesLabels[i]);
            result.setWidget(row, 7, trackedTwaInDegreesLabels[i]);
            final Button useTrackedDataButton = new Button(stringMessages.useTrackedData());
            useTrackedDataButton.addClickHandler(e->{
                twaBoxes[index].setValue(
                        trackingBasedCourseGeometry.length <= index || trackingBasedCourseGeometry[index] == null ? null
                                : trackingBasedCourseGeometry[index].getTwa().getDegrees());
                distanceInNauticalMilesBoxes[index].setValue(
                        trackingBasedCourseGeometry.length <= index || trackingBasedCourseGeometry[index] == null ? null
                                : trackingBasedCourseGeometry[index].getLength().getNauticalMiles());
                validateAndUpdate();
            });
            result.setWidget(row, 8, useTrackedDataButton);
            row++;
        }
        return result;
    }

    private void spreadTotalDistance() {
        final Double desiredTotalDistanceInNauticalMiles = desiredTotalCourseDistanceInNauticalMilesBox.getValue();
        if (desiredTotalDistanceInNauticalMiles != null) {
            final Distance desiredTotalDistance = new NauticalMileDistance(desiredTotalDistanceInNauticalMiles);
            final ORCPerformanceCurveLegImpl[] legData = getResult();
            final Distance currentTotalDistance = getEffectiveTotalDistance(legData);
            Distance checkSum = Distance.NULL;
            for (int zeroBasedLegIndex=0; zeroBasedLegIndex<waypointList.getList().size()-1; zeroBasedLegIndex++) {
                final Distance effectiveLegDistance = getEffectiveLegDistance(legData, zeroBasedLegIndex);
                final Distance newExplicitLegDistance = desiredTotalDistance.scale(effectiveLegDistance.divide(currentTotalDistance));
                distanceInNauticalMilesBoxes[zeroBasedLegIndex].setValue(newExplicitLegDistance.getNauticalMiles());
                checkSum = checkSum.add(effectiveLegDistance);
            }
            updateTotalDistanceLabel();
            validateAndUpdate();
        }
    }

    @Override
    public FocusWidget getInitialFocusWidget() {
        return desiredTotalCourseDistanceInNauticalMilesBox;
    }

    @Override
    protected ORCPerformanceCurveLegImpl[] getResult() {
        final ORCPerformanceCurveLegImpl[] result = new ORCPerformanceCurveLegImpl[waypointList.getList().size()-1];
        for (int i=0; i<result.length; i++) {
            final ORCPerformanceCurveLegTypes selectedLegType = getSelectedLegType(i);
            if (selectedLegType == null) {
                result[i] = null;
            } else if (selectedLegType == ORCPerformanceCurveLegTypes.TWA) {
                result[i] = new ORCPerformanceCurveLegImpl(
                        distanceInNauticalMilesBoxes[i].getValue() == null ? null
                                : new NauticalMileDistance(distanceInNauticalMilesBoxes[i].getValue()),
                        twaBoxes[i].getValue() == null ? null : new DegreeBearingImpl(twaBoxes[i].getValue()));
            } else {
                result[i] = new ORCPerformanceCurveLegImpl(distanceInNauticalMilesBoxes[i].getValue() == null ? null
                        : new NauticalMileDistance(distanceInNauticalMilesBoxes[i].getValue()), selectedLegType);
            }
        }
        return result;
    }
}
