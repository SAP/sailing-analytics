package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import java.io.IOException;
import java.util.Arrays;
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
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sailing.gwt.ui.shared.racemap.Pattern;
import com.sap.sailing.gwt.ui.shared.racemap.Shape;
import com.sap.sse.common.Color;
import com.sap.sse.gwt.client.ColorTextBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class MarkTemplateEditDialog extends DataEntryDialog<MarkTemplateDTO> {
    private final TextBox nameTextBox;
    private final ColorTextBox displayColorTextBox;
    private final TextBox shortNameTextBox;
    private final ValueListBox<MarkType> markTypeValueListBox;
    private final ValueListBox<Pattern> patternValueListBox;
    private final ValueListBox<Shape> shapeValueListBox;
    private final StringMessages stringMessages;
    private Label labelShape;
    private Label labelPattern;

    public MarkTemplateEditDialog(final StringMessages stringMessages, MarkTemplateDTO markTemplateToEdit,
            DialogCallback<MarkTemplateDTO> callback) {
        super(stringMessages.edit() + " " + stringMessages.markTemplates(), null, stringMessages.ok(),
                stringMessages.cancel(), new Validator<MarkTemplateDTO>() {
                    @Override
                    public String getErrorMessage(MarkTemplateDTO valueToValidate) {
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
        this.ensureDebugId("MarkTemplateToEditEditDialog");
        this.stringMessages = stringMessages;
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
        this.nameTextBox = createTextBox(markTemplateToEdit.getName());
        this.shortNameTextBox = createTextBox(markTemplateToEdit.getCommonMarkProperties().getShortName());
        this.markTypeValueListBox.setValue(markTemplateToEdit.getCommonMarkProperties().getType() != null
                ? markTemplateToEdit.getCommonMarkProperties().getType()
                : MarkType.values()[0]);
        markTypeValueListBox.setAcceptableValues(Arrays.asList(MarkType.values()));
        markTypeValueListBox.addValueChangeHandler(v -> handleMarkTypeChange());
        this.displayColorTextBox = createColorTextBox(markTemplateToEdit.getCommonMarkProperties().getColor());
        shapeValueListBox.setValue(null);
        shapeValueListBox.setAcceptableValues(Arrays.asList(Shape.values()));
        patternValueListBox.setValue(null);
        patternValueListBox.setAcceptableValues(Arrays.asList(Pattern.values()));
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
    protected MarkTemplateDTO getResult() {
        final Color color;
        if (!displayColorTextBox.isValid()) {
            color = new InvalidColor(stringMessages, displayColorTextBox.getValue());
        } else {
            color = displayColorTextBox.getColor();
        }
        final MarkTemplateDTO markTemplate = new MarkTemplateDTO(UUID.randomUUID(), nameTextBox.getValue(),
                shortNameTextBox.getValue(), color,
                shapeValueListBox.getValue() == null ? "" : shapeValueListBox.getValue().name(),
                patternValueListBox.getValue() == null ? "" : patternValueListBox.getValue().name(),
                markTypeValueListBox.getValue());
        return markTemplate;
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(6, 2);
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
        return result;
    }

}
