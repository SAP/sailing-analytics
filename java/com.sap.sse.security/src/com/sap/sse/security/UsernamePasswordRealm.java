package com.sap.sse.security;

import java.util.logging.Logger;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;

import com.sap.sse.security.interfaces.SimpleSaltedAuthenticationInfo;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.UsernamePasswordAccount;
import com.sap.sse.security.shared.impl.User;

public class UsernamePasswordRealm extends AbstractCompositeAuthorizingRealm {
    private static final Logger logger = Logger.getLogger(UsernamePasswordRealm.class.getName());

    public UsernamePasswordRealm() {
        super();
        setAuthenticationTokenClass(UsernamePasswordToken.class);
    }
    
    @Override
    public boolean supports(AuthenticationToken token) {
        final boolean result;
        if (token == null) {
            result = false;
        } else if (! (token instanceof UsernamePasswordToken)) {
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        final UsernamePasswordToken userPassToken = (UsernamePasswordToken) token;
        final String username = userPassToken.getUsername();
        if (username == null) {
            return null;
        }
        // read password hash and salt from db
        String saltedPassword = null;
        byte[] salt = null;
        final User user = getUserStore().getUserByName(username);
        if (user == null) {
            logger.warning("Rejecting authentication attempt for non-existing user "+username);
            return null;
        }
        if (user.getLockingAndBanning().isAuthenticationLocked()) {
            logger.warning("Rejected attempt to authenticate user "+username+" because it is locked: "+user.getLockingAndBanning());
            throw new LockedAccountException("Password authentication for user "+username+" is currently locked");
        }
        final UsernamePasswordAccount upa = (UsernamePasswordAccount) user.getAccount(AccountType.USERNAME_PASSWORD);
        if (upa == null){
            return null;
        }
        saltedPassword = upa.getSaltedPassword();
        salt = upa.getSalt();
        if (saltedPassword == null) {
            return null;
        }
        if (salt == null) {
            return null;
        }
        // return salted credentials
        SaltedAuthenticationInfo sai = new SimpleSaltedAuthenticationInfo(username, saltedPassword, salt);
        return sai;
    }
}
