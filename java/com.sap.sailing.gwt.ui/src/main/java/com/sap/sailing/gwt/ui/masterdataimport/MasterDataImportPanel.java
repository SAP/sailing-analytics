package com.sap.sailing.gwt.ui.masterdataimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.MasterDataImportObjectCreationCount;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.ui.client.MasterDataImportNotification;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.filter.impl.KeywordMatcher;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.progressbar.PollingProgressPanel;
import com.sap.sse.gwt.client.dialog.DialogUtils;

public class MasterDataImportPanel extends VerticalPanel {

    private ListBox leaderboardgroupListBox;
    private TextBox hostBox;
    private Button importLeaderboardGroupsButton;
    private Button fetchIdsButton;   
    /**
     * A map containing a leaderboardgroup UUID as key and the name of that leaderboard group as value.
     */
    private Map<String, String> allLeaderboardGroupsNameAndIdsMap;

    private final StringMessages stringMessages;
    private String currentHost;
    private SailingServiceWriteAsync sailingServiceWrite;
    private CheckBox overrideSwitch;
    private final Presenter presenter;
    private CheckBox compressSwitch;
    private CheckBox exportWindSwitch;
    private CheckBox exportDeviceConfigsSwitch;
    private CheckBox exportTrackedRacesAndStartTrackingSwitch;
    private TextBox filterBox;
    private TextBox usernameBox;
    private TextBox passwordBox;

    public MasterDataImportPanel(final Presenter presenter, StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.stringMessages = stringMessages;
        this.presenter = presenter;
        HorizontalPanel serverAddressPanel = new HorizontalPanel();
        serverAddressPanel.add(new Label(stringMessages.importRemoteHost()));
        hostBox = new TextBox();
        hostBox.setText("https://www.sapsailing.com/");
        hostBox.setWidth("300px");
        serverAddressPanel.add(hostBox);
        HorizontalPanel usernamePanel = new HorizontalPanel();
        usernamePanel.add(new Label(stringMessages.username()));
        usernameBox = new TextBox();
        usernameBox.setText("");
        usernameBox.setWidth("300px");
        usernamePanel.add(usernameBox);
        HorizontalPanel passwordPanel = new HorizontalPanel();
        passwordPanel.add(new Label(stringMessages.password()));
        passwordBox = new PasswordTextBox();
        passwordBox.setText("");
        passwordBox.setWidth("300px");
        passwordPanel.add(passwordBox);
        HorizontalPanel userContextInformation = new HorizontalPanel();
        userContextInformation.setWidth("350px");
        userContextInformation.add(new Label(stringMessages.keepEmptyForDefaultUserAndPassword(), true));
        fetchIdsButton = new Button(stringMessages.importFetchRemoteLgs());
        fetchIdsButton.ensureDebugId("fetchLeaderboardGroupList");
        DialogUtils.linkEnterToButton(fetchIdsButton, usernameBox);
        DialogUtils.linkEnterToButton(fetchIdsButton, passwordBox);
        this.add(serverAddressPanel);
        this.add(userContextInformation);
        this.add(usernamePanel);
        this.add(passwordPanel);
        this.add(fetchIdsButton);
        ScrollPanel scrollPanel = new ScrollPanel();
        this.add(scrollPanel);
        VerticalPanel contentPanel = new VerticalPanel();
        scrollPanel.setWidget(contentPanel);
        addContentToLeftPanel(contentPanel);
        setListeners();
    }

    private void setListeners() {
        fetchIdsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                fireIdRequestsAndFillLists();
            }
        });
        hostBox.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    fireIdRequestsAndFillLists();
                }
            }
        });
        importLeaderboardGroupsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                importLeaderboardGroups();
            }
        });
    }

    protected void importLeaderboardGroups() {
        final UUID[] leaderboardGroupIds = createLeaderboardGroupIdsFromListBox();
        if (leaderboardGroupIds.length >= 1) {
            disableAllButtons();
            final boolean override = overrideSwitch.getValue();
            final boolean compress = compressSwitch.getValue();
            final boolean exportWind = exportWindSwitch.getValue();
            final boolean exportDeviceConfigs = exportDeviceConfigsSwitch.getValue();
            final boolean exportTrackedRacesAndStartTracking = exportTrackedRacesAndStartTrackingSwitch.getValue();
            sailingServiceWrite.importMasterData(currentHost, leaderboardGroupIds, override, compress, exportWind,
                    exportDeviceConfigs, usernameBox.getValue(), passwordBox.getValue(), exportTrackedRacesAndStartTracking, new AsyncCallback<UUID>() {
                @Override
                public void onFailure(final Throwable caught) {
                    changeButtonStateAccordingToApplicationState();
                    showErrorAlert(caught.getLocalizedMessage());
                }

                @Override
                public void onSuccess(final UUID resultId) {
                    showImportProgress(resultId);
                }
            });
        } else {
            showErrorAlert(stringMessages.importSelectAtLeastOne());
        }
    }

    private void showImportProgress(final UUID operationId) {
        final com.sap.sailing.landscape.ui.client.i18n.StringMessages importMessages =
                com.sap.sailing.landscape.ui.client.i18n.StringMessages.INSTANCE;
        final PollingProgressPanel<DataImportProgress> progressPanel = new PollingProgressPanel<>(
                importMessages.overallProgress(), importMessages.importServerError(),
                callback -> sailingServiceWrite.getImportOperationProgress(operationId, callback),
                DataImportProgress::getOverallProgressPct,
                progress -> progress.getCurrentSubProgress().getMessage(importMessages),
                DataImportProgress::getCurrentSubProgressPct,
                progress -> progress.failed() ? (progress.getErrorMessage() == null
                        ? importMessages.importServerError() : progress.getErrorMessage()) : null,
                progress -> progress.getResult() != null, new AsyncCallback<DataImportProgress>() {
                    @Override
                    public void onFailure(final Throwable caught) {
                        changeButtonStateAccordingToApplicationState();
                        showErrorAlert(caught.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(final DataImportProgress result) {
                        showCreationMessage(result.getResult());
                    }
                });
        progressPanel.setWidth("100%");
        add(progressPanel);
        progressPanel.start();
    }

    private void showCreationMessage(final MasterDataImportObjectCreationCount creationCount) {
        final int leaderboardsCreated = creationCount.getLeaderboardCount();
        final int leaderboardGroupsCreated = creationCount.getLeaderboardGroupCount();
        final int eventsCreated = creationCount.getEventCount();
        final int regattasCreated = creationCount.getRegattaCount();
        final int mediaTracksImported = creationCount.getMediaTrackCount();
        if (regattasCreated > 0) {
            presenter.getRegattasRefresher().reloadAndCallFillAll();
        }
        if (eventsCreated > 0) {
            presenter.getEventsRefresher().reloadAndCallFillAll();
        }
        if (leaderboardGroupsCreated > 0) {
            presenter.getLeaderboardGroupsRefresher().reloadAndCallFillAll();
        }
        if (leaderboardsCreated > 0) {
            presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
        }
        if (mediaTracksImported > 0) {
            presenter.getMediaTracksRefresher().reloadAndCallFillAll();
        }
        MasterDataImportNotification.notifySuccess(creationCount,
                com.sap.sailing.landscape.ui.client.i18n.StringMessages.INSTANCE);
        changeButtonStateAccordingToApplicationState();
    }

    private void disableAllButtons() {
        boolean enabled = false;
        importLeaderboardGroupsButton.setEnabled(enabled);
        overrideSwitch.setEnabled(enabled);
        fetchIdsButton.setEnabled(enabled);
    }
    
    private void changeButtonStateAccordingToApplicationState() {
        if (leaderboardgroupListBox.getItemCount() > 0 && countSelectedItems() > 0) {
            importLeaderboardGroupsButton.setEnabled(true);
        } else {
            importLeaderboardGroupsButton.setEnabled(false);
        }
        overrideSwitch.setEnabled(true);
        fetchIdsButton.setEnabled(true);
    }


    private int countSelectedItems() {
        int count = 0;
        for (int i = 0; i < leaderboardgroupListBox.getItemCount(); i++) {
            if (leaderboardgroupListBox.isItemSelected(i)) {
                count++;
            }
        }
        return count;
    }

    private UUID[] createLeaderboardGroupIdsFromListBox() {
        List<UUID> uuids = new ArrayList<UUID>();
        for (int i = 0; i < leaderboardgroupListBox.getItemCount(); i++) {
            if (leaderboardgroupListBox.isItemSelected(i)) {
                uuids.add(UUID.fromString(leaderboardgroupListBox.getValue(i)));
            }
        }
        return uuids.toArray(new UUID[uuids.size()]);
    }

    protected void fireIdRequestsAndFillLists() {
        final String host = hostBox.getText();
        final elemental2.dom.URL url = new elemental2.dom.URL(host);
        if (!SharedLandscapeConstants.isTrustedDomain(url.host)) {
            Notification.notify(stringMessages.masterDataImportOnlyFromTrustedDomains(
                    SharedLandscapeConstants.TRUSTED_DOMAINS.toString(), url.host), NotificationType.ERROR);
        } else {
            if (host != null && !host.isEmpty()) {
                fireLgIdRequestAndFillList(host);
            }
        }
    }

    private void fireLgIdRequestAndFillList(final String host) {
        currentHost = host;
        disableAllButtons();
        sailingServiceWrite.getLeaderboardGroupNamesAndIdsAsStringsFromRemoteServer(host, usernameBox.getValue(),
                passwordBox.getValue(), new AsyncCallback<Map<String, String>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        showErrorAlert(stringMessages.importGetLeaderboardsFailed(host, caught.getMessage()));
                        changeButtonStateAccordingToApplicationState();
                    }

                    @Override
                    public void onSuccess(Map<String, String> result) {
                        clearListBox();
                        allLeaderboardGroupsNameAndIdsMap = result;
                        fillLeaderboardGroupListBox(allLeaderboardGroupsNameAndIdsMap);
                        changeButtonStateAccordingToApplicationState();
                        if (!filterBox.getValue().isEmpty()) {
                            filterLeaderboardGroupList();
                        }
                    }
                });
    }

    private void showErrorAlert(String string) {
        Notification.notify(string, NotificationType.ERROR);
    }

    private void addContentToLeftPanel(VerticalPanel contentPanel) {
        contentPanel.add(new Label(stringMessages.availableLeaderboardGroups()));
        HorizontalPanel filterPanel = new HorizontalPanel();
        filterPanel.add(new Label(stringMessages.filterName() + ":"));
        filterBox = new TextBox();
        setFilterHandler(filterBox);
        filterPanel.add(filterBox);
        contentPanel.add(filterPanel);
        leaderboardgroupListBox = new ListBox();
        leaderboardgroupListBox.ensureDebugId("LeaderBoardGroupListBox");
        leaderboardgroupListBox.setMultipleSelect(true);
        addSelectionChangedListener();
        contentPanel.add(leaderboardgroupListBox);
        overrideSwitch = new CheckBox(stringMessages.importOverrideSwitchLabel());
        overrideSwitch.ensureDebugId("overrideExisting");
        overrideSwitch.setValue(false);
        contentPanel.add(overrideSwitch);
        compressSwitch = new CheckBox(stringMessages.compress());
        compressSwitch.setTitle(stringMessages.compressTooltip());
        compressSwitch.setValue(true);
        contentPanel.add(compressSwitch);
        exportWindSwitch = new CheckBox(stringMessages.importWind());
        exportWindSwitch.setTitle(stringMessages.importWindTooltip());
        exportWindSwitch.setValue(true);
        exportWindSwitch.ensureDebugId("wind");
        contentPanel.add(exportWindSwitch);
        exportDeviceConfigsSwitch = new CheckBox(stringMessages.importDeviceConfigurations());
        exportDeviceConfigsSwitch.setTitle(stringMessages.importDeviceConfigurationsTooltip());
        exportDeviceConfigsSwitch.setValue(false);
        contentPanel.add(exportDeviceConfigsSwitch);
        exportTrackedRacesAndStartTrackingSwitch = new CheckBox(stringMessages.exportTrackedRacesAndStartTracking());
        exportTrackedRacesAndStartTrackingSwitch.setTitle(stringMessages.exportTrackedRacesAndStartTrackingTooltip());
        exportTrackedRacesAndStartTrackingSwitch.setValue(true);
        contentPanel.add(exportTrackedRacesAndStartTrackingSwitch);
        importLeaderboardGroupsButton = new Button(stringMessages.importSelectedLeaderboardGroups());
        importLeaderboardGroupsButton.ensureDebugId("import");
        importLeaderboardGroupsButton.setEnabled(false);
        contentPanel.add(importLeaderboardGroupsButton);
    }

    private void addSelectionChangedListener() {
        leaderboardgroupListBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                changeButtonStateAccordingToApplicationState();
            }
        });
    }

    private void setFilterHandler(final TextBox filterBox) {
        filterBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                filterLeaderboardGroupList();
            }
        });
    }

    public void clearListBox() {
        int itemCount = leaderboardgroupListBox.getItemCount();
        for (int i = itemCount - 1; i >= 0; i--) {
            leaderboardgroupListBox.removeItem(i);
        }
    }

    private void filterLeaderboardGroupList() {
        clearListBox();
        Iterable<String> filterTexts = Util.splitAlongWhitespaceRespectingDoubleQuotedPhrases(filterBox.getText());
        final KeywordMatcher<String> matcher = new KeywordMatcher<String>() {
            @Override
            public Iterable<String> getStrings(String t) {
                return Collections.singleton(t);
            } 
        };
        if (allLeaderboardGroupsNameAndIdsMap != null) {
            Map<String, String> filteredMap = allLeaderboardGroupsNameAndIdsMap.entrySet().stream()
                    .filter(entry -> matcher.matches(filterTexts, entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            fillLeaderboardGroupListBox(filteredMap);
            changeButtonStateAccordingToApplicationState();
        }
    }

    private void fillLeaderboardGroupListBox(Map<String, String> leaderboardGroupsMap) {
        final List<Pair<String, String>> list = new ArrayList<>();
        for (final String key : leaderboardGroupsMap.keySet()) {
            list.add(new Pair<>(leaderboardGroupsMap.get(key), key));
        }
        Collections.sort(list, (a, b)->a.getA().compareTo(b.getA()));
        for (final Pair<String, String> pair : list) {
            leaderboardgroupListBox.addItem(pair.getA(), pair.getB());
        }
        leaderboardgroupListBox.setVisibleItemCount(list.size());
    }

}
