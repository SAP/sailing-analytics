package com.sap.sse.security.datamining.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.security.datamining.data.HasRoleOfUserGroupContext;
import com.sap.sse.security.datamining.data.HasUserGroupContext;
import com.sap.sse.security.datamining.data.impl.RoleOfUserGroupWithContext;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class SecurityRolesOfUserGroupRetrievalProcessor extends AbstractRetrievalProcessor<HasUserGroupContext, HasRoleOfUserGroupContext> {
    public SecurityRolesOfUserGroupRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasRoleOfUserGroupContext, ?>> resultReceivers, int retrievalLevel,
            String retrievedDataTypeMessageKey) {
        super(HasUserGroupContext.class, HasRoleOfUserGroupContext.class, executor, resultReceivers,
                retrievalLevel, retrievedDataTypeMessageKey);
    }

    @Override
    protected Iterable<HasRoleOfUserGroupContext> retrieveData(HasUserGroupContext element) {
        final Set<HasRoleOfUserGroupContext> data = new HashSet<>();
        for (final Entry<RoleDefinition, Boolean> roleDefinitionAndForAll : element.getUserGroup().getRoleDefinitionMap().entrySet()) {
            if (isAborted()) {
                break;
            }
            final Subject subject = SecurityUtils.getSubject();
            if (subject.isPermitted(roleDefinitionAndForAll.getKey().getIdentifier().getStringPermission(DefaultActions.READ))) {
                data.add(new RoleOfUserGroupWithContext(element, roleDefinitionAndForAll.getKey(), roleDefinitionAndForAll.getValue()));
            }
        }
        return data;
    }
}
