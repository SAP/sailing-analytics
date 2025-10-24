package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPropertiesDTO;
import com.sap.sailing.gwt.ui.shared.racemap.Pattern;
import com.sap.sailing.gwt.ui.shared.racemap.Shape;
import com.sap.sse.common.Color;
import com.sap.sse.gwt.client.ColorTextBox;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.controls.listedit.StringListEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class MarkPropertiesEditDialog extends DataEntryDialog<MarkPropertiesDTO> {
    private final TextBox nameTextBox;
    private final ColorTextBox displayColorTextBox;
    private final TextBox shortNameTextBox;
    private final ValueListBox<MarkType> markTypeValueListBox;
    private final ValueListBox<Pattern> patternValueListBox;
    private final ValueListBox<Shape> shapeValueListBox;
    final StringMessages stringMessages;
    private final Label labelShape;
    private final Label labelPattern;
    private final StringListEditorComposite tagsEditor;

    private final UUID id;

    public MarkPropertiesEditDialog(final StringMessages stringMessages, MarkPropertiesDTO markPropertiesToEdit,
            DialogCallback<MarkPropertiesDTO> callback) {
        super(stringMessages.edit() + " " + stringMessages.markProperties(), null, stringMessages.ok(),
                stringMessages.cancel(), new Validator<MarkPropertiesDTO>() {
                    @Override
                    public String getErrorMessage(MarkPropertiesDTO valueToValidate) {
                        String result = null;
                        boolean invalidName = valueToValidate.getName() == null || valueToValidate.getName().isEmpty();
                        boolean invalidMarkType = valueToValidate.getCommonMarkProperties().getType() == null;
                        if (invalidName) {
                            result = stringMessages.pleaseEnterAName();
                        } else if (invalidMarkType) {
                            result = stringMessages.pleaseEnterAValidValueFor(stringMessages.type(), "");
                        } else if (valueToValidate.getCommonMarkProperties().getColor() instanceof InvalidColor) {
                            result = valueToValidate.getCommonMarkProperties().getColor().getAsHtml();
                        }
                        return result;
                    }
                }, /* animationEnabled */true, callback);
        this.ensureDebugId("MarkPropertiesToEditEditDialog");
        id = markPropertiesToEdit.getUuid();
        this.stringMessages = stringMessages;

        this.markTypeValueListBox = new ValueListBox<>(new Renderer<MarkType>() {
            @Override
            public String render(MarkType object) {
                return object == null ? "" : object.name();
            }

            @Override
            public void render(MarkType object, Appendable appendable) throws IOException {
                appendable.append(render(object));

            }
        });

        labelPattern = new Label(stringMessages.pattern());
        this.patternValueListBox = new ValueListBox<>(new Renderer<Pattern>() {
            @Override
            public String render(Pattern object) {
                return object != null ? object.name() : "";
            }

            @Override
            public void render(Pattern object, Appendable appendable) throws IOException {
                appendable.append(render(object));
            }
        });

        labelShape = new Label(stringMessages.shape());
        this.shapeValueListBox = new ValueListBox<>(new Renderer<Shape>() {
            @Override
            public String render(Shape object) {
                return object != null ? object.name() : "";
            }

            @Override
            public void render(Shape object, Appendable appendable) throws IOException {
                appendable.append(render(object));
            }
        });

        this.nameTextBox = createTextBox(markPropertiesToEdit.getName());
        this.shortNameTextBox = createTextBox(markPropertiesToEdit.getCommonMarkProperties().getShortName());

        // setup mark selection
        this.markTypeValueListBox.setValue(markPropertiesToEdit.getCommonMarkProperties().getType() != null
                ? markPropertiesToEdit.getCommonMarkProperties().getType()
                : MarkType.values()[0]);
        markTypeValueListBox.setAcceptableValues(Arrays.asList(MarkType.values()));
        markTypeValueListBox.addValueChangeHandler(v -> handleMarkTypeChange());

        // setup display color textbox
        this.displayColorTextBox = createColorTextBox(markPropertiesToEdit.getCommonMarkProperties().getColor());

        // setup shape selection
        final String loadedShape = markPropertiesToEdit.getCommonMarkProperties().getShape();
        if (loadedShape != null && !loadedShape.isEmpty()) {
            shapeValueListBox.setValue(Shape.valueOf(loadedShape));
        }
        final Collection<Shape> shapeValues = new ArrayList<>(Arrays.asList(Shape.values()));
        shapeValues.add(null);
        shapeValueListBox.setAcceptableValues(shapeValues);

        // setup pattern selection
        final String loadedPattern = markPropertiesToEdit.getCommonMarkProperties().getPattern();
        if (loadedPattern != null && !loadedPattern.isEmpty()) {
            patternValueListBox.setValue(Pattern.valueOf(loadedPattern));
        }
        final Collection<Pattern> patternValues = new ArrayList<>(Arrays.asList(Pattern.values()));
        patternValues.add(null);
        patternValueListBox.setAcceptableValues(patternValues);

        // setup tag editor
        tagsEditor = new StringListEditorComposite(markPropertiesToEdit.getTags(), stringMessages,
                stringMessages.edit(stringMessages.tags()), IconResources.INSTANCE.removeIcon(),
                Collections.emptyList(), stringMessages.tag());

        handleMarkTypeChange();
    }

    private void handleMarkTypeChange() {
        final boolean isBuoy = markTypeValueListBox.getValue() == MarkType.BUOY;

        shapeValueListBox.setVisible(isBuoy);
        patternValueListBox.setVisible(isBuoy);
        labelShape.setVisible(isBuoy);
        labelPattern.setVisible(isBuoy);
        if (!isBuoy) {
            shapeValueListBox.setValue(null);
            patternValueListBox.setValue(null);
        }

        validateAndUpdate();
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return nameTextBox;
    }

    @Override
    protected MarkPropertiesDTO getResult() {
        final Color color;
        if (!displayColorTextBox.isValid()) {
            color = new InvalidColor(stringMessages, displayColorTextBox.getValue());
        } else {
            color = displayColorTextBox.getColor();
        }
        final MarkPropertiesDTO markProperties = new MarkPropertiesDTO(id, nameTextBox.getValue(),
                tagsEditor.getValue(), shortNameTextBox.getValue(), color,
                shapeValueListBox.getValue() == null ? "" : shapeValueListBox.getValue().name(),
                patternValueListBox.getValue() == null ? "" : patternValueListBox.getValue().name(),
                markTypeValueListBox.getValue(), /* positioning type */ null);
        return markProperties;
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(7, 2);
        result.setWidget(0, 0, new Label(stringMessages.name()));
        result.setWidget(0, 1, nameTextBox);
        result.setWidget(1, 0, new Label(stringMessages.shortName()));
        result.setWidget(1, 1, shortNameTextBox);
        result.setWidget(2, 0, new Label(stringMessages.color()));
        result.setWidget(2, 1, displayColorTextBox);
        result.setWidget(3, 0, labelShape);
        result.setWidget(3, 1, shapeValueListBox);
        result.setWidget(4, 0, labelPattern);
        result.setWidget(4, 1, patternValueListBox);
        result.setWidget(5, 0, new Label(stringMessages.type()));
        result.setWidget(5, 1, markTypeValueListBox);
        result.setWidget(6, 0, new Label(stringMessages.tags()));
        result.setWidget(6, 1, tagsEditor);
        return result;
    }

}
