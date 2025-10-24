package com.sap.sailing.landscape.ui.client;

import java.util.ArrayList;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.sap.sailing.domain.common.DataImportProgress;
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

public interface LandscapeManagementWriteService extends RemoteService {
    ArrayList<String> getRegions();
    
    ArrayList<String> getInstanceTypeNames(boolean canBeDeployedInNlbInstanceBasedTargetGroup);

    ArrayList<MongoEndpointDTO> getMongoEndpoints(String region) throws Exception;
    
    ArrayList<ReverseProxyDTO> getReverseProxies(String region) throws Exception;
    
    void removeReverseProxy(ReverseProxyDTO instance, String region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    void rotateHttpdLogs(ReverseProxyDTO proxy, String region, String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)throws Exception;
    
    void addReverseProxy(String instanceName, String instanceType, String region, String launchKey, AvailabilityZoneDTO availabilityZoneDTO);
    
    ArrayList<AvailabilityZoneDTO> describeAvailabilityZones(String region);
    
    MongoEndpointDTO getMongoEndpoint(String region, String replicaSetName) throws Exception;

    ArrayList<SSHKeyPairDTO> getSshKeys(String regionId);

    void removeSshKey(SSHKeyPairDTO keyPair);

    SSHKeyPairDTO generateSshKeyPair(String regionId, String keyName, String privateKeyEncryptionPassphrase);

    SSHKeyPairDTO addSshKeyPair(String regionId, String keyName, String publicKey,
            String encryptedPrivateKey) throws Exception;

    byte[] getEncryptedSshPrivateKey(String regionId, String keyName) throws Exception;

    byte[] getSshPublicKey(String regionId, String keyName) throws Exception;

    ArrayList<AmazonMachineImageDTO> getAmazonMachineImages(String region);

    void removeAmazonMachineImage(String region, String machineImageId);

    AmazonMachineImageDTO upgradeAmazonMachineImage(String region, String machineImageId) throws Exception;

    void scaleMongo(String region, MongoScalingInstructionsDTO mongoScalingInstructions, String keyName) throws Exception;

    Boolean verifyPassphrase(String regionId, SSHKeyPairDTO key, String privateKeyEncryptionPassphrase);
    
    /**
     * For a combination of an AWS access key ID, the corresponding secret plus an MFA token code produces new session
     * credentials and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. The current user must have the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    void createMfaSessionCredentials(String awsAccessKey, String awsSecret, String mfaTokenCode);

    /**
     * For a combination of an AWS access key ID, the corresponding secret plus a valid session token produces the session
     * credentials and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. The current user must have the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    void createSessionCredentials(String awsAccessKey, String awsSecret, String awsSessionToken);

    /**
     * For the current user who has to have the {@code LANDSCAPE:MANAGE:AWS} permission, clears the preference in the
     * user's preference store which holds any session credentials created previously using
     * {@link #createMfaSessionCredentials(String, String, String)}.
     */
    void clearSessionCredentials();

    boolean hasValidSessionCredentials();
    
    ArrayList<SailingApplicationReplicaSetDTO<String>> getApplicationReplicaSets(String regionId,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    SerializationDummyDTO serializationDummy(ProcessDTO mongoProcessDTO, AwsInstanceDTO awsInstanceDTO, AwsShardDTO shardDTO,
            SailingApplicationReplicaSetDTO<String> sailingApplicationReplicationSetDTO, LeaderboardNameDTO leaderboard);

    SailingApplicationReplicaSetDTO<String> createApplicationReplicaSet(String regionId, String name, boolean sharedMasterInstance,
            String masterInstanceType, String optionalReplicaInstanceTypeOrNull, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String securityReplicationBearerToken,
            String replicaReplicationBearerToken, String optionalDomainName, Integer minimumAutoScalingGroupSizeOrNull,
            Integer maximumAutoScalingGroupSizeOrNull, Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            Integer optionalIgtimiRiotPort) throws Exception;

    void defineDefaultRedirect(String regionId, String hostname, RedirectDTO redirect, String keyName, String passphraseForPrivateKeyDecryption);

    String removeApplicationReplicaSet(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToRemove, MongoEndpointDTO moveDatabaseHere,
            String optionalKeyName, byte[] passphraseForPrivateKeyDescryption) throws Exception;

    SailingApplicationReplicaSetDTO<String> createDefaultLoadBalancerMappings(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToCreateLoadBalancerMappingFor,
            boolean useDynamicLoadBalancer, String optionalDomainName, boolean forceDNSUpdate) throws Exception;
    
    SailingApplicationReplicaSetDTO<String> upgradeApplicationReplicaSet(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToUpgrade, String releaseOrNullForLatestMaster,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String securityReplicationBearerToken) throws Exception;

    ArrayList<ReleaseDTO> getReleases();

    Triple<DataImportProgress, CompareServersResultDTO, String> archiveReplicaSet(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToArchive,
            String bearerTokenOrNullForApplicationReplicaSetToArchive,
            String bearerTokenOrNullForArchive,
            Duration durationToWaitBeforeCompareServers,
            int maxNumberOfCompareServerAttempts, boolean removeApplicationReplicaSet,
            MongoEndpointDTO moveDatabaseHere, String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception;

    SailingApplicationReplicaSetDTO<String> deployApplicationToExistingHost(String replicaSetName,
            AwsInstanceDTO hostToDeployTo, String replicaInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String masterReplicationBearerToken, String replicaReplicationBearerToken, String optionalDomainName,
            Integer optionalMinimumAutoScalingGroupSizeOrNull, Integer optionalMaximumAutoScalingGroupSizeOrNull,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            Integer optionalIgtimiRiotPort, AwsInstanceDTO optionalPreferredInstanceToDeployUnmanagedReplicaTo) throws Exception;


    Boolean ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSet, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken) throws Exception;

    ArrayList<SailingApplicationReplicaSetDTO<String>> updateImageForReplicaSets(String regionId,
            ArrayList<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsToUpdate,
            AmazonMachineImageDTO amiDTO,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    SailingApplicationReplicaSetDTO<String> useDedicatedAutoScalingReplicasInsteadOfShared(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception;

    SailingApplicationReplicaSetDTO<String> useSingleSharedInsteadOfDedicatedAutoScalingReplica(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            String optionalSharedReplicaInstanceType) throws Exception;

    SailingApplicationReplicaSetDTO<String> moveMasterToOtherInstance(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, boolean useSharedInstance,
            String optionalInstanceTypeOrNull, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String optionalMasterReplicationBearerTokenOrNull, String optionalReplicaReplicationBearerTokenOrNull,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull) throws Exception;

    SailingApplicationReplicaSetDTO<String> changeAutoScalingReplicasInstanceType(
            SailingApplicationReplicaSetDTO<String> replicaSet, String instanceTypeName,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    ArrayList<LeaderboardNameDTO> getLeaderboardNames(SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken) throws Exception;
    
    void addShard(String shardName, ArrayList<LeaderboardNameDTO> selectedLeaderBoardNames, SailingApplicationReplicaSetDTO<String> replicaSet,
            String bearerToken, String region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    public Map<AwsShardDTO, Iterable<String>> getShards(SailingApplicationReplicaSetDTO<String> replicaSet, String region, String bearerToken, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    public void removeShard(AwsShardDTO shard, SailingApplicationReplicaSetDTO<String> replicaSet, String region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    void appendShardingKeysToShard(Iterable<LeaderboardNameDTO> shardingKeysToAppend, String region, String shardName, SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    void removeShardingKeysFromShard(Iterable<LeaderboardNameDTO> shardingKeysToRemove, String region, String shardName, SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    void moveAllApplicationProcessesAwayFrom(AwsInstanceDTO host, String optionalInstanceTypeForNewInstance,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    boolean hasDNSResourceRecordsForReplicaSet(String replicaSetName, String optionalDomainName);
}
