package com.sap.sailing.server.gateway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.DataImportSubProgress;
import com.sap.sailing.domain.common.impl.BaseMasterDataImportObjectCreationCountImpl;
import com.sap.sailing.domain.common.impl.DataImportProgressImpl;
import com.sap.sailing.server.gateway.deserialization.impl.DataImportProgressJsonDeserializer;
import com.sap.sailing.server.gateway.impl.SailingServerImpl;
import com.sap.sailing.server.gateway.jaxrs.api.MasterDataImportResource;
import com.sap.sailing.server.gateway.serialization.impl.DataImportProgressJsonSerializer;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.SecurityService;

public class AsynchronousMasterDataImportRestTest {
    @Test
    public void restEndpointAcceptsAsyncImportAndReturnsItsUuid() throws Exception {
        final UUID id = UUID.randomUUID();
        final RacingEventService service = mock(RacingEventService.class);
        final SecurityService security = mock(SecurityService.class);
        final MasterDataImportResource resource = new MasterDataImportResource() {
            @Override
            public RacingEventService getService() {
                return service;
            }

            @Override
            public SecurityService getSecurityService() {
                return security;
            }
        };
        final Response response = resource.importMasterDataAsync("https://localhost", /* remoteServerUsername */ null,
                /* remoteServerPassword */ null, /* remoteServerBearerToken */ null, Collections.emptyList(),
                /* override */ true, /* compress */ true, /* exportWind */ true, /* exportDeviceConfigs */ false,
                /* exportTrackedRacesAndStartTracking */ true, id.toString());
        assertEquals(/* expected */ 202, response.getStatus());
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(body);
        final JSONObject json = (JSONObject) new JSONParser().parse(body.toString("UTF-8"));
        assertEquals(id.toString(), json.get(MasterDataImportResource.PROGRESS_TRACKING_UUID));
        verify(service).importMasterDataAsync(eq("https://localhost"), any(), eq(/* value */ true), eq(/* value */ true),
                eq(/* value */ true), eq(/* value */ false), any(), any(), any(), eq(/* value */ true), eq(id));
        verify(service, never()).importMasterData(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    public void clientUsesAsyncEndpointAndHandlesProgressStatuses() throws Exception {
        final UUID id = UUID.randomUUID();
        final JSONObject accepted = new JSONObject();
        accepted.put(MasterDataImportResource.PROGRESS_TRACKING_UUID, id.toString());
        final AtomicReference<Pair<Object, Integer>> response = new AtomicReference<>(new Pair<>(accepted, /* b */ 202));
        final AtomicReference<HttpUriRequest> request = new AtomicReference<>();
        final SailingServerImpl server = new SailingServerImpl(new URL("https://localhost/"), /* bearerToken */ null) {
            @Override
            protected Pair<Object, Integer> getJsonParsedResponse(final HttpUriRequest actualRequest) {
                request.set(actualRequest);
                return response.get();
            }
        };
        assertEquals(id, server.importMasterDataAsync(server, Collections.singleton(id), /* override */ true,
                /* compress */ true, /* exportWind */ true, /* exportDeviceConfigs */ false,
                /* exportTrackedRacesAndStartTracking */ true, Optional.of(id)));
        assertEquals("/sailingserver/api/v1/masterdataimport/async", request.get().getURI().getPath());
        final String form = EntityUtils.toString(((HttpEntityEnclosingRequestBase) request.get()).getEntity());
        assertTrue(form.contains("progressTrackingUUID=" + id));
        response.set(new Pair<>(/* a */ null, /* b */ 404));
        assertNull(server.getMasterDataImportProgress(id));
        assertEquals("/sailingserver/api/v1/masterdataimport/progress", request.get().getURI().getPath());
        response.set(new Pair<>(/* a */ null, /* b */ 403));
        assertThrows(SecurityException.class, () -> server.getMasterDataImportProgress(id));
        response.set(new Pair<>(/* a */ null, /* b */ 503));
        assertThrows(HttpResponseException.class, () -> server.getMasterDataImportProgress(id));
        response.set(new Pair<>(/* a */ null, /* b */ 200));
        assertThrows(IOException.class, () -> server.getMasterDataImportProgress(id));
    }

    @Test
    public void completionMetadataSurvivesJsonAndDistinguishesMissingFromEmpty() throws Exception {
        final UUID eventId = UUID.randomUUID();
        final DataImportProgress progress = new DataImportProgressImpl(UUID.randomUUID());
        progress.setCurrentSubProgress(DataImportSubProgress.IMPORT_SENSOR_FIXES);
        final DataImportProgressJsonSerializer serializer = new DataImportProgressJsonSerializer();
        final DataImportProgressJsonDeserializer deserializer = new DataImportProgressJsonDeserializer();
        assertNull(deserializer.deserialize(serializer.serialize(progress)).getResult());
        final BaseMasterDataImportObjectCreationCountImpl counts = new BaseMasterDataImportObjectCreationCountImpl();
        counts.setEventIdsImported(Collections.singleton(eventId));
        progress.setResult(counts);
        final DataImportProgress completed = deserializer.deserialize(serializer.serialize(progress));
        assertEquals(/* expected */ 0, completed.getResult().getEventCount());
        assertTrue(Util.contains(completed.getResult().getEventIdsImported(), eventId));
        counts.setEventIdsImported(Collections.emptyList());
        final DataImportProgress empty = deserializer.deserialize(serializer.serialize(progress));
        assertNotNull(empty.getResult().getEventIdsImported());
        assertTrue(Util.isEmpty(empty.getResult().getEventIdsImported()));
        final JSONObject legacyJson = serializer.serialize(progress);
        ((JSONObject) legacyJson.get(DataImportProgressJsonSerializer.RESULT)).remove(DataImportProgressJsonSerializer.EVENT_IDS_IMPORTED);
        assertNull(deserializer.deserialize(legacyJson).getResult().getEventIdsImported());
    }
}
