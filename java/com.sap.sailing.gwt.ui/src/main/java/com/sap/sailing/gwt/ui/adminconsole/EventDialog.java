package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.DataEntryDialogWithBootstrap;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.listedit.StringListInlineEditorComposite;
import com.sap.sailing.gwt.ui.shared.BetterDateTimeBox;
import com.sap.sailing.gwt.ui.shared.CourseAreaDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;

public abstract class EventDialog extends DataEntryDialogWithBootstrap<EventDTO> {
    private final AdminConsoleResources resources = GWT.create(AdminConsoleResources.class);
    protected StringMessages stringMessages;
    protected TextBox nameEntryField;
    protected TextArea descriptionEntryField;
    protected TextBox venueEntryField;
    protected BetterDateTimeBox startDateBox;
    protected BetterDateTimeBox endDateBox;
    protected CheckBox isPublicCheckBox;
    protected UUID id;
    protected TextBox officialWebsiteURLEntryField;
    protected TextBox logoImageURLEntryField;
    protected StringListInlineEditorComposite courseAreaNameList;
    protected StringListInlineEditorComposite imageURLList;
    protected StringListInlineEditorComposite videoURLList;
    protected StringListInlineEditorComposite sponsorImageURLList;
    
    /**
     * Allows subclasses to "park" a set of associated leaderboard groups for re-attachment to the new {@link EventDTO} produced
     * by {@link #getResult()}.
     */
    private final Iterable<LeaderboardGroupDTO> leaderboardGroups;

    protected static class EventParameterValidator implements Validator<EventDTO> {

        private StringMessages stringMessages;
        private ArrayList<EventDTO> existingEvents;

        public EventParameterValidator(StringMessages stringMessages, Collection<EventDTO> existingEvents) {
            this.stringMessages = stringMessages;
            this.existingEvents = new ArrayList<EventDTO>(existingEvents);
        }

        @Override
        public String getErrorMessage(EventDTO eventToValidate) {
            String errorMessage = null;
            boolean nameNotEmpty = eventToValidate.getName() != null && eventToValidate.getName().length() > 0;
            boolean venueNotEmpty = eventToValidate.venue.getName() != null && eventToValidate.venue.getName().length() > 0;
            boolean courseAreaNotEmpty = eventToValidate.venue.getCourseAreas() != null && eventToValidate.venue.getCourseAreas().size() > 0;

            if (courseAreaNotEmpty) {
                for (CourseAreaDTO courseArea : eventToValidate.venue.getCourseAreas()) {
                    courseAreaNotEmpty = courseArea.getName() != null && courseArea.getName().length() > 0;
                    if (!courseAreaNotEmpty)
                        break;
                }
            }

            boolean unique = true;
            for (EventDTO event : existingEvents) {
                if (event.getName().equals(eventToValidate.getName())) {
                    unique = false;
                    break;
                }
            }

            Date startDate = eventToValidate.startDate;
            Date endDate = eventToValidate.endDate;
            String datesErrorMessage = null;
            // remark: startDate == null and endDate == null is valid
            if(startDate != null && endDate != null) {
                if(startDate.after(endDate)) {
                    datesErrorMessage = stringMessages.pleaseEnterStartAndEndDate(); 
                }
            } else if((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
                datesErrorMessage = stringMessages.pleaseEnterStartAndEndDate();
            }
            
            if(datesErrorMessage != null) {
                errorMessage = datesErrorMessage;
            } else if (!nameNotEmpty) {
                errorMessage = stringMessages.pleaseEnterAName();
            } else if (!venueNotEmpty) {
                errorMessage = stringMessages.pleaseEnterNonEmptyVenue();
            } else if (!courseAreaNotEmpty) {
                errorMessage = stringMessages.pleaseEnterNonEmptyCourseArea();
            } else if (!unique) {
                errorMessage = stringMessages.eventWithThisNameAlreadyExists();
            }

            return errorMessage;
        }

    }

    /**
     * @param leaderboardGroups even though not editable in this dialog, this parameter gives an editing subclass a chance to "park" the leaderboard group
     * assignments for re-association with the new {@link EventDTO} created by the {@link #getResult} method.
     */
    public EventDialog(EventParameterValidator validator, StringMessages stringMessages, Iterable<LeaderboardGroupDTO> leaderboardGroups,
            DialogCallback<EventDTO> callback) {
        super(stringMessages.event(), null, stringMessages.ok(), stringMessages.cancel(), validator,
                callback);
        this.leaderboardGroups = leaderboardGroups;
        this.stringMessages = stringMessages;
        final ValueChangeHandler<List<String>> valueChangeHandler = new ValueChangeHandler<List<String>>() {
            @Override
            public void onValueChange(ValueChangeEvent<List<String>> event) {
                validate();
            }
        };
        courseAreaNameList = new StringListInlineEditorComposite(Collections.<String> emptyList(),
                new StringListInlineEditorComposite.ExpandedUi(stringMessages, resources.removeIcon(), /* suggestValues */
                        SuggestedCourseAreaNames.suggestedCourseAreaNames, 50));
        courseAreaNameList.addValueChangeHandler(valueChangeHandler);
        final List<String> imageSuggestionURLs = Arrays.asList(new String[] { "http://", "https://", "http://www.", "https://www" });
        imageURLList = new StringListInlineEditorComposite(Collections.<String> emptyList(),
                new StringListInlineEditorComposite.ExpandedUi(stringMessages, resources.removeIcon(),
                /* suggestValues */ imageSuggestionURLs, 80));
        imageURLList.addValueChangeHandler(valueChangeHandler);
        List<String> videoURLSuggestions = new ArrayList<>(imageSuggestionURLs);
        videoURLSuggestions.add("http://www.youtube.com/watch?v=");
        videoURLList = new StringListInlineEditorComposite(Collections.<String> emptyList(),
                new StringListInlineEditorComposite.ExpandedUi(stringMessages, resources.removeIcon(),
                /* suggestValues */ videoURLSuggestions, 80));
        videoURLList.addValueChangeHandler(valueChangeHandler);
        sponsorImageURLList = new StringListInlineEditorComposite(Collections.<String> emptyList(),
                new StringListInlineEditorComposite.ExpandedUi(stringMessages, resources.removeIcon(),
                /* suggestValues */ imageSuggestionURLs, 80));
        sponsorImageURLList.addValueChangeHandler(valueChangeHandler);
    }

    @Override
    protected EventDTO getResult() {
        EventDTO result = new EventDTO();
        for (LeaderboardGroupDTO lg : leaderboardGroups) {
            result.addLeaderboardGroup(lg);
        }
        result.setName(nameEntryField.getText());
        result.setDescription(descriptionEntryField.getText());
        result.setOfficialWebsiteURL(officialWebsiteURLEntryField.getText().trim().isEmpty() ? null : officialWebsiteURLEntryField.getText().trim());
        result.setLogoImageURL(logoImageURLEntryField.getText().trim().isEmpty() ? null : logoImageURLEntryField.getText().trim());
        result.startDate = startDateBox.getValue();
        result.endDate = endDateBox.getValue();
        result.isPublic = isPublicCheckBox.getValue();
        result.id = id;

        List<CourseAreaDTO> courseAreas = new ArrayList<CourseAreaDTO>();
        for (String courseAreaName : courseAreaNameList.getValue()) {
            CourseAreaDTO courseAreaDTO = new CourseAreaDTO();
            courseAreaDTO.setName(courseAreaName);
            courseAreas.add(courseAreaDTO);
        }
        for (String imageURL : imageURLList.getValue()) {
            result.addImageURL(imageURL);
        }
        for (String videoURL : videoURLList.getValue()) {
            result.addVideoURL(videoURL);
        }
        for (String sponsorImageURL : sponsorImageURLList.getValue()) {
            result.addSponsorImageURL(sponsorImageURL);
        }
        result.venue = new VenueDTO(venueEntryField.getText(), courseAreas);
        return result;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        Widget additionalWidget = super.getAdditionalWidget();
        if (additionalWidget != null) {
            panel.add(additionalWidget);
        }

        Grid formGrid = new Grid(8, 2);
        panel.add(formGrid);

        formGrid.setWidget(0,  0, new Label(stringMessages.name() + ":"));
        formGrid.setWidget(0, 1, nameEntryField);
        formGrid.setWidget(1,  0, new Label(stringMessages.description() + ":"));
        formGrid.setWidget(1, 1, descriptionEntryField);
        formGrid.setWidget(2, 0, new Label(stringMessages.venue() + ":"));
        formGrid.setWidget(2, 1, venueEntryField);
        formGrid.setWidget(3, 0, new Label(stringMessages.startDate() + ":"));
        formGrid.setWidget(3, 1, startDateBox);
        formGrid.setWidget(4, 0, new Label(stringMessages.endDate() + ":"));
        formGrid.setWidget(4, 1, endDateBox);
        formGrid.setWidget(5, 0, new Label(stringMessages.isPublic() + ":"));
        formGrid.setWidget(5, 1, isPublicCheckBox);
        formGrid.setWidget(6, 0, new Label(stringMessages.eventOfficialWebsiteURL() + ":"));
        formGrid.setWidget(6, 1, officialWebsiteURLEntryField);
        formGrid.setWidget(7, 0, new Label(stringMessages.eventLogoImageURL() + ":"));
        formGrid.setWidget(7, 1, logoImageURLEntryField);

        panel.add(createHeadlineLabel(stringMessages.courseAreas()));
        panel.add(courseAreaNameList);
        panel.add(createHeadlineLabel(stringMessages.imageURLs()));
        panel.add(imageURLList);
        panel.add(createHeadlineLabel(stringMessages.videoURLs()));
        panel.add(videoURLList);
        panel.add(createHeadlineLabel(stringMessages.sponsorImageURLs()));
        panel.add(sponsorImageURLList);
        return panel;
    }

    @Override
    public void show() {
        super.show();
        nameEntryField.setFocus(true);
    }

}
