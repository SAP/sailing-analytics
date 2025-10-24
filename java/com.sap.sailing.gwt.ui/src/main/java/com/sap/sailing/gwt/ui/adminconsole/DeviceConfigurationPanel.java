package com.sap.sailing.gwt.ui.adminconsole;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.common.client.help.HelpButton;
import com.sap.sailing.gwt.common.client.help.HelpButtonResources;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationWithSecurityDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.SelectedElementsCountingButton;

public class DeviceConfigurationPanel extends SimplePanel implements DeviceConfigurationDetailComposite.DeviceConfigurationFactory {
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final UserService userService;
    private final StringMessages stringMessages;
    private final ErrorReporter errorReporter;
    private Button addConfigurationButton;
    private Button removeConfigurationButton;
    private Button refreshConfigurationsButton;
    private DeviceConfigurationListComposite listComposite;
    private DeviceConfigurationDetailComposite detailComposite;
    private final RefreshableMultiSelectionModel<DeviceConfigurationWithSecurityDTO> refreshableMultiSelectionModel;
    
    public DeviceConfigurationPanel(final Presenter presenter, final StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.userService = presenter.getUserService();
        this.stringMessages = stringMessages;
        this.errorReporter = presenter.getErrorReporter();
        listComposite = new DeviceConfigurationListComposite(sailingServiceWrite, errorReporter, stringMessages, userService);
        refreshableMultiSelectionModel = listComposite.getSelectionModel();
        detailComposite = setupUi(presenter);
        refreshableMultiSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<DeviceConfigurationWithSecurityDTO> selectedConfigurations = refreshableMultiSelectionModel
                        .getSelectedSet();
                if (selectedConfigurations.size() == 1 && selectedConfigurations.iterator().hasNext()) {
                    detailComposite.setConfiguration(selectedConfigurations.iterator().next());
                } else {
                    detailComposite.setConfiguration(null);
                }
                removeConfigurationButton.setEnabled(!selectedConfigurations.isEmpty());
            }
        });
    }
    
    private DeviceConfigurationDetailComposite setupUi(Presenter presenter) {
        VerticalPanel mainPanel = new VerticalPanel();
        setWidget(mainPanel);
        mainPanel.setWidth("100%");
        setupControlPanel(mainPanel);
        return setupConfigurationPanels(mainPanel, presenter);
    }

    private void setupControlPanel(VerticalPanel mainPanel) {
        HorizontalPanel deviceManagementControlPanel = new HorizontalPanel();
        deviceManagementControlPanel.setSpacing(5);
        addConfigurationButton = new Button(stringMessages.addConfiguration());
        addConfigurationButton.ensureDebugId("addDeviceConfigurationButton");
        addConfigurationButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                createConfiguration();
            }
        });
        if (userService.hasCreatePermission(SecuredDomainType.RACE_MANAGER_APP_DEVICE_CONFIGURATION)) {
            deviceManagementControlPanel.add(addConfigurationButton);
        }
        removeConfigurationButton = new SelectedElementsCountingButton<DeviceConfigurationWithSecurityDTO>(
                stringMessages.remove(), refreshableMultiSelectionModel,
                StringMessages.INSTANCE::doYouReallyWantToRemoveSelectedElements, (event) -> removeConfiguration());
        deviceManagementControlPanel.add(removeConfigurationButton);
        refreshConfigurationsButton = new Button(stringMessages.refresh());
        refreshConfigurationsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                listComposite.refreshTable();
            }
        });
        deviceManagementControlPanel.add(refreshConfigurationsButton);
        deviceManagementControlPanel.add(new HelpButton(HelpButtonResources.INSTANCE,
                stringMessages.videoGuide(), "https://wiki.sapsailing.com/wiki/howto/tutorials/sailingracemanager/sailing-race-guide.md"));
        mainPanel.add(deviceManagementControlPanel);
    }

    private DeviceConfigurationDetailComposite setupConfigurationPanels(VerticalPanel mainPanel, Presenter presenter) {
        Grid grid = new Grid(1 ,2);
        mainPanel.add(grid);
        grid.setWidget(0, 0, listComposite);
        grid.getRowFormatter().setVerticalAlign(0, HasVerticalAlignment.ALIGN_TOP);
        grid.getColumnFormatter().getElement(1).getStyle().setPaddingTop(2.0, Unit.EM);
        DeviceConfigurationDetailComposite myDetailComposite = new DeviceConfigurationDetailComposite(presenter, stringMessages, this);
        myDetailComposite.setVisible(false);
        grid.setWidget(0, 1, myDetailComposite);
        return myDetailComposite;
    }
    
    private void createConfiguration() {
        final DeviceConfigurationWithSecurityDTO newConfiguration = new DeviceConfigurationWithSecurityDTO(null);
        newConfiguration.id = UUID.randomUUID(); // the name will be obtained by the following call
        obtainAndSetNameForConfigurationAndAdd(newConfiguration);
    }

    /**
     * Asks the user for a new name for {@code configurationToObtainAndSetNameForAndAdd} that is not used yet by any of
     * the configurations in all the configurations obtained by
     * {@link SailingServiceAsync#getDeviceConfigurations(AsyncCallback)}. If such a name has been provided by the user
     * it is set on {@code configurationToObtainAndSetNameForAndAdd}, and
     * {@code configurationToObtainAndSetNameForAndAdd} is submitted to the server for creation. This method assumes
     * that the {@link DeviceConfigurationDTO.id ID} of the {@code configurationToObtainAndSetNameForAndAdd} is unique.
     */
    @Override
    public void obtainAndSetNameForConfigurationAndAdd(
            final DeviceConfigurationWithSecurityDTO configurationToObtainAndSetNameForAndAdd) {
        sailingServiceWrite.getDeviceConfigurations(
                new MarkedAsyncCallback<>(new AsyncCallback<List<DeviceConfigurationWithSecurityDTO>>() {
            @Override
                    public void onSuccess(List<DeviceConfigurationWithSecurityDTO> allConfigurations) {
                new SelectNameForNewDeviceConfigurationDialog(stringMessages, new SelectNameForNewDeviceConfigurationDialog.MatcherValidator(allConfigurations, stringMessages), new DialogCallback<String>() {
                    @Override
                    public void ok(String nameForNewDeviceConfiguration) {
                        configurationToObtainAndSetNameForAndAdd.name = nameForNewDeviceConfiguration;
                        sailingServiceWrite.createOrUpdateDeviceConfiguration(configurationToObtainAndSetNameForAndAdd, 
                                new MarkedAsyncCallback<>(new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                listComposite.refreshTable();
                                refreshableMultiSelectionModel.clear();
                            }
                            
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(caught.getMessage());
                            }
                        }));
                    }
                
                    @Override
                    public void cancel() { }
                }).show();
            }
    
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }
        }));
    }
    
    @Override
    public void update(DeviceConfigurationWithSecurityDTO configurationToUpdate) {
        listComposite.update(configurationToUpdate);
    }
    
    private void removeConfiguration() {
        detailComposite.setConfiguration(/* configuration to display */ null);
        for (DeviceConfigurationDTO config : refreshableMultiSelectionModel.getSelectedSet()) {
            sailingServiceWrite.removeDeviceConfiguration(config.id, new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    listComposite.refreshTable();
                }
                
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError(caught.getMessage());
                }                  
            });
        }
    }

}
