package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.racemap.Pattern;
import com.sap.sailing.gwt.ui.shared.racemap.Shape;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class MarkEditDialog extends DataEntryDialog<MarkDTO> {
    private final MarkDTO markToEdit; //color, shape, pattern, type
    private final TextBox name;
    private final TextBox shortName;
    private final ListBox shape;
    private final ListBox pattern;
    private final ListBox type;
    private final TextBox color;
    private final TextBox colorSample;
    private final StringMessages stringMessages;
    
    public MarkEditDialog(final StringMessages stringMessages, MarkDTO markToEdit, boolean isNewMark,
            DialogCallback<MarkDTO> callback) {
        this(stringMessages, markToEdit, isNewMark, new TextBox[1], callback);
    }
    
    private MarkEditDialog(final StringMessages stringMessages, MarkDTO markToEdit, boolean isNewMark,
            final TextBox[] color, DialogCallback<MarkDTO> callback) {
        // the validator would like to access the color TextBox but it cannot access instance fields;
        // therefore, an array is used which can be initialized in the calling constructor and can
        // be filled in this constructor before the validator will ever get called
        super(isNewMark ? stringMessages.add(stringMessages.mark()) : stringMessages.edit(stringMessages.mark()),
                isNewMark ? stringMessages.add(stringMessages.mark()) : stringMessages.edit(stringMessages.mark()),
                        stringMessages.ok(), stringMessages.cancel(), new DataEntryDialog.Validator<MarkDTO>() {
                    @Override
                    public String getErrorMessage(MarkDTO valueToValidate) {
                        String result = null;
                        if (valueToValidate.getName() == null || valueToValidate.getName().isEmpty()) {
                            result = stringMessages.pleaseEnterAName();
                        } else if (valueToValidate.color == null && color[0] != null && !color[0].getText().isEmpty()) {
                            result = color[0].getText();
                        }
                        return result;
                    }
                }, /* animationEnabled */true, callback);
        this.stringMessages = stringMessages;
        this.markToEdit = markToEdit;
        this.name = createTextBox(markToEdit.getName());
        this.shortName = createTextBox(markToEdit.getShortName());
        this.shape = createAndSelectListBox(Shape.values(), markToEdit.shape, true);
        this.type = createAndSelectListBox(MarkType.values(), markToEdit.type == null ? null : markToEdit.type.name(), false);
        color[0] = createTextBox(markToEdit.color==null?null:markToEdit.color.getAsHtml());
        colorSample = new TextBox();
        colorSample.setVisibleLength(3);
        colorSample.setEnabled(false);
        this.color = color[0];
        this.pattern = createAndSelectListBox(Pattern.values(), markToEdit.pattern, true);
    }
    
    private <T extends Enum<T>> ListBox createAndSelectListBox(T[] values, String toSelect, boolean emptySelectionPossible) {
        ListBox result = createListBox(/* isMultipleSelect */ false);
        int i=0;
        List<String> list = new ArrayList<String>();
        for (T t : values) {
            list.add(t.name());
        }
        if (emptySelectionPossible) {
            list.add("");
        }
        Collections.sort(list);
        for (String name : list) {
            result.addItem(name);
            if (name.equals(toSelect)) {
                result.setSelectedIndex(i);
            }
            i++;
        }
        return result;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return name;
    }

    @Override
    protected MarkDTO getResult() {
        MarkDTO result = new MarkDTO(markToEdit.getIdAsString(), name.getText(), shortName.getText());
        result.shape = shape.getItemText(shape.getSelectedIndex());
        result.type = MarkType.valueOf(type.getItemText(type.getSelectedIndex()));
        result.color = AbstractColor.getCssColor(color.getText());
        if (result.color == null) {
            colorSample.getElement().removeAttribute("style");
        } else {
            colorSample.getElement().setAttribute("style", "background-color:"+result.color.getAsHtml());
        }
        result.pattern = pattern.getItemText(pattern.getSelectedIndex());
        return result;
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(7, 4);
        result.setWidget(0, 0, new Label(stringMessages.name()));
        result.setWidget(0, 1, name);
        result.setWidget(1, 0, new Label(stringMessages.shortName()));
        result.setWidget(1, 1, shortName);
        result.setWidget(2, 0, new Label(stringMessages.shape()));
        result.setWidget(2, 1, shape);
        result.setWidget(3, 0, new Label(stringMessages.type()));
        result.setWidget(3, 1, type);
        result.setWidget(4, 0, new Label(stringMessages.color()));
        result.setWidget(4, 1, color);
        result.setWidget(5, 2, new Label(stringMessages.sampleColor()));
        result.setWidget(5, 3, colorSample);
        result.setWidget(6, 0, new Label(stringMessages.pattern()));
        result.setWidget(6, 1, pattern);
        return result;
    }
}
