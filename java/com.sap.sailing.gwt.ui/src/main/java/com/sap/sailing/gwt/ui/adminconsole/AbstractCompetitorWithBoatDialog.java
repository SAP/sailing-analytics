package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTOImpl;
import com.sap.sailing.gwt.common.client.suggestion.BoatClassMasterdataSuggestOracle;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Color;
import com.sap.sse.common.Distance;
import com.sap.sse.gwt.client.ColorTextBox;

/**
 * An abstract base class for dialogs which can handle a competitor AND a boat together 
 * @author Frank Mittag
 * 
 */
public abstract class AbstractCompetitorWithBoatDialog extends CompetitorEditDialog<CompetitorWithBoatDTO>{
    protected final SuggestBox boatClassNameTextBox;
    protected final TextBox sailIdTextBox;
    protected final TextBox boatNameTextBox;
    protected final ColorTextBox boatDisplayColorTextBox;
    
    protected static class CompetitorWithBoatValidator extends CompetitorWithoutBoatValidator<CompetitorWithBoatDTO> {
        protected final StringMessages stringMessages;
        private final BoatDTO originalBoat;

        public CompetitorWithBoatValidator(StringMessages stringMessages, BoatDTO originalBoat) {
            super(stringMessages);
            this.stringMessages = stringMessages;
            this.originalBoat = originalBoat;
        }
        
        @Override
        public String getErrorMessage(CompetitorWithBoatDTO competitorToValidate) {
            String result = super.getErrorMessage(competitorToValidate);
            if (result == null) {
                BoatDTO boatToValidate = competitorToValidate.getBoat();
                boolean invalidSailId = boatToValidate.getSailId() == null || boatToValidate.getSailId().isEmpty();
                boolean invalidName = boatToValidate.getName() == null || boatToValidate.getName().isEmpty();
                if (invalidSailId && invalidName) {
                    result = stringMessages.pleaseEnterASailNumberOrABoatName();
                } else if (boatToValidate.getColor() != null && boatToValidate.getColor() instanceof InvalidColor) {
                    result = boatToValidate.getColor().getAsHtml();
                } else if (boatClassWasChanged(boatToValidate, originalBoat) && isBoatClassInvalid(boatToValidate)) {
                    // only validate if boat class has changed and the originalBoat actually exists and is not to be
                    // created here
                    result = stringMessages.pleaseEnterABoatClass();
                }
            }
            return result;
        }

        private boolean isBoatClassInvalid(BoatDTO boatToValidate) {
            return boatToValidate.getBoatClass().getName() == null || boatToValidate.getBoatClass().getName().isEmpty();
        }

        private boolean boatClassWasChanged(BoatDTO boatToValidate, BoatDTO originalBoat) {
            boolean changed = false;
            if (boatToValidate.getBoatClass() == null && originalBoat.getBoatClass() != null) {
                // boat class has been added
                changed = true;
            } else if (boatToValidate.getBoatClass() != null && originalBoat.getBoatClass() == null) {
                //
                changed = true;
            } else if (boatToValidate.getBoatClass() == null && originalBoat.getBoatClass() == null) {
                // this is a create dialog, the boat class was just created
                changed = true;
            } else if (originalBoat.getBoatClass().getName()
                    .equals(boatToValidate.getBoatClass().getName())) {
                // boat class exists and stayed the same
                changed = false;
            }
            return changed;
        }
    }
    
    /**
     * The class creates the UI-dialog to type in the Data about a competitor.
     * 
     * @param competitorWithBoat
     *            The 'competitorWithBoat' parameter contains the competitor which should be changed or initialized.
     * @param boatClass
     *            The boat class is the default shown boat class for new boats. Set <code>null</code> if your boat is
     *            already initialized or you don't want a default boat class.
     * @param The
     *            {@link Validator} for competitor with boat
     */
    public AbstractCompetitorWithBoatDialog(String dialogTitle, StringMessages stringMessages,
            CompetitorWithBoatDTO competitorWithBoat, DialogCallback<CompetitorWithBoatDTO> callback, String boatClass,
            CompetitorWithoutBoatValidator<CompetitorWithBoatDTO> validator) {
        super(dialogTitle, stringMessages, competitorWithBoat, validator, callback);
        final BoatDTO boat = competitorWithBoat.getBoat();
        this.boatClassNameTextBox = createSuggestBox(new BoatClassMasterdataSuggestOracle());
        boatClassNameTextBox.ensureDebugId("BoatClassNameSuggestBox");
        if (boat.getBoatClass() != null) {
            boatClassNameTextBox.setValue(boat.getBoatClass().getName());
            setBoatClassNameEnabled(boatClassNameTextBox, false);
        } else {
            boatClassNameTextBox.setValue(boatClass); // widgets have to accept null values here
        }
        this.boatNameTextBox = createTextBox(boat.getName());
        boatNameTextBox.ensureDebugId("BoatNameTextBox");
        this.boatDisplayColorTextBox = createColorTextBox(boat.getColor());
        this.sailIdTextBox = createTextBox(boat.getSailId());
        sailIdTextBox.ensureDebugId("SailIdTextBox");
    }

    public AbstractCompetitorWithBoatDialog(String dialogTitle, StringMessages stringMessages,
            CompetitorWithBoatDTO competitorWithBoat, DialogCallback<CompetitorWithBoatDTO> callback,
            String boatClass) {
        this(dialogTitle, stringMessages, competitorWithBoat, callback, boatClass,
                new AbstractCompetitorWithBoatDialog.CompetitorWithBoatValidator(stringMessages,
                        competitorWithBoat.getBoat()));
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return super.getInitialFocusWidget();
    }

    protected BoatDTO getBoat() {
        BoatDTO result = null;
        Color boatColor;
        if (boatDisplayColorTextBox.isValid()) {
            boatColor = boatDisplayColorTextBox.getColor();
        } else {
            boatColor = new InvalidColor(new IllegalArgumentException(boatDisplayColorTextBox.getValue()),
                    getStringMessages());
        }
        BoatClassDTO boatClass = new BoatClassDTO(boatClassNameTextBox.getValue(), Distance.NULL, Distance.NULL);
        result = new BoatDTO(getCompetitorToEdit().getBoat().getIdAsString(), boatNameTextBox.getValue(), boatClass,
                sailIdTextBox.getValue(), boatColor);
        return result;
    }

    @Override
    protected CompetitorWithBoatDTO getResult() {
        CompetitorDTO c = getBaseResult();
        BoatDTO boat = getBoat();
        CompetitorWithBoatDTO result = new CompetitorWithBoatDTOImpl(c.getName(), c.getShortName(), c.getColor(), c.getEmail(),
                c.getTwoLetterIsoCountryCode(), c.getThreeLetterIocCountryCode(), c.getCountryName(),
                getCompetitorToEdit().getIdAsString(),
                c.getImageURL(), c.getFlagImageURL(),
                c.getTimeOnTimeFactor(), c.getTimeOnDistanceAllowancePerNauticalMile(), c.getSearchTag(),
                boat);
        return result;
    }

    protected void setBoatClassNameEnabled(SuggestBox boatClassNameTextBox, boolean enabled) {
        boatClassNameTextBox.setEnabled(enabled);
    }

}
