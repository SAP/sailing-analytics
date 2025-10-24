package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Set;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;

/**
 * Allows administrators to manage a sailing event.
 * 
 * @author Frank Mittag (C5163974)
 * @author Axel Uhl (d043530)
 */
public class EventManagementPanel extends SimplePanel
        implements FilterablePanelProvider<EventDTO> {
    private EventListComposite eventListComposite;
    private EventDetailsComposite eventDetailsComposite;
    private final CaptionPanel eventsPanel;
    private final RefreshableMultiSelectionModel<EventDTO> refreshableEventSelectionModel;
    
    public EventManagementPanel(final Presenter presenter, final StringMessages stringMessages, final PlaceController placeController) {
        VerticalPanel mainPanel = new VerticalPanel();
        setWidget(mainPanel);
        mainPanel.setWidth("100%");
        eventsPanel = new CaptionPanel(stringMessages.events());
        mainPanel.add(eventsPanel);
        VerticalPanel eventsContentPanel = new VerticalPanel();
        eventsPanel.setContentWidget(eventsContentPanel);
        eventListComposite = new EventListComposite(presenter, placeController, stringMessages);
        eventListComposite.ensureDebugId("EventListComposite");
        eventsContentPanel.add(eventListComposite);
        eventDetailsComposite = new EventDetailsComposite(presenter.getSailingService(), presenter.getErrorReporter(), stringMessages);
        eventDetailsComposite.ensureDebugId("EventDetailsComposite");
        eventDetailsComposite.setVisible(false);
        mainPanel.add(eventDetailsComposite);
        refreshableEventSelectionModel = eventListComposite.getRefreshableMultiSelectionModel();
        refreshableEventSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final Set<EventDTO> selectedEvents = refreshableEventSelectionModel.getSelectedSet();
                if (selectedEvents.size() == 1 && eventListComposite.getAllEvents() != null) {
                    final EventDTO selectedEvent = selectedEvents.iterator().next();
                        for (EventDTO eventDTO : eventListComposite.getAllEvents()) {
                            if (eventDTO.id.equals(selectedEvent.id)) {
                                eventDetailsComposite.setEvent(eventDTO);
                                eventDetailsComposite.setVisible(true);
                                break;
                            }
                    }
                } else {
                    eventDetailsComposite.setEvent(null);
                    eventDetailsComposite.setVisible(false);
                }
            }
        });
    }
    
    private final Displayer<EventDTO> eventsDisplayer = new Displayer<EventDTO>() {
        @Override
        public void fill(Iterable<EventDTO> result) {
            eventListComposite.fillEvents(result);
        }
    };
    
    public Displayer<EventDTO> getEventsDisplayer() {
        return eventsDisplayer;
    }

    public Displayer<LeaderboardGroupDTO> leaderboardGroupsDisplayer = new Displayer<LeaderboardGroupDTO>() {
        @Override
        public void fill(Iterable<LeaderboardGroupDTO> result) {
            fillLeaderboardGroups(result);
        }
    };
    
    public Displayer<LeaderboardGroupDTO> getLeaderboardGroupsDisplayer() {
        return leaderboardGroupsDisplayer;
    }

    public void fillLeaderboardGroups(Iterable<LeaderboardGroupDTO> leaderboardGroups) {
        eventListComposite.fillLeaderboardGroups(leaderboardGroups);
    }

    @Override
    public AbstractFilterablePanel<EventDTO> getFilterablePanel() {
        return eventListComposite.filterTextbox;
    }
}
