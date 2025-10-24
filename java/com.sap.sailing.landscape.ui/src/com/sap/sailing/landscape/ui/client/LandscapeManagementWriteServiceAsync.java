package com.sap.sailing.landscape.ui.client;

import java.util.ArrayList;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.ui.shared.AmazonMachineImageDTO;
import com.sap.sailing.landscape.ui.shared.AvailabilityZoneDTO;
import com.sap.sailing.landscape.ui.shared.AwsInstanceDTO;
import com.sap.sailing.landscape.ui.shared.AwsShardDTO;
import com.sap.sailing.landscape.ui.shared.CompareServersResultDTO;
import com.sap.sailing.landscape.ui.shared.LeaderboardNameDTO;
import com.sap.sailing.landscape.ui.shared.MongoEndpointDTO;
import com.sap.sailing.landscape.ui.shared.MongoScalingInstructionsDTO;
import com.sap.sailing.landscape.ui.shared.ProcessDTO;
import com.sap.sailing.landscape.ui.shared.ReleaseDTO;
import com.sap.sailing.landscape.ui.shared.ReverseProxyDTO;
import com.sap.sailing.landscape.ui.shared.SSHKeyPairDTO;
import com.sap.sailing.landscape.ui.shared.SailingApplicationReplicaSetDTO;
import com.sap.sailing.landscape.ui.shared.SerializationDummyDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.landscape.aws.common.shared.RedirectDTO;

public interface LandscapeManagementWriteServiceAsync {
    void getRegions(AsyncCallback<ArrayList<String>> callback);
    
    /**
     * @param canBeDeployedInNlbInstanceBasedTargetGroup
     *            A boolean indicating, if true, that the list of available instance types should not contain those,
     *            which cannot be added to an instance-based Network Load Balancer.
     */
    void getInstanceTypeNames(boolean canBeDeployedInNlbInstanceBasedTargetGroup, AsyncCallback<ArrayList<String>> callback);

    void getMongoEndpoints(String regionId, AsyncCallback<ArrayList<MongoEndpointDTO>> callback);

    void getMongoEndpoint(String region, String replicaSetName,
            AsyncCallback<MongoEndpointDTO> callback);
    
    void getReverseProxies(String regionId, AsyncCallback<ArrayList<ReverseProxyDTO>> callback);
    
    /**
     * Removes a reverse proxy from the given cluster and terminates it.
     * @return Returns true if a success.
     */
    void removeReverseProxy(ReverseProxyDTO instance, String region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase, AsyncCallback<Void> callback);
    
    /**
     * Rotates the httpd logs on a proxy instance.
     * @param optionalKeyName Name of key used to connect to the instance to restart
     * @param passphraseForPrivateKeyDecryption the passphrase for the given key
     */
    void rotateHttpdLogs(ReverseProxyDTO proxy, String region, String optionalKeyName,
            byte[] passphraseForPrivateKeyDecryption, AsyncCallback<Void> callback);
    
    /**
     * Adds a reverse proxy to the cluster and the right load balancer's target group.
     */
    void addReverseProxy(String instanceName, String instanceType, String region, String launchKey, AvailabilityZoneDTO availabilityZoneDTO, AsyncCallback<Void> callback);
    
    
    /**
     * Gets all availability zones in a region.
     */
    void describeAvailabilityZones(String region,AsyncCallback<ArrayList<AvailabilityZoneDTO>> asyncCallback);
    
    /**
     * The calling subject will see only those keys for which it has the {@code READ} permission.
     */
    void getSshKeys(String regionId, AsyncCallback<ArrayList<SSHKeyPairDTO>> callback);

    /**
     * The calling subject must have {@code DELETE} permission for the key requested.
     */
    void removeSshKey(SSHKeyPairDTO keyPair, AsyncCallback<Void> asyncCallback);

    /**
     * The calling subject must have {@code CREATE} permission for the key name and region requested as well as the
     * {@link CREATE_OBJECT} permission on the server on which this is called.
     */
    void generateSshKeyPair(String regionId, String keyName, String privateKeyEncryptionPassphrase, AsyncCallback<SSHKeyPairDTO> callback);
    
    /**
     * Verifies a passphrase for an SSH private key. Returns {@code true} if the passphrase can decipher the private key
     * and {@code false} if this does not work or the key is invalid, or the key is {@code null}.
     */
    void verifyPassphrase(String regionId, SSHKeyPairDTO key, String privateKeyEncryptionPassphrase, AsyncCallback<Boolean> callback);

    /**
     * The calling subject must have {@code CREATE} permission for the key requested as well as the
     * {@link CREATE_OBJECT} permission on the server on which this is called.
     */
    void addSshKeyPair(String regionId, String keyName, String publicKey, String encryptedPrivateKey, AsyncCallback<SSHKeyPairDTO> callback);

    void getEncryptedSshPrivateKey(String regionId, String keyName, AsyncCallback<byte[]> callback);

    void getSshPublicKey(String regionId, String keyName, AsyncCallback<byte[]> callback);

    void getAmazonMachineImages(String region, AsyncCallback<ArrayList<AmazonMachineImageDTO>> callback);

    void removeAmazonMachineImage(String region, String machineImageId, AsyncCallback<Void> callback);

    void upgradeAmazonMachineImage(String region, String machineImageId, AsyncCallback<AmazonMachineImageDTO> callback);

    void scaleMongo(String region, MongoScalingInstructionsDTO mongoScalingInstructions, String keyName,
            AsyncCallback<Void> asyncCallback);

    /**
     * Probes whether the current user has the {@code LANDSCAPE:MANAGE:AWS} permission and has previously
     * {@link #createMfaSessionCredentials(String, String, String, AsyncCallback) created} a valid set of session
     * credentials.
     */
    void hasValidSessionCredentials(AsyncCallback<Boolean> callback);
    
    /**
     * For a combination of an AWS access key ID, the corresponding secret plus an MFA token code produces new session
     * credentials and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. The current user must have the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    void createMfaSessionCredentials(String awsAccessKey, String awsSecret, String mfaTokenCode,
            AsyncCallback<Void> callback);

    /**
     * For a combination of an AWS access key ID, the corresponding secret plus a valid session token produces the session
     * credentials and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. The current user must have the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    void createSessionCredentials(String awsAccessKey, String awsSecret, String awsSessionToken,
            AsyncCallback<Void> callback);

    /**
     * For the current user who has to have the {@code LANDSCAPE:MANAGE:AWS} permission, clears the preference in the
     * user's preference store which holds any session credentials created previously using
     * {@link #createMfaSessionCredentials(String, String, String)}.
     */
    void clearSessionCredentials(AsyncCallback<Void> callback);
    
    void getApplicationReplicaSets(String regionId, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<ArrayList<SailingApplicationReplicaSetDTO<String>>> callback);

    void createApplicationReplicaSet(String regionId, String name, boolean sharedMasterInstance,
            String sharedInstanceType, String dedicatedInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String securityReplicationBearerToken, String replicaReplicationBearerToken, String optionalDomainName,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            Integer minimumAutoScalingGroupSizeOrNull, Integer maximumAutoScalingGroupSizeOrNull, Integer optionalIgtimiRiotPort,
            AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    void serializationDummy(ProcessDTO mongoProcessDTO, AwsInstanceDTO awsInstanceDTO, AwsShardDTO shardDTO,
            SailingApplicationReplicaSetDTO<String> sailingApplicationReplicationSetDTO, LeaderboardNameDTO leaderboard,
            AsyncCallback<SerializationDummyDTO> callback);

    void defineDefaultRedirect(String regionId, String hostname, RedirectDTO redirect, String keyName,
            String passphraseForPrivateKeyDecryption, AsyncCallback<Void> callback);

    void removeApplicationReplicaSet(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToRemove, MongoEndpointDTO moveDatabaseHere,
            String optionalKeyName, byte[] passphraseForPrivateKeyDescryption, AsyncCallback<String> callback);

    void createDefaultLoadBalancerMappings(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToCreateLoadBalancerMappingFor,
            boolean useDynamicLoadBalancer, String optionalDomainName, boolean forceDNSUpdate,
            AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    void upgradeApplicationReplicaSet(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToUpgrade, String releaseOrNullForLatestMaster,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicationBearerToken,
            AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    void getReleases(AsyncCallback<ArrayList<ReleaseDTO>> asyncCallback);

    void archiveReplicaSet(String regionId, SailingApplicationReplicaSetDTO<String> applicationReplicaSetToArchive,
            String bearerTokenOrNullForApplicationReplicaSetToArchive, String bearerTokenOrNullForArchive,
            Duration durationToWaitBeforeCompareServers, int maxNumberOfCompareServerAttempts,
            boolean removeApplicationReplicaSet, MongoEndpointDTO moveDatabaseHere, String optionalKeyName,
            byte[] passphraseForPrivateKeyDecryption,
            AsyncCallback<Triple<DataImportProgress, CompareServersResultDTO, String>> callback);

    void deployApplicationToExistingHost(String replicaSetName, AwsInstanceDTO hostToDeployTo,
            String replicaInstanceType, boolean dynamicLoadBalancerMapping, String releaseNameOrNullForLatestMaster,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String masterReplicationBearerToken,
            String replicaReplicationBearerToken, String optionalDomainName,
            Integer optionalMinimumAutoScalingGroupSizeOrNull, Integer optionalMaximumAutoScalingGroupSizeOrNull,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort,
            AwsInstanceDTO optionalPreferredInstanceToDeployUnmanagedReplicaTo,
            AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    /**
     * For the given replica set ensures there is at least one healthy replica, then stops replicating on all replicas and
     * removes the master from the public and master target groups. This can be used as a preparatory action for upgrading
     * the master while keeping one or more replicas available to handle read traffic.<p>
     * 
     * Other than de-registering the master from the replica set's target groups this method does nothing to the master
     * process/host.
     */
    void ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSet, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
            AsyncCallback<Boolean> callback);

    /**
     * Updates the AMI to use in the launch template version of those of the {@code replicaSets} that have an auto-scaling group.
     * Any running replica will not be affected by this. Only new replicas will be launched based on the AMI specified.
     * 
     * @param replicaSets
     *            those without an auto-scaling group won't be affected
     * @param amiDTOOrNullForLatest
     *            defaults to the latest image of type {@link SharedLandscapeConstants#IMAGE_TYPE_TAG_VALUE_SAILING}
     * @return those replica sets that were updated according to this request; those from {@code replicaSets} not part
     *         of this result have not had their AMI upgraded, probably because we didn't find an auto-scaling group and
     *         hence no launch template to update
     */
    void updateImageForReplicaSets(String regionId,
            ArrayList<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsToUpdate,
            AmazonMachineImageDTO amiDTOOrNullForLatest, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<ArrayList<SailingApplicationReplicaSetDTO<String>>> callback);

    void useDedicatedAutoScalingReplicasInsteadOfShared(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    void useSingleSharedInsteadOfDedicatedAutoScalingReplica(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            String optionalSharedReplicaInstanceType, AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    void moveMasterToOtherInstance(SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO,
            boolean useSharedInstance, String optionalInstanceTypeOrNull, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, String optionalMasterReplicationBearerTokenOrNull,
            String optionalReplicaReplicationBearerTokenOrNull, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull,
            AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    void changeAutoScalingReplicasInstanceType(SailingApplicationReplicaSetDTO<String> replicaSet,
            String instanceTypeName, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<SailingApplicationReplicaSetDTO<String>> callback);

    void getLeaderboardNames(SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken,
            AsyncCallback<ArrayList<LeaderboardNameDTO>> names);

    void addShard(String shardName, ArrayList<LeaderboardNameDTO> selectedLeaderBoardNames,
            SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken, String region,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<Void> callback);

    /**
     * @param callback
     *            returns the shards as keys and the sharding keys (escaped / mangled leaderboard names) as values.
     *            Those sharding keys are what you get when mangling the {@link AwsShardDTO#getLeaderboardNames()
     *            leaderboard names} of the shard.
     */
    void getShards(SailingApplicationReplicaSetDTO<String> replicaset, String region, String bearerToken,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<Map<AwsShardDTO, Iterable<String>>> callback);

    /**
     * Removes {@code shard} from the replica set. This deletes the load balancer listener rules, and the auto scaling
     * group and the target group.
     * 
     * @param shard
     *            the shard to remove
     * @param replicaSet
     *            the replica set which contains the shard.
     * @param region
     *            replica set's region
     * 
     */
    public void removeShard(AwsShardDTO shard, SailingApplicationReplicaSetDTO<String> replicaSet, String region,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<Void> callback);

    /**
     * Appends sharding keys for each leader board in {@code selectedLeaderboards} to the shard, identified by
     * {@code shardName}, from the {@code replicaset}. This function inserts rules to the replica set's load balancer
     * for each {@selectedLeaderboards}'s sharding key. It the replica set's load balancer does not have enough rules
     * left, a new one gets created. For inserting the rules, first every existing rule of this shard get's checked for
     * space left and if there is, it gets filled with a sharding key and after that new rules are created. Throws an
     * Exception if: - the shard is not found. - shards cannot be retrived - sharding rules cannot be inserted - the is
     * no free load balancer or the process of moving the replica set to another load balancer failed.
     * 
     * @param selectedLeaderBoards
     *            list of selected leaderboards. These are the names and not sharding keys.
     * @param region
     *            landscape region
     * @param shardName
     *            shard's name where the keys are supposed to be appended
     * @param replicaSet
     *            shard's replica set
     */
    void appendShardingKeysToShard(Iterable<LeaderboardNameDTO> selectedLeaderBoards, String region, String shardName,
            SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<Void> callback);

    /**
     * Removes the shardingkeys for {@selectedLeaderBoards} from a shard, identified by {@code shardName} from
     * {@code replicaset}. Throws Exception if: - no shard is found - replicaset is not found - shards from replica set
     * cannot be retrieved. - sharding rules cannot be updated
     * 
     * @param selectedLeaderBoards
     *            Sharding keys for all selected leader boards.
     * @param region
     *            shard's regio
     * @param shardName
     *            Shard's name where the keys should be removed from
     * @param replicaSet
     *            replica set which contains the shard
     */
    void removeShardingKeysFromShard(Iterable<LeaderboardNameDTO> selectedLeaderBoards, String region, String shardName,
            SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            AsyncCallback<Void> callback);

    /**
     * Removes all application processes {@link SailingAnalyticsHost#getApplicationProcesses(Optional, Optional, byte[])
     * found running} on {@code host} and deploys them to another host in {@code host}'s availability zone.
     * The default configuration for primaries and replicas is used based on the application replica set the
     * processes belong to, except for the memory configuration which is copied from the processes running
     * on {@code host}. This will mean that any hand-crafted special configuration will get lost during the
     * process. So don't apply this operation to hosts running non-standard application processes with non-default
     * configurations.
     * <p>
     * 
     * For those processes that are the primary ("master") instance of their replica set this method ensures that there
     * is at least one healthy replica available before moving the primary instance to the new host. When moving a
     * primary process, it is first removed from the "-m" target group, the new process is launched on the new host, and
     * when it is healthy it is added to the "-m" target group.
     * <p>
     * 
     * Moving a replica is easier: the replica can be launched on the new host first, added to the public target group
     * when healthy, and then the replica on the old {@code host} can be terminated and removed.
     * 
     * @param host
     *            must be a "multi-instance" host intended for sharing; this must be indicated by the tag value
     *            {@link SharedLandscapeConstants#MULTI_PROCESS_INSTANCE_TAG_VALUE "___multi___"} on the
     *            {@code sailing-analytics-server} tag of the instance. Otherwise, the method will throw an
     *            {@link IllegalStateException}.
     * @param optionalInstanceTypeForNewInstance
     *            if not specified, the new multi-instance launched will use the same instance type as the one from
     *            where the processes are moved away ({@code host})
     */
    void moveAllApplicationProcessesAwayFrom(AwsInstanceDTO host, String optionalInstanceTypeForNewInstance,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, AsyncCallback<Void> callback);

    void hasDNSResourceRecordsForReplicaSet(String replicaSetName, String optionalDomainName, AsyncCallback<Boolean> callback);
}
