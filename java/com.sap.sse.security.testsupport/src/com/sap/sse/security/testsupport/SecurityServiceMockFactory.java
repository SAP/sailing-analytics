package com.sap.sse.security.testsupport;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.sse.security.Action;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.UserGroupImpl;

public class SecurityServiceMockFactory {
    @SuppressWarnings("unchecked")
    public static SecurityService mockSecurityService() {
        UserGroupImpl defaultTenant = new UserGroupImpl(new UUID(0, 1), "defaultTenant");
        final SecurityService result = Mockito.mock(SecurityService.class);
        Mockito.doReturn(defaultTenant).when(result).getServerGroup();
        Mockito.doAnswer(invocation -> {
            invocation.getArgument(/* index */ 3, Runnable.class).run();
            return invocation.getArgument(/* index */ 4, Callable.class).call();
        }).when(result).setOwnershipCheckPermissionForObjectCreationAndRevertOnError(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(Runnable.class), Mockito.any(Callable.class));
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(4, Callable.class).call();
            }
        }).when(result).setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(Action.class));
        Mockito.doAnswer(new Answer<Object>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                for (Object arg : invocation.getArguments()) {
                    if (arg instanceof Callable) {
                        return ((Callable) arg).call();
                    }
                }
                return null;
            }
        }).when(result).setOwnershipCheckPermissionForObjectCreationAndRevertOnError(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(Callable.class));
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgument(1, Action.class).run();
                return null;
            }
        }).when(result).checkPermissionAndDeleteOwnershipForObjectRemoval(Mockito.any(QualifiedObjectIdentifier.class), Mockito.any(Action.class));
        Mockito.doReturn(true).when(result)
                .hasCurrentUserReadPermission(Mockito.any(WithQualifiedObjectIdentifier.class));
        Mockito.doNothing().when(result).checkCurrentUserReadPermission(Mockito.any());
        return result;
    }
}
