package com.sap.sailing.landscape.ui.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sailing.landscape.ui.shared.SailingApplicationReplicaSetDTO;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.celltable.ImagesBarCell;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;

public class ApplicationReplicaSetsImagesBarCell extends ImagesBarCell {
    static final String ACTION_REMOVE = DefaultActions.DELETE.name();
    static final String ACTION_UPGRADE = "UPGRADE";
    static final String ACTION_ACTIVATE_ARCHIVE_CANDIDATE = "ACTIVATE_ARCHIVE_CANDIDATE";
    static final String ACTION_ARCHIVE = "ARCHIVE";
    static final String ACTION_DEFINE_LANDING_PAGE = "DEFINE_LANDING_PAGE";
    static final String ACTION_CREATE_LOAD_BALANCER_MAPPING = "CREATE_LOAD_BALANGER_MAPPING";
    static final String ACTION_LAUNCH_ANOTHER_REPLICA_SET_ON_THIS_MASTER = "LAUNCH_ANOTHER_REPLICA_SET_ON_THIS_MASTER";
    static final String ACTION_ENSURE_ONE_REPLICA_THEN_STOP_REPLICATING_AND_REMOVE_MASTER_FROM_TARGET_GROUPS = "ACTION_ENSURE_ONE_REPLICA_THEN_STOP_REPLICATING_AND_REMOVE_MASTER_FROM_TARGET_GROUPS";
    static final String ACTION_UPDATE_AMI_FOR_AUTO_SCALING_REPLICAS = "ACTION_UPDATE_AMI_FOR_AUTO_SCALING_REPLICAS";
    static final String ACTION_SWITCH_TO_AUTO_SCALING_REPLICAS_ONLY = "ACTION_SWITCH_TO_AUTO_SCALING_REPLICAS_ONLY";
    static final String ACTION_SWITCH_TO_REPLICA_ON_SHARED_INSTANCE = "ACTION_SWITCH_TO_REPLICA_ON_SHARED_INSTANCE";
    static final String ACTION_MOVE_MASTER_TO_OTHER_INSTANCE = "ACTION_MOVE_MASTER_TO_OTHER_INSTANCE";
    static final String ACTION_SCALE_AUTO_SCALING_REPLICAS_UP_DOWN = "ACTION_SCALE_AUTO_SCALING_REPLICAS_UP_DOWN";
    static final String ACTION_OPEN_SHARD_MANAGEMENT = "ACTION_OPEN_SHARD_MANAGEMENT";
    static final String ACTION_MOVE_ALL_APPLICATION_PROCESSES_AWAY_FROM = "ACTION_MOVE_ALL_APPLICATION_PROCESSES_AWAY_FROM";

    private final StringMessages stringMessages;
    private final UserService userService;

    public ApplicationReplicaSetsImagesBarCell(UserService userService, StringMessages stringMessages) {
        super();
        this.stringMessages = stringMessages;
        this.userService = userService;
    }

    public ApplicationReplicaSetsImagesBarCell(SafeHtmlRenderer<String> renderer, UserService userService,
            StringMessages stringMessages) {
        this(userService, stringMessages);
    }

    @Override
    protected Iterable<ImageSpec> getImageSpecs() {
        @SuppressWarnings("unchecked")
        final SailingApplicationReplicaSetDTO<String> applicationReplicaSet = (SailingApplicationReplicaSetDTO<String>) getContext()
                .getKey();
        final List<ImageSpec> result = new ArrayList<>();
        if (!applicationReplicaSet.isLocalReplicaSet(userService) && !applicationReplicaSet.isArchive()) {
            result.add(new ImageSpec(ACTION_ARCHIVE, stringMessages.archive(), IconResources.INSTANCE.archiveIcon()));
            result.add(new ImageSpec(ACTION_REMOVE, stringMessages.remove(), IconResources.INSTANCE.removeIcon()));
        }
        if (!applicationReplicaSet.isArchive()) {
            result.add(new ImageSpec(ACTION_DEFINE_LANDING_PAGE, stringMessages.defineLandingPage(),
                    IconResources.INSTANCE.editIcon()));
        }
        result.add(new ImageSpec(ACTION_CREATE_LOAD_BALANCER_MAPPING, stringMessages.createLoadBalancerMapping(),
                IconResources.INSTANCE.loadBalancerIcon()));
        result.add(new ImageSpec(ACTION_LAUNCH_ANOTHER_REPLICA_SET_ON_THIS_MASTER,
                stringMessages.launchAnotherReplicaSetOnThisMaster(),
                IconResources.INSTANCE.launchAnotherReplicaSetOnThisMasterIcon()));
        if (!applicationReplicaSet.isLocalReplicaSet(userService)) {
            result.add(new ImageSpec(ACTION_UPGRADE, stringMessages.upgrade(), IconResources.INSTANCE.refreshIcon()));
        }
        if (applicationReplicaSet.isArchive()) {
            result.add(new ImageSpec(ACTION_ACTIVATE_ARCHIVE_CANDIDATE, stringMessages.activateArchiveCandidate(), IconResources.INSTANCE.check()));
        }
        if (!applicationReplicaSet.isArchive()) {
            result.add(
                    new ImageSpec(ACTION_ENSURE_ONE_REPLICA_THEN_STOP_REPLICATING_AND_REMOVE_MASTER_FROM_TARGET_GROUPS,
                            stringMessages
                                    .ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(),
                            IconResources.INSTANCE.unlinkIcon()));
        }
        if (applicationReplicaSet.getAutoScalingGroupAmiId() != null) {
            result.add(new ImageSpec(ACTION_UPDATE_AMI_FOR_AUTO_SCALING_REPLICAS,
                    stringMessages.updateAmiForAutoScalingReplicas(), IconResources.INSTANCE.redGearsIcon()));
            result.add(new ImageSpec(ACTION_SWITCH_TO_AUTO_SCALING_REPLICAS_ONLY,
                    stringMessages.switchToAutoScalingReplicasOnly(), IconResources.INSTANCE.scaleUpIcon()));
            result.add(new ImageSpec(ACTION_OPEN_SHARD_MANAGEMENT, stringMessages.openShardManagement(),
                    IconResources.INSTANCE.shardManagementIcon()));
        }
        if (!applicationReplicaSet.isArchive()) {
            result.add(new ImageSpec(ACTION_SWITCH_TO_REPLICA_ON_SHARED_INSTANCE,
                    stringMessages.switchToReplicaOnSharedInstance(), IconResources.INSTANCE.scaleDownIcon()));
        }
        if (!applicationReplicaSet.isLocalReplicaSet(userService) && !applicationReplicaSet.isArchive()) {
            result.add(new ImageSpec(ACTION_MOVE_MASTER_TO_OTHER_INSTANCE, stringMessages.moveMasterToOtherInstance(),
                    IconResources.INSTANCE.moveIcon()));
        }
        if (!applicationReplicaSet.isArchive()) {
            result.add(new ImageSpec(ACTION_SCALE_AUTO_SCALING_REPLICAS_UP_DOWN,
                    stringMessages.scaleAutoScalingReplicasUpOrDown(), IconResources.INSTANCE.scaleIcon()));
        }
        if (applicationReplicaSet.getMaster().getHost().isShared()) {
            result.add(new ImageSpec(ACTION_MOVE_ALL_APPLICATION_PROCESSES_AWAY_FROM,
                    stringMessages.moveAllApplicationProcessesAwayFromMaster(), IconResources.INSTANCE.moveAway()));
        }
        return result;
    }
}
