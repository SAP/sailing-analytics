package com.sap.sailing.gwt.ui.masterdataimport;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
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
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.filter.impl.KeywordMatcher;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.progressbar.CustomProgressBar;
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
        UUID[] leaderboardGroupIds = createLeaderboardGroupIdsFromListBox();
        final Label overallName = new Label(stringMessages.overallProgress() + ":");
        this.add(overallName);
        final CustomProgressBar overallProgressBar = CustomProgressBar.determinate();
        overallProgressBar.ensureDebugId("overallProgressBar");
        this.add(overallProgressBar);
        final Label subProgressName = new Label();
        this.add(subProgressName);
        final CustomProgressBar subProgressBar = CustomProgressBar.determinate();
        this.add(subProgressBar);
        if (leaderboardGroupIds.length >= 1) {
            disableAllButtons();
            boolean override = overrideSwitch.getValue();
            boolean compress = compressSwitch.getValue();
            boolean exportWind = exportWindSwitch.getValue();
            boolean exportDeviceConfigs = exportDeviceConfigsSwitch.getValue();
            boolean exportTrackedRacesAndStartTracking = exportTrackedRacesAndStartTrackingSwitch.getValue();
            sailingServiceWrite.importMasterData(currentHost, leaderboardGroupIds, override, compress, exportWind,
                    exportDeviceConfigs, usernameBox.getValue(), passwordBox.getValue(), exportTrackedRacesAndStartTracking, new AsyncCallback<UUID>() {
                @Override
                public void onFailure(Throwable caught) {
                    showErrorAlert(caught.getLocalizedMessage());
                }

                @Override
                public void onSuccess(final UUID resultId) {
                    final Timer timer = new Timer() {
                        @Override
                        public void run() {
                            sailingServiceWrite.getImportOperationProgress(resultId,
                                    new AsyncCallback<DataImportProgress>() {
                                        @Override
                                        public void onFailure(Throwable caught) {
                                            showErrorAlert(stringMessages.importServerError());
                                        }

                                        @Override
                                        public void onSuccess(DataImportProgress result) {
                                            if (result == null) {
                                                showErrorAlert(stringMessages.importServerError());
                                            } else {
                                                if (result.failed()) {
                                                    // Got error, cancel timer
                                                    cancel();
                                                    deleteProgressIndication(overallName, overallProgressBar,
                                                            subProgressName, subProgressBar);

                                                    showErrorAlert(result.getErrorMessage());
                                                    changeButtonStateAccordingToApplicationState();
                                                } else {
                                                    MasterDataImportObjectCreationCount creationCount = result.getResult();
                                                    if (creationCount != null) {
                                                        // Got result, cancel timer
                                                        cancel();
                                                        deleteProgressIndication(overallName, overallProgressBar,
                                                                subProgressName, subProgressBar);
                                                        showCreationMessage(creationCount);
                                                    } else {
                                                        overallProgressBar.setValue(result.getOverallProgressPct());
                                                        subProgressName.setText(result.getCurrentSubProgress()
                                                                .getMessage(stringMessages));
                                                        subProgressBar.setValue(result.getCurrentSubProgressPct());
                                                    }
                                                }
                                            }
                                        }
                                    });
                        }
                    };
                    timer.scheduleRepeating(5000);
                }
            });
        } else {
            showErrorAlert(stringMessages.importSelectAtLeastOne());
        }
    }

    private void showCreationMessage(MasterDataImportObjectCreationCount creationCount) {
        int leaderboardsCreated = creationCount.getLeaderboardCount();
        int leaderboardGroupsCreated = creationCount.getLeaderboardGroupCount();
        int eventsCreated = creationCount.getEventCount();
        int regattasCreated = creationCount.getRegattaCount();
        int mediaTracksImported = creationCount.getMediaTrackCount();
        int trackedRacesImported = creationCount.getTrackedRacesCount();
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
        final Iterable<String> overwrittenRegattas = creationCount.getNamesOfOverwrittenRegattaNames();
        showSuccessAlert(leaderboardsCreated, leaderboardGroupsCreated, eventsCreated, regattasCreated,
                mediaTracksImported, trackedRacesImported, overwrittenRegattas);
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

    protected void showSuccessAlert(int leaderboardsCreated, int leaderboardGroupsCreated, int eventsCreated,
            int regattasCreated, int mediaTracksImported, int trackedRacesImported, Iterable<String> overwrittenRegattas) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(stringMessages.importSuccess(leaderboardGroupsCreated, leaderboardsCreated, eventsCreated,
                regattasCreated, mediaTracksImported, trackedRacesImported));
        if (!Util.isEmpty(overwrittenRegattas)) {
            buffer.append("\n\n" + stringMessages.importSuccessOverwriteInfo() + "\n");
            for (String regattaName : overwrittenRegattas) {
                buffer.append(regattaName + "\n");
            }
        }
        Notification.notify(buffer.toString(), NotificationType.SUCCESS);

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
        String host = hostBox.getText();
        if (host != null && !host.isEmpty()) {
            fireLgIdRequestAndFillList(host);
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
        Map<String, String> filteredMap = allLeaderboardGroupsNameAndIdsMap.entrySet().stream()
                .filter(entry -> matcher.matches(filterTexts, entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        fillLeaderboardGroupListBox(filteredMap);
        changeButtonStateAccordingToApplicationState();
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

    private void deleteProgressIndication(IsWidget... widgetsToRemove) {
        Arrays.asList(widgetsToRemove).forEach(this::remove);
    }
}
