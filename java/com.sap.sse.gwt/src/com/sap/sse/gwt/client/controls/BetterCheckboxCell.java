package com.sap.sse.gwt.client.controls;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * A checkbox cell that can be styled to a certain degree.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class BetterCheckboxCell extends AbstractEditableCell<Boolean, Boolean> {
    public final static String DEFAULT_CSS_CLASS_SELECTED = "betterCheckboxCellSelected";
    public final static String DEFAULT_CSS_CLASS_DESELECTED = "betterCheckboxCellDeselected";
    
    /**
     * An html string representation of a checked input box.
     */
    private final SafeHtml INPUT_CHECKED;

    /**
     * An html string representation of an unchecked input box.
     */
    private final SafeHtml INPUT_UNCHECKED;

    public BetterCheckboxCell() {
        this(DEFAULT_CSS_CLASS_SELECTED, DEFAULT_CSS_CLASS_DESELECTED);
    }
    
    public BetterCheckboxCell(String cssClassSelected, String cssClassDeselected) {
        super(BrowserEvents.CHANGE, BrowserEvents.KEYDOWN, BrowserEvents.CLICK);
        INPUT_CHECKED = SafeHtmlUtils.fromSafeConstant("<div class=\""+cssClassSelected+"\"/>");
        INPUT_UNCHECKED = SafeHtmlUtils.fromSafeConstant("<div class=\""+cssClassDeselected+"\"/>");
    }

    
    @Override
    public void onBrowserEvent(Context context, Element parent, Boolean value, NativeEvent event,
            ValueUpdater<Boolean> valueUpdater) {
        String type = event.getType();
        boolean enterPressed = BrowserEvents.KEYDOWN.equals(type) && event.getKeyCode() == KeyCodes.KEY_ENTER;
        if (BrowserEvents.CLICK.equals(type) || BrowserEvents.CHANGE.equals(type) || enterPressed) {
            InputElement input = parent.getFirstChild().cast();
            Boolean isChecked = input.isChecked();
            /*
             * Toggle the value if the enter key was pressed and the cell handles selection or doesn't depend on
             * selection. If the cell depends on selection but doesn't handle selection, then ignore the enter key and
             * let the SelectionEventManager determine which keys will trigger a change.
             */
            if (enterPressed && (handlesSelection() || !dependsOnSelection())) {
                isChecked = !isChecked;
                input.setChecked(isChecked);
            }
            if (valueUpdater != null) {
                valueUpdater.update(isChecked);
            }
            event.stopPropagation();
        }
    }

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, Boolean value, SafeHtmlBuilder sb) {
        if (value != null && value) {
          sb.append(INPUT_CHECKED);
        } else {
          sb.append(INPUT_UNCHECKED);
        }
    }

    @Override
    public boolean isEditing(com.google.gwt.cell.client.Cell.Context context, Element parent, Boolean value) {
        // A checkbox is never in "edit mode". There is no intermediate state
        // between checked and unchecked.
        return false;
    }

    
}
