package com.sap.sse.security.interfaces;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.shiro.crypto.hash.Sha256Hash;

import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.UserGroupProvider;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecurityUserImpl;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.subscription.Subscription;

public class UserImpl extends SecurityUserImpl<RoleDefinition, Role, UserGroup, WildcardPermission> implements User {
    private static final long serialVersionUID = 1788215575606546042L;

    /**
     * An optional clear-text user name, used to address the user, e.g., in the UI ("Hello ...")
     */
    private String fullName;

    /**
     * An optional company affiliation. May be used, e.g., to better understand the statistics of
     * corporate vs. private users, if used as a marketing tool.
     */
    private String company;
    
    /**
     * An optional field specifying the locale preference of the user. This can be used to internationalize User
     * specific elements as UIs or notification mails.
     */
    private Locale locale;

    private String email;
    
    /**
     * When a new e-mail is set for the user, a validation process should be started.
     * The validation generates a secret which is then put into a URL which is sent to
     * the new e-mail address. When the user follows the URL, the URL parameter will be
     * used to validate against the secret stored here. If the secret matches, the
     * email address is {@link #emailValidated marked as validated}.
     */
    private String validationSecret;
    
    /**
     * When someone has requested a password reset, only the owner of the validated e-mail address is
     * permitted to actually carry out the reset. This is verified by sending a "reset secret" to the
     * validated e-mail address, giving the user a link to an entry point for actually carrying out the
     * reset. The reset is only accepted if the reset secret was provided correctly.
     */
    private String passwordResetSecret;
    
    private boolean emailValidated;

    private final Map<AccountType, Account> accounts;

    private transient UserGroupProvider userGroupProvider;
    
    /**
     * Roles can refer back to this user object, e.g., for the user qualification, or during the tenant qualification if
     * this user belongs to the tenant for which the role is qualified. Therefore, this set has to be transient, and the
     * {@link #roleListForSerialization} field takes over the serialization.
     * <p>
     * 
     * While it seems obvious to resolve the list into a {@link HashSet} in the {@code readResolve()} method, care must
     * be taken due to the possibility of object graph cycles. In such cases the {@link UserGroup} and {@link User}
     * objects referenced by {@link Role#getQualifiedForTenant() user group} and {@link Role#getQualifiedForUser() user}
     * qualifications may not be fully resolved yet when this object's {@code readResolve()} method is invoked.
     * Therefore, this field is resolved lazily. It remains {@code null} during the entire de-serialization process,
     * including {@code readResolve()}. It is initialized with a new {@link Set} only when first requested.
     * When a thread reads a non-{@code null} reference here then the set returned is fully initialized.
     * 
     * @see #writeObject
     * @see #readResolve
     * @see #roleListForSerialization
     */
    private transient Set<Role> roles;
    
    private List<Role> roleListForSerialization;

    private Subscription[] subscriptions;
    
    private final LockingAndBanning lockingAndBanning;

    public UserImpl(String name, String email, Map<String, UserGroup> defaultTenantForServer,
            UserGroupProvider userGroupProvider, LockingAndBanning lockingAndBanning, Account... accounts) {
        this(name, email, defaultTenantForServer, Arrays.asList(accounts), userGroupProvider, lockingAndBanning);
    }

    public UserImpl(String name, String email, Map<String, UserGroup> defaultTenantForServer,
            Collection<Account> accounts, UserGroupProvider userGroupProvider, LockingAndBanning lockingAndBanning) {
        this(name, email, /* fullName */ null, /* company */ null, /* locale */ null, /* is email validated */ false,
                /* password reset secret */ null, /* validation secret */ null, defaultTenantForServer, accounts,
                userGroupProvider, lockingAndBanning);
    }

    public UserImpl(String name, String email, String fullName, String company, Locale locale, Boolean emailValidated,
            String passwordResetSecret, String validationSecret, Map<String, UserGroup> defaultTenantForServer,
            Collection<Account> accounts, UserGroupProvider userGroupProvider, LockingAndBanning lockingAndBanning) {
        super(name);
        this.lockingAndBanning = lockingAndBanning;
        this.defaultTenantForServer = defaultTenantForServer;
        this.fullName = fullName;
        this.company = company;
        this.locale = locale;
        this.email = email;
        this.passwordResetSecret = passwordResetSecret;
        this.validationSecret = validationSecret;
        this.emailValidated = emailValidated;
        this.accounts = new HashMap<>();
        this.userGroupProvider = userGroupProvider;
        for (Account a : accounts) {
            this.accounts.put(a.getAccountType(), a);
        }
        roles = new HashSet<>();
    }
    
    @Override
    protected Set<Role> getRolesInternal() {
        final Set<Role> result;
        if (roles != null) {
            result = roles;
        } else {
            synchronized (this) {
                if (roles == null) {
                    result = new HashSet<>(roleListForSerialization);
                    roleListForSerialization = null;
                    roles = result;
                }
            }
        }
        return roles;
    }
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(new ArrayList<>(getRolesInternal()));
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        roleListForSerialization = (List<Role>) ois.readObject();
    }
    
    /**
     * The main use case for this method is to restore the link to a {@link UserStore} after de-serialization, e.g.,
     * on a replica.
     */
    @Override
    public void setUserGroupProvider(UserGroupProvider userGroupProvider) {
        this.userGroupProvider = userGroupProvider;
    }

    @Override
    public UserGroupProvider getUserGroupProvider() {
        return userGroupProvider;
    }

    /**
     * The tenant to use as {@link Ownership#getTenantOwner() tenant owner} of new objects created by this user
     */
    private Map<String, UserGroup> defaultTenantForServer;

    /**
     * For the time being, the user {@link #getName() name} is used as ID
     */
    @Override
    public Serializable getId() {
        return getName();
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String getCompany() {
        return company;
    }

    @Override
    public void setCompany(String company) {
        this.company = company;
    }
    
    @Override
    public Locale getLocale() {
        return locale;
    }
    
    @Override
    public void setLocale(Locale locale) {
        this.locale = locale;
    }
    
    @Override
    public Locale getLocaleOrDefault() {
        return locale == null ? Locale.ENGLISH : locale;
    }

    @Override
    public Account getAccount(AccountType type) {
        return accounts.get(type);
    }

    @Override
    public void removeAccount(AccountType type) {
        accounts.remove(type);
    }

    @Override
    public Map<AccountType, Account> getAllAccounts() {
        return Collections.unmodifiableMap(accounts);
    }

    @Override
    public String getEmail() {
        return email;
    }
    
    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * The email address is set to not yet validated by resetting the
     * {@link #emailValidated} flag. A new {@link #validationSecret} is generated and returned which
     * can be used in a call to {@link #validate(String)} to validate the e-mail address.
     */
    @Override
    public void startEmailValidation(String randomSecret) {
        validationSecret = randomSecret;
        emailValidated = false;
    }
    
    /**
     * Creates, remembers and returns a new password reset secret. This secret can later again be obtained
     * by calling {@link #getPasswordResetSecret()}. A user store should only allow a service call to reset
     * a user's password in case the service can provide the correct password reset secret.
     */
    @Override
    public void startPasswordReset(String randomSecret) {
        passwordResetSecret = randomSecret;
    }
    
    @Override
    public String getPasswordResetSecret() {
        return passwordResetSecret;
    }
    
    @Override
    public String createRandomSecret() {
        final byte[] bytes1 = new byte[64];
        new SecureRandom().nextBytes(bytes1);
        final byte[] bytes2 = new byte[64];
        new Random().nextBytes(bytes2);
        return new Sha256Hash(bytes1, bytes2, 1024).toBase64();
    }
    
    /**
     * If the user's e-mail has already been {@link #isEmailValidated() validated}, or the <code>validationSecret</code>
     * passed matches {@link #validationSecret}, the e-mail is {@link #emailValidated marked as validated}, and
     * <code>true</code> is returned. Otherwise, the validation secret on this user remains in place, and the e-mail
     * address is not marked as validated.
     */
    @Override
    public boolean validate(final String validationSecret) {
        final boolean result;
        if (emailValidated) {
            result = true;
        } else if (validationSecret.equals(this.validationSecret)) {
            emailValidated = true;
            this.validationSecret = null;
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    /**
     * Clears the {@link #passwordResetSecret}.
     */
    @Override
    public void passwordWasReset() {
        passwordResetSecret = null;
    }

    @Override
    public boolean isEmailValidated() {
        return emailValidated;
    }

    /**
     * Uses the {@link #userGroupProvider} passed to this object's constructor to dynamically
     * query the groups that this user is member of.
     */
    @Override
    public Iterable<UserGroup> getUserGroups() {
        return userGroupProvider.getUserGroupsOfUser(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserImpl [");
        builder.append("getName()=");
        builder.append(getName());
        builder.append(", ");
        if (defaultTenantForServer != null) {
            builder.append("defaultTenantForServer=[");
            for (Entry<String, UserGroup> entry : defaultTenantForServer.entrySet()) {
                builder.append(entry.getValue().getName());
                builder.append("@");
                builder.append(entry.getKey());
                builder.append(",");
            }
            builder.append("], ");
        }
        if (getFullName() != null) {
            builder.append("getFullName()=");
            builder.append(getFullName());
            builder.append(", ");
        }
        if (getCompany() != null) {
            builder.append("getCompany()=");
            builder.append(getCompany());
            builder.append(", ");
        }
        if (getLocale() != null) {
            builder.append("getLocale()=");
            builder.append(getLocale());
            builder.append(", ");
        }
        if (getEmail() != null) {
            builder.append("getEmail()=");
            builder.append(getEmail());
            builder.append(", ");
        }
        builder.append("isEmailValidated()=");
        builder.append(isEmailValidated());
        builder.append(", ");
        if (getPermissions() != null) {
            builder.append("getPermissions()=");
            builder.append(getPermissions());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String getValidationSecret() {
        return validationSecret;
    }

    @Override
    public UserGroup getDefaultTenant(String serverName) {
        return defaultTenantForServer.get(serverName);
    }

    @Override
    public void setDefaultTenant(UserGroup newDefaultTenant, String serverName) {
        this.defaultTenantForServer.put(serverName, newDefaultTenant);
    }

    @Override
    public Map<String, UserGroup> getDefaultTenantMap() {
        return defaultTenantForServer;
    }

    @Override
    public Iterable<Subscription> getSubscriptions() {
        final Iterable<Subscription> result;
        if (subscriptions == null) {
            result = null;
        } else {
            result = Arrays.asList(subscriptions);
        }
        return result;
    }

    @Override
    public void setSubscriptions(Subscription[] subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public Subscription getSubscriptionByPlan(String planId) {
        if (planId != null && !planId.equals("") && subscriptions != null && 
                subscriptions != null && subscriptions.length > 0) {
            for (Subscription subscription : subscriptions) {
                if (subscription.getPlanId() != null && subscription.getPlanId().equals(planId)) {
                    return subscription;
                }
            }
        }
        return null;
    }

    @Override
    public Subscription getSubscriptionById(String subscriptionId) {
        if (subscriptionId != null && !subscriptionId.isEmpty() && 
                subscriptions != null && subscriptions.length > 0) {
            for (Subscription subscription : subscriptions) {
                if (subscription.getSubscriptionId() != null
                        && subscription.getSubscriptionId().equals(subscriptionId)) {
                    return subscription;
                }
            }
        }
        return null;
    }
    
    @Override
    public boolean hasActiveSubscription(String planId) {
        boolean result = false;
        if (subscriptions != null && subscriptions.length > 0) {
            for (Subscription subscription : subscriptions) {
                if (subscription.isActiveSubscription() && subscription.getPlanId() != null
                        && subscription.getPlanId().equals(planId)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasAnySubscription(String planId) {
        if (subscriptions != null && subscriptions.length > 0) {
            for (Subscription subscription : subscriptions) {
                if (planId.equals(subscription.getPlanId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public LockingAndBanning getLockingAndBanning() {
        return lockingAndBanning;
    }
}
