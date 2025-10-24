package com.sap.sse.security.shared.impl;

import java.util.Locale;
import java.util.Map;

import com.sap.sse.common.Named;
import com.sap.sse.common.WithID;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.SecurityUser;
import com.sap.sse.security.shared.UserGroupProvider;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.subscription.Subscription;

/**
 * The {@link Named#getName() name} is the ID for this user; usually a nickname or short name. Implements the
 * {@link WithID} key, so {@link WithID#getId()} does return the result of {@link #getName()}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface User extends SecurityUser<RoleDefinition, Role, UserGroup> {
    /**
     * An optional clear-text user name, used to address the user, e.g., in the UI ("Hello ...")
     */
    String getFullName();

    void setFullName(String fullName);

    /**
     * An optional company affiliation. May be used, e.g., to better understand the statistics of
     * corporate vs. private users, if used as a marketing tool.
     */
    String getCompany();

    void setCompany(String company);

    String getEmail();

    void addPermission(WildcardPermission permission);

    void removePermission(WildcardPermission permission);

    void addRole(Role role);

    void removeRole(Role role);

    Account getAccount(AccountType type);

    void removeAccount(AccountType type);

    Map<AccountType, Account> getAllAccounts();

    /**
     * Sets an e-mail address for this user. The address is considered not yet validated, therefore the
     * caller shall also ensure that {@link #startEmailValidation} is invoked hereafter, with a secret
     * produced by {@link #createRandomSecret()}.
     */
    void setEmail(String email);

    /**
     * When someone has requested a password reset, only the owner of the validated e-mail address is
     * permitted to actually carry out the reset. This is verified by sending a "reset secret" to the
     * validated e-mail address, giving the user a link to an entry point for actually carrying out the
     * reset. The reset is only accepted if the reset secret was provided correctly.
     */
    String getPasswordResetSecret();

    void startPasswordReset(String randomSecret);

    /**
     * Resets the {@link #isEmailValidated()} property and stores the new {@code randomSecret} as the
     * e-mail validation secret. The {@link #isEmailValidated()} method will return {@code true} only
     * after {@link #validate(String)} has been called with the {@code randomSecret} passed here.
     */
    void startEmailValidation(String randomSecret);

    boolean validate(String validationSecret);

    void passwordWasReset();

    boolean isEmailValidated();

    /**
     * When a new e-mail is set for the user, a validation process should be started.
     * The validation generates a secret which is then put into a URL which is sent to
     * the new e-mail address. When the user follows the URL, the URL parameter will be
     * used to validate against the secret stored here. If the secret matches, the
     * email address is {@link #emailValidated marked as validated}.
     */
    String getValidationSecret();

    /**
     * An optional field specifying the locale preference of the user. This can be used to internationalize User
     * specific elements as UIs or notification mails.
     */
    Locale getLocale();

    void setLocale(Locale locale);

    Locale getLocaleOrDefault();

    UserGroup getDefaultTenant(String serverName);

    Map<String, UserGroup> getDefaultTenantMap();

    void setDefaultTenant(UserGroup newDefaultTenant, String serverName);

    void setUserGroupProvider(UserGroupProvider userGroupProvider);
    
    UserGroupProvider getUserGroupProvider();

    String createRandomSecret();

    Iterable<Subscription> getSubscriptions();
    
    boolean hasActiveSubscription(String planId);
    
    boolean hasAnySubscription(String planId);
    
    void setSubscriptions(Subscription[] subscriptions);
    
    Subscription getSubscriptionByPlan(String planId);
    
    Subscription getSubscriptionById(String subscriptionId);
    
    LockingAndBanning getLockingAndBanning();
}
