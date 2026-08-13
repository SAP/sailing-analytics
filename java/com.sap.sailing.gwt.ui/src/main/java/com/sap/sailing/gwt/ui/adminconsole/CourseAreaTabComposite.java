package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Tab within {@link EventDialog} to create and maintain course areas with their names and optional
 * geometries, as well as the possibility to copy course area definitions from other local or remote
 * events.
 */
public class CourseAreaTabComposite extends Composite {
    private final CourseAreaListInlineEditorComposite courseAreaList;

    public CourseAreaTabComposite(final Collection<EventDTO> existingEvents,
            final SailingServiceWriteAsync sailingServiceWrite, final StringMessages stringMessages) {
        courseAreaList = new CourseAreaListInlineEditorComposite(Collections.<CourseAreaDTO> emptyList(),
                new CourseAreaListInlineEditorComposite.ExpandedUi(stringMessages, IconResources.INSTANCE.removeIcon(),
                        SuggestedCourseAreaNames.suggestedCourseAreaNames, stringMessages.enterCourseAreaName(), 30));
        final VerticalPanel courseAreasPanel = new VerticalPanel();
        courseAreasPanel.setSpacing(5);
        final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
        final List<String> oracleSuggestions = new ArrayList<>();
        oracleSuggestions.add(stringMessages.localEvents());
        oracleSuggestions.add(SharedLandscapeConstants.DEFAULT_SAILING_SERVER_URL);
        oracleSuggestions.add("http://localhost:8889");
        oracle.addAll(oracleSuggestions);
        oracle.setDefaultSuggestionsFromText(oracleSuggestions);
        final SuggestBox sourceBox = new SuggestBox(oracle);
        sourceBox.setValue(stringMessages.localEvents());
        sourceBox.setWidth("300px");
        final String[] bearerTokenHolder = new String[1];
        final Runnable[] loadEventsHolder = new Runnable[1];
        final Button authenticateButton = new Button(stringMessages.authenticate());
        authenticateButton.addClickHandler(e -> {
            new DataEntryDialog<String>(stringMessages.authenticate(), null, stringMessages.ok(), stringMessages.cancel(), null,
                    new DataEntryDialog.DialogCallback<String>() {
                @Override
                public void ok(final String result) {
                    bearerTokenHolder[0] = result.trim().isEmpty() ? null : result.trim();
                    loadEventsHolder[0].run();
                }
                @Override
                public void cancel() {}
            }) {
                private final TextBox tokenBox = createTextBox(bearerTokenHolder[0] != null ? bearerTokenHolder[0] : "", 40);
                @Override
                protected String getResult() {
                    return tokenBox.getValue();
                }
                @Override
                protected Focusable getInitialFocusWidget() {
                    return tokenBox;
                }
                @Override
                protected Widget getAdditionalWidget() {
                    final VerticalPanel panel = new VerticalPanel();
                    panel.setSpacing(3);
                    final HorizontalPanel tokenRow = new HorizontalPanel();
                    tokenRow.setSpacing(3);
                    tokenRow.add(new Label(stringMessages.bearerTokenOrNullForRemoteEvents()));
                    tokenRow.add(tokenBox);
                    panel.add(tokenRow);
                    final Label explanationLabel = new Label(stringMessages.helptextBearerToken());
                    explanationLabel.setWidth("400px");
                    explanationLabel.getElement().getStyle().setProperty("whiteSpace", "normal");
                    explanationLabel.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);
                    explanationLabel.getElement().getStyle().setFontSize(11, Style.Unit.PX);
                    panel.add(explanationLabel);
                    return panel;
                }
            }.show();
        });
        final BusyIndicator remoteLoadBusyIndicator = new SimpleBusyIndicator(false, 0.7f) {
            @Override
            public void setBusy(final boolean busy) {
                super.setBusy(busy);
                getElement().getStyle().setDisplay(Style.Display.BLOCK);
                getElement().getStyle().setVisibility(busy ? Style.Visibility.VISIBLE : Style.Visibility.HIDDEN);
            }
        };
        remoteLoadBusyIndicator.setBusy(false);
        final Map<String, EventDTO> eventsCache = new HashMap<>();
        for (final EventDTO event : existingEvents) {
            eventsCache.put(event.getId().toString(), event);
        }
        final ListBox eventDropDown = new ListBox();
        eventDropDown.addItem(stringMessages.pleaseSelectAnEvent());
        final List<EventDTO> sortedExistingEvents = new ArrayList<>(existingEvents);
        sortedExistingEvents.sort(Comparator.comparing(EventDTO::getName));
        for (final EventDTO event : sortedExistingEvents) {
            eventDropDown.addItem(event.getName(), event.getId().toString());
        }
        final ListBox courseAreaDropDown = new ListBox();
        courseAreaDropDown.addItem(stringMessages.chooseWhatToAdd());
        courseAreaDropDown.setEnabled(false);
        courseAreaDropDown.setWidth("400px");
        final Button clearAllButton = new Button(stringMessages.clearAllCourseAreas());
        clearAllButton.addStyleName("btn-secondary");
        eventDropDown.addChangeHandler(e -> {
            final int selectedIndex = eventDropDown.getSelectedIndex();
            courseAreaDropDown.clear();
            if (selectedIndex > 0) {
                final EventDTO selected = eventsCache.get(eventDropDown.getValue(selectedIndex));
                if (selected != null && selected.getVenue() != null) {
                    courseAreaDropDown.addItem(stringMessages.chooseWhatToAdd());
                    courseAreaDropDown.addItem(stringMessages.addAllCourseAreas());
                    for (final CourseAreaDTO area : selected.getVenue().getCourseAreas()) {
                        courseAreaDropDown.addItem(area.getName(), area.getId().toString());
                    }
                    courseAreaDropDown.setEnabled(true);
                }
            } else {
                courseAreaDropDown.addItem(stringMessages.chooseWhatToAdd());
                courseAreaDropDown.setEnabled(false);
            }
        });
        courseAreaDropDown.addChangeHandler(e -> {
            final int selectedIndex = courseAreaDropDown.getSelectedIndex();
            if (selectedIndex <= 0) {
                return;
            }
            final EventDTO selectedEvent = eventsCache.get(eventDropDown.getValue(eventDropDown.getSelectedIndex()));
            if (selectedEvent == null) {
                return;
            }
            final List<CourseAreaDTO> toAdd = new ArrayList<>();
            if (selectedIndex == 1) {
                for (final CourseAreaDTO area : selectedEvent.getVenue().getCourseAreas()) {
                    toAdd.add(new CourseAreaDTO(UUID.randomUUID(), area.getName(), area.getCenterPosition(), area.getRadius()));
                }
            } else {
                final List<CourseAreaDTO> areas = selectedEvent.getVenue().getCourseAreas();
                if (selectedIndex - 2 < areas.size()) {
                    final CourseAreaDTO area = areas.get(selectedIndex - 2);
                    toAdd.add(new CourseAreaDTO(UUID.randomUUID(), area.getName(), area.getCenterPosition(), area.getRadius()));
                }
            }
            final List<CourseAreaDTO> updated = new ArrayList<>(courseAreaList.getValue());
            updated.addAll(toAdd);
            courseAreaList.setValue(updated);
            courseAreaDropDown.setSelectedIndex(0);
        });
        clearAllButton.addClickHandler(e -> courseAreaList.setValue(new ArrayList<>()));
        final Runnable loadEvents = () -> {
            eventDropDown.clear();
            eventsCache.clear();
            final String source = sourceBox.getValue().trim();
            final boolean isLocal = source.isEmpty() || source.equals(stringMessages.localEvents());
            if (isLocal) {
                eventDropDown.addItem(stringMessages.pleaseSelectALocalEvent());
                final List<EventDTO> sorted = new ArrayList<>(existingEvents);
                sorted.sort(Comparator.comparing(EventDTO::getName));
                for (final EventDTO event : sorted) {
                    eventsCache.put(event.getId().toString(), event);
                    eventDropDown.addItem(event.getName(), event.getId().toString());
                }
                eventDropDown.setEnabled(true);
            } else {
                eventDropDown.addItem(stringMessages.pleaseSelectAnEventFrom(source));
                remoteLoadBusyIndicator.setBusy(true);
                eventDropDown.setEnabled(false);
                final String bearerTokenOrNull = bearerTokenHolder[0];
                sailingServiceWrite.getRemoteEvents(source, bearerTokenOrNull,new AsyncCallback<List<EventDTO>>() {
                    @Override
                    public void onSuccess(final List<EventDTO> result) {
                        remoteLoadBusyIndicator.setBusy(false);
                        result.sort(Comparator.comparing(EventDTO::getName));
                        for (final EventDTO event : result) {
                            eventsCache.put(event.getId().toString(), event);
                            eventDropDown.addItem(event.getName(), event.getId().toString());
                        }
                        eventDropDown.setEnabled(true);
                    }
                    @Override
                    public void onFailure(final Throwable caught) {
                        remoteLoadBusyIndicator.setBusy(false);
                        eventDropDown.clear();
                        eventDropDown.addItem(caught.getMessage());
                    }
                });
            }
        };
        loadEventsHolder[0] = loadEvents;
        sourceBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(final SelectionEvent<SuggestOracle.Suggestion> event) {
                loadEvents.run();
            }
        });
        sourceBox.getValueBox().addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(final KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    loadEvents.run();
                }
            }
        });
        final VerticalPanel copyPanel = new VerticalPanel();
        copyPanel.setWidth("100%");
        copyPanel.setSpacing(3);
        final Grid inputGrid = new Grid(3, 3);
        inputGrid.setWidget(0, 0, new Label(stringMessages.copyCourseAreasFromAnotherUrl()));
        sourceBox.setTitle(stringMessages.helptextCopySource());
        final HorizontalPanel sourceBoxRow = new HorizontalPanel();
        sourceBoxRow.setSpacing(3);
        sourceBoxRow.add(sourceBox);
        sourceBoxRow.add(remoteLoadBusyIndicator);
        inputGrid.setWidget(0, 1, sourceBoxRow);
        inputGrid.setWidget(0, 2, authenticateButton);
        eventDropDown.setWidth("400px");
        inputGrid.setWidget(1, 0, new Label(stringMessages.event() + ":"));
        inputGrid.setWidget(1, 1, eventDropDown);
        inputGrid.setWidget(1, 2, clearAllButton);
        courseAreaDropDown.setWidth("400px");
        inputGrid.setWidget(2, 0, new Label(stringMessages.courseAreas() + ":"));
        inputGrid.setWidget(2, 1, courseAreaDropDown);
        copyPanel.add(inputGrid);
        courseAreasPanel.add(copyPanel);
        courseAreasPanel.add(courseAreaList);
        initWidget(courseAreasPanel);
    }

    public List<CourseAreaDTO> getValue() {
        return courseAreaList.getValue();
    }

    public void setValue(final List<CourseAreaDTO> value) {
        courseAreaList.setValue(value);
    }

    public void addValueChangeHandler(final ValueChangeHandler<Iterable<CourseAreaDTO>> handler) {
        courseAreaList.addValueChangeHandler(handler);
    }
}
