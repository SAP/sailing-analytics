package com.sap.sailing.gwt.managementconsole.partials.inputs.courseareas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;

/**
 * Modal used by {@link CourseAreasInput} to let the user pick a subset (or all) of the course areas offered by another
 * event before appending copies of them to the course-area list currently being edited. Each course area is shown as a
 * compact, tappable row: a checkbox next to the name and, where defined, an economical one-line summary of its center
 * position and radius. The layout is built with flexbox so that it stays usable on narrow phone screens.
 */
class CourseAreaPickerDialog extends DialogBox {
    private final List<CourseAreaDTO> offeredCourseAreas;
    private final List<CheckBox> checkBoxes;

    CourseAreaPickerDialog(final List<CourseAreaDTO> offeredCourseAreas, final StringMessages stringMessages,
            final Consumer<List<CourseAreaDTO>> onConfirm) {
        super(/* autoHide */ false, /* modal */ true);
        CourseAreasInputResources.INSTANCE.style().ensureInjected();
        this.offeredCourseAreas = offeredCourseAreas;
        this.checkBoxes = new ArrayList<>();
        final ManagementConsoleResources appResources = ManagementConsoleResources.INSTANCE;
        final CourseAreasInputResources.Style style = CourseAreasInputResources.INSTANCE.style();
        setText(stringMessages.selectCourseAreas());
        setGlassEnabled(/* enabled */ true);
        final FlowPanel content = new FlowPanel();
        content.addStyleName(appResources.style().card());
        content.addStyleName(style.pickerContent());
        // Select-all / deselect-all shortcut on its own header row
        final CheckBox selectAllCheckBox = new CheckBox(stringMessages.selectAll());
        selectAllCheckBox.addValueChangeHandler(event -> {
            for (final CheckBox checkBox : checkBoxes) {
                checkBox.setValue(event.getValue());
            }
        });
        final FlowPanel selectAllRow = new FlowPanel();
        selectAllRow.addStyleName(style.pickerSelectAll());
        selectAllRow.add(selectAllCheckBox);
        content.add(selectAllRow);
        // One compact row per offered course area
        final FlowPanel list = new FlowPanel();
        list.addStyleName(style.pickerList());
        for (final CourseAreaDTO courseArea : offeredCourseAreas) {
            list.add(createCourseAreaRow(courseArea, style));
        }
        content.add(list);
        // Action buttons, right-aligned
        final FlowPanel buttonRow = new FlowPanel();
        buttonRow.addStyleName(style.pickerButtons());
        final Button confirmButton = new Button(stringMessages.appendCourseAreasConfirm());
        confirmButton.addStyleName(appResources.style().button());
        confirmButton.addStyleName(appResources.style().primary());
        confirmButton.addClickHandler(e -> {
            hide();
            onConfirm.accept(collectSelection());
        });
        final Button cancelButton = new Button(stringMessages.cancel());
        cancelButton.addStyleName(appResources.style().button());
        cancelButton.addClickHandler(e -> hide());
        buttonRow.add(confirmButton);
        buttonRow.add(cancelButton);
        content.add(buttonRow);
        setWidget(content);
    }

    private FlowPanel createCourseAreaRow(final CourseAreaDTO courseArea, final CourseAreasInputResources.Style style) {
        final FlowPanel row = new FlowPanel();
        row.addStyleName(style.pickerRow());
        final CheckBox checkBox = new CheckBox();
        checkBoxes.add(checkBox);
        row.add(checkBox);
        // Name and, where present, an economical geometry summary; tapping the details toggles the checkbox so that
        // the whole line (not just the small checkbox) is a comfortable touch target on phones
        final FlowPanel details = new FlowPanel();
        details.addStyleName(style.pickerDetails());
        final InlineLabel nameLabel = new InlineLabel(courseArea.getName() != null ? courseArea.getName() : "");
        nameLabel.addStyleName(style.pickerName());
        details.add(nameLabel);
        final String metaText = formatGeometry(courseArea);
        if (!metaText.isEmpty()) {
            final InlineLabel metaLabel = new InlineLabel(metaText);
            metaLabel.addStyleName(style.pickerMeta());
            details.add(metaLabel);
        }
        details.addDomHandler(e -> checkBox.setValue(!checkBox.getValue(), /* fireEvents */ true),
                ClickEvent.getType());
        row.add(details);
        return row;
    }

    private String formatGeometry(final CourseAreaDTO courseArea) {
        final Position centerPosition = courseArea.getCenterPosition();
        final Distance radius = courseArea.getRadius();
        final NumberFormat coordinateFormat = NumberFormat.getFormat("0.####");
        final NumberFormat radiusFormat = NumberFormat.getFormat("0");
        final StringBuilder builder = new StringBuilder();
        if (centerPosition != null) {
            builder.append(coordinateFormat.format(centerPosition.getLatDeg())).append(", ")
                    .append(coordinateFormat.format(centerPosition.getLngDeg()));
        }
        if (radius != null) {
            if (builder.length() > 0) {
                builder.append(" \u00b7 ");
            }
            builder.append('r').append(' ').append(radiusFormat.format(radius.getMeters())).append(" m");
        }
        return builder.toString();
    }

    private List<CourseAreaDTO> collectSelection() {
        final List<CourseAreaDTO> selected = new ArrayList<>();
        for (int index = 0; index < offeredCourseAreas.size(); index++) {
            if (checkBoxes.get(index).getValue()) {
                selected.add(offeredCourseAreas.get(index));
            }
        }
        return selected;
    }

    void showCentered() {
        center();
        show();
    }
}
