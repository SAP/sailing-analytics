package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTOImpl;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Color;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.gwt.adminconsole.URLFieldWithFileUpload;
import com.sap.sse.gwt.client.ColorTextBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DialogUtils;
import com.sap.sse.gwt.client.dialog.DoubleBox;

/**
 * The competitors produced by this dialog will have a <code>null</code>
 * {@link CompetitorWithBoatDTO#getTwoLetterIsoCountryCode() twoLetterIsoCountryCode} and a <code>null</code>
 * {@link CompetitorWithBoatDTO#getCountryName() countryName} because all of these can be derived from a valid
 * {@link CompetitorWithBoatDTO#getThreeLetterIocCountryCode() threeLetterIocCountryCode}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public abstract class CompetitorEditDialog<CompetitorType extends CompetitorDTO> extends DataEntryDialog<CompetitorType> {
    private static final double YARDSTICK_100 = 100.0;
    private static final double YARDSTICK_1000 = 1000.0;
    private final CompetitorType competitorToEdit;
    private final TextBox name;
    private final TextBox shortName;
    private final ColorTextBox displayColorTextBox;
    private final ListBox threeLetterIocCountryCode;
    private final TextBox email;
    private final TextBox searchTag;
    private final StringMessages stringMessages;

    private final URLFieldWithFileUpload flagImageURL;
    private final URLFieldWithFileUpload imageUrlAndUploadComposite;
    private final Label yardstickLabel;
    private final FlowPanel yardstickPanel;
    private final DoubleBox yardstickNumber;
    private double yardstickScale = YARDSTICK_100;
    private boolean enableYardstickScaleDetection = true;
    private boolean yardstickLastModified = true;
    private final DoubleBox timeOnTimeFactor;
    private final DoubleBox timeOnDistanceAllowanceInSecondsPerNauticalMile;
    
    protected static class CompetitorWithoutBoatValidator<CompetitorType extends CompetitorDTO> implements Validator<CompetitorType> {
        protected final StringMessages stringMessages;

        public CompetitorWithoutBoatValidator(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }
        
        @Override
        public String getErrorMessage(CompetitorType valueToValidate) {
            String result = null;
            if (valueToValidate.getName() == null || valueToValidate.getName().isEmpty()) {
                result = stringMessages.pleaseEnterAName();
            } else if (valueToValidate.getColor() != null) {
                Color displayColor = valueToValidate.getColor();
                if (displayColor instanceof CompetitorEditDialog.InvalidColor) {
                    result = displayColor.getAsHtml();
                }
            }
            return result;
        }
    }

    /**
     * Creates an edit dialog for competitors that may be competitors without a boat assigned (not instance of
     * type {@link CompetitorWithBoatDTO}.
     */
    public static CompetitorEditDialog<CompetitorDTO> create(StringMessages stringMessages, CompetitorDTO competitorToEdit,
            DialogCallback<CompetitorDTO> callback) {
        return new CompetitorEditDialog<CompetitorDTO>(stringMessages.editCompetitor(), stringMessages, competitorToEdit,
                new CompetitorEditDialog.CompetitorWithoutBoatValidator<CompetitorDTO>(stringMessages), callback) {
                    @Override
                    protected CompetitorDTO getResult() {
                        return getBaseResult();
                    }
        };
    }
    
    /**
     * The class creates the UI-dialog to type in the data of a competitor.
     * 
     * @param competitorToEdit
     *            The 'competitorToEdit' parameter contains the competitor which should be changed or initialized.
     */
    protected CompetitorEditDialog(String dialogTitle, StringMessages stringMessages, CompetitorType competitorToEdit,
            Validator<CompetitorType> validator,  DialogCallback<CompetitorType> callback) {
        super(dialogTitle, null, stringMessages.ok(), stringMessages.cancel(),
                validator, /* animationEnabled */true, callback);
        this.ensureDebugId("CompetitorEditDialog");
        this.stringMessages = stringMessages;
        this.competitorToEdit = competitorToEdit;
        this.name = createTextBox(competitorToEdit.getName());
        name.ensureDebugId("NameTextBox");
        this.shortName = createTextBox(competitorToEdit.getShortName());
        shortName.ensureDebugId("ShortNameTextBox");
        this.email = createTextBox(competitorToEdit.getEmail());
        this.searchTag = createTextBox(competitorToEdit.getSearchTag());
        this.displayColorTextBox = createColorTextBox(competitorToEdit.getColor()); 
        this.threeLetterIocCountryCode = createListBox(/* isMultipleSelect */ false);
        DialogUtils.makeCountrySelection(this.threeLetterIocCountryCode, competitorToEdit.getThreeLetterIocCountryCode());
        this.flagImageURL = new URLFieldWithFileUpload(stringMessages, "image/*");
        this.flagImageURL.setUri(competitorToEdit.getFlagImageURL());
        this.imageUrlAndUploadComposite = new URLFieldWithFileUpload(stringMessages, "image/*");
        this.imageUrlAndUploadComposite.setUri(competitorToEdit.getImageURL());
        getCancelButton().addClickHandler(clickEvent-> {
            flagImageURL.deleteCurrentFile();
            imageUrlAndUploadComposite.deleteCurrentFile();
        });
        this.yardstickLabel = new Label(stringMessages.yardstickNumber(yardstickScale));
        this.yardstickNumber = createDoubleBox(competitorToEdit.getTimeOnTimeFactor() == null ? null
                : convertYardstickTimeOnTime(competitorToEdit.getTimeOnTimeFactor(), yardstickScale), 10);
        Button yardstickScaleButton = new Button(SafeHtmlUtils.fromSafeConstant(stringMessages.changeScale()));
        yardstickScaleButton.getElement().getStyle().setMarginLeft(3, Unit.PX);
        this.yardstickPanel = new FlowPanel();
        this.yardstickPanel.add(yardstickNumber);
        this.yardstickPanel.add(yardstickScaleButton);
        this.timeOnTimeFactor = createDoubleBox(competitorToEdit.getTimeOnTimeFactor(), 10);
        this.yardstickNumber.addChangeHandler(event -> {
            if (yardstickNumber.getValue() == null) {
                enableYardstickScaleDetection = true;
            }
            detectAndSetYardstickScale();
            convertYardstickToToT();
            yardstickLastModified = true;
        });
        this.timeOnTimeFactor.addChangeHandler(event -> {
            if (timeOnTimeFactor.getValue() != null) {
                yardstickNumber.setValue(convertYardstickTimeOnTime(timeOnTimeFactor.getValue(), yardstickScale), /* fireEvents */ false);
            }
            yardstickLastModified = false;
        });
        yardstickScaleButton.addClickHandler(event -> {
            enableYardstickScaleDetection = false;
            setYardstickScale(yardstickScale == YARDSTICK_1000 ? YARDSTICK_100 : YARDSTICK_1000, /* convertYardstickNumber */ !yardstickLastModified);
            convertYardstickToToT();
        });
        this.timeOnDistanceAllowanceInSecondsPerNauticalMile = createDoubleBox(
                competitorToEdit.getTimeOnDistanceAllowancePerNauticalMile() == null ? null : competitorToEdit
                        .getTimeOnDistanceAllowancePerNauticalMile().asSeconds(), 10);
    }

    private void convertYardstickToToT() {
        if (yardstickNumber.getValue() != null) {
            timeOnTimeFactor.setValue(convertYardstickTimeOnTime(yardstickNumber.getValue(), yardstickScale), /* fireEvents */ false);
        }
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return name;
    }

    /**
     * Encodes an invalid color; can be used 
     * @author Axel Uhl (D043530)
     *
     */
    protected static class InvalidColor implements Color {
        private static final long serialVersionUID = 4012986110898149543L;
        private final String exceptionMessage;
        
        protected InvalidColor(Exception exception, StringMessages stringMessages) {
            this.exceptionMessage = stringMessages.invalidColor(exception.getMessage());
        }

        @Override
        public com.sap.sse.common.Util.Triple<Integer, Integer, Integer> getAsRGB() {
            return null;
        }

        @Override
        public com.sap.sse.common.Util.Triple<Float, Float, Float> getAsHSV() {
            return null;
        }

        @Override
        public String getAsHtml() {
            return exceptionMessage;
        }
        
        @Override
        public Color invert() {
            return null;
        }
    }

    /**
     * Concrete sub-types need to tell which type of {@link CompetitorDTO} object they create; the {@link #getBaseResult} method
     * can be used by sub-types to obtain a {@link CompetitorWithDTO} object that has all values managed by this implementation.
     */
    protected abstract CompetitorType getResult();
    
    protected CompetitorDTO getBaseResult() {
        Color color;
        if (displayColorTextBox.isValid()) {
            color = displayColorTextBox.getColor();
        } else {
            color = new InvalidColor(new IllegalArgumentException(displayColorTextBox.getValue()), stringMessages);
        }
        CompetitorWithBoatDTO result = new CompetitorWithBoatDTOImpl(name.getText(),
                shortName.getText().trim().isEmpty() ? null : shortName.getText(), color,
                email.getText().trim().isEmpty() ? null : email.getText(),
                /* twoLetterIsoCountryCode */ null,
                threeLetterIocCountryCode.getValue(threeLetterIocCountryCode.getSelectedIndex()),
                /* countryName */ null, competitorToEdit.getIdAsString(),
                imageUrlAndUploadComposite.getUri(), flagImageURL.getUri(),
                timeOnTimeFactor.getValue(),
                timeOnDistanceAllowanceInSecondsPerNauticalMile.getValue() == null ? null :
                        new MillisecondsDurationImpl((long) (timeOnDistanceAllowanceInSecondsPerNauticalMile.getValue()*1000)), searchTag.getValue(), null);
        return result;
    }

    /**
     * Sets the yardstick scale to the given value and updates {@link #yardstickLabel}.
     * @param scale {@code double} value to set scale to.
     * @param convertYardstickNumber if {@code true} the current yardstick number will be converted to the new scale.
     */
    protected void setYardstickScale(double scale, boolean convertYardstickNumber) {
        if (convertYardstickNumber && yardstickNumber.getValue() != null) {
            final double norm = yardstickNumber.getValue() / yardstickScale;
            yardstickNumber.setValue(norm * scale, /* fireEvents */ false);
        }
        yardstickScale = scale;
        yardstickLabel.setText(stringMessages.yardstickNumber(yardstickScale));
    }

    /**
     * If {@link #enableYardstickScaleDetection} is set {@link #yardstickScale} will be set to {@code 100} or
     * {@code 1000} depending on which is closer to the current {@link #yardstickNumber}.
     */
    private void detectAndSetYardstickScale() {
        if (enableYardstickScaleDetection) {
            Double number = yardstickNumber.getValue();
            if (number != null) {
                if (number < 550) {
                    setYardstickScale(YARDSTICK_100, false);
                } else {
                    setYardstickScale(YARDSTICK_1000, false);
                }
            }
        }
    }

    /**
     * Converts between yardstick number and time-on-time factor and vice-versa.
     */
    private static double convertYardstickTimeOnTime(double yardstickOrTimeOnTime, double scale) {
        return scale / yardstickOrTimeOnTime;
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(11, 2);
        result.setWidget(0, 0, new Label(stringMessages.name()));
        result.setWidget(0, 1, name);
        result.setWidget(1, 0, new Label(stringMessages.shortName()));
        result.setWidget(1, 1, shortName);
        result.setWidget(2, 0, new Label(stringMessages.nationality()));
        result.setWidget(2, 1, threeLetterIocCountryCode);
        result.setWidget(3, 0, new Label(stringMessages.color()));
        result.setWidget(3, 1, displayColorTextBox);
        result.setWidget(4, 0, new Label(stringMessages.email()));
        result.setWidget(4, 1, email);
        result.setWidget(5, 0, new Label(stringMessages.searchTag()));
        result.setWidget(5, 1, searchTag);
        result.setWidget(6, 0, new Label(stringMessages.flagImageURL()));
        result.setWidget(6, 1, flagImageURL);
        result.setWidget(7, 0, new Label(stringMessages.imageURL()));
        result.setWidget(7, 1, imageUrlAndUploadComposite);
        result.setWidget(8, 0, yardstickLabel);
        result.setWidget(8, 1, yardstickPanel);
        result.setWidget(9, 0, new Label(stringMessages.timeOnTimeFactor()));
        result.setWidget(9, 1, timeOnTimeFactor);
        result.setWidget(10, 0, new Label(stringMessages.timeOnDistanceAllowanceInSecondsPerNauticalMile() +
                " ["+DetailTypeFormatter.getUnit(DetailType.OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE)+"]"));
        result.setWidget(10, 1, timeOnDistanceAllowanceInSecondsPerNauticalMile);
        return result;
    }

    protected CompetitorType getCompetitorToEdit() {
        return competitorToEdit;
    }

    protected StringMessages getStringMessages() {
        return stringMessages;
    }

}
