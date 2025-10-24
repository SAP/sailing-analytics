package com.sap.sse.security;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.builder.api.FlickrApi;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.builder.api.GoogleApi;
import org.scribe.builder.api.ImgUrApi;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.builder.api.LiveApi;
import org.scribe.builder.api.TumblrApi;
import org.scribe.builder.api.TwitterApi;
import org.scribe.builder.api.VimeoApi;
import org.scribe.builder.api.YahooApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.sap.sse.security.interfaces.Credential;
import com.sap.sse.security.interfaces.OAuthToken;
import com.sap.sse.security.interfaces.Social;
import com.sap.sse.security.interfaces.SocialSettingsKeys;
import com.sap.sse.security.shared.SocialUserAccount;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public class OAuthRealm extends AbstractCompositeAuthorizingRealm {
    private static final Logger logger = Logger.getLogger(OAuthRealm.class.getName());
    
    public OAuthRealm() {
        super();
        setAuthenticationTokenClass(OAuthToken.class);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        if (token == null)
            return false;
        if (!(token instanceof OAuthToken))
            return false;
        return true;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        OAuthToken otoken = (OAuthToken) token;
        Credential credential = (Credential) otoken.getCredentials();
        System.out.println("Checking authentication!");
        int authProvider = credential.getAuthProvider();
        logger.info("authProvider: " + authProvider);
        String authProviderName = ClientUtils.getAuthProviderName(authProvider);
        logger.info("Verifying social usr from " + authProviderName);

        Token requestToken = null;
        String yahooGuid = null;
        String protectedResourceUrl = ClientUtils.getProctedResourceUrl(authProvider);

        if (authProvider == ClientUtils.FACEBOOK || authProvider == ClientUtils.INSTAGRAM) {
            logger.info("Verifying state: " + credential.getState());
            // verifyState(credential.getState());
        }
        /* if there is any request token in session, get it */
        requestToken = SessionUtils.getRequestTokenFromSession();
        OAuthService service = null;
        Verifier verifier = null;
        Token accessToken = null;
        /* Get Access Token */
        if (authProvider != ClientUtils.DEFAULT) {
            service = getOAuthService(authProvider);
            verifier = new Verifier(credential.getVerifier());
            logger.info("Requesting access token with requestToken: " + requestToken);
            logger.info("verifier=" + verifier);
            try {
                accessToken = service.getAccessToken(requestToken, verifier);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error receiving request token:", e);
            }
            if (accessToken == null) {
                logger.severe("Could not get Access Token for " + authProviderName);
                throw new AuthenticationException("Could not get Access Token");
            }
            logger.info("Got the access token: " + accessToken);
            logger.info(" Token: " + accessToken.getToken());
            logger.info(" Secret: " + accessToken.getSecret());
            logger.info(" Raw: " + accessToken.getRawResponse());
        }
        // if (authProvider == ClientUtils.INSTAGRAM)
        // {
        // try
        // {
        // instragramToken = InstragramToken.parse(accessToken.getRawResponse());
        // } catch (ParseException e)
        // {
        // throw new RuntimeException("Could not parse " + authProviderName + " Json AccessToken");
        // }
        // logger.info("Getting Instragram Access Token");
        // logger.info(" access token" + instragramToken.getAcessToken());
        // logger.info(" userId: " + instragramToken.getUserId());
        // logger.info(" full name: " + instragramToken.getFullName());
        // logger.info(" username: " + instragramToken.getFullName());
        // logger.info(" raw: " + instragramToken.getRawResponse());
        //
        // // replace userId and access token in protected resource url
        // protectedResourceUrl = ClientUtils.getProctedResourceUrl(authProvider);
        // logger.info("Instragram protected resource url: " + protectedResourceUrl);
        // protectedResourceUrl = String.format(protectedResourceUrl,
        // instragramToken.getUserId(),instragramToken.getAcessToken());
        // logger.info("Instragram protected resource url: " + protectedResourceUrl);
        // }

        if (authProvider == ClientUtils.GITHUB || authProvider == ClientUtils.FOURSQUARE) {
            protectedResourceUrl = String.format(protectedResourceUrl, accessToken.getToken());
        }
        if (authProvider == ClientUtils.YAHOO) {
            // throw new OurException("Not implemented for yahoo yet!)");
            /* we need to replace <GUID> */
            yahooGuid = getQueryStringValue(accessToken.getRawResponse(), "xoauth_yahoo_guid");
            if (yahooGuid == null) {
                throw new RuntimeException("Could not get Yahoo GUID from Query String");
            }
            // must save it to session. we'll use to get the user profile
            SessionUtils.saveYahooGuidToSession(yahooGuid);

            protectedResourceUrl = ClientUtils.getProctedResourceUrl(authProvider);
            protectedResourceUrl = String.format(protectedResourceUrl, yahooGuid);
            logger.info("Yahoo protected resource url: " + protectedResourceUrl);

        }
        // make session id
        String sessionId = makeRandomString();
        // must save session id to session
        SessionUtils.saveSessionIdToSession(sessionId);
        // must save authProvider to session
        SessionUtils.saveAuthProviderToSession(authProvider);
        SocialUserAccount socialUser = null;
        // must save acess token to session
        SessionUtils.saveAccessTokenToSession(accessToken);
        // must save the protected resource url to session
        SessionUtils.saveProtectedResourceUrlToSession(protectedResourceUrl);
        // now request protected resource
        logger.info("Getting protected resource");
        logger.info("Protected resource url: " + protectedResourceUrl);
        try {
            OAuthRequest request = new OAuthRequest(Verb.GET, protectedResourceUrl);
            service.signRequest(accessToken, request);

            Response response = request.send();
            logger.info("Status code: " + response.getCode());
            logger.info("Body: " + response.getBody());

            String json = response.getBody();
            socialUser = getSocialUserFromJson(json, authProvider);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not retrieve protected resource: ", e);
            throw new RuntimeException("Could not retrieve protected resource: " + e);
        }
        
        if (socialUser.getProperty("NAME") == null){
            throw new AuthenticationException("Username cannot be null!");
        }
        
        socialUser.setSessionId(sessionId);
        socialUser.setProperty(Social.PROVIDER.name(), authProviderName);
        
        String socialname = authProviderName + "*" + socialUser.getProperty(Social.NAME.name());

        User user = getUserStore().getUserByName(socialname);
        if (user == null) {
            try {
                UserGroup tenant = getUserStore().createUserGroup(UUID.randomUUID(), socialname + SecurityService.TENANT_SUFFIX);
                getAccessControlStore().setOwnership(tenant.getIdentifier(), user, tenant, tenant.getName());
                user = getUserStore().createUser(socialname, socialUser.getProperty(Social.EMAIL.name()), new LockingAndBanningImpl(), socialUser);
                tenant.add(user);
                getUserStore().updateUserGroup(tenant);
            } catch (UserManagementException | UserGroupManagementException e) {
                throw new AuthenticationException(e.getMessage());
            }
        }
        SimpleAuthenticationInfo sai = new SimpleAuthenticationInfo();
        SimplePrincipalCollection spc = new SimplePrincipalCollection();
        spc.add(otoken.getPrincipal(), otoken.getPrincipal().toString());
        sai.setCredentials(otoken.getCredentials());
        sai.setPrincipals(spc);
        return sai;
    }

    private OAuthService getOAuthService(int authProvider) throws AuthenticationException {
        OAuthService service = null;
        switch (authProvider) {
        case ClientUtils.FACEBOOK: {
            service = new ServiceBuilder().provider(FacebookApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_FACEBOOK_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_FACEBOOK_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.GOOGLE: {
            service = new ServiceBuilder().provider(GoogleApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_GOOGLE_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_GOOGLE_APP_SECRET.name(), String.class)).scope(getUserStore().getSetting(SocialSettingsKeys.OAUTH_GOOGLE_SCOPE.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();

            break;
        }

        case ClientUtils.TWITTER: {
            service = new ServiceBuilder().provider(TwitterApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_TWITTER_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_TWITTER_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }
        case ClientUtils.YAHOO: {
            service = new ServiceBuilder().provider(YahooApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_YAHOO_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_YAHOO_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.LINKEDIN: {
            service = new ServiceBuilder().provider(LinkedInApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_LINKEDIN_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_LINKEDIN_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.INSTAGRAM: {
            service = new ServiceBuilder().provider(InstagramApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_INSTAGRAM_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_INSTAGRAM_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.GITHUB: {
            service = new ServiceBuilder().provider(GithubApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_GITHUB_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_GITHUB_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;

        }

        case ClientUtils.IMGUR: {
            service = new ServiceBuilder().provider(ImgUrApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_IMGUR_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_IMGUR_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.FLICKR: {
            service = new ServiceBuilder().provider(FlickrApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_FLICKR_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_FLICKR_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.VIMEO: {
            service = new ServiceBuilder().provider(VimeoApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_VIMEO_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_VIMEO_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.WINDOWS_LIVE: {
            // a Scope must be specified
            service = new ServiceBuilder().provider(LiveApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_WINDOWS_LIVE_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_WINDOWS_LIVE_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl())
                    .scope("wl.basic").build();
            break;
        }

        case ClientUtils.TUMBLR: {
            service = new ServiceBuilder().provider(TumblrApi.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_TUMBLR_LIVE_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_TUMBLR_LIVE_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.FOURSQUARE: {
            service = new ServiceBuilder().provider(Foursquare2Api.class).apiKey(getUserStore().getSetting(SocialSettingsKeys.OAUTH_FOURSQUARE_APP_ID.name(), String.class))
                    .apiSecret(getUserStore().getSetting(SocialSettingsKeys.OAUTH_FOURSQUARE_APP_SECRET.name(), String.class)).callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        default: {
            return null;
        }

        }
        return service;
    }

    public static String stackTraceToString(Throwable caught) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : caught.getStackTrace()) {
            sb.append(e.toString()).append("\n");
        }
        return sb.toString();
    }

    private String makeRandomString() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private String getQueryStringValue(String qs, String name) {
        Map<String, String> map = parseQueryString(qs);
        return map.get(name);
    }

    private static Map<String, String> parseQueryString(String qs) {
        String[] ps = qs.split("&");
        Map<String, String> map = new HashMap<String, String>();

        for (String p : ps) {
            String k = p.split("=")[0];
            String v = p.split("=")[1];
            map.put(k, v);
        }
        return map;
    }

    private SocialUserAccount getSocialUserFromJson(String json, int authProvider) throws AuthenticationException {
        String authProviderName = ClientUtils.getAuthProviderName(authProvider);
        Object obj = null;
        JSONParser jsonParser = new JSONParser();
        SocialUserAccount socialUser = new SocialUserAccount();
        switch (authProvider) {
        case ClientUtils.FACEBOOK: {
            /*
             * --Facebook-- { "id":"537157209", "name":"Muhammad Muquit", "first_name":"Muhammad", "last_name":"Muquit",
             * "link":"http:\/\/www.facebook.com\/muhammad.muquit", "username":"muhammad.muquit", "gender":"male",
             * "timezone":-5,"locale":"en_US", "verified":true, "updated_time":"2012-11-10T23:13:04+0000"} }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;

                socialUser.setProperty(Social.NAME.name(),(String) jsonObj.get("name"));
                socialUser.setProperty(Social.FIRSTNAME.name(),(String) jsonObj.get("first_name"));
                socialUser.setProperty(Social.LASTNAME.name(),(String) jsonObj.get("last_name"));
                socialUser.setProperty(Social.GENDER.name(),(String) jsonObj.get("gender"));
                socialUser.setProperty(Social.EMAIL.name(),(String) jsonObj.get("email"));

                socialUser.setProperty(Social.JSON.name(),json);

                return socialUser;
            } catch (ParseException pe) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + pe.getMessage());
            }
        }

        case ClientUtils.YAHOO: {
            /*
             * --YAHOO-- http://developer.yahoo.com/social/rest_api_guide/extended-profile-resource.html# { "profile": {
             * "uri": "http:\/\/social.yahooapis.com\/v1\/user\/ECUFIYO7BLY5FOV54XAPEQDC3Y\/profile", "guid":
             * "ECUFIYO7BLY5FOAPEQDC3Y", "birthYear": 1969, "created": "2010-01-23T13:07:10Z", "displayAge": 89,
             * "gender": "M", "image": { "height": 192, "imageUrl":
             * "http:\/\/l.yimg.com\/a\/i\/identity2\/profile_192c.png", "size": "192x192", "width": 192 }, "location":
             * "Philadelphia, Pennsylvania", "memberSince": "2006-08-04T13:27:58Z", "nickname": "jdoe", "profileUrl":
             * "http:\/\/profile.yahoo.com\/ECUFIYO7BLY5FOV54XAPEQDC3Y", "searchable": false, "updated":
             * "2011-04-16T07:28:00Z", "isConnected": false } }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;
                // get profile object
                JSONObject jsonObjPeople = (JSONObject) jsonObj.get("profile");

                socialUser.setProperty(Social.JSON.name(),json);

                socialUser.setProperty(Social.NICKNAME.name(),(String) jsonObjPeople.get("nickname"));
                socialUser.setProperty(Social.GENDER.name(),(String) jsonObjPeople.get("gender"));
                socialUser.setProperty(Social.FIRSTNAME.name(),(String) jsonObjPeople.get("givenName"));
                socialUser.setProperty(Social.LASTNAME.name(),(String) jsonObjPeople.get("familyName"));

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.GOOGLE: {
            /*
             * --Google-- { "id": "116397076041912827850", "name": "Muhammad Muquit", "given_name": "Muhammad",
             * "family_name": "Muquit", "link": "https://plus.google.com/116397076041912827850", "gender": "male",
             * "locale": "en-US" }
             */

            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;

                socialUser.setProperty(Social.JSON.name(),json);

                socialUser.setProperty(Social.NAME.name(),(String) jsonObj.get("name"));
                socialUser.setProperty(Social.FIRSTNAME.name(),(String) jsonObj.get("given_name"));
                socialUser.setProperty(Social.LASTNAME.name(),(String) jsonObj.get("family_name"));
                socialUser.setProperty(Social.GENDER.name(),(String) jsonObj.get("gender"));

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.LINKEDIN: {
            /*
             * --Linkedin-- { "firstName": "Muhammad", "headline": "Sr. Software Engineer at British Telecom",
             * "lastName": "Muquit", }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;

                socialUser.setProperty(Social.JSON.name(),json);

                socialUser.setProperty(Social.FIRSTNAME.name(),(String) jsonObj.get("firstName"));
                socialUser.setProperty(Social.LASTNAME.name(),(String) jsonObj.get("lastName"));

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.TWITTER: {
            /*
             * --Twitter -- { "id":955924206, "contributors_enabled":false, "profile_use_background_image":true,
             * "time_zone":"Eastern Time (US & Canada)", "following":false, "friends_count":3, "profile_text_color":
             * "333333", "geo_enabled":false, "created_at":"Sun Nov 18 17:54:22 +0000 2012", "utc_offset":-18000,
             * "follow_request_sent":false, "name":"Muhammad Muquit", "id_str":"955924206",
             * "default_profile_image":true, "verified":false, "profile_sidebar_border_color":"C0DEED", "url":null,
             * "favourites_count":0, .. "lang":"en", "profile_background_color":"C0DEED", "screen_name":"mmqt2012", .. }
             */

            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;

                socialUser.setProperty(Social.JSON.name(),json);

                socialUser.setProperty(Social.NAME.name(),(String) jsonObj.get("name"));
                socialUser.setProperty(Social.GENDER.name(),(String) jsonObj.get("gender"));

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.INSTAGRAM: {
            /*
             * -- Instragram -- { "data": { "id": "1574083", "username": "snoopdogg", "full_name": "Snoop Dogg",
             * "profile_picture": "http://distillery.s3.amazonaws.com/profiles/profile_1574083_75sq_1295469061.jpg",
             * "bio": "This is my bio", "website": "http://snoopdogg.com", "counts": { "media": 1320, "follows": 420,
             * "followed_by": 3410 } }
             */

            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;
                // get profile object
                JSONObject jsonObjData = (JSONObject) jsonObj.get("data");

                socialUser.setProperty(Social.JSON.name(),json);
                socialUser.setProperty(Social.NAME.name(),(String) jsonObjData.get("username"));

                return socialUser;

            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }

        }

        case ClientUtils.GITHUB: {
            /*
             * -- github -- { "plan":{ "private_repos":0, "space":307200, "name":"free", "collaborators":0 },
             * "followers":0, "type":"User", "events_url":"https://api.github.com/users/oauthdemo2012/events{/privacy}",
             * "owned_private_repos":0, "public_gists":0, "avatar_url":
             * "https://secure.gravatar.com/avatar/e0cb08c2b353cc1c3022dc65ebd060d1?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-user-420.png"
             * , "received_events_url":"https://api.github.com/users/oauthdemo2012/received_events", "private_gists":0,
             * "disk_usage":0, "url":"https://api.github.com/users/oauthdemo2012",
             * "followers_url":"https://api.github.com/users/oauthdemo2012/followers", "login":"oauthdemo2012",
             * "created_at":"2012-12-20T01:36:36Z",
             * "following_url":"https://api.github.com/users/oauthdemo2012/following",
             * "organizations_url":"https://api.github.com/users/oauthdemo2012/orgs", "following":0,
             * "starred_url":"https://api.github.com/users/oauthdemo2012/starred{/owner}{/repo}", "collaborators":0,
             * "public_repos":0, "repos_url":"https://api.github.com/users/oauthdemo2012/repos",
             * "gists_url":"https://api.github.com/users/oauthdemo2012/gists{/gist_id}", "id":3085592,
             * "total_private_repos":0, "html_url":"https://github.com/oauthdemo2012",
             * "subscriptions_url":"https://api.github.com/users/oauthdemo2012/subscriptions",
             * "gravatar_id":"e0cb08c2b353cc1c3022dc65ebd060d1" }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;

                socialUser.setProperty(Social.JSON.name(),json);
                socialUser.setProperty(Social.NAME.name(),(String) jsonObj.get("login"));

                return socialUser;

            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.FLICKR: {
            /*
             * -- flickr -- { "user": { "id": "91390211@N06", "username": { "_content": "oauthdemo2012" } }, "stat":
             * "ok" }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;
                JSONObject jsonObjUser = (JSONObject) jsonObj.get("user");
                JSONObject jsonObjUsername = (JSONObject) jsonObjUser.get("username");
                socialUser.setProperty(Social.NAME.name(),(String) jsonObjUsername.get("_content"));
                socialUser.setProperty(Social.JSON.name(),json);

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.VIMEO: {
            /*
             * --Vimeo starts -- { "generated_in": "0.0698", "stat": "ok", "person": { "created_on":
             * "2012-12-22 23:37:55", "id": "15432968", "is_contact": "0", "is_plus": "0", "is_pro": "0", "is_staff":
             * "0", "is_subscribed_to": "0", "username": "user15432968", "display_name": "oauthdemo2012", "location":
             * "", "url": [ "" ], ..... } }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;
                JSONObject jsonObjPerson = (JSONObject) jsonObj.get("person");
                String userName = (String) jsonObjPerson.get("username");
                String displayName = (String) jsonObjPerson.get("display_name");

                if (displayName != null) {
                    socialUser.setProperty(Social.NAME.name(),displayName);
                } else if (userName != null) {
                    socialUser.setProperty(Social.NAME.name(),userName);
                } else {
                    socialUser.setProperty(Social.NAME.name(),"Unknown");
                }
                socialUser.setProperty(Social.JSON.name(),json);

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.WINDOWS_LIVE: {
            /*
             * Windows Live --starts -- { "id" : "contact.c1678ab4000000000000000000000000", "first_name" : "Roberto",
             * "last_name" : "Tamburello", "name" : "Roberto Tamburello", "gender" : "male", "locale" : "en_US" }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;
                JSONObject jsonErrorObj = (JSONObject) jsonObj.get("error");
                if (jsonErrorObj != null) {
                    /*
                     * { "error": { "code": "request_token_too_many", "message":
                     * "The request includes more than one access token. Only one access token is allowed." } }
                     */
                    String message = (String) jsonErrorObj.get("message");
                    throw new AuthenticationException("Error: " + message);
                }
                socialUser.setProperty(Social.NAME.name(),(String) jsonObj.get("name"));
                socialUser.setProperty(Social.LASTNAME.name(),(String) jsonObj.get("last_name"));
                socialUser.setProperty(Social.FIRSTNAME.name(),(String) jsonObj.get("first_name"));
                socialUser.setProperty(Social.JSON.name(),json);

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.TUMBLR: {
            /*
             * tumblr. -- { "meta": { "status": 200, "msg": "OK" }, "response": { "user": { "name": "oauthdemo2012",
             * "likes": 0, "following": 1, "default_post_format": "html", "blogs": [ { "name": "oauthdemo2012", "url":
             * "http:\/\/oauthdemo2012.tumblr.com\/", "followers": 0, "primary": true, "title": "Untitled",
             * "description": "", "admin": true, "updated": 0, "posts": 0, "messages": 0, "queue": 0, "drafts": 0,
             * "share_likes": true, "ask": false, "tweet": "N", "facebook": "N", "facebook_opengraph_enabled": "N",
             * "type": "public" } ] } } }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;
                JSONObject jsonObjResponse = (JSONObject) jsonObj.get("response");
                JSONObject jsonObjUser = (JSONObject) jsonObjResponse.get("user");
                String userName = (String) jsonObjUser.get("name");
                socialUser.setProperty(Social.NAME.name(),userName);
                socialUser.setProperty(Social.JSON.name(),json);

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        case ClientUtils.FOURSQUARE: {

            /*
             * foursquare -- { "meta": { "code": 200, "errorType": "deprecated", "errorDetail":
             * "Please provide an API version to avoid future errors.See http://bit.ly/vywCav" }, "notifications": [ {
             * "type": "notificationTray", "item": { "unreadCount": 0 } } ], "response": { "user": { "id": "43999331",
             * "firstName": "OAuth", "lastName": "Demo", "gender": "none", "relationship": "self", "photo":
             * "https://foursquare.com/img/blank_boy.png", "friends": { "count": 0, "groups": [ { "type": "friends",
             * "name": "Mutual friends", "count": 0, "items": [] }, { "type": "others", "name": "Other friends",
             * "count": 0, "items": [] } ] }, ...... } } }
             */
            try {
                obj = jsonParser.parse(json);
                JSONObject jsonObj = (JSONObject) obj;
                JSONObject jsonObjResponse = (JSONObject) jsonObj.get("response");
                JSONObject jsonObjUser = (JSONObject) jsonObjResponse.get("user");
                String firstName = (String) jsonObjUser.get("firstName");
                String lastName = (String) jsonObjUser.get("lastName");
                if (firstName != null && lastName != null) {
                    socialUser.setProperty(Social.NAME.name(),firstName + " " + lastName);
                } else {
                    socialUser.setProperty(Social.NAME.name(),"UNKNOWN");
                }
                socialUser.setProperty(Social.NAME.name(),json);

                return socialUser;
            } catch (Exception e) {
                throw new AuthenticationException("Could not parse JSON data from " + authProviderName + ":"
                        + e.getMessage());
            }
        }

        default: {
            throw new AuthenticationException("Unknown Auth Provider: " + authProviderName);
        }
        }

        /*
         * We don't use Gson() anymore as it choked on nested Facebook JSON data Dec-03-2012
         */

        /*
         * // map json to SocialUser try { Gson gson = new Gson(); SocialUser user =
         * gson.fromJson(json,SocialUser.class); // pretty print json //gson = new
         * GsonBuilder().setPrettyPrinting().create(); //String jsonPretty = gson.toJson(json); user.setJson(json);
         * return user; } catch (Exception e) { e.printStackTrace(); throw new
         * AuthenticationException("Could not map userinfo JSON to SocialUser class: " + e); }
         */
    }
}
