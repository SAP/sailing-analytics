package com.sap.sailing.gwt.ui.adminconsole;

import java.util.List;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;

public abstract class RegattaWithSeriesAndFleetsDialog extends AbstractRegattaWithSeriesAndFleetsDialog<RegattaDTO> {

    protected TextBox nameEntryField;
    protected TextBox boatClassEntryField;

    public RegattaWithSeriesAndFleetsDialog(RegattaDTO regatta, List<EventDTO> existingEvents, String title,
            String okButton, StringMessages stringMessages, Validator<RegattaDTO> validator,
            DialogCallback<RegattaDTO> callback) {
        super(regatta, existingEvents, title, okButton, stringMessages, validator, callback);
        this.stringMessages = stringMessages;
        nameEntryField = createTextBox(null);
        nameEntryField.ensureDebugId("NameTextBox");
        nameEntryField.setVisibleLength(40);
        nameEntryField.setText(regatta.getName());
        boatClassEntryField = createTextBox(null);
        boatClassEntryField.ensureDebugId("BoatClassTextBox");
        boatClassEntryField.setVisibleLength(20);
        if (regatta.boatClass != null) {
            boatClassEntryField.setText(regatta.boatClass.getName());
        }
		setSeriesEditor();
    }

    @Override
    protected RegattaDTO getResult() {
        regatta.setName(nameEntryField.getText());
        regatta.boatClass = new BoatClassDTO(boatClassEntryField.getText(), 0.0);
        regatta.scoringScheme = getSelectedScoringSchemeType();
        regatta.useStartTimeInference = useStartTimeInferenceCheckBox.getValue();
        setCourseAreaInRegatta(regatta);
        return regatta;
    }
    
    @Override
    protected void setupAdditionalWidgetsOnPanel(VerticalPanel panel){
        Grid formGrid = (Grid)panel.getWidget(0);
        formGrid.insertRow(0);
        formGrid.insertRow(0);
        formGrid.setWidget(0, 0, new Label(stringMessages.name() + ":"));
        formGrid.setWidget(0, 1, nameEntryField);
        formGrid.setWidget(1, 0, new Label(stringMessages.boatClass() + ":"));
        formGrid.setWidget(1, 1, boatClassEntryField);
    }
}
