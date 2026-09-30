package com.sap.sailing.gwt.ui.polarmining;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.datamining.shared.FoilingSegmentsDataMiningSettings;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.polars.datamining.shared.PolarDataMiningSettings;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.gwt.client.dialog.DoubleBox;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;

/**
 * Provides a widget for configuring {@link PolarDataMiningSettings}, including validation.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class FoilingSegmentsDataMiningSettingsDialogComponent implements SettingsDialogComponent<FoilingSegmentsDataMiningSettings> {

    private FoilingSegmentsDataMiningSettings settings;
    private StringMessages stringMessages;
    private DoubleBox minimumFoilingSegmentsDurationInSecondsBox;
    private DoubleBox minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBox;
    private DoubleBox minimumSpeedForFoilingInKnotsBox;
    private DoubleBox maximumSpeedNotFoilingInKnotsBox;
    private DoubleBox minimumRideHeightInMetersBox;

    public FoilingSegmentsDataMiningSettingsDialogComponent(FoilingSegmentsDataMiningSettings settings) {
        this.settings = settings;
        this.stringMessages = StringMessages.INSTANCE;
    }

    @Override
    public Widget getAdditionalWidget(DataEntryDialog<?> dialog) {
        VerticalPanel vp = new VerticalPanel();
        Grid grid = new Grid(5, 2);
        grid.setCellPadding(5);
        vp.add(grid);
        setupGrid(grid, dialog);
        return vp;
    }

    private void setupGrid(Grid grid, DataEntryDialog<?> dialog) {
        Label minimumFoilingSegmentsDurationInSecondsLabel = new Label(stringMessages.minimumFoilingSegmentsDurationInSeconds() + ":");
        minimumFoilingSegmentsDurationInSecondsLabel.setTitle(stringMessages.minimumFoilingSegmentsDurationInSecondsTooltip());
        grid.setWidget(0, 0, minimumFoilingSegmentsDurationInSecondsLabel);
        if (settings.getMinimumFoilingSegmentDuration() == null) {
            minimumFoilingSegmentsDurationInSecondsBox = dialog.createDoubleBox(6);
        } else {
            minimumFoilingSegmentsDurationInSecondsBox = dialog.createDoubleBox(settings.getMinimumFoilingSegmentDuration().asSeconds(), 6);
        }
        grid.setWidget(0, 1, minimumFoilingSegmentsDurationInSecondsBox);
        Label minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBoxLabel = new Label(stringMessages.minimumDurationBetweenAdjacentFoilingSegmentsInSeconds() + ":");
        minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBoxLabel.setTitle(stringMessages.minimumDurationBetweenAdjacentFoilingSegmentsInSecondsTooltip());
        grid.setWidget(1, 0, minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBoxLabel);
        if (settings.getMinimumDurationBetweenAdjacentFoilingSegments() == null) {
            minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBox = dialog.createDoubleBox(6);
        } else {
            minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBox = dialog.createDoubleBox(settings.getMinimumFoilingSegmentDuration().asSeconds(), 6);
        }
        grid.setWidget(1, 1, minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBox);
        Label maximumSpeedNotFoilingInKnotsLabel = new Label(stringMessages.maximumSpeedNotFoilingInKnots() + ":");
        maximumSpeedNotFoilingInKnotsLabel.setTitle(stringMessages.maximumSpeedNotFoilingInKnotsTooltip());
        grid.setWidget(2, 0, maximumSpeedNotFoilingInKnotsLabel);
        if (settings.getMaximumSpeedNotFoiling() == null) {
            maximumSpeedNotFoilingInKnotsBox = dialog.createDoubleBox(6);
        } else {
            maximumSpeedNotFoilingInKnotsBox = dialog.createDoubleBox(settings.getMaximumSpeedNotFoiling().getKnots(), 6);
        }
        grid.setWidget(2, 1, maximumSpeedNotFoilingInKnotsBox);
        Label minimumSpeedForFoilingInKnotsLabel = new Label(stringMessages.minimumSpeedForFoilingInKnots() + ":");
        minimumSpeedForFoilingInKnotsLabel.setTitle(stringMessages.minimumSpeedForFoilingInKnotsTooltip());
        grid.setWidget(3, 0, minimumSpeedForFoilingInKnotsLabel);
        if (settings.getMinimumSpeedForFoiling() == null) {
            minimumSpeedForFoilingInKnotsBox = dialog.createDoubleBox(6);
        } else {
            minimumSpeedForFoilingInKnotsBox = dialog.createDoubleBox(settings.getMinimumSpeedForFoiling().getKnots(), 6);
        }
        grid.setWidget(3, 1, minimumSpeedForFoilingInKnotsBox);
        Label minimumRideHeightInMetersLabel = new Label(stringMessages.minimumRideHeightInMeters() + ":");
        minimumRideHeightInMetersLabel.setTitle(stringMessages.minimumRideHeightInMetersTooltip());
        grid.setWidget(4, 0, minimumRideHeightInMetersLabel);
        if (settings.getMinimumRideHeight() == null) {
            minimumRideHeightInMetersBox = dialog.createDoubleBox(6);
        } else {
            minimumRideHeightInMetersBox = dialog.createDoubleBox(settings.getMinimumRideHeight().getMeters(), 6);
        }
        grid.setWidget(4, 1, minimumRideHeightInMetersBox);
    }

    @Override
    public FoilingSegmentsDataMiningSettings getResult() {
        return new FoilingSegmentsDataMiningSettings(
                minimumFoilingSegmentsDurationInSecondsBox.getValue() == null ? null
                        : new MillisecondsDurationImpl((long) (minimumFoilingSegmentsDurationInSecondsBox.getValue() * 1000.)),
                minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBox.getValue() == null ? null
                        : new MillisecondsDurationImpl((long) (minimumDurationBetweenAdjacentFoilingSegmentsInSecondsBox.getValue() * 1000.)),
                minimumSpeedForFoilingInKnotsBox.getValue() == null ? null
                        : new KnotSpeedImpl(minimumSpeedForFoilingInKnotsBox.getValue()),
                maximumSpeedNotFoilingInKnotsBox.getValue() == null ? null
                        : new KnotSpeedImpl(maximumSpeedNotFoilingInKnotsBox.getValue()), minimumRideHeightInMetersBox.getValue() == null ? null
                        : new MeterDistance(minimumRideHeightInMetersBox.getValue()));
    }

    @Override
    public FocusWidget getFocusWidget() {
        return minimumFoilingSegmentsDurationInSecondsBox;
    }

    @Override
    public Validator<FoilingSegmentsDataMiningSettings> getValidator() {
        return new FoilingSegmentsDataMiningSettingsValidator(stringMessages);
    }

}
