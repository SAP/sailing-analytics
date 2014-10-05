package com.sap.sse.security.ui.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.sap.sse.security.Credential;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.security.ui.Activator;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.oauth.client.SocialUserDTO;
import com.sap.sse.security.ui.oauth.shared.OAuthException;
import com.sap.sse.security.ui.shared.AccountDTO;
import com.sap.sse.security.ui.shared.SuccessInfo;
import com.sap.sse.security.ui.shared.UserDTO;
import com.sap.sse.security.ui.shared.UserManagementService;
import com.sap.sse.security.ui.shared.UsernamePasswordAccountDTO;
import com.sap.sse.security.userstore.shared.Account;
import com.sap.sse.security.userstore.shared.Account.AccountType;
import com.sap.sse.security.userstore.shared.FieldNames.Social;
import com.sap.sse.security.userstore.shared.SocialUserAccount;
import com.sap.sse.security.userstore.shared.User;
import com.sap.sse.security.userstore.shared.UserManagementException;
import com.sap.sse.security.userstore.shared.UserStore;
import com.sap.sse.security.userstore.shared.UsernamePasswordAccount;

public class UserManagementServiceImpl extends RemoteServiceServlet implements UserManagementService {

    private static final long serialVersionUID = 4458564336368629101L;
    
    private static final Logger logger = Logger.getLogger(UserManagementServiceImpl.class.getName());

    private final BundleContext context;
    private FutureTask<SecurityService> securityService;

    public UserManagementServiceImpl() {
        context = Activator.getContext();
        final ServiceTracker<SecurityService, SecurityService> tracker = new ServiceTracker<>(context, SecurityService.class, /* customizer */ null);
        tracker.open();
        setSecurityService(new FutureTask<SecurityService>(new Callable<SecurityService>() {
            @Override
            public SecurityService call() {
                SecurityService result = null;
                try {
                    logger.info("Waiting for SecurityService...");
                    result = tracker.waitForService(0);
                    logger.info("Obtained SecurityService "+getSecurityService());
                    return result;
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Interrupted while waiting for UserStore service", e);
                }
                return result;
            }
        }));
        new Thread("ServiceTracker in bundle com.sap.sse.security.ui waiting for SecurityService") {
            @Override
            public void run() {
                SecurityUtils.setSecurityManager(getSecurityService().getSecurityManager());
            }
        }.start();
    }

    @Override
    public String sayHello() {
        return "Hello";
    }

    @Override
    public Collection<UserDTO> getUserList() {
        List<UserDTO> users = new ArrayList<>();
        for (User u : getSecurityService().getUserList()) {
            UserDTO userDTO = createUserDTOFromUser(u);
            users.add(userDTO);
        }
        return users;
    }
    
    

    @Override
    public UserDTO getCurrentUser() {
        logger.info("Request: " + getThreadLocalRequest().getRequestURL());
        User user = getSecurityService().getCurrentUser();
        if (user == null){
            return null;
        }
        return createUserDTOFromUser(user);
    }

    @Override
    public SuccessInfo login(String username, String password) {
        try {
            String success = getSecurityService().login(username, password);
            return new SuccessInfo(true, success);
        } catch (UserManagementException e) {
            return new SuccessInfo(false, "Failed to login.");
        }
    }

    @Override
    public SuccessInfo logout() {
        logger.info("Logging out user: " + SessionUtils.loadUsername());
        getSecurityService().logout();
        getHttpSession().invalidate();
        logger.info("Invalidated HTTP session");
        return new SuccessInfo(true, "Logged out.");
    }

    @Override
    public UserDTO createSimpleUser(String name, String email, String password) {
        User u = null;
        try {
            u = getSecurityService().createSimpleUser(name, email, password);
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        if (u == null) {
            return null;
        }
        return createUserDTOFromUser(u);
    }

    @Override
    public Collection<UserDTO> getFilteredSortedUserList(String filter) {
        List<UserDTO> users = new ArrayList<>();
        for (User u : getSecurityService().getUserList()) {
            if (filter != null && !"".equals(filter)) {
                if (u.getName().contains(filter)) {
                    users.add(createUserDTOFromUser(u));
                }
            } else {
                users.add(createUserDTOFromUser(u));
            }
        }

        Collections.sort(users, new Comparator<UserDTO>() {

            @Override
            public int compare(UserDTO u1, UserDTO u2) {
                return u1.getName().compareTo(u2.getName());
            }
        });
        return users;
    }

    @Override
    public SuccessInfo addRoleForUser(String username, String role) {
        Subject currentUser = SecurityUtils.getSubject();

        if (currentUser.hasRole(UserStore.DefaultRoles.ADMIN.getName())) {
            User u = getSecurityService().getUserByName(username);
            if (u == null) {
                return new SuccessInfo(false, "User does not exist.");
            }
            try {
                getSecurityService().addRoleForUser(username, role);
                return new SuccessInfo(true, "Added role: " + role + ".");
            } catch (UserManagementException e) {
                return new SuccessInfo(false, e.getMessage());
            }
        } else {
            return new SuccessInfo(false, "You don't have the required permissions to add a role.");
        }
    }

    @Override
    public SuccessInfo deleteUser(String username) {
        try {
            getSecurityService().deleteUser(username);
            return new SuccessInfo(true, "Deleted user: " + username + ".");
        } catch (UserManagementException e) {
            return new SuccessInfo(false, "Could not delete user.");
        }
    }

    private UserDTO createUserDTOFromUser(User user){
        UserDTO userDTO;
        Map<AccountType, Account> accounts = user.getAllAccounts();
        List<AccountDTO> accountDTOs = new ArrayList<>();
        for (Account a : accounts.values()){
            switch (a.getAccountType()) {
            case SOCIAL_USER:
                accountDTOs.add(createSocialUserDTO((SocialUserAccount) a));
                break;

            default:
                UsernamePasswordAccount upa = (UsernamePasswordAccount) a;
                accountDTOs.add(new UsernamePasswordAccountDTO(upa.getName(), upa.getSaltedPassword(), ((ByteSource) upa.getSalt()).getBytes()));
                break;
            }
        }
        userDTO = new UserDTO(user.getName(), user.getEmail(), accountDTOs);
        userDTO.addRoles(user.getRoles());
        return userDTO;
    }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> settings = new TreeMap<String, String>();
        for (Entry<String, Object> e : getSecurityService().getAllSettings().entrySet()){
            settings.put(e.getKey(), e.getValue().toString());
        }
        return settings;
    }

    @Override
    public void setSetting(String key, String clazz, String setting) {
        if (clazz.equals(Boolean.class.getName())){
            getSecurityService().setSetting(key, Boolean.parseBoolean(setting));
        }
        else if (clazz.equals(Integer.class.getName())){
            getSecurityService().setSetting(key, Integer.parseInt(setting));
        }
        else {
            getSecurityService().setSetting(key, setting);
        }
        getSecurityService().refreshSecurityConfig(getServletContext());
    }

    @Override
    public Map<String, String> getSettingTypes() {
        Map<String, String> settingTypes = new TreeMap<String, String>();
        for (Entry<String, Class<?>> e : getSecurityService().getAllSettingTypes().entrySet()){
            settingTypes.put(e.getKey(), e.getValue().getName());
        }
        return settingTypes;
    }
    
    
    
    
    
    
    
    //--------------------------------------------------------- OAuth Implementations -------------------------------------------------------------------------

    @Override
    public String getAuthorizationUrl(CredentialDTO credential) throws OAuthException {
        logger.info("callback url: " + credential.getRedirectUrl());
        String authorizationUrl = null;
        
        try {
            authorizationUrl = getSecurityService().getAuthenticationUrl(createCredentialFromDTO(credential));
        } catch (UserManagementException e) {
            throw new OAuthException(e.getMessage());
        }

        return authorizationUrl;
    }

    @Override
    public UserDTO verifySocialUser(CredentialDTO credentialDTO) {
        
        User user = null;
        try {
            user = getSecurityService().verifySocialUser(createCredentialFromDTO(credentialDTO));
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        return createUserDTOFromUser(user);
    }

    private HttpSession getHttpSession() {
        return getThreadLocalRequest().getSession();
    }

    private Credential createCredentialFromDTO(CredentialDTO credentialDTO){
        Credential credential = new Credential();
        credential.setAuthProvider(credentialDTO.getAuthProvider());
        credential.setAuthProviderName(credentialDTO.getAuthProviderName());
        credential.setEmail(credentialDTO.getEmail());
        credential.setLoginName(credentialDTO.getLoginName());
        credential.setPassword(credentialDTO.getPassword());
        credential.setRedirectUrl(credentialDTO.getRedirectUrl());
        credential.setState(credentialDTO.getState());
        credential.setVerifier(credentialDTO.getVerifier());
        credential.setOauthToken(credentialDTO.getOauthToken());
        return credential;
    }
    
    private SocialUserDTO createSocialUserDTO(SocialUserAccount socialUser){
        SocialUserDTO socialUserDTO = new SocialUserDTO(socialUser.getProperty(Social.PROVIDER.name()));
        socialUserDTO.setSessionId(socialUser.getSessionId());
        
        for (Social s : Social.values()){
            socialUserDTO.setProperty(s.name(), socialUser.getProperty(s.name()));
        }
        return socialUserDTO;
    }

    @Override
    public void addSetting(String key, String clazz, String setting) {
        try {
            getSecurityService().addSetting(key, Class.forName(clazz));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        if (clazz.equals(Boolean.class.getName())){
            getSecurityService().setSetting(key, Boolean.parseBoolean(setting));
        }
        else if (clazz.equals(Integer.class.getName())){
            getSecurityService().setSetting(key, Integer.parseInt(setting));
        }
        else {
            getSecurityService().setSetting(key, setting);
        }
        getSecurityService().refreshSecurityConfig(getServletContext());
    }

    private SecurityService getSecurityService() {
        try {
            return securityService.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void setSecurityService(FutureTask<SecurityService> securityService) {
        this.securityService = securityService;
    }
}
