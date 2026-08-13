package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Pop-up dialog used by {@link CourseAreaTabComposite} to let the user pick zero or more {@link CourseAreaDTO course
 * areas} from a list.
 */
public class CourseAreaSelectionDialog extends DataEntryDialog<List<CourseAreaDTO>> {
    private final List<CourseAreaDTO> courseAreas;
    private final StringMessages stringMessages;
    private Grid selectionGrid;

    public CourseAreaSelectionDialog(final List<CourseAreaDTO> courseAreas, final StringMessages stringMessages,
            final DialogCallback<List<CourseAreaDTO>> callback) {
        super(stringMessages.selectCourseAreas(), null, stringMessages.appendCourseAreasConfirm(), stringMessages.cancel(), null, callback);
        this.courseAreas = courseAreas;
        this.stringMessages = stringMessages;
    }

    @Override
    protected List<CourseAreaDTO> getResult() {
        final List<CourseAreaDTO> selected = new ArrayList<>();
        for (int i = 0; i < courseAreas.size(); i++) {
            final CheckBox checkBox = (CheckBox) selectionGrid.getWidget(i + 1, 1);
            if (checkBox.getValue()) {
                selected.add(courseAreas.get(i));
            }
        }
        return selected;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(5);
        selectionGrid = new Grid(courseAreas.size() + 1, 2);
        selectionGrid.setCellSpacing(5);
        final Label selectAllLabel = new Label(stringMessages.selectAll());
        selectAllLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
        final CheckBox selectAllCheckBox = new CheckBox();
        selectionGrid.setWidget(0, 0, selectAllLabel);
        selectionGrid.setWidget(0, 1, selectAllCheckBox);
        for (int i = 0; i < courseAreas.size(); i++) {
            selectionGrid.setWidget(i + 1, 0, new Label(courseAreas.get(i).getName()));
            selectionGrid.setWidget(i + 1, 1, new CheckBox());
        }
        selectAllCheckBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                final boolean checked = selectAllCheckBox.getValue();
                for (int i = 1; i < selectionGrid.getRowCount(); i++) {
                    ((CheckBox) selectionGrid.getWidget(i, 1)).setValue(checked);
                }
            }
        });
        mainPanel.add(selectionGrid);
        return mainPanel;
    }
}
