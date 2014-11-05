package com.sap.sse.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.shiro.crypto.hash.Sha256Hash;

import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;

public class User {

    private String name;

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

    private final Set<String> roles;
    private final Map<AccountType, Account> accounts;

    public User(String name, String email, Account... accounts) {
        this(name, email, Arrays.asList(accounts));
    }

    public User(String name, String email, Collection<Account> accounts) {
        this(name, email, /* is email validated */ false, /* validation secret */ null, accounts);
    }

    public User(String name, String email, Boolean emailValidated, String validationSecret,
            Collection<Account> accounts) {
        super();
        this.name = name;
        this.roles = new HashSet<>();
        this.email = email;
        this.validationSecret = validationSecret;
        this.emailValidated = emailValidated;
        this.accounts = new HashMap<>();
        for (Account a : accounts) {
            this.accounts.put(a.getAccountType(), a);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void addRole(String role) {
        roles.add(role);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    public void removeRole(String role) {
        roles.remove(role);
    }

    public Account getAccount(AccountType type) {
        return accounts.get(type);
    }

    public void removeAccount(AccountType type) {
        accounts.remove(type);
    }

    public Map<AccountType, Account> getAllAccounts() {
        return accounts;
    }

    public String getEmail() {
        return email;
    }
    
    /**
     * Sets an e-mail address for this user. The address is considered not yet validated, therefore the
     * {@link #emailValidated} flag is reset, and a new {@link #validationSecret} is generated and returned which
     * can be used in a call to {@link #validate(String)} to validate the e-mail address.
     */
    public String setEmail(String email) {
        this.email = email;
        return startEmailValidation();
    }

    /**
     * The email address is set to not yet validated by resetting the
     * {@link #emailValidated} flag. A new {@link #validationSecret} is generated and returned which
     * can be used in a call to {@link #validate(String)} to validate the e-mail address.
     */
    public String startEmailValidation() {
        validationSecret = createRandomSecret();
        emailValidated = false;
        return validationSecret;
    }
    
    /**
     * Creates, remembers and returns a new password reset secret. This secret can later again be obtained
     * by calling {@link #getPasswordResetSecret()}. A user store should only allow a service call to reset
     * a user's password in case the service can provide the correct password reset secret.
     */
    public String startPasswordReset() {
        passwordResetSecret = createRandomSecret();
        return passwordResetSecret;
    }
    
    public String getPasswordResetSecret() {
        return passwordResetSecret;
    }
    
    private String createRandomSecret() {
        final byte[] bytes1 = new byte[64];
        new Random().nextBytes(bytes1);
        final byte[] bytes2 = new byte[64];
        new Random().nextBytes(bytes2);
        return new Sha256Hash(bytes1, bytes2, 1024).toBase64();
    }
    
    /**
     * If the <code>validationSecret</code> passed matches {@link #validationSecret}, the e-mail is
     * {@link #emailValidated marked as validated}, and <code>true</code> is returned. Otherwise, the validation secret
     * on this user remains in place, and the e-mail address is not marked as validated.
     */
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

    public boolean isEmailValidated() {
        return emailValidated;
    }

    @Override
    public String toString() {
        return "User [name=" + name + ", email=" + email + (isEmailValidated()?" (validated)":")")+", roles="
                + Arrays.toString(roles.toArray(new String[roles.size()])) + ", accounts="
                + Arrays.toString(accounts.keySet().toArray(new AccountType[accounts.size()])) + "]";
    }

    public String getValidationSecret() {
        return validationSecret;
    }

}
