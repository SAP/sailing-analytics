package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import java.util.UUID;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class MarkRoleEditDialog extends DataEntryDialog<MarkRoleDTO> {

    private final StringMessages stringMessages;
    private final TextBox nameTextBox;
    private final TextBox shortNameTextBox;
    private final UUID id;

    public MarkRoleEditDialog(final StringMessages stringMessages, MarkRoleDTO markRoleToEdit,
            DialogCallback<MarkRoleDTO> callback) {
        super(stringMessages.edit() + " " + stringMessages.markRoles(), null, stringMessages.ok(),
                stringMessages.cancel(), new Validator<MarkRoleDTO>() {
                    @Override
                    public String getErrorMessage(MarkRoleDTO valueToValidate) {
                        String result = null;
                        boolean invalidName = valueToValidate.getName() == null || valueToValidate.getName().isEmpty();
                        if (invalidName) {
                            result = stringMessages.pleaseEnterAName();
                        }
                        return result;
                    }
                }, /* animationEnabled */ true, callback);
        this.ensureDebugId("MarkRolesEditDialog");
        id = markRoleToEdit.getUuid();
        this.stringMessages = stringMessages;
        this.nameTextBox = createTextBox(markRoleToEdit.getName());
        this.shortNameTextBox = createTextBox(markRoleToEdit.getShortName());
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return nameTextBox;
    }

    @Override
    protected MarkRoleDTO getResult() {
        return new MarkRoleDTO(id, nameTextBox.getValue(), shortNameTextBox.getValue());
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(7, 2);
        result.setWidget(0, 0, new Label(stringMessages.name()));
        result.setWidget(0, 1, nameTextBox);
        result.setWidget(1, 0, new Label(stringMessages.shortName()));
        result.setWidget(1, 1, shortNameTextBox);
        return result;
    }

}
