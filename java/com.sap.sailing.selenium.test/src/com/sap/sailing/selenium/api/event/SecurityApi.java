package com.sap.sailing.selenium.api.event;

import static com.sap.sailing.selenium.api.core.ApiRequest.Context.SECURITY;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.ApiRequest;
import com.sap.sailing.selenium.api.core.JsonWrapper;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.SecurityService;

public class SecurityApi {

    private static final String CREATE_USER_URL = "/api/restsecurity/create_user";
    private static final String GET_USER_URL = "/api/restsecurity/user";
    private static final String SAY_HELLO_URL = "/api/restsecurity/hello";
    
    private TimePoint lastCreateUser;

    public AccessToken createUser(ApiContext ctx, String username, String fullName, String company,
            /* String email, */ String password) {
        final TimePoint now = TimePoint.now();
        if (lastCreateUser != null && now.minus(SecurityService.DEFAULT_CLIENT_IP_BASED_USER_CREATION_LOCKING_DURATION).before(lastCreateUser)) {
            try {
                Thread.sleep(now.until(lastCreateUser.plus(SecurityService.DEFAULT_CLIENT_IP_BASED_USER_CREATION_LOCKING_DURATION)).asMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("username", username);
        queryParams.put("fullName", fullName);
        queryParams.put("company", company);
        // queryParams.put("email", email); //if email is provided a validation mail would be sent
        queryParams.put("password", password);
        final AccessToken result = new AccessToken(ctx.post(CREATE_USER_URL, queryParams));
        lastCreateUser = TimePoint.now();
        return result;
    }

    public static ApiRequest<?, AccessToken> createUser(String username, String fullName, String company,
            /* String email, */ String password) {
        return SECURITY.post(CREATE_USER_URL).queryParam("username", username).queryParam("fullName", fullName)
                .queryParam("company", company).queryParam("password", password).wrapJsonResult(AccessToken.class);
    }

    public static ApiRequest<?, AccessToken> createUser(String username, String password) {
        return SECURITY.post(CREATE_USER_URL).queryParam("username", username).queryParam("fullName", username)
                .queryParam("password", password).wrapJsonResult(AccessToken.class);
    }

    public User getUser(ApiContext ctx, String username) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("username", username);
        return new User(ctx.get(GET_USER_URL, queryParams));
    }

    public Hello sayHello(ApiContext ctx) {
        return new Hello(ctx.get(SAY_HELLO_URL));
    }

    public static class AccessToken extends JsonWrapper {

        public AccessToken(JSONObject json) {
            super(json);
        }

        public String getUsername() {
            return get("username");
        }

        public String getAccessToken() {
            return get("access_token");
        }
    }

    public class User extends JsonWrapper {

        public User(JSONObject json) {
            super(json);
        }

        public String getUsername() {
            return get("username");
        }

        public String getFullName() {
            return get("fullname");
        }
    }

    public class Hello extends JsonWrapper {

        public Hello(JSONObject json) {
            super(json);
        }

        public String getPrincipal() {
            return get("principal");
        }

        public Boolean isAuthenticated() {
            return get("authenticated");
        }
    }
}
