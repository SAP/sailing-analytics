package com.sap.sailing.polars.jaxrs.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.polars.jaxrs.AbstractPolarResource;
import com.sap.sse.ServerInfo;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

@Path("/polar_data")
public class PolarDataResource extends AbstractPolarResource {
    private static final Logger logger = Logger.getLogger(PolarDataResource.class.getName());

    @GET
    @Produces("application/octet-stream;charset=UTF-8")
    public Response getRegressions() throws IOException {
        final Subject subject = SecurityUtils.getSubject();
        logger.info("Polar Data requested by "+ (subject.getPrincipal() == null ? "anonymous user" : subject.getPrincipal().toString()));
        subject.checkPermission(SecuredDomainType.POLAR_DATA.getStringPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                            new TypeRelativeObjectIdentifier(ServerInfo.getName())));
        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                getPolarDataServiceImpl().serializeForInitialReplication(output);
            }
        }).header("Content-Type", "application/octet-stream").build();
    }
}
