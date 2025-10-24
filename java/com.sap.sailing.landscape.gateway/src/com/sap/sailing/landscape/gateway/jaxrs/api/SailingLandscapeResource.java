package com.sap.sailing.landscape.gateway.jaxrs.api;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.landscape.AwsSessionCredentialsWithExpiry;
import com.sap.sailing.landscape.LandscapeService;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.gateway.impl.AwsApplicationProcessJsonSerializer;
import com.sap.sailing.landscape.gateway.impl.AwsApplicationReplicaSetJsonSerializer;
import com.sap.sailing.landscape.gateway.impl.HostJsonSerializer;
import com.sap.sailing.landscape.gateway.jaxrs.AbstractLandscapeResource;
import com.sap.sailing.landscape.procedures.SailingAnalyticsHostSupplier;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sailing.server.gateway.serialization.impl.CompareServersResultJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DataImportProgressJsonSerializer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.MongoUriParser;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

import software.amazon.awssdk.services.ec2.model.InstanceType;

@Path("/landscape")
public class SailingLandscapeResource extends AbstractLandscapeResource {
    private static final Logger logger = Logger.getLogger(SailingLandscapeResource.class.getName());

    private static final String REGION_FORM_PARAM = "regionId";
    private static final String SHARED_MASTER_INSTANCE_FORM_PARAM = "sharedMasterInstance";
    private static final String REPLICA_SET_NAME_FORM_PARAM = "replicaSetName";
    private static final String DEDICATED_INSTANCE_TYPE_FORM_PARAM = "dedicatedInstanceType";
    private static final String SHARED_INSTANCE_TYPE_FORM_PARAM = "sharedInstanceType";
    private static final String INSTANCE_ID = "instanceId";
    private static final String INSTANCE_TYPE_FORM_PARAM = "instanceType";
    private static final String DYNAMIC_LOAD_BALANCER_MAPPING_FORM_PARAM = "dynamicLoadBalancerMapping";
    private static final String RELEASE_NAME_FORM_PARAM = "releaseName";
    private static final String KEY_NAME_FORM_PARAM = "keyName";
    private static final String PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM = "privateKeyEncryptionPassphrase";
    private static final String MASTER_REPLICATION_BEARER_TOKEN_FORM_PARAM = "masterReplicationBearerToken";
    private static final String REPLICA_REPLICATION_BEARER_TOKEN_FORM_PARAM = "replicaReplicationBearerToken";
    private static final String DOMAIN_NAME_FORM_PARAM = "domainName";
    private static final String MEMORY_IN_MEGABYTES_FORM_PARAM = "memoryInMegabytes";
    private static final String MEMORY_TOTAL_SIZE_FACTOR_FORM_PARAM = "memoryTotalSizeFactor";
    private static final String MINIMUM_AUTO_SCALING_GROUP_SIZE_FORM_PARAM = "minimumAutoScalingGroupSize";
    private static final String MAXIMUM_AUTO_SCALING_GROUP_SIZE_FORM_PARAM = "maximumAutoScalingGroupSize";
    private static final String TIMEOUT_IN_MILLISECONDS_FORM_PARAM = "timeoutInMilliseconds";
    private static final String REMOVE_APPLICATION_REPLICA_SET_FORM_PARAM = "removeApplicationReplicaSet";
    private static final String MAX_NUMBER_OF_COMPARE_SERVER_ATTEMPTS_FORM_PARAM = "maxNumberOfCompareserverAttempts";
    private static final String DURATION_TO_WAIT_BEFORE_COMPARE_SERVERS_IN_MILLISECONDS_FORM_PARAM = "durationToWaitBeforeCompareServersInMilliseconds";
    private static final String MDI_PROGRESS = "mdiProgress";
    private static final String COMPARE_SERVERS_RESULT = "compareServersResult";
    private static final String ARCHIVE_MONGODB_RESULT = "archiveMongoDBResult";
    private static final String BEARER_TOKEN_FOR_REPLICA_SET_TO_ARCHIVE_FORM_PARAM = "bearerTokenForReplicaSetToArchive";
    private static final String BEARER_TOKEN_FOR_ARCHIVE_FORM_PARAM = "bearerTokenForArchive";
    private static final String MONGO_URI_TO_ARCHIVE_DB_TO_FORM_PARAM = "mongoUriToArchiveDbTo";
    private static final String AWS_KEY_ID_FORM_PARAM = "awsKeyId";
    private static final String AWS_KEY_SECRET_FORM_PARAM = "awsKeySecret";
    private static final String AWS_MFA_TOKEN_FORM_PARAM = "awsMfaToken";
    private static final String AWS_SESSION_TOKEN_FORM_PARAM = "awsSessionToken";
    private static final String SESSION_TOKEN_EXPIRY_UNIX_TIME_MILLIS = "sessionTokenExpiryMillis";
    private static final String SESSION_TOKEN_EXPIRY_ISO = "sessionTokenExpiryISO";
    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_ERROR_MESSAGE = "message";
    private static final String RESPONSE_RELEASE = "release";
    private static final String HOST_ID_FORM_PARAM = "hostId";
    private static final String NEW_HOST_ID = "newHostId";
    private static final String MASTER_PROCESSES_MOVED = "masterProcessesMoved";
    private static final String REPLICA_PROCESSES_MOVED = "replicaProcessesMoved";
    private static final String REPLICA_SET_NAME = "replicaSetName";
    private static final String PORT = "port";
    private static final String IGTIMI_RIOT_PORT = "igtimiRiotPort";

    @Context
    UriInfo uriInfo;
    
    @Path("/createmfasessioncredentials")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response createMfaSessionCredentials(
            @FormParam(AWS_KEY_ID_FORM_PARAM) String awsKeyId,
            @FormParam(AWS_KEY_SECRET_FORM_PARAM) String awsKeySecret,
            @FormParam(AWS_MFA_TOKEN_FORM_PARAM) String mfaToken
            ) {
        checkLandscapeManageAwsPermission();
        Response response;
        try {
            getLandscapeService().createMfaSessionCredentials(awsKeyId, awsKeySecret, mfaToken);
            response = Response.ok().build();
        } catch (Exception e) {
            response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return response;
    }
    
    @Path("/createsessioncredentials")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response createSessionCredentials(
            @FormParam(AWS_KEY_ID_FORM_PARAM) String awsKeyId,
            @FormParam(AWS_KEY_SECRET_FORM_PARAM) String awsKeySecret,
            @FormParam(AWS_SESSION_TOKEN_FORM_PARAM) String sessionToken
            ) {
        checkLandscapeManageAwsPermission();
        Response response;
        try {
            getLandscapeService().createSessionCredentials(awsKeyId, awsKeySecret, sessionToken);
            response = Response.ok().build();
        } catch (Exception e) {
            response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return response;
    }
    
    @Path("/getsessioncredentials")
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getSessionCredentials() {
        checkLandscapeManageAwsPermission();
        Response response;
        try {
            final AwsSessionCredentialsWithExpiry sessionCredentials = getLandscapeService().getSessionCredentials();
            if (sessionCredentials == null) {
                response = Response.status(Status.NOT_FOUND).entity("No session credentials found for user "+getSecurityService().getCurrentUser().getName()).build();
            } else {
                final JSONObject result = new JSONObject();
                result.put(SESSION_TOKEN_EXPIRY_UNIX_TIME_MILLIS, sessionCredentials.getExpiration().asMillis());
                result.put(SESSION_TOKEN_EXPIRY_ISO, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(sessionCredentials.getExpiration().asDate()));
                response = Response.ok().entity(streamingOutput(result)).build();
            }
        } catch (Exception e) {
            response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return response;
    }
    
    @Path("/clearsessioncredentials")
    @POST
    @Produces("application/json;charset=UTF-8")
    public Response clearSessionCredentials() {
        checkLandscapeManageAwsPermission();
        Response response;
        try {
            getLandscapeService().clearSessionCredentials();
            response = Response.ok().build();
        } catch (Exception e) {
            response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return response;
    }
    
    @Path("/createapplicationreplicaset")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response createApplicationReplicaSet(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(SHARED_MASTER_INSTANCE_FORM_PARAM) @DefaultValue("false") boolean sharedMasterInstance,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(DEDICATED_INSTANCE_TYPE_FORM_PARAM) String dedicatedInstanceType,
            @FormParam(SHARED_INSTANCE_TYPE_FORM_PARAM) String sharedInstanceTypeOrNull,
            @FormParam(DYNAMIC_LOAD_BALANCER_MAPPING_FORM_PARAM) @DefaultValue("false") boolean dynamicLoadBalancerMapping,
            @FormParam(RELEASE_NAME_FORM_PARAM) String releaseNameOrNullForLatestMaster,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(MASTER_REPLICATION_BEARER_TOKEN_FORM_PARAM) String masterReplicationBearerToken,
            @FormParam(REPLICA_REPLICATION_BEARER_TOKEN_FORM_PARAM) String replicaReplicationBearerToken,
            @FormParam(DOMAIN_NAME_FORM_PARAM) String domainName,
            @FormParam(MEMORY_IN_MEGABYTES_FORM_PARAM) Integer optionalMemoryInMegabytesOrNull,
            @FormParam(MEMORY_TOTAL_SIZE_FACTOR_FORM_PARAM) Integer optionalMemoryTotalSizeFactorOrNull,
            @FormParam(MINIMUM_AUTO_SCALING_GROUP_SIZE_FORM_PARAM) Integer optionalMinimumAutoScalingGroupSize,
            @FormParam(MAXIMUM_AUTO_SCALING_GROUP_SIZE_FORM_PARAM) Integer optionalMaximumAutoScalingGroupSize,
            @FormParam(IGTIMI_RIOT_PORT) Integer optionalIgtimiRiotPort) {
        checkLandscapeManageAwsPermission();
        Response response;
        try {
            final Release release = getLandscapeService().getRelease(releaseNameOrNullForLatestMaster);
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet = getLandscapeService()
                    .createApplicationReplicaSet(regionId, replicaSetName, sharedMasterInstance, sharedInstanceTypeOrNull,
                            dedicatedInstanceType, dynamicLoadBalancerMapping, release.getName(),
                            optionalKeyName, privateKeyEncryptionPassphrase == null ? null : privateKeyEncryptionPassphrase.getBytes(), masterReplicationBearerToken,
                            replicaReplicationBearerToken, domainName, optionalMemoryInMegabytesOrNull,
                            optionalMemoryTotalSizeFactorOrNull, optionalIgtimiRiotPort, Optional.ofNullable(optionalMinimumAutoScalingGroupSize),
                            Optional.ofNullable(optionalMaximumAutoScalingGroupSize));
            final JSONObject result = new AwsApplicationReplicaSetJsonSerializer(release.getName()).serialize(replicaSet);
            response = Response.ok(streamingOutput(result)).build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception trying to create application replica set "+replicaSetName+" on behalf of user "+getSecurityService().getCurrentUser(), e);
            response = badRequest(e.getMessage());
        }
        return response;
    }
    
    @Path("/upgradeapplicationreplicaset")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response upgradeApplicationReplicaSet(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(RELEASE_NAME_FORM_PARAM) String releaseNameOrNullForLatestMaster,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(REPLICA_REPLICATION_BEARER_TOKEN_FORM_PARAM) String replicaReplicationBearerToken,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds) {
        checkLandscapeManageAwsPermission();
        ResponseBuilder responseBuilder;
        final JSONObject result = new JSONObject();
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet;
        try {
            replicaSet = getLandscapeService().getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName, passphraseForPrivateKeyDecryption);
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> upgradedReplicaSet = getLandscapeService().upgradeApplicationReplicaSet(region, replicaSet,
                    releaseNameOrNullForLatestMaster, optionalKeyName, passphraseForPrivateKeyDecryption,
                    replicaReplicationBearerToken);
            responseBuilder = Response.ok();
            result.put(RESPONSE_STATUS, "OK");
            result.put(RESPONSE_RELEASE, upgradedReplicaSet.getVersion(Optional.ofNullable(optionalTimeoutInMilliseconds).map(Duration::ofMillis),
                    Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption).getName());
        } catch (Exception e) {
            result.put(RESPONSE_STATUS, "ERROR");
            result.put(RESPONSE_ERROR_MESSAGE, e.getMessage());
            responseBuilder = Response.serverError();
        }
        return responseBuilder.entity(streamingOutput(result)).build();
    }    
    
    @Path("/movetoarchiveserver")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response moveToArchiveServer(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(BEARER_TOKEN_FOR_REPLICA_SET_TO_ARCHIVE_FORM_PARAM) String bearerTokenOrNullForApplicationReplicaSetToArchive,
            @FormParam(BEARER_TOKEN_FOR_ARCHIVE_FORM_PARAM) String bearerTokenOrNullForArchive,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds,
            @FormParam(DURATION_TO_WAIT_BEFORE_COMPARE_SERVERS_IN_MILLISECONDS_FORM_PARAM) @DefaultValue("300000") long durationToWaitBeforeCompareServersInMillis,
            @FormParam(MAX_NUMBER_OF_COMPARE_SERVER_ATTEMPTS_FORM_PARAM) @DefaultValue("5") int maxNumberOfCompareServerAttempts,
            @FormParam(REMOVE_APPLICATION_REPLICA_SET_FORM_PARAM) @DefaultValue("true") boolean removeApplicationReplicaSet,
            @FormParam(MONGO_URI_TO_ARCHIVE_DB_TO_FORM_PARAM) String mongoUriToArchiveDbTo) {
        checkLandscapeManageAwsPermission();
        if (removeApplicationReplicaSet) {
            getSecurityService().checkCurrentUserDeletePermission(SecuredSecurityTypes.SERVER.getQualifiedObjectIdentifier(
                    new TypeRelativeObjectIdentifier(replicaSetName)));
        }
        Response response;
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        final AwsLandscape<String> landscape = getLandscapeService().getLandscape();
        final MongoUriParser<String> mongoUriParser = new MongoUriParser<>(landscape, region);
        try {
            final MongoEndpoint moveDatabaseHere = mongoUriToArchiveDbTo == null ? null : mongoUriParser.parseMongoUri(mongoUriToArchiveDbTo).getEndpoint();
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSetToArchive = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (applicationReplicaSetToArchive == null) {
                response = badRequest("Application replica set with name " + replicaSetName + " not found in region "+regionId);
            } else {
                final Triple<DataImportProgress, CompareServersResult, String> mdiProgressAndCompareServersResult = getLandscapeService().archiveReplicaSet(regionId, applicationReplicaSetToArchive,
                    bearerTokenOrNullForApplicationReplicaSetToArchive, bearerTokenOrNullForArchive,
                    Duration.ofMillis(durationToWaitBeforeCompareServersInMillis), maxNumberOfCompareServerAttempts, removeApplicationReplicaSet,
                    moveDatabaseHere, optionalKeyName, passphraseForPrivateKeyDecryption);
                final JSONObject result = new JSONObject();
                result.put(MDI_PROGRESS, mdiProgressAndCompareServersResult.getA()==null?null:new DataImportProgressJsonSerializer().serialize(mdiProgressAndCompareServersResult.getA()));
                result.put(COMPARE_SERVERS_RESULT, mdiProgressAndCompareServersResult.getB()==null?null:new CompareServersResultJsonSerializer().serialize(mdiProgressAndCompareServersResult.getB()));
                result.put(ARCHIVE_MONGODB_RESULT, mdiProgressAndCompareServersResult.getC());
                response = Response.ok(streamingOutput(result)).build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage(), e);
            response = badRequest("Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage());
        }
        return response;
    }

    @Path("/moveallapplicationprocessesawayfrom")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response moveAllApplicationProcessesAwayFrom(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(INSTANCE_ID) String fromInstanceWithId,
            @FormParam(INSTANCE_TYPE_FORM_PARAM) String optionalInstanceType,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase) {
        checkLandscapeManageAwsPermission();
        Response response;
        try {
            final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
            final SailingAnalyticsHost<String> fromHost = getLandscapeService().getLandscape().getHostByInstanceId(region, fromInstanceWithId, new SailingAnalyticsHostSupplier<>());
            final byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
            final Triple<SailingAnalyticsHost<String>, Map<String, SailingAnalyticsProcess<String>>, Map<String, SailingAnalyticsProcess<String>>> result = getLandscapeService()
                    .moveAllApplicationProcessesAwayFrom(fromHost,
                            Optional.ofNullable(optionalInstanceType == null ? null : InstanceType.valueOf(optionalInstanceType)),
                            optionalKeyName, passphraseForPrivateKeyDecryption);
            final JSONObject jsonResult = new JSONObject();
            jsonResult.put(NEW_HOST_ID, result.getA().getId());
            final JSONArray masterProcessesMoved = getProcessesAndTheirReplicaSetNamesAsJSONArray(result.getB());
            jsonResult.put(MASTER_PROCESSES_MOVED, masterProcessesMoved);
            final JSONArray replicaProcessesMoved = getProcessesAndTheirReplicaSetNamesAsJSONArray(result.getC());
            jsonResult.put(REPLICA_PROCESSES_MOVED, replicaProcessesMoved);
            response = Response.ok(streamingOutput(jsonResult)).build();
        } catch (Exception e) {
            final String msg = "Error trying to move processes away from instance with ID "+fromInstanceWithId+" in region "+regionId+": "+e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            response = badRequest(msg);
        }
        return response;
    }

    private JSONArray getProcessesAndTheirReplicaSetNamesAsJSONArray(Map<String, SailingAnalyticsProcess<String>> processesKeyedByTheirApplicationReplicaSetName) {
        final JSONArray result = new JSONArray();
        for (final Entry<String, SailingAnalyticsProcess<String>> e : processesKeyedByTheirApplicationReplicaSetName.entrySet()) {
            final JSONObject processJson = new JSONObject();
            processJson.put(REPLICA_SET_NAME, e.getKey());
            processJson.put(PORT, e.getValue().getPort());
        }
        return result;
    }

    @Path("/removeapplicationreplicaset")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response removeApplicationReplicaSet(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(MONGO_URI_TO_ARCHIVE_DB_TO_FORM_PARAM) String mongoUriToArchiveDbTo) {
        checkLandscapeManageAwsPermission();
        getSecurityService().checkCurrentUserDeletePermission(SecuredSecurityTypes.SERVER.getQualifiedObjectIdentifier(
                new TypeRelativeObjectIdentifier(replicaSetName)));
        Response response;
        final AwsLandscape<String> landscape = getLandscapeService().getLandscape();
        final AwsRegion region = new AwsRegion(regionId, landscape);
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        try {
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSetToRemove = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (applicationReplicaSetToRemove == null) {
                response = badRequest("Application replica set with name "+replicaSetName+" not found in region "+regionId);
            } else {
                final MongoUriParser<String> mongoUriParser = new MongoUriParser<>(landscape, region);
                final MongoEndpoint moveDatabaseHere = mongoUriToArchiveDbTo == null ? null : mongoUriParser.parseMongoUri(mongoUriToArchiveDbTo).getEndpoint();
                getLandscapeService().removeApplicationReplicaSet(regionId, applicationReplicaSetToRemove, moveDatabaseHere,
                        optionalKeyName, passphraseForPrivateKeyDecryption);
                response = Response.ok().build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage(), e);
            response = badRequest("Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage());
        }
        return response;
    }
    
    @Path("/deployreplicatoexistinghost")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response deployReplicaToExistingHost(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(HOST_ID_FORM_PARAM) String hostId,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(REPLICA_REPLICATION_BEARER_TOKEN_FORM_PARAM) String replicaReplicationBearerToken,
            @FormParam(MEMORY_IN_MEGABYTES_FORM_PARAM) Integer optionalMemoryInMegabytesOrNull,
            @FormParam(MEMORY_TOTAL_SIZE_FACTOR_FORM_PARAM) Integer optionalMemoryTotalSizeFactorOrNull,
            @FormParam(IGTIMI_RIOT_PORT) Integer optionalIgtimiRiotPort) {
        checkLandscapeManageAwsPermission();
        Response response;
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        try {
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (replicaSet == null) {
                response = badRequest("Application replica set with name "+replicaSetName+" not found in region "+regionId);
            } else {
                final LandscapeService landscapeService = getLandscapeService();
                final SailingAnalyticsHost<String> hostToDeployTo = landscapeService.getLandscape().getHostByInstanceId(region, hostId, new SailingAnalyticsHostSupplier<>());
                if (landscapeService.isEligibleForDeployment(hostToDeployTo, replicaSet.getServerName(), replicaSet.getPort(),
                        optionalIgtimiRiotPort,
                        optionalTimeoutInMilliseconds == null ? Optional.empty() : Optional.of(Duration.ofMillis(optionalTimeoutInMilliseconds)),
                        optionalKeyName, passphraseForPrivateKeyDecryption)) {
                    final SailingAnalyticsProcess<String> process = getLandscapeService().deployReplicaToExistingHost(replicaSet, hostToDeployTo, optionalKeyName,
                            passphraseForPrivateKeyDecryption, replicaReplicationBearerToken,
                            optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull, optionalIgtimiRiotPort);
                    response = Response.ok().entity(streamingOutput(
                            new AwsApplicationProcessJsonSerializer<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>()
                                    .serialize(process)))
                            .build();
                } else {
                    response = badRequest("Host " + hostToDeployTo
                            + " is not eligible for deploying a process of application replica set " + replicaSetName
                            + ". Check port and directory.");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage(), e);
            response = badRequest("Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage());
        }
        return response;
    }

    @Path("/gethostseligibleforreplicasetprocessdeployment")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response getHostsEligibleForReplicaSetProcessDeployment(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase) {
        checkLandscapeManageAwsPermission();
        Response response;
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        try {
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (replicaSet == null) {
                response = badRequest("Application replica set with name "+replicaSetName+" not found in region "+regionId);
            } else {
                final JSONArray result = new JSONArray();
                for (final SailingAnalyticsHost<String> host : getLandscapeService().getEligibleSharedHostsForReplicaSet(region, replicaSet, optionalKeyName, passphraseForPrivateKeyDecryption)) {
                    result.add(new HostJsonSerializer<String>().serialize(host));
                }
                response = Response.ok().entity(streamingOutput(result)).build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage(), e);
            response = badRequest("Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage());
        }
        return response;
    }

    @Path("/usededicatedautoscalingreplicasinsteadofshared")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response useDedicatedAutoScalingReplicasInsteadOfShared(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase) {
        checkLandscapeManageAwsPermission();
        Response response;
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        try {
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (replicaSet == null) {
                response = badRequest("Application replica set with name "+replicaSetName+" not found in region "+regionId);
            } else {
                final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result =
                        getLandscapeService().useDedicatedAutoScalingReplicasInsteadOfShared(replicaSet, optionalKeyName, passphraseForPrivateKeyDecryption);
                response = Response.ok()
                        .entity(streamingOutput(new AwsApplicationReplicaSetJsonSerializer(
                                result.getVersion(optionalTimeoutInMilliseconds==null?Landscape.WAIT_FOR_PROCESS_TIMEOUT:Optional.of(Duration.ofMillis(optionalTimeoutInMilliseconds)),
                                        Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption).getName())
                                                .serialize(result)))
                        .build();
            }
        } catch (Exception e) {
            final String message = "Error trying to use dedicated auto-scaling replicas only for replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage();
            logger.log(Level.SEVERE, message, e);
            response = badRequest(message);
        }
        return response;
    }

    @Path("/usesinglesharedinsteadofdedicatedautoscalingreplica")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response useSingleSharedInsteadOfDedicatedAutoScalingReplica(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds,
            @FormParam(SHARED_INSTANCE_TYPE_FORM_PARAM) String sharedInstanceTypeOrNull,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(REPLICA_REPLICATION_BEARER_TOKEN_FORM_PARAM) String replicaReplicationBearerToken,
            @FormParam(MEMORY_IN_MEGABYTES_FORM_PARAM) Integer optionalMemoryInMegabytesOrNull,
            @FormParam(MEMORY_TOTAL_SIZE_FACTOR_FORM_PARAM) Integer optionalMemoryTotalSizeFactorOrNull) {
        checkLandscapeManageAwsPermission();
        Response response;
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        try {
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (replicaSet == null) {
                response = badRequest("Application replica set with name "+replicaSetName+" not found in region "+regionId);
            } else {
                final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result = getLandscapeService()
                        .useSingleSharedInsteadOfDedicatedAutoScalingReplica(replicaSet, optionalKeyName,
                                passphraseForPrivateKeyDecryption, replicaReplicationBearerToken,
                                optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull,
                                Optional.ofNullable(sharedInstanceTypeOrNull).map(sharedInstanceTypeName->InstanceType.valueOf(sharedInstanceTypeName)));
                response = Response.ok()
                        .entity(streamingOutput(new AwsApplicationReplicaSetJsonSerializer(result.getVersion(
                                optionalTimeoutInMilliseconds == null ? Landscape.WAIT_FOR_PROCESS_TIMEOUT
                                        : Optional.of(Duration.ofMillis(optionalTimeoutInMilliseconds)),
                                Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption).getName()).serialize(result)))
                        .build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage(), e);
            response = badRequest("Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage());
        }
        return response;
    }

    @Path("/movemastertootherinstance")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response moveMasterToOtherInstance(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(SHARED_MASTER_INSTANCE_FORM_PARAM) @DefaultValue("false") Boolean useSharedInstance,
            @FormParam(TIMEOUT_IN_MILLISECONDS_FORM_PARAM) Long optionalTimeoutInMilliseconds,
            @FormParam(INSTANCE_TYPE_FORM_PARAM) String optionalInstanceType,
            @FormParam(HOST_ID_FORM_PARAM) String optionalIdOfHostToDeployTo,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(MASTER_REPLICATION_BEARER_TOKEN_FORM_PARAM) String masterReplicationBearerToken,
            @FormParam(REPLICA_REPLICATION_BEARER_TOKEN_FORM_PARAM) String replicaReplicationBearerToken,
            @FormParam(MEMORY_IN_MEGABYTES_FORM_PARAM) Integer optionalMemoryInMegabytesOrNull,
            @FormParam(MEMORY_TOTAL_SIZE_FACTOR_FORM_PARAM) Integer optionalMemoryTotalSizeFactorOrNull) {
        checkLandscapeManageAwsPermission();
        Response response;
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        try {
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, optionalTimeoutInMilliseconds, optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (replicaSet == null) {
                response = badRequest("Application replica set with name "+replicaSetName+" not found in region "+regionId);
            } else {
                final Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo = Optional
                        .ofNullable(optionalIdOfHostToDeployTo).map(instanceId -> getLandscapeService().getLandscape()
                                .getHostByInstanceId(region, instanceId, new SailingAnalyticsHostSupplier<String>()));
                final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result = getLandscapeService()
                        .moveMasterToOtherInstance(replicaSet, useSharedInstance,
                                Optional.ofNullable(optionalInstanceType).map(instanceTypeName->InstanceType.valueOf(instanceTypeName)),
                                optionalPreferredInstanceToDeployTo, optionalKeyName, passphraseForPrivateKeyDecryption,
                                masterReplicationBearerToken, replicaReplicationBearerToken,
                                optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull);
                response = Response.ok()
                        .entity(streamingOutput(new AwsApplicationReplicaSetJsonSerializer(result.getVersion(
                                optionalTimeoutInMilliseconds == null ? Landscape.WAIT_FOR_PROCESS_TIMEOUT
                                        : Optional.of(Duration.ofMillis(optionalTimeoutInMilliseconds)),
                                Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption).getName()).serialize(result)))
                        .build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage(), e);
            response = badRequest("Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage());
        }
        return response;
    }

    @Path("/changeautoscalingreplicasinstancetype")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response changeAutoScalingReplicasInstanceType(
            @FormParam(REGION_FORM_PARAM) String regionId,
            @FormParam(REPLICA_SET_NAME_FORM_PARAM) String replicaSetName,
            @FormParam(KEY_NAME_FORM_PARAM) String optionalKeyName,
            @FormParam(PRIVATE_KEY_ENCRYPTION_PASSPHRASE_FORM_PARAM) String privateKeyEncryptionPassphrase,
            @FormParam(INSTANCE_TYPE_FORM_PARAM) String instanceType) {
        checkLandscapeManageAwsPermission();
        Response response;
        final AwsRegion region = new AwsRegion(regionId, getLandscapeService().getLandscape());
        byte[] passphraseForPrivateKeyDecryption = privateKeyEncryptionPassphrase==null?null:privateKeyEncryptionPassphrase.getBytes();
        try {
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet = getLandscapeService()
                    .getApplicationReplicaSet(region, replicaSetName, Landscape.WAIT_FOR_PROCESS_TIMEOUT.get().asMillis(), optionalKeyName,
                            passphraseForPrivateKeyDecryption);
            if (replicaSet == null) {
                response = badRequest("Application replica set with name "+replicaSetName+" not found in region "+regionId);
            } else {
                final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result = getLandscapeService()
                        .changeAutoScalingReplicasInstanceType(replicaSet, InstanceType.valueOf(instanceType),
                                Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption);
                response = Response.ok()
                        .entity(streamingOutput(new AwsApplicationReplicaSetJsonSerializer(result.getVersion(
                                Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption).getName()).serialize(result)))
                        .build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage(), e);
            response = badRequest("Error trying to archive replica set "+replicaSetName+" in region "+regionId+": "+e.getMessage());
        }
        return response;
    }
}
