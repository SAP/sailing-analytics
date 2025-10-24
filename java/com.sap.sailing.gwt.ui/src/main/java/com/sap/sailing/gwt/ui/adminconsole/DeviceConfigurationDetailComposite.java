package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.racelog.AuthorPriority;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO.RegattaConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.controls.listedit.StringListEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;

public class DeviceConfigurationDetailComposite extends Composite {
    private final AdminConsoleResources resources = GWT.create(AdminConsoleResources.class);

    protected final SailingServiceWriteAsync sailingServiceWrite;
    protected final ErrorReporter errorReporter;
    protected final StringMessages stringMessages;
    private final CaptionPanel captionPanel;
    private final VerticalPanel contentPanel;
    private final Button cloneButton;
    private final Button updateButton;
    protected DeviceConfigurationWithSecurityDTO originalConfiguration;
    protected TextBox uuidBox;
    protected TextBox identifierBox;
    private StringListEditorComposite allowedCourseAreasList;
    private TextBox mailRecipientBox;
    private StringListEditorComposite courseNamesList;
    private CheckBox overwriteRegattaConfigurationBox;
    
    /**
     * names are the event names, values are the event UUIDs as String
     */
    private final ListBox eventListBox;
    
    private final Map<UUID, EventDTO> eventsById;

    /**
     * names are the course area names, values are the course area UUIDs as String
     */
    private final ListBox courseAreaListBox;
    
    /**
     * Names are the internationalized descriptions of the roles, values are their corresponding integer values as
     * String, or the empty string for the first empty "placeholder" record.
     */
    private final ListBox priorityListBox;
    
    private RegattaConfigurationDTO currentRegattaConfiguration;
    private final UserService userService;

    public static interface DeviceConfigurationFactory {
        void obtainAndSetNameForConfigurationAndAdd(final DeviceConfigurationWithSecurityDTO configurationToObtainAndSetNameForAndAdd);
        void update(DeviceConfigurationWithSecurityDTO configurationToUpdate);
    }

    public DeviceConfigurationDetailComposite(Presenter presenter, StringMessages stringMessages, final DeviceConfigurationFactory callbackInterface) {
        this.eventsById = new HashMap<>();
        this.sailingServiceWrite = presenter.getSailingService();
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        this.currentRegattaConfiguration = null;
        priorityListBox = createPriorityListBox();
        courseAreaListBox = new ListBox();
        courseAreaListBox.addChangeHandler(e->markAsDirty(true));
        eventListBox = new ListBox();
        eventListBox.addChangeHandler(e->{
            markAsDirty(true);
            final String selectedValue = eventListBox.getSelectedValue();
            fillCourseAreaListBox(eventsById.get(selectedValue==null||selectedValue.isEmpty() ? null : UUID.fromString(selectedValue)));
        });
        captionPanel = new CaptionPanel(stringMessages.configuration());
        captionPanel.ensureDebugId("DeviceConfigurationDetailComposite");
        VerticalPanel verticalPanel = new VerticalPanel();
        contentPanel = new VerticalPanel();
        cloneButton = new Button(stringMessages.save() + " + " + stringMessages.clone());
        cloneButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                updateConfiguration(callbackInterface);
                // create a new, cloned configuration object and set a new UUID; then ask for a name,
                // then send it to the server and add to the local list
                final DeviceConfigurationWithSecurityDTO cloned = getResult();
                cloned.id = UUID.randomUUID();
                callbackInterface.obtainAndSetNameForConfigurationAndAdd(cloned);
            }
        });
        updateButton = new Button(stringMessages.save());
        updateButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                updateConfiguration(callbackInterface);
            }
        });
        verticalPanel.add(contentPanel);
        verticalPanel.add(new HTML("<hr  style=\"width:100%;\" />"));
        HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.add(cloneButton);
        actionPanel.add(updateButton);
        verticalPanel.add(actionPanel);
        captionPanel.add(verticalPanel);
        initWidget(captionPanel);
        setConfiguration(/* configuration to display */ null);
        this.userService = presenter.getUserService();
        presenter.getEventsRefresher().addDisplayerAndCallFillOnInit(eventDTOs->fillEvents(eventDTOs));
    }

    private ListBox createPriorityListBox() {
        final ListBox result = new ListBox();
        result.addItem(stringMessages.selectARole(), "");
        for (final AuthorPriority priority : AuthorPriority.values()) {
            result.addItem(AuthorPriorityFormatter.getDescription(priority, stringMessages), Integer.toString(priority.getPriority()));
        }
        return result;
    }

    private void fillEvents(Iterable<EventDTO> events) {
        eventListBox.clear();
        eventsById.clear();
        eventListBox.addItem(stringMessages.selectSailingEvent(), "");
        final List<EventDTO> eventsSortedByName = new ArrayList<>();
        Util.addAll(events, eventsSortedByName);
        Collections.sort(eventsSortedByName, (e1, e2)->{
            int result = e1.getName().compareTo(e2.getName());
            if (result == 0) {
                result = e1.getId().compareTo(e2.getId());
            }
            return result;
        });
        for (final EventDTO event : eventsSortedByName) {
            eventsById.put(event.getId(), event);
            eventListBox.addItem(event.getName(), event.getId().toString());
        }
    }

    public void setConfiguration(final DeviceConfigurationWithSecurityDTO config) {
        if (config == null) {
            clearUi();
            setVisible(false);
        } else {
            setupUi(config);
            setVisible(true);
        }
    }
    
    private void updateEventSelection(UUID eventId) {
        for (int i=0; i<eventListBox.getItemCount(); i++) {
            final String value = eventListBox.getValue(i);
            if (eventId == null && value.isEmpty() || Util.equalsWithNull(eventId==null?null:eventId.toString(), value)) {
                eventListBox.setSelectedIndex(i);
                fillCourseAreaListBox(eventsById.get(eventId));
            }
        }
    }

    private void fillCourseAreaListBox(EventDTO event) {
        courseAreaListBox.clear();
        courseAreaListBox.addItem(stringMessages.selectCourseArea(), "");
        if (event != null) {
            int i=1;
            for (final CourseAreaDTO courseArea : event.getVenue().getCourseAreas()) {
                courseAreaListBox.addItem(courseArea.getName(), courseArea.getId().toString());
                if (Util.equalsWithNull(originalConfiguration.courseAreaId, courseArea.getId())) {
                    courseAreaListBox.setSelectedIndex(i);
                }
                i++;
            }
        }
    }

    private void setupUi(DeviceConfigurationWithSecurityDTO config) {
        clearUi();
        this.originalConfiguration = config;
        this.currentRegattaConfiguration = config.regattaConfiguration;
        setupGeneral();
        setupRegattaConfiguration();
        final boolean hasUpdatePermission = userService.hasPermission(config, DefaultActions.UPDATE);
        final boolean hasUpdateAndCreatePermission = hasUpdatePermission
                && userService.hasCreatePermission(SecuredDomainType.RACE_MANAGER_APP_DEVICE_CONFIGURATION);
        cloneButton.setVisible(hasUpdateAndCreatePermission);
        updateButton.setVisible(hasUpdatePermission);
        allowedCourseAreasList.setEnabled(hasUpdatePermission);
        courseNamesList.setEnabled(hasUpdatePermission);
        overwriteRegattaConfigurationBox.setEnabled(hasUpdatePermission);
        updateEventSelection(config.eventId);
        updatePrioritySelection(config.priority);
    }

    private void updatePrioritySelection(Integer priority) {
        if (priority == null) {
            priorityListBox.setSelectedIndex(0);
        } else {
            for (int i=1; i<priorityListBox.getItemCount(); i++) {
                if (Integer.valueOf(priorityListBox.getValue(i)).equals(priority)) {
                    priorityListBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void setupRegattaConfiguration() {
        Grid grid = new Grid(1, 3);
        final Button editButton = new Button(stringMessages.edit());
        overwriteRegattaConfigurationBox = new CheckBox(stringMessages.overwriteRacingProceduresConfiguration());
        overwriteRegattaConfigurationBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                markAsDirty(true);
                editButton.setEnabled(event.getValue());
            }
        });
        grid.setWidget(0, 0, overwriteRegattaConfigurationBox);
        editButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new RegattaConfigurationDialog(currentRegattaConfiguration == null ? new RegattaConfigurationDTO() : currentRegattaConfiguration, 
                        stringMessages, new DialogCallback<DeviceConfigurationDTO.RegattaConfigurationDTO>() {
                            @Override
                            public void ok(RegattaConfigurationDTO newConfiguration) {
                                currentRegattaConfiguration = newConfiguration;
                                markAsDirty(true);
                            }

                            @Override
                            public void cancel() {
                            }
                        }).show();
            }
        });
        overwriteRegattaConfigurationBox.setValue(currentRegattaConfiguration != null);
        editButton.setEnabled(currentRegattaConfiguration != null);
        Image helpImage = new Image(resources.help());
        helpImage.setAltText(stringMessages.overwriteRacingProceduresConfigurationHelpText());
        helpImage.setTitle(stringMessages.overwriteRacingProceduresConfigurationHelpText());
        grid.setWidget(0, 1, editButton);
        grid.setWidget(0, 2, helpImage);
        contentPanel.add(grid);
    }

    private void setupGeneral() {
        Grid grid = new Grid(8, 2);
        int row = 0;
        identifierBox = new TextBox();
        identifierBox.setWidth("80%");
        identifierBox.setText(originalConfiguration.name);
        identifierBox.setReadOnly(true);
        grid.setWidget(row, 0, new Label(stringMessages.identifier()));
        HorizontalPanel panel = new HorizontalPanel();
        final Button qrCodeButton = createQrCodeButton();
        panel.add(identifierBox);
        panel.add(qrCodeButton);
        grid.setWidget(row++, 1, panel);
        uuidBox = new TextBox();
        uuidBox.setWidth("80%");
        uuidBox.setText(originalConfiguration.id.toString());
        uuidBox.setReadOnly(true);
        grid.setWidget(row, 0, new Label(stringMessages.id()));
        grid.setWidget(row++, 1, uuidBox);
        setupCourseAreasBox(grid, row++);
        setupRecipientBox(grid, row++);
        setupCourseNameBox(grid, row++);
        grid.setWidget(row, 0, new Label(stringMessages.event()));
        grid.setWidget(row++, 1, eventListBox);
        grid.setWidget(row, 0, new Label(stringMessages.courseArea()));
        grid.setWidget(row++, 1, courseAreaListBox);
        grid.setWidget(row, 0, new Label(stringMessages.authorPriority()));
        grid.setWidget(row++, 1, priorityListBox);
        contentPanel.add(grid);
    }

    private void setupCourseAreasBox(Grid grid, int gridRow) {
        List<String> initialValues = originalConfiguration.allowedCourseAreaNames == null ? Collections
                .<String> emptyList() : originalConfiguration.allowedCourseAreaNames;
        allowedCourseAreasList = new StringListEditorComposite(initialValues, stringMessages, stringMessages.courseAreas(), IconResources.INSTANCE.removeIcon(),
                SuggestedCourseAreaNames.suggestedCourseAreaNames, stringMessages.enterCourseAreaName());
        allowedCourseAreasList.setWidth("80%");
        allowedCourseAreasList.addValueChangeHandler(dirtyValueMarker);
        grid.setWidget(gridRow, 0, new Label(stringMessages.allowedCourseAreas()));
        grid.setWidget(gridRow, 1, allowedCourseAreasList);
    }

    private void setupCourseNameBox(Grid grid, int gridRow) {
        List<String> initialValues = originalConfiguration.byNameDesignerCourseNames == null ? Collections
                .<String> emptyList() : originalConfiguration.byNameDesignerCourseNames;
        List<String> suggestedCourseNames = Arrays.asList(stringMessages.upWind(), stringMessages.downWind());
        courseNamesList = new StringListEditorComposite(initialValues, stringMessages, stringMessages.courseNames(), IconResources.INSTANCE.removeIcon(), suggestedCourseNames,
                stringMessages.enterCourseName());
        courseNamesList.setWidth("80%");
        courseNamesList.addValueChangeHandler(dirtyValueMarker);
        grid.setWidget(gridRow, 0, new Label(stringMessages.courseNames()));
        grid.setWidget(gridRow, 1, courseNamesList);
    }

    private void setupRecipientBox(Grid grid, int gridRow) {
        mailRecipientBox = new TextBox();
        mailRecipientBox.setWidth("80%");
        mailRecipientBox.addKeyUpHandler(dirtyMarker);
        if (originalConfiguration.resultsMailRecipient != null) {
            mailRecipientBox.setText(originalConfiguration.resultsMailRecipient);
        }
        grid.setWidget(gridRow, 0, new Label(stringMessages.resultsMailRecipient()));
        grid.setWidget(gridRow, 1, mailRecipientBox);
    }

    private void clearUi() {
        this.originalConfiguration = null;
        contentPanel.clear();
    }

    private void markAsDirty(boolean dirty) {
        if (dirty) {
            if (captionPanel.getCaptionText().equals(stringMessages.configuration())) {
                captionPanel.setCaptionText(stringMessages.configuration() + "* (" + stringMessages.changed() + ")");
            }
        } else {
            if (!captionPanel.getCaptionText().equals(stringMessages.configuration())) {
                captionPanel.setCaptionText(stringMessages.configuration());
            }
        }
    }

    private DeviceConfigurationWithSecurityDTO getResult() {
        DeviceConfigurationWithSecurityDTO result = new DeviceConfigurationWithSecurityDTO(originalConfiguration.getIdentifier());
        result.setOwnership(originalConfiguration.getOwnership());
        result.setAccessControlList(originalConfiguration.getAccessControlList());
        result.name = identifierBox.getValue();
        result.id = UUID.fromString(uuidBox.getValue());
        if (!allowedCourseAreasList.getValue().isEmpty()) {
            result.allowedCourseAreaNames = allowedCourseAreasList.getValue();
        }
        if (!mailRecipientBox.getText().isEmpty()) {
            result.resultsMailRecipient = mailRecipientBox.getText().isEmpty() ? null : mailRecipientBox.getText();
        }
        if (!courseNamesList.getValue().isEmpty()) {
            result.byNameDesignerCourseNames = courseNamesList.getValue();
        }
        if (overwriteRegattaConfigurationBox.getValue()) {
            result.regattaConfiguration = currentRegattaConfiguration;
        }
        if (!eventListBox.getSelectedValue().isEmpty()) {
            result.eventId = UUID.fromString(eventListBox.getSelectedValue());
        }
        if (!courseAreaListBox.getSelectedValue().isEmpty()) {
            result.courseAreaId = UUID.fromString(courseAreaListBox.getSelectedValue());
        }
        if (!priorityListBox.getSelectedValue().isEmpty()) {
            result.priority = Integer.decode(priorityListBox.getSelectedValue());
        }
        return result;
    }

    private void updateConfiguration(DeviceConfigurationFactory callbackInterface) {
        if (originalConfiguration == null) {
            errorReporter.reportError(stringMessages.invalidState());
            return;
        }
        DeviceConfigurationWithSecurityDTO dto = getResult();
        sailingServiceWrite.createOrUpdateDeviceConfiguration(dto, new MarkedAsyncCallback<>(new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                markAsDirty(false);
                callbackInterface.update(dto);
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }
        }));
    }

    private KeyUpHandler dirtyMarker = new KeyUpHandler() {
        @Override
        public void onKeyUp(KeyUpEvent event) {
            markAsDirty(true);
        }
    };

    private ValueChangeHandler<Iterable<String>> dirtyValueMarker = new ValueChangeHandler<Iterable<String>>() {
        @Override
        public void onValueChange(ValueChangeEvent<Iterable<String>> event) {
            markAsDirty(true);
        }
    };

    private Button createQrCodeButton() {
        Button qrCodeButton = new Button(stringMessages.qrSync());
        qrCodeButton.ensureDebugId("DeviceConfigurationDetailsQrCodeButton");
        qrCodeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (identifierBox.getValue() == null || identifierBox.getValue().isEmpty()) {
                    Notification.notify(stringMessages.thereIsNoIdentifierSet(), NotificationType.ERROR);
                } else {
                    UserDTO currentUser = userService.getCurrentUser();
                    if (currentUser == null) {
                        createAndShowDialogForAccessToken(/* accessToken */ null);
                    } else {
                        userService.getUserManagementService().getOrCreateAccessToken(currentUser.getName(),
                                new MarkedAsyncCallback<>(new AsyncCallback<String>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                errorReporter.reportError(stringMessages.couldNotObtainAccessTokenForUser(caught.getMessage()), /* silentMode */ true);
                                    }

                                    @Override
                                    public void onSuccess(String accessToken) {
                                        createAndShowDialogForAccessToken(accessToken);
                                    }
                                }));
                    }
                }
            }
        });
        return qrCodeButton;
    }

    private void createAndShowDialogForAccessToken(String accessToken) {
        final DeviceConfigurationWithSecurityDTO config = getResult();
        final DialogBox dialog = new DeviceConfigurationQRIdentifierDialog(uuidBox.getValue(), identifierBox.getValue(),
                config.eventId, config.courseAreaId, config.priority, accessToken, stringMessages);
        dialog.show();
        dialog.center();
    }
}
