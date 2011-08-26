package com.sap.sailing.gwt.ui.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A text entry dialog with ok/cancel button and configurable validation rule. Subclasses may provide a redefinition for
 * {@link #getAdditionalWidget()} to add a widget below the text field, e.g., for capturing additional data.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TextfieldEntryDialog extends DataEntryDialog<String> {
    private final TextBox entryField;
    
    public TextfieldEntryDialog(String title, String message, String okButtonName, String cancelButtonName,
            String initialValue, final Validator<String> validator, final AsyncCallback<String> callback) {
        super(title, message, okButtonName, cancelButtonName, initialValue, validator, callback);
        entryField = createTextBox(initialValue);
    }
    
    /**
     * Can contribute an additional widget to be displayed underneath the text entry field. If <code>null</code> is
     * returned, no additional widget will be displayed. This is the default behavior of this default implementation.
     */
    protected Widget getAdditionalWidget() {
       return entryField;
    }
    
    public void show() {
        super.show();
        entryField.setFocus(true);
    }

    @Override
    protected String getResult() {
        return entryField.getText();
    }
}
