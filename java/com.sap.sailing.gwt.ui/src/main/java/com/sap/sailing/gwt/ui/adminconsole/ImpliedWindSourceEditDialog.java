package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Set;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.orc.FixedSpeedImpliedWind;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.domain.common.orc.ImpliedWindSourceVisitor;
import com.sap.sailing.domain.common.orc.OtherRaceAsImpliedWindSource;
import com.sap.sailing.domain.common.orc.OwnMaxImpliedWind;
import com.sap.sailing.domain.common.orc.impl.FixedSpeedImpliedWindSourceImpl;
import com.sap.sailing.domain.common.orc.impl.OtherRaceAsImpliedWindSourceImpl;
import com.sap.sailing.domain.common.orc.impl.OwnMaxImpliedWindImpl;
import com.sap.sailing.gwt.ui.adminconsole.AbstractLeaderboardConfigPanel.RaceColumnDTOAndFleetDTOWithNameBasedEquality;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DoubleBox;
import com.sap.sse.security.ui.client.UserService;

public class ImpliedWindSourceEditDialog extends DataEntryDialog<ImpliedWindSource> {
    private static final String RADIO_BUTTON_GROUP_NAME = "IMPLIED_WIND_SOURCE_SELECTION";
    
    private final ImpliedWindSource previousImpliedWindSource;
    private final RadioButton noneButton;
    private final RadioButton fixedButton;
    private final RadioButton ownMaxImpliedWindButton;
    private final RadioButton fromOtherRaceButton;
    private final RaceSlotSelectionPanel raceSlotSelectionPanel;
    private final DoubleBox fixedImpliedWindSpeedBox;
    private final StringMessages stringMessages;
    
    public ImpliedWindSourceEditDialog(RaceColumnDTOAndFleetDTOWithNameBasedEquality raceSlot,
            final ImpliedWindSource previousImpliedWindSource,
            Iterable<StrippedLeaderboardDTO> availableLeaderboards, final StringMessages stringMessages,
            ErrorReporter errorReporter, SailingServiceWriteAsync sailingServiceWrite, UserService userService,
            DialogCallback<ImpliedWindSource> callback) {
        super(stringMessages.impliedWindSource(),
                stringMessages.provideImpliedWindSourceForRace(
                        raceSlot.getC().getName() + "/" + raceSlot.getA().getName() + "/" + raceSlot.getB().getName()),
                stringMessages.ok(), stringMessages.cancel(), new DataEntryDialog.Validator<ImpliedWindSource>() {
                    @Override
                    public String getErrorMessage(ImpliedWindSource valueToValidate) {
                        final String result;
                        if (valueToValidate == null) {
                            result = null;
                        } else {
                            result = valueToValidate.accept(new ImpliedWindSourceVisitor<String>() {
                                @Override
                                public String visit(FixedSpeedImpliedWind impliedWindSource) {
                                    return impliedWindSource.getFixedImpliedWindSpeed() == null ?
                                            stringMessages.pleaseProvideAFixedValueForImpliedWindSpeed() : null;
                                }

                                @Override
                                public String visit(OtherRaceAsImpliedWindSource impliedWindSource) {
                                    return impliedWindSource.getLeaderboardAndRaceColumnAndFleetOfDefiningRace() == null ?
                                            stringMessages.pleaseSelectARaceWhoseImpliedWindToUse() : null;
                                }

                                @Override
                                public String visit(OwnMaxImpliedWind impliedWindSource) {
                                    return null;
                                }
                            });
                        }
                        return result;
                    }
                }, callback);
        this.stringMessages = stringMessages;
        this.previousImpliedWindSource = previousImpliedWindSource;
        this.noneButton = new RadioButton(RADIO_BUTTON_GROUP_NAME, stringMessages.none());
        this.fixedButton = new RadioButton(RADIO_BUTTON_GROUP_NAME, stringMessages.fixedImpliedWindInKnots());
        this.ownMaxImpliedWindButton = new RadioButton(RADIO_BUTTON_GROUP_NAME, stringMessages.ownMaxImpliedWind());
        this.fromOtherRaceButton = new RadioButton(RADIO_BUTTON_GROUP_NAME, stringMessages.useImpliedWindFromOtherRace());
        this.fixedImpliedWindSpeedBox = createDoubleBox(previousImpliedWindSource instanceof FixedSpeedImpliedWind &&
                ((FixedSpeedImpliedWind) previousImpliedWindSource).getFixedImpliedWindSpeed() != null ?
                        ((FixedSpeedImpliedWind) previousImpliedWindSource).getFixedImpliedWindSpeed().getKnots() : null, /* visible length */ 5);
        this.raceSlotSelectionPanel = new RaceSlotSelectionPanel(sailingServiceWrite, userService, stringMessages,
                errorReporter, /* multiSelection */ false, availableLeaderboards, previousImpliedWindSource instanceof OtherRaceAsImpliedWindSource ?
                        getLeaderboardNameAndRaceColumnNameAndFleetName((OtherRaceAsImpliedWindSource) previousImpliedWindSource, availableLeaderboards) : null);
        this.raceSlotSelectionPanel.getSelectionModel().addSelectionChangeHandler(e->validateAndUpdate());
        final ValueChangeHandler<Boolean> buttonSelectionHandler = e->{
            raceSlotSelectionPanel.setVisible(fromOtherRaceButton.getValue());
            fixedImpliedWindSpeedBox.setEnabled(fixedButton.getValue());
            validateAndUpdate();
        };
        noneButton.addValueChangeHandler(buttonSelectionHandler);
        fixedButton.addValueChangeHandler(buttonSelectionHandler);
        ownMaxImpliedWindButton.addValueChangeHandler(buttonSelectionHandler);
        fromOtherRaceButton.addValueChangeHandler(buttonSelectionHandler);
    }

    private RaceColumnDTOAndFleetDTOWithNameBasedEquality getLeaderboardNameAndRaceColumnNameAndFleetName(
            OtherRaceAsImpliedWindSource previousImpliedWindSource, Iterable<StrippedLeaderboardDTO> availableLeaderboards) {
        final RaceColumnDTOAndFleetDTOWithNameBasedEquality result;
        if (previousImpliedWindSource == null || previousImpliedWindSource.getLeaderboardAndRaceColumnAndFleetOfDefiningRace() == null) {
            result = null;
        } else {
            RaceColumnDTOAndFleetDTOWithNameBasedEquality preResult = null;
            for (final StrippedLeaderboardDTO leaderboard : availableLeaderboards) {
                if (leaderboard.getName().equals(previousImpliedWindSource.getLeaderboardAndRaceColumnAndFleetOfDefiningRace().getA())) {
                    final RaceColumnDTO raceColumn = leaderboard.getRaceColumnByName(previousImpliedWindSource.getLeaderboardAndRaceColumnAndFleetOfDefiningRace().getB());
                    if (raceColumn != null) {
                        for (final FleetDTO fleet : raceColumn.getFleets()) {
                            if (fleet.getName().equals(previousImpliedWindSource.getLeaderboardAndRaceColumnAndFleetOfDefiningRace().getC())) {
                                preResult = new RaceColumnDTOAndFleetDTOWithNameBasedEquality(raceColumn, fleet, leaderboard);
                                break;
                            }
                        }
                    }
                }
            }
            result = preResult;
        }
        return result;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel result = new VerticalPanel();
        final CaptionPanel options = new CaptionPanel(stringMessages.impliedWindSource());
        final Grid optionsGrid = new Grid(4, 2);
        result.add(options);
        options.add(optionsGrid);
        optionsGrid.setWidget(0, 0, noneButton);
        optionsGrid.setWidget(1, 0, fixedButton);
        optionsGrid.setWidget(1, 1, fixedImpliedWindSpeedBox);
        optionsGrid.setWidget(2, 0, ownMaxImpliedWindButton);
        optionsGrid.setWidget(3, 0, fromOtherRaceButton);
        if (previousImpliedWindSource == null) {
            noneButton.setValue(true, /* fireEvents */ true);
        } else {
            previousImpliedWindSource.accept(new ImpliedWindSourceVisitor<Void>() {
                @Override
                public Void visit(FixedSpeedImpliedWind impliedWindSource) {
                    fixedButton.setValue(true, /* fireEvents */ true);
                    return null;
                }
    
                @Override
                public Void visit(OtherRaceAsImpliedWindSource impliedWindSource) {
                    fromOtherRaceButton.setValue(true, /* fireEvents */ true);
                    return null;
                }
    
                @Override
                public Void visit(OwnMaxImpliedWind impliedWindSource) {
                    ownMaxImpliedWindButton.setValue(true, /* fireEvents */ true);
                    return null;
                }
            });
        }
        result.add(raceSlotSelectionPanel);
        return result;
    }

    @Override
    protected ImpliedWindSource getResult() {
        final ImpliedWindSource result;
        if (noneButton.getValue()) {
            result = null;
        } else if (fixedButton.getValue()) {
            result = new FixedSpeedImpliedWindSourceImpl(fixedImpliedWindSpeedBox.getValue()==null?null:new KnotSpeedImpl(fixedImpliedWindSpeedBox.getValue()));
        } else if (ownMaxImpliedWindButton.getValue()) {
            result = new OwnMaxImpliedWindImpl();
        } else {
            final Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality> otherRaceSelection = raceSlotSelectionPanel.getSelectionModel().getSelectedSet();
            if (otherRaceSelection != null && otherRaceSelection.size() == 1) {
                final RaceColumnDTOAndFleetDTOWithNameBasedEquality selectedRace = otherRaceSelection.iterator().next();
                result = new OtherRaceAsImpliedWindSourceImpl(new Triple<>(selectedRace.getC().getName(), selectedRace.getA().getName(), selectedRace.getB().getName()));
            } else {
                result = new OtherRaceAsImpliedWindSourceImpl(null); // invalid!
            }
        }
        return result;
    }
}
