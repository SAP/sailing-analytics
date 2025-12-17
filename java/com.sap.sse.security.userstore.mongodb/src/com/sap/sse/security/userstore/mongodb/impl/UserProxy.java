package com.sap.sse.security.userstore.mongodb.impl;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import com.sap.sse.common.TimedLock;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.UserGroupProvider;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.shared.subscription.Subscription;

public class UserProxy implements User {
    private static final long serialVersionUID = 1L;
    private String name;

    public UserProxy(String name) {
        this.name = name;
    }

    @Override
    public Iterable<WildcardPermission> getPermissions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasRole(Role role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Role> getRoles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<UserGroup> getUserGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Serializable getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HasPermissions getPermissionType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFullName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFullName(String fullName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCompany() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCompany(String company) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEmail() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPermission(WildcardPermission permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePermission(WildcardPermission permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRole(Role role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRole(Role role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Account getAccount(AccountType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAccount(AccountType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<AccountType, Account> getAllAccounts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEmail(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPasswordResetSecret() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startPasswordReset(String randomSecret) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startEmailValidation(String randomSecret) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean validate(String validationSecret) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void passwordWasReset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmailValidated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValidationSecret() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocaleOrDefault() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserGroupImpl getDefaultTenant(String serverName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, UserGroup> getDefaultTenantMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultTenant(UserGroup newDefaultTenant, String serverName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserGroupProvider(UserGroupProvider userGroupProvider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserGroupProvider getUserGroupProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String createRandomSecret() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Subscription> getSubscriptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSubscriptions(Subscription[] subscriptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Subscription getSubscriptionByPlan(String planId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Subscription getSubscriptionById(String subscriptionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAnySubscription(String planId) {
        throw new UnsupportedOperationException();
    }
    

    @Override
    public boolean hasActiveSubscription(String planId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TimedLock getTimedLock() {
        throw new UnsupportedOperationException();
    }
}
