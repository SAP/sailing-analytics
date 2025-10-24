package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DoubleBox;

/**
 * Fills a {@link RaceColumnDTO} object by setting its {@link RaceColumnDTO#setMedalRace(boolean) medal race} property,
 * the column factor and its name.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class RaceColumnInLeaderboardDialog extends DataEntryDialog<RaceColumnInLeaderboardDialog.RaceColumnDescriptor> {
    private final TextBox raceNameBox;
    private final DoubleBox explicitFactorBox;
    private final CheckBox isMedalRace;
    private final StringMessages stringMessages;
    private final boolean isRegattaLeaderboard;

    public static class RaceColumnDescriptor {
        private String name;
        private boolean isMedalRace;
        private Double explicitFactor;
        public RaceColumnDescriptor(String name, boolean isMedalRace, Double explicitFactor) {
            super();
            this.name = name;
            this.isMedalRace = isMedalRace;
            this.explicitFactor = explicitFactor;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public boolean isMedalRace() {
            return isMedalRace;
        }
        public void setMedalRace(boolean isMedalRace) {
            this.isMedalRace = isMedalRace;
        }
        public Double getExplicitFactor() {
            return explicitFactor;
        }
        public void setExplicitFactor(Double explicitFactor) {
            this.explicitFactor = explicitFactor;
        }
    }

    private static class RaceDialogValidator implements Validator<RaceColumnDescriptor> {
        private final StringMessages stringMessages;
        private final Iterable<RaceColumnDTO> existingRaces;

        public RaceDialogValidator(StringMessages stringConstants, Iterable<RaceColumnDTO> existingRaceColumnsAndFleetNames) {
            this.stringMessages = stringConstants;
            this.existingRaces = existingRaceColumnsAndFleetNames;
        }

        @Override
        public String getErrorMessage(RaceColumnDescriptor valueToValidate) {
            String errorMessage;
            String racename = valueToValidate.getName();
            Boolean isMedalRace = valueToValidate.isMedalRace();
            boolean isNameNotEmpty = racename != null & !racename.isEmpty();
            boolean medalRaceNotNull = isMedalRace != null;

            boolean unique = true;
            for (RaceColumnDTO raceColumn : existingRaces) {
                if (raceColumn.getRaceColumnName().equals(valueToValidate.getName())) {
                    unique = false;
                }
            }

            if (!isNameNotEmpty) {
                errorMessage = stringMessages.pleaseEnterAName();
            } else if (!medalRaceNotNull) {
                errorMessage = stringMessages.medalRaceIsNull();
            } else if (!unique) {
                errorMessage = stringMessages.raceWithThisNameAlreadyExists();
            } else {
                return errorMessage = null;
            }
            return errorMessage;
        }
    }

    public RaceColumnInLeaderboardDialog(Iterable<RaceColumnDTO> existingRaces, RaceColumnDTO raceColumnToEdit, 
            boolean isRegattaLeaderboard, StringMessages stringMessages, DialogCallback<RaceColumnDescriptor> callback) {
        super(stringMessages.actionRaceEdit(), null, stringMessages.ok(), stringMessages.cancel(),
                new RaceDialogValidator(stringMessages, existingRaces), callback);
        this.isRegattaLeaderboard = isRegattaLeaderboard;
        this.stringMessages = stringMessages;
        raceNameBox = createTextBox(raceColumnToEdit.getRaceColumnName());
        raceNameBox.setEnabled(!isRegattaLeaderboard);
        explicitFactorBox = raceColumnToEdit.getExplicitFactor() == null ?
                createDoubleBox(/* visibleLength */ 4) : createDoubleBox(raceColumnToEdit.getExplicitFactor(), /* visibleLength */ 4);
        isMedalRace = createCheckbox(stringMessages.medalRace());
        isMedalRace.setValue(raceColumnToEdit.isMedalRace());
        isMedalRace.setEnabled(!isRegattaLeaderboard);
    }

    @Override
    protected RaceColumnDescriptor getResult() {
        return new RaceColumnDescriptor(raceNameBox.getValue(), isMedalRace.getValue(),
                explicitFactorBox.getText().trim().length() == 0 ? null : explicitFactorBox.getValue());
    }

    @Override
    protected Widget getAdditionalWidget() {
        VerticalPanel mainPanel = new VerticalPanel();
        
        HorizontalPanel raceNamePanel = new HorizontalPanel();
        raceNamePanel.setSpacing(3);
        mainPanel.add(raceNamePanel);
        raceNamePanel.add(new Label(stringMessages.name() + ":"));
        raceNamePanel.add(raceNameBox);
        HorizontalPanel factorPanel = new HorizontalPanel();
        factorPanel.setSpacing(3);
        mainPanel.add(factorPanel);
        factorPanel.add(new Label(stringMessages.factor() + ":"));
        factorPanel.add(explicitFactorBox);
        alignAllPanelWidgetsVertically(raceNamePanel, HasVerticalAlignment.ALIGN_MIDDLE);
        mainPanel.add(isMedalRace);
        return mainPanel;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        final FocusWidget result;
        if (isRegattaLeaderboard) {
            result = explicitFactorBox;
        } else {
            result = raceNameBox;
        }
        return result;
    }

    @Override
    public void show() {
        super.show();
        if (isRegattaLeaderboard) {
            explicitFactorBox.selectAll();
        }
    }
}
