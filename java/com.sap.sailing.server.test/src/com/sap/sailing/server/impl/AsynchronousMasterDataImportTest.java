package com.sap.sailing.server.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import java.util.Collections;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.DataImportSubProgress;
import com.sap.sailing.domain.common.impl.BaseMasterDataImportObjectCreationCountImpl;
import com.sap.sailing.server.interfaces.DataImportLockWithProgress;
import com.sap.sailing.server.operationaltransformation.CreateOrUpdateDataImportProgress;
import com.sap.sse.common.Util;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.User;

public class AsynchronousMasterDataImportTest {
    @Test
    public void replicaReceivesEventIdsOnlyWithTerminalResult() throws Exception {
        final RacingEventServiceImpl replica = mock(RacingEventServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        final DataImportLockWithProgress lock = new DataImportLockWithProgress();
        doReturn(lock).when(replica).getDataImportLock();
        final UUID id = UUID.randomUUID();
        final UUID eventId = UUID.randomUUID();
        new CreateOrUpdateDataImportProgress(id, /* overallProgressPct */ 0.8,
                DataImportSubProgress.IMPORT_SENSOR_FIXES, /* subProgressPct */ 0.0).internalApplyTo(replica);
        assertNull(lock.getProgress(id).getResult());
        final BaseMasterDataImportObjectCreationCountImpl counts = new BaseMasterDataImportObjectCreationCountImpl();
        counts.setEventIdsImported(Collections.singleton(eventId));
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(new CreateOrUpdateDataImportProgress(id, /* overallProgressPct */ 1.0,
                    DataImportSubProgress.IMPORT_SENSOR_FIXES, /* subProgressPct */ 1.0, counts));
        }
        try (final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ((CreateOrUpdateDataImportProgress) input.readObject()).internalApplyTo(replica);
        }
        assertTrue(Util.contains(lock.getProgress(id).getResult().getEventIdsImported(), eventId));
        assertEquals(/* expected */ 0, lock.getProgress(id).getResult().getEventCount());
    }

    @Test
    public void registersBeforeReturningPreservesSubjectAndReportsEarlyFailure() throws Exception {
        final RacingEventServiceImpl service = mock(RacingEventServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        final DataImportLockWithProgress lock = new DataImportLockWithProgress();
        final SecurityService security = mock(SecurityService.class);
        doReturn(lock).when(service).getDataImportLock();
        doReturn(security).when(service).getSecurityService();
        doReturn(mock(User.class)).when(security).getCurrentUser();
        doNothing().when(service).replicate(any());
        final CountDownLatch acquiringToken = new CountDownLatch(/* count */ 1);
        final CountDownLatch releaseToken = new CountDownLatch(/* count */ 1);
        final CountDownLatch finished = new CountDownLatch(/* count */ 1);
        final AtomicReference<Object> workerPrincipal = new AtomicReference<>();
        doAnswer(invocation -> {
            workerPrincipal.set(SecurityUtils.getSubject().getPrincipal());
            acquiringToken.countDown();
            assertTrue(releaseToken.await(/* timeout */ 10, TimeUnit.SECONDS));
            throw new IllegalStateException("token acquisition failed");
        }).when(security).getOrCreateTargetServerBearerToken(anyString(), any(), any(), any());
        doAnswer(invocation -> {
            finished.countDown();
            return null;
        }).when(service).setDataImportDeleteProgressFromMapTimerWithReplication(any());
        final Subject previousSubject = ThreadContext.getSubject();
        final DefaultSecurityManager securityManager = new DefaultSecurityManager();
        final Subject subject = new Subject.Builder(securityManager)
                .principals(new SimplePrincipalCollection("import-user", "test"))
                .authenticated(/* authenticated */ true).buildSubject();
        final UUID id = UUID.randomUUID();
        ThreadContext.bind(subject);
        try {
            service.importMasterDataAsync("https://localhost", new UUID[0], /* override */ true, /* compress */ true,
                    /* exportWind */ true, /* exportDeviceConfigurations */ false, /* targetServerUsername */ null,
                    /* targetServerPassword */ null, /* targetServerBearerToken */ null,
                    /* exportTrackedRacesAndStartTracking */ true, id);
            final DataImportProgress progress = lock.getProgress(id);
            assertNotNull(progress);
            assertTrue(acquiringToken.await(/* timeout */ 5, TimeUnit.SECONDS));
            assertNull(progress.getResult());
            assertFalse(progress.failed());
            assertEquals("import-user", workerPrincipal.get());
            assertThrows(IllegalArgumentException.class, () -> service.importMasterDataAsync("https://localhost",
                    new UUID[0], /* override */ true, /* compress */ true, /* exportWind */ true,
                    /* exportDeviceConfigurations */ false, /* targetServerUsername */ null,
                    /* targetServerPassword */ null, /* targetServerBearerToken */ null,
                    /* exportTrackedRacesAndStartTracking */ true, id));
            releaseToken.countDown();
            assertTrue(finished.await(/* timeout */ 5, TimeUnit.SECONDS));
            assertTrue(progress.failed());
            assertTrue(progress.getErrorMessage().contains("token acquisition failed"));
            assertNull(progress.getResult());
        } finally {
            releaseToken.countDown();
            ThreadContext.unbindSubject();
            if (previousSubject != null) {
                ThreadContext.bind(previousSubject);
            }
            securityManager.destroy();
        }
    }
}
