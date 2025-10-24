package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.EnumSet;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.DataEntryDialogWithDateTimeBox;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.common.client.RandomString;
import com.sap.sailing.gwt.ui.leaderboard.RankingMetricTypeFormatter;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.controls.datetime.DateAndTimeInput;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;
import com.sap.sse.gwt.client.controls.listedit.ListEditorComposite;
import com.sap.sse.gwt.client.dialog.DoubleBox;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;

/**
 * Uses a {@link RegattaDTO} to initialize the view. {@link #getRegattaDTO()} can be used by implementations of
 * {@link #getResult()} to produce a result regatta object and always returns a new {@link RegattaDTO} object.
 * The original {@link RegattaDTO} passed to the constructor is not modified by this dialog.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <T>
 */
public abstract class AbstractRegattaWithSeriesAndFleetsDialog<T> extends DataEntryDialogWithDateTimeBox<T> {
    protected final SailingServiceAsync sailingService; 
    protected StringMessages stringMessages;
    private final RegattaDTO regatta;
    protected final DateAndTimeInput startDateBox;
    protected final DateAndTimeInput endDateBox;
    protected final ListBox scoringSchemeListBox;
    protected final CourseAreaSelection courseAreaSelection;
    protected final ListBox sailingEventsListBox;
    protected final CheckBox useStartTimeInferenceCheckBox;
    protected final CheckBox controlTrackingFromStartAndFinishTimesCheckBox;
    protected final CheckBox autoRestartTrackingUponCompetitorSetChangeCheckBox;
    protected final DoubleBox buoyZoneRadiusInHullLengthsDoubleBox;
    protected final ListEditorComposite<SeriesDTO> seriesEditor;
    private final ListBox rankingMetricListBox;
    protected final ListBox competitorRegistrationTypeListBox;
    protected final Iterable<EventDTO> existingEvents;
    private EventDTO defaultEvent;
    private RegistrationLinkWithQRCode registrationLinkWithQRCode;
    protected final CaptionPanel secretPanel;
    private final UserService userService;

    public AbstractRegattaWithSeriesAndFleetsDialog(final SailingServiceAsync sailingService, UserService userService,
            RegattaDTO regatta, Iterable<SeriesDTO> series, Iterable<EventDTO> existingEvents, EventDTO correspondingEvent,
            String title, String okButton, StringMessages stringMessages, Validator<T> validator, DialogCallback<T> callback) {
        super(title, null, okButton, stringMessages.cancel(), validator, callback);
        this.sailingService = sailingService;
        this.userService = userService;
        this.stringMessages = stringMessages;
        this.regatta = regatta;
        this.defaultEvent = correspondingEvent;
        this.existingEvents = existingEvents;
        rankingMetricListBox = createListBox(/* isMultipleSelect */ false);
        for (RankingMetrics rankingMetricType : RankingMetrics.values()) {
            rankingMetricListBox.addItem(RankingMetricTypeFormatter.format(rankingMetricType, stringMessages), rankingMetricType.name());
            final SelectElement selectElement = rankingMetricListBox.getElement().cast();
            final NodeList<OptionElement> options = selectElement.getOptions();
            options.getItem(options.getLength()-1).setTitle(RankingMetricTypeFormatter.getDescription(rankingMetricType, stringMessages));
        }
        startDateBox = createDateTimeBox(regatta.startDate, Accuracy.MINUTES);
        startDateBox.ensureDebugId("StartDateTimeBox");
        endDateBox = createDateTimeBox(regatta.endDate, Accuracy.MINUTES);
        endDateBox.ensureDebugId("EndDateTimeBox");
        scoringSchemeListBox = createListBox(false);
        scoringSchemeListBox.ensureDebugId("ScoringSchemeListBox");
        for (ScoringSchemeType scoringSchemeType : ScoringSchemeType.values()) {
            scoringSchemeListBox.addItem(ScoringSchemeTypeFormatter.format(scoringSchemeType, stringMessages),
                    String.valueOf(scoringSchemeType.ordinal()));
            if (scoringSchemeType == regatta.scoringScheme) {
                scoringSchemeListBox.setSelectedIndex(scoringSchemeListBox.getItemCount() - 1);
            }
        }
        sailingEventsListBox = createListBox(false);
        sailingEventsListBox.ensureDebugId("EventListBox");
        useStartTimeInferenceCheckBox = createCheckbox(stringMessages.useStartTimeInference());
        useStartTimeInferenceCheckBox.ensureDebugId("UseStartTimeInferenceCheckBox");
        useStartTimeInferenceCheckBox.setValue(regatta.useStartTimeInference);
        controlTrackingFromStartAndFinishTimesCheckBox = createCheckbox(stringMessages.controlTrackingFromStartAndFinishTimes());
        controlTrackingFromStartAndFinishTimesCheckBox.ensureDebugId("ControlTrackingFromStartAndFinishTimesCheckBox");
        controlTrackingFromStartAndFinishTimesCheckBox.setValue(regatta.controlTrackingFromStartAndFinishTimes);
        autoRestartTrackingUponCompetitorSetChangeCheckBox = createCheckbox(stringMessages.autoRestartTrackingUponCompetitorSetChange());
        autoRestartTrackingUponCompetitorSetChangeCheckBox.ensureDebugId("AutoRestartTrackingUponCompetitorSetChangeCheckBox");
        autoRestartTrackingUponCompetitorSetChangeCheckBox.setValue(regatta.autoRestartTrackingUponCompetitorSetChange);
        buoyZoneRadiusInHullLengthsDoubleBox = createDoubleBox(regatta.buoyZoneRadiusInHullLengths, 10);
        buoyZoneRadiusInHullLengthsDoubleBox.ensureDebugId("BuoyZoneRadiusInHullLengthsDoubleBox");
        courseAreaSelection = new CourseAreaSelection(stringMessages, this);
        courseAreaSelection.ensureDebugId("CourseAreaListBox");
        courseAreaSelection.setEnabled(false);
        this.seriesEditor = createSeriesEditor(series);
        setupEventAndCourseAreaListBoxes(stringMessages);
        competitorRegistrationTypeListBox = createListBox(false);
        competitorRegistrationTypeListBox.ensureDebugId("CompetitorRegistrationTypeListBox");
        EnumSet.allOf(CompetitorRegistrationType.class).forEach(t->competitorRegistrationTypeListBox.addItem(t.getLabel(stringMessages), t.name()));
        competitorRegistrationTypeListBox.setSelectedIndex(regatta.competitorRegistrationType.ordinal());
        // Registration Link
        registrationLinkWithQRCode = new RegistrationLinkWithQRCode();
        if (regatta.registrationLinkSecret == null) {
            final String secret = RandomString.createRandomSecret(20);
            registrationLinkWithQRCode.setSecret(secret);
            regatta.registrationLinkSecret = secret;
        } else {
            registrationLinkWithQRCode.setSecret(regatta.registrationLinkSecret);
        }
        // secret panel
        final TextBox secretTextBox = createTextBox(regatta.registrationLinkSecret, 30);
        secretPanel = new CaptionPanel(stringMessages.registrationLinkSecret());
        createSecretPanel(stringMessages, secretPanel, secretTextBox);
    }

    private void createSecretPanel(final StringMessages stringMessages, final CaptionPanel secretPanel,
            final TextBox secretTextBox) {
        final VerticalPanel secretPanelContent = new VerticalPanel();
        secretPanel.add(secretPanelContent);
        // explain Label with description of secret
        final Label secretExplainLabel = new Label(stringMessages.registrationLinkSecretExplain());
        secretPanelContent.add(secretExplainLabel);
        secretExplainLabel.setWordWrap(true);
        // Label
        Label secretLabel = new Label(stringMessages.registrationLinkSecret() + ":");
        // Textbox
        secretTextBox.ensureDebugId("SecretTextBox");
        secretTextBox.addChangeHandler(e -> validateSecret(stringMessages, secretTextBox));
        // Generate-Button
        final Button generateSecretButton = new Button(stringMessages.registrationLinkSecretGenerate(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        final String randomString = RandomString.createRandomSecret(20);
                        registrationLinkWithQRCode.setSecret(randomString);
                        secretTextBox.setText(randomString);
                        validateSecret(stringMessages, secretTextBox);
                    }
                });
        generateSecretButton.ensureDebugId("GenerateSecretButton");
        // add all to grid
        final Grid secretPanelFormGrid = new Grid(1, 3);
        secretPanelFormGrid.setWidget(0, 0, secretLabel);
        secretPanelFormGrid.setWidget(0, 1, secretTextBox);
        secretPanelFormGrid.setWidget(0, 2, generateSecretButton);
        secretPanelContent.add(secretPanelFormGrid);
    }

    private void validateSecret(final StringMessages stringMessages, final TextBox secretTextBox) {
        if (secretTextBox.getText() == null || secretTextBox.getText().equals("")) {
            getStatusLabel().setText(stringMessages.invalidSecret());
            getStatusLabel().setStyleName("errorLabel");
            getOkButton().setEnabled(false);
        }
        else {
            getOkButton().setEnabled(true);
        }
        registrationLinkWithQRCode.setSecret(secretTextBox.getText());
    }

    /**
     * Subclasses that want to display a list box for the ranking metric can use this method to create a tab
     * panel that can be inserted into the grid panel in the {@link #setupAdditionalWidgetsOnPanel(VerticalPanel, Grid)} method.
     */
    protected void insertRankingMetricTabPanel(Grid formGrid) {
        int gridRow = formGrid.insertRow(formGrid.getRowCount());
        formGrid.setWidget(gridRow, 0, new Label(stringMessages.rankingMetric()));
        formGrid.setWidget(gridRow, 1, rankingMetricListBox);
    }

    protected void setRankingMetrics(RegattaDTO dto) {
        dto.rankingMetricType = RankingMetrics.valueOf(getRankingMetricListBox().getValue(getRankingMetricListBox().getSelectedIndex()));
    }

    protected ListBox getRankingMetricListBox() {
        return rankingMetricListBox;
    }
    
   /**
     * @param panel
     *            the panel holding dialog elements
     * @param formGrid
     *            a grid at the top of the <code>panel</code> in which the default label/checkbox pairs are presented;
     *            implementors may use this to {@link Grid#insertRow(int) insert} more rows which then are formatted
     *            properly together with the label/checkbox pairs provided by this class.
     */
    protected abstract void setupAdditionalWidgetsOnPanel(VerticalPanel panel, Grid formGrid);

    protected abstract ListEditorComposite<SeriesDTO> createSeriesEditor(Iterable<SeriesDTO> series);
    
    protected ListEditorComposite<SeriesDTO> getSeriesEditor() {
        return seriesEditor;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        Widget additionalWidget = super.getAdditionalWidget();
        if (additionalWidget != null) {
            panel.add(additionalWidget);
        }
        Grid formGrid = new Grid(12, 2);
        panel.add(formGrid);

        formGrid.setWidget(0, 0, new Label(stringMessages.timeZone() + ":"));
        formGrid.setWidget(0, 1, new Label(DateAndTimeFormatterUtil.getClientTimeZoneAsGMTString()));
        formGrid.setWidget(1, 0, new Label(stringMessages.startDate() + ":"));
        formGrid.setWidget(1, 1, startDateBox);
        formGrid.setWidget(2, 0, new Label(stringMessages.endDate() + ":"));
        formGrid.setWidget(2, 1, endDateBox);
        formGrid.setWidget(3, 0, new Label(stringMessages.scoringSystem() + ":"));
        formGrid.setWidget(3, 1, scoringSchemeListBox);
        formGrid.setWidget(4, 0, new Label(stringMessages.event() + ":"));
        formGrid.setWidget(4, 1, sailingEventsListBox);
        formGrid.setWidget(5, 0, new Label(stringMessages.courseArea() + ":"));
        formGrid.setWidget(5, 1, courseAreaSelection);
        formGrid.setWidget(6, 0, new Label(stringMessages.useStartTimeInference() + ":"));
        formGrid.setWidget(6, 1, useStartTimeInferenceCheckBox);
        formGrid.setWidget(7, 0, new Label(stringMessages.controlTrackingFromStartAndFinishTimes() + ":"));
        formGrid.setWidget(7, 1, controlTrackingFromStartAndFinishTimesCheckBox);
        formGrid.setWidget(8, 0, new Label(stringMessages.autoRestartTrackingUponCompetitorSetChange() + ":"));
        formGrid.setWidget(8, 1, autoRestartTrackingUponCompetitorSetChangeCheckBox);
        formGrid.setWidget(9, 0, new Label(stringMessages.buoyZoneRadiusInHullLengths() + ":"));
        formGrid.setWidget(9, 1, buoyZoneRadiusInHullLengthsDoubleBox);
        formGrid.setWidget(10, 0, new Label(stringMessages.competitorRegistrationType() + ":"));
        formGrid.setWidget(10, 1, competitorRegistrationTypeListBox);

        panel.add(secretPanel);
        setupAdditionalWidgetsOnPanel(panel, formGrid);
        return panel;
    }

    protected void setCourseAreaInRegatta(RegattaDTO regatta) {
        regatta.courseAreas = new ArrayList<>();
        Util.addAll(getSelectedCourseAreas(), regatta.courseAreas);
    }

    public ScoringSchemeType getSelectedScoringSchemeType() {
        int index = scoringSchemeListBox.getSelectedIndex();
        if (index >= 0) {
            return ScoringSchemeType.values()[Integer.valueOf(scoringSchemeListBox.getValue(index))];
        }
        return null;
    }

    private boolean isAnyOfTheCourseAreasInEvent(EventDTO event, Iterable<CourseAreaDTO> courseAreas) {
        final boolean result;
        if (event.getVenue() == null) {
            result = false;
        } else {
            result = Util.containsAny(event.getVenue().getCourseAreas(), courseAreas);
        }
        return result;
    }
    
    private void setupEventAndCourseAreaListBoxes(StringMessages stringMessages) {
        sailingEventsListBox.addItem(stringMessages.selectSailingEvent(), stringMessages.selectSailingEvent());
        final Iterable<EventDTO> eventsSortedByName = Util.stream(existingEvents)
                .filter(event->userService.hasPermission(event, DefaultActions.UPDATE))
                .sorted(Util.naturalNamedComparator(/* caseSensitive */ true))::iterator;
        for (EventDTO event : eventsSortedByName) {
            sailingEventsListBox.addItem(event.getName(), event.getId().toString());
            if (defaultEvent != null) {
                if (defaultEvent.getId().equals(event.getId())) {
                    sailingEventsListBox.setSelectedIndex(sailingEventsListBox.getItemCount() - 1);
                    fillCourseAreaListBox(event);
                    if (event.getVenue().getCourseAreas().size() == 1) {
                        // select the single course area as the default:
                        courseAreaSelection.setSelected(event.getVenue().getCourseAreas().iterator().next(), true);
                    }
                }
            } else {
                if (isAnyOfTheCourseAreasInEvent(event, regatta.courseAreas)) {
                    sailingEventsListBox.setSelectedIndex(sailingEventsListBox.getItemCount() - 1);
                    fillCourseAreaListBox(event);
                }
            }
        }
        sailingEventsListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                onEventSelectionChanged();
            }
        });
    }

    protected void onEventSelectionChanged() {
        setCourseAreaSelection();
    }

    protected void setCourseAreaSelection() {
        final EventDTO selectedEvent = getSelectedEvent();
        courseAreaSelection.removeAll();
        courseAreaSelection.setEnabled(false);
        if (selectedEvent != null) {
            fillCourseAreaListBox(selectedEvent);
        }
    }

    protected void fillCourseAreaListBox(EventDTO selectedEvent) {
        for (final CourseAreaDTO courseAreaInEvent : selectedEvent.getVenue().getCourseAreas()) {
            courseAreaSelection.addCourseArea(courseAreaInEvent);
        }
        for (final CourseAreaDTO courseAreaForRegatta : regatta.courseAreas) {
            courseAreaSelection.setSelected(courseAreaForRegatta, true);
        }
        courseAreaSelection.setEnabled(true);
    }

    public EventDTO getSelectedEvent() {
        EventDTO result = null;
        int selIndex = sailingEventsListBox.getSelectedIndex();
        if (selIndex > 0) { // the zero index represents the 'no selection' text
            String itemValue = sailingEventsListBox.getValue(selIndex);
            for (EventDTO eventDTO : existingEvents) {
                if (eventDTO.getId().toString().equals(itemValue)) {
                    result = eventDTO;
                    break;
                }
            }
        }
        return result;
    }

    public Iterable<CourseAreaDTO> getSelectedCourseAreas() {
        return courseAreaSelection.getSelectedCourseAreas();
    }
    
    public RegattaDTO getRegattaDTO(String name) {
        RegattaDTO result = new RegattaDTO(name, getSelectedScoringSchemeType());
        result.startDate = startDateBox.getValue();
        result.endDate = endDateBox.getValue();
        result.useStartTimeInference = useStartTimeInferenceCheckBox.getValue();
        result.controlTrackingFromStartAndFinishTimes = controlTrackingFromStartAndFinishTimesCheckBox.getValue();
        result.autoRestartTrackingUponCompetitorSetChange = autoRestartTrackingUponCompetitorSetChangeCheckBox.getValue();
        result.buoyZoneRadiusInHullLengths = buoyZoneRadiusInHullLengthsDoubleBox.getValue();
        setCourseAreaInRegatta(result);
        result.series = getSeriesEditor().getValue();
        result.competitorRegistrationType = CompetitorRegistrationType.valueOf(competitorRegistrationTypeListBox.getSelectedValue());
        result.registrationLinkSecret = registrationLinkWithQRCode.getSecret();
        return result;
    }

}
