package com.sap.sailing.domain.igtimiadapter.gateway.oauth;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.igtimiadapter.gateway.impl.Activator;
import com.sap.sailing.domain.igtimiadapter.impl.IgtimiConnectionFactoryImpl;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.User;

@Path(AuthorizationCallback.OAUTH + RestApiApplication.V1 + AuthorizationCallback.AUTHORIZATIONCALLBACK)
public class AuthorizationCallback {
    private static final Logger logger = Logger.getLogger(AuthorizationCallback.class.getName());
    
    private static final String OAUTH = "/oauth";
    private static final String AUTHORIZATIONCALLBACK = "/authorizationcallback";
    private final IgtimiConnectionFactoryImpl connectionFactory;
    
    public AuthorizationCallback() throws ClientProtocolException, IllegalStateException, IOException, ParseException {
        connectionFactory = (IgtimiConnectionFactoryImpl) Activator.getInstance().getConnectionFactory();
    }

    /**
     * When a non-empty {@code state} parameter is passed, it is assumed to hold the base URL of the server
     * to which to re-direct this request. In this case we're probably running on www.sapsailing.com, and
     * an authorization request redirect callback from the Igtimi Oauth provider has reached us. The original
     * server that requested authentication is expected to have set its base URL in the {@code state} parameter
     * and will then receive the same call, only without the {@code state} parameter.<p>
     * 
     * If the {@code state} parameter is empty, the call is meant for "us" (this server instance), and the
     * access token will be obtained from the {@code code} passed with the request.
     */
    @GET
    @Produces("text/plain;charset=UTF-8")
    public Response obtainAccessToken(@QueryParam("code") String code, @QueryParam("state") String state)
            throws ClientProtocolException, IOException, IllegalStateException, ParseException, URISyntaxException {
        Response result;
        if (state != null) {
            // redirect to the server whose base URL is contained in the state parameter
            String target = Base64.getEncoder().encodeToString(
                    (state + "/igtimi/oauth/v1" + AUTHORIZATIONCALLBACK + "?code=" + URLEncoder.encode(code, "UTF-8"))
                            .getBytes());
            result = Response.temporaryRedirect(new URI(state + "/igtimi/redirect.html?" + target)).build();
        } else {
            if (code != null) {
                try {
                    final SecurityService securityService = Activator.getInstance().getSecurityService();
                    final User currentUser = securityService.getCurrentUser();
                    if (currentUser != null) {
                        connectionFactory.obtainAccessTokenFromAuthorizationCode(currentUser.getName(), code);
                        result = Response.ok()
                                .entity("SAP Sailing Analytics authorized successfully to access user's tracks")
                                .build();
                    } else {
                        result = Response.status(Status.UNAUTHORIZED).entity(
                                "SAP Sailing Analytics could not authorize access user's tracks: Current user not found.")
                                .build();
                    }
                } catch (Exception e) {
                    result = Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
                    logger.log(Level.SEVERE, "Error trying to obtain access token from authentication code " + code, e);
                }
            } else {
                result = Response.status(Status.UNAUTHORIZED).entity("code parameter not found in request").build();
                logger.log(Level.SEVERE, "Authentication code not found in request");
            }
        }
        return result;
    }

    @POST
    @Produces("text/plain;charset=UTF-8")
    @Path(AUTHORIZATIONCALLBACK)
    public Response obtainAccessTokenPost(@QueryParam("code") String code, @QueryParam("state") String state)
            throws ClientProtocolException, IOException, IllegalStateException, ParseException, URISyntaxException {
        return obtainAccessToken(code, state);
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("uri")
    public Response getWithUri(@Context UriInfo info) {
        return Response.ok().build();
    }
}
