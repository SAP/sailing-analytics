package com.sap.sailing.windestimation.jaxrs.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;

import com.sap.sailing.domain.common.security.Permission;
import com.sap.sailing.domain.common.security.Permission.Mode;
import com.sap.sailing.windestimation.integration.ExportedModels;
import com.sap.sailing.windestimation.integration.WindEstimationModelsUpdateOperation;
import com.sap.sailing.windestimation.integration.WindEstimationModelsUpdateOperationImpl;
import com.sap.sailing.windestimation.jaxrs.AbstractWindEstimationDataResource;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
@Path("/windestimation_data")
public class WindEstimationDataResource extends AbstractWindEstimationDataResource {
    @GET
    @Produces("application/octet-stream;charset=UTF-8")
    public Response getInternalModelData() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        getWindEstimationFactoryServiceImpl().serializeForInitialReplication(bos);
        bos.close();
        return Response.ok(new ByteArrayInputStream(bos.toByteArray()))
                .header("Content-Type", "application/octet-stream").build();
    }

    @POST
    @Produces("text/plain")
    public Response postInternalModelData(InputStream inputStream) throws Exception {
        SecurityUtils.getSubject().checkPermission(Permission.WIND_ESTIMATION_MODELS.getStringPermission(Mode.UPDATE));
        ObjectInputStream ois = getWindEstimationFactoryServiceImpl()
                .createObjectInputStreamResolvingAgainstCache(inputStream);
        ExportedModels exportedModels = (ExportedModels) ois.readObject();
        WindEstimationModelsUpdateOperation windEstimationModelsUpdateOperation = new WindEstimationModelsUpdateOperationImpl(
                exportedModels);
        getWindEstimationFactoryServiceImpl().apply(windEstimationModelsUpdateOperation);
        return Response.ok("Wind estimation models accepted").build();
    }
}
