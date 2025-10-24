package com.sap.sailing.landscape.ui.client;

import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.ui.client.CreateApplicationReplicaSetDialog.CreateApplicationReplicaSetInstructions;
import com.sap.sailing.landscape.ui.client.MoveMasterProcessDialog.MoveMasterToOtherInstanceInstructions;
import com.sap.sailing.landscape.ui.client.SwitchToReplicaOnSharedInstanceDialog.SwitchToReplicaOnSharedInstanceDialogInstructions;
import com.sap.sailing.landscape.ui.client.UpgradeApplicationReplicaSetDialog.UpgradeApplicationReplicaSetInstructions;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sailing.landscape.ui.shared.AmazonMachineImageDTO;
import com.sap.sailing.landscape.ui.shared.AvailabilityZoneDTO;
import com.sap.sailing.landscape.ui.shared.AwsInstanceDTO;
import com.sap.sailing.landscape.ui.shared.CompareServersResultDTO;
import com.sap.sailing.landscape.ui.shared.MongoEndpointDTO;
import com.sap.sailing.landscape.ui.shared.MongoScalingInstructionsDTO;
import com.sap.sailing.landscape.ui.shared.ProcessDTO;
import com.sap.sailing.landscape.ui.shared.ReleaseDTO;
import com.sap.sailing.landscape.ui.shared.ReverseProxyDTO;
import com.sap.sailing.landscape.ui.shared.SSHKeyPairDTO;
import com.sap.sailing.landscape.ui.shared.SailingAnalyticsProcessDTO;
import com.sap.sailing.landscape.ui.shared.SailingApplicationReplicaSetDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.ActionsColumn;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.TableWrapperWithMultiSelectionAndFilter;
import com.sap.sse.gwt.client.celltable.TableWrapperWithSingleSelectionAndFilter;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.landscape.aws.common.shared.RedirectDTO;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.SelectedElementsCountingButton;

/**
 * A panel for managing an SAP Sailing Analytics landscape in the AWS cloud. The main widgets offered will address the
 * following areas:
 * <ul>
 * <li>AWS Credentials: manage an AWS key (with persistence) and the corresponding secret (without persistence, maybe
 * provided by the browser's password manager)</li>
 * <li>SSH Key Pairs: generate, import, export, and deploy SSH key pairs used for spinning up and connect to compute
 * instances</li>
 * <li>Application server replica sets: single process per instance, or multiple processes per instance; with or without
 * auto scaling groups and launch templates for auto-scaling the number of replicas; change the software version running on an application server
 * replica set while maintaining availability as good as possible by de-registering the master instance from the master target group, then
 * spinning up a new master, then any desired number of replicas, then swap the old replicas for the new replicas in the public target group
 * and register the master instance again.</li>
 * <li>MongoDB replica sets: single node or true replica set; scale out / in by adding / removing instances</li>
 * <li>RabbitMQ infrastructure: usually a single node per region</li>
 * <li>Central Reverse Proxies: currently a single node per region, but ideally this could potentially be a
 * multi-instance scenario for high availability with those instances sharing a common configuration; eventually, the
 * functionality of these reverse proxies may be taken over by the AWS Application Load Balancer component, such as
 * central logging as well as specific re-write rules, SSL offloading, certificate management, and http-to-https
 * forwarding.</li>
 * <li>Amazon Machine Images (AMIs) of different types: the {@code image-type} tag tells the type; images and their
 * snapshots can exist in one or more versions, and updates can be triggered explicitly. Maybe at some point we would
 * have automated tests asserting that an update went well.</li>
 * </ul>
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class LandscapeManagementPanel extends SimplePanel {
    private final LandscapeManagementWriteServiceAsync landscapeManagementService;
    private final TableWrapperWithSingleSelectionAndFilter<String, StringMessages, AdminConsoleTableResources> regionsTable;
    private final TableWrapperWithSingleSelectionAndFilter<MongoEndpointDTO, StringMessages, AdminConsoleTableResources> mongoEndpointsTable;
    private final BusyIndicator mongoEndpointsBusy;
    private final TableWrapperWithSingleSelectionAndFilter<AmazonMachineImageDTO, StringMessages, AdminConsoleTableResources> machineImagesTable;
    private final BusyIndicator machineImagesBusy;
    private final SshKeyManagementPanel sshKeyManagementPanel;
    private final TableWrapperWithMultiSelectionAndFilter<SailingApplicationReplicaSetDTO<String>, StringMessages, AdminConsoleTableResources> applicationReplicaSetsTable;
    private final SimpleBusyIndicator applicationReplicaSetsBusy;
    private final ErrorReporter errorReporter;
    private final AwsMfaLoginWidget mfaLoginWidget;
    private TableWrapperWithMultiSelectionAndFilter<ReverseProxyDTO, StringMessages, AdminConsoleTableResources> proxiesTable;
    private final BusyIndicator proxiesTableBusy;
    private final static String AWS_DEFAULT_REGION_USER_PREFERENCE = "aws.region.default";
    private final static Duration DURATION_TO_WAIT_BETWEEN_REPLICA_SET_UPGRADE_REQUESTS = Duration.ONE_MINUTE;
    /**
     * The time to wait after archiving a replica set and before starting a "compare servers" run for the content
     * archived. This waiting period is owed to the process of loading the race content which is an asynchronous
     * process running mainly in the "background" and after the master data import has reported "completion."
     */
    private static final Duration DEFAULT_DURATION_TO_WAIT_BEFORE_COMPARE_SERVERS = Duration.ONE_MINUTE.times(5);
    private static final int DEFAULT_NUMBER_OF_COMPARE_SERVERS_ATTEMPTS = 5;
    
    //Spacing for Setupbar (AWS/SSH/REGION)
    private static final int DEFAULT_SETUPBAR_HEIGHT = 300;
    private static final int DEFAULT_AMOUNT_SSHKEYS_PER_PAGE = 3;

    public LandscapeManagementPanel(StringMessages stringMessages, UserService userService,
            AdminConsoleTableResources tableResources, ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        landscapeManagementService = initAndRegisterLandscapeManagementService();
        final VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        this.add(mainPanel);
        final HorizontalPanel awsCredentialsAndSshKeys = new HorizontalPanel();
        mainPanel.add(awsCredentialsAndSshKeys);
        final CaptionPanel awsCredentialsPanel = new CaptionPanel(stringMessages.awsCredentials());
        awsCredentialsPanel.setHeight("" + DEFAULT_SETUPBAR_HEIGHT + "px");
        awsCredentialsAndSshKeys.add(awsCredentialsPanel);
        mfaLoginWidget = new AwsMfaLoginWidget(landscapeManagementService, errorReporter, userService, stringMessages);
        mfaLoginWidget.addListener(validSession->refreshAllThatNeedsAwsCredentials());
        awsCredentialsPanel.add(mfaLoginWidget);       
        regionsTable = new TableWrapperWithSingleSelectionAndFilter<String, StringMessages, AdminConsoleTableResources>(
                stringMessages, errorReporter, /* enablePager */ false,
                /* entity identity comparator */ Optional.empty(), GWT.create(AdminConsoleTableResources.class),
                /* checkbox filter function */ Optional.empty(), /* filter label */ Optional.empty(),
                /* filter checkbox label */ null) {
            @Override
            protected Iterable<String> getSearchableStrings(String t) {
                return Collections.singleton(t);
            }
        };
        sshKeyManagementPanel = new SshKeyManagementPanel(stringMessages, userService,
                landscapeManagementService, tableResources, errorReporter, /* access key provider */ mfaLoginWidget, regionsTable.getSelectionModel(), DEFAULT_AMOUNT_SSHKEYS_PER_PAGE);
        final CaptionPanel sshKeysCaptionPanel = new CaptionPanel(stringMessages.sshKeys());
        sshKeysCaptionPanel.setHeight("" + DEFAULT_SETUPBAR_HEIGHT + "px");
        regionsTable.addColumn(new TextColumn<String>() {
            @Override
            public String getValue(String s) {
                return s;
            }
        }, stringMessages.region(), new NaturalComparator());
        final CaptionPanel regionsCaptionPanel = new CaptionPanel(stringMessages.region());
        final SimplePanel regionPanel = new  SimplePanel();
        final ScrollPanel regionScrollPanel = new ScrollPanel();
        regionsCaptionPanel.setHeight("" + DEFAULT_SETUPBAR_HEIGHT + "px");
        regionScrollPanel.setHeight("" + (DEFAULT_SETUPBAR_HEIGHT - 35)  + "px");
        regionScrollPanel.setAlwaysShowScrollBars(true);
        regionPanel.add(regionsTable);
        regionScrollPanel.add(regionPanel);
        regionsCaptionPanel.add(regionScrollPanel); 
        awsCredentialsAndSshKeys.add(regionsCaptionPanel);
        refreshRegionsTable(userService);
        awsCredentialsAndSshKeys.add(sshKeysCaptionPanel);
        sshKeysCaptionPanel.add(sshKeyManagementPanel);
        
        // MongoDB endpoints:
        mongoEndpointsTable = new TableWrapperWithSingleSelectionAndFilter<MongoEndpointDTO, StringMessages, AdminConsoleTableResources>(
                stringMessages, errorReporter, /* enablePager */ false,
                /* entity identity comparator */ Optional.empty(), GWT.create(AdminConsoleTableResources.class),
                /* checkbox filter function */ Optional.empty(), /* filter label */ Optional.empty(),
                /* filter checkbox label */ null) {
            @Override
            protected Iterable<String> getSearchableStrings(MongoEndpointDTO mongoEndpointDTO) {
                final Set<String> result = new HashSet<>();
                if (mongoEndpointDTO.getReplicaSetName() != null) {
                    result.add(mongoEndpointDTO.getReplicaSetName());
                }
                for (final ProcessDTO hostnameAndPort : mongoEndpointDTO.getHostnamesAndPorts()) {
                    result.add(hostnameAndPort.getHost().getInstanceId());
                    result.add(hostnameAndPort.getHostname());
                    result.add(Integer.toString(hostnameAndPort.getPort()));
                }
                return result;
            }
        };
        mongoEndpointsTable.addColumn(mongoEndpointDTO->mongoEndpointDTO.getReplicaSetName(), stringMessages.replicaSet());
        mongoEndpointsTable.addColumn(new TextColumn<MongoEndpointDTO>() {
            @Override
            public String getValue(MongoEndpointDTO mongoEndpointDTO) {
                return Util.joinStrings(",",
                        Util.map(mongoEndpointDTO.getHostnamesAndPorts(),
                                hostnameAndPort -> hostnameAndPort.getHostname() + ":" + hostnameAndPort.getPort()
                                        + " (" + hostnameAndPort.getHost().getInstanceId() + ")"));
            }
        }, stringMessages.hostname());
        final ActionsColumn<MongoEndpointDTO, MongoEndpointsImagesBarCell> mongoEndpointsActionColumn = new ActionsColumn<MongoEndpointDTO, MongoEndpointsImagesBarCell>(
                new MongoEndpointsImagesBarCell(stringMessages), /* permission checker */ (mongoEndpoint, action)->true);
        mongoEndpointsActionColumn.addAction(MongoEndpointsImagesBarCell.ACTION_SCALE,
                mongoEndpointToScale -> scaleMongoEndpoint(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(), mongoEndpointToScale));
        mongoEndpointsTable.addColumn(mongoEndpointsActionColumn, stringMessages.actions());
        final CaptionPanel mongoEndpointsCaptionPanel = new CaptionPanel(stringMessages.mongoEndpoints());
        final VerticalPanel mongoEndpointsVerticalPanel = new VerticalPanel();
        mongoEndpointsCaptionPanel.add(mongoEndpointsVerticalPanel);
        mongoEndpointsVerticalPanel.add(mongoEndpointsTable);
        mongoEndpointsBusy = new SimpleBusyIndicator();
        mongoEndpointsVerticalPanel.add(mongoEndpointsBusy);
        mainPanel.add(mongoEndpointsCaptionPanel);
        // application replica sets:
        applicationReplicaSetsTable = new TableWrapperWithMultiSelectionAndFilter<SailingApplicationReplicaSetDTO<String>, StringMessages, AdminConsoleTableResources>(
                stringMessages, errorReporter, /* enablePager */ false,
                /* entity identity comparator */ Optional.of(new EntityIdentityComparator<SailingApplicationReplicaSetDTO<String>>() {
                    @Override
                    public boolean representSameEntity(SailingApplicationReplicaSetDTO<String> dto1,
                            SailingApplicationReplicaSetDTO<String> dto2) {
                        return dto1.getName().equals(dto2.getName());
                    }

                    @Override
                    public int hashCode(SailingApplicationReplicaSetDTO<String> t) {
                        return t.getName().hashCode();
                    }
                }), GWT.create(AdminConsoleTableResources.class),
                /* checkbox filter function */ Optional.empty(), /* filter label */ Optional.empty(),
                /* filter checkbox label */ null) {
            @Override
            protected Iterable<String> getSearchableStrings(SailingApplicationReplicaSetDTO<String> t) {
                final Set<String> result = new HashSet<>();
                result.add(t.getReplicaSetName());
                result.add(t.getMaster().getHostname());
                result.add(Integer.toString(t.getMaster().getPort()));
                result.add(t.getMaster().getServerName());
                result.add(t.getMaster().getHost().getInstanceId());
                if (t.getAutoScalingGroupAmiId() != null) {
                    result.add(t.getAutoScalingGroupAmiId());
                }
                for (final SailingAnalyticsProcessDTO replica : t.getReplicas()) {
                    result.add(replica.getHostname());
                    result.add(replica.getServerName());
                    result.add(replica.getHost().getInstanceId());
                }
                return result;
            }
        };
        applicationReplicaSetsTable.addColumn(rs->rs.getReplicaSetName(), stringMessages.name(), (rs1, rs2)->rs1.getReplicaSetName().toLowerCase().compareTo(rs2.getReplicaSetName().toLowerCase()));
        final SafeHtmlCell versionCell = new SafeHtmlCell();
        final Column<SailingApplicationReplicaSetDTO<String>, SafeHtml> versionColumn = new Column<SailingApplicationReplicaSetDTO<String>, SafeHtml>(versionCell) {
            @Override
            public SafeHtml getValue(SailingApplicationReplicaSetDTO<String> replicaSet) {
                return new LinkBuilder().setReplicaSet(replicaSet).setPathMode(LinkBuilder.pathModes.Version).build();
            }
        };
        applicationReplicaSetsTable.addColumn(versionColumn, stringMessages.versionHeader(), (rs1, rs2)->new NaturalComparator().compare(rs1.getVersion(), rs2.getVersion()));
        final SafeHtmlCell masterCell = new SafeHtmlCell();
        final Column<SailingApplicationReplicaSetDTO<String>, SafeHtml> masterColumn = new Column<SailingApplicationReplicaSetDTO<String>, SafeHtml>(masterCell) {
            @Override
            public SafeHtml getValue(SailingApplicationReplicaSetDTO<String> replicaSet) {
                return new LinkBuilder().setPathMode(LinkBuilder.pathModes.MasterHost).setReplicaSet(replicaSet).build();
            }
        };
        applicationReplicaSetsTable.addColumn(masterColumn, stringMessages.masterHostName(), (rs1, rs2)->new NaturalComparator().compare(rs1.getMaster().getHost().getPublicIpAddress(), rs2.getMaster().getHost().getPublicIpAddress()));
        applicationReplicaSetsTable.addColumn(rs->Integer.toString(rs.getMaster().getPort()), stringMessages.masterPort());
        final SafeHtmlCell hostnameCell = new SafeHtmlCell();
        final Column<SailingApplicationReplicaSetDTO<String>, SafeHtml> hostnameColumn = new Column<SailingApplicationReplicaSetDTO<String>, SafeHtml>(hostnameCell) {
            @Override
            public SafeHtml getValue(SailingApplicationReplicaSetDTO<String> replicaSet) {
                return new LinkBuilder().setReplicaSet(replicaSet).setPathMode(LinkBuilder.pathModes.Hostname).build()
;            }
        };
        applicationReplicaSetsTable.addColumn(hostnameColumn, stringMessages.hostname(), (rs1, rs2)->new NaturalComparator().compare(rs1.getHostname(), rs2.getMaster().getHostname()));
        final SafeHtmlCell masterInstanceIdCell = new SafeHtmlCell();
        final Column<SailingApplicationReplicaSetDTO<String>, SafeHtml> masterInstanceIdColumn = new Column<SailingApplicationReplicaSetDTO<String>, SafeHtml>(masterInstanceIdCell) {
            @Override
            public SafeHtml getValue(SailingApplicationReplicaSetDTO<String> replicaSet) {
                return new LinkBuilder().setRegion(regionsTable.getSelectionModel().getSelectedObject()).setInstanceId(replicaSet.getMaster().getHost().getInstanceId()).setPathMode(LinkBuilder.pathModes.InstanceSearch).build();
            }
        };
        applicationReplicaSetsTable.addColumn(masterInstanceIdColumn, stringMessages.masterInstanceId(),
                (rs1, rs2)->new NaturalComparator().compare(rs1.getMaster().getHost().getInstanceId(), rs2.getMaster().getHost().getInstanceId()));
        applicationReplicaSetsTable.addColumn(rs->""+rs.getMaster().getStartTimePoint(), stringMessages.startTimePoint(),
                (rs1, rs2)->Comparator.nullsLast(Comparator.<TimePoint>naturalOrder()).compare(rs1.getMaster().getStartTimePoint(), rs2.getMaster().getStartTimePoint()));
        final SafeHtmlCell replicasCell = new SafeHtmlCell();
        final Column<SailingApplicationReplicaSetDTO<String>, SafeHtml> replicasColumn = new Column<SailingApplicationReplicaSetDTO<String>, SafeHtml>(replicasCell) {
            @Override
            public SafeHtml getValue(SailingApplicationReplicaSetDTO<String> replicaSet) {
                LinkBuilder linkBuilder = new LinkBuilder();
                linkBuilder.setReplicaSet(replicaSet);
                linkBuilder.setPathMode(LinkBuilder.pathModes.ReplicaLinks).setRegion(regionsTable.getSelectionModel().getSelectedObject());
                return linkBuilder.build();
            }
        };
        applicationReplicaSetsTable.addColumn(replicasColumn, stringMessages.replicas());
        applicationReplicaSetsTable.addColumn(rs->rs.getDefaultRedirectPath(), stringMessages.defaultRedirectPath());
        final SafeHtmlCell autoScalingGroupAmiIdCell = new SafeHtmlCell();
        final Column<SailingApplicationReplicaSetDTO<String>, SafeHtml> autoScalingGroupAmiIdColumn = new Column<SailingApplicationReplicaSetDTO<String>, SafeHtml>(autoScalingGroupAmiIdCell) {
            @Override
            public SafeHtml getValue(SailingApplicationReplicaSetDTO<String> replicaSet) {
                return new LinkBuilder().setReplicaSet(replicaSet).setRegion(regionsTable.getSelectionModel().getSelectedObject()).setPathMode(LinkBuilder.pathModes.AmiSearch).build();
            }
        };
        applicationReplicaSetsTable.addColumn(autoScalingGroupAmiIdColumn, stringMessages.machineImageId(), (rs1, rs2)->new NaturalComparator().compare(rs1.getAutoScalingGroupAmiId(), rs2.getAutoScalingGroupAmiId()));
        final ActionsColumn<SailingApplicationReplicaSetDTO<String>, ApplicationReplicaSetsImagesBarCell> applicationReplicaSetsActionColumn = new ActionsColumn<SailingApplicationReplicaSetDTO<String>, ApplicationReplicaSetsImagesBarCell>(
                new ApplicationReplicaSetsImagesBarCell(userService, stringMessages), /* permission checker */ (applicationReplicaSet, action)->true);
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_ARCHIVE,
                applicationReplicaSetToArchive -> archiveApplicationReplicaSet(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(), applicationReplicaSetToArchive));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_UPGRADE,
                applicationReplicaSetToUpgrade -> upgradeApplicationReplicaSet(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(), Collections.singleton(applicationReplicaSetToUpgrade)));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_DEFINE_LANDING_PAGE,
                applicationReplicaSetForWhichToDefineLandingPage -> defineLandingPage(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(), applicationReplicaSetForWhichToDefineLandingPage));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_CREATE_LOAD_BALANCER_MAPPING,
                applicationReplicaSetForWhichToDefineLoadBalancerMapping -> createDefaultLoadBalancerMappings(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(), applicationReplicaSetForWhichToDefineLoadBalancerMapping));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_LAUNCH_ANOTHER_REPLICA_SET_ON_THIS_MASTER,
                applicationReplicaSetOnWhichToDeployMaster -> createApplicationReplicaSetWithMasterOnExistingHost(stringMessages,
                        applicationReplicaSetOnWhichToDeployMaster));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_REMOVE,
                applicationReplicaSetToRemove -> removeApplicationReplicaSet(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(), applicationReplicaSetToRemove));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_ENSURE_ONE_REPLICA_THEN_STOP_REPLICATING_AND_REMOVE_MASTER_FROM_TARGET_GROUPS,
                applicationReplicaSetForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups ->
                    ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(),
                        Collections.singleton(applicationReplicaSetForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups)));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_SWITCH_TO_REPLICA_ON_SHARED_INSTANCE,
                applicationReplicaSetToUpgrade -> switchToReplicaOnSharedInstance(stringMessages,
                        Collections.singleton(applicationReplicaSetToUpgrade)));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_SWITCH_TO_AUTO_SCALING_REPLICAS_ONLY,
                applicationReplicaSetToUpgrade -> switchToAutoScalingReplicasOnly(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(), Collections.singleton(applicationReplicaSetToUpgrade)));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_SCALE_AUTO_SCALING_REPLICAS_UP_DOWN,
                applicationReplicaSetToUpgrade -> scaleAutoScalingReplicasUpDown(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(),
                        Collections.singleton(applicationReplicaSetToUpgrade)));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_OPEN_SHARD_MANAGEMENT,
                selectedReplicaSet -> openShardManagementPanel(stringMessages, regionsTable.getSelectionModel().getSelectedObject(), selectedReplicaSet,
                        sshKeyManagementPanel.getSelectedKeyPair().getName(),
                        sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                                ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes()
                                : null));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_MOVE_ALL_APPLICATION_PROCESSES_AWAY_FROM,
                applicationReplicaSetWhoseMastersHostToDecommission -> moveAllApplicationProcessesAwayFrom(stringMessages,
                        applicationReplicaSetWhoseMastersHostToDecommission));
        // see below for the finalization o the applicationRelicaSetsActionColumn; we need to have the machineImagesTable ready for the last action...
        final CaptionPanel applicationReplicaSetsCaptionPanel = new CaptionPanel(stringMessages.applicationReplicaSets());
        final VerticalPanel applicationReplicaSetsVerticalPanel = new VerticalPanel();
        final HorizontalPanel applicationReplicaSetsButtonPanel = new HorizontalPanel();
        applicationReplicaSetsVerticalPanel.add(applicationReplicaSetsButtonPanel);
        final Button applicationReplicaSetsRefreshButton = new Button(stringMessages.refresh());
        applicationReplicaSetsButtonPanel.add(applicationReplicaSetsRefreshButton);
        applicationReplicaSetsRefreshButton.addClickHandler(e->refreshApplicationReplicaSetsTable());
        final Button addApplicationReplicaSetButton = new Button(stringMessages.add());
        applicationReplicaSetsButtonPanel.add(addApplicationReplicaSetButton);
        addApplicationReplicaSetButton.addClickHandler(e->createApplicationReplicaSet(stringMessages, regionsTable.getSelectionModel().getSelectedObject()));
        final SelectedElementsCountingButton<SailingApplicationReplicaSetDTO<String>> removeApplicationReplicaSetButton = new SelectedElementsCountingButton<>(
                stringMessages.remove(), applicationReplicaSetsTable.getSelectionModel(), /* element name mapper */ rs -> rs.getName(),
                StringMessages.INSTANCE::doYouReallyWantToRemoveSelectedElements,
                e -> removeApplicationReplicaSets(stringMessages, regionsTable.getSelectionModel().getSelectedObject(), applicationReplicaSetsTable.getSelectionModel().getSelectedSet()));
        disableButtonWhenLocalReplicaSetIsSelected(removeApplicationReplicaSetButton, userService);
        disableButtonWhenArchive(removeApplicationReplicaSetButton);
        applicationReplicaSetsButtonPanel.add(removeApplicationReplicaSetButton);
        final SelectedElementsCountingButton<SailingApplicationReplicaSetDTO<String>> upgradeApplicationReplicaSetButton = new SelectedElementsCountingButton<>(
                stringMessages.upgrade(), applicationReplicaSetsTable.getSelectionModel(),
                e->upgradeApplicationReplicaSet(stringMessages, regionsTable.getSelectionModel().getSelectedObject(),
                        applicationReplicaSetsTable.getSelectionModel().getSelectedSet()));
        disableButtonWhenLocalReplicaSetIsSelected(upgradeApplicationReplicaSetButton, userService);
        disableButtonWhenArchive(upgradeApplicationReplicaSetButton);
        applicationReplicaSetsButtonPanel.add(upgradeApplicationReplicaSetButton);
        final SelectedElementsCountingButton<SailingApplicationReplicaSetDTO<String>> stopReplicatingAndUnregisterMasterButton = new SelectedElementsCountingButton<>(
                stringMessages.stopReplicating(), applicationReplicaSetsTable.getSelectionModel(),
                e->ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(stringMessages, regionsTable.getSelectionModel().getSelectedObject(),
                        applicationReplicaSetsTable.getSelectionModel().getSelectedSet()));
        applicationReplicaSetsButtonPanel.add(stopReplicatingAndUnregisterMasterButton);
        final SelectedElementsCountingButton<SailingApplicationReplicaSetDTO<String>> useOnlyAutoScalingReplicasButton = new SelectedElementsCountingButton<>(
                stringMessages.switchToAutoScalingReplicasOnly(), applicationReplicaSetsTable.getSelectionModel(),
                e->switchToAutoScalingReplicasOnly(stringMessages, regionsTable.getSelectionModel().getSelectedObject(),
                        applicationReplicaSetsTable.getSelectionModel().getSelectedSet()));
        applicationReplicaSetsButtonPanel.add(useOnlyAutoScalingReplicasButton);
        final SelectedElementsCountingButton<SailingApplicationReplicaSetDTO<String>> useSharedInsteadOfDedicatedAutoScalingReplicasButton = new SelectedElementsCountingButton<>(
                stringMessages.switchToReplicaOnSharedInstance(), applicationReplicaSetsTable.getSelectionModel(),
                e->switchToReplicaOnSharedInstance(stringMessages, applicationReplicaSetsTable.getSelectionModel().getSelectedSet()));
        applicationReplicaSetsButtonPanel.add(useSharedInsteadOfDedicatedAutoScalingReplicasButton);
        disableButtonWhenArchive(useSharedInsteadOfDedicatedAutoScalingReplicasButton);
        final SelectedElementsCountingButton<SailingApplicationReplicaSetDTO<String>> scaleAutoScalingReplicasUpDown = new SelectedElementsCountingButton<>(
                stringMessages.scaleAutoScalingReplicasUpOrDown(), applicationReplicaSetsTable.getSelectionModel(),
                e->scaleAutoScalingReplicasUpDown(stringMessages, regionsTable.getSelectionModel().getSelectedObject(),
                        applicationReplicaSetsTable.getSelectionModel().getSelectedSet()));
        disableButtonWhenArchive(scaleAutoScalingReplicasUpDown);
        applicationReplicaSetsButtonPanel.add(scaleAutoScalingReplicasUpDown);
        applicationReplicaSetsCaptionPanel.add(applicationReplicaSetsVerticalPanel);
        applicationReplicaSetsVerticalPanel.add(applicationReplicaSetsTable);
        applicationReplicaSetsBusy = new SimpleBusyIndicator();
        applicationReplicaSetsVerticalPanel.add(applicationReplicaSetsBusy);
        mainPanel.add(applicationReplicaSetsCaptionPanel);
        // machine images:
        machineImagesTable = new TableWrapperWithSingleSelectionAndFilter<AmazonMachineImageDTO, StringMessages, AdminConsoleTableResources>(
                stringMessages, errorReporter, /* enablePager */ false,
                /* entity identity comparator */ Optional.empty(), GWT.create(AdminConsoleTableResources.class),
                /* checkbox filter function */ Optional.of(this::isNewest), /* filter label */ Optional.empty(),
                /* filter checkbox label */ stringMessages.showNewestOnlyPerType()) {
            @Override
            protected Iterable<String> getSearchableStrings(AmazonMachineImageDTO t) {
                return Arrays.asList(t.getRegionId(), t.getId(), t.getName(), t.getState() );
            }
        };
        // the button action needs the machineImagesTable initialized already
        final SelectedElementsCountingButton<SailingApplicationReplicaSetDTO<String>> updateAutoScalingReplicaAmisButton = new SelectedElementsCountingButton<>(
                stringMessages.updateAmiForAutoScalingReplicas(), applicationReplicaSetsTable.getSelectionModel(),
                e->updateAutoScalingReplicaAmi(stringMessages, regionsTable.getSelectionModel().getSelectedObject(),
                        applicationReplicaSetsTable.getSelectionModel().getSelectedSet(), machineImagesTable.getSelectionModel().getSelectedObject()));
        applicationReplicaSetsButtonPanel.add(updateAutoScalingReplicaAmisButton);
        // Need to initialize this action after the machineImagesTable has been created
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_UPDATE_AMI_FOR_AUTO_SCALING_REPLICAS,
                applicationReplicaSetToUpdateAutoScalingReplicaAmiFor -> updateAutoScalingReplicaAmi(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(),
                        Collections.singleton(applicationReplicaSetToUpdateAutoScalingReplicaAmiFor),
                        // if a specific sailing-analytics-server machine image was selected, use it:
                        machineImagesTable.getSelectionModel().getSelectedObject()==null || machineImagesTable.getSelectionModel().getSelectedObject().getType().equals(SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING) ?
                                null : machineImagesTable.getSelectionModel().getSelectedObject()));
        applicationReplicaSetsActionColumn.addAction(ApplicationReplicaSetsImagesBarCell.ACTION_MOVE_MASTER_TO_OTHER_INSTANCE,
                applicationReplicaSetToMoveMasterFor -> moveMasterToOtherInstance(stringMessages,
                        regionsTable.getSelectionModel().getSelectedObject(),
                        Collections.singleton(applicationReplicaSetToMoveMasterFor)));
        applicationReplicaSetsTable.addColumn(applicationReplicaSetsActionColumn, stringMessages.actions());
        
        final SafeHtmlCell amiIdCell = new SafeHtmlCell();
        final Column<AmazonMachineImageDTO, SafeHtml> amiIdColumn = new Column<AmazonMachineImageDTO, SafeHtml>(amiIdCell) {
            @Override
            public SafeHtml getValue(AmazonMachineImageDTO ami) {
                return new LinkBuilder()
                        .setRegion(regionsTable.getSelectionModel().getSelectedObject())
                        .setAmiId(ami.getId())
                        .setPathMode(LinkBuilder.pathModes.InstanceByAmiIdSearch)
                        .build();
            }
        };
        machineImagesTable.addColumn(amiIdColumn, stringMessages.id());
        machineImagesTable.addColumn(object->object.getRegionId(), stringMessages.region());
        machineImagesTable.addColumn(object->object.getName(), stringMessages.name());
        machineImagesTable.addColumn(object->object.getType(), stringMessages.imageType());
        machineImagesTable.addColumn(object->object.getState(), stringMessages.state());
        machineImagesTable.addColumn(new TextColumn<AmazonMachineImageDTO>() {
            @Override
            public String getValue(AmazonMachineImageDTO object) {
                return object.getCreationTimePoint().toString();
            }
        }, stringMessages.createdAt(), Comparator.nullsLast((t1, t2)->t1.getCreationTimePoint().compareTo(t2.getCreationTimePoint())));
        final ActionsColumn<AmazonMachineImageDTO, AmazonMachineImagesImagesBarCell> machineImagesActionColumn = new ActionsColumn<AmazonMachineImageDTO, AmazonMachineImagesImagesBarCell>(
                new AmazonMachineImagesImagesBarCell(stringMessages), /* permission checker */ (machineImage, action)->true);
        machineImagesActionColumn.addAction(AmazonMachineImagesImagesBarCell.ACTION_REMOVE, DefaultActions.DELETE, machineImageToRemove->removeMachineImage(stringMessages, machineImageToRemove));
        machineImagesActionColumn.addAction(AmazonMachineImagesImagesBarCell.ACTION_UPGRADE,
                machineImageToUpgrade->upgradeMachineImage(stringMessages, machineImageToUpgrade, getApplicationReplicaSetsToUpgradeAutoScalingReplicaAmisFor(machineImageToUpgrade)));
        machineImagesTable.addColumn(machineImagesActionColumn, stringMessages.actions());
        final CaptionPanel machineImagesCaptionPanel = new CaptionPanel(stringMessages.machineImages());
        final VerticalPanel machineImagesVerticalPanel = new VerticalPanel();
        machineImagesCaptionPanel.add(machineImagesVerticalPanel);
        final Button machineTableRefreshButton = new Button(stringMessages.refresh());
        machineImagesVerticalPanel.add(machineTableRefreshButton);
        machineTableRefreshButton.addClickHandler(event -> refreshMachineImagesTable());
        machineImagesVerticalPanel.add(machineImagesTable);
        machineImagesBusy = new SimpleBusyIndicator();
        machineImagesVerticalPanel.add(machineImagesBusy);
        mainPanel.add(machineImagesCaptionPanel);
        final SafeHtmlCell amiForProxyCell = new SafeHtmlCell();
        final SafeHtmlCell instanceIdCell = new SafeHtmlCell();
        final SafeHtmlCell instancePublicIpCell = new SafeHtmlCell();
        final SafeHtmlCell instancePrivateIpCell = new SafeHtmlCell();
        final Column<ReverseProxyDTO,SafeHtml> amiProxyProxiesColumn = new Column<ReverseProxyDTO, SafeHtml>(amiForProxyCell) {
            @Override
            public SafeHtml getValue(ReverseProxyDTO proxy) {
                return new LinkBuilder()
                       .setAmiId(proxy.getImageId())
                       .setRegion(regionsTable.getSelectionModel().getSelectedObject())
                       .setPathMode(LinkBuilder.pathModes.InstanceByAmiIdSearch)
                       .build();
            }
        };
        final Column<ReverseProxyDTO, SafeHtml> instanceIdProxiesColumn = new Column<ReverseProxyDTO, SafeHtml>(instanceIdCell) {
            @Override
            public SafeHtml getValue(ReverseProxyDTO reverseProxy) {
                return new LinkBuilder().setInstanceId(reverseProxy.getInstanceId()).setRegion(regionsTable.getSelectionModel().getSelectedObject()).setPathMode(LinkBuilder.pathModes.InstanceSearch).build();
            }
        };
        final Column<ReverseProxyDTO, SafeHtml> instancePublicIpProxiesColumn = new Column<ReverseProxyDTO, SafeHtml>(instancePublicIpCell) {
            @Override
            public SafeHtml getValue(ReverseProxyDTO reverseProxy) {
              return new LinkBuilder().setRegion(regionsTable.getSelectionModel().getSelectedObject()).setPathMode(LinkBuilder.pathModes.publicIp).setPublicIp(reverseProxy.getPublicIpAddress()).build();
            }
        };
        final Column<ReverseProxyDTO, SafeHtml> instancePrivateIpProxiesColumn = new Column<ReverseProxyDTO, SafeHtml> (instancePrivateIpCell) {
            @Override
            public SafeHtml getValue(ReverseProxyDTO reverseProxy) {
                return new LinkBuilder().setRegion(regionsTable.getSelectionModel().getSelectedObject()).setPathMode(LinkBuilder.pathModes.privateIp).setPrivateIp(reverseProxy.getPrivateIpAddress()).build();
            }
        };
        proxiesTable = new TableWrapperWithMultiSelectionAndFilter<ReverseProxyDTO, StringMessages, AdminConsoleTableResources>(
                stringMessages, errorReporter, /* enablePager */ false,
                /* entity identity comparator */ Optional.empty(), GWT.create(AdminConsoleTableResources.class),
                /* checkbox filter function */ Optional.empty(), /* filter label */ Optional.empty(),
                /* filter checkbox label */ null) {
            @Override
            protected Iterable<String> getSearchableStrings(ReverseProxyDTO reverseProxyDTO) {
                final Set<String> result = new HashSet<>();
                if (reverseProxyDTO.getInstanceId() != null) {
                    result.add(reverseProxyDTO.getInstanceId());
                    result.add(reverseProxyDTO.getPrivateIpAddress());
                    result.add(reverseProxyDTO.getPublicIpAddress());
                    result.add(reverseProxyDTO.getRegion());
                }
                return result;
            }
        };
       proxiesTable.addColumn(reverseProxyDTO -> reverseProxyDTO.getName(), stringMessages.name());
       proxiesTable.addColumn(instanceIdProxiesColumn, stringMessages.instanceId());
       proxiesTable.addColumn(amiProxyProxiesColumn, stringMessages.id());
       proxiesTable.addColumn(instancePublicIpProxiesColumn, stringMessages.publicIp());
       proxiesTable.addColumn(instancePrivateIpProxiesColumn, stringMessages.privateIp());
       proxiesTable.addColumn(reverseProxyDTO -> reverseProxyDTO.getAvailabilityZoneName(), stringMessages.availabilityZone());
       proxiesTable.addColumn(reverseProxyDTO -> reverseProxyDTO.getHealth(), stringMessages.state());
       proxiesTable.addColumn(reverseProxyDTO -> reverseProxyDTO.getLaunchTimePoint().toString(), stringMessages.startTimePoint(),
               (rp1, rp2)->rp1.getLaunchTimePoint().compareTo(rp2.getLaunchTimePoint())); // don't compare by string representation but time point4
       //setup actions
       final ActionsColumn<ReverseProxyDTO, ReverseProxyImagesBarCell> proxiesActionColumn = new ActionsColumn<ReverseProxyDTO, ReverseProxyImagesBarCell>(
               new ReverseProxyImagesBarCell(stringMessages), (revProxy, action) -> true);
       proxiesActionColumn.addAction(ReverseProxyImagesBarCell.ACTION_REMOVE, reverseProxy -> {
           if (reverseProxy.isDisposable()) {
               removeReverseProxy(reverseProxy, reverseProxy.getRegion(), stringMessages);
           } else {
               errorReporter.reportError(stringMessages.invalidOperationForThisProxy());
           }
       }); 
       proxiesActionColumn.addAction(ReverseProxyImagesBarCell.ACTION_ROTATE_HTTPD_LOGS, reverseProxy -> rotateHttpdLogs(reverseProxy, stringMessages));
       proxiesTable.addColumn(proxiesActionColumn, stringMessages.actions());
       final CaptionPanel proxiesTableCaptionPanel = new CaptionPanel(stringMessages.reverseProxies());
       final VerticalPanel proxiesTableVerticalPanel = new VerticalPanel();
       final HorizontalPanel proxiesTableButtonPanel = new HorizontalPanel();
       //setup buttons above rows
       final Button proxiesTableRefreshButton = new Button(stringMessages.refresh());
       final Button proxiesTableAddButton = new Button(stringMessages.add());
       final SelectedElementsCountingButton<ReverseProxyDTO> removeProxiesButton = new SelectedElementsCountingButton<>(stringMessages.remove(), proxiesTable.getSelectionModel(), /* element name mapper */ proxy -> proxy.getName(),
                StringMessages.INSTANCE::doYouReallyWantToRemoveSelectedElements,
                e -> removeReverseProxies(stringMessages, regionsTable.getSelectionModel().getSelectedObject(), proxiesTable.getSelectionModel().getSelectedSet()));
       proxiesTableRefreshButton.addClickHandler(event -> refreshProxiesTable());
       proxiesTableAddButton.addClickHandler(event -> addReverseProxyToCluster(stringMessages, regionsTable.getSelectionModel().getSelectedObject()));
       proxiesTableButtonPanel.add(proxiesTableRefreshButton);
       proxiesTableButtonPanel.add(proxiesTableAddButton);
       proxiesTableButtonPanel.add(removeProxiesButton);
       proxiesTableVerticalPanel.add(proxiesTableButtonPanel);
       proxiesTableVerticalPanel.add(proxiesTable);
       proxiesTableBusy= new SimpleBusyIndicator();
       proxiesTableVerticalPanel.add(proxiesTableBusy);
       proxiesTableCaptionPanel.add(proxiesTableVerticalPanel);
       mainPanel.add(proxiesTableCaptionPanel);
       regionsTable.getSelectionModel().addSelectionChangeHandler(e -> {
           final String selectedRegion = regionsTable.getSelectionModel().getSelectedObject();
           refreshAllThatNeedsAwsCredentials();
           storeRegionSelection(userService, selectedRegion);
       });
       AsyncCallback<Boolean> validatePassphraseCallback = new AsyncCallback<Boolean>() {
           @Override
           public void onSuccess(Boolean result) {
               sshKeyManagementPanel.setPassphraseValidation(result.booleanValue(), stringMessages);
               addApplicationReplicaSetButton.setVisible(result);
               applicationReplicaSetsRefreshButton.setVisible(result);
               applicationReplicaSetsCaptionPanel.setVisible(result);
               machineImagesCaptionPanel.setVisible(result);
               mongoEndpointsCaptionPanel.setVisible(result);
               proxiesTableCaptionPanel.setVisible(result);
               if (result) {
                   refreshApplicationReplicaSetsTable();
               }
           }

           public void onFailure(Throwable caught) {
               errorReporter.reportError(stringMessages.passphraseCheckError());
           }
       };
       sshKeyManagementPanel.addSshKeySelectionChangedHandler(event -> {
           validatePassphrase(stringMessages, validatePassphraseCallback);
       });
       sshKeyManagementPanel.addOnPassphraseChangedListener(event -> {
           validatePassphrase(stringMessages, validatePassphraseCallback);
       });
       validatePassphrase(stringMessages, validatePassphraseCallback);
       // TODO try to identify archive servers
       // TODO support archive server upgrade
       // TODO upon region selection show RabbitMQ, and Central Reverse Proxy clusters in region
   }

    private void openShardManagementPanel(StringMessages stringMessages, String region, SailingApplicationReplicaSetDTO<String> replicaset, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) {
        new ShardManagementDialog(landscapeManagementService, replicaset, region, errorReporter, stringMessages,
                optionalKeyName, privateKeyEncryptionPassphrase, new DialogCallback<Boolean>() {
            @Override
            public void ok(Boolean hasAnythingChanged) {
                if (hasAnythingChanged) {
                    refreshApplicationReplicaSetsTable();
                }
            }

            @Override
            public void cancel() {
                // there is no cancel button
            }
      }).show();
    }
    
    private void disableButtonWhenLocalReplicaSetIsSelected(Button button, UserService userService) {
        applicationReplicaSetsTable.getSelectionModel().addSelectionChangeHandler(e->button.setEnabled(
                !applicationReplicaSetsTable.getSelectionModel().getSelectedSet().stream().filter(arsDTO->arsDTO.isLocalReplicaSet(userService)).findAny().isPresent()));
    }
    
    private void disableButtonWhenArchive(Button button) {
        applicationReplicaSetsTable.getSelectionModel().addSelectionChangeHandler(e->button.setEnabled(
                !applicationReplicaSetsTable.getSelectionModel().getSelectedSet().stream().filter(arsDTO-> arsDTO.isArchive()).findAny().isPresent()));
    }
    
    private void validatePassphrase(StringMessages stringMessages, AsyncCallback<Boolean> callback) {
        landscapeManagementService.verifyPassphrase(regionsTable.getSelectionModel().getSelectedObject(),
                sshKeyManagementPanel.getSelectedKeyPair(),
                sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption(), callback);

    }

    private void moveMasterToOtherInstance(StringMessages stringMessages, String regionId, Set<SailingApplicationReplicaSetDTO<String>> replicaSetsForWhichToMoveMaster) {
        new MoveMasterProcessDialog(landscapeManagementService, stringMessages, errorReporter,
                new DialogCallback<MoveMasterToOtherInstanceInstructions>() {
                    @Override
                    public void ok(MoveMasterToOtherInstanceInstructions instructions) {
                        final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator = replicaSetsForWhichToMoveMaster.iterator();
                        if (replicaSetIterator.hasNext()) {
                            applicationReplicaSetsBusy.setBusy(true);
                            moveMasterToOtherInstance(instructions, replicaSetIterator, stringMessages);
                        }
                    }
        
                    private void moveMasterToOtherInstance(
                            MoveMasterToOtherInstanceInstructions instructions,
                            final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator, StringMessages stringMessages) {
                        assert replicaSetIterator.hasNext();
                        final SailingApplicationReplicaSetDTO<String> replicaSet = replicaSetIterator.next();
                        landscapeManagementService.moveMasterToOtherInstance(replicaSet,
                                instructions.isSharedMasterInstance(), instructions.getInstanceTypeOrNull(),
                                sshKeyManagementPanel.getSelectedKeyPair() == null ? null : sshKeyManagementPanel.getSelectedKeyPair().getName(),
                                sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                                instructions.getMasterReplicationBearerToken(), instructions.getReplicaReplicationBearerToken(),
                                instructions.getOptionalMemoryInMegabytesOrNull(), instructions.getOptionalMemoryTotalSizeFactorOrNull(),
                                new ApplicationReplicaSetActionChainingCallback<MoveMasterToOtherInstanceInstructions>(
                                        replicaSetIterator, replicaSet, (i, sri)->moveMasterToOtherInstance(instructions, replicaSetIterator, stringMessages), instructions,
                                        replicaSetName->stringMessages.successfullyMovedMasterOfReplicaSet(replicaSetName)));
                    }

                    @Override
                    public void cancel() {}
        }).show();
    }

    private static interface ApplicationReplicaSetChainingAction<INSTRUCTIONS> {
        void run(INSTRUCTIONS instructions, Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator);
    }
    
    private class ApplicationReplicaSetActionChainingCallback<INSTRUCTIONS> implements AsyncCallback<SailingApplicationReplicaSetDTO<String>> {
        private final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator;
        private final SailingApplicationReplicaSetDTO<String> replicaSet;
        private final ApplicationReplicaSetChainingAction<INSTRUCTIONS> action;
        private final INSTRUCTIONS instructions;
        private final Function<String, String> successMessageSupplier;
        
        public ApplicationReplicaSetActionChainingCallback(
                Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator,
                SailingApplicationReplicaSetDTO<String> replicaSet,
                ApplicationReplicaSetChainingAction<INSTRUCTIONS> action, INSTRUCTIONS instructions,
                Function<String, String> successMessageSupplier) {
            super();
            this.replicaSetIterator = replicaSetIterator;
            this.replicaSet = replicaSet;
            this.action = action;
            this.instructions = instructions;
            this.successMessageSupplier = successMessageSupplier;
        }

        @Override
        public void onFailure(Throwable caught) {
            errorReporter.reportError(caught.getMessage() == null ? caught.getClass().getName() : caught.getMessage());
            if (!replicaSetIterator.hasNext()) {
                applicationReplicaSetsBusy.setBusy(false);
            } else {
                action.run(instructions, replicaSetIterator);
            }
        }

        @Override
        public void onSuccess(SailingApplicationReplicaSetDTO<String> result) {
            Notification.notify(successMessageSupplier.apply(replicaSet.getName()), NotificationType.SUCCESS);
            if (result != null) {
                applicationReplicaSetsTable.replaceBasedOnEntityIdentityComparator(result);
            } else {
                applicationReplicaSetsTable.remove(replicaSet);
            }
            if (!replicaSetIterator.hasNext()) {
                applicationReplicaSetsBusy.setBusy(false);
            } else {
                action.run(instructions, replicaSetIterator);
            }
        }
    }

    private void switchToReplicaOnSharedInstance(StringMessages stringMessages, Set<SailingApplicationReplicaSetDTO<String>> selectedSet) {
        new SwitchToReplicaOnSharedInstanceDialog(stringMessages, errorReporter, landscapeManagementService,
                new DialogCallback<SwitchToReplicaOnSharedInstanceDialog.SwitchToReplicaOnSharedInstanceDialogInstructions>() {
                    @Override
                    public void ok(SwitchToReplicaOnSharedInstanceDialog.SwitchToReplicaOnSharedInstanceDialogInstructions instructions) {
                        final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator = selectedSet.iterator();
                        if (replicaSetIterator.hasNext()) {
                            applicationReplicaSetsBusy.setBusy(true);
                            scaleSingleAutoScalingReplicaSetUpDown(instructions, replicaSetIterator, stringMessages);
                        }
                    }

                    private void scaleSingleAutoScalingReplicaSetUpDown(
                            SwitchToReplicaOnSharedInstanceDialog.SwitchToReplicaOnSharedInstanceDialogInstructions instructions,
                            final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator, StringMessages stringMessages) {
                        assert replicaSetIterator.hasNext();
                        final SailingApplicationReplicaSetDTO<String> replicaSet = replicaSetIterator.next();
                        landscapeManagementService.useSingleSharedInsteadOfDedicatedAutoScalingReplica(replicaSet,
                                sshKeyManagementPanel.getSelectedKeyPair() == null ? null : sshKeyManagementPanel.getSelectedKeyPair().getName(),
                                sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                                instructions.getReplicaReplicationBearerToken(),
                                instructions.getOptionalMemoryInMegabytesOrNull(),
                                instructions.getOptionalMemoryTotalSizeFactorOrNull(),
                                instructions.getOptionalSharedReplicaInstanceType(),
                                new ApplicationReplicaSetActionChainingCallback<SwitchToReplicaOnSharedInstanceDialogInstructions>(replicaSetIterator, replicaSet,
                                        (i, rsi)->scaleSingleAutoScalingReplicaSetUpDown(i, rsi, stringMessages), instructions,
                                        replicaSetName->stringMessages.successfullyCreatedReplicaSet(replicaSetName)));
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private void switchToAutoScalingReplicasOnly(StringMessages stringMessages, String selectedObject,
            Iterable<SailingApplicationReplicaSetDTO<String>> replicaSets) {
        applicationReplicaSetsBusy.setBusy(true);
        final int[] count = { Util.size(replicaSets) };
        final String optionalKeyName = sshKeyManagementPanel.getSelectedKeyPair().getName();
        final byte[] privateKeyEncryptionPassphrase = sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
        ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null;
        for (final SailingApplicationReplicaSetDTO<String> replicaSet : replicaSets) {
            landscapeManagementService.useDedicatedAutoScalingReplicasInsteadOfShared(replicaSet,
                    optionalKeyName, privateKeyEncryptionPassphrase, new AsyncCallback<SailingApplicationReplicaSetDTO<String>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.problemSwitchingReplicaSetToAutoReplicasOnly(replicaSet.getName(),
                                    caught.getMessage()), /* silentMode */ true);
                            if (--count[0] <= 0) {
                                applicationReplicaSetsBusy.setBusy(false);
                            }
                        }

                        @Override
                        public void onSuccess(SailingApplicationReplicaSetDTO<String> result) {
                            if (result != null) {
                                applicationReplicaSetsTable.replaceBasedOnEntityIdentityComparator(result);
                                applicationReplicaSetsTable.refresh();
                                Notification.notify(stringMessages.successfullySwitchedReplicaSetToAutoReplicasOnly(
                                        replicaSet.getName()), NotificationType.SUCCESS);
                            }
                            if (--count[0] <= 0) {
                                applicationReplicaSetsBusy.setBusy(false);
                            }
                        }
                
            });
        }
    }

    private void scaleAutoScalingReplicasUpDown(StringMessages stringMessages, String selectedObject, Set<SailingApplicationReplicaSetDTO<String>> selectedSet) {
        new ChangeAutoScalingReplicaInstanceTypeDialog(landscapeManagementService, stringMessages, errorReporter,
                new DialogCallback<String>() {
                    @Override
                    public void ok(String instanceTypeName) {
                        final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator = selectedSet.iterator();
                        if (replicaSetIterator.hasNext()) {
                            applicationReplicaSetsBusy.setBusy(true);
                            scaleSingleAutoScalingReplicaSetUpDown(instanceTypeName, replicaSetIterator, stringMessages);
                        }
                    }

                    private void scaleSingleAutoScalingReplicaSetUpDown(
                            String instanceTypeName,
                            final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator, StringMessages stringMessages) {
                        assert replicaSetIterator.hasNext();
                        final SailingApplicationReplicaSetDTO<String> replicaSet = replicaSetIterator.next();
                        final String optionalKeyName = sshKeyManagementPanel.getSelectedKeyPair().getName();
                        final byte[] privateKeyEncryptionPassphrase = sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                        ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null;
                        landscapeManagementService.changeAutoScalingReplicasInstanceType(replicaSet,
                                instanceTypeName, optionalKeyName, privateKeyEncryptionPassphrase,
                                new ApplicationReplicaSetActionChainingCallback<String>(replicaSetIterator, replicaSet,
                                        (itn, rsi)->scaleSingleAutoScalingReplicaSetUpDown(itn, rsi, stringMessages), instanceTypeName,
                                        replicaSetName->stringMessages.successfullyScaledAutoScalingReplicasForReplicaSet(replicaSetName)));
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private Iterable<SailingApplicationReplicaSetDTO<String>> getApplicationReplicaSetsToUpgradeAutoScalingReplicaAmisFor(AmazonMachineImageDTO amiBeingUpdated) {
        final Iterable<SailingApplicationReplicaSetDTO<String>> result;
        final Set<SailingApplicationReplicaSetDTO<String>> selection = applicationReplicaSetsTable.getSelectionModel().getSelectedSet();
        if (selection == null || selection.isEmpty()) {
            result = Util.filter(applicationReplicaSetsTable.getFilterPanel().getAll(), rs->rs.getAutoScalingGroupAmiId().equals(amiBeingUpdated.getId()));
        } else {
            result = selection;
        }
        return result;
    }
    
    private void updateAutoScalingReplicaAmi(StringMessages stringMessages, String regionId,
            Iterable<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsToUpdateAutoScalingReplicaAmiFor,
            AmazonMachineImageDTO amiOrNullForLatest) {
        final ArrayList<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsToUpdate = new ArrayList<>();
        Util.addAll(applicationReplicaSetsToUpdateAutoScalingReplicaAmiFor, applicationReplicaSetsToUpdate);
        updateAutoScalingReplicaAmis(stringMessages, regionId, applicationReplicaSetsToUpdate, amiOrNullForLatest);
    }

    private void updateAutoScalingReplicaAmis(StringMessages stringMessages, String regionId,
            final ArrayList<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsToUpdate,
            AmazonMachineImageDTO amiOrNullForLatest) {
        applicationReplicaSetsBusy.setBusy(true);
        landscapeManagementService.updateImageForReplicaSets(regionId, applicationReplicaSetsToUpdate, amiOrNullForLatest,
                sshKeyManagementPanel.getSelectedKeyPair().getName(), sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                        ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                new AsyncCallback<ArrayList<SailingApplicationReplicaSetDTO<String>>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        applicationReplicaSetsBusy.setBusy(false);
                        errorReporter.reportError(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(ArrayList<SailingApplicationReplicaSetDTO<String>> result) {
                        applicationReplicaSetsBusy.setBusy(false);
                        for (final SailingApplicationReplicaSetDTO<String> updatedReplicaSet : result) {
                            applicationReplicaSetsTable.replaceBasedOnEntityIdentityComparator(updatedReplicaSet);
                            Notification.notify(stringMessages.successfullyUpdatedMachineImageForAutoScalingReplicas(
                                    updatedReplicaSet.getName(), updatedReplicaSet.getAutoScalingGroupAmiId()),
                                    NotificationType.SUCCESS);
                        }
                        applicationReplicaSetsTable.refresh();
                    }
                });
    }

    private void ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(
            StringMessages stringMessages, String selectedObject,
            Iterable<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups) {
        final String selectedRegion = regionsTable.getSelectionModel().getSelectedObject();
        new EnsureReplicaStopReplicatingRemoveMasterFromTargetGroupsDialog(stringMessages, errorReporter, new DialogCallback<String>() {
            @Override
            public void ok(String replicaReplicationBearerToken) {
                applicationReplicaSetsBusy.setBusy(true);
                final int[] howManyMoreToGo = new int[] { Util.size(applicationReplicaSetsForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups) };
                for (final SailingApplicationReplicaSetDTO<String> applicationReplicaSetForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups :
                    applicationReplicaSetsForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups) {
                    landscapeManagementService.ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(
                            selectedRegion, applicationReplicaSetForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups,
                            sshKeyManagementPanel.getSelectedKeyPair().getName(),
                            sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                            ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                            replicaReplicationBearerToken, new AsyncCallback<Boolean>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    decrementHowManyMoreToGoAndSetNonBusyIfDone(howManyMoreToGo);
                                    errorReporter.reportError(caught.getMessage());
                                }
                                
                                @Override
                                public void onSuccess(Boolean result) {
                                    decrementHowManyMoreToGoAndSetNonBusyIfDone(howManyMoreToGo);
                                    Notification.notify(stringMessages.successfullyStoppedReplicatingAndRemovedMasterFromTargetGroups(
                                            applicationReplicaSetForWhichToEnsureAtLeastOneReplicaStopReplicatingAndRemoveMasterFromTargetGroups.getName()),
                                            NotificationType.SUCCESS);
                                }
                            });
                }
            }

            @Override public void cancel() {}
        }).show();
    }

    private void decrementHowManyMoreToGoAndSetNonBusyIfDone(int[] howManyMoreToGo) {
        if (--howManyMoreToGo[0] <= 0) {
            applicationReplicaSetsBusy.setBusy(false);
        }
    }
    
    private void defineLandingPage(StringMessages stringMessages, String selectedRegion,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToDefineLandingPageFor) {
        if (sshKeyManagementPanel.getSelectedKeyPair() == null) {
            Notification.notify(stringMessages.pleaseSelectSshKeyPair(), NotificationType.INFO);
        } else {
            new DefineRedirectDialog(applicationReplicaSetToDefineLandingPageFor, stringMessages, errorReporter, landscapeManagementService, new DialogCallback<RedirectDTO>() {
                @Override
                public void ok(final RedirectDTO redirect) {
                    applicationReplicaSetsBusy.setBusy(true);
                    landscapeManagementService.defineDefaultRedirect(selectedRegion, applicationReplicaSetToDefineLandingPageFor.getHostname(),
                            redirect, sshKeyManagementPanel.getSelectedKeyPair().getName(),
                            sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption(),
                        new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                applicationReplicaSetsBusy.setBusy(false);
                                errorReporter.reportError(caught.getMessage());
                            }
    
                            @Override
                            public void onSuccess(Void result) {
                                applicationReplicaSetsBusy.setBusy(false);
                                final String newDefaultRedirect = RedirectDTO.toString(redirect.getPath(), redirect.getQuery());
                                applicationReplicaSetsTable.getFilterPanel().remove(applicationReplicaSetToDefineLandingPageFor);
                                applicationReplicaSetsTable.getFilterPanel().add(new SailingApplicationReplicaSetDTO<String>(
                                        applicationReplicaSetToDefineLandingPageFor.getReplicaSetName(),
                                        applicationReplicaSetToDefineLandingPageFor.getMaster(),
                                        applicationReplicaSetToDefineLandingPageFor.getReplicas(),
                                        applicationReplicaSetToDefineLandingPageFor.getVersion(),
                                        applicationReplicaSetToDefineLandingPageFor.getHostname(),
                                        newDefaultRedirect,
                                        applicationReplicaSetToDefineLandingPageFor.getAutoScalingGroupAmiId()));
                                Notification.notify(stringMessages.successfullyUpdatedLandingPage(), NotificationType.SUCCESS);
                            }
                        });
                }
    
                @Override
                public void cancel() {
                }
            }).show();
        }
    }

    private void createApplicationReplicaSet(StringMessages stringMessages, String regionId) {
        landscapeManagementService.getReleases(new AsyncCallback<ArrayList<ReleaseDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }

            @Override
            public void onSuccess(ArrayList<ReleaseDTO> result) {
                new CreateApplicationReplicaSetDialog(landscapeManagementService, /* sharedMasterInstanceAlreadyExists */ false,
                        result.stream().map(r->r.getName())::iterator,
                        stringMessages, errorReporter, new DialogCallback<CreateApplicationReplicaSetDialog.CreateApplicationReplicaSetInstructions>() {
               @Override
               public void ok(CreateApplicationReplicaSetInstructions instructions) {
                applicationReplicaSetsBusy.setBusy(true);
                landscapeManagementService.createApplicationReplicaSet(regionId, 
                        instructions.getName(), instructions.isSharedMasterInstance(),
                        instructions.getOptionalSharedInstanceType(),
                        instructions.getDedicatedInstanceType(),
                        instructions.isDynamicLoadBalancerMapping(), instructions.getReleaseNameOrNullForLatestMaster(),
                        sshKeyManagementPanel.getSelectedKeyPair()==null?null:sshKeyManagementPanel.getSelectedKeyPair().getName(),
                        sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                        instructions.getMasterReplicationBearerToken(), instructions.getReplicaReplicationBearerToken(),
                        instructions.getOptionalDomainName(),
                        /* minimum auto-scaling group size: */ instructions.isFirstReplicaOnSharedInstance()?0:null,
                        /* maximum auto-scaling group size remains at default: */ null,
                        instructions.getOptionalMemoryInMegabytesOrNull(),
                        instructions.getOptionalMemoryTotalSizeFactorOrNull(),
                        instructions.getOptionalIgtimiRiotPort(),
                        new AsyncCallback<SailingApplicationReplicaSetDTO<String>>() {
                         @Override
                         public void onFailure(Throwable caught) {
                            applicationReplicaSetsBusy.setBusy(false);
                            errorReporter.reportError(caught.getMessage());
                         }
                         
                         @Override
                         public void onSuccess(SailingApplicationReplicaSetDTO<String> result) {
                            applicationReplicaSetsBusy.setBusy(false);
                            Notification.notify(stringMessages.successfullyCreatedReplicaSet(instructions.getName()), NotificationType.SUCCESS);
                            if (result != null) {
                                applicationReplicaSetsTable.getFilterPanel().add(result);
                            }
                         }
                      });
               }
               
               @Override
               public void cancel() {
               }
            }, regionId.equals(SharedLandscapeConstants.REGION_WITH_DEFAULT_LOAD_BALANCER)).show();
            }
        });
    }
    
    private void moveAllApplicationProcessesAwayFrom(StringMessages stringMessages, SailingApplicationReplicaSetDTO<String> applicationReplicaSetOnWhichToDeployMaster) {
        final AwsInstanceDTO fromHost = applicationReplicaSetOnWhichToDeployMaster.getMaster().getHost();
        new MoveAllAwayFromHostDialog(landscapeManagementService, fromHost,
                applicationReplicaSetsTable.getDataProvider().getList(), stringMessages, errorReporter, new DialogCallback<String>() {
                    @Override
                    public void ok(String optionalInstanceTypeName) {
                        applicationReplicaSetsBusy.setBusy(true);
                        landscapeManagementService.moveAllApplicationProcessesAwayFrom(fromHost, optionalInstanceTypeName,
                                sshKeyManagementPanel.getSelectedKeyPair()==null?null:sshKeyManagementPanel.getSelectedKeyPair().getName(),
                                sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                                ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                                new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        applicationReplicaSetsBusy.setBusy(false);
                                        errorReporter.reportError(caught.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        applicationReplicaSetsBusy.setBusy(false);
                                        Notification.notify(stringMessages.successfullyMovedAllProcessesAwayFromHost(fromHost.getInstanceId()), NotificationType.SUCCESS);
                                        refreshApplicationReplicaSetsTable();
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private void createApplicationReplicaSetWithMasterOnExistingHost(StringMessages stringMessages, SailingApplicationReplicaSetDTO<String> applicationReplicaSetOnWhichToDeployMaster) {
        landscapeManagementService.getReleases(new AsyncCallback<ArrayList<ReleaseDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }

            @Override
            public void onSuccess(ArrayList<ReleaseDTO> result) {
                new CreateApplicationReplicaSetDialog(landscapeManagementService, /* sharedMasterInstanceAlreadyExists */ true,
                        result.stream().map(r->r.getName())::iterator, stringMessages, errorReporter, new DialogCallback<CreateApplicationReplicaSetDialog.CreateApplicationReplicaSetInstructions>() {
                    @Override
                    public void ok(CreateApplicationReplicaSetInstructions instructions) {
                        applicationReplicaSetsBusy.setBusy(true);
                        landscapeManagementService.deployApplicationToExistingHost(instructions.getName(), applicationReplicaSetOnWhichToDeployMaster.getMaster().getHost(), 
                                instructions.getDedicatedInstanceType(), instructions.isDynamicLoadBalancerMapping(),
                                        instructions.getReleaseNameOrNullForLatestMaster(), sshKeyManagementPanel.getSelectedKeyPair()==null?null:sshKeyManagementPanel.getSelectedKeyPair().getName(),
                                                sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                                                ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                                                instructions.getMasterReplicationBearerToken(), instructions.getReplicaReplicationBearerToken(),
                                                instructions.getOptionalDomainName(),
                                                /* minimum auto-scaling group size: */ instructions.isFirstReplicaOnSharedInstance() ? 0 : null,
                                                /* maximum auto-scaling group size (use default) */ null,
                                                instructions.getOptionalMemoryInMegabytesOrNull(), instructions.getOptionalMemoryTotalSizeFactorOrNull(),
                                                instructions.getOptionalIgtimiRiotPort(),
                                                getMasterHostFromFirstSelectedApplicationReplicaSetThatIsNot(applicationReplicaSetOnWhichToDeployMaster),
                                                new AsyncCallback<SailingApplicationReplicaSetDTO<String>>() {
                                 @Override
                                 public void onFailure(Throwable caught) {
                                    applicationReplicaSetsBusy.setBusy(false);
                                    errorReporter.reportError(caught.getMessage());
                                 }
                                 
                                 @Override
                                 public void onSuccess(SailingApplicationReplicaSetDTO<String> result) {
                                    applicationReplicaSetsBusy.setBusy(false);
                                    Notification.notify(stringMessages.successfullyCreatedReplicaSet(instructions.getName()), NotificationType.SUCCESS);
                                    if (result != null) {
                                        applicationReplicaSetsTable.getFilterPanel().add(result);
                                    }
                                 }
                              });
                    }
                    
                    @Override
                    public void cancel() {
                    }
                 },
                regionsTable.getSelectionModel().getSelectedObject().equals(SharedLandscapeConstants.REGION_WITH_DEFAULT_LOAD_BALANCER))
                .show();
            }
        });
    }

    private AwsInstanceDTO getMasterHostFromFirstSelectedApplicationReplicaSetThatIsNot(SailingApplicationReplicaSetDTO<String> applicationReplicaSetOnWhichToDeployMaster) {
        for (final SailingApplicationReplicaSetDTO<String> selectedReplicaSet : applicationReplicaSetsTable.getSelectionModel().getSelectedSet()) {
            if (!selectedReplicaSet.getName().equals(applicationReplicaSetOnWhichToDeployMaster.getName())) {
                return selectedReplicaSet.getMaster().getHost();
            }
        }
        return null;
    }
    
    private void removeApplicationReplicaSet(StringMessages stringMessages, String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToRemove) {
        if (Window.confirm(stringMessages.reallyRemoveApplicationReplicaSet(applicationReplicaSetToRemove.getName()))) {
            removeApplicationReplicaSets(stringMessages, regionId, Collections.singleton(applicationReplicaSetToRemove));
        }
    }
    
    private void removeApplicationReplicaSets(StringMessages stringMessages, String regionId,
        Iterable<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsToRemove) {
        applicationReplicaSetsBusy.setBusy(true);
        final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator = applicationReplicaSetsToRemove.iterator();
        if (replicaSetIterator.hasNext()) {
            applicationReplicaSetsBusy.setBusy(true);
            removeApplicationReplicaSet(regionId, replicaSetIterator, stringMessages);
        }
    }

    private void removeApplicationReplicaSet(String regionId, final Iterator<SailingApplicationReplicaSetDTO<String>> replicaSetIterator, StringMessages stringMessages) {
        assert replicaSetIterator.hasNext();
        final MongoEndpointDTO selectedMongoEndpointForDBArchiving = mongoEndpointsTable.getSelectionModel().getSelectedObject();
        final SailingApplicationReplicaSetDTO<String> applicationReplicaSetToRemove = replicaSetIterator.next();
        final ApplicationReplicaSetActionChainingCallback<String> applicationReplicaSetActionChainingCallback = new ApplicationReplicaSetActionChainingCallback<String>(replicaSetIterator, applicationReplicaSetToRemove,
                (rId, rsi)->removeApplicationReplicaSet(rId, rsi, stringMessages), regionId,
                replicaSetName->stringMessages.successfullyRemovedApplicationReplicaSet(replicaSetName));
        landscapeManagementService.removeApplicationReplicaSet(regionId, applicationReplicaSetToRemove, selectedMongoEndpointForDBArchiving,
                sshKeyManagementPanel.getSelectedKeyPair()==null?null:sshKeyManagementPanel.getSelectedKeyPair().getName(),
                        sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                        ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                        new AsyncCallback<String>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                applicationReplicaSetActionChainingCallback.onFailure(caught);
                            }

                            @Override
                            public void onSuccess(String mongoDbArchivingErrorMessage) {
                                applicationReplicaSetActionChainingCallback.onSuccess(/* application replica set (removed) */ null);
                                if (mongoDbArchivingErrorMessage != null) {
                                    errorReporter.reportError(stringMessages.errorArchivingMongoDBTo(
                                            selectedMongoEndpointForDBArchiving.getReplicaSetName(), mongoDbArchivingErrorMessage));
                                }
                            }
                        });
    }
    
    private static class ReplicaSetArchivingParameters {
        private final String bearerTokenOrNullForApplicationReplicaSetToArchive;
        private final String bearerTokenOrNullForArchive;
        private final boolean removeApplicationReplicaSet;
        private final Duration durationToWaitBeforeAndBetweenCompareServerAttempts;
        private final int numberOfTimesToTryCompareServers;
        public ReplicaSetArchivingParameters(String bearerTokenOrNullForApplicationReplicaSetToArchive,
                String bearerTokenOrNullForArchive, boolean removeApplicationReplicaSet,
                Duration durationToWaitBeforeAndBetweenCompareServerAttempts,
                int numberOfTimesToTryCompareServers) {
            super();
            this.bearerTokenOrNullForApplicationReplicaSetToArchive = bearerTokenOrNullForApplicationReplicaSetToArchive;
            this.bearerTokenOrNullForArchive = bearerTokenOrNullForArchive;
            this.removeApplicationReplicaSet = removeApplicationReplicaSet;
            this.durationToWaitBeforeAndBetweenCompareServerAttempts = durationToWaitBeforeAndBetweenCompareServerAttempts;
            this.numberOfTimesToTryCompareServers = numberOfTimesToTryCompareServers;
        }
        public String getBearerTokenOrNullForApplicationReplicaSetToArchive() {
            return bearerTokenOrNullForApplicationReplicaSetToArchive;
        }
        public String getBearerTokenOrNullForArchive() {
            return bearerTokenOrNullForArchive;
        }
        public boolean isRemoveApplicationReplicaSet() {
            return removeApplicationReplicaSet;
        }
        public Duration getDurationToWaitBeforeAndBetweenCompareServerAttempts() {
            return durationToWaitBeforeAndBetweenCompareServerAttempts;
        }
        public int getNumberOfTimesToTryCompareServers() {
            return numberOfTimesToTryCompareServers;
        }
    }

    private void archiveApplicationReplicaSet(StringMessages stringMessages, String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToArchive) {
        final MongoEndpointDTO selectedMongoEndpointForDBArchiving = mongoEndpointsTable.getSelectionModel().getSelectedObject();
        new DataEntryDialog<ReplicaSetArchivingParameters>(stringMessages.archive(), stringMessages.archive(),
                stringMessages.ok(), stringMessages.cancel(), /* validator */ null, new DialogCallback<ReplicaSetArchivingParameters>() {
                    @Override
                    public void ok(ReplicaSetArchivingParameters bearerTokensAndWhetherToRemoveReplicaSet) {
                        applicationReplicaSetsBusy.setBusy(true);
                        landscapeManagementService.archiveReplicaSet(regionId, applicationReplicaSetToArchive,
                                bearerTokensAndWhetherToRemoveReplicaSet.getBearerTokenOrNullForApplicationReplicaSetToArchive(),
                                bearerTokensAndWhetherToRemoveReplicaSet.getBearerTokenOrNullForArchive(),
                                bearerTokensAndWhetherToRemoveReplicaSet.getDurationToWaitBeforeAndBetweenCompareServerAttempts(),
                                bearerTokensAndWhetherToRemoveReplicaSet.getNumberOfTimesToTryCompareServers(),
                                bearerTokensAndWhetherToRemoveReplicaSet.isRemoveApplicationReplicaSet(),
                                selectedMongoEndpointForDBArchiving, sshKeyManagementPanel.getSelectedKeyPair().getName(),
                                sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                                    ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                                new AsyncCallback<Triple<DataImportProgress, CompareServersResultDTO, String>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                applicationReplicaSetsBusy.setBusy(false);
                                errorReporter.reportError(caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Triple<DataImportProgress, CompareServersResultDTO, String> result) {
                                applicationReplicaSetsBusy.setBusy(false);
                                final String mongoDBArchivingErrorMessage = result.getC();
                                if (result == null || result.getA() == null || result.getA().failed()) {
                                    errorReporter.reportError(stringMessages.errorDuringImport(result==null||result.getA()==null?"":result.getA().getErrorMessage()));
                                } else if (result.getB() == null) {
                                    errorReporter.reportError(stringMessages.errorWhileComparingServerContent());
                                } else if (result.getB().hasDiffs()) {
                                    errorReporter.reportError(stringMessages.differencesInServerContentFound(
                                            result.getB().getServerAName(), result.getB().getADiffs().toString(),
                                            result.getB().getServerBName(), result.getB().getBDiffs().toString()));
                                } else if (mongoDBArchivingErrorMessage != null) {
                                    errorReporter.reportError(stringMessages.errorArchivingMongoDBTo(
                                            selectedMongoEndpointForDBArchiving != null ? selectedMongoEndpointForDBArchiving.getReplicaSetName() : "",
                                            mongoDBArchivingErrorMessage));
                                }
                                if (result != null && result.getA() != null && !result.getA().failed()
                                 && result.getB() != null && !result.getB().hasDiffs()) {
                                    if (bearerTokensAndWhetherToRemoveReplicaSet.isRemoveApplicationReplicaSet()) {
                                        applicationReplicaSetsTable.getFilterPanel().remove(applicationReplicaSetToArchive);
                                    }
                                    Notification.notify(stringMessages.successfullyArchivedReplicaSet(
                                            applicationReplicaSetToArchive.getName()), NotificationType.SUCCESS);
                                }
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
            
        }) {
            private final TextBox bearerTokenOrNullForApplicationReplicaSetToArchiveBox = createTextBox("");
            private final TextBox bearerTokenOrNullForArchiveBox = createTextBox("");
            private final IntegerBox numberOfMinutesBeforeAndBetweenCompareServersBox = createIntegerBox((int) DEFAULT_DURATION_TO_WAIT_BEFORE_COMPARE_SERVERS.asMinutes(), /* visible length */ 3);
            private final IntegerBox numberOfCompareServersAttemptsBox = createIntegerBox(DEFAULT_NUMBER_OF_COMPARE_SERVERS_ATTEMPTS, /* visible length */ 3);
            private final CheckBox removeReplicaSetBox = createCheckbox(stringMessages.removeArchivedReplicaSet());
            
            @Override
            protected Widget getAdditionalWidget() {
                final Grid result = new Grid(5, 2);
                int row=0;
                result.setWidget(row, 0, new Label(stringMessages.bearerTokenOrNullForApplicationReplicaSetToArchive(applicationReplicaSetToArchive.getName())));
                result.setWidget(row++, 1, bearerTokenOrNullForApplicationReplicaSetToArchiveBox);
                result.setWidget(row, 0, new Label(stringMessages.bearerTokenOrNullForArchive()));
                result.setWidget(row++, 1, bearerTokenOrNullForArchiveBox);
                result.setWidget(row, 0, new Label(stringMessages.numberOfMinutesBeforeAndBetweenCompareServers()));
                result.setWidget(row++, 1, numberOfMinutesBeforeAndBetweenCompareServersBox);
                result.setWidget(row, 0, new Label(stringMessages.numberOfCompareServersAttempts()));
                result.setWidget(row++, 1, numberOfCompareServersAttemptsBox);
                result.setWidget(row++, 0, removeReplicaSetBox);
                return result;
            }

            @Override
            protected Focusable getInitialFocusWidget() {
                return bearerTokenOrNullForApplicationReplicaSetToArchiveBox;
            }

            @Override
            protected ReplicaSetArchivingParameters getResult() {
                return new ReplicaSetArchivingParameters(
                        Util.hasLength(bearerTokenOrNullForApplicationReplicaSetToArchiveBox.getValue())
                                ? bearerTokenOrNullForApplicationReplicaSetToArchiveBox.getValue()
                                : null,
                        Util.hasLength(bearerTokenOrNullForArchiveBox.getValue())
                                ? bearerTokenOrNullForArchiveBox.getValue()
                                : null,
                        removeReplicaSetBox.getValue(),
                        numberOfMinutesBeforeAndBetweenCompareServersBox.getValue() == null
                                ? DEFAULT_DURATION_TO_WAIT_BEFORE_COMPARE_SERVERS
                                : Duration.ONE_MINUTE
                                        .times(numberOfMinutesBeforeAndBetweenCompareServersBox.getValue()),
                        numberOfCompareServersAttemptsBox.getValue() == null
                                ? DEFAULT_NUMBER_OF_COMPARE_SERVERS_ATTEMPTS
                                : numberOfCompareServersAttemptsBox.getValue());
            }
        }.show();
    }
    
    private void createDefaultLoadBalancerMappings(final StringMessages stringMessages, String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToCreateLoadBalancerMappingFor) {
        new DataEntryDialog<Triple<Boolean, String, Boolean>>(stringMessages.createLoadBalancerMapping(), stringMessages.createLoadBalancerMapping(),
                stringMessages.ok(), stringMessages.cancel(), /* validator */ null, new DialogCallback<Triple<Boolean, String, Boolean>>() {
                    @Override
                    public void ok(Triple<Boolean, String, Boolean> useDynamicLoadBalancerAndOptionalDomainNameAndForceDNSUpdate) {
                        applicationReplicaSetsBusy.setBusy(true);
                        landscapeManagementService.createDefaultLoadBalancerMappings(regionId,
                                applicationReplicaSetToCreateLoadBalancerMappingFor,
                                useDynamicLoadBalancerAndOptionalDomainNameAndForceDNSUpdate.getA(),
                                useDynamicLoadBalancerAndOptionalDomainNameAndForceDNSUpdate.getB(),
                                useDynamicLoadBalancerAndOptionalDomainNameAndForceDNSUpdate.getC(),
                                new AsyncCallback<SailingApplicationReplicaSetDTO<String>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                applicationReplicaSetsBusy.setBusy(false);
                                errorReporter.reportError(caught.getMessage());
                            }

                            @Override
                            public void onSuccess(SailingApplicationReplicaSetDTO<String> result) {
                                applicationReplicaSetsBusy.setBusy(false);
                                applicationReplicaSetsTable.getFilterPanel().remove(applicationReplicaSetToCreateLoadBalancerMappingFor);
                                applicationReplicaSetsTable.getFilterPanel().add(result);
                                Notification.notify(stringMessages.successfullyCreatedLoadBalancerMappingFor(
                                        applicationReplicaSetToCreateLoadBalancerMappingFor.getName()), NotificationType.SUCCESS);
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
            
        }) {
            private final CheckBox useDynamicLoadBalancerCheckbox = createCheckbox(stringMessages.useDynamicLoadBalancer());
            private final TextBox optionalDomainNameBox = createTextBox(SharedLandscapeConstants.DEFAULT_DOMAIN_NAME);
            private final CheckBox forceDNSUpdateCheckbox = createCheckbox(stringMessages.forceDNSUpdate());
            
            @Override
            protected Widget getAdditionalWidget() {
                final VerticalPanel result = new VerticalPanel();
                result.add(useDynamicLoadBalancerCheckbox);
                final HorizontalPanel domainNamePanel = new HorizontalPanel();
                result.add(domainNamePanel);
                domainNamePanel.add(new Label(stringMessages.domainName()));
                domainNamePanel.add(optionalDomainNameBox);
                result.add(forceDNSUpdateCheckbox);
                return result;
            }

            @Override
            protected Focusable getInitialFocusWidget() {
                return useDynamicLoadBalancerCheckbox;
            }

            @Override
            protected Triple<Boolean, String, Boolean> getResult() {
                return new Triple<>(useDynamicLoadBalancerCheckbox.getValue(),
                        Util.hasLength(optionalDomainNameBox.getValue())?optionalDomainNameBox.getValue():null,
                        forceDNSUpdateCheckbox.getValue());
            }
        }.show();
    }

    /**
     * Performs an in-place upgrade for the master service if the replica set has distinct public and master
     * target groups. If no replica exists, one is launched with the master's release, and the method waits
     * until the replica has reached its healthy state. The replica is then registered in the public target group.<p>
     * 
     * Then, the {@code ./refreshInstance.sh install-release <release>} command is sent to the master which will
     * download and unpack the new release but will not yet stop the master process. In parallel, the existing
     * default launch template version will be copied and updated with user data reflecting the new release to be used.
     * The auto-scaling group will then use the new default launch template version.<p>
     * 
     * Replication is then stopped for all existing replicas, then the master is de-registered from the master
     * target group and the public target group, effectively making the replica set "read-only." Then, the {@code ./stop}
     * command is issued which is expected to wait until all process resources have been released so that it's
     * appropriate to call {@code ./start} just after the {@code ./stop} call has returned, thus spinning up the
     * master process with the new release configuration.<p>
     * 
     * When the master process has reached its healthy state, it is registered with both target groups while all other
     * replicas are de-registered and then stopped. For replica processes being the last on their host, the host will
     * be terminated. It is up to an auto-scaling group or to the user to decide whether to launch new replicas again.
     * This won't happen automatically by this procedure.
     */
    private void upgradeApplicationReplicaSet(StringMessages stringMessages, String regionId,
            Iterable<SailingApplicationReplicaSetDTO<String>> replicaSets) {
        landscapeManagementService.getReleases(new AsyncCallback<ArrayList<ReleaseDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }

            @Override
            public void onSuccess(ArrayList<ReleaseDTO> result) {
                new UpgradeApplicationReplicaSetDialog(landscapeManagementService, result.stream().map(r->r.getName())::iterator,
                        stringMessages, errorReporter, new DialogCallback<UpgradeApplicationReplicaSetDialog.UpgradeApplicationReplicaSetInstructions>() {
                            @Override
                            public void ok(UpgradeApplicationReplicaSetInstructions upgradeInstructions) {
                                applicationReplicaSetsBusy.setBusy(true);
                                final int[] howManyMoreToGo = new int[] { Util.size(replicaSets) };
                                Duration timeToWaitUntilUpgradingNextReplicaSet = Duration.NULL;
                                for (final SailingApplicationReplicaSetDTO<String> replicaSet : replicaSets) {
                                    new Timer() {
                                        @Override
                                        public void run() {
                                            landscapeManagementService.upgradeApplicationReplicaSet(regionId, replicaSet,
                                                    upgradeInstructions.getReleaseNameOrNullForLatestMaster(),
                                                    sshKeyManagementPanel.getSelectedKeyPair()==null?null:sshKeyManagementPanel.getSelectedKeyPair().getName(),
                                                            sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                                                            ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes() : null,
                                                    upgradeInstructions.getReplicaReplicationBearerToken(),
                                                    new AsyncCallback<SailingApplicationReplicaSetDTO<String>>() {
                                                        @Override
                                                        public void onFailure(Throwable caught) {
                                                            decrementHowManyMoreToGoAndSetNonBusyIfDone(howManyMoreToGo);
                                                            errorReporter.reportError(caught.getMessage());
                                                        }
            
                                                        @Override
                                                        public void onSuccess(SailingApplicationReplicaSetDTO<String> result) {
                                                            decrementHowManyMoreToGoAndSetNonBusyIfDone(howManyMoreToGo);
                                                            if (result != null) {
                                                                Notification.notify(stringMessages.successfullyUpgradedApplicationReplicaSet(
                                                                                result.getName(), result.getVersion()), NotificationType.SUCCESS);
                                                                applicationReplicaSetsTable.replaceBasedOnEntityIdentityComparator(result);
                                                                applicationReplicaSetsTable.refresh();
                                                            } else {
                                                                Notification.notify(stringMessages.upgradingApplicationReplicaSetFailed(replicaSet.getName()),
                                                                        NotificationType.ERROR);
                                                            }
                                                        }
                                                    });
                                        }
                                    }.schedule((int) timeToWaitUntilUpgradingNextReplicaSet.asMillis());
                                    timeToWaitUntilUpgradingNextReplicaSet = timeToWaitUntilUpgradingNextReplicaSet.plus(
                                            DURATION_TO_WAIT_BETWEEN_REPLICA_SET_UPGRADE_REQUESTS);
                                }
                            }

                            @Override
                            public void cancel() {
                            }
                }).show();
            }
        });
    }

    private void refreshRegionsTable(UserService userService) {
        landscapeManagementService.getRegions(new AsyncCallback<ArrayList<String>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }

            @Override
            public void onSuccess(ArrayList<String> regions) {
                regionsTable.refresh(regions);
                userService.getPreference(AWS_DEFAULT_REGION_USER_PREFERENCE, new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log("Couldn't obtain "+AWS_DEFAULT_REGION_USER_PREFERENCE+" preference for user "+userService.getCurrentUser().getName());
                    }

                    @Override
                    public void onSuccess(String defaultRegion) {
                        regionsTable.getFilterPanel().search(defaultRegion);
                        regionsTable.getSelectionModel().setSelected(defaultRegion, /* selected */ true);
                    }
                });
            }
        });
    }
    
    private void refreshAllThatNeedsAwsCredentials() {
        refreshMongoEndpointsTable();
        refreshApplicationReplicaSetsTable();
        refreshMachineImagesTable();
        refreshProxiesTable();
        sshKeyManagementPanel.showKeysInRegion(mfaLoginWidget.hasValidSessionCredentials() ?
                regionsTable.getSelectionModel().getSelectedObject() : null);
    }
    
    private void removeReverseProxies(StringMessages stringMessages, String regionId,
            Iterable<ReverseProxyDTO> reverseProxiesToRemove) {
        Iterator<ReverseProxyDTO> iterator = reverseProxiesToRemove.iterator();
        while (iterator.hasNext()) {
            removeReverseProxy(iterator.next(), regionId, stringMessages);
        }
    }
    /**
     * Removes a reverse proxy from the cluster and terminates it.
     * @param instance The reverse proxy to remove from the cluster.
     * @param regionId The region the proxy is in.
     */
    private void removeReverseProxy(ReverseProxyDTO instance, String regionId, StringMessages stringMessages) {
        if (sshKeyManagementPanel.getSelectedKeyPair() == null) {
            Notification.notify(stringMessages.pleaseSelectSshKeyPair(), NotificationType.INFO);
        } else {
            proxiesTableBusy.setBusy(true);
            landscapeManagementService.removeReverseProxy(instance, regionId,
                    sshKeyManagementPanel.getSelectedKeyPair().getName(),
                    sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                            ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes()
                            : null,
                    new AsyncCallback<Void>() {

                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(caught.getMessage());
                            proxiesTableBusy.setBusy(false);

                        }

                        @Override
                        public void onSuccess(Void result) {
                            proxiesTableBusy.setBusy(false);
                            refreshProxiesTable();
                        }

                    });
        }
    }

    /**
     * Creates a reverse proxy based on the user's input.
     * @param region The region to spawn the reverse proxy in.
     */
    private void addReverseProxyToCluster(StringMessages stringMessages, String region) {
        if (sshKeyManagementPanel.getSelectedKeyPair() == null || regionsTable.getSelectionModel().getSelectedObject() == null) {
            Notification.notify(stringMessages.pleaseSelectSshKeyPair(), NotificationType.INFO);
        } else {
            landscapeManagementService.describeAvailabilityZones(regionsTable.getSelectionModel().getSelectedObject(),
                    new AsyncCallback<ArrayList<AvailabilityZoneDTO>>() {
                        @Override
                        public void onSuccess(ArrayList<AvailabilityZoneDTO> result) {
                            showReverseProxyDialog(stringMessages, region, result);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(caught.getMessage());
                        }
                    });
        }
    }
    
    private void showReverseProxyDialog(StringMessages stringMessages, String region, ArrayList<AvailabilityZoneDTO> availabilityZones) {
        new CreateReverseProxyInClusterDialog(stringMessages, errorReporter, landscapeManagementService, region,
                proxiesTable.getTable().getVisibleItems(), availabilityZones,
                new DialogCallback<CreateReverseProxyInClusterDialog.CreateReverseProxyInstructions>() {
                    @Override
                    public void ok(CreateReverseProxyInClusterDialog.CreateReverseProxyInstructions editedObject) {
                        editedObject.setKey(sshKeyManagementPanel.getSelectedKeyPair() == null ? null
                                : sshKeyManagementPanel.getSelectedKeyPair().getName());
                        proxiesTableBusy.setBusy(true);
                        landscapeManagementService.addReverseProxy(editedObject.getName(),
                                editedObject.getInstanceType(), editedObject.getRegion(), editedObject.getKey(), editedObject.getAvailabilityZoneDTO(),
                                new AsyncCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Notification.notify(stringMessages.success(), NotificationType.SUCCESS);
                                        proxiesTableBusy.setBusy(false);
                                        refreshProxiesTable();
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        proxiesTableBusy.setBusy(false);
                                        errorReporter.reportError(caught.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }
    
    /**
     * Updates the proxies table with all the reverse proxies in the region.
     */
    private void refreshProxiesTable() {
        proxiesTable.getFilterPanel().removeAll();
        if (mfaLoginWidget.hasValidSessionCredentials()
                && regionsTable.getSelectionModel().getSelectedObject() != null) {
            proxiesTableBusy.setBusy(true);
            landscapeManagementService.getReverseProxies(regionsTable.getSelectionModel().getSelectedObject(),
                    new AsyncCallback<ArrayList<ReverseProxyDTO>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(caught.getMessage());
                            proxiesTableBusy.setBusy(false);
                        }
                        
                        @Override
                        public void onSuccess(ArrayList<ReverseProxyDTO> reverseProxyDTOs) {
                            proxiesTable.refresh(reverseProxyDTOs);
                            proxiesTableBusy.setBusy(false);
                        }
                    });
        }
    }
    
    /**
     * Rotates the httpd logs on an instance.
     * @param reverseProxy The instance to rotate the logs on.
     */
    private void rotateHttpdLogs(ReverseProxyDTO reverseProxy, StringMessages stringMessages) {
        if (sshKeyManagementPanel.getSelectedKeyPair() == null) {
            Notification.notify(stringMessages.pleaseSelectSshKeyPair(), NotificationType.INFO);
        } else {
            landscapeManagementService.rotateHttpdLogs(reverseProxy, reverseProxy.getRegion(),
                    sshKeyManagementPanel.getSelectedKeyPair().getName(),
                    sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption() != null
                            ? sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes()
                            : null,
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void result) {
                            Notification.notify(
                                    stringMessages.successfullyRotatedHttpdLogsOnInstance(reverseProxy.getInstanceId()),
                                    NotificationType.SUCCESS);
                        }
                    });
        }
    }
    
    private void storeRegionSelection(UserService userService, String selectedRegion) {
        if (selectedRegion != null) {
            userService.setPreference(AWS_DEFAULT_REGION_USER_PREFERENCE, selectedRegion, new AsyncCallback<Void>() {
                @Override public void onFailure(Throwable caught) {} @Override public void onSuccess(Void result) {}
            });
        }
    }

    private void scaleMongoEndpoint(StringMessages stringMessages, String selectedRegion, MongoEndpointDTO mongoEndpointToScale) {
        if (sshKeyManagementPanel.getSelectedKeyPair() == null) {
            Notification.notify(stringMessages.pleaseSelectSshKeyPair(), NotificationType.INFO);
        } else {
            new MongoScalingDialog(mongoEndpointToScale, stringMessages, errorReporter, landscapeManagementService, new DialogCallback<MongoScalingInstructionsDTO>() {
                @Override
                public void ok(MongoScalingInstructionsDTO mongoScalingInstructions) {
                    mongoEndpointsBusy.setBusy(true);
                    landscapeManagementService.scaleMongo(selectedRegion, mongoScalingInstructions, sshKeyManagementPanel.getSelectedKeyPair().getName(),
                        new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                mongoEndpointsBusy.setBusy(false);
                                errorReporter.reportError(caught.getMessage());
                            }
    
                            @Override
                            public void onSuccess(Void result) {
                                mongoEndpointsBusy.setBusy(false);
                                Notification.notify(stringMessages.successfullyScaledMongoDB(),
                                        NotificationType.SUCCESS);
                                refreshMongoEndpointsTable();
                            }
                        });
                }
    
                @Override
                public void cancel() {
                }
            }).show();
        }
    }

    private boolean isNewest(AmazonMachineImageDTO ami) {
        final Comparator<TimePoint> timePointComparator = Comparator.nullsLast(Comparator.reverseOrder());
        final Comparator<AmazonMachineImageDTO> imageByTimePointComparator = (AmazonMachineImageDTO i1, AmazonMachineImageDTO i2)->
                timePointComparator.compare(i1.getCreationTimePoint(), i2.getCreationTimePoint());
        return ami.getCreationTimePoint().equals(
                machineImagesTable.getDataProvider().getList().stream().filter(imageFromTable->imageFromTable.getType().equals(ami.getType()))
                .sorted(imageByTimePointComparator)
                .findFirst().get().getCreationTimePoint());
    }

    private void refreshMongoEndpointsTable() {
        mongoEndpointsTable.getFilterPanel().removeAll();
        if (mfaLoginWidget.hasValidSessionCredentials() && regionsTable.getSelectionModel().getSelectedObject() != null) {
            mongoEndpointsBusy.setBusy(true);
            landscapeManagementService.getMongoEndpoints(regionsTable.getSelectionModel().getSelectedObject(), new AsyncCallback<ArrayList<MongoEndpointDTO>>() {
               @Override
               public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
                mongoEndpointsBusy.setBusy(false);
               }
      
               @Override
               public void onSuccess(ArrayList<MongoEndpointDTO> mongoEndpointDTOs) {
                mongoEndpointsTable.refresh(mongoEndpointDTOs);
                mongoEndpointsBusy.setBusy(false);
               }
            });
        }
    }
    
    private void refreshApplicationReplicaSetsTable() {
        applicationReplicaSetsTable.getFilterPanel().removeAll();
        if (mfaLoginWidget.hasValidSessionCredentials() && regionsTable.getSelectionModel().getSelectedObject() != null
                && Util.hasLength(sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption())) {
            applicationReplicaSetsBusy.setBusy(true);
            final SSHKeyPairDTO selectedSshKeyPair = sshKeyManagementPanel.getSelectedKeyPair();
            landscapeManagementService.getApplicationReplicaSets(regionsTable.getSelectionModel().getSelectedObject(),
                    selectedSshKeyPair==null?null:selectedSshKeyPair.getName(),
                    sshKeyManagementPanel.getPassphraseForPrivateKeyDecryption().getBytes(),
                    new AsyncCallback<ArrayList<SailingApplicationReplicaSetDTO<String>>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(caught.getMessage());
                            applicationReplicaSetsBusy.setBusy(false);
                        }

                        @Override
                        public void onSuccess(
                                ArrayList<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetDTOs) {
                            applicationReplicaSetsTable.refresh(applicationReplicaSetDTOs);
                            applicationReplicaSetsBusy.setBusy(false);
                        }
                    });
        }
    }
    
    private void refreshMachineImagesTable() {
        machineImagesTable.getFilterPanel().removeAll();
        if (mfaLoginWidget.hasValidSessionCredentials() && regionsTable.getSelectionModel().getSelectedObject() != null) {
            machineImagesBusy.setBusy(true);
            landscapeManagementService.getAmazonMachineImages(regionsTable.getSelectionModel().getSelectedObject(), new AsyncCallback<ArrayList<AmazonMachineImageDTO>>() {
               @Override
               public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
                machineImagesBusy.setBusy(false);
               }
      
               @Override
               public void onSuccess(ArrayList<AmazonMachineImageDTO> machineImagesDTOs) {
                machineImagesTable.refresh(machineImagesDTOs);
                machineImagesBusy.setBusy(false);
               }
            });
        }
    }

    private void upgradeMachineImage(final StringMessages stringMessages, final AmazonMachineImageDTO machineImageToUpgrade, Iterable<SailingApplicationReplicaSetDTO<String>> selectedApplicationReplicaSetsToUpdate) {
        Notification.notify(stringMessages.startedImageUpgrade(machineImageToUpgrade.getName(), machineImageToUpgrade.getId(), machineImageToUpgrade.getRegionId()), NotificationType.INFO);
        machineImagesBusy.setBusy(true);
        landscapeManagementService.upgradeAmazonMachineImage(machineImageToUpgrade.getRegionId(), machineImageToUpgrade.getId(),
                new AsyncCallback<AmazonMachineImageDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
                machineImagesBusy.setBusy(false);
            }

            @Override
            public void onSuccess(AmazonMachineImageDTO result) {
                machineImagesBusy.setBusy(false);
                refreshMachineImagesTable();
                Notification.notify(
                        stringMessages.successfullyUpgradedMachineImage(machineImageToUpgrade.getName(),
                                machineImageToUpgrade.getId(), machineImageToUpgrade.getRegionId(), result.getName()),
                        NotificationType.SUCCESS);
                if (Util.equalsWithNull(result.getType(), SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING)
                        && selectedApplicationReplicaSetsToUpdate != null
                        && !Util.isEmpty(selectedApplicationReplicaSetsToUpdate)) {
                    if (Window.confirm(stringMessages.updateSelectedReplicaSetAmisToo(Util.join(", ", selectedApplicationReplicaSetsToUpdate)))) {
                        updateAutoScalingReplicaAmi(stringMessages, machineImageToUpgrade.getRegionId(), selectedApplicationReplicaSetsToUpdate, result);
                    }
                }
            }
        });
    }

    private void removeMachineImage(final StringMessages stringMessages, final AmazonMachineImageDTO machineImageToRemove) {
        if (Window.confirm(stringMessages.doYouReallyWantToRemoveMachineImage(machineImageToRemove.getName(), machineImageToRemove.getId(), machineImageToRemove.getRegionId()))) {
            landscapeManagementService.removeAmazonMachineImage(machineImageToRemove.getRegionId(), machineImageToRemove.getId(),
                    new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError(caught.getMessage());
                }
    
                @Override
                public void onSuccess(Void result) {
                    machineImagesTable.remove(machineImageToRemove);
                            Notification.notify(
                                    stringMessages.successfullyRemovedMachineImage(machineImageToRemove.getName(),
                                            machineImageToRemove.getId(), machineImageToRemove.getRegionId()),
                                    NotificationType.SUCCESS);
                        }
            });
        }
    }

    private LandscapeManagementWriteServiceAsync initAndRegisterLandscapeManagementService() {
        final LandscapeManagementWriteServiceAsync result = GWT.create(LandscapeManagementWriteService.class);
        EntryPointHelper.registerASyncService((ServiceDefTarget) result,
                RemoteServiceMappingConstants.landscapeManagementServiceRemotePath, HEADER_FORWARD_TO_MASTER);
        return result;
    }
}
