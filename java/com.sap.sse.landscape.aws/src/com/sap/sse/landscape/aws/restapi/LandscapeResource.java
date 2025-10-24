package com.sap.sse.landscape.aws.restapi;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.TimePoint;
import com.sap.sse.landscape.aws.AwsLandscapeState;
import com.sap.sse.landscape.aws.impl.Activator;
import com.sap.sse.landscape.ssh.SSHKeyPair;
import com.sap.sse.security.jaxrs.AbstractSecurityResource;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.shared.json.JsonSerializer;

@Path("/landscape")
public class LandscapeResource extends AbstractSecurityResource {
    private static final Logger logger = Logger.getLogger(LandscapeResource.class.getName());

    private static final JsonSerializer<SSHKeyPair> sshKeyPairJsonSerializer = new SSHKeyPairJsonSerializer();
    
    /**
     * Obtains the SSH key pairs whose {@link SSHKeyPair#getCreatorName() creator name} equals that of any of the
     * {@code usernames} passed in any of the {@code username[]} query parameters. The requesting user must have the
     * {@link DefaultActions#READ} permission for the respective SSH keys, otherwise the key won't be added to the
     * response.
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/get_ssh_keys_owned_by_user")
    public Response getSshKeysOwnedByUser(@QueryParam("username[]") final Set<String> usernames) throws IOException {
        final JSONArray sshKeysAsJsonArray = new JSONArray();
        final AwsLandscapeState landscape = Activator.getInstance().getLandscapeState();
        for (final SSHKeyPair sshKeyPair : landscape.getSSHKeyPairs()) {
            if (usernames.contains(sshKeyPair.getCreatorName()) &&
                    SecurityUtils.getSubject().isPermitted(sshKeyPair.getIdentifier().getStringPermission(DefaultActions.READ))) {
                sshKeysAsJsonArray.add(sshKeyPairJsonSerializer.serialize(sshKeyPair));
            }
        }
        return Response.ok(streamingOutput(sshKeysAsJsonArray)).build();
    }

    /**
     * Creates an SSH key pair with the name given, in the region specified by {@code regionId}. The method checks whether
     * the user authenticated is permitted to 
     */
    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("/add_ssh_key")
    public Response addSshKey(@FormParam("region") final String regionId,
            @FormParam("key_name") String keyName,
            @FormParam("public_key") String publicKey,
            @FormParam("encrypted_private_key") String encryptedPrivateKey) throws IOException {
        Response response;
        final Subject subject = SecurityUtils.getSubject();
        final SSHKeyPair sshKeyPair = new SSHKeyPair(regionId, subject.getPrincipal().toString(), 
                TimePoint.now(), keyName, publicKey.getBytes(), encryptedPrivateKey.getBytes());
        final AwsLandscapeState landscape = Activator.getInstance().getLandscapeState();
        try {
            getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(sshKeyPair.getPermissionType(),
                sshKeyPair.getIdentifier().getTypeRelativeObjectIdentifier(), keyName,
                ()->landscape.addSSHKeyPair(sshKeyPair));
            response = Response.ok(streamingOutput(sshKeyPairJsonSerializer.serialize(sshKeyPair))).build();
        } catch (AuthorizationException e) {
            final String message = "User "+subject.getPrincipal()+" not authorized to create SSH key pair "+keyName+" in region "+regionId;
            logger.warning(message);
            response = Response.serverError().entity(message).build();
        }
        return response;
    }

    /**
     * Deletes the SSH key pair with the name provided
     */
    @DELETE
    @Produces("application/json;charset=UTF-8")
    @Path("/delete_ssh_key")
    public Response deleteSshKey(@QueryParam("region") final String regionId, @QueryParam("key_name") final String keyName) throws IOException {
        final JSONArray sshKeysAsJsonArray = new JSONArray();
        final AwsLandscapeState landscape = Activator.getInstance().getLandscapeState();
        final SSHKeyPair sshKeyPair = landscape.getSSHKeyPair(regionId, keyName);
        final Response result;
        if (sshKeyPair == null) {
            result = Response.status(Status.NOT_FOUND).entity("Key "+keyName+" not found in region "+regionId).build();
        } else {
            SecurityUtils.getSubject().checkPermission(sshKeyPair.getIdentifier().getStringPermission(DefaultActions.DELETE));
            landscape.deleteKeyPair(regionId, keyName);
            sshKeysAsJsonArray.add(sshKeyPairJsonSerializer.serialize(sshKeyPair));
            result = Response.ok(streamingOutput(sshKeysAsJsonArray)).build();
        }
        return result;
    }

    /**
     * Obtains the time point when the last change in the set of users having the permission {@code LANDSCAPE:MANAGE:AWS} was
     * observed. The earliest time point that may be reported is the time this bundle has been activated.
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/get_time_point_of_last_change_in_ssh_keys_of_aws_landscape_managers")
    public Response getTimePointOfLastChangeInSshKeysOfAwsLandscapeManagers() throws IOException {
        final JSONObject result = new JSONObject();
        final TimePoint timePointOfLastChangeOfSetOfLandscapeManagers = Activator.getInstance().getTimePointOfLastChangeOfSetOfLandscapeManagers();
        result.put("timePointOfLastChangeOfSetOfLandscapeManagers-millis", timePointOfLastChangeOfSetOfLandscapeManagers.asMillis());
        final DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmmX");
        result.put("timePointOfLastChangeOfSetOfLandscapeManagers-iso", isoDateFormat.format(timePointOfLastChangeOfSetOfLandscapeManagers.asDate()));
        return Response.ok(streamingOutput(result)).build();
    }
}
