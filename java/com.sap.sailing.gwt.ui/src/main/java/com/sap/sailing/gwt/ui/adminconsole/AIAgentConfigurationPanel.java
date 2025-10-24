package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sailing.domain.common.security.SecuredDomainType.EVENT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.TableWrapperWithMultiSelectionAndFilterForSecuredDTO;

public class AIAgentConfigurationPanel extends SimplePanel {
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final StringMessages stringMessages;
    private final UserService userService;
    private final ErrorReporter errorReporter;
    private final TableWrapperWithMultiSelectionAndFilterForSecuredDTO<EventDTO, StringMessages, AdminConsoleTableResources> eventsTableWrapper;
    private final Set<EventDTO> selectedEvents;
    private boolean selectionUpdatedAfterEventsHaveLoaded;
    private boolean handleSelectionChangeEvents;
    private String languageModelName;

    public AIAgentConfigurationPanel(final Presenter presenter, final StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.stringMessages = stringMessages;
        this.userService = presenter.getUserService();
        this.errorReporter = presenter.getErrorReporter();
        this.eventsTableWrapper = this.getEventsTableWrapper();
        this.selectedEvents = new HashSet<>();
        setWidget(this.getMainPanel(presenter));
    }
    
    private TableWrapperWithMultiSelectionAndFilterForSecuredDTO<EventDTO, StringMessages, AdminConsoleTableResources> getEventsTableWrapper() {
        final AdminConsoleTableResources adminConsoleTableResources = GWT.create(AdminConsoleTableResources.class);
        final TableWrapperWithMultiSelectionAndFilterForSecuredDTO<EventDTO, StringMessages, AdminConsoleTableResources> eventsTableWrapper
            = new TableWrapperWithMultiSelectionAndFilterForSecuredDTO<EventDTO, StringMessages, AdminConsoleTableResources>(
                this.stringMessages, this.errorReporter,
                /* enablePager */ true, Optional.of(new EntityIdentityComparator<EventDTO>() {
                    @Override
                    public boolean representSameEntity(EventDTO dto1, EventDTO dto2) {
                        return dto1.getId().equals(dto2.getId());
                    }
    
                    @Override
                    public int hashCode(EventDTO t) {
                        return t.getId().hashCode();
                    }
                }), adminConsoleTableResources,
                userService, Optional.of(stringMessages.filterEventsByName())) {
            @Override
            protected Iterable<String> getSearchableStrings(EventDTO t) {
                return Arrays.asList(t.getName(), t.getDescription(), t.getVenue().getName());
            }
        };
        eventsTableWrapper.addColumn(EventDTO::getName, stringMessages.name());
        final SafeHtmlCell descriptionCell = new SafeHtmlCell();
        final Column<EventDTO, SafeHtml> descriptionColumn = new Column<EventDTO, SafeHtml>(descriptionCell) {
            @Override
            public SafeHtml getValue(EventDTO event) {
                final SafeHtmlBuilder builder = new SafeHtmlBuilder();
                builder.appendEscaped(event.getDescription());
                return builder.toSafeHtml();
            }
        };
        descriptionColumn.setCellStyleNames(adminConsoleTableResources.cellTableStyle().cellTableWrapText());
        eventsTableWrapper.addColumn(descriptionColumn, stringMessages.description());
        eventsTableWrapper.addColumn(e->e.getVenue().getName(), stringMessages.venue());
        eventsTableWrapper.addColumn(e->e.getId().toString(), stringMessages.id());
        eventsTableWrapper.getSelectionModel().addSelectionChangeHandler(selectionChangeEvent->{
            if (handleSelectionChangeEvents) {
                final Set<EventDTO> newSelection = eventsTableWrapper.getSelectionModel().getSelectedSet();
                final Set<EventDTO> addedToSelection = new HashSet<>(newSelection);
                addedToSelection.removeAll(selectedEvents);
                final Set<EventDTO> removedFromSelection = new HashSet<>(selectedEvents);
                removedFromSelection.removeAll(newSelection);
                selectedEvents.clear();
                selectedEvents.addAll(eventsTableWrapper.getSelectionModel().getSelectedSet());
                addedToSelection.forEach(e->sailingServiceWrite.startAICommentingOnEvent(e.getId(), new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Notification.notify(stringMessages.successfullyActivatedAICommentingForEvent(e.getName()), NotificationType.INFO);
                    }
                    
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(stringMessages.errorActivatingAICommentingForEvent(e.getName(), caught.getMessage()), NotificationType.ERROR);
                    }
                }));
                removedFromSelection.forEach(e->sailingServiceWrite.stopAICommentingOnEvent(e.getId(), new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Notification.notify(stringMessages.successfullyStoppedAICommentingForEvent(e.getName()), NotificationType.INFO);
                    }
                    
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(stringMessages.errorStoppingAICommentingForEvent(e.getName(), caught.getMessage()), NotificationType.ERROR);
                    }
                }));
            }
        });
        return eventsTableWrapper;
    }
    
    private Widget getMainPanel(final Presenter presenter) {
        final VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, EVENT);
        final Button refresh = buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> presenter.getEventsRefresher().reloadAndCallFillAll());
        refresh.ensureDebugId("RefreshEventsButton");
        final CaptionPanel eventsCaptionPanel = new CaptionPanel(stringMessages.selectEventsForWhichToUseAICommenting());
        eventsCaptionPanel.setWidth("100%");
        final VerticalPanel contents = new VerticalPanel();
        contents.setWidth("100%");
        contents.add(buttonPanel);
        final TextArea credentialsTextArea = new TextArea();
        credentialsTextArea.getElement().setAttribute("placeholder", stringMessages.placeholderAICoreCredentialsAsJSON());
        credentialsTextArea.setSize("60em", "15em");
        final CaptionPanel credentialsCaptionPanel = new CaptionPanel(stringMessages.credentials());
        final VerticalPanel credentialsVP = new VerticalPanel();
        credentialsVP.setSpacing(5);
        final HorizontalPanel hasOrResetCredentialsHP = new HorizontalPanel();
        hasOrResetCredentialsHP.setSpacing(5);
        hasOrResetCredentialsHP.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        final Label hasCredentialsLabel = new Label();
        hasOrResetCredentialsHP.add(hasCredentialsLabel);
        final Label languageModelNameLabel = new Label();
        final Button resetCredentialsButton = new Button(stringMessages.resetCredentials());
        final AsyncCallback<Boolean> hasAIAgentCredentialsCallback = new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
                updateHasCredentialsLabelAndSetEventsCaptionVisibility(
                        false, hasCredentialsLabel, eventsCaptionPanel, resetCredentialsButton, languageModelNameLabel);
                Notification.notify(stringMessages.errorTryingToCheckForAIAgentCredentials(caught.getMessage()), NotificationType.ERROR);
            }
    
            @Override
            public void onSuccess(Boolean result) {
                updateHasCredentialsLabelAndSetEventsCaptionVisibility(
                        result, hasCredentialsLabel, eventsCaptionPanel, resetCredentialsButton, languageModelNameLabel);
                if (result) {
                    sailingServiceWrite.getAIAgentLanguageModelName(new AsyncCallback<String>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            Notification.notify(stringMessages.errorObtainingAIAgentLanguageModelName(caught.getMessage()), NotificationType.ERROR);
                        }
                        
                        @Override
                        public void onSuccess(String result) {
                            if (result != null) {
                                languageModelName = result;
                                languageModelNameLabel.setText(stringMessages.languageModelUsedForAICommenting(result));
                                contents.add(eventsTableWrapper);
                                eventsCaptionPanel.setContentWidget(contents);
                                if (!selectionUpdatedAfterEventsHaveLoaded) {
                                    presenter.getEventsRefresher().reloadAndCallFillAll();
                                }
                            } else {
                                languageModelNameLabel.setText(stringMessages.noValidAICoreConfiguration());
                            }
                        }
                    });
                }
            }
        };
        resetCredentialsButton.addClickHandler(e->sailingServiceWrite.resetAIAgentCredentials(
                new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        sailingServiceWrite.hasAIAgentCredentials(hasAIAgentCredentialsCallback);
                        Notification.notify(stringMessages.successfullyResetAIAgentCredentials(), NotificationType.INFO);
                    }
                    
                    @Override
                    public void onFailure(Throwable caught) {
                        sailingServiceWrite.hasAIAgentCredentials(hasAIAgentCredentialsCallback);
                        Notification.notify(stringMessages.errorUpdatingAIAgentCredentials(caught.getMessage()), NotificationType.ERROR);
                    }
                }));
        hasOrResetCredentialsHP.add(resetCredentialsButton);
        credentialsVP.add(hasOrResetCredentialsHP);
        final HorizontalPanel updateCredentialsHP = new HorizontalPanel();
        updateCredentialsHP.setSpacing(5);
        credentialsCaptionPanel.setContentWidget(credentialsVP);
        updateCredentialsHP.add(credentialsTextArea);
        final Button credentialsUpdateButton = new Button(stringMessages.updateCredentials());
        eventsTableWrapper.getTable().setWidth("100%");
        sailingServiceWrite.hasAIAgentCredentials(hasAIAgentCredentialsCallback);
        credentialsUpdateButton.addClickHandler(e->sailingServiceWrite.setAIAgentCredentials(
                credentialsTextArea.getText(), new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        credentialsTextArea.setText("");
                        sailingServiceWrite.hasAIAgentCredentials(hasAIAgentCredentialsCallback);
                        Notification.notify(stringMessages.successfullyUpdatedAIAgentCredentials(), NotificationType.INFO);
                    }
                    
                    @Override
                    public void onFailure(Throwable caught) {
                        credentialsTextArea.setText("");
                        sailingServiceWrite.hasAIAgentCredentials(hasAIAgentCredentialsCallback);
                        Notification.notify(stringMessages.errorUpdatingAIAgentCredentials(caught.getMessage()), NotificationType.ERROR);
                    }
                }));
        updateCredentialsHP.add(credentialsUpdateButton);
        credentialsVP.add(updateCredentialsHP);
        mainPanel.add(credentialsCaptionPanel);
        mainPanel.add(languageModelNameLabel);
        mainPanel.add(eventsCaptionPanel);
        return mainPanel;
    }
    
    public Displayer<EventDTO> getEventsDisplayer() {
        return new Displayer<EventDTO>() {
            @Override
            public void fill(Iterable<EventDTO> result) {
                handleSelectionChangeEvents = false;
                eventsTableWrapper.getFilterPanel().updateAll(result);
                if (!selectionUpdatedAfterEventsHaveLoaded && languageModelName != null) {
                    selectionUpdatedAfterEventsHaveLoaded = true;
                    sailingServiceWrite.getIdsOfEventsWithAICommenting(new AsyncCallback<List<EventDTO>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            Notification.notify(stringMessages.errorTryingToFetchEventsWithAICommentingActive(caught.getMessage()), NotificationType.ERROR);
                            handleSelectionChangeEvents = true;
                        }

                        @Override
                        public void onSuccess(List<EventDTO> result) {
                            selectedEvents.clear();
                            selectedEvents.addAll(result);
                            result.forEach(e->eventsTableWrapper.getSelectionModel().setSelected(e, true));
                            Scheduler.get().scheduleDeferred(()->handleSelectionChangeEvents = true);
                        }
                    });
                } else {
                    Scheduler.get().scheduleDeferred(()->handleSelectionChangeEvents = true);
                }
            }
        };
    }

    private void updateHasCredentialsLabelAndSetEventsCaptionVisibility(
            final boolean hasCredentials, 
            final Label hasCredentialsLabel,
            final CaptionPanel eventsCaptionPanel,
            final Button resetCredentialsButton,
            final Label languageModelNameLabel) {
        final String label = hasCredentials ? stringMessages.hasAIAgentCredentials() : stringMessages.hasNoAIAgentCredentials();
        hasCredentialsLabel.setText(label);
        eventsCaptionPanel.setVisible(hasCredentials);
        resetCredentialsButton.setVisible(hasCredentials);
        languageModelNameLabel.setVisible(hasCredentials);
    }
}
