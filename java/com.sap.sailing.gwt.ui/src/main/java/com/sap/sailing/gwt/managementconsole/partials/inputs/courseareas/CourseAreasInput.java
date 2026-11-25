package com.sap.sailing.gwt.managementconsole.partials.inputs.courseareas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.managementconsole.places.UiUtils;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sailing.gwt.ui.adminconsole.SuggestedCourseAreaNames;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;

/**
 * Management-console course-area editor. It shows the event's course areas as editable {@link CourseAreaRow rows}
 * (each with a name and optional latitude/longitude/radius geometry) and, behind a "copy from another event"
 * disclosure, lets the user append course areas taken from another event. That other event may be a local one or one
 * fetched from a remote SAP Sailing server (with an optional bearer token). Copied course areas keep the original
 * name and geometry but receive fresh {@link UUID identities} so that they become independent copies.
 */
public class CourseAreasInput extends Composite implements HasValue<List<CourseAreaDTO>> {

    /**
     * Supplies the events offered as copy sources. The management-console activity backs this with the
     * {@code EventService} so that this widget stays free of any direct service dependency.
     */
    public interface DataProvider {
        void getLocalEvents(AsyncCallback<List<EventDTO>> callback);
        void getRemoteEvents(String baseUrl, String bearerTokenOrNull, AsyncCallback<List<EventDTO>> callback);
    }

    private final StringMessages stringMessages = GWT.create(StringMessages.class);
    private final ManagementConsoleResources appResources = ManagementConsoleResources.INSTANCE;
    private final List<CourseAreaRow> rows = new ArrayList<>();
    private final Map<String, EventDTO> eventsCache = new HashMap<>();
    private final FlowPanel rowsPanel = new FlowPanel();
    private final RadioButton localRadio;
    private final RadioButton remoteRadio;
    private final FlowPanel remoteControls = new FlowPanel();
    private final TextBox remoteUrlBox = new TextBox();
    private final TextBox bearerTokenBox = new TextBox();
    private final SimpleBusyIndicator busyIndicator = new SimpleBusyIndicator(/* busy */ false, /* scale */ 0.7f);
    private final ListBox eventDropDown = new ListBox();
    private final MultiWordSuggestOracle nameOracle = createNameOracle();
    private DataProvider dataProvider;

    public CourseAreasInput() {
        CourseAreasInputResources.INSTANCE.style().ensureInjected();
        final FlowPanel container = new FlowPanel();
        container.add(rowsPanel);
        // "Add course area" control; focus the freshly added name field so the user can type immediately
        final Anchor addAnchor = new Anchor(stringMessages.addCourseArea());
        addAnchor.addStyleName(appResources.icons().icon());
        addAnchor.addStyleName(appResources.icons().iconAdd());
        addAnchor.addClickHandler(e -> addRow(new CourseAreaDTO(UUID.randomUUID(), "")).focusName());
        container.add(addAnchor);
        // Collapsible "copy course areas from another event" section
        final Anchor copyToggle = new Anchor(stringMessages.copyCourseAreasFromEvent());
        copyToggle.addStyleName(appResources.icons().icon());
        copyToggle.addStyleName(appResources.icons().iconDropdownChev());
        final FlowPanel copyPanel = new FlowPanel();
        copyPanel.addStyleName(CourseAreasInputResources.INSTANCE.style().copyPanel());
        copyPanel.setVisible(/* visible */ false);
        copyToggle.addClickHandler(e -> toggleCopyPanel(copyPanel));
        // Source selection: local events vs. a remote server
        localRadio = new RadioButton("courseAreaCopySource", stringMessages.localEvents());
        remoteRadio = new RadioButton("courseAreaCopySource", stringMessages.copyCourseAreasFromAnotherUrl());
        localRadio.setValue(/* checked */ true);
        final FlowPanel sourceRow = new FlowPanel();
        sourceRow.addStyleName(CourseAreasInputResources.INSTANCE.style().buttonRow());
        sourceRow.add(localRadio);
        sourceRow.add(remoteRadio);
        copyPanel.add(sourceRow);
        localRadio.addValueChangeHandler(event -> onSourceChanged());
        remoteRadio.addValueChangeHandler(event -> onSourceChanged());
        // Remote server controls, revealed only when the remote source is chosen; stacked as full-width fields
        remoteControls.addStyleName(CourseAreasInputResources.INSTANCE.style().copyRow());
        remoteUrlBox.setValue(SharedLandscapeConstants.DEFAULT_SAILING_SERVER_URL);
        remoteUrlBox.setTitle(stringMessages.helptextCopySource());
        bearerTokenBox.getElement().setPropertyString("placeholder", stringMessages.bearerTokenPlaceholder());
        bearerTokenBox.setTitle(stringMessages.helptextBearerToken());
        remoteControls.add(labelled(stringMessages.copyCourseAreasFromAnotherUrl(), remoteUrlBox));
        // "Load" fetches the events from the remote server
        final Button loadButton = new Button(stringMessages.loadRemoteEvents());
        loadButton.addStyleName(appResources.style().button());
        loadButton.addStyleName(appResources.style().primary());
        loadButton.addClickHandler(e -> loadEvents());
        final FlowPanel loadButtonRow = new FlowPanel();
        loadButtonRow.addStyleName(CourseAreasInputResources.INSTANCE.style().buttonRow());
        loadButtonRow.add(loadButton);
        loadButtonRow.add(busyIndicator);
        remoteControls.add(loadButtonRow);
        // Largely invisible credentials disclosure: only needed in the rare case that the remote server uses a
        // separate security service, so the bearer-token field stays hidden (and takes no space) until requested
        final FlowPanel credentialsPanel = new FlowPanel();
        credentialsPanel.add(labelled(stringMessages.credentials(), bearerTokenBox));
        credentialsPanel.setVisible(/* visible */ false);
        final Anchor credentialsToggle = new Anchor(stringMessages.credentials());
        credentialsToggle.addStyleName(appResources.icons().icon());
        credentialsToggle.addStyleName(appResources.icons().iconDropdownChev());
        credentialsToggle.addStyleName(CourseAreasInputResources.INSTANCE.style().credentialsToggle());
        credentialsToggle.setTitle(stringMessages.helptextBearerToken());
        credentialsToggle.addClickHandler(e -> credentialsPanel.setVisible(!credentialsPanel.isVisible()));
        remoteControls.add(credentialsToggle);
        remoteControls.add(credentialsPanel);
        remoteControls.setVisible(/* visible */ false);
        busyIndicator.setBusy(/* busy */ false);
        copyPanel.add(remoteControls);
        // Event picker plus the append/clear actions, stacked: the dropdown then a button row
        eventDropDown.addItem(stringMessages.pleaseSelectAnEvent());
        final Button addFromEventButton = new Button(stringMessages.selectCourseAreas());
        addFromEventButton.addStyleName(appResources.style().button());
        addFromEventButton.addStyleName(appResources.style().primary());
        addFromEventButton.addClickHandler(e -> openPickerForSelectedEvent());
        final Button clearAllButton = new Button(stringMessages.clearAllCourseAreas());
        clearAllButton.addStyleName(appResources.style().button());
        clearAllButton.addClickHandler(e -> clearAllRows());
        final FlowPanel eventRow = new FlowPanel();
        eventRow.addStyleName(CourseAreasInputResources.INSTANCE.style().copyRow());
        eventRow.add(labelled(stringMessages.event(), eventDropDown));
        final FlowPanel eventButtonRow = new FlowPanel();
        eventButtonRow.addStyleName(CourseAreasInputResources.INSTANCE.style().buttonRow());
        eventButtonRow.add(addFromEventButton);
        eventButtonRow.add(clearAllButton);
        eventRow.add(eventButtonRow);
        copyPanel.add(eventRow);
        container.add(copyToggle);
        container.add(copyPanel);
        initWidget(container);
    }

    public void setDataProvider(final DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    private FlowPanel labelled(final String labelText, final com.google.gwt.user.client.ui.Widget field) {
        final FlowPanel wrapper = new FlowPanel();
        wrapper.addStyleName(appResources.style().flexItemAutoWidth());
        final InlineLabel label = new InlineLabel(labelText);
        label.addStyleName(appResources.style().label());
        wrapper.add(label);
        wrapper.add(field);
        return wrapper;
    }

    private void toggleCopyPanel(final FlowPanel copyPanel) {
        final boolean nowVisible = !copyPanel.isVisible();
        copyPanel.setVisible(nowVisible);
        // When opening with the (default) local source selected, populate the event dropdown right away
        if (nowVisible && localRadio.getValue()) {
            loadEvents();
        }
    }

    private void onSourceChanged() {
        remoteControls.setVisible(remoteRadio.getValue());
        // Local events can be loaded right away; remote events wait for the explicit load action
        if (localRadio.getValue()) {
            loadEvents();
        }
    }

    private void loadEvents() {
        if (dataProvider != null) {
            busyIndicator.setBusy(/* busy */ true);
            eventDropDown.setEnabled(/* enabled */ false);
            final AsyncCallback<List<EventDTO>> callback = new AsyncCallback<List<EventDTO>>() {
                @Override
                public void onSuccess(final List<EventDTO> result) {
                    busyIndicator.setBusy(/* busy */ false);
                    populateEventDropDown(result);
                    eventDropDown.setEnabled(/* enabled */ true);
                }
                @Override
                public void onFailure(final Throwable caught) {
                    busyIndicator.setBusy(/* busy */ false);
                    eventDropDown.clear();
                    eventDropDown.addItem(caught.getMessage());
                }
            };
            if (localRadio.getValue()) {
                dataProvider.getLocalEvents(callback);
            } else {
                final String bearerTokenOrNull = bearerTokenBox.getValue().trim().isEmpty() ? null
                        : bearerTokenBox.getValue().trim();
                dataProvider.getRemoteEvents(remoteUrlBox.getValue().trim(), bearerTokenOrNull, callback);
            }
        }
    }

    private void populateEventDropDown(final List<EventDTO> events) {
        eventDropDown.clear();
        eventsCache.clear();
        eventDropDown.addItem(stringMessages.pleaseSelectAnEvent());
        final List<EventDTO> sortedEvents = new ArrayList<>(events);
        sortedEvents.sort(Comparator.comparing(EventDTO::getName));
        for (final EventDTO event : sortedEvents) {
            eventsCache.put(event.getId().toString(), event);
            eventDropDown.addItem(event.getName(), event.getId().toString());
        }
    }

    private void openPickerForSelectedEvent() {
        final int selectedIndex = eventDropDown.getSelectedIndex();
        final EventDTO selectedEvent = selectedIndex > 0
                ? eventsCache.get(eventDropDown.getValue(selectedIndex)) : null;
        final VenueDTO venue = selectedEvent != null ? selectedEvent.getVenue() : null;
        if (venue != null && venue.getCourseAreas() != null && !venue.getCourseAreas().isEmpty()) {
            new CourseAreaPickerDialog(venue.getCourseAreas(), stringMessages, this::appendCopies).showCentered();
        }
    }

    private void appendCopies(final List<CourseAreaDTO> selectedCourseAreas) {
        for (final CourseAreaDTO source : selectedCourseAreas) {
            final Position centerPosition = source.getCenterPosition();
            final Distance radius = source.getRadius();
            addRow(new CourseAreaDTO(UUID.randomUUID(), source.getName(), centerPosition, radius));
        }
    }

    private CourseAreaRow addRow(final CourseAreaDTO courseArea) {
        final CourseAreaRow row = new CourseAreaRow(courseArea, stringMessages, nameOracle, this::removeRow);
        rows.add(row);
        rowsPanel.add(row);
        return row;
    }

    private void removeRow(final CourseAreaRow row) {
        rows.remove(row);
        rowsPanel.remove(row);
    }

    private static MultiWordSuggestOracle createNameOracle() {
        final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
        final List<String> names = new ArrayList<>();
        for (final String name : SuggestedCourseAreaNames.suggestedCourseAreaNames) {
            names.add(name);
        }
        oracle.addAll(names);
        oracle.setDefaultSuggestionsFromText(names);
        return oracle;
    }

    private void clearAllRows() {
        rows.clear();
        rowsPanel.clear();
    }

    @Override
    public List<CourseAreaDTO> getValue() {
        final List<CourseAreaDTO> result = new ArrayList<>();
        for (final CourseAreaRow row : rows) {
            if (UiUtils.isNotBlank(row.getName())) {
                result.add(row.getValue());
            }
        }
        return result;
    }

    @Override
    public void setValue(final List<CourseAreaDTO> value) {
        setValue(value, /* fireEvents */ false);
    }

    @Override
    public void setValue(final List<CourseAreaDTO> value, final boolean fireEvents) {
        clearAllRows();
        if (value != null) {
            for (final CourseAreaDTO courseArea : value) {
                addRow(courseArea);
            }
        }
        // Always keep at least one empty row so the user can start typing immediately
        if (rows.isEmpty()) {
            addRow(new CourseAreaDTO(UUID.randomUUID(), ""));
        }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<List<CourseAreaDTO>> handler) {
        return null;
    }
}
